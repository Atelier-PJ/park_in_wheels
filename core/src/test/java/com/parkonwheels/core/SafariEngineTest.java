package com.parkonwheels.core;

import static com.parkonwheels.core.VehicleGateway.Gate.*;
import static com.parkonwheels.core.SafariEngine.Phase.*;

/** Executable scenario checks; runs with JDK 17 alone or Gradle :core:scenarioTest. */
public final class SafariEngineTest {
    private static int assertions;
    private static void expect(boolean ok, String message) {
        assertions++;
        if (!ok) throw new AssertionError(message);
    }
    private static SafariEngine engine() { return new SafariEngine(SafariEngine.Snapshot.empty(), s -> {}); }
    public static void main(String[] args) {
        SafariEngine e = engine();
        e.onOdometer(100.0);
        expect(!e.start(), "Unknown gate blocks starting");
        e.onGate(AVAILABLE);
        expect(e.start(), "Available + odometer permits start");
        e.onGate(RESTRICTED);
        expect(e.snapshot().phase == PAUSED && !e.canPlay(), "Restriction pauses immediately");
        e.onOdometer(106.0);
        expect(!e.hatch() && !e.resume(), "Restricted hatch/resume blocked");
        e.onGate(AVAILABLE);
        expect(e.snapshot().phase == PAUSED, "Restriction release never auto resumes");
        expect(e.resume() && e.hatch(), "Explicit resume then hatch");
        expect(!e.hatch() && e.snapshot().cards == 1, "Repeated hatch cannot duplicate card");
        e.onOdometer(106.0);
        expect(e.snapshot().cards == 1, "Repeated vehicle event cannot duplicate card");
        expect(e.start() && e.snapshot().travelledKm == 0, "New journey resets distance");
        e.onOdometer(110.0);
        expect(!e.hatch(), "Insufficient distance blocks hatch");
        e.onGate(UNKNOWN);
        expect(!e.canPlay() && e.snapshot().phase == PAUSED, "Lost gate connection fails closed");
        e.onGate(AVAILABLE);
        e.resume();
        SafariEngine restored = new SafariEngine(e.snapshot(), s -> {});
        expect(!restored.canPlay() && restored.snapshot().phase == PAUSED, "Process restart blocks and pauses");
        restored.onOdometer(111.0);
        restored.onGate(AVAILABLE);
        expect(restored.resume() && restored.hatch(), "Restored baseline credits distance while app was closed");
        e.onOdometer(null);
        expect(!e.hasDistance() && !e.hatch(), "Unsupported odometer blocks hatch");
        e.onOdometer(Double.NaN);
        expect(!e.hasDistance(), "NaN rejected");
        e.onOdometer(3.0);
        e.onOdometer(200.0);
        expect(!e.hasDistance() && !e.hatch(), "Rollback invalidates journey until cancelled");
        expect(e.cancel(), "Can abandon invalid journey");
        e.onOdometer(200.0);
        expect(e.start(), "Fresh valid journey after cancellation");
        e.onOdometer(205.0);
        e.onOdometer(204.0);
        expect(!e.hatch(), "Rollback after threshold must not award a card");
        SafariEngine failing = new SafariEngine(new SafariEngine.Snapshot(ACTIVE, 100, 5, 0), s -> { throw new IllegalStateException("Disk full"); });
        failing.onOdometer(105.0);
        failing.onGate(AVAILABLE);
        try { failing.resume(); } catch (IllegalStateException expected) { /* state remains inspectable */ }
        try { failing.onGate(RESTRICTED); } catch (IllegalStateException expected) { /* must still block */ }
        expect(!failing.canPlay() && !failing.readyToHatch(), "Save error cannot override restriction");
        VehicleReadiness ready = new VehicleReadiness();
        ready.ux(AVAILABLE);
        expect(ready.gate(100) == UNKNOWN, "UX alone must not allow gameplay");
        expect(ready.distance(1000f, 100) == 1000d, "SDK km values preserve units");
        expect(ready.gate(200) == AVAILABLE, "Fresh SDK value plus UX permits gameplay");
        expect(ready.gate(5101) == UNKNOWN, "Stale SDK value blocks gameplay");
        ready.ux(RESTRICTED);
        expect(ready.gate(9000) == RESTRICTED, "Restriction wins even with stale data");
        ready.ux(AVAILABLE);
        ready.distance(Float.NaN, 9001);
        expect(ready.gate(9001) == UNKNOWN, "NaN cannot establish connection readiness");
        ready.distance(-1f, 9002);
        expect(!ready.fresh(9002), "Negative odometer is invalid");
        ready.distance(1001f, 9003);
        ready.reset();
        expect(ready.gate(9004) == UNKNOWN, "Foreground reconnect requires fresh UX and distance");
        System.out.println("PASS: " + assertions + " domain scenario assertions");
    }
}
