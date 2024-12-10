/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.util.logging;

import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;


/**
 * A Java logging formatter which includes the name of the thread which issued the log message.
 * <p>
 * More precisely it includes the name of the thread which <tt>formats</tt> the message, and thus
 * in the case of an asynchronous Handler this may not be the thread which logged the message. But
 * For simple Handlers such as the {@link java.util.logging.ConsoleHandler} or {@link java.util.logging.FileHandler}
 * this formatter will be able to include the name of the thread which has issued the log message.
 * </p>
 *
 * @author mf  2013.09.23
 */
public class SimpleThreadAwareFormatter
    extends SimpleFormatter
    {
    @Override
    public String format(LogRecord record)
        {
        String sTxt   = super.format(record);
        int    ofLine = sTxt.indexOf(System.lineSeparator());

        return sTxt.substring(0, ofLine) + " " + Thread.currentThread().getName() + sTxt.substring(ofLine);
        }
    }
