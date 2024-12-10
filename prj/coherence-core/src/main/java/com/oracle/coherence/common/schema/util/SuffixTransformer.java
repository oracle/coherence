/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.common.schema.util;


/**
 * A {@link NameTransformer} implementation that adds or removes specified
 * suffix to/from an input string.
 *
 * @author as  2013.11.21
 */
public class SuffixTransformer
        implements NameTransformer
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Construct {@code PrefixTransformer} instance.
     *
     * @param suffix  the suffix to add or remove to/from an input string
     * @param mode    the transformer mode
     */
    public SuffixTransformer(String suffix, Mode mode)
        {
        if (StringUtils.isEmpty(suffix))
            {
            throw new IllegalArgumentException("suffix cannot be null or empty");
            }

        m_suffix = suffix;
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

        String suffix = m_suffix;
        if (m_mode == Mode.ADD && !source.endsWith(suffix))
            {
            return source + suffix;
            }
        else if (source.endsWith(suffix))
            {
            return source.substring(0, source.length() - suffix.length());
            }

        return source;
        }

    @Override
    public String[] transform(String[] source)
        {
        if (source != null && source.length > 0)
            {
            String suffix = m_suffix;
            if (m_mode == Mode.ADD && !suffix.equals(source[source.length - 1]))
                {
                String[] result = new String[source.length + 1];
                result[source.length] = suffix;
                System.arraycopy(source, 0, result, 0, source.length);
                return result;
                }
            else if (suffix.equals(source[source.length - 1]))
                {
                String[] result = new String[source.length - 1];
                System.arraycopy(source, 0, result, 0, source.length - 1);
                return result;
                }
            }
        return source;
        }

    // ---- data members ----------------------------------------------------

    private String m_suffix;
    private Mode m_mode;

    // ---- inner enum: Mode ------------------------------------------------

    /**
     * Suffix mode enum.
     */
    public enum Mode
        {
        ADD,
        REMOVE
        }
    }
