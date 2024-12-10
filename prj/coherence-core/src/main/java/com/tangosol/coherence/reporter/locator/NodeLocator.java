/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.reporter.locator;


import com.tangosol.coherence.reporter.extractor.ConstantExtractor;

import com.tangosol.net.CacheFactory;

import com.tangosol.util.ValueExtractor;


/**
 * Locator for the node macro; finds the nodeId of the local member
 *
 * @since Coherence 12.1.3
 * @author sw 2012.11.29
 */
public class NodeLocator
        extends BaseLocator
    {
    /**
     * @inheritDoc
     */
    public ValueExtractor getExtractor()
        {
        ValueExtractor ve = m_veExtractor;
        if (ve == null)
            {
            ve = m_veExtractor = new ConstantExtractor(
                    Integer.valueOf(CacheFactory.ensureCluster().getLocalMember().getId()));
            }
        return ve;
        }
    }
