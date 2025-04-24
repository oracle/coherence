/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.guides.security;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.coherence.component.net.Security;

import com.tangosol.coherence.component.net.security.Standard;

import com.tangosol.net.ClusterPermission;
import com.tangosol.net.Service;

import com.tangosol.net.security.AccessController;
import com.tangosol.net.security.IdentityAsserter;
import com.tangosol.net.security.PermissionInfo;

import javax.security.auth.Subject;

import java.security.SignedObject;

import java.util.Set;

/**
 * An {@link IdentityAsserter} implementation that expects a {@link Subject}
 * to be sent by the Extend client as the token. The asserter then uses the
 * standard Coherence security permission check to verify the client.
 * </p>
 * The {@link AccessController} configured in the
 * security section of the operational configuration file will be used to verify
 * the subject. The subject principal must have the "join" permission for the
 * Extend proxy service name in the permissions file used by the access controller.
 *
 * @author Jonathan Knight 2025.04.11
 */
public class CertIdentityAsserter
        implements IdentityAsserter
    {
    @Override
    public Subject assertIdentity(Object oToken, Service service) throws SecurityException
        {
        if (!Security.isSecurityEnabled())
            {
            // security is not enabled, so we do not create a subject
            return null;
            }

        if (!(oToken instanceof PermissionInfo))
            {
            // invalid token type
            Logger.info("Failed to assert identity, incorrect token type: " + oToken);
            throw new SecurityException("Unauthorized");
            }

        PermissionInfo info         = (PermissionInfo) oToken;
        Subject        subject      = info.getSubject();
        SignedObject   signedObject = info.getSignedPermission();

        if (subject == null)
            {
            // missing subject
            Logger.info("Failed to assert identity, missing subject in token: " + oToken);
            throw new SecurityException("Unauthorized");
            }

        if (signedObject == null)
            {
            // missing subject
            Logger.info("Failed to assert identity, missing signed object in token: " + oToken);
            throw new SecurityException("Unauthorized");
            }

        try
            {
            Standard          security     = (Standard) Security.getInstance();
            AccessController  controller   = security.getDependencies().getAccessController();

            // verify the decrypted signed object, this will also verify trust of the public cert
            if (!(controller.decrypt(signedObject, subject, null) instanceof ClusterPermission))
                {
                Logger.info("Failed to assert identity, decrypted permission mismatch: " + oToken);
                throw new SecurityException("Unauthorized");
                }

            // Verify the Subject has permissions to connect
            String sClusterName = service.getCluster().getClusterName();
            String sServiceName = service.getInfo().getServiceName();
            String sTarget      = "service=" + sServiceName;
            controller.checkPermission(new ClusterPermission(sClusterName, sTarget, "join"), subject);

            // The subject we return must only have principals or it will break other Coherence permission checks
            return new Subject(false, subject.getPrincipals(), Set.of(), Set.of());
            }
        catch (Exception e)
            {
            // We log the exception here as  we do not include root cause exception in the
            // SecurityException we will throw here as it may expose internal information to
            // the caller
            Logger.err("Failed to create identity token", e);
            throw new SecurityException("Failed to verify identity token");
            }
        }
    }
