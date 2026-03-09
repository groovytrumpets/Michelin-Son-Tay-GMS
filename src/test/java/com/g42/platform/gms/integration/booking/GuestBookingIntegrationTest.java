package com.g42.platform.gms.integration.booking;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.g42.platform.gms.booking.customer.api.dto.GuestBookingRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class GuestBookingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private GuestBookingRequest baseRequest() {

        GuestBookingRequest request = new GuestBookingRequest();

        request.setAppointmentDate(LocalDate.of(2026,3,10));
        request.setAppointmentTime(LocalTime.of(9,0));

        request.setUserNote("Integration Test");
        request.setPhone("0912345678");
        request.setFullName("Test User");

        request.setSelectedServiceIds(List.of(1));

        return request;
    }

    @Test
    @DisplayName("GB_001 - Happy case booking")
    void happyCaseBooking() throws Exception {

        GuestBookingRequest request = baseRequest();

        mockMvc.perform(post("/api/booking/guest/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.requestId").exists());
    }

    @Test
    @DisplayName("GB_002 - Wrong format name")
    void wrongFormatName() throws Exception {

        GuestBookingRequest request = baseRequest();
        request.setFullName("152345");

        mockMvc.perform(post("/api/booking/guest/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GB_003 - Null service list")
    void nullServiceList() throws Exception {

        GuestBookingRequest request = baseRequest();
        request.setSelectedServiceIds(null);

        mockMvc.perform(post("/api/booking/guest/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk());
    }
}