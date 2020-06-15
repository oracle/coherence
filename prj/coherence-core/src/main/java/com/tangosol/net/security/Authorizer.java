/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.security;


import com.tangosol.net.ClusterPermission;

import javax.security.auth.Subject;

/**
 * This interface represents an environment-specific facility for authorizing callers to perform actions
 * described by the corresponding permission objects. Such authorization executes before or in place of the
 * Coherence specific JAAS based security checks (see AccessController). Moreover, if Coherence authorization
 * is configured, it must use the Subject object returned by the {@link #authorize} method to perform its
 * own authorization.
 *
 * @author dag  2012.03.07
 * @since Coherence 12.1.2
 */
public interface Authorizer
    {
    /**
     * Authorize the caller to perform the action specified by the permission.
     *
     * @param subject     the current caller's Subject if known (may be null)
     * @param permission  the permission that represents the targets and the action
     *                    to be performed against the targets
     *
     * @return the Subject representing the caller.
     *
     * @throws SecurityException if the caller's identity cannot established or they
     *         lack permission to execute the requested action
     */
    Subject authorize(Subject subject, ClusterPermission permission);
    }
