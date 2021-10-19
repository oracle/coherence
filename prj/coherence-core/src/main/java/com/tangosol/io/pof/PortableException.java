/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io.pof;


import com.tangosol.io.SerializationSupport;

import com.tangosol.util.Base;
import com.tangosol.util.ClassHelper;

import java.io.IOException;
import java.io.ObjectStreamException;
import java.io.PrintStream;
import java.io.PrintWriter;

import java.lang.reflect.InvocationTargetException;

import java.util.ArrayList;


/**
* A {@code PortableException} is an exception that allows information about a remote
* exception or error to be serialized and deserialized to/from a {@code POF} stream.
*
* @author jh,mf  2006.08.04
*/
public class PortableException
        extends RuntimeException
        implements PortableObject, SerializationSupport
    {
    /**
    * Bug 33456075 This field was added to fix the backwards compatibility issue when serializing exceptions
    * to or from Coherence versions older than 12.2.1-4-5
    */
    static final long serialVersionUID = -9223304018856031609L;

    // ----- constructors -----------------------------------------------------

    /**
    * Default constructor.
    */
    protected PortableException()
        {
        m_sName = getClass().getName();
        }

    /**
    * Constructs a {@code PortableException} with the specified detail message.
    *
    * @param sMessage  the String that contains a detailed message
    */
    protected PortableException(String sMessage)
        {
        super(sMessage);

        m_sName    = getClass().getName();
        m_sMessage = sMessage;
        }

    /**
    * Construct a {@code PortableException} from a {@link Throwable} object.
    *
    * @param e  the {@link Throwable} object
    */
    protected PortableException(Throwable e)
        {
        super(e);

        m_sName = getClass().getName();
        }

    /**
    * Construct a {@code PortableException} from a {@link Throwable} object and an additional
    * description.
    *
    * @param sMessage  the additional description
    * @param e         the {@link Throwable} object
    */
    protected PortableException(String sMessage, Throwable e)
        {
        super(sMessage, e);

        m_sName    = getClass().getName();
        m_sMessage = sMessage;
        }

    // ----- SerializationSupport methods -----------------------------------

    /**
    * Returns the original type, iff the type is a subclass of {@code PortableException},
    * during deserialization opposed to a new {@code PortableException} with the metadata of the original exception.
    *
    * @return a reconstructed {@code PortableException} subclass, if possible, otherwise returns the current
    *         instance
    *
    * @throws ObjectStreamException if an error occurs
    */
    @Override
    public Object readResolve()
            throws ObjectStreamException
        {
        Class<?> clzOriginalType;
        try
            {
            clzOriginalType = Class.forName(m_sName, false, Base.getContextClassLoader());
            }
        catch (ClassNotFoundException e)
            {
            return this;
            }

        if (!PortableException.class.equals(clzOriginalType) &&
                    PortableException.class.isAssignableFrom(clzOriginalType))
                {
                try
                    {
                    return ClassHelper.newInstance(clzOriginalType, new Object[] {m_sMessage, getCause()});
                    }
                catch (InstantiationException | InvocationTargetException e)
                    {
                    return this;
                    }
                }

        return this;
        }

    // ----- PortableException methods ----------------------------------------

    /**
    * Return the name of the exception.
    *
    * @return the name of the exception
    */
    public String getName()
        {
        Class<? extends PortableException> clazz = getClass();
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

    @Override
    public void readExternal(PofReader in)
            throws IOException
        {
        m_sName         = in.readString(0);
        m_sMessage      = in.readString(1);
        m_asStackRemote = in.readCollection(2, new ArrayList<String>(64)).
                toArray(new String[0]);
        initCause(in.readObject(3));
        }

    @Override
    public void writeExternal(PofWriter out)
            throws IOException
        {
        ThrowablePofSerializer.writeThrowable(out, this);
        }


    // ----- Throwable methods ------------------------------------------------

    /**
    * Return the detail message string of this PortableException.
    *
    * @return the detail message string (may be {@code null})
    */
    public String getMessage()
        {
        return m_sMessage;
        }

    /**
    * Print this {@code PortableException} and its stack trace to the specified stream.
    *
    * @param stream  the {@link PrintStream} to use for the output
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
    * Print this {@code PortableException} and its stack trace to the specified writer.
    *
    * @param writer  the {@link PrintWriter} to use for the output
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
