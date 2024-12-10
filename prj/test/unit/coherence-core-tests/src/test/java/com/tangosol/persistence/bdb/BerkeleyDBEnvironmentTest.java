/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.persistence.bdb;

import com.tangosol.persistence.AbstractPersistenceEnvironmentTest;
import com.tangosol.persistence.AbstractPersistenceEnvironment;

import java.io.IOException;

/**
 * Unit test of the BerkeleyDBEnvironment.
 *
 * @author jh  2013.05.23
 */
public class BerkeleyDBEnvironmentTest
        extends AbstractPersistenceEnvironmentTest
    {

    // ----- test lifecycle -------------------------------------------------

    @Override
    protected AbstractPersistenceEnvironment createPersistenceEnvironment()
            throws IOException
        {
        BerkeleyDBEnvironment env = new BerkeleyDBEnvironment(m_fileActive, m_fileSnapshot, m_fileTrash);
        env.setDaemonPool(m_pool);
        return env;
        }
    }
