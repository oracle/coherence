/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.preprocessor;

import com.tangosol.config.ConfigurationException;

import com.tangosol.config.xml.DocumentElementPreprocessor;
import com.tangosol.config.xml.ProcessingContext;

import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;

/**
 * {@link DocumentElementPreprocessor.ElementPreprocessor} for the {@value VIEW_SCHEME_ELEMENT} element.
 *
 * @author rlubke
 * @since 12.2.1.4
 */
public class ViewSchemePreprocessor
        implements DocumentElementPreprocessor.ElementPreprocessor
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor.
     */
    private ViewSchemePreprocessor()
        {
        }

    // ----- interface ElementPreprocessor ----------------------------------

    @SuppressWarnings("unchecked")
    @Override
    public boolean preprocess(ProcessingContext context, XmlElement element) throws ConfigurationException
        {
        boolean fModified = false;
        if (VIEW_SCHEME_ELEMENT.equals(element.getName()))
            {
            XmlElement xmlViewFilter  = element.getElement(VIEW_FILTER_ELEMENT);
            XmlElement xmlFrontScheme = element.getElement(FRONT_SCHEME_ELEMENT);

            if (xmlViewFilter == null && xmlFrontScheme == null)
                {
                element.getElementList().add(DEFAULT_XML.clone());
                xmlFrontScheme = element.getElement(FRONT_SCHEME_ELEMENT);
                fModified = true;
                }
            else
                {
                if (xmlFrontScheme == null)
                    {
                    xmlFrontScheme           = XmlHelper.ensureElement(element, FRONT_SCHEME_ELEMENT);
                    XmlElement xmlViewScheme = XmlHelper.ensureElement(xmlFrontScheme,
                                                                       CONTINUOUS_QUERY_CACHE_SCHEME_ELEMENT);

                    xmlViewScheme.getElementList().add(xmlViewFilter);
                    element.getElementList().remove(xmlViewFilter);
                    fModified = true;
                    }
                }

            XmlElement xmlViewScheme = xmlFrontScheme.getElement(CONTINUOUS_QUERY_CACHE_SCHEME_ELEMENT);
            for (int i = 0, len = ELEMENTS_TO_MOVE.length; i < len; i++)
                {
                XmlElement elementToMove = element.getElement(ELEMENTS_TO_MOVE[i]);
                if (elementToMove != null)
                    {
                    xmlViewScheme.getElementList().add(elementToMove);
                    element.getElementList().remove(elementToMove);
                    }
                }
            }
        return fModified;
        }

    // ----- constants ---------------------------------------------------

    /**
     * The {@code view-filter} element.
     */
    private static final String VIEW_FILTER_ELEMENT = "view-filter";

    /**
     * The {@code read-only} element.
     */
    private static final String READ_ONLY_ELEMENT = "read-only";

    /**
     * The {@code transformer} element.
     */
    private static final String TRANSFORMER_ELEMENT = "transformer";

    /**
     * The {@code reconnect-interval} element.
     */
    private static final String RECONNECT_INTERVAL_ELEMENT = "reconnect-interval";

    /**
     * The {@code view-scheme} element.
     */
    private static final String VIEW_SCHEME_ELEMENT = "view-scheme";

    /**
     * The {@code front-scheme} element.
     */
    private static final String FRONT_SCHEME_ELEMENT = "front-scheme";

    /**
     * The {@code continuous-query-cache-scheme} element.
     */
    private static final String CONTINUOUS_QUERY_CACHE_SCHEME_ELEMENT = "continuous-query-cache-scheme";

    /**
     * The {@code listener} element.
     */
    private static final String LISTENER_ELEMENT = "listener";

    /**
     * XML that will be injected if no {@value VIEW_FILTER_ELEMENT} element is present.
     */
    private static final XmlElement DEFAULT_XML =
            XmlHelper.loadXml("<front-scheme>"
                            + " <continuous-query-cache-scheme>"
                            + "  <view-filter>"
                            + "   <class-scheme>"
                            + "    <class-name>com.tangosol.util.filter.AlwaysFilter</class-name>"
                            + "   </class-scheme>"
                            + "  </view-filter>"
                            + " </continuous-query-cache-scheme>"
                            + "</front-scheme>");

    /**
     * Elements that are top-level within the {@value VIEW_SCHEME_ELEMENT} element and need to moved as children
     * of the {@value CONTINUOUS_QUERY_CACHE_SCHEME_ELEMENT} element.
     */
    private static final String[] ELEMENTS_TO_MOVE =
        {
        LISTENER_ELEMENT, READ_ONLY_ELEMENT, RECONNECT_INTERVAL_ELEMENT, TRANSFORMER_ELEMENT
        };

    /**
     * Singleton {@link ViewSchemePreprocessor} reference.
     */
    public static final ViewSchemePreprocessor INSTANCE = new ViewSchemePreprocessor();
    }
