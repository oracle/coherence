/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;

import com.tangosol.internal.util.PartitionedIndexMap;
import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.net.partition.PartitionSet;

import com.tangosol.util.aggregator.QueryRecorder;

import com.tangosol.util.filter.ArrayFilter;
import com.tangosol.util.filter.BetweenFilter;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.Collection;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.util.function.Function;

import java.util.stream.Collectors;

import javax.json.bind.annotation.JsonbProperty;

/**
 * Simple QueryRecord implementation.
 *
 * @since Coherence 3.7.1
 *
 * @author tb 2011.05.26
 */
public class SimpleQueryRecord
        implements QueryRecord, ExternalizableLite, PortableObject
    {
    // ----- Constructors ---------------------------------------------------

    /**
     * Default constructor (necessary for the ExternalizableLite
     * interface).
     */
    public SimpleQueryRecord()
        {
        }

    /**
     * Construct a SimpleQueryRecord from the given collection of partial
     * results.
     *
     * @param type        the record type
     * @param colResults  the collection of partial results
     */
    public SimpleQueryRecord(QueryRecorder.RecordType type, Collection colResults)
        {
        m_type = type;
        mergeResults(colResults);
        }


    // ----- QueryRecord interface ------------------------------------------

    /**
     * {@inheritDoc}
     */
    public QueryRecorder.RecordType getType()
        {
        return m_type;
        }

    /**
     * {@inheritDoc}
     */
    public List<? extends QueryRecord.PartialResult> getResults()
        {
        return m_listResults;
        }


    // ----- helper methods -------------------------------------------------

    /**
     * Merge the partial results from the associated record.  Matching
     * partial results are merged into a single result for the report.
     *
     * @param colResults  the collection of partial results
     */
    protected void mergeResults(Collection colResults)
        {
        List<SimpleQueryRecord.PartialResult> listResults = m_listResults;
        for (Object oResult : colResults)
            {
            QueryRecord.PartialResult resultThat = (QueryRecord.PartialResult) oResult;

            if (!resultThat.getSteps().isEmpty())
                {
                for (PartialResult resultThis : listResults)
                    {
                    if (resultThis.isMatching(resultThat))
                        {
                        resultThis.merge(resultThat);
                        resultThat = null;
                        break;
                        }
                    }

                if (resultThat != null)
                    {
                    // no matching partial result found; create a new one
                    listResults.add(new PartialResult(resultThat));
                    }
                }
            }
        }


        // ----- ExternalizableLite interface ---------------------------

        /**
         * {@inheritDoc}
         */
        public void readExternal(DataInput in)
                throws IOException
            {
            m_type = QueryRecorder.RecordType.fromInt(in.readInt());

            ExternalizableHelper.readCollection(in, m_listResults, null);
            }

        /**
         * {@inheritDoc}
         */
        public void writeExternal(DataOutput out)
                throws IOException
            {
            out.writeInt(m_type.toInt());

            ExternalizableHelper.writeCollection(out, m_listResults);
            }

        // ----- PortableObject interface --------------------------------

        /**
         * {@inheritDoc}
         */
        public void readExternal(PofReader in)
                throws IOException
            {
            m_type = QueryRecorder.RecordType.fromInt(in.readInt(0));
            in.readCollection(1, m_listResults);
            }

        /**
         * {@inheritDoc}
         */
        public void writeExternal(PofWriter out)
                throws IOException
            {
            out.writeInt(0, m_type.toInt());

            out.writeCollection(1, m_listResults);
            }


    // ----- Object overrides -----------------------------------------------

    /**
     * {@inheritDoc}
     */
    public String toString()
        {
        return SimpleQueryRecordReporter.report(this);
        }


    // ----- inner classes --------------------------------------------------

    /**
     * Simple QueryRecord.PartialResult implementation.
     */
    public static class PartialResult
            implements QueryRecord.PartialResult, ExternalizableLite, PortableObject
        {
        // ----- Constructors -------------------------------------------

        /**
         * Default constructor (necessary for the ExternalizableLite
         * interface).
         */
        public PartialResult()
            {
            }

        /**
         * Construct a PartialResult.
         *
         * @param partMask  the partitions to be included in creating this
         *                  partial result
         */
        public PartialResult(PartitionSet partMask)
            {
            m_partMask = partMask;
            }

        /**
         * Construct a PartialResult.
         *
         * @param ctx       the query context
         * @param partMask  the partitions to be included in creating this
         *                  partial result
         */
        public PartialResult(QueryContext ctx, PartitionSet partMask)
            {
            this(partMask);

            m_ctx = ctx;
            }

        /**
         * Copy constructor for a Result.
         *
         * @param result  the result to copy
         */
        public PartialResult(QueryRecord.PartialResult result)
            {
            this(result.getPartitions());

            List listSteps = m_listSteps;
            for (QueryRecord.PartialResult.Step step : result.getSteps())
                {
                listSteps.add(new Step(step));
                }
            }

        // ----- PartialResult interface --------------------------------

        /**
         * {@inheritDoc}
         */
        public List<? extends QueryRecord.PartialResult.Step> getSteps()
            {
            return m_listSteps;
            }

        /**
         * {@inheritDoc}
         */
        public PartitionSet getPartitions()
            {
            return m_partMask;
            }

        // ----- helper methods -----------------------------------------

        /**
         * Instantiate a new explain step for the given filter and add it to
         * this result's list of steps.  This method is called on the server
         * for the top level filter.
         *
         * @param filter  the filter
         *
         * @return the new explain step
         */
        public QueryRecord.PartialResult.ExplainStep instantiateExplainStep(Filter filter)
            {
            ExplainStep step = new ExplainStep(filter);

            m_listSteps.add(step);

            return step;
            }

        /**
         * Instantiate a new trace step for the given filter and add it to
         * this result's list of steps.  This method is called on the server
         * for the top level filter(s).
         *
         * @param filter  the filter
         *
         * @return the new trace step
         */
        public QueryRecord.PartialResult.TraceStep instantiateTraceStep(Filter filter)
            {
            TraceStep step = new TraceStep(filter);

            m_listSteps.add(step);

            return step;
            }

        /**
         * Merge the given result with this one.
         *
         * @param result  the result to merge
         */
        protected void merge(QueryRecord.PartialResult result)
            {
            getPartitions().add(result.getPartitions());

            List<Step> listStepsThis = m_listSteps;

            List<? extends QueryRecord.PartialResult.Step> listStepsThat =
                    result.getSteps();

            for (int i = 0; i < listStepsThat.size(); i++)
                {
                QueryRecord.PartialResult.Step step = listStepsThat.get(i);
                Step mergeStep = listStepsThis.get(i);

                mergeStep.merge(step);
                }
            }

        /**
         * Determine whether or not the given result is capable of being
         * placed in one-to-one correspondence with this result.  Results are
         * matching if their owned lists of steps have the same size, and
         * all pairs of steps in the two lists are matching.
         *
         * @param result  the result to be checked
         *
         * @return true iff the given result matches with this result
         */
        protected boolean isMatching(QueryRecord.PartialResult result)
            {
            List<SimpleQueryRecord.PartialResult.Step> listStepsThis = m_listSteps;

            List<? extends QueryRecord.PartialResult.Step> listStepsThat =
                    result.getSteps();

            if (listStepsThis.size() != listStepsThat.size())
                {
                return false;
                }

            for (int i = 0; i < listStepsThis.size(); i++)
                {
                if (!listStepsThis.get(i).isMatching(listStepsThat.get(i)))
                    {
                    return false;
                    }
                }
            return true;
            }

        // ----- ExternalizableLite interface ---------------------------

        /**
         * {@inheritDoc}
         */
        public void readExternal(DataInput in)
                throws IOException
            {
            m_partMask = (PartitionSet) ExternalizableHelper.readObject(in);

            ExternalizableHelper.readCollection(in, m_listSteps, null);
            }

        /**
         * {@inheritDoc}
         */
        public void writeExternal(DataOutput out)
                throws IOException
            {
            ExternalizableHelper.writeObject(out, m_partMask);

            // convert the list of RecordableSteps into Steps
            List<Step> listSteps = new LinkedList<Step>();
            for (Step step : m_listSteps)
                {
                listSteps.add(new Step(step));
                }
            ExternalizableHelper.writeCollection(out,listSteps);
            }

        // ----- PortableObject interface --------------------------------

        /**
         * {@inheritDoc}
         */
        public void readExternal(PofReader in)
                throws IOException
            {
            m_partMask = (PartitionSet) in.readObject(0);
            in.readCollection(1, m_listSteps);
            }

        /**
         * {@inheritDoc}
         */
        public void writeExternal(PofWriter out)
                throws IOException
            {
            out.writeObject(0, m_partMask);

            // convert the list of RecordableSteps into Steps
            List<Step> listSteps = new LinkedList<Step>();
            for (Step step : m_listSteps)
                {
                listSteps.add(new Step(step));
                }
            out.writeCollection(1, listSteps);
            }

        // ----- inner classes ------------------------------------------

        /**
         * Simple QueryRecord.PartialResult.Step implementation.
         */
        public static class Step
                implements QueryRecord.PartialResult.Step, ExternalizableLite, PortableObject

            {
            // ----- Constructors ---------------------------------------

            /**
             * Default constructor (necessary for the ExternalizableLite
             * interface).
             */
            public Step()
                {
                }

            /**
             * Construct a step.
             *
             * @param filter  the filter
             */
            public Step(Filter filter)
                {
                if (filter instanceof ArrayFilter && !(filter instanceof BetweenFilter))
                    {
                    String sFullClass = filter.getClass().getName();
                    int    ofDotClass = sFullClass.lastIndexOf('.');
                    m_sFilter = ofDotClass < 0 ? sFullClass : sFullClass.substring(ofDotClass + 1);
                    }
                else
                    {
                    m_sFilter = filter.toString();
                    }
                }

            /**
             * Copy constructor for a Step.
             *
             * @param step  the step to copy
             */
            public Step(QueryRecord.PartialResult.Step step)
                {
                m_sFilter     = step.getFilterDescription();
                m_nSizeIn     = step.getPreFilterKeySetSize();
                m_nSizeOut    = step.getPostFilterKeySetSize();
                m_nEfficiency = step.getEfficiency();
                m_cMillis     = step.getDuration();

                for (QueryRecord.PartialResult.IndexLookupRecord record :
                        step.getIndexLookupRecords())
                    {
                    m_setIndexLookupRecords.add(new IndexLookupRecord(record));
                    }

                for (QueryRecord.PartialResult.Step stepInner : step.getSteps())
                    {
                    m_listSubSteps.add(new Step(stepInner));
                    }
                }

            // ----- Step interface -------------------------------------

            /**
             * {@inheritDoc}
             */
            public List<? extends QueryRecord.PartialResult.Step> getSteps()
                {
                return m_listSubSteps;
                }

            /**
             * {@inheritDoc}
             */
            public String getFilterDescription()
                {
                return m_sFilter;
                }

            /**
             * {@inheritDoc}
             */
            public Set<? extends QueryRecord.PartialResult.IndexLookupRecord>
                    getIndexLookupRecords()
                {
                return m_setIndexLookupRecords;
                }

            /**
             * {@inheritDoc}
             */
            public int getEfficiency()
                {
                return m_nEfficiency;
                }

            /**
             * {@inheritDoc}
             */
            public int getPreFilterKeySetSize()
                {
                return m_nSizeIn;
                }

            /**
             * {@inheritDoc}
             */
            public int getPostFilterKeySetSize()
                {
                return m_nSizeOut;
                }

            /**
             * {@inheritDoc}
             */
            public long getDuration()
                {
                return m_cMillis;
                }

            // ----- helper methods -------------------------------------

            /**
             * Determine whether or not the given step is capable of being
             * placed in one-to-one correspondence with this step.  Steps are
             * defined to be matching if both steps have equivalent name,
             * index lookup records and owned lists of sub-steps.
             *
             * @param step  the step to check
             *
             * @return true iff the given step matches with this step
             */
            protected boolean isMatching(QueryRecord.PartialResult.Step step)
                {
                if (getFilterDescription().equals(step.getFilterDescription()) &&
                    getIndexLookupRecords().equals(step.getIndexLookupRecords()))
                    {
                    List<? extends QueryRecord.PartialResult.Step> listSteps =
                            step.getSteps();

                    if (m_listSubSteps.size() == listSteps.size())
                        {
                        int i = 0;
                        for (Step subStep : m_listSubSteps )
                            {
                            if (!subStep.isMatching(listSteps.get(i++)))
                                {
                                return false;
                                }
                            }

                        return true;
                        }
                    }

                return false;
                }

            /**
             * Merge the given step with this one.  This method assumes that
             * the given step matches with this one.
             *
             * @param step  the step to merge
             */
            protected void merge(QueryRecord.PartialResult.Step step)
                {
                m_nSizeIn     += step.getPreFilterKeySetSize();
                m_nSizeOut    += step.getPostFilterKeySetSize();
                m_nEfficiency += step.getEfficiency();
                m_cMillis     += step.getDuration();

                Map<IndexLookupRecord, IndexLookupRecord> mapIndexRecords =
                        ((Set<IndexLookupRecord>) step.getIndexLookupRecords()).stream().collect(Collectors.toMap(Function.identity(), Function.identity()));
                for (IndexLookupRecord indexRecord : m_setIndexLookupRecords)
                    {
                    IndexLookupRecord stepIndexRecord = mapIndexRecords.get(indexRecord);

                    if (stepIndexRecord != null && stepIndexRecord.m_sIndex != null)
                        {
                        indexRecord.m_cBytes         += stepIndexRecord.m_cBytes;
                        indexRecord.m_cDistinctValues = Math.max(indexRecord.m_cDistinctValues, stepIndexRecord.m_cDistinctValues);
                        indexRecord.m_sIndex          = indexRecord.buildIndexDescription();
                        }
                    }

                List<? extends QueryRecord.PartialResult.Step> listSteps =
                        step.getSteps();

                int i = 0;
                for (Step subStep : m_listSubSteps )
                    {
                    subStep.merge(listSteps.get(i++));
                    }
                }

            // ----- ExternalizableLite interface -----------------------

            /**
             * {@inheritDoc}
             */
            public void readExternal(DataInput in)
                    throws IOException
                {
                m_sFilter     = (String) ExternalizableHelper.readObject(in);
                m_nEfficiency = in.readInt();
                m_nSizeIn     = in.readInt();
                m_nSizeOut    = in.readInt();
                m_cMillis     = in.readLong();
                ExternalizableHelper.readCollection(in, m_setIndexLookupRecords, null);
                ExternalizableHelper.readCollection(in, m_listSubSteps, null);
                }

            /**
             * {@inheritDoc}
             */
            public void writeExternal(DataOutput out)
                    throws IOException
                {
                ExternalizableHelper.writeObject(out, m_sFilter);
                out.writeInt(m_nEfficiency);
                out.writeInt(m_nSizeIn);
                out.writeInt(m_nSizeOut);
                out.writeLong(m_cMillis);
                ExternalizableHelper.writeCollection(out, m_setIndexLookupRecords);

                // convert the list of RecordableSteps into Steps
                List<Step> listSteps = new LinkedList<Step>();
                for (Step step : m_listSubSteps)
                    {
                    listSteps.add(new Step(step));
                    }
                ExternalizableHelper.writeCollection(out, listSteps);
                }

            // ----- PortableObject interface ---------------------------

            /**
             * {@inheritDoc}
             */
            public void readExternal(PofReader in)
                    throws IOException
                {
                m_sFilter     = (String) in.readObject(0);
                m_nEfficiency = in.readInt(1);
                m_nSizeIn     = in.readInt(2);
                m_nSizeOut    = in.readInt(3);
                m_cMillis     = in.readLong(4);

                in.readCollection(5, m_setIndexLookupRecords);
                in.readCollection(6, m_listSubSteps);
                }

            /**
             * {@inheritDoc}
             */
            public void writeExternal(PofWriter out)
                    throws IOException
                {
                out.writeObject(    0, m_sFilter);
                out.writeInt(       1, m_nEfficiency);
                out.writeInt(       2, m_nSizeIn);
                out.writeInt(       3, m_nSizeOut);
                out.writeLong(      4, m_cMillis);
                out.writeCollection(5, m_setIndexLookupRecords);

                // convert the list of RecordableSteps into Steps
                List<Step> listSteps = new LinkedList<Step>();
                for (Step step : m_listSubSteps)
                    {
                    listSteps.add(new Step(step));
                    }
                out.writeCollection(6, listSteps);
                }

            // ----- data members ---------------------------------------

            /**
             * The filter description.
             */
            @JsonbProperty("filter")
            protected String m_sFilter;

            /**
             * The estimated cost.
             */
            @JsonbProperty("efficiency")
            protected int m_nEfficiency;

            /**
             * The pre-execution key set size.
             */
            @JsonbProperty("keySetSizePre")
            protected int m_nSizeIn = 0;

            /**
             * The post-execution key set size.
             */
            @JsonbProperty("keySetSizePost")
            protected int m_nSizeOut = 0;

            /**
             * The execution time in milliseconds.
             */
            @JsonbProperty("millis")
            protected long m_cMillis = 0L;

            /**
             * The set of index lookup records.
             */
            @JsonbProperty("indexLookupRecords")
            protected Set<IndexLookupRecord> m_setIndexLookupRecords =
                    new HashSet<IndexLookupRecord>();

            /**
             * The list of child steps.
             */
            @JsonbProperty("subSteps")
            protected List<Step> m_listSubSteps =
                    new LinkedList<Step>();
            }

        /**
         * Simple abstract RecordableStep implementation.
         */
        public abstract class AbstractRecordableStep
                extends Step
                implements QueryRecord.PartialResult.RecordableStep
            {
            /**
             * Construct an AbstractRecordableStep.
             *
             * @param filter  the step filter
             */
            public AbstractRecordableStep(Filter filter)
                {
                super(filter);
                }

            // ----- RecordableStep interface ---------------------------

            /**
             * {@inheritDoc}
             */
            public void recordPreFilterKeys(int nSizeIn)
                {
                m_nSizeIn += nSizeIn;
                }

            /**
             * {@inheritDoc}
             */
            public void recordExtractor(ValueExtractor extractor)
                {
                MapIndex index = m_ctx.getBackingMapContext().getIndexMap().get(extractor);

                m_setIndexLookupRecords.add(new IndexLookupRecord(extractor, index));
                }

            // ----- data members ---------------------------------------

            /**
             * The Map of child steps.
             */
            protected Map<Filter, Step> m_mapSteps =
                    new IdentityHashMap<Filter, Step>();
            }

        /**
         * Simple QueryRecord.PartialResult.ExplainStep implementation.
         */
        public class ExplainStep
                extends AbstractRecordableStep
                implements QueryRecord.PartialResult.ExplainStep
            {
            // ----- Constructors ---------------------------------------

            /**
             * Construct an ExplainStep
             *
             * @param filter  the step filter
             */
            public ExplainStep(Filter filter)
                {
                super(filter);
                }

            // ----- ExplainStep interface ------------------------------

            /**
             * {@inheritDoc}
             */
            public void recordEfficiency(int nEfficiency)
                {
                m_nEfficiency = nEfficiency;
                }

            /**
             * {@inheritDoc}
             */
            public QueryRecord.PartialResult.ExplainStep ensureStep(Filter filter)
                {
                ExplainStep subStep = (ExplainStep) m_mapSteps.get(filter);
                if (subStep == null)
                    {
                    subStep = new ExplainStep(filter);

                    m_mapSteps.put(filter, subStep);
                    m_listSubSteps.add(subStep);
                    }

                return subStep;
                }
            }

        /**
         * Simple QueryRecord.PartialResult.TraceStep implementation.
         */
        public class TraceStep
                extends AbstractRecordableStep
                implements QueryRecord.PartialResult.TraceStep
            {
            // ----- Constructors ---------------------------------------

            /**
             * Construct a TraceStep
             *
             * @param filter  the step filter
             */
            public TraceStep(Filter filter)
                {
                super(filter);
                }

            // ----- TraceStep interface --------------------------------

            /**
             * {@inheritDoc}
             */
            public void recordPostFilterKeys(int nSizeOut)
                {
                m_nSizeOut += nSizeOut;
                }

            /**
             * {@inheritDoc}
             */
            public void recordDuration(long cMillisElapsed)
                {
                m_cMillis += cMillisElapsed;
                }

            /**
             * {@inheritDoc}
             */
            public QueryRecord.PartialResult.TraceStep ensureStep(Filter filter)
                {
                TraceStep subStep = (TraceStep) m_mapSteps.get(filter);
                if (subStep == null)
                    {
                    subStep = new TraceStep(filter);

                    m_mapSteps.put(filter, subStep);
                    m_listSubSteps.add(subStep);
                    }

                return subStep;
                }
            }

        /**
         * Simple QueryRecord.PartialResult.IndexLookupRecord implementation.
         */
        public static class IndexLookupRecord
                implements QueryRecord.PartialResult.IndexLookupRecord,
                           ExternalizableLite, PortableObject
            {
            // ----- Constructors ---------------------------------------

            /**
             * Default constructor (necessary for the ExternalizableLite
             * interface).
             */
            public IndexLookupRecord()
                {
                }

            /**
             * Construct an IndexLookupRecord.
             *
             * @param extractor  the extractor
             * @param index      the index
             */
            public IndexLookupRecord(ValueExtractor extractor, MapIndex index)
                {
                m_sExtractor = extractor.toString();
                m_fOrdered   = index != null && index.isOrdered();

                String sIndex = null;
                if (index != null)
                    {
                    if (index instanceof SimpleMapIndex)
                        {
                        if (index instanceof ConditionalIndex)
                            {
                            ConditionalIndex condIdx = (ConditionalIndex) index;

                            sIndex = "Conditional: Filter=" + condIdx.getFilter()
                            + ", ForwardIndex=" + condIdx.isForwardIndexSupported() + ", ";
                            }
                        else
                            {
                            sIndex = "Simple: ";
                            }

                        m_cBytes          = index.getUnits();
                        m_cDistinctValues = index.getIndexContents().size();
                        m_sIndexDef       = sIndex;

                        sIndex = buildIndexDescription();
                        }
                    else if (index instanceof PartitionedIndexMap.PartitionedIndex)
                        {
                        sIndex = "Partitioned: ";

                        m_cBytes          = index.getUnits();
                        m_cDistinctValues = index.getIndexContents().size();
                        m_sIndexDef       = sIndex;

                        sIndex = buildIndexDescription();
                        }
                    else
                        {
                        sIndex = index.toString();
                        }
                    }

                m_sIndex = sIndex;
                }

            /**
             * Copy constructor for an IndexLookupRecord.
             *
             * @param record  the record to copy
             */
            public IndexLookupRecord(
                    QueryRecord.PartialResult.IndexLookupRecord record)
                {
                this(record.getExtractorDescription(),
                     record.getIndexDescription(),
                     record.isOrdered(),
                     ((IndexLookupRecord) record).getMemoryUsage(),
                     ((IndexLookupRecord) record).getSize(),
                     ((IndexLookupRecord) record).getIndexDef());
                }

            /**
             * Construct an IndexLookupRecord.
             *
             * @param sExtractor       the extractor description
             * @param sIndex           the index description
             * @param fOrdered         indicates whether or not the associated
             *                         index is ordered
             * @param cBytes           the index footprint
             * @param cDistinctValues  the index size
             * @param sIndexDef        the index definition
             */
            protected IndexLookupRecord(String sExtractor, String sIndex, boolean fOrdered, long cBytes,
                                        int cDistinctValues, String sIndexDef)
                {
                m_sExtractor      = sExtractor;
                m_sIndex          = sIndex;
                m_fOrdered        = fOrdered;
                m_cBytes          = cBytes;
                m_cDistinctValues = cDistinctValues;
                m_sIndexDef       = sIndexDef;
                }

            // ----- IndexLookupRecord interface ------------------------

            /**
             * {@inheritDoc}
             */
            public String getExtractorDescription()
                {
                return m_sExtractor;
                }

            /**
             * {@inheritDoc}
             */
            public String getIndexDescription()
                {
                return m_sIndex;
                }

            /**
             * Returns index memory usage in bytes.
             *
             * @return index memory usage in bytes; -1 if there is no index
             */
            public long getMemoryUsage()
                {
                return m_cBytes;
                }

            /**
             * Return index content map size.
             *
             * @return index content map size; -1 if there is no index
             */
            public int getSize()
                {
                return m_cDistinctValues;
                }

            /**
             * Returns the index definition.
             *
             * @return the index definition; null if there is no index
             */
            public String getIndexDef()
                {
                return m_sIndexDef;
                }

            /**
             * {@inheritDoc}
             */
            public boolean isOrdered()
                {
                return m_fOrdered;
                }

            // ----- Object methods -------------------------------------

            /**
             * {@inheritDoc}
             */
            public int hashCode()
                {
                return (m_fOrdered ? 1 : 0 ) +
                        Base.hashCode(m_sIndexDef) + m_sExtractor.hashCode();
                }

            /**
             * {@inheritDoc}
             */
            public boolean equals(Object o)
                {
                if (o instanceof IndexLookupRecord)
                    {
                    IndexLookupRecord that = (IndexLookupRecord) o;

                    // Note: IndexLookupRecords are considered equivalent based on
                    //       the definition of the indices and not varying factors
                    //       such as index footprint and size.
                    return m_fOrdered == that.m_fOrdered          &&
                           m_sExtractor.equals(that.m_sExtractor) &&
                           Base.equals(this.m_sIndexDef, that.m_sIndexDef);
                    }

                return false;
                }

            // ----- ExternalizableLite interface -----------------------

            /**
             * {@inheritDoc}
             */
            public void readExternal(DataInput in)
                    throws IOException
                {
                m_sExtractor = (String) ExternalizableHelper.readObject(in);
                m_sIndex     = (String) ExternalizableHelper.readObject(in);
                m_fOrdered   = in.readBoolean();

                if (m_sIndex != null)
                    {
                    parseIndexDescription(m_sIndex);
                    }
                }

            /**
             * {@inheritDoc}
             */
            public void writeExternal(DataOutput out)
                    throws IOException
                {
                ExternalizableHelper.writeObject(out, m_sExtractor);
                ExternalizableHelper.writeObject(out, m_sIndex);
                out.writeBoolean(m_fOrdered);
                }


            // ----- PortableObject interface ----------------------------

            /**
             * {@inheritDoc}
             */
            public void readExternal(PofReader in)
                    throws IOException
                {
                m_sExtractor = (String) in.readObject(0);
                m_sIndex     = (String) in.readObject(1);
                m_fOrdered   = in.readBoolean(2);

                if (m_sIndex != null)
                    {
                    parseIndexDescription(m_sIndex);
                    }
                }

            /**
             * {@inheritDoc}
             */
            public void writeExternal(PofWriter out)
                    throws IOException
                {
                out.writeObject(0, m_sExtractor);
                out.writeObject(1, m_sIndex);
                out.writeBoolean(2, m_fOrdered);
                }

            /**
             * Build an index description for this index.
             *
             * @return an index description for this index if there is an index definition;
             *         null otherwise
             */
            private String buildIndexDescription()
                {
                if (m_sIndexDef == null)
                    {
                    return m_sIndexDef;
                    }

                String sFP = Base.toMemorySizeString(m_cBytes, false);
                return m_sIndexDef + FOOTPRINT + (sFP.endsWith("B") ? sFP : sFP + "B")
                                 + MAP_SIZE + m_cDistinctValues;
                }

            /**
             * Parses an index description into it's definition, footprint,
             * and map size.
             *
             * @param sIndex  the index description
             */
            protected void parseIndexDescription(String sIndex)
                {
                int iStart = sIndex.indexOf(FOOTPRINT);

                if (iStart <= 0)
                    {
                    return;
                    }

                m_sIndexDef = sIndex.substring(0, iStart);

                int iEnd = sIndex.indexOf(',', iStart);
                m_cBytes = Base.parseMemorySize(sIndex.substring(iStart + FOOTPRINT_LEN, iEnd));

                iStart = sIndex.indexOf(MAP_SIZE);
                m_cDistinctValues = Integer.parseInt(sIndex.substring(iStart + MAP_SIZE_LEN));
                }


            // ----- constants ------------------------------------------

            /*
             * Footprint string in the index description.
             */
            private static final String FOOTPRINT     = "Footprint=";

            /*
             * Map size string in the index description.
             */
            private static final String MAP_SIZE      = ", Size=";

            /*
             * Footprint string length in the index description.
             */
            private static final int    FOOTPRINT_LEN = FOOTPRINT.length();

            /*
             * Map size string length in the index description.
             */
            private static final int    MAP_SIZE_LEN  = MAP_SIZE.length();


            // ----- data members ---------------------------------------

            /**
             * The extractor description.
             */
            @JsonbProperty("extractor")
            private String m_sExtractor;

            /**
             * The index description.
             */
            @JsonbProperty("index")
            private String m_sIndex;

            /**
             * Indicates whether or not the associated index is ordered.
             */
            @JsonbProperty("ordered")
            private boolean m_fOrdered;

            /**
             * The index type description.
             */
            @JsonbProperty("indexDesc")
            private String m_sIndexDef;

            /**
             * The index footprint in bytes.
             */
            @JsonbProperty("bytes")
            private long m_cBytes = -1;

            /**
             * The index content map size.
             */
            @JsonbProperty("distinctValues")
            private int m_cDistinctValues = -1;
            }

        // ----- data members -------------------------------------------

        /**
         * The map of steps.
         */
        @JsonbProperty("steps")
        protected List<Step> m_listSteps = new LinkedList<Step>();

        /**
         * The partitions.
         */
        @JsonbProperty("partitionSet")
        private PartitionSet m_partMask;

        /**
         * The query context used during the recording of the steps.
         */
        private transient QueryContext m_ctx;
        }


    // ----- data members ---------------------------------------------------

    /**
     * This record type.
     */
    @JsonbProperty("type")
    private QueryRecorder.RecordType m_type;

    /**
     * The list of partial results.
     */
    @JsonbProperty("results")
    private List<SimpleQueryRecord.PartialResult> m_listResults =
            new LinkedList<SimpleQueryRecord.PartialResult>();
    }
