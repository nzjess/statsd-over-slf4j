package org.ubercraft.statsd;

/**
 * Try running this with and without the following system property setting:
 * <p/>
 * <code>-Dorg.ubercraft.statsd.StatsdLoggerFactory.LOGGER_IMPL_CLASS=org.ubercraft.statsd.MyStatsdLogger</code>
 */
public class StatsdLoggerMain {

    // some stats
    private static final StatsdCounter counterStat = StatsdLoggerFactory.getLogger("statsd.test.counter");
    private static final StatsdTimer timerStat = StatsdLoggerFactory.getLogger("statsd.test.timer");

    public static void main(String... args) throws Exception {
        // log some stats
        counterStat.infoCount(4);
        timerStat.infoTime(50);

        // no async queue flush on JVM exit - allow some time for this to occur on its own
        Thread.sleep(1000);
    }
}
