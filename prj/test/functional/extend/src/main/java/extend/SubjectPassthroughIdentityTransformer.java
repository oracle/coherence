/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package extend;

import com.tangosol.net.Service;

import com.tangosol.net.security.IdentityTransformer;

import javax.security.auth.Subject;

/**
 * Explicit test transformer for functional tests that intentionally send
 * Subject identity tokens.
 *
 * @author OpenAI  2026.05.16
 */
public class SubjectPassthroughIdentityTransformer
        implements IdentityTransformer
    {
    @Override
    public Object transformIdentity(Subject subject, Service service)
        {
        return subject;
        }
    }
