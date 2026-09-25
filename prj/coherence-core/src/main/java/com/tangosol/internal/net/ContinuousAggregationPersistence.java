/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net;

import com.tangosol.io.Serializer;

import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.Filter;
import com.tangosol.util.InvocableMap;

/**
 * Versioned serialization helpers for continuous aggregation definitions and
 * exact partition-state checkpoints.
 *
 * @author Aleks Seovic  2026.09.11
 * @since 26.10
 */
public final class ContinuousAggregationPersistence
    {
    // ----- constructors ---------------------------------------------------

    private ContinuousAggregationPersistence()
        {
        }

    // ----- definition serialization -------------------------------------

    /**
     * Serialize a continuous aggregation definition.
     *
     * @param definition  the definition
     * @param serializer  the service serializer
     *
     * @return the serialized definition
     */
    public static Binary toDefinitionBinary(ContinuousAggregationDefinition definition,
                                            Serializer serializer)
        {
        return ExternalizableHelper.toBinary(new Object[]
            {
            Integer.valueOf(DEFINITION_FORMAT_VERSION),
            definition.getFilter(),
            definition.getAggregator()
            }, serializer);
        }

    /**
     * Deserialize a continuous aggregation definition.
     *
     * @param binary      the serialized definition
     * @param serializer  the service serializer
     *
     * @return the definition
     */
    @SuppressWarnings("rawtypes")
    public static ContinuousAggregationDefinition fromDefinitionBinary(Binary binary,
                                                                        Serializer serializer)
        {
        Object o = ExternalizableHelper.fromBinary(binary, serializer);
        if (!(o instanceof Object[]) || ((Object[]) o).length != 3)
            {
            throw new IllegalArgumentException("Invalid continuous aggregation definition");
            }

        Object[] ao = (Object[]) o;
        if (!(ao[0] instanceof Number)
                || ((Number) ao[0]).intValue() != DEFINITION_FORMAT_VERSION
                || !(ao[1] instanceof Filter)
                || !(ao[2] instanceof InvocableMap.StreamingAggregator))
            {
            throw new IllegalArgumentException("Unsupported continuous aggregation definition format");
            }

        return new ContinuousAggregationDefinition(
                (Filter) ao[1], (InvocableMap.StreamingAggregator) ao[2]);
        }

    // ----- state serialization ------------------------------------------

    /**
     * Serialize an exact partition-state checkpoint.
     *
     * @param binDefinition    the serialized definition fingerprint
     * @param nPartition       the partition identifier
     * @param lOwnershipVersion the partition ownership version
     * @param lDataVersion     the logical partition data version
     * @param oState           the aggregator state snapshot
     * @param serializer       the service serializer
     *
     * @return the serialized state envelope
     */
    public static Binary toStateBinary(Binary binDefinition,
                                       int nPartition,
                                       long lOwnershipVersion,
                                       long lDataVersion,
                                       Object oState,
                                       Serializer serializer)
        {
        return ExternalizableHelper.toBinary(new Object[]
            {
            Integer.valueOf(STATE_FORMAT_VERSION),
            binDefinition,
            Integer.valueOf(nPartition),
            Long.valueOf(lOwnershipVersion),
            Long.valueOf(lDataVersion),
            oState
            }, serializer);
        }

    /**
     * Deserialize and validate an exact partition-state checkpoint.
     * A negative expected version disables validation of that version. This
     * is used for a state recovered atomically with its partition snapshot.
     *
     * @param binary             the serialized state envelope
     * @param binDefinition      the expected definition fingerprint
     * @param nPartition         the expected partition identifier
     * @param lOwnershipVersion  the expected ownership version, or negative
     * @param lDataVersion       the expected logical data version, or negative
     * @param serializer         the service serializer
     *
     * @return the validated state record
     */
    public static StateRecord fromStateBinary(Binary binary,
                                              Binary binDefinition,
                                              int nPartition,
                                              long lOwnershipVersion,
                                              long lDataVersion,
                                              Serializer serializer)
        {
        Object o = ExternalizableHelper.fromBinary(binary, serializer);
        if (!(o instanceof Object[]) || ((Object[]) o).length != 6)
            {
            throw new IllegalArgumentException("Invalid continuous aggregation state");
            }

        Object[] ao = (Object[]) o;
        if (!(ao[0] instanceof Number)
                || ((Number) ao[0]).intValue() != STATE_FORMAT_VERSION
                || !(ao[1] instanceof Binary)
                || !(ao[2] instanceof Number)
                || !(ao[3] instanceof Number)
                || !(ao[4] instanceof Number))
            {
            throw new IllegalArgumentException("Unsupported continuous aggregation state format");
            }

        Binary binActualDefinition = (Binary) ao[1];
        int    nActualPartition     = ((Number) ao[2]).intValue();
        long   lActualOwnership     = ((Number) ao[3]).longValue();
        long   lActualData          = ((Number) ao[4]).longValue();

        if (!binDefinition.equals(binActualDefinition)
                || nPartition != nActualPartition
                || lOwnershipVersion >= 0 && lOwnershipVersion != lActualOwnership
                || lDataVersion >= 0 && lDataVersion != lActualData)
            {
            throw new IllegalArgumentException("Continuous aggregation state identity mismatch");
            }

        return new StateRecord(lActualOwnership, lActualData, ao[5]);
        }

    // ----- inner class: StateRecord --------------------------------------

    /**
     * A decoded and validated continuous aggregation partition state.
     */
    public static final class StateRecord
        {
        private StateRecord(long lOwnershipVersion, long lDataVersion, Object oState)
            {
            f_lOwnershipVersion = lOwnershipVersion;
            f_lDataVersion      = lDataVersion;
            f_oState            = oState;
            }

        /**
         * Return the ownership version captured by the checkpoint.
         *
         * @return the ownership version
         */
        public long getOwnershipVersion()
            {
            return f_lOwnershipVersion;
            }

        /**
         * Return the logical data version captured by the checkpoint.
         *
         * @return the logical data version
         */
        public long getDataVersion()
            {
            return f_lDataVersion;
            }

        /**
         * Return the decoded aggregator state.
         *
         * @return the aggregator state
         */
        public Object getState()
            {
            return f_oState;
            }

        private final long f_lOwnershipVersion;
        private final long f_lDataVersion;
        private final Object f_oState;
        }

    // ----- constants -----------------------------------------------------

    /** Definition serialization format version. */
    public static final int DEFINITION_FORMAT_VERSION = 1;

    /** State serialization format version. */
    public static final int STATE_FORMAT_VERSION = 1;
    }
