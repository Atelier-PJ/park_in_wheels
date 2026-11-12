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
        expect(ready.gate(200) == UNKNOWN, "UX plus distance without P must block");
        ready.parked(true, 100);
        expect(ready.gate(200) == AVAILABLE, "Fresh P plus distance and UX permits gameplay");
        ready.parked(false, 250);
        expect(ready.gate(250) == RESTRICTED, "Non-P gear blocks even with unrestricted UX");
        ready.parked(null, 300);
        expect(ready.gate(300) == UNKNOWN, "Unknown gear blocks");
        ready.parked(true, 100);
        ready.distance(1001f, 5101);
        expect(ready.gate(5101) == UNKNOWN, "Stale P blocks even with fresh distance");
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
        SafariEngine parked = engine();
        parked.onOdometer(10.0);
        expect(!parked.finishParkedExperience(true), "Parked ending blocked without start and permission");
        parked.onGate(AVAILABLE); parked.start();
        expect(!parked.finishParkedExperience(false), "Unfinished missions cannot award stamp");
        parked.onGate(RESTRICTED);
        expect(!parked.finishParkedExperience(true), "Parked ending blocked when restricted");
        parked.onGate(AVAILABLE);
        expect(!parked.finishParkedExperience(true), "Paused session requires explicit resume");
        parked.resume();
        expect(parked.finishParkedExperience(true), "Complete parked mission awards without fabricating distance");
        expect(parked.snapshot().travelledKm == 0 && parked.snapshot().cards == 1, "Parked route preserves actual distance");
        expect(!parked.finishParkedExperience(true), "Repeated completion cannot duplicate reward");
        SafariEngine rejectedSave = new SafariEngine(new SafariEngine.Snapshot(PAUSED, 1, 5, 0), next -> { throw new IllegalStateException("full"); });
        rejectedSave.onGate(AVAILABLE); rejectedSave.onOdometer(6.0);
        try { rejectedSave.resume(); } catch (IllegalStateException expected) { }
        expect(rejectedSave.snapshot().phase == PAUSED, "Failed commit must not advance in-memory session");
        java.util.List<String> choices = java.util.List.of("아삭아삭 풀잎", "달콤한 열매");
        expect(VoiceChoiceParser.parse("첫 번째", choices) == 0, "Spoken ordinal selects first");
        expect(VoiceChoiceParser.parse("2번", choices) == 1, "Spoken second command");
        expect(VoiceChoiceParser.parse("열매", choices) == 1, "Choice keyword selects matching option");
        expect(VoiceChoiceParser.parse("둘 다", choices) == VoiceChoiceParser.UNKNOWN, "Ambiguous speech is not a choice");
        expect(VoiceChoiceParser.parse("그만", choices) == VoiceChoiceParser.STOP, "Stop command always recognized");
        expect(VoiceChoiceParser.parse("깨어나라", choices) == VoiceChoiceParser.FINISH, "Ending command parsed separately from permission");
        expect(VoiceChoiceParser.parse("별", java.util.List.of("노란 별", "분홍 별")) == VoiceChoiceParser.UNKNOWN, "Shared word cannot choose");
        expect(VoiceChoiceParser.parse("다음", choices) == VoiceChoiceParser.NEXT, "Continue command");
        System.out.println("PASS: " + assertions + " domain scenario assertions");
    }
}
