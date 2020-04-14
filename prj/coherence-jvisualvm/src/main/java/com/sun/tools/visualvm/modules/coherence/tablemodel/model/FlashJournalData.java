/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.sun.tools.visualvm.modules.coherence.tablemodel.model;

/**
 * A class to hold basic FlashJournal data.
 *
 * @author tam  2014.04.11
 * @since  12.1.3
 *
 */
public class FlashJournalData
        extends AbstractElasticData
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create FlashJournalData passing in the number of columns.
     */
    public FlashJournalData()
        {
        super();
        }

    // ----- AbstractElasticData methods ------------------------------------------

    /**
     * {@inheritDoc}
     */
    protected Data getDataObject()
        {
        return new FlashJournalData();
        }

    /**
     * {@inheritDoc}
     */
    protected String getJMXQueryPrefix()
        {
        return "FlashJournalRM";
        }

    @Override
    protected String getElasticDataType()
        {
        return "flash";
        }

    }
