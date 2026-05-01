package com.pingine.fleetpulse.persistence.mongo;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;

import com.pingine.fleetpulse.persistence.mongo.projection.TelemetryPointProjection;

@RequiredArgsConstructor
public class TripQueryRepositoryImpl implements TripQueryRepository {

    private final MongoTemplate mongoTemplate;

    @Override
    public List<TelemetryPointProjection> findRecentPoints(String vehicleId, int limit) {
        Query query = new Query()
                .addCriteria(Criteria.where("vehicleId").is(vehicleId))
                .with(Sort.by(Sort.Direction.DESC, "ts"))
                .limit(limit);
        query.withHint("vehicleId_ts_covered_idx");
        query.cursorBatchSize(limit);
        query.fields().include("lat", "lon", "speed", "ts", "ignition").exclude("_id");
        return mongoTemplate.find(query, TelemetryPointProjection.class,"telemetry_points");
    }
}
