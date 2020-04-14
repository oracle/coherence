/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.common.schema.lang.java;


import com.oracle.coherence.common.schema.AbstractTypeDescriptor;
import com.oracle.coherence.common.schema.CanonicalTypeDescriptor;
import com.oracle.coherence.common.schema.Schema;
import com.oracle.coherence.common.schema.TypeDescriptor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Implementation of a {@link TypeDescriptor} that is used to represent Java
 * types.
 *
 * @author as  2013.08.27
 */
public class JavaTypeDescriptor
        extends AbstractTypeDescriptor<JavaTypeDescriptor>
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Construct {@code JavaTypeDescriptor} instance.
     *
     * @param name       the type name
     */
    private JavaTypeDescriptor(String name)
        {
        super(name);
        }

    /**
     * Construct {@code JavaTypeDescriptor} instance.
     *
     * @param namespace  the type namespace
     * @param name       the type name
     */
    private JavaTypeDescriptor(String[] namespace, String name)
        {
        super(namespace, name);
        }

    /**
     * Construct {@code JavaTypeDescriptor} instance.
     *
     * @param namespace  the type namespace
     * @param name       the type name
     * @param fArray     the flag specifying whether the type is an array type
     */
    private JavaTypeDescriptor(String[] namespace, String name, boolean fArray)
        {
        super(namespace, name, fArray);
        }

    /**
     * Construct {@code JavaTypeDescriptor} instance.
     *
     * @param namespace    the type namespace
     * @param name         the type name
     * @param fArray       the flag specifying whether the type is an array type
     * @param genericArgs  the list of generic argument descriptors
     */
    private JavaTypeDescriptor(String[] namespace, String name, boolean fArray,
                               List<JavaTypeDescriptor> genericArgs)
        {
        super(namespace, name, fArray, genericArgs);
        }

    // ---- factory methods -------------------------------------------------

    /**
     * Create {@code JavaTypeDescriptor} by parsing fully qualified type name.
     *
     * @param type  the fully qualified Java type name
     *
     * @return a {@code JavaTypeDescriptor} for the specified type name
     */
    public static JavaTypeDescriptor parse(String type)
        {
        return PARSER.parse(type);
        }

    /**
     * Create {@code JavaTypeDescriptor} by parsing fully qualified internal
     * type name.
     *
     * @param type  the fully qualified internal Java type name
     *
     * @return a {@code JavaTypeDescriptor} for the specified type name
     */
    public static JavaTypeDescriptor fromInternal(String type)
        {
        return PARSER.parseInternal(type);
        }

    /**
     * Create {@code JavaTypeDescriptor} from a canonical type descriptor.
     *
     * @param type    the canonical type descriptor
     * @param schema  the schema specified type is defined in
     *
     * @return a {@code JavaTypeDescriptor} for the specified canonical type
     *         descriptor
     */
    public static JavaTypeDescriptor from(CanonicalTypeDescriptor type, Schema schema)
        {
        JavaTypeDescriptor jtd =
                new JavaTypeDescriptor(type.getNamespaceComponents(), type.getName(),
                                       type.isArray(), from(type.getGenericArguments(), schema));

        JavaType javaType = schema.getType(type, JavaType.class);
        if (javaType != null)
            {
            String namespace = javaType.getNamespace();
            if (namespace != null)
                {
                jtd.setNamespace(namespace);
                }
            String name = javaType.getName();
            if (name != null)
                {
                jtd.setName(name);
                }
            }

        return jtd;
        }

    /**
     * Create a list of {@code JavaTypeDescriptor}s from a list of canonical
     * type descriptors.
     *
     * @param types   the list of canonical type descriptor
     * @param schema  the schema specified types are defined in
     *
     * @return a list of {@code JavaTypeDescriptor}s for the specified
     *         list of canonical type descriptors
     */
    private static List<JavaTypeDescriptor> from(List<CanonicalTypeDescriptor> types, Schema schema)
        {
        if (types == null)
            {
            return null;
            }

        List<JavaTypeDescriptor> result = new ArrayList<>(types.size());

        for (CanonicalTypeDescriptor type : types)
            {
            result.add(from(type, schema));
            }

        return result;
        }

    // ---- AbstractTypeDescriptor implementation ---------------------------

    @Override
    protected JavaTypeDescriptor createArrayType(String[] namespace, String name)
        {
        return new JavaTypeDescriptor(namespace, name, true);
        }

    @Override
    protected Parser getParser()
        {
        return PARSER;
        }

    // ---- inner class: JavaTypeParser -------------------------------------

    /**
     * An implementation of a Java type name parser.
     */
    private static class JavaTypeParser extends Parser<JavaTypeDescriptor>
        {
        // ---- constructors ------------------------------------------------

        /**
         * Construct {@code JavaTypeParser} instance.
         *
         * @param separator  the namespace separator
         */
        public JavaTypeParser(String separator)
            {
            super(separator);
            }

        // ---- AbstractTypeDescriptor.Parser implementation ----------------

        @Override
        protected JavaTypeDescriptor createTypeDescriptor(
                String[] namespace, String name, boolean fArray,
                List<JavaTypeDescriptor> genericArgs)
            {
            return new JavaTypeDescriptor(namespace, name, fArray, genericArgs);
            }

        @Override
        protected JavaTypeDescriptor getStandardType(String type)
            {
            return STANDARD_TYPES.get(type);
            }

        // ---- static initializer ------------------------------------------

        private static final Map<String, JavaTypeDescriptor> STANDARD_TYPES;
        static
            {
            Map<String, JavaTypeDescriptor> map = new HashMap<>();

            // primitive types
            map.put("boolean", new JavaTypeDescriptor("boolean"));
            map.put("byte",    new JavaTypeDescriptor("byte"));
            map.put("char",    new JavaTypeDescriptor("char"));
            map.put("short",   new JavaTypeDescriptor("short"));
            map.put("int",     new JavaTypeDescriptor("int"));
            map.put("long",    new JavaTypeDescriptor("long"));
            map.put("float",   new JavaTypeDescriptor("float"));
            map.put("double",  new JavaTypeDescriptor("double"));

            // reference types
            map.put("java.lang.Object",         OBJECT);
            map.put("java.lang.String",         new JavaTypeDescriptor(JAVA_LANG, "String"));
            map.put("java.math.BigDecimal",     new JavaTypeDescriptor(JAVA_MATH, "BigDecimal"));
            map.put("java.math.BigInteger",     new JavaTypeDescriptor(JAVA_MATH, "BigInteger"));
            map.put("java.util.Date",           new JavaTypeDescriptor(JAVA_UTIL, "Date"));
            map.put("java.time.LocalDate",      new JavaTypeDescriptor(JAVA_TIME, "LocalDate"));
            map.put("java.time.LocalDateTime",  new JavaTypeDescriptor(JAVA_TIME, "LocalDateTime"));
            map.put("java.time.LocalTime",      new JavaTypeDescriptor(JAVA_TIME, "LocalTime"));
            map.put("java.time.OffsetDateTime", new JavaTypeDescriptor(JAVA_TIME, "OffsetDateTime"));
            map.put("java.time.OffsetTime",     new JavaTypeDescriptor(JAVA_TIME, "OffsetTime"));
            map.put("java.time.ZonedDateTime",  new JavaTypeDescriptor(JAVA_TIME, "ZonedDateTime"));

            STANDARD_TYPES = map;
            }
        }

    // ---- static members --------------------------------------------------

    private static String[] JAVA_LANG = new String[] {"java", "lang"};
    private static String[] JAVA_MATH = new String[] {"java", "math"};
    private static String[] JAVA_UTIL = new String[] {"java", "util"};
    private static String[] JAVA_TIME = new String[] {"java", "time"};

    /**
     * A type descriptor for java.lang.Object.
     */
    public static final JavaTypeDescriptor OBJECT = new JavaTypeDescriptor(JAVA_LANG, "Object");

    /**
     * An instance of a Java type name parser.
     */
    public static final JavaTypeParser PARSER = new JavaTypeParser(".");
    }
