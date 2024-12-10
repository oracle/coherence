/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io.pof;


import com.tangosol.util.ImmutableArrayList;

import java.io.IOException;


/**
* {@link PofSerializer} implementation that can serialize and deserialize a
* {@link Throwable} to/from a {@code POF} stream.
* <p>
* This {@link PofSerializer serializer} provides a catch-all mechanism for serializing exceptions.
* Any deserialized exception will loose type information, and simply be
* represented as a {@link PortableException}.  The basic detail information of the
* exception is retained.
* <p>
* {@link PortableException} and this class work asymmetrically to provide the
* serialization routines for exceptions.
*
* @author mf  2008.08.25
*/
public class ThrowablePofSerializer
        implements PofSerializer
    {
    // ----- constructors -----------------------------------------------------

    /**
    * Default constructor.
    */
    public ThrowablePofSerializer()
        {
        }


    // ----- PofSerializer interface ------------------------------------------

    @Override
    public void serialize(PofWriter out, Object o)
            throws IOException
        {
        writeThrowable(out, (Throwable) o);
        out.writeRemainder(null);
        }

    @Override
    public Object deserialize(PofReader in)
            throws IOException
        {
        PortableException e = new PortableException();
        in.registerIdentity(e);
        e.readExternal(in);
        in.readRemainder();
        return e;
        }


    // ----- helpers ----------------------------------------------------------

    /**
    * Write the Throwable to the specified stream.
    *
    * @param out  the stream to write to
    * @param t    the Throwable to write
    */
    static void writeThrowable(PofWriter out, Throwable t)
            throws IOException
        {
        String   sName;
        String[] asTrace;

        if (t instanceof PortableException)
            {
            PortableException e = (PortableException) t;
            sName   = e.getName();
            asTrace = e.getFullStackTrace();
            }
        else
            {
            Object[] aoTrace = t.getStackTrace();

            sName   = t.getClass().getName();
            asTrace = new String[aoTrace.length];

            for (int i = 0, c = aoTrace.length; i < c; ++i)
                {
                asTrace[i] = "at " + aoTrace[i];
                }
            }

        out.writeString(0, sName);
        out.writeString(1, t.getMessage());
        out.writeCollection(2, new ImmutableArrayList(asTrace), String.class);
        out.writeObject(3, t.getCause());
        }
    }
