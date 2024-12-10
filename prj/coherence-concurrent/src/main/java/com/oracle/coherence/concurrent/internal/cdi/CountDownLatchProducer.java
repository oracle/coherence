/*
 * Copyright (c) 2021, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.internal.cdi;

import com.oracle.coherence.cdi.Name;
import com.oracle.coherence.cdi.Remote;

import com.oracle.coherence.concurrent.LocalCountDownLatch;
import com.oracle.coherence.concurrent.RemoteCountDownLatch;
import com.oracle.coherence.concurrent.CountDownLatch;
import com.oracle.coherence.concurrent.Latches;
import com.oracle.coherence.concurrent.cdi.Count;

import java.lang.reflect.Member;

import javax.enterprise.context.ApplicationScoped;

import javax.enterprise.inject.Produces;
import javax.enterprise.inject.Typed;
import javax.enterprise.inject.spi.DefinitionException;
import javax.enterprise.inject.spi.InjectionPoint;

/**
 * CDI producers for {@link LocalCountDownLatch} and {@link RemoteCountDownLatch} instances.
 *
 * @author Luk Ho  2021.12.01
 * @since 21.12
 */
@ApplicationScoped
public class CountDownLatchProducer
    {
    /**
     * Returns either a local or remote {@link CountDownLatch} for the provided {@link InjectionPoint}.
     * <p>
     * If the injection point is annotated with the {@link Remote} qualifier a remote
     * {@link CountDownLatch} will be returned, otherwise a local {@link CountDownLatch}
     * will be returned.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a local or remote {@link CountDownLatch} for the provided {@link InjectionPoint}
     */
    @Produces
    @Name("")
    @Count
    @Remote
    CountDownLatch getCountDownLatch(InjectionPoint ip)
        {
        if (ip.getQualifiers().contains(Remote.Literal.INSTANCE))
            {
            return getRemoteCountDownLatch(ip);
            }
        return getLocalCountDownLatch(ip);
        }

    /**
     * Returns an {@link LocalCountDownLatch} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a {@link LocalCountDownLatch} for the provided {@link InjectionPoint}
     */
    @Produces
    LocalCountDownLatch getUnqualifiedLocalCountDownLatch(InjectionPoint ip)
        {
        return getLocalCountDownLatch(ip);
        }

    /**
     * Returns a {@link LocalCountDownLatch} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a {@link LocalCountDownLatch} for the provided {@link InjectionPoint}
     */
    @Produces
    @Name("")
    @Count
    @Typed(LocalCountDownLatch.class)
    LocalCountDownLatch getLocalCountDownLatch(InjectionPoint ip)
        {
        return Latches.localCountDownLatch(getName(ip), getCount(ip));
        }

    /**
     * Returns an {@link RemoteCountDownLatch} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a {@link RemoteCountDownLatch} for the provided {@link InjectionPoint}
     */
    @Produces
    @Typed(RemoteCountDownLatch.class)
    RemoteCountDownLatch getUnqualifiedRemoteCountDownLatch(InjectionPoint ip)
        {
        return getRemoteCountDownLatch(ip);
        }

    /**
     * Returns a {@link RemoteCountDownLatch} for the provided {@link InjectionPoint}.
     *
     * @param ip  the CDI {@link InjectionPoint}
     *
     * @return a {@link RemoteCountDownLatch} for the provided {@link InjectionPoint}
     */
    @Produces
    @Name("")
    @Count
    @Typed(RemoteCountDownLatch.class)
    RemoteCountDownLatch getRemoteCountDownLatch(InjectionPoint ip)
        {
        return Latches.remoteCountDownLatch(getName(ip), getCount(ip));
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
                String sMsg = "Cannot determine the name of the latch. No @Name"
                              + " qualifier and injection point member is null";
                throw new DefinitionException(sMsg);
                }
            sName = member.getName();
            }

        return sName;
        }

    /**
     * Determine the initial count of the semaphore from a {@link Count}
     * annotation.
     *
     * @param ip  the injection point
     *
     * @return the count of the semaphore to inject
     */
    protected int getCount(InjectionPoint ip)
        {
        return ip.getQualifiers().stream()
                .filter(a -> Count.class.equals(a.annotationType()))
                .findFirst()
                .map(a -> ((Count) a).value())
                .orElse(1);
        }
    }
