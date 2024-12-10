/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.common.schema.lang.java;


import com.oracle.coherence.common.schema.ExtensibleType;
import com.oracle.coherence.common.schema.lang.AbstractLangType;


/**
 * The representation of the Java type metadata.
 *
 * @author as  2013.06.21
 */
public class JavaType extends AbstractLangType<JavaProperty, JavaTypeDescriptor>
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Construct {@code JavaType} instance.
     *
     * @param parent  the parent {@link ExtensibleType}
     */
    public JavaType(ExtensibleType parent)
        {
        super(parent);
        }

    // ---- public API ------------------------------------------------------
    
    /**
     * Return a descriptor for the wrapper type associated with this type,
     * if any.
     *
     * @return a descriptor for the wrapper type associated with this type
     */
    public JavaTypeDescriptor getWrapperType()
        {
        return m_wrapperType;
        }

    /**
     * Set the wrapper type associated with this type.
     *
     * @param wrapperType  the wrapper type associated with this type
     */
    public void setWrapperType(JavaTypeDescriptor wrapperType)
        {
        m_wrapperType = wrapperType;
        }

    /**
     * Return {@code true} if this type implements the specified interface.
     *
     * @param name  the name of the interface to check
     *
     * @return {@code true} if this type implements the specified interface,
     *         {@code false} otherwise
     */
    public boolean implementsInterface(String name)
        {
        for (JavaTypeDescriptor td : getAliases())
            {
            if (name.equals(td.getFullName()))
                {
                return true;
                }
            }

        return false;
        }

    // ---- Object methods --------------------------------------------------

    @Override
    public String toString()
        {
        return getClass().getSimpleName() + "{" +
               "name=" + getName() +
               ", desc=" + getDescriptor() +
               ", wrapper=" + getWrapperType() +
               '}';
        }

    // ---- data members ----------------------------------------------------

    private JavaTypeDescriptor m_wrapperType;
    }
