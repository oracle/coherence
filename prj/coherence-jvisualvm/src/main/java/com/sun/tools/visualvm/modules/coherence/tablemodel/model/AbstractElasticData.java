/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.sun.tools.visualvm.modules.coherence.tablemodel.model;

import com.fasterxml.jackson.databind.JsonNode;

import com.sun.tools.visualvm.modules.coherence.VisualVMModel;

import com.sun.tools.visualvm.modules.coherence.helper.HttpRequestSender;
import com.sun.tools.visualvm.modules.coherence.helper.RequestSender;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.AttributeList;
import javax.management.ObjectName;

import static com.sun.tools.visualvm.modules.coherence.helper.JMXUtils.getAttributeValueAsString;

/**
 * Abstract implementation that can represent either
 * {@link FlashJournalData} or {@link RamJournalData}.
 *
 * @author tam  2014.04.26
 * @since  12.1.3
 */
public abstract class AbstractElasticData
        extends AbstractData
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create new instance with column count.
     */
    public AbstractElasticData()
        {
        super(CURRENT_COLLECTION_LOAD_FACTOR + 1);
        }

    // ----- AbstractElasticData methods ------------------------------------

    /**
     * Returns the data implementation object.
     *
     * @return the data implementation object.
     */
    protected abstract Data getDataObject();

    /**
     * Returns the prefix to use for querying JMX.
     *
     * @return the prefix to use for querying JMX
     */
    protected abstract String getJMXQueryPrefix();

    /**
     * Returns the prefix to use for querying JMX.
     *
     * @return the prefix to use for querying JMX
     */
    protected abstract String getElasticDataType();

    // ----- DataRetriever methods ------------------------------------------

    /**
     * {@inheritDoc}
     */
    public List<Map.Entry<Object, Data>> getJMXData(RequestSender sender, VisualVMModel model)
        {
        SortedMap<Object, Data> mapData = new TreeMap<Object, Data>();
        Data                    data;

        // The following rules for compaction count
        // 3.7.0 did not include any compaction information
        // 3.7.1 introduced TotalCompactionCount
        // All other versions have CompactionCount and ExhaustiveCompactionCount
        String sCompactionCount = model.getClusterVersion().contains("3.7.0")
                                  ? null
                                  : (model.getClusterVersion().contains("3.7")
                                     ? "TotalCompactionCount" : "CompactionCount");

        try
            {
            Set<ObjectName> setNodeData = sender.getAllJournalMembers(getJMXQueryPrefix());

            for (Iterator<ObjectName> nodIter = setNodeData.iterator(); nodIter.hasNext(); )
                {
                ObjectName nodeNameObjName = nodIter.next();
                Integer    nodeId          = Integer.valueOf(nodeNameObjName.getKeyProperty("nodeId"));

                data = getDataObject();

                AttributeList listAttr = sender.getAttributes(nodeNameObjName,
                    new String[] { ATTR_MAX_JOURNAL_FILES, ATTR_FILE_COUNT, ATTR_MAX_FILE_SIZE,
                                   ATTR_TOTAL_DATA_SIZE });

                int  nMaxJournalFilesNumber = Integer.parseInt(getAttributeValueAsString(listAttr, ATTR_MAX_JOURNAL_FILES));
                int  nFileCount             = Integer.parseInt(getAttributeValueAsString(listAttr, ATTR_FILE_COUNT));
                long nMaxFileSize           = Long.parseLong(getAttributeValueAsString(listAttr, ATTR_MAX_FILE_SIZE));

                data.setColumn(NODE_ID, nodeId);
                data.setColumn(FILE_COUNT, Integer.valueOf(nFileCount));
                data.setColumn(MAX_FILE_SIZE, Long.valueOf(nMaxFileSize));
                data.setColumn(TOTAL_COMMITTED_BYTES, Long.valueOf(nMaxFileSize * nFileCount));
                data.setColumn(MAX_COMMITTED_BYTES, Long.valueOf(nMaxFileSize * nMaxJournalFilesNumber));
                data.setColumn(TOTAL_DATA_SIZE, Long.parseLong(getAttributeValueAsString(listAttr, ATTR_TOTAL_DATA_SIZE)));

                try
                    {
                    if (sCompactionCount != null)
                        {
                        data.setColumn(COMPACTION_COUNT,
                                       Integer.parseInt(sender.getAttribute(nodeNameObjName, sCompactionCount)));
                        }

                    if ("CompactionCount".equals(sCompactionCount))
                        {
                        data.setColumn(EXHAUSTIVE_COMPACTION_COUNT,
                                Integer.parseInt(sender.getAttribute(nodeNameObjName, "ExhaustiveCompactionCount")));
                        }

                        // only introduced in 3.7.1
                        data.setColumn(CURRENT_COLLECTION_LOAD_FACTOR,
                                Double.parseDouble(sender.getAttribute(nodeNameObjName, "CurrentCollectorLoadFactor")));
                    }
                catch (Exception eIgnore)
                    {
                    // ExhaustiveCompactionCount was not available in 3.7 so lets ignore
                    }

                data.setColumn(MAX_FILES, Integer.valueOf(nMaxJournalFilesNumber));

                mapData.put(nodeId, data);
                }

            return new ArrayList<Map.Entry<Object, Data>>(mapData.entrySet());
            }
        catch (Exception e)
            {
            LOGGER.log(Level.WARNING, "Error getting " + this.getClass().getName() + "  statistics", e);
            e.printStackTrace();

            return null;
            }
        }

    /**
     * {@inheritDoc}
     */
    public Data processReporterData(Object[] aoColumns, VisualVMModel model)
        {
        Data data = getDataObject();

        data.setColumn(NODE_ID, Integer.valueOf(getNumberValue(aoColumns[2].toString())));
        data.setColumn(FILE_COUNT, Integer.valueOf(getNumberValue(aoColumns[3].toString())));
        data.setColumn(MAX_FILES, Integer.valueOf(getNumberValue(aoColumns[4].toString())));
        data.setColumn(MAX_FILE_SIZE, Long.valueOf(getNumberValue(aoColumns[5].toString())));
        data.setColumn(TOTAL_COMMITTED_BYTES, Long.valueOf(getNumberValue(aoColumns[6].toString())));
        data.setColumn(MAX_COMMITTED_BYTES, Long.valueOf(getNumberValue(aoColumns[7].toString())));
        data.setColumn(TOTAL_DATA_SIZE, Long.valueOf(getNumberValue(aoColumns[8].toString())));
        data.setColumn(COMPACTION_COUNT, Integer.valueOf(getNumberValue(aoColumns[9].toString())));
        data.setColumn(EXHAUSTIVE_COMPACTION_COUNT, Integer.valueOf(getNumberValue(aoColumns[10].toString())));
        data.setColumn(CURRENT_COLLECTION_LOAD_FACTOR, Double.valueOf(aoColumns[11].toString()));

        return data;
        }

    @Override
    public SortedMap<Object, Data> getAggregatedDataFromHttpQuerying(VisualVMModel     model,
                                                                     HttpRequestSender requestSender)
            throws Exception
        {
        JsonNode rootNode                = requestSender.getDataForElasticDataMembers(getElasticDataType());
        JsonNode nodeJournalMemberItems  = rootNode.get("items");

        SortedMap<Object, Data> mapData = new TreeMap<Object, Data>();

        if (nodeJournalMemberItems != null && nodeJournalMemberItems.isArray())
            {
            for (int i = 0; i < nodeJournalMemberItems.size(); i++)
                {
                Data data = getDataObject();

                JsonNode nodeJournalMember      = nodeJournalMemberItems.get(i);
                int      nMaxJournalFilesNumber =
                        Integer.parseInt(nodeJournalMember.get("maxJournalFilesNumber").asText());
                long     nMaxFileSize           = Long.parseLong(nodeJournalMember.get("maxFileSize").asText());
                int      nFileCount             = Integer.parseInt(nodeJournalMember.get("fileCount").asText());

                data.setColumn(NODE_ID, Integer.parseInt(nodeJournalMember.get("nodeId").asText()));
                data.setColumn(FILE_COUNT, nFileCount);
                data.setColumn(MAX_FILES, nMaxJournalFilesNumber);
                data.setColumn(MAX_FILE_SIZE, nMaxFileSize);
                data.setColumn(TOTAL_COMMITTED_BYTES,
                        (long) nFileCount * nMaxFileSize);
                data.setColumn(MAX_COMMITTED_BYTES,
                        (long) nMaxJournalFilesNumber * nMaxFileSize);
                data.setColumn(TOTAL_DATA_SIZE, Long.parseLong(nodeJournalMember.get("totalDataSize").asText()));
                data.setColumn(COMPACTION_COUNT, Integer.parseInt(nodeJournalMember.get("compactionCount").asText()));
                data.setColumn(EXHAUSTIVE_COMPACTION_COUNT,
                        Integer.parseInt(nodeJournalMember.get("exhaustiveCompactionCount").asText()));
                data.setColumn(CURRENT_COLLECTION_LOAD_FACTOR,
                        Double.parseDouble(nodeJournalMember.get("currentCollectorLoadFactor").asText()));
                mapData.put(data.getColumn(0), data);
                }
            }
        return mapData;

        }

    /**
     * {@inheritDoc}
     */
    public String getReporterReport()
        {
        return REPORT_ELASTIC_DATA;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public String preProcessReporterXML(VisualVMModel model, String sReporterXML)
        {
        // the report XML contains the following tokens that require substitution:
        // %QUERY_PREFIX%

        return sReporterXML.replaceAll("%QUERY_PREFIX%", getJMXQueryPrefix());
        }

    // ----- constants ------------------------------------------------------

    /**
     * Array index for node id.
     */
    public static final int NODE_ID = 0;

    /**
     * Array index for file count files.
     */
    public static final int FILE_COUNT = 1;

    /**
     * Array index for max files.
     */
    public static final int MAX_FILES = 2;

    /**
     * Array index for max file size.
     */
    public static final int MAX_FILE_SIZE = 3;

    /**
     * Array index for total committed bytes.
     */
    public static final int TOTAL_COMMITTED_BYTES = 4;

    /**
     * Array index for max committed bytes.
     */
    public static final int MAX_COMMITTED_BYTES = 5;

    /**
     * Array index for total data size.
     */
    public static final int TOTAL_DATA_SIZE = 6;

    /**
     * Array index for compaction count.
     */
    public static final int COMPACTION_COUNT = 7;

    /**
     * Array index for exhaustive compaction count.
     */
    public static final int EXHAUSTIVE_COMPACTION_COUNT = 8;

    /**
     * Array index for exhaustive compaction count.
     */
    public static final int CURRENT_COLLECTION_LOAD_FACTOR = 9;

    /**
     * The logger object to use.
     */
    private static final Logger LOGGER = Logger.getLogger(AbstractElasticData.class.getName());

    /**
     * Report for service data.
     */
    public static final String REPORT_ELASTIC_DATA = "reports/visualvm/elasticdata-detail-stats.xml";

    /**
     * JMX attribute name for Max Journal Files Number.
     */
    protected static final String ATTR_MAX_JOURNAL_FILES = "MaxJournalFilesNumber";

    /**
     * JMX attribute name for File Count.
     */
    protected static final String ATTR_FILE_COUNT = "FileCount";

    /**
     * JMX attribute name for Max File Size.
     */
    protected static final String ATTR_MAX_FILE_SIZE = "MaxFileSize";

    /**
     * JMX attribute name for Total Data Size.
     */
    protected static final String ATTR_TOTAL_DATA_SIZE = "TotalDataSize";
    }
