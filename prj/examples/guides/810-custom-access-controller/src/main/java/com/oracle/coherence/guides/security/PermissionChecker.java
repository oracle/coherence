/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.guides.security;

import com.tangosol.net.ClusterPermission;

import javax.security.auth.Subject;
import java.security.AccessControlException;

/**
 * A permissions checker that verifies a principal is authorized for
 * a requested permission.
 *
 * @author Jonathan Knight 2025.04.11
 */
public interface PermissionChecker
    {
    /**
     * Determine whether the cluster access request indicated by the
     * specified permission should be allowed or denied for a given
     * Subject (requestor).
     * <p>
     * This method should quietly return if the access request is permitted,
     * or throw a suitable AccessControlException if the specified
     * authentication is invalid or insufficient.
     *
     * @param permission  the permission object that represents access
     *                    to a clustered resource
     * @param subject     the Subject object representing the requestor
     *
     * @throws AccessControlException if the specified permission
     *         is not permitted, based on the current security policy
     */
    void checkPermission(ClusterPermission permission, Subject subject);
    }
