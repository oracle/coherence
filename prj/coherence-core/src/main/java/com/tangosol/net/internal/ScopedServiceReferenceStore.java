/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.internal;

import com.tangosol.net.CacheService;
import com.tangosol.net.InvocationService;
import com.tangosol.net.Service;
import com.tangosol.net.security.Security;

import com.tangosol.util.ConcurrentMap;

/**
 * {@link ScopedServiceReferenceStore} holds scoped service references.
 * <p>
 * Service references are scoped, optionally, by Subject. Subject scoping is
 * handled automatically; ScopedServiceReferenceStore requires no explicit input about
 * Subjects from its clients. Subject scoping is configured in the operational
 * configuration and applies only to remote services.
 * <p>
 * Thead safety documented in {@link AbstractScopedReferenceStore}.
 *
 * @author jf 2015.06.22
 *
 * @since Coherence 12.2.1
 *
 * @see AbstractScopedReferenceStore
 */
public class ScopedServiceReferenceStore
        extends AbstractScopedReferenceStore
    {
    // ----- ScopedServiceReferenceStore methods ----------------------------------------------------------------------

    /**
     * Retrieve the Service reference based on the passed in service name.
     *
     * @param sServiceName  the service name
     *
     * @return the Service reference
     */
    public Service getService(String sServiceName)
        {
        Object oHolder = m_mapByName.get(sServiceName);

        if (oHolder == null || oHolder instanceof Service)
            {
            return (Service) oHolder;
            }
        else if (oHolder instanceof SubjectScopedReference)
            {
            return (Service) ((SubjectScopedReference) oHolder).get();
            }

        throw new UnsupportedOperationException();
        }

    /**
     * Store a service reference.
     * <p>
     * Service name and type are passed in rather than using service.getInfo()
     * because the service may not have been configured and started yet, so
     * the info may not be safely available.
     *
     * @param service  the referenced service
     * @param sName    the service name
     * @param sType    the service type
     */
    public void putService(Service service, String sName, String sType)
        {
        ConcurrentMap mapByName = m_mapByName;

        if (isRemoteServiceType(sType) && Security.SUBJECT_SCOPED)
            {
            SubjectScopedReference scopedRef = (SubjectScopedReference) mapByName.get(sName);

            if (scopedRef == null)
                {
                SubjectScopedReference refTmp = (SubjectScopedReference) m_mapByName.putIfAbsent(sName,
                                                    scopedRef = new SubjectScopedReference());

                scopedRef = refTmp == null ? scopedRef : refTmp;
                }

            scopedRef.set(service);
            }
        else
            {
            mapByName.put(sName, service);
            }
        }

    /**
     * Determine if the service type is remote.
     *
     * @param sType  the service type
     *
     * @return whether the service type is remote
     */
    public static boolean isRemoteServiceType(String sType)
        {
        return sType.equals(CacheService.TYPE_REMOTE) || sType.equals(InvocationService.TYPE_REMOTE);
        }
    }
