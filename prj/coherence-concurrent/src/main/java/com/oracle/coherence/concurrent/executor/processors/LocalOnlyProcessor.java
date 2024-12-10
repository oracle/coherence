/*
 * Copyright (c) 2016, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.executor.processors;

import com.oracle.coherence.concurrent.executor.Result;

import com.oracle.coherence.concurrent.executor.PortableAbstractProcessor;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.Member;

import com.tangosol.util.InvocableMap;

import java.io.IOException;

import java.util.Map;

/**
 * An {@link InvocableMap.EntryProcessor} that invokes another
 * {@link InvocableMap.EntryProcessor} only on {@link Map.Entry}s that are located
 * in the process that created the {@link LocalOnlyProcessor}.
 *
 * @param <K> the type of the Map entry key
 * @param <V> the type of the Map entry value
 * @param <R> the type of value returned by the EntryProcessor
 *
 * @since 21.12
 */
public class LocalOnlyProcessor<K, V, R>
        extends PortableAbstractProcessor<K, V, R>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a {@link LocalOnlyProcessor} (required for serialization).
     */
    @SuppressWarnings("unused")
    public LocalOnlyProcessor()
        {
        f_submittingMember = CacheFactory.getCluster().getLocalMember();
        }

    /**
     * Constructs a {@link LocalOnlyProcessor} for the specified {@link InvocableMap.EntryProcessor}.
     *
     * @param processor  the {@link InvocableMap.EntryProcessor}
     */
    public LocalOnlyProcessor(InvocableMap.EntryProcessor<K, V, R> processor)
        {
        m_processor        = processor;
        f_submittingMember = CacheFactory.getCluster().getLocalMember();
        }

    // ----- EntryProcessor interface ---------------------------------------

    @SuppressWarnings("unchecked")
    @Override
    public R process(InvocableMap.Entry<K, V> entry)
        {
        Member localMember = CacheFactory.getCluster().getLocalMember();

        if (localMember.equals(f_submittingMember))
            {
            return (R) Result.of(m_processor.process(entry));
            }
        else
            {
            return (R) Result.none();
            }
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Creates a {@link LocalOnlyProcessor} for the specified
     * {@link InvocableMap.EntryProcessor}.
     *
     * @param processor  the {@link InvocableMap.EntryProcessor}
     * @param <K>        the type of the Map entry key
     * @param <V>        the type of the Map entry value
     * @param <R>        the type of value returned by the EntryProcessor
     *
     * @return a {@link LocalOnlyProcessor}
     */
    public static <K, V, R> LocalOnlyProcessor<K, V, R> of(InvocableMap.EntryProcessor<K, V, R> processor)
        {
        return new LocalOnlyProcessor<>(processor);
        }

    // ----- PortableObject interface ---------------------------------------

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        m_processor = in.readObject(0);
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeObject(0, m_processor);
        }

    // ----- data members ---------------------------------------------------

    /**
     * The {@link InvocableMap.EntryProcessor} to invoke.
     */
    protected InvocableMap.EntryProcessor<K, V, R> m_processor;

    /**
     * The {@link Member} that submitted the {@link LocalOnlyProcessor}.
     */
    protected final Member f_submittingMember;
    }
