/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.run.xml;


import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.List;
import java.util.Map;
import java.util.Iterator;


/**
* A MapAdapter supports properties whose types implement the java.util.Map
* interface.
*
* <pre>{@code
* &lt;property&gt;
*   &lt;name&gt;People&lt;/name&gt;
*   &lt;xml-name&gt;people&lt;/xml-name&gt;         &lt;!-- defaults to &lt;name&gt; --&gt;
*   &lt;type&gt;java.util.Map&lt;/type&gt;          &lt;!-- defaults via reflection --&gt;
*   &lt;class&gt;java.util.HashMap&lt;/class&gt;    &lt;!-- defaults to &lt;type&gt; --&gt;
*   &lt;empty-is-null&gt;true&lt;/empty-is-null&gt; &lt;!-- defaults to false --&gt;
*   &lt;element&gt;
*     &lt;xml-name&gt;person&lt;/xml-name&gt;       &lt;!-- required --&gt;
*     &lt;key&gt;
*       &lt;name&gt;ssn&lt;/name&gt;                &lt;!-- defaults to "key" --&gt;
*       &lt;xml-name&gt;ssn&lt;/xml-name&gt;        &lt;!-- defaults to &lt;name&gt; --&gt;
*       &lt;type&gt;String&lt;/type&gt;             &lt;!-- required --&gt;
*       &lt;adapter&gt;...&lt;/adapter&gt;          &lt;!-- optional --&gt;
*       &lt;...&gt;                           &lt;!-- for the type-specific adapter --&gt;
*     &lt;/key&gt;
*     &lt;value&gt;
*       &lt;name&gt;info&lt;/name&gt;               &lt;!-- defaults to "value" --&gt;
*       &lt;xml-name&gt;info&lt;/xml-name&gt;       &lt;!-- defaults to &lt;name&gt; --&gt;
*       &lt;type&gt;com...PersonBean&lt;/type&gt;   &lt;!-- required --&gt;
*       &lt;adapter&gt;...&lt;/adapter&gt;          &lt;!-- optional --&gt;
*       &lt;...&gt;                           &lt;!-- for the type-specific adapter --&gt;
*     &lt;/value&gt;
*   &lt;/elelemt&gt;  
* &lt;/property&gt;
*
* Example of map nested within collection tags:
*
*   &lt;doc&gt;
*     &lt;people&gt;
*       &lt;person&gt;
*         &lt;ssn&gt;...&lt;/ssn&gt;
*         &lt;info&gt;...&lt;/info&gt;
*       &lt;/person&gt;
*       &lt;person&gt;
*         &lt;ssn&gt;...&lt;/ssn&gt;
*         &lt;info&gt;...&lt;/info&gt;
*       &lt;/person&gt;
*       ...
*     &lt;/people&gt;
*   &lt;/doc&gt;
* }</pre>
*
* @version 1.00  2001.03.18
* @author cp
*/
public class MapAdapter
        extends IterableAdapter
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a MapAdapter.
    *
    * @param infoBean BeanInfo for a bean containing this property
    * @param clzType  the type of the property
    * @param sName    the property name
    * @param sXml     the XML tag name
    * @param xml      additional XML information
    */
    public MapAdapter(XmlBean.BeanInfo infoBean, Class clzType, String sName, String sXml, XmlElement xml)
        {
        super(infoBean, clzType, sName, sXml, xml);
        azzert(Map.class.isAssignableFrom(clzType));

        Class    clzMap = getType();
        XmlValue xmlClz = xml.getElement("class");
        if (xmlClz != null)
            {
            clzMap = infoBean.resolveClass(xmlClz.getString());
            }
        m_clzMap = clzMap;

        m_adapterKey   = findAdapter(infoBean, xml.findElement("element/key"));
        m_adapterValue = findAdapter(infoBean, xml.findElement("element/value"));
        }


    // ----- Object method helpers ------------------------------------------

    /**
    * Make a clone of the passed object.
    *
    * @param o  the object to clone
    *
    * @return a clone of the passed object
    */
    public Object clone(Object o)
        {
        if (o == null)
            {
            return null;
            }

        PropertyAdapter adapterKey   = m_adapterKey;
        PropertyAdapter adapterValue = m_adapterValue;

        Map mapOld = (Map) o;
        Map mapNew = instantiateMap();

        for (Iterator iter = mapOld.entrySet().iterator(); iter.hasNext(); )
            {
            Map.Entry entry = (Map.Entry) iter.next();

            Object oKey = entry.getKey();
            if (oKey != null)
                {
                oKey = adapterKey.clone(oKey);
                }

            Object oValue = entry.getValue();
            if (oValue != null)
                {
                oValue = adapterValue.clone(oValue);
                }

            mapNew.put(oKey, oValue);
            }

        return mapNew;
        }


    // ----- XmlSerializable helpers ----------------------------------------

    /**
    * @param xml  the XML element containing the XML elements to deserialize
    *             from
    *
    * @return the object deserialized from the XML (not null)
    */
    protected Object readElements(XmlElement xml)
        {
        String sElement = getElementName();
        if (sElement == null)
            {
            sElement = "entry";
            }

        PropertyAdapter adapterKey   = m_adapterKey;
        PropertyAdapter adapterValue = m_adapterValue;

        Map      map  = instantiateMap();
        Iterator iter = XmlHelper.getElements(xml, sElement, getNamespaceUri());

        while (iter.hasNext())
            {
            XmlElement xmlEntry = (XmlElement) iter.next();

            XmlElement xmlKey   = adapterKey.findElement(xmlEntry);
            Object     oKey     = null;
            if (xmlKey != null)
                {
                oKey = adapterKey.fromXml(xmlKey);
                }
            
            XmlElement xmlValue = adapterValue.findElement(xmlEntry);
            Object     oValue   = null;
            if (xmlValue != null)
                {
                oValue = adapterValue.fromXml(xmlValue);
                }
            
            map.put(oKey, oValue);
            }
        return map;
        }

    /**
    * @param xml  the XML element to which the iterable elements are written
    * @param o    the object to serialize (not null)
    */
    protected void writeElements(XmlElement xml, Object o)
        {
        String sElement = XmlHelper.getUniversalName(getElementName(), getNamespacePrefix());
        if (sElement == null)
            {
            sElement = "entry";
            }

        PropertyAdapter adapterKey   = m_adapterKey;
        PropertyAdapter adapterValue = m_adapterValue;

        for (Iterator iter = ((Map) o).entrySet().iterator(); iter.hasNext(); )
            {
            Map.Entry  entry    = (Map.Entry) iter.next();
            XmlElement xmlEntry = xml.addElement(sElement);
            List       list     = xmlEntry.getElementList();

            Object oKey = entry.getKey();
            if (oKey != null)
                {
                list.add(adapterKey.toXml(oKey));
                }

            Object oValue = entry.getValue();
            if (oValue != null)
                {
                list.add(adapterValue.toXml(oValue));
                }
            }
        }


    // ----- ExternalizableLite helpers -------------------------------------

    /**
    * Read a value from the passed DataInput object.
    *
    * @param in  the DataInput stream to read property data from
    *
    * @return   the data read from the DataInput; never null
    *
    * @exception IOException   if an I/O exception occurs
    */
    public Object readExternal(DataInput in)
            throws IOException
        {
        Map             map        = instantiateMap();
        PropertyAdapter adapterKey = getKeyAdapter();
        PropertyAdapter adapterVal = getValueAdapter();

        // "in" contains a collection size and, for each element, a non-null
        // indicator and (if non-null) the object
        for (int i = 0, c = readInt(in); i < c; ++i)
            {
            Object oKey = (in.readBoolean() ? adapterKey.readExternal(in) : null);
            Object oVal = (in.readBoolean() ? adapterVal.readExternal(in) : null);
            map.put(oKey, oVal);
            }
        return map;
        }

    /**
    * Write the specified data to the passed DataOutput object.
    *
    * @param out  the DataOutput stream to write to
    * @param o    the data to write to the DataOutput; never null
    *
    * @exception IOException  if an I/O exception occurs
    */
    public void writeExternal(DataOutput out, Object o)
            throws IOException
        {
        Map             map        = (Map) o;
        PropertyAdapter adapterKey = getKeyAdapter();
        PropertyAdapter adapterVal = getValueAdapter();

        int c = map.size();
        writeInt(out, c);

        int cActual = 0;
        for (Iterator iter = map.entrySet().iterator(); iter.hasNext(); )
            {
            Map.Entry entry = (Map.Entry) iter.next();
            Object    oKey  = entry.getKey();
            Object    oVal  = entry.getValue();

            boolean fExists  = (oKey != null);
            out.writeBoolean(fExists);
            if (fExists)
                {
                adapterKey.writeExternal(out, oKey);
                }

            fExists  = (oVal != null);
            out.writeBoolean(fExists);
            if (fExists)
                {
                adapterVal.writeExternal(out, oVal);
                }

            ++cActual;
            }
        if (c != cActual)
            {
            throw new IOException("expected " + c + " entries, but wrote " + cActual);
            }
        }


    // ----- PropertyAdapter methods ----------------------------------------

    /**
    * Determine if the specified value is empty.
    *
    * @param o  the value
    *
    * @return  true if the object is considered to be empty for persistence
    *          and XML-generation purposes
    */
    public boolean isEmpty(Object o)
        {
        return o == null || isEmptyIsNull() && ((Map) o).isEmpty();
        }


    // ----- internal helpers -----------------------------------------------

    /**
    * @return a PropertyAdapter for map entry keys
    */
    public PropertyAdapter getKeyAdapter()
        {
        return m_adapterKey;
        }

    /**
    * @return a PropertyAdapter for map entry values
    */
    public PropertyAdapter getValueAdapter()
        {
        return m_adapterValue;
        }

    /**
    * @return a new Map instance
    */
    protected Map instantiateMap()
        {
        try
            {
            return (Map) m_clzMap.newInstance();
            }
        catch (Throwable e)
            {
            throw ensureRuntimeException(e);
            }
        }


    // ----- data members ---------------------------------------------------

    private Class           m_clzMap;
    private PropertyAdapter m_adapterKey;
    private PropertyAdapter m_adapterValue;
    }
