/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.run.xml;


import com.tangosol.io.Base64OutputStream;
import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.NotActiveException;
import java.io.OutputStream;
import java.io.PrintWriter;

import java.math.BigDecimal;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;


/**
* A simple implementation of the XmlValue interface.  Protected methods are
* provided to support inheriting classes.
*
* @author cp  2000.10.18
*/
public class SimpleValue
        extends ExternalizableHelper
        implements XmlValue, Cloneable, ExternalizableLite, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct an empty SimpleValue.
    *
    * Also used by the ExternalizableLite implementation.
    */
    public SimpleValue()
        {
        this(null, false, false);
        }

    /**
    * Construct a SimpleValue.  This form constructs an element's content
    * value from the passed Object value.  If the Object is a String, then
    * the String should be un-escaped by this point; it must not still be
    * in the form of the CDATA construct.
    *
    * @param oValue  the initial value for this SimpleValue
    *
    * @throws IllegalArgumentException  if the String value is illegal
    */
    public SimpleValue(Object oValue)
        {
        this(oValue, false, false);
        }

    /**
    * Construct a SimpleValue.  This form constructs an element's content or
    * attribute value from the passed String value.  The String should be
    * un-escaped by this point; it must not still be in the form of the CDATA
    * construct.
    *
    * @param oValue      the initial value for this SimpleValue
    * @param fAttribute  true if this SimpleValue is an element attribute
    *                    value; false if an element's content's value
    *
    * @throws IllegalArgumentException  if the String value is illegal
    */
    public SimpleValue(Object oValue, boolean fAttribute)
        {
        this(oValue, fAttribute, false);
        }

    /**
    * Construct a SimpleValue.  This form constructs an element's content or
    * attribute value from the passed String value, and also allows the
    * caller to specify that the value is immutable.  The String should be
    * un-escaped by this point; it must not still be in the form of the CDATA
    * construct.
    *
    * @param oValue      the initial value for this SimpleValue
    * @param fAttribute  true if this SimpleValue is an element attribute
    *                    value; false if an element's content's value
    * @param fReadOnly   true if this SimpleValue is intended to be read-
    *                    only once the constructor has finished
    *
    * @throws IllegalArgumentException  if the String value is illegal
    */
    public SimpleValue(Object oValue, boolean fAttribute, boolean fReadOnly)
        {
        if (oValue != null &&
            !(   oValue instanceof Boolean
              || oValue instanceof Integer
              || oValue instanceof Long
              || oValue instanceof Double
              || oValue instanceof BigDecimal
              || oValue instanceof String
              || oValue instanceof Binary
              || oValue instanceof Date
              || oValue instanceof Time
              || oValue instanceof Timestamp))
            {
            throw new IllegalArgumentException("Unsupported type: "
                    + oValue.getClass().getName());
            }

        // attribute values must not be null
        if (fAttribute && oValue == null)
            {
            oValue = "";
            }

        setAttribute(fAttribute);
        setInternalValue(oValue);
        setMutable(!fReadOnly); // must be the last call in case fReadOnly is true
        }


    // ----- XmlValue interface ---------------------------------------------

    /**
    * Get the value as a boolean.
    *
    * @return the value as a boolean
    */
    public boolean getBoolean()
        {
        return getBoolean(false);
        }

    /**
    * Get the value as a boolean.
    *
    * @param fDefault  the default return value if the internal value can
    *                  not be translated into a legal value of type boolean
    *
    * @return the value as a boolean
    */
    public boolean getBoolean(boolean fDefault)
        {
        Boolean bool = (Boolean) ensureType(TYPE_BOOLEAN);
        return bool == null ? fDefault : bool.booleanValue();
        }

    /**
    * Set the boolean value.
    *
    * @param fVal  a new value of type boolean
    */
    public void setBoolean(boolean fVal)
        {
        setInternalValue(fVal ? Boolean.TRUE : Boolean.FALSE);
        }

    /**
    * Get the value as an int.
    *
    * @return the value as an int
    */
    public int getInt()
        {
        return getInt(0);
        }

    /**
    * Get the value as an int.
    *
    * @param nDefault  the default return value if the internal value can
    *                  not be translated into a legal value of type int
    *
    * @return the value as an int
    */
    public int getInt(int nDefault)
        {
        Integer I = (Integer) ensureType(TYPE_INT);
        return I == null ? nDefault : I.intValue();
        }

    /**
    * Set the int value.
    *
    * @param nVal  a new value of type int
    */
    public void setInt(int nVal)
        {
        setInternalValue(nVal);
        }

    /**
    * Get the value as a long.
    *
    * @return the value as a long
    */
    public long getLong()
        {
        return getLong(0L);
        }

    /**
    * Get the value as a long.
    *
    * @param lDefault  the default return value if the internal value can
    *                  not be translated into a legal value of type long
    *
    * @return the value as a long
    */
    public long getLong(long lDefault)
        {
        Long L = (Long) ensureType(TYPE_LONG);
        return L == null ? lDefault : L.longValue();
        }

    /**
    * Set the long value.
    *
    * @param lVal  a new value of type long
    */
    public void setLong(long lVal)
        {
        setInternalValue(lVal);
        }

    /**
    * Get the value as a double.
    *
    * @return the value as a double
    */
    public double getDouble()
        {
        return getDouble(0.0);
        }

    /**
    * Get the value as a double.
    *
    * @param dflDefault  the default return value if the internal value can
    *                    not be translated into a legal value of type double
    *
    * @return the value as a double
    */
    public double getDouble(double dflDefault)
        {
        Double D = (Double) ensureType(TYPE_DOUBLE);
        return D == null ? dflDefault : D.doubleValue();
        }

    /**
    * Set the double value.
    *
    * @param dflVal  a new value of type double
    */
    public void setDouble(double dflVal)
        {
        setInternalValue(dflVal);
        }

    /**
    * Get the value as a decimal.
    *
    * @return the value as a BigDecimal
    */
    public BigDecimal getDecimal()
        {
        return getDecimal(DEC_ZERO);
        }

    /**
    * Get the value as a decimal.
    *
    * @param decDefault  the default return value if the internal value can
    *                    not be translated into a legal value of type decimal
    *
    * @return the value as a decimal
    */
    public BigDecimal getDecimal(BigDecimal decDefault)
        {
        BigDecimal dec = (BigDecimal) ensureType(TYPE_DECIMAL);
        return dec == null ? decDefault : dec;
        }

    /**
    * Set the dcimal value.
    *
    * @param decVal  a new value of type BigDecimal
    */
    public void setDecimal(BigDecimal decVal)
        {
        setInternalValue(decVal);
        }

    /**
    * Get the value as a String.
    *
    * @return the value as a String
    */
    public String getString()
        {
        return getString("");
        }

    /**
    * Get the value as a String.
    *
    * @param sDefault  the default return value if the internal value can
    *                  not be translated into a legal value of type String
    *
    * @return the value as a String
    */
    public String getString(String sDefault)
        {
        String s = (String) ensureType(TYPE_STRING);
        return s == null ? sDefault : s;
        }

    /**
    * Set the String value.
    *
    * @param sVal  a new value of type String
    *
    * @throws IllegalArgumentException  if the String value is null
    */
    public void setString(String sVal)
        {
        setInternalValue(sVal);
        }

    /**
    * Get the value as binary.  The XML format is expected to be Base64.
    *
    * @return the value as a Binary
    */
    public Binary getBinary()
        {
        return getBinary(NO_BYTES);
        }

    /**
    * Get the value as binary.  The XML format is expected to be Base64.
    *
    * @param binDefault  the default return value if the internal value can
    *                    not be translated into a legal value of type Binary
    *
    * @return the value as a Binary
    */
    public Binary getBinary(Binary binDefault)
        {
        Binary bin = (Binary) ensureType(TYPE_BINARY);
        return bin == null ? binDefault : bin;
        }

    /**
    * Set the binary value.
    *
    * @param binVal  a new value of type Binary
    *
    * @throws IllegalArgumentException  if the binary value is null
    */
    public void setBinary(Binary binVal)
        {
        setInternalValue(binVal);
        }

    /**
    * Get the value as a Date.
    *
    * @return the value as a Date
    */
    public Date getDate()
        {
        return getDate(DFT_DATE);
        }

    /**
    * Get the value as a Date.
    *
    * @param dtDefault  the default return value if the internal value can
    *                   not be translated into a legal value of type Date
    *
    * @return the value as a Date
    */
    public Date getDate(Date dtDefault)
        {
        Date dt = (Date) ensureType(TYPE_DATE);
        return dt == null ? dtDefault : dt;
        }

    /**
    * Set the Date value.
    *
    * @param dtVal  a new value of type Date
    */
    public void setDate(Date dtVal)
        {
        setInternalValue(dtVal);
        }

    /**
    * Get the value as a Time.
    *
    * @return the value as a Time
    */
    public Time getTime()
        {
        return getTime(DFT_TIME);
        }

    /**
    * Get the value as a Time.
    *
    * @param dtDefault  the default return value if the internal value can
    *                   not be translated into a legal value of type Time
    *
    * @return the value as a Time
    */
    public Time getTime(Time dtDefault)
        {
        Time dt = (Time) ensureType(TYPE_TIME);
        return dt == null ? dtDefault : dt;
        }

    /**
    * Set the Time value.
    *
    * @param dtVal  a new value of type Time
    */
    public void setTime(Time dtVal)
        {
        setInternalValue(dtVal);
        }

    /**
    * Get the value as a Timestamp.
    *
    * @return the value as a Timestamp
    */
    public Timestamp getDateTime()
        {
        return getDateTime(DFT_DATETIME);
        }

    /**
    * Get the value as a Timestamp.
    *
    * @param dtDefault  the default return value if the internal value can
    *                   not be translated into a legal value of type Timestamp
    *
    * @return the value as a Timestamp
    */
    public Timestamp getDateTime(Timestamp dtDefault)
        {
        Timestamp dt = (Timestamp) ensureType(TYPE_DATETIME);
        return dt == null ? dtDefault : dt;
        }

    /**
    * Set the Timestamp value.
    *
    * @param dtVal  a new value of type Timestamp
    */
    public void setDateTime(Timestamp dtVal)
        {
        setInternalValue(dtVal);
        }

    /**
    * Get the value as an Object.  The following types are supported:
    *
    *   Boolean
    *   Integer
    *   Long
    *   Double
    *   BigDecimal
    *   String
    *   Binary
    *   Date
    *   Time
    *   Timestamp
    *
    * It is always legal for an implementation to return the value as a
    * String, for example returning a binary value in a base64 encoding.
    *
    * This method exists to allow one value to copy from another value.
    *
    * @return the value as an Object or null if the XmlValue does not
    *         have a value; attributes never have a null value
    */
    public Object getValue()
        {
        return getInternalValue();
        }

    /**
    * Get the parent element of this element.
    *
    * @return the parent element, or null if this element has no parent
    */
    public XmlElement getParent()
        {
        return m_parent;
        }

    /**
    * Set the parent element of this value.  The parent cannot be modified
    * once set.
    *
    * @param element  the parent element
    *
    * @throws IllegalArgumentException thrown if the specified parent is null
    * @throws IllegalStateException throw if the parent is already set
    */
    public void setParent(XmlElement element)
        {
        if (!isMutable())
            {
            throw new UnsupportedOperationException("value \"" + this + "\" is not mutable");
            }

        if (element == null)
            {
            throw new IllegalArgumentException("parent cannot be null");
            }

        XmlElement xmlParent = getParent();
        if (xmlParent != null && xmlParent != element)
            {
            throw new IllegalStateException("parent already set");
            }

        m_parent = element;
        }

    /**
    * Determine if the value is empty.
    *
    * @return true if the value is empty
    */
    public boolean isEmpty()
        {
        Object o = getInternalValue();

        if (o == null)
            {
            return true;
            }

        if (o instanceof String && ((String) o).length() == 0)
            {
            return true;
            }

        if (o instanceof Binary && ((Binary) o).length() == 0)
            {
            return true;
            }

        return false;
        }

    /**
    * Determine if this value is an element attribute.
    *
    * @return true if this value is an element attribute, otherwise false
    */
    public boolean isAttribute()
        {
        return m_fAttribute;
        }

    /**
    * Determine if this value is an element's content.
    *
    * @return true if this value is an element's content, otherwise false
    */
    public boolean isContent()
        {
        return !m_fAttribute;
        }

    /**
    * Determine if this value can be modified.  If the value cannot be
    * modified, all mutating methods are required to throw an
    * UnsupportedOperationException.
    *
    * @return true if this value can be modified, otherwise false to
    *         indicate that this value is read-only
    */
    public boolean isMutable()
        {
        if (!m_fMutable)
            {
            return false;
            }

        XmlElement parent = getParent();
        if (parent != null && !parent.isMutable())
            {
            return false;
            }

        return true;
        }

    /**
    * Write the value as it will appear in XML.
    *
    * @param out      a PrintWriter object to use to write to
    * @param fPretty  true to specify that the output is intended to be as
    *                 human readable as possible
    */
    public void writeValue(PrintWriter out, boolean fPretty)
        {
        Object o = getInternalValue();
        if (isAttribute())
            {
            if (o instanceof Binary)
                {
                try
                    {
                    OutputStream stream = new Base64OutputStream(out, fPretty);
                    ((Binary) o).writeTo(stream);
                    stream.close();
                    }
                catch (IOException e)
                    {
                    throw ensureRuntimeException(e);
                    }
                }
            else
                {
                out.print(XmlHelper.quote(o.toString()));
                }
            }
        else
            {
            if (o instanceof String)
                {
                out.print(XmlHelper.encodeContent((String) o, true));
                }
            else if (o instanceof Binary)
                {
                try
                    {
                    OutputStream stream = new Base64OutputStream(out, fPretty);
                    ((Binary) o).writeTo(stream);
                    stream.close();
                    }
                catch (IOException e)
                    {
                    throw ensureRuntimeException(e);
                    }
                }
            else if (o != null)
                {
                out.print(o.toString());
                }
            }
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
        if (m_oValue != null || m_fAttribute || !m_fMutable)
            {
            throw new NotActiveException();
            }

        switch (in.readByte())
            {
            case 0:
                break;

            case TYPE_BOOLEAN:
                m_oValue = in.readBoolean() ? Boolean.TRUE : Boolean.FALSE;
                break;

            case TYPE_INT:
                m_oValue = readInt(in);
                break;

            case TYPE_LONG:
                m_oValue = readLong(in);
                break;

            case TYPE_DOUBLE:
                m_oValue = in.readDouble();
                break;

            case TYPE_DECIMAL:
                m_oValue = readBigDecimal(in);
                break;

            case TYPE_STRING:
                m_oValue = readUTF(in);
                break;

            case TYPE_BINARY:
                {
                Binary bin = new Binary();
                bin.readExternal(in);
                m_oValue = bin;
                }
                break;

            case TYPE_DATE:
                m_oValue = readDate(in);
                break;

            case TYPE_TIME:
                m_oValue = readTime(in);
                break;

            case TYPE_DATETIME:
                m_oValue = readTimestamp(in);
                break;

            default:
                throw new IOException();
            }

        m_fAttribute = in.readBoolean();
        m_fMutable   = in.readBoolean();
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
        // note: ExternalizableLite uses a recursive approach to serializing
        // a tree, so this object is not responsible for writing out its
        // parent

        Object o = m_oValue;
        if (o == null)
            {
            out.writeByte(0);
            }
        else if (o instanceof String)
            {
            out.writeByte(TYPE_STRING);
            writeUTF(out, (String) o);
            }
        else if (o instanceof Boolean)
            {
            out.writeByte(TYPE_BOOLEAN);
            out.writeBoolean(((Boolean) o).booleanValue());
            }
        else if (o instanceof Integer)
            {
            out.writeByte(TYPE_INT);
            writeInt(out, ((Integer) o).intValue());
            }
        else if (o instanceof Long)
            {
            out.writeByte(TYPE_LONG);
            writeLong(out, ((Long) o).longValue());
            }
        else if (o instanceof Double)
            {
            out.writeByte(TYPE_DOUBLE);
            out.writeDouble(((Double) o).doubleValue());
            }
        else if (o instanceof BigDecimal)
            {
            out.writeByte(TYPE_DECIMAL);
            writeBigDecimal(out, (BigDecimal) o);
            }
        else if (o instanceof Binary)
            {
            out.writeByte(TYPE_BINARY);
            ((Binary) o).writeExternal(out);
            }
        else if (o instanceof Date)
            {
            out.writeByte(TYPE_DATE);
            writeDate(out, (Date) o);
            }
        else if (o instanceof Time)
            {
            out.writeByte(TYPE_TIME);
            writeTime(out, (Time) o);
            }
        else if (o instanceof Timestamp)
            {
            out.writeByte(TYPE_DATETIME);
            writeTimestamp(out, (Timestamp) o);
            }
        else
            {
            throw new IOException("unsupported type to write: " + o.getClass().getName());
            }

        out.writeBoolean(m_fAttribute);
        out.writeBoolean(m_fMutable);
        }


    // ----- PortableObject interface ---------------------------------------

    /**
    * Restore the contents of a user type instance by reading its state using
    * the specified PofReader object.
    *
    * @param in  the PofReader from which to read the object's state
    *
    * @exception IOException  if an I/O error occurs
    */
    public void readExternal(PofReader in)
            throws IOException
        {
        if (m_oValue != null || m_fAttribute || !m_fMutable)
            {
            throw new NotActiveException();
            }

        switch (in.readByte(0))
            {
            case 0:
                break;

            case TYPE_BOOLEAN:
                m_oValue = in.readBoolean(1) ? Boolean.TRUE : Boolean.FALSE;
                break;

            case TYPE_INT:
                m_oValue = in.readInt(1);
                break;

            case TYPE_LONG:
                m_oValue = in.readLong(1);
                break;

            case TYPE_DOUBLE:
                m_oValue = in.readDouble(1);
                break;

            case TYPE_DECIMAL:
                m_oValue = in.readBigDecimal(1);
                break;

            case TYPE_STRING:
                m_oValue = in.readString(1);
                break;

            case TYPE_BINARY:
                m_oValue = in.readBinary(1);
                break;

            case TYPE_DATE:
                m_oValue = in.readDate(1);
                break;

            case TYPE_TIME:
                m_oValue = in.readRawTime(1).toSqlTime();
                break;

            case TYPE_DATETIME:
                m_oValue = in.readRawDateTime(1).toSqlTimestamp();
                break;

            default:
                throw new IOException();
            }

        m_fAttribute = in.readBoolean(2);
        m_fMutable   = in.readBoolean(3);
        }

    /**
    * Save the contents of a POF user type instance by writing its state using
    * the specified PofWriter object.
    *
    * @param out  the PofWriter to which to write the object's state
    *
    * @exception IOException  if an I/O error occurs
    */
    public void writeExternal(PofWriter out)
            throws IOException
        {
        // note: similarly to the ExternalizableLite approach, this object is
        // not responsible for writing out its parent

        Object o = m_oValue;
        if (o == null)
            {
            out.writeByte(0, (byte) 0);
            }
        else if (o instanceof String)
            {
            out.writeByte(0, (byte) TYPE_STRING);
            out.writeString(1, (String) o);
            }
        else if (o instanceof Boolean)
            {
            out.writeByte(0, (byte) TYPE_BOOLEAN);
            out.writeBoolean(1, ((Boolean) o).booleanValue());
            }
        else if (o instanceof Integer)
            {
            out.writeByte(0, (byte) TYPE_INT);
            out.writeInt(1, ((Integer) o).intValue());
            }
        else if (o instanceof Long)
            {
            out.writeByte(0, (byte) TYPE_LONG);
            out.writeLong(1, ((Long) o).longValue());
            }
        else if (o instanceof Double)
            {
            out.writeByte(0, (byte) TYPE_DOUBLE);
            out.writeDouble(1, ((Double) o).doubleValue());
            }
        else if (o instanceof BigDecimal)
            {
            out.writeByte(0, (byte) TYPE_DECIMAL);
            out.writeBigDecimal(1, (BigDecimal) o);
            }
        else if (o instanceof Binary)
            {
            out.writeByte(0, (byte) TYPE_BINARY);
            out.writeBinary(1, (Binary) o);
            }
        else if (o instanceof Date)
            {
            out.writeByte(0, (byte) TYPE_DATE);
            out.writeDate(1, (Date) o);
            }
        else if (o instanceof Time)
            {
            out.writeByte(0, (byte) TYPE_TIME);
            out.writeTime(1, (Time) o);
            }
        else if (o instanceof Timestamp)
            {
            out.writeByte(0, (byte) TYPE_DATETIME);
            out.writeDateTime(1, (Timestamp) o);
            }
        else
            {
            throw new IOException("unsupported type to write: " + o.getClass().getName());
            }

        out.writeBoolean(2, m_fAttribute);
        out.writeBoolean(3, m_fMutable);
        }


    // ----- support for inheriting classes ---------------------------------

    /**
    * Get the internal value of this XmlValue.
    *
    * This method acts as a single point to which all accessor calls route.
    * As such, it is intended to be extended by inheriting implementations.
    *
    * @return the current value of this SimpleValue object or null
    */
    protected Object getInternalValue()
        {
        return m_oValue;
        }

    /**
    * Update the internal representation of the XmlValue.
    *
    * This method acts as a single point to which all mutator calls route.
    * As such, it is intended to be extended by inheriting implementations.
    *
    * @param oValue  the new value for this SimpleValue object
    *
    * @throws UnsupportedOperationException if this XmlValue is not mutable
    */
    protected void setInternalValue(Object oValue)
        {
        if (!isMutable())
            {
            throw new UnsupportedOperationException(
                    "value \"" + this + "\" is not mutable");
            }

        // attribute values must not be null
        if (isAttribute() && oValue == null)
            {
            throw new IllegalArgumentException("attribute value must not be null");
            }

        m_oValue = oValue;
        }

    /**
    * Change the type of the internal representation of the XmlValue.  A
    * failed conversion will leave the value as null.
    *
    * @param nType  the enumerated type to convert to
    *
    * @return the current value of this SimpleValue object as the specified
    *         type or null
    */
    protected Object ensureType(int nType)
        {
        Object oOld = getInternalValue();
        Object oNew = convert(oOld, nType);
        if (oOld != oNew && isMutable())
            {
            setInternalValue(oNew);
            }
        return oNew;
        }

    /**
    * Convert the passed Object to the specified type.
    *
    * @param o      the object value
    * @param nType  the enumerated type to convert to
    *
    * @return an object of the specified type
    */
    protected Object convert(Object o, int nType)
        {
        try
            {
            return XmlHelper.convert(o, nType);
            }
        catch (RuntimeException e)
            {
            return null;
            }
        }

    /**
    * Specify that this value is an element attribute.
    *
    * @param fAttribute  true if this value is an element attribute, false if
    *                    this value is an element's content
    */
    protected void setAttribute(boolean fAttribute)
        {
        m_fAttribute = fAttribute;
        }

    /**
    * Specify whether this value can be modified or not.
    *
    * @param fMutable  pass true to allow this value to be modified,
    *                  otherwise false to indicate that this value is
    *                  read-only
    */
    protected void setMutable(boolean fMutable)
        {
        m_fMutable = fMutable;
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Format the XML value into a String in a display format.
    *
    * @return a String representation of the XML value
    */
    public String toString()
        {
        return getString();
        }

    /**
    * Provide a hash value for this XML value.  The hash value is defined
    * as one of the following:
    * <ol>
    * <li> 0 if getValue() returns null
    * <li> otherwise the hash value is the hashCode() of the string
    *      representation of the value
    * </ol>
    * @return the hash value for this XML value
    */
    public int hashCode()
        {
        return XmlHelper.hashValue(this);
        }

    /**
    * Compare this XML value with another XML value for equality.
    *
    * @return true if the values are equal, false otherwise
    */
    public boolean equals(Object o)
        {
        if (!(o instanceof XmlValue))
            {
            return false;
            }

        return XmlHelper.equalsValue(this, (XmlValue) o);
        }

    /**
    * Creates and returns a copy of this SimpleValue.
    *
    * The returned copy is "unlinked" from the parent and mutable
    *
    * @return  a clone of this instance.
    */
    public Object clone()
        {
        SimpleValue that;
        try
            {
            that = (SimpleValue) super.clone();
            }
        catch (CloneNotSupportedException e)
            {
            throw ensureRuntimeException(e);
            }

        Object oValue = that.m_oValue;
        if (oValue instanceof Date)
            {
            that.m_oValue = ((Date) oValue).clone();
            }
        else if (oValue instanceof Time)
            {
            that.m_oValue = ((Time) oValue).clone();
            }
        else if (oValue instanceof Timestamp)
            {
            that.m_oValue = ((Timestamp) oValue).clone();
            }

        that.m_parent   = null;
        that.m_fMutable = true;

        return that;
        }


    // ----- constants  -----------------------------------------------------

    private static final Binary     NO_BYTES     = Binary.NO_BINARY;
    private static final BigDecimal DEC_ZERO     = new BigDecimal("0");
    private static final Date       DFT_DATE     = new Date(0);
    private static final Time       DFT_TIME     = new Time(0);
    private static final Timestamp  DFT_DATETIME = new Timestamp(0);


    // ----- data members ---------------------------------------------------

    /**
    * The XmlElement object that contains this value.
    */
    private XmlElement m_parent;

    /**
    * The value of this SimpleValue object.  The SimpleValue implementation
    * supports the following types for this value:
    *
    *   Boolean
    *   Integer
    *   Long
    *   Double
    *   BigDecimal
    *   String
    *   Binary
    *   java.sql.Date
    *   java.sql.Time
    *   java.sql.Timestamp
    *
    * All values can convert through String, meaning if necessary, any of
    * the above types can be converted to String then to any other of the
    * above types.
    */
    private Object m_oValue;

    /**
    * True if an element attribute value, otherwise assumed to be element
    * content.
    */
    private boolean m_fAttribute;

    /**
    * True if this value is mutable.
    */
    private boolean m_fMutable = true;
    }
