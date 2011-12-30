package org.ubercraft.statsd;

public interface StatsdLogger extends StatsdCounter, StatsdTimer {

    // Info level

    void infoStat(StatsdStatType type, int value, double sampleRate);

    // Debug level

    void debugStat(StatsdStatType type, int value, double sampleRate);

    // Trace level

    void traceStat(StatsdStatType type, int value, double sampleRate);
}
