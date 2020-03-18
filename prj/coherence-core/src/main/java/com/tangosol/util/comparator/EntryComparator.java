/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util.comparator;


import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;

import com.tangosol.util.QueryMap;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.Comparator;
import java.util.Map;

import javax.json.bind.annotation.JsonbProperty;


/**
* Comparator implementation used to compare map entries. Depending on the
* comparison style this comparator will compare entries' values, entries'
* keys or, when the provided comparator is an instance of
* {@link QueryMapComparator}, the entries themselves.
*
* @author gg 2002.12.14
*/
public class EntryComparator
        extends SafeComparator
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor (for ExternalizableLite and PortableObject).
    */
    public EntryComparator()
        {
        }

    /**
    * Construct an EntryComparator to compare entries' values using the
    * provided Comparator object. The EntryComparator will choose the
    * comparison style based on the specified comparator type: if the
    * comparator is an instance of the
    * {@link com.tangosol.util.extractor.KeyExtractor}, the CMP_KEY style
    * will be assumed; otherwise, the CMP_VALUE style is used.
    *
    * @param comparator  the comparator to use; if not specified the
    *                    "natural" comparison of entries' values is used
    */
    public EntryComparator(Comparator comparator)
        {
        this(comparator, CMP_AUTO);
        }

    /**
    * Construct an EntryComparator to compare entries using the provided
    * Comparator object according to the specified comparison style. If the
    * style is CMP_AUTO then the comparator type is checked: if the
    * comparator is an instance of the
    * {@link com.tangosol.util.extractor.KeyExtractor}, the CMP_KEY style
    * will be assumed; otherwise, the CMP_VALUE style is used.
    *
    * @param comparator  the comparator to use; if not specified the
    *                    "natural" comparison is used
    * @param nStyle      the comparison style to use; valid values are any
    *                     of the CMP_* constants
    */
    public EntryComparator(Comparator comparator, int nStyle)
        {
        super(comparator);

        switch (nStyle)
            {
            case CMP_AUTO:
                nStyle = isKeyComparator(comparator) ? CMP_KEY : CMP_VALUE;
                break;
            case CMP_VALUE:
            case CMP_KEY:
            case CMP_ENTRY:
                break;
            default:
                throw new IllegalArgumentException(
                        "Invalid comparison style: " + nStyle);
            }

        m_nStyle = nStyle;
        }


    // ----- Comparator interface -------------------------------------------

    /**
    * Compares two arguments for order. The arguments must be
    * {@link java.util.Map.Entry} objects. Depending on the comparison style,
    * this method will pass either the entries' values, keys or the entries
    * themselves to the underlying Comparator.
    */
    public int compare(Object o1, Object o2)
        {
        Map.Entry e1 = (Map.Entry) o1;
        Map.Entry e2 = (Map.Entry) o2;

        switch (m_nStyle)
            {
            case CMP_KEY:
                return super.compare(e1.getKey(), e2.getKey());

            case CMP_ENTRY:
                if (e1 instanceof QueryMap.Entry && e2 instanceof QueryMap.Entry)
                    {
                    return compareEntries((QueryMap.Entry) e1, (QueryMap.Entry) e2);
                    }
                // fall through
            default:
            case CMP_VALUE:
                return super.compare(e1.getValue(), e2.getValue());
            }
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Determine if two EntryComparator objects are equal.
    *
    * @param o  the other object
    *
    * @return true if the passed object is equal to this
    */
    public boolean equals(Object o)
        {
        if (o instanceof EntryComparator)
            {
            EntryComparator that = (EntryComparator) o;
            return super.equals(o) && this.m_nStyle == that.m_nStyle;
            }

        return false;
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Obtain the comparison style value utilized by this EntryComparator. The
    * returned value should be one of the CMP_* constants.
    *
    * @return  the comparison style value
    */
    public int getComparisonStyle()
        {
        return m_nStyle;
        }

    /**
    * Check whether or not this EntryComparator uses entries' values to pass
    * for comparison to the underlying Comparator.
    *
    * @return true iff entries' values are used for comparison
    */
    public boolean isCompareValue()
        {
        return m_nStyle == CMP_VALUE;
        }

    /**
    * Check whether or not this EntryComparator uses entries' keys to pass
    * for comparison to the underlying Comparator.
    *
    * @return true iff entries' keys are used for comparison
    */
    public boolean isCompareKey()
        {
        return m_nStyle == CMP_KEY;
        }

    /**
    * Check whether or not this EntryComparator pass entries themselves for
    * comparison to the underlying
    * {@link QueryMapComparator#compareEntries compareEntries()} method.
    *
    * @return true iff entries themselves are used for comparison
    */
    public boolean isCompareEntry()
        {
        return m_nStyle == CMP_ENTRY;
        }


    // ----- ExternalizableLite interface -----------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(DataInput in)
            throws IOException
        {
        super.readExternal(in);

        m_nStyle = in.readInt();
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(DataOutput out)
            throws IOException
        {
        super.writeExternal(out);

        out.writeInt(m_nStyle);
        }


    // ----- PortableObject interface ---------------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(PofReader in)
            throws IOException
        {
        super.readExternal(in);

        m_nStyle = in.readInt(10);
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(PofWriter out)
            throws IOException
        {
        super.writeExternal(out);

        out.writeInt(10, m_nStyle);
        }


    // ----- constants ------------------------------------------------------

    /**
    * Indicates that this EntryComparator should choose the comparison style
    * based on the underying comparator type.
    */
    public static final int CMP_AUTO = 0;

    /**
    * Indicates that this EntryComparator should compare the entries' values.
    */
    public static final int CMP_VALUE = 1;

    /**
    * Indicates that this EntryComparator should compare the entries' keys.
    */
    public static final int CMP_KEY   = 2;

    /**
    * Indicates that entries that implement
    * {@link com.tangosol.util.QueryMap.Entry} interface will be compared
    * using the {@link QueryMapComparator#compareEntries compareEntries()}
    * method.
    */
    public static final int CMP_ENTRY = 3;


    // ----- data members  --------------------------------------------------

    /**
    * Comparison style utilized by this EntryComparator. Valid values are any
    * of the CMP_* constants.
    */
    @JsonbProperty("style")
    protected int m_nStyle;
    }