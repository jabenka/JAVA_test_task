package com.pingine.fleetpulse.persistence.mongo;

import java.util.List;

import com.pingine.fleetpulse.persistence.mongo.projection.TelemetryPointProjection;

public interface TripQueryRepository {

    List<TelemetryPointProjection> findRecentPoints(String vehicleId, int limit);
}
