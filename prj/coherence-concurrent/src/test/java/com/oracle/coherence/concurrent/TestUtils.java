/*
 * Copyright (c) 2022 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent;

import java.net.InetAddress;

import com.tangosol.net.Member;
import com.tangosol.net.proxy.RemoteMember;

import com.tangosol.util.UID;
import com.tangosol.util.UUID;

/**
 * Testing utilities.
 */
public class TestUtils
    {
    /**
     * Create remote member.
     *
     * @param address  the IP listen address of the remote member
     *                 ProxyService Acceptor
     * @param nPort    the TCP listen port of the remote member
     *                 ProxyService Acceptor
     * @return remote member
     */
    public static Member createRemoteMember(InetAddress address, int nPort)
        {
        return new RemoteMember(address, nPort)
            {
            @Override
            public int getId()
                {
                return 1;
                }

            @Override
            public UID getUid()
                {
                return f_uid;
                }

            @Override
            public UUID getUuid()
                {
                return f_uuid;
                }

            // ---- data members ------------------------------------------------

            final UID f_uid = new UID();

            final UUID f_uuid = new UUID();
            };
        }
    }
