package org.ubercraft.statsd;

import java.lang.reflect.Constructor;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Use this class's static <code>getLogger(...)</code> factory methods to obtain {@link StatsdLogger} instances. These
 * factory methods are equivalent to the similarly styled <code>getLogger(...)</code> methods on
 * <code>org.slf4j.LoggerFactory</code>.
 * <p/>
 * Normally this class returns instances of {@link StatsdLoggerImpl}. You can use an alternative implementation instead
 * by specifying a fully qualified class name using the system property:
 * <code>org.ubercraft.statsd.StatsdLoggerFactory.LOGGER_IMPL_CLASS</code>. Implementations should provide a public
 * constructor that takes a single argument of type {@link Logger}.
 * <p/>
 * Repeat calls to this factory for the same logger (i.e. by the same name) will return the same logger instance.
 */
public class StatsdLoggerFactory {

    private static final String LOGGER_IMPL_CLASS_SYS_PROP = "org.ubercraft.statsd.StatsdLoggerFactory.LOGGER_IMPL_CLASS";

    private static final Constructor<? extends StatsdLogger> LOGGER_CONSTRUCTOR;

    static {
        try {
            @SuppressWarnings("unchecked")
            Class<? extends StatsdLogger> loggerImplClass = (Class<? extends StatsdLogger>) //
                    Class.forName(System.getProperty(LOGGER_IMPL_CLASS_SYS_PROP, //
                            StatsdLoggerImpl.class.getName()));
            LOGGER_CONSTRUCTOR = loggerImplClass.getConstructor(Logger.class);
        }
        catch (Exception e) {
            throw new RuntimeException("failed finding statsd logger constructor", e);
        }
    }

    private static final ConcurrentMap<String, StatsdLogger> CACHE = new ConcurrentHashMap<String, StatsdLogger>();

    public static StatsdLogger getLogger(String name) {
        return getLogger(LoggerFactory.getLogger(name));
    }

    public static StatsdLogger getLogger(Class<?> clazz) {
        return getLogger(LoggerFactory.getLogger(clazz));
    }

    public static StatsdLogger getLogger(Logger logger) {
        StatsdLogger clientLogger = CACHE.get(logger.getName());
        if (clientLogger == null) {
            try {
                clientLogger = LOGGER_CONSTRUCTOR.newInstance(logger);
            }
            catch (Exception e) {
                throw new RuntimeException("failed constructing statsd logger", e);
            }
            StatsdLogger cachedClientLogger = CACHE.putIfAbsent(logger.getName(), clientLogger);
            if (cachedClientLogger != null) {
                clientLogger = cachedClientLogger;
            }
        }
        return clientLogger;
    }
}
