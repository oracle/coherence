/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io.pof;


import java.io.Externalizable;
import java.io.IOException;


/**
* {@link PofSerializer} implementation that supports the serialization and
* deserialization of any class that implements {@link Externalizable} to
* and from a POF stream. This implementation is provided to ease migration
* of Externalizable implementations to support the POF stream format.
* <p>
* <b>Warning:</b> This implementation does not correctly support all possible
* Externalizable implementations. It will likely support most simple
* Externalizable implementations that read and write their properties is a
* manner analogous to a proper implementation of the PortableObject
* interface. Incompatibilities are likely when the user type does direct (or
* indirect) binary-level I/O through the ObjectInput and ObjectOutput stream
* objects passed to the
* {@link Externalizable#readExternal(java.io.ObjectInput)} )} and
* {@link Externalizable#writeExternal(java.io.ObjectOutput)} methods
* respectively. Note that the helper methods on ExternalizableHelper
* <b>are</b> POF aware, and thus safe to use within the readExternal and
* writeExternal methods.
*
* @author cp  2006.07.31
*
* @since Coherence 3.2
*/
public class ExternalizablePofSerializer
        extends PofHelper
        implements PofSerializer
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Create a new ExternalizablePofSerializer for the user type with the
    * given type identifier.
    *
    * @param nTypeId  the user type identifier
    */
    public ExternalizablePofSerializer(int nTypeId)
        {
        azzert(nTypeId >= 0, "user type identifier cannot be negative");
        m_nTypeId = nTypeId;
        }


    // ----- PofSerializer interface ----------------------------------------

    /**
    * {@inheritDoc}
    */
    public void serialize(PofWriter out, Object o)
            throws IOException
        {
        Externalizable externalizable;
        try
            {
            externalizable  = (Externalizable) o;
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
                    "An exception occurred writing an Externalizable"
                    + " user type to a POF stream: type-id=" + m_nTypeId
                    + (sClass == null ? "" : ", class-name=" + sClass)
                    + (sActual == null ? "" : ", actual class-name=" + sActual)
                    + ", exception=\n" + e);
            }

        // write out the object's properties
        externalizable.writeExternal(new PofOutputStream(out));
        out.writeRemainder(null);
        }

    /**
    * {@inheritDoc}
    */
    public Object deserialize(PofReader in)
            throws IOException
        {
        // create a new instance of the user type
        Externalizable externalizable;
        try
            {
            externalizable = (Externalizable) in.getPofContext()
                    .getClass(m_nTypeId).newInstance();
            in.registerIdentity(externalizable);
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
                    "An exception occurred instantiating an Externalizable"
                    + " user type from a POF stream: type-id=" + m_nTypeId
                    + (sClass == null ? "" : ", class-name=" + sClass)
                    + ", exception=\n" + e);
            }

        try
            {
            // read the object's properties
            externalizable.readExternal(new PofInputStream(in));
            }
        catch (IOException e)
            {
            throw e;
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
                    "An exception occurred reading an Externalizable"
                    + " user type from a POF stream: type-id=" + m_nTypeId
                    + (sClass == null ? "" : ", class-name=" + sClass)
                    + ", exception=\n" + e);
            }

        in.readRemainder();

        return externalizable;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The type identifier of the user type to serialize and deserialize.
    */
    protected final int m_nTypeId;
    }
