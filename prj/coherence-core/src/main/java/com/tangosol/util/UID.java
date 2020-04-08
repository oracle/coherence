/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import java.net.InetAddress;

import java.util.Date;
import java.util.Random;

import javax.json.bind.annotation.JsonbProperty;


/**
* A UID is a 128-bit identifier that is almost guaranteed to be unique.
*
* @author cp  2000.09.12
*/
public class UID
        extends Base
        implements Comparable, Serializable, ExternalizableLite, PortableObject
    {
    // ----- command line ---------------------------------------------------

    /**
    * Allow UID identifiers to be generated or parsed at the command line.
    * For example, to generate 10 identifiers:
    *
    *   java com.tangosol.util.UID 10
    */
    public static void main(String[] asArgs)
        {
        try
            {
            if (asArgs.length >= 2)
                {
                InetAddress addr  = InetAddress.getByName(asArgs[0]);
                int         nCnt  = Integer.parseInt(asArgs[1]);
                int         nAddr = (int) (toLong(addr) & 0xFFFFFFFFL);

                out(new UID(nAddr, new Date().getTime(), nCnt));
                }
            else
                {
                int cIds = Math.max(1, Integer.parseInt(asArgs[0]));
                for (int i = 0; i < cIds; ++i)
                    {
                    UID    uid  = new UID();
                    String sUID = uid.toString();

                    azzert(uid.equals(new UID(sUID)));

                    out(uid);
                    }
                }
            }
        catch (Exception e)
            {
            try
                {
                UID uid = new UID(asArgs[0]);

                out("Address  =" + toString(uid.getAddress()));
                out("Timestamp=" + new Date(uid.getTimestamp()));
                out("Port     =" + (uid.getCount() & 0x0000FFFF));
                out("MachineId=" + ((uid.getCount() & 0xFFFF0000) >>> 16));
                }
            catch (Exception ex)
                {
                out("Usage:");
                out("   java com.tangosol.util.UID <number> | <UID> | (<IP> <count>)");
                }
            }
        }


    // ----- constructors ---------------------------------------------------

    /**
    * Construct a UID (default constructor).
    */
    public UID()
        {
        m_nAddr     = s_nHostAddr;
        m_lDatetime = getSafeTimeMillis();
        synchronized (LOCK)
            {
            m_nCount = ++s_nLastCount;
            }
        }

    /**
    * Construct a UID from its constituent members (advanced constructor).
    *
    * @param nAddr      the InetAddress-related portion of the UID
    * @param lDatetime  the creation date/time millis portion of the UID
    * @param nCount     the counter portion of the UID
    */
    public UID(int nAddr, long lDatetime, int nCount)
        {
        m_nAddr     = nAddr;
        m_lDatetime = lDatetime;
        m_nCount    = nCount;
        }

    /**
    * Construct a UID from a byte array.
    *
    * @param ab  a byte array as would be returned from UID.toByteArray()
    */
    public UID(byte[] ab)
        {
        azzert(ab != null && ab.length == 16);

        m_nAddr     = (int)  (ab[ 0] & 0xFF) << 24
                    | (int)  (ab[ 1] & 0xFF) << 16
                    | (int)  (ab[ 2] & 0xFF) <<  8
                    | (int)  (ab[ 3] & 0xFF);
        m_lDatetime = (long) (ab[ 4] & 0xFF) << 56
                    | (long) (ab[ 5] & 0xFF) << 48
                    | (long) (ab[ 6] & 0xFF) << 40
                    | (long) (ab[ 7] & 0xFF) << 32
                    | (long) (ab[ 8] & 0xFF) << 24
                    | (long) (ab[ 9] & 0xFF) << 16
                    | (long) (ab[10] & 0xFF) <<  8
                    | (long) (ab[11] & 0xFF);
        m_nCount    = (int)  (ab[12] & 0xFF) << 24
                    | (int)  (ab[13] & 0xFF) << 16
                    | (int)  (ab[14] & 0xFF) <<  8
                    | (int)  (ab[15] & 0xFF);
        }

    /**
    * Construct a UID from a String.
    *
    * @param s  a String as would be returned from UID.toString()
    */
    public UID(String s)
        {
        this(parseHex(s));
        }

    /**
    * Construct a UID from a stream.
    *
    * This is a custom serialization implementation that is unrelated to the
    * Serializable interface and the Java implementation of persistence.
    *
    * @param stream  an object implementing java.io.DataInput that contains
    *                the internal information that a previous UID wrote
    *
    * @exception IOException if an error occurs reading from the stream
    */
    public UID(DataInput stream)
            throws IOException
        {
        this(readBytes(stream));
        }

    /**
    * Internal method to support construction from DataInput.
    */
    private static byte[] readBytes(DataInput stream)
            throws IOException
        {
        byte[] ab = new byte [16];
        stream.readFully(ab);
        return ab;
        }


    // ----- UID data access ------------------------------------------------

    /**
    * Determine the internet address of the host that generated the UID
    * instance (or a random number if no internet address was available)
    *
    * @return a 4-byte integer value holding an internet address in the
    *         byte order that is used by InetAddress and LicensedObject
    */
    public int getAddress()
        {
        return m_nAddr;
        }

    /**
    * Determine the date/time value that the UID instance was generated.
    *
    * @return date/time value in millis that the UID instance was generated
    */
    public long getTimestamp()
        {
        return m_lDatetime;
        }

    /**
    * Determine the "counter" portion of the UID that ensures that two UIDs
    * generated at the same exact time by the same process are unique.
    *
    * @return a number that helps to make the UID unique
    */
    public int getCount()
        {
        return m_nCount;
        }

    /**
    * Convert the UID to a printable String.
    *
    * @return the UID data as a 0x-prefixed hex string.
    */
    public String toString()
        {
        return toHexEscape(toByteArray());
        }

    /**
    * Convert the UID to a byte array of 16 bytes.
    *
    * @return the UID data as a byte array of 16 bytes
    */
    public byte[] toByteArray()
        {
        byte[] ab        = new byte[16];
        int    nAddr     = m_nAddr;
        long   lDatetime = m_lDatetime;
        int    nCount    = m_nCount;

        ab[ 0] = (byte) (nAddr >>> 24);
        ab[ 1] = (byte) (nAddr >>> 16);
        ab[ 2] = (byte) (nAddr >>> 8);
        ab[ 3] = (byte) nAddr;
        ab[ 4] = (byte) (lDatetime >>> 56);
        ab[ 5] = (byte) (lDatetime >>> 48);
        ab[ 6] = (byte) (lDatetime >>> 40);
        ab[ 7] = (byte) (lDatetime >>> 32);
        ab[ 8] = (byte) (lDatetime >>> 24);
        ab[ 9] = (byte) (lDatetime >>> 16);
        ab[10] = (byte) (lDatetime >>> 8);
        ab[11] = (byte) lDatetime;
        ab[12] = (byte) (nCount >>> 24);
        ab[13] = (byte) (nCount >>> 16);
        ab[14] = (byte) (nCount >>> 8);
        ab[15] = (byte) nCount;

        return ab;
        }

    /**
    * Convert the UID to a character array of 32 hex digits.
    *
    * @return the UID data as a character array of 32 hex digits
    */
    public char[] toCharArray()
        {
        return toHex(toByteArray()).toCharArray();
        }


    // ----- hashing and comparison support ---------------------------------

    /**
    * Determine if two UIDs are equal.
    *
    * @param o  the other UID
    *
    * @return true if the passed object is equal to this
    */
    public boolean equals(Object o)
        {
        if (o instanceof UID)
            {
            UID that = (UID) o;
            return this.m_nAddr     == that.m_nAddr
                && this.m_lDatetime == that.m_lDatetime
                && this.m_nCount    == that.m_nCount;
            }

        return false;
        }

    /**
    * Compares this object with the specified object for order.  Returns a
    * negative integer, zero, or a positive integer as this object is less
    * than, equal to, or greater than the specified object.<p>
    *
    * @param   o  the Object to be compared.
    *
    * @return  a negative integer, zero, or a positive integer as this object
    *          is less than, equal to, or greater than the specified object
    *
    * @throws  ClassCastException if the specified object's type prevents it
    *          from being compared to this Object
    */
    public int compareTo(Object o)
        {
        UID that = (UID) o;

        if (this.m_lDatetime != that.m_lDatetime)
            {
            return this.m_lDatetime < that.m_lDatetime ? -1 : 1;
            }

        if (this.m_nAddr != that.m_nAddr)
            {
            return this.m_nAddr < that.m_nAddr ? -1 : 1;
            }

        if (this.m_nCount != that.m_nCount)
            {
            return this.m_nCount < that.m_nCount ? -1 : 1;
            }

        return 0;
        }

    /**
    * Determine a hash code for the UID object.
    *
    * @return a hash code reflecting the UID's data
    */
    public int hashCode()
        {
        return m_nAddr ^ (int) (m_lDatetime >>> 32) ^ (int) m_lDatetime ^ m_nCount;
        }


    // ----- persistence ----------------------------------------------------

    /**
    * Store this UID in a stream.
    *
    * This is a custom serialization implementation that is unrelated to the
    * Serializable interface and the Java implementation of persistence.
    *
    * @param stream  an object implementing java.io.DataOutput that this UID
    *                should write its internal information to.
    *
    * @exception IOException if an error occurs writing to the stream
    */
    public void save(DataOutput stream)
            throws IOException
        {
        stream.write(toByteArray());
        }


    // ----- shared members -------------------------------------------------

    /**
    * Random number generator.
    */
    private static Random s_rnd = getRandom();

    /**
    * The IP address of the host or a random number if none available.
    */
    private static int s_nHostAddr;

    /**
    * The last integer counter used to make a UID pseudo-unique.
    */
    private static int s_nLastCount = s_rnd.nextInt();

    /**
    * A mutex for accessing/updating s_nLastCount.
    */
    private static final Object LOCK = new Object();

    /**
    * Determine the IP address of this host or use a random number if
    * an address is not available.
    */
    static
        {
        s_nHostAddr = s_rnd.nextInt();
        try
            {
            byte[] abIP = InetAddress.getLocalHost().getAddress();
            int    nIP  = (abIP[0] & 0xFF) << 24
                        | (abIP[1] & 0xFF) << 16
                        | (abIP[2] & 0xFF) <<  8
                        | (abIP[3] & 0xFF);
            if (nIP != 0x00000000 && nIP != 0x7F000001)
                {
                s_nHostAddr = nIP;
                }
            }
        catch (Exception e)
            {
            }
        }


    // ----- ExternalizableLite interface -----------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(DataInput in)
            throws IOException
        {
        m_nAddr     = in.readInt();
        m_lDatetime = in.readLong();
        m_nCount    = in.readInt();
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(DataOutput out)
            throws IOException
        {
        out.writeInt (m_nAddr);
        out.writeLong(m_lDatetime);
        out.writeInt (m_nCount);
        }


    // ----- PortableObject interface ---------------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(PofReader in)
            throws IOException
        {
        m_nAddr     = in.readInt(0);
        m_lDatetime = in.readLong(1);
        m_nCount    = in.readInt(2);
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(PofWriter out)
            throws IOException
        {
        out.writeInt (0, m_nAddr);
        out.writeLong(1, m_lDatetime);
        out.writeInt (2, m_nCount);
        }


    // ----- Serializable interface -----------------------------------------

    /**
    * Write this object to an ObjectOutputStream.
    *
    * @param out  the ObjectOutputStream to write this object to
    *
    * @throws IOException  thrown if an exception occurs writing this object
    */
    private void writeObject(ObjectOutputStream out)
            throws IOException
        {
        out.writeInt(m_nAddr);
        out.writeLong(m_lDatetime);
        out.writeInt(m_nCount);
        }

    /**
    * Read this object from an ObjectInputStream.
    *
    * @param in  the ObjectInputStream to read this object from
    *
    * @throws IOException  if an exception occurs reading this object
    * @throws ClassNotFoundException  if the class for an object being
    *         read cannot be found
    */
    private void readObject(ObjectInputStream in)
            throws IOException, ClassNotFoundException
        {
        m_nAddr     = in.readInt();
        m_lDatetime = in.readLong();
        m_nCount    = in.readInt();
        }


    // ----- helpers --------------------------------------------------------

    /**
    * Converts and IP address to a long value.
    *
    * @param addr  an instance of InetAddress to convert to a long
    *
    * @return  a long value holding the IP address
    */
    public static long toLong(InetAddress addr)
        {
        byte[] ab = addr.getAddress();
        return ((((long) ab[0]) & 0xFFL) << 24)
               | ((((long) ab[1]) & 0xFFL) << 16)
               | ((((long) ab[2]) & 0xFFL) <<  8)
               | ((((long) ab[3]) & 0xFFL)      );
        }

    /**
    * Converts a long value to an address string in the form returned by
    * InetAddress#getHostAddress.
    *
    * @param l  the long value holding the IP address
    *
    * @return  the IP address string "%d.%d.%d.%d"
    */
    public static String toString(long l)
        {
        return ((l & 0xFF000000L) >>> 24) + "." +
               ((l & 0x00FF0000L) >>> 16) + "." +
               ((l & 0x0000FF00L) >>>  8) + "." +
               ((l & 0x000000FFL)       );
        }


    // ----- data members ---------------------------------------------------

    /**
    * Internet address of host that generated the UID instance (or a
    * random number).
    */
    @JsonbProperty("address")
    private int m_nAddr;

    /**
    * Date/time value that the UID instance was generated.
    */
    @JsonbProperty("dateTime")
    private long m_lDatetime;

    /**
    * A number that helps to make the UID unique.
    */
    @JsonbProperty("count")
    private int m_nCount;
    }
