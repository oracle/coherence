/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.graal;

import com.oracle.coherence.common.base.SimpleHolder;
import com.oracle.coherence.common.internal.security.SecurityProvider;
import com.oracle.coherence.common.schema.SchemaExtension;
import com.oracle.coherence.common.schema.SchemaSource;

import com.oracle.coherence.inject.Injector;

import com.tangosol.application.LifecycleListener;

import com.tangosol.coherence.Component;

import com.tangosol.coherence.component.net.management.Model;
import com.tangosol.coherence.config.EnvironmentVariableResolver;
import com.tangosol.coherence.config.SystemPropertyResolver;

import com.tangosol.coherence.config.xml.CacheConfigNamespaceHandler;
import com.tangosol.coherence.config.xml.OperationalConfigNamespaceHandler;

import com.tangosol.coherence.http.HttpApplication;
import com.tangosol.coherence.http.HttpServer;

import com.tangosol.config.xml.DocumentElementPreprocessor;
import com.tangosol.config.xml.DocumentPreprocessor;
import com.tangosol.config.xml.ElementProcessor;
import com.tangosol.config.xml.NamespaceHandler;

import com.tangosol.internal.tracing.TracingShimLoader;

import com.tangosol.internal.util.graal.ScriptHandler;

import com.tangosol.internal.util.invoke.Lambdas;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.SerializationSupport;
import com.tangosol.io.Serializer;
import com.tangosol.io.SerializerFactory;

import com.tangosol.io.pof.PofConfigProvider;
import com.tangosol.io.pof.PofSerializer;
import com.tangosol.io.pof.PortableException;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.io.pof.schema.annotation.PortableType;

import com.tangosol.license.LicensedObject;
import com.tangosol.net.AbstractInvocable;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.ClusterPermission;
import com.tangosol.net.Coherence;
import com.tangosol.net.CompressionFilter;
import com.tangosol.net.DefaultCacheServer;
import com.tangosol.net.MemberEvent;
import com.tangosol.net.MemberIdentityProvider;
import com.tangosol.net.SessionConfiguration;
import com.tangosol.net.SessionProvider;

import com.tangosol.net.events.InterceptorMetadataResolver;

import com.tangosol.net.grpc.GrpcAcceptorController;

import com.tangosol.net.management.MBeanAccessor;
import com.tangosol.net.management.MapJsonBodyHandler;

import com.tangosol.net.metrics.MetricsRegistryAdapter;

import com.tangosol.net.topic.TopicException;
import com.tangosol.net.topic.TopicPublisherException;
import com.tangosol.run.xml.PropertyAdapter;
import com.tangosol.run.xml.XmlSerializable;

import com.tangosol.util.AbstractSparseArray;
import com.tangosol.util.AnyEvent;
import com.tangosol.util.CacheCollator;
import com.tangosol.util.Filter;
import com.tangosol.util.HealthCheck;

import com.tangosol.util.InvocableMap;
import com.tangosol.util.Listeners;
import com.tangosol.util.LongArray;
import com.tangosol.util.NullImplementation;
import com.tangosol.util.SafeHashMap;
import com.tangosol.util.SimpleLongArray;
import com.tangosol.util.SimpleMapEntry;
import com.tangosol.util.ValueExtractor;
import com.tangosol.util.WrapperCollections;
import com.tangosol.util.WrapperException;
import com.tangosol.util.extractor.AbstractExtractor;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import org.graalvm.nativeimage.hosted.RuntimeProxyCreation;
import org.graalvm.nativeimage.hosted.RuntimeReflection;
import org.graalvm.nativeimage.hosted.RuntimeResourceAccess;
import org.graalvm.nativeimage.hosted.RuntimeSerialization;

import javax.management.Descriptor;
import javax.management.MBeanException;
import javax.management.MBeanFeatureInfo;
import javax.management.MBeanInfo;
import javax.management.openmbean.OpenMBeanConstructorInfoSupport;
import java.io.IOException;
import java.io.InputStream;
import java.io.NotSerializableException;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.annotation.Annotation;

import java.lang.invoke.SerializedLambda;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.nio.file.Path;

import java.security.Permission;
import java.security.Principal;
import java.security.SignedObject;
import java.security.cert.CertPath;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.BiConsumer;

/**
 * A GraalVM native image {@link org.graalvm.nativeimage.hosted.Feature}
 * used when building Coherence native applications.
 */
@SuppressWarnings("unused")
public class CoherenceNativeImageFeature
        extends AbstractNativeImageFeature
    {
    /**
     * Create a {@link CoherenceNativeImageFeature}
     */
    public CoherenceNativeImageFeature()
        {
        }

    @Override
    protected Set<Class<?>> getSupertypes()
        {
        return SUPERTYPES;
        }

    @Override
    protected Set<Class<?>> getSerializableTypes()
        {
        return SERIALIZABLE_TYPES;
        }

    @Override
    protected Set<Class<? extends Annotation>> getAnnotations()
        {
        return ANNOTATIONS;
        }

    protected Set<String> getLoadAllClassesFromPackages()
        {
        return loadAllClassesFromPackages;
        }

    @Override
    public void duringSetup(DuringSetupAccess access)
        {
        addPackageNames(System.getProperty(PROP_PREFIX + PROP_LOAD_ALL_CLASSES));

        ClassLoader imageClassLoader = access.getApplicationClassLoader();
        List<Path>  classPath        = access.getApplicationClassPath();

        try (ScanResult result = new ClassGraph()
                .acceptClasspathElementsContainingResourcePath(COHERENCE_NATIVE_IMAGE_PROPERTIES_PATTERN)
                .overrideClasspath(classPath)
                .overrideClassLoaders(imageClassLoader)
                .enableAllInfo()
                .scan(Runtime.getRuntime().availableProcessors()))
            {
            List<URL> urLs = result.getResourcesWithLeafName(COHERENCE_NATIVE_IMAGE_PROPERTIES).getURLs();
            for (URL url : urLs)
                {
                try
                    {
                    System.out.println("Oracle Coherence: Loading Coherence native properties " + url);
                    try (InputStream in = url.openStream())
                        {
                        Properties props = new Properties();
                        props.load(in);
                        COHERENCE_PROPERTIES.add(props);
                        addPackageNames(props.getProperty(PROP_LOAD_ALL_CLASSES));
                        }
                    }
                catch (IOException e)
                    {
                    throw new RuntimeException("Failed to load Coherence properties from " + url, e);
                    }
                }
            }


        }

    @Override
    public void beforeAnalysis(BeforeAnalysisAccess access)
        {
        ClassLoader imageClassLoader = access.getApplicationClassLoader();
        List<Path>  classPath        = access.getApplicationClassPath();

        scan(imageClassLoader, classPath, classInfo ->
            {
            try
                {
                var clazz = Class.forName(classInfo.getName(), false, imageClassLoader);
                String packageName = clazz.getPackageName();

                if (packageName.startsWith("com.oracle.coherence") || packageName.startsWith("com.tangosol"))
                    {
                    // Find all the classes that are Coherence functional interfaces,
                    // basically any class under a Coherence package annotated with @FunctionalInterface
                    if (clazz.isAnnotationPresent(FunctionalInterface.class) && Serializable.class.isAssignableFrom(clazz))
                        {
                        access.registerSubtypeReachabilityHandler(LambdaReachableTypeHandler.INSTANCE, clazz);
                        }

                    // Assume any Coherence interface with a name ending MBean needs to be proxied
                    if (clazz.isInterface() && clazz.getSimpleName().endsWith("MBean"))
                        {
                        RuntimeProxyCreation.register(clazz);
                        }

                    if (Collection.class.isAssignableFrom(clazz) && Serializable.class.isAssignableFrom(clazz))
                        {
                        // this is a Coherence serializable collection so register it
                        RuntimeSerialization.register(clazz);
                        }
                    else if (Map.class.isAssignableFrom(clazz) && Serializable.class.isAssignableFrom(clazz))
                        {
                        // this is a Coherence serializable map so register it
                        RuntimeSerialization.register(clazz);
                        }
                    }
                }
            catch (ClassNotFoundException | NoClassDefFoundError e)
                {
                // ignore: due to incomplete classpath
                }
            });

        super.beforeAnalysis(access);

        // Register main classes
        registerAllElements(Coherence.class);
        registerAllElements(DefaultCacheServer.class);
        registerAllElements(CacheFactory.class);

        // Register exceptions
        RuntimeSerialization.register(ClassNotFoundException.class);
        RuntimeSerialization.register(Exception.class);
        RuntimeSerialization.register(InvocationTargetException.class);
        RuntimeSerialization.register(MBeanException.class);
        RuntimeSerialization.register(NotSerializableException.class);
        RuntimeSerialization.register(ObjectStreamException.class);
        RuntimeSerialization.register(ReflectiveOperationException.class);
        RuntimeSerialization.register(RejectedExecutionException.class);
        RuntimeSerialization.register(RuntimeException.class);
        RuntimeSerialization.register(StackTraceElement.class);
        RuntimeSerialization.register(Throwable.class);
        RuntimeSerialization.register(WrapperException.class);

        RuntimeSerialization.register(LicensedObject.class);
        RuntimeSerialization.register(Permission.class);
        registerAllElements(Permission.class);
        RuntimeSerialization.register(ClusterPermission.class);
        registerAllElements(ClusterPermission.class);
        RuntimeSerialization.register(SignedObject.class);

        //noinspection deprecation
        registerAllElements(CompressionFilter.class);

        RuntimeSerialization.register(MBeanAccessor.QueryBuilder.ParsedQuery.class);
        registerAllElements(MBeanAccessor.QueryBuilder.ParsedQuery.class);

        RuntimeSerialization.register(BigDecimal.class);
        RuntimeSerialization.register(BigInteger.class);

        RuntimeSerialization.register(OpenMBeanConstructorInfoSupport[].class);

        RuntimeSerialization.register(TreeSet.class);
        RuntimeSerialization.register(TreeMap.class);

        RuntimeSerialization.register(SafeHashMap.class);
        RuntimeSerialization.register(SimpleLongArray.class);

        // Coherence may return collections from some calls, so
        // ensure these classes are registered for serialization
        // just in case the default serializer is in use
        RuntimeSerialization.register(LinkedList.class);
        registerAllElements(LinkedList.class);
        RuntimeSerialization.register(Collections.unmodifiableList(new ArrayList<>()).getClass());
        registerAllElements(Collections.unmodifiableList(new ArrayList<>()).getClass());
        RuntimeSerialization.register(Collections.EMPTY_LIST.getClass());
        registerAllElements(Collections.EMPTY_LIST.getClass());
        RuntimeSerialization.register(Collections.unmodifiableMap(new HashMap<>()).getClass());
        registerAllElements(Collections.unmodifiableMap(new HashMap<>()).getClass());
        RuntimeSerialization.register(Collections.EMPTY_MAP.getClass());
        registerAllElements(Collections.EMPTY_MAP.getClass());
        RuntimeSerialization.register(Collections.unmodifiableSet(new HashSet<>()).getClass());
        registerAllElements(Collections.unmodifiableSet(new HashSet<>()).getClass());
        RuntimeSerialization.register(Collections.EMPTY_SET.getClass());
        registerAllElements(Collections.EMPTY_SET.getClass());

        RuntimeSerialization.register(SimpleHolder.class);
        registerAllElements(SimpleHolder.class);
        RuntimeSerialization.register(com.tangosol.util.SimpleHolder.class);
        registerAllElements(com.tangosol.util.SimpleHolder.class);

        registerAllElements(SecurityProvider.class);
        }

    @Override
    protected void processClassAfterRegistration(AfterRegistrationAccess access, Class<?> clazz)
        {
        if (XmlSerializable.class.isAssignableFrom(clazz))
            {
            // this is an XML bean so register its corresponding config resource
            String xmlName = "/" + clazz.getName().replace('.', '/') + ".xml";
            RuntimeResourceAccess.addResource(clazz.getModule(), xmlName);
            }
        }

    // ----- helper methods -------------------------------------------------

    private void addPackageNames(String packages)
        {
        if (packages != null && !packages.isBlank())
            {
            Arrays.stream(packages.trim().split(","))
                    .map(String::trim)
                    .forEach(loadAllClassesFromPackages::add);
            }
        }

    // ----- inner class: LambdaReachableTypeHandler ------------------------

    public static class LambdaReachableTypeHandler
            implements BiConsumer<DuringAnalysisAccess, Class<?>>
        {
        @Override
        public void accept(DuringAnalysisAccess access, Class<?> clazz)
            {
            if (processed.add(clazz))
                {
                RuntimeSerialization.register(clazz);
                if (Lambdas.isLambdaClass(clazz))
                    {
                    try
                        {
                        RuntimeReflection.registerMethodLookup(clazz, "writeReplace");
                        }
                    catch (Exception e)
                        {
                        // ignored
                        }
                    }
                }
            }

        public static final LambdaReachableTypeHandler INSTANCE = new LambdaReachableTypeHandler();

        private final Set<Class<?>> processed = ConcurrentHashMap.newKeySet();
        }

    // ----- data members ---------------------------------------------------

    /**
     * All subclasses of these types will be included.
     */
    @SuppressWarnings("deprecation")
    public static final Set<Class<?>> SUPERTYPES = Set.of(
            CacheConfigNamespaceHandler.Extension.class,
            Coherence.LifecycleListener.class,
            Component.class,
            DocumentElementPreprocessor.ElementPreprocessor.class,
            DocumentPreprocessor.class,
            Filter.class,
            ValueExtractor.class,
            InvocableMap.EntryAggregator.class,
            InvocableMap.EntryProcessor.class,
            InvocableMap.StreamingAggregator.class,
            InvocableMap.ParallelAwareAggregator.class,
            ElementProcessor.class,
            EnvironmentVariableResolver.class,
            ExternalizableLite.class,
            GrpcAcceptorController.class,
            HealthCheck.class,
            HttpApplication.class,
            HttpServer.class,
            Injector.class,
            InterceptorMetadataResolver.class,
            LifecycleListener.class,
            MapJsonBodyHandler.class,
            MemberIdentityProvider.class,
            MetricsRegistryAdapter.class,
            MemberEvent.class,
            Model.class,
            NamespaceHandler.class,
            OperationalConfigNamespaceHandler.Extension.class,
            PropertyAdapter.class,
            PofConfigProvider.class,
            PofSerializer.class,
            PortableObject.class,
            SchemaExtension.class,
            SchemaSource.class,
            ScriptHandler.class,
            Serializer.class,
            SerializerFactory.class,
            SessionConfiguration.class,
            SessionProvider.class,
            SystemPropertyResolver.class,
            TracingShimLoader.class,
            XmlSerializable.class);

    /**
     * The types and their subtypes to register for serialization.
     */
    public static final Set<Class<?>> SERIALIZABLE_TYPES = Set.of(
            AbstractExtractor.class, // <-- Serializable but not Ext'Lite nor POF and some subclasses are not either
            ExternalizableLite.class,
            PortableException.class,
            Certificate.class,
            Principal.class,
            CertPath.class,
            Component.class,
            LongArray.class,
            AbstractSparseArray.class,
            AbstractSparseArray.Node.class,
            AbstractInvocable.class,
            TopicException.class,
            TopicPublisherException.class,
            MemberEvent.class,
            Model.class,
            Number.class,
            PropertyAdapter.class,
            Listeners.class,
            AnyEvent.class,
            NullImplementation.NullSet.class,
            CacheCollator.class,
            SerializedLambda.class, // lambdas
            MBeanInfo.class, // MBeans
            MBeanFeatureInfo.class, // MBeans
            Descriptor.class, // MBeans
            SerializationSupport.class,
            SimpleMapEntry.class,
            WrapperCollections.AbstractWrapperCollection.class,
            WrapperCollections.AbstractWrapperEntry.class,
            WrapperCollections.AbstractWrapperMap.class);

    /**
     * All types with these annotations will be included.
     */
    public static final Set<Class<? extends Annotation>> ANNOTATIONS = Set.of(PortableType.class);

    public static final String PROP_LOAD_ALL_CLASSES = "loadAllClasses";

    public static final String PROP_PREFIX = "coherence.graalvm.native.";

    public static final String COHERENCE_NATIVE_IMAGE_PROPERTIES = "coherence-native.properties";

    public static final String COHERENCE_NATIVE_IMAGE_PROPERTIES_PATTERN = "META-INF/*/"
            + COHERENCE_NATIVE_IMAGE_PROPERTIES;

    /**
     * The list of {@code coherence-native.properties} file loaded.
     */
    private final List<Properties> COHERENCE_PROPERTIES = new CopyOnWriteArrayList<>();

    private final SortedSet<String> loadAllClassesFromPackages = new TreeSet<>(
            Set.of("com.oracle.coherence",
                    "com.tangosol",
                    "com.github.benmanes.caffeine.cache"));
    }
