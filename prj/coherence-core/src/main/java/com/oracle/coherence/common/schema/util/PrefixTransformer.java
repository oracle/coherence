/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.common.schema.util;


/**
 * A {@link NameTransformer} implementation that adds or removes specified
 * prefix to/from an input string.
 *
 * @author as  2013.11.21
 */
public class PrefixTransformer
        implements NameTransformer
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Construct {@code PrefixTransformer} instance.
     *
     * @param prefix  the prefix to add or remove to/from an input string
     * @param mode    the transformer mode
     */
    public PrefixTransformer(String prefix, Mode mode)
        {
        if (StringUtils.isEmpty(prefix))
            {
            throw new IllegalArgumentException("prefix cannot be null or empty");
            }

        m_prefix = prefix;
        m_mode   = mode;
        }

    // ---- NameTransformer implementation ----------------------------------

    @Override
    public String transform(String source)
        {
        if (StringUtils.isEmpty(source))
            {
            return null;
            }

        String prefix = m_prefix;
        if (m_mode == Mode.ADD && !source.startsWith(prefix))
            {
            return prefix + source;
            }
        else if (source.startsWith(prefix))
            {
            return source.substring(prefix.length());
            }

        return source;
        }

    @Override
    public String[] transform(String[] source)
        {
        if (source != null && source.length > 0)
            {
            String prefix = m_prefix;
            if (m_mode == Mode.ADD && !prefix.equals(source[0]))
                {
                String[] result = new String[source.length + 1];
                result[0] = prefix;
                System.arraycopy(source, 0, result, 1, source.length);
                return result;
                }
            else if (prefix.equals(source[0]))
                {
                String[] result = new String[source.length - 1];
                System.arraycopy(source, 1, result, 0, source.length - 1);
                return result;
                }
            }
        return source;
        }

    // ---- data members ----------------------------------------------------

    private String m_prefix;
    private Mode m_mode;

    // ---- inner enum: Mode ------------------------------------------------

    /**
     * Prefix mode enum.
     */
    public enum Mode
        {
        ADD,
        REMOVE
        }
    }
