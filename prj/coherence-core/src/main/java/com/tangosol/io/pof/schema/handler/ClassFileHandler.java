/*
 * Copyright (c) 2013, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io.pof.schema.handler;

import com.oracle.coherence.common.base.Formatting;

import com.oracle.coherence.common.schema.AbstractPropertyHandler;
import com.oracle.coherence.common.schema.AbstractTypeHandler;
import com.oracle.coherence.common.schema.ExtensibleProperty;
import com.oracle.coherence.common.schema.ExtensibleType;
import com.oracle.coherence.common.schema.PropertyHandler;
import com.oracle.coherence.common.schema.Schema;
import com.oracle.coherence.common.schema.TypeHandler;
import com.oracle.coherence.common.schema.util.AsmUtils;

import com.tangosol.io.pof.DateMode;

import com.tangosol.io.pof.schema.PofArray;
import com.tangosol.io.pof.schema.PofCollection;
import com.tangosol.io.pof.schema.PofDate;
import com.tangosol.io.pof.schema.PofList;
import com.tangosol.io.pof.schema.PofMap;
import com.tangosol.io.pof.schema.PofProperty;
import com.tangosol.io.pof.schema.PofSet;
import com.tangosol.io.pof.schema.PofType;

import com.tangosol.io.pof.schema.annotation.Portable;
import com.tangosol.io.pof.schema.annotation.PortableArray;
import com.tangosol.io.pof.schema.annotation.PortableDate;
import com.tangosol.io.pof.schema.annotation.PortableList;
import com.tangosol.io.pof.schema.annotation.PortableMap;
import com.tangosol.io.pof.schema.annotation.PortableSet;
import com.tangosol.io.pof.schema.annotation.PortableType;

import java.nio.charset.StandardCharsets;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;

import static com.oracle.coherence.common.schema.util.AsmUtils.getAnnotationAttribute;

/**
 * Reads {@link PofType} metadata from a class file.
 *
 * @author as  2013.11.20
 */
public class ClassFileHandler
    {
    // ---- inner class: ClassFileTypeHandler -------------------------------

    /**
     * A {@link TypeHandler} implementation that reads {@link PofType} metadata from
     * a class file.
     */
    public static class ClassFileTypeHandler
            extends AbstractTypeHandler<PofType, ClassNode>
        {
        @Override
        public PofType createType(ExtensibleType parent)
            {
            return new PofType(parent);
            }

        @Override
        public void importType(PofType type, ClassNode source, Schema schema)
            {
            AnnotationNode an = AsmUtils.getAnnotation(source, PortableType.class);
            if (an != null)
                {
                int id = (int) getAnnotationAttribute(an, "id");
                type.setId(id > 0 ? id : Math.abs(Formatting.toCrc(type.getFullName().getBytes(StandardCharsets.UTF_8))));
                type.setVersion((Integer) getAnnotationAttribute(an, "version"));
                }
            }

        }

    // ---- inner class: ClassFilePropertyHandler ---------------------------

    /**
     * A {@link PropertyHandler} implementation that reads {@link PofProperty}
     * metadata from a class file.
     */
    public static class ClassFilePropertyHandler
            extends AbstractPropertyHandler<PofProperty, FieldNode>
        {
        @Override
        public PofProperty createProperty(ExtensibleProperty parent)
            {
            return new PofProperty(parent);
            }

        @Override
        public void importProperty(PofProperty property, FieldNode source, Schema schema)
            {
            property.setName(source.name);

            AnnotationNode an = AsmUtils.getAnnotation(source, PORTABLE_ANNOTATIONS);
            if (an != null)
                {
                property.setSince((Integer) getAnnotationAttribute(an, "since"));
                property.setOrder((Integer) getAnnotationAttribute(an, "order"));

                if (an.desc.endsWith("PortableDate;"))
                    {
                    property.setInfo(new PofDate(
                            (DateMode) getAnnotationAttribute(an, "mode"),
                            (Boolean)  getAnnotationAttribute(an, "includeTimezone")
                    ));
                    }
                if (an.desc.endsWith("PortableArray;"))
                    {
                    property.setInfo(new PofArray(
                            ((Type)   getAnnotationAttribute(an, "elementClass")).getClassName(),
                            (Boolean) getAnnotationAttribute(an, "useRawEncoding")
                    ));
                    }
                if (an.desc.endsWith("PortableCollection;"))
                    {
                    property.setInfo(new PofCollection(
                            ((Type) getAnnotationAttribute(an, "clazz")).getClassName(),
                            ((Type) getAnnotationAttribute(an, "elementClass")).getClassName()
                    ));
                    }
                if (an.desc.endsWith("PortableList;"))
                    {
                    property.setInfo(new PofList(
                            ((Type) getAnnotationAttribute(an, "clazz")).getClassName(),
                            ((Type) getAnnotationAttribute(an, "elementClass")).getClassName()
                    ));
                    }
                if (an.desc.endsWith("PortableSet;"))
                    {
                    property.setInfo(new PofSet(
                            ((Type) getAnnotationAttribute(an, "clazz")).getClassName(),
                            ((Type) getAnnotationAttribute(an, "elementClass")).getClassName()
                    ));
                    }
                if (an.desc.endsWith("PortableMap;"))
                    {
                    property.setInfo(new PofMap(
                            ((Type) getAnnotationAttribute(an, "clazz")).getClassName(),
                            ((Type) getAnnotationAttribute(an, "keyClass")).getClassName(),
                            ((Type) getAnnotationAttribute(an, "valueClass")).getClassName()
                    ));
                    }
                }
            }
        }

    // ---- constants -------------------------------------------------------

    /**
     * An array of all property-level POF annotations.
     */
    private static final Class[] PORTABLE_ANNOTATIONS = new Class[]{
            Portable.class, PortableArray.class, PortableDate.class, PortableList.class, PortableMap.class, PortableSet.class};
    }
