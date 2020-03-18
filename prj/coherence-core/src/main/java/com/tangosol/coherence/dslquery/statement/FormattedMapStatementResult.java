/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dslquery.statement;

import java.io.PrintWriter;

import java.util.Map;
import java.util.Set;

/**
 * An implementation of a {@link com.tangosol.coherence.dslquery.StatementResult} which assumes the result is
 * a {@link Map} with value being a {@link Object} or {@link Object}[].
 * The caller may call setColumnHeaders to set the column header values and the
 * formatting of the results will be based upon the largest value in each column.
 *
 * @author tam 2014.08.05
 * @since 12.2.1
 */
public class FormattedMapStatementResult
        extends DefaultStatementResult
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct an instance with the given result which will should be a
     * {@link Map}. If the result is not a {@link Map} then it is just output
     * using super class.
     *
     * @param oResult  the result
     */
    public FormattedMapStatementResult(Object oResult)
        {
        super(oResult, true);
        }

    // ----- StatementResult interface --------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public void print(PrintWriter writer, String sTitle)
        {
        if (f_oResult instanceof Map)
            {
            if (sTitle != null)
                {
                writer.println(sTitle);
                }

            printResultsObjectMap(writer, (Map) f_oResult, f_fShowKeys);
            }
        else
            {
            printResults(writer, sTitle, f_oResult, f_fShowKeys);
            }

        writer.flush();
        }

    /**
     * Print the contents of the specified {@link Map} to the specified
     * {@link PrintWriter} and format as required.  The {@link Map} is assumed to
     * have values of either {@link Object} or {@link Object}[].
     *
     * @param writer    a PrintWriter to print on
     * @param map       the Map to print
     * @param fShowKey  a flag controlling whether to print the Maps keys
     */
    protected void printResultsObjectMap(PrintWriter writer, Map map, boolean fShowKey)
        {
        int nKeyMax      = -1;
        int anValueMax[] = null;

        // determine the max length of the key and values
        for (Map.Entry entry : (Set<Map.Entry>) map.entrySet())
            {
            Object oKey   = entry.getKey();
            Object oValue = entry.getValue();

            if (oKey.toString().length() > nKeyMax)
                {
                nKeyMax = oKey.toString().length();

                // we have not yet created the array of max values for each of the values
                // then do this now
                if (anValueMax == null)
                    {
                    int nValueLen = ensureObjectArray(oValue).length;

                    if (nValueLen == 0)
                        {
                        throw new IllegalArgumentException("Cannot use " + this.getClass() + " for Map with no value");
                        }

                    anValueMax = new int[nValueLen];

                    for (int i = 0; i < nValueLen; i++)
                        {
                        anValueMax[i] = 0;
                        }
                    }
                }

            Object[] oValueResult = ensureObjectArray(oValue);

            // get max length of results
            int i = 0;

            for (Object o : oValueResult)
                {
                int nLen = o.toString().length();

                if (nLen > anValueMax[i])
                    {
                    anValueMax[i] = nLen;
                    }

                i++;
                }
            }

        // always show keys
        if (m_asColumnHeaders != null && anValueMax != null)
            {
            if (m_asColumnHeaders.length != anValueMax.length + 1)
                {
                throw new IllegalArgumentException("The number of column headers is " + m_asColumnHeaders.length
                    + " which does not match the number of results + key value which is " + (anValueMax.length + 1));
                }

            if (m_asColumnHeaders[0].length() > nKeyMax)
                {
                nKeyMax = m_asColumnHeaders[0].length();
                }

            // output the key column header
            writer.print(rightPad(m_asColumnHeaders[0].toString(), nKeyMax) + " ");

            // output the value column headers
            for (int i = 1; i < anValueMax.length + 1; i++)
                {
                // check to see if the column label is > max
                if (m_asColumnHeaders[i].length() > anValueMax[i - 1])
                    {
                    anValueMax[i - 1] = m_asColumnHeaders[i].length();
                    }

                writer.print(rightPad(m_asColumnHeaders[i].toString(), anValueMax[i - 1]) + " ");
                }
            }

        if (map.size() > 0)
            {
            writer.println();
            writer.println(underline('-', nKeyMax, anValueMax));

            for (Map.Entry entry : (Set<Map.Entry>) map.entrySet())
                {
                Object oValue = entry.getValue();

                if (fShowKey)
                    {
                    writer.print(rightPad(entry.getKey().toString(), nKeyMax) + " ");
                    }

                Object[] oValueResult = ensureObjectArray(oValue);
                int      i            = 0;

                for (Object o : oValueResult)
                    {
                    writer.print(rightPad(o.toString(), anValueMax[i++]) + " ");
                    }

                writer.println();
                }
            }
        }

    // ----- FormattedMapStatementResult methods -------------------------------

    /**
     * Set the column headers to print.
     *
     * @param asColumnHeaders the column headers to print
     */
    public void setColumnHeaders(String[] asColumnHeaders)
        {
        m_asColumnHeaders = asColumnHeaders;
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Ensure that a value is an Object array. If the passed value is
     * an Object then convert to an Object[1], otherwise return the Object[].
     *
     * @param oValue  the value to convert to Object[]
     *
     * @return the converted Object[]
     */
    private Object[] ensureObjectArray(Object oValue)
        {
        return oValue instanceof Object[] ? (Object[]) oValue : new Object[] {(Object) oValue};
        }

    /**
     * Create an underline String
     *
     * @param cChar     the character to use for underline
     * @param nKeyLen   the length of the key
     * @param anWidths  the array of value widths
     *
     * @return a String that can be used for underline
     */
    private String underline(char cChar, int nKeyLen, int... anWidths)
        {
        StringBuilder sb = new StringBuilder();

        sb.append(fillChar(cChar, nKeyLen) + " ");

        for (int i = 0; i < anWidths.length; i++)
            {
            sb.append(fillChar(cChar, anWidths[i]) + " ");
            }

        return sb.toString();
        }

    /**
     * Return a {@link String} of characters a certain length
     *
     * @param c    the character to repeat
     * @param nLen the length of the new string
     *
     * @return the completed string
     */
    private String fillChar(char c, int nLen)
        {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < nLen; i++)
            {
            sb.append(c);
            }

        return sb.toString();
        }

    /**
     * Right pad a string up to the max length.
     *
     * @param sValue the value to right pad
     * @param nLen   the length to right pad to
     *
     * @return the formatted string
     */
    private String rightPad(String sValue, int nLen)
        {
        return String.format("%1$-" + nLen + "s", sValue);
        }

    // ----- data members ---------------------------------------------------

    /**
     * The column headers.
     */
    private String[] m_asColumnHeaders = null;
    }