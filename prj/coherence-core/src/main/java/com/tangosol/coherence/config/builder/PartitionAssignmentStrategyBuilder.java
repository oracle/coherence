/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.builder;

import com.tangosol.coherence.config.ParameterList;

import com.tangosol.config.ConfigurationException;
import com.tangosol.config.expression.ParameterResolver;

import com.tangosol.net.partition.MirroringAssignmentStrategy;
import com.tangosol.net.partition.PartitionAssignmentStrategy;
import com.tangosol.net.partition.SimpleAssignmentStrategy;

import com.tangosol.run.xml.XmlElement;

import com.tangosol.util.Base;

/**
 * The {@link PartitionAssignmentStrategyBuilder} builds a {@link PartitionAssignmentStrategy}.
 *
 * @author jf 2015.01.29
 * @since Coherence 12.2.1
 */
public class PartitionAssignmentStrategyBuilder
        extends DefaultBuilderCustomization<PartitionAssignmentStrategy>
        implements ParameterizedBuilder<PartitionAssignmentStrategy>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs {@link PartitionAssignmentStrategyBuilder}
     *
     * @param bldr        customized PartitionAssignmentStrategy builder
     * @param xmlElement  optional configuration context to be used to provide details if
     *                    a ConfigurationException is detected.
     *
     */
    public PartitionAssignmentStrategyBuilder(ParameterizedBuilder<?> bldr, XmlElement xmlElement)
        {
        m_builder    = bldr;
        m_sStrategy  = null;
        m_xmlElement = xmlElement;
        }

    /**
     * Constructs a {@link PartitionAssignmentStrategyBuilder} for a predefined strategy.
     *
     * @param sStrategy   implementation defined partition assignment strategy
     * @param xmlElement  optional configuration context to be used to provide details if
     *                    a ConfigurationException is detected.
     */
    public PartitionAssignmentStrategyBuilder(String sStrategy, XmlElement xmlElement)
        {
        m_sStrategy  = sStrategy;
        m_xmlElement = xmlElement;
        m_builder    = null;
        }

    // ----- ParameterizedBuilder methods -----------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public PartitionAssignmentStrategy realize(ParameterResolver resolver, ClassLoader loader,
        ParameterList listParameters)
        {
        ParameterizedBuilder<?> builder = m_builder;
        String                  sPAS    = m_sStrategy;
        XmlElement              xmlPAS  = m_xmlElement;
        if (builder != null)
            {
            try
                {
                return (PartitionAssignmentStrategy) builder.realize(resolver, loader, listParameters);
                }
            catch (Exception e)
                {
                String sName = xmlPAS == null ? "" : xmlPAS.getName();
                throw new ConfigurationException("Invalid <" + sName +
                    "> declaration.  The specified builder doesn't produce a PartitionAssignmentStrategy in [" +
                    xmlPAS + "]", "Please specify a <" + sName + ">", e);
                }
            }
        else if ("simple".equals(sPAS))
            {
            return new SimpleAssignmentStrategy();
            }
        else if (sPAS != null && sPAS.startsWith("mirror:"))
            {
            return new MirroringAssignmentStrategy(sPAS.substring(7).trim());
            }
        throw new ConfigurationException("Invalid <partition-assignment-strategy> declaration of '" + sPAS +
                "' in [" + xmlPAS + "]", "Please specify a valid partition-assignment-strategy");
        }

    // ----- data members ---------------------------------------------------

    /**
     * Customized builder
     */
    private ParameterizedBuilder<?> m_builder;

    /**
     * Provides configuration content if there is a ConfigurationException.
     */
    private XmlElement m_xmlElement;

    /**
     * Predefined strategy names
     */
    private String m_sStrategy;
    }
