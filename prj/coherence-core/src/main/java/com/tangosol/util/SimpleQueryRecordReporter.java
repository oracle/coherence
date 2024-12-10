/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


import com.tangosol.util.aggregator.QueryRecorder;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


/**
 * Simple query record reporter used to obtain a string representation of
 * {@link QueryRecord} object.
 *
 * @since Coherence 3.7.1
 *
 * @author tb 2011.05.26
 */
public class SimpleQueryRecordReporter
    {
    // ----- helper methods -------------------------------------------------

    /**
     * Return a report for the given query record.
     *
     * @param record  the record
     *
     * @return  a report for the given query record
     */
    public static String report(QueryRecord record)
        {
        StringBuilder sb = new StringBuilder();

        List<QueryRecord.PartialResult.IndexLookupRecord> listIndexLookups =
                new LinkedList<QueryRecord.PartialResult.IndexLookupRecord>();

        List<? extends QueryRecord.PartialResult> listRecords = record.getResults();

        boolean      fReportPartition = listRecords.size() > 1;
        List<String> listFooter       = new ArrayList<String>();

        for (QueryRecord.PartialResult partial : listRecords)
            {
            sb.append(reportResult(partial, record.getType(),
                    listIndexLookups, fReportPartition, listFooter));
            }

        sb.append(reportIndexLookUps(listIndexLookups, listFooter));
        sb.append(reportFooter(listFooter));

        return sb.toString();
        }

    /**
     * Report the given result.
     *
     * @param result            the result
     * @param type              the record type
     * @param listIndexLookups  the list of lookup ids
     * @param fReportPartition  indicates whether or not to report partitions
     * @param listFooter        the list of full names which were truncated elsewhere
     *                          in the report
     *
     * @return a report for the given result
     */
    protected static String reportResult(QueryRecord.PartialResult result,
            QueryRecorder.RecordType type,
            List<QueryRecord.PartialResult.IndexLookupRecord> listIndexLookups,
            boolean fReportPartition, List<String> listFooter)
        {
        StringBuilder sb = new StringBuilder();

        if (type == QueryRecorder.RecordType.TRACE)
            {
            sb.append(String.format(TRACE_HEADER_FORMAT,
                    "Filter Name", "Index", "Effectiveness", "Duration"));
            }
        else
            {
            sb.append(String.format(EXPLAIN_HEADER_FORMAT,
                    "Filter Name", "Index", "Cost"));
            }

        sb.append(String.format(DIVIDER));

        for (QueryRecord.PartialResult.Step childStep : result.getSteps())
            {
            sb.append(reportStep(childStep, type, listIndexLookups, 0, listFooter));
            sb.append(String.format("%n"));
            }

        sb.append(String.format("%n"));

        if (fReportPartition)
            {
            sb.append(String.format(PARTITION_FORMAT,
                    result.getPartitions().toString()));
            }

        return sb.toString();
        }

    /**
     * Report the index look ups.
     *
     * @param listIndexLookups  the list of lookup ids
     * @param listFooter        the list containing complete names which were truncated elsewhere
     *                          in the report
     *
     * @return a report for the index look ups
     */
    protected static String reportIndexLookUps(
            List<QueryRecord.PartialResult.IndexLookupRecord> listIndexLookups, List<String> listFooter)
        {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(INDEX_HEADER_FORMAT, "Index", "Description", "Extractor", "Ordered"));
        sb.append(String.format(DIVIDER));

        for (int i = 0; i < listIndexLookups.size(); i++)
            {
            sb.append(reportIndexLookupRecord(i, listIndexLookups.get(i), listFooter));
            }

        sb.append(String.format("%n"));
        return sb.toString();
        }

    /**
     * Report the index look ups.
     *
     * @param listFooter  the list of full names to be reported
     *
     * @return a footer portion of the report, where name that had to be truncated
     *         in the main portion, are printed in full
     */
    protected static String reportFooter(List<String> listFooter)
        {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(FOOTER_HEADER_FORMAT, "N", "Full Name"));
        sb.append(String.format(DIVIDER));

        for (int i = 0; i < listFooter.size(); i++)
            {
            sb.append(reportFooterItem(i, listFooter.get(i)));
            }

        return sb.toString();
        }

    /**
     * Report the given step.
     *
     * @param step              the step
     * @param type              the record type
     * @param listIndexLookups  the list of lookup ids
     * @param nLevel            the indent level
     * @param listFooter        the list containing complete names, which had to be truncated
     *                          in the main report
     *
     * @return a report line for the given step
     */
    protected static String reportStep(QueryRecord.PartialResult.Step step,
                                       QueryRecorder.RecordType type,
                                       List<QueryRecord.PartialResult.IndexLookupRecord> listIndexLookups,
                                       int nLevel,
                                       List<String> listFooter)
        {
        StringBuilder sbName = new StringBuilder();
        for (int i = 0; i < nLevel; ++i)
            {
            sbName.append("  ");
            }
        sbName.append(step.getFilterDescription());

        checkTruncation(listFooter, sbName,
                        type == QueryRecorder.RecordType.EXPLAIN ? EXPLAIN_NAME_WIDTH : TRACE_NAME_WIDTH);

        String sCost     = step.getEfficiency() >= 0 ? Integer.toString(step.getEfficiency()) : REPORT_NA;
        String sSizeIn   = step.getPreFilterKeySetSize() >= 0 ? Integer.toString(step.getPreFilterKeySetSize()) : REPORT_NA;
        String sSizeOut  = step.getPostFilterKeySetSize() >= 0 ? Integer.toString(step.getPostFilterKeySetSize()) : REPORT_NA;
        String sDuration = step.getDuration() >= 0 ? Long.toString(step.getDuration()) : REPORT_NA;

        StringBuilder sbIndex = new StringBuilder();
        for (QueryRecord.PartialResult.IndexLookupRecord record : step.getIndexLookupRecords())
            {
            int nIndex = listIndexLookups.indexOf(record);
            if (nIndex == -1)
                {
                nIndex = listIndexLookups.size();

                listIndexLookups.add(record);
                }
            sbIndex.append(sbIndex.length() > 0 ? "," : "" + nIndex);
            }

        StringBuilder sbStep = new StringBuilder();
        if (type == QueryRecorder.RecordType.TRACE)
            {
            int nEff = step.getPreFilterKeySetSize() == 0 ? 0 :
                    (step.getPreFilterKeySetSize() - step.getPostFilterKeySetSize()) * 100 / step.getPreFilterKeySetSize();

            String sEff = sSizeIn + "|" + sSizeOut +  "(" + nEff + "%)";

            sbStep.append(String.format(TRACE_STEP_FORMAT,
                    sbName,
                    sbIndex.length() > 0 ? sbIndex : REPORT_NA,
                    sEff, sDuration));
            }
        else
            {
            sbStep.append(String.format(EXPLAIN_STEP_FORMAT,
                    sbName,
                    sbIndex.length() > 0 ? sbIndex : REPORT_NA,
                    sCost));
            }

        for (QueryRecord.PartialResult.Step stepChild : step.getSteps())
            {
            sbStep.append(String.format("%n")).append(reportStep(stepChild,
                    type, listIndexLookups, nLevel + 1, listFooter));
            }

        return sbStep.toString();
        }

    /**
     * Check if the given name will be truncated in the table, and if yes,
     * add the full name to the footer table.
     *
     * @param listFooter    the list containing complete names
     * @param sbName        the name to check
     * @param nColumnWidth  the width of the table column, where the name must fit
     */
    protected static void checkTruncation(List<String> listFooter, StringBuilder sbName, int nColumnWidth)
        {
        if (sbName.length() > nColumnWidth)
            {
            // remove step indentation
            String sFLine = sbName.toString().replaceAll("^\\s+", "");
            int nInd = listFooter.indexOf(sFLine);
            if (nInd == -1)
                {
                nInd = listFooter.size();
                listFooter.add(sFLine);
                }
            // truncate and add footnote pointer
            String sRef = "... (" + nInd + ")";
            sbName.replace(nColumnWidth - sRef.length(), nColumnWidth, sRef);
            }
        }

    /**
     * Report the given index lookup record with the given id.
     *
     * @param nIndexLookupId  the index lookup id
     * @param record          the index lookup record
     * @param listFooter      the list containing complete names, which had to be truncated
     *
     * @return a report line for the given index lookup
     */
    protected static String reportIndexLookupRecord(int nIndexLookupId,
        QueryRecord.PartialResult.IndexLookupRecord record, List<String> listFooter)
        {
        String        sDesc       = record.getIndexDescription();
        StringBuilder sbIndexDesc = new StringBuilder(sDesc == null ? NO_INDEX : sDesc);
        String        sExtr       = record.getExtractorDescription();
        StringBuilder sbIndexExtr = new StringBuilder(sExtr == null ? REPORT_NA : sExtr);

        checkTruncation(listFooter, sbIndexDesc, INDEX_DESCR_WIDTH);
        checkTruncation(listFooter, sbIndexExtr, INDEX_EXTRACTOR_WIDTH);

        return String.format(INDEX_LOOKUP_FORMAT,
                Integer.toString(nIndexLookupId),
                sbIndexDesc.toString(),
                sbIndexExtr.toString(),
                record.isOrdered());
        }

    /**
     * Print the full name that corresponds to the given footer number.
     *
     * @param n      the number used in the main report as a footer pointer
     * @param sItem  the footer item to print
     *
     * @return       a formatted entry in the footer table, containing the full name
     */
    protected static String reportFooterItem(int n, String sItem)
        {
        int           cLen   = sItem.length();
        int           cLines = cLen / FOOTER_LINE_WIDTH;

        StringBuilder sbLine = new StringBuilder(String.format(FOOTER_LEAD_FORMAT,
            Integer.toString(n), sItem.substring(0, Math.min(cLen, FOOTER_LINE_WIDTH))));

        for (int l = 1; l < cLines; l++)
            {
            sbLine.append(String.format(FOOTER_NEXT_LINE_FORMAT,
                sItem.substring(l * FOOTER_LINE_WIDTH, (l + 1) * FOOTER_LINE_WIDTH)));
            }

        if (cLines > 0 && cLen % FOOTER_LINE_WIDTH > 0)
            {
            sbLine.append(String.format(FOOTER_NEXT_LINE_FORMAT,
                sItem.substring(cLines * FOOTER_LINE_WIDTH)));
            }

        return sbLine.append("\n").toString();
        }

    // ----- constants ------------------------------------------------------

    /**
     * Report format.
     */
    private static final int FOOTER_LINE_WIDTH     = 79;
    private static final int EXPLAIN_NAME_WIDTH    = 65;
    private static final int TRACE_NAME_WIDTH      = 41;
    private static final int INDEX_DESCR_WIDTH     = 37;
    private static final int INDEX_EXTRACTOR_WIDTH = 31;

    private static final String DIVIDER =
        "======================================================================================%n";
    private static final String REPORT_NA        = "----";
    private static final String NO_INDEX         = "No index found";
    private static final String PARTITION_FORMAT = "%s%n%n";

    private static final String TRACE_HEADER_FORMAT = "%nTrace%n"
            + "%-" + TRACE_NAME_WIDTH + "." + TRACE_NAME_WIDTH + "s   %-5.5s   %-20.20s   %-10.10s%n";
    private static final String TRACE_STEP_FORMAT =
              "%-" + TRACE_NAME_WIDTH + "." + TRACE_NAME_WIDTH + "s | %-5.5s | %-20.20s | %-10.10s";

    private static final String EXPLAIN_HEADER_FORMAT = "%nExplain Plan%n"
            + "%-" + EXPLAIN_NAME_WIDTH + "." + EXPLAIN_NAME_WIDTH + "s   %-5.5s   %-10.10s%n";
    private static final String EXPLAIN_STEP_FORMAT =
              "%-" + EXPLAIN_NAME_WIDTH + "." + EXPLAIN_NAME_WIDTH + "s | %-5.5s | %-10.10s";

    private static final String INDEX_HEADER_FORMAT = "%nIndex Lookups%n"
            + "%-5.5s %-" + INDEX_DESCR_WIDTH + "." + INDEX_DESCR_WIDTH + "s   %-"
            + INDEX_EXTRACTOR_WIDTH + "." + INDEX_EXTRACTOR_WIDTH + "s  %-7.7s%n";
    private static final String INDEX_LOOKUP_FORMAT =
              "%-4.4s| %-" + INDEX_DESCR_WIDTH + "." + INDEX_DESCR_WIDTH + "s | %-"
            + INDEX_EXTRACTOR_WIDTH + "." + INDEX_EXTRACTOR_WIDTH + "s | %-6.6s%n";

    private static final String FOOTER_HEADER_FORMAT    = "%nComplete filter and index descriptions%n%-1.1s     %s%n";
    private static final String FOOTER_LEAD_FORMAT      = "%-4.4s| %s%n";
    private static final String FOOTER_NEXT_LINE_FORMAT = "    | %s%n";
    }
