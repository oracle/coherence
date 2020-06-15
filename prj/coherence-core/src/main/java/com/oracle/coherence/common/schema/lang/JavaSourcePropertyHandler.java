/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.common.schema.lang;


import com.oracle.coherence.common.schema.AbstractPropertyHandler;
import com.oracle.coherence.common.schema.JavaSourceSchemaExporter;
import com.oracle.coherence.common.schema.PropertyHandler;
import com.oracle.coherence.common.schema.Schema;
import com.oracle.coherence.common.schema.TypeDescriptor;
import com.sun.codemodel.JAnnotationUse;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;


/**
 * A base class for {@link PropertyHandler} implementations that write
 * property metadata to a Java source file.
 *
 * @param <T>   the type of property class that is handled by this {@link
 *              PropertyHandler}
 * @param <TD>  the type of type descriptor used to represent property type
 * @param <A>   the annotation type that can be used to provide hints to this
 *              {@link PropertyHandler}
 *
 * @author as  2013.11.21
 */
@SuppressWarnings("unchecked")
public class JavaSourcePropertyHandler
        <
            T extends AbstractLangProperty<TD>,
            TD extends TypeDescriptor,
            A extends Annotation
        >
        extends AbstractPropertyHandler<T, JavaSourceSchemaExporter.JProperty>
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Construct a {@code JavaSourcePropertyHandler} instance.
     */
    public JavaSourcePropertyHandler()
        {
        java.lang.reflect.Type superclass = getClass().getGenericSuperclass();
        m_annotationClass = (Class<A>)
                ((ParameterizedType) superclass).getActualTypeArguments()[2];
        }

    // ---- PropertyHandler implementation ----------------------------------

    @Override
    public void exportProperty(T property, JavaSourceSchemaExporter.JProperty target, Schema schema)
        {
        if (property.getType() != null)
            {
            JAnnotationUse annotation = target.field().annotate(m_annotationClass);
            annotation.param("type", property.getType().getFullName());
            }
        }

    // ---- data members ----------------------------------------------------

    private final Class<A> m_annotationClass;
    }
