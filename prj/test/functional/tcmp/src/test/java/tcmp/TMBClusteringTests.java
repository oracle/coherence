/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package tcmp;

import common.AbstractFunctionalTest;
import org.junit.BeforeClass;

/**
 * A collection of functional tests for TCMP-TMB.
 *
 * @author jh  2013.12.09
 */
public class TMBClusteringTests
        extends ClusteringTests
    {

    // ----- ClusterTests overrides -----------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public String getProject()
        {
        return System.getProperty("test.project","tcmp-tmb");
        }


    @BeforeClass
    public static void _startup()
        {
        System.setProperty("tangosol.coherence.transport.reliable", "tmb");

        AbstractFunctionalTest._startup();
        }
    }
