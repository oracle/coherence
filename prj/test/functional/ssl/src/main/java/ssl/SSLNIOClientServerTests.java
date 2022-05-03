/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package ssl;


import com.oracle.coherence.testing.net.EchoClient;
import com.oracle.coherence.testing.net.EchoNIOClient;
import com.oracle.coherence.testing.net.EchoNIOServer;
import com.oracle.coherence.testing.net.EchoServer;

import com.oracle.coherence.common.net.SocketProvider;


/**
* Functional tests for SSL where both the client and server-side is using NIO.
*
* @author jh  2010.04.30
*/
public class SSLNIOClientServerTests
        extends SSLTests
    {
    protected EchoClient createClient(SocketProvider provider)
        {
        return new EchoNIOClient(provider, getPort());
        }

    protected EchoServer createServer(SocketProvider provider)
        {
        return new EchoNIOServer(provider, getPort());
        }
    }
