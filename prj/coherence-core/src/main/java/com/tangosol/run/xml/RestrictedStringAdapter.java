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

import java.lang.reflect.Method;


/**
* A property adapter for a String based type that is known to have
* a converting factory method with the following signature:
* <code>public static &lt;type&gt; valueOf(String s)</code>. This approach
* is usually used by the simple types that use derivation by restriction.
*
* @see <a href="http://www.w3.org/TR/2001/REC-xmlschema-2-20010502/#derivation-by-restriction">
* http://www.w3.org/TR/2001/REC-xmlschema-2-20010502/#derivation-by-restriction</a>
*
* @version 1.00  2002.05.21
* @author gg
*/
public class RestrictedStringAdapter
        extends SimpleAdapter
    {
    // ----- constructors ------------------------------------------

    /**
    * Construct a RestrictedStringAdapter.
    *
    * @param infoBean BeanInfo for a bean containing this property
    * @param clzType  the type of the property
    * @param sName    the property name
    * @param sXml     the XML tag name
    * @param xml      additional XML information
    */
    public RestrictedStringAdapter(XmlBean.BeanInfo infoBean, Class clzType, String sName, String sXml, XmlElement xml)
        {
        super(infoBean, clzType, sName, sXml, xml);

        String sMethodName = xml.getSafeElement("factory-method").getString("valueOf");
        try
            {
            m_methodFactory = getType().getMethod(sMethodName, String.class);
            }
        catch (Throwable e)
            {
            throw ensureRuntimeException(e);
            }
        }


    // ----- accessors ---------------------------------------------

    /**
    * @return true if the property value must be "deep" cloned when the
    *         containing object is cloned
    */
    public boolean isCloneRequired()
        {
        return false;
        }


    // ----- XmlSerializable helpers -------------------------------

    /**
    * Deserialize an object from an XML element.
    *
    * @param xml  the XML element to deserialize from
    *
    * @return the object deserialized from the XML element
    */
    public Object fromXml(XmlElement xml)
        {
        String s = xml.getString(null);
        return s == null ? null : instantiateBean(s);
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

        return new SimpleElement(getXmlName(), o.toString());
        }


    // ----- UriSerializable helpers -------------------------------

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
        return instantiateBean(SimpleAdapter.decodeString(sUri));
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
        return o == null ? null : SimpleAdapter.encodeString(o.toString());
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
        return instantiateBean(readUTF(in));
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
        writeUTF(out, o.toString());
        }


    // ----- internal helpers -----------------------------------------------

    /**
    * Convert a String into an instance of the bean.
    *
    * @param s  a String value to be passed to the factory method
    *
    * @return an instance of the bean (property value)
    */
    protected Object instantiateBean(String s)
        {
        try
            {
            return m_methodFactory.invoke(null, s);
            }
        catch (Exception e)
            {
            return null;
            }
        }

    // ----- data fields ----------------------------------------------------

    /**
    * The factory method.
    */
    protected Method m_methodFactory;
    }
