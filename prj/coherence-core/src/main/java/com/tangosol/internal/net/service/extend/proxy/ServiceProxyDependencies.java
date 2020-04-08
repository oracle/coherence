/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service.extend.proxy;

import com.tangosol.coherence.config.builder.ParameterizedBuilder;
import com.tangosol.net.Service;
import com.tangosol.run.xml.XmlElement;

/**
 * The ServiceProxyDependencies interface provides a ServiceProxy object with its external
 * dependencies.
 *
 * @author pfm  2011.07.25
 * @since Coherence 12.1.2
 */
public interface ServiceProxyDependencies
        extends ProxyDependencies
    {
    /**
     * Return the XML that specifies the pluggable Service class-name and init-params.
     *
     * @return xml for CacheService class
     */
    public XmlElement getServiceClassConfig();

    /**
     * Return the custom proxy Service builder.
     *
     * @return  the custom proxy Service builder
     */
    public ParameterizedBuilder<? extends Service> getServiceBuilder();
    }
