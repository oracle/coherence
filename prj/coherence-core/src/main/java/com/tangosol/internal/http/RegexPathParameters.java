/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.http;

import java.util.regex.Matcher;

/**
 * A {@link PathParameters} implementation that uses a regular expression
 * to extract the parameters from the request path.
 *
 * @author Jonathan Knight 2022.01.25
 * @since 22.06
 */
public class RegexPathParameters
        implements PathParameters
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a {@link RegexPathParameters} that uses a specified {@link Matcher}
     * to extract path parameters.
     *
     * @param matcher  the reg-ex {@link Matcher} to use
     */
    public RegexPathParameters(Matcher matcher)
        {
        f_matcher = matcher;
        }

    // ----- PathParameters API ---------------------------------------------

    @Override
    public String getFirst(String sKey)
        {
        try
            {
            return f_matcher.group(sKey);
            }
        catch (Exception e)
            {
            return null;
            }
        }

    // ----- data members ---------------------------------------------------

    /**
     * The {@link Matcher} to extract parameters.
     */
    private final Matcher f_matcher;
    }
