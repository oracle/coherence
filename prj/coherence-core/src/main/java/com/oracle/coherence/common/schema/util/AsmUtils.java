/*
 * Copyright (c) 2020, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.common.schema.util;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.internal.asm.ClassReaderInternal;

import com.tangosol.util.Base;

import java.io.IOException;
import java.io.InputStream;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.Type;

import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
/**
 * Various ASM helpers.
 *
 * @author as  2013.07.11
 */
@SuppressWarnings("unchecked")
public class AsmUtils
    {
    /**
     * Convert internal class name to Java class name.
     *
     * @param sInternalName  internal name to convert
     *
     * @return Java class name
     */
    public static String javaName(String sInternalName)
        {
        return sInternalName.replace('/', '.');
        }

    /**
     * Convert Java class name to internal class name.
     *
     * @param sJavaName  Java name to convert
     *
     * @return internal class name
     */
    public static String internalName(String sJavaName)
        {
        return sJavaName.replace('.', '/');
        }

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
            node.visibleAnnotations = new ArrayList<>();
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
            node.visibleAnnotations = new ArrayList<>();
            }
        node.visibleAnnotations.add(annotation);
        }

    /**
     * Add specified annotation to the method.
     *
     * @param node        the {@code MethodNode} to add annotation to
     * @param annotation  the annotation to add
     */
    public static void addAnnotation(MethodNode node, AnnotationNode annotation)
        {
        if (node.visibleAnnotations == null)
            {
            node.visibleAnnotations = new ArrayList<>();
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
     * Return {@code true} if the method has specified annotation.
     *
     * @param node             the {@code MethodNode} to check
     * @param annotationClass  the class of the annotation to check for
     *
     * @return {@code true} if the method has specified annotation,
     *         {@code false} otherwise
     */
    public static boolean hasAnnotation(MethodNode node, Class annotationClass)
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
        return findAnnotationNode(node.visibleAnnotations, annotationClasses);
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
        return findAnnotationNode(node.visibleAnnotations, annotationClasses);
        }

    /**
     * Return the first of the specified annotations that is present on the
     * method.
     *
     * @param node               the {@code MethodNode} to get the annotation from
     * @param annotationClasses  the ordered list of annotations to search for
     *
     * @return the first of the specified annotations that is present on the
     *         field, or {@code null} if none of the specified annotations are
     *         present
     */
    public static AnnotationNode getAnnotation(MethodNode node, Class... annotationClasses)
        {
        return findAnnotationNode(node.visibleAnnotations, annotationClasses);
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
     *
     * @since 22.06.10
     */
    public static AnnotationNode getAnnotation(ClassNode node, String... annotationClasses)
        {
        return findAnnotationNode(node.visibleAnnotations, annotationClasses);
        }

    /**
     * Returns a {@link Map} of attributes and their associated value extracted
     * from the first annotation found from the provided array of annotation
     * names discovered on the given class.
     *
     * @param clsTarget      the target class to search
     * @param saAnnotations  the ordered array of annotations to search from
     * @param saAttributes   the attributes to extract from the discovered
     *                       annotation
     *
     * @return  a {@link Map} keyed by the annotation attribute with the value
     *          being the value extracted from the annotation.  If no value
     *          is found for a particular attribute, the key won't be present
     *          in the return {@link Map}.  If not attribute values are found
     *          or the given annotations are not found on the class, this
     *          will return an empty {@link Map}
     *
     * @since 22.06.10
     */
    public static Map<String, Object> getAnnotationValues(Class<?> clsTarget,
                                                          String[] saAnnotations, String[] saAttributes)
        {
        ClassNode source = new ClassNode();

        try (InputStream in = Base.getContextClassLoader(clsTarget)
                .getResourceAsStream(internalName(clsTarget.getName()) + ".class"))
            {
            ClassReaderInternal reader = new ClassReaderInternal(in);
            reader.accept(source, 0);
            }
        catch (IOException e)
            {
            Logger.err(String.format("Exception reading class %s", clsTarget.getName()), e);

            return Collections.emptyMap();
            }

        Map<String, Object> mapAttributes = new HashMap<>();
        AnnotationNode      annotation    = getAnnotation(source, saAnnotations);

        if (annotation != null)
            {
            for (String sAttribute : saAttributes)
                {
                Object oValue = getAnnotationAttribute(annotation, sAttribute);
                if (oValue != null)
                    {
                    mapAttributes.put(sAttribute, oValue);
                    }
                }
            }

        return mapAttributes;
        }

    /**
     * Find first of the specified annotation classes that is present in the
     * list of annotation nodes.
     *
     * @param annotations          a list of annotation nodes
     * @param saAnnotationClasses  the annotations to search for, in order
     *
     * @return first of the specified annotation classes that is present in the
     *         list of annotation nodes; {@code null} if none are present
     *
     * @since 22.06.10
     */
    private static AnnotationNode findAnnotationNode(List<AnnotationNode> annotations, String[] saAnnotationClasses)
        {
        if (annotations != null)
            {
            for (String sAnnotationClass : saAnnotationClasses)
                {
                String sDesc = Type.getType('L' + internalName(sAnnotationClass) + ';').getDescriptor();
                for (AnnotationNode an : annotations)
                    {
                    if (sDesc.equals(an.desc))
                        {
                        return an;
                        }
                    }
                }
            }
        return null;
        }

    /**
     * Find first of the specified annotation classes that is present in the
     * list of annotation nodes.
     *
     * @param annotations        a list of annotation nodes
     * @param annotationClasses  the annotations to search for, in order
     *
     * @return first of the specified annotation classes that is present in the
     *         list of annotation nodes; {@code null} if none are present
     */
    private static AnnotationNode findAnnotationNode(List<AnnotationNode> annotations, Class[] annotationClasses)
        {
        if (annotations != null)
            {
            for (Class annotationClass : annotationClasses)
                {
                String desc = Type.getDescriptor(annotationClass);
                for (AnnotationNode an : annotations)
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
