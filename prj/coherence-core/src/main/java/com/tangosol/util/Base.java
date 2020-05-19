/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util;

import com.oracle.coherence.common.base.Assertions;
import com.oracle.coherence.common.base.Blocking;
import com.oracle.coherence.common.base.Classes;
import com.oracle.coherence.common.base.Exceptions;
import com.oracle.coherence.common.base.Hasher;
import com.oracle.coherence.common.base.HashHelper;
import com.oracle.coherence.common.base.Formatting;
import com.oracle.coherence.common.base.Objects;
import com.oracle.coherence.common.base.Loggers;
import com.oracle.coherence.common.base.Randoms;
import com.oracle.coherence.common.base.Reads;
import com.oracle.coherence.common.base.StackTrace;

import com.oracle.coherence.common.base.TimeHelper;
import com.oracle.coherence.common.util.CommonMonitor;

import com.tangosol.coherence.config.Config;

import com.tangosol.net.CacheFactory;

import java.io.CharArrayWriter;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;

import java.math.BigDecimal;

import java.net.URL;

import java.rmi.RemoteException;

import java.security.AccessController;
import java.security.PrivilegedAction;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.TimeZone;

/**
 * Base class for providing standard functionality.
 *
 * @author cp  2000.08.02
 */
@SuppressWarnings({"unused", "deprecation"})
public abstract class Base
    {
    // ----- debugging support:  expression evaluation ----------------------

    /**
     * Display the value of a boolean expression.
     *
     * @param fVal  the boolean value
     */
    public static void trace(boolean fVal)
        {
        Loggers.trace(fVal);
        }

    /**
     * Display the value of a char expression.
     *
     * @param chVal  the char value
     */
    public static void trace(char chVal)
        {
        Loggers.trace(chVal);
        }

    /**
     * Display the value of an int expression.
     *
     * @param nVal  the int value
     */
    public static void trace(int nVal)
        {
        Loggers.trace(nVal);
        }

    /**
     * Display the value of a long expression.
     *
     * @param lVal  the long value
     */
    public static void trace(long lVal)
        {
        Loggers.trace(lVal);
        }

    /**
     * Display the value of a float expression.
     *
     * @param flVal  the float value
     */
    public static void trace(float flVal)
        {
        Loggers.trace(flVal);
        }

    /**
     * Display the value of a double expression.
     *
     * @param dflVal  the double value
     */
    public static void trace(double dflVal)
        {
        Loggers.trace(dflVal);
        }

    /**
     * Display the value of a byte array expression.
     *
     * @param ab  the byte array value
     */
    public static void trace(byte[] ab)
        {
        Loggers.trace(ab);
        }

    /**
     * Display the value of a String expression.
     *
     * @param sVal  the String value
     */
    public static void trace(String sVal)
        {
        Loggers.trace(sVal);
        }

    /**
     * Display the value of an Object expression.
     *
     * @param oVal  the Object value
     */
    public static void trace(Object oVal)
        {
        Loggers.trace(oVal);
        }

    // ----- assertion support ----------------------------------------------

    /**
     * Definite assertion failure.
     *
     * @return null
     */
    public static RuntimeException azzert()
        {
        return Assertions.azzert();
        }

    /**
     * Test an assertion.
     *
     * @param f  the boolean to be checked
     */
    public static void azzert(boolean f)
        {
        Assertions.azzert(f);
        }

    /**
     * Test an assertion, and print the specified message if the assertion
     * fails.
     *
     * @param f  the boolean to be checked
     * @param s  the assertion message
     */
    public static void azzert(boolean f, String s)
        {
        Assertions.azzert(f, s);
        }

    /**
     * Throw an assertion exception.
     *
     * @param sMsg  the assertion message
     */
    public static void azzertFailed(String sMsg)
        {
        Assertions.azzertFailed(sMsg);
        }


    // ----- exception support ----------------------------------------------

    /**
     * Convert the passed exception to a RuntimeException if necessary.
     *
     * @apiNote We can't use the {@link Exceptions#ensureRuntimeException(Throwable)}
     *          because we want to return a {@link WrapperException} for Coherence.
     *
     * @param e  any Throwable object
     *
     * @return a RuntimeException
     */
    public static RuntimeException ensureRuntimeException(Throwable e)
        {
        return ensureRuntimeException(e, null);
        }

    /**
     * Convert the passed exception to a RuntimeException if necessary.
     *
     * @param e any Throwable object
     * @param s an additional description
     *
     * @return a RuntimeException
     *
     * @apiNote We can't use the {@link Exceptions#ensureRuntimeException(Throwable, String)}
     *          because we want to return a {@link WrapperException} for Coherence.
     */
    public static RuntimeException ensureRuntimeException(Throwable e, String s)
        {
        if (e instanceof RuntimeException && s == null)
            {
            return (RuntimeException) e;
            }
        else
            {
            return new WrapperException(e, s);
            }
        }

    /**
     * Unwind the wrapper (runtime) exception to extract the original
     *
     * @apiNote We can't use the {@link Exceptions#getOriginalException(RuntimeException)}
     *          because we want to return a {@link WrapperException} for Coherence.
     *
     * @param e  Runtime exception (wrapper)
     *
     * @return an original wrapped exception
     */
    public static Throwable getOriginalException(RuntimeException e)
        {
        Throwable t = e;

        while (true)
            {
            if (t instanceof WrapperException)
                {
                t = ((WrapperException) t).getOriginalException();
                }
            else if (t instanceof RemoteException)
                {
                t = ((RemoteException) t).detail;
                }
            // we do not want to have runtime dependency on j2ee classes
            else if (t.getClass().getName().equals("javax.ejb.EJBException"))
                {
                try
                    {
                    t = (Throwable) ClassHelper.invoke(t,
                            "getCausedByException", ClassHelper.VOID);
                    }
                catch (Exception x)
                    {
                    return t;
                    }
                }
            else
                {
                return t;
                }
            }
        }


    // ----- printed output support -----------------------------------------

    /**
     * Prints a blank line.
     */
    public static void out()
        {
        Loggers.out();
        }

    /**
     * Prints the passed Object.
     *
     * @param o  the Object to print.
     */
    public static void out(Object o)
        {
        Loggers.out(o);
        }

    /**
     * Prints the passed String value.
     *
     * @param s  the String to print.
     */
    public static void out(String s)
        {
        Loggers.out(s);
        }

    /**
     * Prints the passed class information.
     *
     * @param clz  the class object to print.
     */
    public static void out(Class<?> clz)
        {
        Loggers.out(clz);
        }

    /**
     * Prints the passed exception information.
     *
     * @param e  the Throwable object to print
     */
    public static void out(Throwable e)
        {
        Loggers.out(e);
        }

    /**
     * Prints a blank line to the trace Writer.
     */
    public static void err()
        {
        Loggers.err();
        }

    /**
     * Prints the passed Object to the trace Writer.
     *
     * @param o  the Object to print
     */
    public static void err(Object o)
        {
        Loggers.err(o);
        }

    /**
     * Prints the passed String value to the trace Writer.
     *
     * @param s  the String to print
     */
    public static void err(String s)
        {
        Loggers.err(s);
        }

    /**
     * Prints the passed class information to the trace Writer.
     *
     * @param clz  the class object to print
     */
    public static void err(Class<?> clz)
        {
        Loggers.err(clz);
        }

    /**
     * Prints the passed exception information to the trace Writer.
     *
     * @param e  the Throwable object to print
     */
    public static void err(Throwable e)
        {
        Loggers.err(e);
        }

    /**
     * Prints a blank line to the log.
     */
    public static void log()
        {
        Loggers.log();
        }

    /**
     * Prints the passed Object to the log.
     *
     * @param o  the Object to print
     */
    public static void log(Object o)
        {
        Loggers.log(o);
        }

    /**
     * Prints the passed String value to the log.
     *
     * @param s  the String to print
     */
    public static void log(String s)
        {
        Loggers.log(s);
        }

    /**
     * Prints the passed class information to the log.
     *
     * @param clz  the class object to print
     */
    public static void log(Class<?> clz)
        {
        Loggers.log(clz);
        }

    /**
     * Prints the passed exception information to the log.
     *
     * @param e  the Throwable object to print
     */
    public static void log(Throwable e)
        {
        Loggers.log(e);
        }


    // ----- class loader support --------------------------------------------

    /**
     * Obtain a non-null ClassLoader.
     *
     * @param loader  a ClassLoader (may be null)
     *
     * @return the passed ClassLoader (if not null), or the ContextClassLoader
     */
    public static ClassLoader ensureClassLoader(ClassLoader loader)
        {
        return Classes.ensureClassLoader(loader);
        }

    /**
     * Try to determine the ClassLoader that supports the current context.
     *
     * @return a ClassLoader to use for the current context
     */
    public static ClassLoader getContextClassLoader()
        {
        return Classes.getContextClassLoader();
        }

    /**
     * Try to determine the ClassLoader that supports the current context.
     *
     * @param o  the calling object, or any object out of the application
     *           that is requesting the class loader
     *
     * @return a ClassLoader to use for the current context
     */
    public static ClassLoader getContextClassLoader(Object o)
        {
        return Classes.getContextClassLoader(o);
        }


    // ----- stack trace support --------------------------------------------

    /**
     * Get the StackFrame information for the caller of the current method.
     *
     * @return the StackFrame information for the caller of the current method
     */
    public static StackTrace.StackFrame getCallerStackFrame()
        {
        return StackTrace.getCallerStackFrame();
        }

    /**
     * Get the StackFrame information for the current method.
     *
     * @return the StackFrame information for the current method
     */
    public static StackTrace.StackFrame getStackFrame()
        {
        return StackTrace.getStackFrame();
        }

    /**
     * Iterate the StackFrame information for all callers, going from the
     * inside outwards, and starting with the caller of this method.
     *
     * @return an Iterator of StackFrames
     */
    public static StackTrace.StackFrame[] getStackFrames()
        {
        return StackTrace.getStackFrames();
        }

    /**
     * Build a stack trace for the current thread.
     *
     * @return a String containing a printable stack trace
     */
    public static String getStackTrace()
        {
        return StackTrace.getStackTrace();
        }

    /**
     * Build a stack trace for the passed exception that does not include
     * the exception description itself.
     *
     * @param e  a Throwable object that contains stack trace information
     *
     * @return a String containing a printable stack trace
     */
    public static String getStackTrace(Throwable e)
        {
        return StackTrace.getStackTrace(e);
        }

    /**
     * Extract a throwable's message, and all causal messages.
     *
     * @param t       the throwable
     * @param sDelim  the delimiter to include between messages
     *
     * @return the concatenated messages
     */
    public static String getDeepMessage(Throwable t, String sDelim)
        {
        return StackTrace.getDeepMessage(t, sDelim);
        }

    /**
     * Build a stack trace for the passed exception.
     *
     * @param e  a Throwable object that contains stack trace information
     *
     * @return a String containing a printable stack trace
     */
    public static String printStackTrace(Throwable e)
        {
        return StackTrace.printStackTrace(e);
        }

    /**
     * Ensure the specified Number is a BigDecimal value or convert it into a
     * new BigDecimal object.
     *
     * @param num  a Number object
     *
     * @return a BigDecimal object that is equal to the passed in Number
     */
    public static BigDecimal ensureBigDecimal(Number num)
        {
        return StackTrace.ensureBigDecimal(num);
        }

    // ----- thread factory support -----------------------------------------

    /**
     * Create a Thread with the specified group, runnable, and name, using
     * the configured ThreadFactory, as specified by the
     * <i>tangosol.coherence.threadfactory</i> system property.
     *
     * @param group     (optional) the thread's thread group
     * @param runnable  (optional) the thread's runnable
     * @param sName     (optional) the thread's name
     *
     * @return a new thread using the specified parameters
     */
    public static Thread makeThread(ThreadGroup group, Runnable runnable,
                                    String sName)
        {
        ThreadFactory factory = s_threadFactory;
        if (factory == null)
            {
            return sName == null ? new Thread(group, runnable)
                                 : new Thread(group, runnable, sName);
            }
        else
            {
            return factory.makeThread(group, runnable, sName);
            }
        }

    /**
     * Return the configured thread factory.
     *
     * @return the configured thread factory, or null if none has been configured
     */
    public static ThreadFactory getThreadFactory()
        {
        return s_threadFactory;
        }

    /**
     * Instantiate the configured thread factory.
     *
     * @return the configured thread factory, or null if none has been configured
     */
    private static ThreadFactory instantiateThreadFactory()
        {
        String sFactory = AccessController.doPrivileged(
            (PrivilegedAction<String>) () -> Config.getProperty("coherence.threadfactory"));

        if (sFactory == null)
            {
            return null;
            }

        try
            {
            return (ThreadFactory) Class.forName(sFactory).newInstance();
            }
        catch (Exception e)
            {
            throw ensureRuntimeException(e);
            }
        }


    // ----- thread sleep/wait support --------------------------------------

    /**
     * Convenience method for {@link java.lang.Thread#sleep(long)}.
     * <p>
     * If the thread is interrupted before the sleep time expires, a
     * RuntimeException is thrown and the thread's interrupt state is set.
     *
     * @param cMillis  the maximum time to wait in milliseconds
     */
    public static void sleep(long cMillis)
        {
        try
            {
            while (cMillis > 0)
                {
                long lStart = getSafeTimeMillis();
                Blocking.sleep(cMillis);
                // safe time ensures time only moves forward
                cMillis -= (getSafeTimeMillis() - lStart);
                }
            }
        catch (InterruptedException e)
            {
            Thread.currentThread().interrupt();
            throw ensureRuntimeException(e);
            }
        }

    /**
     * Convenience method for {@link java.lang.Object#wait(long)}.
     * <p>
     * If the thread is interrupted before being notified or the wait time
     * expires, a RuntimeException is thrown and the thread's interrupt state is
     * set.
     *
     * @param o        the Object to wait for
     * @param cMillis  the maximum time to wait in milliseconds
     */
    public static void wait(Object o, long cMillis)
        {
        try
            {
            Blocking.wait(o, cMillis);
            }
        catch (InterruptedException e)
            {
            Thread.currentThread().interrupt();
            throw ensureRuntimeException(e);
            }
        }


    // ----- common monitor support -----------------------------------------

    /**
     * Return the common monitor associated with the specified integer value.
     *
     * @param i the common monitor identifier
     *
     * @return the associated monitor
     *
     * @see CommonMonitor
     */
    public static CommonMonitor getCommonMonitor(int i)
        {
        return CommonMonitor.getCommonMonitor(i);
        }

    /**
     * Return the common monitor associated with the specified long value.
     *
     * @param l the common monitor identifier
     *
     * @return the associated monitor
     *
     * @see CommonMonitor
     */
    public static CommonMonitor getCommonMonitor(long l)
        {
        return CommonMonitor.getCommonMonitor(l);
        }

    /**
     * Return the common monitor associated with the specified object based on
     * its identity hashCode.
     *
     * @param o  the object to obtain a common monitor for
     *
     * @return the associated monitor
     *
     * @see CommonMonitor
     */
    public static CommonMonitor getCommonMonitor(Object o)
        {
        return CommonMonitor.getCommonMonitor(o);
        }


    // ----- class formatting support ---------------------------------------

    /**
     * Formats Class information for debug output purposes.
     *
     * @param clz  the Class to print information for
     *
     * @return a String describing the Class in detail
     */
    public static String toString(Class<?> clz)
        {
        return Classes.toString(clz);
        }

    // ----- formatting support: decimal values -----------------------------

    /**
     * Returns true if the passed character is a decimal digit.
     *
     * @param ch The character to check
     *
     * @return {@code true} if the passed character is a decimal digit
     */
    public static boolean isDecimal(char ch)
        {
        return Formatting.isDecimal(ch);
        }

    /**
     * Returns the integer value of a decimal digit.
     *
     * @param ch The character to convert
     *
     * @return the integer value of a decimal digit
     */
    public static int decimalValue(char ch)
        {
        return Formatting.decimalValue(ch);
        }

    /**
     * Calculate the number of decimal digits needed to display the passed
     * value.
     *
     * @param n  the value
     *
     * @return the number of decimal digits needed to display the value
     */
    public static int getMaxDecDigits(int n)
        {
        return Formatting.getMaxDecDigits(n);
        }

    /**
     * Format the passed non-negative integer as a fixed-length decimal string.
     *
     * @param n        the value
     * @param cDigits  the length of the resulting decimal string
     *
     * @return the decimal value formatted to the specified length string
     */
    public static String toDecString(int n, int cDigits)
        {
        return Formatting.toDecString(n, cDigits);
        }

    /**
     * Return the smallest value that is not less than the first argument and
     * is a multiple of the second argument. Effectively rounds the first
     * argument up to a multiple of the second.
     *
     * @param lMin       the smallest value to return
     * @param lMultiple  the return value will be a multiple of this argument
     *
     * @return the smallest multiple of the second argument that is not less
     *         than the first
     */
    public static long pad(long lMin, long lMultiple)
        {
        return Formatting.pad(lMin, lMultiple);
        }


    // ----- formatting support: octal values -------------------------------

    /**
     * Returns true if the passed character is an octal digit.
     *
     * @param ch The character to check
     *
     * @return {@code true} if the passed character is an octal digit
     */
    public static boolean isOctal(char ch)
        {
        return Formatting.isOctal(ch);
        }

    /**
     * Returns the integer value of an octal digit.
     *
     * @param ch The character to convert
     *
     * @return the integer value of an octal digit
     */
    public static int octalValue(char ch)
        {
        return Formatting.octalValue(ch);
        }


    // ----- formatting support: hex values ---------------------------------

    /**
     * Returns true if the passed character is a hexadecimal digit.
     *
     * @param ch The character to check
     *
     * @return {@code true} if the passed character is a hexadecimal digit
     */
    public static boolean isHex(char ch)
        {
        return Formatting.isHex(ch);
        }

    /**
     * Returns the integer value of a hexadecimal digit.
     *
     * @param ch The character to convert
     *
     * @return the integer value
     */
    public static int hexValue(char ch)
        {
        return Formatting.hexValue(ch);
        }

    /**
     * Calculate the number of hex digits needed to display the passed value.
     *
     * @param n  the value
     *
     * @return the number of hex digits needed to display the value
     */
    public static int getMaxHexDigits(int n)
        {
        return Formatting.getMaxHexDigits(n);
        }

    /**
     * Format the passed integer as a fixed-length hex string.
     *
     * @param n        the value
     * @param cDigits  the length of the resulting hex string
     *
     * @return the hex value formatted to the specified length string
     */
    public static String toHexString(int n, int cDigits)
        {
        return Formatting.toHexString(n, cDigits);
        }

    /**
     * Convert a byte to the hex sequence of 2 hex digits.
     *
     * @param   b  the byte
     *
     * @return  the hex sequence
     */
    public static String toHex(int b)
        {
        return Formatting.toHex(b);
        }

    /**
     * Convert a byte array to the hex sequence of 2 hex digits per byte.
     *
     * This is a replacement for Text.toString(char[]).
     *
     * @param   ab  the byte array
     *
     * @return  the hex sequence
     */
    public static String toHex(byte[] ab)
        {
        return Formatting.toHex(ab);
        }

    /**
     * Convert a byte to a hex sequence of '0' + 'x' + 2 hex digits.
     *
     * @param   b  the byte
     *
     * @return  the hex sequence
     */
    public static String toHexEscape(byte b)
        {
        return Formatting.toHexEscape(b);
        }

    /**
     * Convert a byte array to a hex sequence of '0' + 'x' + 2 hex digits
     * per byte.
     *
     * @param   ab  the byte array
     *
     * @return  the hex sequence
     */
    public static String toHexEscape(byte[] ab)
        {
        return Formatting.toHexEscape(ab);
        }

    /**
     * Convert a byte array to a hex sequence of '0' + 'x' + 2 hex digits
     * per byte.
     *
     * @param   ab  the byte array
     * @param   of  the offset into array
     * @param   cb  the number of bytes to convert
     *
     * @return  the hex sequence
     */
    public static String toHexEscape(byte[] ab, int of, int cb)
        {
        return Formatting.toHexEscape(ab, of, cb);
        }

    /**
     * Convert a ByteSequence to a hex sequence of '0' + 'x' + 2 hex digits
     * per byte.
     *
     * @param   seq  the ByteSequence
     * @param   of   the offset into the byte sequence
     * @param   cb   the number of bytes to convert
     *
     * @return  the hex sequence
     *
     * @since Coherence 3.7
     */
    public static String toHexEscape(ByteSequence seq, int of, int cb)
        {
        return Formatting.toHexEscape(seq, of, cb);
        }

    /**
     * Convert a byte array to a hex dump.
     *
     * This is a replacement for Text.toString(byte[] ab, int cBytesPerLine).
     *
     * @param ab             the byte array to format as a hex string
     * @param cBytesPerLine  the number of bytes to display on a line
     *
     * @return a multi-line hex dump
     */
    public static String toHexDump(byte[] ab, int cBytesPerLine)
        {
        return Formatting.toHexDump(ab, cBytesPerLine);
        }

    /**
     * Parse the passed String of hexadecimal characters into a binary
     * value.  This implementation allows the passed String to be prefixed
     * with "0x".
     *
     * @param s  the hex String to evaluate
     *
     * @return  the byte array value of the passed hex String
     */
    public static byte[] parseHex(String s)
        {
        return Formatting.parseHex(s);
        }

    /**
     * Return the integer value of a hexadecimal digit.
     *
     * @param ch  the hex character to evaluate
     *
     * @return  the integer value of the passed hex character
     */
    public static int parseHex(char ch)
        {
        return Formatting.parseHex(ch);
        }


    // ----- formatting support: double -------------------------------------

    /**
     * Format a double value as a String.
     *
     * @param dfl         a double value
     * @param cMinDigits  the minimum number of digits of precision to display
     *
     * @return the double value formatted as a String
     */
    public static String toString(double dfl, int cMinDigits)
        {
        return Formatting.toString(dfl, cMinDigits);
        }


    // ----- formatting support: character/String ---------------------------

    /**
     * Format a Unicode character to the Unicode escape sequence of '\'
     * + 'u' + 4 hex digits.
     *
     * @param ch  the character
     *
     * @return  the Unicode escape sequence
     */
    public static String toUnicodeEscape(char ch)
        {
        return Formatting.toUnicodeEscape(ch);
        }

    /**
     * Format a char to a printable escape if necessary.
     *
     * @param ch  the char
     *
     * @return  a printable String representing the passed char
     */
    public static String toCharEscape(char ch)
        {
        return Formatting.toCharEscape(ch);
        }

    /**
     * Format a char to a printable escape if necessary as it would
     * appear (quoted) in Java source code.
     *
     * This is a replacement for Text.printableChar().
     *
     * @param   ch  the character
     *
     * @return  a printable String in single quotes  representing the
     *          passed char
     */
    public static String toQuotedCharEscape(char ch)
        {
        return Formatting.toQuotedCharEscape(ch);
        }

    /**
     * Format a String escaping characters as necessary.
     *
     * @param   s  the String
     *
     * @return  a printable String representing the passed String
     */
    public static String toStringEscape(String s)
        {
        return Formatting.toStringEscape(s);
        }

    /**
     * Format a String as it would appear (quoted) in Java source code,
     * escaping characters as necessary.
     *
     * This is a replacement for Text.printableString().
     *
     * @param   s  the String
     *
     * @return  a printable String in double quotes  representing the
     *          passed String
     */
    public static String toQuotedStringEscape(String s)
        {
        return Formatting.toQuotedStringEscape(s);
        }

    /**
     * Format a char to a printable escape if necessary, putting the result
     * into the passed array.  The array must be large enough to accept six
     * characters.
     *
     * @param ch   the character to format
     * @param ach  the array of characters to format into
     * @param of   the offset in the array to format at
     *
     * @return  the number of characters used to format the char
     */
    public static int escape(char ch, char[] ach, int of)
        {
        return Formatting.escape(ch, ach, of);
        }

    /**
     * Escapes the string for SQL.
     *
     * @param s  the String to escape
     *
     * @return the string quoted for SQL and escaped as necessary
     */
    public static String toSqlString(String s)
        {
        return Formatting.toSqlString(s);
        }

    /**
     * Indent the passed multi-line string.
     *
     * @param sText   the string to indent
     * @param sIndent a string used to indent each line
     *
     * @return the string, indented
     */
    public static String indentString(String sText, String sIndent)
        {
        return Formatting.indentString(sText, sIndent);
        }

    /**
     * Textually indent the passed multi-line string.
     *
     * @param sText       the string to indent
     * @param sIndent     a string used to indent each line
     * @param fFirstLine  true indents all lines;
     *                    false indents all but the first
     *
     * @return the string, indented
     */
    public static String indentString(String sText, String sIndent, boolean fFirstLine)
        {
        return Formatting.indentString(sText, sIndent, fFirstLine);
        }

    /**
     * Breaks the specified string into a multi-line string.
     *
     * @param sText   the string to break
     * @param nWidth  the max width of resulting lines (including the indent)
     * @param sIndent a string used to indent each line
     *
     * @return the string, broken and indented
     */
    public static String breakLines(String sText, int nWidth, String sIndent)
        {
        return Formatting.breakLines(sText, nWidth, sIndent);
        }

    /**
     * Breaks the specified string into a multi-line string.
     *
     * @param sText       the string to break
     * @param nWidth      the max width of resulting lines (including the
     *                    indent)
     * @param sIndent     a string used to indent each line
     * @param fFirstLine  if true indents all lines;
     *                    otherwise indents all but the first
     *
     * @return the string, broken and indented
     */
    public static String breakLines(String sText, int nWidth, String sIndent, boolean fFirstLine)
        {
        return Formatting.breakLines(sText, nWidth, sIndent, fFirstLine);
        }

    /**
     * Create a String of the specified length containing the specified
     * character.
     *
     * @param ch   the character to fill the String with
     * @param cch  the length of the String
     *
     * @return a String containing the character &lt;ch&gt; repeated &lt;cch&gt; times
     */
    public static String dup(char ch, int cch)
        {
        return Formatting.dup(ch, cch);
        }

    /**
     * Create a String which is a duplicate of the specified number of the
     * passed String.
     *
     * @param s  the String to fill the new String with
     * @param c  the number of duplicates to put into the new String
     *
     * @return a String containing the String <tt>s</tt> repeated <tt>c</tt>
     *         times
     */
    public static String dup(String s, int c)
        {
        return Formatting.dup(s, c);
        }

    /**
     * Replace all occurrences of the specified substring in the specified
     * string.
     *
     * @param sText  string to change
     * @param sFrom  pattern to change from
     * @param sTo    pattern to change to
     *
     * @return   modified string
     */
    public static String replace(String sText, String sFrom, String sTo)
        {
        return Formatting.replace(sText, sFrom, sTo);
        }

    /**
     * Parse a character-delimited String into an array of Strings.
     *
     * @param s        character-delimited String to parse
     * @param chDelim  character delimiter
     *
     * @return an array of String objects parsed from the passed String
     */
    public static String[] parseDelimitedString(String s, char chDelim)
        {
        return Formatting.parseDelimitedString(s, chDelim);
        }

    /**
     * Format the content of the passed integer array as a delimited string.
     *
     * @param an      the array
     * @param sDelim  the delimiter
     *
     * @return the formatted string
     */
    public static String toDelimitedString(int[] an, String sDelim)
        {
        return Formatting.toDelimitedString(an, sDelim);
        }

    /**
     * Format the content of the passed long array as a delimited string.
     *
     * @param al      the array
     * @param sDelim  the delimiter
     *
     * @return the formatted string
     */
    public static String toDelimitedString(long[] al, String sDelim)
        {
        return Formatting.toDelimitedString(al, sDelim);
        }

    /**
     * Format the content of the passed Object array as a delimited string.
     *
     * @param ao      the array
     * @param sDelim  the delimiter
     *
     * @return the formatted string
     */
    public static String toDelimitedString(Object[] ao, String sDelim)
        {
        return Formatting.toDelimitedString(ao, sDelim);
        }

    /**
     * Format the content of the passed Iterator as a delimited string.
     *
     * @param iter    the Iterator
     * @param sDelim  the delimiter
     *
     * @return the formatted string
     */
    public static String toDelimitedString(Iterator<?> iter, String sDelim)
        {
        return Formatting.toDelimitedString(iter, sDelim);
        }

    /**
     * Capitalize a string.
     *
     * @param  s  the string to capitalize
     *
     * @return the capitalized string
     */
    public static String capitalize(String s)
        {
        return Formatting.capitalize(s);
        }

    /**
     * Truncate a string to the specified character count.
     *
     * @param s       the String to be truncated
     * @param cLimit  expected character count
     *
     * @return the truncated String
     */
    public static String truncateString(String s, int cLimit)
        {
        return Formatting.truncateString(s, cLimit);
        }

    /**
     * Provide a string representation of elements within the collection until
     * the concatenated string length exceeds {@code cLimit}.
     *
     * @param coll    the collection of elements to describe
     * @param cLimit  expected character count
     *
     * @return the truncated string representation of the provided collection
     */
    public static String truncateString(Collection<?> coll, int cLimit)
        {
        return Formatting.truncateString(coll, cLimit);
        }


    // ----- formatting support: bandwidth ----------------------------------

    /**
     * Parse the given string representation of a number of bytes per second.
     * The supplied string must be in the format:
     * <p>
     * <tt>[\d]+[[.][\d]+]?[K|k|M|m|G|g|T|t]?[[B|b][P|p][S|s]]?</tt>
     * <p>
     * where the first non-digit (from left to right) indicates the factor
     * with which the preceding decimal value should be multiplied:
     * <ul>
     * <li><tt>K</tt> or <tt>k</tt> (kilo, 2<sup>10</sup>)</li>
     * <li><tt>M</tt> or <tt>m</tt> (mega, 2<sup>20</sup>)</li>
     * <li><tt>G</tt> or <tt>g</tt> (giga, 2<sup>30</sup>)</li>
     * <li><tt>T</tt> or <tt>t</tt> (tera, 2<sup>40</sup>)</li>
     * </ul>
     * <p>
     * If the string value does not contain a factor, a factor of one is
     * assumed.
     * <p>
     * The optional last three characters indicate the unit of measure,
     * <tt>[b][P|p][S|s]</tt> in the case of bits per second and
     * <tt>[B][P|p][S|s]</tt> in the case of bytes per second. If the string
     * value does not contain a unit, a unit of bits per second is assumed.
     *
     * @param s  a string with the format:
     *           <tt>[\d]+[[.][\d]+]?[K|k|M|m|G|g|T|t]?[[B|b][P|p][S|s]]?</tt>
     *
     * @return the number of bytes per second represented by the given string
     */
    public static long parseBandwidth(String s)
        {
        return Formatting.parseBandwidth(s);
        }

    /**
     * Parse the given string representation of a number of bytes per second.
     * The supplied string must be in the format:
     * <p>
     * <tt>[\d]+[[.][\d]+]?[K|k|M|m|G|g|T|t]?[[B|b][P|p][S|s]]?</tt>
     * <p>
     * where the first non-digit (from left to right) indicates the factor
     * with which the preceding decimal value should be multiplied:
     * <ul>
     * <li><tt>K</tt> or <tt>k</tt> (kilo, 2<sup>10</sup>)</li>
     * <li><tt>M</tt> or <tt>m</tt> (mega, 2<sup>20</sup>)</li>
     * <li><tt>G</tt> or <tt>g</tt> (giga, 2<sup>30</sup>)</li>
     * <li><tt>T</tt> or <tt>t</tt> (tera, 2<sup>40</sup>)</li>
     * </ul>
     * <p>
     * If the string value does not contain an explicit or implicit factor, a
     * factor calculated by raising 2 to the given default power is used. The
     * default power can be one of:
     * <ul>
     * <li>{@link #POWER_0}</li>
     * <li>{@link #POWER_K}</li>
     * <li>{@link #POWER_M}</li>
     * <li>{@link #POWER_G}</li>
     * <li>{@link #POWER_T}</li>
     * </ul>
     * <p>
     * The optional last three characters indicate the unit of measure,
     * <tt>[b][P|p][S|s]</tt> in the case of bits per second and
     * <tt>[B][P|p][S|s]</tt> in the case of bytes per second. If the string
     * value does not contain a unit, a unit of bits per second is assumed.
     *
     * @param s              a string with the format:
     *                       <tt>[\d]+[[.][\d]+]?[K|k|M|m|G|g|T|t]?[[B|b][P|p][S|s]]?</tt>
     * @param nDefaultPower  the exponent used to calculate the factor used in
     *                       the conversion if one is not implied by the given
     *                       string
     *
     * @return the number of bytes per second represented by the given string
     */
    public static long parseBandwidth(String s, int nDefaultPower)
        {
        return Formatting.parseBandwidth(s, nDefaultPower);
        }

    /**
     * Format the passed bandwidth (in bytes per second) as a String that can
     * be parsed by {@link #parseBandwidth} such that
     * <tt>cb==parseBandwidth(toBandwidthString(cb))</tt> holds true for
     * all legal values of <tt>cbps</tt>.
     *
     * @param cbps  the number of bytes per second
     *
     * @return a String representation of the given bandwidth
     */
    public static String toBandwidthString(long cbps)
        {
        return Formatting.toBandwidthString(cbps);
        }

    /**
     * Format the passed bandwidth (in bytes per second) as a String. This
     * method will possibly round the memory size for purposes of producing a
     * more-easily read String value unless the <tt>fExact</tt> parameters is
     * passed as true; if <tt>fExact</tt> is true, then
     * <tt>cb==parseBandwidth(toBandwidthString(cb, true))</tt> holds true
     * for all legal values of <tt>cbps</tt>.
     *
     * @param cbps    the number of bytes per second
     * @param fExact  true if the String representation must be exact, or
     *                false if it can be an approximation
     *
     * @return a String representation of the given bandwidth
     */
    public static String toBandwidthString(long cbps, boolean fExact)
        {
        return Formatting.toBandwidthString(cbps, fExact);
        }


    // ----- formatting support: memory size --------------------------------

    /**
     * Parse the given string representation of a number of bytes. The
     * supplied string must be in the format:
     * <p>
     * <tt>[\d]+[[.][\d]+]?[K|k|M|m|G|g|T|t]?[B|b]?</tt>
     * <p>
     * where the first non-digit (from left to right) indicates the factor
     * with which the preceding decimal value should be multiplied:
     * <ul>
     * <li><tt>K</tt> or <tt>k</tt> (kilo, 2<sup>10</sup>)</li>
     * <li><tt>M</tt> or <tt>m</tt> (mega, 2<sup>20</sup>)</li>
     * <li><tt>G</tt> or <tt>g</tt> (giga, 2<sup>30</sup>)</li>
     * <li><tt>T</tt> or <tt>t</tt> (tera, 2<sup>40</sup>)</li>
     * </ul>
     * <p>
     * If the string value does not contain a factor, a factor of one is
     * assumed.
     * <p>
     * The optional last character <tt>B</tt> or <tt>b</tt> indicates a unit
     * of bytes.
     *
     * @param s  a string with the format
     *           <tt>[\d]+[[.][\d]+]?[K|k|M|m|G|g|T|t]?[B|b]?</tt>
     *
     * @return the number of bytes represented by the given string
     */
    public static long parseMemorySize(String s)
        {
        return Formatting.parseMemorySize(s);
        }

    /**
     * Parse the given string representation of a number of bytes. The
     * supplied string must be in the format:
     * <p>
     * <tt>[\d]+[[.][\d]+]?[K|k|M|m|G|g|T|t]?[B|b]?</tt>
     * <p>
     * where the first non-digit (from left to right) indicates the factor
     * with which the preceding decimal value should be multiplied:
     * <ul>
     * <li><tt>K</tt> or <tt>k</tt> (kilo, 2<sup>10</sup>)</li>
     * <li><tt>M</tt> or <tt>m</tt> (mega, 2<sup>20</sup>)</li>
     * <li><tt>G</tt> or <tt>g</tt> (giga, 2<sup>30</sup>)</li>
     * <li><tt>T</tt> or <tt>t</tt> (tera, 2<sup>40</sup>)</li>
     * </ul>
     * <p>
     * If the string value does not contain an explicit or implicit factor, a
     * factor calculated by raising 2 to the given default power is used. The
     * default power can be one of:
     * <ul>
     * <li>{@link #POWER_0}</li>
     * <li>{@link #POWER_K}</li>
     * <li>{@link #POWER_M}</li>
     * <li>{@link #POWER_G}</li>
     * <li>{@link #POWER_T}</li>
     * </ul>
     * <p>
     * The optional last character <tt>B</tt> or <tt>b</tt> indicates a unit
     * of bytes.
     *
     * @param s              a string with the format
     *                       <tt>[\d]+[[.][\d]+]?[K|k|M|m|G|g|T|t]?[B|b]?</tt>
     * @param nDefaultPower  the exponent used to calculate the factor used in
     *                       the conversion if one is not implied by the given
     *                       string
     *
     * @return the number of bytes represented by the given string
     */
    public static long parseMemorySize(String s, int nDefaultPower)
        {
        return Formatting.parseMemorySize(s, nDefaultPower);
        }

    /**
     * Format the passed memory size (in bytes) as a String that can be
     * parsed by {@link #parseMemorySize} such that
     * <tt>cb==parseMemorySize(toMemorySizeString(cb))</tt> holds true for
     * all legal values of <tt>cb</tt>.
     *
     * @param cb  the number of bytes of memory
     *
     * @return a String representation of the given memory size
     */
    public static String toMemorySizeString(long cb)
        {
        return Formatting.toMemorySizeString(cb);
        }

    /**
     * Format the passed memory size (in bytes) as a String. This method will
     * possibly round the memory size for purposes of producing a more-easily
     * read String value unless the <tt>fExact</tt> parameters is passed as
     * true; if <tt>fExact</tt> is true, then
     * <tt>cb==parseMemorySize(toMemorySizeString(cb, true))</tt> holds true
     * for all legal values of <tt>cb</tt>.
     *
     * @param cb      the number of bytes of memory
     * @param fExact  true if the String representation must be exact, or
     *                false if it can be an approximation
     *
     * @return a String representation of the given memory size
     */
    public static String toMemorySizeString(long cb, boolean fExact)
        {
        return Formatting.toMemorySizeString(cb, fExact);
        }


    // ----- formatting support: time ---------------------------------------

    /**
     * Parse the given string representation of a time duration and return its
     * value as a number of milliseconds. The supplied string must be in the
     * format:
     * <p>
     * <tt>[\d]+[[.][\d]+]?[NS|ns|US|us|MS|ms|S|s|M|m|H|h|D|d]?</tt>
     * <p>
     * where the first non-digits (from left to right) indicate the unit of
     * time duration:
     * <ul>
     * <li><tt>NS</tt> or <tt>ns</tt> (nanoseconds)</li>
     * <li><tt>US</tt> or <tt>us</tt> (microseconds)</li>
     * <li><tt>MS</tt> or <tt>ms</tt> (milliseconds)</li>
     * <li><tt>S</tt>  or <tt>s</tt>  (seconds)</li>
     * <li><tt>M</tt>  or <tt>m</tt>  (minutes)</li>
     * <li><tt>H</tt>  or <tt>h</tt>  (hours)</li>
     * <li><tt>D</tt>  or <tt>d</tt>  (days)</li>
     * </ul>
     * <p>
     * If the string value does not contain a unit, a unit of milliseconds is
     * assumed.
     *
     * @param s  a string with the format
     *           <tt>[\d]+[[.][\d]+]?[NS|ns|US|us|MS|ms|S|s|M|m|H|h|D|d]?</tt>
     *
     * @return the number of milliseconds represented by the given string
     *         rounded down to the nearest millisecond
     *
     * @see #parseTimeNanos(String)
     */
    public static long parseTime(String s)
        {
        return Formatting.parseTime(s);
        }

    /**
     * Parse the given string representation of a time duration and return its
     * value as a number of milliseconds. The supplied string must be in the
     * format:
     * <p>
     * <tt>[\d]+[[.][\d]+]?[NS|ns|US|us|MS|ms|S|s|M|m|H|h|D|d]?</tt>
     * <p>
     * where the first non-digits (from left to right) indicate the unit of
     * time duration:
     * <ul>
     * <li><tt>NS</tt> or <tt>ns</tt> (nanoseconds)</li>
     * <li><tt>US</tt> or <tt>us</tt> (microseconds)</li>
     * <li><tt>MS</tt> or <tt>ms</tt> (milliseconds)</li>
     * <li><tt>S</tt>  or <tt>s</tt>  (seconds)</li>
     * <li><tt>M</tt>  or <tt>m</tt>  (minutes)</li>
     * <li><tt>H</tt>  or <tt>h</tt>  (hours)</li>
     * <li><tt>D</tt>  or <tt>d</tt>  (days)</li>
     * </ul>
     * <p>
     * If the string value does not contain a unit, the specified default unit
     * is assumed. The default unit can be one of:
     * <ul>
     * <li>{@link #UNIT_NS}</li>
     * <li>{@link #UNIT_US}</li>
     * <li>{@link #UNIT_MS}</li>
     * <li>{@link #UNIT_S}</li>
     * <li>{@link #UNIT_M}</li>
     * <li>{@link #UNIT_H}</li>
     * <li>{@link #UNIT_D}</li>
     * </ul>
     *
     * @param s             a string with the format
     *                      <tt>[\d]+[[.][\d]+]?[NS|ns|US|us|MS|ms|S|s|M|m|H|h|D|d]?</tt>
     * @param nDefaultUnit  the unit to use in the conversion to milliseconds
     *                      if one is not specified in the supplied string
     *
     * @return the number of milliseconds represented by the given string
     *         rounded down to the nearest millisecond
     *
     * @see #parseTimeNanos(String, int)
     */
    public static long parseTime(String s, int nDefaultUnit)
        {
        return Formatting.parseTime(s, nDefaultUnit);
        }

    /**
     * Parse the given string representation of a time duration and return its
     * value as a number of nanoseconds. The supplied string must be in the
     * format:
     * <p>
     * <tt>[\d]+[[.][\d]+]?[NS|ns|US|us|MS|ms|S|s|M|m|H|h|D|d]?</tt>
     * <p>
     * where the first non-digits (from left to right) indicate the unit of
     * time duration:
     * <ul>
     * <li><tt>NS</tt> or <tt>ns</tt> (nanoseconds)</li>
     * <li><tt>US</tt> or <tt>us</tt> (microseconds)</li>
     * <li><tt>MS</tt> or <tt>ms</tt> (milliseconds)</li>
     * <li><tt>S</tt>  or <tt>s</tt>  (seconds)</li>
     * <li><tt>M</tt>  or <tt>m</tt>  (minutes)</li>
     * <li><tt>H</tt>  or <tt>h</tt>  (hours)</li>
     * <li><tt>D</tt>  or <tt>d</tt>  (days)</li>
     * </ul>
     * <p>
     * If the string value does not contain a unit, a unit of nanoseconds is
     * assumed.
     *
     * @param s  a string with the format
     *           <tt>[\d]+[[.][\d]+]?[NS|ns|US|us|MS|ms|S|s|M|m|H|h|D|d]?</tt>
     *
     * @return the number of nanoseconds represented by the given string
     *         rounded down to the nearest nanosecond
     */
    public static long parseTimeNanos(String s)
        {
        return Formatting.parseTimeNanos(s);
        }

    /**
     * Parse the given string representation of a time duration and return its
     * value as a number of nanoseconds. The supplied string must be in the
     * format:
     * <p>
     * <tt>[\d]+[[.][\d]+]?[NS|ns|US|us|MS|ms|S|s|M|m|H|h|D|d]?</tt>
     * <p>
     * where the first non-digits (from left to right) indicate the unit of
     * time duration:
     * <ul>
     * <li><tt>NS</tt> or <tt>ns</tt> (nanoseconds)</li>
     * <li><tt>US</tt> or <tt>us</tt> (microseconds)</li>
     * <li><tt>MS</tt> or <tt>ms</tt> (milliseconds)</li>
     * <li><tt>S</tt>  or <tt>s</tt>  (seconds)</li>
     * <li><tt>M</tt>  or <tt>m</tt>  (minutes)</li>
     * <li><tt>H</tt>  or <tt>h</tt>  (hours)</li>
     * <li><tt>D</tt>  or <tt>d</tt>  (days)</li>
     * </ul>
     * <p>
     * If the string value does not contain a unit, the specified default unit
     * is assumed. The default unit can be one of:
     * <ul>
     * <li>{@link #UNIT_NS}</li>
     * <li>{@link #UNIT_US}</li>
     * <li>{@link #UNIT_MS}</li>
     * <li>{@link #UNIT_S}</li>
     * <li>{@link #UNIT_M}</li>
     * <li>{@link #UNIT_H}</li>
     * <li>{@link #UNIT_D}</li>
     * </ul>
     *
     * @param s             a string with the format
     *                      <tt>[\d]+[[.][\d]+]?[NS|ns|US|us|MS|ms|S|s|M|m|H|h|D|d]?</tt>
     * @param nDefaultUnit  the unit to use in the conversion to nanoseconds
     *                      if one is not specified in the supplied string
     *
     * @return the number of nanoseconds represented by the given string
     *         rounded down to the nearest nanosecond
     */
    public static long parseTimeNanos(String s, int nDefaultUnit)
        {
        return Formatting.parseTimeNanos(s, nDefaultUnit);
        }

    /**
     * Format a long value into a human readable date/time string.
     *
     * @param ldt  a Java long containing a date/time value
     *
     * @return a human readable date/time string
     */
    public static String formatDateTime(long ldt)
        {
        return Formatting.formatDateTime(ldt);
        }


    // ----- formatting support: percentage ---------------------------------

    /**
     * Parse the given string representation of a percentage value and return
     * its value as a float in the inclusive range of 0.0 and 1.0. The supplied
     * string must be in the format:
     * <p>
     * <tt>[\d]+[[.][\d]+]?[%]</tt>
     * <p>
     * where the digits are within the closed interval [0.0, 100.0].
     *
     * @param s  a string with the format <tt>[\d]+[[.][\d]+]?[%]</tt>
     *
     * @return a float representing the percentage value in the closed interval
     *         [0.0, 1.0]
     */
    public static float parsePercentage(String s)
        {
        return Formatting.parsePercentage(s);
        }


    // ----- hashing --------------------------------------------------------

    /**
     * Return the hash code for the supplied object or 0 for null.
     *
     * @param o  the object to hash
     *
     * @return  the hash code for the supplied object
     */
    public static int hashCode(Object o)
        {
        return HashHelper.hashCode(o);
        }


    // ----- comparisons ----------------------------------------------------

    /**
     * Compare two references for equality.
     *
     * @param o1  an object
     * @param o2  an object to be compared with o1 for references equality
     *
     * @return  true if equal, false otherwise
     */
    public static boolean equals(Object o1, Object o2)
        {
        return Objects.equals(o1, o2);
        }

    /**
     * Deeply compare two references for equality. This dives down into
     * arrays, including nested arrays.
     *
     * @param o1  an object
     * @param o2  an object to be compared with o1 for deeply equality
     *
     * @return  true if deeply equal, false otherwise
     */
    public static boolean equalsDeep(Object o1, Object o2)
        {
        return Objects.equalsDeep(o1, o2);
        }


    // ----- CRC32 ----------------------------------------------------------

    /**
     * Calculate a CRC32 value from a byte array.
     *
     * @param ab  an array of bytes
     *
     * @return the 32-bit CRC value
     */
    public static int toCrc(byte[] ab)
        {
        return Formatting.toCrc(ab);
        }

    /**
     * Calculate a CRC32 value from a portion of a byte array.
     *
     * @param ab  an array of bytes
     * @param of  the offset into the array
     * @param cb  the number of bytes to evaluate
     *
     * @return the 32-bit CRC value
     */
    public static int toCrc(byte[] ab, int of, int cb)
        {
        return Formatting.toCrc(ab, of, cb, 0xFFFFFFFF);
        }

    /**
     * Continue to calculate a CRC32 value from a portion of a byte array.
     *
     * @param ab    an array of bytes
     * @param of    the offset into the array
     * @param cb    the number of bytes to evaluate
     * @param nCrc  the previous CRC value
     *
     * @return the 32-bit CRC value
     */
    public static int toCrc(byte[] ab, int of, int cb, int nCrc)
        {
        return Formatting.toCrc(ab, of, cb, nCrc);
        }

    /**
     * Calculate a CRC32 value from a ByteSequence.
     *
     * @param seq  a ByteSequence
     *
     * @return the 32-bit CRC value
     */
    public static int toCrc(ByteSequence seq)
        {
        return Formatting.toCrc(seq);
        }

    /**
     * Continue to calculate a CRC32 value from a portion of a ByteSequence .
     *
     * @param seq   a ByteSequence
     * @param of    the offset into the ByteSequence
     * @param cb    the number of bytes to evaluate
     * @param nCrc  the previous CRC value
     *
     * @return the 32-bit CRC value
     */
    public static int toCrc(ByteSequence seq, int of, int cb, int nCrc)
        {
        return Formatting.toCrc(seq, of, cb, nCrc);
        }

    // ----- time routines --------------------------------------------------

    /**
     * Return the number of milliseconds which have elapsed since the JVM was
     * started.
     *
     * @return the number of milliseconds which have elapsed since the JVM was
     *         started
     */
    public static long getUpTimeMillis()
        {
        return TimeHelper.getUpTimeMillis();
        }

    /**
     * Returns a "safe" current time in milliseconds.
     *
     * @return the difference, measured in milliseconds, between
     *          the corrected current time and midnight, January 1, 1970 UTC.
     *
     * @see SafeClock
     */
    public static long getSafeTimeMillis()
        {
        return TimeHelper.getSafeTimeMillis();
        }

    /**
     * Returns the last "safe" time as computed by a previous call to
     * the {@link #getSafeTimeMillis} method.
     *
     * @return the last "safe" time in milliseconds
     *
     * @see SafeClock
     */
    public static long getLastSafeTimeMillis()
        {
        return TimeHelper.getLastSafeTimeMillis();
        }

    /**
     * compute the number of milliseconds until the specified time.
     * <p>
     * Note: this method will only return zero if ldtTimeout == Long.MAX_VALUE.
     *
     * @param ldtTimeout  the timeout as computed by {@link #getSafeTimeMillis}
     *
     * @return the number of milliseconds to wait, or negative if the timeout
     *         has expired
     */
    public static long computeSafeWaitTime(long ldtTimeout)
        {
        return TimeHelper.computeSafeWaitTime(ldtTimeout);
        }

    /**
     * Gets the {@link TimeZone} for the given ID.
     * <p>
     * This method will cache returned TimeZones to avoid the contention
     * caused by the {@link TimeZone#getTimeZone(String)} implementation.
     *
     * @param sId  the ID for a {@link TimeZone}
     *
     * @return the specified {@link TimeZone}, or the GMT zone if the
     *         given ID cannot be understood.
     *
     * @see TimeZone#getTimeZone(String)
     *
     * @since Coherence 12.1.3
     */
    public static TimeZone getTimeZone(String sId)
        {
        return TimeHelper.getTimeZone(sId);
        }


    // ----- immutable intrinsic object caches ------------------------------

    /**
     * Factory method to produce Integer objects with an optimization that
     * uses cached Integer objects for all relatively-low numbers.
     *
     * @param n   an int
     *
     * @return an Integer whose value is the passed int
     *
     * @since Coherence 3.2
     * @deprecated use {@link Integer#valueOf(int)}
     */
    @Deprecated
    public static Integer makeInteger(int n)
        {
        return n;
        }

    /**
     * Factory method to produce Long objects with an optimization that
     * uses cached Long objects for all relatively-low numbers.
     *
     * @param n   a long
     *
     * @return a Long whose value is the passed long
     *
     * @since Coherence 3.2
     * @deprecated use {@link Long#valueOf(long)}
     */
    @Deprecated
    public static Long makeLong(long n)
        {
        return n;
        }


    // ----- random routines ------------------------------------------------

    /**
     * Return a random number assigned to this process.
     * <p>
     * This value will remain the same across invocations, but is generally different across JVMs.
     *
     * @return the process's random number.
     */
    public static int getProcessRandom()
        {
        return Randoms.getProcessRandom();
        }

    /**
    * Obtain a Random object that can be used to get random values.
    *
    * @return a random number generator
    *
    * @since Coherence 3.2
    */
    public static Random getRandom()
        {
        return Randoms.getRandom();
        }

    /**
     * Randomize the order of the elements within the passed collection.
     *
     * @param coll  the Collection to randomize; the passed Collection is not
     *              altered
     *
     * @return a new and immutable List whose contents are identical to those
     *         of the passed collection except for the order in which they appear
     *
     * @since Coherence 3.2
     */
    @SuppressWarnings("rawtypes")
    public static List randomize(Collection coll)
        {
        return Randoms.randomize(coll);
        }

    /**
     * Randomize the order of the elements within the passed array.
     *
     * @param a  an array of objects to randomize
     *
     * @return the array that was passed in, and with its contents unchanged
     *         except for the order in which they appear
     *
     * @since Coherence 3.2
     */
    public static Object[] randomize(Object[] a)
        {
        return Randoms.randomize(a);
        }

    /**
     * Randomize the order of the elements within the passed array.
     *
     * @param a  an array of <tt>int</tt> values to randomize
     *
     * @return the array that was passed in, and with its contents unchanged
     *         except for the order in which they appear
     *
     * @since Coherence 3.2
     */
    public static int[] randomize(int[] a)
        {
        return Randoms.randomize(a);
        }

    /**
     * Randomize the order of the elements within the passed array.
     *
     * @param a  an array of <tt>long</tt> values to randomize
     *
     * @return the array that was passed in, and with its contents unchanged
     *         except for the order in which they appear
     *
     * @since Coherence 12.2.1.4
     */
    public static long[] randomize(long[] a)
        {
        return Randoms.randomize(a);
        }

    /**
     * Generates a random-length Binary within the length bounds provided
     * whose contents are random bytes.
     *
     * @param cbMin  the minimum number of bytes in the resulting Binary
     * @param cbMax  the maximum number of bytes in the resulting Binary
     *
     * @return a randomly generated Binary object
     *
     * @since Coherence 3.2
     */
    public static Binary getRandomBinary(int cbMin, int cbMax)
        {
        return Randoms.getRandomBinary(cbMin, cbMax);
        }

    /**
     * Generates a random-length Binary including {@code abHead} at the head of
     * the Binary, in addition to random bytes within the length bounds provided.
     *
     * @param cbMin   the minimum number of bytes in the resulting Binary
     * @param cbMax   the maximum number of bytes in the resulting Binary
     * @param abHead  the head of the returned Binary
     *
     * @return a randomly generated Binary object with a length of {@code
     *         [len(abHead) + cbMin, cbMax]}
     *
     * @since Coherence 12.1.3
     */
    public static Binary getRandomBinary(int cbMin, int cbMax, byte...abHead)
        {
        return Randoms.getRandomBinary(cbMin, cbMax, abHead);
        }

    /**
     * Generates a random-length String within the length bounds provided.
     * If the ASCII option is indicated, the characters will be in the range
     * [32-127], otherwise the characters will be in the range
     * [0x0000-0xFFFF].
     *
     * @param cchMin   the minimum length of the resulting String
     * @param cchMax   the maximum length of the resulting String
     * @param fAscii   true if the resulting String should contain only ASCII
     *                 values
     *
     * @return a randomly generated String object
     *
     * @since Coherence 3.2
     */
    public static String getRandomString(int cchMin, int cchMax, boolean fAscii)
        {
        return Randoms.getRandomString(cchMin, cchMax, fAscii);
        }


    // ----- validation methods ---------------------------------------------

    /**
     * Check the range of a value.
     *
     * @param lValue  the value to check
     * @param lFrom   the lower limit of the range (inclusive)
     * @param lTo     the upper limit of the range (inclusive)
     * @param sName   the display name of the value
     *
     * @throws IllegalArgumentException if the value is out of range
     */
    public static void checkRange(long lValue, long lFrom, long lTo, String sName)
        {
        if (lValue < lFrom || lValue > lTo)
            {
            throw new IllegalArgumentException(
                sName + " value out of range [" + lFrom + ", " + lTo + "]: " + lValue);
            }
        }

    /**
     * Check that the specified object is non-null and return it.
     *
     * @param o      the object to check
     * @param sName  the name of the corresponding parameter
     * @param <T>    the type parameter for the object to check
     *
     * @return the specified object
     *
     * @throws IllegalArgumentException if o is null
     */
    public static <T> T checkNotNull(T o, String sName)
        {
        if (o == null)
            {
            throw new IllegalArgumentException(sName + " cannot be null");
            }

        return o;
        }

    /**
     * Check that the specified string is neither a null nor an empty string.
     *
     * @param s      the string to check
     * @param sName  the name of the corresponding parameter
     *
     * @throws IllegalArgumentException if s is null or empty string
     */
    public static void checkNotEmpty(String s, String sName)
        {
        checkNotNull(s, sName);
        if (s.length() == 0)
            {
            throw new IllegalArgumentException(sName + " cannot be empty");
            }
        }


    // ---- miscellaneous ---------------------------------------------------

    /**
     * Calculate a modulo of two integer numbers. For a positive dividend the
     * result is the same as the Java remainder operation (<code>n % m</code>).
     * For a negative dividend the result is still positive and equals to
     * (<code>n % m + m</code>).
     *
     * @param n  the dividend
     * @param m  the divisor (must be positive)
     *
     * @return the modulo
     */
    public static int mod(int n, int m)
        {
        return Hasher.mod(n, m);
        }

    /**
     * Calculate a modulo of two long numbers. For a positive dividend the
     * result is the same as the Java remainder operation (<code>n % m</code>).
     * For a negative dividend the result is still positive and equals to
     * (<code>n % m + m</code>).
     *
     * @param n  the dividend
     * @param m  the divisor (must be positive)
     *
     * @return the modulo
     */
    public static long mod(long n, long m)
        {
        return Hasher.mod(n, m);
        }

    // ----- properties -----------------------------------------------------

    /**
     * Obtains the current writer used for printing.
     *
     * @return the current writer used for printing; never null
     */
    public static PrintWriter getOut()
        {
        return Loggers.getOut();
        }

    /**
     * Sets the current writer used for printing.
     *
     * @param writer  the java.io.PrintWriter instance to use for printing;
     *                may be null
     */
    public static void setOut(PrintWriter writer)
        {
        Loggers.setOut(writer);
        }


    /**
     * Obtains the current writer used for tracing.
     *
     * @return the current writer used for tracing; never null
     */
    public static PrintWriter getErr()
        {
        return Loggers.getErr();
        }

    /**
     * Sets the current writer used for tracing.
     *
     * @param writer  the java.io.PrintWriter instance to use for tracing; may
     *                be null
     */
    public static void setErr(PrintWriter writer)
        {
        Loggers.setErr(writer);
        }

    /**
     * Obtains the current writer used for logging.
     *
     * @return the current writer used for logging; never null
     */
    public static PrintWriter getLog()
        {
        return Loggers.getLog();
        }

    /**
     * Sets the current writer used for logging.
     *
     * @param writer  the java.io.PrintWriter instance to use for logging; may
     *                be null
     */
    public static void setLog(PrintWriter writer)
        {
        Loggers.setLog(writer);
        }

    /**
     * Determine if the log is echoed to the console.
     *
     * @return true if the log is echoed to the console
     */
    public static boolean isLogEcho()
        {
        return Loggers.isLogEcho();
        }

    /**
     * Specify whether the log should echo to the console.
     *
     * @param fEcho  true if the log should echo to the console
     */
    public static void setLogEcho(boolean fEcho)
        {
        Loggers.setLogEcho(fEcho);
        }


    // ----- IO support -----------------------------------------------------

    /**
     * Read the contents out of the passed stream into the passed byte array
     * and return the length read.  This method will read up to the number of
     * bytes that can fit into the passed array.
     *
     * @param stream  a java.io.InputStream object to read from
     * @param ab      a byte array to read into
     *
     * @return  the number of bytes read from the InputStream and stored into
     *          the passed byte array
     *
     * @throws IOException  if an I/O error occurs
     */
    public static int read(InputStream stream, byte[] ab)
            throws IOException
        {
        return Reads.read(stream, ab);
        }

    /**
     * Read the contents out of the passed stream and return the result as a
     * byte array.
     *
     * @param stream  a java.io.InputStream object to read from
     *
     * @return  a byte array containing the contents of the passed InputStream
     *
     * @throws IOException  if an I/O error occurs
     */
    public static byte[] read(InputStream stream)
            throws IOException
        {
        return Reads.read(stream);
        }

    /**
     * Read the contents out of the passed stream and return the result as a
     * byte array.
     *
     * @param stream  a java.io.DataInput object to read from
     *
     * @return  a byte array containing the contents of the passed stream
     *
     * @throws IOException  if an I/O error occurs
     */
    public static byte[] read(DataInput stream)
            throws IOException
        {
        return Reads.read(stream);
        }

    /**
     * Read the contents out of the passed stream and return the result as a
     * byte array.
     *
     * @param stream  a java.io.DataInputStream object to read from
     *
     * @return  a byte array containing the contents of the passed InputStream
     *
     * @throws IOException  if an I/O error occurs
     */
    public static byte[] read(DataInputStream stream)
            throws IOException
        {
        return Reads.read(stream);
        }

    /**
     * Read the contents out of the passed Reader and return the result as a
     * String.
     *
     * @param reader  a java.io.Reader object to read from
     *
     * @return  a String containing the contents of the passed Reader
     *
     * @throws IOException  if an I/O error occurs
     */
    public static String read(Reader reader)
            throws IOException
        {
        return Reads.read(reader);
        }

    /**
     * Read the contents out of the specified file and return the result as a
     * byte array.
     *
     * @param file  the java.io.File object to read the contents of
     *
     * @return the contents of the specified File as a byte array
     *
     * @throws IOException  if an I/O error occurs
     */
    public static byte[] read(File file)
            throws IOException
        {
        return Reads.read(file);
        }

    /**
     * Read the contents of the specified URL and return the result as a
     * byte array.
     *
     * @param url  the java.net.URL object to read the contents of
     *
     * @return the contents of the specified URL as a byte array
     *
     * @throws IOException  if an I/O error occurs
     */
    public static byte[] read(URL url)
            throws IOException
        {
        return Reads.read(url);
        }


    // ----- inner class: LoggingWriter -----------------------------------

    /**
     * Inner class for over-riding the destination of log(), out() and err()
     * calls.
     */
    public static class LoggingWriter
            extends PrintWriter
        {
        /**
         * Construct a PrintWriter that logs using the {@link CacheFactory#log}
         * method.
         *
         * @param nSev the severity to log messages with
         */
        public LoggingWriter(int nSev)
            {
            super(new CharArrayWriter());
            m_nSev = nSev;
            }

        /**
         * Log the accumulated String using the logging severity that this
         * PrintWriter was configured with.
         */
        @Override
        public void println()
            {
            CharArrayWriter out = (CharArrayWriter) this.out;
            //noinspection SynchronizeOnNonFinalField
            synchronized (lock)
                {
                String s = out.toString();
                out.reset();
                CacheFactory.log(s, m_nSev);
                }
            }

        /**
         * The severity that this PrintWriter logs with.
         */
        private final int m_nSev;
        }

    // ----- constants ------------------------------------------------------

    /**
     * Integer constant representing a unit of nanoseconds.
     *
     * @see #parseTimeNanos(String, int)
     */
    public static final int UNIT_NS = -1000000;

    /**
     * Integer constant representing a unit of microseconds.
     *
     * @see #parseTimeNanos(String, int)
     */
    public static final int UNIT_US = -1000;

    /**
     * Integer constant representing a unit of milliseconds.
     *
     * @see #parseTime(String, int)
     */
    public static final int UNIT_MS = 1;

    /**
     * Integer constant representing a unit of seconds.
     *
     * @see #parseTime(String, int)
     */
    public static final int UNIT_S  = 1000*UNIT_MS;

    /**
     * Integer constant representing a unit of minutes.
     *
     * @see #parseTime(String, int)
     */
    public static final int UNIT_M  = 60*UNIT_S;

    /**
     * Integer constant representing a unit of hours.
     *
     * @see #parseTime(String, int)
     */
    public static final int UNIT_H  = 60*UNIT_M;

    /**
     * Integer constant representing a unit of days.
     *
     * @see #parseTime(String, int)
     */
    public static final int UNIT_D  = 24*UNIT_H;

    /**
     * Integer constant representing an exponent of zero.
     *
     * @see #parseBandwidth(String, int)
     * @see #parseMemorySize(String, int)
     */
    public static final int POWER_0 = 0;

    /**
     * Integer constant representing an exponent of 10.
     *
     * @see #parseBandwidth(String, int)
     * @see #parseMemorySize(String, int)
     */
    public static final int POWER_K = 10;

    /**
     * Integer constant representing an exponent of 20.
     *
     * @see #parseBandwidth(String, int)
     * @see #parseMemorySize(String, int)
     */
    public static final int POWER_M = 20;

    /**
     * Integer constant representing an exponent of 30.
     *
     * @see #parseBandwidth(String, int)
     * @see #parseMemorySize(String, int)
     */
    public static final int POWER_G = 30;

    /**
     * Integer constant representing an exponent of 40.
     *
     * @see #parseBandwidth(String, int)
     * @see #parseMemorySize(String, int)
     */
    public static final int POWER_T = 40;

    /**
     * The minimum logging level indicator.
     */
    public static final int LOG_MIN = 0;

    /**
     * The maximum logging level indicator.
     */
    public static final int LOG_MAX = 9;

    /**
     * It is expected that items with a log level of 0 will always be logged.
     */
    public static final int LOG_ALWAYS = 0;

    /**
     * Log level 1 indicates an error.
     */
    public static final int LOG_ERR = 1;

    /**
     * Log level 2 indicates a warning.
     */
    public static final int LOG_WARN = 2;

    /**
     * Log level 3 indicates information that should likely be logged.
     */
    public static final int LOG_INFO = 3;

    /**
     * Log level 4 indicates configuration related log message.
     */
    public static final int LOG_CONFIG = 4;

    /**
     * As of Coherence 3.2, the default logging level is 5, so using the level
     * of 5 will show up in the logs by default as a debug message.
     */
    public static final int LOG_DEBUG = 5;

    /**
     * Log level 5 indicates an essential debug message.
     */
    public static final int LOG_FINE = 5;

    /**
     * As of Coherence 3.2, the default logging level is 5, so using a level
     * higher than 5 will be "quiet" by default, meaning that it will not show
     * up in the logs unless the configured logging level is increased.
     */
    public static final int LOG_QUIET = 6;

    /**
     * Log level 6 indicates a non-essential debug message.
     */
    public static final int LOG_FINER = 6;

    /**
     * Log level 7 indicates a very low-level, non-essential debug message
     * or a tracing message.
     */
    public static final int LOG_FINEST = 7;

    // ----- data members ---------------------------------------------------

    /**
     * The configured ThreadFactory.
     */
    private static final ThreadFactory s_threadFactory = instantiateThreadFactory();

    // initialize logging through Coherence
    static
        {
        setOut(new LoggingWriter(LOG_ALWAYS));
        setErr(new LoggingWriter(LOG_ERR));
        setLog(new LoggingWriter(LOG_INFO));
        setLogEcho(false);
        }
    }
