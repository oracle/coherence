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
import java.util.Properties;

/**
 * A collection of functional tests for TCMP-TMBS.
 *
 * @author jh  2013.12.09
 */
public class TMBSClusteringTests
        extends ClusteringTests
    {

    @BeforeClass
    public static void _startup()
        {
        s_properties = new Properties();

        URL url = Resources.findFileOrResource("keystore.jks", getContextClassLoader());
        s_properties.setProperty("tangosol.coherence.security.keystore", url.toExternalForm());
        s_properties.setProperty("tangosol.coherence.transport.reliable", "tmbs");
        s_properties.setProperty("tangosol.coherence.socketprovider", "ssl");
        s_properties.setProperty("tangosol.coherence.security.password", "password");
        s_properties.setProperty("tangosol.coherence.wka", "127.0.0.1");
        s_properties.setProperty("tangosol.coherence.wka.port", "8888");
        System.getProperties().putAll(s_properties);

        AbstractFunctionalTest._startup();
        }

    // ----- ClusterTests overrides -----------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public String getProject()
        {
        return System.getProperty("test.project","tcmp-tmbs");
        }

    @Override
    public Properties getProperties()
        {
        Properties properties = super.getProperties();
        properties.putAll(s_properties);
        return properties;
        }

    protected static Properties s_properties;
    }
