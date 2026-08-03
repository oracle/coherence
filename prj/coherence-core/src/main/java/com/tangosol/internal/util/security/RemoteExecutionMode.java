/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util.security;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.coherence.config.Config;

import com.tangosol.internal.util.CoherenceMode;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Central resolver for the dynamic-remote-code gate. Used by
 * LambdaBytecodeGate (wire-arriving DYNAMIC lambda bytecode) and, under
 * CACHE-01, by MethodInvocationProcessor and ScriptProcessor mode gates.
 * Long-term property; mode-dependent default (dev=allow, prod=deny).
 *
 * @author Aleks Seovic  2026.05.01
 * @since 26.07
 */
public final class RemoteExecutionMode
    {
    /**
     * Runtime policy for unauthenticated dynamic remote payloads.
     */
    public static final String PROP_DYNAMIC_REMOTE_UNAUTH = "coherence.remote.dynamic.unauthenticated";

    /**
     * Return {@code true} if unauthenticated dynamic remote payloads are
     * allowed.
     *
     * @return {@code true} if unauthenticated dynamic remote payloads are
     *         allowed
     */
    public static boolean isDynamicRemoteAllowed()
        {
        Boolean flag = S_DYNAMIC_REMOTE_ALLOWED.get();
        if (flag == null)
            {
            flag = resolveDynamicRemoteAllowed();
            if (!S_DYNAMIC_REMOTE_ALLOWED.compareAndSet(null, flag))
                {
                flag = S_DYNAMIC_REMOTE_ALLOWED.get();
                }
            }
        return flag;
        }

    /**
     * Reset cached policy for tests.
     */
    public static void resetForTesting()
        {
        S_DYNAMIC_REMOTE_ALLOWED.set(null);
        }

    private static boolean resolveDynamicRemoteAllowed()
        {
        String sValue = Config.getProperty(PROP_DYNAMIC_REMOTE_UNAUTH);
        if (sValue != null && sValue.isBlank())
            {
            sValue = null;
            }
        if (sValue == null)
            {
            return !CoherenceMode.isDynamicRemoteDefaultDeny();
            }

        switch (sValue.trim().toLowerCase(Locale.ROOT))
            {
            case "allow":
                return true;
            case "deny":
                return false;
            default:
                Logger.err("Invalid " + PROP_DYNAMIC_REMOTE_UNAUTH
                        + " value '" + sValue + "'; defaulting to "
                        + (CoherenceMode.isDynamicRemoteDefaultDeny() ? "deny" : "allow")
                        + ". Expected 'allow' or 'deny'.");
                return !CoherenceMode.isDynamicRemoteDefaultDeny();
            }
        }

    private RemoteExecutionMode()
        {
        }

    private static final AtomicReference<Boolean> S_DYNAMIC_REMOTE_ALLOWED = new AtomicReference<>();
    }
