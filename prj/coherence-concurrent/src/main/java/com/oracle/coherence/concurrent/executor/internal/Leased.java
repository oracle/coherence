/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.concurrent.executor.internal;

import com.tangosol.util.InvocableMap;

/**
 * A {@link Leased} object is one that supports tracking of last use and automated
 * cleanup upon expiry.
 *
 * @author bo
 * @since 21.12
 */
public interface Leased
    {
    /**
     * Renews the lease for some period of time.
     */
    void renew();

    /**
     * Obtains the time the lease will expiry (in milliseconds since the epoc).
     *
     * @return the time in milliseconds since the epoc
     */
    long getLeaseExpiryTime();

    /**
     * A callback invoked as part of an {@link InvocableMap.EntryProcessor} to perform
     * custom lease expiry operations when a lease has expired.
     *
     * @return <code>true</code> if the {@link Leased} state was mutated and should
     *         be saved, <code>false</code> otherwise
     */
    boolean onLeaseExpiry();
    }
