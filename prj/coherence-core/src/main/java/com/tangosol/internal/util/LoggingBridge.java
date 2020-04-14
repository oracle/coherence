/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util;

import com.tangosol.net.CacheFactory;

import com.tangosol.util.Base;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
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
    public static Logger createBridge()
        {
        Logger logger = Logger.getAnonymousLogger();

        int nLevel = 0;
        for (; nLevel < 9 && CacheFactory.isLogEnabled(nLevel); ++nLevel)
            {
            }

        Level level;
        if (CacheFactory.isLogEnabled(9))
            {
            level = Level.ALL;
            }
        else if (CacheFactory.isLogEnabled(7))
            {
            level = Level.FINER;
            }
        else if (CacheFactory.isLogEnabled(5))
            {
            level = Level.FINE;
            }
        else if (CacheFactory.isLogEnabled(4))
            {
            level = Level.CONFIG;
            }
        else if (CacheFactory.isLogEnabled(Base.LOG_INFO))
            {
            level = Level.INFO;
            }
        else if (CacheFactory.isLogEnabled(Base.LOG_WARN))
            {
            level = Level.WARNING;
            }
        else // if (CacheFactory.isLogEnabled(Base.LOG_ERR))
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
                int nLevelDst = nLevelSrc <= FINEST  ? 9
                              : nLevelSrc <= FINER   ? 7
                              : nLevelSrc <= FINE    ? 5
                              : nLevelSrc <= CONFIG  ? 4
                              : nLevelSrc <= INFO    ? CacheFactory.LOG_INFO
                              : nLevelSrc <= WARNING ? CacheFactory.LOG_WARN
                              :            /*SEVERE*/  CacheFactory.LOG_ERR;

                if (CacheFactory.isLogEnabled(nLevelDst))
                    {
                    Throwable ex = record.getThrown();
                    if (ex == null)
                        {
                        CacheFactory.log(getFormatter().formatMessage(record), nLevelDst);
                        }
                    else
                        {
                        CacheFactory.log(getFormatter().formatMessage(record) + "\n" +
                                Base.printStackTrace(ex), nLevelDst);
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
