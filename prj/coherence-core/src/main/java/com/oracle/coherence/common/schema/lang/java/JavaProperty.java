/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.common.schema.lang.java;


import com.oracle.coherence.common.schema.ExtensibleProperty;
import com.oracle.coherence.common.schema.ExtensibleType;
import com.oracle.coherence.common.schema.Schema;
import com.oracle.coherence.common.schema.lang.AbstractLangProperty;


/**
 * The representation of the Java property metadata.
 *
 * @author as  2013.07.12
 */
public class JavaProperty
        extends AbstractLangProperty<JavaTypeDescriptor>
    {
    /**
     * Construct {@code JavaProperty} instance.
     *
     * @param parent  the parent {@link ExtensibleProperty}
     */
    public JavaProperty(ExtensibleProperty parent)
        {
        super(parent);
        }

    /**
     * Resolve and return the type of this property.
     *
     * @param schema  the schema to use for type lookup
     *
     * @return the descriptor for the property type
     *
     * @throws IllegalStateException  if the property type cannot be resolved
     */
    public JavaTypeDescriptor resolveType(Schema schema)
        {
        if (getType() != null)
            {
            return getType();
            }
        else
            {
            ExtensibleType extType = schema.getType(getParent().getType());
            if (extType != null)
                {
                JavaType javaType = extType.getExtension(JavaType.class);
                if (javaType != null)
                    {
                    return javaType.getDescriptor();
                    }
                }
            }

        throw new IllegalStateException("Unable to resolve property type");
        }
    }
