/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config;

import com.tangosol.coherence.config.scheme.Scheme;
import com.tangosol.coherence.config.scheme.ServiceScheme;

import com.tangosol.util.UUID;

import java.util.Iterator;
import java.util.LinkedHashMap;

/**
 * A {@link ServiceSchemeRegistry} provides a mechanism manage a collection
 * of {@link ServiceScheme}s together with the ability to search the registry for
 * said {@link ServiceScheme}s, either by name or service name.
 * <p>
 * {@link ServiceSchemeRegistry}s are {@link Iterable}, the order of iteration
 * being the order in which the {@link ServiceScheme}s where added to the said
 * {@link ServiceSchemeRegistry}.
 *
 * @author bo  2012.05.02
 * @since Coherence 12.1.2
 */
public class ServiceSchemeRegistry
        implements Iterable<ServiceScheme>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a {@link ServiceSchemeRegistry}.
     */
    public ServiceSchemeRegistry()
        {
        m_mapServiceSchemesBySchemeName = new LinkedHashMap<String, ServiceScheme>();
        }

    // ----- Iterable interface ---------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<ServiceScheme> iterator()
        {
        return m_mapServiceSchemesBySchemeName.values().iterator();
        }

    // ----- SchemeRegistry methods -----------------------------------------

    /**
     * Attempts to register the specified {@link ServiceScheme}.
     *
     * @param scheme  the {@link ServiceScheme} to register
     *
     * @throws IllegalArgumentException if a {@link ServiceScheme} with the same
     *                                  scheme and/or service name has already
     *                                  been registered
     */
    public void register(ServiceScheme scheme)
        {
        if (scheme.isAnonymous())
            {
            // ensure that there is no other scheme with the same service name
            ServiceScheme schemeOther = findSchemeByServiceName(scheme.getServiceName());

            if (schemeOther != null)
                {
                throw new IllegalArgumentException(String.format(
                    "Attempted to register an anonymous service scheme with a <service-name>%s</service-name> that is already defined.",
                    scheme.getSchemeName()));
                }
            else
                {
                // register the scheme with an anonymous name.  no one will ever
                // try to look this up, but it needs a scheme name in the registry
                m_mapServiceSchemesBySchemeName.put("anonymous-" + scheme.getServiceName() + "-" + new UUID(), scheme);
                }
            }
        else if (m_mapServiceSchemesBySchemeName.containsKey(scheme.getSchemeName()))
            {
            throw new IllegalArgumentException(String.format(
                "Attempted to define multiple service schemes of <scheme-name>%s</scheme-name>.\n" +
                "Defined %s, Invalid duplicate %s.", scheme.getSchemeName(),
                describeServiceScheme(m_mapServiceSchemesBySchemeName.get(scheme.getSchemeName())),
                describeServiceScheme(scheme)));
            }
        else
            {
            m_mapServiceSchemesBySchemeName.put(scheme.getSchemeName(), scheme);
            }
        }

    /**
     * Attempts to locate a {@link ServiceScheme} registered with the specified
     * {@link ServiceScheme#getSchemeName()}.
     *
     * @param sSchemeName  the scheme of the {@link ServiceScheme} to find
     *
     * @return the registered {@link ServiceScheme} or
     *         <code>null</code> if not registered
     */
    public ServiceScheme findSchemeBySchemeName(String sSchemeName)
        {
        return m_mapServiceSchemesBySchemeName.get(sSchemeName);
        }

    /**
     * Attempts to locate a {@link ServiceScheme} registered with the specified
     * {@link ServiceScheme#getServiceName()} giving preference to "autostart"
     * schemes.
     *
     * @param sServiceName  the service name of {@link ServiceScheme} to find
     *
     * @return the registered {@link ServiceScheme} or
     *         <code>null</code> if not registered
     */
    public ServiceScheme findSchemeByServiceName(String sServiceName)
        {
        ServiceScheme schemeMatch = null;
        for (ServiceScheme scheme : this)
            {
            if (scheme.getServiceName().equals(sServiceName))
                {
                if (scheme.isAutoStart()) // see COH-15292
                    {
                    return scheme;
                    }
                schemeMatch = scheme;
                }
            }

        return schemeMatch;
        }

    /**
     * Determines the number of {@link Scheme}s registered with the
     * {@link ServiceSchemeRegistry}.
     *
     * @return the number of {@link Scheme}s
     */
    public int size()
        {
        return m_mapServiceSchemesBySchemeName.size();
        }

    // ----- helpers --------------------------------------------------------

    private static String describeServiceScheme(ServiceScheme scheme)
        {
        StringBuilder sb = new StringBuilder();

        if (scheme != null)
            {
            sb.append("ServiceScheme scheme-name=").append(scheme.getSchemeName());
            sb.append(" service-name=").append(scheme.getServiceName());
            sb.append(" service type=").append(scheme.getServiceType());
            }

        return sb.toString();
        }

    // ----- data members ---------------------------------------------------

    /**
     * The map of {@link ServiceScheme}s keyed by {@link ServiceScheme#getSchemeName()}.
     */
    private LinkedHashMap<String, ServiceScheme> m_mapServiceSchemesBySchemeName;
    }
