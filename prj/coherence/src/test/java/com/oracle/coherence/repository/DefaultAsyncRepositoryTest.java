/*
 * Copyright (c) 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.repository;

import com.tangosol.net.AsyncNamedMap;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedMap;

import com.tangosol.net.cache.TypeAssertion;

import data.repository.Person;

import org.junit.AfterClass;
import org.junit.BeforeClass;

/**
 * Test {@link AbstractAsyncRepository}.
 *
 * @since 21.06
 */
public class DefaultAsyncRepositoryTest
        extends AbstractAsyncRepositoryTest
    {
    @BeforeClass
    public static void _before()
        {
        System.setProperty("coherence.distributed.localstorage", "true");
        NamedMap<String, Person> cache = CacheFactory.getCache("people",
                TypeAssertion.withTypes(String.class, Person.class));
        s_personAsyncNamedMap = cache.async();
        s_personRepo = new AsyncPeopleRepository(s_personAsyncNamedMap);
        }

    @AfterClass
    public static void _after()
        {
        CacheFactory.shutdown();
        System.getProperties().remove("coherence.distributed.localstorage");
        }

    protected AsyncNamedMap<String, Person> getMap()
        {
        return s_personAsyncNamedMap;
        }

    protected AbstractAsyncRepository<String, Person> people()
        {
        return s_personRepo;
        }

    private static AsyncNamedMap<String, Person> s_personAsyncNamedMap;

    private static AbstractAsyncRepository<String, Person> s_personRepo;
    }
