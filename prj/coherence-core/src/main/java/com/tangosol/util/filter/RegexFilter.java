/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util.filter;


import com.tangosol.util.ValueExtractor;
import java.util.regex.Pattern;

/**
* Filter which uses the regular expression pattern match defined by the
* {@link Pattern#matches(String, CharSequence)} contract.
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

    // ----- Filter interface -----------------------------------------------

    protected String getOperator()
        {
        return "MATCHES";
        }

    // ----- ExtractorFilter methods ----------------------------------------

    /**
    * {@inheritDoc}
    */
    protected boolean evaluateExtracted(E extracted)
        {
        ensureInitialized();
        return m_pattern.matcher(String.valueOf(extracted)).matches();
        }

    // ---- helpers ---------------------------------------------------------

    /**
     * Ensure that the regex pattern is compiled only once.
     */
    private void ensureInitialized()
        {
        if (m_pattern == null)
            {
            m_pattern = Pattern.compile(getValue());
            }
        }

    // ---- data members ----------------------------------------------------

    /**
     * Compiled regex pattern.
     */
    private transient volatile Pattern m_pattern;
    }
