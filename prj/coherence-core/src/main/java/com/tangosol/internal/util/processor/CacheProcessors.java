/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util.processor;


import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.net.GuardSupport;
import com.tangosol.net.Guardian;
import com.tangosol.net.NamedCache;

import com.tangosol.util.BinaryEntry;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.LiteMap;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import javax.json.bind.annotation.JsonbProperty;


/**
 * Contains factory methods and entry processor classes that are used to implement
 * functionality exposed via different variants of {@link NamedCache} API.
 *
 * @author as  2015.01.17
 * @since 12.2.1
 */
public class CacheProcessors
    {
    // ---- Factory methods -------------------------------------------------

    public static <K, V> InvocableMap.EntryProcessor<K, V, Void> nop()
        {
        return new Null<>();
        }

    public static <K, V> InvocableMap.EntryProcessor<K, V, V> get()
        {
        return new Get<>();
        }

    public static <K, V> InvocableMap.EntryProcessor<K, V, Optional<V>> getOrDefault()
        {
        return new GetOrDefault<>();
        }

    public static <K, V> InvocableMap.EntryProcessor<K, V, Void> put(V value, long cMillis)
        {
        return new Put<>(value, cMillis);
        }

    public static <K, V> InvocableMap.EntryProcessor<K, V, Void> putAll(Map<? extends K, ? extends V> map)
        {
        return new PutAll<>(map);
        }

    public static <K, V> InvocableMap.EntryProcessor<K, V, V> putIfAbsent(V value)
        {
        return new PutIfAbsent<>(value);
        }

    public static <K, V> InvocableMap.EntryProcessor<K, V, V> remove()
        {
        return new Remove<>();
        }

    public static <K, V> InvocableMap.EntryProcessor<K, V, Void> removeBlind()
        {
        return new RemoveBlind<>();
        }

    public static <K, V> InvocableMap.EntryProcessor<K, V, Boolean> remove(Object value)
        {
        return new RemoveValue<>(value);
        }

    public static <K, V> InvocableMap.EntryProcessor<K, V, V> replace(V value)
        {
        return new Replace<>(value);
        }

    public static <K, V> InvocableMap.EntryProcessor<K, V, Boolean> replace(V oldValue, V newValue)
        {
        return new ReplaceValue<>(oldValue, newValue);
        }

    // ---- Lambda-based processors -----------------------------------------

    /**
     * Return an entry processor which replaces the value of an entry by
     * applying the specified function to key and current value.
     *
     * @param <K>       key type
     * @param <V>       value type
     * @param function  function that should be used to compute new value
     *
     * @return  a higher order function implementing
     *          {@link InvocableMap.EntryProcessor} interface
     */
    public static <K, V> InvocableMap.EntryProcessor<K, V, Void> replace(
            BiFunction<? super K, ? super V, ? extends V> function)
        {
        Objects.requireNonNull(function);
        return (entry) ->
            {
            entry.setValue(function.apply(entry.getKey(), entry.getValue()));
            return null;
            };
        }

    /**
     * Return an entry processor that attempts to compute the value
     * using the given mapping function, if the specified key is not already
     * associated with a value (or is mapped to {@code null}), and enters it
     * into this map unless {@code null}.
     *
     * @param <K>             key type
     * @param <V>             value type
     * @param mappingFunction the function to compute a value
     *
     * @return  a higher order function implementing
     *          {@link InvocableMap.EntryProcessor} interface
     */
    public static <K, V> InvocableMap.EntryProcessor<K, V, V> computeIfAbsent(
            Function<? super K, ? extends V> mappingFunction)
        {
        return (entry) ->
            {
            V value = entry.getValue();
            if (value == null)
                {
                value = mappingFunction.apply(entry.getKey());
                if (value != null)
                    {
                    entry.setValue(value);
                    }
                }

            return value;
            };
        }

    /**
     * Return an entry processor that attempts to compute a new mapping
     * using the remapping function, if the value is present and non-null.
     * <p>
     * If the remapping function returns {@code null}, the mapping is
     * removed. If the function itself throws an (unchecked) exception, the
     * exception is rethrown, and the current mapping is left unchanged.
     *
     * @param <K>               key type
     * @param <V>               value type
     * @param remappingFunction the function to compute a value
     *
     * @return  a higher order function implementing
     *          {@link InvocableMap.EntryProcessor} interface
     */
    public static <K, V> InvocableMap.EntryProcessor<K, V, V> computeIfPresent(
            BiFunction<? super K, ? super V, ? extends V> remappingFunction)
        {
        return (entry) ->
            {
            V oldValue = entry.getValue();
            if (oldValue != null)
                {
                V newValue = remappingFunction.apply(entry.getKey(), oldValue);
                if (newValue == null)
                    {
                    entry.remove(false);
                    }
                else
                    {
                    entry.setValue(newValue);
                    return newValue;
                    }
                }
            return null;
            };
        }

    /**
     * Return an entry processor that computes a new value of an entry by
     * applying specified remapping function to the key and current value.
     * <p>
     * If the remapping function returns {@code null}, the mapping is
     * removed. If the function itself throws an (unchecked) exception, the
     * exception is rethrown, and the current mapping is left unchanged.
     *
     * @param <K>               key type
     * @param <V>               value type
     * @param remappingFunction the function to compute a value
     *
     * @return  a higher order function implementing
     *          {@link InvocableMap.EntryProcessor} interface
     */
    public static <K, V> InvocableMap.EntryProcessor<K, V, V> compute(
            BiFunction<? super K, ? super V, ? extends V> remappingFunction)
        {
        return (entry) ->
            {
            Objects.requireNonNull(remappingFunction);

            V oldValue = entry.getValue();
            V newValue = remappingFunction.apply(entry.getKey(), oldValue);

            if (newValue == null)
                {
                if (entry.isPresent())
                    {
                    entry.remove(false);
                    }
                return null;
                }
            else
                {
                entry.setValue(newValue);
                return newValue;
                }
            };
        }

    /**
     * Return an entry processor that sets entry value to the given
     * non-null value (if the entry is absent or null) or to the result
     * of the remapping function applied to the old and new value.
     * <p>
     * If the remapping function returns {@code null}, the mapping is
     * removed. If the function itself throws an (unchecked) exception, the
     * exception is rethrown, and the current mapping is left unchanged.
     *
     * @param <K>               key type
     * @param <V>               value type
     * @param value             the non-null value to be merged with the
     *                          existing value or to be associated with the
     *                          key if the existing value is absent or null
     * @param remappingFunction the function to compute a value
     *
     * @return  a higher order function implementing
     *          {@link InvocableMap.EntryProcessor} interface
     */
    public static <K, V> InvocableMap.EntryProcessor<K, V, V> merge(
            V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction)
        {
        return (entry) ->
            {
            Objects.requireNonNull(remappingFunction);
            Objects.requireNonNull(value);

            V valueOld = entry.getValue();
            V valueNew = valueOld == null
                         ? value
                         : remappingFunction.apply(valueOld, value);

            if (valueNew == null)
                {
                entry.remove(false);
                }
            else
                {
                entry.setValue(valueNew);
                }

            return valueNew;
            };
        }

    // ---- Entry Processors ------------------------------------------------

    /**
     * Abstract base class for entry processors.
     *
     * @param <K> the type of the Map entry key
     * @param <V> the type of the Map entry value
     * @param <R> the type of value returned by the EntryProcessor
     */
    static abstract class BaseProcessor<K, V, R>
            extends ExternalizableHelper
            implements InvocableMap.EntryProcessor<K, V, R>,
                       ExternalizableLite, PortableObject
        {
        // ---- ExternalizableLite methods ----------------------------------

        @Override
        public void readExternal(DataInput in)
                throws IOException
            {
            }

        @Override
        public void writeExternal(DataOutput out)
                throws IOException
            {
            }

        // ---- PortableObject methods --------------------------------------

        @Override
        public void readExternal(PofReader in)
                throws IOException
            {
            }

        @Override
        public void writeExternal(PofWriter out)
                throws IOException
            {
            }
        }

    /**
     * Null entry processor.
     *
     * @param <K> the type of the Map entry key
     * @param <V> the type of the Map entry value
     */
    public static class Null<K, V>
            extends BaseProcessor<K, V, Void>
        {
        public Void process(InvocableMap.Entry<K, V> entry)
            {
            return null;
            }
        }

    /**
     * Get entry processor.
     *
     * @param <K> the type of the Map entry key
     * @param <V> the type of the Map entry value
     */
    public static class Get<K, V>
            extends BaseProcessor<K, V, V>
        {
        public V process(InvocableMap.Entry<K, V> entry)
            {
            return entry.getValue();
            }

        @Override
        public Map<K, V> processAll(Set<? extends InvocableMap.Entry<K, V>> setEntries)
            {
            Map<K, V>             mapResults = new LiteMap<>();
            Guardian.GuardContext ctxGuard   = GuardSupport.getThreadContext();
            long                  cMillis    = ctxGuard == null ? 0L : ctxGuard.getTimeoutMillis();

            for (Iterator<? extends InvocableMap.Entry<K, V>> iter = setEntries.iterator(); iter.hasNext(); )
                {
                InvocableMap.Entry<K, V> entry = iter.next();
                if (entry.isPresent())
                    {
                    mapResults.put(entry.getKey(), process(entry));
                    }

                iter.remove();

                if (ctxGuard != null)
                    {
                    ctxGuard.heartbeat(cMillis);
                    }
                }
            return mapResults;
            }
        }

    /**
     * GetOrDefault entry processor.
     *
     * @param <K> the type of the Map entry key
     * @param <V> the type of the Map entry value
     */
    public static class GetOrDefault<K, V>
            extends BaseProcessor<K, V, Optional<V>>
        {
        public Optional<V> process(InvocableMap.Entry<K, V> entry)
            {
            return Optional.ofNullable(entry.getValue());
            }
        }

    /**
     * Put entry processor.
     *
     * @param <K> the type of the Map entry key
     * @param <V> the type of the Map entry value
     */
    public static class Put<K, V>
            extends BaseProcessor<K, V, Void>
        {
        public Put()
            {
            }

        public Put(V value, long cMillis)
            {
            m_value = value;
            m_cMillis = cMillis;
            }

        // ---- EntryProcessor methods --------------------------------------

        @Override
        public Void process(InvocableMap.Entry<K, V> entry)
            {
            if (entry instanceof BinaryEntry)
                {
                ((BinaryEntry) entry).expire(m_cMillis);
                }
            entry.setValue(m_value);
            return null;
            }

        // ---- ExternalizableLite methods ----------------------------------

        @Override
        public void readExternal(DataInput in)
                throws IOException
            {
            m_value   = readObject(in);
            m_cMillis = readLong(in);
            }

        @Override
        public void writeExternal(DataOutput out)
                throws IOException
            {
            writeObject(out, m_value);
            writeLong(out, m_cMillis);
            }

        // ---- PortableObject methods --------------------------------------

        @Override
        public void readExternal(PofReader in)
                throws IOException
            {
            m_value   = in.readObject(0);
            m_cMillis = in.readLong(1);
            }

        @Override
        public void writeExternal(PofWriter out)
                throws IOException
            {
            out.writeObject(0, m_value);
            out.writeLong(1, m_cMillis);
            }

        // ---- accessors ---------------------------------------------------

        public V getValue()
            {
            return m_value;
            }

        public long getMillis()
            {
            return m_cMillis;
            }

        // ---- data members ------------------------------------------------

        @JsonbProperty("value")
        protected V m_value;

        @JsonbProperty("ttl")
        protected long m_cMillis;
        }

    /**
     * PutAll entry processor.
     *
     * @param <K> the type of the Map entry key
     * @param <V> the type of the Map entry value
     */
    public static class PutAll<K, V>
            extends BaseProcessor<K, V, Void>
        {
        public PutAll()
            {
            }

        public PutAll(Map<? extends K, ? extends V> map)
            {
            m_map = map;
            }

        // ---- EntryProcessor methods --------------------------------------

        @Override
        public Void process(InvocableMap.Entry<K, V> entry)
            {
            // avoid returning the old value by using the synthetic variant
            // of setValue.
            entry.setValue(m_map.get(entry.getKey()), /*fSynthetic*/ false);
            return null;
            }

        // ---- ExternalizableLite methods ----------------------------------

        @Override
        public void readExternal(DataInput in)
                throws IOException
            {
            readMap(in, m_map, null);
            }

        @Override
        public void writeExternal(DataOutput out)
                throws IOException
            {
            writeMap(out, m_map);
            }

        // ---- PortableObject methods --------------------------------------

        @Override
        public void readExternal(PofReader in)
                throws IOException
            {
            in.readMap(0, m_map);
            }

        @Override
        public void writeExternal(PofWriter out)
                throws IOException
            {
            out.writeMap(0, m_map);
            }

        // ---- accessors ---------------------------------------------------

        public Map<? extends K, ? extends V> getMap()
            {
            return m_map;
            }

        // ---- data members ------------------------------------------------

        @JsonbProperty("entries")
        protected Map<? extends K, ? extends V> m_map = new HashMap<>();
        }

    /**
     * PutIfAbsent entry processor.
     *
     * @param <K> the type of the Map entry key
     * @param <V> the type of the Map entry value
     */
    public static class PutIfAbsent<K, V>
            extends BaseProcessor<K, V, V>
        {
        public PutIfAbsent()
            {
            }

        public PutIfAbsent(V value)
            {
            m_value = value;
            }

        // ---- EntryProcessor methods --------------------------------------

        @Override
        public V process(InvocableMap.Entry<K, V> entry)
            {
            if (entry.getValue() != null)
                {
                return entry.getValue();
                }
            else
                {
                entry.setValue(m_value);
                return null;
                }
            }

        // ---- ExternalizableLite methods ----------------------------------

        @Override
        public void readExternal(DataInput in)
                throws IOException
            {
            m_value = readObject(in);
            }

        @Override
        public void writeExternal(DataOutput out)
                throws IOException
            {
            writeObject(out, m_value);
            }

        // ---- PortableObject methods --------------------------------------

        @Override
        public void readExternal(PofReader in)
                throws IOException
            {
            m_value = in.readObject(0);
            }

        @Override
        public void writeExternal(PofWriter out)
                throws IOException
            {
            out.writeObject(0, m_value);
            }

        // ---- accessors ---------------------------------------------------

        public V getValue()
            {
            return m_value;
            }

        // ---- data members ------------------------------------------------

        @JsonbProperty("value")
        protected V m_value;
        }

    /**
     * Remove entry processor.
     *
     * @param <K> the type of the Map entry key
     * @param <V> the type of the Map entry value
     */
    public static class Remove<K, V>
            extends BaseProcessor<K, V, V>
        {
        public V process(InvocableMap.Entry<K, V> entry)
            {
            V value = entry.getValue();
            entry.remove(false);
            return value;
            }
        }

    /**
     * Remove entry processor.
     *
     * @param <K> the type of the Map entry key
     * @param <V> the type of the Map entry value
     */
    public static class RemoveBlind<K, V>
            extends BaseProcessor<K, V, Void>
        {
        public Void process(InvocableMap.Entry<K, V> entry)
            {
            entry.remove(false);
            return null;
            }
        }

    /**
     * RemoveValue entry processor.
     *
     * @param <K> the type of the Map entry key
     * @param <V> the type of the Map entry value
     */
    public static class RemoveValue<K, V>
            extends BaseProcessor<K, V, Boolean>
        {
        public RemoveValue()
            {
            }

        public RemoveValue(Object oValue)
            {
            m_oValue = oValue;
            }

        // ---- EntryProcessor methods --------------------------------------

        @Override
        public Boolean process(InvocableMap.Entry<K, V> entry)
            {
            V valueCurrent = entry.getValue();
            if (Objects.equals(valueCurrent, m_oValue))
                {
                entry.remove(false);
                return true;
                }

            return false;
            }

        // ---- ExternalizableLite methods ----------------------------------

        @Override
        public void readExternal(DataInput in)
                throws IOException
            {
            m_oValue = readObject(in);
            }

        @Override
        public void writeExternal(DataOutput out)
                throws IOException
            {
            writeObject(out, m_oValue);
            }

        // ---- PortableObject methods --------------------------------------

        @Override
        public void readExternal(PofReader in)
                throws IOException
            {
            m_oValue = in.readObject(0);
            }

        @Override
        public void writeExternal(PofWriter out)
                throws IOException
            {
            out.writeObject(0, m_oValue);
            }

        // ---- accessors ---------------------------------------------------

        public Object getValue()
            {
            return m_oValue;
            }

        // ---- data members ------------------------------------------------

        @JsonbProperty("value")
        protected Object m_oValue;
        }

    /**
     * Replace entry processor.
     *
     * @param <K> the type of the Map entry key
     * @param <V> the type of the Map entry value
     */
    public static class Replace<K, V>
            extends BaseProcessor<K, V, V>
        {
        public Replace()
            {
            }

        public Replace(V value)
            {
            m_value = value;
            }

        // ---- EntryProcessor methods --------------------------------------

        @Override
        public V process(InvocableMap.Entry<K, V> entry)
            {
            return entry.getValue() != null || entry.isPresent()
                    ? entry.setValue(m_value)
                    : null;
            }

        // ---- ExternalizableLite methods ----------------------------------

        @Override
        public void readExternal(DataInput in)
                throws IOException
            {
            m_value = readObject(in);
            }

        @Override
        public void writeExternal(DataOutput out)
                throws IOException
            {
            writeObject(out, m_value);
            }

        // ---- PortableObject methods --------------------------------------

        @Override
        public void readExternal(PofReader in)
                throws IOException
            {
            m_value = in.readObject(0);
            }

        @Override
        public void writeExternal(PofWriter out)
                throws IOException
            {
            out.writeObject(0, m_value);
            }

        // ---- accessors ---------------------------------------------------

        public V getValue()
            {
            return m_value;
            }

        // ---- data members ------------------------------------------------

        @JsonbProperty("value")
        protected V m_value;
        }

    /**
     * ReplaceValue entry processor.
     *
     * @param <K> the type of the Map entry key
     * @param <V> the type of the Map entry value
     */
    public static class ReplaceValue<K, V>
            extends BaseProcessor<K, V, Boolean>
        {
        public ReplaceValue()
            {
            }

        public ReplaceValue(V oldValue, V newValue)
            {
            m_oldValue = oldValue;
            m_newValue = newValue;
            }

        // ---- EntryProcessor methods --------------------------------------

        @Override
        public Boolean process(InvocableMap.Entry<K, V> entry)
            {
            V valueCurrent = entry.getValue();
            if (entry.isPresent() && Objects.equals(valueCurrent, m_oldValue))
                {
                entry.setValue(m_newValue);
                return true;
                }

            return false;
            }

        // ---- ExternalizableLite methods ----------------------------------

        @Override
        public void readExternal(DataInput in)
                throws IOException
            {
            m_oldValue = readObject(in);
            m_newValue = readObject(in);
            }

        @Override
        public void writeExternal(DataOutput out)
                throws IOException
            {
            writeObject(out, m_oldValue);
            writeObject(out, m_newValue);
            }

        // ---- PortableObject methods --------------------------------------

        @Override
        public void readExternal(PofReader in)
                throws IOException
            {
            m_oldValue = in.readObject(0);
            m_newValue = in.readObject(1);
            }

        @Override
        public void writeExternal(PofWriter out)
                throws IOException
            {
            out.writeObject(0, m_oldValue);
            out.writeObject(1, m_newValue);
            }

        // ---- accessors ---------------------------------------------------

        public V getOldValue()
            {
            return m_oldValue;
            }

        public V getNewValue()
            {
            return m_newValue;
            }

        // ---- data members ------------------------------------------------

        @JsonbProperty("oldValue")
        protected V m_oldValue;

        @JsonbProperty("newValue")
        protected V m_newValue;
        }
    }
