/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic.impl.paged.model;

import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.net.topic.Position;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.Objects;

/**
 * A representation of the position of a value in a paged topic independent
 * of a channel.
 *
 * @author Jonathan Knight  2021.05.03
 * @since 21.06
 */
public class PagedPosition
        implements Position, PortableObject, ExternalizableLite
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor for serialization.
     */
    public PagedPosition()
        {
        this(Page.NULL_PAGE, NULL_OFFSET);
        }

    /**
     * Create a {@link PagedPosition}.
     *
     * @param nPage    the page within the channel
     * @param nOffset  the offset within the page
     */
    public PagedPosition(long nPage, int nOffset)
        {
        m_nPage   = nPage;
        m_nOffset = nOffset;
        }

    // ----- accessors ------------------------------------------------------

    /**
     * Returns the page number within the channel.
     *
     * @return the page number within the channel
     */
    public long getPage()
        {
        return m_nPage;
        }

    /**
     * Returns the offset within the page.
     *
     * @return the offset within the page
     */
    public int getOffset()
        {
        return m_nOffset;
        }

    /**
     * Set the offset within the page.
     *
     * @param nOffset offset within the page
     */
    public void setOffset(int nOffset)
        {
        m_nOffset = nOffset;
        }

    /**
     * Return the next position after this position.
     *
     * @return the next position after this position
     */
    public PagedPosition next()
        {
        if (m_nPage == Page.EMPTY)
            {
            return this;
            }
        if (m_nOffset == Integer.MAX_VALUE)
            {
            return new PagedPosition(m_nPage + 1, 0);
            }
        return new PagedPosition(m_nPage, m_nOffset + 1);
        }

    // ----- Comparable methods ---------------------------------------------

    @Override
    public int compareTo(Position other)
        {
        int nResult = -1;
        if (other instanceof PagedPosition)
            {
            nResult = Long.compare(m_nPage, ((PagedPosition) other).m_nPage);
            if (nResult == 0)
                {
                nResult = Integer.compare(m_nOffset, ((PagedPosition) other).m_nOffset);
                }
            }
        return nResult;
        }

    // ----- ExternalizableLite methods -------------------------------------

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        m_nPage   = in.readLong();
        m_nOffset = in.readInt();
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        out.writeLong(m_nPage);
        out.writeInt(m_nOffset);
        }

    // ----- PortableObject methods -----------------------------------------

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        m_nPage = in.readLong(0);
        m_nOffset = in.readInt(1);
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeLong(0, m_nPage);
        out.writeInt(1, m_nOffset);
        }

    // ----- Object methods -----------------------------------------------------

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
        PagedPosition that = (PagedPosition) o;
        return m_nPage == that.m_nPage && m_nOffset == that.m_nOffset;
        }

    @Override
    public int hashCode()
        {
        return Objects.hash(m_nPage, m_nOffset);
        }

    @Override
    public String toString()
        {
        if (m_nPage == Page.EMPTY && m_nOffset == NULL_OFFSET)
            {
            return "PagedPosition(None)";
            }
        return "PagedPosition("
                + "page=" + m_nPage
                + ", offset=" + m_nOffset
                + ')';
        }

    // ----- constants ------------------------------------------------------

    /**
     * A singleton null position.
     */
    public static final PagedPosition NULL_POSITION = new PagedPosition(Page.EMPTY, Integer.MAX_VALUE);

    /**
     * The null page offset.
     */
    public static final int NULL_OFFSET = -1;

    // ----- data members ---------------------------------------------------

    /**
     * The page within the channel.
     */
    private long m_nPage;

    /**
     * The offset within the page.
     */
    private int m_nOffset;
    }
