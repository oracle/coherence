/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util.aggregator;


import com.tangosol.internal.util.graal.ScriptManager;

import com.tangosol.io.SerializationSupport;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;

import com.tangosol.util.AbstractScript;
import com.tangosol.util.Filter;
import com.tangosol.util.InvocableMap;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.ObjectStreamException;

import static com.tangosol.util.InvocableMap.StreamingAggregator;


/**
 * ScriptAggregator is a {@link StreamingAggregator} that wraps a script written
 * in one of the languages supported by Graal VM.
 *
 * @param <K> the type of the Map entry keys
 * @param <V> the type of the Map entry values
 * @param <P> the type of the partial result
 * @param <R> the type of the final result
 *
 * @author mk 2019.09.24
 * @since 14.1.1.0
 */
public class ScriptAggregator<K, V, P, R>
        extends AbstractScript
        implements StreamingAggregator<K, V, P, R>, SerializationSupport
    {
    // ------ constructors ---------------------------------------------------

    /**
     * Default constructor for ExternalizableLite.
     */
    public ScriptAggregator()
        {
        }

    /**
     * Create a {@link StreamingAggregator} that wraps the specified script.
     *
     * @param language         the language the script is written.
     * @param name             the name of the {@link Filter} that needs to
     *                         be evaluated
     * @param characteristics  a bit mask representing the set of characteristics
     *                         of this aggregator
     * @param args             the arguments to be passed to the script during
     *                         evaluation
     */
    public ScriptAggregator(String language, String name, int characteristics, Object... args)
        {
        super(language, name, args);
        m_nCharacteristics = characteristics;
        }

    /**
     * Create a {@link StreamingAggregator} that wraps the specified delegate,
     * with the specified characteristics.
     *
     * @param delegate         the script instance to delegate to
     * @param characteristics  a bit mask representing the set of characteristics
     *                         of this aggregator
     */
    private ScriptAggregator(StreamingAggregator<K, V, P, R> delegate, int characteristics)
        {
        m_delegate         = delegate;
        m_nCharacteristics = characteristics;
        }

    // ----- StreamingAggregator interface ----------------------------------

    @Override
    public int characteristics()
        {
        return m_nCharacteristics == 0
               ? StreamingAggregator.super.characteristics()
               : m_nCharacteristics;
        }

    @Override
    public StreamingAggregator<K, V, P, R> supply()
        {
        return new ScriptAggregator<>(createDelegate(), m_nCharacteristics);
        }

    @Override
    public boolean accumulate(InvocableMap.Entry<? extends K, ? extends V> entry)
        {
        return m_delegate.accumulate(entry);
        }

    @Override
    public boolean combine(P partialResult)
        {
        return m_delegate.combine(partialResult);
        }

    @Override
    public P getPartialResult()
        {
        return m_delegate.getPartialResult();
        }

    @Override
    public R finalizeResult()
        {
        return m_delegate.finalizeResult();
        }

    // ----- PortableObject interface ----------------------------------------

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        super.readExternal(in);
        m_nCharacteristics = in.readInt(10);
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        super.writeExternal(out);
        out.writeInt(10, m_nCharacteristics);
        }

    // ----- ExternalizableLite interface ------------------------------------

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        super.readExternal(in);
        m_nCharacteristics = in.readInt();
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        super.writeExternal(out);
        out.writeInt(m_nCharacteristics);
        }

    // ----- SerializationSupport interface ----------------------------------

    public Object readResolve() throws ObjectStreamException
        {
        m_delegate = createDelegate();
        return this;
        }

    // ----- helper methods --------------------------------------------------

    @SuppressWarnings("unchecked")
    private InvocableMap.StreamingAggregator<K, V, P, R> createDelegate()
        {
        return ScriptManager.getInstance()
                            .execute(m_sLanguage, m_sName, m_aoArgs)
                            .as(InvocableMap.StreamingAggregator.class);
        }

    // ------ data members ---------------------------------------------------

    /**
     * A bit mask representing the set of characteristics of this aggregator.
     */
    private int m_nCharacteristics;

    /**
     * The guest language {@link StreamingAggregator} to delegate to.
     */
    private transient StreamingAggregator<K, V, P, R> m_delegate;
    }
