package org.ubercraft.statsd;

public interface StatsdLogger extends StatsdCounter, StatsdTimer {

    // Info level

    void infoStat(StatsdStatType type, long value, double sampleRate);

    // Debug level

    void debugStat(StatsdStatType type, long value, double sampleRate);

    // Trace level

    void traceStat(StatsdStatType type, long value, double sampleRate);
}
