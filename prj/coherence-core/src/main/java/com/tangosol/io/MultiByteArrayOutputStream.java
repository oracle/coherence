/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io;


import com.tangosol.util.Base;

import java.io.IOException;
import java.io.OutputStream;

import java.util.ArrayList;
import java.util.List;


/**
* An OutputStream that accumulates the written data to a series of byte
* arrays that do not exceed a specified size.
*
* @author cp  2001.11.13
*/
public class MultiByteArrayOutputStream
        extends OutputStream
        implements OutputStreaming
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a MultiByteArrayOutputStream to write to byte arrays of the
    * specified length.
    *
    * @param cbBlock  the number of bytes (maximum) per block
    */
    public MultiByteArrayOutputStream(int cbBlock)
        {
        this(cbBlock, 0, 0);
        }

    /**
    * Construct a MultiByteArrayOutputStream to write to byte arrays of the
    * specified length, leaving the specified amount of padding at the front
    * and back of each byte array.
    *
    * @param cbBlock     the number of data bytes (maximum) per block
    * @param cbPadFront  the number of additional bytes to allocate and
    *                    leave free at the front (start) of each block
    * @param cbPadBack   the number of additional bytes to allocate and
    *                    leave free at the back (end) of each block
    */
    public MultiByteArrayOutputStream(int cbBlock, int cbPadFront, int cbPadBack)
        {
        m_cbBlock    = cbBlock;
        m_cbPadFront = cbPadFront;
        m_cbPadBack  = cbPadBack;
        }


    // ----- OutputStream implementation ------------------------------------

    /**
    * Writes the specified byte to this output stream.
    *
    * @param      b   the <code>byte</code>.
    *
    * @exception  IOException  if an I/O error occurs. In particular,
    *             an <code>IOException</code> may be thrown if the
    *             output stream has been closed.
    */
    public void write(int b) throws IOException
        {
        check();

        // get current block data
        byte[] ab = m_ab;
        int    cb = (ab == null ? 0 : ab.length - m_cbPadBack);
        int    of = m_of;

        // check if the block has sufficient space (one byte)
        if (of >= cb)
            {
            requestCapacity(1);
            ab = m_ab;
            of = m_of;
            }

        // write byte
        ab[of] = (byte) b;

        // update offset
        m_of = of + 1;
        }

    /**
    * Writes <code>len</code> bytes from the specified byte array
    * starting at offset <code>off</code> to this output stream.
    * <p>
    * If <code>b</code> is <code>null</code>, a
    * <code>NullPointerException</code> is thrown.
    * <p>
    * If <code>off</code> is negative, or <code>len</code> is negative, or
    * <code>off+len</code> is greater than the length of the array
    * <code>b</code>, then an <tt>IndexOutOfBoundsException</tt> is thrown.
    *
    * @param      abSrc  the data
    * @param      ofSrc  the start offset in the data
    * @param      cbSrc  the number of bytes to write
    *
    * @exception  IOException  if an I/O error occurs. In particular,
    *             an <code>IOException</code> is thrown if the output
    *             stream is closed.
    */
    public void write(byte[] abSrc, int ofSrc, int cbSrc) throws IOException
        {
        check();

        // get current block data
        byte[] ab     = m_ab;
        int    of     = m_of;
        int    cbBack = m_cbPadBack;

        while (cbSrc > 0)
            {
            // check if the block has any free space
            int cb = (ab == null ? 0 : ab.length - cbBack);
            if (of >= cb)
                {
                m_of = of;
                cb = requestCapacity(cbSrc);
                ab = m_ab;
                of = m_of;
                }

            // copy bytes
            int cbMax    = cb - of;
            int cbActual = Math.min(cbSrc, cbMax);
            System.arraycopy(abSrc, ofSrc, ab, of, cbActual);

            // update offsets etc.
            of    += cbActual;
            ofSrc += cbActual;
            cbSrc -= cbActual;
            }

        // store current block data
        m_ab = ab;
        m_of = of;
        }

    /**
    * Flush any accumulated bytes.
    *
    * @exception  IOException  if an I/O error occurs
    */
    public void flush() throws IOException
        {
        check();
        }

    /**
    * Close the stream, flushing any accumulated bytes.
    *
    * @exception  IOException  if an I/O error occurs
    */
    public void close() throws IOException
        {
        if (!m_fClosed)
            {
            flush();
            m_fClosed = true;
            }
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Create a human readable string representing the data written to the
    * stream.
    *
    * @return a String representation of the stream's contents
    */
    public String toString()
        {
        StringBuffer sb = new StringBuffer();

        int cBlocks = getBlockCount();
        sb.append("Results: ")
          .append(cBlocks)
          .append(" blocks:");

        for (int i = 0; i < cBlocks; ++i)
            {
            sb.append("\nBlock ")
              .append(i)
              .append(": ");

            byte[] ab = getBlock(i);
            sb.append(ab == null ? "null" : Base.toHexEscape(ab));
            }

        return sb.toString();
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Determine the number of blocks that have been written thus far.
    *
    * @return the number of blocks (byte arrays) of output that have any data
    */
    public int getBlockCount()
        {
        // An "empty" active block doesn't count
        return m_of == m_cbPadFront ? m_cBlocks : m_cBlocks + 1;
        }

    /**
    * Obtain the specified block of data.
    *
    * @param i  block index in the range [0..getBlockCount()]; passing the
    *           getBlockCount() will return the active block
    *
    * @return the specified block (byte array) of output
    */
    public byte[] getBlock(int i)
        {
        if (i == m_cBlocks)
            {
            // retrieve the active block (common case for short streams)
            return m_ab;
            }
        else
            {
            // retrieve a flushed block
            List listBlock = m_listBlock;
            if (listBlock == null)
                {
                throw new IllegalArgumentException("invalid block index: " + i);
                }
            return (byte[]) listBlock.get(i);
            }
        }

    /**
    * Determine the specific number of bytes of data stored in the specified
    * block.
    *
    * @param i  block index in the range [0..getBlockCount()]; passing the
    *           getBlockCount() will return the size of the active block
    *
    * @return the number of data bytes in a block
    */
    public int getBlockDataSize(int i)
        {
        // allow size computation of the active block
        return i == m_cBlocks ? m_of : m_cbBlock;
        }

    /**
    * Determine the maximum number of bytes of data that will be stored in
    * each block.
    *
    * @return the number of data bytes (maximum) per block
    */
    public int getBlockDataSize()
        {
        return m_cbBlock;
        }

    /**
    * Determine the number of extra bytes of padding that will be allocated
    * and left blank at the start of each block in front of the data portion.
    *
    * @return the number of additional bytes to allocate and leave free at
    *         the front (start) of each block
    */
    public int getFrontPaddingSize()
        {
        return m_cbPadFront;
        }

    /**
    * Determine the number of extra bytes of padding that will be allocated
    * and left blank at the end of each block after the data portion.
    *
    * @return the number of additional bytes to allocate and leave free at
    *         the back (end) of each block
    */
    public int getBackPaddingSize()
        {
        return m_cbPadBack;
        }


    // ----- internal -------------------------------------------------------

    /**
    * Verify that the stream is still open.
    */
    protected void check() throws IOException
        {
        if (m_fClosed)
            {
            throw new IOException("MultiByteArrayOutputStream is closed");
            }
        }

    /**
    * Ensure that the current block contains some available capacity, preferably
    * enough to fulfill the specified capacity. As a result of calling this
    * method m_ab and m_of may change. If this call returns then the current
    * block is guarenteed to contain at least one free byte of available
    * capacity.
    *
    * @param cbMore  the requested capacity
    *
    * @return the size of the current block
    */
    protected int requestCapacity(int cbMore)
        {
        int    cbMax   = m_cbBlock;
        int    cbFront = m_cbPadFront;
        int    cbPad   = m_cbPadBack + cbFront;
        byte[] abOld   = m_ab;

        if (cbMore == 0)
            {
            return abOld == null ? 0 : abOld.length - cbPad;
            }

        if (abOld == null)
            {
            // first allocation, start with a reasonably small size
            int cbNew = Math.min(cbMax, Math.max(cbMore, 2048));
            m_ab = new byte[cbNew + cbPad];
            m_of = cbFront;
            return cbNew;
            }

        int cbOld = abOld.length - cbPad;
        if (cbOld == cbMax)
            {
            // full block, save and allocate a new max size block
            List listBlock = m_listBlock;
            if (listBlock == null)
                {
                m_listBlock = listBlock = new ArrayList();
                }
            listBlock.add(abOld);
            ++m_cBlocks;

            m_ab = new byte[cbMax + cbPad];
            m_of = cbFront;
            return cbMax;
            }
        else
            {
            // resizeable block, increase size aggressively
            int    cbNew = Math.min(cbMax, cbOld + Math.max(cbOld, cbMore));
            byte[] abNew = new byte[cbNew + cbPad];

            // copy data portion of old block into resized block
            System.arraycopy(abOld, cbFront, abNew, cbFront, cbOld);
            m_ab = abNew;
            return cbNew;
            }
        }


    // ----- data members ---------------------------------------------------

    /**
    * The current block of output.
    */
    protected byte[] m_ab;

    /**
    * The offset into the current block of output.
    */
    protected int m_of;

    /**
    * The max size of each block.
    */
    protected int m_cbBlock;

    /**
    * The number of additional bytes to allocate and leave free at the front
    * (start) of each block
    */
    protected int m_cbPadFront;

    /**
    * The number of additional bytes to allocate and leave free at the back
    * (end) of each block
    */
    protected int m_cbPadBack;

    /**
    * The list of blocks.
    */
    protected List m_listBlock;

    /**
    * The number of flushed blocks.
    */
    protected int m_cBlocks;

    /**
    * True after close is invoked.
    */
    protected boolean m_fClosed;
    }
