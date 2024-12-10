/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.common.schema.lang.dotnet;


import com.oracle.coherence.common.schema.ExtensibleType;
import com.oracle.coherence.common.schema.lang.AbstractLangType;


/**
 * The representation of the .NET type metadata.
 *
 * @author as  2013.06.21
 */
public class DotNetType
        extends AbstractLangType<DotNetProperty, DotNetTypeDescriptor>
    {
    /**
     * Construct {@code DotNetType} instance.
     *
     * @param parent  the parent {@link ExtensibleType}
     */
    public DotNetType(ExtensibleType parent)
        {
        super(parent);
        }
    }
