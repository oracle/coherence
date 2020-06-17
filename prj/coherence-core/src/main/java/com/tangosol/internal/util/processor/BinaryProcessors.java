/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.util.processor;

import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.ReadBuffer;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.net.GuardSupport;
import com.tangosol.net.Guardian;

import com.tangosol.util.Binary;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.InvocableMap.EntryProcessor;
import com.tangosol.util.LiteMap;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * A utility class of {@link EntryProcessor} classes that use {@link Binary}
 * keys and values to avoid needless deserialization.
 *
 * @author Mahesh Kannan    2019.11.01
 * @author Jonathan Knight  2019.11.07
 * @since 14.1.2
 */
public final class BinaryProcessors
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Utility class must not have public constructor.
     */
    private BinaryProcessors()
        {
        }

    /**
     * Obtain an instance of the {@link BinaryGetProcessor}.
     *
     * @return an instance of the {@link BinaryGetProcessor}
     */
    public static InvocableMap.EntryProcessor<Binary, Binary, Binary> get()
        {
        return BinaryGetProcessor.INSTANCE;
        }

    /**
     * Obtain an instance of the {@link BinaryPutProcessor}.
     *
     * @param value  the serialized value to put into the cache
     * @param ttl    the expiry value for the entry
     *
     * @return an instance of the {@link BinaryPutProcessor}
     */
    public static InvocableMap.EntryProcessor<Binary, Binary, Binary> put(Binary value, long ttl)
        {
        return new BinaryPutProcessor(value, ttl);
        }

    /**
     * Obtain an instance of the {@link BinaryPutAllProcessor}.
     *
     * @param map  the {@link Map} of {@link Binary} keys and values to add to the cache
     *
     * @return an instance of the {@link BinaryPutProcessor}
     */
    public static InvocableMap.EntryProcessor<Binary, Binary, Binary> putAll(Map<Binary, Binary> map)
        {
        return new BinaryPutAllProcessor(map);
        }

    /**
     * Obtain an instance of the {@link BinaryPutIfAbsentProcessor}.
     *
     * @param value the serialized value to put into the cache
     * @param ttl   the expiry value for the entry
     *
     * @return an instance of the {@link BinaryPutIfAbsentProcessor}
     */
    public static InvocableMap.EntryProcessor<Binary, Binary, Binary> putIfAbsent(Binary value, long ttl)
        {
        return new BinaryPutIfAbsentProcessor(value, ttl);
        }

    // ----- class: BaseProcessor -------------------------------------------

    /**
     * A base {@link com.tangosol.util.InvocableMap.EntryProcessor}.
     *
     * @param <R> the type of the {@link com.tangosol.util.InvocableMap.EntryProcessor}'s result
     */
    public abstract static class BaseProcessor<R>
            implements EntryProcessor<Binary, Binary, R>,
                       ExternalizableLite, PortableObject
        {
        // ----- ExternalizableLite interface -------------------------------

        @Override
        public void readExternal(DataInput dataInput) throws IOException
            {
            }

        @Override
        public void writeExternal(DataOutput dataOutput) throws IOException
            {
            }

        @Override
        public void readExternal(PofReader pofReader) throws IOException
            {
            }

        @Override
        public void writeExternal(PofWriter pofWriter) throws IOException
            {
            }
        }

    // ----- class: BinaryProcessorWithValue --------------------------------

    /**
     * An {@link com.tangosol.util.InvocableMap.EntryProcessor} that contains a value.
     *
     * @param <T> the return type of this {@link EntryProcessor}
     */
    public abstract static class BinaryProcessorWithValue<T>
            extends BaseProcessor<T>
        {
        // ----- constructors -----------------------------------------------

        /**
         * Default constructor for serialization.
         */
        protected BinaryProcessorWithValue()
            {
            }

        /**
         * Create a {@link BinaryProcessorWithValue}.
         *
         * @param value  the {@link com.tangosol.util.Binary} value to set as the
         *               cache entry value
         */
        protected BinaryProcessorWithValue(Binary value)
            {
            this.m_binValue = value;
            }

        // ----- helper methods ---------------------------------------------

        /**
         * Return the {@link Binary} value.
         *
         * @return the {@link Binary} value
         */
        protected Binary getValue()
            {
            return m_binValue;
            }

        // ----- ExternalizableLite interface -------------------------------

        @Override
        public void readExternal(DataInput in) throws IOException
            {
            m_binValue = ExternalizableHelper.readObject(in);
            }

        @Override
        public void writeExternal(DataOutput out) throws IOException
            {
            ExternalizableHelper.writeObject(out, m_binValue);
            }

        @Override
        public void readExternal(PofReader pofReader) throws IOException
            {
            m_binValue = pofReader.readBinary(2);
            }

        @Override
        public void writeExternal(PofWriter pofWriter) throws IOException
            {
            pofWriter.writeBinary(2, m_binValue);
            }

        // ----- data members -----------------------------------------------

        /**
         * The {@link com.tangosol.util.Binary} that is used by this
         * {@link com.tangosol.util.InvocableMap.EntryProcessor}.
         */
        protected Binary m_binValue;
        }

    // ----- class: BinaryContainsValueProcessor ----------------------------

    /**
     * An {@link com.tangosol.util.InvocableMap.EntryProcessor} that checks if the mapping for the
     * key exists in the cache.
     */
    public static class BinaryContainsValueProcessor
            extends BinaryProcessorWithValue<Boolean>
        {
        // ----- constructors -----------------------------------------------

        /**
         * Default constructor for serialization.
         */
        @SuppressWarnings("unused")
        public BinaryContainsValueProcessor()
            {
            }

        /**
         * Create a {@link BinaryContainsValueProcessor}.
         *
         * @param value  the {@link com.tangosol.util.Binary} value to check
         */
        public BinaryContainsValueProcessor(Binary value)
            {
            super(value);
            }

        // ----- Processor interface ----------------------------------------

        @Override
        public Boolean process(InvocableMap.Entry<Binary, Binary> entry)
            {
            if (entry.isPresent())
                {
                Binary     bin    = ((BinaryEntry<Binary, Binary>) entry).getBinaryValue();
                ReadBuffer buffer = ExternalizableHelper.getUndecorated((ReadBuffer) bin);
                return getValue().equals(buffer.toBinary());
                }
            return false;
            }
        }

    // ----- class: BinarySyntheticRemoveBlindProcessor ---------------------

    /**
     * An {@link com.tangosol.util.InvocableMap.EntryProcessor} that removes a cache entry
     * without de-serializing keys or values and returns no
     * result.
     */
    public static class BinarySyntheticRemoveBlindProcessor
            extends BaseProcessor<Void>
        {
        // ----- Processor interface ----------------------------------------

        @Override
        public Void process(InvocableMap.Entry<Binary, Binary> entry)
            {
            if (entry.isPresent())
                {
                entry.remove(true);
                }
            return null;
            }

        // ----- EntryProcessor methods -------------------------------------

        @Override
        public Map<Binary, Void> processAll(Set<? extends InvocableMap.Entry<Binary, Binary>> entries)
            {
            Guardian.GuardContext ctxGuard = GuardSupport.getThreadContext();
            long cMillis                   = ctxGuard == null ? 0L : ctxGuard.getTimeoutMillis();

            Iterator<? extends InvocableMap.Entry<Binary, Binary>> iter = entries.iterator();

            while (iter.hasNext())
                {
                InvocableMap.Entry<Binary, Binary> entry = iter.next();
                process(entry);
                iter.remove();
                if (ctxGuard != null)
                    {
                    ctxGuard.heartbeat(cMillis);
                    }
                }

            return Collections.emptyMap();
            }

        // ----- constants --------------------------------------------------

        /**
         * Singleton RemoveBlindProcessor.
         */
        public static final BinarySyntheticRemoveBlindProcessor INSTANCE = new BinarySyntheticRemoveBlindProcessor();
        }

    // ----- class: BinaryPutProcessor --------------------------------------

    /**
     * An {@link com.tangosol.util.InvocableMap.EntryProcessor} that updates the mapping
     * of a key to a value in a cache.
     */
    public static class BinaryPutProcessor
            extends BaseProcessor<Binary>
        {
        // ----- constructors -----------------------------------------------

        /**
         * Default constructor for serialization.
         */
        public BinaryPutProcessor()
            {
            }

        /**
         * Create a {@link BinaryPutProcessor}.
         *
         * @param value  the {@link com.tangosol.util.Binary} value to set as the
         *               cache entry value
         * @param cTtl   the expiry value for the entry
         */
        BinaryPutProcessor(Binary value, long cTtl)
            {
            this.m_binValue = value;
            this.m_cTtl     = cTtl;
            }

        // ----- Processor interface ----------------------------------------

        @Override
        public Binary process(InvocableMap.Entry<Binary, Binary> entry)
            {
            BinaryEntry<Binary, Binary> binaryEntry = (BinaryEntry<Binary, Binary>) entry;
            Binary                      result      = null;

            if (entry.isPresent())
                {
                result = binaryEntry.getBinaryValue();
                }

            binaryEntry.updateBinaryValue(m_binValue);
            binaryEntry.expire(m_cTtl);

            return result;
            }

        // ----- ExternalizableLite interface -------------------------------

        @Override
        public void readExternal(DataInput in) throws IOException
            {
            m_binValue = ExternalizableHelper.readObject(in);
            m_cTtl     = in.readLong();
            }

        @Override
        public void writeExternal(DataOutput out) throws IOException
            {
            ExternalizableHelper.writeObject(out, m_binValue);
            out.writeLong(m_cTtl);
            }

        @Override
        public void readExternal(PofReader in) throws IOException
            {
            m_binValue = in.readBinary(0);
            m_cTtl     = in.readLong(1);
            }

        @Override
        public void writeExternal(PofWriter out) throws IOException
            {
            out.writeBinary(0, m_binValue);
            out.writeLong(1, m_cTtl);
            }

        // ----- helper methods ---------------------------------------------

        /**
         * Return the {@link Binary} value.
         *
         * @return the {@link Binary} value
         */
        protected Binary getValue()
            {
            return m_binValue;
            }

        /**
         * Return the {@code TTL}.
         *
         * @return the {@code TTL}
         */
        protected long getTtl()
            {
            return m_cTtl;
            }

        // ----- data members -----------------------------------------------

        /**
         * The {@link Binary} value to map to the entry.
         */
        protected Binary m_binValue;

        /**
         * The expiry value for the entry.
         */
        protected long m_cTtl;
        }

    // ----- class: BinaryPutAllProcessor -----------------------------------

    /**
     * An {@link com.tangosol.util.InvocableMap.EntryProcessor} that updates the mapping
     * of a number of key to values in a cache.
     */
    public static class BinaryPutAllProcessor
            extends BaseProcessor<Binary>
        {
        // ----- constructors -----------------------------------------------

        /**
         * Default constructor for serialization.
         */
        @SuppressWarnings("unused")
        public BinaryPutAllProcessor()
            {
            this(new HashMap<>());
            }

        /**
         * Create a {@link BinaryPutAllProcessor}.
         *
         * @param map  the {@link Map} of {@link Binary} key and values to add to the cache
         */
        BinaryPutAllProcessor(Map<Binary, Binary> map)
            {
            this.m_map = map;
            }

        // ----- Processor interface ----------------------------------------

        @Override
        public Binary process(InvocableMap.Entry<Binary, Binary> entry)
            {
            BinaryEntry<Binary, Binary> binaryEntry = (BinaryEntry<Binary, Binary>) entry;
            Binary                      binary      = m_map.get(binaryEntry.getBinaryKey());
            if (binary == null)
                {
                entry.setValue(null);
                }
            else
                {
                binaryEntry.updateBinaryValue(binary);
                }
            return null;
            }

        // ----- EntryProcessor methods -------------------------------------

        @Override
        public Map<Binary, Binary> processAll(Set<? extends InvocableMap.Entry<Binary, Binary>> entries)
            {
            Guardian.GuardContext ctxGuard = GuardSupport.getThreadContext();
            long                  cMillis  = ctxGuard == null ? 0L : ctxGuard.getTimeoutMillis();

            Iterator<? extends InvocableMap.Entry<Binary, Binary>> iterator = entries.iterator();

            while (iterator.hasNext())
                {
                InvocableMap.Entry<Binary, Binary> entry = iterator.next();
                this.process(entry);
                iterator.remove();
                if (ctxGuard != null)
                    {
                    ctxGuard.heartbeat(cMillis);
                    }
                }
            return new LiteMap<>();
            }

        // ----- ExternalizableLite interface -------------------------------

        @Override
        public void readExternal(DataInput in) throws IOException
            {
            ExternalizableHelper.readMap(in, this.m_map, null);
            }

        @Override
        public void writeExternal(DataOutput out) throws IOException
            {
            ExternalizableHelper.writeMap(out, this.m_map);
            }

        @Override
        public void readExternal(PofReader in) throws IOException
            {
            this.m_map = in.readMap(0, new HashMap<>());
            }

        @Override
        public void writeExternal(PofWriter out) throws IOException
            {
            out.writeMap(0, this.m_map, Binary.class, Binary.class);
            }

        // ----- helper methods ---------------------------------------------

        /**
         * Returns the map of keys and values that were/are to be stored.
         *
         * @return the map of keys and values that were/are to be stored
         */
        Map<Binary, Binary> getMap()
            {
            return m_map;
            }

        // ----- data members ----------------------------------------------0

        /**
         * The {@link Binary} value to map to the entry.
         */
        protected Map<Binary, Binary> m_map;
        }

    // ----- class: BinaryPutIfAbsentProcessor ------------------------------

    /**
     * An {@link com.tangosol.util.InvocableMap.EntryProcessor} that puts the specified value in the cache
     * only if there is no existing mapping for the key.
     */
    public static class BinaryPutIfAbsentProcessor
            extends BinaryPutProcessor
        {
        // ----- constructors -----------------------------------------------

        /**
         * Default constructor for serialization.
         */
        @SuppressWarnings("unused")
        public BinaryPutIfAbsentProcessor()
            {
            }

        /**
         * Create a {@link BinaryPutIfAbsentProcessor}.
         *
         * @param value   the {@link com.tangosol.util.Binary} value to set as the
         *                cache entry value
         * @param cTtl    the expiry value for the entry
         */
        BinaryPutIfAbsentProcessor(Binary value, long cTtl)
            {
            super(value, cTtl);
            }

        // ----- Processor interface ----------------------------------------

        @Override
        public Binary process(InvocableMap.Entry<Binary, Binary> entry)
            {
            if (entry.isPresent())
                {
                return ((BinaryEntry<Binary, Binary>) entry).getBinaryValue();
                }
            else
                {
                return super.process(entry);
                }
            }
        }

    // ----- class: BinaryGetProcessor --------------------------------------

    /**
     * An {@link com.tangosol.util.InvocableMap.EntryProcessor} that obtains the {@link com.tangosol.util.Binary}
     * value mapped to a given key in a cache.
     */
    public static class BinaryGetProcessor
            extends BaseProcessor<Binary>
        {

        // ----- Processor interface ----------------------------------------

        @Override
        public Binary process(InvocableMap.Entry<Binary, Binary> entry)
            {
            Binary prevValue = null;
            if (entry.isPresent())
                {
                prevValue = ((BinaryEntry<Binary, Binary>) entry).getBinaryValue();
                }
            return prevValue;
            }

        // ----- EntryProcessor methods -------------------------------------

        @Override
        public Map<Binary, Binary> processAll(Set<? extends InvocableMap.Entry<Binary, Binary>> setEntries)
            {
            Map<Binary, Binary>    mapResults = new LiteMap<>();
            Guardian.GuardContext  ctxGuard   = GuardSupport.getThreadContext();
            long                   cMillis    = ctxGuard == null ? 0L : ctxGuard.getTimeoutMillis();

            Iterator<? extends InvocableMap.Entry<Binary, Binary>> iter = setEntries.iterator();

            while (iter.hasNext())
                {
                InvocableMap.Entry<Binary, Binary> entry = iter.next();
                if (entry.isPresent())
                    {
                    mapResults.put(((BinaryEntry<Binary, Binary>) entry).getBinaryKey(), this.process(entry));
                    }
                iter.remove();
                if (ctxGuard != null)
                    {
                    ctxGuard.heartbeat(cMillis);
                    }
                }

            return mapResults;
            }

        // ----- constants --------------------------------------------------

        /**
         * The singleton {@link BinaryGetProcessor}.
         */
        public static final BinaryGetProcessor INSTANCE = new BinaryGetProcessor();
        }

    // ----- class: BinaryRemoveProcessor -----------------------------------

    /**
     * An {@link com.tangosol.util.InvocableMap.EntryProcessor} that obtains the {@link com.tangosol.util.Binary}
     * value mapped to a given key in a cache.
     */
    public static class BinaryRemoveProcessor
            extends BaseProcessor<Binary>
        {
        // ----- Processor interface ----------------------------------------

        @Override
        public Binary process(InvocableMap.Entry<Binary, Binary> entry)
            {
            Binary prevValue = null;
            if (entry.isPresent())
                {
                prevValue = ((BinaryEntry<Binary, Binary>) entry).getBinaryValue();
                entry.remove(false);
                }
            return prevValue;
            }

        // ----- constants --------------------------------------------------

        /**
         * The singleton {@link BinaryRemoveProcessor}.
         */
        public static final BinaryRemoveProcessor INSTANCE = new BinaryRemoveProcessor();
        }


    // ----- class: BinaryReplaceProcessor ----------------------------------

    /**
     * An {@link com.tangosol.util.InvocableMap.EntryProcessor} that replaces an existing mapping in a cache.
     */
    public static class BinaryReplaceProcessor
            extends BinaryProcessorWithValue<Binary>
        {
        // ----- constructors -----------------------------------------------

        /**
         * Default constructor for serialization.
         */
        @SuppressWarnings("unused")
        public BinaryReplaceProcessor()
            {
            }

        /**
         * Create a {@link BinaryReplaceProcessor}.
         *
         * @param value  the {@link Binary} value to set as the
         *               cache entry value
         */
        public BinaryReplaceProcessor(Binary value)
            {
            super(value);
            }

        // ----- Processor interface ----------------------------------------

        @Override
        public Binary process(InvocableMap.Entry<Binary, Binary> entry)
            {
            Binary result = null;
            if (entry.isPresent())
                {
                result = ((BinaryEntry<Binary, Binary>) entry).getBinaryValue();
                ((BinaryEntry<Binary, Binary>) entry).updateBinaryValue(getValue());
                }
            return result;
            }
        }

    // ----- class: BinaryReplaceMappingProcessor ---------------------------

    /**
     * An {@link com.tangosol.util.InvocableMap.EntryProcessor} that replaces a specific existing mapping in a cache.
     */
    public static class BinaryReplaceMappingProcessor
            extends BinaryProcessorWithValue<Boolean>
        {
        // ----- constructors -----------------------------------------------

        /**
         * Default constructor for serialization.
         */
        @SuppressWarnings("unused")
        public BinaryReplaceMappingProcessor()
            {
            }

        /**
         * Create a {@link BinaryReplaceMappingProcessor}.
         *
         * @param previousValue  the {@link com.tangosol.util.Binary} value of the existing mapping
         * @param newValue       the {@link com.tangosol.util.Binary} value of the new mapping
         */
        public BinaryReplaceMappingProcessor(Binary previousValue, Binary newValue)
            {
            super(previousValue);
            this.m_binNewValue = newValue;
            }

        // ----- static factory methods -------------------------------------

        /**
         * Create an instance of {@link BinaryReplaceMappingProcessor}.
         *
         * @param previousValue  the {@link com.tangosol.util.Binary} value of the existing mapping
         * @param newValue       the {@link com.tangosol.util.Binary} value of the new mapping
         *
         * @return an instance of {@link BinaryReplaceMappingProcessor}
         */
        public static BinaryReplaceMappingProcessor create(Binary previousValue, Binary newValue)
            {
            return new BinaryReplaceMappingProcessor(previousValue, newValue);
            }

        // ----- Processor interface ----------------------------------------

        @Override
        public Boolean process(InvocableMap.Entry<Binary, Binary> entry)
            {
            boolean result = false;
            if (entry.isPresent())
                {
                Binary prevValue = ((BinaryEntry<Binary, Binary>) entry).getBinaryValue();

                if (prevValue.equals(getValue()))
                    {
                    ((BinaryEntry<Binary, Binary>) entry).updateBinaryValue(m_binNewValue);
                    result = true;
                    }
                }

            return result;
            }

        // ----- helper methods ---------------------------------------------

        /**
         * Return the new {@link Binary} value.
         *
         * @return the new {@link Binary} value
         */
        protected Binary getNewValue()
            {
            return m_binNewValue;
            }

        // ----- ExternalizableLite interface -------------------------------

        @Override
        public void readExternal(DataInput in) throws IOException
            {
            super.readExternal(in);
            m_binNewValue = ExternalizableHelper.readObject(in);
            }

        @Override
        public void writeExternal(DataOutput out) throws IOException
            {
            super.writeExternal(out);
            ExternalizableHelper.writeObject(out, m_binNewValue);
            }

        @Override
        public void readExternal(PofReader pofReader) throws IOException
            {
            super.readExternal(pofReader);
            m_binNewValue = pofReader.readBinary(5);
            }

        @Override
        public void writeExternal(PofWriter pofWriter) throws IOException
            {
            super.writeExternal(pofWriter);
            pofWriter.writeBinary(5, m_binNewValue);
            }

        // ----- data members -----------------------------------------------

        /**
         * New {@link Binary} value.
         */
        protected Binary m_binNewValue;
        }
    }
