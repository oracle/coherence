/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io;

/**
 * Optional interface for serializers that accept a configured serialization
 * container limit policy.
 *
 * @author Aleks Seovic  2026.05.20
 * @since 26.07
 */
public interface SerializationLimitAware
    {
    /**
     * Set the serialization container limit policy.
     *
     * @param policy  the policy to use, or {@code null} to inherit defaults
     */
    void setLimitPolicy(SerializationLimitPolicy policy);
    }
