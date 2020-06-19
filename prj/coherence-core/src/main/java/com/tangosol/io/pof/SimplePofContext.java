/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io.pof;

import com.tangosol.io.Evolvable;
import com.tangosol.io.ReadBuffer;
import com.tangosol.io.WriteBuffer;

import com.tangosol.io.pof.schema.annotation.PortableType;

import com.tangosol.util.Base;
import com.tangosol.util.LongArray;
import com.tangosol.util.SimpleLongArray;

import java.io.IOException;

import java.util.HashMap;
import java.util.Map;

/**
 * Basic {@link PofContext} implementation.
 *
 * @author jh  2006.07.18
 * @see PortableObjectSerializer
 * @since Coherence 3.2
 */
public class SimplePofContext
        implements PofContext
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a new SimplePofContext.
     */
    public SimplePofContext()
        {
        }


    // ----- Serializer interface -------------------------------------------

    /**
     * {@inheritDoc}
     */
    public void serialize(WriteBuffer.BufferOutput out, Object o)
            throws IOException
        {
        PofBufferWriter writer = new PofBufferWriter(out, this);

        // COH-5065: due to the complexity of maintaining references
        // in future data, we won't support them for Evolvable objects
        if (isReferenceEnabled() && !(o instanceof Evolvable))
            {
            writer.enableReference();
            }

        try
            {
            writer.writeObject(-1, o);
            }
        catch (RuntimeException e)
            {
            // Guarantee that runtime exceptions from called methods are
            // IOException
            IOException ioex = new IOException(e.getMessage());

            ioex.initCause(e);
            throw ioex;
            }
        }

    /**
     * {@inheritDoc}
     */
    public Object deserialize(ReadBuffer.BufferInput in)
            throws IOException
        {
        PofBufferReader reader = new PofBufferReader(in, this);

        try
            {
            return reader.readObject(-1);
            }
        catch (RuntimeException e)
            {
            // Guarantee that runtime exceptions from called methods are
            // IOException
            IOException ioex = new IOException(e.getMessage());

            ioex.initCause(e);
            throw ioex;
            }
        }


    // ----- PofContext implementation --------------------------------------

    /**
     * {@inheritDoc}
     */
    public PofSerializer getPofSerializer(int nTypeId)
        {
        validateTypeId(nTypeId);

        LongArray laSerializer = m_laSerializer;
        PofSerializer serializer = laSerializer == null
                                   ? null
                                   : (PofSerializer) laSerializer.get(nTypeId);

        if (serializer == null)
            {
            throw new IllegalArgumentException("unknown user type: " + nTypeId);
            }
        return serializer;
        }

    /**
     * {@inheritDoc}
     */
    public int getUserTypeIdentifier(Object o)
        {
        if (o == null)
            {
            throw new IllegalArgumentException("Object cannot be null");
            }
        return getUserTypeIdentifier(o.getClass());
        }

    /**
     * {@inheritDoc}
     */
    public int getUserTypeIdentifier(Class clz)
        {
        if (clz == null)
            {
            throw new IllegalArgumentException("Class cannot be null");
            }

        Map mapTypeId = m_mapTypeId;
        Integer ITypeId = mapTypeId == null
                          ? null
                          : (Integer) mapTypeId.get(clz);

        if (ITypeId == null)
            {
            throw new IllegalArgumentException("unknown user type: " + clz);
            }
        return ITypeId.intValue();
        }

    /**
     * {@inheritDoc}
     */
    public int getUserTypeIdentifier(String sClass)
        {
        if (sClass == null)
            {
            throw new IllegalArgumentException("Class name cannot be null");
            }
        try
            {
            return getUserTypeIdentifier(Class.forName(sClass));
            }
        catch (Exception e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }

    /**
     * {@inheritDoc}
     */
    public String getClassName(int nTypeId)
        {
        return getClass(nTypeId).getName();
        }

    /**
     * {@inheritDoc}
     */
    public Class getClass(int nTypeId)
        {
        validateTypeId(nTypeId);

        LongArray laClass = m_laClass;
        Class clz = laClass == null ? null : (Class) laClass.get(nTypeId);

        if (clz == null)
            {
            throw new IllegalArgumentException("unknown user type: " + nTypeId);
            }
        return clz;
        }

    /**
     * {@inheritDoc}
     */
    public boolean isUserType(Object o)
        {
        if (o == null)
            {
            throw new IllegalArgumentException("Object cannot be null");
            }
        return isUserType(o.getClass());
        }

    /**
     * {@inheritDoc}
     */
    public boolean isUserType(Class clz)
        {
        if (clz == null)
            {
            throw new IllegalArgumentException("Class cannot be null");
            }

        Map mapTypeId = m_mapTypeId;
        return mapTypeId != null && mapTypeId.get(clz) != null;
        }

    /**
     * {@inheritDoc}
     */
    public boolean isUserType(String sClass)
        {
        if (sClass == null)
            {
            throw new IllegalArgumentException("Class name cannot be null");
            }
        try
            {
            return isUserType(Class.forName(sClass));
            }
        catch (Exception e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }

    /**
     * {@inheritDoc}
     */
    public boolean isPreferJavaTime()
        {
        return m_fPreferJavaTime;
        }

    // ----- user type registration -----------------------------------------

    /**
     * Register {@link PortableType}.
     *
     * @param clz  the portable type to register with this PofContext; must
     *             not be null, and must be annotated with {@link PortableType}
     *             annotation
     *
     * @throws IllegalArgumentException on invalid type identifer, class, or
     *                                  PofSerializer
     */
    public <T> SimplePofContext registerPortableType(Class<T> clz)
        {
        PortableType pt = clz.getAnnotation(PortableType.class);
        if (pt == null)
            {
            throw new IllegalArgumentException("Class " + clz.getName() + " is not annotated with @PortableType");
            }
        registerUserType(pt.id(), clz, new PortableTypeSerializer<>(pt.id(), clz));
        return this;
        }

    /**
     * Register multiple {@link PortableType}s.
     *
     * @param aClz  the array of portable types to register with this PofContext;
     *              each specified class must be annotated with {@link PortableType}
     *              annotation
     *
     * @throws IllegalArgumentException if any of the specified classes is not a valid
     *                                  {@link PortableType}
     */
    public SimplePofContext registerPortableTypes(Class<?>... aClz)
        {
        for (Class<?> aClass : aClz)
            {
            registerPortableType(aClass);
            }
        return this;
        }

    /**
     * Associate a user type with a type identifier and {@link PofSerializer}.
     *
     * @param nTypeId    the type identifier of the specified user type; must be
     *                   greater or equal to 0
     * @param clz        the user type to register with this PofContext; must
     *                   not be null
     * @param serializer the PofSerializer that will be used to serialize and
     *                   deserialize objects of the specified class; must not be
     *                   null
     *
     * @throws IllegalArgumentException on invalid type identifer, class, or
     *                                  PofSerializer
     */
    public <T> void registerUserType(int nTypeId, Class<T> clz, PofSerializer<T> serializer)
        {
        validateTypeId(nTypeId);
        if (clz == null)
            {
            throw new IllegalArgumentException("Class cannot be null");
            }
        if (serializer == null)
            {
            throw new IllegalArgumentException("PofSerializer cannot be null");
            }

        Map mapTypeId = m_mapTypeId;
        LongArray laClass = m_laClass;
        LongArray laSerializer = m_laSerializer;

        // add class-to-type identifier mapping
        if (mapTypeId == null)
            {
            m_mapTypeId = mapTypeId = new HashMap();
            }
        mapTypeId.put(clz, Integer.valueOf(nTypeId));

        // add type identifier-to-class mapping
        if (laClass == null)
            {
            m_laClass = laClass = new SimpleLongArray();
            }
        laClass.set(nTypeId, clz);

        // add type identifier-to-serializer mapping
        if (laSerializer == null)
            {
            m_laSerializer = laSerializer = new SimpleLongArray();
            }
        laSerializer.set(nTypeId, serializer);
        }

    /**
     * Unregister a user type that was previously registered using the specified
     * type identifier.
     *
     * @param nTypeId the type identifier of the user type to unregister
     *
     * @throws IllegalArgumentException if the specified user type identifier is
     *                                  unknown to this PofContext
     */
    public void unregisterUserType(int nTypeId)
        {
        Class clz = getClass(nTypeId);
        Map mapTypeId = m_mapTypeId;
        LongArray laClass = m_laClass;
        LongArray laSerializer = m_laSerializer;

        // remove class-to-type identifier mapping
        if (mapTypeId != null)
            {
            mapTypeId.remove(clz);
            if (mapTypeId.isEmpty())
                {
                mapTypeId = m_mapTypeId = null;
                }
            }

        // remove type identifier-to-class mapping
        if (laClass != null)
            {
            laClass.remove(nTypeId);
            if (laClass.isEmpty())
                {
                laClass = m_laClass = null;
                }
            }

        // remove type identifier-to-serializer mapping
        if (laSerializer != null)
            {
            laSerializer.remove(nTypeId);
            if (laSerializer.isEmpty())
                {
                laSerializer = m_laSerializer = null;
                }
            }
        }

    /**
     * Set the flag specifying if Java 8 date/time types (java.time.*) should be
     * preferred over legacy types.
     *
     * @param fPreferJavaTime  whether Java 8 date/time types should be
     */
    public void setPreferJavaTime(boolean fPreferJavaTime)
        {
        m_fPreferJavaTime = fPreferJavaTime;
        }

    // ----- internal methods -----------------------------------------------

    /**
     * Ensure that the given user type identifier is valid.
     *
     * @param nTypeId the user type identifier to validate
     *
     * @throws IllegalArgumentException if the given user type identifier is
     *                                  negative
     */
    protected void validateTypeId(int nTypeId)
        {
        if (nTypeId < 0)
            {
            throw new IllegalArgumentException("negative user type identifier: "
                                               + nTypeId);
            }
        }


    // ----- accessors ------------------------------------------------------

    /**
     * Determine if Identity/Reference type support is enabled for this
     * SimplePofContext.
     *
     * @return true if Identity/Reference type support is enabled
     */
    public boolean isReferenceEnabled()
        {
        return m_fReferenceEnabled;
        }

    /**
     * Enable or disable POF Identity/Reference type support for this
     * SimplePofContext.
     *
     * @param fEnabled true to enable POF Identity/Reference type support; false
     *                 to disable
     */
    public void setReferenceEnabled(boolean fEnabled)
        {
        m_fReferenceEnabled = fEnabled;
        }

    // ----- data members ---------------------------------------------------

    /**
     * A Map that contains mappings from a registered user type into type
     * identifier.
     */
    protected Map m_mapTypeId;

    /**
     * A LongArray of user types, indexed by type identifier.
     */
    protected LongArray m_laClass;

    /**
     * A LongArray of PofSerializer objects, indexed by type identifier.
     */
    protected LongArray m_laSerializer;

    /**
     * True if POF Identity/Reference type support is enabled.
     */
    protected boolean m_fReferenceEnabled;

    /**
     * True if Java 8 date/time types (java.time.*) should be preferred over
     * legacy types.
     */
    protected boolean m_fPreferJavaTime;
    }
