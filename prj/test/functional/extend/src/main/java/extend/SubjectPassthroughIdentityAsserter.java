/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package extend;

import com.tangosol.net.Service;

import com.tangosol.net.security.IdentityAsserter;

import javax.security.auth.Subject;

/**
 * Explicit test asserter for functional tests that intentionally trust Subject
 * identity tokens.
 *
 * @author OpenAI  2026.05.16
 */
public class SubjectPassthroughIdentityAsserter
        implements IdentityAsserter
    {
    @Override
    public Subject assertIdentity(Object oToken, Service service)
            throws SecurityException
        {
        if (oToken == null || oToken instanceof Subject)
            {
            return (Subject) oToken;
            }
        throw new SecurityException("identity token is unsupported type: "
                + oToken.getClass().getName());
        }
    }
