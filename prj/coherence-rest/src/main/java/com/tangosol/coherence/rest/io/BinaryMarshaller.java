/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest.io;

import com.tangosol.util.Binary;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

/**
 * A pass-through marshaller that simply converts HTTP entities into a
 * {@link Binary} and vice versa.
 *
 * @author as  2015.07.27
 */
public class BinaryMarshaller
        implements Marshaller<Binary>
    {
    @Override
    public void marshal(Binary value, OutputStream out, MultivaluedMap<String, Object> httpHeaders)
            throws IOException
        {
        value.writeTo(out);
        }

    @Override
    public Binary unmarshal(InputStream in, MediaType mediaType)
            throws IOException
        {
        return Binary.readBinary(in);
        }
    }
