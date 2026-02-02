package com.g42.platform.gms.booking.service;

import com.g42.platform.gms.auth.entity.CustomerProfile;
import com.g42.platform.gms.auth.repository.CustomerProfileRepository;
import com.g42.platform.gms.booking.dto.BookingRequest;
import com.g42.platform.gms.booking.entity.*;
import com.g42.platform.gms.booking.repository.BookingRepository;
import com.g42.platform.gms.catalog.repository.CatalogItemRepository;
import com.g42.platform.gms.vehicle.service.VehicleService; // Gọi sang Domain Vehicle
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public class BookingService {

    @Autowired private BookingRepository bookingRepo;
    @Autowired private CustomerProfileRepository customerRepo;
    @Autowired private VehicleService vehicleService; // Dùng Service của Vehicle
    @Autowired private CatalogItemRepository catalogRepo;

    @Transactional
    public Booking createBooking(BookingRequest request) {
        CustomerProfile customer;
        boolean isGuest = false;

        // 1. XÁC THỰC KHÁCH HÀNG
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
            // Đã đăng nhập (Lấy phone từ Token)
            customer = customerRepo.findByPhone(auth.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));
        } else {
            // Khách vãng lai (Guest) -> Cần Validate OTP ở đây (tạm bỏ qua code check OTP)
            isGuest = true;
            String phone = request.getPhoneNumber();
            // Tìm xem SĐT này có trong DB chưa, chưa thì tạo profile "ngầm"
            customer = customerRepo.findByPhone(phone).orElseGet(() -> {
                CustomerProfile newCus = new CustomerProfile();
                newCus.setPhone(phone);
                newCus.setFullName(request.getFullName() != null ? request.getFullName() : "Khách vãng lai");
                return customerRepo.save(newCus);
            });
        }

        // 2. XỬ LÝ XE (Gọi sang Domain Vehicle)
        var vehicle = vehicleService.findOrCreateVehicle(
                request.getLicensePlate(),
                request.getCarBrand(),
                request.getCarModel(),
                customer
        );

        // 3. TẠO BOOKING
        Booking booking = new Booking();
        booking.setCustomer(customer);
        booking.setVehicle(vehicle);
        booking.setScheduledDate(request.getAppointmentDate());
        booking.setScheduledTime(request.getAppointmentTime());
        booking.setIsGuest(isGuest);
        booking.setStatus(BookingStatus.NEW);

        // 4. XỬ LÝ MÔ TẢ & DỊCH VỤ
        StringBuilder finalDesc = new StringBuilder();
        if (request.getUserNote() != null) finalDesc.append(request.getUserNote());

        if (request.getSelectedServiceIds() != null && !request.getSelectedServiceIds().isEmpty()) {
            booking.setServices(catalogRepo.findAllById(request.getSelectedServiceIds()));
        } else {
            booking.setServices(new ArrayList<>()); // Không chọn dịch vụ
            if (finalDesc.length() == 0) throw new RuntimeException("Vui lòng nhập mô tả tình trạng xe!");
        }
        booking.setDescription(finalDesc.toString());

        return bookingRepo.save(booking);
    }
}