/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.ai;

import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.ReadBuffer;

import com.tangosol.io.pof.EvolvablePortableObject;

/**
 * An operation that can be performed on a vector.
 *
 * @param <VectorType>  the return type of the operation
 */
public interface VectorOp<VectorType>
        extends ExternalizableLite, EvolvablePortableObject
    {
    /**
     * Perform the operation on the
     * @param buffer  a {@link ReadBuffer} containing the vector data
     *
     * @return  the result of applying the operation
     */
    VectorType apply(ReadBuffer buffer);

    /**
     * Return an identifier for this operation.
     * <p>
     * Operations that perform the same operation on a vector but
     * use a different algorithm may return the same id.
     *
     * @return  an identifier for this operation
     */
    String id();
    }
