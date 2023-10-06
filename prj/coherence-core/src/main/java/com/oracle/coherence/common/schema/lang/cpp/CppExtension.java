/*
 * Copyright (c) 2020, 2023 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.common.schema.lang.cpp;


import com.oracle.coherence.common.schema.PropertyHandler;
import com.oracle.coherence.common.schema.SchemaExtension;
import com.oracle.coherence.common.schema.TypeHandler;
import com.oracle.coherence.common.schema.lang.ClassFilePropertyHandler;
import com.oracle.coherence.common.schema.lang.ClassFileTypeHandler;
import com.oracle.coherence.common.schema.lang.XmlPropertyHandler;
import com.oracle.coherence.common.schema.lang.XmlTypeHandler;

import java.util.Arrays;
import java.util.Collection;


/**
 * An implementation of a {@link SchemaExtension} that provides support for C++.
 *
 * @author as  2013.08.28
 */
public class CppExtension
        implements SchemaExtension
    {
    // ---- SchemaExtension implementation ----------------------------------

    @Override
    public String getName()
        {
        return "c++";
        }

    @Override
    public Collection<TypeHandler> getTypeHandlers()
        {
        return Arrays.asList(new TypeHandler[] {
                new CppClassFileTypeHandler(),
                new CppXmlTypeHandler()
        });
        }

    @Override
    public Collection<PropertyHandler> getPropertyHandlers()
        {
        return Arrays.asList(new PropertyHandler[] {
                new CppClassFilePropertyHandler(),
                new CppXmlPropertyHandler()
            });
        }

    // ---- inner class: CppClassFileTypeHandler ----------------------------

    /**
     * C++ type handler that reads and writes type metadata from/to Java class
     * file.
     */
    public static class CppClassFileTypeHandler
            extends ClassFileTypeHandler<CppType, CppTypeDescriptor,
                            com.oracle.coherence.common.schema.lang.cpp.annotation.CppType>
        {
        }

    // ---- inner class: CppClassFilePropertyHandler ------------------------

    /**
     * C++ property handler that reads and writes property metadata from/to Java
     * class file.
     */
    public static class CppClassFilePropertyHandler
            extends ClassFilePropertyHandler<CppProperty, CppTypeDescriptor,
                            com.oracle.coherence.common.schema.lang.cpp.annotation.CppProperty>
        {
        }

    // ---- inner class: CppXmlTypeHandler ----------------------------------

    /**
     * C++ type handler that reads and writes type metadata from/to XML file.
     */
    public static class CppXmlTypeHandler
            extends XmlTypeHandler<CppType, CppTypeDescriptor>
        {
        public CppXmlTypeHandler()
            {
            super(NS);
            }
        }

    // ---- inner class: CppXmlPropertyHandler ------------------------------

    /**
     * C++ property handler that reads and writes property metadata from/to
     * XML file.
     */
    public static class CppXmlPropertyHandler
            extends XmlPropertyHandler<CppProperty, CppTypeDescriptor>
        {
        public CppXmlPropertyHandler()
            {
            super(NS);
            }
        }

    // ---- static members --------------------------------------------------

    /**
     * The XML namespace handled by {@link CppXmlTypeHandler} and
     * {@link CppXmlPropertyHandler}.
     */
    private static final String NS = "http://xmlns.oracle.com/coherence/schema/cpp";
    }
