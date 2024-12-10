/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.run.xml;


import com.tangosol.util.LiteMap;

import java.util.Iterator;
import java.util.Map;

/**
* A property adapter for
* <a href="http://www.w3.org/TR/2001/REC-xmlschema-2-20010502/#QName">QName</a>
* primitive datatype.
* <pre>
* &lt;adapter&gt;com.tangosol.run.xml.QNameAdapter&lt;/adapter&gt;
* &lt;value-space&gt;
*   &lt;xmlns&gt;
*     &lt;uri&gt;Uri-1&lt;/uri&gt;
*     &lt;prefix&gt;prefix-1&lt;/prefix&gt;
*   &lt;/xmlns&gt;
*   &lt;xmlns&gt;
*     &lt;uri&gt;Uri-2&lt;/uri&gt;
*     &lt;prefix&gt;prefix-2&lt;/prefix&gt;
*   &lt;/xmlns&gt;
*   ...
* &lt;/value-space&gt;
* </pre>
* @see <a href="http://www.w3.org/TR/2001/REC-xmlschema-2-20010502/">
*       XML Schema Part 2: Datatypes</a>
* @version 1.00  2002.07.02
* @author gg
*/
public class QNameAdapter
        extends SimpleAdapter.StringAdapter
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a QNameAdapter.
    *
    * @param infoBean BeanInfo for a bean containing this property
    * @param clzType  the type of the property
    * @param sName    the property name
    * @param sXml     the XML tag name
    * @param xml      additional XML information
    */
    public QNameAdapter(XmlBean.BeanInfo infoBean, Class clzType, String sName, String sXml, XmlElement xml)
        {
        super(infoBean, clzType, sName, sXml, xml);

        XmlElement xmlVS = xml.getElement("value-space");
        if (xmlVS != null)
            {
            Map mapNms = m_mapNms;
            for (Iterator iter = xmlVS.getElements("xmlns"); iter.hasNext();)
                {
                XmlElement xmlNms = (XmlElement) iter.next();

                String sNmsPrefix = xmlNms.getSafeElement("prefix").getString(null);
                String sNmsUri    = xmlNms.getSafeElement("uri")   .getString(null);

                mapNms.put(sNmsPrefix, sNmsUri);
                }
            }
        }


    // ----- XmlSerializable helpers ----------------------------------------

    /**
    * Deserialize an object from an XML element.
    *
    * @param xml  the XML element to deserialize from
    *
    * @return the object deserialized from the XML element
    */
    public Object fromXml(XmlElement xml)
        {
        String sValue = (String) super.fromXml(xml);
        int    of     = sValue.indexOf(':');
        if (of > 0)
            {
            String sPrefix = sValue.substring(0, of);
            String sUri    = XmlHelper.getNamespaceUri(xml, sPrefix);
            if (sUri != null)
                {
                // TODO: store the "runtime" values on the bean itself
                m_mapNms.put(sPrefix, sUri);
                }
            }
        return sValue;
        }

    /**
    * Serialize an object into an XML element.
    *
    * @param o  the object to serialize
    *
    * @return the XML element representing the serialized form of the
    *         passed object
    */
    public XmlElement toXml(Object o)
        {
        XmlElement xml = super.toXml(o);

        String sValue = xml.getString();
        int    of     = sValue.indexOf(':');
        if (of > 0)
            {
            String sPrefix = sValue.substring(0, of);
            String sUri    = (String) m_mapNms.get(sPrefix);
            if (sUri != null)
                {
                XmlHelper.ensureNamespace(xml, sPrefix, sUri);
                }
            }
        return xml;
        }


    // ----- validation helpers ---------------------------------------------

    /**
    * Specifies whether or not the specified QName value is associated with a
    * known namespace declaration.
    *
    * @param sValue  the specified QName value
    *
    * @return true is the specified QName value is associated with a known
    *         namespace declaration; false otherwise
    */
    public boolean isValidQName(String sValue)
        {
        int of = sValue.indexOf(':');
        if (of > 0)
            {
            String sPrefix = sValue.substring(0, of);
            return m_mapNms.containsKey(sPrefix);
            }
        return true;
        }


    // ----- data fields ----------------------------------------------------

    /**
    * Map of valid namespaces for the property values keyed by the namespace
    * prefix with a corresponding value being the namespace URI.
    */
    private Map m_mapNms = new LiteMap();
    }
