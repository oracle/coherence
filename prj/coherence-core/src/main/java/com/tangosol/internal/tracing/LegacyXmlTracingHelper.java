/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.tracing;

import com.tangosol.run.xml.XmlElement;

import com.tangosol.util.Base;

import java.math.BigDecimal;

/**
 * {@code LegacyXmlTracingHelper} parses the {@code <tracing-config>} XML to populate
 * the {@link TracingShim.Dependencies}.
 *
 * NOTE: This code will eventually be replaced by CODI.
 *
 * @author mf  2018.07.24
 * @since Coherence 14.1.1.0
 */
public class LegacyXmlTracingHelper
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construction not supported.
     */
    protected LegacyXmlTracingHelper()
        {
        }

    // ----- public methods -------------------------------------------------

    /**
     * Populate the {@link TracingShim.Dependencies} object from the XML configuration.
     *
     * @param xml   the {@code <tracing-config>} XML element
     * @param deps  the {@link TracingShim.Dependencies} to be populated
     *
     * @return the {@link TracingShim.Dependencies} object that was passed in
     */
    public static TracingShim.DefaultDependencies fromXml(XmlElement xml, TracingShim.DefaultDependencies deps)
        {
        Base.azzert(xml.getName().equals("tracing-config"));

        return deps.setSamplingRatio(xml.getSafeElement("sampling-ratio").
                getDecimal(BigDecimal.valueOf(deps.getSamplingRatio())).floatValue());
        }
    }
