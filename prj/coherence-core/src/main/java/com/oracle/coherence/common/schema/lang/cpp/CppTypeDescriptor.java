/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.common.schema.lang.cpp;


import com.oracle.coherence.common.schema.AbstractTypeDescriptor;
import com.oracle.coherence.common.schema.CanonicalTypeDescriptor;
import com.oracle.coherence.common.schema.TypeDescriptor;
import java.util.ArrayList;
import java.util.List;


/**
 * Implementation of a {@link TypeDescriptor} that is used to represent C++
 * types.
 *
 * @author as  2013.08.27
 */
public class CppTypeDescriptor
        extends AbstractTypeDescriptor<CppTypeDescriptor>
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Construct {@code CppTypeDescriptor} instance.
     *
     * @param namespace  the type namespace
     * @param name       the type name
     * @param fArray     the flag specifying whether the type is an array type
     */
    private CppTypeDescriptor(String[] namespace, String name, boolean fArray)
        {
        super(namespace, name, fArray);
        }

    /**
     * Construct {@code CppTypeDescriptor} instance.
     *
     * @param namespace    the type namespace
     * @param name         the type name
     * @param fArray       the flag specifying whether the type is an array type
     * @param genericArgs  the list of generic argument descriptors
     */
    private CppTypeDescriptor(String[] namespace, String name, boolean fArray,
                              List<CppTypeDescriptor> genericArgs)
        {
        super(namespace, name, fArray, genericArgs);
        }

    // ---- factory methods -------------------------------------------------

    /**
     * Create {@code CppTypeDescriptor} by parsing fully qualified type name.
     *
     * @param type  the fully qualified C++ type name
     *
     * @return a {@code CppTypeDescriptor} for the specified type name
     */
    public static CppTypeDescriptor parse(String type)
        {
        return PARSER.parse(type);
        }

    /**
     * Create {@code CppTypeDescriptor} from a canonical type descriptor.
     *
     * @param type  the canonical type descriptor
     *
     * @return a {@code CppTypeDescriptor} for the specified canonical type
     *         descriptor
     */
    public static CppTypeDescriptor fromCanonical(CanonicalTypeDescriptor type)
        {
        return new CppTypeDescriptor(type.getNamespaceComponents(), type.getName(),
                                      type.isArray(), fromCanonical(type.getGenericArguments()));
        }

    /**
     * Create a list of {@code CppTypeDescriptor}s from a list of canonical
     * type descriptors.
     *
     * @param types  the list of canonical type descriptor
     *
     * @return a list of {@code CppTypeDescriptor}s for the specified
     *         list of canonical type descriptors
     */
    public static List<CppTypeDescriptor> fromCanonical(List<CanonicalTypeDescriptor> types)
        {
        List<CppTypeDescriptor> result = new ArrayList<>(types.size());
        for (CanonicalTypeDescriptor type : types)
            {
            result.add(fromCanonical(type));
            }

        return result;
        }

    // ---- AbstractTypeDescriptor implementation ---------------------------

    @Override
    protected CppTypeDescriptor createArrayType(String[] namespace, String name)
        {
        return new CppTypeDescriptor(namespace, name, true);
        }

    @Override
    protected Parser getParser()
        {
        return PARSER;
        }

    // ---- inner class: CppTypeParser --------------------------------------

    /**
     * An implementation of a C++ type name parser.
     */
    private static class CppTypeParser
            extends Parser<CppTypeDescriptor>
        {
        // ---- constructors ------------------------------------------------

        /**
         * Construct {@code CppTypeParser} instance.
         *
         * @param separator  the namespace separator
         */
        public CppTypeParser(String separator)
            {
            super(separator);
            }

        // ---- AbstractTypeDescriptor.Parser implementation ----------------

        @Override
        protected CppTypeDescriptor getStandardType(String type)
            {
            return null;
            }

        @Override
        protected CppTypeDescriptor createTypeDescriptor(
                String[] namespace, String name, boolean fArray,
                List<CppTypeDescriptor> genericArgs)
            {
            return new CppTypeDescriptor(namespace, name, fArray, genericArgs);
            }
        }

    // ---- static members --------------------------------------------------

    /**
     * An instance of a C++ type name parser.
     */
    public static final CppTypeParser PARSER = new CppTypeParser("::");
    }
