/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package health;

import com.tangosol.util.HealthCheck;

public class DiscoveredHealthCheck
        implements HealthCheck
    {
    // ----- HealthCheck methods --------------------------------------------

    @Override
    public String getName()
        {
        return NAME;
        }

    @Override
    public boolean isReady()
        {
        return true;
        }

    @Override
    public boolean isLive()
        {
        return true;
        }

    @Override
    public boolean isStarted()
        {
        return true;
        }

    @Override
    public boolean isSafe()
        {
        return true;
        }

    // ----- constants ------------------------------------------------------

    public static final String NAME = "Discovered";
    }
