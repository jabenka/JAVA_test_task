package com.pingine.fleetpulse.service.trip;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.pingine.fleetpulse.domain.Trip;
import com.pingine.fleetpulse.persistence.mongo.projection.TelemetryPointProjection;

class TripDetectorTest {

    private final TripDetector detector = new TripDetector();

    @Test
    void detectsSingleTripFromIgnitionOnToOff() {
        LocalDateTime t0 = LocalDateTime.parse("2026-04-27T08:00:00");
        String vehicleId = "v1";
        List<TelemetryPointProjection> points = List.of(
                point(t0.toInstant(ZoneOffset.UTC), 52.5200, 13.4050, 0.0, true),
                point(t0.plusMinutes(5).toInstant(ZoneOffset.UTC), 52.5300, 13.4200, 45.0, true),
                point(t0.plusMinutes(10).toInstant(ZoneOffset.UTC), 52.5450, 13.4400, 62.0, true),
                point(t0.plusMinutes(20).toInstant(ZoneOffset.UTC), 52.5600, 13.4700, 58.0, true),
                point(t0.plusMinutes(30).toInstant(ZoneOffset.UTC), 52.5700, 13.5000, 0.0, false)
        );

        List<Trip> trips = detector.detect(points, vehicleId);

        assertThat(trips).hasSize(1);
        Trip trip = trips.get(0);
        assertThat(trip.getVehicleId()).isEqualTo("v1");
        assertThat(trip.getPoints()).hasSize(5);
        assertThat(trip.getDistanceKm()).isGreaterThan(0.0);
        assertThat(trip.getAvgSpeedKph()).isGreaterThan(0.0);
    }

    @Test
    void handlesDuplicateTimestamps() {
        String vehicleId = "v1";
        LocalDateTime t0 = LocalDateTime.parse("2026-04-27T08:00:00");
        List<TelemetryPointProjection> points = List.of(
                point(t0.toInstant(ZoneOffset.UTC), 52.5200, 13.4050, 0.0, true),
                point(t0.plusMinutes(5).toInstant(ZoneOffset.UTC), 52.5300, 13.4200, 45.0, true),
                point(t0.plusMinutes(5).toInstant(ZoneOffset.UTC), 52.5300, 13.4200, 45.0, true),
                point(t0.plusMinutes(10).toInstant(ZoneOffset.UTC), 52.5450, 13.4400, 62.0, true),
                point(t0.plusMinutes(20).toInstant(ZoneOffset.UTC), 52.5700, 13.5000, 0.0, false)
        );

        List<Trip> trips = detector.detect(points, vehicleId);

        assertThat(trips).hasSize(1);
        Trip trip = trips.get(0);
        assertThat(trip.getPoints()).hasSize(4);
    }

    private static TelemetryPointProjection point(Instant ts,
                                                  double lat, double lon, double speed, boolean ignition) {
        TelemetryPointProjection p = new TelemetryPointProjection();
        p.setTs(ts);
        p.setLat(lat);
        p.setLon(lon);
        p.setSpeed(speed);
        p.setIgnition(ignition);
        return p;
    }
}
