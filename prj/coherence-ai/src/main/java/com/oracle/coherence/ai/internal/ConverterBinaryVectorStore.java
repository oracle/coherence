/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.ai.internal;

import com.oracle.coherence.ai.Vector;
import com.tangosol.io.ReadBuffer;
import com.tangosol.net.Service;
import com.tangosol.util.Binary;
import com.tangosol.util.Converter;

import java.util.Optional;

/**
 * A {@link BinaryVectorStore} that converts binary values
 * from one serializer to and from binary values used by
 * the wrapped {@link BinaryVectorStore}.
 */
public class ConverterBinaryVectorStore
        implements BinaryVectorStore
    {
    /**
     * Create a {@link ConverterBinaryVectorStore}.
     *
     * @param wrapped                    the wrapped {@link BinaryVectorStore}
     * @param converterKeyToBinary       the converter to convert values to the wrapped store
     * @param converterValueToBinary     the converter to convert values to the wrapped store
     * @param converterKeyFromBinary     the converter to convert keys from the wrapped store
     * @param converterValueFromBinary   the converter to convert values from the wrapped store
     * @param converterFromBinaryVector  the converter that converts a binary serialized {@link BinaryVector}
     *                                   from the wrapped store into a {@link BinaryVector} serialized
     *                                   with the converter, including converting the metadata.
     */
    public ConverterBinaryVectorStore(BinaryVectorStore wrapped,
            Converter<Binary, Binary> converterKeyToBinary,
            Converter<Binary, Binary> converterValueToBinary,
            Converter<Binary, Binary> converterKeyFromBinary,
            Converter<Binary, Binary> converterValueFromBinary,
            Converter<Binary, Binary> converterFromBinaryVector)
        {
        f_wrapped                   = wrapped;
        f_converterKeyToBinary      = converterKeyToBinary;
        f_converterValueToBinary    = converterValueToBinary;
        f_converterKeyFromBinary    = converterKeyFromBinary;
        f_converterValueFromBinary  = converterValueFromBinary;
        f_converterFromBinaryVector = converterFromBinaryVector;
        }

    @Override
    public void addBinaryVector(BinaryVector vector, ReadBuffer key)
        {
        f_wrapped.addBinaryVector(vector, f_converterKeyToBinary.convert(key.toBinary()));
        }

    @Override
    public void addBinaryVector(BinaryVector vector, Vector.KeySequence<?> sequence)
        {
        f_wrapped.addBinaryVector(vector, sequence);
        }

    @Override
    public Optional<BinaryVector> getVector(ReadBuffer key)
        {
        return f_wrapped.getVector(f_converterKeyToBinary.convert(key.toBinary()));
        }

    @Override
    public void clear()
        {
        f_wrapped.clear();
        }

    @Override
    public void destroy()
        {
        f_wrapped.destroy();
        }

    @Override
    public void release()
        {
        f_wrapped.release();
        }

    @Override
    public Service getService()
        {
        return f_wrapped.getService();
        }

    // ----- data members ---------------------------------------------------

    private final BinaryVectorStore f_wrapped;

    private final Converter<Binary, Binary> f_converterKeyToBinary;

    private final Converter<Binary, Binary> f_converterValueToBinary;

    private final Converter<Binary, Binary> f_converterKeyFromBinary;

    private final Converter<Binary, Binary> f_converterValueFromBinary;

    private final Converter<Binary, Binary> f_converterFromBinaryVector;
    }
