/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.management;

/**
 * Policy for DiagnosticCommand operations exposed by Coherence management
 * surfaces.
 *
 * @author Aleks Seovic  2026.07.15
 * @since 26.04
 */
public final class DiagnosticCommandPolicy
    {
    /**
     * Return whether an operation is part of the documented management REST
     * JFR command set.
     *
     * @param sOperation  the DiagnosticCommand operation name
     *
     * @return {@code true} if the REST management route may invoke the operation
     */
    public static boolean isRestOperationAllowed(String sOperation)
        {
        if (sOperation == null)
            {
            return false;
            }

        switch (sOperation)
            {
            case "jfrStart":
            case "jfrStop":
            case "jfrDump":
            case "jfrCheck":
                return true;
            default:
                return false;
            }
        }

    /**
     * Return whether an operation is allowed through a wrapped JMX
     * DiagnosticCommand MBean.
     * <p>
     * Wrapped JMX retains {@code vmUnlockCommercialFeatures} for supported
     * pre-JDK-11 Oracle JFR workflows. The management REST route intentionally
     * does not expose that compatibility operation.
     *
     * @param sOperation  the DiagnosticCommand operation name
     *
     * @return {@code true} if wrapped JMX may invoke the operation
     */
    public static boolean isWrappedJmxOperationAllowed(String sOperation)
        {
        return isRestOperationAllowed(sOperation) || UNLOCK_COMMERCIAL_FEATURES.equals(sOperation);
        }

    // ----- constants ------------------------------------------------------

    private static final String UNLOCK_COMMERCIAL_FEATURES = "vmUnlockCommercialFeatures";

    private DiagnosticCommandPolicy()
        {
        }
    }
