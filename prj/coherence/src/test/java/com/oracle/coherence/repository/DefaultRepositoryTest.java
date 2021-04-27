/*
 * Copyright (c) 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.repository;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedMap;

import com.tangosol.net.cache.TypeAssertion;
import com.tangosol.net.cache.WrapperNamedCache;

import data.repository.Person;

import org.junit.AfterClass;
import org.junit.BeforeClass;


/**
 * Simple, in-memory only repository tests against a {@link WrapperNamedCache}.
 *
 * @author Aleks Seovic  2021.02.23
 */
public class DefaultRepositoryTest
        extends AbstractRepositoryTest
    {
    @BeforeClass
    public static void _before()
        {
        System.setProperty("coherence.distributed.localstorage", "true");
        s_personNamedMap = CacheFactory.getCache("people", TypeAssertion.withTypes(String.class, Person.class));
        s_personRepo = new PeopleRepository(s_personNamedMap);
        }

    @AfterClass
    public static void _after()
        {
        CacheFactory.shutdown();
        System.getProperties().remove("coherence.distributed.localstorage");
        }

    protected NamedMap<String, Person> getMap()
        {
        return s_personNamedMap;
        }

    protected AbstractRepository<String, Person> people()
        {
        return s_personRepo;
        }

    private static NamedMap<String, Person> s_personNamedMap;

    private static AbstractRepository<String, Person> s_personRepo;
    }
