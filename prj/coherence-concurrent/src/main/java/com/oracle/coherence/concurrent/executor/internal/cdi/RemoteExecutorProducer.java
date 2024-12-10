/*
 * Copyright (c) 2021, 2024 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.executor.internal.cdi;

import com.oracle.coherence.cdi.Name;
import com.oracle.coherence.concurrent.executor.RemoteExecutor;

import java.lang.reflect.Member;

import javax.enterprise.context.ApplicationScoped;

import javax.enterprise.inject.Produces;
import javax.enterprise.inject.Typed;
import javax.enterprise.inject.spi.DefinitionException;
import javax.enterprise.inject.spi.InjectionPoint;

/**
 * CDI producers for {@link RemoteExecutor}
 *
 * @author Vaso Putica  2021.12.01
 * @since 21.12
 */
@ApplicationScoped
public class RemoteExecutorProducer
    {
    /**
     * Obtains a {@link RemoteExecutor} using no qualifiers.
     *
     * @param injectionPoint  the {@link InjectionPoint}
     *
     * @return the {@link RemoteExecutor} for the given {@link InjectionPoint}
     */
    @Produces
    public RemoteExecutor getUnqualifiedRemoteExecutor(InjectionPoint injectionPoint)
        {
        return getRemoteExecutor(injectionPoint);
        }

    /**
     * Obtains a {@link RemoteExecutor} using a {@code name} qualifier.
     *
     * @param injectionPoint  the {@link InjectionPoint}
     *
     * @return the {@link RemoteExecutor} for the given {@link InjectionPoint}
     */
    @Produces
    @Name("")
    @Typed(RemoteExecutor.class)
    public RemoteExecutor getRemoteExecutor(InjectionPoint injectionPoint)
        {
        return RemoteExecutor.get(getName(injectionPoint));
        }

    /**
     * Obtain the named qualifier for the given {@link InjectionPoint}.
     *
     * @param injectionPoint  the {@link InjectionPoint}
     *
     * @return the named qualifier for the given {@link InjectionPoint}
     */
    protected String getName(InjectionPoint injectionPoint)
        {
        String sName = injectionPoint.getQualifiers().stream()
                .filter(a -> Name.class.equals(a.annotationType()))
                .findFirst()
                .map(a -> ((Name) a).value())
                .orElse(null);

        if (sName == null || sName.trim().isEmpty())
            {
            Member member = injectionPoint.getMember();
            if (member == null)
                {
                String sMsg = "Cannot determine the name of the remote executor. No @Name"
                              + " qualifier and injection point member is null";
                throw new DefinitionException(sMsg);
                }
            sName = member.getName();
            }

        return sName;
        }
    }
