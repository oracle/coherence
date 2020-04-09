/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.common.schema.util;


import java.util.ArrayList;
import java.util.List;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;


/**
 * Various ASM helpers.
 *
 * @author as  2013.07.11
 */
@SuppressWarnings("unchecked")
public class AsmUtils
    {
    /**
     * Add specified annotation to the class.
     *
     * @param node        the {@code ClassNode} to add annotation to
     * @param annotation  the annotation to add
     */
    public static void addAnnotation(ClassNode node, AnnotationNode annotation)
        {
        if (node.visibleAnnotations == null)
            {
            node.visibleAnnotations = new ArrayList();
            }
        node.visibleAnnotations.add(annotation);
        }

    /**
     * Add specified annotation to the field.
     *
     * @param node        the {@code FieldNode} to add annotation to
     * @param annotation  the annotation to add
     */
    public static void addAnnotation(FieldNode node, AnnotationNode annotation)
        {
        if (node.visibleAnnotations == null)
            {
            node.visibleAnnotations = new ArrayList();
            }
        node.visibleAnnotations.add(annotation);
        }

    /**
     * Return {@code true} if the class has specified annotation.
     *
     * @param node             the {@code ClassNode} to check
     * @param annotationClass  the class of the annotation to check for
     *
     * @return {@code true} if the class has specified annotation,
     *         {@code false} otherwise
     */
    public static boolean hasAnnotation(ClassNode node, Class annotationClass)
        {
        return getAnnotation(node, annotationClass) != null;
        }

    /**
     * Return {@code true} if the field has specified annotation.
     *
     * @param node             the {@code FieldNode} to check
     * @param annotationClass  the class of the annotation to check for
     *
     * @return {@code true} if the field has specified annotation,
     *         {@code false} otherwise
     */
    public static boolean hasAnnotation(FieldNode node, Class annotationClass)
        {
        return getAnnotation(node, annotationClass) != null;
        }

    /**
     * Return the first of the specified annotations that is present on the
     * class.
     *
     * @param node               the {@code ClassNode} to get the annotation from
     * @param annotationClasses  the ordered list of annotations to search for
     *
     * @return the first of the specified annotations that is present on the
     *         class, or {@code null} if none of the specified annotations are
     *         present
     */
    public static AnnotationNode getAnnotation(ClassNode node, Class... annotationClasses)
        {
        if (node.visibleAnnotations != null)
            {
            for (Class annotationClass : annotationClasses)
                {
                String desc = Type.getDescriptor(annotationClass);
                for (AnnotationNode an : (List<AnnotationNode>) node.visibleAnnotations)
                    {
                    if (desc.equals(an.desc))
                        {
                        return an;
                        }
                    }
                }
            }
        return null;
        }

    /**
     * Return the first of the specified annotations that is present on the
     * field.
     *
     * @param node               the {@code FieldNode} to get the annotation from
     * @param annotationClasses  the ordered list of annotations to search for
     *
     * @return the first of the specified annotations that is present on the
     *         field, or {@code null} if none of the specified annotations are
     *         present
     */
    public static AnnotationNode getAnnotation(FieldNode node, Class... annotationClasses)
        {
        if (node.visibleAnnotations != null)
            {
            for (Class annotationClass : annotationClasses)
                {
                String desc = Type.getDescriptor(annotationClass);
                for (AnnotationNode an : (List<AnnotationNode>) node.visibleAnnotations)
                    {
                    if (desc.equals(an.desc))
                        {
                        return an;
                        }
                    }
                }
            }
        return null;
        }

    /**
     * Return a single attribute value from the annotation.
     *
     * @param an    the annotation to get the attribute value from
     * @param name  the name of the annotation attribute
     *
     * @return the value of the specified attribute, or its default value if
     *         the attribute is optional and not present within the annotation
     */
    public static Object getAnnotationAttribute(AnnotationNode an, String name)
        {
        if (an.values != null)
            {
            for (int i = 0; i < an.values.size(); i += 2)
                {
                if (name.equals(an.values.get(i)))
                    {
                    Object val = an.values.get(i + 1);
                    if (val instanceof String[])
                        {
                        // enum
                        String[] asVal = (String[]) val;
                        String sEnumClass = Type.getType(asVal[0]).getClassName();
                        String sEnumValue = asVal[1];
                        try
                            {
                            return Enum.valueOf((Class<? extends Enum>) Class.forName(sEnumClass), sEnumValue);
                            }
                        catch (ClassNotFoundException e)
                            {
                            e.printStackTrace();
                            }
                        }
                    return val;
                    }
                }
            }

        // annotation attribute was not explicitly specified, use default from the annotation class
        try
            {
            Class clazz = AsmUtils.class.getClassLoader().loadClass(
                    an.desc.substring(1, an.desc.length() - 1).replace('/', '.'));
            Object defaultValue = clazz.getMethod(name).getDefaultValue();
            return defaultValue instanceof Class
                   ? Type.getType((Class) defaultValue)
                   : defaultValue;
            }
        catch (Exception e)
            {
            throw new RuntimeException(e);
            }
        }

    /**
     * Create an instance of the specified class using default constructor.
     *
     * @param clazz  the class to create an instance of; must have default
     *               constructor
     * @param <T>    the type of the created instance
     *
     * @return an instance of the specified class
     */
    public static <T> T createInstance(Class<T> clazz)
        {
        try
            {
            return clazz.newInstance();
            }
        catch (Exception e)
            {
            throw new RuntimeException(e);
            }
        }
    }
