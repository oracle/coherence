/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.ai.operations;

import com.tangosol.io.ReadBuffer;

import java.nio.LongBuffer;

import java.util.HashMap;

/**
 * A {@link com.oracle.coherence.ai.VectorOp} that executes a Jaccard
 * similarity algorithm against a vector of longs.
 * <p>
 * This implementation uses a brute force non-optimized approach to
 * work out the Jaccard similarity.
 */
public class LongBruteForceJaccard
        extends LongJaccard
    {
    /**
     * Default constructor for serialization.
     */
    public LongBruteForceJaccard()
        {
        }

    /**
     * Create a {@link LongBruteForceJaccard} operation.
     *
     * @param target  the vector to find similarities to
     */
    public LongBruteForceJaccard(long[] target)
        {
        super(target);
        }

    @Override
    public Float apply(ReadBuffer binary)
        {
        LongBuffer buffer = binary.toByteBuffer().asLongBuffer();
        return getJaccardSimilarity(buffer);
        }

    /**
     * Calculate the Jaccard similarity for the specified long vector with
     * this operation's target vector.
     *
     * @param buffer  a {@link LongBuffer} containing the vector to
     *                calculate the similarity to the target vector
     *
     * @return  the Jaccard similarity for the specified vector to
     *          this operation's target vector
     */
    public float getJaccardSimilarity(LongBuffer buffer)
        {
        int   length = buffer.limit();
        // a⋃b
        float union  = unionCount(buffer);
        // a⋂b
        float inter  = length + target.length - union;
        return inter / union;
        }

    /**
     * Calculate the union of the specified long vector with this
     * operation's target vector.
     *
     * @param buffer  a {@link LongBuffer} containing the vector to
     *                calculate the union with the target vector
     *
     * @return  the union for the specified vector with this operation's
     *          target vector
     */
    public float unionCount(LongBuffer buffer)
        {
        int                 lengthA = buffer.limit();
        int                 lengthB = target.length;
        HashMap<Long, Void> map     = new HashMap<>();
        for (int i = 0; i < lengthA; i++)
            {
            map.put(buffer.get(i), null);
            }
        for (int i = 0; i < lengthB; i++)
            {
            map.put(target[i], null);
            }
        return map.size();
        }
    }
