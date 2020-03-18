/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.memcached;

import com.oracle.coherence.common.base.Disposable;

import java.nio.ByteBuffer;

/**
 * Memcached Response Interface.
 *
 * @author bb 2013.05.01
 *
 * @since Coherence 12.1.3
 */
public interface Response
        extends Disposable
    {
    /**
     * Set the response code.
     *
     * @param nResponseCode  the response code to set
     *
     * @return this Response
     */
    public Response setResponseCode(int nResponseCode);

    /**
     * Get the response code.
     *
     * @return response code
     */
    public int getResponseCode();

    /**
     * Set the version for the entry.
     *
     * @param lVersion  the version to set
     *
     * @return this Response
     */
    public Response setVersion(long lVersion);

    /**
     * Set the key in the response.
     *
     * @param sKey  the Key to set
     *
     * @return this Response
     */
    public Response setKey(String sKey);

    /**
     * Set the value in the response.
     *
     * @param abValue  the value to set
     *
     * @return this Response
     */
    public Response setValue(byte[] abValue);

    /**
     * Set the extra data for the response.
     *
     * @param extras  payload for the extra data field
     *
     * @return this Response
     */
    public Response setExtras(ByteBuffer extras);

    // ----- inner enum: ResponseCode ---------------------------------------

    /**
     * Memcached Response Codes.
     */
    public static enum ResponseCode
        {
        OK(0x0000),
        KEYNF(0x0001),
        KEYEXISTS(0x0002),
        TOOLARGE(0x0003),
        INVARG(0x0004),
        NOT_STORED(0x0005),
        NAN(0x0006),
        AUTH_ERROR(0x0008),
        AUTH_CONTINUE(0x0009),
        UNKNOWN(0x0081),
        OOM(0x00082),
        NOT_SUPPORTED(0x0083),
        INTERNAL_ERROR(0x0084),
        BUSY(0x085),
        TEMPORARY_FAILURE(0x086);

        /**
         * Constructor.
         *
         * @param nCode  numeric Response code
         */
        ResponseCode(int nCode)
            {
            m_sCode = (short) nCode;
            }

        /**
         * Get the response code.
         *
         * @return numeric response code
         */
        public short getCode()
            {
            return m_sCode;
            }

        /**
         * Numeric response code.
         */
        protected short m_sCode;
        }
    }