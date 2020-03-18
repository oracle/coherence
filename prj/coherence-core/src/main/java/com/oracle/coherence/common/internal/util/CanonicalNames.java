/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.internal.util;

/**
 * Helper class for Canonical Name processing.
 *
 * @author mf/jf  12.15.2017
 * @since Coherence 19.1.0.0
 */
public class CanonicalNames
    {
    /**
     * Compute the canonical name for a ValueExtractor.
     * <p>
     * Canonical name is null when one or more optional method parameters
     * are provided. If <code>sName</code> does not end in {@link #VALUE_EXTRACTOR_METHOD_SUFFIX},
     * <code>"()"</code>, the canonical name is <code>sName</code> and represents a property.
     * If <code>sName</code> is prefixed with one of the {@link #VALUE_EXTRACTOR_BEAN_ACCESSOR_PREFIXES}
     * and ends in {@link #VALUE_EXTRACTOR_METHOD_SUFFIX},
     * the canonical name is <code>sName</code> with prefix and suffix removed.
     * This canonical name represents a property.
     * If the <code>sName</code> just ends in {#link #VALUE_EXTRACTOR_METHOD_SUFFIX},
     * the canonical name is same as <code>sName</code>. This canonical name
     * references a method. If there are one or more parameters in <code>aoParam</code>,
     * the canonical name is null.
     *
     * @param sName   a property or method based on processing described above
     * @param aoParam optional parameters
     *
     * @return return canonical name of sName if it exist; otherwise, null
     */
    public static String computeValueExtractorCanonicalName(String sName, Object[] aoParam)
        {
        final int nMethodSuffixLength = VALUE_EXTRACTOR_METHOD_SUFFIX.length();
        if (aoParam != null && aoParam.length > 0)
            {
            return null;
            }
        else if (sName.endsWith(VALUE_EXTRACTOR_METHOD_SUFFIX))
            {
            // check for JavaBean accessor and convert to property if found.
            String sNameCanonical = sName;
            int nNameLength = sName.length();
            for (String sPrefix : VALUE_EXTRACTOR_BEAN_ACCESSOR_PREFIXES)
                {
                int nPrefixLength = sPrefix.length();
                if (nNameLength > nPrefixLength && sName.startsWith(sPrefix))
                    {
                    // detected a JavaBean accessor; convert method to a property. remove prefix and suffix.
                    sNameCanonical = Character.toLowerCase(sName.charAt(nPrefixLength)) +
                            sName.substring(nPrefixLength + 1, nNameLength - nMethodSuffixLength);
                    break;
                    }
                }
            return sNameCanonical;
            }
        else
            {
            // is a property
            return sName;
            }
        }

    /**
     * Compute canonical name when no optional parameters.
     *
     * @param sName   a property or method based on processing described in
     *                {@link #computeValueExtractorCanonicalName(String, Object[]) above}
     *
     * @return canonicalName
     */
    public static String computeValueExtractorCanonicalName(String sName)
        {
        return computeValueExtractorCanonicalName(sName, null);
        }

    // ----- constants ------------------------------------------------------

    /**
     * If <code>sName</code> parameter in {@link #computeValueExtractorCanonicalName(String, Object[])}
     * ends with this suffix, it represents a method name.
     */
    public static final String VALUE_EXTRACTOR_METHOD_SUFFIX          = "()";

    /**
     * JavaBean accessor prefixes.
     */
    public static final String[] VALUE_EXTRACTOR_BEAN_ACCESSOR_PREFIXES =
            {"get", "is"};
    }
