/*
 * Copyright (c) 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.repository;

import com.tangosol.net.NamedMap;

import com.tangosol.net.cache.WrapperNamedCache;

import data.repository.Person;

/**
 * Simple, in-memory only repository tests against a {@link WrapperNamedCache}.
 *
 * @author Aleks Seovic  2021.02.23
 */
public class DefaultRepositoryTest
        extends AbstractRepositoryTest
    {
    protected NamedMap<String, Person> getMap()
        {
        return MAP;
        }

    protected AbstractRepository<String, Person> people()
        {
        return REPOSITORY;
        }

    private static final NamedMap<String, Person> MAP = new WrapperNamedCache<>("people");
    private static final AbstractRepository<String, Person> REPOSITORY = new PeopleRepository(MAP);
    }
