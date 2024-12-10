/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util;

/**
 * HashEncoded interface represents an ability to retrieve an encoded hash
 * value; most commonly used to calculate a partition id.
 *
 * @author hr/gg/cp
 * @since Coherence 12.1.2
 */
public interface HashEncoded
    {
    /**
     * Return the encoded hash value or {@link #UNENCODED} if absent.
     *
     * @return the encoded hash value or {@link #UNENCODED} if absent
     */
    public int getEncodedHash();

    // ----- constants --------------------------------------------------

    /**
     * A reserved value that suggests the absence of the encoded hash.
     */
    public final int UNENCODED = Integer.MIN_VALUE;
    }
