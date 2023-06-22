/*
 * Copyright (c) 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package management;

import org.junit.BeforeClass;

/**
 * Run the ManagementInfoResourceTests tests using read-only management.
 */
public class ReadOnlyManagementInfoResourceTests
        extends ManagementInfoResourceTests
    {
    // ----- junit lifecycle methods ----------------------------------------

    /**
     * Initialize the test class.
     * <p>
     * This method starts the Coherence cluster, if it isn't already running.
     */
    @BeforeClass
    public static void _startup()
        {
        setReadOnly(true);
        ManagementInfoResourceTests._startup();
        }
    }
