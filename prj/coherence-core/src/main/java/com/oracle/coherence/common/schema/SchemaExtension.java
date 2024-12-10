/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.common.schema;

import java.util.Collection;


/**
 * An interface that must be implemented by the custom schema extensions.
 *
 * @author as  2013.08.28
 */
public interface SchemaExtension
    {
    /**
     * Return the name of this schema extension.
     *
     * @return the name of this schema extension
     */
    String getName();

    /**
     * Return a collection of type handlers defined by this schema extension.
     *
     * @return a collection of type handlers defined by this schema extension
     */
    Collection<TypeHandler> getTypeHandlers();

    /**
     * Return a collection of property handlers defined by this schema extension.
     *
     * @return a collection of property handlers defined by this schema extension
     */
    Collection<PropertyHandler> getPropertyHandlers();
    }
