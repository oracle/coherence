/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.guides.security;

import com.tangosol.coherence.component.net.Security;

import com.tangosol.coherence.component.net.security.Standard;

import com.tangosol.net.ClusterPermission;
import com.tangosol.net.Service;

import com.tangosol.net.security.AccessController;
import com.tangosol.net.security.IdentityTransformer;
import com.tangosol.net.security.PermissionInfo;

import javax.security.auth.Subject;
import java.security.SignedObject;

/**
 * An {@link IdentityTransformer} implementation that uses certificate based
 * authentication.
 * <p>
 * The {@link Subject} will be used to create a {@link PermissionInfo} instance
 * that will be used as the token to send to the proxy server.
 * The {@link PermissionInfo} token when sent to the proxy will contain the subject's
 * principal, and any public credentials, it's private credentials are not sent.
 * The token also contains a {@link ClusterPermission} and an encrypted version of the
 * same permission that was signed with the Subject's private key.
 * <p>
 * The corresponding {@link CertIdentityAsserter} on the proxy will decrypt the signed
 * permission using the subject's public key. This will verify that the received token
 * was sent by a client that has both the private and public credentials for the subject.
 * <p>
 * The proxy will then verify the public key using its configured verification mechanism,
 * for example this might be verifying against a trust store.
 *
 * @author Jonathan Knight 2025.04.11
 */
public class CertIdentityTransformer
        implements IdentityTransformer
    {
    @Override
    public Object transformIdentity(Subject subject, Service service) throws SecurityException
        {
        try
            {
            Object oToken = null;
            if (Security.isSecurityEnabled())
                {
                Standard          security     = (Standard) Security.getInstance();
                AccessController  controller   = security.getDependencies().getAccessController();
                String            sServiceName = service.getInfo().getServiceName();
                String            sTarget      = "service=Proxy";
                ClusterPermission permission   = new ClusterPermission(null, sTarget, "join");
                SignedObject      signedObject = controller.encrypt(permission, subject);
                oToken = new PermissionInfo(permission, sServiceName, signedObject, subject);
                }
            return oToken;
            }
        catch (Exception e)
            {
            throw new SecurityException("Failed to create identity token", e);
            }
        }
    }
