/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.scheme;

import java.util.List;

import com.oracle.coherence.common.util.Options;

import com.tangosol.application.ContainerContext;

import com.tangosol.coherence.config.builder.NamedCollectionBuilder;
import com.tangosol.coherence.config.builder.NamedEventInterceptorBuilder;
import com.tangosol.coherence.config.builder.ServiceBuilder;

import com.tangosol.internal.net.service.grid.DefaultPartitionedServiceDependencies;

import com.tangosol.net.NamedCollection;

/**
 * This interface exposes service related scheme information.  Other schemes,
 * such as {@link CachingScheme}, extend this class to add support for caches
 * and maps.
 *
 * @author pfm  2011.12.30
 * @since Coherence 12.1.2
 */
public interface ServiceScheme
        extends Scheme
    {
    /**
     * Return true if the service has auto-start enabled.
     *
     * @return the auto-start flag.
     */
    public boolean isAutoStart();

    /**
     * Return the service name.
     *
     * @return the service name
     */
    public String getServiceName();

    /**
     * Return the service name with any scoping applied.  The scoped name in
     * general has the following format:
     * <pre>
     *   [&lt;domain-partition-name&gt;'/'] [&lt;application-scope&gt;':'] &lt;service-name&gt;
     * </pre>
     *
     * @return the scoped service name
     */
    public String getScopedServiceName();

    /**
     * Return a scoped service name to use as the string parameter for {@link DefaultPartitionedServiceDependencies#PROP_SERVICE_PARTITIONS PROP_SERVICE_PARTITIONS}.
     * <p>
     * When present, replace {@link #DELIM_DOMAIN_PARTITION} and {@link #DELIM_APPLICATION_SCOPE} in scoped service name with character period.
     * For example, scoped service name <code>tenant/appscope:PartitionedCache</code> is transformed to <code>tenant.appscope.PartitionedCache</code>.
     *
     * @return transformed scoped service name
     *
     * @since 24.09
     */
    default public String getScopedServiceNameForProperty()
        {
        return getScopedServiceName().replace(DELIM_DOMAIN_PARTITION.charAt(0), '.').replace(DELIM_APPLICATION_SCOPE.charAt(0), '.');
        }

    /**
     * Return the service type.
     *
     * @return the service type
     */
    public String getServiceType();

    /**
     * Return the {@link ServiceBuilder} that is needed to build a service.
     *
     * @return the {@link ServiceBuilder} or null if the scheme does not support
     *         services.
     */
    public ServiceBuilder getServiceBuilder();

    /**
     * Obtains the {@link List} of {@link NamedEventInterceptorBuilder}s that have been
     * defined for the {@link ServiceScheme}.
     * <p>
     * Note: For those {@link ServiceScheme}s don't support event interceptors,
     *       the returned value <strong>must be an empty list.</strong>
     *
     * @return an {@link List} over {@link NamedEventInterceptorBuilder}s
     */
    public List<NamedEventInterceptorBuilder> getEventInterceptorBuilders();

    @SuppressWarnings("rawtypes")
    default NamedCollectionBuilder getNamedCollectionBuilder(Class<? extends NamedCollection> clz, Options<NamedCollection.Option> options)
        {
        throw new UnsupportedOperationException();
        }

    /**
     * Return a scope name prefixed with any tenancy name.
     *
     * @param sScopeName  the scope name to prefix
     * @param context     an optional container name to use to obtain the domain partition
     *
     * @return a scope name with a domain partition prefix, or the plain scope
     *         name if there is no domain partition
     */
    static String getScopePrefix(String sScopeName, ContainerContext context)
        {
        if (sScopeName == null)
            {
            return null;
            }

        String sTenant = context == null || context.isGlobalDomainPartition() ? null : context.getDomainPartition();

        return sTenant == null || sTenant.trim().length() == 0
                ? sScopeName : sTenant + DELIM_DOMAIN_PARTITION + sScopeName;
        }

    /**
     * Return a scoped service name
     *
     * @param sScopeName    the optional scope name
     * @param sServiceName  the service name
     *
     * @return  the scoped service name
     */
    static String getScopedServiceName(String sScopeName, String sServiceName)
        {
        return sScopeName == null || sScopeName.trim().length() == 0
                ? sServiceName : sScopeName + DELIM_APPLICATION_SCOPE + sServiceName;
        }

    /**
     * Delimiter for the Domain Partition name in the {@link #getScopedServiceName()
     * scoped service name}
     */
    public final static String DELIM_DOMAIN_PARTITION = "/";

    /**
     * Delimiter for the Application Scope in the {@link #getScopedServiceName()
     * scoped service name}
     */
    public final static String DELIM_APPLICATION_SCOPE = ":";

    }
