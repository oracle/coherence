/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.common.schema;


import com.oracle.coherence.common.schema.lang.java.JavaProperty;
import com.oracle.coherence.common.schema.lang.java.JavaType;
import com.oracle.coherence.common.schema.lang.java.JavaTypeDescriptor;
import com.oracle.coherence.common.schema.util.CapitalizationTransformer;
import com.oracle.coherence.common.schema.util.NameTransformer;
import com.oracle.coherence.common.schema.util.StringUtils;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JPackage;
import com.sun.codemodel.JVar;
import com.sun.codemodel.writer.SingleStreamCodeWriter;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 * A {@link SchemaExporter} implementation that creates Java source files for
 * the types defined in the schema.
 *
 * @author as  2013.12.02
 */
@SuppressWarnings("unchecked")
public class JavaSourceSchemaExporter
        extends AbstractSchemaExporter<JDefinedClass, JavaSourceSchemaExporter.JProperty>
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Construct {@code JavaSourceSchemaExporter} instance that will write schema
     * metadata to the console ({@code System.out}).
     */
    public JavaSourceSchemaExporter()
        {
        this(System.out);
        }

    /**
     * Construct {@code JavaSourceSchemaExporter} instance.
     *
     * @param outputStream  the output stream to write schema metadata to
     */
    public JavaSourceSchemaExporter(OutputStream outputStream)
        {
        m_outputStream = outputStream;
        }

    // ---- SchemaExporter implementation -----------------------------------

    @Override
    public void export(Schema schema)
        {
        try
            {
            JCodeModel cm = new JCodeModel();

            for (ExtensibleType t : schema)
                {
                if (t.getNamespace() != null)
                    {
                    JavaType javaType = t.getExtension(JavaType.class);
                    System.out.println("Generating " + javaType.getFullName());

                    String[] classNames = StringUtils.split(javaType.getName(), "$");

                    String outerClassName = javaType.getNamespace() + "." + classNames[0];
                    JDefinedClass clazz = cm._getClass(outerClassName);
                    if (clazz == null)
                        {
                        clazz = cm._class(outerClassName);
                        }
                    for (int i = 1; i < classNames.length; i++)
                        {
                        JDefinedClass nestedClass = getNestedClass(clazz, classNames[i]);
                        if (nestedClass == null)
                            {
                            nestedClass = clazz._class(JMod.PUBLIC | JMod.STATIC, classNames[i]);
                            }
                        clazz = nestedClass;
                        }
                    exportType(t, clazz, schema);

                    for (TypeHandler handler : schema.getTypeHandlers(getExternalTypeClass()))
                        {
                        Type ext = t.getExtension(handler.getInternalTypeClass());
                        handler.exportType(ext, clazz, schema);
                        }

                    for (ExtensibleProperty p : t.getProperties())
                        {
                        JProperty property = JProperty.create(clazz, p, schema);
                        exportProperty(p, property, schema);

                        for (PropertyHandler handler : schema.getPropertyHandlers(getExternalPropertyClass()))
                            {
                            Property ext = p.getExtension(handler.getInternalPropertyClass());
                            handler.exportProperty(ext, property, schema);
                            }
                        }
                    }
                }

            Iterator<JPackage> itp = cm.packages();
            while (itp.hasNext())
                {
                JPackage p = itp.next();
                System.out.println("package " + p.name());
                Iterator<JDefinedClass> itc = p.classes();
                while (itc.hasNext())
                    {
                    JDefinedClass c = itc.next();
                    System.out.println("\tclass " + c.fullName());
                    Iterator<JDefinedClass> itc2 = c.classes();
                    while (itc2.hasNext())
                        {
                        JDefinedClass c2 = itc2.next();
                        System.out.println("\t\tclass " + c2.fullName());
                        }
                    }
                }
            cm.build(new SingleStreamCodeWriter(m_outputStream));
            }
        catch (Exception e)
            {
            throw new RuntimeException(e);
            }
        }

    private JDefinedClass getNestedClass(JDefinedClass outerClass, String className)
        {
        Iterator<JDefinedClass> it = outerClass.classes();
        while (it.hasNext())
            {
            JDefinedClass clazz = it.next();
            if (clazz.name().equals(className))
                {
                return clazz;
                }
            }

        return null;
        }

    // ---- TypeHandler implementation --------------------------------------

    @Override
    public Class<JDefinedClass> getExternalTypeClass()
        {
        return JDefinedClass.class;
        }

    @Override
    public void exportType(ExtensibleType type, JDefinedClass target, Schema schema)
        {
        if (type.getBase() != null)
            {
            JavaType baseType = schema.getType(type.getBase(), JavaType.class);
            target._extends(target.owner().ref(baseType.getFullName()));
            }

        for (CanonicalTypeDescriptor td : type.getInterfaces())
            {
            JavaType intfType = schema.getType(td, JavaType.class);
            target._implements(target.owner().ref(intfType.getFullName()));
            }
        }

    // ---- PropertyHandler implementation ----------------------------------

    @Override
    public Class<JProperty> getExternalPropertyClass()
        {
        return JProperty.class;
        }

    @Override
    public void exportProperty(ExtensibleProperty property, JProperty target, Schema schema)
        {
        }

    // ---- Inner class: JProperty ------------------------------------------

    public static class JProperty
        {
        private JProperty(JFieldVar field, JMethod getter, JMethod setter)
            {
            m_field  = field;
            m_getter = getter;
            m_setter = setter;
            }

        public static JProperty create(JDefinedClass clazz, ExtensibleProperty p, Schema schema)
            {
            String fieldName = FIRST_LOWER.transform(p.getName());
            String getterName =
                    (CanonicalTypeDescriptor.BOOLEAN.equals(p.getType())
                    ? "is" : "get") + FIRST_UPPER.transform(p.getName());
            String setterName = "set" + FIRST_UPPER.transform(p.getName());

            JavaProperty jp = p.getExtension(JavaProperty.class);
            JavaTypeDescriptor jtd = jp.resolveType(schema);

            JClass type = clazz.owner().ref(jtd.getFullName().replace("$", "."));
            if (jtd.isGenericType())
                {
                type = type.narrow(resolveGenericParameters(clazz.owner(), jtd.getGenericArguments()));
                }

            JFieldVar field = clazz.field(JMod.PRIVATE, type, "m_" + fieldName);

            JMethod getter = clazz.method(JMod.PUBLIC, type, getterName);
            getter.body()._return(field);

            JMethod setter = clazz.method(JMod.PUBLIC, void.class, setterName);
            JVar setterParam = setter.param(type, fieldName);
            setter.body().assign(JExpr._this().ref(field), setterParam);

            return new JProperty(field, getter, setter);
            }

        private static List<JClass> resolveGenericParameters(JCodeModel cm, List<JavaTypeDescriptor> genericArguments)
            {
            List<JClass> params = new ArrayList<>(genericArguments.size());
            for (JavaTypeDescriptor arg : genericArguments)
                {
                params.add(cm.ref(arg.getFullName()));
                }
            return params;
            }

        public JFieldVar field()
            {
            return m_field;
            }

        public JMethod getter()
            {
            return m_getter;
            }

        public JMethod setter()
            {
            return m_setter;
            }

        private JFieldVar m_field;
        private JMethod m_getter;
        private JMethod m_setter;
        }

    // ---- static members --------------------------------------------------

    private static NameTransformer FIRST_LOWER =
            new CapitalizationTransformer(CapitalizationTransformer.Mode.FIRST_LOWER);
    private static NameTransformer FIRST_UPPER =
            new CapitalizationTransformer(CapitalizationTransformer.Mode.FIRST_UPPER);

    // ---- data members ----------------------------------------------------

    private OutputStream m_outputStream;
    }
