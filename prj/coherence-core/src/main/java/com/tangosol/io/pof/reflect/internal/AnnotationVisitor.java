/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io.pof.reflect.internal;

import com.tangosol.io.pof.annotation.Portable;
import com.tangosol.io.pof.annotation.PortableProperty;

import com.tangosol.io.pof.reflect.Codec;
import com.tangosol.io.pof.reflect.Codecs;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import java.util.HashSet;
import java.util.Set;

/**
 * An AnnotationVisitor is a {@link Visitor} implementation that injects
 * information obtained by inspecting a provided class into a
 * {@link ClassMetadataBuilder}. The modified builder will then realize a
 * {@link TypeMetadata} instance with this injected information.
 * <p>
 * This implementation is responsible purely for injecting explicitly defined
 * information in the form of annotations. It depends upon, and hence is
 * aware of, only the following annotations:
 * <ul>
 *      <li>{@link Portable}</li>
 *      <li>{@link PortableProperty}</li>
 * </ul>
 * <p>
 * This class has two strategies of metadata discovery - field and accessor -
 * inspected respectively. Duplication is deemed by the same name derived by
 * {@link NameMangler} implementations. 
 *
 * @author hr
 *
 * @param <T>  the type this AnnotationVisitor will be inspecting
 * 
 * @since 3.7.1
 */
public class AnnotationVisitor<T>
        implements Visitor<ClassMetadataBuilder<T>>
    {

    // ----- constructors ---------------------------------------------------

    /**
     * Construct an AnnotationVisitor instance
     */
    public AnnotationVisitor()
        {
        this(false);
        }

    /**
     * Construct an AnnotationVisitor instance specifying whether to use
     * auto-indexing
     *
     * @param fAutoIndex  whether to enable auto-indexing
     */
    public AnnotationVisitor(boolean fAutoIndex)
        {
        this.m_fAutoIndex = fAutoIndex;
        }

    // ----- Visitor interface ----------------------------------------------

    /**
     * {@inheritDoc}
     */
    public <C> void visit(ClassMetadataBuilder<T> builder, Class<C> clz)
        {
        Portable portable = clz.getAnnotation(Portable.class);

        // fast escape switch
        if (portable == null)
            {
            return;
            }

        // get header level information
        boolean fAutoIndex = m_fAutoIndex;
        builder.setClass((Class<T>) clz);

        // get field level information
        Field[]     aFields            = clz.getDeclaredFields();
        Set<String> listFieldsExcluded = new HashSet<String>(aFields.length);
        for (int i = 0; i < aFields.length; ++i)
            {
            Field            field     = aFields[i];
            PortableProperty attribute = field.getAnnotation(PortableProperty.class);
            if (attribute == null)
                {
                continue;
                }

            String   sFieldName   = field.getName();
            Class<?> clzField     = field.getType();
            String   sMangledName = NameManglers.FIELD_MANGLER.mangle(sFieldName);
            Codec    codec        = Codecs.getCodec(attribute.codec());

            if (!fAutoIndex && attribute.value() < 0)
                {
                throw new IllegalArgumentException("A POF Index must be specified for the property "
                    + clz.getName()+"#"+sFieldName+" by specifying "
                    + "within the annotation or enabling autoIndexing on the "
                    + "Portable annotation");
                }

            builder.addAttribute(
                    builder.newAttribute()
                           .setName(sMangledName)
                           .setCodec(codec)
                           .setInvocationStrategy(new InvocationStrategies.FieldInvocationStrategy(field))
                           .setIndex(attribute.value()).build());

            // field level annotations take precedence over accessor
            // annotations
            listFieldsExcluded.add(sMangledName);
            }

        // get method level information
        Method[] aMethods = clz.getDeclaredMethods();
        for (int i = 0; i < aMethods.length; ++i)
            {
            Method           method    = aMethods[i];
            PortableProperty attribute = method.getAnnotation(PortableProperty.class);
            if (attribute == null)
                {
                continue;
                }

            String sMethodName = method.getName();
            if (sMethodName.startsWith("get") || sMethodName.startsWith("set")
               || sMethodName.startsWith("is"))
                {
                String sName = NameManglers.METHOD_MANGLER.mangle(sMethodName);
                if (listFieldsExcluded.contains(sName))
                    {
                    continue;
                    }

                InvocationStrategies.MethodInvocationStrategy<T,Object> strategy =
                        new InvocationStrategies.MethodInvocationStrategy<T,Object>(method);
                Codec codec = Codecs.getCodec(attribute.codec());

                if (!fAutoIndex && attribute.value() < 0)
                    {
                    throw new IllegalArgumentException("A POF Index must be specified for the method "
                        + clz.getName() + "#" + sMethodName + " by specifying "
                        + "within the annotation or enabling autoIndexing on the "
                        + "Portable annotation");
                    }

                builder.addAttribute(
                        builder.newAttribute()
                               .setName(sName)
                               .setCodec(codec)
                               .setInvocationStrategy(strategy)
                               .setIndex(attribute.value()).build());

                // in the case where both accessors (getters and setters) are
                // annotated we only use the values in the first annotation
                // we encounter
                listFieldsExcluded.add(sName);
                }
            }
        }

    // ----- data members ---------------------------------------------------

    /**
     * Whether to use the auto-indexing feature to derive indexes
     */
    private final boolean m_fAutoIndex;
    }
