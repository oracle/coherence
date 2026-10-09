/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.net.exabus.util;


import com.oracle.coherence.common.base.Hasher;
import com.oracle.coherence.common.net.SocketProvider;
import com.oracle.coherence.common.net.exabus.EndPoint;

import java.net.SocketAddress;

import java.util.Objects;
import java.util.UUID;


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
     *
     * Query parameters are treated as opaque transport data by this
     * constructor.
     */
    public UrlEndPoint(String sName, SocketProvider provider,
            Hasher<? super SocketAddress> hasher)
        {
        this(sName, provider, hasher, null);
        }

    /**
     * Construct a SocketEndPoint with an optional logical peer identity.
     * <p>
     * When {@code uuid} is non-null, a {@code bus-id} query parameter is
     * reserved for that identity. The canonical name retains the parameter,
     * while {@link #getQueryString()} and {@link #getTransportName()} exclude
     * it and expose only transport data.
     *
     * @param sName     the endpoint name
     * @param provider  the provider
     * @param hasher    the SocketAddress hasher
     * @param uuid      the logical peer identity, or {@code null}
     *
     * @since 26.10
     */
    public UrlEndPoint(String sName, SocketProvider provider,
            Hasher<? super SocketAddress> hasher, UUID uuid)
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

        UUID uuidQuery = uuid == null ? null : parseIdentity(sQuery);
        if (uuid != null && uuidQuery != null && !uuid.equals(uuidQuery))
            {
            throw new IllegalArgumentException("endpoint contains a different logical peer identity");
            }

        UUID   uuidIdentity = uuid;
        String sQueryTransport = uuid == null ? sQuery : withoutIdentity(sQuery);
        String sTransportName  = sProtocol + PROTOCOL_DELIMITER + sAddress
                + (sQueryTransport == null ? "" : '?' + sQueryTransport);
        String sCanonical   = uuidIdentity == null || uuidQuery != null
                ? sName
                : withIdentity(sName, uuidIdentity);

        f_sName          = sCanonical;
        f_sTransportName = sTransportName;
        f_sProtocol      = sProtocol;
        f_hasher         = hasher;
        f_address        = provider.resolveAddress(sAddress);
        f_sQuery         = sQueryTransport;
        f_uuid           = uuidIdentity;
        f_nHashCode = sProtocol.hashCode()
                + (uuidIdentity == null ? hasher.hashCode(f_address) : uuidIdentity.hashCode());
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
     * Return the transport query string, if any.
     * <p>
     * For an identified endpoint, this value excludes the reserved
     * {@code bus-id} logical identity parameter. Use
     * {@link #getCanonicalName()} for the complete resolvable endpoint name.
     *
     * @return the transport query string, or {@code null}
     */
    public String getQueryString()
        {
        return f_sQuery;
        }

    /**
     * Return the logical peer identity, if this endpoint represents an
     * identified peer rather than only a transport address.
     *
     * @return the logical peer identity, or {@code null}
     *
     * @since 26.10
     */
    public UUID getPeerIdentity()
        {
        return f_uuid;
        }

    /**
     * Return the transport-only endpoint name, excluding the reserved logical
     * peer identity.
     *
     * @return the transport endpoint name
     *
     * @since 26.10
     */
    public String getTransportName()
        {
        return f_sTransportName;
        }


    // ----- EndPoint interface ---------------------------------------------

    /**
     * Return the complete resolvable endpoint name.
     * <p>
     * For an identified endpoint, this name includes the reserved
     * {@code bus-id} logical identity parameter.
     *
     * @return the complete resolvable endpoint name
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
     * The equality is not String equality. Identified endpoints compare by
     * protocol and logical peer identity, allowing the same peer to use
     * different transport aliases. Unidentified endpoints compare by protocol
     * and resolved address. The query string is not considered.
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
            UUID uuidThis = f_uuid;
            UUID uuidThat = that.f_uuid;

            return getProtocol().equals(that.getProtocol()) &&
                   (uuidThis == null && uuidThat == null
                        ? f_hasher.equals(getAddress(), that.getAddress())
                        : Objects.equals(uuidThis, uuidThat));
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

    /**
     * Add the logical peer identity to the diagnostic form of an endpoint.
     *
     * @param sName  the transport endpoint name
     * @param uuid   the logical peer identity
     *
     * @return the identified endpoint name
     *
     * @since 26.10
     */
    private static String withIdentity(String sName, UUID uuid)
        {
        return sName + (sName.indexOf('?') < 0 ? '?' : '&') + BUS_ID_QUERY_PREFIX + uuid;
        }

    /**
     * Parse the reserved logical peer identity query parameter.
     *
     * @param sQuery  the endpoint query string
     *
     * @return the logical peer identity, or {@code null}
     *
     * @since 26.10
     */
    private static UUID parseIdentity(String sQuery)
        {
        UUID uuid = null;
        if (sQuery != null)
            {
            for (String sParameter : sQuery.split("&", -1))
                {
                if (sParameter.startsWith(BUS_ID_QUERY_PREFIX))
                    {
                    UUID uuidParsed;
                    try
                        {
                        uuidParsed = UUID.fromString(sParameter.substring(BUS_ID_QUERY_PREFIX.length()));
                        }
                    catch (IllegalArgumentException e)
                        {
                        throw new IllegalArgumentException("endpoint contains an invalid logical peer identity", e);
                        }

                    if (uuid != null && !uuid.equals(uuidParsed))
                        {
                        throw new IllegalArgumentException("endpoint contains conflicting logical peer identities");
                        }
                    uuid = uuidParsed;
                    }
                }
            }
        return uuid;
        }

    /**
     * Remove the reserved logical peer identity from a transport query.
     *
     * @param sQuery  the endpoint query string
     *
     * @return the transport-only query string, or {@code null}
     *
     * @since 26.10
     */
    private static String withoutIdentity(String sQuery)
        {
        if (sQuery == null)
            {
            return null;
            }

        StringBuilder builder = null;
        for (String sParameter : sQuery.split("&", -1))
            {
            if (!sParameter.startsWith(BUS_ID_QUERY_PREFIX))
                {
                if (builder == null)
                    {
                    builder = new StringBuilder(sParameter);
                    }
                else
                    {
                    builder.append('&').append(sParameter);
                    }
                }
            }
        return builder == null ? null : builder.toString();
        }


    // ----- constants -----------------------------------------------------

    /**
     * The reserved query parameter prefix for a logical peer identity.
     */
    private static final String BUS_ID_QUERY_PREFIX = "bus-id=";

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
     * The transport-only endpoint name.
     */
    private final String f_sTransportName;

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
     * The optional logical peer identity.
     */
    private final UUID f_uuid;

    /**
     * The endpoint's hashcode
     */
    private final int f_nHashCode;

    /**
     * The SocketAddress hasher.
     */
    private final Hasher<? super SocketAddress> f_hasher;
    }
