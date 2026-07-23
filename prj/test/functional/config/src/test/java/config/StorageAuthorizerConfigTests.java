/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package config;

import com.tangosol.coherence.config.builder.ParameterizedBuilder;
import com.tangosol.coherence.config.builder.ParameterizedBuilderRegistry;

import com.tangosol.net.BackingMapContext;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.OperationalContext;
import com.tangosol.net.security.AuditingAuthorizer;
import com.tangosol.net.security.StorageAccessAuthorizer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import common.SystemPropertyResource;

import java.lang.reflect.Proxy;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import static org.junit.Assert.fail;

/**
 * Operational storage-authorizer alias tests.
 *
 * @author OpenAI  2026.05.16
 */
public class StorageAuthorizerConfigTests
    {
    @Before
    public void setup()
        {
        CacheFactory.shutdown();
        }

    @After
    public void cleanup()
        {
        CacheFactory.shutdown();
        }

    @Test
    public void shouldRealizeAuditingAliases()
        {
        try (SystemPropertyResource ignored = new SystemPropertyResource("coherence.cluster",
                "StorageAuthorizerConfigTests-" + System.currentTimeMillis()))
            {
            ParameterizedBuilderRegistry registry = ((OperationalContext) CacheFactory.getCluster()).getBuilderRegistry();

            StorageAccessAuthorizer auditing = realize(registry, "auditing");
            StorageAccessAuthorizer strict   = realize(registry, "strict-auditing");

            assertThat(auditing, is(instanceOf(AuditingAuthorizer.class)));
            assertThat(strict, is(instanceOf(AuditingAuthorizer.class)));

            BackingMapContext context = newBackingMapContext();

            auditing.checkWriteAny(context, null, StorageAccessAuthorizer.REASON_PUT);
            try
                {
                strict.checkWriteAny(context, null, StorageAccessAuthorizer.REASON_PUT);
                fail("strict-auditing should reject a null subject");
                }
            catch (SecurityException expected)
                {
                // expected
                }
            }
        }

    protected StorageAccessAuthorizer realize(ParameterizedBuilderRegistry registry, String sName)
        {
        ParameterizedBuilder<StorageAccessAuthorizer> builder =
                registry.getBuilder(StorageAccessAuthorizer.class, sName);

        assertThat(builder, is(notNullValue()));
        return builder.realize(null, null, null);
        }

    protected BackingMapContext newBackingMapContext()
        {
        return (BackingMapContext) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class[] {BackingMapContext.class},
                (proxy, method, args) -> "getCacheName".equals(method.getName()) ? "test" : null);
        }
    }
