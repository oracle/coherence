/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util.collection;

import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.WrapperCollections;

import com.tangosol.util.function.Remote;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import jakarta.json.bind.annotation.JsonbProperty;
import java.util.Set;

/**
 * A wrapper Map that controls wrapped map's serialization.
 *
 * @param <K> the key type
 * @param <V> the value type
 *
 * @author as  2014.11.19
 * @since 12.2.1
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class PortableMap<K, V>
        extends WrapperCollections.AbstractWrapperMap<K, V>
        implements PortableObject, ExternalizableLite
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Construct RemoteMap instance which wraps an instance of a {@link HashMap}.
     */
    public PortableMap()
        {
        }

    /**
     * Construct RemoteMap instance with a specified supplier.
     *
     * @param supplier  a wrapped set supplier
     */
    public PortableMap(Remote.Supplier<Map<K, V>> supplier)
        {
        m_supplier = supplier;
        }

    /**
    * Return the supplier for this map.
    *
    * @return the supplier.
    */
    public Remote.Supplier<Map<K, V>> getSupplier()
        {
        return m_supplier == null ? DEFAULT_SUPPLIER : m_supplier;
        }

    @Override
    protected Map<K, V> getDelegate()
        {
        return m_mapDelegate == null ? (m_mapDelegate = getSupplier().get()) : m_mapDelegate;
        }

    /**
     * Set the delegate to use.
     *
     * @param setDelegate  the delegate to use
     */
    protected void setDelegate(Map<K, V> setDelegate)
        {
        m_mapDelegate = setDelegate;
        }

    /**
     * Make this map unmodifiable.
     *
     * @return  the unmodifiable instance of this map
     */
    public Map<K, V> unmodifiable()
        {
        return new Unmodifiable<>(getDelegate());
        }

    // ---- ExternalizableLite implementation -------------------------------

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        m_supplier = ExternalizableHelper.readObject(in);

        Map<K, V> delegate = getSupplier().get();
        ExternalizableHelper.readMap(in, delegate, null);
        setDelegate(delegate);
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        ExternalizableHelper.writeObject(out, m_supplier);
        ExternalizableHelper.writeMap(out, getDelegate());
        }

    // ---- PortableObject implementation -----------------------------------

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        m_supplier = in.readObject(0);

        Map<K, V> delegate = getSupplier().get();
        in.readMap(1, delegate);
        setDelegate(delegate);
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeObject(0, m_supplier);
        out.writeMap(1, getDelegate());
        }

    // ---- inner class: Unmodifiable ---------------------------------------

    /**
     * Unmodifiable implementation of PortableList.
     *
     * @param <K> the key type
     * @param <V> the value type
     *
     * @since 22.09
     */
    public static class Unmodifiable<K, V>
            extends PortableMap<K, V>
        {
        /**
         * Default constructor.
         */
        @SuppressWarnings("unused")
        public Unmodifiable()
            {
            }

        /**
         * Construct {@code Unmodifiable} instance.
         *
         * @param mapDelegate  the map to make unmodifiable and delegate all
         *                     method calls to
         */
        private Unmodifiable(Map<K, V> mapDelegate)
            {
            setDelegate(mapDelegate);
            }

        @Override
        protected void setDelegate(Map<K, V> mapDelegate)
            {
            m_mapDelegate = Collections.unmodifiableMap(mapDelegate);
            }
        }

    // ---- static members -------------------------------------------------

    /**
     * Default supplier for the wrapped Map instance, used for
     * interoperability with .NET and C++ clients which are not able to handle
     * lambda-based Java suppliers.
     */
    protected static final Remote.Supplier DEFAULT_SUPPLIER = HashMap::new;

    // ---- data members ----------------------------------------------------

    /**
     * Supplier for the wrapped Map instance.
     */
    @JsonbProperty("supplier")
    protected Remote.Supplier<Map<K, V>> m_supplier;
    }
