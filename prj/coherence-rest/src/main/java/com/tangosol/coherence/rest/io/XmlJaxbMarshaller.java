/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest.io;

/**
 * JAXB-based marshaller that marshals object to/from XML.
 *
 * @deprecated  As of 12.2.1.0.0, replaced by {@link JaxbXmlMarshaller}
 *
 * @author as  2011.07.10
 */
@Deprecated
public class XmlJaxbMarshaller<T>
        extends JaxbXmlMarshaller<T>
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Construct an XmlJaxbMarshaller instance.
     *
     * @param clzRoot  class of the root object this marshaller is for
     */
    public XmlJaxbMarshaller(Class<T> clzRoot)
        {
        super(clzRoot);
        }
    }
