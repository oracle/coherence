/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package ssl;


import com.oracle.coherence.common.net.SocketProvider;

import com.oracle.coherence.testing.net.EchoClient;
import com.oracle.coherence.testing.net.EchoNIOClient;


/**
* Functional tests for SSL where the client-side is using NIO.
*
* @author jh  2010.04.30
*/
public class SSLNIOClientTests
        extends SSLTests
    {
    protected EchoClient createClient(SocketProvider provider)
        {
        return new EchoNIOClient(provider, getPort());
        }
    }
