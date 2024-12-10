/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.run.xml;


/**
* An IterableAdapter is the base class for any data type that must be
* iterated over to serialize/deserialize, such as arrays, collections
* and maps.
*
* <pre><code>
* &lt;property&gt;
*   &lt;name&gt;People&lt;/name&gt;
*   &lt;xml-name&gt;people&lt;/xml-name&gt;         &lt;!-- optional, empty name indicates anonymous element --&gt;
*   &lt;adapter&gt;...&lt;/adapter&gt;              &lt;!-- optional --&gt;
*   &lt;type&gt;...&lt;/type&gt;                    &lt;!-- defaults via reflection --&gt;
*   &lt;class&gt;...&lt;/class&gt;                  &lt;!-- defaults to &lt;type&gt; --&gt;
*   &lt;sparse&gt;true&lt;/sparse&gt;               &lt;!-- defaults to false --&gt;
*   &lt;empty-is-null&gt;true&lt;/empty-is-null&gt; &lt;!-- defaults to false --&gt;
*   &lt;element&gt;                       &lt;!-- optional, depends on the adapter --&gt;
*     &lt;xml-name&gt;person&lt;/xml-name&gt;   &lt;!-- optional, nests the elements --&gt;
*   &lt;/element&gt;
* &lt;property&gt;
*
* Example of collection/array nested within collection tags:
*
*   &lt;doc&gt;
*     &lt;people&gt;
*       &lt;person&gt;...&lt;/person&gt;
*       &lt;person&gt;...&lt;/person&gt;
*       ...
*     &lt;/people&gt;
*   &lt;/doc&gt;
*
* Example of collection/array nested directly within the document:
*
*   &lt;doc&gt;
*     &lt;person&gt;...&lt;/person&gt;
*     &lt;person&gt;...&lt;/person&gt;
*     ...
*   &lt;/doc&gt;
*
* Example of map nested within collection tags:
*
*   &lt;doc&gt;
*     &lt;people&gt;
*       &lt;person&gt;
*         &lt;name&gt;...&lt;/name&gt;
*         &lt;number&gt;...&lt;/number&gt;
*       &lt;/person&gt;
*       &lt;person&gt;
*         &lt;name&gt;...&lt;/name&gt;
*         &lt;number&gt;...&lt;/number&gt;
*       &lt;/person&gt;
*       ...
*     &lt;/people&gt;
*   &lt;/doc&gt;
*
* Example of map nested directly within the document:
*
*   &lt;doc&gt;
*     &lt;person&gt;
*       &lt;name&gt;...&lt;/name&gt;
*       &lt;number&gt;...&lt;/number&gt;
*     &lt;/person&gt;
*     &lt;person&gt;
*       &lt;name&gt;...&lt;/name&gt;
*       &lt;number&gt;...&lt;/number&gt;
*     &lt;/person&gt;
*     ...
*   &lt;/doc&gt;
* </code></pre>
*
* @version 1.00  2001.03.18
* @author cp
*/
public abstract class IterableAdapter
        extends PropertyAdapter
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a IterableAdapter.
    *
    * @param infoBean BeanInfo for a bean containing this property
    * @param clzType  the type of the property
    * @param sName    the property name
    * @param sXml     the XML tag name
    * @param xml      additional XML information
    */
    public IterableAdapter(XmlBean.BeanInfo infoBean, Class clzType, String sName, String sXml, XmlElement xml)
        {
        super(infoBean, clzType, sName, sXml, xml);

        // determine if sparse array storage is desired
        m_fSparse = xml.getSafeElement("sparse").getBoolean();

        // determine if empty and null are synonymous
        m_fEmptyIsNull = xml.getSafeElement("empty-is-null").getBoolean();

        // determine if the elements will be nested (true if there is
        // a sub-element name)
        XmlElement xmlElement = xml.findElement("element/xml-name");
        if (xmlElement == null)
            {
            // sparse requires the elements to be nested one level down
            azzert(!m_fSparse);
            }
        else
            {
            m_sElement = xmlElement.getString();
            }
        }


    // ----- accessors ------------------------------------------------------

    /**
    * @return true if the property value must be "deep" cloned when the
    *         containing object is cloned
    */
    public boolean isCloneRequired()
        {
        return true;
        }

    /**
    * @return  true if the iterable data should be stored in a sparse format
    *          in XML
    */
    public boolean isSparse()
        {
        return m_fSparse;
        }

    /**
    * @return  true if the iterable data should not be stored at all if it
    *          is empty, such as a zero-length array or empty collection
    */
    public boolean isEmptyIsNull()
        {
        return m_fEmptyIsNull;
        }

    /**
    * @return  the local XML name of the individual array elements
    *          (null if the array elements are nested directly within
    *          the document)
    */
    public String getElementName()
        {
        return m_sElement;
        }

    /**
    * @return  true only if this adapter creates a single XML element on
    *          writeXml and reads from a single XML element on readXml
    */
    public boolean isNested()
        {
        return getElementName() != null;
        }


    // ----- XmlSerializable helpers ----------------------------------------

    /**
    * Deserialize an object from an XML element.
    *
    * @param xml  the XML element to deserialize from
    *
    * @return the object deserialized from the XML element
    *
    * @exception UnsupportedOperationException  if the property cannot be
    *            read from a single XML element
    */
    public Object fromXml(XmlElement xml)
        {
        return readElements(xml);
        }

    /**
    * Serialize an object into an XML element.
    *
    * @param o  the object to serialize
    *
    * @return the XML element representing the serialized form of the
    *         passed object
    *
    * @exception UnsupportedOperationException  if the property cannot be
    *            written to a single XML element
    */
    public XmlElement toXml(Object o)
        {
        // anonymous element's name must be discarded by the caller
        XmlElement xml = new SimpleElement(getXmlName());

        writeElements(xml, o);

        return xml;
        }

    /**
    * @param xml  the XML element containing the XML elements to deserialize
    *             from
    *
    * @return the object deserialized from the XML (not null)
    */
    protected abstract Object readElements(XmlElement xml);

    /**
    * @param xml  the XML element to which the iterable elements are written
    * @param o    the object to serialize (not null)
    */
    protected abstract void writeElements(XmlElement xml, Object o);


    // ----- helpers --------------------------------------------------------

    /**
    * Obtain a PropertyAdapapter for a map entry key or value
    *
    * @param infoBean  BeanInfo for a bean containing this property
    * @param xml       the information about the map entry key or value
    *
    * @return a PropertyAdapter for the map entry key or value
    */
    protected PropertyAdapter findAdapter(XmlBean.BeanInfo infoBean, XmlElement xml)
        {
        azzert(xml != null);

        String   sName   = null;
        XmlValue xmlName = xml.getElement("xml-name");
        if (xmlName != null)
            {
            sName = xmlName.getString();
            }

        XmlValue xmlType = xml.getElement("type");
        azzert(xmlType != null);
        Class clz = infoBean.resolveClass(xmlType.getString());

        return infoBean.makeAdapter(clz, null, sName, xml);
        }


    // ----- data members ---------------------------------------------------

    /**
    * Sparse array storage option.
    */
    protected boolean m_fSparse;

    /**
    * Empty-is-null option: Empty iterable values are not stored in the
    * serialized form of the XmlBean nor in the XML form of the XmlBean.
    */
    protected boolean m_fEmptyIsNull;

    /**
    * Name used for each element of the array when formatted into XML.  If
    * null, then the elements are placed directly into the document using the
    * adapter's XML name.
    */
    protected String m_sElement;
    }
