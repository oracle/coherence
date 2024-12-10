/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net.queue.processor;

import com.tangosol.internal.net.queue.model.QueueKey;

import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.EvolvablePortableObject;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;

import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.InvocableMap;


import javax.json.bind.annotation.JsonbProperty;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * An entry processor that returns the queue name has
 * for a given queue name.
 * <p>
 * This code was introduced mainly for non-Java clients that cannot
 * perform the same CRC32 hash on a String that the queue code
 * does when calling {@link QueueKey#calculateQueueHash}
 * <p>
 * This entry processor can be invoked against any key as it
 * does nothing with the entry it is invoked on. The entry
 * does not have to exist in the cache.
 */
public class QueueNameHash
        extends AbstractQueueProcessor<Object, Object, Integer>
        implements EvolvablePortableObject, ExternalizableLite
    {
    /**
     * Default constructor for serialization.
     */
    public QueueNameHash()
        {
        }

    /**
     * Create a {@link QueueNameHash} processor.
     *
     * @param sName  the queue name to obtain the hash for
     */
    public QueueNameHash(String sName)
        {
        m_sName = sName;
        }

    @Override
    public Integer process(InvocableMap.Entry<Object, Object> entry)
        {
        return QueueKey.calculateQueueHash(m_sName);
        }

    @Override
    public int getImplVersion()
        {
        return 0;
        }

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        m_sName = ExternalizableHelper.readSafeUTF(in);
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        ExternalizableHelper.writeSafeUTF(out, m_sName);
        }

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        m_sName = in.readString(0);
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeString(0, m_sName);
        }

    // ----- data members ---------------------------------------------------

    /**
     * The queue name to obtain the hash for.
     */
    @JsonbProperty("name")
    private String m_sName;
    }
