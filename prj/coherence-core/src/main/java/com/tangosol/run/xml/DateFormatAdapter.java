/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.run.xml;


import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.text.ParseException;

import java.util.Date;
import java.util.Locale;

/**
* A property adapter for formatting formatting and parsing dates in a 
* locale-sensitive manner. Following is an example of the adapter 
* description.
*
* <pre>
* &lt;adapter&gt;com.tangosol.run.xml.DateFormatAdapter&lt;/adapter&gt;
* &lt;pattern&gt;yyyy.MM.dd G 'at' hh:mm:ss z&lt;/pattern&gt;
* &lt;locale&gt;
*   &lt;language&gt;en&lt;/language&gt;
*   &lt;country&gt;US&lt;/country&gt;
* &lt;/locale&gt;
* </pre>
*
* @see SimpleDateFormat
*
* @version 1.00  2002.10.11
* @author gg
*/
public class DateFormatAdapter
        extends SimpleAdapter.OldDateAdapter
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a DateFormatAdapter.
    *
    * @param infoBean BeanInfo for a bean containing this property
    * @param clzType  the type of the property
    * @param sName    the property name
    * @param sXml     the XML tag name
    * @param xml      additional XML information
    */
    public DateFormatAdapter(XmlBean.BeanInfo infoBean, Class clzType, String sName, String sXml, XmlElement xml)
        {
        super(infoBean, clzType, sName, sXml, xml);

        DateFormat formatter = null;
        String     sPattern  = xml.getSafeElement("pattern").getString();

        if (sPattern.length() == 0)
            {
            formatter = new SimpleDateFormat();
            }
        else
            {
            XmlElement xmlLocale = xml.getElement("locale");
            if (xmlLocale == null)
                {
                formatter = new SimpleDateFormat(sPattern);
                }
            else
                {
                String sLanguage = xmlLocale.getSafeElement("language").getString();
                String sCountry  = xmlLocale.getSafeElement("country") .getString();
                Locale locale    = new Locale(sLanguage, sCountry);

                formatter = new SimpleDateFormat(sPattern, locale);
                }
            }
        formatter.setLenient(true);
        
        m_formatter = formatter;
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
        String sDate = xml.getString(null);
        if (sDate == null)
            {
            return null;
            }
        else
            {
            try
                {
                return m_formatter.parse(sDate);
                }
            catch (ParseException e)
                {
                return new java.util.Date(0);
                }
            }
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
        return o == null ? null :
            new SimpleElement(getXmlName(), m_formatter.format((Date) o));
        }

    
    // ----- data fields ----------------------------------------------------

    /**
    * The formatter to be used for formatting and parsing dates in a 
    * locale-sensitive manner.
    */
    private DateFormat m_formatter;
    }
