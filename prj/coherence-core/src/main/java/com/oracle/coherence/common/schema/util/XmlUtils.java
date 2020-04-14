/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.common.schema.util;


import java.util.ArrayList;
import java.util.List;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;


/**
 * Various XML helpers.
 *
 * @author as  2013.07.12
 */
public class XmlUtils
    {
    /**
     * Return the first child element with a given name and namespace.
     *
     * @param parent  the element to get the child from
     * @param ns      the namespace of the child element
     * @param name    the name of the child element
     *
     * @return the first child element with a given name and namespace, or
     *         {@code null} if no such child elements exist
     */
    public static Element getChildElement(Element parent, String ns, String name)
        {
        NodeList nodes = parent.getElementsByTagNameNS(ns, name);
        if (nodes != null && nodes.getLength() > 0)
            {
            return (Element) nodes.item(0);
            }
        return null;
        }

    /**
     * Convert the DOM {@code NodeList} to a list of elements.
     *
     * @param nodes  the {@code NodeList} to convert
     *
     * @return the list of elements from the specified {@code NodeList}
     */
    public static List<Element> toElementList(NodeList nodes)
        {
        List<Element> elements = new ArrayList<>(nodes.getLength());
        for (int i = 0; i < nodes.getLength(); i++)
            {
            elements.add((Element) nodes.item(i));
            }
        return elements;
        }

    /**
     * Convert the DOM {@code NamedNodeMap} to a list of attributes.
     *
     * @param attributeMap  the {@code NamedNodeMap} to convert
     *
     * @return the list of attributes from the specified {@code NamedNodeMap}
     */
    public static List<Attr> toAttributeList(NamedNodeMap attributeMap)
        {
        List<Attr> attributes = new ArrayList<>(attributeMap.getLength());
        for (int i = 0; i < attributeMap.getLength(); i++)
            {
            attributes.add((Attr) attributeMap.item(i));
            }
        return attributes;
        }

    /**
     * Return the value of a boolean XML attribute.
     * <p/>
     * If the attribute is not present within the specified element, this method
     * will return {@code false}.
     *
     * @param element  the element to get the attribute value from
     * @param name     the name of the boolean attribute to get
     *
     * @return the boolean value of the specified attribute, or {@code false}
     *         if the attribute is not present within the specified element
     */
    public static boolean getBooleanAttribute(Element element, String name)
        {
        String value = element.getAttribute(name);
        return !StringUtils.isEmpty(value) && Boolean.parseBoolean(value);
        }
    }
