/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.guides.partitions.books;

import com.tangosol.io.AbstractEvolvable;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * A representation of book sales data.
 *
 * @author Jonathan Knight 2023.01.14
 * @since 22.06.4
 */
public class BookSales
        extends AbstractEvolvable
        implements ExternalizableLite, PortableObject
    {
    /**
     * A default no-args constructor.
     */
    public BookSales()
        {
        }

    // ----- accessors ------------------------------------------------------

    /**
     * Return the E-Book sales.
     *
     * @return the E-Book sales
     */
    public long getEBookSales()
        {
        return eBook;
        }

    /**
     * Set the E-Book sales.
     *
     * @param amount  the E-Book sales
     */
    public void setEBookSales(long amount)
        {
        eBook = amount;
        }

    /**
     * Increment the E-Book sales.
     *
     * @param amount  the amount to increment E-Book sales
     */
    public void incrementEBookSales(long amount)
        {
        eBook += amount;
        }

    /**
     * Return the Audiobook sales.
     *
     * @return the Audiobook sales
     */
    public long getAudioSales()
        {
        return audio;
        }

    /**
     * Set the Audiobook sales.
     *
     * @param amount  the Audiobook sales
     */
    public void setAudioSales(long amount)
        {
        audio = amount;
        }

    /**
     * Increment the audiobook sales.
     *
     * @param amount  the amount to increment audiobook sales
     */
    public void incrementAudioSales(long amount)
        {
        audio += amount;
        }

    /**
     * Return the paper book sales.
     *
     * @return the paper book sales
     */
    public long getPaperSales()
        {
        return paper;
        }

    /**
     * Return the paper book sales.
     *
     * @param amount  the paper book sales
     */
    public void setPaperSales(long amount)
        {
        paper = amount;
        }

    /**
     * Increment the paper book sales.
     *
     * @param amount  the amount to increment paper book sales
     */
    public void incrementPaperSales(long amount)
        {
        paper += amount;
        }

    // ----- serialization methods ------------------------------------------

    @Override
    public int getImplVersion()
        {
        return IMPLEMENTATION_VERSION;
        }

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        eBook = in.readLong();
        audio = in.readLong();
        paper = in.readLong();
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        out.writeLong(eBook);
        out.writeLong(audio);
        out.writeLong(paper);
        }

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        eBook = in.readLong(0);
        audio = in.readLong(1);
        paper = in.readLong(2);
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeLong(0, eBook);
        out.writeLong(1, audio);
        out.writeLong(2, paper);
        }

    // ----- data members ---------------------------------------------------

    /**
     * The evolvable POF implementation version of this class.
     */
    public static final int IMPLEMENTATION_VERSION = 1;

    /**
     * The number of e-books sold.
     */
    private long eBook;
    
    /**
     * The number of audiobooks sold.
     */
    private long audio;
    
    /**
     * The number of paper sold.
     */
    private long paper;
    }
