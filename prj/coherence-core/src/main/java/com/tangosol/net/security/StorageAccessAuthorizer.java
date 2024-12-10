/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.security;

import com.tangosol.net.BackingMapContext;

import com.tangosol.util.BinaryEntry;

import javax.security.auth.Subject;

/**
 * A pluggable facility for the server side access control authorization.
 */
public interface StorageAccessAuthorizer
    {
    /**
     * Check if the caller represented by the specified Subject is authorized
     * to a read access for the specified entry.
     *
     * @param entry    the entry
     * @param subject  the Subject
     * @param nReason  one of the REASON_* constants
     *
     * @throws SecurityException if the action is not authorized
     */
    public void checkRead(BinaryEntry entry, Subject subject, int nReason);

    /**
     * Check if the caller represented by the specified Subject is authorized
     * to a write access for the specified entry.
     *
     * @param entry    the entry
     * @param subject  the Subject
     * @param nReason  one of the REASON_* constants
     *
     * @throws SecurityException if the action is not authorized
     */
    public void checkWrite(BinaryEntry entry, Subject subject, int nReason);

    /**
     * Check if the caller represented by the specified Subject is authorized
     * to read any data.
     * <p>
     * For example, this check would be performed to install a {@link
     * com.tangosol.util.MapListener map listener} (except for lite listeners)
     *
     * @param context  the BackingMapContext
     * @param subject  the Subject
     * @param nReason  one of the REASON_* constants
     *
     * @throws SecurityException if the action is not authorized
     */
    public void checkReadAny(BackingMapContext context, Subject subject, int nReason);

    /**
     * Check if the caller represented by the specified Subject is authorized
     * to update any data.
     * <p>
     * For example, this check would be performed to install a trigger.
     *
     * @param context  the BackingMapContext
     * @param subject  the Subject
     * @param nReason  one of the REASON_* constants
     *
     * @throws SecurityException if the action is not authorized
     */
    public void checkWriteAny(BackingMapContext context, Subject subject, int nReason);

    /**
     * Return a human-readable description for the specified REASON_ constant.
     *
     * @param nReason  one of the REASON_ constants
     *
     * @return the operation description
     */
    public static String reasonToString(int nReason)
        {
        switch (nReason)
            {
            case REASON_GET:
                return "get";
            case REASON_PUT:
                return "put";
            case REASON_REMOVE:
                return "remove";
            case REASON_KEYSET:
               return "keySet";
            case REASON_ENTRYSET:
                return "entrySet";
            case REASON_VALUES:
                return "values";
            case REASON_INVOKE:
                return "invoke";
            case REASON_AGGREGATE:
                return "aggregate";
            case REASON_INDEX_ADD:
                return "addIndex";
            case REASON_LISTENER_ADD:
                return "addListener";
            case REASON_LISTENER_REMOVE:
                return "removeListener";
            case REASON_INDEX_REMOVE:
                return "removeIndex";
            case REASON_TRIGGER_ADD:
                return "addTrigger";
            case REASON_TRIGGER_REMOVE:
                return "removeTrigger";
            case REASON_INTERCEPTOR_ADD:
                return "addInterceptor";
            case REASON_INTERCEPTOR_REMOVE:
                return "removeInterceptor";
            case REASON_CLEAR:
                return "clear";
            default:
                return "<unknown>";
            }
        }

    // ----- constants -------------------------------------------------------

    public static final int REASON_UNKNOWN = 0;
    public static final int REASON_GET = 1;
    public static final int REASON_PUT = 2;
    public static final int REASON_REMOVE = 3;
    public static final int REASON_KEYSET = 4;
    public static final int REASON_ENTRYSET = 5;
    public static final int REASON_VALUES = 6;
    public static final int REASON_CLEAR = 7;
    public static final int REASON_INVOKE = 8;
    public static final int REASON_AGGREGATE = 9;
    public static final int REASON_INDEX_ADD = 10;
    public static final int REASON_INDEX_REMOVE = 11;
    public static final int REASON_LISTENER_ADD = 12;
    public static final int REASON_LISTENER_REMOVE = 13;
    public static final int REASON_TRIGGER_ADD = 14;
    public static final int REASON_TRIGGER_REMOVE = 15;
    public static final int REASON_INTERCEPTOR_ADD = 16;
    public static final int REASON_INTERCEPTOR_REMOVE = 17;
    }
