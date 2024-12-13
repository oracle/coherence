/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.jpa.jpatest;

import com.oracle.coherence.jpa.JpaCacheLoader;
import com.oracle.coherence.jpa.JpaCacheStore;

import data.persistence.DomainClassPolicy;

import org.junit.Before;

public class JpaCacheStoreWithEmbeddedIdTest
        extends AbstractPersistenceTest
    {
    // ----- test lifecycle -------------------------------------------------

    @Before
    public void setUp()
            throws Exception
        {
        super.setUp();
        m_loader = new JpaCacheLoader(ENTITY_PERSION_ID_EMBEDDED,
                ENTITY_CLASS_NAME, PERSISTENCE_UNIT);
        m_store = new JpaCacheStore(ENTITY_PERSION_ID_EMBEDDED,
                ENTITY_CLASS_NAME, PERSISTENCE_UNIT);
        }


    // ----- helper methods -------------------------------------------------

    protected void initDomainClassPolicy()
        {
        m_policy = new DomainClassPolicy.CompoundPerson2Class();
        }


    // ----- constants ------------------------------------------------------

    public static String ENTITY_CLASS_NAME = "data.persistence.CompoundPerson2";
    }