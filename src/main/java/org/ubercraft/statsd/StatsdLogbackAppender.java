package org.ubercraft.statsd;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

/**
 * A logback appender implementation intended for use in conjunction with {@link StatsdLoggerImpl}.
 * 
 * @see StatsdLoggerImpl
 * @see StatsdClient
 */
public class StatsdLogbackAppender extends AppenderBase<ILoggingEvent> {

    private static final int DEFAULT_QUEUE_SIZE = 500;
    private static final int DEFAULT_QUEUE_OFFER_TIMEOUT = 0;

    private String host;
    private int port = StatsdClient.DEFAULT_PORT;

    private int queueSize = DEFAULT_QUEUE_SIZE;
    private long queueOfferTimeout = DEFAULT_QUEUE_OFFER_TIMEOUT;

    private StatsdClient client;

    private boolean warnQueueFull = true;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getQueueSize() {
        return queueSize;
    }

    public void setQueueSize(int queueSize) {
        this.queueSize = queueSize;
    }

    public long getQueueOfferTimeout() {
        return queueOfferTimeout;
    }

    public void setQueueOfferTimeout(long queueOfferTimeout) {
        this.queueOfferTimeout = queueOfferTimeout;
    }

    @Override
    public void start() {
        if (isStarted()) {
            return;
        }

        if (host == null) {
            throw new IllegalStateException("host property is required for appender: " + name);
        }

        try {
            client = new StatsdClient(host, port, null, queueSize) {
                @Override
                protected void errorQueueFull(String stat) {
                    if (warnQueueFull) {
                        warnQueueFull = false;
                        addWarn("statsd appender queue is full, if you see this message " + //
                                "it means the queue size needs to be increased, " + //
                                "or the number of stats logged decreased: " + stat);
                    }
                }

                @Override
                protected void handleError(String message, String stat, Exception e) {
                    addError(message + ": sending " + stat + " to " + toString(), e);
                }
            };

            client.setQueueOfferTimeout(queueOfferTimeout);

            started = true;
        }
        catch (Exception e) {
            addError("could not create statsd client", e);
        }
    }

    @Override
    public void stop() {
        if (!isStarted()) {
            return;
        }

        client.shutdown();
        client = null;

        started = false;
    }

    @Override
    protected void append(ILoggingEvent event) {
        boolean sent = false;

        Object[] args = event.getArgumentArray();
        if (args != null && args.length == 3) {
            if (args[0] instanceof StatsdStatType && //
                    args[1] instanceof Integer && //
                    args[2] instanceof Double) {

                StatsdStatType type = (StatsdStatType)args[0];

                String key = event.getLoggerName();
                int value = (Integer)args[1];
                double sampleRate = (Double)args[2];

                sent = client.stat(type, key, value, sampleRate);
            }
        }

        if (sent) {
            warnQueueFull = true;
        }
    }
}
