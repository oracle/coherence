/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package repository;

import com.oracle.bedrock.junit.CoherenceClusterResource;
import com.oracle.bedrock.junit.SessionBuilder;
import com.oracle.bedrock.junit.SessionBuilders;

import com.oracle.bedrock.runtime.LocalPlatform;
import com.oracle.bedrock.runtime.coherence.options.LocalHost;
import com.oracle.bedrock.runtime.coherence.options.LocalStorage;
import com.oracle.bedrock.runtime.java.options.SystemProperty;

import com.oracle.coherence.repository.AbstractRepository;

import com.tangosol.coherence.component.util.SafeNamedCache;

import com.tangosol.coherence.config.Config;

import com.tangosol.internal.util.invoke.Lambdas;

import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.NamedMap;

import com.tangosol.util.WrapperException;

import data.repository.Person;

import java.io.NotSerializableException;
import java.util.Arrays;
import java.util.Collection;

import java.util.Optional;

import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Integration tests for {@link AbstractRepository}.
 *
 * @author Aleks Seovic  2021.02.24
 */
@RunWith(Parameterized.class)
public class RepositoryTests
        extends AbstractRepositoryTests
    {
    @ClassRule
    public static CoherenceClusterResource cluster = new CoherenceClusterResource()
            .include(2, LocalStorage.enabled())
            .with(SystemProperty.of("coherence.nameservice.address", LocalPlatform.get().getLoopbackAddress().getHostAddress()),
                  LocalHost.only(),
                  SystemProperty.of("coherence.extend.enabled", "true"),
                  SystemProperty.of("coherence.clusterport", "7574"),
                  SystemProperty.of(Lambdas.LAMBDAS_SERIALIZATION_MODE_PROPERTY,
                                    Config.getProperty(Lambdas.LAMBDAS_SERIALIZATION_MODE_PROPERTY)));

    public static SessionBuilder MEMBER = SessionBuilders.storageDisabledMember();
    public static SessionBuilder EXTEND = SessionBuilders.extendClient("client-cache-config.xml");

    @Parameterized.Parameters(name = "client={0}, serializer={1}")
    public static Collection<Object[]> parameters()
        {
        return Arrays.asList(new Object[][]{
                {MEMBER, "pof"}, {MEMBER, "java"},
                {EXTEND, "pof"}, {EXTEND, "java"}
            });
        }

    public RepositoryTests(SessionBuilder bldrSession, String sSerializer)
        {
        ConfigurableCacheFactory cacheFactory = cluster.createSession(bldrSession);

        m_map = cacheFactory.ensureCache(sSerializer, null);
        m_people = new PeopleRepository(m_map);
        }

    @Test
    public void testGroupByCollectorFiltered() throws Throwable
        {
        // NOTE: this test is failing when using Java serialization over Extend
        //       because the Optional is not Serializable. There is no way to fix
        //       that because we don't control the serialization of a Map
        //       containing the Optional, so we'll ignore the failure in that one
        //       case and make sure that the test passes in all other cases.
        try
            {
            super.testGroupByCollectorFiltered();
            }
        catch (Throwable e)
            {
            if (e instanceof WrapperException)
                {
                e = e.getCause();
                }
            if (e instanceof NotSerializableException &&
                Optional.class.getName().equals(e.getMessage()) &&
                "java".equals(m_map.getName()) && m_map instanceof SafeNamedCache)
                {
                return;
                }
            throw e;
            }
        }

    protected NamedMap<String, Person> getMap()
        {
        return m_map;
        }

    protected AbstractRepository<String, Person> people()
        {
        return m_people;
        }

    private final NamedMap<String, Person> m_map;
    private final AbstractRepository<String, Person> m_people;
    }
