/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.io.json.internal;


/**
 * Utility class to convert String IP addresses into an array of bytes.
 *
 * @since 20.06
 */
final class IPAddressUtil
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Utility class, construction not allowed.
     */
    private IPAddressUtil()
        {
        }

    // ----- public methods -------------------------------------------------

    /**
     * Converts IPv4 address in its textual presentation form
     * into its numeric binary form.
     *
     * @param src  a String representing an IPv4 address in standard format
     *
     * @return a byte array representing the IPv4 numeric address
     */
    @SuppressWarnings("fallthrough")
    public static byte[] textToNumericFormatV4(String src)
        {
        byte[] res = new byte[INADDR4SZ];

        long    tmpValue = 0;
        int     currByte = 0;
        boolean newOctet = true;

        int len = src.length();
        if (len == 0 || len > 15)
            {
            return null;
            }
        /*
         * When only one part is given, the value is stored directly in
         * the network address without any byte rearrangement.
         *
         * When a two part address is supplied, the last part is
         * interpreted as a 24-bit quantity and placed in the right
         * most three bytes of the network address. This makes the
         * two part address format convenient for specifying Class A
         * network addresses as net.host.
         *
         * When a three part address is specified, the last part is
         * interpreted as a 16-bit quantity and placed in the right
         * most two bytes of the network address. This makes the
         * three part address format convenient for specifying
         * Class B net- work addresses as 128.net.host.
         *
         * When four parts are specified, each is interpreted as a
         * byte of data and assigned, from left to right, to the
         * four bytes of an IPv4 address.
         *
         * We determine and parse the leading parts, if any, as single
         * byte values in one pass directly into the resulting byte[],
         * then the remainder is treated as a 8-to-32-bit entity and
         * translated into the remaining bytes in the array.
         */
        for (int i = 0; i < len; i++)
            {
            char c = src.charAt(i);
            if (c == '.')
                {
                if (newOctet || tmpValue < 0 || tmpValue > 0xff || currByte == 3)
                    {
                    return null;
                    }
                res[currByte++] = (byte) (tmpValue & 0xff);
                tmpValue        = 0;
                newOctet        = true;
                }
            else
                {
                int digit = Character.digit(c, 10);
                if (digit < 0)
                    {
                    return null;
                    }
                tmpValue *= 10;
                tmpValue += digit;
                newOctet  = false;
                }
            }
        if (newOctet || tmpValue < 0 || tmpValue >= (1L << ((4 - currByte) * 8)))
            {
            return null;
            }
        switch (currByte)
            {
            case 0:
                res[0] = (byte) ((tmpValue >> 24) & 0xff);
            case 1:
                res[1] = (byte) ((tmpValue >> 16) & 0xff);
            case 2:
                res[2] = (byte) ((tmpValue >> 8) & 0xff);
            case 3:
                res[3] = (byte) ((tmpValue) & 0xff);
            default:
            }
        return res;
        }

    /**
     * Convert IPv6 presentation level address to network order binary form.
     * credit:
     * Converted from C code from Solaris 8 (inet_pton)
     * <p>
     * Any component of the string following a per-cent % is ignored.
     *
     * @param src  a String representing an IPv6 address in textual format
     *
     * @return a byte array representing the IPv6 numeric address
     */
    public static byte[] textToNumericFormatV6(String src)
        {
        // Shortest valid string is "::", hence at least 2 chars
        if (src.length() < 2)
            {
            return null;
            }

        int     colonp;
        char    ch;
        boolean sawXDigit;
        int     val;

        char[]  srcb = src.toCharArray();
        byte[]  dst  = new byte[INADDR16SZ];

        int srcbLength = srcb.length;
        int pc         = src.indexOf('%');
        if (pc == srcbLength - 1)
            {
            return null;
            }

        if (pc != -1)
            {
            srcbLength = pc;
            }

        colonp = -1;

        int i = 0;
        int j = 0;
        /* Leading :: requires some special handling. */
        if (srcb[i] == ':')
            {
            if (srcb[++i] != ':')
                {
                return null;
                }
            }
        int curtok = i;

        sawXDigit = false;
        val       = 0;
        while (i < srcbLength)
            {
            ch = srcb[i++];
            int chval = Character.digit(ch, 16);
            if (chval != -1)
                {
                val <<= 4;
                val |= chval;
                if (val > 0xffff)
                    {
                    return null;
                    }
                sawXDigit = true;
                continue;
                }
            if (ch == ':')
                {
                curtok = i;
                if (!sawXDigit)
                    {
                    if (colonp != -1)
                        {
                        return null;
                        }
                    colonp = j;
                    continue;
                    }
                else if (i == srcbLength)
                    {
                    return null;
                    }
                if (j + INT16SZ > INADDR16SZ)
                    {
                    return null;
                    }
                dst[j++]  = (byte) ((val >> 8) & 0xff);
                dst[j++]  = (byte) (val & 0xff);
                sawXDigit = false;
                val       = 0;
                continue;
                }
            if (ch == '.' && ((j + INADDR4SZ) <= INADDR16SZ))
                {
                String ia4      = src.substring(curtok, srcbLength);
                /* check this IPv4 address has 3 dots, ie. A.B.C.D */
                int    dotCount = 0;
                int    index    = 0;
                while ((index = ia4.indexOf('.', index)) != -1)
                    {
                    dotCount++;
                    index++;
                    }
                if (dotCount != 3)
                    {
                    return null;
                    }
                byte[] v4addr = textToNumericFormatV4(ia4);
                if (v4addr == null)
                    {
                    return null;
                    }
                for (int k = 0; k < INADDR4SZ; k++)
                    {
                    dst[j++] = v4addr[k];
                    }
                sawXDigit = false;
                break;  /* '\0' was seen by inet_pton4(). */
                }
            return null;
            }
        if (sawXDigit)
            {
            if (j + INT16SZ > INADDR16SZ)
                {
                return null;
                }
            dst[j++] = (byte) ((val >> 8) & 0xff);
            dst[j++] = (byte) (val & 0xff);
            }

        if (colonp != -1)
            {
            int n = j - colonp;

            if (j == INADDR16SZ)
                {
                return null;
                }
            for (i = 1; i <= n; i++)
                {
                dst[INADDR16SZ - i] = dst[colonp + n - i];
                dst[colonp + n - i] = 0;
                }
            j = INADDR16SZ;
            }
        if (j != INADDR16SZ)
            {
            return null;
            }
        byte[] newdst = convertFromIPv4MappedAddress(dst);
        if (newdst != null)
            {
            return newdst;
            }
        else
            {
            return dst;
            }
        }

    /**
     * Return a boolean indicating whether src is an IPv4 literal address.
     *
     * @param src  a String representing an IPv4 address in textual format.
     *
     * @return a boolean indicating whether src is an IPv4 literal address
     */
    public static boolean isIPv4LiteralAddress(String src)
        {
        return textToNumericFormatV4(src) != null;
        }

    /**
     * Return a boolean indicating whether src is an IPv6 literal address.
     *
     * @param src  a String representing an IPv6 address in textual format
     *
     * @return a boolean indicating whether src is an IPv6 literal address
     */
    public static boolean isIPv6LiteralAddress(String src)
        {
        return textToNumericFormatV6(src) != null;
        }

    /**
     * Convert IPv4-Mapped address to IPv4 address. Both input and
     * returned value are in network order binary form.
     *
     * @param addr  a byte array representing the IPv4 numeric address
     *
     * @return a String representing an IPv4-Mapped address in textual format
     */
    public static byte[] convertFromIPv4MappedAddress(byte[] addr)
        {
        if (isIPv4MappedAddress(addr))
            {
            byte[] newAddr = new byte[INADDR4SZ];
            System.arraycopy(addr, 12, newAddr, 0, INADDR4SZ);
            return newAddr;
            }
        return null;
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Utility routine to check if the InetAddress is an
     * IPv4 mapped IPv6 address.
     *
     * @return a {@code boolean} indicating if the InetAddress is
     * an IPv4 mapped IPv6 address; or false if address is IPv4 address.
     */
    private static boolean isIPv4MappedAddress(byte[] addr)
        {
        if (addr.length < INADDR16SZ)
            {
            return false;
            }
        return (addr[0] == 0x00) && (addr[1] == 0x00)
               && (addr[2] == 0x00) && (addr[3] == 0x00)
               && (addr[4] == 0x00) && (addr[5] == 0x00)
               && (addr[6] == 0x00) && (addr[7] == 0x00)
               && (addr[8] == 0x00) && (addr[9] == 0x00)
               && (addr[10] == (byte) 0xff)
               && (addr[11] == (byte) 0xff);
        }

    // ----- constants ------------------------------------------------------

    /**
     * IPv4 address size in bytes.
     */
    private static final int INADDR4SZ = 4;

    /**
     * IPv6 address size in bytes.
     */
    private static final int INADDR16SZ = 16;

    /**
     * 16-bit integer size in bytes.
     */
    private static final int INT16SZ = 2;
    }
