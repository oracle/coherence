/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.sun.tools.visualvm.modules.coherence.tablemodel;

import com.sun.tools.visualvm.modules.coherence.tablemodel.model.Data;

/**
 * A model for holding JCache configuration data.<p>
 * Note: This model gets populated by the JCachePanel and is not
 * directly related to the JCacheConfigurationData object.
 *
 * @author tam  2014.09.22
 * @since  12.2.1
 */
public class JCacheConfigurationTableModel
        extends AbstractCoherenceTableModel<Object, Data>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Creates a table model with the given columns.
     *
     * @param asColumns the columns for this table model
     */
    public JCacheConfigurationTableModel(String[] asColumns)
        {
        super(asColumns);
        }

    // ----- constants ------------------------------------------------------

    private static final long serialVersionUID = 8895857232631589048L;
    }
