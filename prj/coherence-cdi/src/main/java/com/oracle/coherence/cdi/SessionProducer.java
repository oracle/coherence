/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.cdi;

import com.oracle.coherence.inject.Name;
import com.oracle.coherence.inject.SessionName;
import com.tangosol.net.CacheFactoryBuilder;
import com.tangosol.net.Coherence;
import com.tangosol.net.Session;

import javax.enterprise.context.ApplicationScoped;

import javax.enterprise.inject.Produces;

import javax.enterprise.inject.spi.DefinitionException;
import javax.enterprise.inject.spi.InjectionPoint;

/**
 * A CDI producer for {@link Session} instances.
 *
 * @author Jonathan Knight  2019.11.06
 * @since 20.06
 */
@ApplicationScoped
public class SessionProducer
    {
    // ---- producer methods ------------------------------------------------

    /**
     * Produces the default {@link Session}.
     *
     * @param injectionPoint  the {@link InjectionPoint} that the cache factory
     *                        it to be injected into
     *
     * @return the default {@link Session}
     */
    @Produces
    public Session getDefaultSession(InjectionPoint injectionPoint)
        {
        return Coherence.getInstance(Coherence.DEFAULT_NAME).getSession(CacheFactoryBuilder.URI_DEFAULT);
        }

    /**
     * Produces a {@link Session} for a given scope.
     * <p>
     * If the value of the scope qualifier is blank or empty, the default
     * {@link Session} will be returned.
     *
     * @param injectionPoint  the {@link InjectionPoint} that the cache factory
     *                        it to be injected into
     *
     * @return the named {@link Session}
     */
    @Produces
    @SessionName
    public Session getSessionWithSessionName(InjectionPoint injectionPoint)
        {
        String sName = injectionPoint.getQualifiers()
                .stream()
                .filter(q -> q.annotationType().isAssignableFrom(SessionName.class))
                .map(q -> ((SessionName) q).value().trim())
                .findFirst()
                .orElse(Coherence.DEFAULT_NAME);

        return getSession(sName);
        }

    /**
     * Produces a {@link Session} for a given scope.
     * <p>
     * If the value of the scope qualifier is blank or empty, the default
     * {@link Session} will be returned.
     *
     * @param injectionPoint  the {@link InjectionPoint} that the cache factory
     *                        it to be injected into
     *
     * @return the named {@link Session}
     */
    @Produces
    @Name("")
    public Session getSessionWithName(InjectionPoint injectionPoint)
        {
        String sName = injectionPoint.getQualifiers()
                .stream()
                .filter(q -> q.annotationType().isAssignableFrom(Name.class))
                .map(q -> ((Name) q).value().trim())
                .findFirst()
                .orElse(Coherence.DEFAULT_NAME);

        return getSession(sName);
        }

    /**
     * Produces a {@link Session} for a given scope.
     * <p>
     * If the value of the scope qualifier is blank or empty, the default
     * {@link Session} will be returned.
     *
     * @return the named {@link Session}
     */
    private Session getSession(String sName)
        {
        String sSessionName;
        if (sName == null || sName.trim().isEmpty())
            {
            sSessionName = Coherence.DEFAULT_NAME;
            }
        else
            {
            sSessionName = sName;
            }

        return Coherence.findSession(sSessionName)
                .orElseThrow(() -> new DefinitionException("No Session is configured with name " + sSessionName));
        }
    }
