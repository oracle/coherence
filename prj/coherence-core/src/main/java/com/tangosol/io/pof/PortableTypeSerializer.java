/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io.pof;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.io.Evolvable;

import com.tangosol.util.Base;

import java.io.IOException;

import java.lang.reflect.Constructor;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * A {@link PofSerializer} implementation that serializes classes that implement
 * {@link PortableObject} interface (and optionally {@link EvolvableObject}
 * interface).
 * <p>
 * Unlike legacy {@link PortableObjectSerializer}, this class serializes attributes
 * of each class in the object's hierarchy into a separate nested POF stream,
 * which allows for independent evolution of each class in the hierarchy, as well
 * as the evolution of the hierarchy itself (addition of new classes at any level
 * in the hierarchy).
 *
 * @author as  2013.05.01
 * @since  12.2.1
 */
@SuppressWarnings("unchecked")
public class PortableTypeSerializer<T>
        implements PofSerializer<T>
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Create a new PortableTypeSerializer for the user type with the given type
     * identifier and class.
     *
     * @param nTypeId  the type identifier of the user type to serialize and deserialize
     * @param clazz    the class of the user type to serialize and deserialize
     */
    public PortableTypeSerializer(int nTypeId, Class<T> clazz)
        {
        Base.azzert(nTypeId >= 0, "user type identifier cannot be negative");
        if (!PortableObject.class.isAssignableFrom(clazz))
            {
            Logger.err("Class [" + clazz + "] does not implement a PortableObject interface");
            }
        m_nTypeId = nTypeId;
        }

    // ---- PofSerializer implementation ------------------------------------

    @Override
    public void serialize(PofWriter writer, T value)
            throws IOException
        {
        if (!(value instanceof PortableObject))
            {
            throw new IOException("Class [" + value.getClass() +
                    "] does not implement a PortableObject interface");
            }

        PortableObject po = (PortableObject) value;
        boolean fEvolvable = value instanceof EvolvableObject;
        EvolvableObject et = fEvolvable ? (EvolvableObject) value : null;

        try
            {
            PofContext ctx = writer.getPofContext();
            Set<Integer> typeIds = getTypeIds(value, ctx);

            for (int typeId : typeIds)
                {
                Evolvable e = null;
                if (fEvolvable)
                    {
                    e = et.getEvolvable(typeId);
                    }

                PofWriter out = writer.createNestedPofWriter(typeId, typeId);

                if (fEvolvable)
                    {
                    out.setVersionId(Math.max(e.getDataVersion(),
                                                 e.getImplVersion()));
                    }

                Class<?> cls = getClassForTypeId(ctx, typeId);
                if (cls != null)
                    {
                    po.writeExternal(out);
                    }

                out.writeRemainder(fEvolvable ? e.getFutureData() : null);
                }

            writer.writeRemainder(null);
            }
        catch (Exception e)
            {
            e.printStackTrace();
            String sClass = null;
            try
                {
                sClass = writer.getPofContext().getClassName(m_nTypeId);
                }
            catch (Exception ignore) {}

            String sActual = null;
            try
                {
                sActual = value.getClass().getName();
                }
            catch (Exception ignore) {}

            throw new IOException(
                    "An exception occurred writing a PortableObject"
                    + " user type to a POF stream: type-id=" + m_nTypeId
                    + (sClass == null ? "" : ", class-name=" + sClass)
                    + (sActual == null ? "" : ", actual class-name=" + sActual)
                    + ", exception=" + e, e);
            }
        }

    @Override
    public T deserialize(PofReader reader)
            throws IOException
        {
        try
            {
            Class<?> clazz = getClassForTypeId(reader.getPofContext(), m_nTypeId);

            try
                {
                // try to find constructor that accepts PofReader (24.09 or later)
                Constructor<?> ctor = clazz.getDeclaredConstructor(PofReader.class);
                PortableObject po   = (PortableObject) ctor.newInstance(reader);

                if (po instanceof EvolvableObject)
                    {
                    EvolvableObject et = (EvolvableObject) po;

                    PofBufferReader.UserTypeReader userTypeReader = (PofBufferReader.UserTypeReader) reader;
                    int typeId = userTypeReader.getNextPropertyIndex();
                    while (typeId > 0)
                        {
                        PofReader in = userTypeReader.createNestedPofReader(typeId);
                        et.readEvolvable(in);
                        typeId = userTypeReader.getNextPropertyIndex();
                        }
                    }

                reader.readRemainder();
                return (T) po;
                }
            catch (NoSuchMethodException ex)
                {
                // fall back to default constructor (pre-24.09 behavior)
                Constructor<?> ctor = clazz.getDeclaredConstructor();
                PortableObject po   = (PortableObject) ctor.newInstance();

                boolean fEvolvable = po instanceof EvolvableObject;
                EvolvableObject et = fEvolvable ? (EvolvableObject) po : null;

                PofBufferReader.UserTypeReader userTypeReader =
                        (PofBufferReader.UserTypeReader) reader;
                int typeId = userTypeReader.getNextPropertyIndex();
                while (typeId > 0)
                    {
                    PofReader in = userTypeReader.createNestedPofReader(typeId);

                    po.readExternal(in);

                    if (fEvolvable)
                        {
                        et.readEvolvable(in);
                        }
                    typeId = userTypeReader.getNextPropertyIndex();
                    }

                reader.readRemainder();
                return (T) po;
                }
            }
        catch (Exception e)
            {
            //e.printStackTrace();
            String sClass = null;
            try
                {
                sClass = reader.getPofContext().getClassName(m_nTypeId);
                }
            catch (Exception ignore) {}

            throw new IOException(
                    "An exception occurred instantiating a PortableObject"
                    + " user type from a POF stream: type-id=" + m_nTypeId
                    + (sClass == null ? "" : ", class-name=" + sClass)
                    + ", exception=\n" + e, e);
            }
        }

    // ---- helper methods --------------------------------------------------

    /**
     * Return a sorted set of type identifiers for all user types in a class
     * hierarchy.
     *
     * @param o           the object to return type identifiers for
     * @param pofContext  the POF context
     *
     * @return  a sorted set of type identifiers for all user types in a class
     *          hierarchy
     */
    private SortedSet<Integer> getTypeIds(Object o, PofContext pofContext)
        {
        SortedSet<Integer> typeIds = new TreeSet<>();

        Class<?> clazz = o.getClass();
        while (pofContext.isUserType(clazz))
            {
            typeIds.add(pofContext.getUserTypeIdentifier(clazz));
            clazz = clazz.getSuperclass();
            }

        if (o instanceof EvolvableObject)
            {
            EvolvableHolder evolvableHolder = ((EvolvableObject) o).getEvolvableHolder();
            if (!evolvableHolder.isEmpty())
                {
                typeIds.addAll(evolvableHolder.getTypeIds());
                }
            }

        return typeIds;
        }

    /**
     * Return the class associated with a specified type identifier, or null
     * if the identifier is not defined in the current POF context.
     *
     * @param ctx      the POF context
     * @param nTypeId  the type identifier to lookup
     *
     * @return  the class associated with a specified type identifier, or null
     *          if the identifier is not defined in the current POF context
     */
    protected Class<?> getClassForTypeId(PofContext ctx, int nTypeId)
        {
        try
            {
            return ctx.getClass(nTypeId);
            }
        catch (IllegalArgumentException e)
            {
            return null;
            }
        }

    // ----- data members ---------------------------------------------------

    /**
    * The type identifier of the user type to serialize and deserialize.
    */
    protected final int m_nTypeId;
    }
