/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.net;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import java.util.Objects;

/**
 * A socket address retaining the application-supplied hostname independently
 * of the mutable reverse-lookup cache in {@link InetAddress}. TLS uses this
 * original name as its reference identity.
 *
 * @author phf  2026.10.08
 * @since 26.10
 */
public final class InetSocketAddressWithHostname
        extends InetSocketAddress
    {
    /**
     * Resolve a socket address and retain the supplied name.
     *
     * @param sHost  the application-supplied hostname or IP literal
     * @param nPort  the port
     */
    public InetSocketAddressWithHostname(String sHost, int nPort)
        {
        super(sHost, nPort);
        m_sHost = sHost;
        }

    /**
     * Retain a name that has already been resolved, without resolving it again.
     *
     * @param sHost    the application-supplied hostname or IP literal
     * @param address  the resolved address
     * @param nPort    the port
     */
    public InetSocketAddressWithHostname(String sHost, InetAddress address, int nPort)
        {
        super(Objects.requireNonNull(address), nPort);
        m_sHost = Objects.requireNonNull(sHost);
        }

    /**
     * Return the application-supplied name without consulting reverse DNS.
     *
     * @return the original hostname or IP literal
     */
    public String getOriginalHostName()
        {
        return m_sHost;
        }

    private final String m_sHost;

    private static final long serialVersionUID = 1L;
    }
