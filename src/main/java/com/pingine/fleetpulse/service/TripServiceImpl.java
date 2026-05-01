package com.pingine.fleetpulse.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.pingine.fleetpulse.api.dto.TripResponse;
import com.pingine.fleetpulse.api.dto.VehicleResponse;
import com.pingine.fleetpulse.domain.Trip;
import com.pingine.fleetpulse.persistence.mongo.TelemetryRepository;
import com.pingine.fleetpulse.persistence.mongo.projection.TelemetryPointProjection;
import com.pingine.fleetpulse.service.trip.TripDetector;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TripServiceImpl implements TripService {

    private final TelemetryRepository telemetryRepository;
    private final TripDetector tripDetector;
    private final VehicleService vehicleService;

    @Value("${application.telemetry.query-limit}")
    private int queryLimit;

    @Override
    public TripResponse getLastTrip(String vehicleId) {
        List<Long> diffTime = new ArrayList<>();
        for (int i = 0; i < 101; i++) {
            Long start = System.currentTimeMillis();
            List<TelemetryPointProjection> pointList = telemetryRepository.findRecentPoints(vehicleId, queryLimit);
            Long end = System.currentTimeMillis();
            diffTime.add(end - start);
        }
        Collections.sort(diffTime);
        System.out.println("Medain: " + diffTime.get(diffTime.size() / 2));
        System.out.println(diffTime.stream().mapToLong(Long::longValue).average());

        List<TelemetryPointProjection> pointList = telemetryRepository.findRecentPoints(vehicleId, queryLimit);
        List<Trip> trips = tripDetector.detect(pointList, vehicleId);
        if (trips.isEmpty()) {
            throw new NotFoundException(String.format("Trips for vehicle [%s] not found", vehicleId));
        }
        return buildResponse(vehicleId, trips.get(trips.size() - 1));
    }

    private TripResponse buildResponse(String vehicleId, Trip trip) {
        VehicleResponse vehicle = vehicleService.getById(vehicleId);
        return TripResponse.builder()
                .vehicle(vehicle)
                .startedAt(trip.getStartedAt())
                .endedAt(trip.getEndedAt())
                .distanceKm(trip.getDistanceKm())
                .avgSpeedKph(trip.getAvgSpeedKph())
                .pointCount(trip.getPoints().size())
                .points(mapToDto(trip.getPoints()))
                .build();
    }

    private List<TripResponse.PointDto> mapToDto(List<Trip.TripPoint> points) {
        return points.stream().map(p -> TripResponse.PointDto.builder()
                        .ts(p.getTs())
                        .lat(p.getLat())
                        .lon(p.getLon())
                        .speedKph(p.getSpeedKph())
                        .build())
                .collect(Collectors.toList());

    }

}
