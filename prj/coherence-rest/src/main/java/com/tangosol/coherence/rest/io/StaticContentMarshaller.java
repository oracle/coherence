/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest.io;

import com.tangosol.coherence.rest.util.StaticContent;

import com.tangosol.util.Binary;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;

/**
 * A pass-through marshaller that simply converts HTTP entities into a
 * {@link StaticContent} and vice versa.
 *
 * @author as  2015.11.10
 */
public class StaticContentMarshaller
        implements Marshaller<StaticContent>
    {
    @Override
    public void marshal(StaticContent value, OutputStream out, MultivaluedMap<String, Object> httpHeaders)
            throws IOException
        {
        httpHeaders.putSingle(HttpHeaders.CONTENT_TYPE, value.getMediaType());
        value.getContent().writeTo(out);
        }

    @Override
    public StaticContent unmarshal(InputStream in, MediaType mediaType)
            throws IOException
        {
        Binary binContent = Binary.readBinary(in);
        String sMediaType = mediaType.getType() + "/" + mediaType.getSubtype();

        return new StaticContent(binContent, sMediaType);
        }
    }
