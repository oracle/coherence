/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util.security;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Stream;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Generates {@code META-INF/coherence/security-config.xml} from annotations
 * found in one compiled-classes directory.
 *
 * @author Aleks Seovic  2026.05.01
 * @since 26.07
 */
public final class SecurityConfigGenerator
    {
    // ----- constructors ---------------------------------------------------

    private SecurityConfigGenerator()
        {
        }

    // ----- SecurityConfigGenerator methods -------------------------------

    /**
     * Scan a compiled-classes directory and return the generated config.
     *
     * @param classesDir  compiled-classes directory
     *
     * @return the generated config
     *
     * @throws IOException if class files cannot be read
     */
    public static GeneratedConfig generate(Path classesDir)
            throws IOException
        {
        if (classesDir == null || !Files.isDirectory(classesDir))
            {
            return new GeneratedConfig(Collections.<Entry>emptyList());
            }

        Map<String, ClassInfo> mapClasses = new HashMap<>();
        try (Stream<Path> stream = Files.walk(classesDir))
            {
            stream.filter(Files::isRegularFile)
                    .filter(SecurityConfigGenerator::isClassFile)
                    .forEach(path ->
                        {
                        try
                            {
                            ClassInfo info = scan(path);
                            mapClasses.put(info.getInternalName(), info);
                            }
                        catch (IOException e)
                            {
                            throw new ReadFailure(e);
                            }
                        });
            }
        catch (ReadFailure e)
            {
            throw e.getCause();
            }

        Map<String, String> mapAllowed      = new LinkedHashMap<>();
        Map<String, String> mapExecutable   = new LinkedHashMap<>();
        Map<String, String> mapLambdaTarget = new LinkedHashMap<>();

        for (ClassInfo info : mapClasses.values())
            {
            if (info.isPackageInfo() && info.getAllowed() != null)
                {
                String  sPackage  = packageName(info.getInternalName());
                boolean fRecursive = info.getAllowed().isRecursive();

                for (ClassInfo candidate : mapClasses.values())
                    {
                    if (!candidate.isPackageInfo() && isInPackage(candidate.getInternalName(), sPackage, fRecursive))
                        {
                        add(mapAllowed, candidate.getClassName(), SOURCE_REMOTE_ALLOWED_PACKAGE);
                        }
                    }
                }
            }

        for (ClassInfo info : mapClasses.values())
            {
            if (!info.isPackageInfo() && info.getAllowed() != null)
                {
                add(mapAllowed, info.getClassName(), SOURCE_REMOTE_ALLOWED);
                if (info.getAllowed().isRecursive())
                    {
                    addInnerClasses(mapClasses, mapAllowed, info);
                    }
                }
            }

        for (ClassInfo info : mapClasses.values())
            {
            if (!info.isPackageInfo() && info.isExecutable())
                {
                if (info.isConcreteClass())
                    {
                    add(mapExecutable, info.getClassName(), SOURCE_REMOTE_EXECUTABLE);
                    add(mapAllowed, info.getClassName(), SOURCE_REMOTE_EXECUTABLE);
                    }
                else if (info.isInterface())
                    {
                    if (info.isFunctionalInterface())
                        {
                        if (isSerializable(info, mapClasses))
                            {
                            add(mapLambdaTarget, info.getClassName(), SOURCE_REMOTE_EXECUTABLE);
                            add(mapAllowed, info.getClassName(), SOURCE_REMOTE_EXECUTABLE);
                            }
                        else
                            {
                            errorNonSerializableLambdaTarget(info);
                            }
                        }
                    else
                        {
                        warnInvalidLambdaTarget(info);
                        }
                    }
                }
            }

        for (ClassInfo info : mapClasses.values())
            {
            if (!info.isPackageInfo() && info.isPortableType())
                {
                // @PortableType is a stronger source attribution for duplicate matches.
                add(mapAllowed, info.getClassName(), SOURCE_PORTABLE_TYPE);
                }
            }

        List<Entry> listEntries = new ArrayList<>(mapAllowed.size());
        for (Map.Entry<String, String> entry : mapAllowed.entrySet())
            {
            String  sName       = entry.getKey();
            boolean fExecutable = mapExecutable.containsKey(sName);
            boolean fLambda     = mapLambdaTarget.containsKey(sName);
            listEntries.add(new Entry(sName, fExecutable || fLambda ? SOURCE_REMOTE_EXECUTABLE : entry.getValue(),
                    fExecutable, fLambda));
            }
        Collections.sort(listEntries);

        return new GeneratedConfig(listEntries);
        }

    /**
     * Serialize a generated config as XML.
     *
     * @param config  the generated config
     *
     * @return the XML bytes
     */
    public static byte[] toXml(GeneratedConfig config)
        {
        GeneratedConfig generated = config == null
                ? new GeneratedConfig(Collections.<Entry>emptyList())
                : config;

        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\"?>\n")
          .append("<security-config xmlns=\"").append(NAMESPACE).append("\"\n")
          .append("                 version=\"1.0\">\n")
          .append("  <allowed-classes");

        if (generated.getEntries().isEmpty())
            {
            sb.append("/>\n");
            }
        else
            {
            sb.append(">\n");
            for (Entry entry : generated.getEntries())
                {
                sb.append("    <class name=\"").append(escape(entry.getName())).append("\"")
                  .append(" source=\"").append(escape(entry.getSource())).append("\"");
                if (entry.isExecutable())
                    {
                    sb.append(" executable=\"true\"");
                    }
                if (entry.isLambdaTarget())
                    {
                    sb.append(" lambda-target=\"true\"");
                    }
                sb.append("/>\n");
                }
            sb.append("  </allowed-classes>\n");
            }

        sb.append("</security-config>\n");

        byte[] abXml = sb.toString().getBytes(StandardCharsets.UTF_8);
        validate(abXml);
        return abXml;
        }

    /**
     * Scan and write {@code META-INF/coherence/security-config.xml}.
     *
     * @param classesDir  compiled-classes directory
     *
     * @return the generated file path
     *
     * @throws IOException if class files cannot be read or output cannot be written
     */
    public static Path generateAndWrite(Path classesDir)
            throws IOException
        {
        GeneratedConfig config = generate(classesDir);
        Path            path   = classesDir.resolve(OUTPUT_RESOURCE);
        byte[]          abXml  = Files.exists(path) ? toMergedXml(path, config) : toXml(config);

        Files.createDirectories(path.getParent());
        if (Files.exists(path))
            {
            path.toFile().setWritable(true);
            }
        Files.write(path, abXml);
        return path;
        }

    /**
     * Command-line entry point used by the build.
     *
     * @param asArgs  the command-line arguments
     *
     * @throws IOException if class files cannot be read or output cannot be written
     */
    public static void main(String[] asArgs)
            throws IOException
        {
        if (asArgs.length != 1)
            {
            throw new IllegalArgumentException("Usage: SecurityConfigGenerator <classesDir>");
            }
        generateAndWrite(Path.of(asArgs[0]));
        }

    // ----- helper methods -------------------------------------------------

    private static ClassInfo scan(Path path)
            throws IOException
        {
        ClassInfoVisitor visitor = new ClassInfoVisitor();
        new ClassReader(Files.readAllBytes(path)).accept(visitor, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return visitor.getClassInfo();
        }

    private static byte[] toMergedXml(Path path, GeneratedConfig config)
            throws IOException
        {
        Map<String, ClassEntry> mapEntries = new LinkedHashMap<>();

        readExisting(path, mapEntries);
        for (Entry entry : config.getEntries())
            {
            mapEntries.putIfAbsent(entry.getName(), ClassEntry.fromGenerated(entry));
            }

        List<ClassEntry> listEntries = new ArrayList<>(mapEntries.values());
        Collections.sort(listEntries);
        return toXml(listEntries);
        }

    private static void readExisting(Path path, Map<String, ClassEntry> mapEntries)
            throws IOException
        {
        Document document = parse(path);
        NodeList listClass = document.getElementsByTagNameNS(NAMESPACE, "class");
        for (int i = 0; i < listClass.getLength(); i++)
            {
            ClassEntry entry = ClassEntry.fromElement((Element) listClass.item(i));
            mapEntries.putIfAbsent(entry.getName(), entry);
            }
        }

    private static Document parse(Path path)
            throws IOException
        {
        try (InputStream in = Files.newInputStream(path))
            {
            DocumentBuilder builder = newDocumentBuilder();
            builder.setErrorHandler(THROWING_ERROR_HANDLER);
            return builder.parse(in);
            }
        catch (ParserConfigurationException | SAXException e)
            {
            throw new IOException("Unable to parse existing " + OUTPUT_RESOURCE + " at " + path + ": "
                    + e.getMessage(), e);
            }
        }

    private static byte[] toXml(List<ClassEntry> listEntries)
        {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\"?>\n")
          .append("<security-config xmlns=\"").append(NAMESPACE).append("\"\n")
          .append("                 version=\"1.0\">\n")
          .append("  <allowed-classes");

        if (listEntries.isEmpty())
            {
            sb.append("/>\n");
            }
        else
            {
            sb.append(">\n");
            for (ClassEntry entry : listEntries)
                {
                sb.append("    <class");
                for (Map.Entry<String, String> attribute : entry.getAttributes().entrySet())
                    {
                    sb.append(" ").append(attribute.getKey()).append("=\"")
                      .append(escape(attribute.getValue())).append("\"");
                    }
                sb.append("/>\n");
                }
            sb.append("  </allowed-classes>\n");
            }

        sb.append("</security-config>\n");

        byte[] abXml = sb.toString().getBytes(StandardCharsets.UTF_8);
        validate(abXml);
        return abXml;
        }

    private static DocumentBuilder newDocumentBuilder()
            throws ParserConfigurationException, SAXException, IOException
        {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        factory.setNamespaceAware(true);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        factory.setSchema(schema());
        return factory.newDocumentBuilder();
        }

    private static Schema schema()
            throws SAXException, IOException
        {
        try (InputStream inSchema = SecurityConfigGenerator.class.getResourceAsStream("/coherence-security-config.xsd"))
            {
            if (inSchema == null)
                {
                throw new IOException("coherence-security-config.xsd not found on the classpath");
                }

            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            return factory.newSchema(new StreamSource(inSchema));
            }
        }

    private static boolean isClassFile(Path path)
        {
        String sName = path.getFileName().toString();
        return sName.endsWith(".class")
                && !sName.equals("module-info.class")
                && !sName.contains("$Lambda$")
                && !sName.contains("$$Lambda$");
        }

    private static void addInnerClasses(Map<String, ClassInfo> mapClasses, Map<String, String> mapAllowed, ClassInfo info)
        {
        for (String sInnerName : info.getInnerNames())
            {
            ClassInfo inner = mapClasses.get(sInnerName);
            if (inner != null && !inner.isPackageInfo())
                {
                add(mapAllowed, inner.getClassName(), SOURCE_REMOTE_ALLOWED);
                addInnerClasses(mapClasses, mapAllowed, inner);
                }
            }
        }

    private static void add(Map<String, String> mapAllowed, String sClassName, String sSource)
        {
        String sCurrent = mapAllowed.get(sClassName);
        if (sCurrent == null || rank(sSource) > rank(sCurrent))
            {
            mapAllowed.put(sClassName, sSource);
            }
        }

    private static void warnInvalidLambdaTarget(ClassInfo info)
        {
        String sMessage = "@Remote.Executable on interface " + info.getClassName()
                + " did not emit lambda-target=true because @FunctionalInterface is missing";
        LOGGER.warning(sMessage);
        }

    private static void errorNonSerializableLambdaTarget(ClassInfo info)
        {
        String sMessage = "@Remote.Executable on interface " + info.getClassName()
                + " did not emit lambda-target=true because the interface does not transitively implement java.io.Serializable";
        LOGGER.severe(sMessage);
        if (Boolean.getBoolean(PROP_SECURITY_CONFIG_STRICT))
            {
            throw new IllegalStateException(sMessage);
            }
        }

    private static boolean isSerializable(ClassInfo info, Map<String, ClassInfo> mapClasses)
        {
        return implementsSerializable(info.getInterfaces(), mapClasses, new HashSet<>());
        }

    private static boolean implementsSerializable(Collection<String> colInterfaces, Map<String, ClassInfo> mapClasses,
            Set<String> setVisited)
        {
        for (String sInterface : colInterfaces)
            {
            if (SERIALIZABLE_INTERNAL_NAME.equals(sInterface))
                {
                return true;
                }

            if (setVisited.add(sInterface))
                {
                ClassInfo info = mapClasses.get(sInterface);
                if (info == null)
                    {
                    info = loadClassInfo(sInterface);
                    }
                if (info != null && implementsSerializable(info.getInterfaces(), mapClasses, setVisited))
                    {
                    return true;
                    }
                }
            }
        return false;
        }

    private static ClassInfo loadClassInfo(String sInternalName)
        {
        String sResource = sInternalName + ".class";
        try (InputStream in = openClassResource(sResource))
            {
            if (in == null)
                {
                return null;
                }
            ClassInfoVisitor visitor = new ClassInfoVisitor();
            new ClassReader(in).accept(visitor, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            return visitor.getClassInfo();
            }
        catch (IOException e)
            {
            return null;
            }
        }

    private static InputStream openClassResource(String sResource)
        {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream in = loader == null ? null : loader.getResourceAsStream(sResource);
        if (in == null)
            {
            in = SecurityConfigGenerator.class.getClassLoader().getResourceAsStream(sResource);
            }
        return in == null ? ClassLoader.getSystemResourceAsStream(sResource) : in;
        }

    private static int rank(String sSource)
        {
        if (SOURCE_REMOTE_EXECUTABLE.equals(sSource))
            {
            return 4;
            }
        if (SOURCE_PORTABLE_TYPE.equals(sSource))
            {
            return 3;
            }
        if (SOURCE_REMOTE_ALLOWED.equals(sSource))
            {
            return 2;
            }
        return 1;
        }

    private static boolean isInPackage(String sInternalName, String sPackage, boolean fRecursive)
        {
        String sClassPackage = packageName(sInternalName);
        return fRecursive
                ? sClassPackage.equals(sPackage) || sClassPackage.startsWith(sPackage + ".")
                : sClassPackage.equals(sPackage);
        }

    private static String packageName(String sInternalName)
        {
        int of = sInternalName.lastIndexOf('/');
        return of < 0 ? "" : sInternalName.substring(0, of).replace('/', '.');
        }

    private static String className(String sInternalName)
        {
        return sInternalName.replace('/', '.');
        }

    private static String escape(String s)
        {
        return s.replace("&", "&amp;")
                .replace("\"", "&quot;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
        }

    private static void validate(byte[] abXml)
        {
        try (InputStream inSchema = SecurityConfigGenerator.class.getResourceAsStream("/coherence-security-config.xsd"))
            {
            if (inSchema == null)
                {
                throw new IllegalStateException("coherence-security-config.xsd not found on the classpath");
                }

            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Validator     validator = factory.newSchema(new StreamSource(inSchema)).newValidator();

            validator.validate(new StreamSource(new ByteArrayInputStream(abXml)));
            }
        catch (IOException | SAXException e)
            {
            throw new IllegalStateException("Generated security-config.xml is invalid: "
                    + e.getMessage() + "\n" + new String(abXml, StandardCharsets.UTF_8), e);
            }
        }

    // ----- inner class: ClassEntry --------------------------------------

    private static final class ClassEntry
            implements Comparable<ClassEntry>
        {
        private ClassEntry(Map<String, String> mapAttributes)
            {
            m_mapAttributes = Collections.unmodifiableMap(new LinkedHashMap<>(mapAttributes));
            }

        private static ClassEntry fromGenerated(Entry entry)
            {
            Map<String, String> mapAttributes = new LinkedHashMap<>();

            mapAttributes.put("name", entry.getName());
            mapAttributes.put("source", entry.getSource());
            if (entry.isExecutable())
                {
                mapAttributes.put("executable", "true");
                }
            if (entry.isLambdaTarget())
                {
                mapAttributes.put("lambda-target", "true");
                }
            return new ClassEntry(mapAttributes);
            }

        private static ClassEntry fromElement(Element element)
            {
            Map<String, String> mapAttributes = new LinkedHashMap<>();
            NamedNodeMap        mapNodes      = element.getAttributes();

            for (int i = 0; i < mapNodes.getLength(); i++)
                {
                Node node = mapNodes.item(i);
                if (node instanceof Attr && ((Attr) node).getSpecified())
                    {
                    mapAttributes.put(node.getNodeName(), node.getNodeValue());
                    }
                }
            return new ClassEntry(mapAttributes);
            }

        private String getName()
            {
            return m_mapAttributes.get("name");
            }

        private Map<String, String> getAttributes()
            {
            return m_mapAttributes;
            }

        @Override
        public int compareTo(ClassEntry that)
            {
            return getName().compareTo(that.getName());
            }

        private final Map<String, String> m_mapAttributes;
        }

    // ----- inner class: GeneratedConfig ----------------------------------

    /**
     * Generated config entries.
     */
    public static final class GeneratedConfig
        {
        private GeneratedConfig(List<Entry> listEntries)
            {
            m_listEntries = Collections.unmodifiableList(new ArrayList<>(listEntries));
            }

        /**
         * Return the entries.
         *
         * @return the entries
         */
        public List<Entry> getEntries()
            {
            return m_listEntries;
            }

        /**
         * Return the entry count.
         *
         * @return the entry count
         */
        public int size()
            {
            return m_listEntries.size();
            }

        // ----- data members ----------------------------------------------

        private final List<Entry> m_listEntries;
        }

    // ----- inner class: Entry --------------------------------------------

    /**
     * Generated allowed-class entry.
     */
    public static final class Entry
            implements Comparable<Entry>
        {
        private Entry(String sName, String sSource, boolean fExecutable)
            {
            this(sName, sSource, fExecutable, false);
            }

        private Entry(String sName, String sSource, boolean fExecutable, boolean fLambdaTarget)
            {
            m_sName         = sName;
            m_sSource       = sSource;
            m_fExecutable   = fExecutable;
            m_fLambdaTarget = fLambdaTarget;
            }

        /**
         * Return the class name.
         *
         * @return the class name
         */
        public String getName()
            {
            return m_sName;
            }

        /**
         * Return the source attribution.
         *
         * @return the source attribution
         */
        public String getSource()
            {
            return m_sSource;
            }

        /**
         * Return whether the entry is executable.
         *
         * @return {@code true} if the entry is executable
         */
        public boolean isExecutable()
            {
            return m_fExecutable;
            }

        /**
         * Return whether the entry is an allowed lambda target.
         *
         * @return {@code true} if the entry is an allowed lambda target
         */
        public boolean isLambdaTarget()
            {
            return m_fLambdaTarget;
            }

        @Override
        public int compareTo(Entry that)
            {
            return m_sName.compareTo(that.m_sName);
            }

        // ----- data members ----------------------------------------------

        private final String m_sName;

        private final String m_sSource;

        private final boolean m_fExecutable;

        private final boolean m_fLambdaTarget;
        }

    // ----- inner class: ClassInfo ----------------------------------------

    private static final class ClassInfo
        {
        private ClassInfo(String sInternalName, int nAccess, boolean fPortableType, boolean fExecutable,
                          boolean fFunctionalInterface,
                          Allowed allowed, Collection<String> colInnerNames, Collection<String> colInterfaces)
            {
            m_sInternalName        = sInternalName;
            m_nAccess              = nAccess;
            m_fPortableType        = fPortableType;
            m_fExecutable          = fExecutable;
            m_fFunctionalInterface = fFunctionalInterface;
            m_allowed              = allowed;
            m_listInnerName        = new ArrayList<>(colInnerNames);
            m_listInterface        = new ArrayList<>(colInterfaces);
            }

        private String getInternalName()
            {
            return m_sInternalName;
            }

        private String getClassName()
            {
            return className(m_sInternalName);
            }

        private boolean isPackageInfo()
            {
            return m_sInternalName.endsWith("/package-info");
            }

        private boolean isPortableType()
            {
            return m_fPortableType;
            }

        private boolean isExecutable()
            {
            return m_fExecutable;
            }

        private boolean isFunctionalInterface()
            {
            return m_fFunctionalInterface;
            }

        private boolean isInterface()
            {
            return (m_nAccess & ACCESS_INTERFACE) != 0;
            }

        private boolean isConcreteClass()
            {
            return !isInterface() && (m_nAccess & ACCESS_ABSTRACT) == 0;
            }

        private Allowed getAllowed()
            {
            return m_allowed;
            }

        private List<String> getInnerNames()
            {
            return m_listInnerName;
            }

        private List<String> getInterfaces()
            {
            return m_listInterface;
            }

        private final String m_sInternalName;

        private final int m_nAccess;

        private final boolean m_fPortableType;

        private final boolean m_fExecutable;

        private final boolean m_fFunctionalInterface;

        private final Allowed m_allowed;

        private final List<String> m_listInnerName;

        private final List<String> m_listInterface;

        private static final int ACCESS_INTERFACE = 0x0200;

        private static final int ACCESS_ABSTRACT = 0x0400;
        }

    // ----- inner class: Allowed ------------------------------------------

    private static final class Allowed
        {
        private Allowed(boolean fRecursive)
            {
            m_fRecursive = fRecursive;
            }

        private boolean isRecursive()
            {
            return m_fRecursive;
            }

        private final boolean m_fRecursive;
        }

    // ----- inner class: ClassInfoVisitor ---------------------------------

    private static final class ClassInfoVisitor
            extends ClassVisitor
        {
        private ClassInfoVisitor()
            {
            super(Opcodes.ASM9);
            }

        @Override
        public void visit(int nVersion, int nAccess, String sName, String sSignature, String sSuperName, String[] asInterfaces)
            {
            m_sInternalName = sName;
            m_nAccess       = nAccess;
            if (asInterfaces != null)
                {
                Collections.addAll(m_listInterface, asInterfaces);
                }
            }

        @Override
        public AnnotationVisitor visitAnnotation(String sDescriptor, boolean fVisible)
            {
            if (DESCRIPTOR_REMOTE_ALLOWED.equals(sDescriptor))
                {
                m_allowed = new Allowed(true);
                return new AnnotationVisitor(Opcodes.ASM9)
                    {
                    @Override
                    public void visit(String sName, Object oValue)
                        {
                        if ("recursive".equals(sName) && oValue instanceof Boolean)
                            {
                            m_allowed = new Allowed(((Boolean) oValue).booleanValue());
                            }
                        }
                    };
                }
            if (DESCRIPTOR_PORTABLE_TYPE.equals(sDescriptor))
                {
                m_fPortableType = true;
                }
            if (DESCRIPTOR_REMOTE_EXECUTABLE.equals(sDescriptor))
                {
                m_fExecutable = true;
                }
            if (DESCRIPTOR_FUNCTIONAL_INTERFACE.equals(sDescriptor))
                {
                m_fFunctionalInterface = true;
                }
            return null;
            }

        @Override
        public void visitInnerClass(String sName, String sOuterName, String sInnerName, int nAccess)
            {
            if (m_sInternalName != null && m_sInternalName.equals(sOuterName))
                {
                m_listInnerName.add(sName);
                }
            }

        private ClassInfo getClassInfo()
            {
            return new ClassInfo(m_sInternalName, m_nAccess, m_fPortableType, m_fExecutable, m_fFunctionalInterface,
                    m_allowed, m_listInnerName, m_listInterface);
            }

        private String m_sInternalName;

        private int m_nAccess;

        private boolean m_fPortableType;

        private boolean m_fExecutable;

        private boolean m_fFunctionalInterface;

        private Allowed m_allowed;

        private final List<String> m_listInnerName = new ArrayList<>();

        private final List<String> m_listInterface = new ArrayList<>();
        }

    // ----- inner class: ReadFailure --------------------------------------

    private static final class ReadFailure
            extends RuntimeException
        {
        private ReadFailure(IOException cause)
            {
            super(cause);
            }

        @Override
        public IOException getCause()
            {
            return (IOException) super.getCause();
            }
        }

    // ----- constants ------------------------------------------------------

    /**
     * Generated security config resource path.
     */
    public static final String OUTPUT_RESOURCE = "META-INF/coherence/security-config.xml";

    /**
     * Security config XML namespace.
     */
    public static final String NAMESPACE = "http://xmlns.oracle.com/coherence/coherence-security-config";

    /**
     * Type-level {@code @Remote.Allowed} source.
     */
    public static final String SOURCE_REMOTE_ALLOWED = "@Remote.Allowed";

    /**
     * Package-level {@code @Remote.Allowed} source.
     */
    public static final String SOURCE_REMOTE_ALLOWED_PACKAGE = "@Remote.Allowed(package)";

    /**
     * Type-level {@code @Remote.Executable} source.
     */
    public static final String SOURCE_REMOTE_EXECUTABLE = "@Remote.Executable";

    /**
     * {@code @PortableType} source.
     */
    public static final String SOURCE_PORTABLE_TYPE = "@PortableType";

    private static final String DESCRIPTOR_REMOTE_ALLOWED = "Lcom/tangosol/util/function/Remote$Allowed;";

    private static final String DESCRIPTOR_REMOTE_EXECUTABLE = "Lcom/tangosol/util/function/Remote$Executable;";

    private static final String DESCRIPTOR_FUNCTIONAL_INTERFACE = "Ljava/lang/FunctionalInterface;";

    private static final String DESCRIPTOR_PORTABLE_TYPE = "Lcom/tangosol/io/pof/schema/annotation/PortableType;";

    private static final String SERIALIZABLE_INTERNAL_NAME = "java/io/Serializable";

    private static final String PROP_SECURITY_CONFIG_STRICT = "security.config.strict";

    private static final ErrorHandler THROWING_ERROR_HANDLER = new ErrorHandler()
        {
        @Override
        public void warning(SAXParseException e)
                throws SAXException
            {
            throw e;
            }

        @Override
        public void error(SAXParseException e)
                throws SAXException
            {
            throw e;
            }

        @Override
        public void fatalError(SAXParseException e)
                throws SAXException
            {
            throw e;
            }
        };

    private static final Logger LOGGER = Logger.getLogger(SecurityConfigGenerator.class.getName());
    }
