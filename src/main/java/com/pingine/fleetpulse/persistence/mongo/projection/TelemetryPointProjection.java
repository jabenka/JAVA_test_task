package com.pingine.fleetpulse.persistence.mongo.projection;

import java.time.Instant;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode
public class TelemetryPointProjection {
    private Instant ts;
    private double lat;
    private double lon;
    private double speed;
    private boolean ignition;
}
