/*
 * Copyright (c) 2020, 2022 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.base;

import com.tangosol.net.CacheFactory;

import com.tangosol.util.Base;

import java.util.Arrays;
import java.util.Objects;

import java.util.function.Supplier;

import static com.oracle.coherence.common.base.StackTrace.printStackTrace;

/**
 * Logging API.
 *
 * @author Aleks Seovic  2020.05.18
 * @since 20.06
 */
public abstract class Logger
    {
    // ----- basic logging support ------------------------------------------

    /**
     * Return {@code true} if the specified severity level should be logged.
     *
     * @param nSeverity  the severity level
     *
     * @return {@code true} if the specified severity level should be logged;
     *         {@code false} otherwise
     */
    @SuppressWarnings("deprecation")
    public static boolean isEnabled(int nSeverity)
        {
        return CacheFactory.isLogEnabled(nSeverity);
        }

    /**
     * Log the specified message at the specified severity level.
     *
     * @param sMessage   the message to log
     * @param nSeverity  the severity level
     */
    @SuppressWarnings("deprecation")
    public static void log(String sMessage, int nSeverity)
        {
        CacheFactory.log(sMessage, nSeverity);
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
            log(supplierMessage.get(), nSeverity);
            }
        }

    /**
     * Log the specified message and the exception stack trace at the
     * specified severity level.
     *
     * @param sMessage   the message to log
     * @param e          the exception to log the stack trace for
     * @param nSeverity  the severity level
     */
    public static void log(String sMessage, Throwable e, int nSeverity)
        {
        if (isEnabled(nSeverity))
            {
            sMessage = sMessage.trim();
            if (sMessage.length() > 0 && sMessage.charAt(sMessage.length() - 1) != ':')
                {
                sMessage += ':';
                }
            log(sMessage + " " + e, nSeverity);
            log(printStackTrace(e), nSeverity);
            }
        }

    /**
     * Log the specified message and the exception stack trace at the
     * specified severity level.
     * 
     * The message is provided by the {@link Supplier}, which will only be 
     * evaluated if the messages should be logged at the specified severity
     * level. This avoids potentially expensive message construction if the
     * message isn't going to be logged.
     *
     * @param supplierMessage  the supplier of the message to log; only evaluated
     *                         if the specified severity level should be logged
     * @param e                the exception to log the stack trace for
     * @param nSeverity        the severity level
     */
    public static void log(Supplier<String> supplierMessage, Throwable e, int nSeverity)
        {
        if (isEnabled(nSeverity))
            {
            log(supplierMessage.get(), e, nSeverity);
            }
        }

    /**
     * Log the specified exception information (message and stack trace)
     * at the specified severity level.
     *
     * @param e          the exception to log
     * @param nSeverity  the severity level
     */
    public static void log(Throwable e, int nSeverity)
        {
        log(e.toString(), nSeverity);
        log(printStackTrace(e), nSeverity);
        }

    // ---- ALWAYS support --------------------------------------------------

    /**
     * Log the specified message with {@link #ALWAYS} severity.
     *
     * @param sMessage  the message to log
     */
    public static void out(String sMessage)
        {
        log(sMessage, ALWAYS);
        }

    /**
     * Log the specified message with {@link #ALWAYS} severity.
     *
     * The message is provided by the {@link Supplier}, which will only be
     * evaluated if the messages should be logged at the specified severity
     * level. This avoids potentially expensive message construction if the
     * message isn't going to be logged.
     *
     * @param supplierMessage  the supplier of the message to log; only evaluated
     *                         if the specified severity level should be logged
     */
    public static void out(Supplier<String> supplierMessage)
        {
        log(supplierMessage, ALWAYS);
        }

    /**
     * Log the specified message and the exception stack trace with
     * {@link #ALWAYS} severity.
     *
     * @param sMessage  the message to log
     * @param e         the exception to log the stack trace for
     */
    public static void out(String sMessage, Throwable e)
        {
        log(sMessage, e, ALWAYS);
        }

    /**
     * Log the specified message and the exception stack trace with
     * {@link #ALWAYS} severity.
     *
     * The message is provided by the {@link Supplier}, which will only be
     * evaluated if the messages should be logged at the specified severity
     * level. This avoids potentially expensive message construction if the
     * message isn't going to be logged.
     *
     * @param supplierMessage  the supplier of the message to log; only evaluated
     *                         if the specified severity level should be logged
     * @param e                the exception to log the stack trace for
     */
    public static void out(Supplier<String> supplierMessage, Throwable e)
        {
        log(supplierMessage, e, ALWAYS);
        }

    /**
     * Log the specified exception information (message and stack trace) with
     * {@link #ALWAYS} severity.
     *
     * @param e  the exception to log
     */
    public static void out(Throwable e)
        {
        log(e, ALWAYS);
        }

    // ---- ERROR support ---------------------------------------------------

    /**
     * Log the specified message with {@link #ERROR} severity.
     *
     * @param sMessage  the message to log
     */
    public static void err(String sMessage)
        {
        log(sMessage, ERROR);
        }

    /**
     * Log the specified message with {@link #ERROR} severity.
     *
     * The message is provided by the {@link Supplier}, which will only be
     * evaluated if the messages should be logged at the specified severity
     * level. This avoids potentially expensive message construction if the
     * message isn't going to be logged.
     *
     * @param supplierMessage  the supplier of the message to log; only evaluated
     *                         if the specified severity level should be logged
     */
    public static void err(Supplier<String> supplierMessage)
        {
        log(supplierMessage, ERROR);
        }

    /**
     * Log the specified message and the exception stack trace with
     * {@link #ERROR} severity.
     *
     * @param sMessage  the message to log
     * @param e         the exception to log the stack trace for
     */
    public static void err(String sMessage, Throwable e)
        {
        log(sMessage, e, ERROR);
        }

    /**
     * Log the specified message and the exception stack trace with
     * {@link #ERROR} severity.
     *
     * The message is provided by the {@link Supplier}, which will only be
     * evaluated if the messages should be logged at the specified severity
     * level. This avoids potentially expensive message construction if the
     * message isn't going to be logged.
     *
     * @param supplierMessage  the supplier of the message to log; only evaluated
     *                         if the specified severity level should be logged
     * @param e                the exception to log the stack trace for
     */
    public static void err(Supplier<String> supplierMessage, Throwable e)
        {
        log(supplierMessage, e, ERROR);
        }

    /**
     * Log the specified exception information (message and stack trace) with
     * {@link #ERROR} severity.
     *
     * @param e  the exception to log
     */
    public static void err(Throwable e)
        {
        log(e, ERROR);
        }

    // ---- WARNING support -------------------------------------------------

    /**
     * Log the specified message with {@link #WARNING} severity.
     *
     * @param sMessage  the message to log
     */
    public static void warn(String sMessage)
        {
        log(sMessage, WARNING);
        }

    /**
     * Log the specified message with {@link #WARNING} severity.
     *
     * The message is provided by the {@link Supplier}, which will only be
     * evaluated if the messages should be logged at the specified severity
     * level. This avoids potentially expensive message construction if the
     * message isn't going to be logged.
     *
     * @param supplierMessage  the supplier of the message to log; only evaluated
     *                         if the specified severity level should be logged
     */
    public static void warn(Supplier<String> supplierMessage)
        {
        log(supplierMessage, WARNING);
        }

    /**
     * Log the specified message and the exception stack trace with
     * {@link #WARNING} severity.
     *
     * @param sMessage  the message to log
     * @param e         the exception to log the stack trace for
     */
    public static void warn(String sMessage, Throwable e)
        {
        log(sMessage, e, WARNING);
        }

    /**
     * Log the specified message and the exception stack trace with
     * {@link #WARNING} severity.
     *
     * The message is provided by the {@link Supplier}, which will only be
     * evaluated if the messages should be logged at the specified severity
     * level. This avoids potentially expensive message construction if the
     * message isn't going to be logged.
     *
     * @param supplierMessage  the supplier of the message to log; only evaluated
     *                         if the specified severity level should be logged
     * @param e                the exception to log the stack trace for
     */
    public static void warn(Supplier<String> supplierMessage, Throwable e)
        {
        log(supplierMessage, e, WARNING);
        }

    /**
     * Log the specified exception information (message and stack trace) with
     * {@link #WARNING} severity.
     *
     * @param e  the exception to log
     */
    public static void warn(Throwable e)
        {
        log(e, WARNING);
        }

    // ---- INFO support ----------------------------------------------------

    /**
     * Log the specified message with {@link #INFO} severity.
     *
     * @param sMessage  the message to log
     */
    public static void info(String sMessage)
        {
        log(sMessage, INFO);
        }

    /**
     * Log the specified message with {@link #INFO} severity.
     *
     * The message is provided by the {@link Supplier}, which will only be
     * evaluated if the messages should be logged at the specified severity
     * level. This avoids potentially expensive message construction if the
     * message isn't going to be logged.
     *
     * @param supplierMessage  the supplier of the message to log; only evaluated
     *                         if the specified severity level should be logged
     */
    public static void info(Supplier<String> supplierMessage)
        {
        log(supplierMessage, INFO);
        }

    /**
     * Log the specified message and the exception stack trace with
     * {@link #INFO} severity.
     *
     * @param sMessage  the message to log
     * @param e         the exception to log the stack trace for
     */
    public static void info(String sMessage, Throwable e)
        {
        log(sMessage, e, INFO);
        }

    /**
     * Log the specified message and the exception stack trace with
     * {@link #INFO} severity.
     *
     * The message is provided by the {@link Supplier}, which will only be
     * evaluated if the messages should be logged at the specified severity
     * level. This avoids potentially expensive message construction if the
     * message isn't going to be logged.
     *
     * @param supplierMessage  the supplier of the message to log; only evaluated
     *                         if the specified severity level should be logged
     * @param e                the exception to log the stack trace for
     */
    public static void info(Supplier<String> supplierMessage, Throwable e)
        {
        log(supplierMessage, e, INFO);
        }

    /**
     * Log the specified exception information (message and stack trace) with
     * {@link #INFO} severity.
     *
     * @param e  the exception to log
     */
    public static void info(Throwable e)
        {
        log(e, INFO);
        }

    // ---- CONFIG support ----------------------------------------------------

    /**
     * Log the specified message with {@link #CONFIG} severity.
     *
     * @param sMessage  the message to log
     */
    public static void config(String sMessage)
        {
        log(sMessage, CONFIG);
        }

    /**
     * Log the specified message with {@link #CONFIG} severity.
     *
     * The message is provided by the {@link Supplier}, which will only be
     * evaluated if the messages should be logged at the specified severity
     * level. This avoids potentially expensive message construction if the
     * message isn't going to be logged.
     *
     * @param supplierMessage  the supplier of the message to log; only evaluated
     *                         if the specified severity level should be logged
     */
    public static void config(Supplier<String> supplierMessage)
        {
        log(supplierMessage, CONFIG);
        }

    /**
     * Log the specified message and the exception stack trace with
     * {@link #CONFIG} severity.
     *
     * @param sMessage  the message to log
     * @param e         the exception to log the stack trace for
     */
    public static void config(String sMessage, Throwable e)
        {
        log(sMessage, e, CONFIG);
        }

    /**
     * Log the specified message and the exception stack trace with
     * {@link #CONFIG} severity.
     *
     * The message is provided by the {@link Supplier}, which will only be
     * evaluated if the messages should be logged at the specified severity
     * level. This avoids potentially expensive message construction if the
     * message isn't going to be logged.
     *
     * @param supplierMessage  the supplier of the message to log; only evaluated
     *                         if the specified severity level should be logged
     * @param e                the exception to log the stack trace for
     */
    public static void config(Supplier<String> supplierMessage, Throwable e)
        {
        log(supplierMessage, e, CONFIG);
        }

    /**
     * Log the specified exception information (message and stack trace) with
     * {@link #CONFIG} severity.
     *
     * @param e  the exception to log
     */
    public static void config(Throwable e)
        {
        log(e, CONFIG);
        }

    // ---- FINE support ----------------------------------------------------

    /**
     * Log the specified message with {@link #FINE} severity.
     *
     * @param sMessage  the message to log
     */
    public static void fine(String sMessage)
        {
        log(sMessage, FINE);
        }

    /**
     * Log the specified message with {@link #FINE} severity.
     *
     * The message is provided by the {@link Supplier}, which will only be
     * evaluated if the messages should be logged at the specified severity
     * level. This avoids potentially expensive message construction if the
     * message isn't going to be logged.
     *
     * @param supplierMessage  the supplier of the message to log; only evaluated
     *                         if the specified severity level should be logged
     */
    public static void fine(Supplier<String> supplierMessage)
        {
        log(supplierMessage, FINE);
        }

    /**
     * Log the specified message and the exception stack trace with 
     * {@link #FINE} severity.
     *
     * @param sMessage  the message to log
     * @param e         the exception to log the stack trace for
     */
    public static void fine(String sMessage, Throwable e)
        {
        log(sMessage, e, FINE);
        }

    /**
     * Log the specified message and the exception stack trace with 
     * {@link #FINE} severity.
     *
     * The message is provided by the {@link Supplier}, which will only be
     * evaluated if the messages should be logged at the specified severity
     * level. This avoids potentially expensive message construction if the
     * message isn't going to be logged.
     *
     * @param supplierMessage  the supplier of the message to log; only evaluated
     *                         if the specified severity level should be logged
     * @param e                the exception to log the stack trace for
     */
    public static void fine(Supplier<String> supplierMessage, Throwable e)
        {
        log(supplierMessage, e, FINE);
        }

    /**
     * Log the specified exception information (message and stack trace) with 
     * {@link #FINE} severity.
     *
     * @param e  the exception to log
     */
    public static void fine(Throwable e)
        {
        log(e, FINE);
        }

    // ---- FINER support ----------------------------------------------------

    /**
     * Log the specified message with {@link #FINER} severity.
     *
     * @param sMessage  the message to log
     */
    public static void finer(String sMessage)
        {
        log(sMessage, FINER);
        }

    /**
     * Log the specified message with {@link #FINER} severity.
     *
     * The message is provided by the {@link Supplier}, which will only be
     * evaluated if the messages should be logged at the specified severity
     * level. This avoids potentially expensive message construction if the
     * message isn't going to be logged.
     *
     * @param supplierMessage  the supplier of the message to log; only evaluated
     *                         if the specified severity level should be logged
     */
    public static void finer(Supplier<String> supplierMessage)
        {
        log(supplierMessage, FINER);
        }

    /**
     * Log the specified message and the exception stack trace with 
     * {@link #FINER} severity.
     *
     * @param sMessage  the message to log
     * @param e         the exception to log the stack trace for
     */
    public static void finer(String sMessage, Throwable e)
        {
        log(sMessage, e, FINER);
        }

    /**
     * Log the specified message and the exception stack trace with 
     * {@link #FINER} severity.
     *
     * The message is provided by the {@link Supplier}, which will only be
     * evaluated if the messages should be logged at the specified severity
     * level. This avoids potentially expensive message construction if the
     * message isn't going to be logged.
     *
     * @param supplierMessage  the supplier of the message to log; only evaluated
     *                         if the specified severity level should be logged
     * @param e                the exception to log the stack trace for
     */
    public static void finer(Supplier<String> supplierMessage, Throwable e)
        {
        log(supplierMessage, e, FINER);
        }

    /**
     * Log the specified exception information (message and stack trace) with 
     * {@link #FINER} severity.
     *
     * @param e  the exception to log
     */
    public static void finer(Throwable e)
        {
        log(e, FINER);
        }

    // ---- FINEST support --------------------------------------------------

    /**
     * Log the specified message with {@link #FINEST} severity.
     *
     * @param sMessage  the message to log
     */
    public static void finest(String sMessage)
        {
        log(sMessage, FINEST);
        }

    /**
     * Log the specified message with {@link #FINEST} severity.
     *
     * The message is provided by the {@link Supplier}, which will only be
     * evaluated if the messages should be logged at the specified severity
     * level. This avoids potentially expensive message construction if the
     * message isn't going to be logged.
     *
     * @param supplierMessage  the supplier of the message to log; only evaluated
     *                         if the specified severity level should be logged
     */
    public static void finest(Supplier<String> supplierMessage)
        {
        log(supplierMessage, FINEST);
        }

    /**
     * Log the specified message and the exception stack trace with 
     * {@link #FINEST} severity.
     *
     * @param sMessage  the message to log
     * @param e         the exception to log the stack trace for
     */
    public static void finest(String sMessage, Throwable e)
        {
        log(sMessage, e, FINEST);
        }

    /**
     * Log the specified message and the exception stack trace with 
     * {@link #FINEST} severity.
     *
     * The message is provided by the {@link Supplier}, which will only be
     * evaluated if the messages should be logged at the specified severity
     * level. This avoids potentially expensive message construction if the
     * message isn't going to be logged.
     *
     * @param supplierMessage  the supplier of the message to log; only evaluated
     *                         if the specified severity level should be logged
     * @param e                the exception to log the stack trace for
     */
    public static void finest(Supplier<String> supplierMessage, Throwable e)
        {
        log(supplierMessage, e, FINEST);
        }

    /**
     * Log the specified exception information (message and stack trace) with 
     * {@link #FINEST} severity.
     *
     * @param e  the exception to log
     */
    public static void finest(Throwable e)
        {
        log(e, FINEST);
        }

    // ---- entering, exiting, throwing support -----------------------------

    /**
     * Entry logging.
     *
     * @param clz      the source {@link Class}
     * @param sMethod  the source method
     * @param params   zero or more parameters to log
     *
     * @throws NullPointerException if either {@code clz} or {@code sMethod}
     *                              are {@code null}
     *
     * @since 22.06
     */
    public static void entering(Class<?> clz, String sMethod, Object... params)
        {
        ensureRequirements(clz, sMethod);

        finest(() -> (params == null || params.length == 0)
                        ? String.format("ENTRY [%s.%s]", clz.getName(), sMethod)
                        : String.format("ENTRY [%s.%s] params=%s", clz.getName(), sMethod, Arrays.toString(params)));
        }

    /**
     * Exit logging.
     *
     * @param clz      the source {@link Class}
     * @param sMethod  the source method
     *
     * @throws NullPointerException if either {@code clz} or {@code sMethod}
     *                              are {@code null}
     *
     * @since 22.06
     */
    public static void exiting(Class<?> clz, String sMethod)
        {
        ensureRequirements(clz, sMethod);

        finest(() -> String.format("EXIT [%s.%s]", clz.getName(), sMethod));
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
     *
     * @since 22.06
     */
    public static void exiting(Class<?> clz, String sMethod, Object result, Object... additionalInfo)
        {
        ensureRequirements(clz, sMethod);

        finest(() -> (additionalInfo == null || additionalInfo.length == 0)
                     ? String.format("EXIT [%s.%s] returning=%s", clz.getName(), sMethod, result)
                     : String.format("EXIT [%s.%s] returning=%s, additional-info=%s",
                                     clz.getName(), sMethod, result, Arrays.toString(additionalInfo)));
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
     *
     * @since 22.06
     */
    public static void throwing(Class<?> clz, String sMethod, Throwable throwable, Object... additionalInfo)
        {
        Objects.requireNonNull(throwable, "A throwable must be specified");
        ensureRequirements(clz, sMethod);

        finest(() -> String.format("THROWING [%s.%s] exception=%s, additional-info=%s",
                                   clz.getName(), sMethod, Base.getStackTrace(throwable),
                                   Arrays.toString(additionalInfo)));
        }

    /**
     * Set the logging level.
     *
     * @param nSeverity  the severity level
     *
     * @since 22.09
     */
    public static void setLoggingLevel(int nSeverity)
        {
        CacheFactory.setLoggingLevel(Integer.valueOf(nSeverity));
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Ensures the provided arguments are not {@code null}.
     *
     * @param clz      the source {@link Class}
     * @param sMethod  the source method
     *
     * @since 22.06
     */
    private static void ensureRequirements(Class<?> clz, String sMethod)
        {
        Objects.requireNonNull(clz,     "A class must be specified");
        Objects.requireNonNull(sMethod, "A method name must be specified");
        }

    // ---- constants -------------------------------------------------------

    /**
     * Severity 0 will always be logged.
     */
    public static final int ALWAYS = 0;

    /**
     * Severity 1 indicates an error.
     */
    public static final int ERROR = 1;

    /**
     * Severity 2 indicates a warning.
     */
    public static final int WARNING = 2;

    /**
     * Severity 3 indicates information that should likely be logged.
     */
    public static final int INFO = 3;

    /**
     * Severity 4 indicates configuration related log message.
     */
    public static final int CONFIG = 4;

    /**
     * Severity 5 indicates an essential debug message.
     */
    public static final int FINE = 5;

    /**
     * Severity 6 indicates a non-essential debug message.
     */
    public static final int FINER = 6;

    /**
     * Severity 7 indicates a very low-level, non-essential debug message
     * or a tracing message.
     */
    public static final int FINEST = 7;
    }
