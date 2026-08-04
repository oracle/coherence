/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io;

import com.tangosol.io.internal.SerializationAllowlist;

import java.lang.invoke.MethodHandles;

/**
 * Registration facade for Coherence-generated serialization helper classes.
 *
 * @author Vaso Putica  2026.05.11
 * @since 26.04
 */
public final class SerializationGeneratedClasses
    {
    /**
     * Register a REST-generated partial projection class.
     *
     * @param lookup  caller proof from the REST {@code PartialObject} class
     * @param clz     generated partial projection class
     */
    public static void registerRestGeneratedPartialClass(MethodHandles.Lookup lookup, Class<?> clz)
        {
        SerializationAllowlist.registerRestGeneratedPartialClass(lookup, clz);
        }

    private SerializationGeneratedClasses()
        {
        }
    }
