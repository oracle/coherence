/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io.pof.reflect;


import com.tangosol.io.BinaryDeltaCompressor;
import com.tangosol.io.ByteArrayWriteBuffer;
import com.tangosol.io.ReadBuffer;
import com.tangosol.io.WriteBuffer;

import com.tangosol.io.pof.PofBufferReader;
import com.tangosol.io.pof.PofBufferWriter;
import com.tangosol.io.pof.PofConstants;
import com.tangosol.io.pof.PofContext;
import com.tangosol.io.pof.PofHelper;

import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.LongArray;
import com.tangosol.util.SparseArray;

import java.io.IOException;

import java.math.BigDecimal;
import java.math.BigInteger;

import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;


/**
* An abstract base class that implements common functionality for all
* PofValue types.
*
* @author as  2009.02.12
* @since Coherence 3.5
*/
public abstract class AbstractPofValue
        extends    ExternalizableHelper
        implements PofValue
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a PofValue instance wrapping the supplied buffer.
    *
    * @param valueParent  parent value within the POF stream
    * @param bufValue     buffer containing POF representation of this value
    * @param ctx          POF context to use when reading or writing properties
    * @param of           offset of this value from the beginning of POF stream
    * @param nType        POF type identifier for this value
    */
    public AbstractPofValue(PofValue valueParent, ReadBuffer bufValue,
                            PofContext ctx, int of, int nType)
        {
        m_valueParent = valueParent;
        m_bufValue    = bufValue;
        m_ctx         = ctx;
        m_nType       = nType;
        m_of          = of;
        }


    // ----- PofValue interface ---------------------------------------------

    /**
    * {@inheritDoc}
    */
    public int getTypeId()
        {
        return m_nType;
        }

    /**
    * {@inheritDoc}
    */
    public PofValue getRoot()
        {
        PofValue value = this;
        while (true)
            {
            PofValue valueParent = value.getParent();
            if (valueParent == null)
                {
                return value;
                }
            value = valueParent;
            }
        }

    /**
    * {@inheritDoc}
    */
    public PofValue getParent()
        {
        return m_valueParent;
        }

    /**
    * {@inheritDoc}
    */
    public Object getValue()
        {
        return getValue(PofConstants.T_UNKNOWN);
        }

    /**
    * {@inheritDoc}
    */
    public Object getValue(Class clz)
        {
        return getValue(clz == null
                ? PofConstants.T_UNKNOWN
                : PofHelper.getPofTypeId(clz, m_ctx));
        }

    /**
    * {@inheritDoc}
    */
    public Object getValue(int nType)
        {
        Object oValue     = m_oValue;
        int    nValueType = m_nType;

        if (nType == PofConstants.T_UNKNOWN)
            {
            nType = nValueType;
            }

        if (oValue == NO_VALUE || nType != nValueType)
            {
            oValue = new PofValueReader().readValue(nType);

            if (nType == nValueType)
                {
                // cache the retrieved value for the "default" type
                m_oValue = oValue;
                }
            }

        return oValue;
        }

    /**
    * {@inheritDoc}
    */
    public void setValue(Object oValue)
        {
        m_oValue = oValue;
        setDirty();
        }

    /**
    * {@inheritDoc}
    */
    public Binary applyChanges()
        {
        if (!isRoot())
            {
            throw new UnsupportedOperationException("applyChanges() method can"
                    + " only be invoked on the root PofValue instance.");
            }

        if (m_arrayRefs != null)
            {
            // TODO: see COH-11347
            throw new UnsupportedOperationException("applyChanges() method can not"
                    + " be invoked when Object Identity/Reference is enabled.");
            }

        ReadBuffer bufOriginal = m_bufOriginal;
        ReadBuffer bufDeco     = m_bufDecorations;
        ReadBuffer bufDelta    = getChanges();
        ReadBuffer bufNewValue = bufDelta == null
                                 ? bufOriginal
                                 : new BinaryDeltaCompressor()
                                         .applyDelta(bufOriginal, bufDelta);

        if (bufDeco == null)
            {
            return bufNewValue.toBinary();
            }
        else
            {
            int cbCap = MAX_DECO_HEADER_BYTES + bufNewValue.length()
                              + bufDeco.length();

            WriteBuffer bufDecoValue = new ByteArrayWriteBuffer(cbCap);
            WriteBuffer.BufferOutput out = bufDecoValue.getBufferOutput();

            try
                {
                if (Long.highestOneBit(m_nDecoMask) < (1 << Byte.SIZE))
                    {
                    out.writeByte(FMT_BIN_DECO);
                    out.writeByte((int) m_nDecoMask);
                    }
                else
                    {
                    out.writeByte(FMT_BIN_EXT_DECO);
                    out.writePackedLong(m_nDecoMask);
                    }
                writeInt(out, bufNewValue.length());
                out.writeBuffer(bufNewValue);
                out.writeBuffer(bufDeco);

                return bufDecoValue.toBinary();
                }
            catch (IOException e)
                {
                throw ensureRuntimeException(e);
                }
            }
        }

    /**
    * {@inheritDoc}
    */
    public ReadBuffer getChanges()
        {
        if (!isRoot())
            {
            throw new UnsupportedOperationException("getChanges() method can"
                    + " only be invoked on the root PofValue instance.");
            }

        if (m_cDirty == 0)
            {
            // no changes need to be applied
            return null;
            }
        else if (getDirtyBytesCount() * 100L / getSize() > REPLACE_THRESHOLD)
            {
            // encode delta in FMT_REPLACE format
            return new ReplacementEncoder().encode();
            }
        else
            {
            // encode delta in FMT_BINDIFF format
            return new BinaryDiffEncoder().encode();
            }
        }

    /**
    * {@inheritDoc}
    */
    public boolean getBoolean()
        {
        return ((Boolean) getValue(PofConstants.T_BOOLEAN)).booleanValue();
        }

    /**
    * {@inheritDoc}
    */
    public byte getByte()
        {
        return ((Byte) getValue(PofConstants.T_OCTET)).byteValue();
        }

    /**
    * {@inheritDoc}
    */
    public char getChar()
        {
        return ((Character) getValue(PofConstants.T_CHAR)).charValue();
        }

    /**
    * {@inheritDoc}
    */
    public short getShort()
        {
        return ((Short) getValue(PofConstants.T_INT16)).shortValue();
        }

    /**
    * {@inheritDoc}
    */
    public int getInt()
        {
        return ((Integer) getValue(PofConstants.T_INT32)).intValue();
        }

    /**
    * {@inheritDoc}
    */
    public long getLong()
        {
        return ((Long) getValue(PofConstants.T_INT64)).longValue();
        }

    /**
    * {@inheritDoc}
    */
    public float getFloat()
        {
        return ((Float) getValue(PofConstants.T_FLOAT32)).floatValue();
        }

    /**
    * {@inheritDoc}
    */
    public double getDouble()
        {
        return ((Double) getValue(PofConstants.T_FLOAT64)).doubleValue();
        }

    /**
    * {@inheritDoc}
    */
    public boolean[] getBooleanArray()
        {

        return (boolean[]) getValue(PofConstants.T_ARRAY);
        }

    /**
    * {@inheritDoc}
    */
    public byte[] getByteArray()
        {
        return (byte[]) getValue(PofConstants.T_ARRAY);
        }

    /**
    * {@inheritDoc}
    */
    public char[] getCharArray()
        {
        return (char[]) getValue(PofConstants.T_ARRAY);
        }

    /**
    * {@inheritDoc}
    */
    public short[] getShortArray()
        {
        return (short[]) getValue(PofConstants.T_ARRAY);
        }

    /**
    * {@inheritDoc}
    */
    public int[] getIntArray()
        {
        return (int[]) getValue(PofConstants.T_ARRAY);
        }

    /**
    * {@inheritDoc}
    */
    public long[] getLongArray()
        {
        return (long[]) getValue(PofConstants.T_ARRAY);
        }

    /**
    * {@inheritDoc}
    */
    public float[] getFloatArray()
        {
        return (float[]) getValue(PofConstants.T_ARRAY);
        }

    /**
    * {@inheritDoc}
    */
    public double[] getDoubleArray()
        {
        return (double[]) getValue(PofConstants.T_ARRAY);
        }

    /**
    * {@inheritDoc}
    */
    public BigInteger getBigInteger()
        {
        return (BigInteger) getValue(PofConstants.T_INT128);
        }

    /**
    * {@inheritDoc}
    */
    public BigDecimal getBigDecimal()
        {
        return (BigDecimal) getValue(PofConstants.T_DECIMAL128);
        }

    /**
    * {@inheritDoc}
    */
    public String getString()
        {
        return (String) getValue(PofConstants.T_CHAR_STRING);
        }

    /**
    * {@inheritDoc}
    */
    public Date getDate()
        {
        return (Date) getValue(PofConstants.T_DATE);
        }

    /**
    * {@inheritDoc}
    */
    public Object[] getObjectArray()
        {
        return (Object[]) getValue(PofConstants.T_ARRAY);
        }

    /**
    * {@inheritDoc}
    */
    public Collection getCollection(Collection coll)
        {
        Collection collData = (Collection) getValue(PofConstants.T_COLLECTION);
        if (coll == null)
            {
            coll = collData;
            }
        else
            {
            coll.addAll(collData);
            }
        return coll;
        }

    /**
    * {@inheritDoc}
    */
    public Map getMap(Map map)
        {
        Map mapData = (Map) getValue(PofConstants.T_MAP);
        if (map == null)
            {
            map = mapData;
            }
        else
            {
            map.putAll(mapData);
            }
        return map;
        }


    // ----- public API -----------------------------------------------------

    /**
    * Return the POF context to use for serialization and deserialization.
    *
    * @return the POF context
    */
    public PofContext getPofContext()
        {
        return m_ctx;
        }

    /**
    * Return the offset of this value from the beginning of POF stream.
    *
    * @return the offset of this value from the beginning of POF stream
    */
    public int getOffset()
        {
        return m_of;
        }

    /**
    * Return the size of the encoded value in bytes.
    *
    * @return the size of the encoded value
    */
    public int getSize()
        {
        return m_bufValue.length();
        }

    /**
    * Return <tt>true</tt> if this value has been modified,
    * <tt>false</tt> otherwise.
    *
    * @return <tt>true</tt> if this value has been modified,
    * <tt>false</tt> otherwise
    */
    public boolean isDirty()
        {
        return m_fDirty;
        }

    /**
    * Set the dirty flag for this value.
    */
    protected void setDirty()
        {
        if (!isDirty())
            {
            ((AbstractPofValue) getRoot()).incrementDirtyValuesCount();
            ((AbstractPofValue) getRoot()).incrementDirtyBytesCount(getSize());
            m_fDirty = true;
            }
        }

    /**
    * Return this value's serialized form.
    *
    * @return this value's serialized form
    */
    public ReadBuffer getSerializedValue()
        {
        if (isDirty())
            {
            try
                {
                WriteBuffer buf = new ByteArrayWriteBuffer(getSize());
                PofBufferWriter writer =
                        new PofBufferWriter(buf.getBufferOutput(), m_ctx);
                writer.writeObject(getPropertyIndex(), m_oValue);
                if (isUniformEncoded())
                    {
                    ReadBuffer bufRead = buf.getReadBuffer();
                    ReadBuffer.BufferInput in = bufRead.getBufferInput();

                    // skip type id
                    in.readPackedInt();
                    int of = in.getOffset();
                    return bufRead.getReadBuffer(of, buf.length() - of);
                    }
                else
                    {
                    return buf.getReadBuffer();
                    }
                }
            catch (IOException e)
                {
                throw ensureRuntimeException(e);
                }
            }
        else
            {
            return m_bufValue;
            }
        }

    protected int getPropertyIndex()
        {
        return 0;
        }


    // ----- internal API ---------------------------------------------------

    /**
    * Get the original buffer the changes should be applied to.
    *
    * @return buffer containing the original value
    */
    protected ReadBuffer getOriginalBuffer()
        {
        return m_bufOriginal;
        }

    /**
    * Set the original buffer the changes should be applied to.
    *
    * @param bufValue  buffer containing the original value
    */
    protected void setOriginalBuffer(ReadBuffer bufValue)
        {
        m_bufOriginal = bufValue;
        }

    /**
    * Obtain the registry for identity-reference pairs, creating it if
    * necessary.
    *
    * @return the identity-reference registry, never null
    */
    protected LongArray ensureReferenceRegistry()
        {
        LongArray array = m_arrayRefs;

        if (array == null)
            {
            AbstractPofValue root = (AbstractPofValue) getRoot();
            m_arrayRefs = array = root == this
                    ? new SparseArray()
                    : root.ensureReferenceRegistry();
            }

        return array;
        }

    /**
    * Register the passed value with the passed identity.
    *
    * @param nId     the identity within the POF stream of the object
    * @param oValue  the object to associate with the passed identity
    *
    * @throws IllegalArgumentException  if the specified identity is already
    *                                   registered with a different object
    */
    protected void registerIdentity(int nId, Object oValue)
        {
        if (nId >= 0)
            {
            LongArray array = ensureReferenceRegistry();
            Object    o     = array.get(nId);
            if (o != null && o != oValue)
                {
                throw new IllegalArgumentException("duplicate identity: " + nId);
                }

            array.set(nId, oValue);
            }
        }

    /**
    * Look up the specified identity and return the PofValue to which it
    * refers.
    *
    * @param nId  the identity
    *
    * @return the object registered under that identity
    *
    * @throws IOException  if the requested identity is not registered
    */
    protected PofValue lookupIdentity(int nId)
            throws IOException
        {
        LongArray array = ensureReferenceRegistry();
        if (!array.exists(nId))
            {
            throw new IOException("missing identity: " + nId);
            }

        return (PofValue) array.get(nId);
        }

    /**
    * Get the raw value buffer.
    *
    * @return buffer containing the raw value
    */
    protected ReadBuffer getValueBuffer()
        {
        return m_bufValue;
        }

    /**
    * Set the decoration mask and decorations for the PofValue.
    *
    * @param nDecoMask  decoration identifiers bit mask
    * @param bufDeco    buffer containing the decorations
    */
    protected void setDecorations(long nDecoMask, ReadBuffer bufDeco)
        {
        m_nDecoMask      = nDecoMask;
        m_bufDecorations = bufDeco;
        }

    /**
    * Return <tt>true</tt> if this instance is the root of the PofValue
    * hierarchy.
    *
    * @return <tt>true</tt> if this is the root value
    */
    protected boolean isRoot()
        {
        return getParent() == null;
        }

    /**
    * Return <tt>true</tt> if the buffer contains only the value, without the
    * type identifier.
    *
    * @return <tt>true</tt> if the buffer contains only the value
    */
    protected boolean isUniformEncoded()
        {
        return m_fUniformEncoded;
        }

    /**
    * Specifies that the buffer contains only a value, without a type identifier.
    */
    protected void setUniformEncoded()
        {
        m_fUniformEncoded = true;
        }

    /**
    * Get the estimated number of dirty bytes in this POF value hierarchy.
    *
    * @return the number of dirty bytes
    */
    protected int getDirtyBytesCount()
        {
        return m_cbDirty;
        }

    /**
    * Increment the counter representing the number of values within this POF
    * hierarchy that have been modified.
    */
    protected void incrementDirtyValuesCount()
        {
        m_cDirty++;
        }

    /**
    * Increment the counter representing the estimated number of bytes in the
    * original buffer that have been modified.
    *
    * @param cb  the number of bytes to increment counter for
    */
    protected void incrementDirtyBytesCount(int cb)
        {
        m_cbDirty += cb;
        }


    // ----- PofValueReader inner class -------------------------------------

    /**
    * PofBufferReader that allows reading of both complete and uniform encoded
    * values.
    */
    class PofValueReader
            extends PofBufferReader
        {
        // ----- constructors -----------------------------------------------

        /**
        * Construct a PofValueReader instance.
        */
        public PofValueReader()
            {
            super(m_bufValue.getBufferInput(), AbstractPofValue.this.m_ctx);
            }


        // ----- public API -------------------------------------------------

        /**
        * Return the deserialized value of this POF value.
        *
        * @return the deserialized value of this POF value
        */
        public Object readValue()
            {
            try
                {
                return isUniformEncoded()
                        ? super.readAsObject(m_nType)
                        : super.readObject(0);
                }
            catch (IOException e)
                {
                throw ensureRuntimeException(e);
                }
            }

        /**
        * Return the deserialized value which this PofValue represents.
        *
        * @param nType  PofType expected as a result
        *
        * @return the deserialized value
        */
        public Object readValue(int nType)
            {
            try
                {
                // Prevent promotion of null to an intrinsic default value.
                if (m_nType == PofConstants.V_REFERENCE_NULL)
                    {
                    return readValue();
                    }

                if (isUniformEncoded())
                    {
                    return super.readAsObject(nType);
                    }

                switch (nType)
                    {
                    // Return pof "small" values as the specified type
                    // because the serialized form has lost knowledge of
                    // the original type.
                    case T_INT16:
                        return readShort(0);

                    case T_INT32:
                        return readInt(0);

                    case T_INT64:
                        return readLong(0);

                    case T_FLOAT32:
                        return readFloat(0);

                    case T_FLOAT64:
                        return readDouble(0);

                    case T_BOOLEAN:
                        return readBoolean(0) ? Boolean.TRUE : Boolean.FALSE;

                    case T_OCTET:
                        return readByte(0);

                    case T_CHAR:
                        return readChar(0);

                    case T_DATE:
                    case T_DATETIME:
                    case T_TIME:
                        return getPofContext().isPreferJavaTime() ? readObject(0) : readDate(0);

                    case T_UNKNOWN:
                        return readValue();

                    case PofConstants.T_ARRAY:
                    case PofConstants.T_UNIFORM_ARRAY:
                    case PofConstants.T_UNIFORM_SPARSE_ARRAY:
                        {
                        Object o = readValue();
                        if (!o.getClass().isArray() && !(o instanceof SparseArray))
                            {
                            throw new ClassCastException(
                                    o.getClass().getName() + "is not an array");
                            }
                        return o;
                        }

                    default:
                        return PofReflectionHelper.ensureType(readValue(),
                                nType, m_ctx);
                    }
                }
            catch (IOException e)
                {
                throw ensureRuntimeException(e);
                }
            }

        /**
        * {@inheritDoc}
        */
        @Override
        protected Object lookupIdentity(int nId)
                throws IOException
            {
            Object    o     = null;
            LongArray array = m_arrayRefs;
            if (array != null)
                {
                o = array.get(nId);
                }

            return o == null ?  AbstractPofValue.this.lookupIdentity(nId).getValue() : o;
            }
        }


    // ----- BinaryDiffEncoder inner class -------------------------------------

    /**
    * Encode changes made to this POF value in FMT_BINDIFF delta format, as
    * defined by the {@link BinaryDeltaCompressor} class.
    */
    class BinaryDiffEncoder
            extends BinaryDeltaCompressor
        {
        // ----- constructors -----------------------------------------------

        /**
        * Construct a BinDiffEncoder instance.
        */
        public BinaryDiffEncoder()
            {
            }


        // ----- public API -------------------------------------------------

        /**
        * Encode changes made to this POF value in FMT_BINDIFF delta format, as
        * defined by the {@link BinaryDeltaCompressor} class.
        *
        * @return a binary delta containing the changes that can be applied to
        *         the original buffer to reflect the current state of this
        *         POF value.
        */
        public ReadBuffer encode()
            {
            AbstractPofValue value = AbstractPofValue.this;

            WriteBuffer bufDelta = new ByteArrayWriteBuffer(
                    value.getDirtyBytesCount() * 2);
            WriteBuffer.BufferOutput out = bufDelta.getBufferOutput();

            try
                {
                int pos = 0;
                out.write(FMT_BINDIFF);
                pos = encodeValue(out, value, pos);

                int cbOld = getOriginalBuffer().length();
                if (pos < cbOld)
                    {
                    writeExtract(out, pos, cbOld - pos);
                    }
                out.write(OP_TERM);

                return bufDelta.getReadBuffer();
                }
            catch (IOException e)
                {
                throw ensureRuntimeException(e);
                }
            }

        // ----- helper methods ---------------------------------------------

        /**
        * Encode the changes in the PofValue hierarchy recursively.
        *
        * @param out    buffer output to write changes into
        * @param value  POF value to encode
        * @param pos    current position in the original POF stream
        *
        * @return current position in the original POF stream
        */
        protected int encodeValue(WriteBuffer.BufferOutput out,
                                   AbstractPofValue value, int pos)
            {
            if (value.isDirty())
                {
                int of = value.getOffset();
                if (pos < of)
                    {
                    writeExtract(out, pos, of - pos);
                    }
                writeAppend(out, value.getSerializedValue());
                pos = of + value.getSize();
                }
            else if (value instanceof ComplexPofValue)
                {
                Iterator it = ((ComplexPofValue) value).getChildrenIterator();
                while (it.hasNext())
                    {
                    pos = encodeValue(out, (AbstractPofValue) it.next(), pos);
                    }
                }
            // else if SimplePofValue: handled by isDirty block
            return pos;
            }

        /**
        * Encode a binary diff "append" operator to indicate that bytes should
        * be appended from the delta stream to the new value.
        *
        * @param out  the existing BufferOutput for the diff
        * @param buf  the byte array from which to get the bytes to append
        *
        * @return a BufferOutput, never null
        */
        protected WriteBuffer.BufferOutput writeAppend(
                    WriteBuffer.BufferOutput out, ReadBuffer buf)
            {
            try
                {
                out.write(OP_APPEND);
                out.writePackedInt(buf.length());
                out.writeBuffer(buf);
                return out;
                }
            catch (IOException e)
                {
                throw ensureRuntimeException(e);
                }
            }

        /**
        * Encode a binary diff "extract" operator to indicate that bytes
        * should be copied from the old value to the new value.
        *
        * @param out    the existing BufferOutput for the diff
        * @param of     the offset of the old buffer to append
        * @param cb     the length of the old buffer to append
        *
        * @return a BufferOutput, never null
        */
        protected WriteBuffer.BufferOutput writeExtract(
                    WriteBuffer.BufferOutput out, int of, int cb)
            {
            try
                {
                out.write(OP_EXTRACT);
                out.writePackedInt(of);
                out.writePackedInt(cb);
                return out;
                }
            catch (IOException e)
                {
                throw ensureRuntimeException(e);
                }
            }
        }


    // ----- ReplacementEncoder inner class ---------------------------------

    /**
    * Encode changes made to this POF value in FMT_REPLACE delta format, as
    * defined by the {@link BinaryDeltaCompressor} class.
    */
    class ReplacementEncoder
            extends BinaryDeltaCompressor
        {
        // ----- constructors -----------------------------------------------

        /**
        * Construct a ReplacementEncoder instance.
        */
        public ReplacementEncoder()
            {
            }

        // ----- public API -------------------------------------------------

        /**
        * Encode changes made to this POF value in FMT_REPLACE delta format, as
        * defined by the {@link BinaryDeltaCompressor} class.
        *
        * @return a binary delta containing the changes that can be applied to
        *         the original buffer to reflect the current state of this
        *         POF value.
        */
        public ReadBuffer encode()
            {
            AbstractPofValue value = AbstractPofValue.this;

            WriteBuffer bufDelta = new ByteArrayWriteBuffer(
                    value.getDirtyBytesCount() * 2);
            WriteBuffer.BufferOutput out = bufDelta.getBufferOutput();

            try
                {
                int pos = 0;
                out.write(FMT_REPLACE);
                pos = encodeValue(out, value, pos);

                int cbOld = getOriginalBuffer().length();
                if (pos < cbOld)
                    {
                    copyFromOriginal(out, pos, cbOld - pos);
                    }

                return bufDelta.getReadBuffer();
                }
            catch (IOException e)
                {
                throw ensureRuntimeException(e);
                }
            }

        // ----- helper methods ---------------------------------------------

        /**
        * Encode the changes in the PofValue hierarchy recursively.
        *
        * @param out    buffer output to write changes into
        * @param value  POF value to encode
        * @param pos    current position in the original POF stream
        *
        * @return current position in the original POF stream
        */
        protected int encodeValue(WriteBuffer.BufferOutput out,
                                   AbstractPofValue value, int pos)
            {
            if (value.isDirty())
                {
                try
                    {
                    int of = value.getOffset();
                    if (pos < of)
                        {
                        copyFromOriginal(out, pos, of - pos);
                        }
                    out.writeBuffer(value.getSerializedValue());
                    pos = of + value.getSize();
                    }
                catch (IOException e)
                    {
                    throw ensureRuntimeException(e);
                    }
                }
            else if (value instanceof ComplexPofValue)
                {
                Iterator it = ((ComplexPofValue) value).getChildrenIterator();
                while (it.hasNext())
                    {
                    pos = encodeValue(out, (AbstractPofValue) it.next(), pos);
                    }
                }
            // else if SimplePofValue: handled by isDirty block
            return pos;
            }

        /**
        * Copy region from the original value into the output buffer.
        *
        * @param out  output buffer to copy bytes into
        * @param of   offset of the region to copy within the original value
        * @param cb   number of bytes to copy
        */
        protected void copyFromOriginal(WriteBuffer.BufferOutput out, int of, int cb)
            {
            try
                {
                out.writeBuffer(getOriginalBuffer(), of, cb);
                }
            catch (IOException e)
                {
                throw ensureRuntimeException(e);
                }
            }
        }


    // ----- data members ---------------------------------------------------

    /**
    * Constant that allows us to differentiate between POF values that haven't
    * been deserialized and those where the actual value is <tt>null</tt>.
    */
    private static final Object NO_VALUE = new Object();

    /**
    * Threshold that determines if the delta generated when applying changes
    * should be in FMT_REPLACE or FMT_BINDIFF format. If more than a specified
    * percentage of bytes are "dirty", the FMT_REPLACE will be used. Otherwise,
    * FMT_BINDIFF format will be used to capture the changes.
    */
    private static final int REPLACE_THRESHOLD = 67;

    /**
    * Parent value.
    */
    private PofValue m_valueParent;

    /**
    * POF context to use for serialization and deserialization.
    */
    private PofContext m_ctx;

    /**
    * The decoration identifiers bit mask.
    */
    private long m_nDecoMask;

    /**
    * The decorations from the original value.
    */
    private ReadBuffer m_bufDecorations;

    /**
    * Original buffer containing this value, possibly with integer decorations,
    * but without binary decorations.
    */
    private ReadBuffer m_bufOriginal;

    /**
    * Lazily-constructed mapping of identities to references.
    */
    protected LongArray m_arrayRefs;

    /**
    * Buffer containing POF representation of this value without any format
    * identification (e.g. FMT_EXT) or decorations.
    */
    private ReadBuffer m_bufValue;

    /**
    * Offset of this value from the beginning of POF stream.
    */
    private int m_of;

    /**
    * POF type identifer of this value.
    */
    protected int m_nType;

    /**
    * Deserialized representation of this value.
    */
    protected Object m_oValue = NO_VALUE;

    /**
    * True if the this PofValue represents a uniform value without the type id;
    * false for a complete POF value that includes the type id.
    */
    private boolean m_fUniformEncoded;

    /**
    * True iff this value has been changed.
    */
    private boolean m_fDirty;

    /**
    * The number of "dirty" values within this POF hierarchy.
    */
    private int m_cDirty;

    /**
    * The number of "dirty" bytes within this POF hierarchy.
    */
    private int m_cbDirty;
    }
