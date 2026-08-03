/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util.security;

import com.tangosol.io.SerializationRole;
import com.tangosol.io.internal.SerializationTelemetry;

import com.tangosol.net.security.SecurityHelper;

import com.tangosol.util.OperationReason;
import com.tangosol.util.RemoteExecutablePolicy;

import javax.security.auth.Subject;

/**
 * Shared dynamic-remote mode gate for built-in script-backed cache executables.
 *
 * @author Aleks Seovic  2026.05.13
 * @since 26.07
 */
public final class RemoteScriptGate
    {
    /**
     * Enforce script evaluation policy before a built-in script executable
     * reaches {@code ScriptManager.execute(...)} or delegate creation.
     *
     * @param clz        the script executable class
     * @param sLanguage  the script language
     * @param sName      the script binding name
     */
    public static void enforceScriptEvaluation(Class<?> clz, String sLanguage, String sName)
        {
        SerializationRole role    = SerializationRole.current();
        Subject           subject = SecurityHelper.getCurrentSubject();

        enforceScriptEvaluation(clz, sLanguage, sName, role, subject);
        }

    /**
     * Enforce script evaluation policy for a known remote installation role.
     *
     * @param clz        the script executable class
     * @param sLanguage  the script language
     * @param sName      the script binding name
     * @param role       the serialization role
     * @param subject    the current subject, or {@code null}
     */
    public static void enforceScriptEvaluation(Class<?> clz, String sLanguage, String sName, SerializationRole role,
                                               Subject subject)
        {
        RemoteExecutablePolicy.current().enforce(clz, OperationReason.SCRIPT_EVAL, role, subject);
        if (!RemoteExecutionMode.isDynamicRemoteAllowed())
            {
            SerializationTelemetry.recordExecutablePolicyCheck("rejected", clz, OperationReason.SCRIPT_EVAL,
                    role, subject, SerializationTelemetry.SUB_REASON_MODE_GATE);
            SerializationTelemetry.logRejection(TOPIC_CACHE_SCRIPT, role.name(), subject,
                    sLanguage + ':' + sName, REASON_DENIED_BY_MODE);
            throw new SecurityException(REASON_DENIED_BY_MODE);
            }
        }

    /**
     * Mode-gate rejection reason.
     */
    public static final String REASON_DENIED_BY_MODE = "script-eval-denied-by-mode";

    private static final String TOPIC_CACHE_SCRIPT = "cache.script";

    private RemoteScriptGate()
        {
        }
    }
