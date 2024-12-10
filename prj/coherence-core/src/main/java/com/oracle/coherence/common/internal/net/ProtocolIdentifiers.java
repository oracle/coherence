/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.internal.net;

import com.oracle.coherence.common.internal.net.socketbus.SocketMessageBus;

/**
 * ProtocolIdentifiers serves as a single location to track the various protocol identifiers contained within this
 * project.  The intent of this singular location is to help to easily avoid having conflicting identifiers. Once
 * an ID has been assigned to a protocol is should not be changed or recycled.
 *
 * The prescribed ID allocation pattern uses 32 bit ids in the range of 0x05AC1E000..0x05AC1EFFF,
 * i.e. ORACLE000..ORACLEFFF.
 *
 * @author mf  2013.11.06
 */
public class ProtocolIdentifiers
    {
    /**
     * @see MultiplexedSocketProvider
     */
    public static final int MULTIPLEXED_SOCKET = 0x05AC1E000;

    /**
     * @see SocketMessageBus
     */
    public static final int SOCKET_MESSAGE_BUS = 0x05AC1E001;
    }
