/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.internal.extend.message.response;

import com.google.protobuf.Int32Value;
import com.google.protobuf.Message;

import com.tangosol.coherence.component.net.extend.message.response.GrpcResponse;

import java.net.URI;

/**
 * A {@link GrpcResponse} that produces an
 * {@link Int32Value} from a channel URI.
 *
 * @author Jonathan Knight  2024.11.26
 */
public class ChannelUriResponse
        extends BaseProxyResponse
    {
    public ChannelUriResponse()
        {
        }

    public int getChannelId()
        {
        int id = m_nChannelId;
        if (id < 0)
            {
            String sURI = getURIValue();
            URI    uri  = URI.create(sURI);
            id = m_nChannelId = Integer.parseInt(uri.getSchemeSpecificPart());
            }
        return id;
        }

    @Override
    public int getProxyId()
        {
        return getChannelId();
        }

    @Override
    public Message getMessage()
        {
        return Int32Value.of(getChannelId());
        }

    @Override
    public void setResult(Object oResult)
        {
        m_nChannelId = -1;
        super.setResult(oResult);
        }

    protected String getURIValue()
        {
        return (String) getResult();
        }

    // ----- data members ---------------------------------------------------

    /**
     * The channel identifier.
     */
    private int m_nChannelId = -1;
    }
