/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.common.schema.lang.java.handler;


import com.oracle.coherence.common.schema.AbstractPropertyHandler;
import com.oracle.coherence.common.schema.AbstractTypeDescriptor;
import com.oracle.coherence.common.schema.AbstractTypeHandler;
import com.oracle.coherence.common.schema.CanonicalTypeDescriptor;
import com.oracle.coherence.common.schema.Schema;
import com.oracle.coherence.common.schema.lang.java.JavaProperty;
import com.oracle.coherence.common.schema.lang.java.JavaType;
import com.oracle.coherence.common.schema.lang.java.JavaTypeDescriptor;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;


/**
 * A container class for a type and property handler that read and write type
 * metadata from/to Java class file.
 *
 * @author as  2013.11.20
 */
public class ClassFileHandler
    {
    // ---- inner class: ClassFileTypeHandler -------------------------------

    /**
     * Java type handler that reads and writes type metadata from/to Java class
     * file.
     */
    public static class ClassFileTypeHandler
            extends AbstractTypeHandler<JavaType, ClassNode>
        {
        @Override
        public void importType(JavaType type, ClassNode source, Schema schema)
            {
            JavaTypeDescriptor jtd = JavaTypeDescriptor.fromInternal(source.name);
            if (jtd.getNamespace() == null || !jtd.isNameEqual(type.getParent().getDescriptor()))
                {
                type.setDescriptor(jtd);
                }
            }
        }

    // ---- inner class: ClassFilePropertyHandler ---------------------------

    /**
     * Java property handler that reads and writes property metadata from/to
     * Java class file.
     */
    public static class ClassFilePropertyHandler
            extends AbstractPropertyHandler<JavaProperty, FieldNode>
        {
        @Override
        public void importProperty(JavaProperty property, FieldNode source, Schema schema)
            {
            CanonicalTypeDescriptor ctd = property.getParent().getType();
            if (ctd != null)
                {
                JavaType javaType = schema.getType(ctd, JavaType.class);
                JavaTypeDescriptor jtd = JavaTypeDescriptor.fromInternal(
                        source.signature != null ? source.signature : source.desc);
                if (javaType == null || jtd.isGenericType())
                    {
                    property.setType(jtd);
                    }
                else
                    {
                    AbstractTypeDescriptor desc =
                            javaType.getDescriptor() == null
                            ? ctd
                            : javaType.getDescriptor();

                    if (!jtd.isNameEqual(desc))
                        {
                        property.setType(jtd);
                        }
                    }
                }
            }
        }
    }
