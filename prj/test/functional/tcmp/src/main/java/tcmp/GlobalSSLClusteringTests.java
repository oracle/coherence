/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package tcmp;

import com.oracle.coherence.testing.AbstractFunctionalTest;
import com.tangosol.net.SocketProviderFactory;
import com.tangosol.util.Resources;
import org.junit.BeforeClass;

import java.net.URL;

public class GlobalSSLClusteringTests
        extends ClusteringTests
    {

    // ----- ClusterTests overrides -----------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public String getProject()
        {
        return System.getProperty("test.project","global-ssl");
        }


    @BeforeClass
    public static void _startup()
        {
        URL url = Resources.findFileOrResource("keystore.jks", getContextClassLoader());
        System.setProperty("coherence.security.keystore", url.toExternalForm());
        System.setProperty(SocketProviderFactory.PROP_GLOBAL_PROVIDER, "ssl");
        System.setProperty("coherence.security.password", "password");
        System.setProperty("coherence.wka",               "127.0.0.1");
        System.setProperty("coherence.wka.port",          "8888");

        AbstractFunctionalTest._startup();
        }
    }
