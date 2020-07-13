/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.common.schema.util;


/**
 * Defines an interface that various name transformers must implement.
 *
 * @author as  2013.11.21
 */
public interface NameTransformer
    {
    /**
     * Transform the specified source name.
     *
     * @param source  the source name to transform
     *
     * @return the transformed name
     */
    String transform(String source);

    /**
     * Transform the specified source name components.
     *
     * @param source  the possibly null, possibly empty, source name components to transform
     *
     * @return the transformed name components
     */
    String[] transform(String[] source);
    }
