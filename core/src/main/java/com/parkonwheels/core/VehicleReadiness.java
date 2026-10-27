package com.parkonwheels.core;

/** Combines current platform restriction with fresh SDK data, using a monotonic clock. */
public final class VehicleReadiness {
    public static final long MAX_AGE_MS = 5_000;
    private VehicleGateway.Gate ux = VehicleGateway.Gate.UNKNOWN;
    private Double kilometres;
    private long receivedAt;
    public void reset() { ux = VehicleGateway.Gate.UNKNOWN; kilometres = null; }
    public void ux(VehicleGateway.Gate value) { ux = value; }
    public Double distance(Float value, long now) {
        kilometres = value != null && Float.isFinite(value) && value >= 0 ? value.doubleValue() : null;
        receivedAt = now;
        return kilometres;
    }
    public boolean fresh(long now) {
        return kilometres != null && now >= receivedAt && now - receivedAt <= MAX_AGE_MS;
    }
    public VehicleGateway.Gate gate(long now) {
        if (ux == VehicleGateway.Gate.RESTRICTED) return ux;
        return ux == VehicleGateway.Gate.AVAILABLE && fresh(now)
                ? VehicleGateway.Gate.AVAILABLE : VehicleGateway.Gate.UNKNOWN;
    }
}
