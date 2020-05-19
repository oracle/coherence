/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.base;

import com.tangosol.util.NullImplementation;

import java.io.PrintWriter;

import static com.oracle.coherence.common.base.Formatting.toHexEscape;
import static com.oracle.coherence.common.base.Formatting.toQuotedStringEscape;
import static com.oracle.coherence.common.base.StackTrace.getExpression;
import static com.oracle.coherence.common.base.StackTrace.printStackTrace;

/**
 * Class for providing printed output functionality.
 *
 * @author lh, as  2020.04.02
 * @since Coherence 14.1.2
 */
public abstract class Loggers
    {
    // ----- printed output support ----------------------------------------------

    /**
     * Prints a blank line.
     */
    public static void out()
        {
        s_out.println();
        }

    /**
     * Prints the passed Object.
     *
     * @param o  the Object to print.
     */
    public static void out(Object o)
        {
        s_out.println(o);
        }

    /**
     * Prints the passed String value.
     *
     * @param s  the String to print.
     */
    public static void out(String s)
        {
        s_out.println(s);
        }

    /**
     * Prints the passed class information.
     *
     * @param clz  the class object to print.
     */
    public static void out(Class<?> clz)
        {
        s_out.println(Classes.toString(clz));
        }

    /**
     * Prints the passed exception information.
     *
     * @param e  the Throwable object to print.
     */
    public static void out(Throwable e)
        {
        s_out.println(printStackTrace(e));
        }

    /**
     * Prints a blank line to the trace Writer.
     */
    public static void err()
        {
        s_err.println();
        }

    /**
     * Prints the passed Object to the trace Writer.
     *
     * @param o  the Object to print.
     */
    public static void err(Object o)
        {
        s_err.println(o);
        }

    /**
     * Prints the passed String value to the trace Writer.
     *
     * @param s  the String to print.
     */
    public static void err(String s)
        {
        s_err.println(s);
        }

    /**
     * Prints the passed class information to the trace Writer.
     *
     * @param clz  the class object to print.
     */
    public static void err(Class<?> clz)
        {
        s_err.println(Classes.toString(clz));
        }

    /**
     * Prints the passed exception information to the trace Writer.
     *
     * @param e  the Throwable object to print.
     */
    public static void err(Throwable e)
        {
        s_err.println(printStackTrace(e));
        }

    /**
     * Prints a blank line to the log.
     */
    public static void log()
        {
        s_log.println();
        if (s_fEchoLog)
            {
            s_out.println();
            }
        }

    /**
     * Prints the passed Object to the log.
     *
     * @param o  the Object to print.
     */
    public static void log(Object o)
        {
        log(String.valueOf(o));
        }

    /**
     * Prints the passed String value to the log.
     *
     * @param s  the String to print.
     */
    public static void log(String s)
        {
        s_log.println(s);
        if (s_fEchoLog)
            {
            s_out.println(s);
            }
        }

    /**
     * Prints the passed class information to the log.
     *
     * @param clz  the class object to print.
     */
    public static void log(Class<?> clz)
        {
        log(Classes.toString(clz));
        }

    /**
     * Prints the passed exception information to the log.
     *
     * @param e  the Throwable object to print.
     */
    public static void log(Throwable e)
        {
        String s = printStackTrace(e);
        s_log.println(s);
        if (s_fEchoLog)
            {
            s_out.println(s);
            }
        }

    /**
     * Obtains the current writer used for printing.
     *
     * @return the current writer used for printing; never null
     */
    public static PrintWriter getOut()
        {
        return s_out;
        }

    /**
     * Sets the current writer used for printing.
     *
     * @param writer  the java.io.PrintWriter instance to use for printing;
     *                may be null
     */
    public static void setOut(PrintWriter writer)
        {
        s_out = writer == null
                        ? new PrintWriter(NullImplementation.getWriter(), true)
                        : writer;
        }

    /**
     * Obtains the current writer used for tracing.
     *
     * @return the current writer used for tracing; never null
     */
    public static PrintWriter getErr()
        {
        return s_err;
        }

    /**
     * Sets the current writer used for tracing.
     *
     * @param writer  the java.io.PrintWriter instance to use for tracing; may
     *                be null
     */
    public static void setErr(PrintWriter writer)
        {
        s_err = writer == null
                        ? new PrintWriter(NullImplementation.getWriter(), true)
                        : writer;
        }

    /**
     * Obtains the current writer used for logging.
     *
     * @return the current writer used for logging; never null
     */
    public static PrintWriter getLog()
        {
        return s_log;
        }

    /**
     * Sets the current writer used for logging.
     *
     * @param writer  the java.io.PrintWriter instance to use for logging; may
     *                be null
     */
    public static void setLog(PrintWriter writer)
        {
        s_log = writer == null
                        ? new PrintWriter(NullImplementation.getWriter(), true)
                        : writer;
        }

    /**
    * Determine if the log is echoed to the console.
    *
    * @return true if the log is echoed to the console
    */
    public static boolean isLogEcho()
        {
        return s_fEchoLog;
        }

    /**
    * Specify whether the log should echo to the console.
    *
    * @param fEcho  true if the log should echo to the console
    */
    public static void setLogEcho(boolean fEcho)
        {
        s_fEchoLog = fEcho;
        }

    // ----- debugging support:  expression evaluation ----------------------

    /**
     * Display the value of a boolean expression.
     */
    public static void trace(boolean fVal)
        {
        traceImpl(String.valueOf(fVal));
        }

    /**
     * Display the value of a char expression.
     */
    public static void trace(char chVal)
        {
        traceImpl(String.valueOf(chVal));
        }

    /**
     * Display the value of an int expression.
     */
    public static void trace(int nVal)
        {
        traceImpl(String.valueOf(nVal));
        }

    /**
     * Display the value of a long expression.
     */
    public static void trace(long lVal)
        {
        traceImpl(String.valueOf(lVal));
        }

    /**
     * Display the value of a float expression.
     */
    public static void trace(float flVal)
        {
        traceImpl(String.valueOf(flVal));
        }

    /**
     * Display the value of a double expression.
     */
    public static void trace(double dflVal)
        {
        traceImpl(String.valueOf(dflVal));
        }

    /**
     * Display the value of a byte array expression.
     */
    public static void trace(byte[] ab)
        {
        if (ab == null)
            {
            traceImpl(null);
            }
        else
            {
            traceImpl("length=" + ab.length + ", binary=" + toHexEscape(ab));
            }
        }

    /**
     * Display the value of a String expression.
     */
    public static void trace(String sVal)
        {
        traceImpl(sVal == null ? "null" : toQuotedStringEscape(sVal));
        }

    /**
     * Display the value of an Object expression.
     */
    public static void trace(Object oVal)
        {
        traceImpl(String.valueOf(oVal));
        }


    /**
     * Internal implementation for trace methods.
     */
    private static void traceImpl(String sVal)
        {
        String sExpr = getExpression("trace");
        out((sExpr == null ? "?" : sExpr) + '=' + (sVal == null ? "null" : sVal));
        }


    // ----- data members ---------------------------------------------------

    /**
     * The writer to use for print output.
     */
    private static PrintWriter s_out = new PrintWriter(System.out, true);

    /**
     * The writer to use for trace output.
     */
    private static PrintWriter s_err = new PrintWriter(System.err, true);

    /**
     * The writer to use for logging.  By default, there is no persistent
     * log.
     */
    private static PrintWriter s_log = new PrintWriter(NullImplementation.getWriter(), true);

    /**
     * Option to log to the console in addition to the logging writer.  By
     * default, all logged messages are echoed to the console.
     */
    private static boolean s_fEchoLog = true;
    }
