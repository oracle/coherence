/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.common.schema.lang.dotnet;


import com.oracle.coherence.common.schema.AbstractTypeDescriptor;
import com.oracle.coherence.common.schema.CanonicalTypeDescriptor;
import com.oracle.coherence.common.schema.TypeDescriptor;
import java.util.ArrayList;
import java.util.List;


/**
 * Implementation of a {@link TypeDescriptor} that is used to represent .NET
 * types.
 *
 * @author as  2013.08.27
 */
public class DotNetTypeDescriptor
        extends AbstractTypeDescriptor<DotNetTypeDescriptor>
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Construct {@code DotNetTypeDescriptor} instance.
     *
     * @param namespace  the type namespace
     * @param name       the type name
     * @param fArray     the flag specifying whether the type is an array type
     */
    private DotNetTypeDescriptor(String[] namespace, String name, boolean fArray)
        {
        super(namespace, name, fArray);
        }

    /**
     * Construct {@code DotNetTypeDescriptor} instance.
     *
     * @param namespace    the type namespace
     * @param name         the type name
     * @param fArray       the flag specifying whether the type is an array type
     * @param genericArgs  the list of generic argument descriptors
     */
    private DotNetTypeDescriptor(String[] namespace, String name, boolean fArray,
                                 List<DotNetTypeDescriptor> genericArgs)
        {
        super(namespace, name, fArray, genericArgs);
        }

    // ---- factory methods -------------------------------------------------

    /**
     * Create {@code DotNetTypeDescriptor} by parsing fully qualified type name.
     *
     * @param type  the fully qualified .NET type name
     *
     * @return a {@code DotNetTypeDescriptor} for the specified type name
     */
    public static DotNetTypeDescriptor parse(String type)
        {
        return PARSER.parse(type);
        }

    /**
     * Create {@code DotNetTypeDescriptor} from a canonical type descriptor.
     *
     * @param type  the canonical type descriptor
     *
     * @return a {@code DotNetTypeDescriptor} for the specified canonical type
     *         descriptor
     */
    public static DotNetTypeDescriptor fromCanonical(CanonicalTypeDescriptor type)
        {
        return new DotNetTypeDescriptor(type.getNamespaceComponents(), type.getName(),
                                      type.isArray(), fromCanonical(type.getGenericArguments()));
        }

    /**
     * Create a list of {@code DotNetTypeDescriptor}s from a list of canonical
     * type descriptors.
     *
     * @param types  the list of canonical type descriptor
     *
     * @return a list of {@code DotNetTypeDescriptor}s for the specified
     *         list of canonical type descriptors
     */
    public static List<DotNetTypeDescriptor> fromCanonical(List<CanonicalTypeDescriptor> types)
        {
        List<DotNetTypeDescriptor> result = new ArrayList<>(types.size());
        for (CanonicalTypeDescriptor type : types)
            {
            result.add(fromCanonical(type));
            }

        return result;
        }

    // ---- AbstractTypeDescriptor implementation ---------------------------

    @Override
    protected DotNetTypeDescriptor createArrayType(String[] namespace, String name)
        {
        return new DotNetTypeDescriptor(namespace, name, true);
        }

    @Override
    protected Parser getParser()
        {
        return PARSER;
        }

    // ---- inner class: DotNetTypeParser -----------------------------------

    /**
     * An implementation of a .NET type name parser.
     */
    private static class DotNetTypeParser
            extends Parser<DotNetTypeDescriptor>
        {
        // ---- constructors ------------------------------------------------

        /**
         * Construct {@code DotNetTypeParser} instance.
         *
         * @param separator  the namespace separator
         */
        public DotNetTypeParser(String separator)
            {
            super(separator);
            }

        // ---- AbstractTypeDescriptor.Parser implementation ----------------

        @Override
        protected DotNetTypeDescriptor getStandardType(String type)
            {
            return null;
            }

        @Override
        protected DotNetTypeDescriptor createTypeDescriptor(
                String[] namespace, String name, boolean fArray,
                List<DotNetTypeDescriptor> genericArgs)
            {
            return new DotNetTypeDescriptor(namespace, name, fArray, genericArgs);
            }
        }

    // ---- static members --------------------------------------------------

    /**
     * An instance of a .NET type name parser.
     */
    public static final DotNetTypeParser PARSER = new DotNetTypeParser(".");
    }
