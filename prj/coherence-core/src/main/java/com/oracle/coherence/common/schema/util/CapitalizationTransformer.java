/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.common.schema.util;


/**
 * Capitalizes the input string according to the specified capitalization mode.
 *
 * @author as  2013.11.21
 */
public class CapitalizationTransformer
        implements NameTransformer
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Construct {@code CapitalizationTransformer} instance.
     *
     * @param mode  the capitalization mode to apply
     */
    public CapitalizationTransformer(Mode mode)
        {
        m_mode = mode;
        }

    // ---- NameTransformer implementation ----------------------------------

    @Override
    public String transform(String source)
        {
        if (StringUtils.isEmpty(source))
            {
            return null;
            }

        switch (m_mode)
            {
            case UPPER:
                return source.toUpperCase();
            case LOWER:
                return source.toLowerCase();
            case FIRST_UPPER:
                return source.substring(0, 1).toUpperCase()
                        + source.substring(1);
            case FIRST_LOWER:
                return source.substring(0, 1).toLowerCase()
                        + source.substring(1);
            default:
                return source;
            }
        }

    @Override
    public String[] transform(String[] source)
        {
        if (source != null)
            {
            for (int i = 0; i < source.length; i++)
                {
                source[i] = transform(source[i]);
                }
            }

        return source;
        }

    // ---- data members ----------------------------------------------------

    private Mode m_mode;

    // ---- inner enum: Mode ------------------------------------------------

    /**
     * Capitalization mode enum.
     */
    public enum Mode
        {
        UPPER,
        LOWER,
        FIRST_UPPER,
        FIRST_LOWER
        // CAMEL
        }
    }
