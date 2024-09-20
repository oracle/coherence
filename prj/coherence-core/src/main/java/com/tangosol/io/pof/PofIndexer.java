/*
 * Copyright (c) 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io.pof;


import com.oracle.coherence.common.base.Classes;
import com.oracle.coherence.common.base.Logger;

import com.oracle.coherence.common.collections.NullableConcurrentMap;
import com.tangosol.io.pof.generator.PortableTypeGenerator;
import com.tangosol.io.pof.schema.annotation.PortableType;

import com.tangosol.util.LiteMap;
import com.tangosol.util.Resources;
import com.tangosol.util.SafeHashMap;

import io.github.classgraph.AnnotationParameterValue;
import io.github.classgraph.AnnotationParameterValueList;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

/**
* This class is responsible for creating or reading POF index files.
*
* @author Gunnar Hillert  2024.03.04
* @since Coherence 24.09
*/
public class PofIndexer
    {

    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor.
     */
    public PofIndexer()
        {
        this(Classes.getContextClassLoader(), new PortableTypeGenerator.CoherenceLogger());
        }

    /**
     * Constructor with the ability to pass a {@link  PortableTypeGenerator.Logger}.
     *
     * @param logger the {@link  PortableTypeGenerator.Logger} to use
     */
    public PofIndexer(PortableTypeGenerator.Logger logger)
        {
        this(Classes.getContextClassLoader(), logger);
        }

    /**
     * Constructor that allows to specify a custom {@link ClassLoader}.
     *
     * @param classLoader the ClassLoader to set
     */
    public PofIndexer(ClassLoader classLoader)
        {
        this(classLoader, new PortableTypeGenerator.CoherenceLogger());
        }

    /**
     * Constructor that allows to specify a custom {@link ClassLoader} as well as
     * a {@link  PortableTypeGenerator.Logger}.
     *
     * @param classLoader the ClassLoader to set
     * @param logger the {@link  PortableTypeGenerator.Logger} to use
     */
    public PofIndexer(ClassLoader classLoader, PortableTypeGenerator.Logger logger)
        {
        m_classLoader = classLoader == null ? PofIndexer.class.getClassLoader() : classLoader;
        m_log    = logger;
        }

    // ----- Pof Indexer implementation --------------------------------------

    /**
     * Creates a POF index file at the specified base directory. By default, the index will be created
     * in the subdirectory {@code META-INF/pof.idx}.
     *
     * @param pofIndexFileDirectory must be a directory
     */
    public void createIndexInDirectory(File pofIndexFileDirectory)
        {

        if (pofIndexFileDirectory.isFile())
            {
            throw new IllegalArgumentException(String.format("pofIndexFileDirectory '%s' must not be a file.",
                    pofIndexFileDirectory.getAbsolutePath()));
            }

        File pofIndexFile = new File(pofIndexFileDirectory, m_sIndexFileName);
        pofIndexFile.getParentFile().mkdirs();

        createIndex(pofIndexFile);
        }

    /**
     * Creates an index file containing the discovered {@link PortableType} annotated classes.
     *
     * @param pofIndexFile the file that will contain the index of PortableType annotated classes
     */
    public void createIndex(File pofIndexFile)
        {

        m_log.info(String.format("Creating POF index file at '%s'.", pofIndexFile.getAbsolutePath()));
        Map<String, Integer> portableClasses = discoverPortableTypes();
        m_log.info(String.format("Discovered %s class(es) annotated with `%s`.", portableClasses, PortableType.class.getName()));

        Properties pofProperties = new Properties();
        for (Map.Entry<String, Integer> entry : portableClasses.entrySet())
            {
            pofProperties.put(entry.getKey(), entry.getValue() != null ? String.valueOf(entry.getValue()) : "");
            }

        try (FileOutputStream out = new FileOutputStream(pofIndexFile))
            {
            pofProperties.store(out, null);
            }
        catch (FileNotFoundException e)
            {
            throw new RuntimeException(String.format("Unable to create FileOutputStream for POF Index File '%s'.",
                    pofIndexFile.getAbsolutePath()), e);
            }
        catch (IOException e)
            {
            throw new RuntimeException(String.format("Something went wrong while storing the POF index classnames to" +
                    " the underlying FileOutputStream with the specified POF Index File '%s'.", pofIndexFile.getAbsolutePath()), e);
            }
        }

    /**
     * Scans for {@link PortableType} annotated classes. The returned {@link Map}
     * contains the class name as the key and the POF id as its value. Keep in mind that the POF id can currently only be
     * returned for classes obtained via the classpath but not from actual class files or JAR files.
     *
     * @return a Map containing the class name and the POF id if possible
     */
    protected Map<String, Integer> discoverPortableTypes()
        {
        Map<String, Integer> mapPortableTypes = new NullableConcurrentMap<>();
        ClassGraph classGraph = new ClassGraph()
                .enableAnnotationInfo()
                .overrideClassLoaders(m_classLoader);

        if (!m_packagesToScan.isEmpty())
            {
            classGraph.acceptPackages(m_packagesToScan.toArray(new String[0]));
            }
        if (!m_classes.isEmpty())
            {
            classGraph.acceptClasses(m_classes.stream().map(Class::getName).toArray(String[]::new));
            }

        List<URL> classPathUris = new ArrayList<>();

        if (!m_fIgnoreClasspath)
            {
            classPathUris.addAll(classGraph.getClasspathURLs());
            }

        if (!m_jarFiles.isEmpty())
            {
            List<URL> urls = m_jarFiles.stream().map(this::createJarURL).toList();
            classPathUris.addAll(urls);
            }

        if (!m_classesDirectories.isEmpty())
            {
            classPathUris.addAll(m_classesDirectories.stream().map(file ->
                {
                try
                    {
                    return file.toURL();
                    }
                catch (MalformedURLException e)
                    {
                    throw new RuntimeException(e);
                    }
                }).toList());
            }
        classGraph.overrideClasspath(classPathUris);

        try (ScanResult result = classGraph.scan())
            {
            for (ClassInfo classInfo : result.getClassesWithAnnotation(PortableType.class))
                {
                AnnotationParameterValueList annotationParameterValues = classInfo.getAnnotationInfo(PortableType.class).getParameterValues();
                AnnotationParameterValue idValue = annotationParameterValues.get("id");

                int portableTypeId;

                if (idValue != null)
                    {
                    portableTypeId = (int) idValue.getValue();

                    // Can be removed once Bug 36955929 is implemented
                    if (portableTypeId == -1)
                        {
                        throw new IllegalStateException("The PortableType annotation on class "
                                + classInfo.getName() + " did not have a required POF id.");
                        }
                    }
                else
                    {
                    throw new IllegalStateException("The PortableType annotation on class "
                            + classInfo.getName() + " did not have a required POF id.");
                    }
                mapPortableTypes.put(classInfo.getName(), portableTypeId);
                }
            }
        if (m_includeFilterPatterns.isEmpty())
            {
            return mapPortableTypes;
            }
        else
            {

            Map<String, Integer> mapFilteredPortableTypes = new SafeHashMap<>();

            mapPortableTypes.entrySet().stream().filter(
                    mapEntry ->
                        {
                        for (Pattern includePattern : m_includeFilterPatterns)
                            {
                            if (includePattern.matcher(mapEntry.getKey()).matches())
                                {
                                return true;
                                }
                            }
                        return false;
                        }).forEach(mapEntry -> mapFilteredPortableTypes.put(mapEntry.getKey(), mapEntry.getValue()));

            return mapFilteredPortableTypes;
            }

        }

    /**
     * Returns the id value of an {@link PortableType} annotated class.
     *
     * @param classname the class name with the PortableType annotation
     *
     * @return returns the portable id
     */
    public int getPortableTypeIdForClassName(String classname)
        {
        try
            {
            PortableType portableType = Class.forName(classname, true, m_classLoader).getAnnotation(PortableType.class);
            int portableTypeId = portableType.id();
            return portableTypeId;
            }
        catch (ClassNotFoundException e)
            {
            throw new IllegalStateException("Did not find PortableType class " + classname);
            }
        }

    /**
     * Attempt to load all POF index files which will we will use to load classes that have the PortableType annotation.
     *
     * @return a {@link Map} of {@link Properties} keyed by {@link URL} or an empty {@link Map} if none found.
     */
     public Map<URL, Properties> loadIndexes()
        {
        Map<URL, Properties> mapIndexes     = new LiteMap<>();
        String               sIndexFileName = m_sIndexFileName;

        try
            {
            Iterable<URL> iterUrls = Resources.findResources(sIndexFileName, m_classLoader);

            // loop through each URL and load the index
            for (URL url : iterUrls)
                {
                try (InputStream input = url.openStream())
                    {
                    Properties pofProperties = new Properties();
                    pofProperties.load(input);
                    mapIndexes.put(url, pofProperties);
                    }
                catch (Exception ignore)
                    {
                    Logger.warn("Unable to read POF index file " + url + ", error is " + ignore.getMessage());
                    }
                }
            }
        catch (IOException e)
            {
            // any Exception coming from getResources() or toURL() is ignored and
            // the Map of indexes remain empty
            }

        return mapIndexes;
        }

        /**
         * Specifies the POF index file and its path to use. If not set, it will default to {@link #DEFAULT_INDEX_FILE_NAME}.
         *
         * @param sIndexFileName the POF index file.
         */
        public void setIndexFileName(String sIndexFileName)
            {
            this.m_sIndexFileName = sIndexFileName;
            }

        /**
         * Specifies the JAVA packages to scan for {@link PortableType} annotations. This option is only valid for
         * scanning the classpath and thus if {@link #ignoreClasspath(boolean)} is not set to true.
         *
         * @param packagesToScan the java package names to scan
         */
        public void setPackagesToScan(Set<String> packagesToScan)
            {
            this.m_packagesToScan = packagesToScan;
            }

        /**
         * Allows to set regular expressions for classes to be included. E.g. if you want to have only classes included
         * in the POF that end in Address, specify a regular expression string of {code .*Address$}.
         *
         * @param m_includeFilterPatterns add a Set of regular expression strings
         */
        public void setIncludeFilterPatterns(Set<String> m_includeFilterPatterns)
            {
            this.m_includeFilterPatterns.addAll(
                    m_includeFilterPatterns
                            .stream()
                            .map(patternString -> Pattern.compile(patternString))
                            .toList());
            }

        /**
         * Construct a Jar file {@link URI}.
         *
         * @param jarFile the {@link File} representing a Jar file
         *
         * @return a URI in the Jar URI syntax
         */
        public URI createJarURI(File jarFile)
            {
            try
                {
                return new URI("jar", jarFile.toURI() + "!/", null);
                }
            catch (URISyntaxException e)
                {
                throw new IllegalStateException(String.format(
                        "Unable to convert '%s' to a Jar URI.", jarFile.getAbsolutePath()), e);
                }
            }

        /**
         * Helper method to create a JAR file {@link URL}. Will call {@link #createJarURI(File)} underneath. Will not throw
         * any checked exceptions.
         *
         * @param jarFile the JAR file for which to return the corresponding {@link URL}
         *
         * @return the URL that points to the provided JAR file
         */
        public URL createJarURL(File jarFile)
            {
            URI jarUri = createJarURI(jarFile);
            try
                {
                return jarUri.toURL();
                }
            catch (MalformedURLException e)
                {
                throw new RuntimeException(e);
                }
            }

    /**
     * Ignore the classpath when scanning for classes by setting this property to true. by default this property is false.
     * This can be useful when indexing of actual class or Jar files.
     *
     * @param fIgnoreClasspath if true, do not index classes on the classpath
     *
     * @return this {@code PofIndexer}
     */
    public PofIndexer ignoreClasspath(boolean fIgnoreClasspath)
        {
        m_fIgnoreClasspath = fIgnoreClasspath;
        return this;
        }

    /**
     * Add the directory on the file system to read the class files from.
     *
     * @param directories the directories on the file system to read the class
     *                    files from
     *
     * @return this {@code PofIndexer}
     */
    public PofIndexer withClassesFromDirectory(Collection<File> directories)
        {
        m_classesDirectories.addAll(directories);
        return this;
        }

    /**
     * Add one or more JAR files on the file system to read the class files from.
     *
     * @param jarFiles the List of JAR files on the file system to read the class files
     *                 from
     *
     * @return this {@code PofIndexer}
     */
    public PofIndexer withClassesFromJarFile(List<File> jarFiles)
        {
        m_jarFiles.addAll(jarFiles);
        return this;
        }

    /**
     * Add a {@link List} of {@link Class}es.
     *
     * @param clazzes the classes to import
     *
     * @return this {@code PofIndexer}
     */
    public PofIndexer withClasses(List<Class> clazzes)
        {
        this.m_classes.addAll(clazzes);
        return this;
        }

    /**
     * Specify the index file name and path. If not specified it will default to {@link #DEFAULT_INDEX_FILE_NAME}.
     *
     * @param indexFileName the path and index file name
     *
     * @return this {@code PofIndexer}
     */
    public PofIndexer withIndexFileName(String indexFileName)
        {
        this.m_sIndexFileName = indexFileName;
        return this;
        }

    // ----- constants ------------------------------------------------------

    /**
     * Default POF index file.
     */
    public static final String DEFAULT_INDEX_FILE_NAME = "META-INF/pof.idx";

    // ----- data members ---------------------------------------------------

    /**
     * The POF index file name to use.
     */
    private String m_sIndexFileName = DEFAULT_INDEX_FILE_NAME;

    /**
     * A WeakReference to the ClassLoader specified for this PofContext to
     * use.
     */
    private final ClassLoader m_classLoader;

    /**
     * If set to true, {@link #discoverPortableTypes()} will not inspect the classpath to
     * discover {@link com.tangosol.io.pof.schema.annotation.PortableType} annotated classes.
     */
    private boolean m_fIgnoreClasspath = false;

    /**
     * Holds the JAVA packages to scan for {@link PortableType} annotations. This option is only valid for
     * scanning the classpath and thus if {@link #ignoreClasspath(boolean)} is not set to true.
     */
    private Set<String> m_packagesToScan = new HashSet<>();

    /**
     * A {@link List} of {@link Class}es to scan to find clases annotated with {@link PortableType} annotations.
     * This option is only valid for scanning the classpath and thus if {@link #ignoreClasspath(boolean)} is not set to
     * true.
     */
    private List<Class> m_classes = new ArrayList<>();

    /**
     * A List of individual JAR {@link File}s on the file system to scan for classes that contain clases annotated
     * with {@link PortableType} annotations.
     */
    private List<File> m_jarFiles = new ArrayList<>();

    /**
     * A List of individual {@link File} that represent directories on the file system to scan for classes that contain
     * classes annotated with {@link PortableType} annotations.
     */
    private List<File> m_classesDirectories = new ArrayList<>();

    /**
     * A list of regular expression {@link Pattern}s used to only scan for classes that contain classes annotated with
     * {@link PortableType} annotations that also match the specified regular expressions. E.g. if you want to have
     * only classes included in the POF that end in Address, specify a regular expression string of {code .*Address$}.
     */
    private List<Pattern> m_includeFilterPatterns = new ArrayList<>();

    /**
     * The {@link PortableTypeGenerator.Logger} to use.
     */
    private PortableTypeGenerator.Logger m_log;
    }
