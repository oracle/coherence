/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package ssl;


import net.EchoClient;
import net.EchoNIOClient;
import net.EchoNIOServer;
import net.EchoServer;

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
