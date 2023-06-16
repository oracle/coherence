/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package tcmp;

import com.oracle.coherence.testing.AbstractFunctionalTest;

import com.tangosol.util.Resources;

import org.junit.BeforeClass;

import java.net.URL;

public class SecuredProductionClusteringTests
        extends ClusteringTests
    {

    // ----- ClusterTests overrides -----------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public String getProject()
        {
        return System.getProperty("test.project","securedProduction");
        }


    @BeforeClass
    public static void _startup()
        {
        URL url = Resources.findFileOrResource("keystore.jks", getContextClassLoader());

        System.setProperty("coherence.security.keystore",   url.toExternalForm());
        System.setProperty("coherence.security.truststore", url.toExternalForm());
        System.setProperty("coherence.security.password",   "password");
        System.setProperty("coherence.wka",                 "127.0.0.1");
        System.setProperty("coherence.wka.port",            "8888");
        System.setProperty("coherence.mode",                "prod");
        System.setProperty("coherence.secured.production",  "true");

        AbstractFunctionalTest._startup();
        }
    }
