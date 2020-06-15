/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.memcached;

import com.oracle.coherence.common.base.Disposable;

import com.tangosol.net.cache.KeyAssociation;

import java.io.DataInput;

/**
 * Memcached request interface.
 *
 * @author bb 2013.05.01
 *
 * @since Coherence 12.1.3
 */
public interface Request
        extends Disposable, KeyAssociation
    {
    /**
     * Return the request op code.
     *
     * @return the request op code
     */
    public int getOpCode();

    /**
     * Return the extra data associated with this request; the content is operation specific.
     *
     * @return the extra data associated with the request
     */
    public DataInput getExtras();

    /**
     * Return the key associated with this request.
     *
     * @return key the key associated with this request
     */
    public String getKey();

    /**
     * Return the value associated with this request.
     *
     * @return the value associated with this request.
     */
    public byte[] getValue();

    /**
     * Return the version associated with this request.
     *
     * @return version associated with this request
     */
    public long getVersion();

    /**
     * Return the Response object for this request.
     *
     * @return the Response object
     */
    public Response getResponse();
    }