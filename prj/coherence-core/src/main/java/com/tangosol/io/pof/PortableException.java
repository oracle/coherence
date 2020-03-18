/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io.pof;


import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;

import java.util.ArrayList;


/**
* A PortableException is an exception that allows information about a remote
* exception or error to be serialized and deserialized to/from a POF stream.
*
* @author jh,mf  2006.08.04
*/
public class PortableException
        extends RuntimeException
        implements PortableObject
    {
    // ----- constructors -----------------------------------------------------

    /**
    * Default constructor.
    */
    protected PortableException()
        {
        m_sName = getClass().getName();
        }

    /**
    * Constructs a PortableException with the specified detail message.
    *
    * @param sMessage the String that contains a detailed message
    */
    protected PortableException(String sMessage)
        {
        super(sMessage);

        m_sName    = getClass().getName();
        m_sMessage = sMessage;
        }

    /**
    * Construct a PortableException from a Throwable object.
    *
    * @param e  the Throwable object
    */
    protected PortableException(Throwable e)
        {
        super(e);

        m_sName = getClass().getName();
        }

    /**
    * Construct a PortableException from a Throwable object and an additional
    * description.
    *
    * @param sMessage  the additional description
    * @param e         the Throwable object
    */
    protected PortableException(String sMessage, Throwable e)
        {
        super(sMessage, e);

        m_sName    = getClass().getName();
        m_sMessage = sMessage;
        }


    // ----- PortableException methods ----------------------------------------

    /**
    * Return the name of the exception.
    *
    * @return the name of the exception
    */
    public String getName()
        {
        Class  clazz = getClass();
        String sName = m_sName;

        if (sName != null && clazz == PortableException.class)
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
        String[] asStackRemote = m_asStackRemote;
        Object[] aoStackLocal  = getStackTrace();
        int      cLocal        = aoStackLocal.length;
        int      ofLocal;
        String[] asStackFull;

        if (asStackRemote == null)
            {
            asStackFull = new String[cLocal];
            ofLocal = 0;
            }
        else
            {
            int cRemote = asStackRemote.length;
            asStackFull = new String[cRemote + cLocal + 1];
            System.arraycopy(asStackRemote, 0, asStackFull, 0, cRemote);

            asStackFull[cRemote] = "at <process boundary>";
            ofLocal = cRemote + 1;
            }

        for (int i = 0; i < cLocal; ++i)
            {
            asStackFull[ofLocal + i] = "at " + aoStackLocal[i];
            }

        return asStackFull;
        }


    // ----- PortableObject methods -------------------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(PofReader in)
            throws IOException
        {
        m_sName         = in.readString(0);
        m_sMessage      = in.readString(1);
        m_asStackRemote = (String[]) in.readCollection(2, new ArrayList(64)).
                toArray(new String[0]);
        initCause((Throwable) in.readObject(3));
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(PofWriter out)
            throws IOException
        {
        ThrowablePofSerializer.writeThrowable(out, this);
        }


    // ----- Throwable methods ------------------------------------------------

    /**
    * Return the detail message string of this PortableException.
    *
    * @return  the detail message string (may be null)
    */
    public String getMessage()
        {
        return m_sMessage;
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

            for (int i = 0, c = asStackFull.length; i < c; ++i)
                {
                writer.println("\t" + asStackFull[i]);
                }

            Throwable tCause = getCause();
            if (tCause != null)
                {
                writer.print("Caused by: ");
                tCause.printStackTrace(writer);
                }
            }
        }


    // ----- Object methods ---------------------------------------------------

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


    // ----- data members -----------------------------------------------------

    /**
    * The exception's name.
    */
    protected String m_sName;

    /**
    * The exception's message.
    */
    protected String m_sMessage;

    /**
    * A raw representation of the remote stack trace for this exception.
    */
    protected String[] m_asStackRemote;
    }
