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

import java.text.SimpleDateFormat;
import java.text.DecimalFormat;
import java.text.ParsePosition;
import java.text.ParseException;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;


/**
* A property adapter for the &lt;xs:dateTime&gt; format conforming to ISO 8601
*
* @version 1.00  2002.05.21
* @author gg
*/
public class DateTimeAdapter
        extends SimpleAdapter
    {
    // ----- constructors ------------------------------------------

    /**
    * Construct a DateTimeAdapter.
    *
    * @param infoBean BeanInfo for a bean containing this property
    * @param clzType  the type of the property
    * @param sName    the property name
    * @param sXml     the XML tag name
    * @param xml      additional XML information
    */
    public DateTimeAdapter(XmlBean.BeanInfo infoBean, Class clzType, String sName, String sXml, XmlElement xml)
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
        String sDate = xml.getString(null);
        if (sDate == null)
            {
            return null;
            }
        else
            {
            try
                {
                return parse(sDate);
                }
            catch (ParseException e)
                {
                return new java.util.Date(0);
                }
            }
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
        return o == null ? null : super.toXml(format((java.util.Date) o));
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
        sUri = sUri.replace('c', ':')
                   .replace('p', '+');
        try
            {
            return parse(sUri);
            }
        catch (ParseException e)
            {
            return new java.util.Date(0);
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
        String sUri = format((java.util.Date) o);
        return sUri.replace(':', 'c')
                   .replace('+', 'p');
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
        return new Date(readLong(in));
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
        writeLong(out, ((Date) o).getTime());
        }


    // ----- helpers --------------------------------------------------------

    /** 
    * Return a Date represented by ISO8601 compliant string.
    *
    * @param sDate  an ISO8601 compliant Date string
    *
    * @return a date
    *
    * @throws ParseException  if parsing error occurs
    */
    public static Date parse(String sDate)
            throws ParseException
        {
        SimpleDateFormat parser    = s_formatter;
        String[]         asPattern = s_asPattern;

        synchronized (parser)
            {
            ParsePosition  pos = new ParsePosition(0);
            parser.applyPattern(asPattern[0]);

            Date date   = parser.parse(sDate, pos);
            int  ofErr  = pos.getErrorIndex();
            int  ofZone = ofErr < 0 ? pos.getIndex() : ofErr;
            char chZone = ofZone < sDate.length() ? sDate.charAt(ofZone) : '?';

            TimeZone tzOrig = parser.getTimeZone();
            TimeZone tz;
            switch (chZone)
                {
                case 'Z':
                    tz = getTimeZone("UTC");
                    break;
                case '+':
                case '-':
                    tz = getTimeZone("GMT" + sDate.substring(ofZone));
                    break;
                default:
                    throw new ParseException("Invalid zone format: " + sDate, ofZone);
                }

            if (date == null || !tzOrig.hasSameRules(tz))
                {
                ParseException exceptionFormat = null;
                sDate = sDate.substring(0, ofZone);
                parser.setTimeZone(tz);

                try
                    {
                    for (int i = 0, c = asPattern.length; i < c; i++)
                        {
                        try
                            {
                            parser.applyPattern(asPattern[i]);
                            date = s_formatter.parse(sDate);
                            break;
                            }
                        catch (ParseException e)
                            {
                            exceptionFormat = e;
                            }
                        }
                    }
                finally
                    {
                    parser.setTimeZone(tzOrig);
                    }

                if (date == null)
                    {
                    throw exceptionFormat == null ?
                        new ParseException("Failed to parse: " + sDate, 0) :
                        exceptionFormat;
                    }
                }

            return date;
            }
        }

    /** 
    * Return an ISO8601 string for the date/time
    * represented by this Calendar.
    *
    * @param date  a date
    *
    * @return an ISO8601 string for the date/time
    */
    public static String format(Date date)
        {
        SimpleDateFormat formatter = s_formatter;

        synchronized (formatter)
            {
            formatter.applyPattern(s_asPattern[0]);

            StringBuffer sb = new StringBuffer(formatter.format(date));

            Calendar calendar = formatter.getCalendar();
            int      iOffset  = calendar.get(java.util.Calendar.ZONE_OFFSET) +
                                calendar.get(java.util.Calendar.DST_OFFSET);
            if (iOffset == 0)
               {
               sb.append('Z');
               }
            else
               {        
               iOffset = iOffset / 60000;

               DecimalFormat df    = new DecimalFormat("00");
               int           nMin  = Math.abs(iOffset % 60),
                             nHour = Math.abs(iOffset / 60);
   
               sb.append(iOffset >= 0 ? '+' : '-')
                 .append(df.format(nHour))
                 .append(':')
                 .append(df.format(nMin));
               }

            return sb.toString();
            }
        }

    // ----- fields and constants -------------------------------------------

    /**
    * If true, force the UTC time conversion on output; 
    * otherwise use the local time zone (offset).
    */
    private static boolean s_fForceUTC = true;

    /**
    * UTC time zone
    */
    private static TimeZone s_tzUTC;

    /**
    * Formatter
    */
    private static SimpleDateFormat s_formatter;

    /**
    * Array of all allowed parsing formats. The zero element is also used
    * as the format for output conversion.
    */
    private static String[] s_asPattern = new String[]
        {
        "yyyy-MM-dd'T'HH:mm:ss.SSS",
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyyMMdd'T'HHmmss",
        };

    /**
    * Alternative parser (currenty not used)
    */
    // private static SimpleDateFormat m_parser = new SimpleDateFormat("yyyyMMdd'T'HHmmss");

    static
        {
        s_formatter = new SimpleDateFormat();
        s_tzUTC     = getTimeZone("UTC");
        if (s_fForceUTC)
            {
            s_formatter.setTimeZone(s_tzUTC);
            }
        }
    }
