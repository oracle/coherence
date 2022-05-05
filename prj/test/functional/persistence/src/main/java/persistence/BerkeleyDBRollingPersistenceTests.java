/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package persistence;

/**
 * Rolling-restart tests of the BerkeleyDBPersistenceManager.
 *
 * @author jh  2012.10.18
 */
public class BerkeleyDBRollingPersistenceTests
        extends AbstractRollingPersistenceTests
    {

    // ----- AbstractRollingPersistenceTests methods ------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPersistenceManagerName()
        {
        return "BDB";
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCacheConfigPath()
        {
        return "rolling-persistence-bdb-cache-config.xml";
        }
    }
