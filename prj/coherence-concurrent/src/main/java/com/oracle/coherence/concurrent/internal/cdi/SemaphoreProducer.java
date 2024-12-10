/*
 * Copyright (c) 2021, 2024 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.internal.cdi;

import com.oracle.coherence.cdi.Name;

import com.oracle.coherence.cdi.Remote;

import com.oracle.coherence.concurrent.Semaphore;
import com.oracle.coherence.concurrent.LocalSemaphore;
import com.oracle.coherence.concurrent.RemoteSemaphore;
import com.oracle.coherence.concurrent.Semaphores;

import com.oracle.coherence.concurrent.cdi.Permits;

import java.lang.reflect.Member;

import javax.enterprise.context.ApplicationScoped;

import javax.enterprise.inject.Produces;
import javax.enterprise.inject.Typed;
import javax.enterprise.inject.spi.DefinitionException;
import javax.enterprise.inject.spi.InjectionPoint;

/**
 * CDI producer for {@link LocalSemaphore} and {@link RemoteSemaphore} instances.
 *
 * @author Vaso Putica  2021.12.01
 * @since 21.12
 */
@ApplicationScoped
public class SemaphoreProducer
    {
    /**
     * Returns either a local or remote {@link Semaphore} for the provided {@link InjectionPoint}.
     * <p>
     * If the injection point is annotated with the {@link Remote} qualifier a remote
     * {@link Semaphore} will be returned, otherwise a local {@link Semaphore}
     * will be returned.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a local or remote {@link Semaphore} for the provided {@link InjectionPoint}
     */
    @Produces
    @Name("")
    @Permits
    @Remote
    Semaphore getSemaphore(InjectionPoint ip)
        {
        if (ip.getQualifiers().contains(Remote.Literal.INSTANCE))
            {
            return getRemoteSemaphore(ip);
            }
        return getLocalSemaphore(ip);
        }

    /**
     * Returns an {@link LocalSemaphore} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a {@link LocalSemaphore} for the provided {@link InjectionPoint}
     */
    @Produces
    LocalSemaphore getUnqualifiedLocalSemaphore(InjectionPoint ip)
        {
        return getLocalSemaphore(ip);
        }

    /**
     * Returns a {@link LocalSemaphore} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a {@link LocalSemaphore} for the provided {@link InjectionPoint}
     */
    @Produces
    @Name("")
    @Permits
    @Typed(LocalSemaphore.class)
    LocalSemaphore getLocalSemaphore(InjectionPoint ip)
        {
        return Semaphores.localSemaphore(getName(ip), getPermits(ip));
        }

    /**
     * Returns an {@link RemoteSemaphore} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a {@link RemoteSemaphore} for the provided {@link InjectionPoint}
     */
    @Produces
    @Typed(RemoteSemaphore.class)
    RemoteSemaphore getUnqualifiedRemoteSemaphore(InjectionPoint ip)
        {
        return getRemoteSemaphore(ip);
        }

    /**
     * Returns a {@link RemoteSemaphore} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a {@link RemoteSemaphore} for the provided {@link InjectionPoint}
     */
    @Produces
    @Name("")
    @Permits
    @Typed(RemoteSemaphore.class)
    RemoteSemaphore getRemoteSemaphore(InjectionPoint ip)
        {
        return Semaphores.remoteSemaphore(getName(ip), getPermits(ip));
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Determine the name of the semaphore from a {@link Name} annotation
     * or member name.
     *
     * @param ip  the injection point
     *
     * @return the name of the semaphore to inject
     */
    protected String getName(InjectionPoint ip)
        {
        String sName = ip.getQualifiers().stream()
                .filter(a -> Name.class.equals(a.annotationType()))
                .findFirst()
                .map(a -> ((Name) a).value())
                .orElse(null);

        if (sName == null || sName.trim().isEmpty())
            {
            Member member = ip.getMember();
            if (member == null)
                {
                String sMsg = "Cannot determine the name of the semaphore. No @Name"
                              + " qualifier and injection point member is null";
                throw new DefinitionException(sMsg);
                }
            sName = member.getName();
            }

        return sName;
        }

    /**
     * Determine the number of permits from a {@link Permits} annotation.
     *
     * @param ip  the injection point
     *
     * @return the number of permits inject
     */
    protected int getPermits(InjectionPoint ip)
        {
        return ip.getQualifiers().stream()
                .filter(a -> Permits.class.equals(a.annotationType()))
                .findFirst()
                .map(a -> ((Permits) a).value())
                .orElse(0);
        }
    }
