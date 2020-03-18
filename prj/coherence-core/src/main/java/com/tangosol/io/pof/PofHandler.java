/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io.pof;


import com.tangosol.util.Binary;

import java.math.BigDecimal;
import java.math.BigInteger;


/**
* This interface defines the handler for an event-driven approach to parsing
* (or assembling) a POF stream.
*
* @author cp  2006.07.10
*
* @since Coherence 3.2
*/
public interface PofHandler
    {
    /**
    * This method is invoked when an identity is encountered in the POF
    * stream. The identity is used to uniquely identify the next value in
    * the POF stream, and can be later referenced by the
    * {@link #onIdentityReference} method.
    *
    * @param nId  if <tt>(nId &gt;= 0)</tt>, then this is the identity
    *             encountered in the POF stream, otherwise it is an indicator
    *             that the following value <i>could</i> have been assigned an
    *             identifier but was not (i.e. that the subsequent value is
    *             of a referenceable data type)
    */
    public void registerIdentity(int nId);

    /**
    * Specifies that a null value has been encountered in the POF stream.
    *
    * @param iPos  context-sensitive position information: property index
    *              within a user type, array index within an array, element
    *              counter within a collection, entry counter within a map,
    *              -1 otherwise
    */
    public void onNullReference(int iPos);

    /**
    * Specifies that a reference to a previously-identified value has been
    * encountered in the POF stream.
    *
    * @param iPos  context-sensitive position information: property index
    *              within a user type, array index within an array, element
    *              counter within a collection, entry counter within a map,
    *              -1 otherwise
    * @param nId   the identity of the previously encountered value, as was
    *              specified in a previous call to {@link #registerIdentity}
    */
    public void onIdentityReference(int iPos, int nId);

    /**
    * Report that a short integer value has been encountered in the POF
    * stream.
    *
    * @param iPos  context-sensitive position information: property index
    *              within a user type, array index within an array, element
    *              counter within a collection, entry counter within a map,
    *              -1 otherwise
    * @param n     the integer value as a short
    */
    public void onInt16(int iPos, short n);

    /**
    * Report that an integer value has been encountered in the POF stream.
    *
    * @param iPos  context-sensitive position information: property index
    *              within a user type, array index within an array, element
    *              counter within a collection, entry counter within a map,
    *              -1 otherwise
    * @param n     the integer value as an int
    */
    public void onInt32(int iPos, int n);

    /**
    * Report that a long integer value has been encountered in the POF
    * stream.
    *
    * @param iPos  context-sensitive position information: property index
    *              within a user type, array index within an array, element
    *              counter within a collection, entry counter within a map,
    *              -1 otherwise
    * @param n     the integer value as a long
    */
    public void onInt64(int iPos, long n);

    /**
    * Report that a 128-bit integer value has been encountered in the POF
    * stream.
    *
    * @param iPos  context-sensitive position information: property index
    *              within a user type, array index within an array, element
    *              counter within a collection, entry counter within a map,
    *              -1 otherwise
    * @param n     the integer value as a BigInteger
    */
    public void onInt128(int iPos, BigInteger n);

    /**
    * Report that a base-2 single-precision floating point value has been
    * encountered in the POF stream.
    *
    * @param iPos  context-sensitive position information: property index
    *              within a user type, array index within an array, element
    *              counter within a collection, entry counter within a map,
    *              -1 otherwise
    * @param fl    the floating point value as a float
    */
    public void onFloat32(int iPos, float fl);

    /**
    * Report that a base-2 double-precision floating point value has been
    * encountered in the POF stream.
    *
    * @param iPos  context-sensitive position information: property index
    *              within a user type, array index within an array, element
    *              counter within a collection, entry counter within a map,
    *              -1 otherwise
    * @param dfl   the floating point value as a double
    */
    public void onFloat64(int iPos, double dfl);

    /**
    * Report that a base-2 quad-precision floating point value has been
    * encountered in the POF stream.
    *
    * @param iPos  context-sensitive position information: property index
    *              within a user type, array index within an array, element
    *              counter within a collection, entry counter within a map,
    *              -1 otherwise
    * @param qfl   the floating point value as a quad
    */
    public void onFloat128(int iPos, RawQuad qfl);

    /**
    * Report that a single-precision decimal value (a base-10 floating point)
    * has been encountered in the POF stream.
    *
    * @param iPos  context-sensitive position information: property index
    *              within a user type, array index within an array, element
    *              counter within a collection, entry counter within a map,
    *              -1 otherwise
    * @param dec   the decimal value as a BigDecimal
    */
    public void onDecimal32(int iPos, BigDecimal dec);

    /**
    * Report that a double-precision decimal value (a base-10 floating point)
    * has been encountered in the POF stream.
    *
    * @param iPos  context-sensitive position information: property index
    *              within a user type, array index within an array, element
    *              counter within a collection, entry counter within a map,
    *              -1 otherwise
    * @param dec   the decimal value as a BigDecimal
    */
    public void onDecimal64(int iPos, BigDecimal dec);

    /**
    * Report that a quad-precision decimal value (a base-10 floating point)
    * has been encountered in the POF stream.
    *
    * @param iPos  context-sensitive position information: property index
    *              within a user type, array index within an array, element
    *              counter within a collection, entry counter within a map,
    *              -1 otherwise
    * @param dec   the decimal value as a BigDecimal
    */
    public void onDecimal128(int iPos, BigDecimal dec);

    /**
    * Report that a boolean value has been encountered in the POF stream.
    *
    * @param iPos  context-sensitive position information: property index
    *              within a user type, array index within an array, element
    *              counter within a collection, entry counter within a map,
    *              -1 otherwise
    * @param f     the boolean value
    */
    public void onBoolean(int iPos, boolean f);

    /**
    * Report that an octet value (a byte) has been encountered in the POF
    * stream.
    *
    * @param iPos  context-sensitive position information: property index
    *              within a user type, array index within an array, element
    *              counter within a collection, entry counter within a map,
    *              -1 otherwise
    * @param b     the octet value as an int whose value is in the range 0 to
    *              255 (0x00-0xFF) inclusive
    */
    public void onOctet(int iPos, int b);

    /**
    * Report that a octet string value has been encountered in the POF
    * stream.
    *
    * @param iPos  context-sensitive position information: property index
    *              within a user type, array index within an array, element
    *              counter within a collection, entry counter within a map,
    *              -1 otherwise
    * @param bin   the octet string value as a Binary object
    */
    public void onOctetString(int iPos, Binary bin);

    /**
    * Report that a character value has been encountered in the POF stream.
    *
    * @param iPos  context-sensitive position information: property index
    *              within a user type, array index within an array, element
    *              counter within a collection, entry counter within a map,
    *              -1 otherwise
    * @param ch    the character value as a char
    */
    public void onChar(int iPos, char ch);

    /**
    * Report that a character string value has been encountered in the POF
    * stream.
    *
    * @param iPos  context-sensitive position information: property index
    *              within a user type, array index within an array, element
    *              counter within a collection, entry counter within a map,
    *              -1 otherwise
    * @param s     the character string value as a String object
    */
    public void onCharString(int iPos, String s);

    /**
    * Report that a date value has been encountered in the POF stream.
    *
    * @param iPos    context-sensitive position information: property index
    *                within a user type, array index within an array, element
    *                counter within a collection, entry counter within a map,
    *                -1 otherwise
    * @param nYear   the year number as defined by ISO8601; note the
    *                difference with the Java Date class, whose year is
    *                relative to 1900
    * @param nMonth  the month number between 1 and 12 inclusive as defined
    *                by ISO8601; note the difference from the Java Date
    *                class, whose month value is 0-based (0-11)
    * @param nDay    the day number between 1 and 31 inclusive as defined by
    *                ISO8601
    */
    public void onDate(int iPos, int nYear, int nMonth, int nDay);

    /**
    * Report that a year-month interval value has been encountered in the POF
    * stream.
    *
    * @param iPos    context-sensitive position information: property index
    *                within a user type, array index within an array, element
    *                counter within a collection, entry counter within a map,
    *                -1 otherwise
    * @param cYears  the number of years in the year-month interval
    * @param cMonths the number of months in the year-month interval
    */
    public void onYearMonthInterval(int iPos, int cYears, int cMonths);

    /**
    * Report that a time value has been encountered in the POF stream.
    *
    * @param iPos    context-sensitive position information: property index
    *                within a user type, array index within an array, element
    *                counter within a collection, entry counter within a map,
    *                -1 otherwise
    * @param nHour   the hour between 0 and 23 inclusive
    * @param nMinute the minute value between 0 and 59 inclusive
    * @param nSecond the second value between 0 and 59 inclusive (and
    *                theoretically 60 for a leap-second)
    * @param nNano   the nanosecond value between 0 and 999999999 inclusive
    * @param fUTC    true if the time value is UTC or false if the time value
    *                does not have an explicit time zone
    */
    public void onTime(int iPos, int nHour, int nMinute, int nSecond,
            int nNano, boolean fUTC);

    /**
    * Report that a time value (with a timezone offset) has been encountered
    * in the POF stream.
    *
    * @param iPos          context-sensitive position information: property
    *                      index within a user type, array index within an
    *                      array, element counter within a collection, entry
    *                      counter within a map, -1 otherwise
    * @param nHour         the hour between 0 and 23 inclusive
    * @param nMinute       the minute value between 0 and 59 inclusive
    * @param nSecond       the second value between 0 and 59 inclusive (and
    *                      theoretically 60 for a leap-second)
    * @param nNano         the nanosecond value between 0 and 999999999
    *                      inclusive
    * @param nHourOffset   the timezone offset in hours from UTC, for example
    *                      0 for BST, -5 for EST and 1 for CET
    * @param nMinuteOffset the timezone offset in minutes, for example 0 (in
    *                      most cases) or 30
    *
    * @see <a href="http://www.worldtimezone.com/faq.html">worldtimezone.com</a>
    */
    public void onTime(int iPos, int nHour, int nMinute, int nSecond,
            int nNano, int nHourOffset, int nMinuteOffset);

    /**
    * Report that a time interval value has been encountered in the POF
    * stream.
    *
    * @param iPos      context-sensitive position information: property index
    *                  within a user type, array index within an array,
    *                  element counter within a collection, entry counter
    *                  within a map, -1 otherwise
    * @param cHours    the number of hours in the time interval
    * @param cMinutes  the number of minutes in the time interval, from 0 to
    *                  59 inclusive
    * @param cSeconds  the number of seconds in the time interval, from 0 to
    *                  59 inclusive
    * @param cNanos    the number of nanoseconds, from 0 to 999999999
    *                  inclusive
    */
    public void onTimeInterval(int iPos, int cHours, int cMinutes,
            int cSeconds, int cNanos);

    /**
    * Report that a date-time value has been encountered in the POF stream.
    *
    * @param iPos    context-sensitive position information: property index
    *                within a user type, array index within an array, element
    *                counter within a collection, entry counter within a map,
    *                -1 otherwise
    * @param nYear   the year number as defined by ISO8601; note the
    *                difference with the Java Date class, whose year is
    *                relative to 1900
    * @param nMonth  the month number between 1 and 12 inclusive as defined
    *                by ISO8601; note the difference from the Java Date
    *                class, whose month value is 0-based (0-11)
    * @param nDay    the day number between 1 and 31 inclusive as defined by
    *                ISO8601
    * @param nHour   the hour between 0 and 23 inclusive
    * @param nMinute the minute value between 0 and 59 inclusive
    * @param nSecond the second value between 0 and 59 inclusive (and
    *                theoretically 60 for a leap-second)
    * @param nNano   the nanosecond value between 0 and 999999999 inclusive
    * @param fUTC    true if the time value is UTC or false if the time value
    *                does not have an explicit time zone
    */
    public void onDateTime(int iPos, int nYear, int nMonth, int nDay,
            int nHour, int nMinute, int nSecond, int nNano, boolean fUTC);

    /**
    * Report that a date-time value (with a timezone offset) has been
    * encountered in the POF stream.
    *
    * @param iPos          context-sensitive position information: property
    *                      index within a user type, array index within an
    *                      array, element counter within a collection, entry
    *                      counter within a map, -1 otherwise
    * @param nYear         the year number as defined by ISO8601; note the
    *                      difference with the Java Date class, whose year is
    *                      relative to 1900
    * @param nMonth        the month number between 1 and 12 inclusive as
    *                      defined by ISO8601; note the difference from the
    *                      Java Date class, whose month value is 0-based
    *                      (0-11)
    * @param nDay          the day number between 1 and 31 inclusive as
    *                      defined by ISO8601
    * @param nHour         the hour between 0 and 23 inclusive
    * @param nMinute       the minute value between 0 and 59 inclusive
    * @param nSecond       the second value between 0 and 59 inclusive (and
    *                      theoretically 60 for a leap-second)
    * @param nNano         the nanosecond value between 0 and 999999999
    *                      inclusive
    * @param nHourOffset   the timezone offset in hours from UTC, for example
    *                      0 for BST, -5 for EST and 1 for CET
    * @param nMinuteOffset the timezone offset in minutes, for example 0 (in
    *                      most cases) or 30
    */
    public void onDateTime(int iPos, int nYear, int nMonth, int nDay,
            int nHour, int nMinute, int nSecond, int nNano,
            int nHourOffset, int nMinuteOffset);

    /**
    * Report that a day-time interval value has been encountered in the POF
    * stream.
    *
    * @param iPos      context-sensitive position information: property index
    *                  within a user type, array index within an array,
    *                  element counter within a collection, entry counter
    *                  within a map, -1 otherwise
    * @param cDays     the number of days in the day-time interval
    * @param cHours    the number of hours in the day-time interval, from 0
    *                  to 23 inclusive
    * @param cMinutes  the number of minutes in the day-time interval, from 0
    *                  to 59 inclusive
    * @param cSeconds  the number of seconds in the day-time interval, from 0
    *                  to 59 inclusive
    * @param cNanos    the number of nanoseconds in the day-time interval,
    *                  from 0 to 999999999 inclusive
    */
    public void onDayTimeInterval(int iPos, int cDays, int cHours,
            int cMinutes, int cSeconds, int cNanos);

    /**
    * Report that a collection of values has been encountered in the POF
    * stream.
    * <p>
    * This method call will be followed by a separate call to an "on" or
    * "begin" method for each of the <tt>cElements</tt> elements in the
    * collection, and the collection extent will then be terminated by a call
    * to {@link #endComplexValue()}.
    *
    * @param iPos       context-sensitive position information: property
    *                   index within a user type, array index within an
    *                   array, element counter within a collection, entry
    *                   counter within a map, -1 otherwise
    * @param cElements  the exact number of values (elements) in the
    *                   collection
    */
    public void beginCollection(int iPos, int cElements);

    /**
    * Report that a uniform collection of values has been encountered in the
    * POF stream.
    * <p>
    * This method call will be followed by a separate call to an "on" or
    * "begin" method for each of the <tt>cElements</tt> elements in the
    * collection, and the collection extent will then be terminated by a call
    * to {@link #endComplexValue()}.
    *
    * @param iPos       context-sensitive position information: property
    *                   index within a user type, array index within an
    *                   array, element counter within a collection, entry
    *                   counter within a map, -1 otherwise
    * @param cElements  the exact number of values (elements) in the
    *                   collection
    * @param nType      the type identifier for all of the values in the
    *                   uniform collection
    */
    public void beginUniformCollection(int iPos, int cElements, int nType);

    /**
    * Report that an array of values has been encountered in the POF stream.
    * <p>
    * This method call will be followed by a separate call to an "on" or
    * "begin" method for each of the <tt>cElements</tt> elements in the
    * array, and the array extent will then be terminated by a call to
    * {@link #endComplexValue()}.
    *
    * @param iPos       context-sensitive position information: property
    *                   index within a user type, array index within an
    *                   array, element counter within a collection, entry
    *                   counter within a map, -1 otherwise
    * @param cElements  the exact number of values (elements) in the array
    */
    public void beginArray(int iPos, int cElements);

    /**
    * Report that a uniform array of values has been encountered in the POF
    * stream.
    * <p>
    * This method call will be followed by a separate call to an "on" or
    * "begin" method for each of the <tt>cElements</tt> elements in the
    * array, and the array extent will then be terminated by a call to
    * {@link #endComplexValue()}.
    *
    * @param iPos       context-sensitive position information: property
    *                   index within a user type, array index within an
    *                   array, element counter within a collection, entry
    *                   counter within a map, -1 otherwise
    * @param cElements  the exact number of values (elements) in the array
    * @param nType      the type identifier for all of the values in the
    *                   uniform array
    */
    public void beginUniformArray(int iPos, int cElements, int nType);

    /**
    * Report that a sparse array of values has been encountered in the POF
    * stream.
    * <p>
    * This method call will be followed by a separate call to an "on" or
    * "begin" method for present element in the sparse array (up to
    * <tt>cElements</tt> elements), and the array extent will then be
    * terminated by a call to {@link #endComplexValue()}.
    *
    * @param iPos       context-sensitive position information: property
    *                   index within a user type, array index within an
    *                   array, element counter within a collection, entry
    *                   counter within a map, -1 otherwise
    * @param cElements  the exact number of elements in the array, which is
    *                   greater than or equal to the number of values in the
    *                   sparse POF stream; in other words, the number of
    *                   values that will subsequently be reported will not
    *                   exceed this number
    */
    public void beginSparseArray(int iPos, int cElements);

    /**
    * Report that a uniform sparse array of values has been encountered in
    * the POF stream.
    * <p>
    * This method call will be followed by a separate call to an "on" or
    * "begin" method for present element in the sparse array (up to
    * <tt>cElements</tt> elements), and the array extent will then be
    * terminated by a call to {@link #endComplexValue()}.
    *
    * @param iPos       context-sensitive position information: property
    *                   index within a user type, array index within an
    *                   array, element counter within a collection, entry
    *                   counter within a map, -1 otherwise
    * @param cElements  the exact number of elements in the array, which is
    *                   greater than or equal to the number of values in the
    *                   sparse POF stream; in other words, the number of
    *                   values that will subsequently be reported will not
    *                   exceed this number
    * @param nType      the type identifier for all of the values in the
    *                   uniform sparse array
    */
    public void beginUniformSparseArray(int iPos, int cElements, int nType);

    /**
    * Report that a map of key/value pairs has been encountered in the POF
    * stream.
    * <p>
    * This method call will be followed by a separate call to an "on" or
    * "begin" method for each of the <tt>cElements</tt> elements in the
    * map, and the map extent will then be terminated by a call to
    * {@link #endComplexValue()}.
    *
    * @param iPos       context-sensitive position information: property
    *                   index within a user type, array index within an
    *                   array, element counter within a collection, entry
    *                   counter within a map, -1 otherwise
    * @param cElements  the exact number of key/value pairs (entries) in the
    *                   map
    */
    public void beginMap(int iPos, int cElements);

    /**
    * Report that a map of key/value pairs (with the keys being of a uniform
    * type) has been encountered in the POF stream.
    * <p>
    * This method call will be followed by a separate call to an "on" or
    * "begin" method for each of the <tt>cElements</tt> elements in the
    * map, and the map extent will then be terminated by a call to
    * {@link #endComplexValue()}.
    *
    * @param iPos       context-sensitive position information: property
    *                   index within a user type, array index within an
    *                   array, element counter within a collection, entry
    *                   counter within a map, -1 otherwise
    * @param cElements  the exact number of key/value pairs (entries) in the
    *                   map
    * @param nTypeKeys  the type identifier for all of the keys in the
    *                   uniform-keys map
    */
    public void beginUniformKeysMap(int iPos, int cElements, int nTypeKeys);

    /**
    * Report that a map of key/value pairs (with the keys being of a uniform
    * type and the values being of a uniform type) has been encountered in
    * the POF stream.
    * <p>
    * This method call will be followed by a separate call to an "on" or
    * "begin" method for each of the <tt>cElements</tt> elements in the
    * map, and the map extent will then be terminated by a call to
    * {@link #endComplexValue()}.
    *
    * @param iPos         context-sensitive position information: property
    *                     index within a user type, array index within an
    *                     array, element counter within a collection, entry
    *                     counter within a map, -1 otherwise
    * @param cElements    the exact number of key/value pairs (entries) in
    *                     the map
    * @param nTypeKeys    the type identifier for all of the keys in the
    *                     uniform map
    * @param nTypeValues  the type identifier for all of the values in the
    *                     uniform map
    */
    public void beginUniformMap(int iPos, int cElements, int nTypeKeys, int nTypeValues);

    /**
    * Report that a value of a "user type" has been encountered in the POF
    * stream. A user type is analogous to a "class", and a value of a user
    * type is analogous to an "object".
    * <p>
    * This method call will be followed by a separate call to an "on" or
    * "begin" method for each of the property values in the user type, and
    * the user type will then be terminated by a call to
    * {@link #endComplexValue()}.
    *
    * @param iPos         context-sensitive position information: property
    *                     index within a user type, array index within an
    *                     array, element counter within a collection, entry
    *                     counter within a map, -1 otherwise
    * @param nUserTypeId  the user type identifier,
    *                     <tt>(nUserTypeId &gt;= 0)</tt>
    * @param nVersionId   the version identifier for the user data type data
    *                     in the POF stream, <tt>(nVersionId &gt;= 0)</tt>
    */
    public void beginUserType(int iPos, int nUserTypeId, int nVersionId);

    /**
    * Signifies the termination of the current complex value. Complex values
    * are any of the collection, array, map and user types. For each call to
    * one of the "begin" methods, there will be a corresponding call to this
    * method, even if there were no contents in the complex value.
    */
    public void endComplexValue();
    }
