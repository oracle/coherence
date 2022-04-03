/*
 * Copyright (c) 2016, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.concurrent.executor.internal;

import com.oracle.coherence.common.base.Logger;

import com.oracle.coherence.concurrent.executor.ClusteredRegistration;

import com.oracle.coherence.concurrent.executor.options.Debugging;

import com.tangosol.util.Base;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Utility class used to trace executor processors, subscribers, and tasks.
 *
 * @author lh
 * @since 21.12
 */
public class ExecutorTrace
    {
    // ----- public static methods ------------------------------------------

    /**
     * Return true if executor trace logging is enabled; false otherwise.
     *
     * @param nSeverity  the severity level
     *
     * @return true if executor trace logging is enabled
     */
    public static boolean isEnabled(int nSeverity)
        {
        return ClusteredRegistration.s_fTraceLogging && Logger.isEnabled(nSeverity);
        }

    /**
     * Log the specified message at the Debugging severity level. Default
     * severity level is Logger.FINEST.
     *
     * @param message  the message to log
     */
    public static void log(String message)
        {
        log(message, LOGLEVEL);
        }

    /**
     * Log the specified message with the given debugging option.
     *
     * @param message    the message to log
     * @param debugging  the debugging option
     */
    public static void log(String message, Debugging debugging)
        {
        Logger.log(message, debugging.getLogLevel());
        }

    /**
     * Log the specified message at the specified severity level.
     *
     * @param message    the message to log
     * @param nSeverity  the severity level
     */
    public static void log(String message, int nSeverity)
        {
        if (isEnabled(nSeverity))
            {
            Logger.log(message, nSeverity);
            }
        }

    /**
     * Log the specified message at the Debugging severity level. Default
     * severity level is Logger.FINEST.
     *
     * The message is provided by the {@link Supplier}, which will only be
     * evaluated if the messages should be logged at the specified severity
     * level. This avoids potentially expensive message construction if the
     * message isn't going to be logged.
     *
     * @param supplierMessage  the supplier of the message to log; only evaluated
     *                         if the Debugging severity level should be logged
     */
    public static void log(Supplier<String> supplierMessage)
        {
        log(supplierMessage, LOGLEVEL);
        }

    /**
     * Log the specified message with the given debugging option.
     *
     * The message is provided by the {@link Supplier}, which will only be
     * evaluated if the messages should be logged at the specified severity
     * level. This avoids potentially expensive message construction if the
     * message isn't going to be logged.
     *
     * @param supplierMessage  the supplier of the message to log; only evaluated
     *                         if the specified severity level should be logged
     * @param debugging        the debugging option
     */
    public static void log(Supplier<String> supplierMessage, Debugging debugging)
        {
        Logger.log(supplierMessage, debugging.getLogLevel());
        }

    /**
     * Log the specified message at the specified severity level.
     *
     * The message is provided by the {@link Supplier}, which will only be
     * evaluated if the messages should be logged at the specified severity
     * level. This avoids potentially expensive message construction if the
     * message isn't going to be logged.
     *
     * @param supplierMessage  the supplier of the message to log; only evaluated
     *                         if the specified severity level should be logged
     * @param nSeverity        the severity level
     */
    public static void log(Supplier<String> supplierMessage, int nSeverity)
        {
        if (isEnabled(nSeverity))
            {
            Logger.log(supplierMessage, nSeverity);
            }
        }

    /**
     * Entry logging.
     *
     * @param clz      the source {@link Class}
     * @param sMethod  the source method
     * @param params   zero or more parameters to log
     *
     * @throws NullPointerException if either {@code clz} or {@code sMethod}
     *                              are {@code null}
     */
    public static void entering(Class<?> clz, String sMethod, Object... params)
        {
        ensureRequirements(clz, sMethod);

        ExecutorTrace.log(() ->
                                  (params == null || params.length == 0)
                                  ? String.format("ENTRY [%s.%s]", clz.getName(), sMethod)
                                  : String.format("ENTRY [%s.%s] params=%s", clz.getName(), sMethod, Arrays.toString(params)),
                          Logger.FINEST);
        }

    /**
     * Exit logging.
     *
     * @param clz      the source {@link Class}
     * @param sMethod  the source method
     *
     * @throws NullPointerException if either {@code clz} or {@code sMethod}
     *                              are {@code null}
     */
    public static void exiting(Class<?> clz, String sMethod)
        {
        ensureRequirements(clz, sMethod);

        ExecutorTrace.log(() -> String.format("EXIT [%s.%s]", clz.getName(), sMethod), Logger.FINEST);
        }

    /**
     * Exit logging.
     *
     * @param clz            the source {@link Class}
     * @param sMethod        the source method
     * @param result         the result returned by the exiting method
     * @param additionalInfo zero or more additional state details at the time of exit
     *
     * @throws NullPointerException if either {@code clz} or {@code sMethod}
     *                              are {@code null}
     */
    public static void exiting(Class<?> clz, String sMethod, Object result, Object... additionalInfo)
        {
        ensureRequirements(clz, sMethod);

        ExecutorTrace.log(() -> (additionalInfo == null || additionalInfo.length == 0)
                                ? String.format("EXIT [%s.%s] returning=%s", clz.getName(), sMethod, result)
                                : String.format("EXIT [%s.%s] returning=%s, additional-info=%s",
                                                clz.getName(), sMethod, result, Arrays.toString(additionalInfo)),
                          Logger.FINEST);

        }

    /**
     * Throwable logging.
     *
     * @param clz             the source {@link Class}
     * @param sMethod         the source method
     * @param throwable       the {@link Exception} being thrown
     * @param additionalInfo  zero or more additional state details at the time of exit
     *
     * @throws NullPointerException if any arguments are {@code null}
     */
    public static void throwing(Class<?> clz, String sMethod, Throwable throwable, Object... additionalInfo)
        {
        Objects.requireNonNull(throwable, "A throwable must be specified");
        ensureRequirements(clz, sMethod);

        ExecutorTrace.log(() ->
                                  String.format("THROWING [%s.%s] exception=%s, additional-info=%s",
                                                clz.getName(), sMethod, Base.getStackTrace(throwable), Arrays.toString(additionalInfo)),
                          Logger.FINEST);
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Ensures the provided arguments are not {@code null}.
     *
     * @param clz      the source {@link Class}
     * @param sMethod  the source method
     */
    private static void ensureRequirements(Class<?> clz, String sMethod)
        {
        Objects.requireNonNull(clz,     "A class must be specified");
        Objects.requireNonNull(sMethod, "A method name must be specified");
        }

    // ----- static data members ---------------------------------------------

    /**
     * Log level for ExecutorTrace messages
     */
    public static final int LOGLEVEL = Logger.FINEST;
    }
