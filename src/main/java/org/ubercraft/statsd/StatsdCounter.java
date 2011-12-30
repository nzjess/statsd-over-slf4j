package org.ubercraft.statsd;

public interface StatsdCounter extends StatsdLoggerIBase {

    // Info level

    void infoCount();

    void infoCount(int count);

    void infoCount(double sampleRate);

    void infoCount(int count, double sampleRate);

    // Debug level

    void debugCount();

    void debugCount(int count);

    void debugCount(double sampleRate);

    void debugCount(int count, double sampleRate);

    // Trace level

    void traceCount();

    void traceCount(int count);

    void traceCount(double sampleRate);

    void traceCount(int count, double sampleRate);
}
