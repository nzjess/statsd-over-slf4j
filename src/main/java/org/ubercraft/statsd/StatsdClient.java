package org.ubercraft.statsd;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;

/**
 * A Java statsd client. See <a href="https://github.com/etsy/statsd">https://github.com/etsy/statsd</a> for why you
 * might want this.
 * <p/>
 * This class has several constructors. When constructing instances of this class, the <code>host</code> and
 * <code>port</code> parameters determine where to connect to the statsd server. Only the <code>host</code> parameter is
 * required. If the <code>port</code> parameter is less than zero, the default port (8125) will be used.
 * <p/>
 * The <code>queueSize</code> parameter is greater than 0, this instance will operate an asychronous blocking queue of
 * the given size. Instead of sending stats to the server in the calling thread, new stats are enqueued. A background
 * daemon thread watching the queue takes care of sending newly queued stats to the server. In this mode of operation,
 * the {@link #shutdown()} method may be called to cause the background thread to terminate. If the
 * <code>queueSize</code> parameter is 0 or less, no queueing is performed (and the shutdown() method does nothing).
 * <p/>
 * The <code>logger</code> parameter can be used for reporting errors during logging. This class has a set of protected
 * <code>errorXxx(...)</code> methods that are invoked in response to certain error conditions. The default
 * implementations of these method delegate to a single (also protected) {@link #handleError(String, String, Exception)}
 * method. By default this method will log an error level message to the supplier <code>logger</code>. This parameter
 * may be null, in which case no error reporting will be done by default. Users may subclass this class in order to
 * customise error handling for their own requirements.
 * <p/>
 * Once an instance of this class has been created, it may be used to send stats to a listening statsd server. The
 * method to use depends on the type of stat you wish to send. See the <code>count(...)</code>, <code>time(...)</code>
 * and <code>stat(...)</code> methods in their various forms.
 * <p/>
 * When sending a stat to the statsd server, the message is constructed as a Java string and then converted to bytes,
 * normally using the platform default charset. This choice of charset can be overridden by specifying the charset name
 * using the system property: <code>org.ubercraft.statsd.StatsdClient.CHARSET</code>.
 */
public class StatsdClient {

    public static final int DEFAULT_PORT = 8125;

    private static final String COUNTER_FORMAT = "%s:%s|c";
    private static final String TIMER_FORMAT = "%s:%d|ms";
    private static final String SAMPLE_RATE_FORMAT = "%s|@%f";

    private static final String CHARSET_SYS_PROP = "org.ubercraft.statsd.StatsdClient.CHARSET";

    private static final Charset CHARSET;

    static {
        Charset charset = null;
        String charsetName = System.getProperty(CHARSET_SYS_PROP);
        if (charsetName != null) {
            try {
                charset = Charset.forName(charsetName);
            }
            catch (Exception e) {
                // ignored
            }
        }
        if (charset == null) {
            charset = Charset.defaultCharset();
        }
        CHARSET = charset;
    }

    private static final Random RANDOM = new Random();

    protected final InetAddress host;
    protected final int port;

    protected final DatagramSocket sock;

    protected final Logger logger;

    private final String hostPortString;

    private final BlockingQueue<String> queue;

    private SendThread thread;
    private long queueOfferTimeout = 0;

    public StatsdClient(String host, int port) throws UnknownHostException, SocketException {
        this(host, port, null, 0);
    }

    public StatsdClient(InetAddress host, int port) throws SocketException {
        this(host, port, null, 0);
    }

    public StatsdClient(String host, int port, int queueSize) throws UnknownHostException, SocketException {
        this(host, port, null, queueSize);
    }

    public StatsdClient(InetAddress host, int port, int queueSize) throws SocketException {
        this(host, port, null, queueSize);
    }

    public StatsdClient(String host, int port, Logger logger) throws UnknownHostException, SocketException {
        this(host, port, logger, 0);
    }

    public StatsdClient(InetAddress host, int port, Logger logger) throws SocketException {
        this(host, port, logger, 0);
    }

    public StatsdClient(String host, int port, Logger logger, int queueSize) throws UnknownHostException, SocketException {
        this(InetAddress.getByName(host), port, logger, queueSize);
    }

    public StatsdClient(InetAddress host, int port, Logger logger, int queueSize) throws SocketException {
        if (host == null) {
            throw new IllegalArgumentException("null host");
        }

        if (port < 0) {
            port = DEFAULT_PORT;
        }

        this.host = host;
        this.port = port;

        this.sock = new DatagramSocket();

        this.logger = logger;
        this.hostPortString = host + ":" + port;

        if (queueSize > 0) {
            queue = new ArrayBlockingQueue<String>(queueSize);
            thread = new SendThread();
            thread.start();
        }
        else {
            queue = null;
            thread = null;
        }
    }

    @Override
    public String toString() {
        return hostPortString;
    }

    public long getQueueOfferTimeout() {
        return queueOfferTimeout;
    }

    public void setQueueOfferTimeout(long queueOfferTimeout) {
        this.queueOfferTimeout = queueOfferTimeout;
    }

    public void shutdown() {
        if (thread != null) {
            thread.interrupt();
            thread = null;
        }
    }

    private class SendThread extends Thread {

        SendThread() {
            setDaemon(true);
        }

        @Override
        public void run() {
            try {
                while (thread != null) {
                    doSend(queue.take());
                }
            }
            catch (InterruptedException e) {
                // done;
            }
        }
    }

    public boolean count(String key) {
        return count(key, 1);
    }

    public boolean count(String key, int count) {
        return count(key, count, 1.0D);
    }

    public boolean count(String key, double sampleRate) {
        return count(key, 1, sampleRate);
    }

    public boolean count(String key, int count, double sampleRate) {
        return stat(StatsdStatType.COUNTER, key, count, sampleRate);
    }

    public boolean time(String key, int millis) {
        return time(key, millis, 1.0);
    }

    public boolean time(String key, int millis, double sampleRate) {
        return stat(StatsdStatType.TIMER, key, millis, sampleRate);
    }

    public boolean stat(StatsdStatType type, String key, int value, double sampleRate) {
        String format;
        switch (type) {
            case COUNTER:
                format = COUNTER_FORMAT;
                break;
            case TIMER:
                format = TIMER_FORMAT;
                break;
            default:
                throw new IllegalStateException();
        }
        String stat = String.format(format, key, value);
        return send(stat, sampleRate);
    }

    private boolean send(String stat, double sampleRate) {
        if (sampleRate < 1.0D) {
            if (RANDOM.nextDouble() <= sampleRate) {
                stat = String.format(SAMPLE_RATE_FORMAT, stat, sampleRate);
                return send(stat);
            }
            else {
                return false;
            }
        }
        else {
            return send(stat);
        }
    }

    private boolean send(String stat) {
        if (queue != null) {
            try {
                if (queue.offer(stat, queueOfferTimeout, TimeUnit.MILLISECONDS)) {
                    return true;
                }
            }
            catch (Exception e) {
                errorEnqueueFailed(stat, e);
                return false;
            }
            errorQueueFull(stat);
            return false;
        }
        else {
            return doSend(stat);
        }
    }

    private boolean doSend(String stat) {
        try {
            sendToServer(stat);
            return true;
        }
        catch (Exception e) {
            errorSendFailed(stat, e);
            return false;
        }
    }

    protected void sendToServer(String stat) throws IOException {
        byte[] data = stat.getBytes(CHARSET);
        sock.send(new DatagramPacket(data, data.length, host, port));
    }

    protected void errorQueueFull(String stat) {
        handleError("Queue full", stat, null);
    }

    protected void errorEnqueueFailed(String stat, Exception e) {
        handleError("Enqueue failed", stat, e);
    }

    protected void errorSendFailed(String stat, Exception e) {
        handleError("Send failed", stat, e);
    }

    protected void handleError(String message, String stat, Exception e) {
        if (logger != null && logger.isErrorEnabled()) {
            logger.error("{}: sending {} to {}", new Object[] {
                    message, stat, toString(), e
            });
        }
    }
}
