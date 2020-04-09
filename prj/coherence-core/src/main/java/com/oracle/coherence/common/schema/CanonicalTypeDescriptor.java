/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.common.schema;


import com.oracle.coherence.common.schema.lang.java.JavaTypeDescriptor;
import com.oracle.coherence.common.schema.util.StringUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Canonical implementation of a {@link TypeDescriptor}.
 *
 * @author as  2013.08.20
 */
public class CanonicalTypeDescriptor
        extends AbstractTypeDescriptor<CanonicalTypeDescriptor>
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Construct {@code CanonicalTypeDescriptor} instance.
     *
     * @param namespace    the type namespace
     * @param name         the type name
     */
    public CanonicalTypeDescriptor(String namespace, String name)
        {
        this(StringUtils.split(namespace, PARSER.getSeparator()), name);
        }

    /**
     * Construct {@code CanonicalTypeDescriptor} instance.
     *
     * @param name  the type name
     */
    private CanonicalTypeDescriptor(String name)
        {
        super(null, name, false);
        }

    /**
     * Construct {@code CanonicalTypeDescriptor} instance.
     *
     * @param namespace    the type namespace
     * @param name         the type name
     */
    private CanonicalTypeDescriptor(String[] namespace, String name)
        {
        super(namespace, name);
        }

    /**
     * Construct {@code CanonicalTypeDescriptor} instance.
     *
     * @param namespace    the type namespace
     * @param name         the type name
     * @param fArray       the flag specifying whether the type is an array type
     */
    private CanonicalTypeDescriptor(String[] namespace, String name, boolean fArray)
        {
        super(namespace, name, fArray);
        }

    /**
     * Construct {@code CanonicalTypeDescriptor} instance.
     *
     * @param namespace    the type namespace
     * @param name         the type name
     * @param fArray       the flag specifying whether the type is an array type
     * @param genericArgs  the list of generic argument descriptors
     */
    private CanonicalTypeDescriptor(String[] namespace, String name, boolean fArray,
                                    List<CanonicalTypeDescriptor> genericArgs)
        {
        super(namespace, name, fArray, genericArgs);
        }

    // ---- factory methods -------------------------------------------------

    /**
     * Create {@code CanonicalTypeDescriptor} by parsing fully qualified
     * canonical type name.
     *
     * @param type  the fully qualified canonical type name
     *
     * @return a {@code CanonicalTypeDescriptor} for the specified type name
     */
    public static CanonicalTypeDescriptor parse(String type)
        {
        return PARSER.parse(type);
        }

    /**
     * Create {@code CanonicalTypeDescriptor} from a Java type descriptor.
     *
     * @param type    the Java type descriptor
     * @param schema  the schema specified type is defined in
     *
     * @return a {@code CanonicalTypeDescriptor} for the specified Java type
     *         descriptor
     */
    public static CanonicalTypeDescriptor from(JavaTypeDescriptor type, Schema schema)
        {
        CanonicalTypeDescriptor ctd =
                new CanonicalTypeDescriptor(type.getNamespaceComponents(), type.getName(),
                                            type.isArray(), from(type.getGenericArguments(), schema));

        ExtensibleType extType = schema.findTypeByJavaName(type.getFullName());
        if (extType != null)
            {
            ctd.setNamespace(extType.getNamespace());
            ctd.setName(extType.getName());
            }

        return ctd;
        }

    /**
     * Create a list of {@code CanonicalTypeDescriptor}s from a list of Java
     * type descriptors.
     *
     * @param types   the list of canonical type descriptor
     * @param schema  the schema specified types are defined in
     *
     * @return a list of {@code CanonicalTypeDescriptor}s for the specified
     *         list of Java type descriptors
     */
    private static List<CanonicalTypeDescriptor> from(List<JavaTypeDescriptor> types, Schema schema)
        {
        if (types != null)
            {
            List<CanonicalTypeDescriptor> result = new ArrayList<>(types.size());
            for (JavaTypeDescriptor jtd : types)
                {
                ExtensibleType type = schema.findTypeByJavaName(jtd.getFullName());
                if (type == null)
                    {
                    throw new IllegalStateException("Generic argument type "
                            + jtd.getFullName() + " is not present in the schema.");
                    }
                result.add(type.getDescriptor());
                }

            return result;
            }

        return null;
        }

    // ---- AbstractTypeDescriptor implementation ---------------------------

    @Override
    protected CanonicalTypeDescriptor createArrayType(String[] namespace, String name)
        {
        return new CanonicalTypeDescriptor(namespace, name, true);
        }

    @Override
    protected Parser getParser()
        {
        return PARSER;
        }

    // ---- inner class: CanonicalTypeParser --------------------------------

    /**
     * An implementation of a canonical type name parser.
     */
    private static class CanonicalTypeParser extends Parser<CanonicalTypeDescriptor>
        {
        // ---- constructors ------------------------------------------------

        /**
         * Construct {@code CanonicalTypeParser} instance.
         *
         * @param separator  the namespace separator
         */
        public CanonicalTypeParser(String separator)
            {
            super(separator);
            }

        // ---- AbstractTypeDescriptor.Parser implementation ----------------

        @Override
        protected CanonicalTypeDescriptor createTypeDescriptor(
                String[] namespace, String name, boolean fArray,
                List<CanonicalTypeDescriptor> genericArgs)
            {
            return new CanonicalTypeDescriptor(
                    namespace, name, fArray, genericArgs);
            }

        @Override
        protected CanonicalTypeDescriptor getStandardType(String type)
            {
            return STANDARD_TYPES.get(type);
            }

        // ---- static initializer ------------------------------------------

        private static final Map<String, CanonicalTypeDescriptor> STANDARD_TYPES;
        static
            {
            Map<String, CanonicalTypeDescriptor> map = new HashMap<>();

            // primitive types
            map.put("boolean", BOOLEAN);
            map.put("byte",    new CanonicalTypeDescriptor("byte"));
            map.put("char",    new CanonicalTypeDescriptor("char"));
            map.put("short",   new CanonicalTypeDescriptor("short"));
            map.put("int",     new CanonicalTypeDescriptor("int"));
            map.put("long",    new CanonicalTypeDescriptor("long"));
            map.put("float",   new CanonicalTypeDescriptor("float"));
            map.put("double",  new CanonicalTypeDescriptor("double"));

            // reference types
            map.put("Object",     OBJECT);
            map.put("String",     new CanonicalTypeDescriptor("String"));
            map.put("BigDecimal", new CanonicalTypeDescriptor("BigDecimal"));
            map.put("BigInteger", new CanonicalTypeDescriptor("BigInteger"));
            map.put("DateTime",   new CanonicalTypeDescriptor("DateTime"));

            STANDARD_TYPES = map;
            }
        }

    // ---- static members --------------------------------------------------

    /**
     * A type descriptor for a canonical representation of an Object.
     */
    public static final CanonicalTypeDescriptor OBJECT = new CanonicalTypeDescriptor("Object");

    /**
     * A type descriptor for a canonical representation of a Boolean.
     */
    public static final CanonicalTypeDescriptor BOOLEAN = new CanonicalTypeDescriptor("boolean");

    /**
     * An instance of a canonical type name parser.
     */
    public static final CanonicalTypeParser PARSER = new CanonicalTypeParser(".");
    }
