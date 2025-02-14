/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package orm;


import data.persistence.DomainClassPolicy;


public class JpaCacheStoreTests
        extends AbstractPersistenceTests
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    public JpaCacheStoreTests()
        {
        super(CONFIG_FILE_NAME);
        }


    // ----- AbstractPersistenceTests methods -------------------------------

    protected void initCache()
            throws Exception
        {
        m_cache = getNamedCache(ENTITY_PERSON);
        }

    protected void initDomainClassPolicy()
        {
        m_policy = new DomainClassPolicy.PersonClass();
        }


    // ----- constants ------------------------------------------------------

    public static String CONFIG_FILE_NAME = "jpa-cache-config.xml";
    }