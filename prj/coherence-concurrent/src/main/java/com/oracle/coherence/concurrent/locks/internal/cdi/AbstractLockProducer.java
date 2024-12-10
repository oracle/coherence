/*
 * Copyright (c) 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.locks.internal.cdi;

import com.oracle.coherence.cdi.Name;

import java.lang.reflect.Member;

import javax.enterprise.inject.spi.DefinitionException;
import javax.enterprise.inject.spi.InjectionPoint;

/**
 * Abstract base class for CDI producers of locks.
 *
 * @author Aleks Seovic  2021.11.18
 * @since 21.12
 */
class AbstractLockProducer
    {
    // ----- helper methods -------------------------------------------------

    /**
     * Determine the name of the atomic value from a {@link Name} annotation
     * or member name.
     *
     * @param ip  the injection point
     *
     * @return the name of the atomic value to inject
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
                String sMsg = "Cannot determine the name of the lock. No @Name"
                              + " qualifier and injection point member is null";
                throw new DefinitionException(sMsg);
                }
            sName = member.getName();
            }

        return sName;
        }
    }
