/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net;

import com.oracle.coherence.common.base.Predicate;

import com.oracle.coherence.common.internal.net.MultiplexedSocketProvider;

import com.oracle.coherence.common.net.InetAddresses;
import com.oracle.coherence.common.net.InetSocketAddress32;

import com.tangosol.util.Base;
import com.tangosol.util.Filter;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.stream.Collectors;

/**
* Helper class that encapsulates common InetAddress functionality.
*/
public abstract class InetAddressHelper
        extends InetAddresses
    {
    /**
    * Obtain the "best" local host address which matches the supplied filter.
    *
    * @param filter the filter to match
    *
    * @return the InetAddress
    *
    * @throws UnknownHostException if no match can be found
    */
    public static InetAddress getLocalAddress(Filter filter)
            throws UnknownHostException
        {
        return getLocalAddress((Predicate) filter::evaluate);
        }

    /**
     * Return an array of strings representing addresses in the specified collection.
     *
     * @param colAddresses  the collection of addresses
     *
     * @return an array of strings representing addresses in the specified collection
     */
    public static String[] getAddressDescriptions(Collection colAddresses)
        {
        if (colAddresses == null)
            {
            return new String[0];
            }

        String[]      asAddr = new String[colAddresses.size()];
        StringBuilder sb     = new StringBuilder();
        int           i      = 0;

        for (Iterator iter = colAddresses.iterator(); iter.hasNext(); )
            {
            InetSocketAddress addr = (InetSocketAddress) iter.next();
            sb.append(toString(addr.getAddress())).append(':')
              .append(addr.getPort());
            asAddr[i++] = sb.toString();
            sb.setLength(0);
            }

        return asAddr;
        }

    /**
     * Return the set of addresses from the specified destination addresses which appear to be
     * routable from addrLocal.  May return null if no routable addresses are found.
     * <p>
     * If the addrLocal is null, or fLocalSrc is true, then all local addresses are used
     * to compare to the set of destination addresses.
     *
     * @param addrLocal   the local address to compare to the destination addresses; may be null
     * @param fLocalSrc   whether the source should be considered local to this machine
     * @param colDest     the full collection of candidate destination addresses; may
     *                    contain non-routable local only addresses
     * @param fLocalDest  whether the destination is local to this machine
     *
     * @return the set of addresses from the specified destination addresses which appear to be
     *         routable from the source addresses, or null if no addresses are found
     *
     * @since 12.2.1
     */
    public static Collection<InetAddress> getRoutableAddresses(InetAddress addrLocal,
                                                               boolean fLocalSrc,
                                                               Collection<InetAddress> colDest,
                                                               boolean fLocalDest)
        {
        // fLocalDest:
        //
        // fLocalDest cannot always be derived from colDest. If colDest consists entirely of
        // local-only addresses, then it may not be possible to determine if those local-only
        // addresses are for this machine or another machine. Hence the caller may need to
        // use other means (looking at Member details for example) to determine if the
        // destination is another machine.
        // For example, suppose colDest={127.0.0.1}. Is that the loopback address of this
        // machine?  Or another machine?

        Collection<InetAddress> colLocal;

        if (colDest == null)
            {
            return null;
            }

        // workflow:
        //
        // 1. determine the set of candidate local addresses
        // 2. determine the set of candidate destination addresses
        // 3. check for exact matches between colLocal and the filtered colDest
        // 4. filter the destination address set to match the type (IPv4 or IPv6) of the client address
        // 5. attempt to find a routable address using the filtered set of destination addresses
        // 6. if no routable addresses were found from the filtered set, then check the full set of destination
        //    addresses

        // 1. determine the set of candidate local addresses
        // - use all addresses on the machine if the source is local
        if (addrLocal == null)
            {
            fLocalSrc = true;
            colLocal  = getAllLocalAddresses();
            addrLocal = colLocal.iterator().next();
            }
        else
            {
            colLocal  = fLocalSrc ? getAllLocalAddresses() : Collections.singleton(addrLocal);
            }

        // 2. determine the set of candidate destination addresses
        // Note: need to consider whether the source is local as well, as getRoutes() may return
        //       local-only addresses, like 127.0.0.1, as candidates for remote clients
        if (!fLocalDest || !fLocalSrc)
            {
            colDest = colDest.stream().filter(IsRoutable.INSTANCE::evaluate).collect(Collectors.toSet());
            }

        // 3. check for exact matches between colLocal and the filtered colDest
        if (fLocalDest && !Collections.disjoint(colLocal, colDest))
            {
            if (colDest.contains(addrLocal))
                {
                // favor the passed in local address
                return Collections.singleton(addrLocal);
                }
            colLocal.retainAll(colDest);
            return colLocal;
            }

        // compute the set of destination addresses which could connect to the IP which this client connected to
        // the assumption is that the client could connect to those addresses as well

        // Note that getRoutes() returns addresses from its first parameter. colDest is our set of candidate addresses
        // to return, therefore colDest must be the first parameter.

        // 4. filter the destination address set to match the type (IPv4 or IPv6) of the client address
        // - Inet4/6Address(es) are final, so can compare classes
        Class                   clzAddrLocal = addrLocal.getClass();
        Collection<InetAddress> colIpvMatch  = colDest.stream().filter(p -> p.getClass() == clzAddrLocal).collect(Collectors.toList());

        // 5. attempt to find a routable address using the filtered set of destination addresses
        Collection<InetAddress> colResult = getRoutes(colIpvMatch, colLocal);

        // 6. if no routable addresses were found from the filtered set, then check the full set of destination
        //    addresses
        return colResult.isEmpty() ? getRoutes(colDest, colLocal) : colResult;
        }

    /**
     * The RoutableFilter evaluates to true for any InetAddress which is
     * externally routable.
     */
    public static class RoutableFilter
            extends IsRoutable
            implements Filter<InetAddress>
        {
        /**
         * Singleton instance.
         */
        public static final RoutableFilter INSTANCE = new RoutableFilter();
        }

    /**
     * SubnetMaskFilter evaluates to true for any address with matches the
     * pattern for the masked bits
     */
    public static class SubnetMaskFilter
            extends IsSubnetMask
            implements Filter<InetAddress>
        {
        /**
         * Construct a SubnetMaskFilter for the given pattern and mask.
         *
         * @param addrPattern  the pattern to match
         * @param addrMask     the mask identifying the portion of the pattern to match
         */
        public SubnetMaskFilter(InetAddress addrPattern, InetAddress addrMask)
            {
            super(addrPattern, addrMask);
            }

        /**
         * Construct a SubnetMaskFilter for the given pattern and mask bit count.
         *
         * @param addrPattern  the pattern to match
         * @param cMaskBits    the number of mask bits
         */
        public SubnetMaskFilter(InetAddress addrPattern, int cMaskBits)
            {
            super(addrPattern, cMaskBits);
            }

        /**
         * Construct a SubnetMaskFilter for the given pattern and slash mask.
         *
         * @see <a href="http://en.wikipedia.org/wiki/CIDR_notation">
         *      CIDR Notation</a>
         *
         * @param sAddr  the pattern and mask
         */
        public SubnetMaskFilter(String sAddr)
            {
            super(sAddr);
            }
        }

    /**
    * Check whether or not the specified address is a loopback address.
    *
    * @param addr  the InetAddress
    *
    * @return true iff the address is a loopback address
    *
    * @deprecated As of Coherence 3.0, replaced by {@link InetAddress#isLoopbackAddress()}
    */
    @Deprecated
    public static boolean isLoopbackAddress(InetAddress addr)
        {
        Base.azzert(addr != null);

        return addr.isLoopbackAddress();
        }

    /**
    * Check whether or not the specified address is a wildcard address.
    *
    * @param addr  the InetAddress
    *
    * @return true iff the address is a wildcard address
    *
    * @deprecated As of Coherence 3.0, replaced by {@link InetAddress#isAnyLocalAddress()}
    */
    @Deprecated
    public static boolean isAnyLocalAddress(InetAddress addr)
        {
        Base.azzert(addr != null);

        return addr.isAnyLocalAddress();
        }

    /**
    * Check whether or not the specified address is a link local address.
    *
    * @param addr  the InetAddress
    *
    * @return true iff the address is a link local address
    *
    * @deprecated As of Coherence 3.0, replaced by {@link InetAddress#isLinkLocalAddress()}
    */
    @Deprecated
    public static boolean isLinkLocalAddress(InetAddress addr)
        {
        Base.azzert(addr != null);

        return addr.isLinkLocalAddress();
        }

    /**
    * Check whether or not the specified address is a site local address.
    *
    * @param addr  the InetAddress
    *
    * @return true iff the address is a site local address
    *
    * @deprecated As of Coherence 3.0, replaced by {@link InetAddress#isSiteLocalAddress()}
    */
    @Deprecated
    public static boolean isSiteLocalAddress(InetAddress addr)
        {
        Base.azzert(addr != null);

        return addr.isSiteLocalAddress();
        }

    /**
    * Compare specified raw IP addresses taking into account IPv4-compatible
    * IPv6 addresses. Two addresses are considered virtually equal if they
    * have the same protocol (length) and are equal, or if one of them is
    * an IPv6 address that is IPv4-compatible and its IPv4 representation is
    * equal to the other IPv4 address.
    *
    * @param abAddr1  first IP address
    * @param abAddr2  second IP address
    *
    * @return true iff the addresses are compatible
    */
    public static boolean virtuallyEqual(byte[] abAddr1, byte[] abAddr2)
        {
        int cb1 = abAddr1.length;
        int cb2 = abAddr2.length;

        Base.azzert(cb1 == 4 || cb1 == 16);

        int of1 = 0;
        int of2 = 0;
        if (cb1 != cb2)
            {
            Base.azzert(cb2 == 4 || cb2 == 16);

            byte[] abIP6;
            if (cb1 == 4)
                {
                abIP6 = abAddr2;
                of2   = 12;
                }
            else
                {
                abIP6 = abAddr1;
                of1   = 12;
                }
            for (int of = 0; of < 12; of++)
                {
                if (abIP6[of] != 0)
                    {
                    return false;
                    }
                }
            }

        while (of1 < cb1)
            {
            if (abAddr1[of1++] != abAddr2[of2++])
                {
                return false;
                }
            }

        return true;
        }


    /**
    * Converts a byte array to a raw IP address string representation.
    * The format of the address string is one of the following:
    * <ul>
    *   <li> "d.d.d.d" for IPv4 address
    *   <li> "::d.d.d.d" for IPv4 compatible IPv6 address
    *   <li> "x:x:x:x:x:x:d.d.d.d" for IPv6 mapped IPv4 address
    * </ul>
    *
    * @param ab  the byte array holding the IP address
    *
    * @return  the IP address string
    *
    * @see InetAddress#getHostAddress()
    * @see Inet6Address
    */
    public static String toString(byte[] ab)
        {
        int cb = ab.length;
        int of = 0;

        Base.azzert(cb == 4 || cb == 16);

        StringBuilder sb    = new StringBuilder(40);
        boolean       fIPv4 = true;
        if (cb == 16)
            {
            int ofZ    = 0;
            int cZ     = 0;
            int cZMax  = 0;
            int ofZMax = 0;
            for (int i = 0; i < (fIPv4 ? 6 : 8); i++, of+=2)
                {
                int iVal = (((int) ab[of + 0] & 0x00ff) << 8)
                         | (((int) ab[of + 1] & 0x00ff));

                if (i > 0)
                    {
                    sb.append(":");
                    }

                if (iVal == 0)
                    {
                    if (++cZ == 1)
                        {
                        ofZ = sb.length();
                        }
                    }
                else if (cZ > 0)
                    {
                    if (cZ > cZMax)
                        {
                        ofZMax = ofZ;
                        cZMax  = cZ;
                        }
                    cZ = 0; // reset
                    }

                sb.append(Base.toHexString(iVal, Base.getMaxHexDigits(iVal)).toLowerCase()); // IPv6 is generally lower-cased

                fIPv4 &= iVal == (i == 5 ? 0x0ffff : 0);
                }

            if (cZMax > 1)
                {
                // collapse longest set of running zeros to ::
                sb.replace(ofZMax, ofZMax + cZMax * 2, ofZMax == 0 ? "::" : ":");
                }

            if (fIPv4)
                {
                sb.append(':');
                }
            }

        if (fIPv4)
            {
            for (int i = 0; i < 3; i++, of++)
                {
                sb.append(((int) ab[of]) & 0xff)
                  .append('.');
                }
            sb.append(((int) ab[of]) & 0xff);
            }

        return sb.toString();
        }

    /**
    * Format an IP address string representing the specified InetAddress
    * object. The main difference if this method over the
    * <tt>addr.toString()</tt> call is that this implementation avoids a call
    * to the <tt>addr.getHostName()</tt> method, which could be very expensive
    * due to the reverse DNS lookup.
    * <br>
    * For IPv6 addresses this method produces an alternative form of
    * "x:x:x:x:x:x:d.d.d.d" and a compressed form of "::d.d.d.d" for IPv4
    * compatible addresses (instead of the default form "x:x:x:x:x:x:x:x").
    *
    * @param addr  the address for which to format the IP address string
    *
    * @return  the IP address string
    *
    * @see InetAddress#getHostAddress()
    * @see Inet6Address
    */
    public static String toString(InetAddress addr)
        {
        if (addr == null)
            {
            return String.valueOf(addr);
            }

        if (addr instanceof Inet6Address)
            {
            return toString(addr.getAddress());
            }

        return addr.getHostAddress();
        }

    /**
     * Format a port string representing the specified port number.
     * <p>
     * If nPort is an extended 32 bit port, then the output will be of the form
     * "port.sub-port"
     *
     * @param nPort  the port
     *
     * @return the port string
     *
     * @see InetSocketAddress32
     *
     * @since 12.2.1
     */
    public static String toString(int nPort)
        {
        String sPort = Integer.toString(MultiplexedSocketProvider.getBasePort(nPort));
        return MultiplexedSocketProvider.isPortExtended(nPort)
            ? sPort + '.' + MultiplexedSocketProvider.getSubPort(nPort)
            : sPort;
        }
    }
