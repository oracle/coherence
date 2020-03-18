/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package ssl;


import com.oracle.coherence.common.net.SocketProvider;

import net.EchoClient;
import net.EchoNIOClient;


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
