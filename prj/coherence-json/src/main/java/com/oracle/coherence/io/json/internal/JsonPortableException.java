/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.io.json.internal;

import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * A {@link JsonPortableException} is an exception that allows information about a remote
 * exception or error to be serialized and deserialized to/from a JSON stream.
 *
 * @author Jonathan Knight  2018.11.28
 * @since 14.1.2
 */
public class JsonPortableException
        extends RuntimeException
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a {@link JsonPortableException}.
     *
     * @param sName     the name of the serialized throwable class
     * @param sMessage  the message (if any)
     * @param asStack   the stack trace
     *
     * @throws NullPointerException      if {@code sName} or {@code asStack} are {@code null}
     * @throws IllegalArgumentException  if {@code sName} or {@code asStack} are zero-length
     */
    JsonPortableException(String sName, String sMessage, String[] asStack)
        {
        if (sName == null)
            {
            throw new NullPointerException("no name specified");
            }
        if (sName.isEmpty())
            {
            throw new IllegalArgumentException("sName is zero-length");
            }
        if (asStack == null)
            {
            throw new NullPointerException("no stack frames specified");
            }
        if (asStack.length == 0)
            {
            throw new IllegalArgumentException("asStack has a zero length");
            }

        this.f_sName    = sName;
        this.f_sMessage = sMessage;
        this.f_asStack  = asStack;
        }

    // ----- public methods -------------------------------------------------

    /**
     * Return the name of the exception.
     *
     * @return the name of the exception
     */
    public String getName()
        {
        Class<?> clazz = getClass();
        String   sName = f_sName;

        if (sName != null)
            {
            String sPrefix = "Portable(";

            if (sName.startsWith(sPrefix))
                {
                return sName;
                }
            return sPrefix + sName + ')';
            }

        return clazz.getName();
        }

    /**
     * Return an array of Strings containing the full representation of the
     * stack trace.  The first element of the stack represents the exception's
     * point of origin.
     *
     * @return the full stack trace
     */
    public String[] getFullStackTrace()
        {
        String[] asStackRemote = f_asStack;
        Object[] aoStackLocal  = getStackTrace();
        int      cLocal        = aoStackLocal.length;
        int      ofLocal;
        String[] asStackFull;

        if (asStackRemote == null)
            {
            asStackFull = new String[cLocal];
            ofLocal     = 0;
            }
        else
            {
            int cRemote = asStackRemote.length;

            asStackFull = new String[cRemote + cLocal + 1];

            for (int i = 0; i < cRemote; i++)
                {
                asStackFull[i] = "at " + asStackRemote[i];
                }

            asStackFull[cRemote] = "at <process boundary>";
            ofLocal              = cRemote + 1;
            }

        for (int i = 0; i < cLocal; ++i)
            {
            asStackFull[ofLocal + i] = "at " + aoStackLocal[i];
            }

        return asStackFull;
        }

    /**
     * Return the detail message string of this PortableException.
     *
     * @return the detail message string (may be null)
     */
    public String getMessage()
        {
        return f_sMessage;
        }

    /**
     * Print this PortableException and its stack trace to the specified stream.
     *
     * @param stream the PrintStream to use for the output
     */
    public void printStackTrace(PrintStream stream)
        {
        synchronized (stream)
            {
            PrintWriter pw = new PrintWriter(stream);
            printStackTrace(pw);
            pw.flush();
            }
        }

    /**
     * Print this PortableException and its stack trace to the specified writer.
     *
     * @param writer the PrintWriter to use for the output
     */
    public void printStackTrace(PrintWriter writer)
        {
        synchronized (writer)
            {
            writer.println(this);
            String[] asStackFull = getFullStackTrace();

            for (String s : asStackFull)
                {
                writer.println("\t" + s);
                }

            Throwable tCause = getCause();
            if (tCause != null)
                {
                writer.print("Caused by: ");
                tCause.printStackTrace(writer);
                }
            }
        }

    /**
     * Return a human-readable description for this exception.
     *
     * @return a String description of the PortableException
     */
    public String toString()
        {
        String sPrefix  = getName();
        String sMessage = getMessage();

        return sMessage == null ? sPrefix : sPrefix + ": " + sMessage;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The exception's name.
     */
    protected final String f_sName;

    /**
     * The exception's message.
     */
    protected final String f_sMessage;

    /**
     * A raw representation of the remote stack trace for this exception.
     */
    protected final String[] f_asStack;
    }
