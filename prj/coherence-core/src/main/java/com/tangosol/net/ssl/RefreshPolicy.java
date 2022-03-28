/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.ssl;

import com.oracle.coherence.common.net.SSLSocketProvider.Dependencies;
import com.tangosol.internal.net.ssl.KeyStoreListener;
import com.tangosol.internal.net.ssl.ManagerDependencies;

/**
 * Implemented by classes that wish to control whether an {@link javax.net.ssl.SSLContext}
 * is updated when its scheduled update check runs.
 *
 * @author Jonathan Knight  2020.01.25
 * @since 22.06
 */
public interface RefreshPolicy
        extends KeyStoreListener
    {
    /**
     * Return {@code true} if the keys and certs should be refreshed.
     *
     * @param deps          the {@link Dependencies socket provider dependencies}
     * @param depsIdMgr     the {@link ManagerDependencies identity manager dependencies} or
     *                      {@code null} if no identity manger has been configured
     * @param depsTrustMgr  the {@link ManagerDependencies trust manager dependencies} or
     *                      {@code null} if no trust manger has been configured
     *
     * @return  {@code true} if the keys and certs should be refreshed
     */
    boolean shouldRefresh(Dependencies deps, ManagerDependencies depsIdMgr, ManagerDependencies depsTrustMgr);

    /**
     * A {@link RefreshPolicy} that always returns {@code true}.
     */
    RefreshPolicy Always = (deps, depsIdMgr, depsTrustMgr) -> true;
    }
