/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package ssl;


import com.oracle.coherence.common.net.SocketProvider;

import javax.net.ssl.SSLException;
import com.oracle.coherence.testing.net.EchoClient;
import com.oracle.coherence.testing.net.EchoNIOServer;
import com.oracle.coherence.testing.net.EchoServer;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;


/**
* Functional tests for SSL where the server-side is using NIO.
*
* @author jh  2010.04.30
*/
public class SSLNIOServerTests
        extends SSLTests
    {
    protected EchoServer createServer(SocketProvider provider)
        {
        return new EchoNIOServer(provider, getPort());
        }

    @Test
    public void testNonSSLNonEchoServerConfig()
            throws IOException
        {
        EchoClient client = createClient("provider-config-client.xml");
        EchoServer server = createServer((String) null);

        final String sMsg = "HELLO!";

        server.setEcho(false); // server will just eat the content, thus SSL handshake should timeout
        server.start();

        try
            {
            assertEquals(server.getConnectionCount(), 0);
            client.connect(1000); // wait 1s for handshake
            client.echo(sMsg); // SSL may eat the timeout but usage will then fail
            fail("SSL exception expected");
            }
        catch (SSLException e)
            {
            // expected
            }
        catch (IOException e)
            {
            // COH-4002: Handle IOException here since a RST can happen from the
            //           server side when it closes the connection with data
            //           left in the buffer.
            }
        finally
            {
            client.disconnect();
            server.stop();
            }
        }
    }
