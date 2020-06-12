/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.base;

import java.io.BufferedReader;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.Writer;

import java.math.BigDecimal;
import java.math.BigInteger;

import java.util.ArrayList;

import static com.oracle.coherence.common.base.Logger.out;

/**
 * Class for providing StackTrace functionality.
 *
 * @author cp  2000.08.02
 * @since Coherence 14.1.2
 */
public abstract class StackTrace
    {
    // ----- stack trace support ----------------------------------------------

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
                sStack.substring(sStack.indexOf('\n', sStack.lastIndexOf(".getStackFrames(")) + 1)));

        try
            {
            ArrayList<StackFrame> list = new ArrayList<>();
            String sLine = reader.readLine();
            while (sLine != null && sLine.length() > 0)
                {
                StackFrame frame = StackFrame.UNKNOWN;
                try
                    {
                    frame = new StackFrame(sLine);
                    }
                catch (RuntimeException ignore)
                    {
                    }

                list.add(frame);
                sLine = reader.readLine();
                }
            return list.toArray(new StackFrame[0]);
            }
        catch (IOException e)
            {
            throw new RuntimeException(e);
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
     * @param e a Throwable object that contains stack trace information
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
     * @param t      the throwable
     * @param sDelim the delimiter to include between messages
     * @return the concatenated messages
     */
    public static String getDeepMessage(Throwable t, String sDelim)
        {
        StringBuilder sb = new StringBuilder();

        String sMsgLast = null;
        for (; t != null; t = t.getCause())
            {
            String sMsg = t.getMessage();
            if (!Objects.equals(sMsgLast, sMsg))
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
     * @param e a Throwable object that contains stack trace information
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
            throw new RuntimeException(eIO);
            }
        }

    /**
     * Ensure the specified Number is a BigDecimal value or convert it into a
     * new BigDecimal object.
     *
     * @param num a Number object
     * @return a BigDecimal object that is equal to the passed in Number
     */
    public static BigDecimal ensureBigDecimal(Number num)
        {
        return num instanceof BigDecimal
               ? (BigDecimal) num
               : num instanceof BigInteger
                 ? new BigDecimal((BigInteger) num)
                 : BigDecimal.valueOf(num.doubleValue());
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
         * @param sExcept a line of a stack trace in the format used by the
         *                reference implementation of the JVM
         */
        public StackFrame(String sExcept)
            {
            try
                {
                int of = sExcept.indexOf('(');
                String sLeft = sExcept.substring(sExcept.lastIndexOf(' ', of) + 1, of);
                String sRight = sExcept.substring(of + 1, sExcept.lastIndexOf(')'));

                of = sLeft.lastIndexOf('.');
                String sClass = sLeft.substring(0, of);
                String sMethod = sLeft.substring(of + 1);

                String sFile = "unknown";
                int nLine = 0;

                of = sRight.lastIndexOf(':');
                if (of >= 0)
                    {
                    sFile = sRight.substring(0, of);

                    try
                        {
                        nLine = Integer.parseInt(sRight.substring(of + 1));
                        }
                    catch (RuntimeException ignore)
                        {
                        }
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
         * @param sFile   the source file name (e.g. Test.java)
         * @param sClass  the fully qualified class name (e.g. pkg.Test$1)
         * @param sMethod the method name (e.g. main)
         * @param nLine   the line number (e.g. 17) or 0 if unknown
         */
        public StackFrame(String sFile, String sClass, String sMethod, int nLine)
            {
            init(sFile, sClass, sMethod, nLine);
            }

        /**
         * Initialize the fields of the StackFrame object.
         *
         * @param sFile   the source file name (e.g. Test.java)
         * @param sClass  the fully qualified class name (e.g. pkg.Test$1)
         * @param sMethod the method name (e.g. main)
         * @param nLine   the line number (e.g. 17) or 0 if unknown
         */
        protected void init(String sFile, String sClass, String sMethod, int nLine)
            {
            m_sFile = sFile;
            m_sClass = sClass;
            m_sMethod = sMethod;
            m_nLine = nLine;
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
                int of = sClass.indexOf('$');
                if (of >= 0)
                    {
                    sClass = sClass.substring(0, of);
                    }

                InputStream stream = Class.forName(sClass, false,
                        Classes.getContextClassLoader())
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
            catch (Throwable ignore)
                {
                }

            return null;
            }

        /**
         * @return a String representation of the StackFrame
         */
        public String toString()
            {
            int nLine = getLineNumber();
            String sLine = nLine == 0 ? "?" : "" + nLine;

            return getClassName() + '.' + getMethodName() + '('
                           + getFileName() + ':' + sLine + ')';
            }

        /**
         * @return a short String representation of the StackFrame
         */
        @SuppressWarnings("unused")
        public String toShortString()
            {
            int nLine = getLineNumber();
            String sLine = nLine == 0 ? "?" : "" + nLine;

            return getShortClassName() + '.' + getMethodName()
                           + " [" + sLine + ']';
            }

        public static final StackFrame UNKNOWN = new StackFrame(
                                                                       "unknown", "unknown", "unknown", 0);

        private String m_sFile;
        private String m_sClass;
        private String m_sMethod;
        private int m_nLine;
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
    public static String getExpression(String sMethod)
        {
        StackTrace.StackFrame[] aFrames = getStackFrames();
        int i = 0;

        // find method in call stack
        while (!aFrames[i].getMethodName().equals(sMethod))
            {
            ++i;
            }

        // find calling method
        while (aFrames[i].getMethodName().equals(sMethod))
            {
            ++i;
            }

        // get source line
        String sLine = aFrames[i].getLine();
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
    }
