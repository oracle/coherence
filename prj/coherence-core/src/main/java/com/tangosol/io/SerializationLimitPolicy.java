/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.coherence.config.Config;
import com.tangosol.internal.util.CoherenceMode;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.util.Base;

import java.io.IOException;

import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Generic serializer-owned limits for serialized container structures.
 *
 * @author Aleks Seovic  2026.05.20
 * @since 26.04
 */
public class SerializationLimitPolicy
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a policy.
     *
     * @param cbMaxContainer  maximum container bytes, or {@code null}
     * @param cMaxElements    maximum elements, or {@code null}
     * @param cMaxMapEntries  maximum map entries, or {@code null}
     */
    public SerializationLimitPolicy(Long cbMaxContainer, Integer cMaxElements, Integer cMaxMapEntries)
        {
        this(cbMaxContainer, true, cMaxElements, true, cMaxMapEntries, true);
        }

    /**
     * Create a policy.
     *
     * @param cbMaxContainer        maximum container bytes, or {@code null}
     * @param fContainerSpecified   {@code true} if the container byte limit was specified
     * @param cMaxElements          maximum elements, or {@code null}
     * @param fElementsSpecified    {@code true} if the element limit was specified
     * @param cMaxMapEntries        maximum map entries, or {@code null}
     * @param fMapEntriesSpecified  {@code true} if the map-entry limit was specified
     */
    private SerializationLimitPolicy(Long cbMaxContainer, boolean fContainerSpecified,
                                     Integer cMaxElements, boolean fElementsSpecified,
                                     Integer cMaxMapEntries, boolean fMapEntriesSpecified)
        {
        m_cbMaxContainer       = cbMaxContainer;
        m_fContainerSpecified  = fContainerSpecified;
        m_cMaxElements         = cMaxElements;
        m_fElementsSpecified   = fElementsSpecified;
        m_cMaxMapEntries       = cMaxMapEntries;
        m_fMapEntriesSpecified = fMapEntriesSpecified;
        }

    // ----- accessors ------------------------------------------------------

    /**
     * Return the maximum serialized container byte count, or {@code null} for
     * unlimited.
     *
     * @return the maximum serialized container byte count
     */
    public Long getMaxContainerBytes()
        {
        return m_cbMaxContainer;
        }

    /**
     * Return the maximum array, collection, or object-array element count, or
     * {@code null} for unlimited.
     *
     * @return the maximum element count
     */
    public Integer getMaxElements()
        {
        return m_cMaxElements;
        }

    /**
     * Return the maximum map-entry count, or {@code null} for unlimited.
     *
     * @return the maximum map-entry count
     */
    public Integer getMaxMapEntries()
        {
        return m_cMaxMapEntries;
        }

    // ----- validation -----------------------------------------------------

    /**
     * Validate a byte count.
     *
     * @param sKind  the structure kind
     * @param cb     the byte count
     *
     * @throws IOException if the count is invalid or exceeds the policy
     */
    public void validateContainerBytes(String sKind, int cb)
            throws IOException
        {
        if (cb < 0)
            {
            throw invalid(sKind + " byte count is negative: " + cb);
            }
        Long cbMax = m_cbMaxContainer;
        if (cbMax != null && cb > cbMax)
            {
            throw exceeds(sKind + " byte count", cb, cbMax);
            }
        shadow(sKind + " byte count", cb, DEFAULT_MAX_CONTAINER_BYTES);
        }

    /**
     * Validate an element count.
     *
     * @param sKind  the structure kind
     * @param c      the element count
     *
     * @throws IOException if the count is invalid or exceeds the policy
     */
    public void validateElements(String sKind, int c)
            throws IOException
        {
        if (c < 0)
            {
            throw invalid(sKind + " element count is negative: " + c);
            }
        Integer cMax = m_cMaxElements;
        if (cMax != null && c > cMax)
            {
            throw exceeds(sKind + " element count", c, cMax);
            }
        shadow(sKind + " element count", c, DEFAULT_MAX_ELEMENTS);
        }

    /**
     * Validate a map-entry count.
     *
     * @param sKind  the structure kind
     * @param c      the entry count
     *
     * @throws IOException if the count is invalid or exceeds the policy
     */
    public void validateMapEntries(String sKind, int c)
            throws IOException
        {
        if (c < 0)
            {
            throw invalid(sKind + " entry count is negative: " + c);
            }
        Integer cMax = m_cMaxMapEntries;
        if (cMax != null && c > cMax)
            {
            throw exceeds(sKind + " entry count", c, cMax);
            }
        shadow(sKind + " entry count", c, DEFAULT_MAX_MAP_ENTRIES);
        }

    /**
     * Merge this policy with the supplied override. Any {@code null} values in
     * the override inherit from this policy.
     *
     * @param override  the override policy, or {@code null}
     *
     * @return the merged policy
     */
    public SerializationLimitPolicy merge(SerializationLimitPolicy override)
        {
        if (override == null)
            {
            return this;
            }
        return new SerializationLimitPolicy(
                override.m_fContainerSpecified ? override.m_cbMaxContainer : m_cbMaxContainer, true,
                override.m_fElementsSpecified ? override.m_cMaxElements : m_cMaxElements, true,
                override.m_fMapEntriesSpecified ? override.m_cMaxMapEntries : m_cMaxMapEntries, true);
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Return the global default policy.
     *
     * @return the global default policy
     */
    public static SerializationLimitPolicy getDefault()
        {
        boolean fUnlimited = !CoherenceMode.isSecurityHardeningEnabled();

        Long    cbMax = parseBytesProperty(PROP_MAX_CONTAINER_BYTES,
                fUnlimited ? null : DEFAULT_MAX_CONTAINER_BYTES, fUnlimited);
        Integer cElem = parseIntProperty(PROP_MAX_ELEMENTS,
                fUnlimited ? null : DEFAULT_MAX_ELEMENTS, fUnlimited);
        Integer cMap  = parseIntProperty(PROP_MAX_MAP_ENTRIES,
                fUnlimited ? null : DEFAULT_MAX_MAP_ENTRIES, fUnlimited);

        if (!fUnlimited)
            {
            if (cbMax == null || cElem == null || cMap == null)
                {
                throw new IllegalArgumentException("unlimited serializer limits are allowed only when security hardening is disabled");
                }
            }
        return new SerializationLimitPolicy(cbMax, cElem, cMap);
        }

    /**
     * Parse a {@code <limits>} element as an override policy.
     *
     * @param xmlLimits  the limits element, or {@code null}
     *
     * @return the parsed policy, or {@code null}
     */
    public static SerializationLimitPolicy fromXml(XmlElement xmlLimits)
        {
        if (xmlLimits == null)
            {
            return null;
            }

        Limit<Long>    limitBytes = parseBytesElement(xmlLimits.getElement(XML_MAX_CONTAINER_BYTES));
        Limit<Integer> limitElem  = parseIntElement(xmlLimits.getElement(XML_MAX_ELEMENTS));
        Limit<Integer> limitMap   = parseIntElement(xmlLimits.getElement(XML_MAX_MAP_ENTRIES));

        if (!limitBytes.isSpecified() && !limitElem.isSpecified() && !limitMap.isSpecified())
            {
            return null;
            }
        return new SerializationLimitPolicy(limitBytes.getValue(), limitBytes.isSpecified(),
                limitElem.getValue(), limitElem.isSpecified(),
                limitMap.getValue(), limitMap.isSpecified());
        }

    /**
     * Apply a policy to a serializer if it supports limit injection.
     *
     * @param serializer  the serializer
     * @param policy      the policy, or {@code null}
     *
     * @return the serializer
     */
    public static Serializer apply(Serializer serializer, SerializationLimitPolicy policy)
        {
        if (policy != null && serializer instanceof SerializationLimitAware)
            {
            ((SerializationLimitAware) serializer).setLimitPolicy(serializer.getLimitPolicy().merge(policy));
            }
        return serializer;
        }

    private static IOException invalid(String sMessage)
        {
        return new IOException("POF " + sMessage);
        }

    private static IOException exceeds(String sKind, long cActual, long cMax)
        {
        return new IOException("POF " + sKind + " exceeds maximum: " + cActual + " > " + cMax);
        }

    private void shadow(String sKind, long cActual, long cRecommended)
        {
        if (CoherenceMode.isSecurityHardeningEnabled() || cActual <= cRecommended)
            {
            return;
            }

        String sKey = sKind;
        if (S_WARNED.add(sKey))
            {
            Logger.warn("POF " + sKind + " " + cActual
                    + " exceeds recommended hardening threshold " + cRecommended
                    + "; security hardening is disabled, so this payload is allowed for compatibility.");
            }
        }

    private static Limit<Long> parseBytesElement(XmlElement xml)
        {
        if (xml == null)
            {
            return Limit.unspecified();
            }
        String sValue = xml.getString().trim();
        return sValue.isEmpty()
               ? Limit.unspecified()
               : Limit.of(parseBytes(sValue, true, "serializer limit " + xml.getName()));
        }

    private static Limit<Integer> parseIntElement(XmlElement xml)
        {
        if (xml == null)
            {
            return Limit.unspecified();
            }
        String sValue = xml.getString().trim();
        return sValue.isEmpty()
               ? Limit.unspecified()
               : Limit.of(parseInt(sValue, true, "serializer limit " + xml.getName()));
        }

    private static Long parseBytesProperty(String sProperty, Long cbDefault, boolean fAllowUnlimited)
        {
        String sValue = Config.getProperty(sProperty);
        return sValue == null || sValue.isBlank()
               ? cbDefault
               : parseBytes(sValue.trim(), fAllowUnlimited, sProperty);
        }

    private static Integer parseIntProperty(String sProperty, Integer cDefault, boolean fAllowUnlimited)
        {
        String sValue = Config.getProperty(sProperty);
        return sValue == null || sValue.isBlank()
               ? cDefault
               : parseInt(sValue.trim(), fAllowUnlimited, sProperty);
        }

    private static Long parseBytes(String sValue, boolean fAllowUnlimited, String sSource)
        {
        if (isUnlimited(sValue))
            {
            if (fAllowUnlimited && !CoherenceMode.isSecurityHardeningEnabled())
                {
                return null;
                }
            throw new IllegalArgumentException(sSource + " may be unlimited only when security hardening is disabled");
            }
        long cb = Base.parseMemorySize(sValue);
        if (cb < 0)
            {
            throw new IllegalArgumentException(sSource + " must be non-negative: " + sValue);
            }
        return cb;
        }

    private static Integer parseInt(String sValue, boolean fAllowUnlimited, String sSource)
        {
        if (isUnlimited(sValue))
            {
            if (fAllowUnlimited && !CoherenceMode.isSecurityHardeningEnabled())
                {
                return null;
                }
            throw new IllegalArgumentException(sSource + " may be unlimited only when security hardening is disabled");
            }
        int c = Integer.parseInt(sValue);
        if (c < 0)
            {
            throw new IllegalArgumentException(sSource + " must be non-negative: " + sValue);
            }
        return c;
        }

    private static boolean isUnlimited(String sValue)
        {
        return "unlimited".equals(sValue.toLowerCase(Locale.ROOT));
        }

    // ----- inner class: Limit --------------------------------------------

    /**
     * Parsed tri-state limit value.
     *
     * @param <T>  the value type
     */
    private static class Limit<T>
        {
        private Limit(T value, boolean fSpecified)
            {
            m_value      = value;
            m_fSpecified = fSpecified;
            }

        static <T> Limit<T> unspecified()
            {
            return new Limit<>(null, false);
            }

        static <T> Limit<T> of(T value)
            {
            return new Limit<>(value, true);
            }

        T getValue()
            {
            return m_value;
            }

        boolean isSpecified()
            {
            return m_fSpecified;
            }

        private final T m_value;

        private final boolean m_fSpecified;
        }

    // ----- constants ------------------------------------------------------

    /**
     * Maximum serialized container bytes property.
     */
    public static final String PROP_MAX_CONTAINER_BYTES = "coherence.serializer.limit.maxContainerBytes";

    /**
     * Maximum array/collection/object element count property.
     */
    public static final String PROP_MAX_ELEMENTS = "coherence.serializer.limit.maxElements";

    /**
     * Maximum map-entry count property.
     */
    public static final String PROP_MAX_MAP_ENTRIES = "coherence.serializer.limit.maxMapEntries";

    /**
     * Serializer limits element.
     */
    public static final String XML_LIMITS = "limits";

    /**
     * Maximum serialized container bytes element.
     */
    public static final String XML_MAX_CONTAINER_BYTES = "max-container-bytes";

    /**
     * Maximum array/collection/object element count element.
     */
    public static final String XML_MAX_ELEMENTS = "max-elements";

    /**
     * Maximum map-entry count element.
     */
    public static final String XML_MAX_MAP_ENTRIES = "max-map-entries";

    /**
     * Initial hardened serialized container byte cap.
     */
    public static final long DEFAULT_MAX_CONTAINER_BYTES = 64L * 1024L * 1024L;

    /**
     * Initial hardened array/collection/object element cap.
     */
    public static final int DEFAULT_MAX_ELEMENTS = 262144;

    /**
     * Initial hardened map-entry cap.
     */
    public static final int DEFAULT_MAX_MAP_ENTRIES = 131072;

    // ----- data members ---------------------------------------------------

    private final Long m_cbMaxContainer;

    private final boolean m_fContainerSpecified;

    private final Integer m_cMaxElements;

    private final boolean m_fElementsSpecified;

    private final Integer m_cMaxMapEntries;

    private final boolean m_fMapEntriesSpecified;

    private static final Set<String> S_WARNED = ConcurrentHashMap.newKeySet();
    }
