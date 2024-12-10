/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.run.xml;


import com.tangosol.io.ExternalizableLite;

import com.tangosol.util.ClassHelper;
import com.tangosol.util.SafeHashMap;
import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.Externalizable;
import java.io.IOException;
import java.io.NotActiveException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import java.math.BigDecimal;
import java.math.BigInteger;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;

import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;


/**
* This is a base class for building XmlSerializable value objects.
* <p>
* The following property types are supported using standard property
* adapters:
* <pre>
*   1)  XmlValue types:
*           TYPE_BOOLEAN  - boolean, java.lang.Boolean
*           TYPE_INT      - byte, char, short, int, java.lang.Byte,
*                           java.lang.Character, java.lang.Short,
*                           java.lang.Integer
*           TYPE_LONG     - long, java.lang.Long
*           TYPE_DOUBLE   - float, double, java.lang.Float, java.lang.Double
*           TYPE_DECIMAL  - java.math.BigDecimal, java.math.BigInteger
*           TYPE_STRING   - java.lang.String
*           TYPE_BINARY   - com.tangosol.util.Binary, byte[]
*           TYPE_DATE     - java.sql.Date
*           TYPE_TIME     - java.sql.Time
*           TYPE_DATETIME - java.sql.Timestamp, java.util.Date
*
*   2)  Objects implementing XmlSerializable (including XmlBean subclasses)
*
*   3)  Objects implementing Serializable
*
*   4)  Collections of any of the above:
*           Java arrays
*           java.util.Collection
*           java.util.Set
*           java.util.List
*           java.util.Map
*           java.util.SortedSet
*           java.util.SortedMap
* </pre>
*
* Each XmlBean must have a corresponding XML declaration file that provides
* the necessary information to parse XML into the XML bean and to format the
* XML bean into XML. The declaration file should be located in the same package
* (directory) as the class itself.
* <p>
* For example, here is an XmlBean subclass with an int property "Id" and a
* String property "Name":
* <pre><code>
* public class Person extends XmlBean {
*   public Person(int nId, String sName) {...}
*   public int getId() {...}
*   public void setId(int nId) {...}
*   public String getName() {...}
*   public void setName(String sName) {...}
* }
* </code></pre>
*
* The Person XML bean example above would have an XML declaration file that
* resembles the following:
* <pre><code>
* &lt;xml-bean&gt;
*   &lt;name&gt;person&lt;/name&gt;
*   &lt;property&gt;
*     &lt;name&gt;Id&lt;/name&gt;
*     &lt;xml-name&gt;person-id&lt;/xml-name&gt;
*   &lt;/property&gt;
*   &lt;property&gt;
*     &lt;name&gt;Name&lt;/name&gt;
*     &lt;xml-name&gt;full-name&lt;/xml-name&gt;
*   &lt;/property&gt;
* &lt;/xml-bean&gt;
* </code></pre>
*
* Consider the following code:
* <pre><code>System.out.println(new Person(15, "John Smith").toString());</code></pre>
*
* The output would be:
* <pre><code>
* &lt;person&gt;
*   &lt;person-id&gt;15&lt;/person-id&gt;
*   &lt;full-name&gt;John Smith&lt;/full-name&gt;
* &lt;/person&gt;
* </code></pre>
*
* To specify namespace information for an XML bean, add an "xmlns" element to
* the bean's descriptor:
*
* <pre><code>
* &lt;xml-bean&gt;
*   &lt;name&gt;person&lt;/name&gt;
*   &lt;xmlns&gt;
*     &lt;uri&gt;the-schema-URI-goes-here&lt;/uri&gt;
*     &lt;prefix&gt;the-default-namespace-prefix-goes-here&lt;/prefix&gt;
*   &lt;xmlns&gt;
*   &lt;property&gt;
*     ...
*   &lt;/property&gt;
* &lt;/xml-bean&gt;
* </code></pre>
*
* @version 1.2
*
* @author cp 2000.11.10
* @author gg 2002.05.17 anonymous element and XML Namespaces support
* @author cp 2003.03.27 ExternalizableLite support
*/
public abstract class XmlBean
        extends ExternalizableHelper
        implements Cloneable, Externalizable, ExternalizableLite, XmlSerializable
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a value object.
    */
    protected XmlBean()
        {
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Obtain the XmlBean that contains this XmlBean.
    *
    * @return the containing XmlBean, or null if there is none
    */
    public XmlBean getParentXmlBean()
        {
        return m_parent;
        }

    /**
    * Specify the XmlBean that contains this XmlBean.
     *
    * @param parent  the XmlBean that contains this XmlBean
    */
    protected void setParentXmlBean(XmlBean parent)
        {
        XmlBean parentOrig = m_parent;
        if (parentOrig != null && parentOrig != parent)
            {
            throw new IllegalStateException("ParentXmlBean is immutable.");
            }
        m_parent = parent;
        }

    /**
    * Helper to adopt a Map of XmlBean objects.
    *
    * @param map  a Map that may contain keys and/or values that are XmlBeans
    */
    protected void adopt(Map map)
        {
        if (map != null && !map.isEmpty())
            {
            adopt(map.keySet());
            adopt(map.entrySet());
            }
        }

    /**
    * Helper to adopt a Collection of XmlBean objects.
    *
    * @param coll  a Collection that may contain XmlBeans
    */
    protected void adopt(Collection coll)
        {
        if (coll != null && !coll.isEmpty())
            {
            adopt(coll.iterator());
            }
        }

    /**
    * Helper to adopt a collection of XmlBean objects.
    *
    * @param iter  an Iterator that may contain XmlBeans
    */
    protected void adopt(Iterator iter)
        {
        if (iter != null)
            {
            while (iter.hasNext())
                {
                Object o = iter.next();
                if (o instanceof XmlBean)
                    {
                    adopt((XmlBean) o);
                    }
                }
            }
        }

    /**
    * Helper to adopt a collection of XmlBean objects.
    *
    * @param ao  an array that may contain XmlBeans
    */
    protected void adopt(Object[] ao)
        {
        if (ao != null)
            {
            for (int i = 0, c = ao.length; i < c; ++i)
                {
                Object o = ao[i];
                if (o instanceof XmlBean)
                    {
                    adopt((XmlBean) o);
                    }
                }
            }
        }

    /**
    * When an XmlBean adds a contained XmlBean, it should invoke this method
    * to relate the contained XmlBean with this XmlBean.
    *
    * @param child  the XmlBean that is being contained within this XmlBean
    */
    protected void adopt(XmlBean child)
        {
        child.setParentXmlBean(this);
        }

    /**
    * Determine if this value can be modified.  If the value can not be
    * modified, all mutating methods are required to throw an
    * UnsupportedOperationException.
    *
    * @return true if this value can be modified, otherwise false to
    *         indicate that this value is read-only
    */
    public boolean isMutable()
        {
        return m_fMutable;
        }

    /**
    * Specify whether this value can be modified or not.
    *
    * @param  fMutable true to allow this value to be modified, otherwise false
    *         to indicate that this value is read-only
    */
    protected void setMutable(boolean fMutable)
        {
        m_fMutable = fMutable;
        }

    /**
    * Make sure that this XML bean is mutable.
    *
    * @return this XmlBean if it is mutable, otherwise a mutable copy of this
    *         XmlBean
    */
    public XmlBean ensureMutable()
        {
        if (isMutable())
            {
            return this;
            }

        XmlBean that = (XmlBean) this.clone();
        azzert(that.isMutable());
        return that;
        }

    /**
    * Make sure that this value is read-only (immutable).
    */
    public void ensureReadOnly()
        {
        setMutable(false);
        }

    /**
    * Verify that this XmlBean is mutable.  This method is designed to be
    * called by all mutator methods of an XmlBean to ensure that the bean
    * fulfills the contract provided by the Mutable property.
    */
    protected void checkMutable()
        {
        if (!isMutable())
            {
            throw new IllegalStateException(getBeanInfo().getName() +
                    " is immutable");
            }

        // a child XmlBean is immutable if its parent is immutable
        XmlBean parent = getParentXmlBean();
        if (parent != null)
            {
            parent.checkMutable();
            }
        }

    /**
    * Get the cached hash code.  Value objects whose hash code is supposed
    * to change must override the hashCode implementation.
    *
    * @return the cached hash code
    */
    protected int getHashCode()
        {
        return m_nHash;
        }

    /**
    * Set the cached hash code.  Value objects whose hash code is supposed
    * to change must override the hashCode implementation.
    *
    * @param nHash  the hash code
    */
    protected void setHashCode(int nHash)
        {
        m_nHash = nHash;
        }

    /**
    * Obtain the BeanInfo for this XmlBean object, or create and configure
    * a BeanInfo if one does not exist.
    *
    * @return the BeanInfo that describes this XmlBean
    */
    public BeanInfo getBeanInfo()
        {
        BeanInfo info = m_info;

        if (info == null)
            {
            info = findBeanInfo();
            azzert(info != null);
            m_info = info;
            }

        return info;
        }

    /**
    * Obtain the PropertyAdapter objects for this XmlBean.
    *
    * @return the PropertyAdapter objects that handle the properties of
    *         this XmlBean
    */
    public PropertyAdapter[] getAdapters()
        {
        return getBeanInfo().getAdapters();
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Determine if this value object is equal to the passed value object.
    *
    * @param o  the other value object to compare to
    *
    * @return true if the other value object is equal to this
    */
    public boolean equals(Object o)
        {
        // optimization: incompatible class
        if (!(o instanceof XmlBean))
            {
            return false;
            }

        // optimization: same object
        XmlBean that = (XmlBean) o;
        if (this == that)
            {
            return true;
            }

        // optimization: check cached hash codes
        int nThis = this.getHashCode();
        int nThat = that.getHashCode();
        if (nThis != 0 && nThat != 0 && nThis != nThat)
            {
            return false;
            }

        // no optimization available; check each property for inequality
        PropertyAdapter[] aAdapter = getAdapters();
        for (int i = 0, c = aAdapter.length; i < c; ++i)
            {
            PropertyAdapter adapter = aAdapter[i];
            Object oThis = adapter.get(this);
            Object oThat = adapter.get(that);
            if (!adapter.equalsValue(oThis, oThat))
                {
                return false;
                }
            }

        // no inequality found; objects are equal
        return true;
        }

    /**
    * Determine a hash code for this value object.  For value objects with
    * multiple properties, the hash code is calculated from the xor of the
    * hash codes for each property.
    *
    * @return a hash code for this value object
    */
    public int hashCode()
        {
        // check to see if the hashcode is cached
        int n = getHashCode();
        if (n == 0)
            {
            // calculate the hash code as the xor of the hashcode of each
            // property
            PropertyAdapter[] aAdapter = getAdapters();
            for (int i = 0, c = aAdapter.length; i < c; ++i)
                {
                PropertyAdapter adapter = aAdapter[i];
                n ^= adapter.hash(adapter.get(this));
                }

            // don't allow a hash of zero because it would be recalculated
            // on every call
            if (n == 0)
                {
                n = -1;
                }

            // cache the hash code
            setHashCode(n);
            }

        return n;
        }

    /**
    * To assist in debugging, provide a clear indication of the key's
    * state.
    *
    * @return a String representing this key object
    */
    public String toString()
        {
        // default implementation of toString is to convert the XML bean to
        // an XML format and return that as a String
        return XmlHelper.toString(this);
        }

    /**
    * Clone the value object.
    *
    * @return a clone of this object
    */
    public Object clone()
        {
        // start with a shallow clone
        XmlBean that;
        try
            {
            that = (XmlBean) super.clone();
            }
        catch (CloneNotSupportedException e)
            {
            throw ensureRuntimeException(e);
            }

        // the clone is not automatically a child of this XmlBean's parent
        that.m_parent = null;

        that.setMutable(true);

        // clone any properties that need to be deep cloned; if no deep
        // cloning is required, this step is skipped entirely
        BeanInfo info = getBeanInfo();
        if (info.requiresDeepClone())
            {
            PropertyAdapter[] aAdapter = info.getAdapters();
            for (int i = 0, c = aAdapter.length; i < c; ++i)
                {
                PropertyAdapter adapter = aAdapter[i];
                if (adapter.isCloneRequired() && adapter.getMutator() != null)
                    {
                    Object o = adapter.get(this);
                    if (o != null)
                        {
                        adapter.set(that, adapter.clone(o));
                        }
                    }
                }
            }

        return that;
        }


    // ----- XmlSerializable methods ----------------------------------------

    /**
    * Serialize the object into an XmlElement.
    *
    * @return an XmlElement that contains the serialized form of the object
    */
    public XmlElement toXml()
        {
        BeanInfo          info     = getBeanInfo();
        PropertyAdapter[] aAdapter = info.getAdapters();
        String            sName    = info.getName();

        if (sName.length() == 0)
            {
            // the name must be discarded by the caller
            XmlElement xml = new SimpleElement(
                ClassHelper.getSimpleName(info.getType()));

            for (int i = 0, c = aAdapter.length; i < c; ++i)
                {
                PropertyAdapter adapter = aAdapter[i];
                Object o = adapter.get(this);
                if (!adapter.isEmpty(o))
                    {
                    adapter.writeXml(xml, o);

                    // not more then one property for an anonymous [choice] element
                    // (see http://www.w3.org/TR/xmlschema-0/#element-choice)
                    break;
                    }
                }
            return xml;
            }
        else
            {
            String sNmsPrefix = info.getNamespacePrefix();

            XmlElement xml;
            if (sNmsPrefix == null)
                {
                xml = new SimpleElement(sName);
                }
            else
                {
                xml = new SimpleElement(sNmsPrefix + ':' + sName);
                XmlHelper.ensureNamespace(xml, sNmsPrefix, info.getNamespaceUri());
                }

            for (int i = 0, c = aAdapter.length; i < c; ++i)
                {
                PropertyAdapter adapter = aAdapter[i];
                Object o = adapter.get(this);

                // adapters decide whether or not to process null values
                adapter.writeXml(xml, o);
                }

            if (sNmsPrefix != null)
                {
                XmlHelper.purgeChildrenNamespace(xml);
                }
            return xml;
            }
        }

    /**
    * Deserialize the object from an XmlElement.
    *
    * This method can throw one of several RuntimeExceptions.
    *
    * @param xml  an XmlElement that contains the serialized form of the
    *             object
    *
    * @throws UnsupportedOperationException  if the operation is not supported
    * @throws IllegalStateException          if this is not an appropriate state
    * @throws IllegalArgumentException       if there is an illegal argument
    */
    public void fromXml(XmlElement xml)
        {
        BeanInfo          info     = getBeanInfo();
        PropertyAdapter[] aAdapter = info.getAdapters();
        String            sName    = info.getName();

        if (sName.length() == 0)
            {
            // for anonymous elements one and only one adapter may fit
            for (int i = 0, c = aAdapter.length; i < c; ++i)
                {
                PropertyAdapter adapter = aAdapter[i];
                if (adapter.getMutator() != null &&
                        (adapter.isAnonymous() || adapter.isElementMatch(xml)))
                    {
                    XmlElement xmlParent = new SimpleElement(
                        ClassHelper.getSimpleName(getClass()));
                    xmlParent.getElementList().add(xml);

                    // plug the dummy parent into the xml tree
                    // to preserve a Namespace context
                    List list = xml.getElementList();
                    list.add(xmlParent);
                    try
                        {
                        Object o = adapter.readXml(xmlParent);
                        if (o != null)
                            {
                            adapter.set(this, o);
                            }
                        }
                    finally
                        {
                        list.remove(xmlParent);
                        }
                    break;
                    }
                }
            }
        else
            {
            for (int i = 0, c = aAdapter.length; i < c; ++i)
                {
                PropertyAdapter adapter = aAdapter[i];
                if (adapter.getMutator() != null)
                    {
                    Object o = adapter.readXml(xml);

                    if (o != null)
                        {
                        adapter.set(this, o);
                        }
                    }
                }
            }
        }


    // ----- Externalizable interface ---------------------------------------

    /**
    * The object implements the readExternal method to restore its
    * contents by calling the methods of DataInput for primitive
    * types and readObject for objects, strings and arrays.  The
    * readExternal method must read the values in the same sequence
    * and with the same types as were written by writeExternal.
    *
    * @param in  the stream to read data from in order to restore the object
    *
    * @exception IOException            if I/O errors occur
    * @exception ClassNotFoundException if the class for an object being
    *            restored cannot be found.
    */
    public void readExternal(ObjectInput in)
            throws IOException, ClassNotFoundException
        {
        readExternal((DataInput) in);
        }

    /**
    * The object implements the writeExternal method to save its contents
    * by calling the methods of DataOutput for its primitive values or
    * calling the writeObject method of ObjectOutput for objects, strings,
    * and arrays.
    *
    * @serialData Overriding methods should use this tag to describe
    *             the data layout of this Externalizable object.
    *             List the sequence of element types and, if possible,
    *             relate the element to a public/protected field and/or
    *             method of this Externalizable class.
    *
    * @param out             the stream to write the object to
    * @exception IOException includes any I/O exceptions that may occur
    */
    public void writeExternal(ObjectOutput out)
            throws IOException
        {
        writeExternal((DataOutput) out);
        }


    // ----- ExternalizableLite interface -----------------------------------

    /**
    * Restore the contents of this object by loading the object's state from
    * the passed DataInput object.
    *
    * @param in  the DataInput stream to read data from in order to restore
    *            the state of this object
    *
    * @exception IOException         if an I/O exception occurs
    * @exception NotActiveException  if the object is not in its initial
    *            state, and therefore cannot be deserialized into
    */
    public void readExternal(DataInput in)
            throws IOException
        {
        boolean fMutable = in.readBoolean();

        BeanInfo          info     = getBeanInfo();
        PropertyAdapter[] aAdapter = info.getAdapters();

        // the stream "in" is packed with accessor-index/value pairs,
        // and terminated with a -1 index (with no corresponding value)
        int i;
        while ((i = readInt(in)) >= 0)
            {
            PropertyAdapter adapter = aAdapter[i];
            Object          o       = adapter.readExternal(in);
            if (o != null && adapter.getMutator() != null)
                {
                adapter.set(this, o);
                }
            }

        // now that everything is set up, configure the read-only versus
        // mutable attribute of the XML bean
        m_fMutable = fMutable;
        }

    /**
    * Save the contents of this object by storing the object's state into
    * the passed DataOutput object.
    *
    * @param out  the DataOutput stream to write the state of this object to
    *
    * @exception IOException if an I/O exception occurs
    */
    public void writeExternal(DataOutput out)
            throws IOException
        {
        out.writeBoolean(m_fMutable);

        BeanInfo          info     = getBeanInfo();
        PropertyAdapter[] aAdapter = info.getAdapters();
        for (int i = 0, c = aAdapter.length; i < c; ++i)
            {
            PropertyAdapter adapter = aAdapter[i];
            if (adapter.getMutator() != null)   // only save what can be restored
                {
                Object o = adapter.get(this);
                if (!adapter.isEmpty(o))
                    {
                    writeInt(out, i);
                    adapter.writeExternal(out, o);
                    }
                }
            }
        writeInt(out, -1);      // "EOF" aka "end of properties" marker
        }


    // ----- internal -------------------------------------------------------

    /**
    * For backwards compatibility only - loads reflection info.
    *
    * This method is intended to be called by the static initializer of each
    * concrete sub-class.
    *
    * @param clz      the class to initialize
    * @param sName    the name of the value object
    * @param asProp   the property names that make up the value object
    */
    protected static void init(Class clz, String sName, String[] asProp)
        {
        XmlElement xml = new SimpleElement("xml-bean");
        xml.addElement("name").setString(sName);
        for (int i = 0, c = asProp.length; i < c; ++i)
            {
            XmlElement xmlProp = xml.addElement("property");
            String     sProp   = asProp[i];
            xmlProp.addElement("name")    .setString(sProp);
            xmlProp.addElement("xml-name").setString(sProp);
            }
        s_mapXml.put(clz, xml);
        }

    /**
    * Obtain the BeanInfo object for this XML bean.
    *
    * @return the BeanInfo for this Object
    */
    private BeanInfo findBeanInfo()
        {
        Class    clz  = getClass();
        BeanInfo info = (BeanInfo) s_mapInfo.get(clz);
        if (info == null)
            {
            info = initBeanInfo();
            s_mapInfo.put(clz, info);
            }
        return info;
        }

    /**
    * Initialize the Object, loading the XML Bean design information if
    * necessary.
    *
    * @return a BeanInfo object
    */
    protected BeanInfo initBeanInfo()
        {
        Class      clzBean = getClass();
        XmlElement xml     = null;
        Class      clz     = clzBean;

        while (clz != null)
            {
            XmlElement xmlClz = (XmlElement) s_mapXml.get(clz);
            if (xmlClz == null)
                {
                xmlClz = XmlHelper.loadXml(clz);
                }

            if (xml == null)
                {
                xml = xmlClz;
                }
            else if (xmlClz != null)
                {
                // insert the super class properties
                List list = xml.getElementList();
                int  ix   = 0;
                for (Iterator iter = xmlClz.getElements("property"); iter.hasNext();)
                    {
                    XmlElement xmlProp = (XmlElement) iter.next();
                    String     sProp   = xmlProp.getElement("name").getName();

                    // check if a sub-class defined (over-rode) the
                    // property; if so, put it in the order defined by the
                    // superclass by removing it from its current position
                    // and re-inserting it with the other superclass
                    // properties
                    XmlElement xmlSub  = XmlHelper.findElement(xml, "/property/name", sProp);
                    if (xmlSub != null)
                        {
                        xmlProp = xmlSub.getParent();
                        list.remove(xmlProp);
                        }

                    list.add(ix++, xmlProp);
                    }
                }

            clz = clz.getSuperclass();
            }
        azzert(xml != null, "Cannot find bean info for " + clzBean);

        return new BeanInfo(clzBean, xml);
        }

    /**
    * A BeanInfo contains information about the XML bean and its properties.
    * One BeanInfo will be created for each specific class of XmlBean.
    */
    public static class BeanInfo
        {
        /**
        * Construct a BeanInfo.
        *
        * @param clzBean  the class of the bean
        * @param xml      the xml descriptor
        */
        protected BeanInfo(Class clzBean, XmlElement xml)
            {
            // store the class of the specific XML bean
            m_clzBean  = clzBean;

            // determine the XML element name to use for the XML bean
            String sName;
            XmlElement xmlName = xml.getElement("name");
            if (xmlName == null)
                {
                xmlName = xml.getElement("xml-name");
                }
            if (xmlName == null)
                {
                // default:  use class name as XML element name
                sName = ClassHelper.getSimpleName(clzBean);
                }
            else
                {
                sName = xmlName.getString();
                }
            azzert(sName != null);
            m_sName    = sName;

            // determine the XML namespace URI and default prefix, if any
            XmlElement xmlNms  = xml.getElement("xmlns");
            if (xmlNms != null)
                {
                m_sNmsUri    = xmlNms.getSafeElement("uri")   .getString(null);
                m_sNmsPrefix = xmlNms.getSafeElement("prefix").getString(null);
                }

            // obtain a PropertyAdapter for each of the XML beans' properties
            List list = new ArrayList();
            for (Iterator iter = xml.getElements("property"); iter.hasNext(); )
                {
                XmlElement xmlProp = (XmlElement) iter.next();

                String sProp = xmlProp.getSafeElement("name").getString();
                azzert(sProp != null && sProp.length() > 0);

                XmlElement xmlXmlName = xmlProp.getElement("xml-name");
                String     sXmlName   = xmlXmlName == null ?
                        sProp : xmlXmlName.getString();

                Class  clzProp;
                String sClass  = xmlProp.getSafeElement("type").getString();
                if (sClass != null && sClass.length() > 0)
                    {
                    clzProp = resolveClass(sClass);
                    }
                else
                    {
                    // use reflection to find the property's type
                    Method method = null;
                    try
                        {
                        method = clzBean.getMethod("get" + sProp, NOPARAMS);
                        }
                    catch (NoSuchMethodException e)
                        {
                        try
                            {
                            method = clzBean.getMethod("is" + sProp, NOPARAMS);
                            }
                        catch (NoSuchMethodException e2)
                            {
                            }
                        }
                    if (method == null)
                        {
                        throw new RuntimeException("Unable to find accessor for "
                                + sProp + " on " + clzBean.getName());
                        }
                    clzProp = method.getReturnType();
                    }

                list.add(makeAdapter(clzProp, sProp, sXmlName, xmlProp));
                }
            PropertyAdapter[] aAdapter = (PropertyAdapter[])
                    list.toArray(new PropertyAdapter[list.size()]);
            m_aAdapter = aAdapter;

            // determine whether any of the properties causes the XML bean to
            // require "deep" cloning
            for (int i = 0, c = aAdapter.length; i < c; ++i)
                {
                PropertyAdapter adapter = aAdapter[i];
                if (adapter.isCloneRequired() && adapter.getMutator() != null)
                    {
                    m_fDeepClone = true;
                    break;
                    }
                }

            if (USE_XMLBEAN_CLASS_CACHE)
                {
                m_nBeanId = XMLBEAN_CLASS_CACHE.getClassId(clzBean);
                }
            }


        // ----- accessors ----------------------------------------

        /**
        * Get the class of the specific XML bean implementation.
        *
        * @return the type of the XML bean
        */
        public Class getType()
            {
            return m_clzBean;
            }

        /**
        * Get the serialization ID for the specific XML bean implementation.
        *
        * @return the XmlBean ID used by ExternalizableHelper, or -1 if this
        *         XmlBean does not have an ID assigned or if the ID
        *         optimization is not being used
        */
        public int getBeanId()
            {
            return m_nBeanId;
            }

        /**
        * Determine the element name that the XML bean will use when
        * serializing to XML.
        *
        * @return the local XmlElement name for the bean
        */
        public String getName()
            {
            return m_sName;
            }

        /**
        * Obtain the namespace URI for this XML bean.
        *
        * @return the URI that qualifies the default Namespace for this bean
        */
        public String getNamespaceUri()
            {
            return m_sNmsUri;
            }

        /**
        * Obtain the default namespace prefix for this XML bean.
        *
        * @return the default Namespace prefix for this bean
        */
        public String getNamespacePrefix()
            {
            return m_sNmsPrefix;
            }

        /**
        * Set the default Namespace prefix for this XML bean.
        *
        * @param sPrefix  the default namespace prefix
        */
        public void setNamespacePrefix(String sPrefix)
            {
            m_sNmsPrefix = sPrefix;
            }

        /**
        * Obtain the PropertyAdapter objects for the properties of this XML bean.
        *
        * @return the property adapters for this bean
        */
        public PropertyAdapter[] getAdapters()
            {
            return m_aAdapter;
            }

        /**
        * Determine if a clone of the XmlBean should be a deep clone, which
        * typically means that at least one property value is mutable
        * reference type.
        *
        * @return true if any of the property values must be "deep" cloned
        *         when the XmlBean is cloned
        */
        public boolean requiresDeepClone()
            {
            return m_fDeepClone;
            }


        // ----- helpers ----------------------------------------------------

        /**
        * Generate a property adapter instance that will work on this bean class
        * and will adapt for a property of the specified class and of the
        * specified name.
        *
        * @param clz       the class of the property
        * @param sName     the property name
        * @param sXmlName  the corresponding element name
        * @param xml       additional XML information
        *
        * @return an adapter that will handle the specified property
        */
        protected PropertyAdapter makeAdapter(Class clz, String sName, String sXmlName, XmlElement xml)
            {
            // check if the adapter implementation is specified
            String sAdapter  = xml.getSafeElement("adapter").getString();
            Class clzAdapter = sAdapter != null && sAdapter.length() > 0
                    ? resolveClass(sAdapter)
                    : (Class) s_mapClassAdapters.get(clz);

            if (clzAdapter == null)
                {
                if (XmlElement.class.isAssignableFrom(clz))
                    {
                    clzAdapter = XmlElementAdapter.class;
                    }
                else if (XmlSerializable.class.isAssignableFrom(clz))
                    {
                    clzAdapter = XmlSerializableAdapter.class;
                    }
                else if (Object[].class.isAssignableFrom(clz))
                    {
                    clzAdapter = ArrayAdapter.class;
                    }
                else if (Collection.class.isAssignableFrom(clz))
                    {
                    clzAdapter = CollectionAdapter.class;
                    }
                else if (Map.class.isAssignableFrom(clz))
                    {
                    clzAdapter = MapAdapter.class;
                    }
                else if (Serializable.class.isAssignableFrom(clz))
                    {
                    clzAdapter = SerializableAdapter.class;
                    }
                }

            if (clzAdapter == null)
                {
                throw new RuntimeException("XmlBean:  No suitable adapter for:  "
                        + clz.getName());
                }

            try
                {
                // instantiate the adapter
                Constructor constructor = clzAdapter.getConstructor(ADAPTER_INIT_PARAMS);
                Object[] aoParams = new Object[] {this, clz, sName, sXmlName, xml};
                return (PropertyAdapter) constructor.newInstance(aoParams);
                }
            catch (Exception e)
                {
                throw ensureRuntimeException(e,
                        "Instantiating adapter: " + clzAdapter.getName());
                }
            }


        /**
        * Find a property adapter instance for the specified property.
        *
        * @param sName the property name
        *
        * @return an adapter that handles the specified property;
        *         null if none could be found
        */
        public PropertyAdapter findAdapter(String sName)
            {
            PropertyAdapter[] aAdapter = getAdapters();

            for (int i = 0, c = aAdapter.length; i < c; i++)
                {
                PropertyAdapter adapter = aAdapter[i];
                if (adapter.getName().equals(sName))
                    {
                    return adapter;
                    }
                }
            return null;
            }

        /**
        * Resolve a Class name into a Class object.
        *
        * @param sClass  the Class name
        *
        * @return the Class object
        */
        public Class resolveClass(String sClass)
            {
            Class clz = (Class) s_mapClassNames.get(sClass);
            if (clz != null)
                {
                return clz;
                }

            if (sClass.endsWith("[]"))
                {
                clz = resolveClass(sClass.substring(0, sClass.length() - 2));
                if (clz.isArray())
                    {
                    sClass = '[' + clz.getName();
                    }
                else if (clz.isPrimitive())
                    {
                    sClass = "[" + s_mapPrimitiveNames.get(clz);
                    }
                else
                    {
                    sClass = "[L" + clz.getName() + ';';
                    }
                }

            try
                {
                ClassLoader loader = m_clzBean.getClassLoader();
                if (loader == null)
                    {
                    loader = ClassLoader.getSystemClassLoader();
                    }
                return Class.forName(sClass, false, loader);
                }
            catch (Exception e)
                {
                throw ensureRuntimeException(e);
                }
            }

        // ----- Object methods -------------------------------------------------

        /**
        * Debugging support.
        *
        * @return a String description of this Info object
        */
        public String toString()
            {
            StringBuffer sb = new StringBuffer();

            sb.append("BeanInfo for ")
              .append(getName())
              .append(", type=")
              .append(getType().getName())
              .append(", requiresDeepClone=")
              .append(requiresDeepClone())
              .append(", NamespaceUri=")
              .append(getNamespaceUri())
              .append(", NamespacePrefix=")
              .append(getNamespacePrefix());

            return sb.toString();
            }

        // ----- data members -----------------------------------------------

        /**
        * The class of the specific XML bean.
        */
        protected Class             m_clzBean;

        /**
        * The XML element name for the XML bean.
        */
        protected String            m_sName;

        /**
        * The property adapters for the XML bean.
        */
        protected PropertyAdapter[] m_aAdapter;

        /**
        * Specifies whether the XML bean requires a deep clone.
        */
        protected boolean           m_fDeepClone;

        /**
        * Namespace URI.
        */
        protected String m_sNmsUri;

        /**
        * Namespace prefix.
        */
        protected String m_sNmsPrefix;

        /**
        * Serialization ID for the XmlBean class.
        */
        protected int m_nBeanId = -1;


        // ----- constants --------------------------------------------------

        /**
        * Parameters for finding no-parameter methods.
        */
        protected static final Class[] NOPARAMS = new Class[0];

        /**
        * Parameters for finding the default adapter constructor.
        */
        protected static Class[] ADAPTER_INIT_PARAMS = new Class[]
            {
            BeanInfo  .class,
            Class     .class,
            String    .class,
            String    .class,
            XmlElement.class,
            };

        /**
        * Map from type name / short class name to actual class instance.
        */
        protected static final Map s_mapClassNames = new HashMap();
        static
            {
            Map map = s_mapClassNames;
            map.put("boolean"   , boolean       .class);
            map.put("byte"      , byte          .class);
            map.put("char"      , char          .class);
            map.put("short"     , short         .class);
            map.put("int"       , int           .class);
            map.put("long"      , long          .class);
            map.put("float"     , float         .class);
            map.put("double"    , double        .class);
            map.put("Boolean"   , Boolean       .class);
            map.put("Byte"      , Byte          .class);
            map.put("Character" , Character     .class);
            map.put("Short"     , Short         .class);
            map.put("Integer"   , Integer       .class);
            map.put("Long"      , Long          .class);
            map.put("Float"     , Float         .class);
            map.put("Double"    , Double        .class);
            map.put("BigDecimal", BigDecimal    .class);
            map.put("BigInteger", BigInteger    .class);
            map.put("String"    , String        .class);
            map.put("Date"      , Date          .class);
            map.put("Time"      , Time          .class);
            map.put("Timestamp" , Timestamp     .class);
            }

        /**
        * Map from the class of a property type to the class of the adapter
        * that handles the type.
        */
        protected static final Map s_mapClassAdapters = new HashMap();
        static
            {
            Map map = s_mapClassAdapters;
            map.put(boolean.class , SimpleAdapter.BooleanAdapter.class);
            map.put(byte   .class , SimpleAdapter.ByteAdapter   .class);
            map.put(char   .class , SimpleAdapter.CharAdapter   .class);
            map.put(short  .class , SimpleAdapter.ShortAdapter  .class);
            map.put(int    .class , SimpleAdapter.IntAdapter    .class);
            map.put(long   .class , SimpleAdapter.LongAdapter   .class);
            map.put(float  .class , SimpleAdapter.FloatAdapter  .class);
            map.put(double .class , SimpleAdapter.DoubleAdapter .class);

            map.put(Boolean  .class , SimpleAdapter.BooleanAdapter.class);
            map.put(Byte     .class , SimpleAdapter.ByteAdapter   .class);
            map.put(Character.class , SimpleAdapter.CharAdapter   .class);
            map.put(Short    .class , SimpleAdapter.ShortAdapter  .class);
            map.put(Integer  .class , SimpleAdapter.IntAdapter    .class);
            map.put(Long     .class , SimpleAdapter.LongAdapter   .class);
            map.put(Float    .class , SimpleAdapter.FloatAdapter  .class);
            map.put(Double   .class , SimpleAdapter.DoubleAdapter .class);

            map.put(boolean[].class , PrimitiveArrayAdapter.BooleanArrayAdapter.class);
            map.put(byte   [].class , PrimitiveArrayAdapter.ByteArrayAdapter   .class);
            map.put(char   [].class , PrimitiveArrayAdapter.CharArrayAdapter   .class);
            map.put(short  [].class , PrimitiveArrayAdapter.ShortArrayAdapter  .class);
            map.put(int    [].class , PrimitiveArrayAdapter.IntArrayAdapter    .class);
            map.put(long   [].class , PrimitiveArrayAdapter.LongArrayAdapter   .class);
            map.put(float  [].class , PrimitiveArrayAdapter.FloatArrayAdapter  .class);
            map.put(double [].class , PrimitiveArrayAdapter.DoubleArrayAdapter .class);

            map.put(String        .class, SimpleAdapter.StringAdapter    .class);
            map.put(BigDecimal    .class, SimpleAdapter.BigDecimalAdapter.class);
            map.put(BigInteger    .class, SimpleAdapter.BigIntegerAdapter.class);
            map.put(Date          .class, SimpleAdapter.DateAdapter      .class);
            map.put(Time          .class, SimpleAdapter.TimeAdapter      .class);
            map.put(Timestamp     .class, SimpleAdapter.TimestampAdapter .class);
            map.put(java.util.Date.class, SimpleAdapter.OldDateAdapter   .class);
            }

        /**
        * Map from class of an intrinsic type to its JVM signature.
        */
        protected static final Map s_mapPrimitiveNames = new HashMap();
        static
            {
            Map map = s_mapPrimitiveNames;
            map.put(boolean.class , "Z");
            map.put(byte   .class , "B");
            map.put(char   .class , "C");
            map.put(short  .class , "S");
            map.put(int    .class , "I");
            map.put(long   .class , "J");
            map.put(float  .class , "F");
            map.put(double .class , "D");
            }
        }


    // ----- data members ---------------------------------------------------

    /**
    * For backwards compatibility -- caches XML descriptors that wrap the
    * old-style data used for XmlBean initialization.
    */
    private static Map s_mapXml = new SafeHashMap();

    /**
    * Cache by class of reflection information.
    */
    private static Map s_mapInfo = new SafeHashMap();

    /**
    * If this XmlBean is contained by another XmlBean, then the containing
    * bean reference is held by this XmlBean.
    */
    private transient XmlBean m_parent;

    /**
    * Mutable/read-only setting.
    */
    private boolean m_fMutable = true;

    /**
    * Cached hash value.
    */
    private transient int m_nHash;

    /**
    * Cached bean info (for this bean).
    */
    private transient BeanInfo m_info;
    }
