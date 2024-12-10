/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.config.xml;

import com.tangosol.run.xml.XmlAttribute;
import com.tangosol.run.xml.XmlElement;

import java.net.URI;

/**
 * A {@link NamespaceHandler} is responsible for defining the
 * {@link DocumentPreprocessor}, {@link ElementProcessor}s and
 * {@link AttributeProcessor}s
 * required for processing xml content belonging to a specific xml namespace
 * used in an xml document.
 *
 * @see DocumentPreprocessor
 * @see ElementProcessor
 * @see AttributeProcessor
 * @see DocumentProcessor
 * @see AbstractNamespaceHandler
 *
 * @author bo  2011.06.14
 * @since Coherence 12.1.2
 */
public interface NamespaceHandler
    {
    /**
     * Obtains the {@link DocumentPreprocessor} that must be applied to the
     * {@link XmlElement} (ie: document) in which the {@link NamespaceHandler}
     * is defined, prior to {@link XmlElement}s and {@link XmlAttribute}s
     * being processed defined by this {@link NamespaceHandler}.
     *
     * @return  the {@link DocumentPreprocessor} or <code>null</code> if
     *          one is not required or defined for the {@link NamespaceHandler}
     */
    DocumentPreprocessor getDocumentPreprocessor();

    /**
     * Obtains the {@link AttributeProcessor} that is suitable for processing the specified {@link XmlAttribute}
     * in the xml namespace associated with this {@link NamespaceHandler}.
     *
     * @param attribute  the {@link XmlAttribute}
     *
     * @return the {@link AttributeProcessor} or <code>null</code> if a suitable {@link AttributeProcessor} could
     *         not be found
     */
    AttributeProcessor<?> getAttributeProcessor(XmlAttribute attribute);

    /**
     * Obtains the {@link ElementProcessor} that is suitable for processing the specified {@link XmlElement}
     * in the xml namespace associated with this {@link NamespaceHandler}.
     *
     * @param element  the {@link XmlElement}
     *
     * @return the {@link ElementProcessor} or <code>null</code> if a suitable {@link ElementProcessor} could
     *         not be found
     */
    ElementProcessor<?> getElementProcessor(XmlElement element);

    /**
     * Obtains the {@link OverrideProcessor} that is suitable for processing the xml override.
     *
     * @return the {@link OverrideProcessor} or <code>null</code> if a suitable {@link OverrideProcessor} could
     *         not be found
     */
    default OverrideProcessor getOverrideProcessor()
        {
        return null;
        }

    /**
     * Called when the xml namespace associated with the {@link NamespaceHandler} is first encountered in an xml
     * document.
     *
     * @param context  the document {@link ProcessingContext} in which the xml namespace was encountered
     * @param element  the {@link XmlElement} in which the xml namespace was encountered
     * @param sPrefix  the prefix of the declared xml namespace
     * @param uri      the {@link URI} of the declared xml namespace
     */
    void onStartNamespace(ProcessingContext context, XmlElement element, String sPrefix, URI uri);

    /**
     * Called when the xml namespace associated with the {@link NamespaceHandler} is last encountered in an xml document.
     *
     * @param context  the document {@link ProcessingContext} in which the xml namespace was encountered
     * @param element  the {@link XmlElement} in which the xml namespace was encountered
     * @param sPrefix  the prefix of the declared xml namespace
     * @param uri      the {@link URI} of the declared xml namespace
     */
    void onEndNamespace(ProcessingContext context, XmlElement element, String sPrefix, URI uri);
    }
