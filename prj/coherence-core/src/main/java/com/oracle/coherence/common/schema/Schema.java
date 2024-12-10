/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.common.schema;


import com.oracle.coherence.common.schema.lang.cpp.CppExtension;
import com.oracle.coherence.common.schema.lang.dotnet.DotNetExtension;
import com.oracle.coherence.common.schema.lang.java.JavaExtension;
import com.oracle.coherence.common.schema.lang.java.JavaType;
import com.oracle.coherence.common.schema.util.ResourceLoader;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

import static com.oracle.coherence.common.base.Classes.getContextClassLoader;


/**
 * Represents an instance of a {@code Schema} containing a collection of
 * {@link ExtensibleType}s.
 * <p/>
 * Most applications will typically use only one instance of a schema, but it is
 * possible to create multiple schema instances, where each schema instance
 * defines an independent type system.
 * <p/>
 * {@code Schema} instances are created using a {@link SchemaBuilder} configured
 * with a set of {@link SchemaSource}s to use. For example, the Maven plugin
 * used to add serialization code to classes marked with PortableType annotation defines and
 * populates the schema as follows:
 * <pre>
 * Schema schema = new SchemaBuilder()
 *         .addSchemaSource(
 *                 new ClassFileSchemaSource()
 *                         .withClassesFromDirectory(outputDirectory)
 *                         .withTypeFilter(hasAnnotation(PortableType.class))
 *         )
 *         .build();
 * </pre>
 * In his example, we are only interested in compiled classes from the output
 * directory that are annotated with {@code @PortableType} annotation, so we use
 * {@link ClassFileSchemaSource} and the appropriate type filter to select only
 * those classes for inclusion into the schema.
 * <p/>
 * The {@code Schema} is <i>extensible</i>, meaning that each type within a schema
 * can be decorated with additional metadata using {@link SchemaExtension}s.
 * <p/>
 * There are three built-in schema extensions: {@link JavaExtension}, {@link
 * DotNetExtension} and {@link CppExtension}, which add the metadata necessary
 * to represent schema types in Java, .NET and C++, and users are free to
 * implement their own extensions in order to add the metadata they need.
 * <p/>
 * For example, Coherence provides a custom com.oracle.common.io.pof.schema.PofExtension,
 * which adds the metadata necessary for POF serialization to a subset of types
 * within the schema.
 * 
 * @author as  2013.06.21
 *
 * @see SchemaBuilder
 * @see SchemaSource
 * @see ExtensibleType
 * @see SchemaExtension
 */
public class Schema implements Iterable<ExtensibleType>
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Construct a {@code Schema} instance.
     * <p/>
     * This constructor will automatically register all of the built-in extensions
     * ({@link JavaExtension}, {@link DotNetExtension} and {@link CppExtension}),
     * as well as all {@link SchemaExtension} implementations that can be found
     * in the classpath using Java {@link ServiceLoader} mechanism (which, in the
     * case of Coherence, includes com.oracle.common.io.pof.schema.PofExtension).
     *
     * @param typeMap  the map that should be used to store {@link ExtensibleType}s.
     *                 If {@code null}, the {@code LinkedHashMap} will be used.
     */
    protected Schema(Map<String, ExtensibleType> typeMap)
        {
        m_typeMap = typeMap == null
                    ? new LinkedHashMap<>()
                    : typeMap;

        // register built-in extensions
        registerExtension(new JavaExtension());
        registerExtension(new DotNetExtension());
        registerExtension(new CppExtension());

        // register available custom extensions from the classpath
        ServiceLoader<SchemaExtension> loader =
                ServiceLoader.load(SchemaExtension.class, getContextClassLoader());
        for (SchemaExtension ext : loader)
            {
            registerExtension(ext);
            }

        // load all META-INF/schema.xml resources from the classpath
        ResourceLoader resourceLoader =
                ResourceLoader.load("META-INF/schema.xml", getContextClassLoader());
        for (InputStream in : resourceLoader)
            {
            XmlSchemaSource ts = new XmlSchemaSource(in);
            ts.populateSchema(this);
            }
        }

    // ---- Visitor pattern implementation ----------------------------------

    /**
     * An acceptor for the {@link SchemaVisitor}.
     *
     * @param visitor  the visitor
     */
    public void accept(SchemaVisitor visitor)
        {
        for (Type type : this)
            {
            type.accept(visitor);
            }
        }

    // ---- Iterable implementation -----------------------------------------

    /**
     * Return an iterator over {@code ExtensibleType}s contained in this schema.
     *
     * @return an Iterator over {@code ExtensibleType}s contained in this schema
     */
    public Iterator<ExtensibleType> iterator()
        {
        return Collections.unmodifiableCollection(m_typeMap.values()).iterator();
        }

    // ---- public methods --------------------------------------------------

    /**
     * Get {@code ExtensibleType} for the specified type descriptor.
     *
     * @param td  a canonical type descriptor
     *
     * @return the {@code ExtensibleType} associated with the specified type
     *         descriptor, or {@code null} if the type for the specified type
     *         descriptor does not exist in this schema
     */
    public ExtensibleType getType(CanonicalTypeDescriptor td)
        {
        return getType(td.getFullName());
        }

    /**
     * Get {@code ExtensibleType} for the specified canonical type name.
     *
     * @param fullName  a full, canonical type name
     *
     * @return the {@code ExtensibleType} associated with the specified type
     *         name, or {@code null} if the type for the specified type
     *         name does not exist in this schema
     */
    public ExtensibleType getType(String fullName)
        {
        return m_typeMap.get(fullName);
        }

    /**
     * Get {@code Type} extension for the specified type descriptor and
     * extension class.
     *
     * @param td     a canonical type descriptor
     * @param clazz  the class of the type extension to return
     *
     * @return the {@code Type} extension associated with the specified type
     *         descriptor; {@code null} if the type for the specified type
     *         descriptor, or the extension for the specified class does not
     *         exist in this schema
     */
    public <T extends Type> T getType(CanonicalTypeDescriptor td, Class<T> clazz)
        {
        return getType(td.getFullName(), clazz);
        }

    /**
     * Get {@code Type} extension for the specified canonical type name and
     * extension class.
     *
     * @param fullName  a full, canonical type name
     * @param clazz     the class of the type extension to return
     *
     * @return the {@code Type} extension associated with the specified type
     *         descriptor; {@code null} if the type for the specified type
     *         descriptor, or the extension for the specified class does not
     *         exist in this schema
     */
    public <T extends Type> T getType(String fullName, Class<T> clazz)
        {
        return m_typeMap.get(fullName).getExtension(clazz);
        }

    /**
     * Return {@code true} if the type associated with the specified type
     * descriptor exists in this schema.
     *
     * @param td  a canonical type descriptor
     *
     * @return {@code true} if the type associated with the specified type
     *         descriptor exists in this schema, {@code false} otherwise
     */
    public boolean containsType(CanonicalTypeDescriptor td)
        {
        return containsType(td.getFullName());
        }

    /**
     * Return {@code true} if the type associated with the specified canonical
     * type name exists in this schema.
     *
     * @param fullName  a full, canonical type name
     *
     * @return {@code true} if the type associated with the specified canonical
     *         type name exists in this schema, {@code false} otherwise
     */
    public boolean containsType(String fullName)
        {
        return m_typeMap.containsKey(fullName);
        }

    /**
     * Add type to this schema.
     *
     * @param type  the type to add
     */
    public void addType(ExtensibleType type)
        {
        m_typeMap.put(type.getFullName(), type);
        }

    /**
     * A convenience method to find {@code ExtensibleType} based on its Java
     * type name
     *
     * @param name  a fully-qualified Java type name
     *
     * @return the {@code ExtensibleType} associated with the specified Java type
     *         name, or {@code null} if the type for the specified Java type
     *         name does not exist in this schema
     */
    public ExtensibleType findTypeByJavaName(String name)
        {
        for (ExtensibleType type : m_typeMap.values())
            {
            JavaType javaType = type.getExtension(JavaType.class);
            if (javaType != null
                && (name.equals(javaType.getFullName())
                    || (javaType.getWrapperType() != null && name.equals(javaType.getWrapperType().getFullName()))
                    || javaType.implementsInterface(name)))
                {
                return type;
                }
            }

        return null;
        }

    // ---- helper methods --------------------------------------------------

    /**
     * Register schema extension with this schema.
     *
     * @param extension  the schema extension to register
     */
    protected synchronized void registerExtension(SchemaExtension extension)
        {
        m_extensions.add(extension.getName());

        extension.getTypeHandlers().forEach(this::registerTypeHandler);
        extension.getPropertyHandlers().forEach(this::registerPropertyHandler);
        }

    /**
     * Register type handler with this schema.
     *
     * @param handler  the type handler to register
     */
    protected synchronized void registerTypeHandler(TypeHandler handler)
        {
        final String name = handler.getExternalTypeClass().getName();
        m_typeHandlers.computeIfAbsent(name, k -> new HashSet<>()).add(handler);
        }

    /**
     * Register property handler with this schema.
     *
     * @param handler  the property handler to register
     */
    protected synchronized void registerPropertyHandler(PropertyHandler handler)
        {
        final String name = handler.getExternalPropertyClass().getName();
        m_propertyHandlers.computeIfAbsent(name, k -> new HashSet<>()).add(handler);
        }

    /**
     * Return the type handlers for the specified external class.
     *
     * @param external  the external class to get the type handlers for
     *
     * @return the type handlers for the specified external class
     */
    public Set<TypeHandler> getTypeHandlers(Class external)
        {
        return m_typeHandlers.getOrDefault(external.getName(), Collections.emptySet());
        }

    /**
     * Return the property handlers for the specified external class.
     *
     * @param external  the external class to get the property handlers for
     *
     * @return the property handlers for the specified external class
     */
    public Set<PropertyHandler> getPropertyHandlers(Class external)
        {
        return m_propertyHandlers.getOrDefault(external.getName(), Collections.emptySet());
        }

    // ---- data members ----------------------------------------------------

    /**
     * The map of extensible types within this schema, keyed by full canonical
     * type name.
     */
    protected Map<String, ExtensibleType> m_typeMap;

    /**
     * The set of schema extensions registered with this schema.
     */
    protected Set<String> m_extensions = new HashSet<>();

    /**
     * The map of type handlers provided by the registered schema extensions,
     * keyed by external type name.
     */
    protected Map<String, Set<TypeHandler>> m_typeHandlers = new LinkedHashMap<>();

    /**
     * The map of propery handlers provided by the registered schema extensions,
     * keyed by external property type name.
     */
    protected Map<String, Set<PropertyHandler>> m_propertyHandlers = new LinkedHashMap<>();
    }
