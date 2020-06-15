/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.run.xml;


import com.tangosol.util.Binary;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;


/**
* A SerializableAdapter supports Java objects that implement the 
* Serializable interface.
*
* @version 1.00  2001.03.13
* @author cp
*/
public class SerializableAdapter
        extends PropertyAdapter
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a SerializableAdapter.
    *
    * @param infoBean BeanInfo for a bean containing this property
    * @param clzType  the type of the property
    * @param sName    the property name
    * @param sXml     the XML tag name
    * @param xml      additional XML information
    */
    public SerializableAdapter(XmlBean.BeanInfo infoBean, Class clzType, String sName, String sXml, XmlElement xml)
        {
        super(infoBean, clzType, sName, sXml, xml);
        azzert(Serializable.class.isAssignableFrom(clzType));

        if (isAnonymous())
            {
            throw new IllegalStateException("Element for Serializable type cannot be anonymous: " + this);
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
        if (o != null && isCloneRequired() && getCloner() == null)
            {
            return fromByteArray(toByteArray(o));
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
        Binary bin = xml.getBinary();
        if (bin == null || bin.length() == 0)
            {
            return null;
            }

        return fromBinary(bin);
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

        return new SimpleElement(getXmlName(), toBinary(o));
        }


    // ----- UriSerializable helpers ----------------------------------------

    /**
    * Deserialize an object from a URI element.
    *
    * @param sUri  the URI element to deserialize from
    *
    * @return the object deserialized from the URI element
    *
    * @exception UnsupportedOperationException  if the property cannot be
    *            read from a URI element
    */
    public Object fromUri(String sUri)
        {
        return fromByteArray(parseHex(sUri));
        }

    /**
    * Serialize an object into a URI element.
    *
    * @param o  the object to serialize
    *
    * @return the URI element representing the serialized form of the
    *         passed object
    *
    * @exception UnsupportedOperationException  if the property cannot be
    *            written to a URI element
    */
    public String toUri(Object o)
        {
        return toHex(toByteArray(o));
        }


    // ----- ExternalizableLite helpers -------------------------------------

    /**
    * Read a value from the passed DataInput object.
    *
    * @param in  the DataInput stream to read property data from
    *
    * @return   the data read from the DataInput; may be null
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
