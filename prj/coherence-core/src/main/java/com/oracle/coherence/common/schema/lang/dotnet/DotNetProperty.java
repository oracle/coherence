/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.common.schema.lang.dotnet;


import com.oracle.coherence.common.schema.ExtensibleProperty;
import com.oracle.coherence.common.schema.lang.AbstractLangProperty;


/**
 * The representation of the .NET property metadata.
 *
 * @author as  2013.07.12
 */
public class DotNetProperty
        extends AbstractLangProperty<DotNetTypeDescriptor>
    {
    /**
     * Construct {@code CppProperty} instance.
     *
     * @param parent  the parent {@link ExtensibleProperty}
     */
    public DotNetProperty(ExtensibleProperty parent)
        {
        super(parent);
        }
    }
