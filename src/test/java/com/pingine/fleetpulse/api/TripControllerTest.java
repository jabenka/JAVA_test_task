package com.pingine.fleetpulse.api;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import com.pingine.fleetpulse.api.dto.TripResponse;
import com.pingine.fleetpulse.service.NotFoundException;
import com.pingine.fleetpulse.service.TripService;

@WebMvcTest(TripController.class)
class TripControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TripService tripService;

    @Test
    void getLastTrip_ShouldReturnOk_WhenTripExists() throws Exception {
        String vehicleId = "v123";
        TripResponse mockResponse = TripResponse.builder()
                .distanceKm(10.5)
                .avgSpeedKph(60.0)
                .build();

        given(tripService.getLastTrip(vehicleId)).willReturn(mockResponse);

        mockMvc.perform(get("/api/v1/vehicles/{vehicleId}/last-trip", vehicleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.distanceKm", is(10.5)))
                .andExpect(jsonPath("$.avgSpeedKph", is(60.0)));
    }

    @Test
    void getLastTrip_ShouldReturnNotFound_WhenServiceThrowsException() throws Exception {
        String vehicleId = "v999";

        given(tripService.getLastTrip(vehicleId))
                .willThrow(new NotFoundException("Not found"));

        mockMvc.perform(get("/api/v1/vehicles/{vehicleId}/last-trip", vehicleId))
                .andExpect(status().isNotFound());
    }
}