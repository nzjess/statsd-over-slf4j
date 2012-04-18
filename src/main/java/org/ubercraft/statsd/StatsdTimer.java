package org.ubercraft.statsd;

public interface StatsdTimer extends StatsdLoggerIBase {

    // Info level

    void infoTime(long millis);

    void infoTime(long millis, double sampleRate);

    // Debug level

    void debugTime(long millis);

    void debugTime(long millis, double sampleRate);

    // Trace level

    void traceTime(long millis);

    void traceTime(long millis, double sampleRate);
}
