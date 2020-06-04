/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.common.schema;


import com.oracle.coherence.common.base.Classes;
import com.oracle.coherence.common.schema.lang.java.JavaTypeDescriptor;
import com.oracle.coherence.common.schema.util.NameTransformer;
import com.oracle.coherence.common.schema.util.NameTransformerChain;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;


/**
 * A {@link SchemaSource} implementation that reads type and property metadata
 * from a compiled Java class file using ASM.
 * 
 * @author as  2013.06.26
 */
@SuppressWarnings("unchecked")
public class ClassFileSchemaSource
        extends AbstractSchemaSource<ClassNode, FieldNode>
    {
    /**
     * The logger object to use.
     */
    private static final Logger LOG = Logger.getLogger(ClassFileSchemaSource.class.getName());

    // ---- constructors ----------------------------------------------------

    /**
     * Construct a {@link ClassFileSchemaSource} instance.
     */
    public ClassFileSchemaSource()
        {
        }

    // ---- fluent API ------------------------------------------------------

    /**
     * Add the directory on the file system to read the class files from.
     *
     * @param directoryName  the name of the directory on the file system to
     *                       read the class files from
     *
     * @return this {@code ClassFileSchemaSource}
     */
    public ClassFileSchemaSource withClassesFromDirectory(String directoryName)
        {
        return withClassesFromDirectory(new File(directoryName));
        }

    /**
     * Add the directory on the file system to read the class files from.
     *
     * @param directoryPath  the path of the directory on the file system to
     *                       read the class files from
     *
     * @return this {@code ClassFileSchemaSource}
     */
    public ClassFileSchemaSource withClassesFromDirectory(Path directoryPath)
        {
        return withClassesFromDirectory(directoryPath.toFile());
        }

    /**
     * Add the directory on the file system to read the class files from.
     *
     * @param directory  the directory on the file system to read the class
     *                   files from
     *
     * @return this {@code ClassFileSchemaSource}
     */
    public ClassFileSchemaSource withClassesFromDirectory(File directory)
        {
        m_files.add(directory);
        return this;
        }

    /**
     * Add the JAR file on the file system to read the class files from.
     *
     * @param jarFileName  the name of the JAR file on the file system to read
     *                     the class files from
     *
     * @return this {@code ClassFileSchemaSource}
     */
    public ClassFileSchemaSource withClassesFromJarFile(String jarFileName)
        {
        return withClassesFromJarFile(new File(jarFileName));
        }

    /**
     * Add the JAR file on the file system to read the class files from.
     *
     * @param jarFilePath  the path of the JAR file on the file system to read
     *                     the class files from
     *
     * @return this {@code ClassFileSchemaSource}
     */
    public ClassFileSchemaSource withClassesFromJarFile(Path jarFilePath)
        {
        return withClassesFromJarFile(jarFilePath.toFile());
        }

    /**
     * Add the JAR file on the file system to read the class files from.
     *
     * @param jarFile  the JAR file on the file system to read the class files
     *                 from
     *
     * @return this {@code ClassFileSchemaSource}
     */
    public ClassFileSchemaSource withClassesFromJarFile(File jarFile)
        {
        m_files.add(jarFile);
        return this;
        }

    /**
     * Add the individual class file on the file system to import.
     *
     * @param fileName  the name of the class file on the file system to import
     *
     * @return this {@code ClassFileSchemaSource}
     */
    public ClassFileSchemaSource withClassFile(String fileName)
        {
        return withClassFile(new File(fileName));
        }

    /**
     * Add the individual class file on the file system to import.
     *
     * @param filePath  the path of the class file on the file system to import
     *
     * @return this {@code ClassFileSchemaSource}
     */
    public ClassFileSchemaSource withClassFile(Path filePath)
        {
        return withClassFile(filePath.toFile());
        }

    /**
     * Add the individual class file on the file system to import.
     *
     * @param file  the class file on the file system to import
     *
     * @return this {@code ClassFileSchemaSource}
     */
    public ClassFileSchemaSource withClassFile(File file)
        {
        m_files.add(file);
        return this;
        }

    /**
     * Set the filter that should be used to determine whether the type should
     * be imported.
     *
     * @param typeFilter  the filter that should be used to determine whether
     *                    the type should be imported
     *
     * @return this {@code ClassFileSchemaSource}
     */
    public ClassFileSchemaSource withTypeFilter(Predicate<ClassNode> typeFilter)
        {
        m_typeFilter = typeFilter;
        return this;
        }

    /**
     * Set the filter that should be used to determine whether the property
     * should be imported.
     *
     * @param propertyFilter  the filter that should be used to determine
     *                        whether the type should be imported
     *
     * @return this {@code ClassFileSchemaSource}
     */
    public ClassFileSchemaSource withPropertyFilter(Predicate<FieldNode> propertyFilter)
        {
        m_propertyFilter = m_propertyFilter.and(propertyFilter);
        return this;
        }

    /**
     * Set the {@link NameTransformer} that should be used to transform the
     * namespace name.
     *
     * @param transformer  the {@link NameTransformer} that should be used to
     *                     transform the namespace name
     *
     * @return this {@code ClassFileSchemaSource}
     */
    public ClassFileSchemaSource withNamespaceTransformer(NameTransformer transformer)
        {
        m_namespaceTransformer = transformer;
        return this;
        }

    /**
     * Set the {@link NameTransformer} that should be used to transform the
     * class name.
     *
     * @param transformer  the {@link NameTransformer} that should be used to
     *                     transform the class name
     *
     * @return this {@code ClassFileSchemaSource}
     */
    public ClassFileSchemaSource withClassNameTransformer(NameTransformer transformer)
        {
        m_classNameTransformer = transformer;
        return this;
        }

    /**
     * Set the {@link NameTransformer} that should be used to transform the
     * property names.
     *
     * @param transformer  the {@link NameTransformer} that should be used to
     *                     transform the property names
     *
     * @return this {@code ClassFileSchemaSource}
     */
    public ClassFileSchemaSource withPropertyNameTransformer(NameTransformer transformer)
        {
        m_propertyNameTransformer = transformer;
        return this;
        }

    /**
     * Treat properties that cannot be found in the schema as Object.
     *
     * @return this {@code ClassFileSchemaSource}
     */
    public ClassFileSchemaSource withMissingPropertiesAsObject()
        {
        m_fMissingPropsAsObject = true;
        return this;
        }

    // ---- SchemaSource implementation -------------------------------------

    @Override
    public void populateSchema(Schema schema)
        {
        try
            {
            // do two passes in order to ensure that all types are defined
            // before attempting to resolve base types, property types and interfaces
            for (int i = 1; i <= 2; i++)
                {
                m_nPass = i;

                for (File file : m_files)
                    {
                    if (file.isDirectory())
                        {
                        populateSchemaFromDirectory(schema, file);
                        }
                    else if (file.getName().endsWith(".jar"))
                        {
                        populateSchemaFromJarFile(schema, file);
                        }
                    else if (file.getName().endsWith(".class"))
                        {
                        populateSchemaFromFile(schema, file);
                        }
                    else
                        {
                        LOG.finer("Ignoring " + file + ". Unknown file type.");
                        }
                    }
                }
            }
        catch (IOException e)
            {
            throw new RuntimeException(e);
            }
        }

    // ---- helper methods --------------------------------------------------

    /**
     * Populate schema by iterating over classes within a file system directory
     * and its sub-directories.
     *
     * @param schema     the schema to populate
     * @param directory  the directory to search for class files
     *
     * @throws IOException  if an error occurs
     */
    protected void populateSchemaFromDirectory(Schema schema, File directory)
            throws IOException
        {
        LOG.finer("Populating schema from class files in " + directory);
        if (!directory.exists())
            {
            throw new IllegalArgumentException(
                    "Specified path [" + directory.getAbsolutePath()
                    + "] does not exist");
            }

        File[] files = directory.listFiles();
        if (files != null)
            {
            for (File file : files)
                {
                if (isPass(1))
                    {
                    LOG.finer("Processing " + file.getAbsolutePath());
                    }

                if (file.isDirectory())
                    {
                    populateSchemaFromDirectory(schema, file);
                    }
                else if (file.getName().endsWith(".class"))
                    {
                    populateSchemaFromFile(schema, file);
                    }
                }
            }
        }

    /**
     * Populate schema by iterating over classes within a JAR file.
     *
     * @param schema   the schema to populate
     * @param jarFile  the JAR file to search for class files
     *
     * @throws IOException  if an error occurs
     */
    protected void populateSchemaFromJarFile(Schema schema, File jarFile)
            throws IOException
        {
        LOG.finer("Populating schema from JAR file " + jarFile);

        InputStream jarIn;
        if (jarFile.exists())
            {
            jarIn = new FileInputStream(jarFile);
            }
        else
            {
            String sJarFileName = jarFile.toString();
            if (s_isWindows)
                {
                sJarFileName = sJarFileName.replace('\\', '/');
                }

            jarIn = Classes.getContextClassLoader(this).getResourceAsStream(sJarFileName);
            }

        if (jarIn != null)
            {
            try (ZipInputStream zipIn = new ZipInputStream(jarIn))
                {
                for (ZipEntry entry = zipIn.getNextEntry(); entry != null; entry = zipIn.getNextEntry())
                    {
                    String name = entry.getName();

                    if (name.endsWith(".class") && !"module-info.class".equals(name))
                        {
                        if (isPass(1))
                            {
                            LOG.finer("Processing " + name);
                            }
                        populateSchema(schema, zipIn);
                        }
                    }
                }
            }
        else
            {
            throw new IllegalArgumentException(
                    "Specified JAR file [" + jarFile.getAbsolutePath()
                    + "] does not exist");
            }
        }

    /**
     * Populate schema by reading type information from a specified class file.
     *
     * @param schema  the schema to populate
     * @param file    the class file to read type information from
     *
     * @throws IOException  if an error occurs
     */
    protected void populateSchemaFromFile(Schema schema, File file)
            throws IOException
        {
        if (file.exists())
            {
            try (FileInputStream in = new FileInputStream(file))
                {
                populateSchema(schema, in);
                }
            }
        else
            {
            String sFilename = file.toString();
            if (s_isWindows)
                {
                sFilename = sFilename.replace('\\', '/');
                }

            try (InputStream in = Classes.getContextClassLoader(this).getResourceAsStream(sFilename))
                {
                if (in != null)
                    {
                    populateSchema(schema, in);
                    }
                else if (isPass(1))
                    {
                    LOG.finer("Skipping non-existent file " + file);
                    }
                }
            }
        }

    /**
     * Populate schema by reading type information from a specified input stream
     * representing a single class file.
     *
     * @param schema  the schema to populate
     * @param in      the input stream representing a class file to read type
     *                information from
     *
     * @throws IOException  if an error occurs
     */
    protected void populateSchema(Schema schema, InputStream in)
            throws IOException
        {
        ClassReader reader = new ClassReader(in);
        ClassNode source = new ClassNode();
        reader.accept(source, 0);

        if (m_typeFilter.test(source))
            {
            JavaTypeDescriptor jtd  = JavaTypeDescriptor.fromInternal(source.name);
            ExtensibleType     type = schema.findTypeByJavaName(jtd.getFullName());
            if (type == null)
                {
                if (isPass(1))
                    {
                    type = new ExtensibleType();
                    CanonicalTypeDescriptor ctd = CanonicalTypeDescriptor.from(jtd, schema);
                    if (m_namespaceTransformer != null)
                        {
                        ctd.setNamespace(m_namespaceTransformer.transform(ctd.getNamespaceComponents()));
                        }
                    if (m_classNameTransformer != null)
                        {
                        ctd.setName(m_classNameTransformer.transform(ctd.getName()));
                        }
                    type.setDescriptor(ctd);
                    }
                else
                    {
                    throw new IllegalStateException("Type " + jtd.getFullName() +
                            " should have been added to the schema during the first pass");
                    }
                }
            populateTypeInternal(schema, type, source);
            schema.addType(type);
            if (isPass(2))
                {
                LOG.finer("Added type " + type.getFullName() + " to the schema");
                }
            }
        }

    /**
     * Return {@code true} if the current pass number is equal to the specified
     * expected pass number.
     *
     * @param nPass  the expected pass number
     *
     * @return {@code true} if the current pass number is equal to the specified
     *         expected pass number, {@code false} otherwise
     */
    protected boolean isPass(int nPass)
        {
        return m_nPass == nPass;
        }

    // ---- AbstractSchemaSource implementation -------------------------------

    @Override
    protected String getPropertyName(FieldNode source)
        {
        return m_propertyNameTransformer == null
               ? source.name
               : m_propertyNameTransformer.transform(source.name);
        }

    @Override
    protected Collection<FieldNode> getProperties(ClassNode source)
        {
        List<FieldNode> fields = source.fields;

        return fields.stream()
                .filter(m_propertyFilter)
                .collect(Collectors.toList());
        }

    // ---- TypeHandler implementation --------------------------------------

    @Override
    public Class<ClassNode> getExternalTypeClass()
        {
        return ClassNode.class;
        }

    @Override
    public void importType(ExtensibleType type, ClassNode source, Schema schema)
        {
        if (isPass(2))
            {
            JavaTypeDescriptor jtd = JavaTypeDescriptor.fromInternal(source.superName);
            if (jtd != JavaTypeDescriptor.OBJECT)
                {
                //if (schema.findTypeByJavaName(jtd.getFullName()) == null)
                //    {
                //    throw new IllegalStateException("Base type " + jtd.getFullName()
                //                                    + " for type " + type.getFullName()
                //                                    + " is not present in the schema.");
                //    }
                type.setBase(CanonicalTypeDescriptor.from(jtd, schema));
                }

            List<String> interfaces = source.interfaces;
            for (String intf : interfaces)
                {
                jtd = JavaTypeDescriptor.fromInternal(intf);
                CanonicalTypeDescriptor ctd = CanonicalTypeDescriptor.from(jtd, schema);
                if (schema.containsType(ctd.getFullName()))
                    {
                    type.addInterface(ctd);
                    }
                }
            }
        }

    @Override
    public void exportType(ExtensibleType type, ClassNode target, Schema schema)
        {
        // TODO: generate class based on ExtensibleType
        }

    // ---- PropertyHandler implementation ----------------------------------

    @Override
    public Class<FieldNode> getExternalPropertyClass()
        {
        return FieldNode.class;
        }

    @Override
    public void importProperty(ExtensibleProperty property, FieldNode source, Schema schema)
        {
        if (isPass(1))
            {
            String name = m_propertyNameTransformer == null
                          ? source.name
                          : m_propertyNameTransformer.transform(source.name);
            property.setName(name);
            }
        if (isPass(2))
            {
            JavaTypeDescriptor jtd = JavaTypeDescriptor.fromInternal(
                    source.signature != null ? source.signature : source.desc);

            if (schema.findTypeByJavaName(jtd.getFullName()) == null)
                {
                if (m_fMissingPropsAsObject)
                    {
                    jtd = JavaTypeDescriptor.OBJECT;
                    }
                else
                    {
                    throw new IllegalStateException("Property type " + jtd.getFullName()
                            + " is not present in the schema. ");
                    }
                }

            property.setType(CanonicalTypeDescriptor.from(jtd, schema));
            }
        }

    @Override
    public void exportProperty(ExtensibleProperty property, FieldNode target, Schema schema)
        {
        // TODO: generate property based on ExtensibleProperty
        }

    // ---- Filters inner class ---------------------------------------------

    /**
     * Custom filters for type and property filtering.
     */
    public static class Filters
        {
        /**
         * Return a filter that evaluates to {@code true} if the class
         * implements specified interface.
         *
         * @param clzInterface  the interface to check for
         *
         * @return {@code true} if the class implements specified interface,
         *         {@code false} otherwise
         */
        public static Predicate<ClassNode> implementsInterface(final Class clzInterface)
            {
            return cn -> cn.interfaces.contains(
                            org.objectweb.asm.Type.getDescriptor(clzInterface));
            }

        /**
         * Return a filter that evaluates to {@code true} if the class
         * extends specified parent class.
         *
         * @param clzParent  the parent class to check for
         *
         * @return {@code true} if the class extends specified parent class,
         *         {@code false} otherwise
         */
        public static Predicate<ClassNode> extendsClass(final Class clzParent)
            {
            return cn -> cn.superName.equals(
                            org.objectweb.asm.Type.getDescriptor(clzParent));
            }

        /**
         * Return a filter that evaluates to {@code true} if the class
         * is annotated with a specified annotation.
         *
         * @param clzAnnotation  the annotation class to check for
         *
         * @return {@code true} if the class is annotated with a specified annotation,
         *         {@code false} otherwise
         */
        public static Predicate<ClassNode> hasAnnotation(final Class clzAnnotation)
            {
            return cn ->
                {
                if (cn.visibleAnnotations != null)
                    {
                    String desc = org.objectweb.asm.Type.getDescriptor(clzAnnotation);
                    for (AnnotationNode an : (List<AnnotationNode>) cn.visibleAnnotations)
                        {
                        if (desc.equals(an.desc)) return true;
                        }
                    }

                return false;
                };
            }
        }

    // ---- Constants -------------------------------------------------------

    private static final int EXCLUDED_FIELDS =
            Opcodes.ACC_STATIC | Opcodes.ACC_FINAL | Opcodes.ACC_TRANSIENT;

    // ---- Data members ----------------------------------------------------

    private Set<File>            m_files                 = new LinkedHashSet<>();
    private Predicate<ClassNode> m_typeFilter            = t -> true;
    private Predicate<FieldNode> m_propertyFilter        = t -> (t.access & EXCLUDED_FIELDS) == 0;
    private boolean              m_fMissingPropsAsObject = false;

    // name transformers
    private NameTransformer m_namespaceTransformer =
            new NameTransformerChain()
                    .removePrefix("com")
                    .removePrefix("net")
                    .removePrefix("org")
                    .firstLetterToUppercase();
    private NameTransformer m_classNameTransformer = null;
    private NameTransformer m_propertyNameTransformer =
            new NameTransformerChain()
                    .removePrefix("m_")
                    .firstLetterToUppercase();

    private int m_nPass;

    protected static boolean s_isWindows = new StringTokenizer(System.getProperty("os.name").toLowerCase().trim()).nextToken().contains("windows") ? true : false;
    }
