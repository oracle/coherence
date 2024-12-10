/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PortableObject;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;

import com.tangosol.net.InetAddressHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.Externalizable;
import java.io.IOException;
import java.io.NotActiveException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.net.InetAddress;
import java.util.Random;


/**
* A UUID is a 256-bit identifier that, if it is generated, is statistically
* guaranteed to be unique.
*
* @author cp  2004.06.24
*/
public final class UUID
        extends Base
        implements Comparable, Externalizable, ExternalizableLite, PortableObject
    {
    // ----- command line ---------------------------------------------------

    /**
    * Allow UUID identifiers to be generated or parsed at the command line.
    * For example, to generate 10 identifiers:
    *
    *   java com.tangosol.util.UUID 10
    */
    public static void main(String[] asArgs)
        {
        try
            {
            if (asArgs.length <= 1)
                {
                int cIds = asArgs.length == 0 ? 1 :
                            Math.max(1, Integer.parseInt(asArgs[0]));

                for (int i = 0; i < cIds; ++i)
                    {
                    UUID    uid  = new UUID();
                    String sUUID = uid.toString();

                    azzert(uid.isGenerated());
                    azzert(uid.equals(new UUID(sUUID)));

                    System.out.println(uid);
                    }
                }
            else
                {
                InetAddress addr  = InetAddress.getByName(asArgs[0]);
                int         nPort = Integer.parseInt(asArgs[1]);
                UUID        uid   = new UUID(
                    new java.util.Date().getTime(), addr, nPort, s_nLastCount);
                System.out.println(uid);
                }
            }
        catch (Exception e)
            {
            try
                {
                UUID uid = new UUID(asArgs[0]);

                out("Address  =" + InetAddressHelper.toString(uid.getAddress()));
                out("Timestamp=" + new java.util.Date(uid.getTimestamp()));
                out("Port     =" + uid.getPort());
                out("MachineId=" + ((uid.getCount() & 0xFFFF0000) >>> 16));
                }
            catch (Exception ex)
                {
                out("Usage:");
                out("   java com.tangosol.util.UUID <number> | <UUID> | <IP> <port> <count>");
                }
            }
        }


    // ----- constructors ---------------------------------------------------

    /**
    * Generate a UUID.
    */
    public UUID()
        {
        }

    /**
    * Build a UUID from its constituent members (advanced constructor).
    * <p>
    * It is guaranteed that a generated UUID will never equal a built UUID.
    *
    * @param lDatetime  the creation date/time millis portion of the UUID
    * @param addr       the InetAddress portion of the UUID
    * @param nPort      the port number portion of the UUID; a port number
    *                   is 16 bits, but up to 28 bits of data from this value
    *                   will be maintained by the UUID
    * @param nCount     the counter portion of the UUID
    */
    public UUID(long lDatetime, InetAddress addr, int nPort, int nCount)
        {
        this(lDatetime, addr == null ? null : addr.getAddress(), nPort, nCount);
        }

    /**
    * Build a UUID from its constituent members (advanced constructor).
    * <p>
    * It is guaranteed that a generated UUID will never equal a built UUID.
    *
    * @param lDatetime  the creation date/time millis portion of the UUID
    * @param abIP       the InetAddress portion of the UUID
    * @param nPort      the port number portion of the UUID; a port number
    *                   is 16 bits, but up to 28 bits of data from this value
    *                   will be maintained by the UUID
    * @param nCount     the counter portion of the UUID
    */
    public UUID(long lDatetime, byte[] abIP, int nPort, int nCount)
        {
        m_lDatetime = lDatetime;
        m_nCount    = nCount;

        boolean fAddr = false;
        boolean fIPv6 = false;
        if (abIP != null)
            {
            switch (abIP.length)
                {
                default:
                    throw new IllegalArgumentException("unsupported IP address length: " + abIP.length);

                case 16:
                    m_nAddr4 = (abIP[12] & 0xFF) << 24
                             | (abIP[13] & 0xFF) << 16
                             | (abIP[14] & 0xFF) <<  8
                             | (abIP[15] & 0xFF);
                    m_nAddr3 = (abIP[ 8] & 0xFF) << 24
                             | (abIP[ 9] & 0xFF) << 16
                             | (abIP[10] & 0xFF) <<  8
                             | (abIP[11] & 0xFF);
                    m_nAddr2 = (abIP[ 4] & 0xFF) << 24
                             | (abIP[ 5] & 0xFF) << 16
                             | (abIP[ 6] & 0xFF) <<  8
                             | (abIP[ 7] & 0xFF);
                    fIPv6 = true;
                    // fall through
                case 4:
                    m_nAddr1 = (abIP[ 0] & 0xFF) << 24
                             | (abIP[ 1] & 0xFF) << 16
                             | (abIP[ 2] & 0xFF) <<  8
                             | (abIP[ 3] & 0xFF);
                    fAddr = true;
                    break;

                case 0:
                    break;
                }
            }

        m_nPort = (fAddr ? MASK_REALADDR : 0)
                | (fIPv6 ? MASK_IPV6ADDR : 0)
                | (nPort & ~MASK_ALLFLAGS);

        initHashcode();
        }

    /**
    * Construct a UUID from a String.
    *
    * @param s  a String as would be returned from UUID.toString()
    */
    public UUID(String s)
        {
        this(parseHex(s));
        }

    /**
    * Construct a UUID from a byte array.
    *
    * @param ab  a byte array as would be returned from UUID.toByteArray()
    */
    public UUID(byte[] ab)
        {
        azzert(ab != null && ab.length == 32);

        m_lDatetime = (long) (ab[ 0] & 0xFF) << 56
                    | (long) (ab[ 1] & 0xFF) << 48
                    | (long) (ab[ 2] & 0xFF) << 40
                    | (long) (ab[ 3] & 0xFF) << 32
                    | (long) (ab[ 4] & 0xFF) << 24
                    | (long) (ab[ 5] & 0xFF) << 16
                    | (long) (ab[ 6] & 0xFF) <<  8
                    | (long) (ab[ 7] & 0xFF);
        m_nAddr1    = (int)  (ab[ 8] & 0xFF) << 24
                    | (int)  (ab[ 9] & 0xFF) << 16
                    | (int)  (ab[10] & 0xFF) <<  8
                    | (int)  (ab[11] & 0xFF);
        m_nAddr2    = (int)  (ab[12] & 0xFF) << 24
                    | (int)  (ab[13] & 0xFF) << 16
                    | (int)  (ab[14] & 0xFF) <<  8
                    | (int)  (ab[15] & 0xFF);
        m_nAddr3    = (int)  (ab[16] & 0xFF) << 24
                    | (int)  (ab[17] & 0xFF) << 16
                    | (int)  (ab[18] & 0xFF) <<  8
                    | (int)  (ab[19] & 0xFF);
        m_nAddr4    = (int)  (ab[20] & 0xFF) << 24
                    | (int)  (ab[21] & 0xFF) << 16
                    | (int)  (ab[22] & 0xFF) <<  8
                    | (int)  (ab[23] & 0xFF);
        m_nPort     = (int)  (ab[24] & 0xFF) << 24
                    | (int)  (ab[25] & 0xFF) << 16
                    | (int)  (ab[26] & 0xFF) <<  8
                    | (int)  (ab[27] & 0xFF);
        m_nCount    = (int)  (ab[28] & 0xFF) << 24
                    | (int)  (ab[29] & 0xFF) << 16
                    | (int)  (ab[30] & 0xFF) <<  8
                    | (int)  (ab[31] & 0xFF);

        initHashcode();
        }

    /**
    * Construct a UUID from a stream.
    * <p>
    * This is a helper constructor that is just a shorthand for a sequence:
    * <pre>
    *   UUID uid = new UUID();
    *   uid.readExternal(stream);
    * </pre>
    *
    * @param stream  an object implementing java.io.DataInput that contains
    *                the internal information that a previous UID wrote
    *
    * @exception IOException if an error occurs reading from the stream
    */
    public UUID(DataInput stream)
            throws IOException
        {
        readExternal(stream);
        }


    // ----- accessors ------------------------------------------------------

    /**
    * This is true if the UUID was generated, and false if it was built. A
    * generated UUID is universally unique. Note that the port number is
    * random if the UUID is generated.
    *
    * @return true if the UUID was generated
    */
    public boolean isGenerated()
        {
        ensureConstructed();
        return (m_nPort & MASK_GENERATED) != 0;
        }

    /**
    * Determine the date/time value that the UUID instance was generated.
    *
    * @return date/time value in millis that the UUID instance was generated
    */
    public long getTimestamp()
        {
        ensureConstructed();
        return m_lDatetime;
        }

    /**
    * This is true if the IP address is a real IP address. This is only false
    * if two conditions are met: The UUID is generated, and it could not
    * get an IP address (or one that is not a loopback/localhost address)
    * from the VM.
    *
    * @return true if the UUID has IP address information
    */
    public boolean isAddressIncluded()
        {
        ensureConstructed();
        return (m_nPort & MASK_REALADDR) != 0;
        }

    /**
    * Determine the internet address of the host that generated the UUID
    * instance.
    *
    * @return an array of bytes containing the IP address information; the
    *         array can be zero bytes (no address,) four bytes (IPv4) or
    *         16 bytes (IPv6)
    */
    public byte[] getAddress()
        {
        ensureConstructed();

        byte[] ab;

        switch (m_nPort & (MASK_REALADDR | MASK_IPV6ADDR))
            {
            case MASK_REALADDR | MASK_IPV6ADDR:
                {
                int nAddr1 = m_nAddr1;
                int nAddr2 = m_nAddr2;
                int nAddr3 = m_nAddr3;
                int nAddr4 = m_nAddr4;

                ab = new byte[16];
                ab[ 0] = (byte) (nAddr1    >>> 24);
                ab[ 1] = (byte) (nAddr1    >>> 16);
                ab[ 2] = (byte) (nAddr1    >>>  8);
                ab[ 3] = (byte) (nAddr1          );
                ab[ 4] = (byte) (nAddr2    >>> 24);
                ab[ 5] = (byte) (nAddr2    >>> 16);
                ab[ 6] = (byte) (nAddr2    >>>  8);
                ab[ 7] = (byte) (nAddr2          );
                ab[ 8] = (byte) (nAddr3    >>> 24);
                ab[ 9] = (byte) (nAddr3    >>> 16);
                ab[10] = (byte) (nAddr3    >>>  8);
                ab[11] = (byte) (nAddr3          );
                ab[12] = (byte) (nAddr4    >>> 24);
                ab[13] = (byte) (nAddr4    >>> 16);
                ab[14] = (byte) (nAddr4    >>>  8);
                ab[15] = (byte) (nAddr4          );
                }
                break;

            case MASK_REALADDR:
                {
                int nAddr1 = m_nAddr1;

                ab    = new byte[4];
                ab[0] = (byte) (nAddr1    >>> 24);
                ab[1] = (byte) (nAddr1    >>> 16);
                ab[2] = (byte) (nAddr1    >>>  8);
                ab[3] = (byte) (nAddr1          );
                }
                break;

            case 0:
                ab = NO_BYTES;
                break;

            default:
                throw new IllegalStateException();
            }

        return ab;
        }

    /**
    * Determine the port portion of the UUID. Note that the port is a 28-bit
    * value; the first nibble is always 0x0.
    *
    * @return the port portion of the UID
    */
    public int getPort()
        {
        ensureConstructed();
        return m_nPort & ~MASK_ALLFLAGS;
        }

    /**
    * Determine the "counter" portion of the UUID that ensures that two UUIDs
    * generated at the same exact time by the same process are unique.
    *
    * @return a number that helps to make the UUID unique
    */
    public int getCount()
        {
        ensureConstructed();
        return m_nCount;
        }

    /**
    * Convert the UUID to a byte array of 32 bytes.
    *
    * @return the UUID data as a byte array of 32 bytes
    */
    public byte[] toByteArray()
        {
        ensureConstructed();

        byte[] ab        = new byte[32];

        long   lDatetime = m_lDatetime;
        int    nAddr1    = m_nAddr1;
        int    nAddr2    = m_nAddr2;
        int    nAddr3    = m_nAddr3;
        int    nAddr4    = m_nAddr4;
        int    nPort     = m_nPort;
        int    nCount    = m_nCount;

        ab[ 0] = (byte) (lDatetime >>> 56);
        ab[ 1] = (byte) (lDatetime >>> 48);
        ab[ 2] = (byte) (lDatetime >>> 40);
        ab[ 3] = (byte) (lDatetime >>> 32);
        ab[ 4] = (byte) (lDatetime >>> 24);
        ab[ 5] = (byte) (lDatetime >>> 16);
        ab[ 6] = (byte) (lDatetime >>>  8);
        ab[ 7] = (byte) (lDatetime       );
        ab[ 8] = (byte) (nAddr1    >>> 24);
        ab[ 9] = (byte) (nAddr1    >>> 16);
        ab[10] = (byte) (nAddr1    >>>  8);
        ab[11] = (byte) (nAddr1          );
        ab[12] = (byte) (nAddr2    >>> 24);
        ab[13] = (byte) (nAddr2    >>> 16);
        ab[14] = (byte) (nAddr2    >>>  8);
        ab[15] = (byte) (nAddr2          );
        ab[16] = (byte) (nAddr3    >>> 24);
        ab[17] = (byte) (nAddr3    >>> 16);
        ab[18] = (byte) (nAddr3    >>>  8);
        ab[19] = (byte) (nAddr3          );
        ab[20] = (byte) (nAddr4    >>> 24);
        ab[21] = (byte) (nAddr4    >>> 16);
        ab[22] = (byte) (nAddr4    >>>  8);
        ab[23] = (byte) (nAddr4          );
        ab[24] = (byte) (nPort     >>> 24);
        ab[25] = (byte) (nPort     >>> 16);
        ab[26] = (byte) (nPort     >>>  8);
        ab[27] = (byte) (nPort           );
        ab[28] = (byte) (nCount    >>> 24);
        ab[29] = (byte) (nCount    >>> 16);
        ab[30] = (byte) (nCount    >>>  8);
        ab[31] = (byte) (nCount          );

        return ab;
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Convert the UUID to a printable String.
    *
    * @return the UUID data as a 0x-prefixed hex string.
    */
    public String toString()
        {
        return toHexEscape(toByteArray());
        }

    /**
    * Determine if two UUIDs are equal.
    *
    * @param o  the other UUID
    *
    * @return true if the passed object is equal to this
    */
    public boolean equals(Object o)
        {
        ensureConstructed();

        if (o instanceof UUID)
            {
            UUID that = (UUID) o;
            return this == that                             // same object?
                || this.m_nHash     == that.m_nHash         // quick check
                && this.m_lDatetime == that.m_lDatetime
                && this.m_nAddr1    == that.m_nAddr1
                && this.m_nAddr2    == that.m_nAddr2
                && this.m_nAddr3    == that.m_nAddr3
                && this.m_nAddr4    == that.m_nAddr4
                && this.m_nPort     == that.m_nPort
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
        ensureConstructed();

        int  nResult = 0;
        UUID that    = (UUID) o;

        if (this != that)
            {
            if (this.m_lDatetime != that.m_lDatetime)
                {
                nResult = this.m_lDatetime < that.m_lDatetime ? -1 : 1;
                }
            else if (this.m_nAddr1 != that.m_nAddr1)
                {
                nResult = this.m_nAddr1 < that.m_nAddr1 ? -1 : 1;
                }
            else if (this.m_nAddr2 != that.m_nAddr2)
                {
                nResult = this.m_nAddr2 < that.m_nAddr2 ? -1 : 1;
                }
            else if (this.m_nAddr3 != that.m_nAddr3)
                {
                nResult = this.m_nAddr3 < that.m_nAddr3 ? -1 : 1;
                }
            else if (this.m_nAddr4 != that.m_nAddr4)
                {
                nResult = this.m_nAddr4 < that.m_nAddr4 ? -1 : 1;
                }
            else if (this.m_nPort != that.m_nPort)
                {
                nResult = this.m_nPort < that.m_nPort ? -1 : 1;
                }
            else if (this.m_nCount != that.m_nCount)
                {
                nResult = this.m_nCount < that.m_nCount ? -1 : 1;
                }
            }

        return nResult;
        }

    /**
    * Determine a hash code for the UUID object.
    *
    * @return a hash code reflecting the UUID's data
    */
    public int hashCode()
        {
        ensureConstructed();
        return m_nHash;
        }


    // ----- Externalizable interface ---------------------------------------

    /**
    * Read this object from an ObjectInputStream.
    *
    * @param in  the ObjectInputStream to read this object from
    *
    * @throws IOException  if an exception occurs reading this object
    */
    public void readExternal(ObjectInput in)
            throws IOException
        {
        readExternal((DataInput) in);
        }

    /**
    * Write this object to an ObjectOutputStream.
    *
    * @param out  the ObjectOutputStream to write this object to
    *
    * @throws IOException  thrown if an exception occurs writing this object
    */
    public void writeExternal(ObjectOutput out)
            throws IOException
        {
        writeExternal((DataOutput) out);
        }


    // ----- ExternalizableLite interface -----------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(DataInput in)
            throws IOException
        {
        // note: this public method must not call ensureConstructed()

        if (m_nHash != 0)
            {
            // an attempt was made to change a UUID -- which is an immutable
            // object -- by deserializing into it!
            throw new NotActiveException();
            }

        m_lDatetime = in.readLong();
        m_nAddr1    = in.readInt();
        m_nAddr2    = in.readInt();
        m_nAddr3    = in.readInt();
        m_nAddr4    = in.readInt();
        m_nPort     = in.readInt();
        m_nCount    = in.readInt();

        initHashcode();
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(DataOutput out)
            throws IOException
        {
        ensureConstructed();

        out.writeLong(m_lDatetime);
        out.writeInt(m_nAddr1);
        out.writeInt(m_nAddr2);
        out.writeInt(m_nAddr3);
        out.writeInt(m_nAddr4);
        out.writeInt(m_nPort);
        out.writeInt(m_nCount);
        }


    // ----- PortableObject interface ---------------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(PofReader in)
            throws IOException
        {
        // note: this public method must not call ensureConstructed()

        if (m_nHash != 0)
            {
            // an attempt was made to change a UUID -- which is an immutable
            // object -- by deserializing into it!
            throw new NotActiveException();
            }

        m_lDatetime = in.readLong(0);
        m_nAddr1    = in.readInt(1);
        m_nAddr2    = in.readInt(2);
        m_nAddr3    = in.readInt(3);
        m_nAddr4    = in.readInt(4);
        m_nPort     = in.readInt(5);
        m_nCount    = in.readInt(6);

        initHashcode();
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(PofWriter out)
            throws IOException
        {
        ensureConstructed();

        out.writeLong(0, m_lDatetime);
        out.writeInt(1, m_nAddr1);
        out.writeInt(2, m_nAddr2);
        out.writeInt(3, m_nAddr3);
        out.writeInt(4, m_nAddr4);
        out.writeInt(5, m_nPort);
        out.writeInt(6, m_nCount);
        }


    // ----- internal -------------------------------------------------------

    /**
    * If this UUID is being used as a generated UUID but its fields have not
    * yet been initialized, this method ensures that the initialization
    * occurs. All public methods, except for deserialization methods, must
    * call this method to ensure that the UUID is properly constructed.
    */
    private void ensureConstructed()
        {
        if (m_nHash == 0)
            {
            synchronized (this)
                {
                if (m_nHash == 0)
                    {
                    // this UUID will be a "generated" UUID
                    synchronized (LOCK)
                        {
                        m_lDatetime = getSafeTimeMillis();
                        m_nCount    = ++s_nLastCount;
                        }

                    boolean fRealAddress = s_fRealAddress;
                    boolean fIPv6        = s_fIPv6;
                    Random  rnd          = s_rnd;

                    // the "address" is either a 128-bit IPv6 address, a 32-bit IPv4
                    // address with the balance random, or a 128-bit random
                    if (fRealAddress)
                        {
                        if (fIPv6)
                            {
                            // 128-bit IPv6 address
                            m_nAddr1 = s_nAddr1;
                            m_nAddr2 = s_nAddr2;
                            m_nAddr3 = s_nAddr3;
                            m_nAddr4 = s_nAddr4;
                            }
                        else
                            {
                            // 32-bit IPv4 address; the rest is random
                            m_nAddr1 = s_nAddr1;
                            m_nAddr2 = rnd.nextInt();
                            m_nAddr3 = rnd.nextInt();
                            m_nAddr4 = rnd.nextInt();
                            }
                        }
                    else
                        {
                        // 128-bit random value instead of an address
                        m_nAddr1 = rnd.nextInt();
                        m_nAddr2 = rnd.nextInt();
                        m_nAddr3 = rnd.nextInt();
                        m_nAddr4 = rnd.nextInt();
                        }

                    // the "port" is mostly random data, except that the flags are
                    // encoded into it
                    m_nPort =                 MASK_GENERATED
                            | (fRealAddress ? MASK_REALADDR : 0)
                            | (fIPv6        ? MASK_IPV6ADDR : 0)
                            | (rnd.nextInt() & ~MASK_ALLFLAGS);

                    initHashcode();
                    }
                }
            }
        }

    /**
    * Finish construction or deserialization. The UUID's internally cached
    * hashcode value is zero until construction is completed, or until
    * deserialization is completed, and never zero otherwise. Every
    * constructor, except for the deserialization constructor, must call this
    * method.
    */
    private void initHashcode()
        {
        int nHash = (int) (m_lDatetime >>> 32)  // high order 32-bits of the date/time
                  ^ (int) m_lDatetime           // low order 32-bits of the date/time
                  ^ m_nAddr1
                  ^ m_nAddr2
                  ^ m_nAddr3
                  ^ m_nAddr4
                  ^ m_nPort
                  ^ m_nCount;

        if (nHash == 0)
            {
            nHash = 2147483647; // Integer.MAX_VALUE is actually a prime ;-)
            }

        m_nHash = nHash;
        }


    // ----- constants ------------------------------------------------------

    /**
    * A bit mask that represents the portion of the "port" value reserved for
    * bit flags.
    */
    private static final int MASK_ALLFLAGS  = 0xF0000000;
    /**
    * The bit mask for the "is generated UUID" flag.
    */
    private static final int MASK_GENERATED = 1 << 31;
    /**
    * The bit mask for the "is a real IP address" flag.
    */
    private static final int MASK_REALADDR  = 1 << 30;
    /**
    * The bit mask for the "is an IPv6 address" flag.
    */
    private static final int MASK_IPV6ADDR  = 1 << 29;
    /**
    * The one remaining bit for future use.
    */
    private static final int MASK_UNUSED    = 1 << 28;

    /**
    * An empty byte array (by definition immutable).
    */
    private static final byte[] NO_BYTES = new byte[0];


    // ----- shared members -------------------------------------------------

    /**
    * Random number generator.
    */
    private static final Random s_rnd = getRandom();

    /**
    * The spinning counter (which starts spinning at a random point).
    */
    private static int s_nLastCount = s_rnd.nextInt();

    /**
    * This is true if the host's IP address is a real IP address. This is
    * false if this class could not get an IP address (or one that is not a
    * loopback/localhost address) from the VM.
    */
    private static final boolean s_fRealAddress;

    /**
    * This is true if the host's address style is known and is IPv6.
    */
    private static final boolean s_fIPv6;

    /**
    * The first four bytes of the IP address of the host.
    */
    private static final int s_nAddr1;
    /**
    * The second four bytes of the IP address of the host.
    */
    private static final int s_nAddr2;
    /**
    * The third four bytes of the IP address of the host.
    */
    private static final int s_nAddr3;
    /**
    * The fourth four bytes of the IP address of the host.
    */
    private static final int s_nAddr4;

    /*
    * Initialize host address information.
    */
    static
        {
        boolean fRealAddress = false;
        boolean fIPv6        = false;
        int     nAddr1       = 0x00000000;
        int     nAddr2       = 0x00000000;
        int     nAddr3       = 0x00000000;
        int     nAddr4       = 0x00000000;

        try
            {
            // the problem here is that the InetAddress from JDK 1.2 has
            // almost no help for us to get anything but a single address
            // for the current machine
            InetAddress addr = InetAddress.getLocalHost();

            // if it's 1.4 or later, we have all sorts of options;
            // to avoid issues with older VMs use a separate class
            try
                {
                addr = InetAddressHelper.getLocalHost();
                }
            catch (Throwable e) {}

            byte[] abIP = NO_BYTES;
            if (addr != null)
                {
                abIP = addr.getAddress();
                }

            switch (abIP.length)
                {
                case 4:
                    {
                    int nIP = (abIP[0] & 0xFF) << 24
                            | (abIP[1] & 0xFF) << 16
                            | (abIP[2] & 0xFF) <<  8
                            | (abIP[3] & 0xFF);

                    if (nIP != 0x00000000 && nIP != 0x7F000001)
                        {
                        fRealAddress = true;
                        nAddr1       = nIP;
                        }
                    }
                    break;

                case 16:
                    {
                    int nIP1 = (abIP[ 0] & 0xFF) << 24
                             | (abIP[ 1] & 0xFF) << 16
                             | (abIP[ 2] & 0xFF) <<  8
                             | (abIP[ 3] & 0xFF);
                    int nIP2 = (abIP[ 4] & 0xFF) << 24
                             | (abIP[ 5] & 0xFF) << 16
                             | (abIP[ 6] & 0xFF) <<  8
                             | (abIP[ 7] & 0xFF);
                    int nIP3 = (abIP[ 8] & 0xFF) << 24
                             | (abIP[ 9] & 0xFF) << 16
                             | (abIP[10] & 0xFF) <<  8
                             | (abIP[11] & 0xFF);
                    int nIP4 = (abIP[12] & 0xFF) << 24
                             | (abIP[13] & 0xFF) << 16
                             | (abIP[14] & 0xFF) <<  8
                             | (abIP[15] & 0xFF);

                    if (!(    nIP1 == 0x00000000
                          &&  nIP2 == 0x00000000
                          &&  nIP3 == 0x00000000
                          && (nIP4 == 0x00000000 || nIP4 == 0x00000001)) // any-local or loopback
                        && !((abIP[0] & 0xFF) == 0xFE && (abIP[1] & 0xC0) == 0x80)) // link-local
                        {
                        fRealAddress = true;
                        fIPv6        = true;
                        nAddr1       = nIP1;
                        nAddr2       = nIP2;
                        nAddr3       = nIP3;
                        nAddr4       = nIP4;
                        }
                    }
                    break;
                }
            }
        catch (Exception e) {}

        s_fRealAddress = fRealAddress;
        s_fIPv6        = fIPv6;
        s_nAddr1       = nAddr1;
        s_nAddr2       = nAddr2;
        s_nAddr3       = nAddr3;
        s_nAddr4       = nAddr4;
        }

    /**
    * A mutex for accessing/updating s_nLastCount.
    * <p>
    * The java.lang.Object class object is a JVM singleton.
    */
    private static final Object LOCK = Object.class;


    // ----- data members ---------------------------------------------------

    /**
    * System date/time value that the UUID instance was generated.
    */
    private long m_lDatetime;

    /**
    * Internet address of host that generated the UUID instance. For IPv4,
    * this contains the entire IP address. For IPv6, this contains only the
    * first four bytes of the address. If the UUID is auto-generated and it
    * could not obtain a real address, then this is a random number.
    */
    private int m_nAddr1;
    /**
    * The second four bytes of the IP address. For IPv6, this is the second
    * four bytes of the IP address. If the UUID is auto-generated and it
    * could not obtain a real address, then this is a random number.
    * Otherwise if the UUID is built (not generated) and the address is
    * IPv4, then this is zero.
    */
    private int m_nAddr2;
    /**
    * The third four bytes of the IP address. For IPv6, this is the third
    * four bytes of the IP address. If the UUID is auto-generated and it
    * could not obtain a real address, then this is a random number.
    * Otherwise if the UUID is built (not generated) and the address is
    * IPv4, then this is zero.
    */
    private int m_nAddr3;
    /**
    * The fourth four bytes of the IP address. For IPv6, this is the fourth
    * four bytes of the IP address. If the UUID is auto-generated and it
    * could not obtain a real address, then this is a random number.
    * Otherwise if the UUID is built (not generated) and the address is
    * IPv4, then this is zero.
    */
    private int m_nAddr4;

    /**
    * The least significant two bytes of this value are the port number if
    * the UUID is built (not generated). Otherwise this is a random number,
    * with the exception of the most significant nibble. The most significant
    * nibble contains the flags of the UUID.
    */
    private int m_nPort;

    /**
    * A rolling counter.
    */
    private int m_nCount;

    /**
    * Cache the hash. Only zero pending deserialization or generation.
    */
    private transient volatile int m_nHash;
    }
