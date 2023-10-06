/*
 * Copyright (c) 2020, 2023 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.common.schema.lang.dotnet;


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
 * An implementation of a {@link SchemaExtension} that provides support for the
 * .NET platform.
 *
 * @author as  2013.08.28
 */
public class DotNetExtension
        implements SchemaExtension
    {
    // ---- SchemaExtension implementation ----------------------------------

    @Override
    public String getName()
        {
        return ".net";
        }

    @Override
    public Collection<TypeHandler> getTypeHandlers()
        {
        return Arrays.asList(new TypeHandler[] {
                new DotNetClassFileTypeHandler(),
                new DotNetXmlTypeHandler()
            });
        }

    @Override
    public Collection<PropertyHandler> getPropertyHandlers()
        {
        return Arrays.asList(new PropertyHandler[] {
                new DotNetClassFilePropertyHandler(),
                new DotNetXmlPropertyHandler()
            });
        }

    // ---- inner class: DotNetClassFileTypeHandler -------------------------

    /**
     * .NET type handler that reads and writes type metadata from/to Java class
     * file.
     */
    public static class DotNetClassFileTypeHandler
            extends ClassFileTypeHandler<DotNetType, DotNetTypeDescriptor,
                    com.oracle.coherence.common.schema.lang.dotnet.annotation.DotNetType>
        {
        }

    // ---- inner class: DotNetClassFilePropertyHandler ---------------------

    /**
     * .NET property handler that reads and writes property metadata from/to
     * Java class file.
     */
    public static class DotNetClassFilePropertyHandler
            extends ClassFilePropertyHandler<DotNetProperty, DotNetTypeDescriptor,
                    com.oracle.coherence.common.schema.lang.dotnet.annotation.DotNetProperty>
        {
        }

    // ---- inner class: DotNetXmlTypeHandler -------------------------------

    /**
     * .NET type handler that reads and writes type metadata from/to XML file.
     */
    public static class DotNetXmlTypeHandler
            extends XmlTypeHandler<DotNetType, DotNetTypeDescriptor>
        {
        public DotNetXmlTypeHandler()
            {
            super(NS);
            }
        }

    // ---- inner class: DotNetXmlPropertyHandler ---------------------------

    /**
     * .NET property handler that reads and writes property metadata from/to
     * XML file.
     */
    public static class DotNetXmlPropertyHandler
            extends XmlPropertyHandler<DotNetProperty, DotNetTypeDescriptor>
        {
        public DotNetXmlPropertyHandler()
            {
            super(NS);
            }
        }

    // ---- static members --------------------------------------------------

    /**
     * The XML namespace handled by {@link DotNetXmlTypeHandler} and
     * {@link DotNetXmlPropertyHandler}.
     */
    private static final String NS = "http://xmlns.oracle.com/coherence/schema/dotnet";
    }
