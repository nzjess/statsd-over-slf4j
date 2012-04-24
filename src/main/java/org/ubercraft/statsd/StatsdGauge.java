package org.ubercraft.statsd;

public interface StatsdGauge extends StatsdLoggerIBase {

    // Info level

    void infoGauge(int value);

    // Debug level

    void debugGauge(int value);

    // Trace level

    void traceGauge(int value);
}
