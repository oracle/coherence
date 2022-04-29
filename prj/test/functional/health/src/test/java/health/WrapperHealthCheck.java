/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package health;

import com.tangosol.util.HealthCheck;

public class WrapperHealthCheck
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
        return s_delegate == null || s_delegate.isReady();
        }

    @Override
    public boolean isLive()
        {
        return s_delegate == null || s_delegate.isLive();
        }

    @Override
    public boolean isStarted()
        {
        return s_delegate == null || s_delegate.isStarted();
        }

    @Override
    public boolean isSafe()
        {
        return s_delegate == null || s_delegate.isSafe();
        }

    // ----- accessor methods -----------------------------------------------

    public static HealthCheck getDelegate()
        {
        return s_delegate;
        }

    public static void setDelegate(HealthCheck delegate)
        {
        s_delegate = delegate;
        }

    // ----- constants ------------------------------------------------------

    public static final String NAME = "Custom";

    // ----- data members ---------------------------------------------------

    private static volatile HealthCheck s_delegate;
    }
