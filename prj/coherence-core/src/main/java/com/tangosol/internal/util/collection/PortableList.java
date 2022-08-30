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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.json.bind.annotation.JsonbProperty;

/**
 * A wrapper List that controls wrapped list's serialization.
 *
 * @param <E> the element type
 *
 * @author as  2014.11.19
 * @since 12.2.1
 */
@SuppressWarnings({"unchecked", "rawtypes"})
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

    /**
     * Set the delegate to use.
     *
     * @param lstDelegate  the delegate to use
     */
    protected void setDelegate(List<E> lstDelegate)
        {
        m_colDelegate = lstDelegate;
        }

    /**
     * Make this list unmodifiable.
     *
     * @return  the unmodifiable instance of this list
     */
    public List<E> unmodifiable()
        {
        return new Unmodifiable<>(getDelegate());
        }

    // ---- ExternalizableLite implementation -------------------------------

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        m_supplier = ExternalizableHelper.readObject(in);

        List<E> delegate = getSupplier().get();
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

        List<E> delegate = getSupplier().get();
        setDelegate(in.readCollection(1, delegate));
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
            extends PortableList<E>
        {
        /**
         * Default constructor.
         */
        public Unmodifiable()
            {
            }

        /**
         * Construct {@code Unmodifiable} instance.
         *
         * @param lstDelegate  the list to make unmodifiable and delegate all
         *                     method calls to
         */
        private Unmodifiable(List<E> lstDelegate)
            {
            setDelegate(lstDelegate);
            }

        @Override
        protected void setDelegate(List<E> lstDelegate)
            {
            m_colDelegate = Collections.unmodifiableList(lstDelegate);
            }
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
