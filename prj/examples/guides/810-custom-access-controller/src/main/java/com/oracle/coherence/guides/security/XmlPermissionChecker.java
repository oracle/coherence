/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.guides.security;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.net.ClusterPermission;

import com.tangosol.net.security.PermissionException;

import com.tangosol.run.xml.SimpleParser;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;

import com.tangosol.util.ClassHelper;

import javax.security.auth.Subject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import java.security.Permissions;
import java.security.Principal;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import java.util.concurrent.ConcurrentHashMap;

import java.util.stream.Collectors;

/**
 * A {@link PermissionChecker} implementation that works in the same way
 * that the Coherence {@link com.tangosol.net.security.DefaultController}
 * checks permissions.
 *
 * @author Jonathan Knight 2025.04.11
 */
public class XmlPermissionChecker
        implements PermissionChecker
    {
    /**
     * Construct {@link CertAccessController} for the specified key store file,
     * permissions description (XML) file, the audit flag, and key store password.
     *
     * @param filePermits  the permissions file
     * @param fAudit       the audit flag; if true, log all the access requests
     *
     * @throws IOException  if an I/O error occurs loading the permissions file
     */
    public XmlPermissionChecker(File filePermits, boolean fAudit) throws IOException
        {
        f_xmlPermits = loadPermissionsFile(filePermits);
        f_fAudit     = fAudit;
        }

    @Override
    public void checkPermission(ClusterPermission permission, Subject subject)
        {
        Set<Principal> setPrincipals = subject.getPrincipals();
        if (setPrincipals != null)
            {
            for (Principal principal : setPrincipals)
                {
                // get the existing permissions and check against them
                Permissions permits = getClusterPermissions(principal);
                if (permits != null && permits.implies(permission))
                    {
                    // permission granted
                    if (f_fAudit)
                        {
                        logPermissionRequest(permission, subject, true);
                        }
                    return;
                    }
                }
            }

        if (f_fAudit)
            {
            logPermissionRequest(permission, subject, false);
            }

        throw new PermissionException("Insufficient rights to perform the operation", permission);
        }

    private XmlElement loadPermissionsFile(File filePermits) throws IOException
        {
        if (filePermits == null)
            {
            throw new IOException("Permission file cannot be null");
            }
        if (!filePermits.exists() || !filePermits.canRead())
            {
            throw new IOException("Permission file is not accessible: " +
                    filePermits.getAbsolutePath());
            }
        try (FileInputStream inPermits = new FileInputStream(filePermits))
            {
            return new SimpleParser().parseXml(inPermits);
            }
        }

    /**
     * Obtain the permissions for the specified principal.
     *
     * @param principal  the Principal object
     *
     * @return an array of Permission objects for the specified principal or
     *         null if no such principal exists
     */
    private Permissions getClusterPermissions(Principal principal)
        {
        return f_mapPermission.computeIfAbsent(principal.getName(), name -> findClusterPermissions(principal));
        }

    /**
     * Obtain the permissions for the specified principal.
     *
     * @param principal  the Principal object
     *
     * @return an array of Permission objects for the specified principal or
     *         null if no such principal exists
     */
    @SuppressWarnings("unchecked")
    private Permissions findClusterPermissions(Principal principal)
        {
        XmlElement xmlName = XmlHelper.findElement(f_xmlPermits, "/grant/principal/name", principal.getName());
        if (xmlName == null)
            {
            return null;
            }

        XmlElement xmlPrincipal  = xmlName.getSafeElement("../");
        String     sPrincipalCls = xmlPrincipal.getSafeElement("class").getString();
        if (!sPrincipalCls.isEmpty())
            {
            // the class is specified; match the passed-in Principal
            if (!principal.getClass().getName().equals(sPrincipalCls))
                {
                return null;
                }
            }

        XmlElement  xmlGrant = xmlPrincipal.getSafeElement("../");
        Permissions permits  = new Permissions();

        for (Iterator<Object> iter = xmlGrant.getElements("permission"); iter.hasNext();)
            {
            XmlElement xmlPermission = (XmlElement) iter.next();
            String sClass  = xmlPermission.getSafeElement("class").getString("com.tangosol.net.ClusterPermission");
            String sTarget = xmlPermission.getSafeElement("target").getString();
            String sAction = xmlPermission.getSafeElement("action").getString();

            ClusterPermission permit;
            try
                {
                permit = (ClusterPermission) ClassHelper.newInstance(
                       Class.forName(sClass), new Object[] {sTarget, sAction});
                permits.add(permit);
                }
            catch (Throwable e)
                {
                // just log the error; try to find a valid permission anyway
                Logger.warn("Invalid permission element: " + xmlPermission + "\nreason: " + e);
                // continue
                }
            }
        return permits;
        }

    /**
     * Log the authorization request.
     *
     * @param permission  the permission checked
     * @param subject     the Subject
     * @param fAllowed    the boolean indicated whether it is allowed
     */
    void logPermissionRequest(
            ClusterPermission permission, Subject subject, boolean fAllowed)
        {
        Logger.info((fAllowed ? "Allowed" : "Denied")
            + " request for " + permission + " on behalf of "
            + subject.getPrincipals().stream().map(Principal::getName).collect(Collectors.joining(",")));
        }

    // ----- data members ---------------------------------------------------

    /**
     * Permissions configuration XML.
     */
    private final XmlElement f_xmlPermits;

    /**
     * The audit flag. If true, log all the access requests.
     */
    private final boolean f_fAudit;

    /**
     * A cache of principal name to permissions.
     */
    private final Map<String, Permissions> f_mapPermission = new ConcurrentHashMap<>();
    }
