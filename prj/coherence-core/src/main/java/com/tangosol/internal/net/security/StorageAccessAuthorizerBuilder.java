/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.security;

import com.tangosol.coherence.config.ParameterList;
import com.tangosol.coherence.config.builder.ParameterizedBuilder;

import com.tangosol.config.expression.ParameterResolver;

import com.tangosol.net.security.StorageAccessAuthorizer;

/**
 * Operational storage authorizer builder plus PEER-01 subject-proof policy.
 *
 * @author Aleks Seovic  2026.05.19
 * @since 15.1.2.0
 */
public class StorageAccessAuthorizerBuilder
        implements ParameterizedBuilder<StorageAccessAuthorizer>
    {
    /**
     * Construct a policy-aware storage access authorizer builder.
     *
     * @param builder                the delegate authorizer builder
     * @param fSubjectProofRequired  whether subject proof is required
     */
    public StorageAccessAuthorizerBuilder(ParameterizedBuilder<StorageAccessAuthorizer> builder,
            boolean fSubjectProofRequired)
        {
        if (builder == null)
            {
            throw new IllegalArgumentException("storage access authorizer builder is required");
            }
        f_builder               = builder;
        f_fSubjectProofRequired = fSubjectProofRequired;
        }

    @Override
    public StorageAccessAuthorizer realize(ParameterResolver resolver, ClassLoader loader,
            ParameterList listParameters)
        {
        return f_builder.realize(resolver, loader, listParameters);
        }

    /**
     * Return {@code true} iff the operational authorizer requires subject
     * proof for non-null request-context subjects.
     *
     * @return {@code true} iff subject proof is required
     */
    public boolean isSubjectProofRequired()
        {
        return f_fSubjectProofRequired;
        }

    /**
     * Return the delegate authorizer builder.
     *
     * @return the delegate authorizer builder
     */
    public ParameterizedBuilder<StorageAccessAuthorizer> getDelegate()
        {
        return f_builder;
        }

    private final ParameterizedBuilder<StorageAccessAuthorizer> f_builder;
    private final boolean                                       f_fSubjectProofRequired;
    }
