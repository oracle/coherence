/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io.pof;


import com.tangosol.io.Evolvable;

import com.tangosol.util.Binary;

import java.io.IOException;


/**
* {@link PofSerializer} implementation that supports the serialization and
* deserialization of any class that implements {@link PortableObject} to and
* from a POF stream.
*
* @author jh  2006.07.18
*
* @since Coherence 3.2
*/
public class PortableObjectSerializer
        extends PofHelper
        implements PofSerializer
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Create a new PortableObjectSerializer for the user type with the given type
    * identifier.
    *
    * @param nTypeId  the user type identifier
    */
    public PortableObjectSerializer(int nTypeId)
        {
        azzert(nTypeId >= 0, "user type identifier cannot be negative");
        m_nTypeId = nTypeId;
        }


    // ----- PofSerializer interface -----------------------------

    /**
    * {@inheritDoc}
    */
    public void serialize(PofWriter out, Object o)
            throws IOException
        {
        PortableObject portable;
        try
            {
            portable = (PortableObject) o;
            }
        catch (ClassCastException e)
            {
            String sClass = null;
            try
                {
                sClass = out.getPofContext().getClassName(m_nTypeId);
                }
            catch (Exception eIgnore) {}

            String sActual = null;
            try
                {
                sActual = o.getClass().getName();
                }
            catch (Exception eIgnore) {}

            throw new IOException(
                    "An exception occurred writing a PortableObject"
                    + " user type to a POF stream: type-id=" + m_nTypeId
                    + (sClass == null ? "" : ", class-name=" + sClass)
                    + (sActual == null ? "" : ", actual class-name=" + sActual)
                    + ", exception=" + e);
            }

        // set the version identifier
        boolean   fEvolvable = portable instanceof Evolvable;
        Evolvable evolvable  = null;
        if (fEvolvable)
            {
            evolvable = (Evolvable) portable;
            out.setVersionId(Math.max(evolvable.getDataVersion(),
                    evolvable.getImplVersion()));
            }

        // write out the object's properties
        portable.writeExternal(out);

        // write out any future properties
        Binary binRemainder = null;
        if (fEvolvable)
            {
            binRemainder = evolvable.getFutureData();
            }
        out.writeRemainder(binRemainder);
        }

    /**
    * {@inheritDoc}
    */
    public Object deserialize(PofReader in)
            throws IOException
        {
        // create a new instance of the user type
        PortableObject portable;
        try
            {
            portable = (PortableObject) in.getPofContext()
                    .getClass(m_nTypeId).newInstance();
            in.registerIdentity(portable);
            }
        catch (Exception e)
            {
            String sClass = null;
            try
                {
                sClass = in.getPofContext().getClassName(m_nTypeId);
                }
            catch (Exception eIgnore) {}

            throw new IOException(
                    "An exception occurred instantiating a PortableObject"
                    + " user type from a POF stream: type-id=" + m_nTypeId
                    + (sClass == null ? "" : ", class-name=" + sClass)
                    + ", exception=\n" + e);
            }

        initialize(portable, in);

        return portable;
        }

    /**
    * Initialize the specified (newly instantiated) PortableObject instance
    * using the specified reader.
    *
    * @param portable  the object to initialize
    * @param in        the PofReader with which to read the object's state  
    *
    * @throws IOException  if an I/O error occurs
    */
    public void initialize(PortableObject portable, PofReader in)
            throws IOException
        {
        // set the version identifier
        boolean   fEvolvable = portable instanceof Evolvable;
        Evolvable evolvable  = null;
        if (fEvolvable)
            {
            evolvable = (Evolvable) portable;
            evolvable.setDataVersion(in.getVersionId());
            }

        // read the object's properties
        portable.readExternal(in);

        // read any future properties
        Binary binRemainder = in.readRemainder();
        if (fEvolvable)
            {
            evolvable.setFutureData(binRemainder);
            }
        }


    // ----- data members ---------------------------------------------------

    /**
    * The type identifier of the user type to serialize and deserialize.
    */
    protected final int m_nTypeId;
    }