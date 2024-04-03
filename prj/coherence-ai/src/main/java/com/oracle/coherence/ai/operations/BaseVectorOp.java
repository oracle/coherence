/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.ai.operations;

import com.oracle.coherence.ai.VectorOp;

import com.tangosol.io.AbstractEvolvable;

import java.util.Objects;

/**
 * A base class for {@link VectorOp} implementations.
 *
 * @param <R> the type of vector the operation executes against
 */
public abstract class BaseVectorOp<R>
        extends AbstractEvolvable
        implements VectorOp<R>
    {
    @Override
    public int hashCode()
        {
        String id = id();
        return id == null ? 0 : id().hashCode();
        }

    @Override
    public boolean equals(Object o)
        {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        BaseVectorOp<?> that = (BaseVectorOp<?>) o;
        return Objects.equals(id(), that.id());
        }
    }
