/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io.pof;


import com.tangosol.util.Binary;

import java.io.PrintWriter;

import java.math.BigDecimal;
import java.math.BigInteger;


/**
* An implementation of PofHandler that logs all of the stream contents for
* debugging / testing purposes.
*
* @author cp  2006.07.11
*
* @since Coherence 3.2
*/
public class LoggingPofHandler
        extends PofHelper
        implements PofHandler
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor: Construct a Logging POF Handler that sends all
    * logging output to standard output.
    */
    public LoggingPofHandler()
        {
        }

    /**
    * Construct a Logging POF Handler that sends all logging output to the
    * specified PrintWriter.
    *
    * @param writer  the PrintWriter for logging output
    */
    public LoggingPofHandler(PrintWriter writer)
        {
        setPrintWriter(writer);
        }


    // ----- PofHandler interface -------------------------------------------

    /**
    * {@inheritDoc}
    */
    public void registerIdentity(int nId)
        {
        String sIdInfo = m_sIdInfo;
        if (sIdInfo == null)
            {
            sIdInfo = String.valueOf(nId);
            }
        else
            {
            sIdInfo += ", " + nId;
            }
        m_sIdInfo = sIdInfo;
        }

    /**
    * {@inheritDoc}
    */
    public void onNullReference(int iPos)
        {
        show(iPos, "null");
        }

    /**
    * {@inheritDoc}
    */
    public void onIdentityReference(int iPos, int nId)
        {
        show(iPos, "reference=" + nId);
        }

    /**
    * {@inheritDoc}
    */
    public void onInt16(int iPos, short n)
        {
        show(iPos, "int16=" + n
                + " (0x" + Integer.toHexString(n).toUpperCase() + ")");
        }

    /**
    * {@inheritDoc}
    */
    public void onInt32(int iPos, int n)
        {
        show(iPos, "int32=" + n
                + " (0x" + Integer.toHexString(n).toUpperCase() + ")");
        }

    /**
    * {@inheritDoc}
    */
    public void onInt64(int iPos, long n)
        {
        show(iPos, "int64=" + n
                + " (0x" + Long.toHexString(n).toUpperCase() + ")");
        }

    /**
    * {@inheritDoc}
    */
    public void onInt128(int iPos, BigInteger n)
        {
        show(iPos, "int128=" + n);
        }

    /**
    * {@inheritDoc}
    */
    public void onFloat32(int iPos, float fl)
        {
        show(iPos, "float32=" + fl);
        }

    /**
    * {@inheritDoc}
    */
    public void onFloat64(int iPos, double dfl)
        {
        show(iPos, "float64=" + dfl);
        }

    /**
    * {@inheritDoc}
    */
    public void onFloat128(int iPos, RawQuad qfl)
        {
        show(iPos, "float128 (bits)=" + qfl.getBits());
        }

    /**
    * {@inheritDoc}
    */
    public void onDecimal32(int iPos, BigDecimal dec)
        {
        show(iPos, "decimal32=" + dec);
        }

    /**
    * {@inheritDoc}
    */
    public void onDecimal64(int iPos, BigDecimal dec)
        {
        show(iPos, "decimal64=" + dec);
        }

    /**
    * {@inheritDoc}
    */
    public void onDecimal128(int iPos, BigDecimal dec)
        {
        show(iPos, "decimal128=" + dec);
        }

    /**
    * {@inheritDoc}
    */
    public void onBoolean(int iPos, boolean f)
        {
        show(iPos, "boolean=" + f);
        }

    /**
    * {@inheritDoc}
    */
    public void onOctet(int iPos, int b)
        {
        show(iPos, "octet=" + (b & 0xFF) + " (0x" + toHex(b) + ")");
        }

    /**
    * {@inheritDoc}
    */
    public void onOctetString(int iPos, Binary bin)
        {
        if (bin.length() > 16)
            {
            show(iPos, "binary=Binary(length=" + bin.length() + ", value=");
            print(indentString(toHexDump(bin.toByteArray(), 16), getIndent() + "    "));
            }
        else
            {
            show(iPos, "binary=" + bin);
            }
        }

    /**
    * {@inheritDoc}
    */
    public void onChar(int iPos, char ch)
        {
        show(iPos, "char=" + toQuotedCharEscape(ch) + " (" + ((int) ch)
                + " / 0x" + Integer.toHexString(ch).toUpperCase() + ")");

        }

    /**
    * {@inheritDoc}
    */
    public void onCharString(int iPos, String s)
        {
        show(iPos, "char-string=" + toQuotedStringEscape(s));
        }

    /**
    * {@inheritDoc}
    */
    public void onDate(int iPos, int nYear, int nMonth, int nDay)
        {
        show(iPos, "date=" + formatDate(nYear, nMonth, nDay));
        }

    /**
    * {@inheritDoc}
    */
    public void onYearMonthInterval(int iPos, int cYears, int cMonths)
        {
        show(iPos, "year-month-inteval=" + cYears + "Y" + cMonths + "M");
        }

    /**
    * {@inheritDoc}
    */
    public void onTime(int iPos, int nHour, int nMinute, int nSecond,
            int nNano, boolean fUTC)
        {
        show(iPos, "time=" + formatTime(nHour, nMinute, nSecond, nNano, fUTC));
        }

    /**
    * {@inheritDoc}
    */
    public void onTime(int iPos, int nHour, int nMinute, int nSecond,
            int nNano, int nHourOffset, int nMinuteOffset)
        {
        show(iPos, "time=" + formatTime(nHour, nMinute, nSecond, nNano,
                nHourOffset, nMinuteOffset));
        }

    /**
    * {@inheritDoc}
    */
    public void onTimeInterval(int iPos, int cHours, int cMinutes,
            int cSeconds, int cNanos)
        {
        show(iPos, "time-interval="
                + formatTime(cHours, cMinutes, cSeconds, cNanos, false));
        }

    /**
    * {@inheritDoc}
    */
    public void onDateTime(int iPos, int nYear, int nMonth, int nDay,
            int nHour, int nMinute, int nSecond, int nNano, boolean fUTC)
        {
        show(iPos, "date-time="
                + formatDate(nYear, nMonth, nDay)
                + ' '
                + formatTime(nHour, nMinute, nSecond, nNano, fUTC));
        }

    /**
    * {@inheritDoc}
    */
    public void onDateTime(int iPos, int nYear, int nMonth, int nDay,
            int nHour, int nMinute, int nSecond, int nNano,
            int nHourOffset, int nMinuteOffset)
        {
        show(iPos, "date-time="
                + formatDate(nYear, nMonth, nDay)
                + ' '
                + formatTime(nHour, nMinute, nSecond, nNano,
                             nHourOffset, nMinuteOffset));
        }

    /**
    * {@inheritDoc}
    */
    public void onDayTimeInterval(int iPos, int cDays, int cHours,
            int cMinutes, int cSeconds, int cNanos)
        {
        show(iPos, "day-time-interval=" + cDays + "T"
                + formatTime(cHours, cMinutes, cSeconds, cNanos, false));
        }

    /**
    * {@inheritDoc}
    */
    public void beginCollection(int iPos, int cElements)
        {
        begin(iPos, "collection[" + cElements + "]");
        }

    /**
    * {@inheritDoc}
    */
    public void beginUniformCollection(int iPos, int cElements, int nType)
        {
        begin(iPos, "uniform-collection[" + cElements + "] (type-id="
                + nType + ")");
        }

    /**
    * {@inheritDoc}
    */
    public void beginArray(int iPos, int cElements)
        {
        begin(iPos, "array[" + cElements + "]");
        }

    /**
    * {@inheritDoc}
    */
    public void beginUniformArray(int iPos, int cElements, int nType)
        {
        begin(iPos, "uniform-array[" + cElements + "] (type-id="
                + nType + ")");
        }

    /**
    * {@inheritDoc}
    */
    public void beginSparseArray(int iPos, int cElements)
        {
        begin(iPos, "sparse-array[" + cElements + "]");
        }

    /**
    * {@inheritDoc}
    */
    public void beginUniformSparseArray(int iPos, int cElements, int nType)
        {
        begin(iPos, "uniform-sparse-array[" + cElements + "] (type-id="
                + nType + ")");
        }

    /**
    * {@inheritDoc}
    */
    public void beginMap(int iPos, int cElements)
        {
        begin(iPos, "uniform-map[" + cElements + "]");
        }

    /**
    * {@inheritDoc}
    */
    public void beginUniformKeysMap(int iPos, int cElements, int nTypeKeys)
        {
        begin(iPos, "uniform-keys-map[" + cElements + "] (key type-id="
                + nTypeKeys + ")");
        }

    /**
    * {@inheritDoc}
    */
    public void beginUniformMap(int iPos, int cElements,
                                int nTypeKeys, int nTypeValues)
        {
        begin(iPos, "uniform-map[" + cElements + "] (key type-id="
                + nTypeKeys + ", value type-id=" + nTypeValues + ")");
        }

    /**
    * {@inheritDoc}
    */
    public void beginUserType(int iPos, int nUserTypeId, int nVersionId)
        {
        begin(iPos, "user-type " + nUserTypeId + " (v" + nVersionId + ")");
        }

    /**
    * {@inheritDoc}
    */
    public void endComplexValue()
        {
        end();
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Obtain the PrintWriter that is used by this LoggingPofHandler.
    *
    * @return the current PrintWriter, or null if none is used
    */
    public PrintWriter getPrintWriter()
        {
        return m_writer;
        }

    /**
    * Specify a PrintWriter to be used by this LoggingPofHandler.
    *
    * @param writer  the PrintWriter to use, or null to use standard output
    */
    public void setPrintWriter(PrintWriter writer)
        {
        m_writer = writer;
        }

    /**
    * Determine the current nested data structure depth within the POF
    * stream. Every time a data structure (e.g. a user type, a collection) is
    * begun, the depth increases by one, and every time a data structure
    * ends, the depth is decreased by one.
    *
    * @return the current complex data structure depth
    */
    protected int getDepth()
        {
        return m_cDepth;
        }

    /**
    * Determine the current textual indent for logging, which is based on the
    * nested data structure depth within the POF stream.
    *
    * @return the current textual indent for logging
    */
    protected String getIndent()
        {
        return m_sIndent;
        }


    // ----- internal methods -----------------------------------------------

    /**
    * Begin a complex data structure, such as a user type or a collection.
    *
    * @param iPos  the current position with a containing data structure
    * @param s     a description to log
    */
    protected void begin(int iPos, String s)
        {
        show(iPos, s);
        adjustDepth(1);
        }

    /**
    * End a complex data structure.
    */
    protected void end()
        {
        adjustDepth(-1);
        }

    /**
    * Adjust the complex data structure depth.
    *
    * @param cDelta  either +1 or -1
    */
    protected void adjustDepth(int cDelta)
        {
        if (cDelta < 0)
            {
            show(-1, "}");
            }

        int cDepth = Math.max(0, m_cDepth + cDelta);

        m_cDepth  = cDepth;
        m_sIndent = dup("  ", cDepth);

        if (cDelta > 0)
            {
            show(-1, "{");
            }
        }

    /**
    * Log information related to the POF stream.
    *
    * @param iPos  the current position with a containing data structure
    * @param s     a description to log
    */
    protected void show(int iPos, String s)
        {
        StringBuffer sb = new StringBuffer();
        sb.append(m_sIndent);

        if (iPos >= 0 && m_cDepth > 0)
            {
            sb.append('[')
              .append(iPos)
              .append("]=");
            }

        sb.append(s);

        String sIdInfo = m_sIdInfo;
        if (sIdInfo != null)
            {
            sb.append("    ; id=")
              .append(sIdInfo);

            // clear it out
            m_sIdInfo = null;
            }

        print(sb.toString());
        }

    /**
    * Print the passed String.
    *
    * @param s  the String to print
    */
    protected void print(String s)
        {
        PrintWriter writer = m_writer;
        if (writer == null)
            {
            out(s);
            }
        else
            {
            writer.println(s);
            }
        }


    // ----- data members ---------------------------------------------------

    /**
    * The optional PrintWriter to use to log to.
    */
    private PrintWriter m_writer;

    /**
    * The current depth of complex values.
    */
    private int m_cDepth;

    /**
    * The string indentation for purposes of showing hierarchy.
    */
    private String m_sIndent = "";

    /**
    * The reference identifier information for the next value, or null.
    */
    private String m_sIdInfo;
    }
