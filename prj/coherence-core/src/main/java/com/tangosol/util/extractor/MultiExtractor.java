/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util.extractor;


import com.tangosol.util.BinaryEntry;
import com.tangosol.util.ImmutableArrayList;
import com.tangosol.util.InvocableMapHelper;
import com.tangosol.util.MapTrigger;
import com.tangosol.util.QueryMap;
import com.tangosol.util.ValueExtractor;

import com.tangosol.util.comparator.SafeComparator;

import java.util.List;
import java.util.Map;


/**
* Composite ValueExtractor implementation based on an array of extractors.
* All extractors in the array are applied to the same target object and the
* result of the extraction is a {@link java.util.List List} of extracted
* values.
* <p>
* Common scenarios for using the MultiExtractor involve the
* {@link com.tangosol.util.aggregator.DistinctValues DistinctValues} or
* {@link com.tangosol.util.aggregator.GroupAggregator GroupAggregator}
* aggregators, that allow clients to collect all distinct combinations of a
* given set of attributes or collect and run additional aggregation against
* the corresponding groups of entries.
*
* @author gg 2006.02.08
* @since Coherence 3.2
*/
public class MultiExtractor
        extends AbstractCompositeExtractor
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor (necessary for the ExternalizableLite interface).
    */
    public MultiExtractor()
        {
        }

    /**
    * Construct a MultiExtractor.
    *
    * @param aExtractor  the ValueExtractor array
    */
    public MultiExtractor(ValueExtractor[] aExtractor)
        {
        super(aExtractor);
        }

    /**
    * Construct a MultiExtractor for a specified method name list.
    *
    * @param sNames  a comma-delimited sequence of method names which results
    *                in a MultiExtractor that is based on a corresponding
    *                array of {@link ValueExtractor} objects; individual
    *                array elements will be either {@link ReflectionExtractor}
    *                or {@link ChainedExtractor} objects
    *
    * @deprecated use {@link com.tangosol.util.Extractors#multi(String...)} which uses {@link UniversalExtractor}.
    */
    public MultiExtractor(String sNames)
        {
        super(createExtractors(sNames));
        }


    // ----- AbstractExtractor methods --------------------------------------

    /**
    * Extract a collection of values from the passed object using the underlying
    * array of ValueExtractor objects. Note that each individual value could
    * be an object of a standard wrapper type (for intrinsic types) or null.
    *
    * @param  oTarget  an Object to retrieve the collection of values from
    *
    * @return a {@link java.util.List List} containing the extracted values
    *         or null if the target object itself is null
    */
    public Object extract(Object oTarget)
        {
        if (oTarget == null)
            {
            return null;
            }

        ValueExtractor[] aExtractor  = getExtractors();
        int              cExtractors = aExtractor.length;
        Object[]         aValue      = new Object[cExtractors];

        for (int i = 0; i < cExtractors; i++)
            {
            aValue[i] = aExtractor[i].extract(oTarget);
            }

        return new ImmutableArrayList(aValue);
        }

    /**
    * Extract a collection of values from the passed entry using the underlying
    * array of ValueExtractor objects. Note that each individual value could be
    * an object of a standard wrapper type (for intrinsic types) or null.
    *
    * @param  entry  an entry to retrieve the collection of values from
    *
    * @return a {@link java.util.List List} containing the extracted values
    */
    public List extractFromEntry(Map.Entry entry)
        {
        ValueExtractor[] aExtractor  = getExtractors();
        int              cExtractors = aExtractor.length;
        Object[]         aValue      = new Object[cExtractors];

        // only use the index if this MultiExtractor is not itself an index;
        // the order of index updates is arbitrary thus index updates must not
        // attempt to use other indices

        boolean fUseIndex = entry instanceof BinaryEntry &&
                !((BinaryEntry) entry).getBackingMapContext().getIndexMap().containsKey(this);

        for (int i = 0; i < cExtractors; i++)
            {
            aValue[i] = fUseIndex
                    ? ((QueryMap.Entry) entry).extract(aExtractor[i])
                    : InvocableMapHelper.extractFromEntry(aExtractor[i], entry);
            }

        return new ImmutableArrayList(aValue);
        }

    /*
    * Analogous to the {@link #extractFromEntry} method, extract the value from
    * the "original value" of the passed Entry object.
    */
    public List extractOriginalFromEntry(MapTrigger.Entry entry)
        {
        ValueExtractor[] aExtractor  = getExtractors();
        int              cExtractors = aExtractor.length;
        Object[]         aValue      = new Object[cExtractors];

        for (int i = 0; i < cExtractors; i++)
            {
            aValue[i] = InvocableMapHelper.extractOriginalFromEntry(aExtractor[i], entry);
            }

        return new ImmutableArrayList(aValue);
        }


    // ----- QueryMapComparator interface -----------------------------------

    /**
    * {@inheritDoc}
    */
    public int compareEntries(QueryMap.Entry entry1, QueryMap.Entry entry2)
        {
        ValueExtractor[] aExtractor = getExtractors();

        for (int i = 0, c = aExtractor.length; i < c; i++)
            {
            ValueExtractor extractor = aExtractor[i];

            int iResult = SafeComparator.compareSafe(null,
                entry1.extract(extractor), entry2.extract(extractor));
            if (iResult != 0)
                {
                return iResult;
                }
            }
        return 0;
        }


    // ----- helpers --------------------------------------------------------

    /**
    * Parse a comma-delimited sequence of method names and instantiate
    * a corresponding array of {@link ValueExtractor} objects. Individual
    * array elements will be either {@link ReflectionExtractor} or
    * {@link ChainedExtractor} objects.
    *
    * @param sNames  a comma-delimited sequence of method names
    *
    * @return an array of {@link ValueExtractor} objects
    *
    * @deprecated
    */
    public static ValueExtractor[] createExtractors(String sNames)
        {
        String[]         asMethod   = parseDelimitedString(sNames, ',');
        int              cMethods   = asMethod.length;
        ValueExtractor[] aExtractor = new ValueExtractor[cMethods];

        for (int i = 0; i < cMethods; i++)
            {
            String sMethod = asMethod[i];

            aExtractor[i] = sMethod.indexOf('.') < 0 ? (ValueExtractor)
                new ReflectionExtractor(sMethod) :
                new ChainedExtractor(sMethod);
            }
        return aExtractor;
        }
    }
