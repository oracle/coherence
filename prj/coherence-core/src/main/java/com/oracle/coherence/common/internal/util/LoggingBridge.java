/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.internal.util;

import com.oracle.coherence.common.base.Logger;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.StreamHandler;

/**
 * A bridge between the JDK standard logger and the Coherence logger.
 *
 * @author  mf  2018.06.11
 */
public class LoggingBridge
    {
    /**
     * Construct an anonymous logger which will direct its output to the Coherence logger.
     *
     * @return a bridge logger
     */
    public static java.util.logging.Logger createBridge()
        {
        java.util.logging.Logger logger = java.util.logging.Logger.getAnonymousLogger();

        Level level;
        if (Logger.isEnabled(Logger.FINEST))
            {
            level = Level.FINEST;
            }
        else if (Logger.isEnabled(Logger.FINER))
            {
            level = Level.FINER;
            }
        else if (Logger.isEnabled(Logger.FINE))
            {
            level = Level.FINE;
            }
        else if (Logger.isEnabled(Logger.CONFIG))
            {
            level = Level.CONFIG;
            }
        else if (Logger.isEnabled(Logger.INFO))
            {
            level = Level.INFO;
            }
        else if (Logger.isEnabled(Logger.WARNING))
            {
            level = Level.WARNING;
            }
        else // if (Logger.isEnabled(Logger.ERROR))
            {
            level = Level.SEVERE;
            }

        logger.setLevel(level);
        logger.setUseParentHandlers(false);

        logger.addHandler(new StreamHandler()
            {
            @Override
            public void publish(LogRecord record)
                {
                int nLevelSrc = record.getLevel().intValue();
                int nLevelDst = nLevelSrc <= FINEST  ? Logger.FINEST
                              : nLevelSrc <= FINER   ? Logger.FINER
                              : nLevelSrc <= FINE    ? Logger.FINE
                              : nLevelSrc <= CONFIG  ? Logger.CONFIG
                              : nLevelSrc <= INFO    ? Logger.INFO
                              : nLevelSrc <= WARNING ? Logger.WARNING
                              :            /*SEVERE*/  Logger.ERROR;

                if (Logger.isEnabled(nLevelDst))
                    {
                    Throwable ex = record.getThrown();
                    if (ex == null)
                        {
                        Logger.log(getFormatter().formatMessage(record), nLevelDst);
                        }
                    else
                        {
                        Logger.log(getFormatter().formatMessage(record), ex, nLevelDst);
                        }
                    }
                }

            @Override
            public void flush()
                {
                }

            @Override
            public void close()
                    throws SecurityException
                {
                }
            });

        return logger;
        }

    // ----- constants ------------------------------------------------------

    protected static final int FINEST  = Level.FINEST.intValue();
    protected static final int FINER   = Level.FINER.intValue();
    protected static final int FINE    = Level.FINE.intValue();
    protected static final int CONFIG  = Level.CONFIG.intValue();
    protected static final int INFO    = Level.INFO.intValue();
    protected static final int WARNING = Level.WARNING.intValue();
    }
