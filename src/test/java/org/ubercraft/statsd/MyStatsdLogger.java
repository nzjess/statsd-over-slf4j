package org.ubercraft.statsd;

import org.slf4j.Logger;

/**
 * Example of subclassing {@link StatsdLoggerImpl} to customise log message creation (in this case, to be logback
 * console appender friendly).
 * <p/>
 * See {@link StatsdLoggerMain} for an example of how to use it.
 * 
 * @see StatsdLoggerMain
 */
public class MyStatsdLogger extends StatsdLoggerImpl {

    private static final long serialVersionUID = 6290698514746194732L;

    public MyStatsdLogger(Logger logger) {
        super(logger);
    }

    @Override
    protected String statMessage(StatsdStatType type, int value, double sampleRate) {
        return "{} {} {}";
    }
}
