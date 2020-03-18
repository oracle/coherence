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

import java.util.Iterator;


/**
* A PrimitiveArrayAdapter supports arrays of primitive types, such as
* "int", "char", etc.
*
* @version 1.00  2001.03.06
* @author cp
*/
public abstract class PrimitiveArrayAdapter
        extends IterableAdapter
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a PrimitiveArrayAdapter.
    *
    * @param infoBean BeanInfo for a bean containing this property
    * @param clzType  the type of the property
    * @param sName    the property name
    * @param sXml     the XML tag name
    * @param xml      additional XML information
    */
    public PrimitiveArrayAdapter(XmlBean.BeanInfo infoBean, Class clzType, String sName, String sXml, XmlElement xml)
        {
        super(infoBean, clzType, sName, sXml, xml);
        }


    // ----- accessors ------------------------------------------------------

    /**
    * @return true if the adapter can format the array of primitive
    * values to/from a single value (e.g. char[], byte[])
    */
    public boolean isStringable()
        {
        return false;
        }


    // ----- Object method helpers ------------------------------------------

    /**
    * compute a hash code for the passed object.
    *
    * @param o  the object to compute a hash code for
    *
    * @return an integer hash code
    */
    public abstract int hash(Object o);

    /**
    * Compare the two passed objects for equality.
    *
    * @param o1  the first object
    * @param o2  the second object
    *
    * @return true if the two objects are equal
    */
    public abstract boolean equalsValue(Object o1, Object o2);

    /**
    * Make a clone of the passed object.
    *
    * @param o  the object to clone
    *
    * @return a clone of the passed object
    */
    public abstract Object clone(Object o);


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
        if (isStringable() && !isNested())
            {
            return xml == null ? null : fromXmlString(xml);
            }
        else
            {
            return super.fromXml(xml);
            }
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
        if (isStringable() && !isNested())
            {
            return o == null ? null : toXmlString(o);
            }
        else
            {
            return super.toXml(o);
            }
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
        if (isStringable() && !isNested())
            {
            return fromXml(findElement(xml));
            }
        else
            {
            return super.readXml(xml);
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
        if (isStringable() && !isNested())
            {
            XmlElement xmlValue = toXml(o);
            if (xmlValue != null)
                {
                xml.getElementList().add(xmlValue);
                }
            }
        else
            {
            super.writeXml(xml, o);
            }
        }

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
            sElement = getLocalXmlName();
            }

        Iterator iter = XmlHelper.getElements(xml, sElement, getNamespaceUri());

        if (isSparse())
            {
            int c = xml.getSafeAttribute("length").getInt();
            if (c < 0)
                {
                throw new IllegalArgumentException("Illegal length: " + c);
                }

            return readSparseArray(iter, c);
            }
        else
            {
            boolean  fNested = isNested();
            if (!fNested && !iter.hasNext())
                {
                // cannot differentiate between null array and
                // 0-length array if the elements are not nested
                // under an xml tag for the property, so assume
                // that the array is null if not nested
                return null;
                }
            return readArray(iter, xml, fNested);
            }
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
            sElement = getXmlName();
            }

        if (isSparse())
            {
            writeSparseArray(xml, o, sElement);
            }
        else
            {
            writeArray(xml, o, sElement);
            }
        }

    /**
    * Deserialize a primitive array from a single XML element.
    *
    * @param xml  the XML element to deserialize from (not null)
    *
    * @return the object deserialized from the XML element
    *
    * @exception UnsupportedOperationException  if the property cannot be
    *            read from a single XML element
    */
    public Object fromXmlString(XmlElement xml)
        {
        throw new UnsupportedOperationException();
        }

    /**
    * Serialize a primitive array into a single XML element.
    *
    * @param o  the object to serialize (not null)
    *
    * @return the XML element representing the serialized form of the
    *         passed object
    *
    * @exception UnsupportedOperationException  if the property cannot be
    *            written to a single XML element
    */
    public XmlElement toXmlString(Object o)
        {
        throw new UnsupportedOperationException();
        }

    /**
    * Read a sparse array of primitive values.
    *
    * @param iter  the iterator of XmlElement objects
    * @param c     the size of the array
    *
    * @return an array of primitive values
    */
    public abstract Object readSparseArray(Iterator iter, int c);

    /**
    * Read an array of primitive values.
    *
    * @param iter     the iterator of XmlElement objects
    * @param xml      the XmlElement from which the iterator was obtained
    * @param fNested  true if the array is nested under an array tag
    *
    * @return an array of primitive values
    */
    public abstract Object readArray(Iterator iter, XmlElement xml, boolean fNested);

    /**
    * Write a sparse array of primitive values.
    *
    * @param xml       the XmlElement that will contain the array
    * @param o         the primitive array
    * @param sElement  the name of the element containing an element value
    */
    public abstract void writeSparseArray(XmlElement xml, Object o, String sElement);

    /**
    * Write a sparse array of primitive values.
    *
    * @param xml       the XmlElement that will contain the array elements
    * @param o         the primitive array
    * @param sElement  the name of the element containing an element value
    */
    public abstract void writeArray(XmlElement xml, Object o, String sElement);


    // ----- inner class:  BooleanArrayAdapter ------------------------------

    /**
    * A PropertyAdapter supporting boolean[].
    *
    * @version 1.00  2001.03.17
    * @author cp
    */
    public static class BooleanArrayAdapter
            extends PrimitiveArrayAdapter
        {
        // ----- constructors ------------------------------------------

        /**
        * Construct a BooleanArrayAdapter.
        *
        * @param infoBean BeanInfo for a bean containing this property
        * @param clzType  the type of the property
        * @param sName    the property name
        * @param sXml     the XML tag name
        * @param xml      additional XML information
        */
        public BooleanArrayAdapter(XmlBean.BeanInfo infoBean, Class clzType, String sName, String sXml, XmlElement xml)
            {
            super(infoBean, clzType, sName, sXml, xml);
            azzert(clzType == boolean[].class);
            }


        // ----- Object method helpers ---------------------------------

        /**
        * compute a hash code for the passed object.
        *
        * @param o  the object to compute a hash code for
        *
        * @return an integer hash code
        */
        public int hash(Object o)
            {
            if (o == null)
                {
                return 0;
                }

            int n = 0;
            boolean[] af = (boolean[]) o;
            for (int i = 0, c = af.length; i < c; ++i)
                {
                n ^= af[i] ? 1231 : 1237;
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
                // we already know that (o1 == o2) failed, so o2 is not null
                return false;
                }

            boolean[] af1 = (boolean[]) o1;
            boolean[] af2 = (boolean[]) o2;
            if (af1.length != af2.length)
                {
                return false;
                }

            for (int i = 0, c = af1.length; i < c; ++i)
                {
                if (af1[i] != af2[i])
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
            return o == null ? null : ((boolean[]) o).clone();
            }


        // ----- XmlSerializable helpers -------------------------------

        /**
        * Deserialize a primitive array from a single XML element.
        *
        * @param xml  the XML element to deserialize from (not null)
        *
        * @return the object deserialized from the XML element
        *
        * @exception UnsupportedOperationException  if the property cannot be
        *            read from a single XML element
        */
        public Object fromXmlString(XmlElement xml)
            {
            return toBooleanArray(xml.getString());
            }

        /**
        * Serialize a primitive array into a single XML element.
        *
        * @param o  the object to serialize (not null)
        *
        * @return the XML element representing the serialized form of the
        *         passed object
        *
        * @exception UnsupportedOperationException  if the property cannot be
        *            written to a single XML element
        */
        public XmlElement toXmlString(Object o)
            {
            return new SimpleElement(getXmlName(), toString((boolean[]) o));
            }

        /**
        * @return true if the adapter can format the array of primitive
        * values to/from a single value (e.g. char[], byte[])
        */
        public boolean isStringable()
            {
            return true;
            }

        /**
        * Determine if the specified value is empty.
        *
        * @param o  the value
        *
        * @return  true if the object is considered to be empty for
        *          persistence and XML-generation purposes
        */
        public boolean isEmpty(Object o)
            {
            return o == null || isEmptyIsNull() && ((boolean[]) o).length == 0;
            }

        /**
        * Read a sparse array of primitive values.
        *
        * @param iter  the iterator of XmlElement objects
        * @param c     the size of the array
        *
        * @return an array of primitive values
        */
        public Object readSparseArray(Iterator iter, int c)
            {
            boolean[] af = new boolean[c];
            while (iter.hasNext())
                {
                XmlElement elem = (XmlElement) iter.next();
                XmlValue   attr = elem.getAttribute("id");
                if (attr == null)
                    {
                    throw new IllegalArgumentException("Element " + elem.getName()
                            + " is missing the required \"id\" attribute");
                    }
                af[attr.getInt(-1)] = elem.getBoolean();
                }
            return af;
            }

        /**
        * Read an array of primitive values.
        *
        * @param iter  the iterator of XmlElement objects
        * @param xml   the XmlElement from which the iterator was obtained
        * @param fNested  true if the array is nested under an array tag
        *
        * @return an array of primitive values
        */
        public Object readArray(Iterator iter, XmlElement xml, boolean fNested)
            {
            // determine a good default size for the array of
            // values based on the number of xml elements that
            // are nested under the current element
            int       cXml = xml.getElementList().size();
            int       cMax = fNested ? cXml : Math.min(cXml, 32);
            boolean[] af   = new boolean[cMax];
            int       c    = 0;
            while (iter.hasNext())
                {
                // check if a larger array is needed; grow by 2x
                if (c >= cMax)
                    {
                    int       cNew  = cMax * 2;
                    boolean[] afNew = new boolean[cNew];
                    System.arraycopy(af, 0, afNew, 0, cMax);
                    af   = afNew;
                    cMax = cNew;
                    }

                XmlElement xmlValue = (XmlElement) iter.next();
                af[c++] = xmlValue.getBoolean();
                }

            if (c == cMax)
                {
                return af;
                }

            boolean[] afNew = new boolean[c];
            System.arraycopy(af, 0, afNew, 0, c);
            return afNew;
            }

        /**
        * Write a sparse array of primitive values.
        *
        * @param xml       the XmlElement that will contain the array
        * @param o         the primitive array
        * @param sElement  the name of the element containing an element value
        */
        public void writeSparseArray(XmlElement xml, Object o, String sElement)
            {
            boolean[] af = (boolean[]) o;
            int       c  = af.length;
            xml.addAttribute("length").setInt(c);
            for (int i = 0; i < c; ++i)
                {
                boolean f = af[i];
                if (f)
                    {
                    XmlElement element = xml.addElement(sElement);
                    element.addAttribute("id").setInt(i);
                    element.setBoolean(f);
                    }
                }
            }

        /**
        * Write a sparse array of primitive values.
        *
        * @param xml       the XmlElement that will contain the array elements
        * @param o         the primitive array
        * @param sElement  the name of the element containing an element value
        */
        public void writeArray(XmlElement xml, Object o, String sElement)
            {
            boolean[] af = (boolean[]) o;
            for (int i = 0, c = af.length; i < c; ++i)
                {
                xml.addElement(sElement).setBoolean(af[i]);
                }
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
            return toBooleanArray(sUri);
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
            return toString((boolean[]) o);
            }


        // ----- ExternalizableLite helpers ----------------------------

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
            return readBooleanArray(in);
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
            writeBooleanArray(out, (boolean[]) o);
            }


        // ----- helpers -----------------------------------------------

        /**
        * @param s  the stringed boolean array
        *
        * @return the corresponding boolean array
        */
        public static boolean[] toBooleanArray(String s)
            {
            char   [] ach = s.toCharArray();
            boolean[] af  = new boolean[ach.length];
            for (int i = 0, c = ach.length; i < c; ++i)
                {
                af[i] = (ach[i] == '1');
                }
            return af;
            }

        /**
        * @param af  the boolean array
        *
        * @return the corresponding stringed boolean array
        */
        public static String toString(boolean[] af)
            {
            char   [] ach = new char[af.length];
            for (int i = 0, c = af.length; i < c; ++i)
                {
                ach[i] = af[i] ? '1' : '0';
                }
            return new String(ach);
            }
        }


    // ----- inner class:  ByteArrayAdapter ---------------------------------

    /**
    * A PropertyAdapter supporting byte[].
    *
    * @version 1.00  2001.03.17
    * @author cp
    */
    public static class ByteArrayAdapter
            extends PrimitiveArrayAdapter
        {
        // ----- constructors ------------------------------------------

        /**
        * Construct a ByteArrayAdapter.
        *
        * @param infoBean BeanInfo for a bean containing this property
        * @param clzType  the type of the property
        * @param sName    the property name
        * @param sXml     the XML tag name
        * @param xml      additional XML information
        */
        public ByteArrayAdapter(XmlBean.BeanInfo infoBean, Class clzType, String sName, String sXml, XmlElement xml)
            {
            super(infoBean, clzType, sName, sXml, xml);
            azzert(clzType == byte[].class);
            }


        // ----- Object method helpers ---------------------------------

        /**
        * compute a hash code for the passed object.
        *
        * @param o  the object to compute a hash code for
        *
        * @return an integer hash code
        */
        public int hash(Object o)
            {
            if (o == null)
                {
                return 0;
                }

            int n = 0;
            byte[] an = (byte[]) o;
            for (int i = 0, c = an.length; i < c; ++i)
                {
                n ^= an[i];
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
                // we already know that (o1 == o2) failed, so o2 is not null
                return false;
                }

            byte[] ab1 = (byte[]) o1;
            byte[] ab2 = (byte[]) o2;
            if (ab1.length != ab2.length)
                {
                return false;
                }

            for (int i = 0, c = ab1.length; i < c; ++i)
                {
                if (ab1[i] != ab2[i])
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
            return o == null ? null : ((byte[]) o).clone();
            }


        // ----- XmlSerializable helpers -------------------------------

        /**
        * Deserialize a primitive array from a single XML element.
        *
        * @param xml  the XML element to deserialize from (not null)
        *
        * @return the object deserialized from the XML element
        *
        * @exception UnsupportedOperationException  if the property cannot be
        *            read from a single XML element
        */
        public Object fromXmlString(XmlElement xml)
            {
            return xml.getBinary().toByteArray();
            }

        /**
        * Serialize a primitive array into a single XML element.
        *
        * @param o  the object to serialize (not null)
        *
        * @return the XML element representing the serialized form of the
        *         passed object
        *
        * @exception UnsupportedOperationException  if the property cannot be
        *            written to a single XML element
        */
        public XmlElement toXmlString(Object o)
            {
            return new SimpleElement(getXmlName(), new Binary((byte[]) o));
            }

        /**
        * @return true if the adapter can format the array of primitive
        * values to/from a single value (e.g. char[], byte[])
        */
        public boolean isStringable()
            {
            return true;
            }

        /**
        * Determine if the specified value is empty.
        *
        * @param o  the value
        *
        * @return  true if the object is considered to be empty for
        *          persistence and XML-generation purposes
        */
        public boolean isEmpty(Object o)
            {
            return o == null || isEmptyIsNull() && ((byte[]) o).length == 0;
            }

        /**
        * Read a sparse array of primitive values.
        *
        * @param iter  the iterator of XmlElement objects
        * @param c     the size of the array
        *
        * @return an array of primitive values
        */
        public Object readSparseArray(Iterator iter, int c)
            {
            byte[] ab = new byte[c];
            while (iter.hasNext())
                {
                XmlElement elem = (XmlElement) iter.next();
                XmlValue   attr = elem.getAttribute("id");
                if (attr == null)
                    {
                    throw new IllegalArgumentException("Element " + elem.getName()
                            + " is missing the required \"id\" attribute");
                    }
                ab[attr.getInt(-1)] = (byte) elem.getInt();
                }
            return ab;
            }

        /**
        * Read an array of primitive values.
        *
        * @param iter  the iterator of XmlElement objects
        * @param xml   the XmlElement from which the iterator was obtained
        * @param fNested  true if the array is nested under an array tag
        *
        * @return an array of primitive values
        */
        public Object readArray(Iterator iter, XmlElement xml, boolean fNested)
            {
            // determine a good default size for the array of
            // values based on the number of xml elements that
            // are nested under the current element
            int    cXml = xml.getElementList().size();
            int    cMax = fNested ? cXml : Math.min(cXml, 32);
            byte[] ab   = new byte[cMax];
            int    c    = 0;
            while (iter.hasNext())
                {
                // check if a larger array is needed; grow by 2x
                if (c >= cMax)
                    {
                    int    cNew  = cMax * 2;
                    byte[] abNew = new byte[cNew];
                    System.arraycopy(ab, 0, abNew, 0, cMax);
                    ab   = abNew;
                    cMax = cNew;
                    }

                XmlElement xmlValue = (XmlElement) iter.next();
                ab[c++] = (byte) xmlValue.getInt();
                }

            if (c == cMax)
                {
                return ab;
                }

            byte[] abNew = new byte[c];
            System.arraycopy(ab, 0, abNew, 0, c);
            return abNew;
            }

        /**
        * Write a sparse array of primitive values.
        *
        * @param xml       the XmlElement that will contain the array
        * @param o         the primitive array
        * @param sElement  the name of the element containing an element value
        */
        public void writeSparseArray(XmlElement xml, Object o, String sElement)
            {
            byte[] an = (byte[]) o;
            int    c  = an.length;
            xml.addAttribute("length").setInt(c);
            for (int i = 0; i < c; ++i)
                {
                int n = an[i];
                if (n != 0)
                    {
                    XmlElement element = xml.addElement(sElement);
                    element.addAttribute("id").setInt(i);
                    element.setInt(n);
                    }
                }
            }

        /**
        * Write a sparse array of primitive values.
        *
        * @param xml       the XmlElement that will contain the array elements
        * @param o         the primitive array
        * @param sElement  the name of the element containing an element value
        */
        public void writeArray(XmlElement xml, Object o, String sElement)
            {
            byte[] ab = (byte[]) o;
            for (int i = 0, c = ab.length; i < c; ++i)
                {
                xml.addElement(sElement).setInt(ab[i]);
                }
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
            return parseHex(sUri);
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
            return toHex((byte[]) o);
            }


        // ----- ExternalizableLite helpers ----------------------------

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
            return readByteArray(in);
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
            writeByteArray(out, (byte[]) o);
            }
        }


    // ----- inner class:  CharArrayAdapter ---------------------------------

    /**
    * A PropertyAdapter supporting char[].
    *
    * @version 1.00  2001.03.17
    * @author cp
    */
    public static class CharArrayAdapter
            extends PrimitiveArrayAdapter
        {
        // ----- constructors ------------------------------------------

        /**
        * Construct a CharArrayAdapter.
        *
        * @param infoBean BeanInfo for a bean containing this property
        * @param clzType  the type of the property
        * @param sName    the property name
        * @param sXml     the XML tag name
        * @param xml      additional XML information
        */
        public CharArrayAdapter(XmlBean.BeanInfo infoBean, Class clzType, String sName, String sXml, XmlElement xml)
            {
            super(infoBean, clzType, sName, sXml, xml);
            azzert(clzType == char[].class);
            }


        // ----- Object method helpers ---------------------------------

        /**
        * compute a hash code for the passed object.
        *
        * @param o  the object to compute a hash code for
        *
        * @return an integer hash code
        */
        public int hash(Object o)
            {
            if (o == null)
                {
                return 0;
                }

            int n = 0;
            char[] an = (char[]) o;
            for (int i = 0, c = an.length; i < c; ++i)
                {
                n ^= an[i];
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
                // we already know that (o1 == o2) failed, so o2 is not null
                return false;
                }

            char[] ach1 = (char[]) o1;
            char[] ach2 = (char[]) o2;
            if (ach1.length != ach2.length)
                {
                return false;
                }

            for (int i = 0, c = ach1.length; i < c; ++i)
                {
                if (ach1[i] != ach2[i])
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
            return o == null ? null : ((char[]) o).clone();
            }


        // ----- XmlSerializable helpers -------------------------------

        /**
        * Deserialize a primitive array from a single XML element.
        *
        * @param xml  the XML element to deserialize from (not null)
        *
        * @return the object deserialized from the XML element
        *
        * @exception UnsupportedOperationException  if the property cannot be
        *            read from a single XML element
        */
        public Object fromXmlString(XmlElement xml)
            {
            return xml.getString().toCharArray();
            }

        /**
        * Serialize a primitive array into a single XML element.
        *
        * @param o  the object to serialize (not null)
        *
        * @return the XML element representing the serialized form of the
        *         passed object
        *
        * @exception UnsupportedOperationException  if the property cannot be
        *            written to a single XML element
        */
        public XmlElement toXmlString(Object o)
            {
            return new SimpleElement(getXmlName(), new String((char[]) o));
            }

        /**
        * @return true if the adapter can format the array of primitive
        * values to/from a single value (e.g. char[], byte[])
        */
        public boolean isStringable()
            {
            return true;
            }

        /**
        * Determine if the specified value is empty.
        *
        * @param o  the value
        *
        * @return  true if the object is considered to be empty for
        *          persistence and XML-generation purposes
        */
        public boolean isEmpty(Object o)
            {
            return o == null || isEmptyIsNull() && ((char[]) o).length == 0;
            }

        /**
        * Read a sparse array of primitive values.
        *
        * @param iter  the iterator of XmlElement objects
        * @param c     the size of the array
        *
        * @return an array of primitive values
        */
        public Object readSparseArray(Iterator iter, int c)
            {
            char[] ach = new char[c];
            while (iter.hasNext())
                {
                XmlElement elem = (XmlElement) iter.next();
                XmlValue   attr = elem.getAttribute("id");
                if (attr == null)
                    {
                    throw new IllegalArgumentException("Element " + elem.getName()
                            + " is missing the required \"id\" attribute");
                    }
                ach[attr.getInt(-1)] = (char) elem.getInt();
                }
            return ach;
            }

        /**
        * Read an array of primitive values.
        *
        * @param iter  the iterator of XmlElement objects
        * @param xml   the XmlElement from which the iterator was obtained
        * @param fNested  true if the array is nested under an array tag
        *
        * @return an array of primitive values
        */
        public Object readArray(Iterator iter, XmlElement xml, boolean fNested)
            {
            // determine a good default size for the array of
            // values based on the number of xml elements that
            // are nested under the current element
            int    cXml = xml.getElementList().size();
            int    cMax = fNested ? cXml : Math.min(cXml, 32);
            char[] ach  = new char[cMax];
            int    c    = 0;
            while (iter.hasNext())
                {
                // check if a larger array is needed; grow by 2x
                if (c >= cMax)
                    {
                    int    cNew   = cMax * 2;
                    char[] achNew = new char[cNew];
                    System.arraycopy(ach, 0, achNew, 0, cMax);
                    ach  = achNew;
                    cMax = cNew;
                    }

                XmlElement xmlValue = (XmlElement) iter.next();
                ach[c++] = (char) xmlValue.getInt();
                }

            if (c == cMax)
                {
                return ach;
                }

            char[] achNew = new char[c];
            System.arraycopy(ach, 0, achNew, 0, c);
            return achNew;
            }

        /**
        * Write a sparse array of primitive values.
        *
        * @param xml       the XmlElement that will contain the array
        * @param o         the primitive array
        * @param sElement  the name of the element containing an element value
        */
        public void writeSparseArray(XmlElement xml, Object o, String sElement)
            {
            char[] ach = (char[]) o;
            int    c   = ach.length;
            xml.addAttribute("length").setInt(c);
            for (int i = 0; i < c; ++i)
                {
                int n = ach[i];
                if (n != 0)
                    {
                    XmlElement element = xml.addElement(sElement);
                    element.addAttribute("id").setInt(i);
                    element.setInt(n);
                    }
                }
            }

        /**
        * Write a sparse array of primitive values.
        *
        * @param xml       the XmlElement that will contain the array elements
        * @param o         the primitive array
        * @param sElement  the name of the element containing an element value
        */
        public void writeArray(XmlElement xml, Object o, String sElement)
            {
            char[] ach = (char[]) o;
            for (int i = 0, c = ach.length; i < c; ++i)
                {
                xml.addElement(sElement).setInt(ach[i]);
                }
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
            return SimpleAdapter.decodeString(sUri).toCharArray();
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
            return SimpleAdapter.encodeString(new String((char[]) o));
            }


        // ----- ExternalizableLite helpers ----------------------------

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
            return readUTF(in).toCharArray();
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
            writeUTF(out, new String((char[]) o));
            }
        }


    // ----- inner class:  ShortArrayAdapter --------------------------------

    /**
    * A PropertyAdapter supporting short[].
    *
    * @version 1.00  2001.03.17
    * @author cp
    */
    public static class ShortArrayAdapter
            extends PrimitiveArrayAdapter
        {
        // ----- constructors ------------------------------------------

        /**
        * Construct a ShortArrayAdapter.
        *
        * @param infoBean BeanInfo for a bean containing this property
        * @param clzType  the type of the property
        * @param sName    the property name
        * @param sXml     the XML tag name
        * @param xml      additional XML information
        */
        public ShortArrayAdapter(XmlBean.BeanInfo infoBean, Class clzType, String sName, String sXml, XmlElement xml)
            {
            super(infoBean, clzType, sName, sXml, xml);
            azzert(clzType == short[].class);
            }


        // ----- Object method helpers ---------------------------------

        /**
        * compute a hash code for the passed object.
        *
        * @param o  the object to compute a hash code for
        *
        * @return an integer hash code
        */
        public int hash(Object o)
            {
            if (o == null)
                {
                return 0;
                }

            int n = 0;
            short[] an = (short[]) o;
            for (int i = 0, c = an.length; i < c; ++i)
                {
                n ^= an[i];
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
                // we already know that (o1 == o2) failed, so o2 is not null
                return false;
                }

            short[] an1 = (short[]) o1;
            short[] an2 = (short[]) o2;
            if (an1.length != an2.length)
                {
                return false;
                }

            for (int i = 0, c = an1.length; i < c; ++i)
                {
                if (an1[i] != an2[i])
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
            return o == null ? null : ((short[]) o).clone();
            }


        // ----- XmlSerializable helpers -------------------------------

        /**
        * Determine if the specified value is empty.
        *
        * @param o  the value
        *
        * @return  true if the object is considered to be empty for
        *          persistence and XML-generation purposes
        */
        public boolean isEmpty(Object o)
            {
            return o == null || isEmptyIsNull() && ((short[]) o).length == 0;
            }

        /**
        * Read a sparse array of primitive values.
        *
        * @param iter  the iterator of XmlElement objects
        * @param c     the size of the array
        *
        * @return an array of primitive values
        */
        public Object readSparseArray(Iterator iter, int c)
            {
            short[] an = new short[c];
            while (iter.hasNext())
                {
                XmlElement elem = (XmlElement) iter.next();
                XmlValue   attr = elem.getAttribute("id");
                if (attr == null)
                    {
                    throw new IllegalArgumentException("Element " + elem.getName()
                            + " is missing the required \"id\" attribute");
                    }
                an[attr.getInt(-1)] = (short) elem.getInt();
                }
            return an;
            }

        /**
        * Read an array of primitive values.
        *
        * @param iter  the iterator of XmlElement objects
        * @param xml   the XmlElement from which the iterator was obtained
        * @param fNested  true if the array is nested under an array tag
        *
        * @return an array of primitive values
        */
        public Object readArray(Iterator iter, XmlElement xml, boolean fNested)
            {
            // determine a good default size for the array of
            // values based on the number of xml elements that
            // are nested under the current element
            int     cXml = xml.getElementList().size();
            int     cMax = fNested ? cXml : Math.min(cXml, 32);
            short[] an   = new short[cMax];
            int     c    = 0;
            while (iter.hasNext())
                {
                // check if a larger array is needed; grow by 2x
                if (c >= cMax)
                    {
                    int     cNew  = cMax * 2;
                    short[] anNew = new short[cNew];
                    System.arraycopy(an, 0, anNew, 0, cMax);
                    an   = anNew;
                    cMax = cNew;
                    }

                XmlElement xmlValue = (XmlElement) iter.next();
                an[c++] = (short) xmlValue.getInt();
                }

            if (c == cMax)
                {
                return an;
                }

            short[] anNew = new short[c];
            System.arraycopy(an, 0, anNew, 0, c);
            return anNew;
            }

        /**
        * Write a sparse array of primitive values.
        *
        * @param xml       the XmlElement that will contain the array
        * @param o         the primitive array
        * @param sElement  the name of the element containing an element value
        */
        public void writeSparseArray(XmlElement xml, Object o, String sElement)
            {
            short[] an = (short[]) o;
            int     c  = an.length;
            xml.addAttribute("length").setInt(c);
            for (int i = 0; i < c; ++i)
                {
                int n = an[i];
                if (n != 0)
                    {
                    XmlElement element = xml.addElement(sElement);
                    element.addAttribute("id").setInt(i);
                    element.setInt(n);
                    }
                }
            }

        /**
        * Write a sparse array of primitive values.
        *
        * @param xml       the XmlElement that will contain the array elements
        * @param o         the primitive array
        * @param sElement  the name of the element containing an element value
        */
        public void writeArray(XmlElement xml, Object o, String sElement)
            {
            short[] an = (short[]) o;
            for (int i = 0, c = an.length; i < c; ++i)
                {
                xml.addElement(sElement).setInt(an[i]);
                }
            }


        // ----- ExternalizableLite helpers ----------------------------

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
            // "in" contains an array length and a string of values
            int     c  = readInt(in);
            short[] an = new short[c];
            for (int i = 0; i < c; ++i)
                {
                an[i] = (short) readInt(in);
                }
            return an;
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
            short[] an = (short[]) o;
            int     c  = an.length;
            writeInt(out, c);
            for (int i = 0; i < c; ++i)
                {
                writeInt(out, an[i]);
                }
            }
        }


    // ----- inner class:  IntArrayAdapter ----------------------------------

    /**
    * A PropertyAdapter supporting int[].
    *
    * @version 1.00  2001.03.17
    * @author cp
    */
    public static class IntArrayAdapter
            extends PrimitiveArrayAdapter
        {
        // ----- constructors ------------------------------------------

        /**
        * Construct an IntArrayAdapter.
        *
        * @param infoBean BeanInfo for a bean containing this property
        * @param clzType  the type of the property
        * @param sName    the property name
        * @param sXml     the XML tag name
        * @param xml      additional XML information
        */
        public IntArrayAdapter(XmlBean.BeanInfo infoBean, Class clzType, String sName, String sXml, XmlElement xml)
            {
            super(infoBean, clzType, sName, sXml, xml);
            azzert(clzType == int[].class);
            }


        // ----- Object method helpers ---------------------------------

        /**
        * compute a hash code for the passed object.
        *
        * @param o  the object to compute a hash code for
        *
        * @return an integer hash code
        */
        public int hash(Object o)
            {
            if (o == null)
                {
                return 0;
                }

            int n = 0;
            int[] an = (int[]) o;
            for (int i = 0, c = an.length; i < c; ++i)
                {
                n ^= an[i];
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
                // we already know that (o1 == o2) failed, so o2 is not null
                return false;
                }

            int[] an1 = (int[]) o1;
            int[] an2 = (int[]) o2;
            if (an1.length != an2.length)
                {
                return false;
                }

            for (int i = 0, c = an1.length; i < c; ++i)
                {
                if (an1[i] != an2[i])
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
            return o == null ? null : ((int[]) o).clone();
            }


        // ----- XmlSerializable helpers -------------------------------

        /**
        * Determine if the specified value is empty.
        *
        * @param o  the value
        *
        * @return  true if the object is considered to be empty for
        *          persistence and XML-generation purposes
        */
        public boolean isEmpty(Object o)
            {
            return o == null || isEmptyIsNull() && ((int[]) o).length == 0;
            }

        /**
        * Read a sparse array of primitive values.
        *
        * @param iter  the iterator of XmlElement objects
        * @param c     the size of the array
        *
        * @return an array of primitive values
        */
        public Object readSparseArray(Iterator iter, int c)
            {
            int[] an = new int[c];
            while (iter.hasNext())
                {
                XmlElement elem = (XmlElement) iter.next();
                XmlValue   attr = elem.getAttribute("id");
                if (attr == null)
                    {
                    throw new IllegalArgumentException("Element " + elem.getName()
                            + " is missing the required \"id\" attribute");
                    }
                an[attr.getInt(-1)] = elem.getInt();
                }
            return an;
            }

        /**
        * Read an array of primitive values.
        *
        * @param iter  the iterator of XmlElement objects
        * @param xml   the XmlElement from which the iterator was obtained
        * @param fNested  true if the array is nested under an array tag
        *
        * @return an array of primitive values
        */
        public Object readArray(Iterator iter, XmlElement xml, boolean fNested)
            {
            if (!iter.hasNext())
                {
                return new int[0];
                }

            // determine a good default size for the array of
            // values based on the number of xml elements that
            // are nested under the current element
            int   cXml = xml.getElementList().size();
            int   cMax = fNested ? cXml : Math.min(cXml, 32);
            int[] an   = new int[cMax];
            int   c    = 0;
            while (iter.hasNext())
                {
                // check if a larger array is needed; grow by 2x
                if (c >= cMax)
                    {
                    int   cNew  = cMax * 2;
                    int[] anNew = new int[cNew];
                    System.arraycopy(an, 0, anNew, 0, cMax);
                    an   = anNew;
                    cMax = cNew;
                    }

                XmlElement xmlValue = (XmlElement) iter.next();
                an[c++] = xmlValue.getInt();
                }

            if (c == cMax)
                {
                return an;
                }

            int[] anNew = new int[c];
            System.arraycopy(an, 0, anNew, 0, c);
            return anNew;
            }

        /**
        * Write a sparse array of primitive values.
        *
        * @param xml       the XmlElement that will contain the array
        * @param o         the primitive array
        * @param sElement  the name of the element containing an element value
        */
        public void writeSparseArray(XmlElement xml, Object o, String sElement)
            {
            int[] an = (int[]) o;
            int   c  = an.length;
            xml.addAttribute("length").setInt(c);
            for (int i = 0; i < c; ++i)
                {
                int n = an[i];
                if (n != 0)
                    {
                    XmlElement element = xml.addElement(sElement);
                    element.addAttribute("id").setInt(i);
                    element.setInt(n);
                    }
                }
            }

        /**
        * Write a sparse array of primitive values.
        *
        * @param xml       the XmlElement that will contain the array elements
        * @param o         the primitive array
        * @param sElement  the name of the element containing an element value
        */
        public void writeArray(XmlElement xml, Object o, String sElement)
            {
            int[] an = (int[]) o;
            for (int i = 0, c = an.length; i < c; ++i)
                {
                xml.addElement(sElement).setInt(an[i]);
                }
            }


        // ----- ExternalizableLite helpers ----------------------------

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
            // "in" contains an array length and a string of values
            int   c  = readInt(in);
            int[] an = new int[c];
            for (int i = 0; i < c; ++i)
                {
                an[i] = readInt(in);
                }
            return an;
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
            int[] an = (int[]) o;
            int   c  = an.length;
            writeInt(out, c);
            for (int i = 0; i < c; ++i)
                {
                writeInt(out, an[i]);
                }
            }
        }


    // ----- inner class:  LongArrayAdapter ---------------------------------

    /**
    * A PropertyAdapter supporting long[].
    *
    * @version 1.00  2001.03.17
    * @author cp
    */
    public static class LongArrayAdapter
            extends PrimitiveArrayAdapter
        {
        // ----- constructors ------------------------------------------

        /**
        * Construct a LongArrayAdapter.
        *
        * @param infoBean BeanInfo for a bean containing this property
        * @param clzType  the type of the property
        * @param sName    the property name
        * @param sXml     the XML tag name
        * @param xml      additional XML information
        */
        public LongArrayAdapter(XmlBean.BeanInfo infoBean, Class clzType, String sName, String sXml, XmlElement xml)
            {
            super(infoBean, clzType, sName, sXml, xml);
            azzert(clzType == long[].class);
            }


        // ----- Object method helpers ---------------------------------

        /**
        * compute a hash code for the passed object.
        *
        * @param o  the object to compute a hash code for
        *
        * @return an integer hash code
        */
        public int hash(Object o)
            {
            if (o == null)
                {
                return 0;
                }

            int n = 0;
            long[] al = (long[]) o;
            for (int i = 0, c = al.length; i < c; ++i)
                {
	            long l = al[i];
	            n ^= (int)(l ^ (l >>> 32));
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
                // we already know that (o1 == o2) failed, so o2 is not null
                return false;
                }

            long[] al1 = (long[]) o1;
            long[] al2 = (long[]) o2;
            if (al1.length != al2.length)
                {
                return false;
                }

            for (int i = 0, c = al1.length; i < c; ++i)
                {
                if (al1[i] != al2[i])
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
            return o == null ? null : ((long[]) o).clone();
            }


        // ----- XmlSerializable helpers -------------------------------

        /**
        * Determine if the specified value is empty.
        *
        * @param o  the value
        *
        * @return  true if the object is considered to be empty for
        *          persistence and XML-generation purposes
        */
        public boolean isEmpty(Object o)
            {
            return o == null || isEmptyIsNull() && ((long[]) o).length == 0;
            }

        /**
        * Read a sparse array of primitive values.
        *
        * @param iter  the iterator of XmlElement objects
        * @param c     the size of the array
        *
        * @return an array of primitive values
        */
        public Object readSparseArray(Iterator iter, int c)
            {
            long[] al = new long[c];
            while (iter.hasNext())
                {
                XmlElement elem = (XmlElement) iter.next();
                XmlValue   attr = elem.getAttribute("id");
                if (attr == null)
                    {
                    throw new IllegalArgumentException("Element " + elem.getName()
                            + " is missing the required \"id\" attribute");
                    }
                al[attr.getInt(-1)] = elem.getLong();
                }
            return al;
            }

        /**
        * Read an array of primitive values.
        *
        * @param iter  the iterator of XmlElement objects
        * @param xml   the XmlElement from which the iterator was obtained
        * @param fNested  true if the array is nested under an array tag
        *
        * @return an array of primitive values
        */
        public Object readArray(Iterator iter, XmlElement xml, boolean fNested)
            {
            // determine a good default size for the array of
            // values based on the number of xml elements that
            // are nested under the current element
            int    cXml = xml.getElementList().size();
            int    cMax = fNested ? cXml : Math.min(cXml, 32);
            long[] al   = new long[cMax];
            int    c    = 0;
            while (iter.hasNext())
                {
                // check if a larger array is needed; grow by 2x
                if (c >= cMax)
                    {
                    int    cNew  = cMax * 2;
                    long[] alNew = new long[cNew];
                    System.arraycopy(al, 0, alNew, 0, cMax);
                    al   = alNew;
                    cMax = cNew;
                    }

                XmlElement xmlValue = (XmlElement) iter.next();
                al[c++] = xmlValue.getLong();
                }

            if (c == cMax)
                {
                return al;
                }

            long[] alNew = new long[c];
            System.arraycopy(al, 0, alNew, 0, c);
            return alNew;
            }

        /**
        * Write a sparse array of primitive values.
        *
        * @param xml       the XmlElement that will contain the array
        * @param o         the primitive array
        * @param sElement  the name of the element containing an element value
        */
        public void writeSparseArray(XmlElement xml, Object o, String sElement)
            {
            long[] al = (long[]) o;
            int    c  = al.length;
            xml.addAttribute("length").setInt(c);
            for (int i = 0; i < c; ++i)
                {
                long l = al[i];
                if (l != 0L)
                    {
                    XmlElement element = xml.addElement(sElement);
                    element.addAttribute("id").setInt(i);
                    element.setLong(l);
                    }
                }
            }

        /**
        * Write a sparse array of primitive values.
        *
        * @param xml       the XmlElement that will contain the array elements
        * @param o         the primitive array
        * @param sElement  the name of the element containing an element value
        */
        public void writeArray(XmlElement xml, Object o, String sElement)
            {
            long[] al = (long[]) o;
            for (int i = 0, c = al.length; i < c; ++i)
                {
                xml.addElement(sElement).setLong(al[i]);
                }
            }


        // ----- ExternalizableLite helpers ----------------------------

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
            // "in" contains an array length and a string of values
            int    c  = readInt(in);
            long[] an = new long[c];
            for (int i = 0; i < c; ++i)
                {
                an[i] = readLong(in);
                }
            return an;
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
            long[] an = (long[]) o;
            int    c  = an.length;
            writeInt(out, c);
            for (int i = 0; i < c; ++i)
                {
                writeLong(out, an[i]);
                }
            }
        }


    // ----- inner class:  FloatArrayAdapter --------------------------------

    /**
    * A PropertyAdapter supporting float[].
    *
    * @version 1.00  2001.03.17
    * @author cp
    */
    public static class FloatArrayAdapter
            extends PrimitiveArrayAdapter
        {
        // ----- constructors ------------------------------------------

        /**
        * Construct a FloatArrayAdapter.
        *
        * @param infoBean BeanInfo for a bean containing this property
        * @param clzType  the type of the property
        * @param sName    the property name
        * @param sXml     the XML tag name
        * @param xml      additional XML information
        */
        public FloatArrayAdapter(XmlBean.BeanInfo infoBean, Class clzType, String sName, String sXml, XmlElement xml)
            {
            super(infoBean, clzType, sName, sXml, xml);
            azzert(clzType == float[].class);
            }


        // ----- Object method helpers ---------------------------------

        /**
        * compute a hash code for the passed object.
        *
        * @param o  the object to compute a hash code for
        *
        * @return an integer hash code
        */
        public int hash(Object o)
            {
            if (o == null)
                {
                return 0;
                }

            int n = 0;
            float[] afl = (float[]) o;
            for (int i = 0, c = afl.length; i < c; ++i)
                {
                n ^= Float.floatToIntBits(afl[i]);
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
                // we already know that (o1 == o2) failed, so o2 is not null
                return false;
                }

            float[] afl1 = (float[]) o1;
            float[] afl2 = (float[]) o2;
            if (afl1.length != afl2.length)
                {
                return false;
                }

            for (int i = 0, c = afl1.length; i < c; ++i)
                {
                if (afl1[i] != afl2[i])
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
            return o == null ? null : ((float[]) o).clone();
            }


        // ----- XmlSerializable helpers -------------------------------

        /**
        * Determine if the specified value is empty.
        *
        * @param o  the value
        *
        * @return  true if the object is considered to be empty for
        *          persistence and XML-generation purposes
        */
        public boolean isEmpty(Object o)
            {
            return o == null || isEmptyIsNull() && ((float[]) o).length == 0;
            }

        /**
        * Read a sparse array of primitive values.
        *
        * @param iter  the iterator of XmlElement objects
        * @param c     the size of the array
        *
        * @return an array of primitive values
        */
        public Object readSparseArray(Iterator iter, int c)
            {
            float[] afl = new float[c];
            while (iter.hasNext())
                {
                XmlElement elem = (XmlElement) iter.next();
                XmlValue   attr = elem.getAttribute("id");
                if (attr == null)
                    {
                    throw new IllegalArgumentException("Element " + elem.getName()
                            + " is missing the required \"id\" attribute");
                    }
                afl[attr.getInt(-1)] = (float) elem.getDouble();
                }
            return afl;
            }

        /**
        * Read an array of primitive values.
        *
        * @param iter  the iterator of XmlElement objects
        * @param xml   the XmlElement from which the iterator was obtained
        * @param fNested  true if the array is nested under an array tag
        *
        * @return an array of primitive values
        */
        public Object readArray(Iterator iter, XmlElement xml, boolean fNested)
            {
            if (!iter.hasNext())
                {
                return new float[0];
                }

            // determine a good default size for the array of
            // values based on the number of xml elements that
            // are nested under the current element
            int     cXml = xml.getElementList().size();
            int     cMax = fNested ? cXml : Math.min(cXml, 32);
            float[] afl = new float[cMax];
            int     c    = 0;
            while (iter.hasNext())
                {
                // check if a larger array is needed; grow by 2x
                if (c >= cMax)
                    {
                    int     cNew   = cMax * 2;
                    float[] aflNew = new float[cNew];
                    System.arraycopy(afl, 0, aflNew, 0, cMax);
                    afl  = aflNew;
                    cMax = cNew;
                    }

                XmlElement xmlValue = (XmlElement) iter.next();
                afl[c++] = (float) xmlValue.getDouble();
                }

            if (c == cMax)
                {
                return afl;
                }

            float[] aflNew = new float[c];
            System.arraycopy(afl, 0, aflNew, 0, c);
            return aflNew;
            }

        /**
        * Write a sparse array of primitive values.
        *
        * @param xml       the XmlElement that will contain the array
        * @param o         the primitive array
        * @param sElement  the name of the element containing an element value
        */
        public void writeSparseArray(XmlElement xml, Object o, String sElement)
            {
            float[] afl = (float[]) o;
            int     c   = afl.length;
            xml.addAttribute("length").setInt(c);
            for (int i = 0; i < c; ++i)
                {
                double dfl = afl[i];
                if (dfl != 0.0)
                    {
                    XmlElement element = xml.addElement(sElement);
                    element.addAttribute("id").setInt(i);
                    element.setDouble(dfl);
                    }
                }
            }

        /**
        * Write a sparse array of primitive values.
        *
        * @param xml       the XmlElement that will contain the array elements
        * @param o         the primitive array
        * @param sElement  the name of the element containing an element value
        */
        public void writeArray(XmlElement xml, Object o, String sElement)
            {
            float[] afl = (float[]) o;
            for (int i = 0, c = afl.length; i < c; ++i)
                {
                xml.addElement(sElement).setDouble(afl[i]);
                }
            }


        // ----- ExternalizableLite helpers ----------------------------

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
            return readFloatArray(in);
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
            writeFloatArray(out, (float[]) o);
            }
        }


    // ----- inner class:  DoubleArrayAdapter -------------------------------

    /**
    * A PropertyAdapter supporting double[].
    *
    * @version 1.00  2001.03.17
    * @author cp
    */
    public static class DoubleArrayAdapter
            extends PrimitiveArrayAdapter
        {
        // ----- constructors ------------------------------------------

        /**
        * Construct a DoubleArrayAdapter.
        *
        * @param infoBean BeanInfo for a bean containing this property
        * @param clzType  the type of the property
        * @param sName    the property name
        * @param sXml     the XML tag name
        * @param xml      additional XML information
        */
        public DoubleArrayAdapter(XmlBean.BeanInfo infoBean, Class clzType, String sName, String sXml, XmlElement xml)
            {
            super(infoBean, clzType, sName, sXml, xml);
            azzert(clzType == double[].class);
            }


        // ----- Object method helpers ---------------------------------

        /**
        * compute a hash code for the passed object.
        *
        * @param o  the object to compute a hash code for
        *
        * @return an integer hash code
        */
        public int hash(Object o)
            {
            if (o == null)
                {
                return 0;
                }

            int n = 0;
            double[] adfl = (double[]) o;
            for (int i = 0, c = adfl.length; i < c; ++i)
                {
	            long l = Double.doubleToLongBits(adfl[i]);
	            n ^= (int)(l ^ (l >>> 32));
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
                // we already know that (o1 == o2) failed, so o2 is not null
                return false;
                }

            double[] adfl1 = (double[]) o1;
            double[] adfl2 = (double[]) o2;
            if (adfl1.length != adfl2.length)
                {
                return false;
                }

            for (int i = 0, c = adfl1.length; i < c; ++i)
                {
                if (adfl1[i] != adfl2[i])
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
            return o == null ? null : ((double[]) o).clone();
            }


        // ----- XmlSerializable helpers -------------------------------

        /**
        * Determine if the specified value is empty.
        *
        * @param o  the value
        *
        * @return  true if the object is considered to be empty for
        *          persistence and XML-generation purposes
        */
        public boolean isEmpty(Object o)
            {
            return o == null || isEmptyIsNull() && ((double[]) o).length == 0;
            }

        /**
        * Read a sparse array of primitive values.
        *
        * @param iter  the iterator of XmlElement objects
        * @param c     the size of the array
        *
        * @return an array of primitive values
        */
        public Object readSparseArray(Iterator iter, int c)
            {
            double[] adfl = new double[c];
            while (iter.hasNext())
                {
                XmlElement elem = (XmlElement) iter.next();
                XmlValue   attr = elem.getAttribute("id");
                if (attr == null)
                    {
                    throw new IllegalArgumentException("Element " + elem.getName()
                            + " is missing the required \"id\" attribute");
                    }
                adfl[attr.getInt(-1)] = elem.getDouble();
                }
            return adfl;
            }

        /**
        * Read an array of primitive values.
        *
        * @param iter  the iterator of XmlElement objects
        * @param xml   the XmlElement from which the iterator was obtained
        * @param fNested  true if the array is nested under an array tag
        *
        * @return an array of primitive values
        */
        public Object readArray(Iterator iter, XmlElement xml, boolean fNested)
            {
            if (!iter.hasNext())
                {
                return new double[0];
                }

            // determine a good default size for the array of
            // values based on the number of xml elements that
            // are nested under the current element
            int      cXml = xml.getElementList().size();
            int      cMax = fNested ? cXml : Math.min(cXml, 32);
            double[] adfl = new double[cMax];
            int      c    = 0;
            while (iter.hasNext())
                {
                // check if a larger array is needed; grow by 2x
                if (c >= cMax)
                    {
                    int      cNew    = cMax * 2;
                    double[] adflNew = new double[cNew];
                    System.arraycopy(adfl, 0, adflNew, 0, cMax);
                    adfl = adflNew;
                    cMax = cNew;
                    }

                XmlElement xmlValue = (XmlElement) iter.next();
                adfl[c++] = xmlValue.getDouble();
                }

            if (c == cMax)
                {
                return adfl;
                }

            double[] adflNew = new double[c];
            System.arraycopy(adfl, 0, adflNew, 0, c);
            return adflNew;
            }

        /**
        * Write a sparse array of primitive values.
        *
        * @param xml       the XmlElement that will contain the array
        * @param o         the primitive array
        * @param sElement  the name of the element containing an element value
        */
        public void writeSparseArray(XmlElement xml, Object o, String sElement)
            {
            double[] adfl = (double[]) o;
            int      c    = adfl.length;
            xml.addAttribute("length").setInt(c);
            for (int i = 0; i < c; ++i)
                {
                double dfl = adfl[i];
                if (dfl != 0.0)
                    {
                    XmlElement element = xml.addElement(sElement);
                    element.addAttribute("id").setInt(i);
                    element.setDouble(dfl);
                    }
                }
            }

        /**
        * Write a sparse array of primitive values.
        *
        * @param xml       the XmlElement that will contain the array elements
        * @param o         the primitive array
        * @param sElement  the name of the element containing an element value
        */
        public void writeArray(XmlElement xml, Object o, String sElement)
            {
            double[] adfl = (double[]) o;
            for (int i = 0, c = adfl.length; i < c; ++i)
                {
                xml.addElement(sElement).setDouble(adfl[i]);
                }
            }


        // ----- ExternalizableLite helpers ----------------------------

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
            return readDoubleArray(in);
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
            writeDoubleArray(out, (double[]) o);
            }
        }
    }
