/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.net.exabus.util;


import com.oracle.coherence.common.base.Hasher;
import com.oracle.coherence.common.net.SocketProvider;
import com.oracle.coherence.common.net.exabus.EndPoint;

import java.net.SocketAddress;


/**
 * UrlEndPoint is an EndPoint formatted using URL like syntax.
 * <p>
 * The basic syntax is protocol://address[?query], the format for the address portion
 * is ultimately parsed by supplied {@link SocketProvider}, and may deviate
 * from proper URL syntax.
 * <p>
 * The UrlEndPoint does not currently support URL portions beyond protocol, address, and query string,
 * such as path.
 *
 * @author mf 2011.01.13
 */
public class UrlEndPoint
        implements EndPoint
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a SocketEndPoint.
     *
     * @param sName     the endpoint name
     * @param provider  the provider
     * @param hasher    the SocketAddress hasher
     */
    public UrlEndPoint(String sName, SocketProvider provider,
            Hasher<? super SocketAddress> hasher)
        {
        if (sName == null)
            {
            throw new IllegalArgumentException("name cannot be null");
            }

        int ofProtocolEnd = sName.indexOf(PROTOCOL_DELIMITER);
        if (ofProtocolEnd == -1)
            {
            throw new IllegalArgumentException("name does not contain a protocol");
            }
        String sProtocol  = sName.substring(0, ofProtocolEnd);
        String sRemainder = sName.substring(ofProtocolEnd + PROTOCOL_DELIMITER.length());
        String sAddress;
        String sQuery;

        if (sRemainder.indexOf('/') != -1)
            {
            throw new IllegalArgumentException("URL paths are not supported");
            }
        else if (sRemainder.indexOf('?') != -1)
            {
            // URL contains a query string; pull it out
            int ofQuery = sRemainder.indexOf('?');
            sAddress = sRemainder.substring(0, ofQuery);
            sQuery   = sRemainder.substring(ofQuery + 1);
            }
        else
            {
            sAddress = sRemainder;
            sQuery   = null;
            }

        f_sName     = sName;
        f_sProtocol = sProtocol;
        f_hasher    = hasher;
        f_address   = provider.resolveAddress(sAddress);
        f_sQuery    = sQuery;
        f_nHashCode = sProtocol.hashCode() + hasher.hashCode(f_address);
        }


    // ----- URLEndPoint interface ---------------------------------------

    /**
     * Return the SocketAddress represented by this EndPoint.
     *
     * @return the SocketAddress represented by this EndPoint
     */
    public SocketAddress getAddress()
        {
        return f_address;
        }

    /**
     * Return the protocol represented by this EndPoint.
     *
     * @return the protocol represented by this EndPoint
     */
    public String getProtocol()
        {
        return f_sProtocol;
        }

    /**
     * Return the URL EndPoints query string if any.
     *
     * @return the query string or null
     */
    public String getQueryString()
        {
        return f_sQuery;
        }


    // ----- EndPoint interface ---------------------------------------------

    /**
     * {@inheritDoc}
     */
    public String getCanonicalName()
        {
        return f_sName;
        }


    // ----- Object interface ----------------------------------------------

    /**
     * {@inheritDoc}
     */
    public int hashCode()
        {
        return f_nHashCode;
        }

    /**
     * Compare two UrlEndPoints for equality.
     * <p>
     * The equality is not String equality, but rather logical equality based upon the protocol and resolved address.
     * The query string if any is not considered in the equality comparison.
     * </p>
     *
     */
    public boolean equals(Object o)
        {
        if (this == o)
            {
            return true;
            }
        else if (o instanceof UrlEndPoint)
            {
            UrlEndPoint that = (UrlEndPoint) o;
            return getProtocol().equals(that.getProtocol()) &&
                   f_hasher.equals(getAddress(), that.getAddress());
            }
        else
            {
            return false;
            }
        }

    /**
     * {@inheritDoc}
     */
    public String toString()
        {
        return f_sName;
        }


    // ----- constants -----------------------------------------------------

    /**
     * The protocol delimiter
     */
    public static final String PROTOCOL_DELIMITER = "://";

    // ----- data members --------------------------------------------------

    /**
     * The canonical name.
     */
    private final String f_sName;

    /**
     * The protocol name.
     */
    private final String f_sProtocol;

    /**
     * The SocketAddress.
     */
    private final SocketAddress f_address;

    /**
     * The query string if any.
     */
    private final String f_sQuery;

    /**
     * The endpoint's hashcode
     */
    private final int f_nHashCode;

    /**
     * The SocketAddress hasher.
     */
    private final Hasher<? super SocketAddress> f_hasher;
    }
