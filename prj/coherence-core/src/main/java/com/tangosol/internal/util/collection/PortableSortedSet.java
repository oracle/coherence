/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
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

import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.json.bind.annotation.JsonbProperty;

/**
 * A wrapper SortedSet that controls wrapped set's serialization.
 *
 * @param <E> the element type
 *
 * @author as  2014.11.19
 * @since 12.2.1
 */
@SuppressWarnings({"unchecked", "SuspiciousToArrayCall", "NullableProblems"})
public class PortableSortedSet<E>
        extends WrapperCollections.AbstractWrapperSortedSet<E>
        implements PortableObject, ExternalizableLite
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Default constructor.
     */
    public PortableSortedSet()
        {
        }

    /**
     * Construct RemoteSortedSet instance which wraps an instance of a
     * {@link TreeSet} and specified comparator.
     */
    public PortableSortedSet(Comparator<? super E> comparator)
        {
        this(() -> new TreeSet<>(comparator));
        }

    /**
     * Construct RemoteSortedSet instance with a specified supplier.
     *
     * @param supplier  a wrapped set supplier
     */
    public PortableSortedSet(Remote.Supplier<SortedSet<E>> supplier)
        {
        m_supplier = supplier;
        }

    /**
     * Return the supplier for this collection.
     *
     * @return the supplier.
     */
    public Remote.Supplier<SortedSet<E>> getSupplier()
        {
        return m_supplier == null ? DEFAULT_SUPPLIER : m_supplier;
        }

    @Override
    protected SortedSet<E> getDelegate()
        {
        return m_colDelegate == null ? (SortedSet<E>) (m_colDelegate = getSupplier().get())
                                     : (SortedSet<E>) m_colDelegate;
        }

    // ---- ExternalizableLite implementation -------------------------------

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        m_supplier = ExternalizableHelper.readObject(in);
        ExternalizableHelper.readCollection(in, getDelegate(), null);
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
        m_supplier    = in.readObject(0);
        m_colDelegate = in.readCollection(1, getDelegate());
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeObject(0, m_supplier);
        out.writeCollection(1, getDelegate());
        }

    // ---- static members -------------------------------------------------

    /**
     * Default supplier for the wrapped Collection instance, used for
     * interoperability with .NET and C++ clients which are not able to handle
     * lambda-based Java suppliers.
     */
    protected static final Remote.Supplier DEFAULT_SUPPLIER = TreeSet::new;

    // ---- data members ----------------------------------------------------

    /**
     * Supplier for the wrapped SortedSet instance.
     */
    @JsonbProperty("supplier")
    protected Remote.Supplier<SortedSet<E>> m_supplier;
    }
