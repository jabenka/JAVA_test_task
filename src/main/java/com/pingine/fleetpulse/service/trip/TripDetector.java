package com.pingine.fleetpulse.service.trip;

import static java.util.Collections.emptyList;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.pingine.fleetpulse.domain.Trip;
import com.pingine.fleetpulse.persistence.mongo.projection.TelemetryPointProjection;

import lombok.RequiredArgsConstructor;

/**
 * Splits a stream of telemetry points into completed trips.
 * A trip starts on ignition=true and ends on the next ignition=false.
 */
@Component
@RequiredArgsConstructor
public class TripDetector {

    public List<Trip> detect(List<TelemetryPointProjection> points, String vehicleId) {
        if (points == null || points.isEmpty()) {
            System.out.println("DEBUG empty points");
            return emptyList();
        }

        List<Trip> trips = new ArrayList<>();

        List<TelemetryPointProjection> sortedPoints = new ArrayList<>(points);
        sortedPoints.sort(Comparator.comparing(TelemetryPointProjection::getTs));

        List<TelemetryPointProjection> currentTripPoints = new ArrayList<>();
        Set<TelemetryPointProjection> seen = new HashSet<>();

        for (TelemetryPointProjection point : sortedPoints) {

            if (point.isIgnition()) {
                if (seen.add(point)) {
                    currentTripPoints.add(point);
                }
            }
            else {
                if (!currentTripPoints.isEmpty()) {
                    if (seen.add(point)) {
                        currentTripPoints.add(point);
                    }
                    trips.add(buildTrip(currentTripPoints, vehicleId));
                    seen.clear();
                    currentTripPoints.clear();
                }
            }
        }

        return trips;
    }

    private Trip buildTrip(List<TelemetryPointProjection> points, String vehicleId) {
        if (points == null || points.isEmpty()) {
            return null;
        }
        else {
            Instant startedAt = points.get(0).getTs();
            Instant endedAt = points.get(points.size() - 1).getTs();
            List<Trip.TripPoint> tripPoints = new ArrayList<>();
            double avgSpeed = 0.0;

            for (TelemetryPointProjection point : points) {
                tripPoints.add(Trip.TripPoint.builder()
                                       .ts(point.getTs())
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
        for (int i = 1; i < points.size(); i++) {
            totalDistance += GeoDistance.haversineKm(points.get(i - 1).getLat(), points.get(i - 1).getLon(),
                                                     points.get(i).getLat(), points.get(i).getLon());
        }
        return totalDistance;
    }
}
