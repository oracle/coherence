/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io;


/**
* The DeltaCompressor interface provides the capability of comparing two
* in-memory buffers containing an old and a new value, and producing a result
* (called a "delta") that can be applied to the old value to create the new
* value.
*
* @author cp  2009.01.06
*/
public interface DeltaCompressor
    {
    /**
    * Compare an old value to a new value and generate a delta that represents
    * the changes that must be made to the old value in order to transform it
    * into the new value.  The generated delta must be a ReadBuffer of non-zero
    * length.
    * <p>
    * If the old value is null, the generated delta must be a "replace", meaning
    * that applying it to any value must produce the specified new value.
    *
    * @param bufOld  the old value
    * @param bufNew  the new value; must not be null
    *
    * @return the changes that must be made to the old value in order to
    *         transform it into the new value, or null to indicate no change
    */
    public ReadBuffer extractDelta(ReadBuffer bufOld, ReadBuffer bufNew);

    /**
    * Apply a delta to an old value in order to create a new value.
    *
    * @param bufOld    the old value
    * @param bufDelta  the delta information returned from
    *                  {@link #extractDelta} to apply to the old value
    *
    * @return the new value
    */
    public ReadBuffer applyDelta(ReadBuffer bufOld, ReadBuffer bufDelta);
    }
