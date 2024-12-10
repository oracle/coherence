/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.health;

import com.tangosol.net.management.Registry;
import com.tangosol.util.HealthCheck;

import java.util.Objects;

/**
 * A {@link HealthCheckWrapperMBean} that wraps a {@link HealthCheck} instance.
 *
 * @author Jonathan Knight 2022.04.02
 * @since 22.06
 */
public class HealthCheckWrapper
        implements HealthCheckWrapperMBean
    {
    public HealthCheckWrapper(HealthCheck healthCheck)
        {
        this(healthCheck, SUBTYPE_APPLICATION);
        }

    public HealthCheckWrapper(HealthCheck healthCheck, String sSybType)
        {
        f_delegate = Objects.requireNonNull(healthCheck);
        f_sSubType = sSybType == null || sSybType.isEmpty()
                ? SUBTYPE_APPLICATION
                : sSybType;
        }

    public HealthCheck getHealthCheck()
        {
        return f_delegate;
        }

    @Override
    public String getSubType()
        {
        return f_sSubType;
        }

    @Override
    public String getClassName()
        {
        return f_delegate.getClass().getName();
        }

    @Override
    public String getDescription()
        {
        return f_delegate.toString();
        }

    @Override
    public String getName()
        {
        return f_delegate.getName();
        }

    @Override
    public boolean isMemberHealthCheck()
        {
        return f_delegate.isMemberHealthCheck();
        }

    @Override
    public boolean isReady()
        {
        return f_delegate.isReady();
        }

    @Override
    public boolean isLive()
        {
        return f_delegate.isLive();
        }

    @Override
    public boolean isStarted()
        {
        return f_delegate.isStarted();
        }

    @Override
    public boolean isSafe()
        {
        return f_delegate.isSafe();
        }

    // ----- helper methods -------------------------------------------------

    public static String getMBeanName(HealthCheck healthCheck)
        {
        String sSubType = healthCheck instanceof HealthCheckWrapperMBean
                ? ((HealthCheckWrapperMBean) healthCheck).getSubType()
                : SUBTYPE_APPLICATION;

        if (SUBTYPE_CLUSTER.equals(sSubType))
            {
            return String.format(MBEAN_CLUSTER_PATTERN, sSubType);
            }
        return String.format(MBEAN_NAME_PATTERN, sSubType, healthCheck.getName());
        }

    // ----- data members ---------------------------------------------------

    /**
     * The {@link HealthCheck} that this MBean delegates to.
     */
    private final HealthCheck f_delegate;

    /**
     * The MBean object name subtype.
     */
    private final String f_sSubType;
    }
