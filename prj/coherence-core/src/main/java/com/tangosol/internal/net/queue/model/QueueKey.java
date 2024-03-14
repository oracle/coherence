/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net.queue.model;

import com.oracle.coherence.common.base.Formatting;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.net.cache.KeyAssociation;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class QueueKey
        implements PortableObject, ExternalizableLite, KeyAssociation<Integer>, Comparable<QueueKey>
    {
    public QueueKey()
        {
        }

    public QueueKey(int nHash, long nId)
        {
        m_nHash = nHash;
        m_nId   = nId;
        }

    // ----- accessors ------------------------------------------------------

    public int getHash()
        {
        return m_nHash;
        }

    public long getId()
        {
        return m_nId;
        }

    public QueueKey next()
        {
        return new QueueKey(m_nHash, m_nId + 1);
        }

    public QueueKey prev()
        {
        return new QueueKey(m_nHash, m_nId - 1);
        }

    public QueueKey randomHead()
        {
        int n = Math.min(-1, -Math.abs(ThreadLocalRandom.current().nextInt()));
        return new QueueKey(m_nHash, n);
        }

    public QueueKey randomTail()
        {
        int n = Math.max(1, Math.abs(ThreadLocalRandom.current().nextInt()));
        return new QueueKey(m_nHash, n);
        }

    // ----- Comparable methods ---------------------------------------------

    @Override
    public int compareTo(QueueKey o)
        {
        int n = Integer.compare(m_nHash, o.m_nHash);
        if (n == 0)
            {
            n = Long.compare(m_nId, o.m_nId);
            }
        return n;
        }

    // ----- PartitionAwareKey methods --------------------------------------

    @Override
    public Integer getAssociatedKey()
        {
        return m_nHash;
        }

    // ----- PortableObject methods -----------------------------------------

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        m_nHash = in.readInt(0);
        m_nId   = in.readLong(1);
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeInt(0, m_nHash);
        out.writeLong(1, m_nId);
        }

    // ----- ExternalizableLite methods -------------------------------------

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        m_nHash = in.readInt();
        m_nId   = in.readLong();
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        out.writeInt(m_nHash);
        out.writeLong(m_nId);
        }

    // ----- Object methods -------------------------------------------------

    @Override
    public boolean equals(Object o)
        {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QueueKey queueKey = (QueueKey) o;
        return m_nHash == queueKey.m_nHash && m_nId == queueKey.m_nId;
        }

    @Override
    public int hashCode()
        {
        return Objects.hash(m_nHash, m_nId);
        }

    @Override
    public String toString()
        {
        return "QueueKey{" +
                "id=" + m_nId +
                ", hash=" + m_nHash +
                '}';
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Calculate the hash from the queue name.
     * <p/>
     * This hash will be consistent across versions and JVMs, which
     * is important as the hash is used in queue keys instead of the
     * queue name as a hash will take up far less space than a long
     * queue name String. This method does not use
     * {@link String#hashCode()}, just in case that is somehow not
     * consistent across versions.
     *
     * @param sQueueName  the name of the queue to hash
     *
     * @return the hash of the queue name
     */
    public static int calculateQueueHash(String sQueueName)
        {
        byte[] ab = sQueueName.getBytes(StandardCharsets.UTF_8);
        return Formatting.toCrc(ab, 0, ab.length, 0);
        }

    public static QueueKey head(String sQueue)
        {
        return new QueueKey(calculateQueueHash(sQueue), ID_HEAD);
        }

    public static QueueKey tail(String sQueue)
        {
        return new QueueKey(calculateQueueHash(sQueue), ID_TAIL);
        }

    // ----- constants ------------------------------------------------------

    /**
     * The fixed head identifier.
     */
    public static final long ID_HEAD = Long.MAX_VALUE;

    /**
     * The fixed tail identifier.
     */
    public static final long ID_TAIL = Long.MIN_VALUE;

    /**
     * The empty queue identifier.
     */
    public static final long EMPTY_ID = 0L;

    // ----- data members ---------------------------------------------------

    private int m_nHash;

    private long m_nId;
    }
