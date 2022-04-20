/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.config;

import com.tangosol.io.WrapperBufferInput;
import com.tangosol.io.WrapperBufferOutput;
import com.tangosol.io.pof.ConfigurablePofContext;

import com.tangosol.util.Base;
import com.tangosol.util.ExternalizableHelper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

/**
 * Helper class for config serialization tests.
 *
 * @author pfm  2013.09.24
 */
public class TestSerializableHelper
    {
    /**
     * Serialize the input value using ExternalizableLite then de-serialize it.
     *
     * @param inVal  the input value
     *
     * @return the output value that was de-serialized
     */
    public static <T> T convertEL(Object inVal)
        {
        try
            {
            Object bin = ExternalizableHelper.CONVERTER_TO_BINARY.convert(inVal);
            return (T) ExternalizableHelper.CONVERTER_FROM_BINARY.convert(bin);
            }
        catch (Exception e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }

    /**
     * Serialize the input value using POF then de-serialize it.
     *
     * @param inVal  the input value
     *
     * @return the output value that was de-serialized
     */
    public static <T> T convertPof(Object inVal)
        {
        try
            {
            ConfigurablePofContext serializer = new ConfigurablePofContext("coherence-pof-config.xml");
            ByteArrayOutputStream outStream   = new ByteArrayOutputStream();

            serializer.serialize(new WrapperBufferOutput(new DataOutputStream(outStream)), inVal);

            ByteArrayInputStream inputStream = new ByteArrayInputStream(outStream.toByteArray());
            return (T) serializer.deserialize(new WrapperBufferInput(new DataInputStream(inputStream), serializer.getContextClassLoader()));
            }
        catch (Exception e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }
    }
