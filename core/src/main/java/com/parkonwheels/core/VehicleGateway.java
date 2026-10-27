package com.parkonwheels.core;

/** SDK adapters translate verified SDK values into these events. UNKNOWN always blocks play. */
public interface VehicleGateway extends AutoCloseable {
    enum Gate { UNKNOWN, AVAILABLE, RESTRICTED }
    interface Listener {
        void onGate(Gate gate);
        /** Normalized to kilometres by the adapter; null means unsupported or invalid. */
        void onOdometer(Double kilometres);
    }
    void start(Listener listener);
    @Override void close();
}
