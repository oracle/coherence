/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.memcached.server;

import com.tangosol.io.ReadBuffer;
import com.tangosol.io.WriteBuffer;
import com.tangosol.io.ReadBuffer.BufferInput;
import com.tangosol.io.WriteBuffer.BufferOutput;

import com.tangosol.net.BackingMapManagerContext;

import com.tangosol.util.Base;
import com.tangosol.util.Binary;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.BinaryWriteBuffer;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.InvocableMap.Entry;

import java.io.IOException;

import java.util.Date;

/**
 * Memcached Helper class.
 *
 * @author bb 2013.05.01
 *
 * @since Coherence 12.1.3
 */
public abstract class MemcachedHelper
    {
    /**
     * Utility method to calculate expiry value. As per the spec:
     * <p>
     * The actual value sent may either be Unix time (number of seconds since January 1, 1970,
     * as a 32-bit value), or a number of seconds starting from current time. In the
     * latter case, this number of seconds may not exceed 60*60*24*30
     * (number of seconds in 30 days); if the number sent by a client is larger than
     * that, the server will consider it to be real Unix time value rather
     * than an offset from current time.
     *
     * @param nExpiry  expiry (relative or absolute in secs.)
     *
     * @return expiry  in millis
     */
    public static int calculateExpiry(int nExpiry)
        {
        return nExpiry <= MAX_RELATIVE_EXPIRY_TIME
                ? nExpiry * 1000
                : Math.max(0, (int) (new Date(nExpiry).getTime() - System.currentTimeMillis()));
        }

    /**
     * Utility method to decorate a binary value with memcached specific decoration.
     *
     * @param bin       the binary value to decorate
     * @param nFlag     the flag to be added to the decoration
     * @param lVersion  the version to be added to the decoration
     *
     * @return decorated binary
     */
    public static Binary decorateBinary(Binary bin, int nFlag, long lVersion)
        {
        WriteBuffer bufDeco = new BinaryWriteBuffer(12, 12);
        try
            {
            BufferOutput out = bufDeco.getBufferOutput();
            out.writeInt(nFlag);
            out.writeLong(lVersion);
            return ExternalizableHelper.decorate(bin, ExternalizableHelper.DECO_MEMCACHED, bufDeco.toBinary());
            }
        catch (IOException e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }

    /**
     * Utility method to extract value and decorations from a decorated binary.
     *
     * @param bin              the decorated binary
     * @param mgrCtx           the backing map mgr context
     * @param fBinaryPassThru  flag indicating if binary-pass-thru is configured
     *
     * @return the DataHolder
     */
    public static DataHolder convertToDataHolder(Binary bin, BackingMapManagerContext mgrCtx, boolean fBinaryPassThru)
        {
        byte[] abValue  = null;
        int    nFlag    = 0;
        long   lVersion = 0L;
        try
            {
            if (ExternalizableHelper.isDecorated(bin, ExternalizableHelper.DECO_MEMCACHED))
                {
                ReadBuffer  buffer   = bin;
                ReadBuffer  bufDeco  = ExternalizableHelper.getDecoration(buffer, ExternalizableHelper.DECO_MEMCACHED);
                BufferInput bufInput = bufDeco.getBufferInput();
                Binary      binValue = (Binary) ExternalizableHelper.getUndecorated(buffer);

                nFlag    = bufInput.readInt();
                lVersion = bufInput.readLong();
                abValue  = fBinaryPassThru
                               ? binValue.toByteArray()
                               : (byte[]) mgrCtx.getValueFromInternalConverter().convert(binValue);
                }
            else
                {
                // Coherence client added to cache directly
                abValue = bin.toByteArray();
                }
            }
        catch (IOException ioe)
            {
            throw Base.ensureRuntimeException(ioe);
            }

        return new DataHolder(abValue, nFlag, lVersion);
        }

    /**
     * Return a utf-8 encoded string representation of the specified byte array.
     *
     * @param abValue  the byte[] to convert to String
     *
     * @return a utf-8 encoded String
     */
    public static String getString(byte[] abValue)
        {
        try
            {
            return new String(abValue, UTF8);
            }
        catch (IOException ioe)
            {
            throw Base.ensureRuntimeException(ioe);
            }
        }

    /**
     * Convert InvocableMap.Entry to BinaryEntry.
     *
     * @param entry  InvocableMap.Entry
     *
     * @return BinaryEntry
     *
     * @throws RuntimeException if entry cannot be cast to BinaryEntry
     */
    public static BinaryEntry getBinaryEntry(Entry entry)
        {
        try
            {
            return (BinaryEntry) entry;
            }
        catch (ClassCastException cce)
            {
            throw new RuntimeException(
                    "The MemcachedAcceptor is only supported by the DistributedCache");
            }
        }

    // ----- constants ------------------------------------------------------

    /**
     * Maximum relative time period for calculating expiry times.
     */
    protected static final int MAX_RELATIVE_EXPIRY_TIME = 60 * 60 * 24 * 30; // 1 month

    /**
     * UTF-8 encoding.
     */
    protected static final String UTF8 = "utf-8";
    }