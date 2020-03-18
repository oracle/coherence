/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.sun.tools.visualvm.modules.coherence.tablemodel.model;

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
 * A class to hold machine data.
 *
 * @author tam  2013.11.14
 * @since  12.1.3
 */
public class MachineData
        extends AbstractData
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create MachineData passing in the number of columns.
     */
    public MachineData()
        {
        super(PERCENT_FREE_MEMORY + 1);
        }

    // ----- DataRetriever methods ------------------------------------------

    /**
     * {@inheritDoc}
     */
    public List<Map.Entry<Object, Data>> getJMXData(RequestSender requestSender, VisualVMModel model)
        {
        SortedMap<Object, Data> mapData = new TreeMap<Object, Data>();
        Data                    data;
        double                  loadAverage;

        // get the list of machines, along with a member id on that machine
        SortedMap<Pair<String, Integer>, Data> initialMachineMap = model.getInitialMachineMap();

        try
            {
            // loop through each of the entries and get the data for a given node only
            // as we know that nodes on the same machine will have the same results
            Iterator<Map.Entry<Pair<String, Integer>, Data>> iter = initialMachineMap.entrySet().iterator();

            while (iter.hasNext())
                {
                Map.Entry<Pair<String, Integer>, Data> entry = iter.next();

                String sQuery = "Coherence:type=Platform,Domain=java.lang,subType=OperatingSystem,nodeId="
                                + entry.getKey().getY() + ",*";

                Set<ObjectName> resultSet = requestSender.getClusterMemberOS(entry.getKey().getY());

                for (Iterator<ObjectName> iterResults = resultSet.iterator(); iterResults.hasNext(); )
                    {
                    ObjectName objectName = iterResults.next();

                    // IBM JVM on AIX for some reason has different attribute name TotalPhysicalMemory
                    // in java.lang:type=OperatingSystem than other JVM's which use
                    // TotalPhysicalMemorySize. (see Bug 22366612)
                    // we cannot use System.getProperty("os.name") as this will only get the O/S the
                    // JVisualvm plug-in is running on, not the target JVM O/S.

                    data = new MachineData();

                    AttributeList listAttr = requestSender.getAttributes(objectName,
                            new String[] { "Name", ATTR_FREE_MEM, ATTR_LOAD_AVG, ATTR_AVAIL_PROC, ATTR_TOTAL_MEM_AIX, ATTR_TOTAL_MEM });

                    String sOSType     = getAttributeValueAsString(listAttr, "Name");
                    String sMemoryAttr = sOSType.toLowerCase().indexOf("aix") >= 0 ? ATTR_TOTAL_MEM_AIX : ATTR_TOTAL_MEM;

                    data.setColumn(MachineData.MACHINE_NAME, entry.getKey().getX());
                    data.setColumn(MachineData.FREE_PHYSICAL_MEMORY, Long.parseLong(getAttributeValueAsString(listAttr, ATTR_FREE_MEM)));

                    loadAverage = Double.parseDouble(getAttributeValueAsString(listAttr, ATTR_LOAD_AVG));
                    if (loadAverage == -1)
                        {
                        model.setLoadAverageAvailable(false);
                        }
                    if (!model.isLoadAverageAvailable())
                        {
                        loadAverage = Double.parseDouble(requestSender.getAttribute(objectName, "SystemCpuLoad"));
                        }

                    data.setColumn(MachineData.SYSTEM_LOAD_AVERAGE, Double.valueOf(loadAverage));
                    data.setColumn(MachineData.PROCESSOR_COUNT, Integer.parseInt(getAttributeValueAsString(listAttr, ATTR_AVAIL_PROC)));
                    data.setColumn(MachineData.TOTAL_PHYSICAL_MEMORY, Long.parseLong(getAttributeValueAsString(listAttr, sMemoryAttr)));

                    data.setColumn(MachineData.PERCENT_FREE_MEMORY,
                                   ((Long) data.getColumn(MachineData.FREE_PHYSICAL_MEMORY) * 1.0f)
                                   / (Long) data.getColumn(MachineData.TOTAL_PHYSICAL_MEMORY));

                    // put it into the mapData with just a machine as the key
                    mapData.put(entry.getKey().getX(), data);

                    }
                }

            return new ArrayList<Map.Entry<Object, Data>>(mapData.entrySet());

            }
        catch (Exception e)
            {
            LOGGER.log(Level.WARNING, "Error getting machine statistics", e);

            return null;
            }
        }

    /**
     * {@inheritDoc}
     */
    public String getReporterReport()
        {
        return null;    // see comment below
        }

    /**
     * {@inheritDoc}
     */
    public Data processReporterData(Object[] aoColumns, VisualVMModel model)
        {
        // difficult to implement with reporter
        return null;
        }

    @Override
    public SortedMap<Object, Data> getAggregatedDataFromHttpQuerying(VisualVMModel model, HttpRequestSender requestSender) throws Exception
        {
        // no reports being used, hence using default functionality provided in getJMXData
        return null;
        }

    // ----- constants ------------------------------------------------------

    private static final long serialVersionUID = -4146745462482520312L;

    /**
     * Array index for machine name.
     */
    public static final int MACHINE_NAME = 0;

    /**
     * Array index for processor count.
     */
    public static final int PROCESSOR_COUNT = 1;

    /**
     * Array index for system load average.
     */
    public static final int SYSTEM_LOAD_AVERAGE = 2;

    /**
     * Array index for total physical memory.
     */
    public static final int TOTAL_PHYSICAL_MEMORY = 3;

    /**
     * Array index for free physical memory.
     */
    public static final int FREE_PHYSICAL_MEMORY = 4;

    /**
     * Array index for percent free memory
     */
    public static final int PERCENT_FREE_MEMORY = 5;

    /**
     * The logger object to use.
     */
    private static final Logger LOGGER = Logger.getLogger(MachineData.class.getName());

    /**
     * JMX attribute name for Free Physical Memory Size.
     */
    protected static final String ATTR_FREE_MEM = "FreePhysicalMemorySize";

    /**
     * JMX attribute name for System Load Average.
     */
    protected static final String ATTR_LOAD_AVG = "SystemLoadAverage";

    /**
     * JMX attribute name for Available Processors.
     */
    protected static final String ATTR_AVAIL_PROC = "AvailableProcessors";

    /**
     * JMX attribute name for Total Physical Memory Size.
     */
    protected static final String ATTR_TOTAL_MEM = "TotalPhysicalMemorySize";

    /**
     * JMX attribute name for Total Physical Memory Size for IBM JDK on AIX.
     */
    protected static final String ATTR_TOTAL_MEM_AIX = "TotalPhysicalMemory";
    }
