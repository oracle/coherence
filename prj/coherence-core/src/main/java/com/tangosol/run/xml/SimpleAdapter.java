/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.run.xml;


import com.tangosol.io.Base64InputStream;
import com.tangosol.io.Base64OutputStream;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.math.BigDecimal;
import java.math.BigInteger;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;


/**
* A SimpleAdapter supports Java intrinsic types and a common set of Java
* classes:
*
*   java.lang.Boolean
*   java.lang.Byte
*   java.lang.Character
*   java.lang.Short
*   java.lang.Integer
*   java.lang.Long
*   java.lang.Float
*   java.lang.Double
*   java.lang.String
*   java.math.BigDecimal
*   java.math.BigInteger
*   java.sql.Date
*   java.sql.Time
*   java.sql.Timestamp
*   java.util.Date
*
* @version 1.00  2001.03.06
* @author cp
*/
public abstract class SimpleAdapter
        extends PropertyAdapter
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a SimpleAdapter.
    *
    * @param infoBean BeanInfo for a bean containing this property
    * @param clzType  the type of the property
    * @param sName    the property name
    * @param sXml     the XML tag name
    * @param xml      additional XML information
    */
    public SimpleAdapter(XmlBean.BeanInfo infoBean, Class clzType, String sName, String sXml, XmlElement xml)
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
        return false;
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
        return xml.getValue();
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
        return o == null ? null : new SimpleElement(getXmlName(), o);
        }

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
        byte[]     abValue  = Base64InputStream.decode(sUri.toCharArray());
        XmlElement xmlValue = new SimpleElement(getXmlName(), new String(abValue));

        return fromXml(xmlValue);
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
        XmlElement xmlValue = toXml(o);
        char[]     achValue = Base64OutputStream.encode(xmlValue.getString("").getBytes());

        return new String(achValue);
        }

    // ----- helpers --------------------------------------------------------

    /**
    * Parse parenthesized number string into a negative number string.
    *
    * @param sValue  the parenthesized number string
    *
    * @return a number string
    */
    protected static String parseNumber(String sValue)
        {
        int cch = sValue.length();
        if (cch > 2 && sValue.charAt(0) == '(' && sValue.charAt(cch - 1) == ')')
            {
            sValue = '-' + sValue.substring(1, sValue.length() - 1);
            }
        return sValue;
        }

    /**
    * Parse escaped string into a string.
    *
    * @param sUri  the escaped string
    *
    * @return a decoded string
    */
    public static String decodeString(String sUri)
        {
        // as is except "u" and "%" are escapes
        // (4-byte hex and 2-byte hex respectively)
        if (sUri.length() == 0)
            {
            return sUri;
            }

        StringBuffer sb     = null;
        char[]       ach    = sUri.toCharArray();
        int          cch    = ach.length;
        int          ofPrev = 0;
        for (int of = 0; of < cch; ++of)
            {
            switch (ach[of])
                {
                case 'u':
                case '%':
                    {
                    if (sb == null)
                        {
                        sb = new StringBuffer(cch);
                        }
                    if (of > ofPrev)
                        {
                        sb.append(ach, ofPrev, of - ofPrev);
                        }

                    int n = 0;
                    if (ach[of] == 'u')
                        {
                        n =            parseHex(ach[++of]);
                        n = (n << 4) + parseHex(ach[++of]);
                        }
                    n = (n << 4) + parseHex(ach[++of]);
                    n = (n << 4) + parseHex(ach[++of]);
                    sb.append((char) n);

                    ofPrev = of + 1;
                    }
                }
            }

        if (sb == null)
            {
            return sUri;
            }
        else
            {
            if (ofPrev < cch)
                {
                sb.append(ach, ofPrev, cch - ofPrev);
                }
            return sb.toString();
            }
        }

    /**
    * Parse escaped string into a string.
    *
    * @param s  the escaped string
    *
    * @return a encoded string
    */
    public static String encodeString(String s)
        {
        StringBuffer sb     = null;
        char[]       ach    = s.toCharArray();
        int          cch    = ach.length;
        int          ofPrev = 0;
        for (int of = 0; of < cch; ++of)
            {
            char ch = ach[of];
            if (ch > 0xFF)
                {
                if (sb == null)
                    {
                    sb = new StringBuffer();
                    }
                if (of > ofPrev)
                    {
                    sb.append(ach, ofPrev, of - ofPrev);
                    }
                int n = ch;
                sb.append('u')
                  .append(HEX[(n & 0xF000) >> 12])
                  .append(HEX[(n & 0x0F00) >>  8])
                  .append(HEX[(n & 0x00F0) >>  4])
                  .append(HEX[(n & 0x000F)      ]);
                ofPrev = of + 1;
                }
            else
                {
                switch (ch)
                    {
                    case 0x00: case 0x01: case 0x02: case 0x03:
                    case 0x04: case 0x05: case 0x06: case 0x07:
                    case 0x08: case 0x09: case 0x0A: case 0x0B:
                    case 0x0C: case 0x0D: case 0x0E: case 0x0F:
                    case 0x10: case 0x11: case 0x12: case 0x13:
                    case 0x14: case 0x15: case 0x16: case 0x17:
                    case 0x18: case 0x19: case 0x1A: case 0x1B:
                    case 0x1C: case 0x1D: case 0x1E: case 0x1F:
                    case ' ':  case ';':  case '/':  case '?':
                    case ':':  case '@':  case '&':  case '=':
                    case '+':  case '$':  case ',':  case '-':
                    case '.':  case '!':  case '<':  case '>':
                    case '#':  case '%':  case '"':  case '{':
                    case '}':  case '|':  case '\\': case '^':
                    case '[':  case ']':  case '`':  case 'u':
                    case '(':  case ')':
                        {
                        if (sb == null)
                            {
                            sb = new StringBuffer();
                            }
                        if (of > ofPrev)
                            {
                            sb.append(ach, ofPrev, of - ofPrev);
                            }
                        int n = ch;
                        sb.append('%')
                          .append(HEX[(n & 0xF0) >> 4])
                          .append(HEX[(n & 0x0F)     ]);
                        ofPrev = of + 1;
                        }
                    }
                }
            }
        if (sb != null && ofPrev < cch)
            {
            sb.append(ach, ofPrev, cch - ofPrev);
            }

        return sb == null ? s : sb.toString();
        }


    // ----- inner class:  BooleanAdapter -----------------------------------

    /**
    * A simple property adapter for boolean.
    *
    * @version 1.00  2001.03.18
    * @author cp
    */
    public static class BooleanAdapter
            extends SimpleAdapter
        {
        // ----- constructors ------------------------------------------

        /**
        * Construct a SimpleAdapter.
        *
        * @param infoBean BeanInfo for a bean containing this property
        * @param clzType  the type of the property
        * @param sName    the property name
        * @param sXml     the XML tag name
        * @param xml      additional XML information
        */
        public BooleanAdapter(XmlBean.BeanInfo infoBean, Class clzType, String sName, String sXml, XmlElement xml)
            {
            super(infoBean, clzType, sName, sXml, xml);
            azzert(clzType == boolean.class || clzType == Boolean.class);
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
            Object o = xml.getValue();

            if (o != null)
                {
                o = xml.getBoolean() ? Boolean.TRUE : Boolean.FALSE;
                }

            return o;
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
            // "false" or "true"
            if (sUri.equals("false"))
                {
                return Boolean.FALSE;
                }
            else if (sUri.equals("true"))
                {
                return Boolean.TRUE;
                }
            else
                {
                throw new IllegalArgumentException(
                        "Illegal boolean value: " + sUri);
                }
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
            return ((Boolean) o).booleanValue() ? "true" : "false";
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
            return in.readBoolean() ? Boolean.TRUE : Boolean.FALSE;
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
            out.writeBoolean(((Boolean) o).booleanValue());
            }
        }


    // ----- inner class:  ByteAdapter --------------------------------------

    /**
    * A simple property adapter for byte.
    *
    * @version 1.00  2001.03.18
    * @author cp
    */
    public static class ByteAdapter
            extends SimpleAdapter
        {
        // ----- constructors ------------------------------------------

        /**
        * Construct a SimpleAdapter.
        *
        * @param infoBean BeanInfo for a bean containing this property
        * @param clzType  the type of the property
        * @param sName    the property name
        * @param sXml     the XML tag name
        * @param xml      additional XML information
        */
        public ByteAdapter(XmlBean.BeanInfo infoBean, Class clzType, String sName, String sXml, XmlElement xml)
            {
            super(infoBean, clzType, sName, sXml, xml);
            azzert(clzType == byte.class || clzType == Byte.class);
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
            Object o = xml.getValue();

            if (o != null)
                {
                o = (byte) xml.getInt();
                }

            return o;
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

            o = ((Number) o).intValue();

            return new SimpleElement(getXmlName(), o);
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
            // "n" or "(n)"
            return Byte.valueOf(parseNumber(sUri));
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
            int n = ((Number) o).intValue();
            if (n < 0)
                {
                return "(" + (-n) + ")";
                }
            else
                {
                return String.valueOf(n);
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
            return in.readByte();
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
            out.writeByte(((Number) o).byteValue());
            }
        }


    // ----- inner class:  CharAdapter --------------------------------------

    /**
    * A simple property adapter for char.
    *
    * @version 1.00  2001.03.18
    * @author cp
    */
    public static class CharAdapter
            extends SimpleAdapter
        {
        // ----- constructors ------------------------------------------

        /**
        * Construct a SimpleAdapter.
        *
        * @param infoBean BeanInfo for a bean containing this property
        * @param clzType  the type of the property
        * @param sName    the property name
        * @param sXml     the XML tag name
        * @param xml      additional XML information
        */
        public CharAdapter(XmlBean.BeanInfo infoBean, Class clzType, String sName, String sXml, XmlElement xml)
            {
            super(infoBean, clzType, sName, sXml, xml);
            azzert(clzType == char.class || clzType == Character.class);
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
            Object o = xml.getValue();

            if (o != null)
                {
                o = (char) xml.getInt();
                }

            return o;
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

            o = (int) ((Character) o).charValue();

            return new SimpleElement(getXmlName(), o);
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
            String s = decodeString(sUri);
            if (s.length() != 1)
                {
                throw new IllegalArgumentException("Illegal character URI: " + sUri);
                }
            return s.charAt(0);
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
            char ch = ((Character) o).charValue();
            return encodeString(new String(new char[] {ch}));
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
            return in.readChar();
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
            out.writeChar(((Character) o).charValue());
            }
        }


    // ----- inner class:  ShortAdapter -------------------------------------

    /**
    * A simple property adapter for short.
    *
    * @version 1.00  2001.03.18
    * @author cp
    */
    public static class ShortAdapter
            extends SimpleAdapter
        {
        // ----- constructors ------------------------------------------

        /**
        * Construct a SimpleAdapter.
        *
        * @param infoBean BeanInfo for a bean containing this property
        * @param clzType  the type of the property
        * @param sName    the property name
        * @param sXml     the XML tag name
        * @param xml      additional XML information
        */
        public ShortAdapter(XmlBean.BeanInfo infoBean, Class clzType, String sName, String sXml, XmlElement xml)
            {
            super(infoBean, clzType, sName, sXml, xml);
            azzert(clzType == short.class || clzType == Short.class);
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
            Object o = xml.getValue();

            if (o != null)
                {
                o = (short) xml.getInt();
                }

            return o;
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

            o = ((Number) o).intValue();

            return new SimpleElement(getXmlName(), o);
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
            // "n" or "(n)"
            return Short.valueOf(parseNumber(sUri));
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
            int n = ((Number) o).intValue();
            if (n < 0)
                {
                return "(" + (-n) + ")";
                }
            else
                {
                return String.valueOf(n);
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
            return in.readShort();
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
            out.writeShort(((Number) o).shortValue());
            }
        }


    // ----- inner class:  IntAdapter ---------------------------------------

    /**
    * A simple property adapter for int.
    *
    * @version 1.00  2001.03.18
    * @author cp
    */
    public static class IntAdapter
            extends SimpleAdapter
        {
        // ----- constructors ------------------------------------------

        /**
        * Construct a SimpleAdapter.
        *
        * @param infoBean BeanInfo for a bean containing this property
        * @param clzType  the type of the property
        * @param sName    the property name
        * @param sXml     the XML tag name
        * @param xml      additional XML information
        */
        public IntAdapter(XmlBean.BeanInfo infoBean, Class clzType, String sName, String sXml, XmlElement xml)
            {
            super(infoBean, clzType, sName, sXml, xml);
            azzert(clzType == int.class || clzType == Integer.class);
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
            Object o = xml.getValue();

            if (o != null)
                {
                o = xml.getInt();
                }

            return o;
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
            // "n" or "(n)"
            return Integer.valueOf(parseNumber(sUri));
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
            int n = ((Integer) o).intValue();
            if (n < 0)
                {
                return "(" + (-n) + ")";
                }
            else
                {
                return String.valueOf(n);
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
            return readInt(in);
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
            writeInt(out, ((Number) o).intValue());
            }
        }


    // ----- inner class:  LongAdapter --------------------------------------

    /**
    * A simple property adapter for long.
    *
    * @version 1.00  2001.03.18
    * @author cp
    */
    public static class LongAdapter
            extends SimpleAdapter
        {
        // ----- constructors ------------------------------------------

        /**
        * Construct a SimpleAdapter.
        *
        * @param infoBean BeanInfo for a bean containing this property
        * @param clzType  the type of the property
        * @param sName    the property name
        * @param sXml     the XML tag name
        * @param xml      additional XML information
        */
        public LongAdapter(XmlBean.BeanInfo infoBean, Class clzType, String sName, String sXml, XmlElement xml)
            {
            super(infoBean, clzType, sName, sXml, xml);
            azzert(clzType == long.class || clzType == Long.class);
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
            Object o = xml.getValue();

            if (o != null)
                {
                o = xml.getLong();
                }

            return o;
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
            // "n" or "(n)"
            return Long.valueOf(parseNumber(sUri));
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
            long n = ((Long) o).longValue();
            if (n < 0)
                {
                return "(" + (-n) + ")";
                }
            else
                {
                return String.valueOf(n);
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
            return readLong(in);
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
            writeLong(out, ((Number) o).longValue());
            }
        }


    // ----- inner class:  FloatAdapter -------------------------------------

    /**
    * A simple property adapter for float.
    *
    * @version 1.00  2001.03.18
    * @author cp
    */
    public static class FloatAdapter
            extends SimpleAdapter
        {
        // ----- constructors ------------------------------------------

        /**
        * Construct a SimpleAdapter.
        *
        * @param infoBean BeanInfo for a bean containing this property
        * @param clzType  the type of the property
        * @param sName    the property name
        * @param sXml     the XML tag name
        * @param xml      additional XML information
        */
        public FloatAdapter(XmlBean.BeanInfo infoBean, Class clzType, String sName, String sXml, XmlElement xml)
            {
            super(infoBean, clzType, sName, sXml, xml);
            azzert(clzType == float.class || clzType == Float.class);
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
            Object o = xml.getValue();

            if (o != null)
                {
                o = (float) xml.getDouble();
                }

            return o;
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

            o = ((Number) o).doubleValue();

            return new SimpleElement(getXmlName(), o);
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
            // "n" or "(n)" (where n is the float's "int bits")
            return Float.intBitsToFloat(Integer.parseInt(parseNumber(sUri)));
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
            float fl = ((Float) o).floatValue();
            int   n  = Float.floatToIntBits(fl);
            if (n < 0)
                {
                return "(" + (-n) + ")";
                }
            else
                {
                return String.valueOf(n);
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
            return in.readFloat();
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
            out.writeFloat(((Number) o).floatValue());
            }
        }


    // ----- inner class:  DoubleAdapter -------------------------------------

    /**
    * A simple property adapter for double.
    *
    * @version 1.00  2001.03.18
    * @author cp
    */
    public static class DoubleAdapter
            extends SimpleAdapter
        {
        // ----- constructors ------------------------------------------

        /**
        * Construct a SimpleAdapter.
        *
        * @param infoBean BeanInfo for a bean containing this property
        * @param clzType  the type of the property
        * @param sName    the property name
        * @param sXml     the XML tag name
        * @param xml      additional XML information
        */
        public DoubleAdapter(XmlBean.BeanInfo infoBean, Class clzType, String sName, String sXml, XmlElement xml)
            {
            super(infoBean, clzType, sName, sXml, xml);
            azzert(clzType == double.class || clzType == Double.class);
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
            Object o = xml.getValue();

            if (o != null)
                {
                o = xml.getDouble();
                }

            return o;
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
            // "n" or "(n)" (where n is the double's "long bits")
            return Double.longBitsToDouble(Long.parseLong(parseNumber(sUri)));
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
            double dfl = ((Double) o).doubleValue();
            long   n   = Double.doubleToLongBits(dfl);
            if (n < 0)
                {
                return "(" + (-n) + ")";
                }
            else
                {
                return String.valueOf(n);
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
            return in.readDouble();
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
            out.writeDouble(((Number) o).doubleValue());
            }
        }


    // ----- inner class:  StringAdapter -------------------------------------

    /**
    * A simple property adapter for String.
    *
    * @version 1.00  2001.03.18
    * @author cp
    */
    public static class StringAdapter
            extends SimpleAdapter
        {
        // ----- constructors ------------------------------------------

        /**
        * Construct a SimpleAdapter.
        *
        * @param infoBean BeanInfo for a bean containing this property
        * @param clzType  the type of the property
        * @param sName    the property name
        * @param sXml     the XML tag name
        * @param xml      additional XML information
        */
        public StringAdapter(XmlBean.BeanInfo infoBean, Class clzType, String sName, String sXml, XmlElement xml)
            {
            super(infoBean, clzType, sName, sXml, xml);
            azzert(clzType == String.class);
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
            return decodeString(sUri);
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
            return encodeString((String) o);
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
            return readUTF(in);
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
        }


    // ----- inner class:  BigDecimalAdapter --------------------------------

    /**
    * A simple property adapter for BigDecimal.
    *
    * @version 1.00  2001.03.18
    * @author cp
    */
    public static class BigDecimalAdapter
            extends SimpleAdapter
        {
        // ----- constructors ------------------------------------------

        /**
        * Construct a SimpleAdapter.
        *
        * @param infoBean BeanInfo for a bean containing this property
        * @param clzType  the type of the property
        * @param sName    the property name
        * @param sXml     the XML tag name
        * @param xml      additional XML information
        */
        public BigDecimalAdapter(XmlBean.BeanInfo infoBean, Class clzType, String sName, String sXml, XmlElement xml)
            {
            super(infoBean, clzType, sName, sXml, xml);
            azzert(clzType == BigDecimal.class);
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
            Object o = xml.getValue();

            if (o != null)
                {
                o = xml.getDecimal();
                }

            return o;
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
            int of = sUri.indexOf('*');
            BigInteger IUnscaled = new BigInteger(
                    parseNumber(sUri.substring(0, of)));
            int nScale = Integer.parseInt(parseNumber(
                    sUri.substring(of + 1)));
            return new BigDecimal(IUnscaled, nScale);
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
            StringBuffer sb = new StringBuffer();
            BigDecimal dec  = (BigDecimal) o;
            String     sInt = dec.unscaledValue().toString();
            if (sInt.charAt(0) == '-')
                {
                sb.append('(')
                  .append(sInt.substring(1))
                  .append(')');
                }
            else
                {
                sb.append(sInt);
                }
            sb.append('*');
            int n = dec.scale();
            if (n < 0)
                {
                sb.append('(')
                  .append(-n)
                  .append(')');
                }
            else
                {
                sb.append(n);
                }
            return sb.toString();
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
            return readBigDecimal(in);
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
            writeBigDecimal(out, (BigDecimal) o);
            }
        }


    // ----- inner class:  BigIntegerAdapter --------------------------------

    /**
    * A simple property adapter for BigInteger.
    *
    * @version 1.00  2001.03.18
    * @author cp
    */
    public static class BigIntegerAdapter
            extends SimpleAdapter
        {
        // ----- constructors ------------------------------------------

        /**
        * Construct a SimpleAdapter.
        *
        * @param infoBean BeanInfo for a bean containing this property
        * @param clzType  the type of the property
        * @param sName    the property name
        * @param sXml     the XML tag name
        * @param xml      additional XML information
        */
        public BigIntegerAdapter(XmlBean.BeanInfo infoBean, Class clzType, String sName, String sXml, XmlElement xml)
            {
            super(infoBean, clzType, sName, sXml, xml);
            azzert(clzType == BigInteger.class);
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
            Object o = xml.getValue();

            if (o != null)
                {
                o = ((BigDecimal) o).toBigInteger();
                }

            return o;
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

            o = new BigDecimal((BigInteger) o);

            return new SimpleElement(getXmlName(), o);
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
            return new BigInteger(parseNumber(sUri));
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
            String sInt = ((BigInteger) o).toString();
            if (sInt.charAt(0) == '-')
                {
                return '(' + sInt.substring(1) + ')';
                }
            else
                {
                return sInt;
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
            return readBigInteger(in);
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
            writeBigInteger(out, (BigInteger) o);
            }
        }


    // ----- inner class:  DateAdapter --------------------------------------

    /**
    * A simple property adapter for Date.
    *
    * @version 1.00  2001.03.18
    * @author cp
    */
    public static class DateAdapter
            extends SimpleAdapter
        {
        // ----- constructors ------------------------------------------

        /**
        * Construct a SimpleAdapter.
        *
        * @param infoBean BeanInfo for a bean containing this property
        * @param clzType  the type of the property
        * @param sName    the property name
        * @param sXml     the XML tag name
        * @param xml      additional XML information
        */
        public DateAdapter(XmlBean.BeanInfo infoBean, Class clzType, String sName, String sXml, XmlElement xml)
            {
            super(infoBean, clzType, sName, sXml, xml);
            azzert(clzType == Date.class);
            }


        // ----- accessors ---------------------------------------------

        /**
        * @return true if the property value must be "deep" cloned when the
        *         containing object is cloned
        */
        public boolean isCloneRequired()
            {
            return true;
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
            return xml.getDate(null);
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
            return readDate(in);
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
            writeDate(out, (Date) o);
            }
        }


    // ----- inner class:  TimeAdapter --------------------------------------

    /**
    * A simple property adapter for Time.
    *
    * @version 1.00  2001.03.18
    * @author cp
    */
    public static class TimeAdapter
            extends SimpleAdapter
        {
        // ----- constructors ------------------------------------------

        /**
        * Construct a SimpleAdapter.
        *
        * @param infoBean BeanInfo for a bean containing this property
        * @param clzType  the type of the property
        * @param sName    the property name
        * @param sXml     the XML tag name
        * @param xml      additional XML information
        */
        public TimeAdapter(XmlBean.BeanInfo infoBean, Class clzType, String sName, String sXml, XmlElement xml)
            {
            super(infoBean, clzType, sName, sXml, xml);
            azzert(clzType == Time.class);
            }


        // ----- accessors ---------------------------------------------

        /**
        * @return true if the property value must be "deep" cloned when the
        *         containing object is cloned
        */
        public boolean isCloneRequired()
            {
            return true;
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
            Object o = xml.getValue();

            if (o != null)
                {
                o = xml.getTime();
                }

            return o;
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
            // hhmmss
            if (sUri.length() == 6)
                {
                return Time.valueOf(sUri.substring(0, 2)
                    + ':' + sUri.substring(2, 4)
                    + ':' + sUri.substring(4, 6));
                }
            else
                {
                throw new IllegalArgumentException(
                        "illegal time value: " + sUri);
                }
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
            char[] ach = o.toString().toCharArray();
            azzert(ach.length == 8); // includes delimiters
            ach[2] = ach[3]; // m
            ach[3] = ach[4]; // m
            ach[4] = ach[6]; // s
            ach[5] = ach[7]; // s
            return new String(ach, 0, 6);
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
            return readTime(in);
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
            writeTime(out, (Time) o);
            }
        }


    // ----- inner class:  TimestampAdapter -------------------------------------

    /**
    * A simple property adapter for Timestamp.
    *
    * @version 1.00  2001.03.18
    * @author cp
    */
    public static class TimestampAdapter
            extends SimpleAdapter
        {
        // ----- constructors ------------------------------------------

        /**
        * Construct a SimpleAdapter.
        *
        * @param infoBean BeanInfo for a bean containing this property
        * @param clzType  the type of the property
        * @param sName    the property name
        * @param sXml     the XML tag name
        * @param xml      additional XML information
        */
        public TimestampAdapter(XmlBean.BeanInfo infoBean, Class clzType, String sName, String sXml, XmlElement xml)
            {
            super(infoBean, clzType, sName, sXml, xml);
            azzert(clzType == Timestamp.class);
            }


        // ----- accessors ---------------------------------------------

        /**
        * @return true if the property value must be "deep" cloned when the
        *         containing object is cloned
        */
        public boolean isCloneRequired()
            {
            return true;
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
            Object o = xml.getValue();

            if (o != null)
                {
                o = xml.getDateTime();
                }

            return o;
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
            return toTimestamp(sUri);
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
            return toString(((Timestamp) o));
            }


        // ----- helpers -----------------------------------------------

        protected static Timestamp toTimestamp(String sUri)
            {
            // yyyymmddhhmmss[ffff]
            if (sUri.length() >= 14)
                {
                String sDateTime = sUri.substring(0, 4)
                    + '-' + sUri.substring(4, 6)
                    + '-' + sUri.substring(6, 8)
                    + ' ' + sUri.substring(8, 10)
                    + ':' + sUri.substring(10, 12)
                    + ':' + sUri.substring(12, 14);
                if (sUri.length() > 14)
                    {
                    sDateTime += '.' + sUri.substring(14);
                    }
                return Timestamp.valueOf(sDateTime);
                }
            else
                {
                throw new IllegalArgumentException(
                        "illegal date/time value: " + sUri);
                }
            }

        protected static String toString(Timestamp ts)
            {
            char[] ach = ts.toString().toCharArray();
            int    cch = ach.length;
            azzert(cch >= 19); // includes delimiters
            ach[ 4] = ach[ 5]; // m
            ach[ 5] = ach[ 6]; // m
            ach[ 6] = ach[ 8]; // d
            ach[ 7] = ach[ 9]; // d
            ach[ 8] = ach[11]; // h
            ach[ 9] = ach[12]; // h
            ach[10] = ach[14]; // m
            ach[11] = ach[15]; // m
            ach[12] = ach[17]; // s
            ach[13] = ach[18]; // s
            if (cch > 20)
                {
                // copy nanos
                for (int of = 20; of < cch; ++of)
                    {
                    ach[of-6] = ach[of]; // f
                    }
                // trim trailing zeros
                int of = cch - 7;
                while (of > 13 && ach[of] == '0')
                    {
                    --of;
                    }
                return new String(ach, 0, of + 1);
                }
            else
                {
                return new String(ach, 0, 14);
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
            return readTimestamp(in);
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
            writeTimestamp(out, (Timestamp) o);
            }
        }


    // ----- inner class:  OldDateAdapter -------------------------------------

    /**
    * A simple property adapter for the Date class from the java/util package.
    *
    * @version 1.00  2001.03.18
    * @author cp
    */
    public static class OldDateAdapter
            extends SimpleAdapter
        {
        // ----- constructors ------------------------------------------

        /**
        * Construct a SimpleAdapter.
        *
        * @param infoBean BeanInfo for a bean containing this property
        * @param clzType  the type of the property
        * @param sName    the property name
        * @param sXml     the XML tag name
        * @param xml      additional XML information
        */
        public OldDateAdapter(XmlBean.BeanInfo infoBean, Class clzType, String sName, String sXml, XmlElement xml)
            {
            super(infoBean, clzType, sName, sXml, xml);
            azzert(clzType == java.util.Date.class);
            }


        // ----- accessors ---------------------------------------------

        /**
        * @return true if the property value must be "deep" cloned when the
        *         containing object is cloned
        */
        public boolean isCloneRequired()
            {
            return true;
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
            Object o = xml.getValue();

            if (o != null)
                {
                Timestamp ts    = xml.getDateTime();
                long      lTime = ts.getTime();
                /*
                * Courtesy of Alin Sinpalean:
                * <quote>
                *   Prior to J2SE 1.4 the getTime() method would return only
                *   integral seconds, but it is no longer true in J2SE 1.4.
                *   The definition of the getTime() method has changed.
                *   The fix checks if the value returned by getTime() is an
                *   integral number of seconds and only in this case adds the
                *   nanoseconds (otherwise they were already added by getTime()).
                * </quote>
                */
                if (lTime % 1000 == 0)
                    {
                    lTime += (long) (ts.getNanos() / 1000000);
                    }
                o = new java.util.Date(lTime);
                }

            return o;
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

            o = new Timestamp(((java.util.Date) o).getTime());

            return new SimpleElement(getXmlName(), o);
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
            Timestamp ts = TimestampAdapter.toTimestamp(sUri);
            return new java.util.Date(ts.getTime() + ((long) (ts.getNanos() / 1000000)));
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
            Timestamp ts = new Timestamp(((java.util.Date) o).getTime());
            return TimestampAdapter.toString(ts);
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
            return new java.util.Date(readLong(in));
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
            writeLong(out, ((java.util.Date) o).getTime());
            }
        }


    // ----- inner class:  SystemTimeAdapter --------------------------------

    /**
    * A simple property adapter for Java <tt>long</tt> and
    * <tt>java.lang.Long</tt> values that is string-formatted as a
    * date/time, assuming that the long value is actualy a system time.
    *
    * @author cp  2005.06.07
    */
    public static class SystemTimeAdapter
            extends LongAdapter
        {
        // ----- constructors ------------------------------------------

        /**
        * Construct a SystemTimeAdapter, which formats a long number of
        * milliseconds as a SQL Timestamp string for XML purposes.
        *
        * @param infoBean BeanInfo for a bean containing this property
        * @param clzType  the type of the property
        * @param sName    the property name
        * @param sXml     the XML tag name
        * @param xml      additional XML information
        */
        public SystemTimeAdapter(XmlBean.BeanInfo infoBean, Class clzType, String sName, String sXml, XmlElement xml)
            {
            super(infoBean, clzType, sName, sXml, xml);
            azzert(clzType == long.class || clzType == Long.class);
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
            Object o = xml.getValue();

            if (o != null)
                {
                Timestamp ts = xml.getDateTime();
                if (ts != null)
                    {
                    o = ts.getTime();
                    }
                }

            return o;
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
            Timestamp ts = null;
            if (o != null)
                {
                ts = new Timestamp(((Long) o).longValue());
                }
            return super.toXml(ts);
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
            return TimestampAdapter.toTimestamp(sUri).getTime();
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
            Timestamp ts = null;
            if (o != null)
                {
                ts = new Timestamp(((Long) o).longValue());
                }
            return TimestampAdapter.toString(ts);
            }
        }


    // ----- constants ------------------------------------------------------

    /**
    * Hex digits.
    */
    private static final char[] HEX = "0123456789ABCDEF".toCharArray();
    }

