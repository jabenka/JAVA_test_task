package com.pingine.fleetpulse.persistence.mongo;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "telemetry_points")
@Getter
@Setter
@CompoundIndex(
        name = "vehicleId_ts_covered_idx",
        def = "{'vehicleId': 1, 'ts': -1, 'ignition': 1}",
        background = true
)
@EqualsAndHashCode(exclude = "id")
public class TelemetryPoint {

    @Id
    private String id;

    private String vehicleId;
    private LocalDateTime ts;
    private double lat;
    private double lon;
    private double speed;
    private boolean ignition;
}

