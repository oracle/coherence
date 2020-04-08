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

import java.util.List;
import java.util.Map;

import java.util.SortedMap;

/**
 * A class to hold persistence notifications data.<br>
 * Note: this class is not populated via normal JMX queries in {@link VisualVMModel}
 * but is populated via JMX Subscriptions in {@link CoherencePersistencePanel}.
 *
 * @author tam  2015.03.01
 * @since  12.2.1
 */
public class PersistenceNotificationsData
        extends AbstractData
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create PersistenceData passing in the number of columns.
     */
    public PersistenceNotificationsData()
        {
        super(MESSAGE + 1);
        }

    // ----- DataRetriever methods ------------------------------------------

    @Override
    public List<Map.Entry<Object, Data>> getJMXData(RequestSender requestSender, VisualVMModel model)
        {
        return null;
        }

    @Override
    public String getReporterReport()
        {
        return null;
        }

    @Override
    public Data processReporterData(Object[] aoColumns, VisualVMModel model)
        {
        return null;
        }

    @Override
    public SortedMap<Object, Data> getAggregatedDataFromHttpQuerying(VisualVMModel     model,
                                                                     HttpRequestSender requestSender)
            throws Exception
        {
        return null;
        }

    // ----- constants ------------------------------------------------------

    private static final long serialVersionUID = 7766559773272105147L;

    /**
     * Array index for service name.
     */
    public static final int SEQUENCE = 0;

    /**
     * Array index for service name.
     */
    public static final int SERVICE = 1;

    /**
     * Array index for operation.
     */
    public static final int OPERATION = 2;

    /**
     * Array index for start time.
     */
    public static final int START_TIME = 3;

    /**
     * Array index for end time.
     */
    public static final int END_TIME = 4;

    /**
     * Array index for duration.
     */
    public static final int DURATION = 5;

    /**
     * Array index for message.
     */
    public static final int MESSAGE = 6;
    }