/*
 * Copyright (c) 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.internal.cdi;

import com.oracle.coherence.cdi.Name;

import com.oracle.coherence.concurrent.cdi.Count;

import javax.enterprise.inject.spi.DefinitionException;
import javax.enterprise.inject.spi.InjectionPoint;

import java.lang.reflect.Member;

/**
 * Abstract base class for CDI producers of latches.
 *
 * @author lh  2021.11.29
 * @since 21.12
 */
class AbstractLatchProducer
    {
    // ----- helper methods -------------------------------------------------

    /**
     * Determine the name of the latch from a {@link Name} annotation
     * or member name.
     *
     * @param ip  the injection point
     *
     * @return the name of the latch to inject
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

    protected int getCount(InjectionPoint ip)
        {
        int nCount = ip.getQualifiers().stream()
                .filter(a -> Count.class.equals(a.annotationType()))
                .findFirst()
                .map(a -> ((Count) a).value())
                .orElse(0);

        if (nCount > 0)
            {
            return nCount;
            }

        String sMsg = "Cannot determine the count of the latch. No @Count qualifier";
        throw new DefinitionException(sMsg);
        }
    }
