/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.ai.internal;

import com.oracle.coherence.ai.Vector;

import com.oracle.coherence.ai.stores.AbstractVectorStore;
import com.tangosol.io.ReadBuffer;

import com.tangosol.net.ConfigurableCacheFactory;

import com.tangosol.util.Binary;

import java.util.Optional;

/**
 * A simple {@link BinaryVectorStore} implementation that accepts vectors,
 * keys and metadata as serialized binary {@link ReadBuffer} instances.
 */
public class PassThruVectorStore
        extends AbstractVectorStore<Binary, Binary>
        implements BinaryVectorStore
    {
    /**
     * Create a {@link AbstractVectorStore}.
     *
     * @param ccf    the {@link ConfigurableCacheFactory} managing the underlying caches
     * @param sName  the name of the vector store
     */
    public PassThruVectorStore(ConfigurableCacheFactory ccf, String sName)
        {
        super(ccf, sName);
        }

    @Override
    public void addBinaryVector(BinaryVector vector, ReadBuffer key)
        {
        ReadBuffer buffer = f_converterValueToBinary.convert(vector);
        f_map.put(key.toBinary(), buffer.toBinary());
        }

    @Override
    public void addBinaryVector(BinaryVector vector, Vector.KeySequence<?> sequence)
        {
        addBinaryVector(vector, f_converterKeyToBinary.convert(sequence.next()));
        }

    @Override
    public Optional<BinaryVector> getVector(ReadBuffer key)
        {
        Binary binary = f_map.get(key.toBinary());
        if (binary != null)
            {
            BinaryVector vector = (BinaryVector) f_converterValueFromBinary.convert(binary);
            return Optional.ofNullable(vector);
            }
        return Optional.empty();
        }
    }
