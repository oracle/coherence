/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package tcmp;

import com.tangosol.util.Resources;
import common.AbstractFunctionalTest;
import org.junit.BeforeClass;

import java.net.URL;

/**
 * A collection of functional tests for TCMP-SSL.
 *
 * @author jh  2013.12.09
 */
public class SSLClusteringTests
        extends ClusteringTests
    {

    // ----- ClusterTests overrides -----------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public String getProject()
        {
        return System.getProperty("test.project","tcmp-ssl");
        }


    @BeforeClass
    public static void _startup()
        {
        URL url = Resources.findFileOrResource("keystore.jks", getContextClassLoader());
        System.setProperty("tangosol.coherence.security.keystore", url.toExternalForm());
        System.setProperty("tangosol.coherence.socketprovider",    "ssl");
        System.setProperty("tangosol.coherence.security.password", "password");
        System.setProperty("tangosol.coherence.wka",               "127.0.0.1");
        System.setProperty("tangosol.coherence.wka.port",          "8888");

        AbstractFunctionalTest._startup();
        }
    }
