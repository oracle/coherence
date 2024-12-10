/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic.impl.paged.model;

import com.tangosol.io.ReadBuffer;

import com.tangosol.net.partition.KeyPartitioningStrategy;

import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.HashHelper;

import java.io.IOException;

/**
 * This class represents the position of an element in a topic.
 * The position is made up of three parts, the channel, the
 * page id, and the id of the element within the page. As elements
 * are added to a topic they are added to a specific page and given that
 * pages id and the next sequential element id within that
 * page.
 *
 * @author jk 2015.05.16
 * @since Coherence 14.1.1
 */
// implementation note: ContentKey does not implement Evolvable
// because adding fields would affect the "equality"
// of a key. Used as key in Cache PagedTopicsCaches#CONTENT.
public class ContentKey
        implements KeyPartitioningStrategy.PartitionAwareKey
    {
    // ----- constructors ---------------------------------------------------
    /**
     * Create a {@link ContentKey}.
     *
     * @param nChannel  the id of the channel
     * @param lPage     the id of the page this key belongs to
     * @param nElement  the id of the element within the page
     */
    public ContentKey(int nChannel, long lPage, int nElement)
        {
        m_nChannel = nChannel;
        m_lPage    = lPage;
        m_nElement = nElement;
        }

    // ----- accessor methods -----------------------------------------------

    /**
     * Return the channel id.
     *
     * @return the channel id.
     */
    public int getChannel()
        {
        return m_nChannel;
        }

    /**
     * Return the page id.
     *
     * @return the page id
     */
    public long getPage()
        {
        return m_lPage;
        }

    /**
     * Return the element id.
     *
     * @return the element id
     */
    public int getElement()
        {
        return m_nElement;
        }

    @Override
    public int getPartitionId()
        {
        return Page.Key.mapPageToPartition(m_nChannel, m_lPage);
        }

    /**
     * Helper to access the EH.FMT_IDO constant.
     */
    private static class DecoHelper
        extends ExternalizableHelper
        {
        static final byte FMT_IDO = ExternalizableHelper.FMT_IDO;
        }

    /**
     * Encode a ContentKey into a Binary.
     *
     * @return the Binary
     */
    public Binary toBinary(int cPartitions)
        {
        return toBinary(getPartitionId() % cPartitions, getChannel(), getPage(), getElement());
        }

    /**
     * Encode a ContentKey into a Binary.
     *
     * @param nPartition  the partition
     * @param nChannel    the channel
     * @param lPage       the page
     * @param nElement    the element
     *
     * @return the Binary
     */
    public static Binary toBinary(int nPartition, int nChannel, long lPage, int nElement)
        {
        // Profiling shows that a significant portion of the server time is spent serializing these very simple ContentKey
        // objects.  As such we need it to be as optimal as possible.  The change to using this direct serialization
        // resulted in at 25% performance improvement in the TopicPerformanceTest run over the network (IB) for 1KB values.

        byte[] ab = new byte[1 + 5 + 5 + 10 + 5];
        int    cb = 0;

        ab[cb++] = DecoHelper.FMT_IDO;

        cb += pack(nPartition, ab, cb); // this is the only portion which is required to be packed
        cb += pack(nChannel,   ab, cb);
        cb += pack(lPage,      ab, cb);
        cb += pack(nElement,   ab, cb);

        return new Binary(ab, 0, cb);
        }

    /**
     * Decode a Binary into a ContentKey
     *
     * @param bin  the binary
     *
     * @return the ContentKey
     */
    public static ContentKey fromBinary(Binary bin)
        {
        return fromBinary(bin, /*fDecorated*/ false);
        }

    /**
     * Decode a Binary into a ContentKey
     *
     * @param bin         the binary
     * @param fDecorated  true iff the binary is decorated
     *
     * @return the ContentKey
     */
    public static ContentKey fromBinary(Binary bin, boolean fDecorated)
        {
        try
            {
            ReadBuffer.BufferInput in = bin.getBufferInput();

            if (fDecorated)
                {
                // skip decorations
                byte bDeco = (byte) in.readByte();
                int  nPart = in.readPackedInt();
                }

            int  nChannel  = in.readPackedInt();
            long lPage     = in.readPackedLong();
            int  nElement  = in.readPackedInt();

            return new ContentKey(nChannel, lPage, nElement);
            }
        catch (IOException e)
            {
            throw new IllegalStateException(e);
            }
        }

    /**
     * Write out the specified long in packed format.
     *
     * @param l  the value to write
     * @param ab the byte array to write to
     * @param of the offset to write at
     *
     * @return the number of bytes written
     */
    private static int pack(long l, byte[] ab, int of)
        {
        int cb = 0;

        // first byte contains sign bit (bit 7 set if neg)
        int b = 0;
        if (l < 0)
            {
            b = 0x40;
            l = ~l;
            }

        // first byte contains only 6 data bits
        b |= (byte) (((int) l) & 0x3F);
        l >>>= 6;

        while (l != 0)
            {
            b |= 0x80;          // bit 8 is a continuation bit
            ab[of + cb++] = (byte) b;

            b = (((int) l) & 0x7F);
            l >>>= 7;
            }

        ab[of + cb++] = (byte) b;

        return cb;
        }

    // ----- Object methods -------------------------------------------------

    @Override
    public boolean equals(Object o)
        {
        if (this == o)
            {
            return true;
            }

        if (o == null || getClass() != o.getClass())
            {
            return false;
            }

        ContentKey key = (ContentKey) o;

        return m_nChannel == key.m_nChannel && m_lPage == key.m_lPage && m_nElement == key.m_nElement;
        }

    @Override
    public int hashCode()
        {
        int hash = HashHelper.hash(m_lPage, 31);

        return HashHelper.hash(m_nElement, hash);
        }

    @Override
    public String toString()
        {
        return getClass().getSimpleName() + "(channel=" + m_nChannel + ", page=" + m_lPage + ", element=" + m_nElement + ')';
        }

    // ----- data members ---------------------------------------------------

    /**
     * The channel within the topic.
     */
    private final int m_nChannel;

    /**
     * The page id within the channel.
     */
    private final long m_lPage;

    /**
     * The element within the page.
     */
    private final int m_nElement;
    }
