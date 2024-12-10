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

import java.util.ArrayList;
import java.util.List;

import javax.json.bind.annotation.JsonbProperty;

/**
 * A wrapper List that controls wrapped list's serialization.
 *
 * @param <E> the element type
 *
 * @author as  2014.11.19
 * @since 12.2.1
 */
@SuppressWarnings({"unchecked", "SuspiciousToArrayCall", "NullableProblems"})
public class PortableList<E>
        extends WrapperCollections.AbstractWrapperList<E>
        implements PortableObject, ExternalizableLite
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Default constructor.
     */
    public PortableList()
        {
        }

    /**
     * Construct RemoteList instance with a specified supplier.
     *
     * @param supplier  a wrapped set supplier
     */
    public PortableList(Remote.Supplier<List<E>> supplier)
        {
        m_supplier = supplier;
        }

    /**
    * Return the supplier for this collection.
    *
    * @return the supplier.
    */
    public Remote.Supplier<List<E>> getSupplier()
        {
        return m_supplier == null ? DEFAULT_SUPPLIER : m_supplier;
        }

    @Override
    protected List<E> getDelegate()
        {
        return m_colDelegate == null ? (List<E>) (m_colDelegate = getSupplier().get())
                                     : (List<E>) m_colDelegate;
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
     * Default supplier for the wrapped List instance, used for
     * interoperability with .NET and C++ clients which are not able to handle
     * lambda-based Java suppliers.
     */
    protected static final Remote.Supplier DEFAULT_SUPPLIER = ArrayList::new;

    // ---- data members ----------------------------------------------------

    /**
     * Supplier for the wrapped List instance.
     */
    @JsonbProperty("supplier")
    protected Remote.Supplier<List<E>> m_supplier;
    }
