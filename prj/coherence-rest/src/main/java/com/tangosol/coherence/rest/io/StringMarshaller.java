/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest.io;

import com.tangosol.util.Base;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

/**
 * Marshaller that marshalls String object.
 *
 * @author lh 2015.11.19
 *
 * @since Coherence 12.2.1.0.1
 */
public class StringMarshaller
        implements Marshaller<String>
    {
    // ---- Marshaller implementation ---------------------------------------

    @Override
    public void marshal(String oValue, OutputStream out, MultivaluedMap<String, Object> httpHeaders)
        throws IOException
        {
        PrintStream stream = new PrintStream(out);
        stream.print(oValue);
        stream.flush();
        }

    @Override
    public String unmarshal(InputStream in, MediaType mediaType)
        throws IOException
        {
        return Base.read(new InputStreamReader(in));
        }
    }
