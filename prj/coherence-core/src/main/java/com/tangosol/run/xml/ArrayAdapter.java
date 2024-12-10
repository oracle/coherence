/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.run.xml;


import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.lang.reflect.Array;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;


/**
* An ArrayAdapter supports properties of Java array types (not including
* arrays of primitive types).
*
* <pre>{@code
* &lt;property&gt;
*   &lt;name&gt;People&lt;/name&gt;
*   &lt;xml-name&gt;people&lt;/xml-name&gt;         &lt;!-- defaults to &lt;name&gt; --&gt;
*   &lt;type&gt;com...Person[]&lt;/type&gt;         &lt;!-- defaults via reflection --&gt;
*   &lt;class&gt;com...Person[]&lt;/class&gt;       &lt;!-- defaults from &lt;type&gt; --&gt;
*   &lt;sparse&gt;true&lt;/sparse&gt;               &lt;!-- defaults to false --&gt;
*   &lt;empty-is-null&gt;true&lt;/empty-is-null&gt; &lt;!-- defaults to false --&gt;
*   &lt;element&gt;                           &lt;!-- optional --&gt;
*     &lt;xml-name&gt;person&lt;/xml-name&gt;       &lt;!-- optional, nests the elements --&gt;
*     &lt;type&gt;com...Person&lt;/type&gt;         &lt;!-- auto-set from &lt;property&gt;&lt;class&gt; --&gt;
*     &lt;adapter&gt;...&lt;/adapter&gt;            &lt;!-- optional --&gt;
*     &lt;...&gt;                             &lt;!-- for the type-specific adapter --&gt;
*   &lt;/element&gt;
* &lt;/property&gt;
* }</pre>
*
* Example of collection nested within collection tags:
*
* <pre>{@code
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
* }</pre>
*
* @version 1.00  2001.03.18
* @author cp
*/
public class ArrayAdapter
        extends IterableAdapter
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a ArrayAdapter.
    *
    * @param infoBean BeanInfo for a bean containing this property
    * @param clzType  the type of the property
    * @param sName    the property name
    * @param sXml     the XML tag name
    * @param xml      additional XML information
    */
    public ArrayAdapter(XmlBean.BeanInfo infoBean, Class clzType, String sName, String sXml, XmlElement xml)
        {
        super(infoBean, clzType, sName, sXml, xml);
        azzert(Object[].class.isAssignableFrom(clzType));

        // determine the array class used
        Class    clzArray = getType();
        XmlValue xmlClz   = xml.getElement("class");
        if (xmlClz != null)
            {
            clzArray = infoBean.resolveClass(xmlClz.getString());
            }

        // determine the element type used
        Class clzElement = clzArray.getComponentType();
        azzert(clzElement != null);
        m_clzElement = clzElement;

        XmlElement xmlElement = xml.ensureElement("element");

        // copy type to the type of the element
        xmlElement.ensureElement("type").setString(clzElement.getName());

        m_adapterElement = findAdapter(infoBean, xmlElement);
        }


    // ----- Object method helpers ------------------------------------------

    /**
    * compute a hash code for the passed object.
    *
    * @param o  the object to compute a hash code for
    *
    * @return an integer hash code
    */
    public int hash(Object o)
        {
        Object[] ao = (Object[]) o;
        if (ao == null || ao.length == 0)
            {
            return 0;
            }

        int n = 0;
        PropertyAdapter adapterElement = m_adapterElement;
        for (int i = 0, c = ao.length; i < c; ++i)
            {
            Object oElement = ao[i];
            if (oElement != null)
                {
                n ^= adapterElement.hash(oElement);
                }
            }
        return n;
        }

    /**
    * Compare the two passed objects for equality.
    *
    * @param o1  the first object
    * @param o2  the second object
    *
    * @return true if the two objects are equal
    */
    public boolean equalsValue(Object o1, Object o2)
        {
        if (o1 == o2)
            {
            return true;
            }

        if (o1 == null || o2 == null)
            {
            return false;
            }

        Object[] ao1 = (Object[]) o1;
        Object[] ao2 = (Object[]) o2;
        if (ao1.length != ao2.length)
            {
            return false;
            }

        PropertyAdapter adapterElement = m_adapterElement;
        for (int i = 0, c = ao1.length; i < c; ++i)
            {
            Object oElement1 = ao1[i];
            Object oElement2 = ao2[i];
            if (!adapterElement.equalsValue(oElement1, oElement2))
                {
                return false;
                }
            }

        return true;
        }

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

        Object[] aoOld = (Object[]) o;
        int      c     = aoOld.length;
        if (c == 0)
            {
            return aoOld;
            }

        Object[] aoNew = (Object[]) aoOld.clone();
        PropertyAdapter adapterElement = m_adapterElement;
        for (int i = 0; i < c; ++i)
            {
            Object oOld = aoNew[i];
            if (oOld != null)
                {
                aoNew[i] = adapterElement.clone(oOld);
                }
            }
        return aoNew;
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
        PropertyAdapter adapterElement = m_adapterElement;

        Iterator iter = adapterElement.isAnonymous() ?
            xml.getElementList().iterator() :
            XmlHelper.getElements(xml, getElementName(), adapterElement.getNamespaceUri());

        if (isSparse())
            {
            int c = xml.getSafeAttribute("length").getInt();
            Object[] ao = (Object[]) Array.newInstance(m_clzElement, c);

            while (iter.hasNext())
                {
                XmlElement xmlElement = (XmlElement) iter.next();
                Object     oElement   = adapterElement.fromXml(xmlElement);
                XmlValue   attrId     = xmlElement.getAttribute("id");
                if (attrId == null)
                    {
                    throw new IllegalArgumentException("Element " + xmlElement.getName()
                            + " is missing the required \"id\" attribute");
                    }
                ao[attrId.getInt(-1)] = oElement;
                }
            return ao;
            }
        else
            {
            List list = new ArrayList();

            while (iter.hasNext())
                {
                XmlElement xmlElement = (XmlElement) iter.next();
                Object     oElement   = adapterElement.fromXml(xmlElement);
                list.add(oElement);
                }

            return list.isEmpty() ? null :
                list.toArray((Object[]) Array.newInstance(m_clzElement, list.size()));
            }
        }

    /**
    * @param xml  the XML element to which the iterable elements are written
    * @param o    the object to serialize (not null)
    */
    protected void writeElements(XmlElement xml, Object o)
        {
        PropertyAdapter adapterElement = m_adapterElement;
        boolean         fAnonymous     = adapterElement.isAnonymous();
        String          sNmsPrefix     = null;
        String          sElement       = null;

        if (!fAnonymous)
            {
            sNmsPrefix = adapterElement.getNamespacePrefix();
            sElement   = XmlHelper.getUniversalName(getElementName(), sNmsPrefix);
            }

        Object[] ao      = (Object[]) o;
        int      c       = ao.length;
        boolean  fSparse = isSparse();
        if (fSparse)
            {
            xml.addAttribute("length").setInt(c);
            }

        List list = xml.getElementList();
        for (int i = 0; i < c; ++i)
            {
            Object oElement = ao[i];
            if (oElement == null)
                {
                if (!fSparse && !fAnonymous)
                    {
                    // add a place-holder for the null element
                    xml.addElement(sElement);
                    }
                }
            else
                {
                XmlElement xmlElement = adapterElement.toXml(oElement);

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
                    if (fSparse)
                        {
                        xmlElement.addAttribute("id").setInt(i);
                        }
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
        // "in" contains an array size and, for each element, a non-null
        // indicator and (if non-null) the object
        int      c  = readInt(in);
        Object[] ao = c < CHUNK_THRESHOLD >> 4
                ? readArray(in, c)
                : readLargeArray(in, c);

        return ao;
        }

    /**
     * Read an array of specified length from the passed DataInput object.
     *
     * @param in  the DataInput stream to read property data from
     *
     * @return   the data read from the DataInput; never null
     *
     * @exception IOException   if an I/O exception occurs
     */
    protected  Object[] readArray(DataInput in, int c)
            throws IOException
        {
        Object[] ao = (Object[]) Array.newInstance(m_clzElement, c);

        PropertyAdapter adapter = m_adapterElement;
        for (int i = 0; i < c; ++i)
            {
            if (in.readBoolean())
                {
                ao[i] = adapter.readExternal(in);
                }
            }
        return ao;
        }

    /**
     * Read an array of property data with length larger than
     * {@link #CHUNK_THRESHOLD} {@literal >>} 4.
     *
     * @param in  the DataInput stream to read property data from
     *
     * @return   the data read from the DataInput; never null
     *
     * @exception IOException   if an I/O exception occurs
     */
    protected  Object[] readLargeArray(DataInput in, int c)
            throws IOException
        {
        int      cBatchMax = CHUNK_SIZE >> 4;
        int      cBatch    = c / cBatchMax + 1;
        Object[] aMerged   = null;
        int      cRead     = 0;
        int      cAllocate = cBatchMax;

        Object[] ao;
        for (int i = 0; i < cBatch && cRead < c; i++)
            {
            ao      = readArray(in, cAllocate);
            aMerged = mergeArray(aMerged, ao);
            cRead  += ao.length;

            cAllocate = Math.min(c - cRead, cBatchMax);
            }

        return aMerged;
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
        Object[] ao = (Object[]) o;
        int      c  = ao.length;
        writeInt(out, c);

        PropertyAdapter adapter = m_adapterElement;
        for (int i = 0; i < c; ++i)
            {
            Object  oElement = ao[i];
            boolean fExists  = (oElement != null);
            out.writeBoolean(fExists);
            if (fExists)
                {
                adapter.writeExternal(out, oElement);
                }
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
        return o == null || isEmptyIsNull() && ((Object[]) o).length == 0;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The element type of the array.
    */
    private Class           m_clzElement;

    /**
    * The adapter for the elements in the array.
    */
    private PropertyAdapter m_adapterElement;
    }
