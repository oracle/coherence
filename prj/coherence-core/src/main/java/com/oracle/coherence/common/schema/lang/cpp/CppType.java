/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.common.schema.lang.cpp;


import com.oracle.coherence.common.schema.ExtensibleType;
import com.oracle.coherence.common.schema.lang.AbstractLangType;


/**
 * The representation of the C++ type metadata.
 *
 * @author as  2013.06.21
 */
public class CppType
        extends AbstractLangType<CppProperty, CppTypeDescriptor>
    {
    /**
     * Construct {@code CppType} instance.
     *
     * @param parent  the parent {@link ExtensibleType}
     */
    public CppType(ExtensibleType parent)
        {
        super(parent);
        }
    }
