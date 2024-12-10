/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.common.schema.lang.cpp;


import com.oracle.coherence.common.schema.ExtensibleProperty;
import com.oracle.coherence.common.schema.lang.AbstractLangProperty;


/**
 * The representation of the C++ property metadata.
 *
 * @author as  2013.07.12
 */
public class CppProperty
        extends AbstractLangProperty<CppTypeDescriptor>
    {
    /**
     * Construct {@code CppProperty} instance.
     *
     * @param parent  the parent {@link ExtensibleProperty}
     */
    public CppProperty(ExtensibleProperty parent)
        {
        super(parent);
        }
    }
