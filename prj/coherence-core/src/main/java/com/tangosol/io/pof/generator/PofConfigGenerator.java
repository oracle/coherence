/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io.pof.generator;

import com.tangosol.dev.introspect.ClassAnnotationSeeker;
import com.tangosol.dev.introspect.ClassPathResourceDiscoverer.InformedResourceDiscoverer;

import com.tangosol.io.pof.annotation.Portable;

import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;

import com.tangosol.util.Base;
import com.tangosol.util.FilterEnumerator;
import com.tangosol.util.extractor.IdentityExtractor;
import com.tangosol.util.extractor.KeyExtractor;
import com.tangosol.util.filter.InFilter;
import com.tangosol.util.filter.NotFilter;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import java.security.CodeSource;
import java.security.ProtectionDomain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static com.tangosol.io.pof.ConfigurablePofContext.DEFAULT_RESOURCE;
import static com.tangosol.io.pof.ConfigurablePofContext.mergeIncludes;

import static com.tangosol.util.Base.azzert;

/**
 * PofConfigGenerator is a utility class allowing the generation of a POF
 * configuration file based on the provided {@link Dependencies}. The
 * generation is influenced by various configuration items present in the
 * Dependencies object. The high level objective of this class is to
 * generate the POF configuration based upon all known / discoverable classes
 * annotated with the {@link Portable} annotation. The generation is capable
 * of determining unallocated user type ids and allocating appropriately.
 * This class is envisaged to be used against various generations thus
 * supports backwards compatibility by specifying a previously defined POF
 * configuration file ensuring all previously allocated type ids are
 * respected.
 * <p>
 * This class is a can be executed as follows:
 * <p>
 * <code>
 *     new PofConfigGenerator(new Dependencies()).generate()
 * </code>
 * <p>
 * There are various configuration items that affect the generation and are
 * described within the {@link Dependencies} object.
 *
 * @author hr  2012.07.04
 *
 * @since Coherence 12.1.2
 *
 * @see PofConfigGenerator.Dependencies
 * @see GarPofConfigGenerator
 * @see Executor
 */
public class PofConfigGenerator
    {

    // ----- constructors ---------------------------------------------------

    /**
     * Construct a PofConfigGenerator with the provided dependencies.
     *
     * @param deps  the configuration items used by this generator
     */
    public PofConfigGenerator(Dependencies deps)
        {
        azzert(deps != null, "PofConfigGenerator requires dependencies");
        deps.validate();
        m_deps = deps.clone();
        }

    // ----- PofConfigGenerator methods -------------------------------------

    /**
     * Executes the various phases of generating a POF Configuration file.
     * The following phases describe the lifecycle of this class:
     * <ol>
     *     <li><b>ensureClassLoader</b> - based on the
     *     {@link Dependencies#getPathRoot() PathRoot} determine the
     *     appropriate {@link ClassLoader} and / or populate the list of
     *     class path entries via {@link #setListClassPath(List)}.</li>
     *     <li><b>ensureReservedUserTypes</b> - determine the reserved
     *     user types returning a map of class info by type id.</li>
     *     <li><b>scan</b> - given the reserved user types scan the
     *     appropriate resources for classes annotated with {@link Portable}.
     *     <li><b>generateXml</b> - based on the reserved and discovered
     *     user types build an XML document.
     *     <li><b>persist</b> - persist the generated XML.
     * </ol>
     */
    public void generate()
        {
        ClassLoader            loader        = ensureClassLoader();
        Map<Integer, TypeInfo> mapReserved   = ensureReservedUserTypes(loader);
        Map<Integer, TypeInfo> mapDiscovered = discoverUserTypes(mapReserved, loader);

        // inject reserved types that should be carried over
        for (Map.Entry<Integer, TypeInfo> entry : mapReserved.entrySet())
            {
            TypeInfo type = entry.getValue();
            if (type.m_fExport)
                {
                mapDiscovered.put(entry.getKey(), type);
                }
            }

        persist(generateXml(mapDiscovered));
        }

    // ----- accessors ------------------------------------------------------

    /**
     * Returns the path the XML file was written to.
     *
     * @return the path the XML file was written to
     */
    public String getWrittenPath()
        {
        return m_sWrittenPath;
        }

    /**
     * The {@link Dependencies} this class is configured with.
     *
     * @return The {@link Dependencies} this class is configured with
     */
    public Dependencies getDependencies()
        {
        return m_deps;
        }

    /**
     * Specify the {@link Dependencies} this class should use.
     *
     * @param deps  the {@link Dependencies} this class should use
     */
    public void setDependencies(Dependencies deps)
        {
        m_deps = deps;
        }

    /**
     * Sets all the {@link URL}s used to scope the scanning of annotations.
     *
     * @param listClassPath  the {@link URL}s used to scope the scanning of
     *                       annotations
     */
    public void setListClassPath(List<URL> listClassPath)
        {
        m_listClassPath = listClassPath;
        }

    // ----- lifecycle methods ----------------------------------------------

    /**
     * Based on the {@link Dependencies#getPathRoot() PathRoot} configuration
     * item return a class loader encompassing the contents within
     * {@literal PathRoot}. There are four states {@literal PathRoot} may
     * hold which results in different outcomes. These states are as
     * follows:
     * <ol>
     *     <li><b>empty</b> - returns that {@link ClassLoader} this class is
     *     executing under.</li>
     *     <li><b>directory</b> - iff the PathRoot is a directory gather all
     *     jar files within the directory, including the directory itself,
     *     creating and returning an appropriate {@link ClassLoader}.</li>
     *     <li><b>jar file</b> - iff PathRoot refers to a jar file use that
     *     jar file as the class loader.</li>
     *     <li><b>delimited list of the above</b> - a file system path
     *     separator delimited list of the above is accepted and each entry
     *     is added to the class path and class loader using the rules
     *     mentioned above.</li>
     * </ol>
     * In the last three states the resolved directory / jars are added to the
     * list of class path entries via {@link #setListClassPath(List)}.
     * <p>
     * The location of Coherence classes is also added to the class loader.
     *
     * @return a derived {@link ClassLoader}
     */
    protected ClassLoader ensureClassLoader()
        {
        List<String> listRoots = m_deps.getPathRoot();
        if (listRoots.isEmpty())
            {
            return Base.getContextClassLoader();
            }

        List<URL> listClassPath = extractClassPath(listRoots);

        // add coherence.jar to the class loader
        ProtectionDomain domain  = getClass().getProtectionDomain();
        CodeSource       codeSrc = domain  == null ? null : domain.getCodeSource();
        URL              url     = codeSrc == null ? null : codeSrc.getLocation();
        if (url != null)
            {
            try
                {
                url = toJarUrl(url);
                }
            catch (MalformedURLException e) {}
            listClassPath.add(url);
            }

        setListClassPath(listClassPath);

        return new URLClassLoader(listClassPath.toArray(new URL[0]));
        }

    /**
     * Determine predefined POF type ids using the provided
     * {@link ClassLoader} and the specified
     * {@link Dependencies#getPofConfig() PofConfig}, returning a map of
     * {@link TypeInfo} objects by type id.
     *
     * @param loader  the ClassLoader used to load the pof config
     *
     * @return a map of TypeInfo objects by user type id
     */
    protected Map<Integer, TypeInfo> ensureReservedUserTypes(ClassLoader loader)
        {
        Dependencies deps             = m_deps;
        String       sPofConfig       = deps.getPofConfig();
        boolean      fInclude         = deps.isInclude();

        sPofConfig = sPofConfig == null ? DEFAULT_RESOURCE : sPofConfig;
        XmlElement   xmlConfig        = XmlHelper.loadFileOrResource(sPofConfig, "POF configuration", loader);
        XmlElement[] axmlUserTypes    = new XmlElement[2];
        XmlElement   xmlConfigGen     = (XmlElement) xmlConfig.clone();
                     axmlUserTypes[0] = xmlConfigGen.getSafeElement("user-type-list");

        // merge the referenced includes in a single user-type-list
        mergeIncludes(sPofConfig, xmlConfig, loader);

        Map<Integer, TypeInfo> mapReserved      = new HashMap<Integer, TypeInfo>();
                               axmlUserTypes[1] = xmlConfig.getSafeElement("user-type-list");
        StringBuilder          sbErrors         = new StringBuilder();

        for (int i = fInclude ? 1 : 0; i < axmlUserTypes.length; ++i)
            {
            XmlElement   xmlUserTypes = axmlUserTypes[i];
            Set<Integer> setTypeIds   = new HashSet<Integer>();

            for (Iterator<XmlElement> iterUserTypes = xmlUserTypes.getElements("user-type"); iterUserTypes.hasNext(); )
                {
                XmlElement xmlUserType  = iterUserTypes.next();
                XmlElement xmlTypeId    = xmlUserType.getSafeElement("type-id");
                XmlElement xmlClassName = xmlUserType.getSafeElement("class-name");
                int        nTypeId      = xmlTypeId.getInt(-1);
                String     sClassName   = xmlClassName.getString();

                if (nTypeId < 0)
                    {
                    sbErrors.append("\t<user-type-list> contains a"
                            + " <user-type> that has a missing or invalid type"
                            + " ID value: " + xmlTypeId.getString(null));
                    }
                if (sClassName.isEmpty())
                    {
                    sbErrors.append("\tMissing class name for type-id: " + nTypeId);
                    }

                Integer ITypeId = nTypeId;
                if (setTypeIds.contains(ITypeId))
                    {
                    sbErrors.append("\tDuplicate user type id: " + nTypeId);
                    }
                else if (!mapReserved.containsKey(ITypeId))
                    {
                    // we only consider the first round of user-types (the virtual
                    // user-types) as those that should contribute to the generated
                    // pof configuration
                    setTypeIds.add(ITypeId);
                    mapReserved.put(ITypeId, new TypeInfo(sClassName, i == 0, xmlUserType));
                    }
                }
            }

        if (sbErrors.length() > 0)
            {
            throw new IllegalStateException("Encountered the following errors when parsing POF Configuration "
                    + sPofConfig + ":\n" + sbErrors.toString());
            }

        // remove existing user-types (and includes iff generating a delta)
        XmlElement xmlUserTypes = xmlConfigGen.getSafeElement("user-type-list");
        for (Iterator<XmlElement> iter = xmlUserTypes.getElementList().iterator(); iter.hasNext(); )
            {
            XmlElement xmlChild = iter.next();
            if (!fInclude && xmlChild.getName().equals("include"))
                {
                continue;
                }
            iter.remove();
            }
        m_xmlConfig = xmlConfigGen;

        return mapReserved;
        }

    /**
     * Use an annotation scanner to scan for all classes annotated with the
     * {@link Portable} annotation. Use the {@literal mapReservedTypes} to
     * distinguish predefined user types that can be discarded by the
     * annotation scanner. This class is also responsible for allocating each
     * discovered class a unique user type id whilst preserving allocated
     * user type ids.
     *
     * @param mapReservedTypes  all discovered classes annotated with
     *                          {@link Portable}
     * @param loader            the {@link ClassLoader}
     *
     * @return a map of discovered user types
     */
    protected Map<Integer, TypeInfo> discoverUserTypes(Map<Integer, TypeInfo> mapReservedTypes, ClassLoader loader)
        {
        Dependencies      deps           = m_deps;
        List<URL>         listClassPath  = m_listClassPath;
        Iterator<Integer> iterTypeId     = new TypeIdIterator(mapReservedTypes.keySet(), deps.getStartTypeId());

        ClassAnnotationSeeker.Dependencies seekerDeps = new ClassAnnotationSeeker.Dependencies()
                .setFilter(new NotFilter(new InFilter(new KeyExtractor(), extractClassNames(mapReservedTypes))))
                .setPackages(deps.getPackages());

        if (listClassPath != null && !listClassPath.isEmpty())
            {
            seekerDeps.setDiscoverer(new InformedResourceDiscoverer(listClassPath.toArray(new URL[listClassPath.size()])));
            }
        seekerDeps.setContextClassLoader(loader);

        List<String> listClassNames = new ArrayList<String>(
                new ClassAnnotationSeeker(seekerDeps).findClassNames(Portable.class));
        Collections.sort(listClassNames);

        Map<Integer, TypeInfo> mapDiscoveredTypes = new TreeMap<Integer, TypeInfo>();
        for (String sClassName : listClassNames)
            {
            mapDiscoveredTypes.put(iterTypeId.next(), new TypeInfo(sClassName, true));
            }

        return mapDiscoveredTypes;
        }

    /**
     * Based on the provided user types generate an XML document
     * incorporating all include references.
     *
     * @param mapAllUserTypes  map of all user types that should be persisted
     *
     * @return the generated XML document
     */
    protected XmlElement generateXml(Map<Integer, TypeInfo> mapAllUserTypes)
        {
        Dependencies deps         = m_deps;
        XmlElement   xmlConfig    = m_xmlConfig;
        XmlElement   xmlUserTypes = xmlConfig.ensureElement("user-type-list");

        // when generating a delta configuration based on an original pof
        // configuration, i.e. complimenting opposed to overwriting, we must
        // reference what we compliment
        if (deps.isInclude())
            {
            String sPofConfig    = deps.getPofConfig();
            File   filePofConfig = new File(sPofConfig);
                   sPofConfig    = filePofConfig.exists() ? filePofConfig.getName() : sPofConfig;
            xmlUserTypes.ensureElement("include").setString(sPofConfig);
            }

        for (Map.Entry<Integer, TypeInfo> entry : mapAllUserTypes.entrySet())
            {
            TypeInfo   type        = entry.getValue();
            XmlElement xmlUserType = type.m_xmlUserType;
            if (xmlUserType == null)
                {
                // this type was discovered
                xmlUserType = xmlUserTypes.addElement("user-type");
                XmlElement xmlTypeId    = xmlUserType.ensureElement("type-id");
                XmlElement xmlClassName = xmlUserType.ensureElement("class-name");

                xmlTypeId.setInt(entry.getKey());
                xmlClassName.setString(type.m_sClassName);
                }
            else
                {
                // this type was pre-existing
                xmlUserTypes.getElementList().add(xmlUserType);
                }
            }

        return xmlConfig;
        }

    /**
     * Persist the passed Xml document to an appropriate file. The file used
     * is specified by {@link Dependencies#getOutputPath() OutputPath}
     * which may have the following representations:
     * <ol>
     *     <li><b>file</b> - the filename to persist the generated POF
     *     configuration to. The file or the directory containing the file
     *     must be writable. In the former case the file is overwritten.</li>
     *     <li><b>directory</b> - the directory to persist a new file
     *     with the following format: {@literal (pof-config.xml |
     *     pof-config-{n}.xml)} where n is in the range of 1 -255 used based
     *     on the presence of a preexisting file.</li>
     * </ol>
     *
     * @param xmlConfig  the generated XML POF configuration to persist
     */
    protected void persist(XmlElement xmlConfig)
        {
        String sOutputPath = m_deps.getOutputPath();
        File   fileOut     = sOutputPath == null || sOutputPath.isEmpty()
                ? new File(".") : new File(sOutputPath);

        try
            {
            if (fileOut.isDirectory())
                {
                fileOut = ensureUniqueFile(fileOut, getDefaultPofConfigFileName());
                }
            else
                {
                fileOut.createNewFile();
                }

            if (!fileOut.canWrite())
                {
                throw new IllegalStateException("Insufficient permissions to write to file: " + fileOut);
                }

            xmlConfig.writeXml(new PrintWriter(fileOut), true);
            m_sWrittenPath = fileOut.getCanonicalPath();
            }
        catch (IOException e)
            {
            throw new IllegalStateException("Error in writing file: " + fileOut, e);
            }
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Interrogate the path root reference provided, adding acceptable files
     * to the {@link List} of {@link URL}s.
     *
     * @param listRoots  a list of locations to contribute to convert to URLs
     *
     * @return the given list of roots converted to a list of URLs
     */
    protected List<URL> extractClassPath(List<String> listRoots)
        {
        List<URL> listClassPath = new ArrayList<URL>(listRoots.size());
        for (String sFile : listRoots)
            {
            File file = new File(sFile);
            if (!file.exists())
                {
                throw new IllegalArgumentException("Path in root is invalid: " + sFile);
                }
            else if (file.isFile())
                {
                String sFileName  = file.getName();
                int    iPosExt    = sFileName.lastIndexOf('.');
                String sExtension = iPosExt < 0 ? "" : sFileName.substring(iPosExt);

                if (!sExtension.endsWith("jar"))
                    {
                    throw new IllegalArgumentException("Root location refers to a file without a jar extension: " + sFileName);
                    }

                try
                    {
                    listClassPath.add(toJarUrl(file));
                    }
                catch (MalformedURLException e)
                    {
                    throw new IllegalArgumentException("Root location can not be referred to: " + sFileName);
                    }
                }
            else if (file.isDirectory())
                {
                String        sErrorHead  = "The following file descriptors could not be referred to:\n";
                StringBuilder sbldrErrors = new StringBuilder(sErrorHead);
                try
                    {
                    listClassPath.add(file.toURI().toURL());
                    }
                catch (MalformedURLException e)
                    {
                    sbldrErrors.append("\t" + file);
                    }

                File[] afileJars = file.listFiles(new FilenameFilter()
                    {
                    public boolean accept(File dir, String name)
                        {
                        return name.endsWith(".jar");
                        }
                    });

                for (File fileJar : afileJars)
                    {
                    try
                        {
                        listClassPath.add(toJarUrl(fileJar));
                        }
                    catch (MalformedURLException e)
                        {
                        sbldrErrors.append("\t" + fileJar);
                        }
                    }

                if (sbldrErrors.length() > sErrorHead.length())
                    {
                    throw new IllegalArgumentException(sbldrErrors.toString());
                    }
                }
            }
        return listClassPath;
        }

    /**
     * Given a directory and a file name ensure a unique file is created and
     * returned.
     *
     * @param fileDir  the base directory
     * @param sName    the name of the file
     *
     * @return a unique file
     *
     * @throws IOException iff a file could not be created
     */
    protected File ensureUniqueFile(File fileDir, String sName)
            throws IOException
        {
        File   fileAttempt;
        File   fileOut   = null;
        String sFileName = sName;
        int    iInject   = sName.lastIndexOf('.');
               iInject   = iInject == -1 ? sName.length() : iInject;
        String sPrefix   = sName.substring(0, iInject) + "-";
        String sSuffix   = sName.substring(iInject, sName.length());

        for (int i = 0; i < 255;)
            {
            fileAttempt = new File(fileDir, sFileName);
            if (!fileAttempt.exists())
                {
                fileOut = fileAttempt;
                break;
                }
            sFileName = sPrefix + ++i + sSuffix;
            }

        if (fileOut == null)
            {
            // fall back to random number injection into the filename
            fileOut = File.createTempFile(sPrefix, sSuffix, fileDir);
            }
        else
            {
            fileOut.createNewFile();
            }

        return fileOut;
        }

    /**
     * Given the map of {@link TypeInfo} objects by user type id return a
     * set of class names.
     *
     * @param mapTypes  map of user types used to extract class names from
     *
     * @return a distinct collection of class names
     */
    protected Set<String> extractClassNames(Map<Integer, TypeInfo> mapTypes)
        {
        Set<String> setClassNames = new HashSet<String>(mapTypes.size());
        for (Map.Entry<Integer, TypeInfo> entry : mapTypes.entrySet())
            {
            setClassNames.add(entry.getValue().m_sClassName);
            }
        return setClassNames;
        }

    /**
     * Based on a {@link File} create a JAR {@link URL}.
     *
     * @param file  the JAR file
     *
     * @return a {@link URL} using a jar protocol
     *
     * @throws MalformedURLException
     */
    protected URL toJarUrl(File file)
            throws MalformedURLException
        {
        return toJarUrl(file.toURI().toURL());
        }

    /**
     * Based on a {@link URL} create a JAR URL.
     *
     * @param url  the original url
     *
     * @return a URL using the jar protocol
     *
     * @throws MalformedURLException
     */
    protected URL toJarUrl(URL url)
            throws MalformedURLException
        {
        String sExternal = url.toExternalForm();
        return url.getProtocol().equals("jar") || !sExternal.endsWith(".jar")
                ? url : new URL("jar:" + sExternal + "!/");
        }

    /**
     * Returns the default POF configuration file name to use when one is
     * not specified or a directory is specified.
     *
     * @return the default POF configuration file name
     */
    protected String getDefaultPofConfigFileName()
        {
        return DEFAULT_POF_CONFIG_FILE_NAME;
        }

    // ----- inner class: TypeInfo ------------------------------------------

    /**
     * A TypeInfo represents a user type based on it's class name, whether it
     * should be present in any generated XML files and an original XML
     * description of the user type.
     */
    protected class TypeInfo
        {

        // ----- constructors -----------------------------------------------

        /**
         * Construct a TypeInfo with a class name and whether it should be
         * present in any generated XML files.
         *
         * @param sClassName  the name of the class
         * @param fExport     whether the user type should be present in any
         *                    generated xml files
         */
        protected TypeInfo(String sClassName, boolean fExport)
            {
            this(sClassName, fExport, null);
            }

        /**
         * Construct a TypeInfo with a class name and whether it should be
         * present in any generated xml files.
         *
         * @param sClassName  the name of the class
         * @param fExport     whether the user type should be present in any
         *                    generated XML files
         */
        protected TypeInfo(String sClassName, boolean fExport, XmlElement xmlUserType)
            {
            m_sClassName  = sClassName;
            m_fExport     = fExport;
            m_xmlUserType = fExport ? xmlUserType : null;
            }

        // ----- data members -----------------------------------------------

        /**
         * The class name of the represented type.
         */
        protected String m_sClassName;

        /**
         * Whether to export this type into the generated POF Configuration
         * file.
         */
        protected boolean m_fExport;

        /**
         * The original XML definition of a user type.
         */
        protected XmlElement m_xmlUserType;
        }

    // ----- inner class: TypeIdIterator ------------------------------------

    /**
     * An {@link Iterator} implementation that understands allocated user
     * type ids thus able to navigate around these allocated blocks.
     */
    protected class TypeIdIterator
            extends FilterEnumerator
        {

        // ----- constructors -----------------------------------------------

        /**
         * Construct a TypeIdIterator with the provided reservations and a
         * starting type id of {@literal 1000}.
         *
         * @param setReservedTypeIds  pre-allocated type ids
         */
        protected TypeIdIterator(Set<Integer> setReservedTypeIds)
            {
            this(setReservedTypeIds, 1000);
            }

        /**
         * Construct a TypeIdIterator with the provided reservations.
         *
         * @param setReservedTypeIds  pre-allocated type ids
         * @param nStartTypeId        the user type id to start allocating type
         *                            ids from
         */
        protected TypeIdIterator(Set<Integer> setReservedTypeIds, final int nStartTypeId)
            {
            super(new Iterator()
                {
                @Override
                public boolean hasNext()
                    {
                    int nCurrentTypeId = m_nCurrentTypeId;
                    return nCurrentTypeId >= 0 && nCurrentTypeId < Integer.MAX_VALUE;
                    }

                @Override
                public Object next()
                    {
                    return m_nCurrentTypeId++;
                    }

                @Override
                public void remove()
                    {
                    throw new UnsupportedOperationException();
                    }

                protected int m_nCurrentTypeId = nStartTypeId;
                }, new NotFilter(new InFilter(IdentityExtractor.INSTANCE, setReservedTypeIds)));
            }
        }

    // ----- inner class: Dependencies --------------------------------------

    /**
     * All dependencies of this class. None of the attributes are mandatory.
     * Below is a description of the properties of this object which affect
     * the POF configuration generation:
     * <table border=1>
     *     <tr><th>Configuration Item</th><th>Influence</th><th>Default</th></tr>
     *     <tr><td>{@link Dependencies#getOutputPath() OutputPath}</td>
     *         <td>The destination of the generated file. Iff this is a file
     *             reference will the file be overwritten otherwise a file is
     *             placed in the referenced directory with the format
     *             {@literal (pof-config.xml | pof-config-{n}.xml)} where n is in
     *             the range of 1 -255 used based on the presence of a
     *             preexisting file.</td>
     *         <td>Current working directory.</td></tr>
     *     <tr><td>{@link Dependencies#getPathRoot() PathRoot}</td>
     *         <td>A list of locations used to scan for annotated classes. Each
     *             location may be a jar file, a directory containing jar files
     *             or a root directory of classes.</td>
     *         <td>The class path this class is executing under.</td></tr>
     *     <tr><td>{@link Dependencies#getPofConfig() PofConfig}</td>
     *         <td>A previous POF configuration file. This file is used to
     *             determine reserved type ids and associated classes. The user
     *             types defined in this file are propagated to the generated
     *             POF configuration.
     *             </td>
     *         <td>Iff present JVM argument {@literal tangosol.pof.confg} or
     *             {@literal pof-config.xml}.</td></tr>
     *     <tr><td>{@link Dependencies#getPackages() Packages}</td>
     *         <td>A set of packages to refine the search for annotated classes.
     *         </td>
     *         <td>All packages.</td></tr>
     *     <tr><td>{@link Dependencies#isInclude() Include}</td>
     *         <td>Iff set to true the generator will generate a delta including
     *             all POF user types not present in the provided
     *             {@link Dependencies#getPofConfig() PofConfig} file but
     *             discovered as a part of the annotation scanning process. The
     *             provided {@literal PofConfig} configuration file will be
     *             referenced as an include in the generated configuration. Iff
     *             this is an absolute path, thus can be referenced as a
     *             {@link File}, only the file name will be used with the
     *             assumption that the file will exist at the root of the class
     *             path.</td>
     *         <td>false.</td></tr>
     *     <tr><td>{@link Dependencies#getStartTypeId()} StartTypeId}</td>
     *         <td>The user type id to start allocations from.</td>
     *         <td>1000.</td></tr>
     * </table>
     */
    public static class Dependencies
        {

        // ----- constructors -----------------------------------------------

        /**
         * Default constructor initializing a Dependencies object with
         * default values.
         */
        public Dependencies()
            {
            m_fInclude = false;
            }

        /**
         * Copy constructor cloning the provided Dependencies into this
         * instance.
         *
         * @param deps  the dependencies to copy from
         */
        public Dependencies(Dependencies deps)
            {
            m_sPofConfig   = deps.getPofConfig();
            m_listRoots    = deps.getPathRoot();
            m_sOutputPath  = deps.getOutputPath();
            m_fInclude     = deps.isInclude();
            m_setPackages  = new HashSet<String>(deps.getPackages());
            m_nStartTypeId = deps.getStartTypeId();
            }

        // ----- accessors --------------------------------------------------

        /**
         * Returns the location of the POF Configuration file to use as the
         * previous generation. This file is used to
         * determine reserved type ids and associated classes. The user
         * types defined in this file are propagated to the generated
         * POF configuration if {@link #isInclude()}{@code == false}.
         * <p>
         * <i>Default:</i> Iff present JVM argument
         * {@literal tangosol.pof.confg} or {@literal pof-config.xml}.
         *
         * @return the path to the POF configuration of the previous
         *         generation
         */
        public String getPofConfig()
            {
            return m_sPofConfig;
            }

        /**
         * Sets the path to POF configuration of a previous generation or of
         * base types to include.
         *
         * @param sPofConfig  path to POF configuration of a previous
         *                    generation
         *
         * @return a reference to this Dependencies object
         */
        public Dependencies setPofConfig(String sPofConfig)
            {
            m_sPofConfig = sPofConfig;
            return this;
            }

        /**
         * Returns a list of locations used to scan for annotated classes.
         * Each location may be a jar file, a directory containing jar files
         * or a root directory of classes.
         * <p>
         * <i>Default:</i> The class path this class is executing under.
         *
         * @return a reference to this Dependencies object
         */
        public List<String> getPathRoot()
            {
            return m_listRoots;
            }

        /**
         * Sets a list of locations used to scan for annotated classes.
         *
         * @param listRoots  a list of locations used to scan for annotated
         *                   classes
         *
         * @return a reference to this Dependencies object
         */
        public Dependencies setPathRoot(List<String> listRoots)
            {
            azzert(listRoots != null, "PofConfigGenerator.Dependencies can not have a null path root");
            m_listRoots = listRoots;
            return this;
            }

        /**
         * The destination of the generated file. Iff this is a file
         * reference will the file be overwritten otherwise a file is
         * placed in the referenced directory with the format
         * {@literal (pof-config.xml | pof-config-{n}.xml)} where n is in
         * the range of 1 - 255 used based on the presence of a
         * preexisting file.
         * <p>
         * <i>Default:</i> Current working directory.
         *
         * @return destination of the generated file
         */
        public String getOutputPath()
            {
            return m_sOutputPath;
            }

        /**
         * Sets the destination of the generated file.
         *
         * @param sOutputPath  destination of the generated file
         *
         * @return a reference to this Dependencies object
         */
        public Dependencies setOutputPath(String sOutputPath)
            {
            m_sOutputPath = sOutputPath;
            return this;
            }

        /**
         * A set of packages to refine the search for annotated classes.
         *
         * @return set of packages to refine the search for annotated classes
         */
        public Set<String> getPackages()
            {
            return m_setPackages;
            }

        /**
         * Sets the set of packages to refine the search for annotated
         * classes.
         *
         * @param setPackages  set of packages to refine the search for
         *                     annotated classes
         *
         * @return a reference to this Dependencies object
         */
        public Dependencies setPackages(Set<String> setPackages)
            {
            azzert(setPackages != null, "PofConfigGenerator.Dependencies can not have a null set of packages");
            m_setPackages = setPackages;
            return this;
            }

        /**
         * Adds a package to a set of packages to refine the search for
         * annotated classes.
         *
         * @param sPackage  add the passed package to the set of packages to
         *                  scan
         *
         * @return a reference to this Dependencies object
         */
        public Dependencies addPackage(String sPackage)
            {
            m_setPackages.add(sPackage);
            return this;
            }

        /**
         * Returns whether to generate a delta with an include referring to
         * the previous configuration.
         * <p>
         * Iff set to true the generator will generate a delta including
         * all POF user types not present in the provided
         * {@link Dependencies#getPofConfig() PofConfig} file but
         * discovered as a part of the annotation scanning process. The
         * provided {@literal PofConfig} configuration file will be
         * referenced as an include in the generated configuration. Iff
         * this is an absolute path, thus can be referenced as a
         * {@link File}, only the file name will be used with the
         * assumption that the file will exist at the root of the class
         * path.
         * <p>
         * <i>Default: </i>false.
         *
         * @return whether to generate a delta with an include
         */
        public boolean isInclude()
            {
            return m_fInclude;
            }

        /**
         * Sets whether to generate a delta with an include referring to
         * the previous configuration.
         *
         * @param fInclude  whether to generate a delta with an include
         *
         * @return a reference to this Dependencies object
         */
        public Dependencies setInclude(boolean fInclude)
            {
            m_fInclude = fInclude;
            return this;
            }

        /**
         * Returns a user type id to start allocations from.
         * <p>
         * <i>Default: </i>1000.
         *
         * @return a user type id to start allocations from
         */
        public int getStartTypeId()
            {
            return m_nStartTypeId;
            }

        /**
         * Sets a user type id to start allocations from.
         *
         * @param nStartTypeId  a user type id to start allocations from
         *
         * @return a reference to this Dependencies object
         */
        public Dependencies setStartTypeId(Integer nStartTypeId)
            {
            m_nStartTypeId = nStartTypeId == null ? m_nStartTypeId : nStartTypeId;
            return this;
            }

        // ----- helpers ----------------------------------------------------

        /**
         * Validates this object ensuring any defaults are populated and
         * mandatory attributes are specified.
         */
        public void validate()
            {
            String sPofConfig = m_sPofConfig;
            if (sPofConfig == null || sPofConfig.isEmpty())
                {
                sPofConfig = m_sPofConfig = DEFAULT_RESOURCE;
                }
            }

        // ----- object methods ---------------------------------------------

        /**
         * {@inheritDoc}
         */
        @Override
        protected Dependencies clone()
            {
            return new Dependencies(this);
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString()
            {
            return "PofConfigGenerator.Dependencies{" +
                    "pathRoot = '" + m_listRoots + '\'' +
                    ", outputPath = '" + m_sOutputPath + '\'' +
                    ", pofConfig = '" + m_sPofConfig + '\'' +
                    ", include = " + m_fInclude +
                    ", packages = " + m_setPackages +
                    ", startTypeId = " + m_nStartTypeId +
                    '}';
            }

        /**
         * Original POF Configuration file. This file is read only and must
         * be respected in any type id allocations.
         */
        protected String       m_sPofConfig;

        /**
         * The root location to search for annotated classes.
         */
        protected List<String> m_listRoots = new ArrayList<String>();

        /**
         * The output path for the generated XML.
         */
        protected String       m_sOutputPath;

        /**
         * The packages to include in the class discovery stage.
         */
        protected Set<String>  m_setPackages = new HashSet<String>();

        /**
         * An indication to the generator to generate a delta POF
         * configuration, complimenting the original POF configuration with
         * an include element referring to the original.
         */
        protected boolean      m_fInclude;

        /**
         * A type id to start the allocations from.
         */
        protected int          m_nStartTypeId = 1000;
        }

    // ----- constants ------------------------------------------------------

    /**
     * The default filename used when generating the POF configuration and
     * a file is not specified.
     */
    protected static final String DEFAULT_POF_CONFIG_FILE_NAME = "pof-config.xml";

    // ----- data members ---------------------------------------------------

    /**
     * The required dependencies for the execution of this class.
     */
    protected Dependencies m_deps;

    /**
     * The generated XML.
     */
    protected XmlElement   m_xmlConfig;

    /**
     * The location the generated file was written to.
     */
    protected String       m_sWrittenPath;

    /**
     * A list of URLs used to build a {@link ClassLoader}. This is only
     * populated when a root location referencing a directory or a jar is
     * specified. The list of URLs allows an isolated resource discovery
     * phase which provides better scoping and performance.
     */
    protected List<URL>    m_listClassPath;
    }