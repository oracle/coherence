/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util.extractor;


import com.tangosol.util.ValueExtractor;

import com.tangosol.util.comparator.SafeComparator;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;

import java.util.Comparator;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.math.BigDecimal;
import java.math.BigInteger;

import javax.json.bind.annotation.JsonbProperty;


/**
* A synthetic ValueExtractor that returns a result of comparison between two
* values extracted from the same target. In a most general case, the extracted
* value represents an Integer value calculated accordingly to the contract of
* {@link Comparable#compareTo} or {@link Comparator#compare} methods. However,
* in more specific cases, when the compared values are of common numeric type,
* the ComparisonValueExtractor will return a numeric difference between those
* values. The Java type of the comparing values will dictate the Java type of
* the result.
* <p>
* For example, lets assume that a cache contains business objects that have two
* properties: SellPrice and BuyPrice (both double). Then, to query for all
* objects that have SellPrice less than BuyPrice we would use the following:
* <pre>
* ValueExtractor extractDiff = new ComparisonValueExtractor(
*   new ReflectionExtractor("getSellPrice"),
*   new ReflectionExtractor("getBuyPrice"));
* Filter filter = new LessFilter(extractDiff, new Double(0.0));
* Set entries = cache.entrySet(filter);
* </pre>
*
* @author gg 2008.02.15
* @since Coherence 3.4
*/
public class ComparisonValueExtractor<T, E extends Number>
        extends AbstractCompositeExtractor<T, E>
    {
    /**
    * Default constructor (necessary for the ExternalizableLite interface).
    */
    public ComparisonValueExtractor()
        {
        }

    /**
    * Construct a ComparisonValueExtractor based on two method names.
    * Note: values returned by both methods must be {@link Comparable}.
    *
    * @param sMethod1  the name of the first method to invoke via reflection
    * @param sMethod2  the name of the second method to invoke via reflection
    */
    public ComparisonValueExtractor(String sMethod1, String sMethod2)
        {
        this(sMethod1, sMethod2, null);
        }

    /**
    * Construct a ComparisonValueExtractor based on two method names and a
    * Comparator object.
    *
    * @param sMethod1  the name of the first method to invoke via reflection
    * @param sMethod2  the name of the second method to invoke via reflection
    * @param comp      the comparator used to compare the extracted values (optional)
    */
    public ComparisonValueExtractor(String sMethod1, String sMethod2,
                                    Comparator<? super E> comp)
        {
        this(new ReflectionExtractor(sMethod1),
             new ReflectionExtractor(sMethod2), comp);
        }

    /**
    * Construct a ComparisonValueExtractor based on two specified extractors.
    * Note: values returned by both extractors must be {@link Comparable}.
    *
    * @param ve1   the ValueExtractor for the first value
    * @param ve2   the ValueExtractor for the second value
    */
    public ComparisonValueExtractor(ValueExtractor<T, E> ve1, ValueExtractor<T, E> ve2)
        {
        this(ve1, ve2, null);
        }

    /**
    * Construct a ComparisonValueExtractor based on two specified extractors and
    * a Comparator object.
    *
    * @param ve1   the ValueExtractor for the first value
    * @param ve2   the ValueExtractor for the second value
    * @param comp  the comparator used to compare the extracted values (optional)
    */
    public ComparisonValueExtractor(ValueExtractor<T, E> ve1, ValueExtractor<T, E> ve2,
                                    Comparator<? super E> comp)
        {
        super(new ValueExtractor[] {ve1, ve2});

        m_comparator = comp;
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Return a Comparator used by this extractor.
    *
    * @return a Comparator used by this extractor; null if the natural value
    *         comparison should be used
    */
    public Comparator getComparator()
        {
        return m_comparator;
        }


    // ----- ValueExtractor interface ---------------------------------------

    /**
    * {@inheritDoc}
    */
    public E extract(Object oTarget)
        {
        ValueExtractor[] aExtractor = getExtractors();
        Comparator       comparator = getComparator();

        Object o1 = aExtractor[0].extract(oTarget);
        Object o2 = aExtractor[1].extract(oTarget);

        if (o1 instanceof Number && o2 instanceof Number && comparator == null)
            {
            Number num1 = (Number) o1;
            Number num2 = (Number) o2;
            int    nType;

            if (num1.getClass() == num2.getClass())
                {
                // most common case; same types
                nType = getStreamFormat(o1);
                }
            else
                {
                int[] anType = new int[] {
                    FMT_BYTE,       // 0
                    FMT_SHORT,      // 1
                    FMT_INT,        // 2
                    FMT_LONG,       // 3
                    FMT_FLOAT,      // 4
                    FMT_DOUBLE,     // 5
                    FMT_INTEGER,    // 6
                    FMT_DECIMAL     // 7
                    };

                int nType1 = getStreamFormat(num1);
                int nType2 = getStreamFormat(num2);
                int cTypes, ix1, ix2;

                ix1 = ix2 = cTypes = anType.length;
                for (int i = 0; i < cTypes; i++)
                    {
                    int nT = anType[i];
                    if (nT == nType1)
                        {
                        ix1 = i;
                        }
                    if (nT == nType2)
                        {
                        ix2 = i;
                        }
                    }

                switch (Math.max(ix1, ix2))
                    {
                    case 1: case 2:
                        nType = FMT_INT;
                        break;
                    case 3:
                        nType = FMT_LONG;
                        break;
                    case 4: case 5:
                        nType = FMT_DOUBLE;
                        break;
                    case 6: case 7:
                        nType = FMT_INTEGER;
                        num1  = ensureBigDecimal(num1);
                        num2  = ensureBigDecimal(num2);
                        break;
                    default:
                        nType = FMT_NONE;
                        break;
                    }
                }

            switch (nType)
                {
                case FMT_BYTE:
                case FMT_SHORT:
                case FMT_INT:
                    return (E) Integer.valueOf(num1.intValue() - num2.intValue());

                case FMT_LONG:
                    return (E) Long.valueOf(num1.longValue() - num2.longValue());

                case FMT_FLOAT:
                    return (E) Float.valueOf(num1.floatValue() - num2.floatValue());

                case FMT_DOUBLE:
                    return (E) Double.valueOf(num1.doubleValue() - num2.doubleValue());

                case FMT_INTEGER:
                    return (E) ((BigInteger) num1).subtract((BigInteger) num2);

                case FMT_DECIMAL:
                    return (E) ((BigDecimal) num1).subtract((BigDecimal) num2);
                }

            }
        return (E) Integer.valueOf(SafeComparator.compareSafe(comparator, o1, o2));
        }


    // ----- ExternalizableLite interface -----------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(DataInput in)
            throws IOException
        {
        super.readExternal(in);

        m_comparator = readObject(in);
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(DataOutput out)
            throws IOException
        {
        super.writeExternal(out);

        writeObject(out, m_comparator);
        }


    // ----- PortableObject interface ---------------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(PofReader in)
            throws IOException
        {
        super.readExternal(in);

        m_comparator = in.readObject(1);
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(PofWriter out)
            throws IOException
        {
        super.writeExternal(out);

        out.writeObject(1, m_comparator);
        }


    // ----- data members ---------------------------------------------------

    /**
    * An underlying Comparator object (optional).
    */
    @JsonbProperty("comparator")
    protected Comparator m_comparator;
    }
