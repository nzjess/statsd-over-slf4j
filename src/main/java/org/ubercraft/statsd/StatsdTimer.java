package org.ubercraft.statsd;

public interface StatsdTimer extends StatsdLoggerIBase {

    // Info level

    void infoTime(int millis);

    void infoTime(int millis, double sampleRate);

    // Debug level

    void debugTime(int millis);

    void debugTime(int millis, double sampleRate);

    // Trace level

    void traceTime(int millis);

    void traceTime(int millis, double sampleRate);
}
