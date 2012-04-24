package org.ubercraft.statsd;

import static org.junit.Assert.assertEquals;
import static org.ubercraft.statsd.StatsdStatType.COUNTER;
import static org.ubercraft.statsd.StatsdStatType.GAUGE;
import static org.ubercraft.statsd.StatsdStatType.TIMER;

import java.io.IOException;

import org.junit.Test;

public class StatsdClientTest {

    private String expected;

    private StatsdClient client = new StatsdClient((String)null, -1) {
        @Override
        protected void sendToServer(String stat) throws IOException {
            assertEquals(expected, stat);
        }
    };

    public StatsdClientTest() throws Exception {}

    @Test
    public void testCount() throws Exception {
        expected = "ka.t:1|c";
        client.count("ka.t");

        expected = "ka.p:2|c";
        client.count("ka.p", 2);

        expected = "ka.q:-2|c";
        client.count("ka.q", -2);

        expected = "ka.r:-2|c|@0.900000";
        while (!client.count("ka.r", -2, 0.9D));
    }

    @Test
    public void testTime() throws Exception {
        expected = "kb:15|ms";
        client.time("kb", 15);

        expected = "kb.r:-2|ms|@0.800000";
        while (!client.time("kb.r", -2, 0.8D));
    }

    @Test
    public void testGuage() throws Exception {
        expected = "ka.t:1|g";
        client.gauge("ka.t", 1);

        expected = "ka.p:2|g";
        client.gauge("ka.p", 2);

        expected = "ka.q:-2|g";
        client.gauge("ka.q", -2);
    }

    @Test
    public void testStat() throws Exception {
        expected = "kc.t:3|c";
        client.stat(COUNTER, "kc.t", 3, 1.0D);

        expected = "kc.p:-3|c";
        client.stat(COUNTER, "kc.p", -3, 1.0D);

        expected = "kb.r:2|c|@0.500000";
        while (!client.stat(COUNTER, "kb.r", 2, 0.5D));

        expected = "kc.q:20|ms";
        client.stat(TIMER, "kc.q", 20, 1.0D);

        expected = "kb.w:25|ms|@0.300000";
        while (!client.stat(TIMER, "kb.w", 25, 0.3D));

        expected = "kc.q:5|g";
        client.stat(GAUGE, "kc.q", 5, 1.0D);
    }
}
