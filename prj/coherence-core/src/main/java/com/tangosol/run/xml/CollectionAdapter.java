/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.run.xml;


import com.tangosol.util.ClassHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;


/**
* A CollectionAdapter supports properties whose types implement the
* java.util.Collection interface.
* <pre><code>
* &lt;property&gt;
*   &lt;name&gt;People&lt;/name&gt;
*   &lt;xml-name&gt;people&lt;/xml-name&gt;         &lt;!-- defaults to &lt;name&gt; --&gt;
*   &lt;type&gt;java.util.Collection&lt;/type&gt;   &lt;!-- defaults via reflection --&gt;
*   &lt;class&gt;java.util.LinkedList&lt;/class&gt; &lt;!-- defaults to &lt;type&gt; --&gt;
*   &lt;empty-is-null&gt;true&lt;/empty-is-null&gt; &lt;!-- defaults to false --&gt;
*   &lt;element&gt;
*     &lt;xml-name&gt;person&lt;/xml-name&gt;       &lt;!-- optional, nests the elements --&gt;
*     &lt;type&gt;com...PersonBean&lt;/type&gt;     &lt;!-- required --&gt;
*     &lt;adapter&gt;...&lt;/adapter&gt;            &lt;!-- optional --&gt;
*     &lt;...&gt;                             &lt;!-- for the type-specific adapter --&gt;
*   &lt;/element&gt;
* &lt;/property&gt;
* </code></pre>
*
* Example of collection nested within collection tags:
* <pre><code>
*   &lt;doc&gt;
*     &lt;people&gt;
*       &lt;person&gt;
*         &lt;...&gt;
*       &lt;/person&gt;
*       &lt;person&gt;
*         &lt;...&gt;
*       &lt;/person&gt;
*       ...
*     &lt;/people&gt;
*   &lt;/doc&gt;
* </code></pre>
*
* @version 1.00  2001.03.18
* @author cp
*/
public class CollectionAdapter
        extends IterableAdapter
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a CollectionAdapter.
    *
    * @param infoBean BeanInfo for a bean containing this property
    * @param clzType  the type of the property
    * @param sName    the property name
    * @param sXml     the XML tag name
    * @param xml      additional XML information
    */
    public CollectionAdapter(XmlBean.BeanInfo infoBean, Class clzType, String sName, String sXml, XmlElement xml)
        {
        super(infoBean, clzType, sName, sXml, xml);
        azzert(Collection.class.isAssignableFrom(clzType));

        // determine the collection class used
        Class    clzCollection = getType();
        XmlValue xmlClz        = xml.getElement("class");
        if (xmlClz != null)
            {
            clzCollection = infoBean.resolveClass(xmlClz.getString());
            }
        m_clzCollection  = clzCollection;

        XmlElement xmlElement = xml.getElement("element");
        if (xmlElement == null)
            {
            throw new IllegalStateException(
                    "Missing the \"<element>\" information for the \"" +
                    sName + "\" property of the \"" +
                    ClassHelper.getSimpleName(infoBean.getType()) +
                    "\" XmlBean.");
            }
        m_adapterElement = findAdapter(infoBean, xmlElement);
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

        PropertyAdapter adapterElement = m_adapterElement;

        Collection colOld = (Collection) o;
        Collection colNew = instantiateCollection();

        for (Iterator iter = colOld.iterator(); iter.hasNext(); )
            {
            Object oElement = iter.next();
            if (oElement != null)
                {
                oElement = adapterElement.clone(oElement);
                }
            colNew.add(oElement);
            }

        return colNew;
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
        Collection      collection     = instantiateCollection();
        PropertyAdapter adapterElement = m_adapterElement;
        boolean         fAnonymous     = adapterElement.isAnonymous();

        Iterator iter = fAnonymous ?
            xml.getElementList().iterator() :
            XmlHelper.getElements(
                xml, getElementName(), adapterElement.getNamespaceUri());

        while (iter.hasNext())
            {
            XmlElement xmlElement = (XmlElement) iter.next();
            Object     oElement   = adapterElement.fromXml(xmlElement);

            // anonymous collections do not allow null elements
            if (!fAnonymous || oElement != null)
                {
                collection.add(oElement);
                }
            }

        return collection;
        }

    /**
    * @param xml  the XML element to which the iterable elements are written
    * @param o    the object to serialize (not null)
    */
    protected void writeElements(XmlElement xml, Object o)
        {
        Collection collection = (Collection) o;
        if (collection.isEmpty())
            {
            return;
            }

        PropertyAdapter adapterElement = m_adapterElement;
        boolean         fAnonymous     = adapterElement.isAnonymous();
        String          sNmsPrefix     = null;
        String          sElement       = null;

        if (!fAnonymous)
            {
            sNmsPrefix = adapterElement.getNamespacePrefix();
            sElement   = XmlHelper.getUniversalName(getElementName(), sNmsPrefix);
            }

        List list = xml.getElementList();
        for (Iterator iter = ((Collection) o).iterator(); iter.hasNext();)
            {
            Object oElement = iter.next();

            // the following is almost identical to a call
            //     adapterElement.writeXml(xml, oElement);
            // except that the element name could be different
            
            XmlElement xmlElement = adapterElement.toXml(oElement);
            if (xmlElement != null)
                {
                if (fAnonymous)
                    {
                    List listElement = xmlElement.getElementList();
                    int  cElements   = listElement.size();

                    if (cElements == 1)
                        {
                        list.add(listElement.get(0));
                        }
                    else if (cElements > 1)
                        {
                        throw new IllegalStateException("Too many elements: " + xmlElement + 
                            "\nadapter=" + adapterElement);
                        }
                    }
                else
                    {
                    xmlElement.setName(sElement);
                    list.add(xmlElement);
                    }
                }
            }

        /*
        if (sNmsPrefix != null)
            {
            XmlHelper.ensureNamespace(xml, sNmsPrefix, adapterElement.getNamespaceUri());
            XmlHelper.purgeChildrenNamespace(xml);
            }
        */
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
        Collection      collection = instantiateCollection();
        PropertyAdapter adapter    = getElementAdapter();

        // "in" contains a collection size and, for each element, a non-null
        // indicator and (if non-null) the object
        for (int i = 0, c = readInt(in); i < c; ++i)
            {
            collection.add(in.readBoolean() ? adapter.readExternal(in) : null);
            }
        return collection;
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
        Collection      collection = (Collection) o;
        PropertyAdapter adapter    = getElementAdapter();

        int c = collection.size();
        writeInt(out, c);

        int cActual = 0;
        for (Iterator iter = collection.iterator(); iter.hasNext(); )
            {
            Object  oElement = iter.next();
            boolean fExists  = (oElement != null);
            out.writeBoolean(fExists);
            if (fExists)
                {
                adapter.writeExternal(out, oElement);
                }
            ++cActual;
            }
        if (c != cActual)
            {
            throw new IOException("expected " + c + " elements, but wrote " + cActual);
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
        return o == null || isEmptyIsNull() && ((Collection) o).isEmpty();
        }

    // ----- internal helpers -----------------------------------------------

    /**
    * @return a PropertyAdapter for collection elements
    */
    public PropertyAdapter getElementAdapter()
        {
        return m_adapterElement;
        }

    /**
    * @return a new Collection instance
    */
    protected Collection instantiateCollection()
        {
        try
            {
            return (Collection) m_clzCollection.newInstance();
            }
        catch (Throwable e)
            {
            throw ensureRuntimeException(e);
            }
        }


    // ----- data members ---------------------------------------------------

    private Class           m_clzCollection;
    private PropertyAdapter m_adapterElement;
    }
