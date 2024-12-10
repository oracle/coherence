/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io.pof.schema.handler;

import com.oracle.coherence.common.schema.AbstractPropertyHandler;
import com.oracle.coherence.common.schema.AbstractTypeHandler;
import com.oracle.coherence.common.schema.ExtensibleProperty;
import com.oracle.coherence.common.schema.ExtensibleType;
import com.oracle.coherence.common.schema.PropertyHandler;
import com.oracle.coherence.common.schema.Schema;
import com.oracle.coherence.common.schema.TypeHandler;
import com.tangosol.io.pof.schema.PofProperty;
import com.tangosol.io.pof.schema.PofType;
import org.w3c.dom.Element;

/**
 * Reads {@link PofType} metadata from XML.
 *
 * @author as  2013.11.20
 */
public class XmlHandler
    {
    // ---- inner class: XmlTypeHandler -------------------------------------

    /**
     * A {@link TypeHandler} implementation that reads {@link PofType} metadata
     * from XML.
     */
    public static class XmlTypeHandler
            extends AbstractTypeHandler<PofType, Element>
        {
        @Override
        public PofType createType(ExtensibleType parent)
            {
            return new PofType(parent);
            }

        @Override
        public void importType(PofType type, Element source, Schema schema)
            {
            if (source.hasAttributeNS(NS, "id"))
                {
                type.setId(Integer.parseInt(source.getAttributeNS(NS, "id")));
                }
            if (source.hasAttributeNS(NS, "version"))
                {
                type.setVersion(Integer.parseInt(source.getAttributeNS(NS, "version")));
                }
            }

        }

    // ---- inner class: XmlPropertyHandler ---------------------------------

    /**
     * A {@link PropertyHandler} implementation that reads {@link PofProperty}
     * metadata from XML.
     */
    public static class XmlPropertyHandler
            extends AbstractPropertyHandler<PofProperty, Element>
        {
        @Override
        public PofProperty createProperty(ExtensibleProperty parent)
            {
            return new PofProperty(parent);
            }

        @Override
        public void importProperty(PofProperty property, Element source, Schema schema)
            {
            if (source.hasAttributeNS(NS, "since"))
                {
                property.setSince(Integer.parseInt(source.getAttributeNS(NS, "since")));
                }
            if (source.hasAttributeNS(NS, "order"))
                {
                property.setOrder(Integer.parseInt(source.getAttributeNS(NS, "order")));
                }
            }
        }

    // ---- constants -------------------------------------------------------

    /**
     * XML namespace for POF metadata.
     */
    private static final String NS = "http://xmlns.oracle.com/coherence/schema/pof";
    }
