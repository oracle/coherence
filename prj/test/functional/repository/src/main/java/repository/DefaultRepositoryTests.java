/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package repository;

import com.oracle.coherence.repository.AbstractRepository;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.Coherence;
import com.tangosol.net.NamedMap;

import com.tangosol.net.cache.WrapperNamedCache;

import data.repository.Person;

import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;

/**
 * Simple, in-memory only repository tests against a {@link WrapperNamedCache}.
 *
 * @author Aleks Seovic  2021.02.23
 */
public class DefaultRepositoryTests
        extends AbstractRepositoryTests
    {
    @BeforeClass
    public static void _before() throws InterruptedException
        {
        Assume.assumeFalse(Boolean.getBoolean("coverage.enabled"));
        CacheFactory.shutdown();

        Thread.sleep(500);

        System.setProperty("coherence.distributed.localstorage", "true");
        System.setProperty("coherence.ttl", "0");
        System.setProperty("coherence.cluster", "CoherenceAsyncRepoTests");

        Coherence coherence = Coherence.clusterMember();
        coherence.start().join();
        s_personNamedMap = coherence.getSession().getMap("people");
        s_personRepo     = new PeopleRepository(s_personNamedMap);
        }

    @AfterClass
    public static void _after()
        {
        Coherence.closeAll();
        CacheFactory.getCacheFactoryBuilder().releaseAll(null);
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
