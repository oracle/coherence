/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.common.schema.lang.java;


import com.oracle.coherence.common.schema.PropertyHandler;
import com.oracle.coherence.common.schema.Schema;
import com.oracle.coherence.common.schema.SchemaExtension;
import com.oracle.coherence.common.schema.TypeHandler;
import com.oracle.coherence.common.schema.lang.XmlPropertyHandler;
import com.oracle.coherence.common.schema.lang.XmlTypeHandler;
import com.oracle.coherence.common.schema.lang.java.handler.ClassFileHandler;
import com.oracle.coherence.common.schema.util.StringUtils;
import java.util.Arrays;
import java.util.Collection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;


/**
 * An implementation of a {@link SchemaExtension} that provides support for the
 * Java platform.
 *
 * @author as  2013.08.28
 */
public class JavaExtension implements SchemaExtension
    {
    // ---- SchemaExtension implementation ----------------------------------

    @Override
    public String getName()
        {
        return "java";
        }

    @Override
    public Collection<TypeHandler> getTypeHandlers()
        {
        return Arrays.asList(new TypeHandler[] {
                new ClassFileHandler.ClassFileTypeHandler(),
                new JavaXmlTypeHandler()
            });
        }

    @Override
    public Collection<PropertyHandler> getPropertyHandlers()
        {
        return Arrays.asList(new PropertyHandler[] {
                new ClassFileHandler.ClassFilePropertyHandler(),
                new JavaXmlPropertyHandler()
            });
        }

    // ---- inner class: JavaXmlTypeHandler ---------------------------------

    /**
     * Java type handler that reads and writes type metadata from/to XML file.
     */
    public static class JavaXmlTypeHandler
            extends XmlTypeHandler<JavaType, JavaTypeDescriptor>
        {
        public JavaXmlTypeHandler()
            {
            super(NS);
            }

        @Override
        protected void importTypeInternal(JavaType type, Element source, Element xmlType, Schema schema)
            {
            String wrapper = xmlType.getAttribute("wrapper");
            if (!StringUtils.isEmpty(wrapper))
                {
                type.setWrapperType(parseTypeName(wrapper));
                }
            }

        @Override
        protected void exportTypeInternal(JavaType type, Document doc, Element target, Element xmlType, Schema schema)
            {
            if (type.getWrapperType() != null)
                {
                xmlType.setAttribute("wrapper", type.getWrapperType().getFullName());
                }
            }
        }

    // ---- inner class: JavaXmlPropertyHandler -----------------------------

    /**
     * Java property handler that reads and writes property metadata from/to
     * XML file.
     */
    public static class JavaXmlPropertyHandler
            extends XmlPropertyHandler<JavaProperty, JavaTypeDescriptor>
        {
        public JavaXmlPropertyHandler()
            {
            super(NS);
            }
        }

    // ---- static members --------------------------------------------------

    /**
     * The XML namespace handled by {@link JavaXmlTypeHandler} and
     * {@link JavaXmlPropertyHandler}.
     */
    private static final String NS = "http://xmlns.oracle.com/coherence/schema/java";
    }
