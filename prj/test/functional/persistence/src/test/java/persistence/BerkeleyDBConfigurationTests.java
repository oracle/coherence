/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package persistence;

import com.tangosol.persistence.bdb.BerkeleyDBEnvironment;

/**
 * Functional persistence configuration tests using BerkeleyDB.
 *
 * @author jh  2014.02.14
 */
public class BerkeleyDBConfigurationTests
        extends AbstractConfigurationPersistenceTests
    {

    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor.
     */
    public BerkeleyDBConfigurationTests()
        {
        super("persistence-bdb-cache-config.xml");
        }

    // ----- AbstractConfigurationPersistenceTests methods ------------------

    /**
     * {@inheritDoc}
     */
    @Override
    protected Class getPersistenceEnvironmentImpl()
        {
        return BerkeleyDBEnvironment.class;
        }
    }
