/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.security;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.net.BackingMapContext;

import com.tangosol.util.BinaryEntry;

import javax.security.auth.Subject;

/**
 * Simple StorageAccessAuthorizer implementation that logs the authorization
 * requests and allows operations to proceed.
 *
 * @author gg 2014.09.25
 */
public class AuditingAuthorizer
        implements StorageAccessAuthorizer
    {
    /**
     * Construct a non-strict AuditingAuthorizer. It will simply log the
     * authorization request and allow the operation to proceed.
     */
    public AuditingAuthorizer()
        {
        this(false);
        }

    /**
     * Construct an AuditingAuthorizer. It will simply log the
     * authorization request and allow the operation to proceed based on the
     * presence of the Subject.
     *
     * @param fStrict if true, a non-null Subject must be presented for the
     *                operation to proceed
     */
    public AuditingAuthorizer(boolean fStrict)
        {
        f_fStrict = fStrict;
        }


    // ----- StorageAccessAuthorizer interface -------------------------------

    @Override
    public void checkRead(BinaryEntry entry, Subject subject, int nReason)
        {
        logEntryRequest(entry, subject, false, nReason);

        if (subject == null && f_fStrict)
            {
            throw new SecurityException("subject is not provided");
            }
        }

    @Override
    public void checkWrite(BinaryEntry entry, Subject subject, int nReason)
        {
        logEntryRequest(entry, subject, true, nReason);

        if (subject == null && f_fStrict)
            {
            throw new SecurityException("subject is not provided");
            }
        }

    @Override
    public void checkReadAny(BackingMapContext context, Subject subject, int nReason)
        {
        logMapRequest(context, subject, false, nReason);

        if (subject == null && f_fStrict)
            {
            throw new SecurityException("subject is not provided");
            }
        }

    @Override
    public void checkWriteAny(BackingMapContext context, Subject subject, int nReason)
        {
        logMapRequest(context, subject, true, nReason);

        if (subject == null && f_fStrict)
            {
            throw new SecurityException("subject is not provided");
            }
        }

    // ----- helper methods --------------------------------------------------

    /**
     * Log the entry level authorization request.
     *
     * @param entry    the entry to authorize access to
     * @param subject  the Subject
     * @param fWrite   true for write operation; read otherwise
     * @param nReason  the reason for the check
     */
    protected void logEntryRequest(BinaryEntry entry, Subject subject, boolean fWrite, int nReason)
        {
        Logger.info('"' + (fWrite ? "Write" : "Read") + "\" request for key=\""
            + entry.getKey()
            + (subject == null ?
                "\" from unidentified user" :
                "\" on behalf of " + subject.getPrincipals())
            + " caused by \"" + StorageAccessAuthorizer.reasonToString(nReason) + "\"");
        }

    /**
     * Log the backing map level authorization request.
     *
     * @param context  the context of the backing map to authorize access to
     * @param subject  the Subject
     * @param fWrite   true for write operation; read otherwise
     * @param nReason  the reason for the check
     */
    protected void logMapRequest(BackingMapContext context, Subject subject, boolean fWrite, int nReason)
        {
        Logger.info('"' + (fWrite ? "Write-any" : "Read-any") + "\" request for cache \""
            + context.getCacheName() + '"'
            + (subject == null ?
                " from unidentified user" :
                " on behalf of " + subject.getPrincipals())
            + " caused by \"" + StorageAccessAuthorizer.reasonToString(nReason) + "\"");
        }


    // ----- data fields -----------------------------------------------------

    /**
     * Flag indicating whether or not a null Subject is allowed.
     */
    private final boolean f_fStrict;
    }
