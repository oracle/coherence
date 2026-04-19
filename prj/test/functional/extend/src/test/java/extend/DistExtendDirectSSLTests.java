/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package extend;

import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.testsupport.deferred.Eventually;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Properties;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertTrue;

/**
* A collection of functional tests for Coherence*Extend that use the
* "dist-extend-direct" cache over SSL.
*
* @author jh  2005.11.29
*/
public class DistExtendDirectSSLTests
        extends AbstractExtendTests
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    public DistExtendDirectSSLTests()
        {
        super(CACHE_DIST_EXTEND_DIRECT, "client-cache-config-ssl.xml");
        }


    // ----- test lifecycle -------------------------------------------------

    /**
    * Initialize the test class.
    */
    @BeforeClass
    public static void _startup()
        {
        setupProps();
        s_member = startCacheServerWithProxy("DistExtendDirectSSLTests", "extend", "server-cache-config-ssl.xml");
        }

    /**
    * Shutdown the test class.
    */
    @AfterClass
    public static void shutdown()
        {
        stopCacheServer("DistExtendDirectSSLTests");
        }

    /**
     * COH-33073 - ensure SSL-configured ExtendTcpProxy (TcpProcessor path)
     * cleanly closes a connection when it receives malformed TLS bytes
     * that cause BUFFER_UNDERFLOW / SSL decode failure.
     */
    @Test
    public void testProxyClosesConnectionOnIncompleteTlsRecord()
            throws Exception
        {
        Eventually.assertDeferred(() -> s_member.isServiceRunning("ExtendTcpProxyService"), is(true));

        Properties props = System.getProperties();
        String     sHost = props.getProperty("test.extend.address.remote", "127.0.0.1");
        int        nPort = Integer.parseInt(props.getProperty("test.extend.port"));

        byte[] abPayload = new byte[]
            {
            (byte) 0x16, (byte) 0x03, (byte) 0x03, (byte) 0x00,
            (byte) 0x50, (byte) 0x01, (byte) 0x02, (byte) 0x03
            };

        boolean fClosed = false;

        try (Socket socket = new Socket(sHost, nPort))
            {
            socket.setSoTimeout(2000);

            OutputStream out = socket.getOutputStream();
            out.write(abPayload);
            out.flush();

            // half close client -> server
            socket.shutdownOutput();

            InputStream in     = socket.getInputStream();
            byte[]      buffer = new byte[32];

            int attempts = 3;

            while (attempts-- > 0)
                {
                try
                    {
                    int n = in.read(buffer);
                    if (n == -1)
                        {
                        // graceful close (FIN)
                        System.out.println("Server closed socket (FIN) after malformed TLS payload");
                        fClosed = true;
                        break;
                        }
                    continue;
                    }
                catch (SocketException e)
                    {
                    // connection reset (RST) also means server closed the connection
                    System.out.println("Server reset socket (RST) after malformed TLS payload: " + e.getMessage());
                    fClosed = true;
                    break;
                    }
                catch (SocketTimeoutException e)
                    {
                    // server kept connection open
                    continue;
                    }
                }
            }

        assertTrue("ExtendTcpProxyService did not close the connection for malformed TLS input", fClosed);

        Eventually.assertDeferred(() -> s_member.isServiceRunning("ExtendTcpProxyService"), is(true));
        }

    // ----- data members ---------------------------------------------------

    private static CoherenceClusterMember s_member;
    }
