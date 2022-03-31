/*
 * Copyright (c) 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.processor;

import static com.tangosol.coherence.config.CacheConfig.TOP_LEVEL_ELEMENT_NAME;

import java.util.ArrayList;
import java.util.List;

import com.tangosol.config.xml.OverrideProcessor;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;

/**
 * Implementation of {@link OverrideProcessor} that process cache configuration
 * override file.
 * 
 * @since 14.1.2/22.06
 */
public class CacheConfigOverrideProcessor
        implements OverrideProcessor
    {

    /**
     * {@inheritDoc}
     */
    @Override
    public void process(XmlElement xmlBase, XmlElement xmlOverride)
        {
        XmlHelper.mergeSchema(xmlBase, xmlOverride);

        for (Object overrideElements : xmlOverride.getElementList())
            {
            XmlElement xmlElement = (XmlElement) overrideElements;

            if ("caching-scheme-mapping".equals(xmlElement.getName()))
                {
                processSchemeMappings(xmlBase, xmlElement, "cache-name");
                }
            else if ("topic-scheme-mapping".equals(xmlElement.getName()))
                {
                processSchemeMappings(xmlBase, xmlElement, "topic-name");
                }
            else if ("caching-schemes".equals(xmlElement.getName()))
                {
                processCachingSchemes(xmlBase, xmlElement);
                }
            else if ("interceptors".equals(xmlElement.getName()))
                {
                processInterceptors(xmlBase, xmlElement);
                }
            else
                {
                List<XmlElement> listElements = new ArrayList<>();

                listElements.add(xmlElement);
                XmlHelper.addElements(xmlBase, listElements.iterator());
                }
            }
        }

    /**
     * Method that process caching-scheme-mapping element of cache config for merging
     * caching-scheme-mapping declared in the override xml file.
     *
     * @param xmlBase                   parent cache config xml
     * @param xmlOverrideSchemeMappings  overriding caching-scheme-mapping element
     * @param sMappingSubElementName    Scheme mapping sub element name e.g cache-name for cache-scheme-mapping
     *                                  or topic-name for topic-scheme-mapping
     */
    private void processSchemeMappings(XmlElement xmlBase,
            XmlElement xmlOverrideSchemeMappings, String sMappingSubElementName)
        {
        List<XmlElement> listElements = new ArrayList<>();

        XmlElement xmlCurrentElement = xmlBase.getElement(xmlOverrideSchemeMappings.getName());
        if (xmlCurrentElement == null)
            {
            listElements.add(xmlOverrideSchemeMappings);
            XmlHelper.addElements(xmlBase, listElements.iterator());
            listElements.clear();
            return;
            }

        for (Object subElements : xmlOverrideSchemeMappings.getElementList())
            {
            XmlElement xmlElementOverride = (XmlElement) subElements;
            String     absXmlPath         = xmlElementOverride.getAbsolutePath();
            String     xmlPath            = absXmlPath.substring(("/" +
                                                TOP_LEVEL_ELEMENT_NAME + "/").length(),
                                                absXmlPath.lastIndexOf("/"));
            XmlElement xmlElementParent   = xmlBase.getElement(xmlPath);
            XmlElement xmlSchemeOverride  = xmlElementOverride.getElement("scheme-name");
            String     sSubElementName    = xmlElementOverride.getName();

            if (xmlElementParent != null && xmlSchemeOverride != null)
                {
                String sOverrideSchemeName = xmlSchemeOverride.getValue().toString();

                for (Object baseElements : xmlElementParent.getElementList())
                    {
                    XmlElement xmlElementBase   = (XmlElement) baseElements;
                    XmlElement xmlElementScheme = xmlElementBase.getElement("scheme-name");
                    String     xmlElementName   = xmlElementBase.getName();

                    if (xmlElementScheme != null)
                        {
                        String sSchemeName        = xmlElementScheme.getValue().toString();
                        String sCacheNameBase     = xmlElementBase.getElement(sMappingSubElementName).toString();
                        String sCacheNameOverride = xmlElementOverride.getElement(sMappingSubElementName).toString();

                        if (sCacheNameOverride.equals(sCacheNameBase))
                            {
                            XmlHelper.overrideElement(xmlElementBase, xmlElementOverride);
                            }
                        else if (!listElements.contains(xmlElementOverride))
                            {
                            listElements.add(xmlElementOverride);
                            }
                        else if (sSchemeName.equals(sOverrideSchemeName))
                            {
                            XmlElement removeElement = null;

                            for (XmlElement xmlEl : listElements)
                                {
                                XmlElement xmlElSchemeName = xmlEl.getElement("scheme-name");
                                if (xmlElSchemeName != null
                                        && sOverrideSchemeName.equals(xmlElSchemeName.getValue().toString()))
                                    {
                                    removeElement = xmlEl;
                                    break;
                                    }
                                }

                            if (removeElement != null)
                                {
                                listElements.remove(removeElement);
                                }

                            if (!xmlElementName.equals(sSubElementName))
                                {
                                xmlElementBase.setName(sSubElementName);
                                }

                            XmlHelper.overrideElement(xmlElementBase, xmlElementOverride);
                            }
                        else if (!listElements.contains(xmlElementOverride))
                            {
                            listElements.add(xmlElementOverride);
                            }
                        }
                    }
                }
            else if (xmlElementParent != null && !xmlElementParent.getElementList().isEmpty())
                {
                for (Object oBaseElement: xmlElementParent.getElementList())
                    {
                    XmlElement xmlElementBase = (XmlElement) oBaseElement;
                    XmlHelper.overrideElement(xmlElementBase, xmlElementOverride);
                    }
                }
            else if (!listElements.contains(xmlElementOverride))
                {
                listElements.add(xmlElementOverride);
                }
            }

        if (!listElements.isEmpty())
            {
            for (Object subElements : xmlCurrentElement.getElementList())
                {
                XmlElement subElement = (XmlElement) subElements;
                listElements.add(subElement);
                }

            for (XmlElement subElement : listElements)
                {
                XmlHelper.removeElement(xmlCurrentElement, subElement.getName());
                }

            XmlHelper.addElements(xmlCurrentElement, listElements.iterator());
            listElements.clear();
            }
        }

    /**
     * Method to process caching-schemes of the cache config xml for merging
     * caching-schemes declared in the override xml file.
     *
     * @param xmlBase                    parent cache config xml
     * @param xmlOverrideCachingSchemes  overriding caching-schemes element
     */
    private void processCachingSchemes(XmlElement xmlBase, XmlElement xmlOverrideCachingSchemes)
        {
        List<XmlElement> listElements = new ArrayList<XmlElement>();

        for (Object subElements : xmlOverrideCachingSchemes.getElementList())
            {
            XmlElement xmlOverride       = (XmlElement) subElements;
            String     absXmlPath        = xmlOverride.getAbsolutePath();
            String     xmlPath           = absXmlPath.substring(("/" +
                                               TOP_LEVEL_ELEMENT_NAME + "/").length(),
                                               absXmlPath.lastIndexOf("/"));
            XmlElement xmlElementParent  = xmlBase.getElement(xmlPath);
            XmlElement xmlSchemeOverride = xmlOverride.getElement("scheme-name");
            String     sSubElementName   = xmlOverride.getName();

            if (xmlSchemeOverride != null)
                {
                String sOverrideSchemeName = xmlSchemeOverride.getValue().toString();

                for (Object baseElements : xmlElementParent.getElementList())
                    {
                    XmlElement xmlElementBase   = (XmlElement) baseElements;
                    XmlElement xmlElementScheme = xmlElementBase.getElement("scheme-name");
                    String     xmlElementName   = xmlElementBase.getName();

                    if (xmlElementScheme != null)
                        {
                        String sSchemeName = xmlElementScheme.getValue().toString();

                        if (sSchemeName.equals(sOverrideSchemeName))
                            {
                            XmlElement xmlElementRemove = null;

                            for (XmlElement xmlEl : listElements)
                                {
                                XmlElement xmlElSchemeName = xmlEl.getElement("scheme-name");
                                if (xmlElSchemeName != null
                                            && sOverrideSchemeName.equals(xmlElSchemeName.getValue().toString()))
                                    {
                                    xmlElementRemove = xmlEl;
                                    break;
                                    }
                                }

                            if (xmlElementRemove != null)
                                {
                                listElements.remove(xmlElementRemove);
                                }

                            if (!xmlElementName.equals(sSubElementName))
                                {
                                xmlElementBase.setName(sSubElementName);
                                }

                            XmlHelper.overrideElement(xmlElementBase, xmlOverride);
                            }
                        else if (!listElements.contains(xmlOverride))
                            {
                            listElements.add(xmlOverride);
                            }
                        }
                    }
                }
            else if (!xmlElementParent.getElementList().isEmpty())
                {
                for (Object oBaseElement: xmlElementParent.getElementList())
                    {
                    XmlElement xmlElementFromBase = (XmlElement) oBaseElement;
                    XmlHelper.overrideElement(xmlElementFromBase, xmlOverride);
                    }
                }
            else if (!listElements.contains(xmlOverride))
                {
                listElements.add(xmlOverride);
                }
            }

        if (!listElements.isEmpty())
            {
            XmlHelper.addElements(xmlBase.getElement(xmlOverrideCachingSchemes.getName()), listElements.iterator());
            listElements.clear();
            }
        }

    /**
     * Method that process interceptors element from provided
     * cache config and override xml file.
     *
     * @param xmlBase                  parent cache config xml
     * @param xmlOverrideInterceptors  interceptors element from cache config override file
     */
    private void processInterceptors(XmlElement xmlBase, XmlElement xmlOverrideInterceptors)
        {
        List<XmlElement> listElements = new ArrayList<>();

        String xmlPathOverride = xmlOverrideInterceptors.getAbsolutePath();
        String xmlPathBase     = xmlPathOverride.substring(("/" + TOP_LEVEL_ELEMENT_NAME + "/").length());

        XmlElement xmlInterceptorsBase = xmlBase.getElement(xmlPathBase);

        if (xmlInterceptorsBase == null)
            {
            listElements.add(xmlOverrideInterceptors);

            XmlHelper.addElements(xmlBase, listElements.iterator());
            }
        else
            {
            for (Object xmlInterceptorsOverride : xmlOverrideInterceptors.getElementList())
                {
                XmlElement xmlInterceptorOverride   = (XmlElement) xmlInterceptorsOverride;

                if (xmlInterceptorOverride.getElement("name") != null)
                    {
                    String sInterceptorNameOverride = xmlInterceptorOverride.getElement("name").getValue().toString();

                    for (Object subElements : xmlInterceptorsBase.getElementList())
                        {
                        XmlElement xmlInterceptorBase = (XmlElement) subElements;

                        if (xmlInterceptorBase.getElement("name") != null)
                            {
                            if (sInterceptorNameOverride.equals(xmlInterceptorBase.getElement("name").getValue().toString()))
                                {
                                XmlHelper.overrideElement(xmlInterceptorBase, xmlInterceptorOverride);
                                }
                            else if (!listElements.contains(xmlInterceptorOverride))
                                {
                                listElements.add(xmlInterceptorOverride);
                                }
                            }
                        }
                    }
                else if (!listElements.contains(xmlInterceptorOverride))
                    {
                    listElements.add(xmlInterceptorOverride);
                    }
                }

            if (!listElements.isEmpty())
                {
                XmlHelper.addElements(xmlInterceptorsBase, listElements.iterator());
                listElements.clear();
                }
            }
        }
    }
