/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io.pof.reflect;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;

import java.io.IOException;

/**
 * A Codec provides an interception point for any specific code that needs to
 * be executed pre or post (de)serialization. In the case of deserialization
 * this could be to return a concrete implementation and with serialization
 * this could be to explicitly call a specific method on {@link PofWriter}
 * that is not carried out by {@link PofWriter#writeObject(int, Object)}.
 *
 * @author hr
 *
 * @since 3.7.1
 */
public interface Codec
    {
    /**
     * Deserialize an object from the provided {@link PofReader}.
     * Implementing this interface allows introducing specific return
     * implementations. 
     *
     * @param in     the PofReader to read from
     * @param iProp  the index of the POF property to deserialize
     *
     * @return a specific implementation of the POF property
     *
     * @throws IOException  if an I/O error occurs
     */
    public Object decode(PofReader in, int iProp) throws IOException;

    /**
     * Serialize an object using the provided {@link PofWriter}.
     * 
     * @param out    the PofWriter to read from
     * @param iProp  the index of the POF property to serialize
     * @param value  the value to serialize
     * 
     * @throws IOException  if an I/O error occurs
     */
    public void encode(PofWriter out, int iProp, Object value) throws IOException;
    }
