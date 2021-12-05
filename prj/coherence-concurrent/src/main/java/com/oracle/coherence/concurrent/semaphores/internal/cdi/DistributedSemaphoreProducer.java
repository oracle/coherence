/*
 * Copyright (c) 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.semaphores.internal.cdi;

import com.oracle.coherence.cdi.Name;
import com.oracle.coherence.concurrent.semaphores.DistributedSemaphore;
import com.oracle.coherence.concurrent.semaphores.Semaphores;
import com.oracle.coherence.concurrent.semaphores.cdi.Permits;

import java.lang.reflect.Member;

import java.util.concurrent.Semaphore;

import javax.enterprise.context.ApplicationScoped;

import javax.enterprise.inject.Produces;
import javax.enterprise.inject.Typed;
import javax.enterprise.inject.spi.DefinitionException;
import javax.enterprise.inject.spi.InjectionPoint;

/**
 * CDI producer for {@link DistributedSemaphore}
 *
 * @author Vaso Putica  2021.12.01
 * @since 21.12
 */
@ApplicationScoped
public class DistributedSemaphoreProducer
    {
    /**
     * Returns a local {@link Semaphore} for the provided
     * {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a local {@link Semaphore} for the provided
     *         {@link InjectionPoint}
     */
    @Produces
    Semaphore getUnqualifiedSemaphore(InjectionPoint ip)
        {
        return getLocalSemaphore(ip);
        }

    /**
     * Returns a {@link DistributedSemaphore} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a {@link DistributedSemaphore} for the provided {@link InjectionPoint}
     */
    @Produces
    DistributedSemaphore getUnqualifiedDistributedSemaphore(InjectionPoint ip)
        {
        return getDistributedSemaphore(ip);
        }

    /**
     * Returns a {@link Semaphore} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a {@link Semaphore} for the provided {@link InjectionPoint}
     */
    @Produces
    @Name("")
    @Permits(0)
    @Typed(Semaphore.class)
    Semaphore getLocalSemaphore(InjectionPoint ip)
        {
        return Semaphores.localSemaphore(getName(ip), getPermits(ip));
        }

    /**
     * Returns a {@link DistributedSemaphore} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a {@link DistributedSemaphore} for the provided {@link InjectionPoint}
     */
    @Produces
    @Name("")
    @Permits(0)
    @Typed(DistributedSemaphore.class)
    DistributedSemaphore getDistributedSemaphore(InjectionPoint ip)
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

    protected int getPermits(InjectionPoint ip)
        {
        Integer nPermits = ip.getQualifiers().stream()
                .filter(a -> Permits.class.equals(a.annotationType()))
                .findFirst()
                .map(a -> ((Permits) a).value())
                .orElse(null);

        if (nPermits != null)
            {
            return nPermits;
            }

        String sMsg = "Cannot determine the permits count. No @Permits qualifier";
        throw new DefinitionException(sMsg);
        }
    }
