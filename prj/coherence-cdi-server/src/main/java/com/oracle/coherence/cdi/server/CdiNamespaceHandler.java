/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.cdi.server;

import com.tangosol.config.xml.AbstractNamespaceHandler;

/**
 * Custom namespace handler for {@code cdi} namespace.
 * <p>
 * This namespace handler supports only one XML element:
 * <ul>
 * <li>{@code &lt;cdi:bean>beanName&lt;/cdi:bean>}, where {@code beanName}
 * is the unique name of a CDI bean defined by the {@code @Named} annotation.
 * This element can only be used as a child of the standard {@code
 * &lt;instance>} element.</li>
 * </ul>
 *
 * @author Aleks Seovic  2019.10.02
 * @since 20.06
 */
public class CdiNamespaceHandler
        extends AbstractNamespaceHandler
    {
    /**
     * Construct {@code CdiNamespaceHandler} instance.
     */
    public CdiNamespaceHandler()
        {
        registerProcessor(BeanProcessor.class);
        }
    }
