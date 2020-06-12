/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.io.json;

import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofSerializer;
import com.tangosol.io.pof.PofWriter;

import com.tangosol.util.Base;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.Versionable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.Externalizable;
import java.io.IOException;
import java.io.NotActiveException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import java.math.BigDecimal;
import java.math.BigInteger;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Extends LinkedHashMap that preserves the order of elements
 * and adds support for POF serialization without fidelity loss in order to
 * support JSON pass-through.
 *
 * @author as  2015.08.25
 */
public class JsonObject
        extends LinkedHashMap<String, Object>
        implements Versionable<Integer>, Externalizable, ExternalizableLite
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a JsonObject.
     */
    public JsonObject()
        {
        }

    /**
     * Construct a JsonObject with the same mappings as the given map.
     *
     * @param map  the map whose mappings are to be placed in this {@code JsonObject}.
     */
    public JsonObject(Map<String, ?> map)
        {
        super(map);
        }

    // ----- type helpers ---------------------------------------------------

    /**
     * Obtain a string value.
     *
     * @param name  the key of the value
     *
     * @return the string value for the given key
     */
    public String getString(String name)
        {
        return (String) get(name);
        }

    /**
     * Obtain a boolean value.
     *
     * @param name  the key of the value
     *
     * @return the boolean value for the given key
     */
    public boolean getBoolean(String name)
        {
        return (boolean) get(name);
        }

    /**
     * Obtain a Number value.
     *
     * @param name  the key of the value
     *
     * @return the Number value for the given key
     */
    public Number getNumber(String name)
        {
        Object o = get(name);
        if (o instanceof String)
            {
            String num = (String) o;
            return num.contains(".")
                   ? new BigDecimal(num)
                   : new BigInteger(num);
            }
        return (Number) o;
        }

    /**
     * Obtain a byte value.
     *
     * @param name  the key of the value
     *
     * @return the byte value for the given key
     */
    public byte getByte(String name)
        {
        return getNumber(name).byteValue();
        }

    /**
     * Obtain a short value.
     *
     * @param name  the key of the value
     *
     * @return the short value for the given key
     */
    public short getShort(String name)
        {
        return getNumber(name).shortValue();
        }

    /**
     * Obtain a int value.
     *
     * @param name  the key of the value
     *
     * @return the int value for the given key
     */
    public int getInt(String name)
        {
        return getNumber(name).intValue();
        }

    /**
     * Obtain a long value.
     *
     * @param name  the key of the value
     *
     * @return the long value for the given key
     */
    public long getLong(String name)
        {
        return getNumber(name).longValue();
        }

    /**
     * Obtain a float value.
     *
     * @param name  the key of the value
     *
     * @return the float value for the given key
     */
    public float getFloat(String name)
        {
        return getNumber(name).floatValue();
        }

    /**
     * Obtain a double value.
     *
     * @param name  the key of the value
     *
     * @return the double value for the given key
     */
    public double getDouble(String name)
        {
        return getNumber(name).doubleValue();
        }

    /**
     * Obtain a BigInteger value.
     *
     * @param name  the key of the value
     *
     * @return the BigInteger value for the given key
     */
    public BigInteger getBigInteger(String name)
        {
        return (BigInteger) getNumber(name);
        }

    /**
     * Obtain a BigDecimal value.
     *
     * @param name  the key of the value
     *
     * @return the BigDecimal value for the given key
     */
    public BigDecimal getBigDecimal(String name)
        {
        return (BigDecimal) getNumber(name);
        }

    /**
     * Set the value for the given key.
     *
     * @param name   the key of te value to set
     * @param value  the value to set
     *
     * @return this {@link JsonObject}
     */
    public JsonObject set(String name, Object value)
        {
        put(name, value);
        return this;
        }

    // ----- class metadata management --------------------------------------

    /**
     * Returns the class name.
     *
     * @return the class name
     */
    public String getClassName()
        {
        return m_sClassName;
        }

    /**
     * Sets the class name.
     *
     * @param className  the class name
     */
    public void setClassName(String className)
        {
        this.m_sClassName = className;
        }

    // ----- Versionable interface -------------------------------------------

    @Override
    public Integer getVersionIndicator()
        {
        return m_nVersion;
        }

    @Override
    public void incrementVersion()
        {
        m_nVersion++;
        }

    // ----- version helpers ------------------------------------------------

    /**
     * Returns {@code true} if the associated type is versioned.
     *
     * @return {@code true} if the associated type is versioned
     */
    public boolean isVersioned()
        {
        return m_nVersion > 0;
        }

    /**
     * Returns the version of the associated type.
     *
     * @return the version of this type or {@code 0} to indicate there is no version
     */
    public int getVersion()
        {
        return m_nVersion;
        }

    /**
     * Set the version of this object.
     *
     * @param version  the version
     *
     * @return this {@link JsonObject}
     */
    public JsonObject setVersion(int version)
        {
        this.m_nVersion = version;
        return this;
        }

    // ----- Object methods -------------------------------------------------

    @Override
    public boolean equals(Object o)
        {
        if (this == o)
            {
            return true;
            }
        if (!(o instanceof JsonObject))
            {
            return false;
            }
        if (!super.equals(o))
            {
            return false;
            }
        JsonObject that = (JsonObject) o;
        return m_nVersion == that.m_nVersion;
        }

    @Override
    public int hashCode()
        {
        return Objects.hash(super.hashCode(), m_nVersion);
        }

    @Override
    public String toString()
        {
        return "JsonObject{"
               + "version=" + m_nVersion
               + ", properties=" + super.toString()
               + '}';
        }

    // ----- Externalizable interface ---------------------------------------

    /**
     * Initialize this object from the data in the passed ObjectInput stream.
     *
     * @param in  the stream to read data from in order to restore the object
     *
     * @throws IOException if an I/O exception occurs
     */
    @Override
    public void readExternal(ObjectInput in) throws IOException
        {
        if (size() > 0)
            {
            throw new NotActiveException();
            }

        m_nVersion = ExternalizableHelper.readInt(in);
        int c = ExternalizableHelper.readInt(in);
        for (int i = 0; i < c; ++i)
            {
            String key   = ExternalizableHelper.readObject(in);
            Object value = ExternalizableHelper.readObject(in);
            put(key, value);
            }
        }

    /**
     * Write this object's data to the passed ObjectOutput stream.
     *
     * @param out  the stream to write the object to
     *
     * @throws IOException if an I/O exception occurs
     */
    @Override
    public synchronized void writeExternal(ObjectOutput out)
            throws IOException
        {
        ExternalizableHelper.writeInt(out, m_nVersion);

        int c = size();
        ExternalizableHelper.writeInt(out, c);
        if (c == 0)
            {
            return;
            }

        @SuppressWarnings("unchecked")
        Map.Entry<String, Object>[] aEntry = (Map.Entry<String, Object>[]) entrySet().toArray(new Map.Entry[c]);
        for (int i = 0; i < c; ++i)
            {
            Map.Entry<String, Object> entry = aEntry[i];
            ExternalizableHelper.writeObject(out, entry.getKey());
            ExternalizableHelper.writeObject(out, entry.getValue());
            }
        }

    // ----- ExternalizableLite interface -----------------------------------

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        if (size() > 0)
            {
            throw new NotActiveException();
            }

        m_sClassName = ExternalizableHelper.readSafeUTF(in);
        m_nVersion   = ExternalizableHelper.readInt(in);
        int c = ExternalizableHelper.readInt(in);
        if (c == 0)
            {
            return;
            }

        boolean fLite = in.readBoolean();
        ObjectInput inObj = fLite
                            ? null
                            : ExternalizableHelper.getObjectInput(in, null);

        Map<String, Object> map = this;

        for (int i = 0; i < c; ++i)
            {
            String name;
            Object value;
            if (fLite)
                {
                name  = ExternalizableHelper.readObject(in);
                value = ExternalizableHelper.readObject(in);
                }
            else
                {
                try
                    {
                    name  = (String) inObj.readObject();
                    value = inObj.readObject();
                    }
                catch (ClassNotFoundException e)
                    {
                    throw new IOException("readObject failed: " + e + "\n" + Base.getStackTrace(e));
                    }
                }

            map.put(name, value);
            }
        }

    @Override
    public synchronized void writeExternal(DataOutput out)
            throws IOException
        {
        ExternalizableHelper.writeSafeUTF(out, m_sClassName);
        ExternalizableHelper.writeInt(out, m_nVersion);

        int c = size();
        ExternalizableHelper.writeInt(out, c);
        if (c == 0)
            {
            return;
            }

        // scan through the contents searching for anything that cannot be
        // streamed to a DataOutput (i.e. anything that requires Java Object
        // serialization); note that the toArray() also resolves concerns
        // related to the synchronization of the data structure itself during
        // serialization
        @SuppressWarnings("unchecked")
        Map.Entry<String, Object>[] aEntry = (Map.Entry<String, Object>[]) entrySet().toArray(new Map.Entry[c]);
        boolean                     fLite  = true;

        for (int i = 0; i < c; ++i)
            {
            Map.Entry<String, Object> entry = aEntry[i];
            if (ExternalizableHelper.getStreamFormat(entry.getKey()) == ExternalizableHelper.FMT_OBJ_SER
                || ExternalizableHelper.getStreamFormat(entry.getValue()) == ExternalizableHelper.FMT_OBJ_SER)
                {
                fLite = false;
                break;
                }
            }
        out.writeBoolean(fLite);

        ObjectOutput outObj = fLite
                              ? null
                              : ExternalizableHelper.getObjectOutput(out);

        for (int i = 0; i < c; ++i)
            {
            Map.Entry<String, Object> entry = aEntry[i];
            String name  = entry.getKey();
            Object value = entry.getValue();
            if (fLite)
                {
                ExternalizableHelper.writeObject(out, name);
                ExternalizableHelper.writeObject(out, value);
                }
            else
                {
                outObj.writeObject(name);
                outObj.writeObject(value);
                }
            }

        if (outObj != null)
            {
            outObj.close();
            }
        }

    // ---- inner class: Serializer -----------------------------------------

    /**
     * POF serializer for JsonObject.
     */
    public static class Serializer
            implements PofSerializer<JsonObject>
        {
        // ----- PofSerializer interface ------------------------------------

        @Override
        public void serialize(PofWriter out, JsonObject value) throws IOException
            {
            out.writeString(0, value.m_sClassName);
            out.writeInt(1,    value.m_nVersion);
            out.writeMap(2,    value);
            out.writeRemainder(null);
            }

        @Override
        public JsonObject deserialize(PofReader in) throws IOException
            {
            try
                {
                JsonObject obj = (JsonObject) in.getPofContext().getClass(in.getUserTypeId()).newInstance();
                obj.m_sClassName = in.readString(0);
                obj.m_nVersion   = in.readInt(1);
                in.readMap(2, obj);
                in.readRemainder();

                return obj;
                }
            catch (Exception e)
                {
                throw new IOException(e);
                }
            }
        }

    // ----- constants ------------------------------------------------------

    private static final long serialVersionUID = 336675295275632710L;

    // ----- data members ---------------------------------------------------

    /**
     * The type associated with this {@code JsonObject}.
     */
    private String m_sClassName;

    /**
     * The version of the type this {@code JsonObject} represents.
     */
    protected int m_nVersion;
    }
