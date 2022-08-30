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
import java.util.HashSet;
import java.util.Set;

import jakarta.json.bind.annotation.JsonbProperty;

/**
 * A wrapper Set that controls wrapped set's serialization.
 *
 * @param <E> the element type
 *
 * @author as  2014.11.19
 * @since 12.2.1
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class PortableSet<E>
        extends WrapperCollections.AbstractWrapperSet<E>
        implements PortableObject, ExternalizableLite
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Construct RemoteSet instance which wraps an instance of a {@link HashSet}.
     */
    public PortableSet()
        {
        }

    /**
     * Construct RemoteSet instance with a specified supplier.
     *
     * @param supplier  a wrapped set supplier
     */
    public PortableSet(Remote.Supplier<Set<E>> supplier)
        {
        m_supplier = supplier;
        }

    /**
     * Return the supplier for this collection.
     *
     * @return the supplier.
     */
    public Remote.Supplier<Set<E>> getSupplier()
        {
        return m_supplier == null ? DEFAULT_SUPPLIER : m_supplier;
        }

    @Override
    protected Set<E> getDelegate()
        {
        return m_colDelegate == null ? (Set<E>) (m_colDelegate = getSupplier().get())
                                     : (Set<E>) m_colDelegate;
        }

    /**
     * Set the delegate to use.
     *
     * @param setDelegate  the delegate to use
     */
    protected void setDelegate(Set<E> setDelegate)
        {
        m_colDelegate = setDelegate;
        }

    /**
     * Make this set unmodifiable.
     *
     * @return  the unmodifiable instance of this set
     */
    public Set<E> unmodifiable()
        {
        return new Unmodifiable<>(getDelegate());
        }

    // ---- ExternalizableLite implementation -------------------------------

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        m_supplier = ExternalizableHelper.readObject(in);

        Set<E> delegate = getSupplier().get();
        ExternalizableHelper.readCollection(in, delegate, null);
        setDelegate(delegate);
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        ExternalizableHelper.writeObject(out, m_supplier);
        ExternalizableHelper.writeCollection(out, getDelegate());
        }

    // ---- PortableObject implementation -----------------------------------

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        m_supplier = in.readObject(0);

        Set<E> delegate = getSupplier().get();
        in.readCollection(1, delegate);
        setDelegate(delegate);
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeObject(0, m_supplier);
        out.writeCollection(1, getDelegate());
        }

    // ---- inner class: Unmodifiable ---------------------------------------

    /**
     * Unmodifiable implementation of PortableList.
     *
     * @param <E>  the element type
     *
     * @since 22.09
     */
    public static class Unmodifiable<E>
            extends PortableSet<E>
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
         * @param setDelegate  the set to make unmodifiable and delegate all
         *                     method calls to
         */
        private Unmodifiable(Set<E> setDelegate)
            {
            setDelegate(setDelegate);
            }

        @Override
        protected void setDelegate(Set<E> setDelegate)
            {
            m_colDelegate = Collections.unmodifiableSet(setDelegate);
            }
        }

    // ---- static members -------------------------------------------------

    /**
     * Default supplier for the wrapped Collection instance, used for
     * interoperability with .NET and C++ clients which are not able to handle
     * lambda-based Java suppliers.
     */
    protected static final Remote.Supplier DEFAULT_SUPPLIER = HashSet::new;

    // ---- data members ----------------------------------------------------

    /**
     * Supplier for the wrapped Set instance.
     */
    @JsonbProperty("supplier")
    protected Remote.Supplier<Set<E>> m_supplier;
    }
