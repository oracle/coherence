/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


import com.oracle.common.base.Blocking;
import com.oracle.common.base.Hasher;
import com.oracle.coherence.common.util.CommonMonitor;

import com.tangosol.coherence.config.Config;

import com.tangosol.io.ReadBuffer;

import com.tangosol.net.CacheFactory;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.CharArrayWriter;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import java.math.BigDecimal;
import java.math.BigInteger;

import java.net.URL;
import java.net.URLConnection;

import java.rmi.RemoteException;

import java.security.AccessController;
import java.security.PrivilegedAction;

import java.sql.Timestamp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TimeZone;

import java.util.concurrent.ThreadLocalRandom;

/**
* Base class for providing standard functionality.
*
* @author cp  2000.08.02
*/
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
        traceImpl(String.valueOf(fVal));
        }

    /**
     * Display the value of a char expression.
     *
     * @param chVal  the char value
     */
    public static void trace(char chVal)
        {
        traceImpl(String.valueOf(chVal));
        }

    /**
     * Display the value of an int expression.
     *
     * @param nVal  the int value
     */
    public static void trace(int nVal)
        {
        traceImpl(String.valueOf(nVal));
        }

    /**
     * Display the value of a long expression.
     *
     * @param lVal  the long value
     */
    public static void trace(long lVal)
        {
        traceImpl(String.valueOf(lVal));
        }

    /**
     * Display the value of a float expression.
     *
     * @param flVal  the float value
     */
    public static void trace(float flVal)
        {
        traceImpl(String.valueOf(flVal));
        }

    /**
     * Display the value of a double expression.
     *
     * @param dflVal  the double value
     */
    public static void trace(double dflVal)
        {
        traceImpl(String.valueOf(dflVal));
        }

    /**
     * Display the value of a byte array expression.
     *
     * @param ab  the byte array value
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
     *
     * @param sVal  the String value
     */
    public static void trace(String sVal)
        {
        traceImpl(sVal == null ? "null" : toQuotedStringEscape(sVal));
        }

    /**
     * Display the value of an Object expression.
     *
     * @param oVal  the Object value
     */
    public static void trace(Object oVal)
        {
        traceImpl(String.valueOf(oVal));
        }

    /**
     * Internal implementation for trace methods.
     *
     * @param sVal  the Object value
     */
    private static void traceImpl(String sVal)
        {
        String sExpr = getExpression("trace");
        out((sExpr == null ? "?" : sExpr) + '=' + (sVal == null ? "null" : sVal));
        }


    // ----- assertion support ----------------------------------------------

    /**
     * Definite assertion failure.
     * 
     * @return null
     */
    public static RuntimeException azzert()
        {
        azzert(false, "Assertion:  Unexpected execution of code at:");
        return null;
        }

    /**
     * Test an assertion.
     *
     * @param f  the boolean to be checked
     */
    public static void azzert(boolean f)
        {
        if (!f)
            {
            azzertFailed(null);
            }
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
        if (!f)
            {
            azzertFailed(s);
            }
        }

    /**
     * Throw an assertion exception.
     *
     * @param sMsg  the assertion message
     */
    public static void azzertFailed(String sMsg)
        {
        if (sMsg == null)
            {
            // default assertion message
            sMsg = "Assertion failed:";

            // try to load the source to print out the exact assertion
            String sSource = getExpression("azzert");
            if (sSource != null)
                {
                sMsg += "  " + sSource;
                }
            }

        // display the assertion
        err(sMsg);

        // display the code that caused the assertion
        String sStack = getStackTrace();
        err(sStack.substring(sStack.indexOf('\n', sStack.lastIndexOf(".azzert(")) + 1));

        throw new AssertionException(sMsg);
        }

    /**
     * Get the source code expression from the method that called the
     * specified method.
     *
     * @param sMethod the first method on the stack after the method whose
     *        source code is desired
     *
     * @return null on any failure, otherwise the expression
     */
    private static String getExpression(String sMethod)
        {
        StackFrame[] aframe = getStackFrames();
        int i = 0;

        // find method in call stack
        while (!aframe[i].getMethodName().equals(sMethod))
            {
            ++i;
            }

        // find calling method
        while (aframe[i].getMethodName().equals(sMethod))
            {
            ++i;
            }

        // get source line
        String sLine = aframe[i].getLine();
        if (sLine != null)
            {
            int of = sLine.indexOf(sMethod);
            if (of >= 0)
                {
                // this is unavoidably imprecise but will usually work correctly
                int ofLParen = sLine.indexOf('(', of);
                int ofRParen = sLine.lastIndexOf(')');
                if (ofLParen > of && ofRParen > ofLParen)
                    {
                    return sLine.substring(ofLParen + 1, ofRParen);
                    }
                }
            }

        return null;
        }


    // ----- exception support ----------------------------------------------

    /**
     * Convert the passed exception to a RuntimeException if necessary.
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
     * @param e  any Throwable object
     * @param s  an additional description
     *
     * @return a RuntimeException
     */
    public static RuntimeException ensureRuntimeException(Throwable e, String s)
        {
        if (e instanceof RuntimeException && (s == null || s.equals(e.getMessage())))
            {
            return (RuntimeException) e;
            }
        else
            {
            return WrapperException.ensure(e, s);
            }
        }

    /**
     * Unwind the wrapper (runtime) exception to extract the original
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
        s_out.println();
        }

    /**
     * Prints the passed Object.
     *
     * @param o  the Object to print.
     */
    public static void out(Object o)
        {
        s_out.println(String.valueOf(o));
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
    public static void out(Class clz)
        {
        s_out.println(toString(clz));
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
        s_err.println(String.valueOf(o));
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
    public static void err(Class clz)
        {
        s_err.println(toString(clz));
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
    public static void log(Class clz)
        {
        log(toString(clz));
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
        return loader == null ? getContextClassLoader() : loader;
        }

    /**
     * Try to determine the ClassLoader that supports the current context.
     *
     * @return a ClassLoader to use for the current context
     */
    public static ClassLoader getContextClassLoader()
        {
        return getContextClassLoader(null);
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
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null)
            {
            if (o != null)
                {
                loader = o.getClass().getClassLoader();
                }
            if (loader == null)
                {
                loader = Base.class.getClassLoader();
                if (loader == null)
                    {
                    loader = ClassLoader.getSystemClassLoader();
                    }
                }
            }
        return loader;
        }


    // ----- stack trace support --------------------------------------------

    /**
     * Get the StackFrame information for the caller of the current method.
     *
     * @return the StackFrame information for the caller of the current method
     */
    public static StackFrame getCallerStackFrame()
        {
        // display the code that caused the assertion
        String sStack = getStackTrace();
        int of = sStack.indexOf('\n', sStack.lastIndexOf(".getCallerStackFrame(")) + 1;
        of = sStack.indexOf('\n', of) + 1;
        try
            {
            return new StackFrame(sStack.substring(of, sStack.indexOf('\n', of)));
            }
        catch (RuntimeException e)
            {
            return StackFrame.UNKNOWN;
            }
        }

    /**
     * Get the StackFrame information for the current method.
     *
     * @return the StackFrame information for the current method
     */
    public static StackFrame getStackFrame()
        {
        // display the code that caused the assertion
        String sStack = getStackTrace();
        int of = sStack.indexOf('\n', sStack.lastIndexOf(".getStackFrame(")) + 1;
        try
            {
            return new StackFrame(sStack.substring(of, sStack.indexOf('\n', of)));
            }
        catch (RuntimeException e)
            {
            return StackFrame.UNKNOWN;
            }
        }

    /**
     * Iterate the StackFrame information for all callers, going from the
     * inside outwards, and starting with the caller of this method.
     *
     * @return an Iterator of StackFrames
     */
    public static StackFrame[] getStackFrames()
        {
        String sStack = getStackTrace();
        LineNumberReader reader = new LineNumberReader(new StringReader(
                sStack.substring(sStack.indexOf('\n', sStack.lastIndexOf(
                ".getStackFrames(")) + 1) ));

        try
            {
            ArrayList list = new ArrayList();
            String sLine = reader.readLine();
            while (sLine != null && sLine.length() > 0)
                {
                StackFrame frame = StackFrame.UNKNOWN;
                try
                    {
                    frame = new StackFrame(sLine);
                    }
                catch (RuntimeException e) {}

                list.add(frame);
                sLine = reader.readLine();
                }
            return (StackFrame[]) list.toArray(new StackFrame[list.size()]);
            }
        catch (IOException e)
            {
            throw ensureRuntimeException(e);
            }
        }

    /**
     * Build a stack trace for the current thread.
     *
     * @return a String containing a printable stack trace
     */
    public static String getStackTrace()
        {
        String s = getStackTrace(new Exception());
        return s.substring(s.indexOf('\n', s.indexOf(".getStackTrace(")) + 1);
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
        String s = printStackTrace(e);
        if (s.startsWith(e.getClass().getName()))
            {
            s = s.substring(s.indexOf('\n') + 1);
            }
        return s;
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
        StringBuilder sb = new StringBuilder();

        String sMsgLast = null;
        for (; t != null; t = t.getCause())
            {
            String sMsg = t.getMessage();
            if (!equals(sMsgLast, sMsg))
                {
                if (sMsgLast != null)
                    {
                    sb.append(sDelim);
                    }
                sb.append(sMsg);
                sMsgLast = sMsg;
                }
            }
        return sb.toString();
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
        try
            {
            try (Writer writerRaw = new CharArrayWriter(1024))
                {
                try (PrintWriter writerOut = new PrintWriter(writerRaw))
                    {
                    e.printStackTrace(writerOut);
                    }

                writerRaw.flush();

                return writerRaw.toString();
                }
            }
        catch (IOException eIO)
            {
            throw ensureRuntimeException(eIO);
            }
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
        return num instanceof BigDecimal ? (BigDecimal) num :
               num instanceof BigInteger ? new BigDecimal((BigInteger) num) :
                                           new BigDecimal(num.doubleValue());
        }

    /**
     * A class that provides "stack frame" information from a line of a stack
     * trace.
     */
    public static class StackFrame
        {
        /**
         * Construct a StackFrame object from a line of a stack trace.
         *
         * @param sExcept  a line of a stack trace in the format used by the
         *                 reference implementation of the JVM
         *
         * @throws RuntimeException  if there is a runtime error
         */
        public StackFrame(String sExcept)
            {
            try
                {
                // sExcept = " at com.tangosol.util.Test.main(Test.java:23)"
                int of = sExcept.indexOf('(');
                String sLeft  = sExcept.substring(sExcept.lastIndexOf(' ', of) + 1, of);
                String sRight = sExcept.substring(of + 1, sExcept.lastIndexOf(')'));

                // sLeft  = "com.tangosol.util.Test.main"
                of = sLeft.lastIndexOf('.');
                String sClass  = sLeft.substring(0, of);
                String sMethod = sLeft.substring(of + 1);

                // sRight = "Test.java:23"
                String sFile = "unknown";
                int    nLine = 0;

                of = sRight.lastIndexOf(':');
                if (of >= 0)
                    {
                    sFile = sRight.substring(0, of);

                    try
                        {
                        nLine = Integer.parseInt(sRight.substring(of + 1));
                        }
                    catch (RuntimeException e) {}
                    }

                init(sFile, sClass, sMethod, nLine);
                }
            catch (RuntimeException e)
                {
                out("Exception constructing StackFrame for \"" + sExcept + "\"");
                throw e;
                }
            }

        /**
         * Construct a StackFrame object from its constituent data members.
         *
         * @param sFile    the source file name (e.g. Test.java)
         * @param sClass   the fully qualified class name (e.g. pkg.Test$1)
         * @param sMethod  the method name (e.g. main)
         * @param nLine    the line number (e.g. 17) or 0 if unknown
         */
        public StackFrame(String sFile, String sClass, String sMethod, int nLine)
            {
            init(sFile, sClass, sMethod, nLine);
            }

        /**
         * Initialize the fields of the StackFrame object.
         *
         * @param sFile    the source file name (e.g. Test.java)
         * @param sClass   the fully qualified class name (e.g. pkg.Test$1)
         * @param sMethod  the method name (e.g. main)
         * @param nLine    the line number (e.g. 17) or 0 if unknown
         */
        protected void init(String sFile, String sClass, String sMethod, int nLine)
            {
            m_sFile   = sFile;
            m_sClass  = sClass;
            m_sMethod = sMethod;
            m_nLine   = nLine;
            }

        /**
         * @return the source file name (e.g. Test.java)
         */
        public String getFileName()
            {
            return m_sFile;
            }

        /**
         * @return the fully qualified class name (e.g. pkg.Test$1)
         */
        public String getClassName()
            {
            return m_sClass;
            }

        /**
         * @return the short class name (e.g. Test.1)
         */
        public String getShortClassName()
            {
            String sClass = m_sClass;
            sClass = sClass.substring(sClass.lastIndexOf('.') + 1);
            return sClass.replace('$', '.');
            }

        /**
         * @return the method name (e.g. main)
         */
        public String getMethodName()
            {
            return m_sMethod;
            }

        /**
         * @return the line number (e.g. 17) or 0 if unknown
         */
        public int getLineNumber()
            {
            return m_nLine;
            }

        /**
         * @return the line of source code if possible or null
         */
        public String getLine()
            {
            int nLine = getLineNumber();
            if (nLine == 0)
                {
                return null;
                }

            try
                {
                String sClass = getClassName();
                int    of     = sClass.indexOf('$');
                if (of >= 0)
                    {
                    sClass = sClass.substring(0, of);
                    }

                InputStream stream = Class.forName(sClass, false,
                            getContextClassLoader())
                                .getClassLoader().getResourceAsStream(
                                    sClass.replace('.', '/') + ".java");
                if (stream != null)
                    {
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(stream)))
                        {
                        String sLine = null;
                        for (int i = 0; i < nLine; ++i)
                            {
                            sLine = reader.readLine();
                            }
                        return sLine;
                        }
                    }
                }
            catch (Throwable t)
                {
                }

            return null;
            }

        /**
         * @return a String representation of the StackFrame
         */
        public String toString()
            {
            int    nLine = getLineNumber();
            String sLine = nLine == 0 ? "?" : "" + nLine;

            return getClassName() + '.' + getMethodName() + '('
                   + getFileName() + ':' + sLine + ')';
            }

        /**
         * @return a short String representation of the StackFrame
         */
        public String toShortString()
            {
            int    nLine = getLineNumber();
            String sLine = nLine == 0 ? "?" : "" + nLine;

            return getShortClassName() + '.' + getMethodName()
                   + " [" + sLine + ']';
            }

        public static final StackFrame UNKNOWN = new StackFrame(
                "unknown", "unknown", "unknown", 0);

        private String m_sFile;
        private String m_sClass;
        private String m_sMethod;
        private int    m_nLine;
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
     * @return the configured thread factory, or null if none has been
     * configured.
     */
    public static ThreadFactory getThreadFactory()
        {
        return s_threadFactory;
        }

    /**
     * Instantiate the configured thread factory.
     *
     * @return the configured thread factory, or null if none has been configured.
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
    public static String toString(Class clz)
        {
        if (clz.isPrimitive())
            {
            return clz.toString();
            }
        else if (clz.isArray())
            {
            return "Array of " + toString(clz.getComponentType());
            }
        else if (clz.isInterface())
            {
            return toInterfaceString(clz, "");
            }
        else
            {
            return toClassString(clz, "");
            }
        }

    /**
     * Formats Class information for debug output purposes.
     *
     * @param clz      the Class to print information for
     * @param sIndent  the indentation to precede each line of output
     *
     * @return a String describing the Class in detail
     */
    private static String toClassString(Class clz, String sIndent)
        {
        StringBuilder sb = new StringBuilder();
        sb.append(sIndent)
          .append("Class ")
          .append(clz.getName())
          .append("  (")
          .append(toString(clz.getClassLoader()))
          .append(')');

        sIndent += "  ";

        Class[] aclz = clz.getInterfaces();
        for (int i = 0, c = aclz.length; i < c; ++i)
            {
            sb.append('\n')
              .append(toInterfaceString(aclz[i], sIndent));
            }

        clz = clz.getSuperclass();
        if (clz != null)
            {
            sb.append('\n')
              .append(toClassString(clz, sIndent));
            }

        return sb.toString();
        }

    /**
     * Formats interface information for debug output purposes.
     *
     * @param clz      the interface Class to print information for
     * @param sIndent  the indentation to precede each line of output
     *
     * @return a String describing the interface Class in detail
     */
    private static String toInterfaceString(Class clz, String sIndent)
        {
        StringBuilder sb = new StringBuilder();
        sb.append(sIndent)
          .append("Interface ")
          .append(clz.getName())
          .append("  (")
          .append(toString(clz.getClassLoader()))
          .append(')');

        Class[] aclz = clz.getInterfaces();
        for (int i = 0, c = aclz.length; i < c; ++i)
            {
            clz = aclz[i];

            sb.append('\n')
              .append(toInterfaceString(clz, sIndent + "  "));
            }

        return sb.toString();
        }

    /**
     * Format a description for the specified ClassLoader object.
     *
     * @param loader  the ClassLoader instance (or null)
     *
     * @return a String description of the ClassLoader
     */
    private static String toString(ClassLoader loader)
        {
        if (loader == null)
            {
            return "System ClassLoader";
            }

        return "ClassLoader class=" + loader.getClass().getName()
                + ", hashCode=" + loader.hashCode();
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
        return (ch >= '0') && (ch <= '9');
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
        if ((ch >= '0') && (ch <= '9'))
            {
            return ch - '0';
            }
        else
            {
            throw new IllegalArgumentException("Character \"" + ch
                    + "\" is not a valid decimal digit.");
            }
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
        int cDigits = 0;
        do
            {
            cDigits += 1;
            n /= 10;
            }
        while (n != 0);

        return cDigits;
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
        char[] ach = new char[cDigits];
        while (cDigits > 0)
            {
            ach[--cDigits] = (char) ('0' + n % 10);
            n /= 10;
            }

        return new String(ach);
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
        return ((lMin + lMultiple - 1) / lMultiple) * lMultiple;
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
        return (ch >= '0') && (ch <= '7');
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
        if ((ch >= '0') && (ch <= '7'))
            {
            return ch - '0';
            }
        else
            {
            throw new IllegalArgumentException("Character \"" + ch + "\" is not a valid octal digit.");
            }
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
        return ((ch >= '0') && (ch <= '9'))
            || ((ch >= 'A') && (ch <= 'F'))
            || ((ch >= 'a') && (ch <= 'f'));
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
        if ((ch >= '0') && (ch <= '9'))
            {
            return ch -'0';
            }
        else if ((ch >= 'A') && (ch <= 'F'))
            {
            return ch - 'A' + 10;
            }
        else if ((ch >= 'a') && (ch <= 'f'))
            {
            return ch - 'a' + 10;
            }
        else
            {
            throw new IllegalArgumentException("Character \"" + ch + "\" is not a valid hexadecimal digit.");
            }
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
        int cDigits = 0;
        do
            {
            cDigits++;
            n >>>= 4;
            }
        while (n != 0);

        return cDigits;
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
        char[] ach = new char[cDigits];
        while (cDigits > 0)
            {
            ach[--cDigits] = HEX[n & 0x0F];
            n >>>= 4;
            }

        return new String(ach);
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
        int    n   = b & 0xFF;
        char[] ach = new char[2];

        ach[0] = HEX[n >> 4];
        ach[1] = HEX[n & 0x0F];

        return new String(ach);
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
        int    cb  = ab.length;
        char[] ach = new char[cb * 2];

        for (int ofb = 0, ofch = 0; ofb < cb; ++ofb)
            {
            int n = ab[ofb] & 0xFF;
            ach[ofch++] = HEX[n >> 4];
            ach[ofch++] = HEX[n & 0x0F];
            }

        return new String(ach);
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
        int    n   = b & 0xFF;
        char[] ach = new char[4];

        ach[0] = '0';
        ach[1] = 'x';
        ach[2] = HEX[n >> 4];
        ach[3] = HEX[n & 0x0F];

        return new String(ach);
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
        return toHexEscape(ab, 0, ab.length);
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
        char[] ach = new char[2 + cb * 2];

        ach[0] = '0';
        ach[1] = 'x';

        for (int ofb = of, ofch = 2, ofStop = of + cb; ofb < ofStop; ++ofb)
            {
            int n = ab[ofb] & 0xFF;
            ach[ofch++] = HEX[n >> 4];
            ach[ofch++] = HEX[n & 0x0F];
            }

        return new String(ach);
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
        if (cb > 0)
            {
            char[] ach = new char[2 + cb * 2];

            ach[0] = '0';
            ach[1] = 'x';

            for (int ofb = of, ofch = 2, ofStop = of + cb; ofb < ofStop; ++ofb)
                {
                int n = seq.byteAt(ofb) & 0xFF;
                ach[ofch++] = HEX[n >> 4];
                ach[ofch++] = HEX[n & 0x0F];
                }

            return new String(ach);
            }
        else
            {
            return "";
            }
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
        int cb = ab.length;
        if (cb == 0)
            {
            return "";
            }

        // calculate number of digits required to show offset
        int cDigits = 0;
        int cbTemp  = cb - 1;
        do
            {
            cDigits += 2;
            cbTemp  /= 0x100;
            }
        while (cbTemp > 0);

        // calculate number and size of lines
        int    cLines = (cb + cBytesPerLine - 1) / cBytesPerLine;
        int    cCharsPerLine = cDigits + 4 * cBytesPerLine + 5;

        // pre-allocate buffer to build hex dump
        int    cch = cLines * cCharsPerLine;
        char[] ach = new char [cch];

        // offsets within each line for formatting stuff
        int ofColon = cDigits;
        int ofLF    = cCharsPerLine - 1;

        // offsets within each line for data
        int ofHexInLine  = ofColon + 3;
        int ofCharInLine = ofLF - cBytesPerLine;

        int ofByte = 0;
        int ofLine = 0;
        for (int iLine = 0; iLine < cLines; ++iLine)
            {
            // format offset
            int nOffset = ofByte;
            int ofDigit = ofLine + cDigits;
            for (int i = 0; i < cDigits; ++i)
                {
                ach[--ofDigit] = HEX[nOffset & 0x0F];
                nOffset >>= 4;
                }

            // formatting
            int ofFmt = ofLine + cDigits;
            ach[ofFmt++] = ':';
            ach[ofFmt++] = ' ';
            ach[ofFmt  ] = ' ';

            // format data
            int ofHex  = ofLine + ofHexInLine;
            int ofChar = ofLine + ofCharInLine;
            for (int i = 0; i < cBytesPerLine; ++i)
                {
                try
                    {
                    int n = ab[ofByte++] & 0xFF;

                    ach[ofHex++ ] = HEX[(n & 0xF0) >> 4];
                    ach[ofHex++ ] = HEX[(n & 0x0F)     ];
                    ach[ofHex++ ] = ' ';
                    ach[ofChar++] = (n < 32 ? '.' : (char) n);
                    }
                catch (ArrayIndexOutOfBoundsException e)
                    {
                    ach[ofHex++ ] = ' ';
                    ach[ofHex++ ] = ' ';
                    ach[ofHex++ ] = ' ';
                    ach[ofChar++] = ' ';
                    }
                }

            // spacing and newline
            ach[ofHex ] = ' ';
            ach[ofChar] = '\n';

            ofLine += cCharsPerLine;
            }

        return new String(ach, 0, cch-1);
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
        char[] ach = s.toCharArray();
        int    cch = ach.length;
        if (cch == 0)
            {
            return new byte[0];
            }

        if ((cch & 0x1) != 0)
            {
            throw new IllegalArgumentException("invalid length hex string");
            }

        int ofch = 0;
        if (ach[1] == 'x' || ach[1] == 'X')
            {
            ofch = 2;
            }

        int    cb = (cch - ofch) / 2;
        byte[] ab = new byte[cb];
        for (int ofb = 0; ofb < cb; ++ofb)
            {
            ab[ofb] = (byte) (parseHex(ach[ofch++]) << 4 | parseHex(ach[ofch++]));
            }

        return ab;
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
        switch (ch)
            {
            case '0': case '1': case '2': case '3': case '4':
            case '5': case '6': case '7': case '8': case '9':
                return ch - '0';

            case 'A': case 'B': case 'C': case 'D': case 'E': case 'F':
                return ch - 'A' + 0x0A;

            case 'a': case 'b': case 'c': case 'd': case 'e': case 'f':
                return ch - 'a' + 0x0A;

            default:
                throw new IllegalArgumentException("illegal hex char: " + ch);
            }
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
        BigDecimal decVal     = new BigDecimal(dfl);
        BigInteger intVal     = decVal.toBigInteger();
        String     sIntVal    = intVal.toString();
        int        cIntDigits = sIntVal.length() - (intVal.signum() <= 0 ? 1 : 0);
        int        cDecDigits = decVal.scale();
        if (cIntDigits >= cMinDigits || cDecDigits == 0)
            {
            return sIntVal;
            }

        int cRemDigits = cMinDigits - cIntDigits;
        if (cDecDigits > cRemDigits)
            {
            decVal = decVal.setScale(cRemDigits, BigDecimal.ROUND_HALF_UP);
            }

        String  sDecVal = decVal.toString();
        int     of      = sDecVal.length() - 1;
        if (sDecVal.length() > 1 && sDecVal.charAt(of) == '0')
            {
            do
                {
                --of;
                }
            while (sDecVal.charAt(of) == '0');
            if (sDecVal.charAt(of) == '.')
                {
                --of;
                }
            sDecVal = sDecVal.substring(0, of + 1);
            }

        return sDecVal;
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
        int    n   = ch;
        char[] ach = new char[6];

        ach[0] = '\\';
        ach[1] = 'u';
        ach[2] = HEX[n >> 12       ];
        ach[3] = HEX[n >>  8 & 0x0F];
        ach[4] = HEX[n >>  4 & 0x0F];
        ach[5] = HEX[n       & 0x0F];

        return new String(ach);
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
        char[] ach = new char[6];
        int cch = escape(ch, ach, 0);
        return new String(ach, 0, cch);
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
        char[] ach = new char[8];
        ach[0] = '\'';
        int cch = escape(ch, ach, 1);
        ach[cch + 1] = '\'';
        return new String(ach, 0, cch + 2);
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
        char[] achSrc = s.toCharArray();
        int    cchSrc = achSrc.length;
        int    ofSrc  = 0;

        int    cchDest = cchSrc * 6;            // 100% safe
        char[] achDest = new char[cchDest];
        int    ofDest  = 0;

        while (ofSrc < cchSrc)
            {
            ofDest += escape(achSrc[ofSrc++], achDest, ofDest);
            }

        return new String(achDest, 0, ofDest);
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
        char[] achSrc = s.toCharArray();
        int    cchSrc = achSrc.length;
        int    ofSrc  = 0;

        int    cchDest = cchSrc * 6 + 2;        // 100% safe
        char[] achDest = new char[cchDest];
        int    ofDest  = 0;

        achDest[ofDest++] = '\"';
        while (ofSrc < cchSrc)
            {
            ofDest += escape(achSrc[ofSrc++], achDest, ofDest);
            }
        achDest[ofDest++] = '\"';

        return new String(achDest, 0, ofDest);
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
        switch (ch)
            {
            case '\b':
                ach[of++] = '\\';
                ach[of  ] = 'b';
                return 2;
            case '\t':
                ach[of++] = '\\';
                ach[of  ] = 't';
                return 2;
            case '\n':
                ach[of++] = '\\';
                ach[of  ] = 'n';
                return 2;
            case '\f':
                ach[of++] = '\\';
                ach[of  ] = 'f';
                return 2;
            case '\r':
                ach[of++] = '\\';
                ach[of  ] = 'r';
                return 2;
            case '\"':
                ach[of++] = '\\';
                ach[of  ] = '\"';
                return 2;
            case '\'':
                ach[of++] = '\\';
                ach[of  ] = '\'';
                return 2;
            case '\\':
                ach[of++] = '\\';
                ach[of  ] = '\\';
                return 2;

            case 0x00:  case 0x01: case 0x02: case 0x03:
            case 0x04:  case 0x05: case 0x06: case 0x07:
                                              case 0x0B:
                                   case 0x0E: case 0x0F:
            case 0x10:  case 0x11: case 0x12: case 0x13:
            case 0x14:  case 0x15: case 0x16: case 0x17:
            case 0x18:  case 0x19: case 0x1A: case 0x1B:
            case 0x1C:  case 0x1D: case 0x1E: case 0x1F:
                ach[of++] = '\\';
                ach[of++] = '0';
                ach[of++] = (char)(ch / 8 + '0');
                ach[of  ] = (char)(ch % 8 + '0');
                return 4;

            default:
                switch (Character.getType(ch))
                    {
                    case Character.CONTROL:
                    case Character.PRIVATE_USE:
                    case Character.UNASSIGNED:
                        {
                        int n = ch;
                        ach[of++] = '\\';
                        ach[of++] = 'u';
                        ach[of++] = HEX[n >> 12       ];
                        ach[of++] = HEX[n >>  8 & 0x0F];
                        ach[of++] = HEX[n >>  4 & 0x0F];
                        ach[of  ] = HEX[n       & 0x0F];
                        }
                        return 6;
                    }
            }

        // character does not need to be escaped
        ach[of] = ch;
        return 1;
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
        if (s == null)
            {
            return "NULL";
            }

        if (s.length() == 0)
            {
            return "''";
            }

        if (s.indexOf('\'') < 0)
            {
            return '\'' + s + '\'';
            }

        char[] ach = s.toCharArray();
        int    cch = ach.length;

        StringBuilder sb = new StringBuilder(cch + 16);

        // open quotes
        sb.append('\'');

        // scan for characters to escape
        int ofPrev = 0;
        for (int ofCur = 0; ofCur < cch; ++ofCur)
            {
            char ch = ach[ofCur];

            switch (ch)
                {
                case '\n':
                case '\'':
                    {
                    // copy up to this point
                    if (ofCur > ofPrev)
                        {
                        sb.append(ach, ofPrev, ofCur - ofPrev);
                        }

                    // process escape
                    switch (ch)
                        {
                        case '\n':
                            // close quote, new line, re-open quote
                            sb.append("\'\n\'");
                            break;
                        case '\'':
                            // escape single quote with a second single quote
                            sb.append("\'\'");
                            break;
                        }

                    // processed up to the following character
                    ofPrev = ofCur + 1;
                    }
                }
            }

        // copy the remainder of the string
        if (ofPrev < cch)
            {
            sb.append(ach, ofPrev, cch - ofPrev);
            }

        // close quotes
        sb.append('\'');

        return sb.toString();
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
        return indentString(sText, sIndent, true);
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
        char[] ach = sText.toCharArray();
        int    cch = ach.length;

        StringBuilder sb = new StringBuilder();

        int iLine  = 0;
        int of     = 0;
        int ofPrev = 0;
        while (of < cch)
            {
            if (ach[of++] == '\n' || of == cch)
                {
                if (iLine++ > 0 || fFirstLine)
                    {
                    sb.append(sIndent);
                    }

                sb.append(sText.substring(ofPrev, of));
                ofPrev = of;
                }
            }

        return sb.toString();
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
        return breakLines(sText, nWidth, sIndent, true);
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
        if (sIndent == null)
            {
            sIndent = "";
            }

        nWidth -= sIndent.length();
        if (nWidth <= 0)
            {
            throw new IllegalArgumentException("The width and indent are incompatible");
            }

        char[] ach = sText.toCharArray();
        int    cch = ach.length;

        StringBuilder sb = new StringBuilder(cch);

        int ofPrev = 0;
        int of     = 0;

        while (of < cch)
            {
            char c = ach[of++];

            boolean fBreak  = false;
            int     ofBreak = of;
            int     ofNext  = of;

            if (c == '\n')
                {
                fBreak = true;
                ofBreak--;
                }
            else if (of == cch)
                {
                fBreak = true;
                }
            else if (of == ofPrev + nWidth)
                {
                fBreak = true;

                while (!Character.isWhitespace(ach[--ofBreak]) && ofBreak > ofPrev)
                    {
                    }
                if (ofBreak == ofPrev)
                    {
                    ofBreak = of; // no spaces -- force the break
                    }
                else
                    {
                    ofNext = ofBreak + 1;
                    }
                }

            if (fBreak)
                {
                if (ofPrev > 0)
                    {
                    sb.append('\n')
                      .append(sIndent);
                    }
                else if (fFirstLine)
                    {
                    sb.append(sIndent);
                    }

                sb.append(sText.substring(ofPrev, ofBreak));

                ofPrev = ofNext;
                }

            }

        return sb.toString();
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
        char[] ach = new char[cch];
        for (int of = 0; of < cch; ++of)
            {
            ach[of] = ch;
            }
        return new String(ach);
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
        if (c < 1)
            {
            return "";
            }
        if (c == 1)
            {
            return s;
            }

        char[] achPat = s.toCharArray();
        int    cchPat = achPat.length;
        int    cchBuf = cchPat * c;
        char[] achBuf = new char[cchBuf];
        for (int i = 0, of = 0; i < c; ++i, of += cchPat)
            {
            System.arraycopy(achPat, 0, achBuf, of, cchPat);
            }
        return new String(achBuf);
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
        if (sFrom.length() == 0)
            {
            return sText;
            }

        StringBuilder sbTextNew = new StringBuilder();
        int iTextLen = sText.length();
        int iStart   = 0;

        while (iStart < iTextLen)
            {
            int iPos = sText.indexOf(sFrom, iStart);
            if (iPos != -1)
                {
                sbTextNew.append(sText.substring(iStart, iPos));
                sbTextNew.append(sTo);
                iStart = iPos + sFrom.length();
                }
            else
                {
                sbTextNew.append(sText.substring(iStart));
                break;
                }
            }

        return sbTextNew.toString();
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
        if (s == null)
            {
            return null;
            }

        List list   = new ArrayList();
        int  ofPrev = -1;
        while (true)
            {
            int ofNext = s.indexOf(chDelim, ofPrev + 1);
            if (ofNext < 0)
                {
                list.add(s.substring(ofPrev + 1));
                break;
                }
            else
                {
                list.add(s.substring(ofPrev + 1, ofNext));
                }

            ofPrev = ofNext;
            }

        return (String[]) list.toArray(new String[list.size()]);
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
        int c = an.length;
        if (c > 0)
            {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < c; i++)
                {
                sb.append(sDelim).append(an[i]);
                }
            return sb.substring(sDelim.length());
            }
        else
            {
            return "";
            }
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
        int c = al.length;
        if (c > 0)
            {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < c; i++)
                {
                sb.append(sDelim).append(al[i]);
                }
            return sb.substring(sDelim.length());
            }
        else
            {
            return "";
            }
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
        int c = ao.length;
        if (c > 0)
            {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < c; i++)
                {
                sb.append(sDelim).append(ao[i]);
                }
            return sb.substring(sDelim.length());
            }
        else
            {
            return "";
            }
        }

    /**
     * Format the content of the passed Iterator as a delimited string.
     *
     * @param iter    the Iterator
     * @param sDelim  the delimiter
     *
     * @return the formatted string
     */
    public static String toDelimitedString(Iterator iter, String sDelim)
        {
        StringBuilder sb = new StringBuilder();
        while (iter.hasNext())
            {
            sb.append(sDelim).append(iter.next());
            }
        return sb.length() == 0 ? "" : sb.substring(sDelim.length());
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
        return s.length() > 1
                ? s.substring(0, 1).toUpperCase() + s.substring(1)
                : s.toUpperCase();
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
        int cChar = s.length();
        return cChar > cLimit
                ? s.substring(0, cLimit) + "...(" + (cChar - cLimit) + " more)"
                : s;
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
    public static String truncateString(Collection coll, int cLimit)
        {
        StringBuilder sb = new StringBuilder(ClassHelper.getSimpleName(coll.getClass()))
                           .append('[');

        cLimit += sb.length() + 1;

        int c = 1;
        for (Iterator iter = coll.iterator(); iter.hasNext() && sb.length() < cLimit; ++c)
            {
            if (c > 1)
                {
                sb.append(", ");
                }
            sb.append(iter.next());
            }

        if (c < coll.size() && sb.length() >= cLimit)
            {
            sb.append(", ...");
            }

        return sb.append(']').toString();
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
        return parseBandwidth(s, POWER_0);
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
        if (s == null)
            {
            throw new IllegalArgumentException("passed String must not be null");
            }

        switch (nDefaultPower)
            {
            case POWER_0:
            case POWER_K:
            case POWER_M:
            case POWER_G:
            case POWER_T:
                break;
            default:
                throw new IllegalArgumentException("illegal default power: "
                        + nDefaultPower);
            }

        // remove trailing "[[P|p][S|s]]?"
        int cch = s.length();
        if (cch >= 2)
            {
            char ch = s.charAt(cch - 1);
            if (ch == 'S' || ch == 's')
                {
                ch = s.charAt(cch - 2);
                if (ch == 'P' || ch == 'p')
                    {
                    cch -= 2;
                    }
                else
                    {
                    throw new IllegalArgumentException("invalid bandwidth: \""
                        + s + "\" (illegal bandwidth unit)");
                    }
                }
            }

        // remove trailing "[B|b]?" and store it as a factor
        // (default is "bps" i.e. baud)
        int     cBitShift = -3;
        boolean fDefault  = true;
        if (cch >= 1)
            {
            switch (s.charAt(cch - 1))
                {
                case 'B':
                    cBitShift = 0;
                    // no break;
                case 'b':
                    --cch;
                    fDefault = false;
                    break;
                }
            }

        // remove trailing "[K|k|M|m|G|g|T|t]?[B|b]?" and update the factor
        if (cch >= 1)
            {
            switch (s.charAt(--cch))
                {
                case 'K': case 'k':
                    cBitShift += POWER_K;
                    break;

                case 'M': case 'm':
                    cBitShift += POWER_M;
                    break;

                case 'G': case 'g':
                    cBitShift += POWER_G;
                    break;

                case 'T': case 't':
                    cBitShift += POWER_T;
                    break;

                default:
                    if (fDefault)
                        {
                        cBitShift += nDefaultPower;
                        }
                    ++cch; // oops: shouldn't have chopped off the last char
                    break;
                }
            }

        // make sure that the string contains some digits
        if (cch == 0)
            {
            throw new NumberFormatException("passed String (\"" + s
                    + "\") must contain a number");
            }

        // extract the digits (decimal form) to assemble the base number
        long    cb       = 0;
        boolean fDecimal = false;
        int     nDivisor = 1;
        for (int of = 0; of < cch; ++of)
            {
            char ch = s.charAt(of);
            switch (ch)
                {
                case '0': case '1': case '2': case '3': case '4':
                case '5': case '6': case '7': case '8': case '9':
                    cb = (cb * 10) + (ch - '0');
                    if (fDecimal)
                        {
                        nDivisor *= 10;
                        }
                    break;

                case '.':
                    if (fDecimal)
                        {
                        throw new NumberFormatException("invalid bandwidth: \""
                            + s + "\" (illegal second decimal point)");
                        }
                    fDecimal = true;
                    break;

                default:
                    throw new NumberFormatException("invalid bandwidth: \""
                            + s + "\" (illegal digit: \"" + ch + "\")");
                }
            }

        if (cBitShift < 0)
            {
            cb >>>= -cBitShift;
            }
        else
            {
            cb <<= cBitShift;
            }

        if (fDecimal)
            {
            if (nDivisor == 1)
                {
                throw new NumberFormatException("invalid bandwidth: \""
                    + s + "\" (illegal trailing decimal point)");
                }
            else
                {
                cb /= nDivisor;
                }
            }

        return cb;
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
        return toBandwidthString(cbps, true);
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
        boolean fBits = (cbps & 0xF00000000000000L) == 0L;

        if (fBits)
            {
            cbps <<= 3;
            }

        StringBuilder sb     = new StringBuilder(toMemorySizeString(cbps, fExact));
        int          ofLast = sb.length() - 1;
        if (sb.charAt(ofLast) == 'B')
            {
            if (fBits)
                {
                sb.setCharAt(ofLast, 'b');
                }
            }
        else
            {
            sb.append(fBits ? 'b' : 'B');
            }
        sb.append("ps");

        return sb.toString();
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
        return parseMemorySize(s, POWER_0);
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
        if (s == null)
            {
            throw new IllegalArgumentException("passed String must not be null");
            }

        switch (nDefaultPower)
            {
            case POWER_0:
            case POWER_K:
            case POWER_M:
            case POWER_G:
            case POWER_T:
                break;
            default:
                throw new IllegalArgumentException("illegal default power: "
                        + nDefaultPower);
            }

        // remove trailing "[K|k|M|m|G|g|T|t]?[B|b]?" and store it as a factor
        int cBitShift = POWER_0;
        int cch       = s.length();
        if (cch > 0)
            {
            boolean fDefault;
            char    ch = s.charAt(cch - 1);
            if (ch == 'B' || ch == 'b')
                {
                // bytes are implicit
                --cch;
                fDefault = false;
                }
            else
                {
                fDefault = true;
                }

            if (cch > 0)
                {
                switch (s.charAt(--cch))
                    {
                    case 'K': case 'k':
                        cBitShift = POWER_K;
                        break;

                    case 'M': case 'm':
                        cBitShift = POWER_M;
                        break;

                    case 'G': case 'g':
                        cBitShift = POWER_G;
                        break;

                    case 'T': case 't':
                        cBitShift = POWER_T;
                        break;

                    default:
                        if (fDefault)
                            {
                            cBitShift = nDefaultPower;
                            }
                        ++cch; // oops: shouldn't have chopped off the last char
                        break;
                    }
                }
            }

        // make sure that the string contains some digits
        if (cch == 0)
            {
            throw new NumberFormatException("passed String (\"" + s
                    + "\") must contain a number");
            }

        // extract the digits (decimal form) to assemble the base number
        long    cb       = 0;
        boolean fDecimal = false;
        int     nDivisor = 1;
        for (int of = 0; of < cch; ++of)
            {
            char ch = s.charAt(of);
            switch (ch)
                {
                case '0': case '1': case '2': case '3': case '4':
                case '5': case '6': case '7': case '8': case '9':
                    cb = (cb * 10) + (ch - '0');
                    if (fDecimal)
                        {
                        nDivisor *= 10;
                        }
                    break;

                case '.':
                    if (fDecimal)
                        {
                        throw new NumberFormatException("invalid memory size: \""
                            + s + "\" (illegal second decimal point)");
                        }
                    fDecimal = true;
                    break;

                default:
                    throw new NumberFormatException("invalid memory size: \""
                            + s + "\" (illegal digit: \"" + ch + "\")");
                }
            }

        cb <<= cBitShift;
        if (fDecimal)
            {
            if (nDivisor == 1)
                {
                throw new NumberFormatException("invalid memory size: \""
                    + s + "\" (illegal trailing decimal point)");
                }
            else
                {
                cb /= nDivisor;
                }
            }
        return cb;
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
        return toMemorySizeString(cb, true);
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
        if (cb < 0)
            {
            throw new IllegalArgumentException("negative quantity: " + cb);
            }

        if (cb < 1024)
            {
            return String.valueOf(cb);
            }

        int cDivs    = 0;
        int cMaxDivs = MEM_SUFFIX.length - 1;

        if (fExact)
            {
            // kilobytes? megabytes? gigabytes? terabytes?
            while (((((int) cb) & KB_MASK) == 0) && cDivs < cMaxDivs)
                {
                cb >>>= 10;
                ++cDivs;
                }
            return cb + MEM_SUFFIX[cDivs];
            }

        // need roughly the 3 most significant decimal digits
        int cbRem = 0;
        while (cb >= KB && cDivs < cMaxDivs)
            {
            cbRem = ((int) cb) & KB_MASK;
            cb >>>= 10;
            ++cDivs;
            }

        StringBuilder sb = new StringBuilder();
        sb.append(String.valueOf(cb));
        int cch = sb.length();
        if (cch < 3 && cbRem != 0)
            {
            // need the first digit or two of string value of cbRem / 1024;
            // format the most significant two digits ".xx" as a string "1xx"
            String sDec = String.valueOf((int) (cbRem / 10.24 + 100));
            sb.append('.')
              .append(sDec.substring(1, 1 + 3 - cch));
            }
        sb.append(MEM_SUFFIX[cDivs]);

        return sb.toString();
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
        return parseTime(s, UNIT_MS);
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
        return parseTimeNanos(s, nDefaultUnit) / 1000000L;
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
        return parseTimeNanos(s, UNIT_NS);
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
        if (s == null)
            {
            throw new IllegalArgumentException("passed String must not be null");
            }

        switch (nDefaultUnit)
            {
            case UNIT_NS:
            case UNIT_US:
            case UNIT_MS:
            case UNIT_S:
            case UNIT_M:
            case UNIT_H:
            case UNIT_D:
                break;
            default:
                throw new IllegalArgumentException("illegal default unit: "
                        + nDefaultUnit);
            }

        // remove trailing "[NS|ns|US|us|MS|ms|S|s|M|m|H|h|D|d]?" and store it as a factor
        long nMultiplier = nDefaultUnit;
        int cch = s.length();
        if (cch > 0)
            {
            switch (s.charAt(--cch))
                {
                case 'S': case 's':
                    nMultiplier = UNIT_S;
                    if (cch > 1)
                        {
                        char c = s.charAt(cch - 1);
                        switch (c)
                            {
                            case 'N': case 'n':
                                --cch;
                                nMultiplier = UNIT_NS;
                                break;
                            case 'U': case 'u':
                                --cch;
                                nMultiplier = UNIT_US;
                                break;
                            case 'M': case 'm':
                                --cch;
                                nMultiplier = UNIT_MS;
                                break;
                            }
                        }
                    break;

                case 'M': case 'm':
                    nMultiplier = UNIT_M;
                    break;

                case 'H': case 'h':
                    nMultiplier = UNIT_H;
                    break;

                case 'D': case 'd':
                    nMultiplier = UNIT_D;
                    break;

                default:
                    ++cch; // oops: shouldn't have chopped off the last char
                    break;
                }
            }

        // convert multiplier into nanos
        nMultiplier = nMultiplier < 0 ? 1000000L / -nMultiplier
                                      : 1000000L * nMultiplier;

        // make sure that the string contains some digits
        if (cch == 0)
            {
            throw new NumberFormatException("passed String (\"" + s
                    + "\") must contain a number");
            }

        // extract the digits (decimal form) to assemble the base number
        long    cNanos   = 0;
        boolean fDecimal = false;
        int     nDivisor = 1;
        for (int of = 0; of < cch; ++of)
            {
            char ch = s.charAt(of);
            switch (ch)
                {
                case '0': case '1': case '2': case '3': case '4':
                case '5': case '6': case '7': case '8': case '9':
                    cNanos = (cNanos * 10) + (ch - '0');
                    if (fDecimal)
                        {
                        nDivisor *= 10;
                        }
                    break;

                case '.':
                    if (fDecimal)
                        {
                        throw new NumberFormatException("invalid time: \""
                            + s + "\" (illegal second decimal point)");
                        }
                    fDecimal = true;
                    break;

                default:
                    throw new NumberFormatException("invalid time: \""
                            + s + "\" (illegal digit: \"" + ch + "\")");
                }
            }

        cNanos *= nMultiplier;
        if (fDecimal)
            {
            if (nDivisor == 1)
                {
                throw new NumberFormatException("invalid time: \""
                    + s + "\" (illegal trailing decimal point)");
                }
            else
                {
                cNanos /= nDivisor;
                }
            }
        return cNanos;
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
        return ldt == 0L ? "none" : new Timestamp(ldt).toString();
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
        int ofPct = s.indexOf('%');
        if (ofPct == -1)
            {
            throw new IllegalArgumentException("The parameter " + s + " does not contain a percentage value.");
            }
        int percent = Integer.parseInt(s.substring(0, ofPct));
        if (percent > 100 || percent < 0)
            {
            throw new IllegalArgumentException("Not a percentage value between 0 - 100:" + s);
            }
        return percent / 100f;
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
        return o == null ? 0 : o.hashCode();
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
        if (o1 == o2)
            {
            return true;
            }

        if (o1 == null || o2 == null)
            {
            return false;
            }

        try
            {
            return o1.equals(o2);
            }
        catch (RuntimeException e)
            {
            return false;
            }
        }

    /**
     * Deeply compare two references for equality. This dives down into
     * arrays, including nested arrays.
     *
     * @param o1  an object
     * @param o2  an ojbect to be compared with o1 for deeply equality
     *
     * @return  true if deeply equal, false otherwise
     */
    public static boolean equalsDeep(Object o1, Object o2)
        {
        if (o1 == o2)
            {
            return true;
            }

        if (o1 == null || o2 == null)
            {
            return false;
            }

        if (o1.getClass().isArray())
            {
            // the following are somewhat in order of likelihood

            if (o1 instanceof byte[])
                {
                return o2 instanceof byte[]
                    && Arrays.equals((byte[]) o1, (byte[]) o2);
                }

            if (o1 instanceof Object[])
                {
                if (o2 instanceof Object[])
                    {
                    Object[] ao1 = (Object[]) o1;
                    Object[] ao2 = (Object[]) o2;
                    int c = ao1.length;
                    if (c == ao2.length)
                        {
                        for (int i = 0; i < c; ++i)
                            {
                            if (!equalsDeep(ao1[i], ao2[i]))
                                {
                                return false;
                                }
                            }
                        return true;
                        }
                    }

                return false;
                }

            if (o1 instanceof int[])
                {
                return o2 instanceof int[]
                    && Arrays.equals((int[]) o1, (int[]) o2);
                }

            if (o1 instanceof char[])
                {
                return o2 instanceof char[]
                    && Arrays.equals((char[]) o1, (char[]) o2);
                }

            if (o1 instanceof long[])
                {
                return o2 instanceof long[]
                    && Arrays.equals((long[]) o1, (long[]) o2);
                }

            if (o1 instanceof double[])
                {
                return o2 instanceof double[]
                    && Arrays.equals((double[]) o1, (double[]) o2);
                }

            if (o1 instanceof boolean[])
                {
                return o2 instanceof boolean[]
                    && Arrays.equals((boolean[]) o1, (boolean[]) o2);
                }

            if (o1 instanceof short[])
                {
                return o2 instanceof short[]
                    && Arrays.equals((short[]) o1, (short[]) o2);
                }

            if (o1 instanceof float[])
                {
                return o2 instanceof float[]
                    && Arrays.equals((float[]) o1, (float[]) o2);
                }
            }

        try
            {
            return o1.equals(o2);
            }
        catch (RuntimeException e)
            {
            return false;
            }
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
        return toCrc(ab, 0, ab.length);
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
        return toCrc(ab, of, cb, 0xFFFFFFFF);
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
        while (cb > 0)
            {
            nCrc = (nCrc >>> 8) ^ CRC32_TABLE[(nCrc ^ ab[of++]) & 0xFF];
            --cb;
            }
        return nCrc;
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
        return toCrc(seq, 0, seq.length(), 0xFFFFFFFF);
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
        while (cb > 0)
            {
            nCrc = (nCrc >>> 8) ^ CRC32_TABLE[(nCrc ^ seq.byteAt(of++)) & 0xFF];
            --cb;
            }
        return nCrc;
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
        Method methodUptime = s_methodUptime;
        if (methodUptime == null)
            {
            return System.currentTimeMillis() - s_ldtStartTime;
            }

        try
            {
            return ((Long) methodUptime.invoke(s_oRuntimeMXBean)).longValue();
            }
        catch (Throwable e)
            {
            if (e instanceof InvocationTargetException)
                {
                e = ((InvocationTargetException) e).getTargetException();
                }
            throw ensureRuntimeException(e);
            }
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
        return s_safeClock.getSafeTimeMillis(System.currentTimeMillis());
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
        return s_safeClock.getLastSafeTimeMillis();
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
        if (ldtTimeout == Long.MAX_VALUE)
            {
            return 0;
            }

        long ldtNow = getSafeTimeMillis();
        return ldtTimeout == ldtNow ? -1 : ldtTimeout - ldtNow;
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
        TimeZone timeZone = (TimeZone) s_mapTimeZones.get(sId);
        if (timeZone == null)
            {
            timeZone = TimeZone.getTimeZone(sId);
            s_mapTimeZones.put(sId, timeZone);
            }
        return timeZone;
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
        return Integer.valueOf(n);
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
        return Long.valueOf(n);
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
        return s_procRand;
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
        Random rnd = s_rnd;

        if (rnd == null)
            {
            // double-check locking is not required to work; the worst that
            // can happen is that we create a couple extra Random objects
            synchronized (Random.class)
                {
                rnd = s_rnd;
                if (rnd == null)
                    {
                    rnd = new Random();

                    // spin the seed a bit
                    long lStop = getSafeTimeMillis() + 31 + rnd.nextInt(31);
                    long cMin  = 1021 + rnd.nextInt(Math.max(1, (int) (lStop % 1021)));
                    while (getSafeTimeMillis() < lStop || --cMin > 0)
                        {
                        cMin += rnd.nextBoolean() ? 1 : -1;
                        rnd.setSeed(rnd.nextLong());
                        }

                    // spin the random until the clock ticks again
                    long lStart = getSafeTimeMillis();
                    do
                        {
                        if (rnd.nextBoolean())
                            {
                            if ((rnd.nextLong() & 0x01L) == (getSafeTimeMillis() & 0x01L))
                                {
                                rnd.nextBoolean();
                                }
                            }
                        }
                    while (getSafeTimeMillis() == lStart);

                    s_rnd = rnd;
                    }
                }
            }

        return rnd;
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
    public static List randomize(Collection coll)
        {
        return new ImmutableArrayList(randomize(coll.toArray()));
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
        int c;
        if (a == null || (c = a.length) <= 1)
            {
            return a;
            }

        Random rnd = getRandom();
        for (int i1 = 0; i1 < c; ++i1)
            {
            int i2 = rnd.nextInt(c);

            // swap i1, i2
            Object o = a[i2];
            a[i2] = a[i1];
            a[i1] = o;
            }

        return a;
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
        int c;
        if (a == null || (c = a.length) <= 1)
            {
            return a;
            }

        Random rnd = getRandom();
        for (int i1 = 0; i1 < c; ++i1)
            {
            int i2 = rnd.nextInt(c);

            // swap i1, i2
            int n = a[i2];
            a[i2] = a[i1];
            a[i1] = n;
            }

        return a;
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
        return getRandomBinary(cbMin, cbMax, (byte[]) null);
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
        assert cbMin >= 0;
        assert cbMax >= cbMin;

        Random rnd    = getRandom();
        int    cbDif  = cbMax - cbMin;
        int    cbHead = abHead == null ? 0 : abHead.length;
        int    cb     = (cbDif <= 0 ? cbMax : cbMin + rnd.nextInt(cbDif)) + cbHead;
        byte[] ab     = new byte[cb];

        rnd.nextBytes(ab);

        if (cbHead > 0)
            {
            System.arraycopy(abHead, 0, ab, 0, cbHead);
            }
        return new Binary(ab);
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
        assert cchMin >= 0;
        assert cchMax >= cchMin;

        Random rnd    = getRandom();
        int    cchDif = cchMax - cchMin;
        int    cch    = cchDif <= 0 ? cchMax : cchMin + rnd.nextInt(cchDif);

        if (fAscii)
            {
            byte[] ab = new byte[cch];
            rnd.nextBytes(ab);
            for (int of = 0; of < cch; ++of)
                {
                int b = ab[of] & 0x7F;
                if (b < 0x20)
                    {
                    b = 0x20 + rnd.nextInt(0x7F - 0x20);
                    }
                ab[of] = (byte) b;
                }
            return new String(ab, 0);
            }
        else
            {
            char[] ach    = new char[cch];
            int    nLimit = Character.MAX_VALUE + 1;
            for (int of = 0; of < cch; ++of)
                {
                ach[of] = (char) rnd.nextInt(nLimit);
                }

            return new String(ach);
            }
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
        final int MAX  = ab.length;
        int       cb   = 0;
        boolean   fEOF = false;
        while (!fEOF && cb < MAX)
            {
            int cbBlock = stream.read(ab, cb, MAX - cb);
            if (cbBlock < 0)
                {
                fEOF = true;
                }
            else
                {
                cb += cbBlock;
                }

            }
        return cb;
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
        final int             BLOCK     = 1024;
        byte[]                ab        = new byte[BLOCK];
        ByteArrayOutputStream streamBuf = new ByteArrayOutputStream(BLOCK);
        while (true)
            {
            try
                {
                int cb = stream.read(ab, 0, BLOCK);
                if (cb < 0)
                    {
                    break;
                    }
                else if (cb > 0)
                    {
                    streamBuf.write(ab, 0, cb);
                    }
                }
            catch (EOFException e)
                {
                break;
                }
            }
        stream.close();
        return streamBuf.toByteArray();
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
        if (stream instanceof ReadBuffer.BufferInput)
            {
            ReadBuffer.BufferInput in  = (ReadBuffer.BufferInput) stream;
            ReadBuffer             buf = in.getBuffer();

            if (buf != null)
                {
                int of = in.getOffset();
                return in.getBuffer().toByteArray(of, buf.length() - of);
                }
            }
        if (stream instanceof InputStream)
            {
            return read((InputStream) stream);
            }

        final int BLOCK = 1024;
        int       cb    = 0;
        byte[]    ab    = new byte[BLOCK];
        ByteArrayOutputStream streamBuf = null;
        try
            {
            while (true)
                {
                ab[cb++] = stream.readByte();
                if (cb == BLOCK)
                    {
                    if (streamBuf == null)
                        {
                        streamBuf = new ByteArrayOutputStream(BLOCK);
                        }
                    streamBuf.write(ab, 0, cb);
                    cb = 0;
                    }
                }
            }
        catch (EOFException e)
            {
            // end of input reached; eat it
            }

        if (streamBuf == null)
            {
            // contents fit in first block
            if (cb == BLOCK)
                {
                // perfect fit
                return ab;
                }
            // shrink block and return
            byte[] abNew = new byte[cb];
            System.arraycopy(ab, 0, abNew, 0, cb);
            return abNew;
            }

        // contents span multiple blocks
        if (cb != 0)
            {
            // copy remainder into streamBuf
            streamBuf.write(ab, 0, cb);
            }
        return streamBuf.toByteArray();
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
        // this method resolves the ambiguity between the read(InputStream)
        // and read(DataInput) methods for DataInputStreams and its derivatives
        return read((InputStream) stream);
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
        final int       BLOCK  = 1024;
        char[]          ach  = new char[BLOCK];
        CharArrayWriter writer = new CharArrayWriter(BLOCK);
        while (true)
            {
            try
                {
                int cch = reader.read(ach, 0, BLOCK);
                if (cch < 0)
                    {
                    break;
                    }
                else if (cch > 0)
                    {
                    writer.write(ach, 0, cch);
                    }
                }
            catch (EOFException e)
                {
                break;
                }
            }
        reader.close();
        return writer.toString();
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
        if (file == null || !file.exists() || !file.isFile())
            {
            return null;
            }

        long cbFile = file.length();
        azzert(cbFile < 0x7FFFFFFFL);

        int    cb = (int) cbFile;
        byte[] ab = new byte[cb];

        InputStream in = new FileInputStream(file);
        try
            {
            int cbRead = read(in, ab);
            azzert(cb == cbRead);
            }
        finally
            {
            try
                {
                in.close();
                }
            catch (Exception e) {}
            }

        return ab;
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
        if (url == null)
            {
            return null;
            }

        URLConnection con = url.openConnection();
        int           cb  = con.getContentLength();
        byte[]        ab  = null;
        InputStream   in  = con.getInputStream();
        try
            {
            if (cb == -1)
                {
                // unknown content length
                ab = read(in);
                }
            else
                {
                // known content length (optimize by pre-allocating fully)
                ab = new byte[cb];
                int cbRead = read(in, ab);
                azzert(cb == cbRead);
                }
            }
        finally
            {
            try
                {
                in.close();
                }
            catch (Exception e) {}
            }

        return ab;
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
        * Construct a PrintWriter that logs using the
        * {@link CacheFactory#log} method.
        *
        * @param nSev  the severity to log messages with
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
        private int m_nSev;
        }


    // ----- constants ------------------------------------------------------

    /**
     * Hex digits.
     */
    private static final char[] HEX = "0123456789ABCDEF".toCharArray();

    /**
     * Memory size constants.
     */
    private static final int      KB         = 1 << 10;
    private static final int      KB_MASK    = KB - 1;
    private static final String[] MEM_SUFFIX = {"", "KB", "MB", "GB", "TB"};

    /**
     * CRC32 constants.
     */
    private static final int   CRC32_BASE  = 0xEDB88320;
    private static final int[] CRC32_TABLE = new int[256];
    static
        {
        for (int i = 0, c = CRC32_TABLE.length; i < c; ++i)
            {
            int nCrc = i;
            for (int n = 0; n < 8; ++n)
                {
                if ((nCrc & 1) == 1)
                    {
                    nCrc = (nCrc >>> 1) ^ CRC32_BASE;
                    }
                else
                    {
                    nCrc >>>= 1;
                    }
                }
            CRC32_TABLE[i] = nCrc;
            }
        }


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
     * As of Coherence 3.2, the default logging level is 5, so using the level
     * of 5 will show up in the logs by default as a debug message.
     */
    public static final int LOG_DEBUG = 5;

    /**
     * As of Coherence 3.2, the default logging level is 5, so using a level
     * higher than 5 will be "quiet" by default, meaning that it will not show
     * up in the logs unless the configured logging level is increased.
     */
    public static final int LOG_QUIET = 6;


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

    /**
     * The java.lang.RuntimeMXBean or null if not available.
     */
    private static final Object s_oRuntimeMXBean;

    /**
     * RuntimeMXBean.getUpTime() Method or null if not available.
     */
    private static final Method s_methodUptime;

    static
        {
        Object oRuntimeMXBean = null;
        Method methodUptime   = null;

        try
            {
            oRuntimeMXBean = Class.forName(
                    "java.lang.management.ManagementFactory").
                    getMethod("getRuntimeMXBean").invoke(null);
            if (oRuntimeMXBean != null)
                {
                methodUptime = Class.forName(
                        "java.lang.management.RuntimeMXBean").
                        getMethod("getUptime");
                }
            }
        catch (Throwable eIgnore) {}

        s_oRuntimeMXBean = oRuntimeMXBean;
        s_methodUptime   = methodUptime;
        }

    /**
     * The estimated JVM start time.
     */
    private static final long s_ldtStartTime = System.currentTimeMillis();

    /**
     * The map of cached {@link TimeZone}s keyed by ID.
     */
    private static Map s_mapTimeZones = new SafeHashMap();

    /**
     * The shared SafeClock.
     */
    private static SafeClock s_safeClock = new SafeClock(s_ldtStartTime);

    /**
     * A lazily-instantiated shared Random object.
     */
    private static Random s_rnd;

    /**
     * Single random value held for the life of the process.
     */
    private static final int s_procRand = ThreadLocalRandom.current().nextInt();

    /**
    * The configured ThreadFactory.
    */
    private static ThreadFactory s_threadFactory = instantiateThreadFactory();

    /**
     * Initialize logging through Coherence
     */
    static
        {
        setOut(new LoggingWriter(LOG_ALWAYS));
        setErr(new LoggingWriter(LOG_ERR));
        setLog(new LoggingWriter(LOG_INFO));
        setLogEcho(false);
        }
    }
