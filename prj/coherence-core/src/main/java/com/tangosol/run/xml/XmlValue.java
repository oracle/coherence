/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.run.xml;


import com.tangosol.util.Binary;

import java.io.PrintWriter;
import java.io.Serializable;

import java.math.BigDecimal;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;


/**
* An interface for XML element content and element attribute values.
*
* @author cp  2000.10.18
*/
public interface XmlValue
        extends Serializable
    {
    // ----- accessors and mutators by type ---------------------------------

    /**
    * Get the value as a boolean.
    *
    * @return the value as a boolean
    */
    public boolean getBoolean();

    /**
    * Set the boolean value.
    *
    * @param fVal  a new value of type boolean
    */
    public void setBoolean(boolean fVal);

    /**
    * Get the value as an int.
    *
    * @return the value as an int
    */
    public int getInt();

    /**
    * Set the int value.
    *
    * @param nVal  a new value of type int
    */
    public void setInt(int nVal);

    /**
    * Get the value as a long.
    *
    * @return the value as a long
    */
    public long getLong();

    /**
    * Set the long value.
    *
    * @param lVal  a new value of type long
    */
    public void setLong(long lVal);

    /**
    * Get the value as a double.
    *
    * @return the value as a double
    */
    public double getDouble();

    /**
    * Set the double value.
    *
    * @param dflVal  a new value of type double
    */
    public void setDouble(double dflVal);

    /**
    * Get the value as a decimal.
    *
    * @return the value as a BigDecimal
    */
    public BigDecimal getDecimal();

    /**
    * Set the dcimal value.
    *
    * @param decVal  a new value of type BigDecimal
    */
    public void setDecimal(BigDecimal decVal);

    /**
    * Get the value as a String.
    *
    * @return the value as a String
    */
    public String getString();

    /**
    * Set the String value.
    *
    * @param sVal  a new value of type String
    */
    public void setString(String sVal);

    /**
    * Get the value as binary.  The XML format is expected to be Base64.
    *
    * @return the value as a Binary object
    */
    public Binary getBinary();

    /**
    * Set the binary value.
    *
    * @param binVal  a new value of type Binary
    */
    public void setBinary(Binary binVal);

    /**
    * Get the value as a Date.
    *
    * @return the value as a Date
    */
    public Date getDate();

    /**
    * Set the Date value.
    *
    * @param dtVal  a new value of type Date
    */
    public void setDate(Date dtVal);

    /**
    * Get the value as a Time.
    *
    * @return the value as a Time
    */
    public Time getTime();

    /**
    * Set the Time value.
    *
    * @param dtVal  a new value of type Time
    */
    public void setTime(Time dtVal);

    /**
    * Get the value as a Timestamp.
    *
    * @return the value as a Timestamp
    */
    public Timestamp getDateTime();

    /**
    * Set the Timestamp value.
    *
    * @param dtVal  a new value of type Timestamp
    */
    public void setDateTime(Timestamp dtVal);


    // ----- accessors with default return values ---------------------------

    /**
    * Get the value as a boolean.
    *
    * @param fDefault  the default return value if the internal value can
    *                  not be translated into a legal value of type boolean
    *
    * @return the value as a boolean
    */
    public boolean getBoolean(boolean fDefault);

    /**
    * Get the value as an int.
    *
    * @param nDefault  the default return value if the internal value can
    *                  not be translated into a legal value of type int
    *
    * @return the value as an int
    */
    public int getInt(int nDefault);

    /**
    * Get the value as a long.
    *
    * @param lDefault  the default return value if the internal value can
    *                  not be translated into a legal value of type long
    *
    * @return the value as a long
    */
    public long getLong(long lDefault);

    /**
    * Get the value as a double.
    *
    * @param dflDefault  the default return value if the internal value can
    *                    not be translated into a legal value of type double
    *
    * @return the value as a double
    */
    public double getDouble(double dflDefault);

    /**
    * Get the value as a decimal.
    *
    * @param decDefault  the default return value if the internal value can
    *                    not be translated into a legal value of type decimal
    *
    * @return the value as a decimal
    */
    public BigDecimal getDecimal(BigDecimal decDefault);

    /**
    * Get the value as a String.
    *
    * @param sDefault  the default return value if the internal value can
    *                  not be translated into a legal value of type String
    *
    * @return the value as a String
    */
    public String getString(String sDefault);

    /**
    * Get the value as binary.  The XML format is expected to be Base64.
    *
    * @param binDefault  the default return value if the internal value can
    *                    not be translated into a legal value of type Binary
    *
    * @return the value as a Binary object
    */
    public Binary getBinary(Binary binDefault);

    /**
    * Get the value as a Date.
    *
    * @param dtDefault  the default return value if the internal value can
    *                   not be translated into a legal value of type Date
    *
    * @return the value as a Date
    */
    public Date getDate(Date dtDefault);

    /**
    * Get the value as a Time.
    *
    * @param dtDefault  the default return value if the internal value can
    *                   not be translated into a legal value of type Time
    *
    * @return the value as a Time
    */
    public Time getTime(Time dtDefault);

    /**
    * Get the value as a Timestamp.
    *
    * @param dtDefault  the default return value if the internal value can
    *                   not be translated into a legal value of type Timestamp
    *
    * @return the value as a Timestamp
    */
    public Timestamp getDateTime(Timestamp dtDefault);


    // ----- miscellaneous --------------------------------------------------

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
    public Object getValue();

    /**
    * Get the parent element of this value.
    *
    * @return the parent element, or null if this value has no parent
    */
    public XmlElement getParent();

    /**
    * Set the parent element of this value.  The parent can not be modified
    * once set.
    *
    * @param element  the parent element
    *
    * @throws IllegalArgumentException thrown if the specified parent is null
    * @throws IllegalStateException throw if the parent is already set
    */
    public void setParent(XmlElement element);

    /**
    * Determine if the value is empty.
    *
    * @return true if the value is empty
    */
    public boolean isEmpty();

    /**
    * Determine if this value is an element attribute.
    *
    * @return true if this value is an element attribute, otherwise false
    */
    public boolean isAttribute();

    /**
    * Determine if this value is an element's content.
    *
    * @return true if this value is an element's content, otherwise false
    */
    public boolean isContent();

    /**
    * Determine if this value can be modified.  If the value can not be
    * modified, all mutating methods are required to throw an
    * UnsupportedOperationException.
    *
    * @return true if this value can be modified, otherwise false to
    *         indicate that this value is read-only
    */
    public boolean isMutable();

    /**
    * Write the value as it will appear in XML.
    *
    * @param out      a PrintWriter object to use to write to
    * @param fPretty  true to specify that the output is intended to be as
    *                 human readable as possible
    */
    public void writeValue(PrintWriter out, boolean fPretty);


    // ----- Object methods -------------------------------------------------

    /**
    * Format the XML value into a String in a display format.
    *
    * @return a String representation of the XML value
    */
    public String toString();

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
    public int hashCode();

    /**
    * Compare this XML value with another XML value for equality.
    *
    * @return true if the values are equal, false otherwise
    */
    public boolean equals(Object o);

    /**
    * Creates and returns a copy of this SimpleValue.
    *
    * The returned copy is "unlinked" from the parent and mutable
    *
    * @return  a clone of this instance.
    */
    public Object clone();


    // ----- enumerated types useful for implementating this interface ------

    public static final int TYPE_BOOLEAN    = 1;
    public static final int TYPE_INT        = 2;
    public static final int TYPE_LONG       = 3;
    public static final int TYPE_DOUBLE     = 4;
    public static final int TYPE_DECIMAL    = 5;
    public static final int TYPE_STRING     = 6;
    public static final int TYPE_BINARY     = 7;
    public static final int TYPE_DATE       = 8;
    public static final int TYPE_TIME       = 9;
    public static final int TYPE_DATETIME   = 10;
    }