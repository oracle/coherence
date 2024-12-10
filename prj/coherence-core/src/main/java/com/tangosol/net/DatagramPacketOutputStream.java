/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net;


import java.io.OutputStream;
import java.io.IOException;

import java.net.DatagramPacket;


/**
* Provides an OutputStream on top of a DatagramPacket.
* <p>
* The flush() method must be invoked prior to sending the DatagramPacket.<br>
* The close() method must be invoked after sending the DatagramPacket; the
* OutputStream will remain open, but will have reset to the beginning of
* the packet buffer.
*
* @author cp  2001.01.05
*/
public class DatagramPacketOutputStream
    extends OutputStream
    {
    // ----- constructors ---------------------------------------------------

    public DatagramPacketOutputStream(DatagramPacket packet)
        {
        m_packet = packet;
        reset();
        }

    // ----- OutputStream methods -------------------------------------------

    /**
    * Writes the specified byte to this output stream.
    */
    public void write(int b)
            throws IOException
        {
        byte[] abDest = m_ab;
        int    ofDest = m_of;
        int    cbDest = m_cb;

        if (ofDest >= cbDest)
            {
            throw new IOException("buffer over-run");
            }

        abDest[ofDest] = (byte) b;
        m_of = ofDest + 1;
        }

    /**
    * Writes len bytes from the specified byte array starting at offset off
    * to this output stream.
    */
    public void write(byte[] abSrc, int ofSrc, int cbCopy)
            throws IOException
        {
        byte[] abDest = m_ab;
        int    ofDest = m_of;
        int    cbDest = m_cb;

        if (cbCopy > cbDest - ofDest)
            {
            throw new IOException("buffer over-run");
            }

        System.arraycopy(abSrc, ofSrc, abDest, ofDest, cbCopy);
        m_of = ofDest + cbCopy;
        }

    /**
    * Flushes this output stream and forces any buffered output bytes to be
    * written out.
    */
    public void flush()
            throws IOException
        {
        m_packet.setLength(m_of);
        }

    /**
    * Closes this output stream and releases any system resources associated
    * with this stream.
    */
    public void close()
        {
        reset();
        }


    // ----- methods --------------------------------------------------------

    /**
    * Reset the output stream back to the beginning of the buffer.
    */
    public void reset()
        {
        m_ab = m_packet.getData();
        m_of = m_packet.getOffset();
        m_cb = m_ab.length;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The packet to write into.
    */
    private DatagramPacket m_packet;

    /**
    * The byte array to write into.
    */
    private byte[] m_ab;

    /**
    * The current offset into the byte array to write at.
    */
    private int m_of;

    /**
    * The length of the buffer; this is equal to one greater than the largest
    * legal byte index into the buffer.
    */
    private int m_cb;
    }