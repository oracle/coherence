/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package extend;

import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.tangosol.dev.tools.CommandLineTool;

import com.tangosol.io.ByteArrayReadBuffer;
import com.tangosol.io.ReadBuffer;
import com.tangosol.io.pof.PofBufferReader;
import com.tangosol.io.pof.SimplePofContext;

import java.io.DataInputStream;
import java.io.DataOutputStream;

import java.net.InetSocketAddress;
import java.net.Socket;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import java.util.zip.CRC32;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;

/**
 * Test Ping to a Coherence proxy server.
 *
 * @author lh  2014.04.17
 */
public class CohPing
    {
    /**
     * Parse and validate the command-line parameters and start the test.
     *
     * @param asArg  an array of command line parameters
     */
    public static void main(String[] asArg)
        {
        Map     mapArgs;
        String  sAddr;
        int     nPort;
        int     nTimeout;
        boolean fList;

        try
            {
            mapArgs = CommandLineTool.parseArguments(asArg, VALID_COMMANDS,
                   true /*case sensitive*/);
            if (mapArgs.isEmpty())
                {
                System.out.println();
                System.out.println("Missing proxy server IP address and port.");
                System.out.println();
                showInstructions();
                return;
                }

            if (mapArgs.containsKey(COMMAND_HELP)
                    || mapArgs.get(Integer.valueOf(0)) != null)
                {
                showInstructions();
                return;
                }

            sAddr = (String) mapArgs.get(COMMAND_ADDR);
            nPort = Integer.parseInt((String) mapArgs.get(COMMAND_PORT));

            String sTimeout = (String) mapArgs.get(COMMAND_TIMEOUT);
            nTimeout = sTimeout == null ? 30 : Integer.parseInt(sTimeout);
            fList    = mapArgs.containsKey(COMMAND_LIST);
            }
        catch (Throwable e)
            {
            System.err.println();
            System.err.println(e);
            System.err.println();
            showInstructions();
            return;
            }

        try
            {
            if (new CohPing(sAddr, nPort, nTimeout, fList).run())
                {
                System.out.println("CohPing succeeded");
                }
            else
                {
                System.out.println("CohPing failed");
                }
            }
        catch (Exception e)
            {
            System.err.println("An exception occurred while executing the CohPing:");
            System.err.println(e);
            System.err.println();
            }
        }

    /**
     * Display the command-line instructions.
     */
    protected static void showInstructions()
        {
        System.out.println();
        System.out.println("java extend.CohPing <commands ...>");
        System.out.println();
        System.out.println("command options:");
        System.out.println("\t-address    the IP address of the proxy server to send the ping request");
        System.out.println("\t-port       the listen port of the proxy server");
        System.out.println("\t-timeout    (optional) the timeout (in seconds) of the ping request, default " + DEFAULT_TIMEOUT + "s");
        System.out.println("\t-list       (optional) returns a list of the address:port of the members in the proxy service, seperated by \',\'");
        System.out.println();
        System.out.println("Example:");
        System.out.println("\tjava extend.CohPing -address 10.120.21.55 -port 9000 -list");
        System.out.println();
        }

    // ----- CohPing methods ------------------------------------------

    /**
     * Construct the CohPing object.
     *
     * @param sAddr     address of the proxy server
     * @param nPort     listen port of the proxy server
     * @param nTimeout  ping timout, in seconds
     * @param fList     whether to return a list of the address:port of the proxy servers in the proxy service.
     */
    public CohPing(String sAddr, int nPort, int nTimeout, boolean fList)
        {
        m_sAddr    = sAddr;
        m_nPort    = nPort;
        m_nTimeout = nTimeout*1000;
        m_fList    = fList;
        }

    /**
     * Run the test.
     */
    public boolean run()
        {
        try
            {
            Socket clientSocket = new Socket();
            clientSocket.connect(new InetSocketAddress(m_sAddr, m_nPort), m_nTimeout);

            DataOutputStream outToServer  = new DataOutputStream(clientSocket.getOutputStream());
            DataInputStream  inFromServer = new DataInputStream(clientSocket.getInputStream());
            if (this.m_fList)
                {
                outToServer.write(QUERY_PING);
                }
            else
                {
                outToServer.write(BASIC_PING);
                }
            outToServer.flush();

            byte[] resultReturned;

            Eventually.assertThat(invoking(inFromServer).available(), is(not(0)));

            int    len = inFromServer.available();
//            while (len == 0)
//                {
//                Thread.sleep(100);
//                len = inFromServer.available();
//                }
            resultReturned = new byte[len];
            len = inFromServer.read(resultReturned);
            if (!m_fList)
                {
                if (Arrays.equals(BASIC_RESULT, resultReturned))
                    {
                    return true;
                    }
                return false;
                }

            int i;
            for (i = 0; i < len; i++)
                {
                if (i > 6 && resultReturned[i] == 0x55)
                    {
                    break;
                    }
                }
            initPOFReader(len);
            System.arraycopy(resultReturned, i, this.m_ab, 0, len - i);
            Collection aAddresses = m_reader.readCollection(0, null);
            System.out.println("Cluster-hash: " + makeCheckSumHash(this.m_ab));
            System.out.println("Cluster-list: " + aAddresses);
            return true;
            }
        catch (Exception e)
            {
            System.err.println("An exception occurred while executing the CohPing:");
            System.err.println(e);
            }
            return false;
        }

    /**
     * Given a byte array, return its checksum hash.
     *
     * @param input  input byte array to make checksum from
     */
    public long makeCheckSumHash(byte[] input)
        {
        CRC32 checksum = new CRC32();
        checksum.reset();
        checksum.update(input);
        return checksum.getValue();
        }

    /**
     * Initialize a POFReader used to parse the POF data returned from proxy server.
     *
     * @param len  length of the buffer.
     */
    protected void initPOFReader(int len)
        {
        m_ab     = new byte[len];
        m_rb     = new ByteArrayReadBuffer(m_ab);
        m_bi     = m_rb.getBufferInput();
        m_ctx    = new SimplePofContext();
        m_reader = new PofBufferReader(m_bi, m_ctx);
        }

    // ---- constants -------------------------------------------------------

    public static final String COMMAND_HELP    = "?";
    public static final String COMMAND_ADDR    = "address";
    public static final String COMMAND_PORT    = "port";
    public static final String COMMAND_TIMEOUT = "timeout";
    public static final String COMMAND_LIST    = "list";
    public static final int    DEFAULT_TIMEOUT = 30;

    public static final String[] VALID_COMMANDS =
        {
        COMMAND_HELP,
        COMMAND_ADDR,
        COMMAND_PORT,
        COMMAND_TIMEOUT,
        COMMAND_LIST
        };

    private static final byte[] BASIC_PING   = new byte[] {7, 0, 3, 0, 0, 66, 0, 64};
    private static final byte[] BASIC_RESULT = new byte[] {9, 0, 4, 2, 0, 66, 0, 3, 100, 64};
    private static final byte[] QUERY_PING   = new byte[] {7, 0, 3, 0, 0, 66, 1, 64};

    // ----- data members ---------------------------------------------------

    private String  m_sAddr;
    private int     m_nPort;
    private int     m_nTimeout;
    private boolean m_fList;

    private byte[]                 m_ab;
    private SimplePofContext       m_ctx;
    private PofBufferReader        m_reader;
    private ReadBuffer             m_rb;
    private ReadBuffer.BufferInput m_bi;
    }
