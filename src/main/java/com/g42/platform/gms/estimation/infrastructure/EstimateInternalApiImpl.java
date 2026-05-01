package com.g42.platform.gms.estimation.infrastructure;

import com.g42.platform.gms.estimation.api.internal.EstimateInternalApi;
import com.g42.platform.gms.estimation.domain.entity.Estimate;
import com.g42.platform.gms.estimation.domain.exception.EstimateErrorCode;
import com.g42.platform.gms.estimation.domain.exception.EstimateException;
import com.g42.platform.gms.estimation.infrastructure.entity.EstimateItemJpa;
import com.g42.platform.gms.estimation.infrastructure.entity.EstimateJpa;
import com.g42.platform.gms.estimation.infrastructure.entity.ServiceReminderJpa;
import com.g42.platform.gms.estimation.infrastructure.entity.StockAllocationJpa;
import com.g42.platform.gms.estimation.infrastructure.mapper.EstimateJpaMapper;
import com.g42.platform.gms.estimation.infrastructure.repository.EstimateItemRepositoryJpa;
import com.g42.platform.gms.estimation.infrastructure.repository.EstimateRepositoryJpa;
import com.g42.platform.gms.estimation.infrastructure.repository.ServiceRemindJpaRepo;
import com.g42.platform.gms.estimation.infrastructure.repository.StockAllocationRepositoryJpa;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class EstimateInternalApiImpl implements EstimateInternalApi {
    @Autowired
    private EstimateRepositoryJpa estimateRepositoryJpa;
    @Autowired
    private EstimateJpaMapper estimateJpaMapper;
    @Autowired
    private ServiceRemindJpaRepo serviceRemindJpaRepo;
    @Autowired
    private StockAllocationRepositoryJpa stockAllocationJpaRepo;
    @Autowired
    private EstimateItemRepositoryJpa estimateItemRepositoryJpa;

    @Override
    public List<Estimate> findAllByServiceTicketId(List<Integer> ticketIds) {
        return estimateRepositoryJpa.findByServiceTicketIdsAndVersionTop(ticketIds).stream().map(estimateJpaMapper::toDomain).toList();
    }

    @Override
    public Estimate findLatestByServiceTicketId(Integer serviceTicketId) {
        if (serviceTicketId == null) {
            return null;
        }
        return estimateRepositoryJpa.findTopByServiceTicketIdOrderByVersionDesc(serviceTicketId)
                .map(estimateJpaMapper::toDomain)
                .orElse(null);
    }

    @Override
    public Estimate findById(Integer estimateId) {
        if (estimateId == null) {
            return null;
        }
        return estimateRepositoryJpa.findById(estimateId)
                .map(estimateJpaMapper::toDomain)
                .orElse(null);
    }

    @Override
    public void linkEstimateToServiceTicket(Integer estimateId, Integer serviceTicketId) {
        if (estimateId == null || serviceTicketId == null) {
            return;
        }
        EstimateJpa estimateJpa = estimateRepositoryJpa.findById(estimateId).orElse(null);
        if (estimateJpa == null) {
            return;
        }
        estimateJpa.setServiceTicketId(serviceTicketId);
        estimateRepositoryJpa.save(estimateJpa);
    }

    @Override
    public void updateBookingToRemindById(Integer reminderId, Integer bookingId) {
        ServiceReminderJpa sr = serviceRemindJpaRepo.findById(reminderId).orElse(null);
        if (sr==null||bookingId==null) {
            System.err.println("INVALID REMINDER/BOOKING ID");
            return;
        }
        sr.setStatus("BOOKED");
        sr.setBookingId(bookingId);
        serviceRemindJpaRepo.save(sr);
    }

    @Override
    public Integer releaseEstimate(Integer allocationId, Integer returnQuantity, Integer staffId) {
        StockAllocationJpa stockAllocation = stockAllocationJpaRepo.findById(allocationId).orElse(null);
        if (stockAllocation==null) {
            throw new EstimateException("Allocation:"+allocationId+" 404",EstimateErrorCode.BAD_REQUEST);
        }
        EstimateItemJpa estimateItemJpa = estimateItemRepositoryJpa.findById(stockAllocation.getEstimateItemId()).orElse(null);
        if (estimateItemJpa==null || estimateItemJpa.getQuantity()==null) {
            throw new EstimateException("EstimateItem of this Allocation:"+allocationId+" 404",EstimateErrorCode.BAD_REQUEST);
        }
        Integer oldQty = estimateItemJpa.getQuantity();
        if (oldQty.equals(returnQuantity)) {
            estimateItemJpa.setIsChecked(false);
            EstimateItemJpa saved = estimateItemRepositoryJpa.save(estimateItemJpa);
            return saved.getId();
        }
        EstimateItemJpa returnEstimate = new EstimateItemJpa();
        BeanUtils.copyProperties(estimateItemJpa,returnEstimate,"id");

        estimateItemJpa.setQuantity(estimateItemJpa.getQuantity() - returnQuantity);
        recalculateItemPrices(estimateItemJpa,oldQty);
        estimateItemRepositoryJpa.save(estimateItemJpa);

        returnEstimate.setIsChecked(false);
        returnEstimate.setQuantity(returnQuantity);
        recalculateItemPrices(returnEstimate,oldQty);
        EstimateItemJpa saved = estimateItemRepositoryJpa.save(returnEstimate);
        return saved.getId();
    }

    private void recalculateItemPrices(EstimateItemJpa estimateItemJpa, Integer oldQuantity) {
        if (estimateItemJpa.getUnitPrice()==null || estimateItemJpa.getQuantity()==null) return;
        //todo calculate price based on promotions, tax and quantity
        BigDecimal totalPrice = estimateItemJpa.getUnitPrice().multiply(BigDecimal.valueOf(estimateItemJpa.getQuantity()));
        estimateItemJpa.setTotalPrice(totalPrice);
        //discount
        BigDecimal newDiscount = BigDecimal.ZERO;
        if (estimateItemJpa.getDiscountAmount()!=null) {
            BigDecimal unitDiscount = estimateItemJpa.getDiscountAmount().divide(BigDecimal.valueOf(oldQuantity),2, RoundingMode.HALF_UP);
            newDiscount = unitDiscount.multiply(BigDecimal.valueOf(estimateItemJpa.getQuantity()));
        }
        estimateItemJpa.setDiscountAmount(newDiscount);
        BigDecimal finalPrice = totalPrice.subtract(newDiscount);
        //tax
        BigDecimal newTaxAmount = BigDecimal.ZERO;
        if (estimateItemJpa.getAppliedTaxRate()!=null) {
            BigDecimal taxRate = estimateItemJpa.getAppliedTaxRate().divide(BigDecimal.valueOf(100),4, RoundingMode.HALF_UP);
            newTaxAmount = finalPrice.multiply(taxRate).setScale(0, RoundingMode.HALF_UP);
        }
        estimateItemJpa.setTaxAmount(newTaxAmount);
        finalPrice = finalPrice.add(estimateItemJpa.getTaxAmount());
        estimateItemJpa.setFinalPrice(finalPrice);

    }
}
