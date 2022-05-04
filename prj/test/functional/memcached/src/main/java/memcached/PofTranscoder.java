/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package memcached;


import com.tangosol.io.pof.ConfigurablePofContext;
import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;

import net.spy.memcached.CachedData;
import net.spy.memcached.compat.SpyObject;
import net.spy.memcached.transcoders.Transcoder;


public class PofTranscoder<T> extends SpyObject implements Transcoder<T>
    {

    public PofTranscoder(String sLocator)
        {
        m_ctx = new ConfigurablePofContext(sLocator);
        }

    @Override
    public boolean asyncDecode(CachedData arg0)
        {
        return Boolean.FALSE;
        }

    @Override
    public T decode(CachedData cachedData)
        {
        int nFlag = cachedData.getFlags();
        Binary bin = new Binary(cachedData.getData());
        return (T) ExternalizableHelper.fromBinary(bin, m_ctx);
        }

    @Override
    public CachedData encode(Object obj)
        {

        byte[] oValue = ExternalizableHelper.toByteArray(obj, m_ctx);
        return new CachedData(FLAG, oValue, CachedData.MAX_SIZE);
        }

    @Override
    public int getMaxSize()
        {
        return CachedData.MAX_SIZE;
        }

    protected ConfigurablePofContext m_ctx;

    protected static final int       FLAG = 4;

    }
