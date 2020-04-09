/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.common.schema.lang;


import com.oracle.coherence.common.schema.AbstractTypeHandler;
import com.oracle.coherence.common.schema.Property;
import com.oracle.coherence.common.schema.Schema;
import com.oracle.coherence.common.schema.TypeDescriptor;
import com.oracle.coherence.common.schema.TypeHandler;

import com.sun.codemodel.JAnnotationUse;
import com.sun.codemodel.JDefinedClass;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;


/**
 * A base class for {@link TypeHandler} implementations that write
 * type metadata to a Java source file.
 *
 * @param <T>   the type of the {@link com.oracle.coherence.common.schema.Type} class
 *              that is handled by this {@link TypeHandler}
 * @param <TD>  the type of the type descriptor used by the type {@code <T>}
 * @param <A>   the annotation type that can be used to provide hints to this
 *              {@link TypeHandler}
 *
 * @author as  2013.11.21
 */
@SuppressWarnings("unchecked")
public class JavaSourceTypeHandler
        <
            T extends AbstractLangType<? extends Property, TD>,
            TD extends TypeDescriptor,
            A extends Annotation
        >
        extends AbstractTypeHandler<T, JDefinedClass>
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Construct a {@code JavaSourceTypeHandler} instance.
     */
    public JavaSourceTypeHandler()
        {
        java.lang.reflect.Type superclass = getClass().getGenericSuperclass();
        m_annotationClass = (Class<A>)
                ((ParameterizedType) superclass).getActualTypeArguments()[2];
        }

    // ---- TypeHandler implementation --------------------------------------

    @Override
    public void exportType(T type, JDefinedClass target, Schema schema)
        {
        if (type.getDescriptor() != null)
            {
            JAnnotationUse annotation = target.annotate(m_annotationClass);
            annotation.param("name", type.getFullName());
            }
        }

    // ---- data members ----------------------------------------------------

    private final Class<A> m_annotationClass;
    }
