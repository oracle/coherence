/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.common.schema.lang;


import com.oracle.coherence.common.schema.AbstractPropertyHandler;
import com.oracle.coherence.common.schema.PropertyHandler;
import com.oracle.coherence.common.schema.Schema;
import com.oracle.coherence.common.schema.TypeDescriptor;
import com.oracle.coherence.common.schema.util.AsmUtils;
import com.oracle.coherence.common.schema.util.StringUtils;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.FieldNode;


/**
 * A base class for {@link PropertyHandler} implementations that read and write
 * property metadata from/to Java class file.
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
public class ClassFilePropertyHandler
        <
            T extends AbstractLangProperty<TD>,
            TD extends TypeDescriptor,
            A extends Annotation
        >
        extends AbstractPropertyHandler<T, FieldNode>
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Construct a {@code ClassFilePropertyHandler} instance.
     */
    public ClassFilePropertyHandler()
        {
        java.lang.reflect.Type superclass = getClass().getGenericSuperclass();
        m_typeDescriptorClass = (Class<TD>)
                ((ParameterizedType) superclass).getActualTypeArguments()[1];
        m_annotationClass = (Class<A>)
                ((ParameterizedType) superclass).getActualTypeArguments()[2];
        }

    // ---- PropertyHandler implementation ----------------------------------

    @Override
    public void importProperty(T property, FieldNode source, Schema schema)
        {
        AnnotationNode an = AsmUtils.getAnnotation(source, m_annotationClass);
        if (an != null)
            {
            String name = (String) AsmUtils.getAnnotationAttribute(an, "type");
            if (!StringUtils.isEmpty(name))
                {
                property.setType(parseTypeName(name));
                }
            }
        }

    @Override
    public void exportProperty(T property, FieldNode target, Schema schema)
        {
        if (property.getType() != null)
            {
            AnnotationNode an = new AnnotationNode(Type.getDescriptor(m_annotationClass));
            an.values = Arrays.asList("type", property.getType().getFullName());

            AsmUtils.addAnnotation(target, an);
            }
        }

    // ---- helper methods --------------------------------------------------

    /**
     * Parse specified platform-specific type name and return the
     * {@link TypeDescriptor} that corresponds to it.
     *
     * @param name  the type name to parse
     *
     * @return the {@link TypeDescriptor} for the specified type name
     */
    protected TD parseTypeName(String name)
        {
        try
            {
            if (m_parseMethod == null)
                {
                m_parseMethod = m_typeDescriptorClass.getMethod("parse", String.class);
                }
            return (TD) m_parseMethod.invoke(m_typeDescriptorClass, name);
            }
        catch (Exception e)
            {
            throw new RuntimeException(e);
            }
        }

    // ---- data members ----------------------------------------------------
    
    private final Class<TD> m_typeDescriptorClass;
    private final Class<A> m_annotationClass;
    private Method m_parseMethod;
    }
