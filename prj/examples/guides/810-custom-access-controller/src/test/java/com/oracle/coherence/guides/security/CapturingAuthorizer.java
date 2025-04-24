/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.guides.security;

import com.oracle.coherence.common.base.Logger;
import com.tangosol.net.BackingMapContext;
import com.tangosol.net.security.StorageAccessAuthorizer;
import com.tangosol.util.Binary;
import com.tangosol.util.BinaryEntry;

import javax.security.auth.Subject;
import java.security.Principal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * A {@link StorageAccessAuthorizer} that is used in tests to
 * capture the Subject that invoked an operation on a key.
 * This subject can then be checked to assert that the expected
 * subject was used to invoke a cache request.
 */
@SuppressWarnings("rawtypes")
public class CapturingAuthorizer
        implements StorageAccessAuthorizer
    {
    @Override
    public void checkRead(BinaryEntry entry, Subject subject, int nReason)
        {
        s_map.put(entry.getBinaryKey(), subject);
        Logger.info("CapturingAuthorizer: checkRead for principals " +
                subject.getPrincipals().stream().map(Principal::getName).collect(Collectors.joining(",")));
        }

    @Override
    public void checkWrite(BinaryEntry entry, Subject subject, int nReason)
        {
        Logger.info("CapturingAuthorizer: checkWrite for principals " +
                subject.getPrincipals().stream().map(Principal::getName).collect(Collectors.joining(",")));
        s_map.put(entry.getBinaryKey(), subject);
        }

    @Override
    public void checkReadAny(BackingMapContext context, Subject subject, int nReason)
        {
        }

    @Override
    public void checkWriteAny(BackingMapContext context, Subject subject, int nReason)
        {
        }

    /**
     * Return the {@link Subject} that last invoked a request
     * against a specific {@link Binary} cache key.
     *
     * @param binary  the {@link Binary} key of the cache
     *
     * @return the {@link Subject} that last invoked a request on a key
     */
    public static Subject getSubject(Binary binary)
        {
        return s_map.get(binary);
        }

    // ----- data members ---------------------------------------------------

    /**
     * The map of {@link Binary} key to requestor {@link Subject}.
     */
    private static final Map<Binary, Subject> s_map = new ConcurrentHashMap<>();
    }
