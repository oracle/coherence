/*
 * Copyright (c) 2022 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.guides.queries;

import com.oracle.coherence.guides.queries.utils.CoherenceHelper;
import com.tangosol.coherence.dslquery.QueryPlus;

/**
 * A simple application that starts the CohQL Console and populates a Coherence cache with demo data.
 *
 * @author Gunnar Hillert  2022.02.25
 */
class StartCohQLConsole {
    public static void  main(String[] args) {
        CoherenceHelper.startCoherence();
        QueryPlus.main(args);
    }
}
