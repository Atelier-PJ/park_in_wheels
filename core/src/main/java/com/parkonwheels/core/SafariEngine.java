package com.parkonwheels.core;

import java.util.Objects;

/** Deterministic domain logic. No Android, SDK, clock or network dependencies. */
public final class SafariEngine implements VehicleGateway.Listener {
    public static final double HATCH_DISTANCE_KM = 5.0;
    public enum Phase { IDLE, ACTIVE, PAUSED, COMPLETE }
    public static final class Snapshot {
        public final Phase phase;
        public final double startKm;
        public final double travelledKm;
        public final int cards;
        public Snapshot(Phase phase, double startKm, double travelledKm, int cards) {
            this.phase = Objects.requireNonNull(phase);
            if (!Double.isFinite(startKm) || startKm < 0 || !Double.isFinite(travelledKm)
                    || travelledKm < 0 || cards < 0) throw new IllegalArgumentException("Invalid snapshot");
            this.startKm = startKm;
            this.travelledKm = travelledKm;
            this.cards = cards;
        }
        public static Snapshot empty() { return new Snapshot(Phase.IDLE, 0, 0, 0); }
    }
    public interface Store { void save(Snapshot snapshot); }
    private final Store store;
    private Snapshot state;
    private VehicleGateway.Gate gate = VehicleGateway.Gate.UNKNOWN;
    private Double odometerKm;
    private boolean distanceInvalid;

    public SafariEngine(Snapshot restored, Store store) {
        this.store = Objects.requireNonNull(store);
        // Restoring the process must never restore permission to play.
        state = restored.phase == Phase.ACTIVE
                ? new Snapshot(Phase.PAUSED, restored.startKm, restored.travelledKm, restored.cards) : restored;
    }
    public Snapshot snapshot() { return state; }
    public VehicleGateway.Gate gate() { return gate; }
    public boolean canPlay() { return gate == VehicleGateway.Gate.AVAILABLE; }
    public boolean hasDistance() { return odometerKm != null && !distanceInvalid; }
    public boolean readyToHatch() {
        return canPlay() && hasDistance() && state.phase == Phase.ACTIVE
                && state.travelledKm >= HATCH_DISTANCE_KM;
    }
    private void update(Snapshot next) { store.save(next); state = next; }

    @Override public void onGate(VehicleGateway.Gate next) {
        gate = Objects.requireNonNull(next);
        // Gate changes before persistence; a save failure must not permit input.
        if (!canPlay() && state.phase == Phase.ACTIVE) pause();
    }
    @Override public void onOdometer(Double km) {
        if (km == null || !Double.isFinite(km) || km < 0) { odometerKm = null; return; }
        boolean inJourney = state.phase == Phase.ACTIVE || state.phase == Phase.PAUSED;
        if (inJourney && (distanceInvalid || km < state.startKm + state.travelledKm)) {
            // Counter reset/rollback is not a completed journey. Start a new journey explicitly.
            distanceInvalid = true;
            odometerKm = null;
            return;
        }
        odometerKm = km;
        if (inJourney) {
            double distance = km - state.startKm;
            if (distance > state.travelledKm)
                update(new Snapshot(state.phase, state.startKm, distance, state.cards));
        }
    }
    public boolean start() {
        if (!canPlay() || !hasDistance() || (state.phase != Phase.IDLE && state.phase != Phase.COMPLETE)) return false;
        update(new Snapshot(Phase.ACTIVE, odometerKm, 0, state.cards));
        return true;
    }
    public void pause() {
        if (state.phase == Phase.ACTIVE)
            update(new Snapshot(Phase.PAUSED, state.startKm, state.travelledKm, state.cards));
    }
    public boolean resume() {
        if (!canPlay() || state.phase != Phase.PAUSED) return false;
        update(new Snapshot(Phase.ACTIVE, state.startKm, state.travelledKm, state.cards));
        return true;
    }
    public boolean hatch() {
        if (!readyToHatch()) return false;
        update(new Snapshot(Phase.COMPLETE, state.startKm, state.travelledKm, state.cards + 1));
        return true;
    }
    /** A separate, explicitly parked-only content route; never fabricates vehicle distance. */
    public boolean finishParkedExperience(boolean missionsComplete) {
        if (!canPlay() || state.phase != Phase.ACTIVE || !missionsComplete) return false;
        update(new Snapshot(Phase.COMPLETE, state.startKm, state.travelledKm, state.cards + 1));
        return true;
    }
    /** Cancel current progress, retain earned cards. Used after an odometer reset too. */
    public boolean cancel() {
        if (!canPlay()) return false;
        distanceInvalid = false;
        update(new Snapshot(Phase.IDLE, 0, 0, state.cards));
        return true;
    }
}
