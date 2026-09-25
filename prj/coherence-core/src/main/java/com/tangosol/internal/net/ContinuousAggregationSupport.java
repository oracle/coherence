/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net;

import com.tangosol.internal.util.VersionHelper;

import java.util.Map;

/**
 * Internal service-provider contract for continuously maintained
 * aggregations.
 *
 * @author Aleks Seovic  2026.09.06
 * @since 26.10
 */
public interface ContinuousAggregationSupport
    {
    /**
     * The service-member configuration key used to advertise continuous
     * aggregation support.
     */
    String MEMBER_CONFIG_KEY = "continuous-aggregation-version";

    /**
     * The current continuous aggregation service protocol version.
     */
    int MEMBER_CONFIG_VERSION = 1;

    /**
     * Prepare a continuous aggregation definition for registration by this
     * map implementation.
     * <p>
     * Transparent wrappers should delegate this method to the wrapped map.
     * Semantic views may return an equivalent definition adapted to the
     * underlying map. The returned definition is used for registration,
     * querying, removal, and partition-local invocation.
     *
     * @param definition  the user-supplied definition
     *
     * @return the definition to register
     */
    default ContinuousAggregationDefinition prepareContinuousAggregation(
            ContinuousAggregationDefinition definition)
        {
        return definition;
        }

    /**
     * Return whether a Coherence product version supports continuous
     * aggregation protocol messages and partition transfer state.
     *
     * @param sVersion           the Coherence product version
     * @param fCommunityEdition  {@code true} for a Community Edition version
     *
     * @return {@code true} if continuous aggregation is supported
     */
    static boolean isVersionCompatible(String sVersion, boolean fCommunityEdition)
        {
        if (sVersion == null || sVersion.isBlank())
            {
            return false;
            }

        String sNormalized = sVersion.replaceFirst("(?i)-SNAPSHOT.*$", "")
                .replace('-', '.');

        try
            {
            if (fCommunityEdition)
                {
                String[] asPart = sNormalized.split("\\.");
                if (asPart.length < 2)
                    {
                    return false;
                    }

                int nYear  = Integer.parseInt(asPart[0]);
                int nMonth = Integer.parseInt(asPart[1]);
                return nYear > 26 || nYear == 26 && nMonth >= 10;
                }

            return VersionHelper.isVersionCompatible(
                    VersionHelper.VERSION_26_1_0_0_0,
                    VersionHelper.parseVersion(sNormalized));
            }
        catch (NumberFormatException e)
            {
            return false;
            }
        }

    /**
     * Return whether an encoded Coherence release train supports continuous
     * aggregation fields in version-aware serialization.
     * <p>
     * Calendar-version encoding records the half-year release train rather
     * than the exact month. Exact service capability negotiation therefore
     * uses {@link #isMemberCompatible(Map)} instead.
     *
     * @param nVersion  the encoded Coherence version
     *
     * @return {@code true} if the encoded release train supports continuous
     *         aggregation serialization fields
     */
    static boolean isVersionCompatible(int nVersion)
        {
        boolean fCalendarVersion = ((nVersion >>> 24) & 0x3F) == 15
                && ((nVersion >>> 18) & 0x3F) == 1
                && ((nVersion >>> 12) & 0x3F) == 1
                && ((nVersion >>> 6) & 0x3F) >= 20;

        return fCalendarVersion
                ? VersionHelper.isVersionCompatible(
                        VersionHelper.VERSION_26_10, nVersion)
                : VersionHelper.isVersionCompatible(
                        VersionHelper.VERSION_26_1_0_0_0, nVersion);
        }

    /**
     * Return whether a service member advertises support for the current
     * continuous aggregation service protocol.
     *
     * @param mapConfig  the service-member configuration map
     *
     * @return {@code true} if the member supports continuous aggregation
     */
    static boolean isMemberCompatible(Map<?, ?> mapConfig)
        {
        Object oVersion = mapConfig == null ? null : mapConfig.get(MEMBER_CONFIG_KEY);
        return oVersion instanceof Number
                && ((Number) oVersion).intValue() >= MEMBER_CONFIG_VERSION;
        }

    /**
     * Return the minimum-version description used in compatibility errors.
     *
     * @return the minimum-version description
     */
    static String getMinimumVersionDescription()
        {
        return "26.1.0.0.0 or CE 26.10";
        }

    /**
     * Register a continuous aggregation definition.
     *
     * @param definition  the definition to register
     *
     */
    void registerContinuousAggregation(ContinuousAggregationDefinition definition);

    /**
     * Remove an active continuous aggregation registration.
     *
     * @param definition  the registered definition
     */
    void removeContinuousAggregation(ContinuousAggregationDefinition definition);

    /**
     * Obtain the current result for an active registration.
     *
     * @param definition  the registered definition
     *
     * @return the current final result
     */
    Object aggregateContinuousAggregation(ContinuousAggregationDefinition definition);
    }
