/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util.filter;


import com.tangosol.util.ValueExtractor;


/**
* Filter which uses the regular expression pattern match defined by the
* {@link String#matches(String)} contract. This implementation is not index
* aware and will not take advantage of existing indexes.
*
* @param <T> the type of the input argument to the filter
* @param <E> the type of the extracted attribute to use for comparison
*
* @author gg  2010.05.02
* @since Coherence 3.6
*/
public class RegexFilter<T, E>
        extends ComparisonFilter<T, E, String>
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor (necessary for the ExternalizableLite interface).
    */
    public RegexFilter()
        {
        }

    /**
    * Construct a RegexFilter for testing pattern matching.
    *
    * @param extractor the ValueExtractor to use by this filter
    * @param sRegex    the regular expression to match the result with
    */
    public RegexFilter(ValueExtractor<? super T, ? extends E> extractor, String sRegex)
        {
        super(extractor, sRegex);
        }

    /**
    * Construct a RegexFilter for testing pattern matching.
    *
    * @param sMethod  the name of the method to invoke via reflection
    * @param sRegex   the regular expression to match the result with
    */
    public RegexFilter(String sMethod, String sRegex)
        {
        super(sMethod, sRegex);
        }


    // ----- ExtractorFilter methods ----------------------------------------

    /**
    * {@inheritDoc}
    */
    protected boolean evaluateExtracted(E extracted)
        {
        return String.valueOf(extracted).matches(String.valueOf(getValue()));
        }
    }