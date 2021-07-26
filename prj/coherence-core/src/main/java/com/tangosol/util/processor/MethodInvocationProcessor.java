/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util.processor;

import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.Base;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.InvocableMap;

import com.tangosol.util.extractor.ReflectionExtractor;

import com.tangosol.util.function.Remote;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import javax.json.bind.annotation.JsonbProperty;

/**
 * An entry processor that invokes specified method on a value of a cache entry
 * and optionally updates the entry with a modified value.
 *
 * @author as  2014.11.19
 * @since 12.2.1
 */
public class MethodInvocationProcessor<K, V, R>
        implements InvocableMap.EntryProcessor<K, V, R>, ExternalizableLite, PortableObject
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Deserialization constructor.
     */
    public MethodInvocationProcessor()
        {
        }

    /**
     * Construct MethodInvocationProcessor instance.
     *
     * @param sMethodName  the name of the method to invoke
     * @param fMutator     a flag specifying whether the method mutates the
     *                     state of a target object, which implies that the
     *                     entry value should be updated after method invocation
     * @param aoArgs       method arguments
     */
    public MethodInvocationProcessor(String sMethodName, boolean fMutator, Object... aoArgs)
        {
        this(null, sMethodName, fMutator, aoArgs);
        }

    /**
     * Construct MethodInvocationProcessor instance.
     *
     * @param supplier     the supplier that should be used to create the entry
     *                     value if this processor is invoked on an entry that
     *                     is not present in the cache
     * @param sMethodName  the name of the method to invoke
     * @param fMutator     the flag specifying whether the method mutates the
     *                     state of a target object, which implies that the
     *                     entry value should be updated after method invocation
     * @param aoArgs       the method arguments
     */
    public MethodInvocationProcessor(Remote.Supplier<V> supplier, String sMethodName, boolean fMutator, Object... aoArgs)
        {
        m_supplier    = supplier;
        m_sMethodName = sMethodName;
        m_fMutator    = fMutator;
        m_aoArgs      = aoArgs;
        }

    // ----- AbstractProcessor implementation -------------------------------

    @Override
    public R process(InvocableMap.Entry<K, V> entry)
        {
        if (!entry.isPresent())
            {
            if (m_supplier != null)
                {
                entry.setValue(m_supplier.get());
                }
            else
                {
                return null;
                }
            }

        ReflectionExtractor extractor = new ReflectionExtractor(m_sMethodName, m_aoArgs);
        if (m_fMutator)
            {
            V value = entry.getValue();
            R result = (R) extractor.extract(value);
            entry.setValue(value);
            return result;
            }
        else
            {
            return (R) entry.extract(extractor);
            }
        }

    // ---- ExternalizableLite implementation -------------------------------

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        m_supplier    = (Remote.Supplier<V>) ExternalizableHelper.readObject(in);
        m_sMethodName = in.readUTF();
        m_fMutator    = in.readBoolean();

        int cArgs = in.readInt();
        Base.azzert(cArgs < 256, "Unexpected number of method parameters.");

        m_aoArgs = new Object[cArgs];
        for (int i = 0; i < m_aoArgs.length; i++)
            {
            m_aoArgs[i] = ExternalizableHelper.readObject(in);
            }
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        ExternalizableHelper.writeObject(out, m_supplier);
        out.writeUTF(m_sMethodName);
        out.writeBoolean(m_fMutator);
        out.writeInt(m_aoArgs.length);
        for (int i = 0; i < m_aoArgs.length; i++)
            {
            ExternalizableHelper.writeObject(out, m_aoArgs[i]);
            }
        }

    // ---- PortableObject implementation -----------------------------------

    @Override
    public void readExternal(PofReader in)
            throws IOException
        {
        m_supplier    = in.readObject(0);
        m_sMethodName = in.readString(1);
        m_fMutator    = in.readBoolean(2);
        m_aoArgs      = in.readArray(3, Object[]::new);
        }

    @Override
    public void writeExternal(PofWriter out)
            throws IOException
        {
        out.writeObject(0, m_supplier);
        out.writeString(1, m_sMethodName);
        out.writeBoolean(2, m_fMutator);
        out.writeObjectArray(3, m_aoArgs);
        }

    // ---- accessors -------------------------------------------------------

    /**
     * Returns the name of the method to invoke.
     *
     * @return the name of the method to invoke
     */
    public String getMethodName()
        {
        return m_sMethodName;
        }

    /**
     * Returns a boolean specifying whether the method mutates the state of a target object.
     *
     * @return a boolean specifying whether the method mutates the state of a target object
     */
    public boolean isMutator()
        {
        return m_fMutator;
        }

    /**
     * Returns the supplier that should be used when an entry is not present.
     *
     * @return the supplier that should be used when an entry is not present
     */
    public Remote.Supplier<V> getSupplier()
        {
        return m_supplier;
        }

    /**
     * Return the method arguments.
     *
     * @return the method arguments
     */
    public Object[] getArgs()
        {
        return m_aoArgs;
        }

    // ---- data members ----------------------------------------------------

    /**
     * The name of the method to invoke.
     */
    @JsonbProperty("methodName")
    protected String m_sMethodName;

    /**
     * A flag specifying whether the method mutates the state of a target object.
     */
    @JsonbProperty("mutator")
    protected boolean m_fMutator;

    /**
     * The supplier that should be used to create the entry value if this
     * processor is invoked on an entry that is not present in the cache.
     */
    @JsonbProperty("supplier")
    protected Remote.Supplier<V> m_supplier;

    /**
     * Method arguments.
     */
    @JsonbProperty("args")
    protected Object[] m_aoArgs;
    }
