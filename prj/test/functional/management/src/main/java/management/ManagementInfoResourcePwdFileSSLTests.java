/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package management;

import com.oracle.coherence.common.net.SSLSocketProvider;
import com.tangosol.coherence.management.internal.MapProvider;
import com.tangosol.internal.net.ssl.LegacyXmlSSLSocketProviderDependencies;
import com.tangosol.run.xml.XmlDocument;
import com.tangosol.run.xml.XmlHelper;
import org.junit.BeforeClass;

import javax.ws.rs.client.ClientBuilder;

/**
 * ManagementInfoResourceSSLTests tests the ManagementInfoResource over SSL.
 *
 * @author lh 2019.02.15
 */
public class ManagementInfoResourcePwdFileSSLTests
        extends ManagementInfoResourceSSLTests
    {
    // ----- junit lifecycle methods ----------------------------------------

    @BeforeClass
    public static void _startup()
        {
        SERVER_PREFIX = "testMgmtRESTPwdFileSSLServer";
        System.setProperty("coherence.override", "tangosol-coherence-override-filePwd.xml");
        System.setProperty("coherence.management.http.provider", "mySSLProvider");

        ManagementInfoResourceTests._startup();
        }

    /**
     * Initialize the test class.
     * <p>
     * This method starts the Coherence cluster, if it isn't already running.
     */
    @BeforeClass
    public static void startup()
        {
        m_client = createSslClient(ClientBuilder.newBuilder()
                .register(MapProvider.class));
        }

    @BeforeClass
    public static void setupSSL()
        {
        XmlDocument xml = XmlHelper.loadFileOrResource("ssl-config-client-pwdfile.xml", null);

        s_sslProviderClient = new SSLSocketProvider(
                new LegacyXmlSSLSocketProviderDependencies(xml));
        }
    }
