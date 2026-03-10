package com.g42.platform.gms.integration.booking;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.g42.platform.gms.auth.entity.CustomerPrincipal;
import com.g42.platform.gms.booking.customer.api.dto.CustomerBookingRequest;
import com.g42.platform.gms.booking.customer.api.dto.ModifyBookingRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CustomerBookingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private CustomerBookingRequest baseRequest() {

        CustomerBookingRequest request = new CustomerBookingRequest();

        request.setAppointmentDate(LocalDate.now().plusDays(1));
        request.setAppointmentTime(LocalTime.of(10,0));
        request.setUserNote("Integration test booking");

        request.setSelectedServiceIds(List.of(1));

        return request;
    }

    // ================= CREATE BOOKING =================

    @Test
    @DisplayName("CB_001 - Happy case create booking")
    void createBookingHappyCase() throws Exception {

        CustomerBookingRequest request = baseRequest();

        CustomerPrincipal principal =
                new CustomerPrincipal(1, "0966720776", "HDTVux");

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
                );

        mockMvc.perform(post("/api/booking/customer/create")
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk());
    }

    // ================= GET BOOKINGS =================

    @Test
    @DisplayName("CB_003 - Get my bookings")
    void getMyBookings() throws Exception {

        CustomerPrincipal principal =
                new CustomerPrincipal(1, "0901234567", "Test Customer");

        mockMvc.perform(get("/api/booking/customer/my-bookings")
                        .with(authentication(
                                new UsernamePasswordAuthenticationToken(
                                        principal,
                                        null,
                                        principal.getAuthorities()
                                )
                        ))).andDo(print())

                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // ================= MODIFY BOOKING =================

    @Test
    @DisplayName("CB_004 - Modify booking")
    void modifyBooking() throws Exception {

        ModifyBookingRequest request = new ModifyBookingRequest();
        request.setNewAppointmentDate(LocalDate.now().plusDays(2));
        request.setNewAppointmentTime(LocalTime.of(11, 0));
        request.setNewUserNote("Modify test");
        request.setNewServiceIds(List.of(1));

        CustomerPrincipal principal =
                new CustomerPrincipal(1, "0123456789", "Test Customer");

        mockMvc.perform(put("/api/booking/customer/37/modify")
                        .with(authentication(
                                new UsernamePasswordAuthenticationToken(
                                        principal,
                                        null,
                                        principal.getAuthorities()
                                )
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                ).andDo(print())
                .andExpect(status().isOk());
    }

    // ================= CANCEL BOOKING =================

    @Test
    @DisplayName("CB_005 - Cancel booking")
    void cancelBooking() throws Exception {

        CustomerPrincipal principal =
                new CustomerPrincipal(1, "0966720776", "HDTVux");

        Authentication auth =
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
                );

        mockMvc.perform(post("/api/booking/customer/42/cancel")
                        .with(authentication(auth))).andDo(print())
                .andExpect(status().isOk());
    }

}