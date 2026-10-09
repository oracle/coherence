/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.config;

import com.tangosol.io.pof.ConfigurablePofContext;

import com.tangosol.util.Base;
import com.tangosol.util.ExternalizableHelper;

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

            // use the standard buffer path so hardened deserialization can install its filter
            return (T) ExternalizableHelper.fromBinary(ExternalizableHelper.toBinary(inVal, serializer), serializer);
            }
        catch (Exception e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }
    }
