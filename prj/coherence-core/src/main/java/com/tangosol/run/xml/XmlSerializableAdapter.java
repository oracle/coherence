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


/**
* An XmlSerializableAdapter supports Java objects that implement the
* XmlSerializable interface.
*
* @version 1.00  2001.03.13
* @author cp
*/
public class XmlSerializableAdapter
        extends PropertyAdapter
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct an XmlSerializableAdapter.
    *
    * @param infoBean BeanInfo for a bean containing this property
    * @param clzType  the type of the property
    * @param sName    the property name
    * @param sXml     the XML tag name
    * @param xml      additional XML information
    */
    public XmlSerializableAdapter(XmlBean.BeanInfo infoBean, Class clzType, String sName, String sXml, XmlElement xml)
        {
        super(infoBean, clzType, sName, sXml, xml);
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
        if (o != null && isCloneRequired())
            {
            if (o instanceof XmlBean)
                {
                return ((XmlBean) o).clone();
                }
            else if (getCloner() == null)
                {
                return fromXml(toXml(o));
                }
            }

        return super.clone(o);
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
        // create a new instance and configure it using the XML
        XmlSerializable xs;
        try
            {
            // find the JavaBean class for the property's value (it can
            // be over-ridden by the "class" attribute in the XML)
            Class clz;
            XmlValue xmlAttr = xml.getAttribute("class");
            if (xmlAttr == null)
                {
                clz = getType();
                }
            else
                {
                ClassLoader loader = getBeanInfo().getType().getClassLoader();
                if (loader == null)
                    {
                    loader = getContextClassLoader();
                    }
                clz = Class.forName(xmlAttr.getString(), false, loader);
                }
            xs = (XmlSerializable) clz.newInstance();
            }
        catch (Exception e)
            {
            throw ensureRuntimeException(e);
            }
        xs.fromXml(xml);
        return xs;
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
        if (o == null)
            {
            return null;
            }

        XmlSerializable xs  = (XmlSerializable) o;
        XmlElement      xml = xs.toXml();

        if (!isAnonymous())
            {
            xml.setName(getXmlName());
            }
        if (o.getClass() != getType())
            {
            xml.addAttribute("class").setString(o.getClass().getName());
            }
        return xml;
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
        return readObject(in, getBeanInfo().getType().getClassLoader());
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
        writeObject(out, o);
        }
    }
