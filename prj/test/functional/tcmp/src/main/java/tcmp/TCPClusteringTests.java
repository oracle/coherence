/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package tcmp;

import com.oracle.coherence.testing.AbstractFunctionalTest;
import org.junit.BeforeClass;

/**
 * A collection of functional tests for TCMP-TCP.
 *
 * @author jh  2013.12.09
 */
public class TCPClusteringTests
        extends ClusteringTests
    {

    // ----- ClusterTests overrides -----------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public String getProject()
        {
        return System.getProperty("test.project","tcmp-tcp");
        }


    @BeforeClass
    public static void _startup()
        {
        System.setProperty("coherence.socketprovider", "tcp");

        AbstractFunctionalTest._startup();
        }
    }
