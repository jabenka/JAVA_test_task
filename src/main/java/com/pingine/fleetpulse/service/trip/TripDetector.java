package com.pingine.fleetpulse.service.trip;

import static java.util.Collections.emptyList;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.pingine.fleetpulse.domain.Trip;
import com.pingine.fleetpulse.persistence.mongo.TelemetryPoint;

import lombok.RequiredArgsConstructor;

/**
 * Splits a stream of telemetry points into completed trips.
 * A trip starts on ignition=true and ends on the next ignition=false.
 */
@Component
@RequiredArgsConstructor
public class TripDetector {

    public List<Trip> detect(List<TelemetryPoint> points) {
        if (points == null || points.isEmpty()) {
            return emptyList();
        }

        List<Trip> trips = new ArrayList<>();

        List<TelemetryPoint> sortedPoints = new ArrayList<>(points);
        sortedPoints.sort(Comparator.comparing(TelemetryPoint::getTs));

        Map<String, List<TelemetryPoint>> currentTripPoints = new HashMap<>();
        Map<String, Set<LocalDateTime>> seenTimestamps = new HashMap<>();

        for (TelemetryPoint point : sortedPoints) {
            String vehicleId = point.getVehicleId();

            if (point.isIgnition()) {
                currentTripPoints.putIfAbsent(vehicleId, new ArrayList<>());
                seenTimestamps.putIfAbsent(vehicleId, new HashSet<>());

                if (seenTimestamps.get(vehicleId).add(point.getTs())) {
                    currentTripPoints.get(vehicleId).add(point);
                }
            }
            else {
                if (currentTripPoints.containsKey(vehicleId) && !currentTripPoints.get(vehicleId).isEmpty()) {
                    if (seenTimestamps.get(vehicleId).add(point.getTs())) {
                        currentTripPoints.get(vehicleId).add(point);
                    }
                    trips.add(buildTrip(currentTripPoints.get(vehicleId)));
                    currentTripPoints.get(vehicleId).clear();
                    currentTripPoints.remove(vehicleId);
                    seenTimestamps.get(vehicleId).clear();
                    seenTimestamps.remove(vehicleId);
                }
            }
        }

        return trips;
    }

    private Trip buildTrip(List<TelemetryPoint> points) {
        if (points == null || points.isEmpty()) {
            return null;
        }
        else {
            String vehicleId = points.get(0).getVehicleId();
            Instant startedAt = points.get(0).getTs().toInstant(ZoneOffset.UTC);
            Instant endedAt = points.get(points.size() - 1).getTs().toInstant(ZoneOffset.UTC);
            List<Trip.TripPoint> tripPoints = new ArrayList<>();
            double avgSpeed = 0.0;

            for (TelemetryPoint point : points) {
                tripPoints.add(Trip.TripPoint.builder()
                                       .ts(point.getTs().toInstant(ZoneOffset.UTC))
                                       .lat(point.getLat())
                                       .lon(point.getLon())
                                       .speedKph(point.getSpeed())
                                       .build());
                avgSpeed += point.getSpeed();
            }
            avgSpeed /= points.size();

            return Trip.builder()
                    .vehicleId(vehicleId)
                    .startedAt(startedAt)
                    .endedAt(endedAt)
                    .avgSpeedKph(avgSpeed)
                    .distanceKm(calculateDistance(tripPoints))
                    .points(tripPoints)
                    .build();
        }
    }

    private double calculateDistance(List<Trip.TripPoint> points) {
       double totalDistance = 0.0;
       for (int i = 1;i < points.size();i++) {
            totalDistance+=GeoDistance.haversineKm(points.get(i - 1).getLat(),points.get(i - 1).getLon(),points.get(i).getLat(),points.get(i).getLon());
       }
       return totalDistance;
    }
}
