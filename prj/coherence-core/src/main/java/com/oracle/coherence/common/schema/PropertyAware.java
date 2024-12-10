/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.common.schema;

/**
 * An interface that can be implemented by {@link Type} implementations in
 * order to receive notifications when a property is added to the parent
 * {@link ExtensibleType}.
 *
 * @author as  2013.11.18
 */
public interface PropertyAware
    {
    /**
     * The method that will be called when a property is added to the parent
     * {@link ExtensibleType}.
     *
     * @param property  the property that was added to the extensible type
     */
    void propertyAdded(ExtensibleProperty property);
    }
