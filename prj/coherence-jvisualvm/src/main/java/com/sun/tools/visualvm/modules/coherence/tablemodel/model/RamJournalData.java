/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.sun.tools.visualvm.modules.coherence.tablemodel.model;

import com.sun.tools.visualvm.modules.coherence.VisualVMModel;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

/**
 * A class to hold basic RamJournal data.
 *
 * @author tam  2014.04.11
 * @since  12.1.3
 *
 */
public class RamJournalData
        extends AbstractElasticData
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create RamJournalData passing in the number of columns.
     */
    public RamJournalData()
        {
        super();
        }

    // ----- AbstractElasticData methods ------------------------------------------

    /**
     * {@inheritDoc}
     */
    protected Data getDataObject()
        {
        return new RamJournalData();
        }

    /**
     * {@inheritDoc}
     */
    protected String getJMXQueryPrefix()
        {
        return "RamJournalRM";
        }

    @Override
    protected String getElasticDataType()
        {
        return "ram";
        }
    }
