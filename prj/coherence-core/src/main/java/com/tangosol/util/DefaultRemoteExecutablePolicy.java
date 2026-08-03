/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util;

import com.tangosol.internal.util.CoherenceMode;

import com.tangosol.io.SerializationRole;
import com.tangosol.io.internal.SerializationTelemetry;

import javax.security.auth.Subject;

/**
 * Default remote executable policy.
 *
 * @author Aleks Seovic  2026.05.01
 * @since 26.07
 */
final class DefaultRemoteExecutablePolicy
        extends RemoteExecutablePolicy
    {
    @Override
    public void enforce(Class<?> clz, OperationReason reason, SerializationRole role, Subject subject)
        {
        if (!isExecutable(clz))
            {
            if (!CoherenceMode.isRemoteExecutableEnforced())
                {
                SerializationTelemetry.recordExecutablePolicyCheck("would_reject", clz, reason, role, subject);
                return;
                }

            SerializationTelemetry.recordExecutablePolicyCheck("rejected", clz, reason, role, subject);
            String sName = clz == null ? "null" : clz.getName();
            throw new SecurityException("Remote execution denied for class " + sName
                    + " (reason=" + reason
                    + ", role=" + role
                    + "); class is not annotated with @Remote.Executable and is not listed as "
                    + "executable=\"true\" in any merged security-config.xml");
            }
        }
    }
