package com.g42.platform.gms.hikvision.sync;

import com.g42.platform.gms.manager.schedule.infrastructure.entity.WorkShiftJpa;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Resolves the appropriate work shift for a given scan time.
 *
 * Priority logic:
 * Step 1: Find the shift with start_time closest to scanTime,
 *         where scanTime >= start_time - 30 minutes (early scan allowed).
 * Step 2 (fallback): If no shift satisfies Step 1,
 *         find the shift with minimum |scanTime - startTime| within ±2 hours.
 */
@Slf4j
@Component
public class ShiftResolver {

    private static final long EARLY_SCAN_MINUTES = 30;
    private static final long MAX_OFFSET_MINUTES = 120; // ±2 hours

    /**
     * Resolve the best matching active shift for the given scan time.
     *
     * @param scanTime     the time the employee scanned
     * @param activeShifts list of active shifts (is_active = true)
     * @return the best matching shift, or empty if none found
     */
    public Optional<WorkShiftJpa> resolve(LocalTime scanTime, List<WorkShiftJpa> activeShifts) {
        if (activeShifts == null || activeShifts.isEmpty()) {
            return Optional.empty();
        }

        // Step 1: scanTime >= start_time - 30 min, pick closest start_time
        Optional<WorkShiftJpa> step1Result = activeShifts.stream()
                .filter(shift -> !scanTime.isBefore(shift.getStartTime().minusMinutes(EARLY_SCAN_MINUTES)))
                .min(Comparator.comparingLong(shift ->
                        Math.abs(Duration.between(shift.getStartTime(), scanTime).toMinutes())));

        if (step1Result.isPresent()) {
            return step1Result;
        }

        // Step 2 (fallback): pick shift with minimum |scanTime - startTime| within ±2 hours
        Optional<WorkShiftJpa> step2Result = activeShifts.stream()
                .filter(shift -> Math.abs(Duration.between(shift.getStartTime(), scanTime).toMinutes()) <= MAX_OFFSET_MINUTES)
                .min(Comparator.comparingLong(shift ->
                        Math.abs(Duration.between(shift.getStartTime(), scanTime).toMinutes())));

        if (step2Result.isEmpty()) {
            log.warn("ShiftResolver: no shift found within ±2h for scanTime={}", scanTime);
        }

        return step2Result;
    }
}
