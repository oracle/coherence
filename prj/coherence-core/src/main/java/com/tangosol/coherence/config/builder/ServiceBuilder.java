/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.builder;

import com.tangosol.config.expression.ParameterResolver;

import com.tangosol.net.Cluster;
import com.tangosol.net.Service;

import com.tangosol.run.xml.XmlElement;


/**
 * The ServiceBuilder interface is used by a builder that creates a Service.
 *
 * @author pfm  2011.12.14
 * @since Coherence 12.1.2
 */
public interface ServiceBuilder
    {
    /**
     * Return true if a running cluster is needed before using a service.
     *
     * @return {@code true} if a running cluster is needed before using a service
     */
    public boolean isRunningClusterNeeded();

    /**
     * Return the scope name.
     *
     * @return the scope name
     */
    public String getScopeName();

    /**
     * Return the XmlElement that may be used to realize a
     * Service by the ServiceBuilder.
     * <p>
     * Note: There's no guarantee an implementation of this interface
     * will use the returned XmlElement.
     *
     * @return the XmlElement
     */
    @Deprecated
    public XmlElement getXml();

    /**
     * Set the XmlElement that may be used to realize a Service.
     * <p>
     * Note: There's no guarantee an implementation of this interface
     * will use the specified XmlElement.
     *
     * @param element  the XmlElement
     */
    @Deprecated
    public void setXml(XmlElement element);

    /**
     * Realize (ensure) a Service. The returned Service is fully
     * configured and ready to be used.
     *
     * @param resolver  the ParameterResolver
     * @param loader    the ClassLoader
     * @param cluster   the Cluster which will already be running if necessary
     *
     * @return the Service
     */
    public Service realizeService(ParameterResolver resolver, ClassLoader loader, Cluster cluster);
    }
