/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package orm;


import data.persistence.DomainClassPolicy;


public class JpaCacheStoreWithEmbeddedIdTests
        extends AbstractPersistenceTests
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    public JpaCacheStoreWithEmbeddedIdTests()
        {
        super(CONFIG_FILE_NAME);
        }


    // ----- AbstractPersistenceTests methods -------------------------------

    protected void initCache()
            throws Exception
        {
        m_cache = getNamedCache(ENTITY_PERSION_ID_EMBEDDED);
        }

    protected void initDomainClassPolicy()
        {
        m_policy = new DomainClassPolicy.CompoundPerson2Class();
        }


    // ----- constants ------------------------------------------------------

    public static String CONFIG_FILE_NAME = "jpa-cache-config.xml";
    }