package com.parkonwheels.core;

import java.util.Set;

/** Optional telemetry cannot authorize gameplay. All times use monotonic milliseconds. */
public final class TravelRewardPolicy {
    public static final long CHARGE_MAX_AGE_MS = 10_000;
    public static final long ARRIVAL_MAX_AGE_MS = 10 * 60_000;
    private Boolean charging;
    private long chargingAt = -1;
    private String arrival;
    private long arrivalAt = -1;
    public void charging(Boolean value, long now) { charging = value; chargingAt = now; }
    public boolean isCharging(long now) {
        return Boolean.TRUE.equals(charging) && chargingAt >= 0 && now >= chargingAt && now - chargingAt <= CHARGE_MAX_AGE_MS;
    }
    public void arrived(String key, long now) {
        if(key == null || key.trim().isEmpty() || key.length() > 128 || key.equals(arrival)) return;
        arrival = key; arrivalAt = now;
    }
    public String claimable(boolean safelyParked, long now, Set<String> claimed) {
        if(!safelyParked || arrival == null || now < arrivalAt || now - arrivalAt > ARRIVAL_MAX_AGE_MS || claimed.contains(arrival)) return null;
        return arrival;
    }
    public void clearArrival() { arrival = null; arrivalAt = -1; }
    public void reset() { charging = null; chargingAt = -1; clearArrival(); }
}
