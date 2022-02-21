/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.management;

import com.tangosol.internal.http.HttpRequest;
import com.tangosol.internal.http.RequestRouter;
import com.tangosol.internal.http.Response;

import com.tangosol.internal.management.resources.ClusterResource;
import com.tangosol.internal.management.resources.VersionsResource;
import com.tangosol.net.CacheFactory;

import com.tangosol.net.management.MBeanServerProxy;

import com.tangosol.util.Filter;
import com.tangosol.util.RegistrationBehavior;
import com.tangosol.util.ResourceRegistry;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import static com.tangosol.internal.management.resources.AbstractManagementResource.CLUSTER_NAME;
import static com.tangosol.internal.management.resources.AbstractManagementResource.DOMAIN_FILTER;
import static com.tangosol.util.BuilderHelper.using;

/**
 * A {@link RequestRouter} to route Coherence management over REST requests.
 *
 * @author Jonathan Knight  2022.01.25
 * @since 22.06
 */
public class ManagementRoutes
        extends RequestRouter
        implements RequestRouter.RequestPreprocessor
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a management over REST request router.
     */
    public ManagementRoutes()
        {
        this(CLUSTER, false, Collections::emptySet, "/mgmt/coherence", "/management/coherence");
        }

    /**
     * Create a management over REST request router.
     */
    public ManagementRoutes(String sPath, String... roots)
        {
        this(sPath, false, Collections::emptySet, roots);
        }

    /**
     * Create a management over REST request router.
     *
     * @param sPath       the root path to the resource
     * @param fVersioned  {@code true} if the root resource should be the {@link VersionsResource}
     * @param supplier    a {@link Supplier} of a {@link Set} of cluster names
     * @param roots       one or more resource roots
     *
     */
    public ManagementRoutes(String sPath, boolean fVersioned, Supplier<Set<String>> supplier, String... roots)
        {
        super(roots);

        setDefaultProduces(APPLICATION_JSON);
        setDefaultConsumes(APPLICATION_JSON);

        addRequestPreprocessor(this);

        addDefaultResponseHeader("X-Content-Type-Options", "nosniff");
        addDefaultResponseHeader("Content-type", APPLICATION_JSON);
        addDefaultResponseHeader("Vary", "Accept-Encoding");

        if (fVersioned)
            {
            addRoutes(sPath, new VersionsResource(supplier));
            }
        else
            {
            addRoutes(sPath, new ClusterResource());
            }
        }

    // ----- accessors ------------------------------------------------------

    /**
     * Set the cluster name to use for all requests.
     *
     * @param sClusterName  the cluster name to use for all requests
     */
    public void setClusterName(String sClusterName)
        {
        m_sClusterName = sClusterName;
        }

    public void setDomainPartitionFilter(Filter<String> domainPartitionFilter)
        {
        m_domainPartitionFilter = domainPartitionFilter;
        }

    // ----- RequestRouter.RequestHandlerPreprocessor methods ---------------

    @Override
    public Optional<Response> process(HttpRequest request)
        {
        ResourceRegistry registry = request.getResourceRegistry();
        MBeanServerProxy proxy    = CacheFactory.getCluster().getManagement().getMBeanServerProxy();

        registry.registerResource(MBeanServerProxy.class, MBeanServerProxy.class.getName(), using(proxy), RegistrationBehavior.IGNORE, null);
        if (m_sClusterName != null)
            {
            registry.registerResource(String.class, CLUSTER_NAME, m_sClusterName);
            }
        if (m_domainPartitionFilter != null)
            {
            registry.registerResource(Filter.class, DOMAIN_FILTER, m_domainPartitionFilter);
            }
        return Optional.empty();
        }

    // ----- constants ------------------------------------------------------

    /**
     * A {@code String} constant representing the json media type.
     */
    public final static String APPLICATION_JSON = "application/json";

    /**
     * The cluster resource path.
     */
    public static final String CLUSTER = "/cluster";

    // ----- data members ---------------------------------------------------

    /**
     * An optional cluster name for all requests.
     */
    private String m_sClusterName;

    /**
     * An optional domain partition filter.
     */
    private Filter<String> m_domainPartitionFilter;
    }
