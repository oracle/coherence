/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


import com.tangosol.net.partition.PartitionSet;

import com.tangosol.util.aggregator.QueryRecorder;

import com.tangosol.util.filter.EntryFilter;
import com.tangosol.util.filter.IndexAwareFilter;
import com.tangosol.util.filter.QueryRecorderFilter;

import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * The QueryRecord object carries information regarding the estimated or actual
 * execution cost for a query operation.
 *
 * @since Coherence 3.7.1
 *
 * @author tb 2011.05.26
 */
public interface QueryRecord
    {
    /**
     * Get the {@link com.tangosol.util.aggregator.QueryRecorder.RecordType type}
     * that was specified when this query record was created.
     *
     * @return the record type
     */
    public QueryRecorder.RecordType getType();

    /**
     * Get the list of partial results for this query record.
     *
     * @return the list of results
     */
    public List<? extends QueryRecord.PartialResult> getResults();


    // ----- QueryRecord.PartialResult interface --------------------------

    /**
     * A QueryRecord.PartialResult is a partial query record that contains
     * recorded costs for a query operation.  Partial results are collected
     * in a query record by a {@link QueryRecorder}.
     */
    public interface PartialResult
        {
        /**
         * Get the list of steps for this query record partial result in the
         * order that they occurred.
         *
         * @return the list of steps
         */
        public List<? extends QueryRecord.PartialResult.Step> getSteps();

        /**
         * Get the set of partitions associated with this partial result.
         *
         * @return the partition set
         */
        public PartitionSet getPartitions();

        // ----- QueryRecord.Step interface -------------------------------

        /**
         * A QueryRecord.Step carries the recorded cost of evaluating a filter
         * as part of a query operation.  This cost may be the estimated or
         * actual execution cost depending on the
         * {@link com.tangosol.util.aggregator.QueryRecorder.RecordType type} of the
         * {@link QueryRecorder recorder} in use when the step was created.
         */
        public interface Step
            {
            /**
             * Get a description of the filter that was associated with this
             * step during its creation.
             *
             * @return the description of the filter
             */
            public String getFilterDescription();

            /**
             * Get the recorded information about the index lookups performed
             * during filter evaluation as part of a query record.
             *
             * @return a set of {@link IndexLookupRecord}
             */
            public Set<? extends IndexLookupRecord> getIndexLookupRecords();

            /**
             * Get the calculated cost of applying the filter as defined by
             * {@link IndexAwareFilter#calculateEffectiveness(Map, Set)
             * calculateEffectiveness}
             *
             * @return an effectiveness estimate of how well the associated
             *         filter can use any applicable index
             */
            public int getEfficiency();

            /**
             * Get the size of the key set prior to evaluating the filter or
             * applying an index.  This value can be used together with
             * {@link #getPostFilterKeySetSize()} to calculate an actual
             * effectiveness (reduction of the key set) for this filter step.
             *
             * @return the size of the key set prior to evaluating the filter
             *         or applying an index
             */
            public int getPreFilterKeySetSize();

            /**
             * Get the size of the key set remaining after evaluating the
             * filter or applying an index.  This value can be used together
             * with {@link #getPreFilterKeySetSize()} to calculate an actual
             * effectiveness (reduction of the key set) for this filter step.
             *
             * @return the size of the key set after evaluating the filter
             *         or applying an index
             */
            public int getPostFilterKeySetSize();

            /**
             * Get the amount of time (in ms) spent evaluating the filter or
             * applying an index for this query plan step.
             *
             * @return the number of milliseconds spent evaluating the filter
             */
            public long getDuration();

            /**
             * Return inner nested steps, may be null if not nested.
             *
             * @return the inner nested steps in the order they are applied
             */
            public List<? extends Step> getSteps();
            }


        // ----- QueryRecord.RecordableStep interface ---------------------

        /**
         * A QueryRecord.RecordableStep is a {@link Step step} that provides the
         * ability to record the cost of evaluating a filter as part of a
         * query operation.
         */
        public interface RecordableStep
                extends Step
            {
            /**
             * Record the number of keys passed to the filter for evaluation.
             * This method may be called repeatedly on the same step instance.
             * Each call will add to the total recorded pre-evaluation key set
             * size for this step.
             * <p>
             * During the scan phase of a query trace operation, each entry is
             * individually evaluated against the filter. Each call to
             * {@link com.tangosol.util.filter.QueryRecorderFilter#trace(QueryContext,
             * QueryRecord.PartialResult.TraceStep, Map.Entry) trace}
             * should record a key count of <code>1</code>.
             *
             * @param cKeys  the number of keys to be evaluated
             */
            public void recordPreFilterKeys(int cKeys);

            /**
             * Record all relevant index information for any index associated
             * with the given extractor (e.g. index lookup and range scan).
             * This method may be called multiple times if there is more than
             * one extractor associated with the filter used to create this
             * step.
             *
             * @param extractor  the extractor associated with the filter for
             *                   this step
             */
            public void recordExtractor(ValueExtractor extractor);
            }


        // ----- QueryRecord.ExplainStep interface ------------------------

        /**
         * A QueryRecord.ExplainStep is a {@link RecordableStep} that provides
         * the ability to record the estimated cost of evaluating a filter as
         * part of a query operation.
         */
        public interface ExplainStep
                extends RecordableStep
            {
            /**
             * Record the calculated cost of applying the filter as defined by
             * {@link IndexAwareFilter#calculateEffectiveness(Map, Set)
             * calculateEffectiveness}
             *
             * @param nCost  an effectiveness estimate of how well the
             *               associated filter can use any applicable index
             */
            public void recordEfficiency(int nCost);

            /**
             * Ensure an inner nested explain step for the given filter.  If
             * there is no inner nested step associated with the given filter
             * then a new step is created.
             *
             * @param filter  the filter to associate the new step with
             *
             * @return the inner nested step associated with the given filter
             */
            public ExplainStep ensureStep(Filter filter);
            }

        // ----- QueryRecord.TraceStep interface --------------------------

        /**
         * A QueryRecord.TraceStep is a {@link RecordableStep} that provides the
         * ability to record the information associated with the actual cost
         * of evaluating a filter as part of a query operation.
         */
        public interface TraceStep extends RecordableStep
            {
            /**
             * Record the number of keys remaining after filter evaluation.
             * This method may be called repeatedly on the same step instance
             * during the scan phase of a query trace plan operation.  Each
             * call will add to the total recorded post-evaluation key set
             * size for this step.
             * <p>
             * During the scan phase of a query trace plan operation, each
             * entry is evaluated against the filter individually. Each call to
             * {@link QueryRecorderFilter#trace(QueryContext,
             * QueryRecord.PartialResult.TraceStep, Map.Entry) trace}
             * should record a key set count of <code>1</code> and a result
             * key set count of <code>fResult ? 1 : 0</code> where
             * <code>fResult</code> is the result of a call to
             * {@link EntryFilter#evaluateEntry(Map.Entry)}.
             *
             * @param cKeys  the number of keys remaining after filter
             *               evaluation
             */
            public void recordPostFilterKeys(int cKeys);

            /**
             * Record the time spent evaluating the filter or applying an
             * index.  This method may be called repeatedly on the same step
             * instance during the scan phase of a query trace plan operation.
             * Calling this method repeatedly will add to the total duration
             * recorded for this step.
             *
             * @param cMillis  the number of milliseconds spent evaluating the
             *                 filter
             */
            public void recordDuration(long cMillis);

            /**
             * Ensure an inner nested trace step for the given filter.  If
             * there is no inner nested step associated with the given filter
             * then a new step is created.
             *
             * @param filter  the filter to associate the new step with
             *
             * @return the inner nested step associated with the given filter
             */
            public TraceStep ensureStep(Filter filter);
            }

        // ----- inner interface: IndexLookupRecord ---------------------

        /**
         * An IndexLookupRecord holds the recorded information about an index
         * lookup performed during filter evaluation as part of a query
         * record.
         * <p>
         * An IndexLookupRecord is created each time that
         * {@link RecordableStep#recordExtractor(ValueExtractor)} is called on
         * a query record step.
         */
        public interface IndexLookupRecord
            {
            /**
             * Get a description of the extractor that was used for the index
             * lookup.
             *
             * @return the extractor description
             */
            public String getExtractorDescription();

            /**
             * Get a description of the associated index.
             *
             * @return the index description; null if no index was found
             *         for the associated extractor
             */
            public String getIndexDescription();

            /**
             * Indicates whether or not the associated index is ordered.
             *
             * @return true if the associated index is ordered; false if the
             *         index is not ordered or if no index was found for the
             *         associated extractor
             */
            public boolean isOrdered();
            }
        }
    }
