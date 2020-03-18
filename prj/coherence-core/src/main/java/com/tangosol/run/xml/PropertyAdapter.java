/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.run.xml;


import com.tangosol.util.ClassHelper;
import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import java.util.List;
import java.util.Map;
import java.util.Iterator;


/**
* The PropertyAdapter is the base class for handling the operations that
* can occur on any property.
*
* <pre>{@code
* &lt;property&gt;
*   &lt;name&gt;People&lt;/name&gt;
*   &lt;xml-name&gt;people&lt;/xml-name&gt;     &lt;!-- optional, empty name indicates anonymous element --&gt;
*   &lt;adapter&gt;...&lt;/adapter&gt;          &lt;!-- optional --&gt;
*   &lt;type&gt;...&lt;/type&gt;                &lt;!-- defaults via reflection --&gt;
*   &lt;class&gt;...&lt;/class&gt;              &lt;!-- defaults to &lt;type&gt; --&gt;
* &lt;property&gt;
* }</pre>
*
* @version 1.00  2001.03.06
* @author cp
*/
public abstract class PropertyAdapter
        extends ExternalizableHelper
        implements Serializable
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a PropertyAdapter.
    * <p>
    * <b>Note:</b> This constructor may narrow the specified property type to
    * match the declared property accessor return type; therefore, subclasses
    * should not assume that the specified property type will be equal to that
    * returned by the {@link #getType()} method.
    *
    * @param infoBean BeanInfo for a bean containing this property
    * @param clzType  the type of the property
    * @param sName    the property name
    * @param sXml     the XML tag name
    * @param xml      additional XML information
    */
    protected PropertyAdapter(XmlBean.BeanInfo infoBean, Class clzType, String sName, String sXml, XmlElement xml)
        {
        m_infoBean   = infoBean;
        m_sName      = sName;
        m_clzType    = clzType;
        m_sXml       = sXml == null || sXml.length() == 0 ? null : sXml;
        m_fAttribute = "attribute".equals(xml.getSafeAttribute("type").getString());

        XmlElement xmlNms = xml.getElement("xmlns");
        if (xmlNms != null)
            {
            m_sNmsUri    = xmlNms.getSafeElement("uri")   .getString(null);
            m_sNmsPrefix = xmlNms.getSafeElement("prefix").getString(null);
            }
        else
            {
            m_sNmsUri    = infoBean.getNamespaceUri();
            m_sNmsPrefix = infoBean.getNamespacePrefix();
            }

        if (sName != null)
            {
            Class clzBean = infoBean.getType();

            try
                {
                // find the accessor and mutator
                // note: the current implementation requires these methods to
                // be public; it is possible in the Sun JVM 1.3 and later to
                // legally access even private members with the correct
                // implementation and policy
                Method methodGet = null;
                if (clzType == Boolean.TYPE)
                    {
                    try
                        {
                        methodGet = clzBean.getMethod("is" + sName, NOPARAMS);
                        }
                    catch (NoSuchMethodException e)
                        {
                        }
                    }
                if (methodGet == null)
                    {
                    try
                        {
                        methodGet = clzBean.getMethod("get" + sName, NOPARAMS);
                        }
                    catch (NoSuchMethodException e)
                        {
                        throw new RuntimeException("Unable to find method "
                            + (clzType == Boolean.TYPE ? "is" : "get") + sName + "()");
                        }
                    }
                m_methodGet = methodGet;

                // make sure the specified property type is assignment
                // compatible with the accessor return type; if the property
                // type is a superclass of the accessor return type,
                // reinitialize the property type to that of the accessor
                Class clzGet = m_methodGet.getReturnType();
                if (clzType.equals(clzGet))
                    {
                    // expected case
                    }
                else if (clzType.isAssignableFrom(clzGet))
                    {
                    m_clzType = clzType = clzGet;
                    }
                else
                    {
                    throw new RuntimeException("The property \""
                            + sName
                            + "\" was specified to be of type \""
                            + clzType.getName()
                            + "\" which is not assignment compatible with the "
                            + "return type (\""
                            + clzGet.getName()
                            + "\") of its accessor.");
                    }

                try
                    {
                    m_methodSet = clzBean.getMethod("set" + sName, new Class[] {clzType});
                    }
                catch (NoSuchMethodException e)
                    {
                    throw new RuntimeException("Unable to find method set" + sName
                            + '(' + clzType.getName() + ')');
                    }

                // check for a public clone method
                try
                    {
                    Method methodClone = clzType.getMethod("clone", NOPARAMS);
                    if (methodClone != null && methodClone.getReturnType() == Object.class)
                        {
                        int nModifiers = methodClone.getModifiers();
                        if (Modifier.isPublic(nModifiers) && !Modifier.isStatic(nModifiers))
                            {
                            m_methodClone = methodClone;
                            }
                        }
                    }
                catch (NoSuchMethodException e)
                    {
                    }
                }
            catch (Exception e)
                {
                throw ensureRuntimeException(e);
                }
            }
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Obtain the containing BeanInfo object.
    *
    * @return the BeanInfo that describes an XML bean containing this property
    */
    public XmlBean.BeanInfo getBeanInfo()
        {
        return m_infoBean;
        }

    /**
    * Obtain the name of the property that this PropertyAdapter handles.
    *
    * @return the property name
    */
    public String getName()
        {
        return m_sName;
        }

    /**
    * Determine the "local" XML element name that will be used to hold the
    * property value.
    *
    * @return the local XML element name for the property value
    */
    public String getLocalXmlName()
        {
        String sXml = m_sXml;
        return sXml == null ? "anonymous" : sXml;
        }

    /**
    * Return the XML element name used to generate an XML.
    *
    * @return the outgoing XML element name for the property value
    */
    public String getXmlName()
        {
        return XmlHelper.getUniversalName(getLocalXmlName(), getNamespacePrefix());
        }

    /**
    * Check whether or not the passed element matches to the property's local
    * name and Namespace URI.
    *
    * @param xml  the XML element
    * 
    * @return true if the specified element represents a value for a property
    *         represented by this adapter
    */
    public boolean isElementMatch(XmlElement xml)
        {
        return XmlHelper.isElementMatch(xml, getLocalXmlName(), getNamespaceUri());
        }

    /**
    * Find a child element of the specified XmlElement that represents
    * a value for a property represented by this adapter.
    *
    * @param xml  the parent XmlElement
    *
    * @return an element that can be processed by this adapther;
    *         null if none could be found
    */
    public XmlElement findElement(XmlElement xml)
        {
        return XmlHelper.getElement(xml, getLocalXmlName(), getNamespaceUri());
        }

    /**
    * Get an iterator of child elements of the specified XmlElement
    * that represent values for a property represented by this adapter.
    *
    * @param xml  the parent XmlElement
    *
    * @return an iterator containing all child elements
    *         that can be processed by this adapther
    */
    public Iterator getElements(XmlElement xml)
        {
        return XmlHelper.getElements(xml, getLocalXmlName(), getNamespaceUri());
        }

    /**
    * Find an attribute of the specified XmlElement that represents
    * a value for a property represented by this adapter.
    *
    * @param xml  the parent XmlElement
    *
    * @return an attribute that can be processed by this adapther;
    *         null if none could be found
    */
    public XmlValue findAttribute(XmlElement xml)
        {
        return XmlHelper.getAttribute(xml, getLocalXmlName(), getNamespaceUri());
        }

    /**
    * Determine the namespace URI for the property.
    *
    * @return the URI that qualifies the Namespace for this property
    */
    public String getNamespaceUri()
        {
        return m_sNmsUri;
        }

    /**
    * Determine the default namespace prefix for the property.
    *
    * @return the default Namespace prefix for this property
    */
    public String getNamespacePrefix()
        {
        return m_sNmsPrefix;
        }

    /**
    * Set the default Namespace prefix for this property.
    *
    * @param sPrefix  the new default namespace for this property
    */
    public void setNamespacePrefix(String sPrefix)
        {
        m_sNmsPrefix = sPrefix;
        }

    /**
    * Obtain the type of the property (the class of the object declared as
    * being returned by the accessor and passed to the mutator).
    *
    * @return the property type
    */
    public Class getType()
        {
        return m_clzType;
        }

    /**
    * Obtain the Method object that is used to call the property accessor.
    *
    * @return the accessor (getter) method for the property
    */
    public Method getAccessor()
        {
        return m_methodGet;
        }

    /**
    * Obtain the Method object that is used to call the property mutator.
    *
    * @return the mutator (setter) method for the property or null if the
    *         property is not settable
    */
    public Method getMutator()
        {
        return m_methodSet;
        }

    /**
    * Obtain the Method object that is used to deep-clone the property value.
    *
    * @return the clone method for the property or null if none is applicable
    *         or available
    */
    public Method getCloner()
        {
        return m_methodClone;
        }

    /**
    * Determine if this property does not have a designated element name.
    *
    * @return true if the property doesn't have an associated XmlElement
    */
    public boolean isAnonymous()
        {
        return m_sXml == null;
        }

    /**
    * Determine if the property is stored in an XML attribute instead of an
    * XML element.
    *
    * @return true if the property is attribute bound
    */
    public boolean isAttribute()
        {
        return m_fAttribute;
        }

    /**
    * Determine if the property value must be deep-cloned. Typically, a
    * property value must be deep-cloned if it is a mutable reference type,
    * e.g. StringBuffer, Date, byte[].
    *
    * @return true if the property value must be "deep" cloned when the
    *         containing object is cloned
    */
    public abstract boolean isCloneRequired();


    // ----- property accessor helpers --------------------------------------

    /**
    * Extract the property value from the passed bean reference.
    *
    * @param bean  the XML bean object to obtain the property value from
    *
    * @return the property value
    */
    public Object get(XmlBean bean)
        {
        try
            {
            return getAccessor().invoke(bean);
            }
        catch (Exception e)
            {
            throw ensureRuntimeException(e, "error invoking get" + getName() + "()");
            }
        }

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
        return o == null;
        }

    /**
    * Store the property value in the passed bean.
    *
    * @param bean  the XML bean object to store the property value into
    * @param o     the property value
    */
    public void set(XmlBean bean, Object o)
        {
        try
            {
            getMutator().invoke(bean, new Object[] {o});
            }
        catch (Exception e)
            {
            throw ensureRuntimeException(e, "error invoking set" + getName() + "()");
            }
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
        return o == null ? 0 : o.hashCode();
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
            // we already know that (o1 != o2)
            return false;
            }

        try
            {
            return o1.equals(o2);
            }
        catch (RuntimeException e)
            {
            return false;
            }
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

        if (!isCloneRequired())
            {
            return o;
            }

        Method methodClone = getCloner();
        if (methodClone != null)
            {
            try
                {
                return methodClone.invoke(o);
                }
            catch (Exception e)
                {
                throw ensureRuntimeException(e);
                }
            }

        throw new UnsupportedOperationException(getClass().getName());
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
        throw new UnsupportedOperationException(getClass().getName());
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
        throw new UnsupportedOperationException(getClass().getName());
        }

    /**
    * Deserialize an object from XML.  Note that the parent element
    * is the one passed to this method; this method is responsible for
    * finding all of the necessarily elements within the parent element.
    * This method is intended to allow collection properties to read
    * their data from multiple XML elements.
    *
    * @param xml  the XML element containing the XML elements to deserialize
    *             from
    *
    * @return the object deserialized from the XML (may be null)
    */
    public Object readXml(XmlElement xml)
        {
        if (isAnonymous())
            {
            return fromXml(xml);
            }
        else
            {
            if (isAttribute())
                {
                // TODO: use dedicated API or replace fromXml with
                // Object fromXml(XmlValue)

                XmlValue xmlValue = findAttribute(xml);
                if (xmlValue == null)
                    {
                    return null;
                    }

                XmlElement xmlStub = new SimpleElement("attribute");
                xmlStub.setString(xmlValue.getString());

                // plug the dummy element "attribute" into the xml tree
                // to preserve a Namespace context
                List list = xml.getElementList();
                list.add(xmlStub);
                try
                    {
                    return fromXml(xmlStub);
                    }
                finally
                    {
                    list.remove(xmlStub);
                    }
                }
            else
                {
                XmlElement xmlValue = findElement(xml);
                return xmlValue == null ? null : fromXml(xmlValue);
                }
            }
        }

    /**
    * Serialize an object into an XML element.  Note that the parent element
    * is the one passed to this method; this method is responsible for
    * creating the necessarily elements within the parent element.
    * This method is intended to allow collection properties to write
    * their data to multiple XML elements.
    *
    * @param xml  the XML element containing the XML elements to serialize to
    * @param o    the object to serialize (may be null)
    */
    public void writeXml(XmlElement xml, Object o)
        {
        if (!isEmpty(o))
            {
            XmlElement xmlValue = toXml(o);
            if (isAnonymous())
                {
                if (xmlValue.getValue() != null)
                    {
                    xml.setString(xmlValue.getString());
                    }
                xml.getElementList().addAll(xmlValue.getElementList());
                }
            else
                {
                if (isAttribute())
                    {
                    // TODO: use dedicated API or replace
                    // "XmlElement toXml(Object)" with "XmlValue toXml(Object)"
                    xml.setAttribute(getLocalXmlName(), new SimpleValue(xmlValue.getString()));

                    // toXml() could add some namespace decarations that have to be
                    // transfered on the parent xml (see QNameAdapter#toXml)
                    for (Iterator iter = xmlValue.getAttributeMap().entrySet().iterator(); iter.hasNext();)
                        {
                        Map.Entry entry = (Map.Entry) iter.next();

                        String sAttr = (String) entry.getKey();
                        if (sAttr.startsWith("xmlns:"))
                            {
                            String sPrefix = sAttr.substring(6); // "xmlns:".length()
                            String sUri    = ((XmlValue) entry.getValue()).getString();

                            XmlHelper.ensureNamespace(xml, sPrefix, sUri);
                            }
                        }

                    }
                else
                    {
                    xmlValue.setName(getXmlName());
                    xml.getElementList().add(xmlValue);

                    String sNmsPrefix = getNamespacePrefix();
                    if (sNmsPrefix != null)
                        {
                        XmlHelper.ensureNamespace(xml, sNmsPrefix, getNamespaceUri());
                        XmlHelper.purgeChildrenNamespace(xml);
                        }
                    }
                }
            }
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
        throw new UnsupportedOperationException(getClass().getName());
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
        throw new UnsupportedOperationException(getClass().getName());
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
        throw new UnsupportedOperationException(getClass().getName());
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
        throw new UnsupportedOperationException(getClass().getName());
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Debugging support.
    *
    * @return a String description of this PropertyAdapter object
    */
    public String toString()
        {
        StringBuffer sb = new StringBuffer();

        sb.append(ClassHelper.getSimpleName(getClass()))
          .append(" for ")
          .append(getName())
          .append(", type=")
          .append(getType().getName())
          .append(", xml-name=")
          .append(getLocalXmlName())
          .append(", NamespaceUri=")
          .append(getNamespaceUri())
          .append(", NamespacePrefix=")
          .append(getNamespacePrefix())
          .append(", isCloneRequired=")
          .append(isCloneRequired())
          .append(", accessor=")
          .append(getAccessor())
          .append(", mutator=")
          .append(getMutator())
          .append(", cloner=")
          .append(getCloner());

        return sb.toString();
        }


    // ----- constants ------------------------------------------------------

    /**
    * Parameters for finding no-parameter methods.
    */
    protected static final Class[] NOPARAMS = new Class [0];


    // ----- data members ---------------------------------------------------

    /**
    * The BeanInfo object that contains this PropertyAdapter.
    */
    protected XmlBean.BeanInfo m_infoBean;

    /**
    * The property name.
    */
    protected String  m_sName;

    /**
    * The XML element name used to store this property.
    */
    protected String  m_sXml;

    /**
    * True if the property is stored in an attribute instead of an XML
    * element.
    */
    protected boolean m_fAttribute;

    /**
    * The type of the property.
    */
    protected Class   m_clzType;

    /**
    * The property "accessor" method.
    */
    protected Method  m_methodGet;

    /**
    * The property "mutator" method.
    */
    protected Method  m_methodSet;

    /**
    * The "public Object clone()" method for the property type, if available.
    */
    protected Method  m_methodClone;

    /**
    * The namespace URI for this property.
    */
    protected String m_sNmsUri;

    /**
    * The namespace prefix for this property.
    */
    protected String m_sNmsPrefix;
    }
