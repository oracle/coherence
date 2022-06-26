/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.bedrock.junit;

import com.tangosol.net.Coherence;

import com.tangosol.net.CoherenceConfiguration;
import com.tangosol.net.Session;
import com.tangosol.net.SessionConfiguration;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import java.util.concurrent.TimeUnit;

/**
 * A JUnit 5 extension that bootstraps a local Coherence  cluster member in the
 * {@link BeforeAllCallback#beforeAll(ExtensionContext)} method and shuts down
 * Coherence in the {@link AfterAllCallback#afterAll(ExtensionContext)} method.
 *
 * @author Jonathan Knight 2022.06.23
 * @since 22.06
 */
public class BootstrapCoherenceExtension
        implements BeforeAllCallback, AfterAllCallback
    {
    @Override
    @SuppressWarnings("resource")
    public void beforeAll(ExtensionContext context) throws Exception
        {
        Class<?>             clz                  = context.getTestClass().orElse(null);
        BootstrapCoherence   annotation           = clz == null ? null : clz.getAnnotation(BootstrapCoherence.class);
        SessionConfiguration sessionConfiguration = createSessionConfiguration(annotation);

        CoherenceConfiguration configuration = CoherenceConfiguration.builder()
                .withSession(sessionConfiguration)
                .build();

        int       cMillis   = annotation == null ? BootstrapCoherence.DEFAULT_TIMEOUT : annotation.timeout();
        Coherence coherence = Coherence.builder(configuration)
                .build()
                .start()
                .get(cMillis, TimeUnit.MILLISECONDS);

        if (clz != null)
            {
            Session session = coherence.getSession();
            for (Field field : clz.getDeclaredFields())
                {
                int nMods = field.getModifiers();

                BootstrapCoherence.Session fieldSession = field.getAnnotation(BootstrapCoherence.Session.class);
                if (fieldSession != null)
                    {
                    if (Modifier.isPublic(nMods) && Modifier.isStatic(nMods))
                        {
                        field.set(null, session);
                        }
                    else
                        {
                        throw new RuntimeException("Cannot inject Session into field " + field.getName() + " - must be public static");
                        }
                    }
                BootstrapCoherence.Cache fieldCache = field.getAnnotation(BootstrapCoherence.Cache.class);
                if (fieldCache != null)
                    {
                    String sName = fieldCache.name();
                    if (!sName.isBlank())
                        {
                        if (Modifier.isPublic(nMods) && Modifier.isStatic(nMods))
                            {
                            field.set(null, session.getCache(sName));
                            }
                        else
                            {
                            throw new RuntimeException("Cannot inject cache \"" + sName + "\" into field \"" + field.getName() + "\" - must be public static");
                            }
                        }
                    }
                }
            }
        }

    @Override
    public void afterAll(ExtensionContext context)
        {
        Coherence.closeAll();
        }

    private SessionConfiguration createSessionConfiguration(BootstrapCoherence annotation)
        {
        SessionConfiguration.Builder builder = SessionConfiguration.builder();

        if (annotation != null)
            {
            builder.withMode(annotation.mode());

            String sConfig = annotation.config();
            if (!sConfig.isBlank())
                {
                builder.withConfigUri(sConfig);
                }

            for (String sProperty : annotation.properties())
                {
                int nIndex = sProperty.indexOf('=');
                if (nIndex > 0)
                    {
                    String sKey = sProperty.substring(0, nIndex);
                    String sValue = sProperty.substring(nIndex + 1);
                    builder.withParameter(sKey, sValue);
                    }
                }
            }

        return builder.build();
        }
    }
