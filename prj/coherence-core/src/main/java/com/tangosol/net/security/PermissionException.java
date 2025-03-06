/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.security;

import java.security.Permission;

public class PermissionException
        extends SecurityException
    {
    @java.io.Serial
    private static final long serialVersionUID = 5138225684096988535L;
    /**
     * The permission that caused the exception to be thrown.
     */
    private Permission perm;

    /**
     * Constructs an {@code AccessControlException} with the
     * specified, detailed message.
     *
     * @param s the detail message.
     */
    public PermissionException(String s)
        {
        super(s);
        }


    /**
     * Constructs an {@code AccessControlException} with the
     * specified, detailed message, and the requested permission that caused
     * the exception.
     *
     * @param s the detail message.
     * @param p the permission that caused the exception.
     */
    public PermissionException(String s, Permission p)
        {
        super(s);
        perm = p;
        }

    /**
     * Gets the {@code Permission} object associated with this exception, or
     * {@code null} if there was no corresponding {@code Permission} object.
     *
     * @return the Permission object.
     */
    public Permission getPermission()
        {
        return perm;
        }
    }
