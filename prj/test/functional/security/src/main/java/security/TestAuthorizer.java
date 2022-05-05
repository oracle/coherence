/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package security;

import com.tangosol.net.ClusterPermission;

import com.tangosol.net.security.Authorizer;

import javax.security.auth.Subject;

/**
 * Test Authorizer.
 *
 * @author dag  2012.03.07
 */
public class TestAuthorizer implements Authorizer
    {

    // ----- Authorizer implementation --------------------------------------

    /**
     * Method description
     *
     * @param subject
     * @param permission
     *
     * @return
     */
    @Override
    public Subject authorize(Subject subject, ClusterPermission permission)
        {
        String sServiceName = permission.getServiceName();
        String sAction      = permission.getActions();

        if (sServiceName == null || sAction == null)
            {
            throw new IllegalArgumentException("service=" + sServiceName +" and/or action=" + sAction + " must not be null");
            }

        return null;
        }
    }
