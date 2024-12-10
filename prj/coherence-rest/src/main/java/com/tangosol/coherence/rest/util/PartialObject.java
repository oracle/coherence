/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest.util;

import com.tangosol.util.asm.BaseClassReaderInternal;

import com.tangosol.util.Base;
import com.tangosol.util.CopyOnWriteMap;
import com.tangosol.util.UID;

import java.io.IOException;
import java.io.InputStream;

import java.lang.reflect.Constructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;

import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.DRETURN;
import static org.objectweb.asm.Opcodes.FRETURN;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.IRETURN;
import static org.objectweb.asm.Opcodes.LRETURN;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Opcodes.V1_8;

/**
 * Base class for dynamically generated partial classes.
 *
 * @author as  2011.06.29
 */
public class PartialObject
    {

    // ----- constructors ---------------------------------------------------

    /**
     * Construct a PartialObject instance.
     */
    public PartialObject()
        {
        }

    /**
     * Construct a PartialObject instance.
     *
     * @param mapProperties  map containing property values for this partial
     *                       object
     */
    public PartialObject(Map mapProperties)
        {
        m_mapProperties = mapProperties;
        }

    // ----- methods --------------------------------------------------------

    /**
     * Get a property value.
     *
     * @param sName  property name
     *
     * @return property value, or null if the property does not exist
     */
    public Object get(String sName)
        {
        return m_mapProperties.get(sName);
        }

    // ----- Object methods -------------------------------------------------

    @Override
    public String toString()
        {
        return "PartialObject{" +
               "properties=" + m_mapProperties +
               '}';
        }

    // ----- factory methods ------------------------------------------------

    /**
     * Create a PartialObject instance.
     *
     * @param oSource      object to extract properties from
     * @param propertySet  properties to extract
     *
     * @return partial object containing the supplied subset of properties
     *         of the specified object
     */
    public static Object create(Object oSource, PropertySet propertySet)
        {
        return propertySet.extract(oSource);
        }

    /**
     * Create a PartialObject instance based on an already extracted set of
     * properties.
     *
     * @param clzSource      class of the object properties were extracted from
     * @param propertySet    extracted properties
     * @param mapProperties  extracted property values
     *
     * @return partial object containing the supplied subset of properties
     *         of the specified object
     */
    public static PartialObject create(Class clzSource,
            PropertySet propertySet, Map<String, Object> mapProperties)
        {
        try
            {
            String sKey = createKey(clzSource, propertySet);

            ConcurrentMap<String, Constructor> mapPartial =
                    getPartialConstructorMap(getPartialClassLoader());

            Constructor ctorPartial = mapPartial.get(sKey);
            if (ctorPartial == null)
                {
                Class clzPartial = getPartialClass(clzSource, propertySet);
                ctorPartial = clzPartial.getConstructor(Map.class);
                Constructor ctorOld = mapPartial.putIfAbsent(sKey, ctorPartial);
                if (ctorOld != null)
                    {
                    ctorPartial = ctorOld;
                    }
                }

            return (PartialObject) ctorPartial.newInstance(mapProperties);
            }
        catch (Exception e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }

    /**
     * Return a partial class for the specified source class and property set.
     *
     * @param clzSource    source class to base the partial class on
     * @param propertySet  a set of properties to define
     *
     * @return class containing only the specified set of properties
     */
    public static Class getPartialClass(Class clzSource, PropertySet propertySet)
        {
        String sKey = createKey(clzSource, propertySet);

        ConcurrentMap<String, Class> mapPartial =
                getPartialClassMap(getPartialClassLoader());;

        Class clzPartial = mapPartial.get(sKey);
        if (clzPartial == null)
            {
            clzPartial = createPartialClass(clzSource, propertySet);
            Class clzOld = mapPartial.putIfAbsent(sKey, clzPartial);
            if (clzOld != null)
                {
                clzPartial = clzOld;
                }
            }

        return clzPartial;
        }

    /**
     * Create a partial class for the specified source class and property set.
     *
     * @param clzSource    a source class to base partial the class on
     * @param propertySet  a set of properties to define
     *
     * @return class containing only the specified set of properties
     */
    public static <T> Class createPartialClass(Class clzSource, PropertySet<T> propertySet)
        {
        ClassNode cn = new ClassNode();
        cn.version   = V1_8;
        cn.access    = ACC_PUBLIC;
        cn.superName = "com/tangosol/coherence/rest/util/PartialObject";
        cn.name      = "com/tangosol/coherence/rest/util/gen/partial/"
                + clzSource.getSimpleName() + "_" + new UID();

        ClassNode cnSrc = getClassNode(clzSource);

        copyClassAnnotations(cnSrc, cn);
        createConstructors(cn);

        List<ClassNode> listClassNodes = new ArrayList<ClassNode>();
        while (!clzSource.equals(Object.class))
            {
            listClassNodes.add(cnSrc);
            clzSource = clzSource.getSuperclass();
            cnSrc = getClassNode(clzSource);
            }

        for (PropertySpec p : propertySet)
            {
            MethodNode mn = findProperty(listClassNodes, p.getName());
            if (mn != null)
                {
                // change return type if this is partial property
                if (p.isPartial())
                    {
                    mn.desc = "()" + Type.getDescriptor(p.getPartialClass());
                    mn.signature = null;
                    }

                // implement getter to delegate to PartialObject.get()
                mn.instructions.clear();
                mn.instructions.add(new VarInsnNode(ALOAD, 0));
                mn.instructions.add(new LdcInsnNode(p.getName()));
                mn.instructions.add(
                        new MethodInsnNode(INVOKEVIRTUAL,
                                           cn.name,
                                           "get",
                                           "(Ljava/lang/String;)Ljava/lang/Object;", false));
                castReturnValue(mn);
                cn.methods.add(mn);

                // create dummy setter as well, in order for JAXB to work properly
                Character chFirst     = p.getName().charAt(0);
                String    sSetterName = "set" + chFirst.toString().toUpperCase()
                        + p.getName().substring(1);

                MethodNode setter = new MethodNode(ACC_PUBLIC,
                        sSetterName,
                        "(" + Type.getReturnType(mn.desc) +")V",
                        null, null);
                setter.instructions.add(new InsnNode(RETURN));
                cn.methods.add(setter);
                }
            }

        return createClass(cn);
        }

    /**
     * Create key for the partial class/constructor cache.
     *
     * @param clzSource    source class to base the partial class on
     * @param propertySet  a set of properties to define
     *
     * @return lookup key
     */
    protected static String createKey(Class clzSource, PropertySet propertySet)
        {
        return clzSource.getName() + "(" + propertySet + ")";
        }

    /**
     * Copy class-level annotations.
     *
     * @param cnSource  source class to copy annotations from
     * @param cnTarget  target class to copy annotations to
     */
    protected static void copyClassAnnotations(ClassNode cnSource, ClassNode cnTarget)
        {
        List<AnnotationNode> listAnnotations =
                (List<AnnotationNode>) cnSource.visibleAnnotations;
        if (listAnnotations != null)
            {
            for (AnnotationNode an : listAnnotations)
                {
                if (!an.desc.equals("Lcom/fasterxml/jackson/annotation/JsonTypeInfo;"))
                    {
                    if (cnTarget.visibleAnnotations == null)
                        {
                        cnTarget.visibleAnnotations =
                                new ArrayList<AnnotationNode>(cnSource.visibleAnnotations.size());
                        }
                    cnTarget.visibleAnnotations.add(an);
                    }
                }
            }
        }

    /**
     * Implement partial class constructors.
     *
     * @param cn  partial class
     */
    protected static void createConstructors(ClassNode cn)
        {
        // default constructor
        MethodNode ctorDefault = new MethodNode(ACC_PUBLIC, "<init>", "()V", null, null);
        ctorDefault.instructions.add(new VarInsnNode(ALOAD, 0));
        ctorDefault.instructions.add(
                new MethodInsnNode(INVOKESPECIAL,
                        "com/tangosol/coherence/rest/util/PartialObject",
                        "<init>",
                        "()V", false));
        ctorDefault.instructions.add(new InsnNode(RETURN));
        cn.methods.add(ctorDefault);

        // Map constructor
        MethodNode ctorMap = new MethodNode(ACC_PUBLIC, "<init>", "(Ljava/util/Map;)V", null, null);
        ctorMap.instructions.add(new VarInsnNode(ALOAD, 0));
        ctorMap.instructions.add(new VarInsnNode(ALOAD, 1));
        ctorMap.instructions.add(
                new MethodInsnNode(INVOKESPECIAL,
                        "com/tangosol/coherence/rest/util/PartialObject",
                        "<init>",
                        "(Ljava/util/Map;)V", false));
        ctorMap.instructions.add(new InsnNode(RETURN));
        cn.methods.add(ctorMap);
        }

    /**
     * Cast a return value into an appropriate type.
     *
     * @param mn  method whose return value needs to be casted
     */
    protected static void castReturnValue(MethodNode mn)
        {
        Type   typeReturn = Type.getReturnType(mn.desc);
        String sType      = typeReturn.getDescriptor();

        if (sType.length() > 1)
            {
            // object or array type -- simply cast to internal name
            mn.instructions.add(new TypeInsnNode(CHECKCAST, typeReturn.getInternalName()));
            mn.instructions.add(new InsnNode(ARETURN));
            }
        else
            {
            String sClass;
            String sMethod;
            int    nRetCode = IRETURN;

            char cType = sType.charAt(0);
            switch (cType)
                {
                case 'Z':
                    sClass  = "java/lang/Boolean";
                    sMethod = "booleanValue";
                    break;

                case 'B':
                    sClass  = "java/lang/Byte";
                    sMethod = "byteValue";
                    break;

                case 'C':
                    sClass  = "java/lang/Character";
                    sMethod = "charValue";
                    break;

                case 'S':
                    sClass  = "java/lang/Short";
                    sMethod = "shortValue";
                    break;

                case 'I':
                    sClass  = "java/lang/Integer";
                    sMethod = "intValue";
                    break;

                case 'J':
                    sClass  = "java/lang/Long";
                    sMethod = "longValue";
                    nRetCode = LRETURN;
                    break;

                case 'F':
                    sClass  = "java/lang/Float";
                    sMethod = "floatValue";
                    nRetCode = FRETURN;
                    break;

                case 'D':
                    sClass  = "java/lang/Double";
                    sMethod = "doubleValue";
                    nRetCode = DRETURN;
                    break;

                default:
                    throw new IllegalArgumentException("unsupported type: "
                            + sType);
                }

            mn.instructions.add(new TypeInsnNode(CHECKCAST, sClass));
            mn.instructions.add(
                    new MethodInsnNode(INVOKEVIRTUAL,
                                       sClass,
                                       sMethod,
                                       "()" + cType, false));
            mn.instructions.add(new InsnNode(nRetCode));
            }
        }

    /**
     * Create partial class based on partial class definition.
     *
     * @param cn  partial class definition
     *
     * @return partial class
     */
    protected static Class createClass(ClassNode cn)
        {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);

        return getPartialClassLoader().defineClass(cn.name.replace('/', '.'),
                cw.toByteArray());
        }

    /**
     * Attempt to find a property with a given name in one of the specified
     * class definitions.
     *
     * @param listClassNodes  class definitions to search
     * @param sName           method name
     *
     * @return method definition if found, <tt>null</tt> otherwise
     */
    protected static MethodNode findProperty(List<ClassNode> listClassNodes, String sName)
        {
        for (ClassNode cn : listClassNodes)
            {
            MethodNode mn = findProperty(cn, sName);
            if (mn != null)
                {
                return mn;
                }
            }

        return null;
        }

    /**
     * Attempt to find a property getter with a given name in the specified
     * class definitions.
     *
     * @param cn     class definition to search
     * @param sName  method name
     *
     * @return method definition if found, <tt>null</tt> otherwise
     */
    protected static MethodNode findProperty(ClassNode cn, String sName)
        {
        for (MethodNode mn : (List<MethodNode>) cn.methods)
            {
            if (mn.desc.startsWith("()") && isMatch(mn.name, sName))
                {
                return mn;
                }
            }

        return null;
        }

    /**
     * Determine if a method represents a property.
     *
     * @param sMethodName    name of the method
     * @param sPropertyName  name of the property
     *
     * @return <tt>true</tt> if the specified method represents the specified
     *         property, <tt>false</tt> otherwise
     */
    protected static boolean isMatch(String sMethodName, String sPropertyName)
        {
        if (sMethodName.startsWith("get"))
            {
            sMethodName = sMethodName.substring(3);
            }
        else if (sMethodName.startsWith("is"))
            {
            sMethodName = sMethodName.substring(2);
            }

        return sMethodName.equalsIgnoreCase(sPropertyName);
        }

    /**
     * Return a class definition for the specified class.
     *
     * @param clz  class to get a definition for
     *
     * @return class definition for the specified class
     */
    protected static ClassNode getClassNode(Class clz)
        {
        InputStream classStream = null;
        try
            {
            ClassNode cn = new ClassNode();
            classStream = getPartialClassLoader().getResourceAsStream(
                    clz.getName().replace('.', '/') + ".class");

            ClassReaderInternal cr = new ClassReaderInternal(classStream);
            cr.accept(cn, 0);
            return cn;
            }
        catch (IOException e)
            {
            throw Base.ensureRuntimeException(e);
            }
        finally
            {
            if (classStream != null)
                {
                try
                    {
                    classStream.close();
                    }
                catch (IOException e)
                    {
                    }
                }
            }
        }
    
    /**
     * Return a PartialClassLoader corresponding to the Context ClassLoader.
     *
     * @return PartialClassLoader
     */
    protected static PartialClassLoader getPartialClassLoader()
        {
        ClassLoader contextLoader = Base.getContextClassLoader();
        PartialClassLoader loader = s_mapPartialClassLoaders.get(contextLoader);
        if (loader == null)
            {
            synchronized (s_mapPartialClassLoaders) 
                {
                loader = s_mapPartialClassLoaders.get(contextLoader);
                if (loader == null)
                    {
                    loader  = new PartialClassLoader(contextLoader);
                    s_mapPartialClassLoaders.put(contextLoader, loader);
                    }
                }
            }
        return loader;
        }
    
    /**
     * Return the partial class map corresponding to the PartialClassLoader.
     *
     * @param loader  the PartialClassLoader corresponding to which the partial class map is required
     * 
     * @return the map of Partial classes corresponding to the PartialClassLoader
     */
    protected static ConcurrentHashMap<String, Class> getPartialClassMap(PartialClassLoader loader)
        {
        ConcurrentHashMap<String, Class> mapPartialClasses = s_mapPartialClasses.get(loader);
        if (mapPartialClasses == null)
            {
            synchronized (loader) 
                {
                mapPartialClasses = s_mapPartialClasses.get(loader);
                if (mapPartialClasses == null)
                    {
                    mapPartialClasses  = new ConcurrentHashMap<String, Class>();
                    s_mapPartialClasses.put(loader, mapPartialClasses);
                    }
                }
            }
        return mapPartialClasses;
        }    

    /**
     * Return the partial constructor map corresponding to the PartialClassLoader.
     * 
     * @param loader  the PartialClassLoader corresponding to which the partial constructor map is required
     *
     * @return the map of Partial constructors corresponding to the PartialClassLoader
     */
    protected static ConcurrentHashMap<String, Constructor> getPartialConstructorMap(PartialClassLoader loader)
        {
        ConcurrentHashMap<String, Constructor> mapPartialConstructors = s_mapPartialConstructors.get(loader);
        if (mapPartialConstructors == null)
            {
            synchronized (loader) 
                {
                mapPartialConstructors = s_mapPartialConstructors.get(loader);
                if (mapPartialConstructors == null)
                    {
                    mapPartialConstructors  = new ConcurrentHashMap<String, Constructor>();
                    s_mapPartialConstructors.put(loader, mapPartialConstructors);
                    }
                }
            }
        return mapPartialConstructors;
        }

    // ----- inner class: PartialClassLoader --------------------------------

    /**
     * Class loader implementation that is used to define and load partial
     * classes.
     */
    protected static class PartialClassLoader
            extends ClassLoader
        {
        // ----- constructors -----------------------------------------------

        /**
         * Construct PartialClassLoader instance.
         */
        public PartialClassLoader(ClassLoader parentLoader)
            {
            super(parentLoader);

            // necessary in order to fix a bug in Jackson
            m_package = definePackage(PACKAGE_PARTIAL_CLASSES,
                    "Coherence REST Partial Classes", "1.0", "Oracle",
                    "Coherence REST Partial Classes", "1.0", "Oracle",
                    null);
            }

        // ---- methods -----------------------------------------------------

        /**
         * Define a class.
         *
         * @param sName  class name
         * @param ab     raw class definition
         *
         * @return defined class
         */
        public Class defineClass(String sName, byte[] ab)
            {
            return defineClass(sName, ab, 0, ab.length);
            }

        // ----- ClassLoader methods ----------------------------------------

        /**
         * {@inheritDoc}
         */
        protected Package getPackage(String name)
            {
            return PACKAGE_PARTIAL_CLASSES.equals(name)
                   ? m_package
                   : super.getPackage(name);
            }

        /**
         * {@inheritDoc}
         */
        protected Package[] getPackages()
            {
            Package[] aPackages    = super.getPackages();
            int       cPackages    = aPackages == null ? 0 : aPackages.length;
            Package[] aPackagesNew = new Package[cPackages + 1];

            if (cPackages > 0)
                {
                System.arraycopy(aPackages, 0, aPackagesNew, 0, cPackages);
                }
            aPackagesNew[cPackages] = m_package;

            return aPackagesNew;
            }

        // ----- constants --------------------------------------------------

        /**
         * The name of the package containing partial classes.
         */
        private static final String PACKAGE_PARTIAL_CLASSES =
                "com.tangosol.coherence.rest.util.gen.partial";

        // ----- data members -----------------------------------------------

        /**
         * Package containing partial classes.
         */
        private Package m_package;
        }

    // ----- inner class: ClassReaderInternal -------------------------------

    /**
     * This class wraps ASM's ClassReader allowing Coherence to bypass the class
     * version checks performed by ASM when reading a class.
     *
     * @since 15.1.1.0
     */
    /*
     * Internal NOTE:  This class is also duplicated in coherence-core and
     *                 coherence-rest.  This is done because each module shades
     *                 ASM within a unique package into the produced JAR and
     *                 thus having to create copes to deal with those package
     *                 differences.
     */
    protected static final class ClassReaderInternal
            extends BaseClassReaderInternal<ClassReader, ClassVisitor>
        {
        // ----- constructors ---------------------------------------------------

        /**
         * @see BaseClassReaderInternal#BaseClassReaderInternal(InputStream)
         */
        public ClassReaderInternal(InputStream streamIn) throws IOException
            {
            super(streamIn);
            }

        /**
         * @see BaseClassReaderInternal#BaseClassReaderInternal(byte[])
         */
        public ClassReaderInternal(byte[] abBytes)
            {
            super(abBytes);
            }

        // ----- BaseClassReaderInternal methods --------------------------------

        @Override
        protected ClassReader createReader(byte[] abBytes)
            {
            return new ClassReader(abBytes);
            }

        @Override
        protected void accept(ClassReader classReader, ClassVisitor classVisitor, int nParsingOptions)
            {
            classReader.accept(classVisitor, nParsingOptions);
            }
        }

    // ----- data members ---------------------------------------------------

    /**
     * Partial classes cache.
     */
    protected static Map<PartialClassLoader,ConcurrentHashMap<String, Class>> s_mapPartialClasses =
            new CopyOnWriteMap<PartialClassLoader,ConcurrentHashMap<String, Class>>(WeakHashMap.class);

    /**
     * Partial class constructors cache.
     */
    protected static Map<PartialClassLoader,ConcurrentHashMap<String, Constructor>>  s_mapPartialConstructors =
            new CopyOnWriteMap<PartialClassLoader,ConcurrentHashMap<String, Constructor>>(WeakHashMap.class);

    /**
     * ClassLoader instances that is used to define and load partial classes. Since their can be multiple
     * applications in a single JVM, each context ClassLoader should have its own PartialClassLoader
     */
    protected static Map<ClassLoader, PartialClassLoader> s_mapPartialClassLoaders =
            new CopyOnWriteMap<ClassLoader, PartialClassLoader>(WeakHashMap.class);

    /**
     * Property values for this object.
     */
    protected Map m_mapProperties;
    }
