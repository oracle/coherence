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
import com.oracle.coherence.common.schema.util.AsmUtils;
import com.oracle.coherence.common.schema.util.StringUtils;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;


/**
 * A base class for {@link TypeHandler} implementations that read and write
 * type metadata from/to Java class file.
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
public class ClassFileTypeHandler
        <
            T extends AbstractLangType<? extends Property, TD>,
            TD extends TypeDescriptor,
            A extends Annotation
        >
        extends AbstractTypeHandler<T, ClassNode>
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Construct a {@code ClassFileTypeHandler} instance.
     */
    public ClassFileTypeHandler()
        {
        java.lang.reflect.Type superclass = getClass().getGenericSuperclass();
        m_typeDescriptorClass = (Class<TD>)
                ((ParameterizedType) superclass).getActualTypeArguments()[1];
        m_annotationClass = (Class<A>)
                ((ParameterizedType) superclass).getActualTypeArguments()[2];
        }

    // ---- TypeHandler implementation --------------------------------------

    @Override
    public void importType(T type, ClassNode source, Schema schema)
        {
        AnnotationNode an = AsmUtils.getAnnotation(source, m_annotationClass);
        if (an != null)
            {
            String name = (String) AsmUtils.getAnnotationAttribute(an, "name");
            if (!StringUtils.isEmpty(name))
                {
                type.setDescriptor(parseTypeName(name));
                }
            }
        }

    @Override
    public void exportType(T type, ClassNode target, Schema schema)
        {
        if (type.getDescriptor() != null)
            {
            AnnotationNode an = new AnnotationNode(Type.getDescriptor(m_annotationClass));
            an.values = Arrays.asList("name", type.getFullName());

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
