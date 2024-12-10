
/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.application.console.Coherence

package com.tangosol.coherence.component.application.console;

import com.oracle.coherence.common.util.Threads;
import com.tangosol.coherence.Component;
import com.tangosol.coherence.component.net.Member;
import com.tangosol.coherence.component.net.MemberSet;
import com.tangosol.coherence.component.net.Message;
import com.tangosol.coherence.component.net.Security;
import com.tangosol.coherence.component.net.jmxHelper.HttpAdapter;
import com.tangosol.coherence.component.net.jmxHelper.ServerConnector;
import com.tangosol.coherence.component.net.management.Gateway;
import com.tangosol.coherence.component.net.memberSet.actualMemberSet.ServiceMemberSet;
import com.tangosol.coherence.component.net.packet.messagePacket.Broadcast;
import com.tangosol.coherence.component.net.socket.UdpSocket;
import com.tangosol.coherence.component.util.SafeCluster;
import com.tangosol.coherence.component.util.SafeService;
import com.tangosol.coherence.component.util.TransactionCache;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService;
import com.oracle.coherence.common.base.Blocking;
import com.tangosol.application.ContainerContext;
import com.tangosol.coherence.config.Config;
import com.tangosol.coherence.dslquery.QueryPlus;
import com.tangosol.coherence.dslquery.internal.PersistenceToolsHelper;
import com.tangosol.dev.component.ComponentClassLoader;
import com.tangosol.dev.component.NullStorage;
import com.tangosol.internal.net.logging.DefaultLoggingDependencies;
import com.tangosol.internal.net.logging.LegacyXmlLoggingHelper;
import com.tangosol.internal.net.management.DefaultGatewayDependencies;
import com.tangosol.internal.net.management.LegacyXmlGatewayHelper;
import com.tangosol.internal.util.ObjectFormatter;
import com.tangosol.internal.util.listener.VersionAwareListeners;
import com.tangosol.license.LicensedObject;
import com.tangosol.net.AsyncNamedCache;
import com.tangosol.net.BackingMapManager;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.CacheService;
import com.tangosol.net.Cluster;
import com.tangosol.net.ClusterDependencies;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.DistributedCacheService;
import com.tangosol.net.InvocationService;
import com.tangosol.net.NamedCache;
import com.tangosol.net.PartitionedService;
import com.tangosol.net.RequestTimeoutException;
import com.tangosol.net.ServiceInfo;
import com.tangosol.net.cache.ContinuousQueryCache;
import com.tangosol.net.partition.PartitionSet;
import com.tangosol.net.partition.SimplePartitionKey;
import com.tangosol.net.partition.VersionAwareMapListener;
import com.tangosol.net.security.SimpleHandler;
import com.tangosol.run.jca.SimpleValidator;
import com.tangosol.run.xml.SimpleElement;
import com.tangosol.run.xml.SimpleParser;
import com.tangosol.run.xml.XmlDocument;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;
import com.tangosol.run.xml.XmlValue;
import com.tangosol.util.Base;
import com.tangosol.util.Binary;
import com.tangosol.util.ClassHelper;
import com.tangosol.util.CompositeKey;
import com.tangosol.util.ConcurrentMap;
import com.tangosol.util.Filter;
import com.tangosol.util.ImmutableArrayList;
import com.tangosol.util.LiteSet;
import com.tangosol.util.MapListener;
import com.tangosol.util.MapListenerSupport;
import com.tangosol.util.MapTriggerListener;
import com.tangosol.util.NullImplementation;
import com.tangosol.util.ObservableMap;
import com.tangosol.util.PrimitiveSparseArray;
import com.tangosol.util.Resources;
import com.tangosol.util.Service;
import com.tangosol.util.TransactionMap;
import com.tangosol.util.UID;
import com.tangosol.util.ValueExtractor;
import com.tangosol.util.Versionable;
import com.tangosol.util.WrapperException;
import com.tangosol.util.aggregator.QueryRecorder;
import com.tangosol.util.comparator.ChainedComparator;
import com.tangosol.util.comparator.InverseComparator;
import com.tangosol.util.comparator.SafeComparator;
import com.tangosol.util.extractor.ChainedExtractor;
import com.tangosol.util.extractor.ConditionalExtractor;
import com.tangosol.util.extractor.DeserializationAccelerator;
import com.tangosol.util.extractor.IdentityExtractor;
import com.tangosol.util.extractor.KeyExtractor;
import com.tangosol.util.extractor.ReflectionExtractor;
import com.tangosol.util.filter.FilterTrigger;
import com.tangosol.util.filter.InKeySetFilter;
import com.tangosol.util.filter.LimitFilter;
import com.tangosol.util.processor.AsynchronousProcessor;
import com.tangosol.util.processor.ConditionalProcessor;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.AccessControlException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;

/**
 * This component serves as a build target as well as a unit test for the
 * Coherence API.
 *
 * @see com.tangosol.net.CacheFactory
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class Coherence
        extends    com.tangosol.coherence.component.application.Console
    {
    // ---- Fields declarations ----

    /**
     * Property BuildNumber
     *
     * Build number is initialized in _initStatic(); therefore, it cannot be
     * designed.
     */
    private static transient String __s_BuildNumber;

    /**
     * Property Cluster
     *
     * The Cluster instance.
     */
    private static transient com.tangosol.coherence.component.util.SafeCluster __s_Cluster;

    /**
     * Property CommandHistory
     *
     * History of recently executed commands
     */
    private transient java.util.List __m_CommandHistory;

    /**
     * Property ConfigurationLoaded
     *
     * Flag to indicate that the configuration has been loaded
     * @volatile
     */
    private static volatile boolean __s_ConfigurationLoaded;

    /**
     * Property COPYRIGHT
     *
     */
    public static final String COPYRIGHT = "Copyright (c) 2000, 2024, Oracle and/or its affiliates. All rights reserved.";

    /**
     * Property DEFAULT_EDITION
     *
     * The default edition.
     */
    public static final String DEFAULT_EDITION = "GE";

    /**
     * Property DEFAULT_MODE
     *
     * The default operational mode.
     */
    public static final String DEFAULT_MODE = "development";

    /**
     * Property Edition
     *
     * The Edition is the product type.
     *
     * 0=Data Client (DC)
     * 1=Real-Time Client (RTC)
     * 2=Compute Client (CC)
     * 3=Standard Edition (SE)
     * 4=Enterprise Edition (EE)
     * 5=Grid Edition (GE)
     */
    private int __m_Edition;

    /**
     * Property EDITION_NAMES
     *
     * Product edition abbreviation array.
     */
    public static final String[] EDITION_NAMES;

    /**
     * Property FILE_CFG_CERTIFICATE
     *
     * File location for the Certificate.
     */
    public static final String FILE_CFG_CERTIFICATE = "tangosol.cer";

    /**
     * Property FILE_CFG_COHERENCE
     *
     * File location for initial configuration information.
     *
     * Note that the forward slash is prescribed by the JAR format and does not
     * violate the 100% Pure Java standard.
     */
    public static final String FILE_CFG_COHERENCE = "/tangosol-coherence.xml";

    /**
     * Property FILE_CFG_COHERENCE_OVERRIDE
     *
     * File name for default override configuration resource.
     *
     * Note that the forward slash is prescribed by the JAR format and does not
     * violate the 100% Pure Java standard.
     */
    public static final String FILE_CFG_COHERENCE_OVERRIDE = "/tangosol-coherence-override.xml";

    /**
     * Property Filters
     *
     * Filters keyed by their names. Used in filter and list commands.
     */
    private transient java.util.Map __m_Filters;

    /**
     * Property LicenseLoaded
     *
     */
    private static transient boolean __s_LicenseLoaded;

    /**
     * Property Logger
     *
     * The low priority logging daemon.
     */
    private transient Coherence.Logger __m_Logger;

    /**
     * Property LoggerRef
     *
     * The logger reference.
     */
    private transient java.util.concurrent.atomic.AtomicReference __m_LoggerRef;

    /**
     * Property Map
     *
     * Map assosiated with currently tested service
     */
    private transient com.tangosol.net.NamedCache __m_Map;

    /**
     * Property Mode
     *
     * The Mode is the "license type", i.e. evaluation, development or
     * production use.
     *
     * 0=evaluation
     * 1=development
     * 2=production
     */
    private int __m_Mode;

    /**
     * Property MODE_NAMES
     *
     * License type ("mode") names.
     */
    public static final String[] MODE_NAMES;

    /**
     * Property PersistenceToolsHelper
     *
     * Holds a help instance for issuing persistence commands.
     */
    private com.tangosol.coherence.dslquery.internal.PersistenceToolsHelper __m_PersistenceToolsHelper;

    /**
     * Property Product
     *
     * The product name.
     */
    private transient String __m_Product;

    /**
     * Property Script
     *
     * Inidcates that the command line tool is in the "scripting" mode.
     */
    private boolean __m_Script;

    /**
     * Property ScriptEngine
     *
     * The script engine (nashorn);
     */
    private javax.script.ScriptEngine __m_ScriptEngine;

    /**
     * Property Service
     *
     * Service that is currently being tested
     */
    private transient com.tangosol.net.CacheService __m_Service;

    /**
     * Property ServiceConfigMap
     *
     * Map containing the service configuration element per service type
     * (including pseudo-types like $Logger, etc.).
     *
     * @see #loadConfiguration
     */
    private static java.util.Map __s_ServiceConfigMap;

    /**
     * Property Stop
     *
     * Specifies whether to stop the command tool.
     */
    private transient boolean __m_Stop;

    /**
     * Property TITLE
     *
     * Any change to this value should be replicated in COPYRIGHT,
     * $Logger.Format, and $Logger.DefaultFormat
     */
    public static final String TITLE = "Oracle Coherence";

    /**
     * Property TloCluster
     *
     * The thread local object to avoid an infinite recursion.
     */
    private static transient ThreadLocal __s_TloCluster;

    /**
     * Property VERSION
     *
     * Build version is initialized in _initStatic().
     */
    public static final String VERSION;

    /**
     * The version encoded to an {@code int} (initialized in __initStatic()).
     */
    public static final int VERSION_INT;

    /**
     * Property VERSION_INTERNAL
     *
     * Build internal version; is initialized in _initStatic().
     */
    public static final String VERSION_INTERNAL;
    private static com.tangosol.util.ListMap __mapChildren;

    private static void _initStatic$Default()
        {
        __initStatic();
        }

    // Static initializer (from _initStatic)
    static
        {
        // import com.tangosol.run.xml.SimpleParser;
        // import com.tangosol.run.xml.XmlDocument;
        // import com.tangosol.run.xml.XmlElement;
        // import com.tangosol.util.Resources;
        // import java.io.InputStream;
        // import java.net.URL;

        EDITION_NAMES = new String[] {"DC", "RTC", "SE", "CE", "EE", "GE"};
        MODE_NAMES    = new String[] {"eval", "dev", "prod"};

        _initStatic$Default();

        InputStream in             = null;
        String      sVersion       = null;
        String      sVersionPrefix = "";
        String      sDesc          = null;

        try
            {
            URL url = Resources.findResource(FILE_CFG_COHERENCE, get_CLASS().getClassLoader());

            if (url != null)
                {
                in  = url.openStream();
                XmlDocument xmlDoc = new SimpleParser(/*fValidate*/ false).parseXml(in);
                XmlElement  xmlCfg = xmlDoc.getSafeElement("license-config");

                setBuildNumber(xmlCfg.getSafeElement("build-number").getString());
                sDesc          = xmlCfg.getSafeElement("build-description").getString();
                sVersion       = xmlCfg.getSafeElement("version").getString();
                sVersionPrefix = xmlCfg.getSafeElement("version-prefix").getString();
                sVersionPrefix = sVersionPrefix == null || sVersionPrefix.isEmpty()
                        ? "" : sVersionPrefix + ".";
                }
            }
        catch (Throwable e)
            {
            e.printStackTrace(System.err);
            }
        finally
            {
            try
                {
                if (in != null)
                    {
                    in.close();
                    }
                }
            catch (Exception ignored) {}
            }

        if (sVersion == null || sVersion.isEmpty())
            {
            System.err.println(TITLE + ": The build information could not be located in the file " +
                FILE_CFG_COHERENCE + "; some functionality may be disabled");
            sVersion = "n/a";
            setBuildNumber("n/a");
            }

        if (sDesc != null && !sDesc.isEmpty())
            {
            sVersion += " " + sDesc;
            }

        // YY.MM.PATCH | <major>.<minor>.<service>.<patchset>.<patch>
        VERSION          = sVersion;

        // <major>.<minor>.<service>.(YY.MM | <patchset>.<patch>)
        VERSION_INTERNAL = sVersionPrefix + sVersion;

        VERSION_INT = ServiceMemberSet.parseVersion(sVersion);
        }

    // Default static initializer
    private static void __initStatic()
        {
        // state initialization: static properties
        try
            {
            setServiceConfigMap(new com.tangosol.util.SafeHashMap());
            __s_TloCluster = new java.lang.ThreadLocal();
            }
        catch (java.lang.Exception e)
            {
            // re-throw as a runtime exception
            throw new com.tangosol.util.WrapperException(e);
            }
        // register child classes
        __mapChildren = new com.tangosol.util.ListMap();
        __mapChildren.put("CacheItem", Coherence.CacheItem.get_CLASS());
        __mapChildren.put("Logger", Coherence.Logger.get_CLASS());
        __mapChildren.put("Worker", Coherence.Worker.get_CLASS());
        }

    // Default constructor
    public Coherence()
        {
        this(null, null, true);
        }

    // Initializing constructor
    public Coherence(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
        {
        super(sName, compParent, false);

        if (fInit)
            {
            __init();
            }
        }

    // Main initializer
    public void __init()
        {
        // singleton initialization
        if (__singleton != null)
            {
            throw new IllegalStateException("A singleton for \"Coherence\" has already been set");
            }
        __singleton = this;

        // private initialization
        __initPrivate();

        // state initialization: public and protected properties
        try
            {
            setCommandHistory(new java.util.LinkedList());
            setLoggerRef(new java.util.concurrent.atomic.AtomicReference());
            }
        catch (java.lang.Exception e)
            {
            // re-throw as a runtime exception
            throw new com.tangosol.util.WrapperException(e);
            }

        // containment initialization: children

        // signal the end of the initialization
        set_Constructed(true);
        }

    // Private initializer
    protected void __initPrivate()
        {

        super.__initPrivate();
        }

    //++ getter for static property _Instance
    /**
     * Instantiate an Application component or return the previously
    * instantiated singleton.
    *
    * The implementation of the get_Instance accessor on the Application
    * component is more complex than that of most other singleton components.
    * First, it must be able to determine what application to instantiate.
    * Secondly, it works with the _Reference property of the root component to
    * maintain a reference to the resulting application component until the
    * very last component instance is garbage-collected.
    *
    * 1)  The _Reference property is static and located on the root component.
    * 2)  The accessor and mutator for the _Reference property are protected
    * and thus the property value can be obtained or modified by any component.
    *  (Specifically, it is set initially by Component.<init>, obtained by
    * Application.get_Instance, and set by Application.onInit.)
    * 3)  Component.<init> (the constructor of the root component) sets the
    * _Reference property to the instance of the component being constructed if
    * and only if the _Reference property is null; this guarantees that a
    * reference to the very first component to be instantiated is initially
    * stored in the _Reference property.
    * 4)  When an application component is instantiated, the Application.onInit
    * method stores a reference to that application in the _Reference property;
    * this ensures that a reference to the application will exist as long as
    * any component instance exists (because the root component class will not
    * be garbage-collectable until all component instances are determined to be
    * garbage-collectable and until all of its sub-classes are determined to be
    * garbage-collectable).  Since Application is a singleton, it is not
    * possible for a second instance of Application to exist, so once an
    * Application component has been instantiated, the _Reference property's
    * value will refer to that application until the root component class is
    * garbage-collected.
    * 5)  When Application.get_Instance() is invoked, if no singleton
    * application (Application.__singleton) has been created, then
    * Application.get_Instance is responsible for instantiating an application,
    * which will result in _Reference being set to the application instance (by
    * way of Application.onInit).
    *
    * The implementation of Application.get_Instance is expected to take the
    * the following steps in order to instantiate the correct application
    * component:
    * 1) If the value of the _Reference property is non-null, it cannot be an
    * instance of Component.Application, because the __init method of an
    * Application component would have set the Application.__singleton field to
    * reference itself.  Application thus assumes that a non-null _Reference
    * value refers to the first component to be instantiated, and invokes the
    * _makeApplication instance method of that component.  If the return value
    * from _makeApplication is non-null, then it is returned by
    * Application.get_Instance.
    * 2)  Application.get_Instance is now in a catch-22.  No instance of
    * Component.Application exists, which means that the entry point (the
    * initially instantiated) component was not an application component, and
    * the component that was the entry point has denied knowledge of what
    * application should be instantiated.  At this point, the
    * Application.get_Instance method must determine the name of the
    * application component to instantiate without any help.  So it drops back
    * and punts.  First, it checks for an environment setting, then it checks
    * for a properties file setting, and if either one exists, then it
    * instantiates the component specified by that setting and returns it.
    * 3)  Finally, without so much as a clue telling Application.get_Instance
    * what to instantiate, it instantiates the default application context.
    *
    * Note that in any of the above scenarios, by the time the value is
    * returned by Application.get_Instance, the value of the _Reference
    * property would have been set to the application instance by
    * Application.onInit, thus fulfilling the goal of holding a reference to
    * the application.
    *
    * Any other case in which a reference must be maintained should be done by
    * having the application hold that reference.  In other words, as long as
    * the application doesn't go away, any reference that it holds won't go
    * away either.
    *
    * @see Component#_Reference
     */
    public static com.tangosol.coherence.Component get_Instance()
        {
        com.tangosol.coherence.Component singleton = __singleton;

        if (singleton == null)
            {
            singleton = new com.tangosol.coherence.component.application.console.Coherence();
            }
        else if (!(singleton instanceof com.tangosol.coherence.component.application.console.Coherence))
            {
            throw new IllegalStateException("A singleton for \"com.tangosol.coherence.component.application.console.Coherence\" has already been set to a different type");
            }
        return singleton;
        }

    //++ getter for static property _CLASS
    /**
     * Getter for property _CLASS.<p>
    * Property with auto-generated accessor that returns the Class object for a
    * given component.
     */
    public static Class get_CLASS()
        {
        Class clz;
        try
            {
            clz = Class.forName("com.tangosol.coherence/component/application/console/Coherence".replace('/', '.'));
            }
        catch (ClassNotFoundException e)
            {
            throw new NoClassDefFoundError(e.getMessage());
            }
        return clz;
        }

    //++ getter for autogen property _Module
    /**
     * This is an auto-generated method that returns the global [design time]
    * parent component.
    *
    * Note: the class generator will ignore any custom implementation for this
    * behavior.
     */
    private com.tangosol.coherence.Component get_Module()
        {
        return this;
        }

    //++ getter for autogen property _ChildClasses
    /**
     * This is an auto-generated method that returns the map of design time
    * [static] children.
    *
    * Note: the class generator will ignore any custom implementation for this
    * behavior.
     */
    protected java.util.Map get_ChildClasses()
        {
        return __mapChildren;
        }

    protected java.util.Set applyFilter(String sFilter, boolean fKeysOnly, java.util.Comparator comparator, int nPage)
        {
        // import com.tangosol.util.Base;
        // import com.tangosol.util.Filter;
        // import com.tangosol.util.filter.LimitFilter;
        // import java.util.Date;
        // import java.util.Set;

        try
            {
            Filter filter   = (Filter) getFilters().get(sFilter);
            Set    setEntry = null;

            if (filter instanceof LimitFilter)
                {
                LimitFilter filterLimit = (LimitFilter) filter;
                if (nPage >= 0)
                    {
                    filterLimit.setPage(nPage);
                    }

                filterLimit.setComparator(comparator); // see LimitFilter.toString()
                }

            if (fKeysOnly)
                {
                setEntry = getMap().keySet(filter);
                }
            else if (comparator != null)
                {
                setEntry = getMap().entrySet(filter, comparator);
                }
            else
                {
                setEntry = getMap().entrySet(filter);
                }

            if (filter instanceof LimitFilter && nPage < 0)
                {
                LimitFilter filterLimit = (LimitFilter) filter;
                if (setEntry.size() < filterLimit.getPageSize())
                    {
                    filterLimit.setPage(0);
                    }
                else
                    {
                    filterLimit.nextPage();
                    }
                }
            return setEntry;
            }
        catch (Exception e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }

    /**
     * Calculate the actual value of the attribute.
     */
    protected static String calculateAttribute(String sAttr)
        {
        // import com.tangosol.coherence.config.Config;
        // import com.tangosol.run.xml.XmlElement;
        // import com.tangosol.util.Base;

        // attribute format could be
        // "value", "{property}", or "{property default-value}"
        // and the value could contain the {mode} macro

        final String MODE = "{mode}";
        if (sAttr.indexOf(MODE) >= 0)
            {
            // Note: calculateAttribute() may be called before configuration is
            // fully loaded.  Read directly from the service config map to avoid
            // recursing.
            XmlElement xmlLicense = (XmlElement) getServiceConfigMap().get("$License");

            sAttr = Base.replace(sAttr, MODE,
                xmlLicense.getSafeElement("license-mode").getString(DEFAULT_MODE));
            }

        while (sAttr.startsWith("{") && sAttr.endsWith("}"))
            {
            int    ofDefault = sAttr.indexOf(' ');
            int    cchLength = sAttr.length();
            String sDefault;
            String sPropName;
            if (ofDefault < 0)
                {
                sPropName = sAttr.substring(1, cchLength - 1);
                sDefault  = sPropName;
                }
            else
                {
                sPropName = sAttr.substring(1, ofDefault);
                sDefault  = sAttr.substring(ofDefault + 1, cchLength - 1);
                }

            sAttr = Config.getProperty(sPropName, sDefault);
            }

        return sAttr;
        }

    protected Object convertArgument(String sParam)
        {
        // import com.tangosol.net.Cluster;
        // import com.tangosol.net.Member as com.tangosol.net.Member;
        // import com.tangosol.net.partition.SimplePartitionKey;
        // import com.tangosol.util.Base;
        // import com.tangosol.util.Binary;
        // import com.tangosol.util.ClassHelper;
        // import com.tangosol.util.CompositeKey;
        // import com.tangosol.util.ImmutableArrayList;
        // import com.tangosol.util.Filter;
        // import com.tangosol.util.PrimitiveSparseArray;
        // import java.util.Date;

        int ofStart = sParam.indexOf('{'); // }
        if (ofStart == 0)
            {
            if (sParam.equals("{cluster}"))
                {
                return getSafeCluster();
                }
            if (sParam.startsWith("{composite:"))
                {
                String sArgs  = sParam.substring(11, sParam.length() - 1).trim();
                int    iSplit = sArgs.indexOf(',');

                return new CompositeKey(convertArgument(sArgs.substring(0, iSplit).trim()),
                        convertArgument(sArgs.substring(iSplit + 1).trim()));
                }
            if (sParam.equals("{Date}"))
                {
                return new Date(Base.getSafeTimeMillis());
                }
            if (sParam.equals("{date}"))
                {
                return new Date(getSafeCluster().getTimeMillis());
                }
            if (sParam.equals("{map}"))
                {
                return getMap();
                }
            if (sParam.equals("{com.tangosol.net.Member}"))
                {
                return getSafeCluster().getLocalMember();
                }
            if (sParam.startsWith("{partition:"))
                {
                Integer NPart = (Integer) convertArgument(sParam.substring(11, sParam.length() - 1));
                return SimplePartitionKey.getPartitionKey(NPart.intValue());
                }
            if (sParam.equals("{random}"))
                {
                return Integer.valueOf(Base.getRandom().nextInt());
                }
            if (sParam.equals("{result}"))
                {
                return ((ThreadLocal) get_Sink()).get();
                }
            if (sParam.equals("{service}"))
                {
                return getService();
                }
            if (sParam.equals("{time}"))
                {
                return Long.valueOf(getSafeCluster().getTimeMillis());
                }

            if (sParam.endsWith("}")) // {
                {
                String sValue = sParam.substring(1, sParam.length() - 1);
                if (sValue.charAt(0) == '#')
                    {
                    // usage example: "{#5 #}" is equivalent to {0,1,2,3,4}
                    int    ofIter = sValue.indexOf(' ');
                    String sIter  = sValue.substring(1, ofIter);
                    int    cIters = Integer.parseInt(sIter);
                    if (ofIter > 0)
                        {
                        Object[] aoValue = new Object[cIters];
                        sValue = sValue.substring(ofIter + 1);
                        for (int i = 0; i < cIters; i++)
                            {
                            String s = sValue.indexOf('#') >= 0 ?
                                Base.replace(sValue, "#", String.valueOf(i)) : sValue;
                            aoValue[i] = convertArgument(s);
                            }
                        return new ImmutableArrayList(aoValue);
                        }
                    }
                else if (sValue.indexOf(',') >= 0)
                    {
                    // comma delimited Set, List or Collection
                    Object[] aoValue = sValue.length() == 0 ? ClassHelper.VOID :
                        convertArguments(Base.parseDelimitedString(sValue, ','));
                    return new ImmutableArrayList(aoValue);
                    }
                else if (sValue.startsWith("filter:"))
                    {
                    // {filter:<name>}
                    String sFilter = sValue.substring(sValue.indexOf(':') + 1);
                    return (Filter) getFilters().get(sFilter);
                    }
                }
            else
                {
                throw new RuntimeException("Unbalanced {} in parameter: " + sParam +
                                           "; quotes may need to be used");
                }
            }

        if (ofStart >= 0)
            {
            String[][] aasReplace = null;
            try
                {
                Cluster cluster = getSafeCluster();
                long    dtTime  = cluster.getTimeMillis();
                com.tangosol.net.Member  member  = cluster.getLocalMember();
                aasReplace = new String[][]
                    {
                    {"{Date}",    String.valueOf(new Date(Base.getSafeTimeMillis()))},
                    {"{date}",    String.valueOf(new Date(dtTime))},
                    {"{random}",  String.valueOf(Base.getRandom().nextInt())},
                    {"{result}",  String.valueOf(((ThreadLocal) get_Sink()).get())},
                    {"{time}",    String.valueOf(dtTime)},
                    {"{com.tangosol.net.Member}",  String.valueOf(member)},
                    {"{member}",  member == null ? "0" : String.valueOf(member.getId())},
                    };
                 }
            catch (Exception ignored) {}

            for (int iR = 0, cR = aasReplace == null ? 0 : aasReplace.length; iR < cR; iR++)
                {
                String[] asReplace = aasReplace[iR];
                sParam = Base.replace(sParam, asReplace[0], asReplace[1]);
                }
            int ofBegin = sParam.indexOf("{random:"); // "random:<max>}"
            if (ofBegin >= 0)
                {
                int ofEnd = sParam.indexOf('}', ofBegin);
                if (ofEnd > ofBegin)
                    {
                    String sMax  = sParam.substring(ofBegin + "{random:".length(), ofEnd);
                    int    ofRnd = sMax.indexOf(',');
                    if (ofRnd >= 0)
                        {
                        int iofRnd = ofRnd;
                        ofRnd = Integer.parseInt(sMax.substring(0, ofRnd));
                        sMax  = sMax.substring(iofRnd + 1);
                        }

                    int iMax  = sMax.length() == 0 ? Integer.MAX_VALUE : Integer.parseInt(sMax);
                    int iRand =
                        iMax > 0 ? Base.getRandom().nextInt(iMax) : // uniform
                        iMax < 0 ? (int) (-iMax*Base.getRandom().nextDouble()*Base.getRandom().nextDouble()) : // pseudo-normal
                                   (int) Math.abs(Integer.MAX_VALUE*Base.getRandom().nextGaussian());     // normal
                    sParam = Base.replace(sParam,
                        sParam.substring(ofBegin, ofEnd + 1), String.valueOf(iRand + ofRnd));
                    }
                }
            }

        if (sParam.startsWith("[") || sParam.startsWith("Set[") ||
            sParam.startsWith("List[") || sParam.startsWith("PSA["))
            {
            if (sParam.equals("[null]"))
                {
                return null;
                }
            if (sParam.equals("[true]"))
                {
                return Boolean.TRUE;
                }
            if (sParam.equals("[false]"))
                {
                return Boolean.FALSE;
                }
            if (sParam.endsWith("]"))
                {
                // comma delimited array
                String   sValue     = sParam.substring(sParam.indexOf('[') + 1, sParam.length() - 1);
                String[] asElements = Base.parseDelimitedString(sValue, ',');

                if (sParam.startsWith("PSA["))
                    {
                    PrimitiveSparseArray la = new PrimitiveSparseArray();

                    for (int i = 0, c = asElements.length; i < c; ++i)
                        {
                        String[] asValue = Base.parseDelimitedString(asElements[i], ':');

                        la.setPrimitive(Long.parseLong(asValue[0]), Long.parseLong(asValue[1]));
                        }

                    return la;
                    }

                Object[] aoValue = sValue.length() == 0
                        ? ClassHelper.VOID
                        : convertArguments(asElements);

                return sParam.startsWith("Set")
                            ? new ImmutableArrayList(aoValue).getSet() :
                       sParam.startsWith("List")
                            ? new ImmutableArrayList(aoValue).getList() :
                              ((Object) aoValue);
                }
            }
        else if (sParam.startsWith("0x"))
            {
            return new Binary(Base.parseHex(sParam));
            }
        else if (sParam.endsWith("l"))
            {
            try
                {
                return Long.valueOf(sParam.substring(0, sParam.length() - 1));
                }
            catch (NumberFormatException ignored) {}
            }
        else if (sParam.endsWith("f"))
            {
            try
                {
                return Float.valueOf(sParam.substring(0, sParam.length() - 1));
                }
            catch (NumberFormatException ignored) {}
            }
        else if (sParam.endsWith("d"))
            {
            try
                {
                return Double.valueOf(sParam.substring(0, sParam.length() - 1));
                }
            catch (NumberFormatException ignored) {}
            }
        else if (sParam.length() == 2 && sParam.charAt(0) == '\\')
            {
            return Character.valueOf(sParam.charAt(1));
            }
        else if (sParam.startsWith(" "))
            {
            // allow numbers as strings
            return sParam.trim();
            }
        else if (Character.isDigit(sParam.charAt(0)))
            {
            try
                {
                return Integer.valueOf(sParam);
                }
            catch (NumberFormatException ignored) {}
            }
        return sParam;
        }

    public Object[] convertArguments(String[] asParam)
        {
        // import java.lang.reflect.Array;
        // import java.util.Arrays;

        int      cParams    = asParam == null ? 0 : asParam.length;
        Object[] aoParam    = new Object[cParams];
        Class    clzUniform = null;
        for (int i = 0; i < cParams; i++)
            {
            String sParam   = asParam[i];
            Object oParam   = convertArgument(sParam);
            Class  clzParam = oParam == null ? Object.class : oParam.getClass();

            clzUniform = i == 0 ? clzParam
                                : clzUniform == clzParam ? clzUniform : null;

            aoParam[i] = oParam;
            if (oParam != sParam)
                {
                asParam[i] = String.valueOf(oParam);
                }
            }

        if (clzUniform != null)
            {
            Object[] aoParamTyped = (Object[]) Array.newInstance(clzUniform, cParams);
            aoParam = Arrays.copyOf(aoParam, cParams, aoParamTyped.getClass());
            }

        return aoParam;
        }

    protected Object createReader()
        {
        // import com.tangosol.util.ClassHelper;
        // import java.io.File;
        // import java.io.InputStreamReader;

        Object reader = null;
        Object jlineReaderBldr = null;
        try
            {
            Class clzJLineReaderBldr = Class.forName("org.jline.reader.LineReaderBuilder");
            jlineReaderBldr = ClassHelper.invokeStatic(clzJLineReaderBldr, "builder", null);
            }
        catch (Exception e) // ClassNotFoundException, ClassNoDefError
            {
            return new InputStreamReader(System.in);
            }

        try
            {
            File fileHistory = new File(".coh-history");
            if (!fileHistory.exists())
                {
                fileHistory.createNewFile();
                }
            Class  clzJLineReader   = Class.forName("org.jline.reader.LineReader");
            String fieldHistoryFile = (String) clzJLineReader.getField("HISTORY_FILE").get(clzJLineReader);
            ClassHelper.invoke(jlineReaderBldr, "variable",
                 new Object[] {fieldHistoryFile, fileHistory});

            return ClassHelper.invoke(jlineReaderBldr, "build", null);
            }
        catch (Exception e)
            {
            System.out.println("failed to setup Jline history: " + e);
            return new InputStreamReader(System.in);
            }
        }

    // Declared at the super level
    /**
     * Prints out the specified message according to the Application context.
    * Derived applications should provide an appropriate implementation for
    * this method if the output should not be sent to the standard output and
    * error output.  For example, if an application wanted to log output, this
    * would be the method to override.
    *
    * @param message  the text message to display
    * @param severity  0 for informational, ascending for more serious output
    * (the default implementation assumes anything not 0 is an error and should
    * be printed to the system error stream)
     */
    public void debugOutput(String message, int severity)
        {
        Coherence.Logger logger = ensureRunningLogger();
        try
            {
            if (logger.isEnabled(severity))
                {
                logger.log(severity, message);
                }
            }
        catch (Throwable error)
            {
            // quite probably an OOME
            System.err.println("<Error>: Failed to write a log message");
            }
        }

    // Declared at the super level
    /**
     * Prints out the specified exception according to the Application context.
    * Derived applications should provide an appropriate implementation for
    * this method if the output should not be sent to the standard output and
    * error output.
    *
    * @param e  the exception to print
     */
    public void debugOutput(Throwable e)
        {
        Coherence.Logger logger = ensureRunningLogger();
        try
            {
            if (logger.isEnabled(Coherence.Logger.LEVEL_ERROR))
                {
                logger.log(Coherence.Logger.LEVEL_ERROR, e);
                }
            }
        catch (Throwable error)
            {
            // quite probably an OOME
            System.err.println("<Error>: Failed to write a log message");
            }
        }

    /**
     * Helper method used by the com.tangosol.net.MulticastTest. Currently it's
    * only called to translate Broadcast messages.
    *
    * @param buff a ReadBuffer that contains the streamed message
    * @param addr the InetSocketAddress of the incoming message
    * @param msgPrevHeartbeat a previous hearbeat message (to avoid repeating
    * the same messages)
    *
    * @return the current hearbeat message or null if any other tyoe
     */
    public static com.tangosol.coherence.component.net.Message displayMessage(com.tangosol.io.ReadBuffer buf, java.net.InetSocketAddress addr, com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService.SeniorMemberHeartbeat msgPrevHeartbeat)
            throws java.io.IOException
        {
        // import Component.Net.Message;
        // import Component.Net.Packet.MessagePacket.Broadcast;
        // import Component.Util.Daemon.QueueProcessor.Service.Grid.ClusterService;
        // import Component.Util.Daemon.QueueProcessor.Service.Grid.ClusterService$SeniorMemberHeartbeat as com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService.SeniorMemberHeartbeat;
        // import com.tangosol.util.Base;
        // import com.tangosol.io.ReadBuffer$BufferInput as com.tangosol.io.ReadBuffer.BufferInput;
        // import java.util.Date;

        // instantiate a Coherence packet object from the raw data
        Broadcast packet = (Broadcast) Broadcast.instantiate(buf.getBufferInput(), 0);

        // use a ClusterService to instantiate the necessary Message
        Message msg = new ClusterService().instantiateMessage(packet.getMessageType());

        msg.setDeserializationRequired(true);
        msg.setMessageType(packet.getMessageType());
        msg.setMessagePartCount(1);
        msg.setPacket(0, packet);

        com.tangosol.io.ReadBuffer.BufferInput input = packet.getReadBuffer().getBufferInput();
        msg.readInternal(input);
        msg.read(input);

        com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService.SeniorMemberHeartbeat msgCurHeartbeat  =
            msg instanceof com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService.SeniorMemberHeartbeat ? (com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService.SeniorMemberHeartbeat) msg : null;

        if (msgCurHeartbeat != null && msgPrevHeartbeat != null
                && packet.getFromId() == msgPrevHeartbeat.getPacket(0).getFromId()
                && msgCurHeartbeat.getMemberSet().toString().equals(msgPrevHeartbeat.getMemberSet().toString()))
            {
            long lLastRecvTimestamp = msgCurHeartbeat.getLastReceivedMillis();
            String sLastRecvTimestamp = (lLastRecvTimestamp == 0L ?
                    "none" : new Date(lLastRecvTimestamp).toString());
            Base.out("Message \"SeniorMemberHeartbeat\" (no change) LastRecvTimestamp=" + sLastRecvTimestamp);
            }
        else
            {
            // display the message
            Base.out(msg);
            }

        return msgCurHeartbeat;
        }

    protected void doAggregate(Object[] aoParam, boolean fSilent)
        {
        // import com.tangosol.net.NamedCache;
        // import com.tangosol.util.ClassHelper;
        // import com.tangosol.util.Filter;
        // import com.tangosol.util.InvocableMap$EntryAggregator as com.tangosol.util.InvocableMap.EntryAggregator;
        // import com.tangosol.util.NullImplementation;
        // import java.util.Collection;
        // import java.util.Collections;

        // ('{' <key> [, <key>]* '}' | "{filter:"<filter-name>'}' | *) <aggregator-name> [<extractor>]");

        NamedCache map = getMap();

        int cParams = aoParam.length;
        if (cParams < 2)
            {
            _trace("Aggregator name must be specified");
            return;
            }

        Object oTarget     = aoParam[0];
        String sAggregator = (String) aoParam[1];
        Object extractor   = null;
        if (cParams > 2)
            {
            String sMethod = (String) aoParam[2];
            extractor = sMethod == null || sMethod.equals("none") ?
                NullImplementation.getValueExtractor() : (Object) sMethod;
            }

        com.tangosol.util.InvocableMap.EntryAggregator aggregator;
        try
            {
            Class clzAggregator = Class.forName("com.tangosol.util.aggregator." + sAggregator);
            aggregator = (com.tangosol.util.InvocableMap.EntryAggregator) ClassHelper.newInstance(clzAggregator,
                extractor == null ? ClassHelper.VOID : new Object[] {extractor});
            }
        catch (Throwable e)
            {
            printException("Invalid aggregator format: " + sAggregator
                         + " " + toString(extractor) + "\n", e);
            return;
            }

        Object oResult =
            oTarget instanceof Filter || oTarget == null ?
                map.aggregate((Filter) oTarget, aggregator) :
            oTarget instanceof Collection ?
                map.aggregate((Collection) oTarget, aggregator) :
            oTarget.equals("*") ?
                map.aggregate(map.keySet(), aggregator) :
            // just a key
                map.aggregate(Collections.singleton(oTarget), aggregator);

        ((ThreadLocal) get_Sink()).set(oResult);
        if (!fSilent)
            {
            _trace(String.valueOf(oResult));
            }
        }

    protected void doBackup(String[] asParam, boolean fSilent)
        {
        // import com.tangosol.internal.util.MapBackupHelper as com.tangosol.internal.util.MapBackupHelper;
        // import java.io.BufferedOutputStream;
        // import java.io.DataOutputStream;
        // import java.io.File;
        // import java.io.FileOutputStream;
        // import java.io.IOException;
        // import java.util.Map;

        if (asParam.length == 0)
            {
            _trace("File name is expected");
            return;
            }

        Map map = getMap();

        try
            {
            File             file       = new File(asParam[0]);
            FileOutputStream streamFile = new FileOutputStream(file);
            DataOutputStream streamData = new DataOutputStream(
                new BufferedOutputStream(streamFile, 32*1024));

            com.tangosol.internal.util.MapBackupHelper.writeMap(streamData, map);

            streamData.close();
            streamFile.close();

            if (!fSilent)
                {
                _trace(map.size() + " entries written to " + file.getAbsolutePath() +
                    " (" + file.length() + " bytes)");
                }
            }
        catch (IOException e)
            {
            _trace("Failed to backup: " + e);
            }
        }

    protected void doBatch(String[] asParam, boolean fSilent)
            throws java.lang.InterruptedException
        {
        // import java.io.BufferedReader;
        // import java.io.File;
        // import java.io.FileReader;
        // import java.io.IOException;

        if (asParam.length == 0)
            {
            _trace("File name is expected");
            return;
            }

        String     sFile     = asParam[0];
        FileReader readerRaw = null;
        try
            {
            BufferedReader reader = new BufferedReader(
                readerRaw = new FileReader(sFile));

            while (true)
                {
                String sCmd = reader.readLine();
                if (sCmd == null)
                    {
                    break;
                    }
                if (sCmd.length() > 0)
                    {
                    if (!fSilent)
                        {
                        _trace(">>> " + sCmd);
                        }
                    if (!sCmd.startsWith("//"))
                        {
                        processCommand(sCmd);
                        }
                    }
                }
            }
        catch (IOException e)
            {
            _trace("Failed to read the batch file: " + e);
            }
        finally
            {
            try
                {
                readerRaw.close();
                }
            catch (Exception ignored) {}
            }
        }

    protected void doBulkPut(String[] asParam, boolean fSilent)
        {
        // import com.tangosol.net.AsyncNamedCache;
        // import com.tangosol.net.NamedCache;
        // import com.tangosol.util.Base;
        // import java.util.ArrayList;
        // import java.util.Collections;
        // import java.util.Date;
        // import java.util.HashMap;
        // import java.util.List;
        // import java.util.Map;
        // import java.util.Random;
        // import java.util.concurrent.CompletableFuture;

        if (!isMapValid())
            {
            return;
            }

        // format: <item count> <item size> <first index> [<batch size> | "all" | "async"]

        int    cIters = 1000;
        int    cbData = 1000;
        String sKey   = "0";
        try
            {
            cIters = Integer.parseInt(asParam[0]);
            cbData = Integer.parseInt(asParam[1]);
            sKey   = asParam[2];
            }
        catch (Exception e)
            {
            _trace("Assuming iterations=" + cIters + ", size=" + cbData);
            }

        int iFirst = 0;
        try
            {
            iFirst = Integer.parseInt(sKey);
            sKey   = "{result}";
            }
        catch (NumberFormatException ignored) {}

        int cBatch = 0;
        if (asParam.length > 3)
            {
            String sBatch = asParam[3];
            if ("all".equals(sBatch))
                {
                cBatch = cIters;
                }
            else if ("async".equals(sBatch))
                {
                cBatch = -1;
                }
            else
                {
                try
                    {
                    cBatch = Integer.parseInt(sBatch);
                    }
                catch (NumberFormatException ignored) {}
                }
            }
        NamedCache      cache       = getMap();
        AsyncNamedCache cacheAsync  = null;
        List            listFutures = Collections.emptyList();
        if (cBatch < 0)
            {
            cacheAsync  = cache.async();
            listFutures = new ArrayList();
            }

        cBatch = Math.abs(cBatch);

        if (cIters < 0)
            {
            cIters = -cIters;
            iFirst -= cIters - 1;
            }

        if (!fSilent)
            {
            _trace(new Date() + ": adding " + cIters +
                " items (starting with #" + iFirst + ") each " + cbData + " bytes ...");
            }

        int nOrigin = 0;
        try
            {
            nOrigin = getService().getCluster().getLocalMember().getId();
            }
        catch (RuntimeException ignored) {} // local cache has no LocalMember

        Map         mapBatch = cBatch > 0 ? new HashMap(cBatch) : null;
        Random      random   = new Random();
        ThreadLocal tlSink   = (ThreadLocal) get_Sink();
        Object      oResult  = tlSink.get();
        long        lBegin   = Base.getSafeTimeMillis();

        for (int i = iFirst, iLast = iFirst + cIters, cB = 0; i < iLast; ++i)
            {
            tlSink.set(Integer.valueOf(i));

            Object oKey;
            Object oVal;
            if (cbData > 0)
                {
                byte[] ab = new byte[cbData];
                ab[0] = (byte) random.nextInt();

                oKey = convertArgument(sKey);
                oVal = ab;
                }
            else
                {
                Coherence.CacheItem item = new Coherence.CacheItem();
                item.setIndex(i);
                item.setOrigin(nOrigin);

                oKey = cbData < 0 ? (Object) item : convertArgument(sKey);
                oVal = item;
                }

            if (cBatch > 0)
                {
                mapBatch.put(oKey, oVal);
                if (++cB == cBatch || (i + 1) == iLast)
                    {
                    if (cacheAsync == null)
                        {
                        cache.putAll(mapBatch);
                        }
                    else
                        {
                        listFutures.add(cacheAsync.putAll(mapBatch));
                        }
                    mapBatch.clear();
                    cB = 0;
                    }
                }
            else
                {
                cache.put(oKey, oVal);
                }
            }

        tlSink.set(oResult);

        if (!fSilent)
            {
            if (!listFutures.isEmpty())
                {
                try
                    {
                    CompletableFuture.allOf((CompletableFuture[]) listFutures.toArray()).get();
                    }
                catch (Throwable ignored) {}
                }
            long lElapsed = Base.getSafeTimeMillis() - lBegin;

            if (cbData == 0)
                {
                cbData = 480; // Bynary length of the Coherence.CacheItem serialization
                }
            cbData += 4; // key length

            double dThrouK = ((double) cIters)*cbData/lElapsed; // KB per sec
            double dThrouI = ((double) cIters)*1000/lElapsed;   // items per sec

            _trace(new Date() + ": done putting (" + lElapsed + "ms, "
                + (int) dThrouK + "KB/sec, "
                + (int) dThrouI + " items/sec)"
                );
            }
        }

    protected void doBulkRemove(String[] asParam, boolean fSilent)
        {
        // import com.tangosol.util.Base;
        // import java.util.Date;
        // import java.util.HashSet;
        // import java.util.Set;

        if (!isMapValid())
            {
            return;
            }

        // format: <item count> <first index> [<batch size> | "all"]

        int cIters = 1000;
        try
            {
            cIters = Integer.parseInt(asParam[0]);
            }
        catch (Exception e)
            {
            _trace("Assuming iterations=" + cIters);
            }

        int    iFirst = 0;
        String sKey   = asParam[2];
        try
            {
            iFirst = Integer.parseInt(sKey);
            sKey   = "{result}";
            }
        catch (NumberFormatException ignored) {}

        int cBatch = 0;
        if (asParam.length > 3)
            {
            String sBatch = asParam[3];
            if ("all".equals(sBatch))
                {
                cBatch = cIters;
                }
            else
                {
                try
                    {
                    cBatch = Integer.parseInt(sBatch);
                    }
                catch (NumberFormatException ignored) {}
                }
            }

        if (cIters < 0)
            {
            cIters = -cIters;
            iFirst -= cIters - 1;
            }

        if (!fSilent)
            {
            _trace(new Date() + ": removing " + cIters +
                " items (starting with #" + iFirst + ")");
            }

        boolean     fResult  = false;
        Set         setAll   = getMap().keySet();
        Set         setBatch = cBatch > 0 ? new HashSet() : null;
        long        lBegin   = Base.getSafeTimeMillis();
        ThreadLocal tlSink   = (ThreadLocal) get_Sink();
        Object      oResult  = tlSink.get();

        for (int i = iFirst, iLast = iFirst + cIters, cB = 0; i < iLast; ++i)
            {
            tlSink.set(Integer.valueOf(i));

            Object oKey = convertArgument(sKey);
            if (cBatch > 0)
                {
                setBatch.add(oKey);
                if (cB++ == cBatch)
                    {
                    fResult |= setAll.removeAll(setBatch);
                    setBatch.clear();
                    cB = 0;
                    }
                }
            else
                {
                fResult |= setAll.remove(oKey);
                }
            }

        if (cBatch > 1 && !setBatch.isEmpty())
            {
            fResult |= setAll.removeAll(setBatch);
            }
        tlSink.set(oResult);

        long lElapsed = Base.getSafeTimeMillis() - lBegin;
        if (!fSilent)
            {
            _trace(new Date() + ": done removing; " +
                   "result=" + fResult + " (" + lElapsed + "ms)");
            }
        }

    protected void doCache(String[] asParam, boolean fSilent)
        {
        // import com.tangosol.internal.util.ObjectFormatter;
        // import com.tangosol.net.CacheFactory;
        // import com.tangosol.net.ConfigurableCacheFactory as com.tangosol.net.ConfigurableCacheFactory;
        // import com.tangosol.net.DefaultConfigurableCacheFactory as com.tangosol.net.DefaultConfigurableCacheFactory;
        // import com.tangosol.net.ExtensibleConfigurableCacheFactory as com.tangosol.net.ExtensibleConfigurableCacheFactory;
        // import com.tangosol.net.DefaultConfigurableCacheFactory$CacheInfo as com.tangosol.net.DefaultConfigurableCacheFactory.CacheInfo;
        // import com.tangosol.net.NamedCache;
        // import com.tangosol.net.cache.ContinuousQueryCache;
        // import com.tangosol.util.Filter;
        // import com.tangosol.util.NullImplementation;
        // import java.security.AccessController;

        com.tangosol.net.ConfigurableCacheFactory factory = CacheFactory.getConfigurableCacheFactory();

        String      sName   = asParam[0];
        boolean     fUnique = asParam.length > 1 && asParam[1].equals("unique");
        boolean     fBinary = asParam.length > 1 && asParam[1].equals("binary");
        boolean     fCQC    = asParam.length > 1 && getFilters().containsKey(asParam[1]);
        ClassLoader loader  = fUnique ? getUniqueClassLoader() :
                              fBinary ? NullImplementation.getClassLoader() :
                                        null;
        NamedCache  cache   = CacheFactory.getCache(sName, loader);
        String      sPrompt = cache.getCacheName();

        if (fCQC)
            {
            // cache <filter-name>
            cache = new ContinuousQueryCache(cache, (Filter) getFilters().get(asParam[1]));
            sPrompt = "CQC-" + sPrompt;
            }

        getLogger().setPrompt(sPrompt);
        setService(cache.getCacheService());
        setMap(cache);

        if (!fSilent)
            {
            if (factory instanceof com.tangosol.net.DefaultConfigurableCacheFactory)
                {
                com.tangosol.net.DefaultConfigurableCacheFactory dccf = (com.tangosol.net.DefaultConfigurableCacheFactory) factory;
                com.tangosol.net.DefaultConfigurableCacheFactory.CacheInfo info = dccf.findSchemeMapping(sName);
                _trace(String.valueOf(dccf.resolveScheme(info)));
                }
            else if (factory instanceof com.tangosol.net.ExtensibleConfigurableCacheFactory)
                {
                com.tangosol.net.ExtensibleConfigurableCacheFactory eccf = (com.tangosol.net.ExtensibleConfigurableCacheFactory) factory;
                _trace((String) AccessController.doPrivileged(new ObjectFormatter().asPrivilegedAction(
                    "Cache Configuration: " + sName,
                    eccf.getCacheConfig().findSchemeByCacheName(sName),
                    eccf.getParameterResolver(sName, null, null, eccf.getContainerContext()))));
                }
            }
        }

    protected void doCacheFactory(String[] asParam, boolean fSilent)
        {
        // import com.tangosol.net.CacheFactory;
        // import com.tangosol.net.ConfigurableCacheFactory;
        // import com.tangosol.net.DefaultConfigurableCacheFactory as com.tangosol.net.DefaultConfigurableCacheFactory;
        // import com.tangosol.net.ExtensibleConfigurableCacheFactory as com.tangosol.net.ExtensibleConfigurableCacheFactory;
        // import com.tangosol.run.xml.XmlElement;
        // import com.tangosol.util.Base;

        int cParams = asParam.length;
        switch (cParams)
            {
            case 2:
                System.setProperty("scope-name", asParam[1]);
            case 1:
                {
                ConfigurableCacheFactory factory =
                    CacheFactory.getCacheFactoryBuilder().getConfigurableCacheFactory(asParam[0],
                        Base.getContextClassLoader());
                CacheFactory.setConfigurableCacheFactory(factory);
                }
            }

        if (!fSilent)
            {
            ConfigurableCacheFactory factory   = CacheFactory.getConfigurableCacheFactory();
            XmlElement               xmlConfig = null;

            if (factory instanceof com.tangosol.net.DefaultConfigurableCacheFactory)
                {
                xmlConfig = ((com.tangosol.net.DefaultConfigurableCacheFactory) factory).getConfig();
                }
            else if (factory instanceof com.tangosol.net.ExtensibleConfigurableCacheFactory)
                {
                xmlConfig = (XmlElement) ((com.tangosol.net.ExtensibleConfigurableCacheFactory) factory).getResourceRegistry()
                    .getResource(XmlElement.class, "legacy-cache-config");
                }

            if (xmlConfig != null)
                {
                _trace(xmlConfig.getSafeElement("caching-scheme-mapping").toString());
                }
            }
        }

    protected void doConnector(String[] asParam, boolean fSilent)
            throws java.lang.InterruptedException
        {
        // import Component.Net.Cluster;
        // import Component.Net.Member;
        // import Component.Net.MemberSet;
        // import Component.Net.Socket.UdpSocket;
        // import Component.Util.SafeCluster;
        // import com.oracle.coherence.common.base.Blocking;
        // import com.tangosol.util.Base;
        // import java.net.InetAddress;
        // import java.net.InetSocketAddress;
        // import java.util.HashSet;

        int cParams = asParam.length;
        if (cParams == 0)
            {
            _trace("Command: connector (listener | publisher) [(unicast | multicast | p2p <ip:port> | member <id> | *) [on | off | drop <drop ratio> | pause <pause ratio> | delay <delay>]]");
            return;
            }

        Member  member  = null;
        HashSet setAddr = null;
        final int CHANNEL_ADV       = 1;
        final int CHANNEL_PREF      = 2;
        final int CHANNEL_BROAD_UDP = 4;
        final int CHANNEL_BROAD_TCP = 8;
        final int CHANNEL_MULTI     = 16;

        int nChannels = CHANNEL_MULTI | CHANNEL_ADV | CHANNEL_PREF | CHANNEL_BROAD_UDP | CHANNEL_BROAD_TCP;
        if (cParams >= 2)
            {
            char chParam = asParam[1].charAt(0);

            switch (chParam)
                {
                case 'u':
                    nChannels = CHANNEL_ADV | CHANNEL_PREF | CHANNEL_BROAD_UDP | CHANNEL_BROAD_TCP;
                    break;

                case 'm':
                    if (asParam[1].startsWith("mu"))
                        {
                        nChannels = CHANNEL_MULTI;
                        }
                    else
                        {
                        // member
                        try
                            {
                            int     nMember = Integer.parseInt(asParam[2]);
                            Cluster cluster = (Cluster) getSafeCluster().getCluster();

                            member  = ((MemberSet) cluster.getMemberSet()).getMember(nMember);

                            setAddr = new HashSet();
                            setAddr.add(member.getSocketAddress());
                            }
                        catch (Exception e)
                            {
                            _trace(e);
                            return;
                            }
                        }
                    break;

                case 'p': // p2p
                    {
                    // parse comman delimited address list
                    // list of comman delimited ip:port elements
                    setAddr = new HashSet();
                    for (int i = 2; i < cParams; ++i)
                        {
                        String sParam = asParam[i];
                        if (sParam.endsWith(","))
                            {
                            sParam = sParam.substring(0, sParam.length() - 1);
                            }
                        else
                            {
                            i = cParams;
                            }

                        try
                            {
                            int iColon = sParam.indexOf(':');
                            if (iColon == -1)
                                {
                                _trace("address must include port");
                                return;
                                }
                            InetAddress ipAddr = iColon == 0 ? InetAddress.getLocalHost()
                                                    : InetAddress.getByName(sParam.substring(0, iColon));
                            int         nPort  = Integer.parseInt(sParam.substring(iColon + 1));
                            setAddr.add(new InetSocketAddress(ipAddr, nPort));
                            }
                        catch (Exception e)
                            {
                            _trace(e);
                            return;
                            }
                        }
                    break;
                    }

                default:
                }
            }

        int     cAddr  = setAddr == null ? 0 : setAddr.size();
        float   flRate = -1.0f; // 0.0 = on; 1.0=0ff
        boolean fPause = false; // pause or drop
        boolean fDelay = false;
        if (cParams >= cAddr + 3)
            {
            String sRate = asParam[cAddr + 2];
            flRate = sRate.equals("off")    ? 1.0f :
                     sRate.equals("on")     ? 0.0f :
                    (sRate.equals("drop") ||
                     sRate.equals("pause")) ? (cParams >= cAddr + 4 ? Float.parseFloat(asParam[cAddr + 3]) : -1.0f) :
                     sRate.equals("delay")  ? 0.0f :
                     -1.0f;

            fPause = sRate.equals("pause") && flRate > 0.0f && flRate < 1.0f;
            fDelay = sRate.equals("delay");
            if (flRate < 0.0f || flRate > 1.0f)
                {
                _trace("Ratio should be within [0.0, 1.0] interval");
                return;
                }
            }

            if (fDelay && member != null)
                {
                // set the send delay for the specified member
                Cluster cluster      = (Cluster) getSafeCluster().getCluster();
                long    nDelay       = Base.parseTime(asParam[cAddr + 3]);
                int     nResendDelay = cluster.getDependencies().getPublisherResendDelayMillis();
                int     nDropCount   = (int) Math.ceil((float) nDelay / nResendDelay);

                _trace("Each packet sent to member " + member
                     + " will be dropped " + nDropCount + " times.");
                member.setTxDebugDropCount(nDropCount);
                return;
                }

        long lPause    = (long) (10000 * flRate);
        int  iDropRate = fPause ? 100000 : (int) (flRate * 100000);

        while (true)
            {
            for (int nChannel = CHANNEL_ADV; nChannel <= CHANNEL_MULTI; nChannel <<= 1)
                {
                if ((nChannels & nChannel) != 0)
                    {
                    String  sTarget = (getMap() == null ? "&" : "&getCacheService.getService.") + "getCluster";
                    String  sDirection;
                    boolean fTx;
                    if (asParam[0].startsWith("l"))
                        {
                        sDirection = "Rx";
                        fTx        = false;

                        // unicast receives may occur on any number of sockets
                        switch (nChannel)
                            {
                            case CHANNEL_ADV:
                                sTarget += ".getPointListener";
                                break;

                            case CHANNEL_PREF:
                                sTarget += ".getPreferredListener";
                                break;

                            case CHANNEL_BROAD_UDP:
                                sTarget += ".getUdpBroadcastListener";
                                break;

                            case CHANNEL_BROAD_TCP:
                                sTarget += ".getTcpBroadcastListener";
                                break;

                            case CHANNEL_MULTI:
                                sTarget += ".getUdpBroadcastListener";
                                break;

                            default:
                                throw new IllegalArgumentException("unknown listener channel " + nChannel);
                            }

                        sTarget += ".getUdpSocket";
                        }
                    else
                        {
                        sDirection = "Tx";
                        fTx        = true;
                        sTarget += ".getPublisher" +
                                      (nChannel == CHANNEL_MULTI
                                            ? ".getUdpSocketMulticast"
                                            : ".getUdpSocketUnicast"); // all unicast sends use this socket
                        }

                    if (flRate == -1.0f)
                        {
                        processCommand(sTarget);
                        }
                    else
                        {
                        UdpSocket socket = (UdpSocket) processCommand('@' + sTarget);
                        if (socket != null)
                            {
                            if (fTx)
                                {
                                socket.setTxDebugDropAddresses(setAddr);
                                }
                            else
                                {
                                socket.setRxDebugDropAddresses(setAddr);
                                }
                            }

                        processCommand('@' + sTarget + ".set" + sDirection + "DebugDropRate " + iDropRate);
                        }
                    }
                }

            if (fPause)
                {
                ((ThreadLocal) get_Sink()).set(null); // release memory
                Blocking.sleep(lPause);
                lPause    = 10000 - lPause;
                iDropRate = Math.abs(iDropRate - 100000);
                }
            else
                {
                break;
                }
            }
        }

    protected void doExplain(String[] asParam, boolean fSilent)
        {
        // import com.tangosol.util.Filter;
        // import com.tangosol.util.aggregator.QueryRecorder;
        // import com.tangosol.util.aggregator.QueryRecorder$RecordType as com.tangosol.util.aggregator.QueryRecorder.RecordType;

        String  sFilter = asParam[0];
        boolean fTrace  = asParam.length > 1 && asParam[1].equals("trace");

        Filter filter = (Filter) getFilters().get(sFilter);
        if (filter != null)
            {
            QueryRecorder recorder = new QueryRecorder(fTrace ? com.tangosol.util.aggregator.QueryRecorder.RecordType.TRACE : com.tangosol.util.aggregator.QueryRecorder.RecordType.EXPLAIN);
            Object        oResult  = getMap().aggregate(filter, recorder);

            if (!fSilent)
                {
                _trace(String.valueOf(oResult));
                }
            }
        }

    protected void doFilter(Object[] aoParam, boolean fSilent)
        {
        // import com.tangosol.net.DistributedCacheService;
        // import com.tangosol.net.partition.PartitionSet;
        // import com.tangosol.util.ClassHelper;
        // import com.tangosol.util.Filter;
        // import com.tangosol.util.ValueExtractor;
        // import com.tangosol.util.extractor.IdentityExtractor;
        // import com.tangosol.util.extractor.KeyExtractor;
        // import java.util.Map;

        Map      mapFilter     = getFilters();
        int      cParams       = aoParam.length;
        String   sFilterName   = (String) aoParam[0];
        String   sFilterType   = (String) aoParam[1];
        Object[] aoFilterParam = new Object[cParams - 2];

        try
            {
            for (int i = 2; i < cParams; i++)
                {
                Object oParam = aoParam[i];
                String sParam = String.valueOf(oParam);
                Filter filter = (Filter) mapFilter.get(sParam);

                if (filter != null)
                    {
                    oParam = filter;
                    }
                else if (sParam.equals("."))
                    {
                    oParam = IdentityExtractor.INSTANCE;
                    }
                else if (sParam.startsWith("!"))
                    {
                    oParam = convertArgument(sParam.substring(1));
                    oParam = oParam == null
                        ? new KeyExtractor((ValueExtractor) null)
                        : new KeyExtractor((String) oParam);
                    }
                else if (sFilterType.equals("Partitioned") && oParam instanceof Integer)
                    {
                    DistributedCacheService service =
                        (DistributedCacheService) getMap().getCacheService();
                    PartitionSet parts = new PartitionSet(service.getPartitionCount());
                    parts.add(((Integer) oParam).intValue());
                    oParam = parts;
                    }
                aoFilterParam[i - 2] = oParam;
                // _trace("Param " + (i-2) + "=" + oParam);
                }

            if (sFilterType.equals("All") || sFilterType.equals("Any"))
                {
                int      cFilters = aoFilterParam.length;
                Filter[] aFilter  = new Filter[cFilters];
                System.arraycopy(aoFilterParam, 0, aFilter, 0, cFilters);
                aoFilterParam = new Object[] {aFilter};
                }

            Class clzFilter = Class.forName("com.tangosol.util.filter." +
                sFilterType + "Filter");
            Filter filter = (Filter) ClassHelper.newInstance(clzFilter, aoFilterParam);
            mapFilter.put(sFilterName, filter);

            if (!fSilent)
                {
                _trace(String.valueOf(filter));
                }
            }
        catch (Throwable e)
            {
            printException("Invalid filter format: ", e);
            }
        }

    protected Object doFunction(String sFunction, Object[] aoParam, boolean fSilent)
        {
        Object  oTarget    = null;
        boolean fSetTarget = true;

        if (sFunction.startsWith("&&") || sFunction.startsWith("&!"))
            {
            fSetTarget = sFunction.startsWith("&&");
            sFunction  = sFunction.substring(2);
            oTarget    = ((ThreadLocal) get_Sink()).get();
            }
        else if (sFunction.startsWith("&/")) // "&/com/tangosol/net/CacheFactory.log"
            {
            try
                {
                String sClass = sFunction.substring(2);
                int    ofClz  = sClass.indexOf('.');

                sFunction = sClass.substring(ofClz + 1);
                sClass    = sClass.substring(0, ofClz).replace('/', '.');
                try
                    {
                    oTarget = Class.forName(sClass);
                    }
                catch (ClassNotFoundException e)
                    {
                    oTarget = Class.forName("java.lang." + sClass);
                    }
                }
            catch (Exception e)
                {
                _trace("Invalid format: " + e);
                return e;
                }
            }
        else if (sFunction.startsWith("&{"))
            {
            int ofTarget = sFunction.indexOf('}');
            if (ofTarget > 0)
                {
                ofTarget++;
                oTarget = convertArgument(sFunction.substring(1, ofTarget));
                if (sFunction.length() > ofTarget && sFunction.charAt(ofTarget) == '.')
                    {
                    ofTarget++;
                    }
                sFunction = sFunction.substring(ofTarget);
                }
            }
        else if (sFunction.startsWith("&~"))
            {
            sFunction = sFunction.substring(2);
            oTarget   = this;
            }
        else
            {
            sFunction = sFunction.substring(1);
            oTarget   = getMap();
            if (oTarget == null)
                {
                oTarget = getSafeCluster();
                }
            }

        if (sFunction.length() > 0)
            {
            oTarget = processFunction(oTarget, sFunction, fSilent, aoParam);
            }
        else
            {
            processFunction(oTarget, "toString", fSilent, aoParam);
            }

        if (fSetTarget)
            {
            ((ThreadLocal) get_Sink()).set(oTarget);
            }
        return oTarget;
        }

    /**
     * Output the command history
     */
    protected void doHistory(String[] asParam, boolean fSilent)
            throws java.lang.InterruptedException
        {
        // import java.util.Iterator;
        // import java.util.LinkedList;
        // import java.util.List;
        // import java.util.ListIterator;
        // import java.util.HashSet;

        List   listCommands = getCommandHistory();
        String sPattern     = null;
        int    nLimit       = -1;

        switch (asParam.length)
            {
            case 0:
                break;

            case 1:
                if (asParam[0].equals("clear"))
                    {
                    if (listCommands != null)
                        {
                        listCommands.clear();
                        }
                    return;
                    }
                else if (asParam[0].equals("on"))
                    {
                    if (listCommands == null)
                        {
                        setCommandHistory(new LinkedList());
                        }
                    return;
                    }
                else if (asParam[0].equals("off"))
                    {
                    setCommandHistory(null);
                    return;
                    }

                // may be limit or pattern
                try
                    {
                    nLimit = Integer.parseInt(asParam[0]);
                    }
                catch (NumberFormatException e)
                    {
                    sPattern = asParam[0];
                    }
                break;

            case 2:
                sPattern = asParam[0];
                nLimit   = Integer.parseInt(asParam[1]);
                break;

            default:
                _trace("history ([<pattern>] [<limit>]) | (['on' | 'off' | 'clear'])");
                return;
            }

        if (fSilent || listCommands == null)
            {
            // nothing to do
            return;
            }

        if (nLimit < 0)
            {
            nLimit = Integer.MAX_VALUE;
            }

        int cCommands = listCommands.size();
        if (sPattern == null)
            {
            int iCmd = Math.max(0, cCommands - nLimit);
            for (Iterator iter = listCommands.listIterator(iCmd); iter.hasNext(); )
                {
                _trace("  " + ++iCmd + "  " + iter.next());
                }
            }
        else
            {
            List    listOut  = new LinkedList();
            int     cMatches = 0;
            int     iCmd     = cCommands;
            HashSet setMatch = new HashSet();
            for (ListIterator iter = listCommands.listIterator(cCommands);
                iter.hasPrevious() && cMatches < nLimit; )
                {
                String sCmd = (String) iter.previous();
                if (sCmd.startsWith(sPattern) || sCmd.matches(sPattern))
                    {
                    if (!setMatch.contains(sCmd))
                        {
                        setMatch.add(sCmd);
                        listOut.add(0, "  " + iCmd + "  " + sCmd);
                        ++cMatches;
                        }
                    }
                --iCmd;
                }

            for (Iterator iter = listOut.iterator(); iter.hasNext(); )
                {
                _trace((String) iter.next());
                }
            }

        _trace("\nenter !<index> to reissue a command");
        }

    protected void doIndex(String[] asParam, boolean fSilent)
        {
        // import com.tangosol.util.Filter;
        // import com.tangosol.util.ValueExtractor as com.tangosol.util.ValueExtractor;
        // import com.tangosol.util.comparator.SafeComparator;
        // import com.tangosol.util.extractor.ConditionalExtractor;
        // import com.tangosol.util.extractor.ChainedExtractor;
        // import com.tangosol.util.extractor.DeserializationAccelerator;
        // import com.tangosol.util.extractor.IdentityExtractor;
        // import com.tangosol.util.extractor.KeyExtractor;
        // import com.tangosol.util.extractor.ReflectionExtractor;

        String  sMethod = asParam[0];
        boolean fRemove = asParam.length > 1 && asParam[1].equals("remove");
        Filter  filter  = asParam.length > 2 ? (Filter) getFilters().get(asParam[2]) : null;
        boolean fFwd    = asParam.length > 3 && Boolean.getBoolean(asParam[3]);

        com.tangosol.util.ValueExtractor extractor;
        if (sMethod.startsWith("!"))
            {
            extractor = new KeyExtractor(sMethod.substring(1));
            }
        else
            {
            extractor =
                sMethod.equals(".")      ? IdentityExtractor.INSTANCE :
                sMethod.equals("~")      ? new DeserializationAccelerator() :
                sMethod.indexOf('.') < 0 ? (com.tangosol.util.ValueExtractor) new ReflectionExtractor(sMethod) :
                                           new ChainedExtractor(sMethod);
            }

        if (filter != null)
            {
            extractor = new ConditionalExtractor(filter, extractor, fFwd);
            }

        if (fRemove)
            {
            getMap().removeIndex(extractor);
            }
        else
            {
            getMap().addIndex(extractor, true, new SafeComparator(null));
            }
        }

    protected void doInvoke(String sService, String[] asParam, boolean fSilent)
        {
        // import com.tangosol.net.CacheFactory;
        // import com.tangosol.net.InvocationService;
        // import com.tangosol.net.Member;
        // import java.util.Iterator;
        // import java.util.Map;
        // import java.util.Set;
        // import java.util.TreeMap;

        // <command> ["all" | "other" | "senior" | <id>] [("async" | "sync")] [priority] [timeout]

        InvocationService srvc;

        try
            {
            srvc = (InvocationService) CacheFactory.getService(sService);
            }
        catch (IllegalArgumentException e)
            {
            srvc = (InvocationService) getCluster().getService(sService);
            if (srvc == null)
                {
                throw e;
                }
            }

        int cParams = asParam.length;
        if (cParams == 0)
            {
            return;
            }

        String  sCommand  = asParam[0];
        String  sTarget   = cParams >= 2 ?  asParam[1] : "all";
        boolean fAsync    = cParams >= 3 && asParam[2].startsWith("a");
        int     iPriority = cParams >= 4 ? Integer.parseInt(asParam[3]) : 0;
        long    cTimeout  = cParams >= 5 ? Long.parseLong(asParam[4]) : 0L;

        Coherence.CacheItem task = new Coherence.CacheItem();
        task.setInvokeCommand(sCommand);
        task.setSchedulingPriority(iPriority);
        task.setExecutionTimeoutMillis(cTimeout);

        Set setMember = null;
        if (!sTarget.equals("all"))
            {
            setMember = srvc.getInfo().getServiceMembers();
            if (sTarget.equals("other"))
                {
                setMember.remove(getSafeCluster().getLocalMember());
                }
            else if (sTarget.equals("senior"))
                {
                setMember.clear();
                setMember.add(srvc.getInfo().getOldestMember());
                }
            else
                {
                setMember.clear();
                Set setAll = srvc.getInfo().getServiceMembers();
                try
                    {
                    int nMember = Integer.parseInt(sTarget);
                    for (Iterator iter = setAll.iterator(); iter.hasNext();)
                        {
                        Member member = (Member) iter.next();
                        if (member.getId() == nMember)
                            {
                            setMember.add(member);
                            break;
                            }
                        }
                    }
                catch (NumberFormatException e)
                    {
                    _trace("Unknown target: " + sTarget);
                    return;
                    }
                }
            }

        if (fAsync)
            {
            srvc.execute(task, setMember, fSilent ? null : (Coherence.Worker) _newChild("Worker"));
            }
        else
            {
            Map mapResult = srvc.query(task, setMember);
            if (!fSilent)
                {
                if (mapResult.isEmpty())
                    {
                    _trace("No results");
                    }
                else
                    {
                    if (mapResult.size() > 1)
                        {
                        mapResult = new TreeMap(mapResult); // sort by member id
                        }
                    for (Iterator iter = mapResult.keySet().iterator(); iter.hasNext();)
                        {
                        Member member  = (Member) iter.next();
                        Object oResult = mapResult.get(member);
                        _trace("Member " + (member == null ? "Proxy" : String.valueOf(member.getId()))
                             + ", Result=" + (oResult instanceof Throwable ?
                                oResult + "\n" + getStackTrace((Throwable) oResult) :
                                String.valueOf(oResult)));
                        }
                    }
                }
            }
        }

    protected void doJmx(String[] asParam)
        {
        // import Component;
        // import Component.Net.JmxHelper.HttpAdapter;
        // import Component.Net.JmxHelper.ServerConnector;

        // [port | url] ["start"] | ["stop"]

        int    cParams = asParam.length;
        int    nPort   = 8082;
        String sUrl    = "";
        try
            {
            if (cParams > 0)
                {
                nPort = Integer.parseInt(asParam[0]);
                }
            }
        catch (NumberFormatException e)
            {
            sUrl = asParam[0];
            }

        boolean fStart = cParams <= 1 || !asParam[1].equals("stop");
        String  sName  = sUrl.length() == 0 ? "Adapter" : "Connector";

        if (fStart)
            {
            Component wrapper = _findChild(sName);
            if (wrapper != null)
                {
                _trace("Already started: " + wrapper);
                return;
                }

            try
                {
                if (sUrl.length() == 0)
                    {
                    wrapper = new HttpAdapter();
                    }
                else
                    {
                    wrapper = new ServerConnector();
                    }
                }
            catch (NoClassDefFoundError e)
                {
                _trace("JMX library is not on a classpath; only remote management is allowed");
                return;
                }

            try
                {
                if (wrapper instanceof HttpAdapter)
                    {
                    ((HttpAdapter) wrapper).start(nPort, getSafeCluster());
                    }
                else
                    {
                    ((ServerConnector) wrapper).start(sUrl, getSafeCluster());
                    }
                }
            catch (RuntimeException e)
                {
                if (cParams > 1)
                    {
                    _trace(e);
                    }
                else
                    {
                    _trace(e.getMessage());
                    }
                return;
                }
            _addChild(wrapper, sName);

            ((ThreadLocal) get_Sink()).set(wrapper);
            _trace("Installed: " + wrapper);
            }
        else
            {
            Component wrapper = _findChild(sName);
            if (wrapper != null)
                {
                if (wrapper instanceof HttpAdapter)
                    {
                    ((HttpAdapter) wrapper).stop();
                    }
                else
                    {
                    ((ServerConnector) wrapper).stop();
                    }
                _removeChild(wrapper);
                }
            }
        }

    protected void doList(String[] asParam, boolean fSilent)
        {
        // import com.tangosol.util.Base;
        // import com.tangosol.util.comparator.ChainedComparator;
        // import com.tangosol.util.comparator.InverseComparator;
        // import com.tangosol.util.comparator.SafeComparator;
        // import com.tangosol.util.extractor.KeyExtractor;
        // import com.tangosol.util.extractor.ReflectionExtractor;
        // import java.util.Comparator;
        // import java.util.Iterator;
        // import java.util.Map;
        // import java.util.Map$Entry as java.util.Map.Entry;
        // import java.util.Set;

        // list <filter-name> [[[<method>[,<method>]*] desc] page]

        Set setEntry;
        Map map     = getMap();
        int cParams = asParam.length;
        if (cParams > 0)
            {
            String sName = asParam[0];
            if (getFilters().containsKey(sName))
                {
                Comparator comparator = null;
                int        nPage      = -1;
                if (cParams > 1)
                    {
                    String[] asMethod = Base.parseDelimitedString(asParam[1], ',');
                    int      cMethods = asMethod.length;
                    if (cMethods > 1)
                        {
                        Comparator[] acomp = new Comparator[cMethods];
                        for (int i = 0; i < cMethods; i++)
                            {
                            String sMethod = asMethod[i];
                            acomp[i] = sMethod.startsWith("!") ? (Comparator)
                                new KeyExtractor(sMethod.substring(1)) :
                                new ReflectionExtractor(sMethod);
                            }
                        comparator = new ChainedComparator(acomp);
                        }
                    else if (asMethod[0].equals("null"))
                        {
                        comparator = new SafeComparator(null);
                        }
                    else
                        {
                        String sMethod = asMethod[0];
                        comparator = sMethod.startsWith("!") ? (Comparator)
                            new KeyExtractor(sMethod.substring(1)) :
                            new ReflectionExtractor(sMethod);
                        }
                    if (cParams > 2 && asParam[2].startsWith("desc"))
                        {
                        comparator = new InverseComparator(comparator);
                        }
                    if (cParams > 3)
                        {
                        nPage = Integer.parseInt(asParam[3]);
                        }
                    }
                setEntry = applyFilter(sName, false, comparator, nPage);
                }
            else
                {
                map = getService().ensureCache(sName, null);
                setEntry = map.entrySet();
                }
            }
        else
            {
            setEntry = map.entrySet();
            }

        int cSize  = setEntry.size();
        int cLimit = Math.min(cSize, 50);
        int c      = 0;
        for (Iterator iter = setEntry.iterator(); iter.hasNext();)
            {
            java.util.Map.Entry entry = (java.util.Map.Entry) iter.next();

            if (c++ < cLimit && !fSilent) // we want to "get" them all regardless
                {
                _trace(entry.getKey() + " = " + entry.getValue());
                }
            }
        if (cLimit < cSize && !fSilent)
            {
            _trace("Only " + cLimit + " out of total " + cSize + " items were printed");
            }
        if (c != cSize)
            {
            _trace("Iterator returned " + c + " items");
            }
        ((ThreadLocal) get_Sink()).set(setEntry);
        }

    protected void doListen(String sSource, boolean fStop, com.tangosol.util.Filter filter, Object oKey, boolean fLite, Long LVersion, com.tangosol.util.PrimitiveSparseArray laVersions)
        {
        // import com.tangosol.internal.util.listener.VersionAwareListeners;
        // import com.tangosol.net.BackingMapManager;
        // import com.tangosol.net.NamedCache;
        // import com.tangosol.net.CacheFactory;
        // import com.tangosol.net.CacheService;
        // import com.tangosol.net.PartitionedService;
        // import com.tangosol.net.partition.VersionAwareMapListener;
        // import com.tangosol.util.ImmutableArrayList;
        // import com.tangosol.util.ClassHelper;
        // import com.tangosol.util.MapListener;
        // import com.tangosol.util.MapListenerSupport$WrapperPrimingListener as com.tangosol.util.MapListenerSupport.WrapperPrimingListener;
        // import com.tangosol.util.MapListenerSupport$WrapperSynchronousListener as com.tangosol.util.MapListenerSupport.WrapperSynchronousListener;
        // import com.tangosol.util.MapTriggerListener;
        // import com.tangosol.util.ObservableMap;
        // import com.tangosol.util.PrimitiveSparseArray as com.tangosol.util.PrimitiveSparseArray;
        // import com.tangosol.util.filter.FilterTrigger;
        // import com.tangosol.util.filter.InKeySetFilter;
        // import java.util.Map;
        // import java.util.Set;

        // resolve which map: assume cluster map
        NamedCache    map       = getMap();
        CacheService  service   = map.getCacheService();
        ObservableMap mapListen = map;

        if (sSource.equals("local"))
            {
            BackingMapManager mgr = service.getBackingMapManager();
            if (mgr == null)
                {
                _trace("Service " + service.getInfo().getServiceName() +
                       " does not have BackingMapManager;" +
                       " use coherence-cache-config.xml and 'cache' command to create a cache");
                return;
                }

            String sCacheName = map.getCacheName();
            try
                {
                mapListen = (ObservableMap) ClassHelper.invoke(mgr, "getBackingMap",
                                    new Object[] {sCacheName});
                }
            catch (ClassCastException e)
                {
                _trace("Local storage for cache: " + sCacheName + " is not observable;" +
                       " use coherence-cache-config.xml to configure");
                return;
                }
            catch (Exception e)
                {
                _trace("Service " + service.getInfo().getServiceName() +
                       " uses unsupported BackingMapManager: " + mgr.getClass().getName());
                return;
                }

            if (mapListen == null)
                {
                _trace("Local storage for cache: " + sCacheName + " is missing");
                return;
                }
            }

        String  sWorker = "Worker!" + sSource;
        Coherence.Worker worker  = (Coherence.Worker) _findChild(sWorker);
        if (fStop)
            {
            if (sSource.startsWith("trigger"))
                {
                int nAction = sSource.endsWith("remove") ? FilterTrigger.ACTION_REMOVE :
                              sSource.endsWith("ignore") ? FilterTrigger.ACTION_IGNORE :
                                                           FilterTrigger.ACTION_ROLLBACK;
                mapListen.removeMapListener(
                    new MapTriggerListener(new FilterTrigger(filter, nAction)));
                }
            else if (worker == null)
                {
                _trace("No listener to stop");
                }
            else
                {
                if (sSource.equals("members"))
                    {
                    service.removeMemberListener(worker);
                    }
                else if (sSource.equals("master"))
                    {
                    service.getCluster().
                        ensureService("Cluster", "Cluster").removeMemberListener(worker);
                    }
                else if (sSource.equals("service"))
                    {
                    service.removeServiceListener(worker);
                    }
                else if (sSource.equals("partition"))
                    {
                    ((PartitionedService) service).removePartitionListener(worker);
                    }
                else if (sSource.equals("interceptor"))
                    {
                    CacheFactory.getConfigurableCacheFactory().
                        getInterceptorRegistry().unregisterEventInterceptor(worker.getInterceptorId());
                    }
                else
                    {
                    MapListener listener = worker;
                    if (sSource.endsWith("-sync"))
                        {
                        listener = new com.tangosol.util.MapListenerSupport.WrapperSynchronousListener(listener);
                        }
                    else if (sSource.endsWith("-priming"))
                        {
                        listener = new com.tangosol.util.MapListenerSupport.WrapperPrimingListener(listener);
                        }
                    else if (sSource.endsWith("-versioned"))
                        {
                        listener = VersionAwareListeners.createListener(listener);
                        }

                    if (oKey == null)
                        {
                        mapListen.removeMapListener(listener, filter);
                        }
                    else if (oKey instanceof Set)
                        {
                        mapListen.removeMapListener(listener, new InKeySetFilter(null, (Set) oKey));
                        }
                    else
                        {
                        mapListen.removeMapListener(listener, oKey);
                        }
                    }
                int cRefCount = worker.getRefCount() - 1;
                if (cRefCount == 0)
                    {
                    _removeChild(worker);
                    }
                else
                    {
                    worker.setRefCount(cRefCount);
                    }
                }
            }
        else
            {
            if (sSource.startsWith("trigger"))
                {
                int nAction = sSource.endsWith("remove") ? FilterTrigger.ACTION_REMOVE :
                              sSource.endsWith("ignore") ? FilterTrigger.ACTION_IGNORE :
                                                           FilterTrigger.ACTION_ROLLBACK;
                mapListen.addMapListener(
                    new MapTriggerListener(new FilterTrigger(filter, nAction)));
                }
            else
                {
                if (worker == null)
                    {
                    worker = new Coherence.Worker();
                    _addChild(worker, sWorker);

                    if (sSource.charAt(0) == '@')
                        {
                        worker.setSilent(true);
                        }
                    }

                if (sSource.equals("members"))
                    {
                    service.addMemberListener(worker);
                    }
                else if (sSource.equals("master"))
                    {
                    service.getCluster().
                        ensureService("Cluster", "Cluster").addMemberListener(worker);
                    }
                else if (sSource.equals("service"))
                    {
                    service.addServiceListener(worker);
                    }
                else if (sSource.equals("partition"))
                    {
                    ((PartitionedService) service).addPartitionListener(worker);
                    }
                else if (sSource.equals("interceptor"))
                    {
                    if (oKey instanceof String)
                        {
                        worker.setEventTypes(new ImmutableArrayList(
                            ((String) oKey).split(",")));
                        }
                    String sId = CacheFactory.getConfigurableCacheFactory().
                        getInterceptorRegistry().registerEventInterceptor(worker);

                    worker.setInterceptorId(sId);
                    }
                else
                    {
                    MapListener listener = worker;
                    if (sSource.endsWith("-sync"))
                        {
                        listener = new com.tangosol.util.MapListenerSupport.WrapperSynchronousListener(listener);
                        }
                    else if (sSource.endsWith("-priming"))
                        {
                        listener = new com.tangosol.util.MapListenerSupport.WrapperPrimingListener(listener);
                        }
                    else if (sSource.endsWith("-versioned"))
                        {
                        if (filter != null)
                            {
                            laVersions = laVersions == null ? new com.tangosol.util.PrimitiveSparseArray() : laVersions;
                            }
                        else
                            {
                            LVersion = LVersion == null ? Long.valueOf(VersionAwareMapListener.HEAD) : LVersion;
                            }
                        }

                    if (oKey instanceof Set)
                        {
                        filter = new InKeySetFilter(null, (Set) oKey);
                        oKey   = null;
                        }

                    if (LVersion != null)
                        {
                        if (service instanceof PartitionedService)
                            {
                            int iPart = ((PartitionedService) service)
                                .getKeyPartitioningStrategy().getKeyPartition(oKey);

                            laVersions = new com.tangosol.util.PrimitiveSparseArray();

                            laVersions.setPrimitive(iPart, LVersion.longValue());
                            }
                        }

                    if (laVersions != null)
                        {
                        listener = VersionAwareListeners.createListener(listener, laVersions);
                        }

                    if (oKey == null)
                        {
                        mapListen.addMapListener(listener, filter, fLite);
                        }
                    else
                        {
                        mapListen.addMapListener(listener, oKey, fLite);
                        }
                    }
                worker.setRefCount(worker.getRefCount() + 1);
                }
            }
        }

    protected void doLog(Object[] aoParam, boolean fSilent)
        {
        // import com.tangosol.util.Base;

        int    cParams = aoParam.length;
        String sMsg    = null;
        int    cchMsg  = 50;
        if (cParams >= 1)
            {
            if (aoParam[0] instanceof Number)
                {
                cchMsg = ((Number) aoParam[0]).intValue();
                cchMsg = Math.max(1, cchMsg);
                }
            else
                {
                sMsg   = String.valueOf(aoParam[0]);
                cchMsg = sMsg.length();
                }
            }
        if (sMsg == null)
            {
            sMsg = Base.dup('*', cchMsg);
            }

        int cIters = 1;
        if (cParams >= 2 && aoParam[1] instanceof Number)
            {
            cIters = ((Number) aoParam[1]).intValue();
            }
        cIters = Math.max(1, cIters);

        int nLevel = 3;
        if (cParams >= 3 && aoParam[2] instanceof Number)
            {
            nLevel = ((Number) aoParam[2]).intValue();
            }

        if (!fSilent)
            {
            _trace("Logging " + cIters + " messages of " + cchMsg + " characters at level " + nLevel + ":");
            }

        if (cIters == 1)
            {
            _trace(sMsg, nLevel);
            }
        else
            {
            for (int i = 1; i <= cIters; ++i)
                {
                _trace(i + ":" + sMsg, nLevel);
                }
            }
        }

    /**
     * Issue a persistence command against the current service.
    *
    * @param asParam                array of parameters to the snapshot command
    * @param fSilent                    indicates if the operation is silent
     */
    protected void doPersistence(String[] asParam, boolean fSilent)
        {
        // import com.tangosol.coherence.dslquery.internal.PersistenceToolsHelper;
        // import com.tangosol.net.Service as com.tangosol.net.Service;

        // processCommands() ensures we have at least one parameter

        String                 sOp     = asParam[0];
        PersistenceToolsHelper helper  = getPersistenceToolsHelper();
        Object                 oResult = "Success";
        if (helper == null)
            {
            helper = new PersistenceToolsHelper();
            setPersistenceToolsHelper(helper);
            }

        try
            {
            if ("force".equals(sOp))
                {
                helper.invokeOperation("forceRecovery", asParam[1], null, null);
                return;
                }

            if (!isMapValid())
                {
                oResult = null;
                return;
                }

            com.tangosol.net.Service service       = getService();
            String  sServiceName  = service.getInfo().getServiceName();
            String  sSnapshotName = null;
            String  sOperation    = null;
            int     cParams       = asParam.length;

            if ("list".equals(sOp))
                {
                // snapshot list [archived]

                boolean fArchived = cParams == 2 &&
                                    "archived".equals(asParam[1]);
                _trace((fArchived ? "Archived s" : "S") +
                       "napshots for service " + sServiceName);
                String[] asSnapshots = fArchived
                                       ? helper.listArchivedSnapshots(sServiceName)
                                       : helper.listSnapshots(sServiceName);
                if (asSnapshots != null)
                    {
                    for (int i = 0; i < asSnapshots.length; i++)
                        {
                        _trace("    " + asSnapshots[i]);
                        }
                    }
                oResult = null;
                }
            else
                {
                // snapshot [create | recover| archive | remove] <snapshot-name>
                // snapshot [retrieve | remove] archived <snapshot-name>

                int i = 1;  // index of snapshot name in asParam
                if ("create".equals(sOp))
                    {
                    sOperation = PersistenceToolsHelper.CREATE_SNAPSHOT;
                    }
                else if ("recover".equals(sOp))
                    {
                    sOperation = PersistenceToolsHelper.RECOVER_SNAPSHOT;
                    }
                else if ("archive".equals(sOp))
                    {
                    sOperation = PersistenceToolsHelper.ARCHIVE_SNAPSHOT;
                    }
                else if ("remove".equals(sOp))
                    {
                    sOperation = cParams == 2
                                 ? PersistenceToolsHelper.REMOVE_SNAPSHOT
                                 : (cParams == 3 && "archived".equals(asParam[1])
                                   ? PersistenceToolsHelper.REMOVE_ARCHIVED_SNAPSHOT : null);
                    i = cParams == 3 ? 2 : 1;
                    }
                else if ("retrieve".equals(sOp))
                    {
                    sOperation = cParams == 3 && "archived".equals(asParam[1])
                                  ? PersistenceToolsHelper.RETRIEVE_ARCHIVED_SNAPSHOT
                                  : null;
                    i = 2;
                    }
                else
                    {
                    _trace("Invalid persistence/snapshot command");
                    return;
                    }
                sSnapshotName = asParam[i];

                if (!fSilent)
                    {
                    _trace("Issuing " + sOperation + " for service " + sServiceName +
                           " and snapshot " + sSnapshotName);
                    }

                helper.invokeOperationWithWait(sOperation, sSnapshotName, sServiceName);
                }
            }
        catch (Exception e)
            {
            oResult = e.getMessage();
            }

        if (!fSilent && oResult != null)
            {
            _trace(oResult.toString());
            }
        ((ThreadLocal) get_Sink()).set(oResult);
        }

    protected void doProcess(Object[] aoParam, boolean fAsync, boolean fSilent)
        {
        // import com.tangosol.net.NamedCache;
        // import com.tangosol.util.ClassHelper;
        // import com.tangosol.util.Filter;
        // import com.tangosol.util.InvocableMap$EntryProcessor as com.tangosol.util.InvocableMap.EntryProcessor;
        // import com.tangosol.util.processor.AsynchronousProcessor;
        // import com.tangosol.util.processor.ConditionalProcessor;
        // import java.util.Collection;

        // (<key> | '{' <key> [, <key>]* '}' | "{filter:"<filter-keys>'}' | *) \
        //      ["{filter:<filter-value>}"] <processor-name> [<paramValue>]+");

        NamedCache map = getMap();

        int cParams = aoParam.length;
        if (cParams < 2)
            {
            _trace("Processor name must be specified");
            return;
            }

        int    iParam  = 0;
        Object oTarget = aoParam[iParam++];
        Filter filter  = aoParam[iParam] instanceof Filter ?
                (Filter) aoParam[iParam++] : null;

        String   sProcessor       = (String) aoParam[iParam++];
        Object[] aoProcessorParam = new Object[cParams -= iParam];
        if (cParams > 0)
            {
            System.arraycopy(aoParam, iParam, aoProcessorParam, 0, cParams);
            }

        com.tangosol.util.InvocableMap.EntryProcessor processor;
        try
            {
            Class clzProcessor = Class.forName("com.tangosol.util.processor." + sProcessor);
            processor = (com.tangosol.util.InvocableMap.EntryProcessor) ClassHelper.newInstance(clzProcessor, aoProcessorParam);
            }
        catch (Throwable e)
            {
            printException("Invalid processor format: " + sProcessor
                           + " " + toString(aoProcessorParam) + "\n", e);
            return;
            }

        if (filter != null)
            {
            processor = new ConditionalProcessor(filter, processor);
            }

        if (fAsync)
            {
            processor = new AsynchronousProcessor(processor);
            }

        Object oResult =
            oTarget instanceof Filter || oTarget == null ?
                map.invokeAll((Filter) oTarget, processor) :
            oTarget instanceof Collection ?
                map.invokeAll((Collection) oTarget, processor) :
            oTarget.equals("*") ?
                map.invokeAll(map.keySet(), processor) :
            // just a key
                map.invoke(oTarget, processor);

        if (fAsync)
            {
            oResult = processor;
            }

        ((ThreadLocal) get_Sink()).set(oResult);
        if (!fSilent)
            {
            _trace(String.valueOf(oResult));
            }
        }

    /**
     * Reexecute a command from the command history.
     */
    protected void doReissueCommand(int nCmd, String sRemainder, boolean fSilent)
            throws java.lang.InterruptedException
        {
        // import java.util.List;

        List list = getCommandHistory();

        if (list == null)
            {
            _trace("history is off");
            return;
            }

        if (nCmd <= 0 || nCmd > list.size())
            {
            _trace("history index out of range");
            return;
            }

        String sCmd = (String) list.get(nCmd - 1);
        if (sRemainder != null && sRemainder.length() > 0)
            {
            sCmd = (fSilent ? "@" + sCmd : sCmd) + " " + sRemainder;
            recordCommand(sCmd);
            }
        else if (fSilent)
            {
            sCmd = "@" + sCmd;
            }

        _trace("reissuing: " + sCmd);
        processCommand(sCmd);
        }

    protected void doRepeat(String sCmd, int cIter, boolean fForce)
            throws java.lang.InterruptedException
        {
        // import java.util.ArrayList;
        // import java.util.List;

        List list   = new ArrayList();
        int  cch    = sCmd.length();
        int  ofPrev = 0;
        while (ofPrev < cch)
            {
            int ofNext;
            if (sCmd.charAt(ofPrev) == '{')
                {
                ofPrev++;
                ofNext = sCmd.lastIndexOf('}');
                }
            else
                {
                ofNext = sCmd.indexOf(';', ofPrev);
                }

            if (ofNext < 0)
                {
                list.add(sCmd.substring(ofPrev));
                break;
                }
            else
                {
                list.add(sCmd.substring(ofPrev, ofNext));
                ofPrev = ofNext + 1;
                }
            }

        int      cCmds = list.size();
        String[] asCmd = (String[]) list.toArray(new String[cCmds]);

        repeat:
        for (int i = 0; i < cIter; i++)
            {
            ((ThreadLocal) get_Sink()).set(Integer.valueOf(i));
            for (int j = 0; j < cCmds; j++)
                {
                sCmd = asCmd[j].trim();
                if (sCmd.length() > 0)
                    {
                    Object oResult = processCommand(sCmd);
                    if (oResult instanceof Throwable)
                        {
                        if (fForce)
                            {
                            // continue next iteration
                            break;
                            }
                        else
                            {
                            // get out
                            break repeat;
                            }
                        }
                    }
                }
            }
        }

    protected void doRestore(String[] asParam, boolean fSilent)
        {
        // import com.tangosol.internal.util.MapBackupHelper as com.tangosol.internal.util.MapBackupHelper;
        // import java.io.BufferedInputStream;
        // import java.io.DataInputStream;
        // import java.io.File;
        // import java.io.FileInputStream;
        // import java.io.IOException;
        // import java.util.HashMap;
        // import java.util.Map;

        if (asParam.length == 0)
            {
            _trace("File name is expected");
            return;
            }

        Map map = getMap();

        try
            {
            ClassLoader     loader     = getClass().getClassLoader();
            File            file       = new File(asParam[0]);
            FileInputStream streamFile = new FileInputStream(file);
            DataInputStream streamData = new DataInputStream(
                new BufferedInputStream(streamFile, 32*1024));

            int cBlock = Integer.MAX_VALUE;
            if (asParam.length > 1)
                {
                try
                    {
                    cBlock = Integer.parseInt(asParam[1]);
                    _assert(cBlock > 0);
                    }
                catch (Exception e)
                    {
                    _trace("Invalid block size");
                    return;
                    }
                }
            int cEntries = com.tangosol.internal.util.MapBackupHelper.readMap(streamData, map, cBlock, loader);

            if (!fSilent)
                {
                _trace(cEntries + " entries restored from " + file.getAbsolutePath());
                }

            streamData.close();
            streamFile.close();
            }
        catch (IOException e)
            {
            _trace("Failed to restore: " + e);
            }
        }

    protected void doScan(String[] asParam)
        {
        // import java.util.Map;

        int iFirst = 1;
        int cIters = 1000;
        try
            {
            iFirst = Integer.parseInt(asParam[0]);
            cIters = Integer.parseInt(asParam[1]);
            }
        catch (NumberFormatException e)
            {
            _trace("Assuming first=" + iFirst + ", iterations=" + cIters);
            }

        Map map       = getMap();
        int iGapStart = 0;
        int iGapEnd   = 0;
        for (int i = iFirst, iLast = iFirst + cIters; i < iLast; ++i)
            {
            if (!map.containsKey(Integer.valueOf(i)))
                {
                iGapStart = i;
                iGapEnd   = i;
                while (++i < iLast && !map.containsKey(Integer.valueOf(i)))
                    {
                    iGapEnd = i;
                    }
                if (iGapStart == iGapEnd)
                    {
                    _trace("\nMissing item: " + iGapStart);
                    }
                else
                    {
                    _trace("\nMissing items: " + iGapStart + " .. " + iGapEnd);
                    }
                }
            if (i > iGapStart + 1000 && i % 1000 == 0)
                {
                System.out.print("\b\b\b\b\b\b\b" + i);
                System.out.flush();
                }
            }
        }

    protected void doScript(String sCmd, boolean fSilent)
        {
        // import com.tangosol.net.NamedCache;
        // import javax.script.ScriptEngine;

        ScriptEngine engine = getScriptEngine();
        NamedCache   cache  = getMap();

        engine.put("cluster", getSafeCluster());
        engine.put("service", getService());
        engine.put("map", cache);

        if (sCmd.length() == 0)
            {
            setScript(true);
            getLogger().setPrompt(getLogger().getPrompt() + " [script]");
            }
        else if (sCmd.equals("bye"))
            {
            setScript(false);
            getLogger().setPrompt(cache == null ? "?" : cache.getCacheName());
            }
        else
            {
            String sPrefix  = "with (imports) {\n";
            String sPostfix = "\n}\n";

            Object oResult;
            try
                {
                oResult = engine.eval(sPrefix + sCmd + sPostfix);
                }
            catch (Exception e)
                {
                oResult = e.getMessage();
                fSilent = false;
                }

            if (!fSilent)
                {
                _trace(toString(oResult));
                }
            }
        }

    protected Object doSecure(String[] asParam, boolean fSilent)
            throws java.lang.InterruptedException
        {
        // import Component.Net.Security;
        // import com.tangosol.net.security.SimpleHandler;
        // import com.tangosol.util.ClassHelper;
        // import com.tangosol.util.WrapperException;
        // import java.lang.reflect.InvocationTargetException;
        // import java.security.AccessControlException;
        // import java.security.PrivilegedAction;
        // import javax.security.auth.callback.CallbackHandler;
        // import javax.security.auth.Subject;

        // <name> <password> <command>

        int cParams = asParam.length;
        if (cParams < 3)
            {
            return null;
            }

        String sName = asParam[0];
        char[] acPwd = asParam[1].toCharArray();
        String sCmd  = asParam[2];

        for (int i = 3; i < cParams; i++)
            {
            sCmd += ' ' + asParam[i];
            }

        boolean fImpersonate = false;
        if (sName.startsWith("hack_"))
            {
            fImpersonate = true;
            sName = sName.substring("hack_".length());
            }


        CallbackHandler  handler;
        PrivilegedAction action;
        Subject          subject;
        try
            {
            handler = new SimpleHandler(sName, acPwd);

            // this.processCommand(sCmd);
            action  = Security.createPrivilegedAction(
                getClass().getMethod("processCommand", new Class[] {String.class}),
                this, new Object[] {sCmd});

            subject = (Subject) ClassHelper.invokeStatic(
                Class.forName("com.tangosol.net.security.Security"),
                "login", new Object[] {handler});
            }
        catch (Throwable e)
            {
            printException(null, e);
            return null;
            }

        if (fImpersonate)
            {
            subject = Security.getInstance().impersonate(subject, sName, asParam[0]);
            }

        try
            {
            return ClassHelper.invokeStatic(
                Class.forName("com.tangosol.net.security.Security"),
                "runAs", new Object[] {subject, action});
            }
        catch (Throwable e)
            {
            Throwable eOrig = e;
            while (true)
                {
                if (e instanceof WrapperException)
                    {
                    e = ((WrapperException) e).getOriginalException();
                    }
                else if (e instanceof InvocationTargetException)
                    {
                    e = eOrig = ((InvocationTargetException) e).getTargetException();
                    }
                else
                    {
                    break;
                    }
                }
            if (e instanceof AccessControlException)
                {
                _trace("Permission rejected: " +
                    ((AccessControlException) e).getPermission(), 1);
                }
            _trace(eOrig);
            return null;
            }
        }

    /**
     * Start a server instance either under the context of the default cache
    * config or a gar file.
     */
    protected void doServer(String[] asParam)
        {
        // import com.tangosol.application.ContainerContext;
        // import com.tangosol.net.DefaultCacheServer as com.tangosol.net.DefaultCacheServer;
        // import com.tangosol.net.DefaultCacheServer$GarServer as com.tangosol.net.DefaultCacheServer.GarServer;
        // import com.tangosol.util.Base;
        // import java.io.File;

        String   sGarName = null;
        String   sAppName = null;
        String[] asTenant = null;

        switch (asParam.length)
            {
            case 3:
                asTenant = Base.parseDelimitedString(asParam[2], ',');
            case 2:
                sAppName = asParam[1];
            case 1:
                sGarName = asParam[0];
            }

        try
            {
            com.tangosol.net.DefaultCacheServer.startServerDaemon();
            }
        catch (Throwable e)
            {
            _trace("Error in starting DefaultCacheServer: " + e.getMessage(), 1);
            }
        }

    protected void doTransaction(String sFunction, String[] asParam)
        {
        // import com.tangosol.util.TransactionMap;

        if (sFunction.equals("begin"))
            {
            String sConcur    = asParam[0]; // E[xternal], O[ptimistic] or P[essimistic]
            String sIsolation = asParam[1]; // C[ommited], R[epeatable] or S[erialized]
            int nTimeout      = 20;
            try
                {
                nTimeout = Integer.parseInt(asParam[2]);
                }
            catch (Exception ignored) {}

            int nConcur;
            switch (sConcur.charAt(0))
                {
                case 'E': case 'e':
                    nConcur = TransactionMap.CONCUR_EXTERNAL;
                    break;
                case 'O': case 'o':
                    nConcur = TransactionMap.CONCUR_OPTIMISTIC;
                    break;
                default:
                    nConcur = TransactionMap.CONCUR_PESSIMISTIC;
                    break;
                    }

            int nIsolation;
            switch (sIsolation.charAt(0))
                {
                case 'C': case 'c':
                    nIsolation = TransactionMap.TRANSACTION_GET_COMMITTED;
                    break;
                case 'R': case 'r':
                    nIsolation = TransactionMap.TRANSACTION_REPEATABLE_GET;
                    break;
                default:
                    nIsolation = TransactionMap.TRANSACTION_SERIALIZABLE;
                    break;
                }
            txStart(nConcur, nIsolation, nTimeout);
            }
        else if (
            sFunction.equals("commit"))
            {
            try
                {
                ((TransactionMap) getMap()).prepare();
                ((TransactionMap) getMap()).commit();
                }
            finally
                {
                txEnd();
                }
            }
        else if (
            sFunction.equals("rollback"))
            {
            ((TransactionMap) getMap()).rollback();
            txEnd();
            }
        }

    protected Coherence.Logger ensureRunningLogger()
        {
        // import com.tangosol.internal.net.logging.DefaultLoggingDependencies;
        // import com.tangosol.internal.net.logging.LegacyXmlLoggingHelper;

        Coherence.Logger logger = getLogger();
        if (!logger.isStarted())
            {
            synchronized (get_CLASS())
                {
                // the getServiceConfig() call below may call the logger recursively,
                // so we need to protect against that by returning not-yet-running logger
                if (!logger.isStarted() && !logger.isConfigured())
                    {
                    try
                        {
                        logger.setConfigured(true);
                        logger.setDependencies(LegacyXmlLoggingHelper.fromXml(getServiceConfig("$Logger"),
                            new DefaultLoggingDependencies()));
                        logger.start();
                        }
                    catch (RuntimeException e)
                        {
                        setLogger(null);
                        throw e;
                        }
                    catch (Error e)
                        {
                        setLogger(null);
                        throw e;
                        }
                    }
                }
            }
        return logger;
        }

    // Declared at the super level
    /**
     * Getter for property _Sink.<p>
    * Property used by Component Integration. This property is not meant to be
    * designed directly.
     */
    public Object get_Sink()
        {
        Object oSink = super.get_Sink();
        if (oSink == null)
            {
            synchronized (this)
                {
                oSink = super.get_Sink();
                if (oSink == null)
                    {
                    set_Sink(oSink = new ThreadLocal());
                    }
                }
            }
        return oSink;
        }

    // Accessor for the property "BuildNumber"
    /**
     * Getter for property BuildNumber.<p>
    * Build number is initialized in _initStatic(); therefore, it cannot be
    * designed.
     */
    public static String getBuildNumber()
        {
        return __s_BuildNumber;
        }

    // Accessor for the property "Cluster"
    /**
     * Getter for property Cluster.<p>
    * The Cluster instance.
     */
    public static com.tangosol.coherence.component.util.SafeCluster getCluster()
        {
        return __s_Cluster;
        }

    /**
     * For a given manifest InputStream, return the manifest attributes if they
    * contain Coherence metadata; if the attributes do not contain Coherence
    * metadata return null.
    *
    * Note that the InputStream will not be closed; this is the responsibility
    * of the caller.
     */
    protected static java.util.jar.Attributes getCoherenceAttributes(java.io.InputStream in)
            throws java.io.IOException
        {
        // import java.util.jar.Attributes;
        // import java.util.jar.Manifest;

        Attributes attrs    = new Manifest(in).getMainAttributes();
        String     sGroupId = attrs.getValue("Implementation-GroupId");
        return sGroupId == null || !sGroupId.startsWith("com.oracle.coherence")
                ? null : attrs;
        }

    // Accessor for the property "CommandHistory"
    /**
     * Getter for property CommandHistory.<p>
    * History of recently executed commands
     */
    public java.util.List getCommandHistory()
        {
        return __m_CommandHistory;
        }

    // Accessor for the property "Edition"
    /**
     * Getter for property Edition.<p>
    * The Edition is the product type.
    *
    * 0=Data Client (DC)
    * 1=Real-Time Client (RTC)
    * 2=Compute Client (CC)
    * 3=Standard Edition (SE)
    * 4=Enterprise Edition (EE)
    * 5=Grid Edition (GE)
     */
    public int getEdition()
        {
        return __m_Edition;
        }

    // Accessor for the property "Filters"
    /**
     * Getter for property Filters.<p>
    * Filters keyed by their names. Used in filter and list commands.
     */
    public java.util.Map getFilters()
        {
        // import java.util.HashMap;
        // import java.util.Map;

        Map mapFilter = __m_Filters;
        if (mapFilter == null)
            {
            setFilters(mapFilter = new HashMap());
            }
        return mapFilter;
        }

    /**
     * Get jline history object.
     */
    protected Object getHistory(Object oReader)
        {
        // import com.tangosol.util.ClassHelper;
        // import java.io.Reader;

        if (!(oReader instanceof Reader))
            {
            try
                {
                return ClassHelper.invoke(oReader, "getHistory", ClassHelper.VOID);
                }
            catch (Exception e)
                {
                System.out.println("failed to flush history: " + e);
                }
            }

        return null;
        }

    /**
     * @see CacheFactory#getLocalTransaction
     */
    public static com.tangosol.util.TransactionMap getLocalTransaction(com.tangosol.net.NamedCache cache)
        {
        // import Component.Util.TransactionCache;
        // import Component.Util.TransactionCache.Local as com.tangosol.coherence.component.util.transactionCache.Local;

        TransactionCache mapTx = new com.tangosol.coherence.component.util.transactionCache.Local();

        mapTx.initialize(cache);

        return mapTx;
        }

    // Accessor for the property "Logger"
    /**
     * Getter for property Logger.<p>
    * The low priority logging daemon.
     */
    public Coherence.Logger getLogger()
        {
        // import java.util.concurrent.atomic.AtomicReference;

        AtomicReference refLogger = getLoggerRef();

        while (true)
            {
            Coherence.Logger logger = (Coherence.Logger) refLogger.get();

            if (logger == null)
                {
                logger = (Coherence.Logger) _newChild("Logger");

                if (!refLogger.compareAndSet(null, logger))
                    {
                    continue;
                    }
                }

            return logger;
            }
        }

    // Accessor for the property "LoggerRef"
    /**
     * Getter for property LoggerRef.<p>
    * The logger reference.
     */
    protected java.util.concurrent.atomic.AtomicReference getLoggerRef()
        {
        return __m_LoggerRef;
        }

    // Accessor for the property "Map"
    /**
     * Getter for property Map.<p>
    * Map assosiated with currently tested service
     */
    public com.tangosol.net.NamedCache getMap()
        {
        return __m_Map;
        }

    // Accessor for the property "Mode"
    /**
     * Getter for property Mode.<p>
    * The Mode is the "license type", i.e. evaluation, development or
    * production use.
    *
    * 0=evaluation
    * 1=development
    * 2=production
     */
    public int getMode()
        {
        return __m_Mode;
        }

    // Accessor for the property "PersistenceToolsHelper"
    /**
     * Getter for property PersistenceToolsHelper.<p>
    * Holds a help instance for issuing persistence commands.
     */
    public com.tangosol.coherence.dslquery.internal.PersistenceToolsHelper getPersistenceToolsHelper()
        {
        return __m_PersistenceToolsHelper;
        }

    // Accessor for the property "Product"
    /**
     * Getter for property Product.<p>
    * The product name.
     */
    public String getProduct()
        {
        return __m_Product;
        }

    public static com.tangosol.coherence.component.util.SafeCluster getSafeCluster()
        {
        // import Component.Net.Management.Gateway;
        // import Component.Util.SafeCluster;
        // import com.tangosol.internal.net.management.LegacyXmlGatewayHelper;
        // import com.tangosol.internal.net.management.DefaultGatewayDependencies;
        // import com.tangosol.net.ClusterDependencies;

        SafeCluster cluster = getCluster();
        if (cluster == null)
            {
            cluster = (SafeCluster) getTloCluster().get();
            if (cluster == null)
                {
                synchronized (get_CLASS())
                    {
                    cluster = getCluster();
                    if (cluster == null)
                        {
                        getTloCluster().set(cluster = new SafeCluster());
                        try
                            {
                            ((Coherence) get_Instance()).ensureRunningLogger();

                            // we must ensure the SafeCluster has dependencies here as the Gateway
                            // uses them to get cluster name
                            cluster.ensureDependencies();

                            DefaultGatewayDependencies dependencies =
                                LegacyXmlGatewayHelper.fromXml(getServiceConfig("$Management"), new DefaultGatewayDependencies());
                            Gateway mgmt = Gateway.createGateway(dependencies, cluster);
                            cluster.setManagement(mgmt);
                            setCluster(cluster);
                            }
                        finally
                            {
                            getTloCluster().set(null);
                            }
                        }
                    }
                }
            }

        return cluster;
        }

    // Accessor for the property "ScriptEngine"
    /**
     * Getter for property ScriptEngine.<p>
    * The script engine (nashorn);
     */
    public javax.script.ScriptEngine getScriptEngine()
        {
        // import javax.script.ScriptEngine;
        // import javax.script.ScriptEngineManager;
        // import javax.script.ScriptException;

        ScriptEngine engine = __m_ScriptEngine;
        if (engine == null)
            {
            String sInit =
                "var imports = new JavaImporter(java.lang, com.tangosol.net,"
              + "com.tangosol.util,"
              + "com.tangosol.util.extractor, com.tangosol.util.filter, com.tangosol.util.processor,"
              + "com.tangosol.util.aggregator)\n"
              ;

            engine = new ScriptEngineManager().getEngineByName("nashorn");
            engine.put("coherence", this);
            try
                {
                engine.eval(sInit);
                }
            catch (ScriptException e)
                {
                throw new RuntimeException(e);
                }

            setScriptEngine(engine);
            }
        return engine;
        }

    // Accessor for the property "Service"
    /**
     * Getter for property Service.<p>
    * Service that is currently being tested
     */
    public com.tangosol.net.CacheService getService()
        {
        return __m_Service;
        }

    /**
     * See CacheFactory#getServiceConfig.
     */
    public static com.tangosol.run.xml.XmlElement getServiceConfig(String sServiceType)
        {
        // import com.tangosol.run.xml.XmlElement;
        // import java.util.Map;

        Map mapConfig = getServiceConfigMap();

        synchronized (get_CLASS())
            {
            if (!isConfigurationLoaded())
                {
                ((Coherence) get_Instance()).ensureRunningLogger();

                if (!isConfigurationLoaded())
                    {
                    loadConfiguration();
                    setConfigurationLoaded(true);

                    validateEnvironment();
                    }
                }
            }

        XmlElement xml = (XmlElement) mapConfig.get(sServiceType);
        return xml == null ? null : (XmlElement) xml.clone();
        }

    // Accessor for the property "ServiceConfigMap"
    /**
     * Getter for property ServiceConfigMap.<p>
    * Map containing the service configuration element per service type
    * (including pseudo-types like $Logger, etc.).
    *
    * @see #loadConfiguration
     */
    protected static java.util.Map getServiceConfigMap()
        {
        return __s_ServiceConfigMap;
        }

    /**
     * For performance measuring only
     */
    protected com.tangosol.net.CacheService getSimpleCache(String sServiceName)
        {
        // import com.tangosol.net.Cluster;
        // import com.tangosol.net.CacheFactory;
        // import com.tangosol.net.CacheService;

        sServiceName = sServiceName == null || sServiceName.length() == 0 ?
            "Default" : sServiceName;

        Cluster      cluster = CacheFactory.ensureCluster();
        CacheService service =
            (CacheService) cluster.ensureService(sServiceName, "SimpleCache");

        if (!service.isRunning())
            {
            service.configure(getServiceConfig("SimpleCache"));
            service.start();
            }

        return service;
        }

    // Accessor for the property "TloCluster"
    /**
     * Getter for property TloCluster.<p>
    * The thread local object to avoid an infinite recursion.
     */
    private static ThreadLocal getTloCluster()
        {
        return __s_TloCluster;
        }

    /**
     * Creates a unique class loader for testing ClassLoader specific issues.
     */
    protected ClassLoader getUniqueClassLoader()
        {
        // import com.tangosol.dev.component.ComponentClassLoader;
        // import com.tangosol.dev.component.NullStorage;

        return new ComponentClassLoader(getClass().getClassLoader(), new NullStorage());
        }

    // Accessor for the property "ConfigurationLoaded"
    /**
     * Getter for property ConfigurationLoaded.<p>
    * Flag to indicate that the configuration has been loaded
    * @volatile
     */
    public static boolean isConfigurationLoaded()
        {
        return __s_ConfigurationLoaded;
        }

    // Declared at the super level
    /**
     * Check whether or not the debug output for the specified severity level is
    * enabled according to the Application context.
     */
    public boolean isDebugOutputEnabled(int iSeverity)
        {
        Coherence.Logger logger = getLogger();
        return logger == null ?
            super.isDebugOutputEnabled(iSeverity) : logger.isEnabled(iSeverity);
        }

    // Accessor for the property "LicenseLoaded"
    /**
     * Getter for property LicenseLoaded.<p>
     */
    private static boolean isLicenseLoaded()
        {
        return __s_LicenseLoaded;
        }

    // Accessor for the property "MapValid"
    /**
     * Getter for property MapValid.<p>
    * Verifies whether the current map is valid.
     */
    protected boolean isMapValid()
        {
        if (getMap() == null)
            {
            _trace("Please specify the current map using \"cache\" or \"map\" command");
            return false;
            }
        return true;
        }

    // Accessor for the property "Script"
    /**
     * Getter for property Script.<p>
    * Inidcates that the command line tool is in the "scripting" mode.
     */
    public boolean isScript()
        {
        return __m_Script;
        }

    // Accessor for the property "Stop"
    /**
     * Getter for property Stop.<p>
    * Specifies whether to stop the command tool.
     */
    public boolean isStop()
        {
        return __m_Stop;
        }

    protected static void loadConfiguration()
        {
        // import com.tangosol.run.xml.SimpleElement;
        // import com.tangosol.run.xml.SimpleParser;
        // import com.tangosol.run.xml.XmlDocument;
        // import com.tangosol.run.xml.XmlElement;
        // import com.tangosol.run.xml.XmlHelper;
        // import com.tangosol.util.Base;
        // import com.tangosol.util.Resources;
        // import com.tangosol.util.WrapperException;
        // import java.io.IOException;
        // import java.net.URL;
        // import java.util.Iterator;
        // import java.util.Map;
        // import java.util.HashSet;

        // instantiate the Coherence singleton (necessary for logging)
        get_Instance();

        if (Config.getBoolean("coherence.debug.operational.config"))
            {
            StringBuilder       sbTrace    = new StringBuilder("Loading Coherence operational configuration, call stack:\n");
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            for (int i = 1; i < stackTrace.length; i++)
                {
                sbTrace.append("\tat ")
                        .append(stackTrace[i])
                        .append("\n");
                }
            _trace(sbTrace.toString());
            }

        Map         mapConfig    = getServiceConfigMap();
        XmlDocument xmlCoherence = XmlHelper.loadResource(FILE_CFG_COHERENCE,
            "operational configuration", get_CLASS().getClassLoader());

        // adjust configurations from soft-coded system property names
        replaceSystemProperties(xmlCoherence);

        XmlElement xmlLicense = xmlCoherence.ensureElement("license-config");

        // pick a license mode (prod, dev, eval)
        // Note: The operational mode cannot be overriden and must be resolved
        // upfront since loadOverrides() method needs it there
        String sModeDescription = resolveMode(xmlLicense);

        // make mode available for use by loadOverrides; the $License entry will be
        // updated again once the edition has been resolved.
        mapConfig.put("$License", xmlLicense.clone());

        // adjust configuration from the xml-overrides
        loadOverrides(xmlCoherence, new HashSet());

        // resolve the product edition; this will load licenses
        String sEditionDesc = resolveEdition(xmlLicense);

        // display the banner
        _trace("\n" + TITLE + " Version " + VERSION  + " Build " + getBuildNumber() +
               "\n " + sEditionDesc + ": " + sModeDescription + " mode" +
               "\n" + COPYRIGHT + "\n");

        logEnvironment();

        XmlElement xmlCluster    = xmlCoherence.ensureElement("cluster-config");
        XmlElement xmlLogging    = xmlCoherence.getSafeElement("logging-config");
        XmlElement xmlTracing    = xmlCoherence.getSafeElement("tracing-config");
        XmlElement xmlFactory    = xmlCoherence.getSafeElement("configurable-cache-factory-config");
        XmlElement xmlBuilder    = xmlCoherence.getSafeElement("cache-factory-builder-config");
        XmlElement xmlSecurity   = xmlCoherence.getSafeElement("security-config");
        XmlElement xmlManagement = xmlCoherence.getSafeElement("management-config");
        XmlElement xmlFederation = xmlCoherence.getSafeElement("federation-config");

        // copy license mode and edition name inside of the cluster config
        xmlCluster.ensureElement("edition-name").setString(xmlLicense.getSafeElement("edition-name").getString());
        xmlCluster.ensureElement("license-mode").setString(xmlLicense.getSafeElement("license-mode").getString());

        mapConfig.put("Cluster",              XmlHelper.mergeSchema((XmlElement) xmlCluster.clone(), xmlCoherence));
        mapConfig.put("$License",             XmlHelper.mergeSchema((XmlElement) xmlLicense.clone(), xmlCoherence));
        mapConfig.put("$Logger",              XmlHelper.mergeSchema((XmlElement) xmlLogging.clone(), xmlCoherence));
        mapConfig.put("$Tracing",             XmlHelper.mergeSchema((XmlElement) xmlTracing.clone(), xmlCoherence));
        mapConfig.put("$CacheFactory",        XmlHelper.mergeSchema((XmlElement) xmlFactory.clone(), xmlCoherence));
        mapConfig.put("$CacheFactoryBuilder", XmlHelper.mergeSchema((XmlElement) xmlBuilder.clone(), xmlCoherence));
        mapConfig.put("$Security",            XmlHelper.mergeSchema((XmlElement) xmlSecurity.clone(), xmlCoherence));
        mapConfig.put("$Management",          XmlHelper.mergeSchema((XmlElement) xmlManagement.clone(), xmlCoherence));
        mapConfig.put("$Federation",          XmlHelper.mergeSchema((XmlElement) xmlFederation.clone(), xmlCoherence));

        // service-specific parameters
        for (Iterator iter = xmlCoherence.getSafeElement("cluster-config/services")
                .getElements("service"); iter.hasNext(); )
            {
            XmlElement xmlSvc   = (XmlElement) iter.next();
            String     sSvcType = xmlSvc.getSafeElement("service-type").getString("service");

            // transform generic param structure into service-specific XML
            XmlElement xmlParams = new SimpleElement(sSvcType);
            XmlHelper.transformInitParams(xmlParams, xmlSvc.getSafeElement("init-params"));
            XmlHelper.mergeSchema(xmlParams, xmlCoherence);

            mapConfig.put(sSvcType, xmlParams);
            }
        }

    /**
     * Load the license info into the license-config element. This is once and
    * only once operation.
     */
    protected static void loadLicenses(com.tangosol.run.xml.XmlElement xml, String sLicenseFile)
        {
        // import com.tangosol.license.LicensedObject;
        // import com.tangosol.license.LicensedObject$LicenseData as com.tangosol.license.LicensedObject.LicenseData;
        // import com.tangosol.run.xml.SimpleParser;
        // import com.tangosol.run.xml.XmlDocument;
        // import com.tangosol.run.xml.XmlElement;
        // import com.tangosol.util.Base;
        // import com.tangosol.util.Binary;
        // import com.tangosol.util.UID;
        // import com.tangosol.util.WrapperException;
        // import java.io.ByteArrayOutputStream;
        // import java.io.DataOutputStream;
        // import java.io.File;
        // import java.io.FileInputStream;
        // import java.io.InputStream;
        // import java.io.IOException;
        // import java.nio.charset.StandardCharsets;
        // import java.net.URL;
        // import java.util.ArrayList;
        // import java.util.Iterator;
        // import java.util.List;
        // import java.security.cert.Certificate;
        // import java.security.cert.CertificateFactory;
        // import java.security.Signature;

        // load the license file
        InputStream stream = null;
        String      sUrl   = null;
        ClassLoader loader = get_CLASS().getClassLoader();
        if (loader == null)
            {
            loader = Base.getContextClassLoader();
            }

        URL url = loader.getResource(sLicenseFile);
        if (url != null)
            {
            try
                {
                stream = url.openStream();
                }
            catch (IOException ignored) {}
            }

        if (stream == null)
            {
            url = LicensedObject.class.getClassLoader().getResource(sLicenseFile);
            if (url != null)
                {
                try
                    {
                    stream = url.openStream();
                    }
                catch (IOException ignored) {}
                }
            }

        if (stream == null)
            {
            String sErrorMsg = "Edition file (" + sLicenseFile
                    + ") is missing from the " + TITLE + " libraries";
            _trace(sErrorMsg, 1);
            throw new RuntimeException(sErrorMsg);
            }
        else
            {
            sUrl = url.toString();
            }

        String sXml;
        try
            {
            sXml = new String(Base.read(stream), StandardCharsets.ISO_8859_1);
            }
        catch (IOException e)
            {
            throw new WrapperException(e,
                    "An exception occurred while reading the license data");
            }
        finally
            {
            try
                {
                stream.close();
                }
            catch (IOException ignored) {}
            }

        if (sXml == null || sXml.length() == 0)
            {
            String sErrorMsg = "Edition data (" + sUrl + ") is missing or empty.";
            _trace(sErrorMsg, 1);
            throw new RuntimeException(sErrorMsg);
            }
        else
            {
            _trace("Loaded edition data from \"" + sUrl + "\"", 6);
            }

        XmlDocument xmlLicenses;
        try
            {
            xmlLicenses = new SimpleParser().parseXml(sXml);
            }
        catch (IOException e)
            {
            throw new WrapperException(e,
                    "An exception occurred while parsing the license data");
            }

        // include all the license details in the $License pseudo-service config
        xml.ensureElement("license-list").getElementList()
                .addAll(xmlLicenses.getElementList());

        // this check could be moved all the way up if we did not need to copy
        // license details above
        if (isLicenseLoaded())
            {
            return;
            }
        setLicenseLoaded(true);

        // instantiate Signature for use in license validation
        Signature   signature;
        InputStream streamCert = null;
        try
            {
            streamCert = loader.getResourceAsStream(FILE_CFG_CERTIFICATE);
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            Certificate        cert    = factory.generateCertificate(streamCert);

            signature = Signature.getInstance("SHA1withDSA");
            signature.initVerify(cert.getPublicKey());
            }
        catch (Exception e)
            {
            throw Base.ensureRuntimeException(e, "Error during license validation");
            }
        finally
            {
            if (streamCert != null)
                {
                try
                    {
                    streamCert.close();
                    }
                catch (IOException ignored) {}
                }
            }

        List list = new ArrayList();
        for (Iterator iter = xmlLicenses.getElements("license"); iter.hasNext(); )
            {
            XmlElement xmlLicense = (XmlElement) iter.next();

            String sLicensee  = xmlLicense.getSafeElement("licensee").getString(null);
            String sAgreement = xmlLicense.getSafeElement("agreement").getString(null);
            String sMode      = xmlLicense.getSafeElement("type").getString(null);
            String sFromDate  = xmlLicense.getSafeElement("from-date").getString(null);
            String sToDate    = xmlLicense.getSafeElement("to-date").getString(null);
            String sRenewDate = xmlLicense.getSafeElement("maintenance-renewal-date").getString(null);
            int    cSeats     = xmlLicense.getSafeElement("max-seats").getInt();
            int    cUsers     = xmlLicense.getSafeElement("max-users").getInt();
            String sSite      = xmlLicense.getSafeElement("site").getString(null);
            int    cServers   = xmlLicense.getSafeElement("max-servers").getInt();
            int    cSockets   = xmlLicense.getSafeElement("max-sockets").getInt();
            int    cCores     = xmlLicense.getSafeElement("max-cpus").getInt();
            String sUid       = xmlLicense.getSafeElement("id").getString(null);
            String sKey       = xmlLicense.getSafeElement("key").getString(null);
            String sSig       = xmlLicense.getSafeElement("signature").getString(null);
            String sClass     = null;
            String sSoftware  = null;
            String sEdition   = null;

            if (sUid == null)
                {
                String sMsg = "You are using an out-of-date license format; "
                    + "please contact Oracle to obtain a replacement license.";
                _trace(sMsg, 1);
                throw new RuntimeException(sMsg);
                }

            // translate license id
            UID uid = new UID(sUid);

            // translate dates
            long lDateFrom  = sFromDate  == null ? 0L : parseDate(sFromDate);
            long lDateTo    = sToDate    == null ? 0L : parseDate(sToDate);
            long lDateRenew = sRenewDate == null ? 0L : parseDate(sRenewDate);

            // translate the mode
            int nMode = -1;
            if (sMode != null && sMode.length() > 0)
                {
                switch (sMode.charAt(0))
                    {
                    case 'e':
                        nMode = 0;
                        break;
                    case 'd':
                        nMode = 1;
                        break;
                    case 'p':
                        nMode = 2;
                        break;
                    }
                }
            if (nMode < 0)
                {
                String sErrorMsg = "Invalid license mode: \"" + sMode + "\"";
                _trace(sErrorMsg, 1);
                throw new RuntimeException(sErrorMsg);
                }

            // check for pre-v3.2 license
            if (sSig == null)
                {
                if (sKey == null)
                    {
                    // ignore an old license without a key
                    continue;
                    }
                // we'll check the uid later
                }
            else
                {
                // Hybrid, 3.2 or later license
                sSoftware = xmlLicense.getSafeElement("software").getString();

                try
                    {
                    // validate signature
                    StringBuffer sb = new StringBuffer();
                    sb.append(sSoftware)
                      .append(sLicensee)
                      .append(sAgreement == null ? "" : sAgreement)
                      .append(nMode)
                      .append(lDateFrom)
                      .append(lDateTo)
                      .append(lDateRenew)
                      .append(cSeats)
                      .append(cUsers)
                      .append(sSite)
                      .append(cServers)
                      .append(cSockets)
                      .append(cCores)
                      .append(uid);

                    ByteArrayOutputStream streamRaw = new ByteArrayOutputStream();
                    DataOutputStream      streamSig = new DataOutputStream(streamRaw);
                    streamSig.writeUTF(sb.toString());

                    signature.update(streamRaw.toByteArray());

                    if (!signature.verify(Base.parseHex(sSig)))
                        {
                        // invalid signature
                        String sMsg = "The " + sSoftware + " license signature "
                                    + "is not valid, please contact Oracle.";
                        System.err.println(sMsg);
                        _trace(sMsg, 1);
                        continue;
                        }
                    }
                catch (Exception e)
                    {
                    throw Base.ensureRuntimeException(e,
                        "Error validating license signature for " + sSoftware);
                    }

                if (sSoftware.endsWith(": Grid Edition"))
                    {
                    sEdition  = "GE";
                    sClass    = "com.tangosol.license.CoherenceDataGridEdition";
                    }
                else if (sSoftware.endsWith(": Enterprise Edition") ||
                         sSoftware.endsWith(": Application Edition"))
                    {
                    sEdition  = "EE";
                    sClass    = "com.tangosol.license.CoherenceApplicationEdition";
                    }
                else if (sSoftware.endsWith(": Community Edition"))
                    {
                    sEdition  = "CE";
                    sClass    = "com.tangosol.license.CoherenceCommunityEdition";
                    }
                else if (sSoftware.endsWith(": Standard Edition") ||
                         sSoftware.endsWith(": Caching Edition"))
                    {
                    sEdition  = "SE";
                    sClass    = "com.tangosol.license.CoherenceCachingEdition";
                    }
                else if (sSoftware.endsWith(": Compute Client"))
                    {
                    sEdition  = "CC";
                    sClass    = "com.tangosol.license.CoherenceComputeClient";
                    }
                else if (sSoftware.endsWith(": Real-Time Client"))
                    {
                    sEdition  = "RTC";
                    sClass    = "com.tangosol.license.CoherenceRealTimeClient";
                    }
                else if (sSoftware.endsWith(": Data Client"))
                    {
                    sEdition  = "DC";
                    sClass    = "com.tangosol.license.CoherenceDataClient";
                    }
                }

            // updgrade pre 3.2 license data
            if (sClass == null && sKey != null)
                {
                try
                    {
                    sClass = new Binary(Base.parseHex(sKey.substring(9, sKey.length() - 1)))
                            .getBufferInput().readUTF();
                    }
                catch (IOException e)
                    {
                    continue;
                    }

                if (sClass.endsWith(".CoherenceEnterprise"))
                    {
                    sSoftware = TITLE + ": Enterprise Edition";
                    sEdition  = "EE";
                    sClass    = "com.tangosol.license.CoherenceApplicationEdition";
                    }
                else if (sClass.endsWith(".Coherence"))
                    {
                    sSoftware = TITLE + ": Standard Edition";
                    sEdition  = "SE";
                    sClass    = "com.tangosol.license.CoherenceCachingEdition";
                    }
                else if (sClass.endsWith(".CoherenceLocal")
                      || sClass.endsWith(".ClientAccess"))
                    {
                    sSoftware = TITLE + ": Data Client";
                    sEdition  = "DC";
                    sClass    = "com.tangosol.license.CoherenceDataClient";
                    }
                else
                    {
                    continue;
                    }
                }

            // check for OEM information encoded in the licensee name
            boolean fOem = sLicensee.startsWith("OEM:");
            String  sApp = null;
            if (fOem)
                {
                sLicensee = sLicensee.substring(4);

                int ofColon = sLicensee.lastIndexOf(':');
                if (ofColon > 0)
                    {
                    sApp      = sLicensee.substring(ofColon + 1);
                    sLicensee = sLicensee.substring(0, ofColon);
                    }
                }

            list.add(new com.tangosol.license.LicensedObject.LicenseData(sSoftware, sEdition, sLicensee, fOem, sAgreement,
                    nMode, sClass, sSite, sApp, lDateFrom, lDateTo, lDateRenew, cSeats, cUsers,
                    cServers, cSockets, cCores, uid));
            }

        // configure the licenses
        LicensedObject.setLicenseData((com.tangosol.license.LicensedObject.LicenseData[]) list.toArray(new com.tangosol.license.LicensedObject.LicenseData[0]));
        }

    /**
     * If the specified XmlElement allows to override some values by the
    * xml-override elemenrs, overload the elements.
    *
    * @param xml the XmlElement to load overrides for
    * @param setOverrides the set of overrides previously loaded while reading
    * this element
     */
    protected static void loadOverrides(com.tangosol.run.xml.XmlElement xml, java.util.Set setOverrides)
        {
        // import com.tangosol.run.xml.SimpleParser;
        // import com.tangosol.run.xml.XmlElement;
        // import com.tangosol.run.xml.XmlValue;
        // import com.tangosol.run.xml.XmlHelper;
        // import com.tangosol.util.Base;
        // import com.tangosol.util.Resources;
        // import com.tangosol.util.WrapperException;
        // import java.io.IOException;
        // import java.io.File;
        // import java.net.URL;
        // import java.util.Iterator;
        // import java.util.HashSet;

        final String ATTR_OVERRIDE = "xml-override";
        final String ATTR_ID       = "id";

        XmlValue attr = xml.getAttribute(ATTR_OVERRIDE);
        if (attr != null)
            {
            // remove the attribute
            xml.setAttribute(ATTR_OVERRIDE, null);

            // find the element's override
            String sOverride = calculateAttribute(attr.getString());
            try
                {
                XmlElement  xmlOverride = null;

                // check for override as explicit file
                File file = new File(sOverride);
                if (file.exists() && file.isDirectory())
                    {
                    // if the sOverride specified a valid directory, check instead
                    // for a config file of the default name in that directory
                    sOverride = new File(file, FILE_CFG_COHERENCE_OVERRIDE).getPath();
                    }

                URL url = Resources.findFileOrResourceOrDefault(sOverride, get_CLASS().getClassLoader());
                if (url != null)
                    {
                    xmlOverride = XmlHelper.loadXml(url);
                    _trace("Loaded operational overrides from \"" + url + '"', 3);
                    }

                if (xmlOverride == null)
                    {
                    _trace("Optional configuration override \"" + sOverride
                        + "\" is not specified", 3);
                    }
                else
                    {
                    if (xmlOverride.getName().equals(xml.getName()))
                        {
                        // it's important to resolve system properties and
                        // load overrides recursivly BEFORE calling the
                        // overrideElement() which could be affected by the attributes
                        replaceSystemProperties(xmlOverride);

                        // make sure that there is no self-reference
                        XmlValue attrOverride = xmlOverride.getAttribute(ATTR_OVERRIDE);
                        if (attrOverride == null ||
                            setOverrides.add(attrOverride.getString()))
                            {
                            loadOverrides(xmlOverride, setOverrides);
                            XmlHelper.overrideElement(xml, xmlOverride, ATTR_ID);
                            }
                        else
                            {
                            throw new IOException("Document \"" + sOverride
                                + "\" is cyclically referenced by the '" + ATTR_OVERRIDE
                                + "' attribute of element '" + xml.getName() + "'");
                            }
                        }
                    else
                        {
                        throw new IOException("Root name mismatch in the document: "
                            + sOverride + "; " + "expected name: \"" + xml.getName() + '"');
                        }
                    }
                }
            catch (Exception e)
                {
                throw Base.ensureRuntimeException(e,
                    "Failed to apply the element override: "
         + sOverride);
                }
            }

        // do the same for each contained element
        for (Iterator iter = xml.getElementList().iterator(); iter.hasNext();)
            {
            loadOverrides((XmlElement) iter.next(), new HashSet(setOverrides));
            }
        }

    protected static void logEnvironment()
        {
        String sSpecName       = System.getProperty("java.specification.name");
        String sSpecVendor     = System.getProperty("java.specification.vendor");
        String sSpecVersion    = System.getProperty("java.specification.version");

        String sJavaVersion    = System.getProperty("java.version");
        String sVendorName     = System.getProperty("java.vendor");
        String sVendorVersion  = System.getProperty("java.vendor.version");
        String sVmVendorUrl    = System.getProperty("java.vendor.url");

        String sVmName         = System.getProperty("java.vm.name");
        String sVmVendor       = System.getProperty("java.vm.vendor");
        String sVmVersion      = System.getProperty("java.vm.version");

        String sRtName         = System.getProperty("java.runtime.name");
        String sRtVersion      = System.getProperty("java.runtime.version");

        String sJavaHome       = System.getProperty("java.home");

        String sOsName         = System.getProperty("os.name");
        String sOsVersion      = System.getProperty("os.version");
        String sOsArchitecture = System.getProperty("os.arch");

        StringBuilder sb = new StringBuilder("\n\n");

        sb.append("java.version: " + sJavaVersion).append('\n')
          .append("java.home: " + sJavaHome).append("\n\n")

          .append("Java Specification:").append('\n')
          .append("- java.specification.name: " + sSpecName).append('\n')
          .append("- java.specification.vendor: " + sSpecVendor).append('\n')
          .append("- java.specification.version: " + sSpecVersion).append('\n')

          .append("Java Vendor:").append('\n')
          .append("- java.vendor: " + sVendorName).append('\n')
          .append("- java.vendor.version: " + sVendorVersion).append('\n')
          .append("- java.vendor.url: " + sVmVendorUrl).append('\n')

          .append("Java Virtual Machine:").append('\n')
          .append("- java.vm.name: " + sVmName).append('\n')
          .append("- java.vm.vendor: " + sVmVendor).append('\n')
          .append("- java.vm.version: " + sVmVersion).append('\n')

          .append("Java Runtime Environment:").append('\n')
          .append("- java.runtime.name: " + sRtName).append('\n')
          .append("- java.runtime.version: " + sRtVersion).append('\n')

          .append("Operating System:").append('\n')
          .append("- os.name: " + sOsName).append('\n')
          .append("- os.version: " + sOsVersion).append('\n')
          .append("- os.arch: " + sOsArchitecture).append('\n');

        _trace(sb.toString(), 6);
        }

    // Declared at the super level
    /**
     * This method is the entry point for executable Java applications.
    *
    * Certain types of Java applications are started by the JVM invoking the
    * main() method of the entry point class.  The Application component
    * assists in building these types of applications by providing a default
    * implementation for the main() method.  Unfortunately, main() is not
    * virtual (it must be static) so an application must either override main()
    * or provide configuration information so that the default main()
    * implementation can determine the identity of the entry point class.  For
    * example, the following is a script that an application
    * (Component.Application.Console.HelloWorld) could use to ensure that the
    * HelloWorld application is instantiated:
    *
    *     // instantiate HelloWorld
    *     get_Instance();
    *     // use the default main() implementation provided by
    * Component.Application
    *     super.main(asArgs);
    *
    * To avoid creating the script on HelloWorld.main(), and if the application
    * were jar'd, the META-INF directory in the .jar file would contain the
    * following:
    *
    *     # -- contents of META-INF/MANIFEST.MF
    *     Main-Class:com.tangosol.coherence.Component.Application.Console.HelloWorld
    *
    *     # -- contents of META-INF/application.properties --
    *     app=Console.HelloWorld
    *
    * The application identity could alternatively be provided on the command
    * line, for example if the application has not been jar'd:
    *
    *     java com.tangosol.coherence.Component.Application.Console.HelloWorld
    * -app=Console.HelloWorld
    *
    * The default implementation (Application.main) stores the arguments for
    * later use in the indexed Argument property, instantiates the application
    * (if an instance does not already exist), and invokes the run() method of
    * the application instance.  It is expected that application implementors
    * will provide an implementation for run() and not for main().
    *
    * Note that "com.tangosol.coherence." is a place-holder for a deployer-specified package
    * name, for example "com.mycompany.myapplication".  The Packaging Wizard
    * allows the deployer to specify the package into which the application
    * will be deployed.  The above examples would have to be changed
    * accordingly.
    *
    * @param asArgs  an array of string arguments
    *
    * @see #get_Instance
     */
    public static void main(String[] asArgs)
        {
        setArgument(asArgs);

        ((Coherence) get_Instance()).run();
        }

    public static String[] parseArguments(String sArguments)
        {
        // import java.util.ArrayList;
        // import java.util.List;

        if (sArguments.length() == 0)
            {
            return new String[0];
            }

        char[]  ach     = sArguments.toCharArray();
        int     cch     = ach.length;
        boolean fEsc    = false;
        boolean fQuote  = false;
        char    chQuote = 0;
        List    list    = new ArrayList();
        StringBuffer sb = new StringBuffer();
        for (int ofCur = 0; ofCur < cch; ++ofCur)
            {
            char ch = ach[ofCur];

            if (fEsc)
                {
                switch (ch)
                    {
                    // escapables
                    case '\'':
                        sb.append('\'');
                        break;
                    case '\"':
                        sb.append('\"');
                        break;
                    case '\\':
                        sb.append('\\');
                        break;
                    case 'b':
                        sb.append('\b');
                        break;
                    case 'f':
                        sb.append('\f');
                        break;
                    case 'n':
                        sb.append('\n');
                        break;
                    case 'r':
                        sb.append('\r');
                        break;
                    case 't':
                        sb.append('\t');
                        break;

                    // special! allow space to be escaped (instead
                    // of quoting params)
                    case ' ':
                        sb.append(' ');
                        break;

                    default:
                        // oops ... it wasn't an escape
                        sb.append('\\')
                          .append(ch);
                        break;
                    }
                fEsc = false;
                }
            else
                {
                switch (ch)
                    {
                    case '\'':
                    case '\"':
                        if (fQuote)
                            {
                            if (ch == chQuote)
                                {
                                fQuote = false;
                                }
                            else
                                {
                                sb.append(ch);
                                }
                            }
                        else
                            {
                            fQuote  = true;
                            chQuote = ch;
                            }
                        break;

                    case ' ':
                        if (fQuote)
                            {
                            sb.append(' ');
                            }
                        else if (sb.length() > 0)
                            {
                            list.add(sb.toString());
                            sb.setLength(0);
                            }
                        break;

                    case '\\':
                        fEsc = true;
                        break;

                    default:
                        sb.append(ch);
                        break;
                    }
                }
            }

        if (fQuote)
            {
            throw new IllegalArgumentException("Unmatched quote ("
                + chQuote + ") in command: " + sArguments);
            }

        if (sb.length() > 0)
            {
            list.add(sb.toString());
            }

        return (String[]) list.toArray(new String[list.size()]);
        }

    public static long parseDate(String s)
        {
        // import com.tangosol.util.Base;
        // import java.util.Date;

        // this block, when it was missing, cost Cameron 10 hours of debugging
        if (s == null || s.length() == 0)
            {
            return 0L;
            }

        String[] asParts = Base.parseDelimitedString(s, '-');
        return Date.UTC(Integer.parseInt(asParts[0]) - 1900,
                        Integer.parseInt(asParts[1]) - 1,
                        Integer.parseInt(asParts[2]),
                        0, 0, 0);
        }

    /**
     * Parse the edition name.
    *
    * @return the corresponding edition index.
     */
    public static int parseEditionName(String sEdition)
        {
        if (sEdition != null)
            {
            String[] asEdition = EDITION_NAMES;
            for (int i = 0, c = asEdition.length; i < c; ++i)
                {
                if (sEdition.equals(asEdition[i]))
                    {
                    return i;
                    }
                }
            }
        _trace("Unknown edition: " + sEdition, 2);
        return 0; // Data Client a free product
        }

    /**
     * Parse the edition name.
    *
    * @return the corresponding edition index.
     */
    public static int parseModeName(String sMode)
        {
        if (sMode != null)
            {
            String[] asMode = MODE_NAMES;
            for (int i = 0, c = asMode.length; i < c; ++i)
                {
                if (sMode.equals(asMode[i]))
                    {
                    return i;
                    }
                }
            }
        _trace("Unknown mode: " + sMode, 2);
        return 1; // dev
        }

    public static void printException(String sPrefix, Throwable e)
        {
        // import com.tangosol.net.RequestTimeoutException;
        // import java.lang.reflect.InvocationTargetException;

        while (e instanceof InvocationTargetException)
            {
            e = ((InvocationTargetException) e).getTargetException();
            }

        if (sPrefix == null)
            {
            _trace(e);
            }
        else
            {
            _trace(sPrefix + getStackTrace(e), 1);
            }

        if (e instanceof RequestTimeoutException)
            {
            Object oPartial = ((RequestTimeoutException) e).getPartialResult();
            if (oPartial != null)
                {
                _trace("Partial result: " + oPartial);
                }
            }
        }

    public Object processCommand(String sCmd)
            throws java.lang.InterruptedException
        {
        // import Component.Net.Cluster;
        // import Component.Net.Member;
        // import Component.Net.MemberSet;
        // import Component.Util.Daemon.QueueProcessor.Service.Grid;
        // import Component.Util.Daemon.QueueProcessor.Service.Grid.ClusterService;
        // import Component.Util.SafeCluster;
        // import Component.Util.SafeService;
        // import com.oracle.coherence.common.base.Blocking;
        // import com.tangosol.coherence.dslquery.internal.PersistenceToolsHelper as com.tangosol.coherence.dslquery.internal.PersistenceToolsHelper;
        // import com.tangosol.coherence.dslquery.QueryPlus;
        // import com.tangosol.net.CacheFactory;
        // import com.tangosol.net.CacheService;
        // import com.tangosol.net.NamedCache;
        // import com.tangosol.net.Service as com.tangosol.net.Service;
        // import com.tangosol.net.ServiceInfo;
        // import com.tangosol.util.Base;
        // import com.tangosol.util.ClassHelper;
        // import com.tangosol.util.Filter;
        // import com.tangosol.util.PrimitiveSparseArray as com.tangosol.util.PrimitiveSparseArray;
        // import com.tangosol.util.Versionable;
        // import java.io.File;
        // import java.util.Arrays;
        // import java.util.Date;
        // import java.util.Enumeration;
        // import java.util.Iterator;
        // import java.util.Map;
        // import java.util.Set;

        com.tangosol.net.Service    service = getService();
        NamedCache map     = getMap();
        Object     oResult = null;
        String     sFunction;

        boolean fSilent = false;
        if (sCmd.charAt(0) == '@')
            {
            fSilent = true;
            sCmd    = sCmd.substring(1);
            }

        if (isScript())
            {
            doScript(sCmd, fSilent);
            return null;
            }

        int ofFunction = sCmd.indexOf(' ');
        if (ofFunction < 0)
            {
            sFunction = sCmd;
            sCmd      = "";
            }
        else
            {
            sFunction = sCmd.substring(0, ofFunction);
            sCmd      = sCmd.substring(ofFunction + 1).trim();
            }

        if (sFunction.startsWith("#")) // #N or ##N
            {
            String  sIter  = sFunction.substring(1);
            boolean fForce = false; // ignore exceptions
            if (sIter.startsWith("#"))
                {
                sIter  = sIter.substring(1);
                fForce = true;
                }

            try
                {
                int cIter = Integer.parseInt(sIter);
                doRepeat(sCmd, cIter, fForce);
                }
            catch (NumberFormatException e)
                {
                _trace("invalid counter: " + sIter);
                }
            return null;
            }

        String[] asParam = parseArguments(sCmd);
        int      cParams = asParam.length;
        Object[] aoParam = sFunction.startsWith("bulk")
                ? null                       // defer argument parsing
                : convertArguments(asParam);

        if (sFunction.startsWith("!")) // !N
            {
            if (sFunction.length() == 1)
                {
                doHistory(asParam, fSilent);
                }
            else
                {
                int nCmd = Integer.parseInt(sFunction.substring(1));
                doReissueCommand(nCmd, sCmd, fSilent);
                }
            return null;
            }
        else if (sFunction.startsWith("&"))
            {
            oResult = doFunction(sFunction, aoParam, fSilent);
            }
        else if (
            sFunction.equals("lock") ||
            sFunction.equals("unlock"))
            {
            if (isMapValid())
                {
                oResult = doFunction('&' + sFunction, aoParam, fSilent);
                }
            }
        else if (
            sFunction.equals("bye"))
            {
            CacheFactory.shutdown();
            setStop(true);
            }
        else if (
            sFunction.equals("echo"))
            {
            _trace(Arrays.deepToString(aoParam));
            return aoParam;
            }
        else if (
            sFunction.equals("help"))
            {
            _trace("The commands are:");
            _trace("  aggregate ('{' <key> [, <key>]* '}' | '{filter:'<filter-name>'}' | '*') <aggregator-name> [<extractor>]");
            _trace("  assert <value>");
            _trace("  backup <path>");
            _trace("  batch <path>");
            _trace("  bulkput <# of iterations> <block size> <start key> [<batch size> | all]");
            _trace("  bulkremove <# of iterations> <start key> [all]");
            _trace("  bye");
            _trace("  cachefactory [<path>]");
            _trace("  cache <name>");
            _trace("  cohql [<paramValue>]*");
            _trace("  clear");
            _trace("  destroy");
            _trace("  explain <filter name> [trace]");
            _trace("  filter <name> <type> [(<accessor> <value>) | [<paramValue>]+]");
            _trace("  get <key>");
            _trace("  hash");
            _trace("  help");
            _trace("  history ([<pattern>] [<limit>]) | (['on' | 'off' | 'clear'])");
            _trace("  inc <key> [<increment>]");
            _trace("  invoke <command> [('all' | 'other' | 'senior' | <id>) [('async' | 'sync')]]");
            _trace("  index <accessor> [add | remove]");
            _trace("  jmx [(<port> | <url>) [('start' | 'stop')]]");
            _trace("  kill");
            _trace("  list [<map name> | <filter name> [<accessor> [asc | desc]]]");
            _trace("  listen [('start' | 'stop') [('cluster' | 'interceptor' | 'local' | 'members' | 'trigger' | 'partition')]] [<filter> | <key> | <event-types>] [<lite>]");
            _trace("  lock <key>");
            _trace("  log (<size> | <message>) [<iterations> [<level>]]");
            _trace("  maps");
            _trace("  memory");
            _trace("  process (<key> | '{' <key> [, <key>]* '}' | '{filter:'<filter-name>'}' | '*') <processor-name> [<paramValue>]+");
            _trace("  profile <command>");
            _trace("  put <key> <value>");
            _trace("  release");
            _trace("  remove <key>");
            _trace("  restore <path>");
            _trace("  resume [all]");
            _trace("  runAs <name> <password> <command>");
            _trace("  server [<path-to-gar>] [<app-name>]");
            _trace("  service");
            _trace("  services");
            _trace("  size [<filter name>]");
            _trace("  sleep [interval]");
            _trace("  snapshot [create | archive | recover | remove] <snapshot-name>");
            _trace("  snapshot list [archived]");
            _trace("  snapshot [remove | retrieve] archived <snapshot-name>");
            _trace("  stats [cluster | service | p2p] [reset]");
            _trace("  suspend [all]");
            _trace("  truncate");
            _trace("  unlock <key>");
            _trace("  waitkey <start key> <stop key>");
            _trace("  who | cluster");
            _trace("  whoami | service");
            _trace("  worker <command>");
            _trace("  #<repeat count> <command>");
            _trace("  &<functionName> [<paramValue>]*");
            _trace("  !<history index>");
            }
        else if (
            sFunction.equals("aggregate"))
            {
            if (isMapValid())
                {
                doAggregate(aoParam, fSilent);
                }
            }
        else if (
            sFunction.equals("assert"))
            {
            if (cParams >= 1)
                {
                String sValue  = String.valueOf(aoParam[0]);
                String sResult = String.valueOf(((ThreadLocal) get_Sink()).get());
                if (!sValue.equals(sResult))
                    {
                    String sMsg = "Assertion failed: expected=\"" + sValue + "\"; actual=\"" + sResult + '"';
                    _trace(sMsg, 1);
                    oResult = new RuntimeException(sMsg);
                    }
                }
            }
        else if (
            sFunction.equals("backup"))
            {
            doBackup(asParam, fSilent);
            }
        else if (
            sFunction.equals("batch"))
            {
            doBatch(asParam, fSilent);
            }
        else if (
            sFunction.equals("bulkput"))
            {
            doBulkPut(asParam, fSilent);
            }
        else if (
            sFunction.equals("bulkremove"))
            {
            doBulkRemove(asParam, fSilent);
            }
        else if (
            sFunction.equals("cachefactory"))
            {
            doCacheFactory(asParam, fSilent);
            }
        else if (
            sFunction.equals("cache") ||
            sFunction.equals("map"))
            {
            if (cParams > 0)
                {
                doCache(asParam, fSilent);
                }
            else
                {
                _trace(String.valueOf(map));
                }
            }
        else if (
            sFunction.equals("cohql"))
            {
            QueryPlus.main(asParam);
            }
        else if (
            sFunction.equals("clear"))
            {
            if (isMapValid())
                {
                map.clear();
                }
            }
        else if (
            sFunction.equals("truncate"))
            {
            if (isMapValid())
                {
                map.truncate();
                }
            }
        else if (
            sFunction.equals("cluster") || sFunction.equals("who"))
            {
            _trace(String.valueOf(getSafeCluster()));
            }
        else if (
            sFunction.equals("connector")) // [listener | publisher] [unicast | multicast] [on | off | drop <rate> | pause <rate>]
            {
            doConnector(asParam, fSilent);
            }
        else if (
            sFunction.equals("destroy"))
            {
            if (isMapValid())
                {
                CacheFactory.destroyCache(map);
                }
            }
        else if (
            sFunction.equals("explain"))
            {
            if (isMapValid())
                {
                doExplain(asParam, fSilent);
                }
            }
        else if (
            sFunction.equals("filter"))
            {
            if (isMapValid())
                {
                doFilter(aoParam, fSilent);
                }
            }
        else if (
            sFunction.equals("filters"))
            {
            _trace("filters = " + getFilters());
            }
        else if (
            sFunction.equals("get"))
            {
            if (isMapValid() && cParams >= 1)
                {
                oResult = map.get(aoParam[0]);
                ((ThreadLocal) get_Sink()).set(oResult);

                if (!fSilent)
                    {
                    _trace(toString(oResult));
                    }
                }
            }
        else if (
            sFunction.equals("hash"))
            {
            if (!isMapValid())
                {
                return null;
                }
            int iHash = 0;
            for (Iterator iter = map.keySet().iterator(); iter.hasNext();)
                {
                Object oKey = iter.next();
                Object oVal = map.get(oKey);

                if (oVal instanceof byte[])
                    {
                    byte[] ab = (byte[]) oVal;
                    for (int i = 0, c = ab.length; i < c; i++)
                        {
                        iHash += ab[i];
                        }
                    }
                else if (oVal != null)
                    {
                    iHash += oVal.hashCode();
                    }
                }
            _trace("hash=" + iHash);
            }
        else if (
            sFunction.equals("history"))
            {
            doHistory(asParam, fSilent);
            }
        else if (
            sFunction.equals("inc")) // key [inc ["optimistic"]]
            {
            if (!isMapValid())
                {
                return null;
                }
            Object  oKey  = aoParam[0];
            int     cInc  = 1;
            boolean fLock;
            try
                {
                cInc = Integer.parseInt(asParam[1]);
                }
            catch (Exception ignored) {}
            fLock = cParams <= 2 || !asParam[2].startsWith("o");

            if (fLock)
                {
                map.lock(oKey, -1);
                }
            try
                {
                Object oVal = map.get(oKey);
                String sVal = fSilent ? null : String.valueOf(oVal);
                if (oVal instanceof Versionable)
                    {
                    Versionable ver = (Versionable) oVal;
                    for (int i = 0; i < cInc; i++)
                        {
                        ver.incrementVersion();
                        }
                    }
                else
                    {
                    int nVal = 0;
                    try
                        {
                        nVal = Integer.parseInt(String.valueOf(oVal));
                        }
                    catch (Exception ignored) {}

                    oVal = Integer.valueOf(nVal + cInc);
                    }

                map.put(oKey, oVal);
                if (!fSilent)
                    {
                    _trace("incremented " + oKey + " from " + sVal + " to " + oVal);
                    }
                ((ThreadLocal) get_Sink()).set(oVal);
                }
            finally
                {
                if (fLock)
                    {
                    map.unlock(oKey);
                    }
                }
            }
        else if (
            sFunction.equals("index")) // <accessor> [[add | remove] [filter]]");
            {
            if (!isMapValid())
                {
                return null;
                }
            doIndex(asParam, fSilent);
            }
        else if (
            sFunction.equals("iterate"))
            {
            int cItems = 0;
            for (Iterator iter = (Iterator) ((ThreadLocal) get_Sink()).get();
                    iter.hasNext(); cItems++)
                {
                Object oNext = iter.next();
                }
            if (!fSilent)
                {
                _trace(cItems + " items");
                }
            }
        else if (
            sFunction.startsWith("invoke")) // invoke[:<service name>] <command>
                                            //     ["all" | "other" | "senior" | <id>]
                                            //     [("async" | "sync")]
            {
            int    ofName = sFunction.indexOf(':');
            String sName  = ofName < 0 ? "InvocationService" : sFunction.substring(ofName + 1);

            doInvoke(sName, asParam, fSilent);
            }
        else if (
            sFunction.equals("jmx")) // [port] ["start" [<browser>]] | ["stop"]
            {
            doJmx(asParam);
            }
        else if (
            sFunction.equals("kill"))
            {
            if (cParams > 0)
                {
                if (asParam[0].equals("all"))
                    {
                    CacheFactory.shutdown();
                    }
                else
                    {
                    // kill a member
                    ClusterService svcCluster = getSafeCluster().getCluster().getClusterService();
                    MemberSet      setMember  = svcCluster.getClusterMemberSet();
                    Member         member     = setMember.getMember(Integer.parseInt(asParam[0]));

                    if (svcCluster.getThisMember().equals(member))
                        {
                        getSafeCluster().shutdown();
                        }
                    else
                        {
                        svcCluster.doMemberLeft(member);
                        return null;
                        }
                    }
                }
            else
                {
                if (service == null)
                    {
                    getSafeCluster().shutdown();
                    }
                else
                    {
                    service.shutdown();
                    }
                }
            setService(null);
            setMap(null);
            getLogger().setPrompt("?");
            }
        else if (
            sFunction.equals("script"))
            {
            doScript(sCmd, fSilent);
            }
        else if (
            sFunction.equals("snapshot") ||
            sFunction.equals("persistence"))
            {
            if (cParams == 0)
                {
                _trace("Please provide correct arguments to persistence/snapshot command");
                }
            else
                {
                doPersistence(asParam, fSilent);
                }
            }
        else if (
            sFunction.equals("suspend") || sFunction.equals("resume"))
            {
            ClusterService svcCluster = getSafeCluster().getCluster().getClusterService();
            boolean        fResume    = sFunction.equals("resume");
            String         sName      = service == null || (asParam.length > 0 && asParam[0].equals("all"))
                ? svcCluster.getServiceName()
                : ((Grid) ((SafeService) service).getService()).getServiceName();

            svcCluster.doServiceQueiscence(sName, fResume);
            }
        else if (
            sFunction.equals("list")) // <filter-name> [[[<method>[,<method>]*] desc] page]
            {
            if (isMapValid())
                {
                doList(asParam, fSilent);
                }
            }
        else if (
            sFunction.equals("listen")) // [start | stop] [members | master | trigger | local | partition | interceptor | <name>]
                                        // [filter | key | <event-types>] [lite] [version(s)]
            {
            boolean fStop      = cParams >= 1 && asParam[0].equals("stop");
            String  sSource    = cParams >= 2 ? asParam[1] : "global";
            Object  oKey       = null;
            Filter  filter     = null;
            boolean fLite      = false;
            Long    LVersion   = null;
            com.tangosol.util.PrimitiveSparseArray     laVersions = null;

            if (cParams >= 3)
                {
                filter = (Filter) getFilters().get(asParam[2]);
                if (filter == null)
                    {
                    oKey = aoParam[2];
                    }
                }
            if (cParams >= 4)
                {
                fLite = asParam[3].equals("lite");

                int iPos = fLite ? 4 : 3;

                LVersion   = aoParam[iPos] instanceof Long ? (Long) aoParam[iPos] : null;
                laVersions = aoParam[iPos] instanceof com.tangosol.util.PrimitiveSparseArray  ? (com.tangosol.util.PrimitiveSparseArray)  aoParam[iPos] : null;
                }
            doListen(sSource, fStop, filter, oKey, fLite, LVersion, laVersions);
            }
        else if (
            sFunction.equals("log"))
            {
            doLog(aoParam, fSilent);
            }
        else if (
            sFunction.equals("maps"))
            {
            for (Enumeration enumS = CacheFactory.ensureCluster().getServiceNames();
                    enumS.hasMoreElements();)
                {
                String  sName = (String) enumS.nextElement();
                com.tangosol.net.Service srv   = getSafeCluster().getService(sName);
                if (srv instanceof CacheService && srv.isRunning())
                    {
                    CacheService rc = (CacheService) srv;
                    for (Enumeration enumC = rc.getCacheNames(); enumC.hasMoreElements();)
                        {
                        _trace(srv.getInfo().getServiceName() + ":" + enumC.nextElement());
                        }
                    }
                }
            }
        else if (
            sFunction.equals("memory"))
            {
            Runtime rt = Runtime.getRuntime();
            rt.gc();

            long lTotal = rt.totalMemory();
            long lFree  = rt.freeMemory();
            _trace("total=" + lTotal/1000 + "K (" + lTotal + ")");
            _trace("free =" + lFree/1000  + "K (" + lFree + ")");
            try
                {
                Long LMax = (Long) ClassHelper.invoke(rt, "maxMemory", ClassHelper.VOID);
                _trace("max  =" + LMax.longValue()/1000  + "K (" + LMax + ")");
                }
            catch (Throwable ignored) {}
            oResult = Long.valueOf(lFree);
            }
        else if (
            sFunction.equals("new")) // new <class> [<params>]*
            {
            if (cParams > 0)
                {
                try
                    {
                    String sClass = asParam[0];
                    if (sClass.startsWith("Component"))
                        {
                        oResult = _newInstance(sClass);
                        }
                    else
                        {
                        Class clz = Class.forName(sClass);
                        if (cParams > 1)
                            {
                            Object[] ao = new Object[cParams - 1];
                            System.arraycopy(aoParam, 1, ao, 0, cParams - 1);
                            oResult = ClassHelper.newInstance(clz, ao);
                            }
                        else
                            {
                            oResult = clz.newInstance();
                            }
                        }
                    }
                catch (Exception e)
                    {
                    printException(null, e);
                    }
                }
            if (!fSilent)
                {
                _trace(toString(oResult));
                }
            ((ThreadLocal) get_Sink()).set(oResult);
            }
        else if (
            sFunction.startsWith("process"))
            {
            if (isMapValid())
                {
                doProcess(aoParam, sFunction.equals("processAsync"), fSilent);
                }
            }
        else if (
            sFunction.equals("profile"))
            {
            long lBegin = Base.getSafeTimeMillis();
            processCommand(sCmd);
            long lElapsed = Base.getSafeTimeMillis() - lBegin;
            _trace("Elapsed " + lElapsed + "ms");
            }
        else if (
            sFunction.equals("put"))
            {
            if (isMapValid() && cParams >= 2)
                {
                oResult = map.put(aoParam[0], aoParam[1]);
                if (!fSilent)
                    {
                    _trace(toString(oResult));
                    }
                }
            }
        else if (
            sFunction.equals("release"))
            {
            if (isMapValid())
                {
                CacheFactory.releaseCache(map);
                }
            }
        else if (
            sFunction.equals("remove"))
            {
            if (isMapValid() && cParams >= 1)
                {
                oResult = map.remove(aoParam[0]);
                ((ThreadLocal) get_Sink()).set(oResult);
                if (!fSilent)
                    {
                    _trace(toString(oResult));
                    }
                }
            }
        else if (
            sFunction.equals("restore"))
            {
            doRestore(asParam, fSilent);
            }
        else if (
            sFunction.equalsIgnoreCase("runAs"))
            {
            oResult = doSecure(asParam, fSilent);
            }
        else if (
            sFunction.equals("scan"))
            {
            if (isMapValid())
                {
                doScan(asParam);
                }
            }
        else if (
            sFunction.equals("server"))
            {
            doServer(asParam);
            }
        else if (
            sFunction.equals("service") || sFunction.equals("whoami"))
            {
            if (service == null)
                {
                processCommand("who");
                }
            else
                {
                _trace(String.valueOf(service));
                _trace(String.valueOf(service.getInfo().getServiceMembers()));
                }
            }
        else if (
            sFunction.equals("services"))
            {
            for (Enumeration e = CacheFactory.ensureCluster().getServiceNames();
                    e.hasMoreElements();)
                {
                String      sName = (String) e.nextElement();
                ServiceInfo info  = getSafeCluster().getServiceInfo(sName);
                if (info != null)
                    {
                    _trace(info.toString());
                    }
                }
            }
        else if (
            sFunction.equals("sleep")) // sleep [-]<interval> [<key>]
            {
            long lMillis = 1L;
            try
                {
                lMillis = Long.parseLong(asParam[0]);
                }
            catch (NumberFormatException ignored) {}

            Object oKey = null;
            if (cParams > 1)
                {
                if (!isMapValid())
                    {
                    return null;
                    }
                oKey = aoParam[1];
                }

            try
                {
                long lStart = Base.getSafeTimeMillis();
                long lStop;
                if (oKey != null)
                    {
                    map.lock(oKey, -1L);
                    }

                if (lMillis > 0)
                    {
                    Blocking.sleep(lMillis);
                    lStop = Base.getSafeTimeMillis();
                    }
                else
                    {
                    do
                        {
                        for (int i = 0; i < 100; i++) {}
                        Thread.yield();
                        lStop = Base.getSafeTimeMillis();
                        }
                    while (lStop < lStart - lMillis);
                    }
                if (!fSilent)
                    {
                    _trace("Elapsed " + (lStop - lStart) + "ms");
                    }
                }
            catch (InterruptedException e)
                {
                _trace("Sleep was interrupted; re-setting the interrupt flag...", 3);
                Thread.currentThread().interrupt();
                if (!fSilent)
                    {
                    throw Base.ensureRuntimeException(e);
                    }
                }
            finally
                {
                if (oKey != null)
                    {
                    map.unlock(oKey);
                    }
                }
            }
        else if (sFunction.equals("size")) // [[<filter-name>] page]
            {
            if (!isMapValid())
                {
                return null;
                }
            Set setEntry;
            if (cParams > 0)
                {
                String sName = asParam[0];
                if (getFilters().containsKey(sName))
                    {
                    int nPage = -1;
                    if (cParams > 1)
                        {
                        nPage = Integer.parseInt(asParam[1]);
                        }
                    setEntry = applyFilter(sName, true, null, nPage);
                    }
                else
                    {
                    map = CacheFactory.getCache(sName);
                    setEntry = map.entrySet();
                    }
                }
            else
                {
                setEntry = map.entrySet();
                }
            int cSize = setEntry.size();
            if (!fSilent)
                {
                _trace(String.valueOf(cSize));
                }
            ((ThreadLocal) get_Sink()).set(setEntry);
            }
        else if (
            sFunction.equals("stats")) // [cluster | service | p2p] [reset]
            {
            boolean fCluster = true;
            boolean fP2p     = false;
            boolean fReset   = false;
            int     iNext    = 0;
            if (cParams > iNext && asParam[iNext].equals("service"))
                {
                fCluster = false;
                iNext++;
                }
            else if (cParams > iNext && asParam[iNext].equals("p2p"))
                {
                fCluster = false;
                fP2p     = true;
                iNext++;
                }

            if (cParams > iNext && asParam[iNext].equals("reset"))
                {
                fReset = true;
                }

            String sSilent = fSilent ? "@" : "";
            if (fCluster)
                {
                String sTarget = getMap() == null ? "&" : "&getCacheService.getService.";
                if (fReset)
                    {
                    processCommand('@' + sTarget + "getCluster.resetStats");
                    }
                oResult = processCommand(sSilent + sTarget + "getCluster.formatStats");
                }
            else if (fP2p)
                {
                String sTarget = getMap() == null ? "&" : "&getCacheService.getService.";
                if (fReset)
                    {
                    processCommand('@' + sTarget + "getCluster.resetPointToPointStats");
                    }
                oResult = processCommand(sSilent + sTarget + "getCluster.formatPointToPointStats");
                }
            else if (isMapValid())
                {
                if (fReset)
                    {
                    processCommand("@&getCacheService.getService.resetStats");
                    }
                oResult = processCommand(sSilent + "&getCacheService.getService.formatStats [true]");
                }
            }
        else if (
            sFunction.equals("sum"))
            {
            if (!isMapValid())
                {
                return null;
                }
            long cSum  = 0;
            int  cNums = 0;
            int  cNots = 0;
            for (Iterator iter = map.values().iterator(); iter.hasNext();)
                {
                Object oVal = iter.next();
                if (oVal instanceof Number)
                    {
                    ++cNums;
                    cSum += ((Number) oVal).longValue();
                    }
                else if (oVal instanceof Versionable)
                    {
                    Object oVer = ((Versionable) oVal).getVersionIndicator();
                    if (oVer instanceof Number)
                        {
                        ++cNums;
                        cSum += ((Number) oVer).longValue();
                        }
                    else
                        {
                        ++cNots;
                        }
                    }
                else
                    {
                    ++cNots;
                    }
                }
            if (!fSilent)
                {
                _trace("sum=" + cSum + " (" + cNums + " values were numbers, "
                        + cNots + " were not)");
                }
            ((ThreadLocal) get_Sink()).set(oResult = Long.valueOf(cSum));
            }
        else if (
            sFunction.equals("waitkey"))
            {
            if (!isMapValid())
                {
                return null;
                }
            Object oKeyStart = aoParam[0];
            Object oKeyStop  = aoParam[1];

            _trace("waiting for key: " + oKeyStart);
            while (!map.containsKey(oKeyStart))
                {
                Blocking.sleep(10);
                }

            _trace("waiting for key: " + oKeyStop);
            long lBegin = Base.getSafeTimeMillis();
            while (!map.containsKey(oKeyStop))
                {
                Blocking.sleep(10);
                }
            long lElapsed = Base.getSafeTimeMillis() - lBegin;
            _trace(new Date() + ": done (" + lElapsed + "ms)");
            }
        else if (
            sFunction.equals("worker")) // [<number> | <command> | "wait"]
            {
            String  sName  = null;
            Coherence.Worker worker = null;

            try
                {
                sName = "Worker!" + Integer.parseInt(asParam[0]);
                worker = (Coherence.Worker) _findChild(sName);
                if (worker == null)
                    {
                    _trace(sName + " has been terminated.");
                    return null;
                    }
                else if (!fSilent)
                    {
                    _trace(worker.toString());
                    }
                }
            catch (RuntimeException ignored) {}

            if (worker == null)
                {
                boolean fWait = cParams == 1 && asParam[0].equals("wait");

                for (int i = 0; i < 1000; i++)
                    {
                    sName  = "Worker!" + i;
                    worker = (Coherence.Worker) _findChild(sName);
                    if (worker == null)
                        {
                        if (cParams > 0 && !fWait)
                            {
                            break;
                            }
                        }
                    else if (fWait)
                        {
                        worker.getThread().join();
                        }
                    else if (cParams == 0)
                        {
                        _trace(worker.toString());
                        }
                    }

                if (!fWait && cParams > 0)
                    {
                    worker = new Coherence.Worker();
                    _addChild(worker, sName);
                    worker.setThreadName(sName);
                    worker.getQueue().add(sCmd);
                    worker.start();
                    }
                }
            ((ThreadLocal) get_Sink()).set(
                worker == null ? (Object) Coherence.Worker.getWorkerGroup() : worker);
            }
        else if (
            sFunction.equals("begin")  ||
            sFunction.equals("commit") ||
            sFunction.equals("rollback"))
            {
            if (isMapValid())
                {
                doTransaction(sFunction, asParam);
                }
            }
        else if (sFunction.equals("pause"))
            {
            SafeCluster cluster = (SafeCluster) getSafeCluster();
            Member      member;
            for (member = (Member) cluster.getLocalMember();
                 member.getId() == 0;
                 member = (Member) cluster.getLocalMember())
                {
                _trace("pausing input; waiting to join cluster");
                Blocking.sleep(1000);
                }

            File file = new File("member" + member.getId());
            _trace("pausing input; create file " + file + " to continue");
            while (!file.exists())
                {
                Blocking.sleep(1000);
                }
            file.delete();
            }
        else
            {
            _trace("Unknown command: \"" + sFunction + '"' +
                "\nPrint \"help\" for command list");
            }

        return oResult;
        }

    public static Object processFunction(Object target, String sFunction, boolean fSilent, Object[] aoParam)
        {
        // import com.tangosol.util.ClassHelper;
        // import com.tangosol.util.ConcurrentMap;
        // import java.lang.reflect.Array;
        // import java.util.Iterator;
        // import java.util.Enumeration;

        boolean fLast = false;
        try
            {
            do
                {
                String sMethod = sFunction;
                int ofNext = sFunction.indexOf('.');
                if (ofNext == -1)
                    {
                    ofNext = sFunction.length();
                    fLast  = true;
                    }
                else
                    {
                    sMethod   = sFunction.substring(0, ofNext);
                    sFunction = sFunction.substring(ofNext + 1);
                    }

                int nIx  = -1;
                int ofIx = sMethod.indexOf('[');
                if (ofIx != -1)
                    {
                    nIx = Integer.parseInt(
                        sMethod.substring(ofIx + 1, sMethod.indexOf(']')));
                    sMethod = sMethod.substring(0, ofIx);
                    }

                if (sMethod.endsWith("lock"))
                    {
                    if (aoParam.length > 0 && "*".equals(aoParam[0]))
                        {
                        aoParam[0] = ConcurrentMap.LOCK_ALL;
                        }
                    }

                boolean fTryStatic   = target instanceof Class;
                boolean fTryInstance = !fTryStatic;
                if (fTryStatic)
                    {
                    try
                        {
                        // _trace("1) Calling " + ((Class) target).getName() + "#" + sMethod +
                        //    "(" + (fLast ? toString(aoParam) : "") + ")");
                        target = ClassHelper.invokeStatic((Class) target, sMethod,
                            fLast ? aoParam : ClassHelper.VOID);
                        }
                    catch (NoSuchMethodException e)
                        {
                        fTryInstance = true;
                        }
                    }

                if (fTryInstance)
                    {
                    // _trace("2) Calling " + target.getClass().getName() + "#" + sMethod +
                    //    "(" + (fLast ? toString(aoParam) : "") + ")");
                    target = ClassHelper.invoke(target, sMethod, fLast ? aoParam : ClassHelper.VOID);
                    }

                if (nIx >= 0)
                    {
                    target = Array.get(target, nIx);
                    }

                if (fLast)
                    {
                    if (!fSilent)
                        {
                        _trace(toString(target));
                        }
                    }
                else if (target == null)
                    {
                    if (!fSilent)
                        {
                        _trace("Exception: " + sMethod + " returned null");
                        }
                    break;
                    }
                }
            while (!fLast);
            }
        catch (Throwable e)
            {
            target = e;
            printException(null, e);
            }
        return target;
        }

    protected String readLine(Object oReader, Object oHistory)
            throws java.lang.Exception
        {
        // import com.tangosol.util.ClassHelper;
        // import java.io.Reader;

        if (oReader instanceof Reader)
            {
            char[] ach = new char[256];
            int    cch = ((Reader) oReader).read(ach);
            return cch <= 1 ? "" : new String(ach).trim();
            }
        else
            {
            String sLine = (String) ClassHelper.invoke(oReader, "readLine", null);
            ClassHelper.invoke(oReader, "redrawLine", null);
            ClassHelper.invoke(oReader, "flush", null);
            return sLine;
            }
        }

    /**
     * Record a command in the history.
     */
    protected void recordCommand(String sCmd)
            throws java.lang.InterruptedException
        {
        // import java.util.List;

        List list = getCommandHistory();

        if (list == null ||
            sCmd.startsWith("history") || sCmd.startsWith("!") ||
            (!list.isEmpty() && list.get(list.size() - 1).equals(sCmd)))
            {
            // don't record history related, or repetitive commands
            return;
            }

        list.add(sCmd);
        }

    protected static void replaceSystemProperties(com.tangosol.run.xml.XmlElement xml)
        {
        // import com.tangosol.run.xml.XmlHelper;

        XmlHelper.replaceSystemProperties(xml, "system-property");
        }

    /**
     * Process the operation configuration and return the edition's description
     */
    public static String resolveEdition(com.tangosol.run.xml.XmlElement xmlLicense)
        {
        // import com.tangosol.license.LicensedObject;
        // import com.tangosol.license.LicensedObject$LicenseData as com.tangosol.license.LicensedObject.LicenseData;
        // import com.tangosol.run.xml.XmlElement;
        // import com.tangosol.util.Base;

        // validate edition (if any)
        String sEdition = xmlLicense.getSafeElement("edition-name").getString(DEFAULT_EDITION);
        String sDescription;
        int    nEdition = 0;
        String sLicenseFile;

        // validate and normalize edition:
        // GE  / grid       - grid edition
        // EE  / enterprise - enterprise edition
        // CE  / community  - community edition
        // SE  / standard   - standard edition
        // RTC / realtime   - real-time client
        // DC  / client     - data client
        if (sEdition.equalsIgnoreCase("ge") || sEdition.equalsIgnoreCase("grid") ||
            sEdition.equalsIgnoreCase("dge"))
            {
            sLicenseFile = "coherence-grid.xml";
            sEdition     = "GE";
            sDescription = "Grid Edition";
            nEdition     = 5;
            }
        else if (sEdition.equalsIgnoreCase("ee") || sEdition.equalsIgnoreCase("enterprise") ||
                 sEdition.equalsIgnoreCase("ae"))
            {
            sLicenseFile = "coherence-enterprise.xml";
            sEdition     = "EE";
            sDescription = "Enterprise Edition";
            nEdition     = 4;
            }
        else if (sEdition.equalsIgnoreCase("ce") || sEdition.equalsIgnoreCase("community"))
            {
            sLicenseFile = "coherence-community.xml";
            sEdition     = "CE";
            sDescription = "Community Edition";
            nEdition     = 3;
            }
        else if (sEdition.equalsIgnoreCase("se") || sEdition.equalsIgnoreCase("standard"))
            {
            sLicenseFile = "coherence-standard.xml";
            sEdition     = "SE";
            sDescription = "Standard Edition";
            nEdition     = 2;
            }
        else if (sEdition.equalsIgnoreCase("rtc") || sEdition.equalsIgnoreCase("realtime") ||
                 sEdition.equalsIgnoreCase("cc")  || sEdition.equalsIgnoreCase("compute"))
            {
            // since Coherence 3.3.1, CC feature set are moved to RTC
            sLicenseFile = "coherence-rtc.xml";
            sEdition     = "RTC";
            sDescription = "Real-Time Client";
            nEdition     = 1;
            }
        else if (sEdition.equalsIgnoreCase("dc") || sEdition.equalsIgnoreCase("client"))
            {
            sLicenseFile = "coherence-client.xml";
            sEdition     = "DC";
            sDescription = "Data Client";
            nEdition     = 0;
            }
        else
            {
            String sErrorMsg = "Invalid \"edition-name\" specified: \"" + sEdition
                 + "\" (valid editions are GE/EE/SE/RTC/DC)";
            _trace(sErrorMsg, 1);
            throw new RuntimeException(sErrorMsg);
            }

        loadLicenses(xmlLicense, sLicenseFile);

        // note that at this point the licenses are not verified; they could easily
        // have been tampered with. the point of this phase is to load all the
        // license information, assuming the best. at some point later, the license
        // data will be verified, and using the product features will only be
        // possible once that has occurred.

        // get the set of licenses
        com.tangosol.license.LicensedObject.LicenseData[] aLicense = LicensedObject.getLicenseData();
        _assert(aLicense != null);

        // remember the edition name
        Coherence app = (Coherence) get_Instance();
        app.setEdition(nEdition);
        app.setProduct(TITLE + ' ' + sEdition);

        xmlLicense.ensureElement("edition-name").setString(sEdition);

        return sDescription;
        }

    /**
     * Select the operational "mode", and mode's description.
     */
    protected static String resolveMode(com.tangosol.run.xml.XmlElement xmlLicense)
        {
        int    nMode = 0;
        String sMode = xmlLicense.getSafeElement("license-mode").getString(DEFAULT_MODE);
        String sDescription;

        switch (sMode.length() > 0 ? sMode.charAt(0) : '?')
            {
            case 'e': case 'E':
                nMode = 0;
                sDescription = "Evaluation";
                break;

            case 'd': case 'D':
                nMode = 1;
                sDescription = "Development";
                break;

            case 'p': case 'P':
                nMode = 2;
                sDescription = "Production";
                break;

            default:
                {
                String sErrorMsg = "Invalid \"mode\" specified: \"" + sMode
                     + "\" (valid modes are eval/dev/prod)";
                _trace(sErrorMsg, 1);
                throw new RuntimeException(sErrorMsg);
                }
            }
        sMode = MODE_NAMES[nMode];

        // remember the mode
        ((Coherence) get_Instance()).setMode(nMode);
        xmlLicense.ensureElement("license-mode").setString(sMode);

        return sDescription;
        }

    // Declared at the super level
    public void run()
        {
        // import com.oracle.coherence.common.base.Blocking;
        // import com.tangosol.net.CacheFactory;
        // import java.io.IOException;
        // import java.lang.reflect.InvocationTargetException;

        super.run();

        String   sCmd  = "";
        String[] asArg = getArgument();
        if (asArg.length > 0)
            {
            if (asArg[0].startsWith("@"))
                {
                sCmd = asArg[0].substring(1);
                }
            else
                {
                sCmd = "cache " + asArg[0];
                }
            for (int i = 1, c = asArg.length; i < c; i++)
                {
                sCmd += ' ' + asArg[i];
                }
            CacheFactory.getCluster();
            }
        else
            {
            CacheFactory.ensureCluster();
            }

        Object oReader = createReader();
        Object oHistory = getHistory(oReader);

        ensureRunningLogger().setPrompt("?");
        while (true)
            {
            if (sCmd.length() > 0)
                {
                try
                    {
                    recordCommand(sCmd);
                    processCommand(sCmd);
                    }
                catch (Throwable e)
                    {
                    printException(null, e);
                    }
                }

            if (isStop())
                {
                setStop(false);
                return;
                }

            try
                {
                Coherence.Logger logger = getLogger();

                Blocking.sleep(50);
                logger.setCommandPrompt(true);

                sCmd = readLine(oReader, oHistory);

                logger.setCommandPrompt(false);
                logger.setPendingLineFeed(false);
                }
            catch (IOException e)
                {
                System.out.println("Shutting down due to " + e);
                CacheFactory.shutdown();
                setStop(true);
                return;
                }
            catch (InvocationTargetException e )
                {
                System.out.println("Shutting down due to " + e);
                CacheFactory.shutdown();
                setStop(true);
                return;
                }
            catch (Exception e)
                {
                System.out.println("resetting reader due to " + e);
                oReader = createReader();
                sCmd    = "";
                ensureRunningLogger();
                }
            }
        }

    // Accessor for the property "BuildNumber"
    /**
     * Setter for property BuildNumber.<p>
    * Build number is initialized in _initStatic(); therefore, it cannot be
    * designed.
     */
    protected static void setBuildNumber(String s)
        {
        __s_BuildNumber = s;
        }

    // Accessor for the property "Cluster"
    /**
     * Setter for property Cluster.<p>
    * The Cluster instance.
     */
    protected static void setCluster(com.tangosol.coherence.component.util.SafeCluster cluster)
        {
        __s_Cluster = cluster;
        }

    // Accessor for the property "CommandHistory"
    /**
     * Setter for property CommandHistory.<p>
    * History of recently executed commands
     */
    protected void setCommandHistory(java.util.List list)
        {
        __m_CommandHistory = list;
        }

    // Accessor for the property "ConfigurationLoaded"
    /**
     * Setter for property ConfigurationLoaded.<p>
    * Flag to indicate that the configuration has been loaded
    * @volatile
     */
    protected static void setConfigurationLoaded(boolean fLoaded)
        {
        __s_ConfigurationLoaded = fLoaded;
        }

    // Accessor for the property "Edition"
    /**
     * Setter for property Edition.<p>
    * The Edition is the product type.
    *
    * 0=Data Client (DC)
    * 1=Real-Time Client (RTC)
    * 2=Compute Client (CC)
    * 3=Standard Edition (SE)
    * 4=Enterprise Edition (EE)
    * 5=Grid Edition (GE)
     */
    private void setEdition(int nEdition)
        {
        __m_Edition = nEdition;
        }

    // Accessor for the property "Filters"
    /**
     * Setter for property Filters.<p>
    * Filters keyed by their names. Used in filter and list commands.
     */
    protected void setFilters(java.util.Map map)
        {
        __m_Filters = map;
        }

    // Accessor for the property "LicenseLoaded"
    /**
     * Setter for property LicenseLoaded.<p>
     */
    private static void setLicenseLoaded(boolean fLoaded)
        {
        if (fLoaded)
            {
            __s_LicenseLoaded = (fLoaded);
            }
        }

    // Accessor for the property "Logger"
    /**
     * Setter for property Logger.<p>
    * The low priority logging daemon.
     */
    public void setLogger(Coherence.Logger logger)
        {
        // import java.util.concurrent.atomic.AtomicReference;

        AtomicReference refLogger  = getLoggerRef();
        Coherence.Logger         loggerPrev;

        while (true)
            {
            loggerPrev = (Coherence.Logger) refLogger.get();

            if (loggerPrev == logger)
                {
                return;
                }

            if (refLogger.compareAndSet(loggerPrev, logger))
                {
                break;
                }
            }

        if (loggerPrev != null)
            {
            loggerPrev.shutdown();
            }
        }

    // Accessor for the property "LoggerRef"
    /**
     * Setter for property LoggerRef.<p>
    * The logger reference.
     */
    protected void setLoggerRef(java.util.concurrent.atomic.AtomicReference logger)
        {
        __m_LoggerRef = logger;
        }

    /**
     * See CacheFactory#setLoggingLevel.
     */
    public static void setLoggingLevel(Integer ILevel)
        {
        Coherence app    = (Coherence) Coherence.get_Instance();
        Coherence.Logger logger = app.getLogger();

        logger.setLevel(ILevel);
        }

    // Accessor for the property "Map"
    /**
     * Setter for property Map.<p>
    * Map assosiated with currently tested service
     */
    protected void setMap(com.tangosol.net.NamedCache map)
        {
        __m_Map = map;
        }

    // Accessor for the property "Mode"
    /**
     * Setter for property Mode.<p>
    * The Mode is the "license type", i.e. evaluation, development or
    * production use.
    *
    * 0=evaluation
    * 1=development
    * 2=production
     */
    private void setMode(int nMode)
        {
        __m_Mode = nMode;
        }

    // Accessor for the property "PersistenceToolsHelper"
    /**
     * Setter for property PersistenceToolsHelper.<p>
    * Holds a help instance for issuing persistence commands.
     */
    public void setPersistenceToolsHelper(com.tangosol.coherence.dslquery.internal.PersistenceToolsHelper helperTools)
        {
        __m_PersistenceToolsHelper = helperTools;
        }

    // Accessor for the property "Product"
    /**
     * Setter for property Product.<p>
    * The product name.
     */
    protected void setProduct(String sProduct)
        {
        _assert(getProduct() == null || getProduct().equals(sProduct));
        __m_Product = (sProduct);

        // reset the log parameters
        getLogger().setLogParameters(null);
        }

    // Accessor for the property "Script"
    /**
     * Setter for property Script.<p>
    * Inidcates that the command line tool is in the "scripting" mode.
     */
    protected void setScript(boolean fScript)
        {
        __m_Script = fScript;
        }

    // Accessor for the property "ScriptEngine"
    /**
     * Setter for property ScriptEngine.<p>
    * The script engine (nashorn);
     */
    protected void setScriptEngine(javax.script.ScriptEngine engine)
        {
        __m_ScriptEngine = engine;
        }

    // Accessor for the property "Service"
    /**
     * Setter for property Service.<p>
    * Service that is currently being tested
     */
    protected void setService(com.tangosol.net.CacheService service)
        {
        __m_Service = service;
        }

    /**
     * See CacheFactory#setServiceConfig.
     */
    public static void setServiceConfig(String sServiceType, com.tangosol.run.xml.XmlElement xmlCfg)
        {
        // import Component.Net.Management.Gateway;
        // import Component.Util.SafeCluster;
        // import com.tangosol.internal.net.management.LegacyXmlGatewayHelper;
        // import com.tangosol.internal.net.management.DefaultGatewayDependencies;

        if (getServiceConfig(sServiceType) == null)
            {
            throw new IllegalArgumentException("Unknown service type: " + sServiceType);
            }

        getServiceConfigMap().put(sServiceType, xmlCfg.clone());

        // Logger configuration is an exception from a general rule;
        // it's allowed to be reconfigured on-the-fly
        if ("$Logger".equals(sServiceType))
            {
            Coherence app    = (Coherence) Coherence.get_Instance();
            Coherence.Logger logger = app.getLogger();

            if (logger.isStarted())
                {
                app.setLogger(null);
                }
            }

        // Gateway needs to be re-created
        if ("$Management".equals(sServiceType))
            {
            SafeCluster cluster = getSafeCluster();
            if (!cluster.isRunning())
                {
                Gateway mgmt = Gateway.createGateway(
                    LegacyXmlGatewayHelper.fromXml(xmlCfg, new DefaultGatewayDependencies()),  cluster);

                cluster.setManagement(mgmt);
                }
            }
        }

    // Accessor for the property "ServiceConfigMap"
    /**
     * Setter for property ServiceConfigMap.<p>
    * Map containing the service configuration element per service type
    * (including pseudo-types like $Logger, etc.).
    *
    * @see #loadConfiguration
     */
    protected static void setServiceConfigMap(java.util.Map map)
        {
        __s_ServiceConfigMap = map;
        }

    // Accessor for the property "Stop"
    /**
     * Setter for property Stop.<p>
    * Specifies whether to stop the command tool.
     */
    protected void setStop(boolean fStop)
        {
        __m_Stop = fStop;
        }

    // Accessor for the property "TloCluster"
    /**
     * Setter for property TloCluster.<p>
    * The thread local object to avoid an infinite recursion.
     */
    private static void setTloCluster(ThreadLocal tloCluster)
        {
        __s_TloCluster = tloCluster;
        }

    /**
     * Shutdown the cluster as well as any associated Coherence state.
     */
    public static void shutdown()
        {
        // import Component.Util.SafeCluster;
        // import java.util.Map;

        SafeCluster cluster;

        synchronized (get_CLASS())
            {
            cluster = getCluster();
            }

        try
            {
            if (cluster != null)
                {
                // shutdown and dispose the cluster itself
                cluster.dispose();
                }
            }
        finally
            {
            // finally, clean up Coherence component state associated with the
            // (previously) running cluster.  This is handled in separate
            // synchronization blocks to avoid holding synchronization on both
            // the Coherence class and cluster object simultaneously.
            synchronized (get_CLASS())
                {
                // check that the cluster hasn't been concurrently restarted
                if (getCluster() == cluster)
                    {
                    // clear the singleton Cluster
                    setCluster(null);

                    // clear the service config map
                    getServiceConfigMap().clear();
                    setConfigurationLoaded(false);

                    // shutdown the logger
                    ((Coherence) get_Instance()).setLogger(null);
                    }
                }
            }
        }

    public static String toString(Object oResult)
        {
        // import com.tangosol.util.Base;
        // import java.lang.reflect.Array;
        // import java.util.Iterator;
        // import java.util.Enumeration;

        final int    MAX_TRACE = 50;
        final String BEGIN     = " {\n ";
        final String NEXT      = "\n ";
        final String END       = "\n }";

        Class clzArrayType = oResult == null ? null : oResult.getClass().getComponentType();
        if (clzArrayType == null)
            {
            if (oResult instanceof Iterator)
                {
                StringBuffer sb = new StringBuffer();
                sb.append(oResult.getClass().getName());

                Iterator iter = (Iterator) oResult;
                if (iter.hasNext())
                    {
                    sb.append(BEGIN);

                    for (int i = 0; iter.hasNext() && i < MAX_TRACE; i++)
                        {
                        sb.append(toString(iter.next()));
                        if (iter.hasNext())
                            {
                            sb.append(NEXT);
                            }
                        }
                    if (iter.hasNext())
                        {
                        sb.append("...");
                        }
                    sb.append(END);
                    }
                return sb.toString();
                }
            else if (oResult instanceof Enumeration)
                {
                StringBuffer sb = new StringBuffer();
                sb.append(oResult.getClass().getName())
                  .append(BEGIN);
                Enumeration e = (Enumeration) oResult;
                for (int i = 0 ; e.hasMoreElements() && i < MAX_TRACE; i++)
                    {
                    sb.append(toString(e.nextElement()));
                    if (e.hasMoreElements())
                        {
                        sb.append(NEXT);
                        }
                    }
                if (e.hasMoreElements())
                    {
                    sb.append("...");
                    }
                sb.append(END);
                return sb.toString();
                }
            else if (oResult instanceof Class)
                {
                return Base.toString((Class) oResult);
                }
            else
                {
                return String.valueOf(oResult);
                }
            }
        else
            {
            int c = Array.getLength(oResult);
            StringBuffer sb = new StringBuffer();

            sb.append(clzArrayType.getName())
              .append('[')
              .append(c)
              .append(']');

            if (c > 0)
                {
                sb.append(BEGIN);

                int cTrace = Math.min(c, MAX_TRACE);
                for (int i = 0; i < cTrace; i++)
                    {
                    if (i > 0)
                        {
                        sb.append(NEXT);
                        }
                    sb.append(toString(Array.get(oResult, i)));
                    }

                if (c > cTrace)
                    {
                    sb.append(", ...");
                    }
                sb.append(END);
                }
            return sb.toString();
            }
        }

    protected void txEnd()
        {
        // import com.tangosol.net.NamedCache;
        // import com.tangosol.util.TransactionMap;

        NamedCache map = getMap();
        if (map instanceof TransactionMap)
            {
            NamedCache cache = (NamedCache) ((TransactionMap) map).getBaseMap();
            setMap(cache);

            Coherence.Logger logger = getLogger();
            logger.setPrompt(logger.getPrompt().substring(3));
            }
        }

    protected void txStart(int nConcur, int nIsolation, int nTimeout)
        {
        // import com.tangosol.net.CacheFactory;
        // import com.tangosol.net.NamedCache;
        // import com.tangosol.run.jca.SimpleValidator;
        // import com.tangosol.util.TransactionMap;

        NamedCache     map = getMap();
        TransactionMap mapTx;
        if (map instanceof TransactionMap)
            {
            mapTx = (TransactionMap) map;
            }
        else
            {
            mapTx = CacheFactory.getLocalTransaction(map);

            mapTx.setConcurrency(nConcur);
            mapTx.setTransactionIsolation(nIsolation);
            mapTx.setTransactionTimeout(nTimeout);
            mapTx.setValidator(new SimpleValidator());

            setMap((NamedCache) mapTx);

            Coherence.Logger logger = getLogger();
            logger.setPrompt("Tx-" + logger.getPrompt());
            }

        mapTx.begin();
        }

    /**
     * Verify the environment requirements that Coherence may have.
     */
    protected static void validateEnvironment()
        {
        // COH-4167: test for broken java/util/concurrent/atomic/AtomicLong
        //           on sparc-solaris Hotspot client VM (Sun bug 7009231)
        String sVMName    = System.getProperty("java.vm.name", "");
        String sOSName    = System.getProperty("os.name", "");
        String sOSArch    = System.getProperty("os.arch", "");
        String sDataModel = System.getProperty("sun.arch.data.model", "");
        if (sOSName.contains("SunOS") &&
            sOSArch.contains("sparc") &&
            sVMName.contains("HotSpot") &&
            sVMName.contains("Client VM") &&
            sDataModel.equals("32"))
            {
            String sMsg = "Coherence requires the 32-bit Hotspot JVM to be run in "
                 + "server mode on Solaris-sparc. Include the '-server' option on "
                 + "the command line to run the server JVM.";

            _trace(sMsg, 1);
            throw new RuntimeException(sMsg);
            }
        }

    // ---- class: com.tangosol.coherence.component.application.console.Coherence$CacheItem

    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class CacheItem
            extends    com.tangosol.coherence.component.Data
            implements com.tangosol.net.Invocable,
                       com.tangosol.net.PriorityTask,
                       com.tangosol.util.Versionable
        {
        // ---- Fields declarations ----

        /**
         * Property ExecutionTimeoutMillis
         *
         */
        private long __m_ExecutionTimeoutMillis;

        /**
         * Property Index
         *
         */
        private int __m_Index;

        /**
         * Property InvokeCommand
         *
         * Used only for Invocation requests.
         */
        private String __m_InvokeCommand;

        /**
         * Property Local
         *
         * Transient value!
         */
        private transient boolean __m_Local;

        /**
         * Property Origin
         *
         */
        private int __m_Origin;

        /**
         * Property Result
         *
         * Invocation result
         */
        private transient Object __m_Result;

        /**
         * Property SchedulingPriority
         *
         */
        private int __m_SchedulingPriority;

        /**
         * Property Service
         *
         * Invocation service
         */
        private transient com.tangosol.net.InvocationService __m_Service;

        /**
         * Property VersionIndicator
         *
         */
        private Comparable __m_VersionIndicator;

        // Default constructor
        public CacheItem()
            {
            this(null, null, true);
            }

        // Initializing constructor
        public CacheItem(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
            {
            super(sName, compParent, false);

            if (fInit)
                {
                __init();
                }
            }

        // Main initializer
        public void __init()
            {
            // private initialization
            __initPrivate();

            // state initialization: public and protected properties
            try
                {
                setLocal(true);
                }
            catch (java.lang.Exception e)
                {
                // re-throw as a runtime exception
                throw new com.tangosol.util.WrapperException(e);
                }

            // signal the end of the initialization
            set_Constructed(true);
            }

        // Private initializer
        protected void __initPrivate()
            {

            super.__initPrivate();
            }

        //++ getter for static property _Instance
        /**
         * Getter for property _Instance.<p>
        * Auto generated
         */
        public static com.tangosol.coherence.Component get_Instance()
            {
            return new com.tangosol.coherence.component.application.console.Coherence.CacheItem();
            }

        //++ getter for static property _CLASS
        /**
         * Getter for property _CLASS.<p>
        * Property with auto-generated accessor that returns the Class object
        * for a given component.
         */
        public static Class get_CLASS()
            {
            Class clz;
            try
                {
                clz = Class.forName("com.tangosol.coherence/component/application/console/Coherence$CacheItem".replace('/', '.'));
                }
            catch (ClassNotFoundException e)
                {
                throw new NoClassDefFoundError(e.getMessage());
                }
            return clz;
            }

        //++ getter for autogen property _Module
        /**
         * This is an auto-generated method that returns the global [design
        * time] parent component.
        *
        * Note: the class generator will ignore any custom implementation for
        * this behavior.
         */
        private com.tangosol.coherence.Component get_Module()
            {
            return this.get_Parent();
            }

        // Declared at the super level
        /**
         * Compares this object with the specified object for order.  Returns a
        * negative integer, zero, or a positive integer as this object is less
        * than, equal to, or greater than the specified object.
        *
        * @param o  the Object to be compared.
        * @return  a negative integer, zero, or a positive integer as this
        * object is less than, equal to, or greater than the specified object.
        *
        * @throws ClassCastException if the specified object's type prevents it
        * from being compared to this Object.
         */
        public int compareTo(Object o)
            {
            Coherence.CacheItem that = (Coherence.CacheItem) o;

            return Integer.compare(this.getIndex(), that.getIndex());
            }

        // Declared at the super level
        public boolean equals(Object obj)
            {
            if (obj instanceof Coherence.CacheItem)
                {
                Coherence.CacheItem that = (Coherence.CacheItem) obj;
                return this.getIndex()  == that.getIndex() &&
                       this.getOrigin() == that.getOrigin();
                }
            return false;
            }

        // From interface: com.tangosol.net.PriorityTask
        // Accessor for the property "ExecutionTimeoutMillis"
        /**
         * Getter for property ExecutionTimeoutMillis.<p>
         */
        public long getExecutionTimeoutMillis()
            {
            return __m_ExecutionTimeoutMillis;
            }

        // Accessor for the property "Index"
        /**
         * Getter for property Index.<p>
         */
        public int getIndex()
            {
            return __m_Index;
            }

        // Accessor for the property "InvokeCommand"
        /**
         * Getter for property InvokeCommand.<p>
        * Used only for Invocation requests.
         */
        public String getInvokeCommand()
            {
            return __m_InvokeCommand;
            }

        // Accessor for the property "Origin"
        /**
         * Getter for property Origin.<p>
         */
        public int getOrigin()
            {
            return __m_Origin;
            }

        // From interface: com.tangosol.net.PriorityTask
        public long getRequestTimeoutMillis()
            {
            return 0L;
            }

        // From interface: com.tangosol.net.Invocable
        // Accessor for the property "Result"
        /**
         * Getter for property Result.<p>
        * Invocation result
         */
        public Object getResult()
            {
            return __m_Result;
            }

        // From interface: com.tangosol.net.PriorityTask
        // Accessor for the property "SchedulingPriority"
        /**
         * Getter for property SchedulingPriority.<p>
         */
        public int getSchedulingPriority()
            {
            return __m_SchedulingPriority;
            }

        // Accessor for the property "Service"
        /**
         * Getter for property Service.<p>
        * Invocation service
         */
        public com.tangosol.net.InvocationService getService()
            {
            return __m_Service;
            }

        // From interface: com.tangosol.util.Versionable
        // Accessor for the property "VersionIndicator"
        /**
         * Getter for property VersionIndicator.<p>
         */
        public synchronized Comparable getVersionIndicator()
            {
            Comparable version = __m_VersionIndicator;
            if (version == null)
                {
                version = Long.valueOf(0L);
                setVersionIndicator(version);
                }
            return version;
            }

        // Declared at the super level
        public int hashCode()
            {
            return getIndex() * getOrigin();
            }

        // From interface: com.tangosol.util.Versionable
        public synchronized void incrementVersion()
            {
            setVersionIndicator(
                Long.valueOf(((Long) getVersionIndicator()).longValue() + 1L));
            }

        // From interface: com.tangosol.net.Invocable
        public void init(com.tangosol.net.InvocationService service)
            {
            setService(service);
            }

        // Accessor for the property "Local"
        /**
         * Getter for property Local.<p>
        * Transient value!
         */
        public boolean isLocal()
            {
            return __m_Local;
            }

        /**
         * This method is a part of "Serializable" pseudo-interface.
         */
        private void readObject(java.io.ObjectInputStream in)
                throws java.io.IOException,
                       java.lang.ClassNotFoundException
            {
            // import java.io.IOException;

            in.defaultReadObject();

            if ("ReadException".equals(getInvokeCommand()))
                {
                throw new IOException("Test exception");
                }
            }

        // From interface: com.tangosol.net.Invocable
        public void run()
            {
            // import com.tangosol.util.Base;
            // import java.io.Serializable;

            String sCommand = getInvokeCommand();
            if ("exception".equals(sCommand))
                {
                throw new RuntimeException("Test exception");
                }

            sCommand = Base.replace(sCommand, "{target}",
                String.valueOf(getService().getCluster().getLocalMember().getId()));
            sCommand = Base.replace(sCommand, "{context}",
                String.valueOf(getService().getUserContext()));

            try
                {
                Coherence app     = (Coherence) Coherence.get_Instance();
                Object  oResult = app.processCommand(sCommand);
                setResult(
                    oResult == null || oResult instanceof Serializable ?
                        oResult : oResult.getClass().getName());
                }
            catch (Throwable e)
                {
                setResult(e);
                }
            }

        // From interface: com.tangosol.net.PriorityTask
        public void runCanceled(boolean fAbandoned)
            {
            }

        // Accessor for the property "ExecutionTimeoutMillis"
        /**
         * Setter for property ExecutionTimeoutMillis.<p>
         */
        public void setExecutionTimeoutMillis(long cMillis)
            {
            __m_ExecutionTimeoutMillis = cMillis;
            }

        // Accessor for the property "Index"
        /**
         * Setter for property Index.<p>
         */
        public void setIndex(int i)
            {
            __m_Index = i;
            }

        // Accessor for the property "InvokeCommand"
        /**
         * Setter for property InvokeCommand.<p>
        * Used only for Invocation requests.
         */
        public void setInvokeCommand(String oKey)
            {
            __m_InvokeCommand = oKey;
            }

        // Accessor for the property "Local"
        /**
         * Setter for property Local.<p>
        * Transient value!
         */
        public void setLocal(boolean fLocal)
            {
            __m_Local = fLocal;
            }

        // Accessor for the property "Origin"
        /**
         * Setter for property Origin.<p>
         */
        public void setOrigin(int i)
            {
            __m_Origin = i;
            }

        // Accessor for the property "Result"
        /**
         * Setter for property Result.<p>
        * Invocation result
         */
        protected void setResult(Object oResult)
            {
            __m_Result = oResult;
            }

        // Accessor for the property "SchedulingPriority"
        /**
         * Setter for property SchedulingPriority.<p>
         */
        public void setSchedulingPriority(int iPriority)
            {
            __m_SchedulingPriority = iPriority;
            }

        // Accessor for the property "Service"
        /**
         * Setter for property Service.<p>
        * Invocation service
         */
        protected void setService(com.tangosol.net.InvocationService service)
            {
            __m_Service = service;
            }

        // Accessor for the property "VersionIndicator"
        /**
         * Setter for property VersionIndicator.<p>
         */
        public void setVersionIndicator(Comparable lVersion)
            {
            __m_VersionIndicator = lVersion;
            }

        // Declared at the super level
        public String toString()
            {
            return "CacheItem{Index=" + getIndex() +
                   ", Origin="  + getOrigin() +
                   ", Local="   + isLocal()   +
                   ", Version=" + getVersionIndicator() +
                   '}';
            }

        /**
         * This method is a part of "Serializable" pseudo-interface.
         */
        private void writeObject(java.io.ObjectOutputStream out)
                throws java.io.IOException
            {
            // import java.io.IOException;

            if ("WriteException".equals(getInvokeCommand()))
                {
                throw new IOException("Test exception");
                }
            out.defaultWriteObject();
            }
        }

    // ---- class: com.tangosol.coherence.component.application.console.Coherence$Logger

    /**
     * A Logger component is used to asynchronously log messages for a specific
     * system or application component.
     *
     * Each Logger instance has an associated logging level. Only log messages
     * that meet or exceed this level are logged. Currently, the Logger defines
     * 10 logging levels (from highest to lowest level):
     *
     * LEVEL_INTERNAL (All messages without a log level)
     * LEVEL_ERROR      (Error messages)
     * LEVEL_WARNING (Warning messages)
     * LEVEL_INFO          (Informational messages)
     * LEVEL_D4             (Debug messages)
     * LEVEL_D5
     * LEVEL_D6
     * LEVEL_D7
     * LEVEL_D8
     * LEVEL_D9
     *
     * Additionally, the Logger defines two "psuedo" levels that instruct the
     * Logger to either log all messages or to not log any messages:
     *
     * LEVEL_ALL
     * LEVEL_NONE
     *
     * Log messages are logged using the log() method. There are several
     * versions of the log() method that allow both string messages and
     * Throwable stack traces to be logged. The Logger uses a string template
     * to format the log message before it is logged using the underlying
     * logging mechanism. The template may contain the following
     * parameterizable strings:
     *
     * {date}     -the date and time that the message was logged
     * {level}    -the level of the log message
     * {thread} -the thread that logged the message
     * {text}     -the text of the message
     *
     * Subclasses of the Logger are free to define additional parameters.
     *
     * The Logger component uses a LogOutput component to log messages to an
     * underlying logging mechanism, such as stdout, stderr, a file, a JDK
     * Logger, or a Log4j Logger. See configure() method for additional detail.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class Logger
            extends    com.tangosol.coherence.component.util.daemon.queueProcessor.Logger
        {
        // ---- Fields declarations ----

        /**
         * Property CommandPrompt
         *
         * Notify the Logger that a command prompt should be output
         */
        private boolean __m_CommandPrompt;

        /**
         * Property Configured
         *
         * Used to prevent infinite recursion during startup.
         */
        private boolean __m_Configured;

        /**
         * Property DMSActiveContextMethod
         *
         * A cached reference to the static method
         * oracle.dms.context.DMSContextManager#getActiveContext.
         */
        private java.lang.reflect.Method __m_DMSActiveContextMethod;

        /**
         * Property DMSErrored
         *
         * Flag to indicate an error has been thrown by a DMS method thus
         * diagnosibility is disabled.
         * @volatile
         */
        private volatile boolean __m_DMSErrored;

        /**
         * Property HashCode
         *
         * Pseudo hash code that contains the license and CPU information.
         */
        private transient int __m_HashCode;

        /**
         * Property LastPromptTimeMillis
         *
         * The last time (in milliseconds) that a command prompt was output by
         * the Logger.
         */
        private long __m_LastPromptTimeMillis;

        /**
         * Property LogParameters
         *
         * A cached array of logger parameters.
         */
        private transient Object[] __m_LogParameters;

        /**
         * Property ODLRecordConstructor
         *
         * A cached reference to the constructor
         * oracle.core.ojdl.logging.ODLLogRecord<init>(Ljava/util/logging/Level;
         * Ljava/lang/String;).
         */
        private java.lang.reflect.Constructor __m_ODLRecordConstructor;

        /**
         * Property ODLRecordGetCtxMethod
         *
         * A cached reference to the method
         * oracle.core.ojdl.logging.ODLLogRecord#getLoggingContext().
         */
        private java.lang.reflect.Method __m_ODLRecordGetCtxMethod;

        /**
         * Property ODLRecordInitMethod
         *
         * A cached reference to the method
         * oracle.core.ojdl.logging.ODLLogRecord#initLoggingContext().
         */
        private java.lang.reflect.Method __m_ODLRecordInitMethod;

        /**
         * Property PendingLineFeed
         *
         * True to indicate that a line feed should be output by the Logger (if
         * necessary).
         */
        private boolean __m_PendingLineFeed;

        /**
         * Property PendingPrompt
         *
         * True to indicate that a prompt should be output by the Logger (if
         * necessary).
         */
        private boolean __m_PendingPrompt;

        /**
         * Property Prompt
         *
         * The prompt that should be output by the Logger.
         */
        private String __m_Prompt;
        private static com.tangosol.util.ListMap __mapChildren;

        // Static initializer
        static
            {
            __initStatic();
            }

        // Default static initializer
        private static void __initStatic()
            {
            // register child classes
            __mapChildren = new com.tangosol.util.ListMap();
            __mapChildren.put("Queue", Coherence.Logger.Queue.get_CLASS());
            }

        // Default constructor
        public Logger()
            {
            this(null, null, true);
            }

        // Initializing constructor
        public Logger(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
            {
            super(sName, compParent, false);

            if (fInit)
                {
                __init();
                }
            }

        // Main initializer
        public void __init()
            {
            // private initialization
            __initPrivate();

            // state initialization: public and protected properties
            try
                {
                setDaemonState(0);
                setDefaultGuardRecovery(0.9F);
                setDefaultGuardTimeout(60000L);
                setFormat("{date} Oracle Coherence {version} <{level}> (thread={thread}): {text}");
                setLevel(10);
                setLimit(65536);
                setNotifier(new com.oracle.coherence.common.base.SingleWaiterMultiNotifier());
                setOverflowed(false);
                String[] a0 = new String[8];
                    {
                    a0[0] = "{logRecord}";
                    a0[1] = "{uptime}";
                    a0[2] = "{thread}";
                    a0[3] = "{member}";
                    a0[4] = "{role}";
                    a0[5] = "{location}";
                    a0[6] = "{product}";
                    a0[7] = "{version}";
                    }
                setParameters(a0);
                setPriority(3);
                setWaitMillis(5000L);
                }
            catch (java.lang.Exception e)
                {
                // re-throw as a runtime exception
                throw new com.tangosol.util.WrapperException(e);
                }

            // containment initialization: children
            _addChild(new com.tangosol.coherence.component.util.Daemon.Guard("Guard", this, true), "Guard");
            _addChild(new Coherence.Logger.ShutdownHook("ShutdownHook", this, true), "ShutdownHook");

            // signal the end of the initialization
            set_Constructed(true);
            }

        // Private initializer
        protected void __initPrivate()
            {

            super.__initPrivate();
            }

        // Getter for virtual constant DefaultWaitMillis
        public long getDefaultWaitMillis()
            {
            return 500L;
            }

        //++ getter for static property _Instance
        /**
         * Getter for property _Instance.<p>
        * Auto generated
         */
        public static com.tangosol.coherence.Component get_Instance()
            {
            return new com.tangosol.coherence.component.application.console.Coherence.Logger();
            }

        //++ getter for static property _CLASS
        /**
         * Getter for property _CLASS.<p>
        * Property with auto-generated accessor that returns the Class object
        * for a given component.
         */
        public static Class get_CLASS()
            {
            Class clz;
            try
                {
                clz = Class.forName("com.tangosol.coherence/component/application/console/Coherence$Logger".replace('/', '.'));
                }
            catch (ClassNotFoundException e)
                {
                throw new NoClassDefFoundError(e.getMessage());
                }
            return clz;
            }

        //++ getter for autogen property _Module
        /**
         * This is an auto-generated method that returns the global [design
        * time] parent component.
        *
        * Note: the class generator will ignore any custom implementation for
        * this behavior.
         */
        private com.tangosol.coherence.Component get_Module()
            {
            return this.get_Parent();
            }

        //++ getter for autogen property _ChildClasses
        /**
         * This is an auto-generated method that returns the map of design time
        * [static] children.
        *
        * Note: the class generator will ignore any custom implementation for
        * this behavior.
         */
        protected java.util.Map get_ChildClasses()
            {
            return __mapChildren;
            }

        // Declared at the super level
        /**
         * Determine whether diagnosability logging (ODL/DMS) can be performed.
         */
        public boolean checkDiagnosability()
            {
            // import java.lang.reflect.Method;

            try
                {
                Method methDMSActiveCtx = getDMSActiveContextMethod();
                return methDMSActiveCtx != null && !isDMSErrored() && methDMSActiveCtx.invoke(null, (Object[]) null) != null;
                }
            catch(Exception e)
                {
                setDMSErrored(true);
                System.err.println("Oracle Diagnostic Logging was discovered but accessing expected methods was unsuccessful.");
                e.printStackTrace();
                }
            return false;
            }

        // Declared at the super level
        /**
         * Collects an array of parameters to be used to format the log message.
         */
        protected Object[] collectLogParameters()
            {
            // import Component.Net.Member;
            // import com.tangosol.net.Cluster;
            // import com.tangosol.util.Base;

            Object[] aoParam = getLogParameters();
            try
                {
                Coherence app        = (Coherence) get_Module();
                Cluster cluster    = app.getCluster();
                Member  member     = cluster == null || !cluster.isRunning()
                                        ? null : (Member) cluster.getLocalMember();
                int     nMemberNew = member  == null ? 0    : member.getId();
                int     nMemberOld = aoParam == null ? -1   : ((Integer) aoParam[0]).intValue();

                if (nMemberNew != nMemberOld)
                    {
                    if (member == null)
                        {
                        aoParam = new Object[]
                            {Base.makeInteger(0), null, null, app.getProduct()};
                        }
                    else
                        {
                        aoParam = new Object[]
                            {Base.makeInteger(nMemberNew), member.getRoleName(),
                             member.getLocationInfo(), app.getProduct()};
                        }
                    setLogParameters(aoParam);
                    }
                }
            catch (Throwable ignored) {}

            return aoParam;
            }

        // Declared at the super level
        /**
         * Format the given log message by substituting parameter names in the
        * message format string with the values represented by the specified
        * LogRecord.
         */
        protected String formatLogRecord(String sFormat, java.util.logging.LogRecord logRecord)
            {
            // import com.tangosol.util.Base;
            // import com.tangosol.util.ClassHelper;
            // import java.lang.reflect.Method;

            String sMessage = super.formatLogRecord(sFormat, logRecord);

            // check to see if we have an ODLRecord (which could hold an EC)
            String sECID         = null;
            Method methODLGetCtx = getODLRecordGetCtxMethod();
            if (methODLGetCtx != null && logRecord.getClass() == methODLGetCtx.getDeclaringClass())
                {
                try
                    {
                    Object oCtx = methODLGetCtx.invoke(logRecord, (Object[]) null);
                    if (oCtx != null)
                        {
                        sECID = (String) ClassHelper.invoke(oCtx, "getECID", null);
                        }
                    }
                catch (Exception ignored) {}
                }

            sMessage = Base.replace(sMessage, "{ecid}", formatParameter("{ecid}", sECID));

            return sMessage;
            }

        // Declared at the super level
        /**
         * Format the given parameter with the given name for output to the
        * underlying logger.
         */
        protected String formatParameter(String sParamName, Object oParamValue)
            {
            if (sParamName != null && sParamName.length() > 3)
                {
                switch (sParamName.charAt(1))
                    {
                    case 'e':
                        if (sParamName.equals("{ecid}"))
                            {
                            return oParamValue == null ? "n/a" : (String) oParamValue;
                            }
                        break;

                    case 'm':
                        if (sParamName.equals("{member}"))
                            {
                            int nMember = oParamValue instanceof Integer ?
                                ((Integer) oParamValue).intValue() : 0;
                            return nMember == 0 ? "n/a" : String.valueOf(nMember);
                            }
                        break;

                    case 'r':
                    case 'l':
                        if (sParamName.equals("{role}") ||
                            sParamName.equals("{location}"))
                            {
                            return oParamValue == null ? "" : (String) oParamValue;
                            }
                        break;

                    case 'p':
                        if (sParamName.equals("{product}"))
                            {
                            String sProduct = (String) oParamValue;
                            return sProduct == null ? Coherence.TITLE : sProduct;
                            }
                        break;

                    case 'v':
                        if (sParamName.equals("{version}"))
                            {
                            return Coherence.VERSION;
                            }
                        break;
                    }
                }

            return super.formatParameter(sParamName, oParamValue);
            }

        // Accessor for the property "DMSActiveContextMethod"
        /**
         * Getter for property DMSActiveContextMethod.<p>
        * A cached reference to the static method
        * oracle.dms.context.DMSContextManager#getActiveContext.
         */
        protected java.lang.reflect.Method getDMSActiveContextMethod()
            {
            return __m_DMSActiveContextMethod;
            }

        // Accessor for the property "HashCode"
        /**
         * Getter for property HashCode.<p>
        * Pseudo hash code that contains the license and CPU information.
         */
        public int getHashCode()
            {
            int nHash = __m_HashCode;
            return nHash == 0 ? System.identityHashCode(get_Module()) : nHash;
            }

        // Accessor for the property "LastPromptTimeMillis"
        /**
         * Getter for property LastPromptTimeMillis.<p>
        * The last time (in milliseconds) that a command prompt was output by
        * the Logger.
         */
        protected long getLastPromptTimeMillis()
            {
            return __m_LastPromptTimeMillis;
            }

        // Accessor for the property "LogParameters"
        /**
         * Getter for property LogParameters.<p>
        * A cached array of logger parameters.
         */
        public Object[] getLogParameters()
            {
            return __m_LogParameters;
            }

        // Accessor for the property "ODLRecordConstructor"
        /**
         * Getter for property ODLRecordConstructor.<p>
        * A cached reference to the constructor
        * oracle.core.ojdl.logging.ODLLogRecord<init>(Ljava/util/logging/Level;L
        * java/lang/String;).
         */
        protected java.lang.reflect.Constructor getODLRecordConstructor()
            {
            return __m_ODLRecordConstructor;
            }

        // Accessor for the property "ODLRecordGetCtxMethod"
        /**
         * Getter for property ODLRecordGetCtxMethod.<p>
        * A cached reference to the method
        * oracle.core.ojdl.logging.ODLLogRecord#getLoggingContext().
         */
        protected java.lang.reflect.Method getODLRecordGetCtxMethod()
            {
            return __m_ODLRecordGetCtxMethod;
            }

        // Accessor for the property "ODLRecordInitMethod"
        /**
         * Getter for property ODLRecordInitMethod.<p>
        * A cached reference to the method
        * oracle.core.ojdl.logging.ODLLogRecord#initLoggingContext().
         */
        protected java.lang.reflect.Method getODLRecordInitMethod()
            {
            return __m_ODLRecordInitMethod;
            }

        // Accessor for the property "Prompt"
        /**
         * Getter for property Prompt.<p>
        * The prompt that should be output by the Logger.
         */
        public String getPrompt()
            {
            return __m_Prompt;
            }

        // Declared at the super level
        /**
         * Getter for property ThreadName.<p>
        * Specifies the name of the daemon thread. If not specified, the
        * component name will be used.
        *
        * This property can be set at design time or runtime. If set at
        * runtime, this property must be configured before start() is invoked
        * to cause the daemon thread to have the specified name.
         */
        public String getThreadName()
            {
            return super.getThreadName() + "@" + getHashCode() + " " + Coherence.VERSION;
            }

        // Declared at the super level
        /**
         * Return a LogRecord that encapsulates the specified level and message.
         */
        protected java.util.logging.LogRecord instantiateLogRecord(java.util.logging.Level level, String sMessage)
            {
            // import java.lang.reflect.Constructor;
            // import java.lang.reflect.Method;
            // import java.util.logging.LogRecord;

            if (checkDiagnosability())
                {
                try
                    {
                    // create a record only if an ExecutionContext exists.
                    // Note: if the EC exists, it will be pulled and populated by
                    //       the ODL Record.  See ODLLogRecord#initLoggingContext
                    Constructor consLogRecord  = getODLRecordConstructor();
                    Method      methInitLogCtx = getODLRecordInitMethod();
                    if (consLogRecord != null && methInitLogCtx != null)
                        {
                        // create an ODLRecord
                        LogRecord logRecord = (LogRecord) consLogRecord.newInstance(
                                               new Object[] {level, sMessage});
                        methInitLogCtx.invoke(logRecord, (Object[]) null);

                        return logRecord;
                        }
                    }
                catch (Exception ignored)
                    {
                    // could not obtain DMS context
                    }
                }

            return super.instantiateLogRecord(level, sMessage);
            }

        // Accessor for the property "CommandPrompt"
        /**
         * Getter for property CommandPrompt.<p>
        * Notify the Logger that a command prompt should be output
         */
        public boolean isCommandPrompt()
            {
            return __m_CommandPrompt;
            }

        // Accessor for the property "Configured"
        /**
         * Getter for property Configured.<p>
        * Used to prevent infinite recursion during startup.
         */
        public boolean isConfigured()
            {
            return __m_Configured;
            }

        // Accessor for the property "DMSErrored"
        /**
         * Getter for property DMSErrored.<p>
        * Flag to indicate an error has been thrown by a DMS method thus
        * diagnosibility is disabled.
        * @volatile
         */
        protected boolean isDMSErrored()
            {
            return __m_DMSErrored;
            }

        // Accessor for the property "PendingLineFeed"
        /**
         * Getter for property PendingLineFeed.<p>
        * True to indicate that a line feed should be output by the Logger (if
        * necessary).
         */
        public boolean isPendingLineFeed()
            {
            return __m_PendingLineFeed;
            }

        // Accessor for the property "PendingPrompt"
        /**
         * Getter for property PendingPrompt.<p>
        * True to indicate that a prompt should be output by the Logger (if
        * necessary).
         */
        protected boolean isPendingPrompt()
            {
            return __m_PendingPrompt;
            }

        // Declared at the super level
        /**
         * The "component has been initialized" method-notification (kind of
        * WM_NCCREATE event) called out of setConstructed() for the topmost
        * component and that in turn notifies all the children. <p>
        *
        * This notification gets called before the control returns back to this
        * component instantiator (using <code>new Component.X()</code> or
        * <code>_newInstance(sName)</code>) and on the same thread. In
        * addition, visual components have a "posted" notification
        * <code>onInitUI</code> that is called after (or at the same time as)
        * the control returns back to the instatiator and possibly on a
        * different thread.
         */
        public void onInit()
            {
            // import com.tangosol.util.Base;
            // import java.lang.reflect.Constructor;
            // import java.lang.reflect.Method;
            // import java.util.logging.Level;

            super.onInit();

            setPrompt("?");

            Class       clzDMSCtx    = null;
            Class       clzODLLogRec = null;
            ClassLoader loader       = get_CLASS().getClassLoader();
            try
                {
                // check for ODL availability and cache the method and constructor
                // references
                // Note: applications that load their own DMS/ODL libraries
                //       will be denied ODL logging
                // Note: skip class initialization because they will remain unused
                //       until there is an activated DMS ExecutionContext
                clzDMSCtx = Class.forName("oracle.dms.context.DMSContextManager",
                    false, loader);
                clzODLLogRec = Class.forName("oracle.core.ojdl.logging.ODLLogRecord",
                    false, loader);

                Class[]     aclzParams    = new Class[0];
                Method      methActiveCtx = clzDMSCtx.getMethod("getActiveContext", aclzParams);
                Method      methInitCtx   = clzODLLogRec.getMethod("initLoggingContext", aclzParams);
                Method      methGetCtx    = clzODLLogRec.getMethod("getLoggingContext", aclzParams);
                Constructor consLogRec    = clzODLLogRec.getConstructor(new Class[]{Level.class, String.class});

                setDMSActiveContextMethod(methActiveCtx);
                setODLRecordInitMethod   (methInitCtx);
                setODLRecordGetCtxMethod (methGetCtx);
                setODLRecordConstructor  (consLogRec);
                }
            catch (ClassNotFoundException ignored)
                {
                }
            catch (NoSuchMethodException e)
                {
                System.err.println("Oracle Diagnostic Logging was discovered but expected methods were not found.");
                e.printStackTrace();
                }
            }

        // Declared at the super level
        /**
         * Event notification for performing low frequency periodic maintance
        * tasks.  The interval is dictated by the WaitMillis property.
        *
        * This is used for tasks which have a high enough cost that it is not
        * reasonble to perform them on every call to onWait() since it could be
        * called with a high frequency in the presense of work-loads with fast
        * oscillation between onWait() and onNotify().  As an example a single
        * threaded client could produce such a load.
         */
        protected void onInterval()
            {
            super.onInterval();

            // automatically shutdown an idle logger which isn't attached to a cluster instance
            Coherence coh = (Coherence) get_Module();
            if (coh.getCluster() == null && // disconnected logger
                getQueue().isEmpty())       // idle
                {
                synchronized (Coherence.class)
                    {
                    if (coh.getCluster() == null)
                        {
                        coh.setLogger(null);
                        }
                    }
                }
            }

        // Declared at the super level
        /**
         * Called immediately before a log message is logged to the underlying
        * LogOutput.
         */
        protected void onLog()
            {
            // import java.io.PrintStream;

            super.onLog();

            if (isPendingLineFeed())
                {
                PrintStream out = System.out;
                synchronized (out)
                    {
                    out.println();
                    }
                setPendingLineFeed(false);
                }
            setPendingPrompt(isCommandPrompt());
            }

        // Declared at the super level
        /**
         * Event notification to perform a regular daemon activity. To get it
        * called, another thread has to set Notification to true:
        * <code>daemon.setNotification(true);</code>
        *
        * @see #onWait
         */
        protected void onNotify()
            {
            // import com.tangosol.util.Base;
            // import java.io.PrintStream;

            super.onNotify();

            if (isPendingPrompt())
                {
                long lCurrent = Base.getSafeTimeMillis();
                if (lCurrent > getLastPromptTimeMillis() + getWaitMillis())
                    {
                    PrintStream out = System.out;
                    synchronized (out)
                        {
                        out.print("\nMap (" + getPrompt() + "): ");
                        out.flush();
                        }
                    setPendingPrompt(false);
                    setPendingLineFeed(true);
                    setLastPromptTimeMillis(lCurrent);
                    }
                }
            }

        // Accessor for the property "CommandPrompt"
        /**
         * Setter for property CommandPrompt.<p>
        * Notify the Logger that a command prompt should be output
         */
        public void setCommandPrompt(boolean fPrompt)
            {
            __m_CommandPrompt = (fPrompt);

            setPendingPrompt(fPrompt);
            setLastPromptTimeMillis(0L);
            setWaitMillis(fPrompt ? getDefaultWaitMillis() : 5000L);
            getNotifier().signal();
            }

        // Accessor for the property "Configured"
        /**
         * Setter for property Configured.<p>
        * Used to prevent infinite recursion during startup.
         */
        public void setConfigured(boolean fConfigured)
            {
            __m_Configured = fConfigured;
            }

        // Accessor for the property "DMSActiveContextMethod"
        /**
         * Setter for property DMSActiveContextMethod.<p>
        * A cached reference to the static method
        * oracle.dms.context.DMSContextManager#getActiveContext.
         */
        protected void setDMSActiveContextMethod(java.lang.reflect.Method methodContext)
            {
            __m_DMSActiveContextMethod = methodContext;
            }

        // Accessor for the property "DMSErrored"
        /**
         * Setter for property DMSErrored.<p>
        * Flag to indicate an error has been thrown by a DMS method thus
        * diagnosibility is disabled.
        * @volatile
         */
        protected void setDMSErrored(boolean fErrored)
            {
            __m_DMSErrored = fErrored;
            }

        // Accessor for the property "HashCode"
        /**
         * Setter for property HashCode.<p>
        * Pseudo hash code that contains the license and CPU information.
         */
        public void setHashCode(int n)
            {
            __m_HashCode = (n);

            Thread thread = getThread();
            if (thread != null)
                {
                thread.setName(getThreadName());
                }
            }

        // Accessor for the property "LastPromptTimeMillis"
        /**
         * Setter for property LastPromptTimeMillis.<p>
        * The last time (in milliseconds) that a command prompt was output by
        * the Logger.
         */
        protected void setLastPromptTimeMillis(long nTime)
            {
            __m_LastPromptTimeMillis = nTime;
            }

        // Accessor for the property "LogParameters"
        /**
         * Setter for property LogParameters.<p>
        * A cached array of logger parameters.
         */
        public void setLogParameters(Object[] aoParameters)
            {
            __m_LogParameters = aoParameters;
            }

        // Accessor for the property "ODLRecordConstructor"
        /**
         * Setter for property ODLRecordConstructor.<p>
        * A cached reference to the constructor
        * oracle.core.ojdl.logging.ODLLogRecord<init>(Ljava/util/logging/Level;L
        * java/lang/String;).
         */
        protected void setODLRecordConstructor(java.lang.reflect.Constructor consLogRec)
            {
            __m_ODLRecordConstructor = consLogRec;
            }

        // Accessor for the property "ODLRecordGetCtxMethod"
        /**
         * Setter for property ODLRecordGetCtxMethod.<p>
        * A cached reference to the method
        * oracle.core.ojdl.logging.ODLLogRecord#getLoggingContext().
         */
        protected void setODLRecordGetCtxMethod(java.lang.reflect.Method methGetCtx)
            {
            __m_ODLRecordGetCtxMethod = methGetCtx;
            }

        // Accessor for the property "ODLRecordInitMethod"
        /**
         * Setter for property ODLRecordInitMethod.<p>
        * A cached reference to the method
        * oracle.core.ojdl.logging.ODLLogRecord#initLoggingContext().
         */
        protected void setODLRecordInitMethod(java.lang.reflect.Method methInit)
            {
            __m_ODLRecordInitMethod = methInit;
            }

        // Accessor for the property "PendingLineFeed"
        /**
         * Setter for property PendingLineFeed.<p>
        * True to indicate that a line feed should be output by the Logger (if
        * necessary).
         */
        public void setPendingLineFeed(boolean fPending)
            {
            __m_PendingLineFeed = fPending;
            }

        // Accessor for the property "PendingPrompt"
        /**
         * Setter for property PendingPrompt.<p>
        * True to indicate that a prompt should be output by the Logger (if
        * necessary).
         */
        protected void setPendingPrompt(boolean fPending)
            {
            __m_PendingPrompt = fPending;
            }

        // Accessor for the property "Prompt"
        /**
         * Setter for property Prompt.<p>
        * The prompt that should be output by the Logger.
         */
        public void setPrompt(String sPrompt)
            {
            __m_Prompt = sPrompt;
            }

        // Declared at the super level
        /**
         * Stop the Logger and release any resources held by the Logger. This
        * method has no effect if the Logger has already been stopped. Stopping
        * a Logger makes it unusable. Any attempt to use a stopped Logger may
        * result in an exception.
         */
        public synchronized void shutdown()
            {
            super.shutdown();
            if (isCommandPrompt())
                {
                System.out.println();
                }
            }

        // ---- class: com.tangosol.coherence.component.application.console.Coherence$Logger$Queue

        /**
         * This is the Queue to which items that need to be processed are
         * added, and from which the daemon pulls items to process.
         */
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class Queue
                extends    com.tangosol.coherence.component.util.daemon.QueueProcessor.Queue
            {
            // ---- Fields declarations ----
            private static com.tangosol.util.ListMap __mapChildren;

            // Static initializer
            static
                {
                __initStatic();
                }

            // Default static initializer
            private static void __initStatic()
                {
                // register child classes
                __mapChildren = new com.tangosol.util.ListMap();
                __mapChildren.put("Iterator", Coherence.Logger.Queue.Iterator.get_CLASS());
                }

            // Default constructor
            public Queue()
                {
                this(null, null, true);
                }

            // Initializing constructor
            public Queue(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
                {
                super(sName, compParent, false);

                if (fInit)
                    {
                    __init();
                    }
                }

            // Main initializer
            public void __init()
                {
                // private initialization
                __initPrivate();

                // state initialization: public and protected properties
                try
                    {
                    setElementList(new com.tangosol.util.RecyclingLinkedList());
                    }
                catch (java.lang.Exception e)
                    {
                    // re-throw as a runtime exception
                    throw new com.tangosol.util.WrapperException(e);
                    }

                // containment initialization: children

                // signal the end of the initialization
                set_Constructed(true);
                }

            // Private initializer
            protected void __initPrivate()
                {

                super.__initPrivate();
                }

            //++ getter for static property _Instance
            /**
             * Getter for property _Instance.<p>
            * Auto generated
             */
            public static com.tangosol.coherence.Component get_Instance()
                {
                return new com.tangosol.coherence.component.application.console.Coherence.Logger.Queue();
                }

            //++ getter for static property _CLASS
            /**
             * Getter for property _CLASS.<p>
            * Property with auto-generated accessor that returns the Class
            * object for a given component.
             */
            public static Class get_CLASS()
                {
                Class clz;
                try
                    {
                    clz = Class.forName("com.tangosol.coherence/component/application/console/Coherence$Logger$Queue".replace('/', '.'));
                    }
                catch (ClassNotFoundException e)
                    {
                    throw new NoClassDefFoundError(e.getMessage());
                    }
                return clz;
                }

            //++ getter for autogen property _Module
            /**
             * This is an auto-generated method that returns the global [design
            * time] parent component.
            *
            * Note: the class generator will ignore any custom implementation
            * for this behavior.
             */
            private com.tangosol.coherence.Component get_Module()
                {
                return this.get_Parent().get_Parent();
                }

            //++ getter for autogen property _ChildClasses
            /**
             * This is an auto-generated method that returns the map of design
            * time [static] children.
            *
            * Note: the class generator will ignore any custom implementation
            * for this behavior.
             */
            protected java.util.Map get_ChildClasses()
                {
                return __mapChildren;
                }

            // ---- class: com.tangosol.coherence.component.application.console.Coherence$Logger$Queue$Iterator

            /**
             * Iterator of a snapshot of the List object that backs the Queue.
             * Supports remove(). Uses the Queue as the monitor if any
             * synchronization is required.
             */
            @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
            public static class Iterator
                    extends    com.tangosol.coherence.component.util.daemon.QueueProcessor.Queue.Iterator
                {
                // ---- Fields declarations ----

                // Default constructor
                public Iterator()
                    {
                    this(null, null, true);
                    }

                // Initializing constructor
                public Iterator(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
                    {
                    super(sName, compParent, false);

                    if (fInit)
                        {
                        __init();
                        }
                    }

                // Main initializer
                public void __init()
                    {
                    // private initialization
                    __initPrivate();


                    // signal the end of the initialization
                    set_Constructed(true);
                    }

                // Private initializer
                protected void __initPrivate()
                    {

                    super.__initPrivate();
                    }

                //++ getter for static property _Instance
                /**
                 * Getter for property _Instance.<p>
                * Auto generated
                 */
                public static com.tangosol.coherence.Component get_Instance()
                    {
                    return new com.tangosol.coherence.component.application.console.Coherence.Logger.Queue.Iterator();
                    }

                //++ getter for static property _CLASS
                /**
                 * Getter for property _CLASS.<p>
                * Property with auto-generated accessor that returns the Class
                * object for a given component.
                 */
                public static Class get_CLASS()
                    {
                    Class clz;
                    try
                        {
                        clz = Class.forName("com.tangosol.coherence/component/application/console/Coherence$Logger$Queue$Iterator".replace('/', '.'));
                        }
                    catch (ClassNotFoundException e)
                        {
                        throw new NoClassDefFoundError(e.getMessage());
                        }
                    return clz;
                    }

                //++ getter for autogen property _Module
                /**
                 * This is an auto-generated method that returns the global
                * [design time] parent component.
                *
                * Note: the class generator will ignore any custom
                * implementation for this behavior.
                 */
                private com.tangosol.coherence.Component get_Module()
                    {
                    return this.get_Parent().get_Parent().get_Parent();
                    }
                }
            }

        // ---- class: com.tangosol.coherence.component.application.console.Coherence$Logger$ShutdownHook

        /**
         * Abstract runnable component used as a virtual-machine shutdown hook.
         * Runnable component used to shutdown the Logger upon virtual-machine
         * shutdown.
         *
         * @see Logger#onInit
         */
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class ShutdownHook
                extends    com.tangosol.coherence.component.util.daemon.queueProcessor.Logger.ShutdownHook
            {
            // ---- Fields declarations ----
            private static com.tangosol.util.ListMap __mapChildren;

            // Static initializer
            static
                {
                __initStatic();
                }

            // Default static initializer
            private static void __initStatic()
                {
                // register child classes
                __mapChildren = new com.tangosol.util.ListMap();
                __mapChildren.put("UnregisterAction", Coherence.Logger.ShutdownHook.UnregisterAction.get_CLASS());
                }

            // Default constructor
            public ShutdownHook()
                {
                this(null, null, true);
                }

            // Initializing constructor
            public ShutdownHook(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
                {
                super(sName, compParent, false);

                if (fInit)
                    {
                    __init();
                    }
                }

            // Main initializer
            public void __init()
                {
                // private initialization
                __initPrivate();


                // containment initialization: children

                // signal the end of the initialization
                set_Constructed(true);
                }

            // Private initializer
            protected void __initPrivate()
                {

                super.__initPrivate();
                }

            //++ getter for static property _Instance
            /**
             * Getter for property _Instance.<p>
            * Auto generated
             */
            public static com.tangosol.coherence.Component get_Instance()
                {
                return new com.tangosol.coherence.component.application.console.Coherence.Logger.ShutdownHook();
                }

            //++ getter for static property _CLASS
            /**
             * Getter for property _CLASS.<p>
            * Property with auto-generated accessor that returns the Class
            * object for a given component.
             */
            public static Class get_CLASS()
                {
                Class clz;
                try
                    {
                    clz = Class.forName("com.tangosol.coherence/component/application/console/Coherence$Logger$ShutdownHook".replace('/', '.'));
                    }
                catch (ClassNotFoundException e)
                    {
                    throw new NoClassDefFoundError(e.getMessage());
                    }
                return clz;
                }

            //++ getter for autogen property _Module
            /**
             * This is an auto-generated method that returns the global [design
            * time] parent component.
            *
            * Note: the class generator will ignore any custom implementation
            * for this behavior.
             */
            private com.tangosol.coherence.Component get_Module()
                {
                return this.get_Parent().get_Parent();
                }

            //++ getter for autogen property _ChildClasses
            /**
             * This is an auto-generated method that returns the map of design
            * time [static] children.
            *
            * Note: the class generator will ignore any custom implementation
            * for this behavior.
             */
            protected java.util.Map get_ChildClasses()
                {
                return __mapChildren;
                }

            // ---- class: com.tangosol.coherence.component.application.console.Coherence$Logger$ShutdownHook$UnregisterAction

            @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
            public static class UnregisterAction
                    extends    com.tangosol.coherence.component.util.daemon.queueProcessor.Logger.ShutdownHook.UnregisterAction
                {
                // ---- Fields declarations ----

                // Default constructor
                public UnregisterAction()
                    {
                    this(null, null, true);
                    }

                // Initializing constructor
                public UnregisterAction(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
                    {
                    super(sName, compParent, false);

                    if (fInit)
                        {
                        __init();
                        }
                    }

                // Main initializer
                public void __init()
                    {
                    // private initialization
                    __initPrivate();


                    // signal the end of the initialization
                    set_Constructed(true);
                    }

                // Private initializer
                protected void __initPrivate()
                    {

                    super.__initPrivate();
                    }

                //++ getter for static property _Instance
                /**
                 * Getter for property _Instance.<p>
                * Auto generated
                 */
                public static com.tangosol.coherence.Component get_Instance()
                    {
                    return new com.tangosol.coherence.component.application.console.Coherence.Logger.ShutdownHook.UnregisterAction();
                    }

                //++ getter for static property _CLASS
                /**
                 * Getter for property _CLASS.<p>
                * Property with auto-generated accessor that returns the Class
                * object for a given component.
                 */
                public static Class get_CLASS()
                    {
                    Class clz;
                    try
                        {
                        clz = Class.forName("com.tangosol.coherence/component/application/console/Coherence$Logger$ShutdownHook$UnregisterAction".replace('/', '.'));
                        }
                    catch (ClassNotFoundException e)
                        {
                        throw new NoClassDefFoundError(e.getMessage());
                        }
                    return clz;
                    }

                //++ getter for autogen property _Module
                /**
                 * This is an auto-generated method that returns the global
                * [design time] parent component.
                *
                * Note: the class generator will ignore any custom
                * implementation for this behavior.
                 */
                private com.tangosol.coherence.Component get_Module()
                    {
                    return this.get_Parent().get_Parent().get_Parent();
                    }
                }
            }
        }

    // ---- class: com.tangosol.coherence.component.application.console.Coherence$Worker

    /**
     * This is a Daemon component that waits for items to process from a Queue.
     * Whenever the Queue contains an item, the onNotify event occurs. It is
     * expected that sub-classes will process onNotify as follows:
     * <pre><code>
     * Object o;
     * while ((o = getQueue().removeNoWait()) != null)
     *     {
     *     // process the item
     *     // ...
     *     }
     * </code></pre>
     * <p>
     * The Queue is used as the synchronization point for the daemon.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class Worker
            extends    com.tangosol.coherence.component.util.daemon.QueueProcessor
            implements com.tangosol.net.InvocationObserver,
                       com.tangosol.net.MemberListener,
                       com.tangosol.net.events.EventDispatcherAwareInterceptor,
                       com.tangosol.net.partition.PartitionListener,
                       com.tangosol.util.MapListener,
                       com.tangosol.util.ServiceListener
        {
        // ---- Fields declarations ----

        /**
         * Property EventTypes
         *
         * Interceptor event types this EventInterceptor should be registered
         * against.
         */
        private java.util.Set __m_EventTypes;

        /**
         * Property InterceptorId
         *
         * The EventInterceptor identifier this interceptor was registered with.
         */
        private String __m_InterceptorId;

        /**
         * Property RefCount
         *
         * Reference count: how many filters or keys this component is a
         * listener for.
         */
        private transient int __m_RefCount;

        /**
         * Property Silent
         *
         * Specifies if there should be no logging.
         */
        private transient boolean __m_Silent;

        /**
         * Property WorkerGroup
         *
         */
        private static transient ThreadGroup __s_WorkerGroup;
        private static com.tangosol.util.ListMap __mapChildren;

        // Static initializer
        static
            {
            __initStatic();
            }

        // Default static initializer
        private static void __initStatic()
            {
            // register child classes
            __mapChildren = new com.tangosol.util.ListMap();
            __mapChildren.put("Queue", Coherence.Worker.Queue.get_CLASS());
            }

        // Default constructor
        public Worker()
            {
            this(null, null, true);
            }

        // Initializing constructor
        public Worker(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
            {
            super(sName, compParent, false);

            if (fInit)
                {
                __init();
                }
            }

        // Main initializer
        public void __init()
            {
            // private initialization
            __initPrivate();

            // state initialization: public and protected properties
            try
                {
                setDaemonState(0);
                setDefaultGuardRecovery(0.9F);
                setDefaultGuardTimeout(60000L);
                setNotifier(new com.oracle.coherence.common.base.SingleWaiterMultiNotifier());
                }
            catch (java.lang.Exception e)
                {
                // re-throw as a runtime exception
                throw new com.tangosol.util.WrapperException(e);
                }

            // containment initialization: children
            _addChild(new com.tangosol.coherence.component.util.Daemon.Guard("Guard", this, true), "Guard");

            // signal the end of the initialization
            set_Constructed(true);
            }

        // Private initializer
        protected void __initPrivate()
            {

            super.__initPrivate();
            }

        //++ getter for static property _Instance
        /**
         * Getter for property _Instance.<p>
        * Auto generated
         */
        public static com.tangosol.coherence.Component get_Instance()
            {
            return new com.tangosol.coherence.component.application.console.Coherence.Worker();
            }

        //++ getter for static property _CLASS
        /**
         * Getter for property _CLASS.<p>
        * Property with auto-generated accessor that returns the Class object
        * for a given component.
         */
        public static Class get_CLASS()
            {
            Class clz;
            try
                {
                clz = Class.forName("com.tangosol.coherence/component/application/console/Coherence$Worker".replace('/', '.'));
                }
            catch (ClassNotFoundException e)
                {
                throw new NoClassDefFoundError(e.getMessage());
                }
            return clz;
            }

        //++ getter for autogen property _Module
        /**
         * This is an auto-generated method that returns the global [design
        * time] parent component.
        *
        * Note: the class generator will ignore any custom implementation for
        * this behavior.
         */
        private com.tangosol.coherence.Component get_Module()
            {
            return this.get_Parent();
            }

        //++ getter for autogen property _ChildClasses
        /**
         * This is an auto-generated method that returns the map of design time
        * [static] children.
        *
        * Note: the class generator will ignore any custom implementation for
        * this behavior.
         */
        protected java.util.Map get_ChildClasses()
            {
            return __mapChildren;
            }

        // From interface: com.tangosol.util.MapListener
        public void entryDeleted(com.tangosol.util.MapEvent e)
            {
            process(e);
            }

        // From interface: com.tangosol.util.MapListener
        public void entryInserted(com.tangosol.util.MapEvent e)
            {
            process(e);
            }

        // From interface: com.tangosol.util.MapListener
        public void entryUpdated(com.tangosol.util.MapEvent e)
            {
            process(e);
            }

        // Declared at the super level
        public boolean equals(Object obj)
            {
            // import com.tangosol.util.MapListener;
            // import com.tangosol.util.MapListenerSupport;

            if (!(obj instanceof MapListener))
                {
                return false;
                }

            return obj != null &&
                super.equals(MapListenerSupport.unwrap((MapListener) obj));
            }

        // Accessor for the property "EventTypes"
        /**
         * Getter for property EventTypes.<p>
        * Interceptor event types this EventInterceptor should be registered
        * against.
         */
        public java.util.Set getEventTypes()
            {
            return __m_EventTypes;
            }

        // Accessor for the property "InterceptorId"
        /**
         * Getter for property InterceptorId.<p>
        * The EventInterceptor identifier this interceptor was registered with.
         */
        public String getInterceptorId()
            {
            return __m_InterceptorId;
            }

        // Accessor for the property "RefCount"
        /**
         * Getter for property RefCount.<p>
        * Reference count: how many filters or keys this component is a
        * listener for.
         */
        public int getRefCount()
            {
            return __m_RefCount;
            }

        // Accessor for the property "WorkerGroup"
        /**
         * Getter for property WorkerGroup.<p>
         */
        public static synchronized ThreadGroup getWorkerGroup()
            {
            ThreadGroup group = __s_WorkerGroup;
            if (group == null || group.isDestroyed())
                {
                group = new ThreadGroup("Worker");
                setWorkerGroup(group);
                }
            return group;
            }

        // From interface: com.tangosol.net.events.EventDispatcherAwareInterceptor
        public void introduceEventDispatcher(String sIdentifier, com.tangosol.net.events.EventDispatcher dispatcher)
            {
            // import com.tangosol.util.LiteSet;
            // import java.util.Iterator;
            // import java.util.Set;

            Set setListenEvents = getEventTypes();
            Set setRetained     = new LiteSet();

            if (!(setListenEvents == null || setListenEvents.isEmpty()))
                {
                Set setSupportedEvents = dispatcher.getSupportedTypes();
                for (Iterator iterSupported = setSupportedEvents.iterator(); iterSupported.hasNext(); )
                    {
                    Enum eventType = (Enum) iterSupported.next();
                    if (setListenEvents.contains(eventType.name()))
                        {
                        setRetained.add(eventType);
                        }
                    }
                }

            if (!setRetained.isEmpty())
                {
                dispatcher.addEventInterceptor(sIdentifier, this, setRetained, true);
                }
            }

        // From interface: com.tangosol.net.InvocationObserver
        public void invocationCompleted()
            {
            if (!isSilent())
                {
                _trace("Received \"invocationCompleted\" notification", 3);
                }
            }

        // Accessor for the property "Silent"
        /**
         * Getter for property Silent.<p>
        * Specifies if there should be no logging.
         */
        public boolean isSilent()
            {
            return __m_Silent;
            }

        // From interface: com.tangosol.net.InvocationObserver
        public void memberCompleted(com.tangosol.net.Member member, Object oResult)
            {
            if (!isSilent())
                {
                _trace("Received \"memberCompleted\" notification for " + member +
                       "\nresult=" + oResult, 3);
                }
            }

        // From interface: com.tangosol.net.InvocationObserver
        public void memberFailed(com.tangosol.net.Member member, Throwable eFailure)
            {
            if (!isSilent())
                {
                _trace("Received \"memberFailed\" notification for " + member +
                       "\nexception=" + eFailure, 3);
                }
            }

        // From interface: com.tangosol.net.MemberListener
        public void memberJoined(com.tangosol.net.MemberEvent e)
            {
            process(e);
            }

        // From interface: com.tangosol.net.MemberListener
        public void memberLeaving(com.tangosol.net.MemberEvent e)
            {
            process(e);
            }

        // From interface: com.tangosol.net.InvocationObserver
        public void memberLeft(com.tangosol.net.Member member)
            {
            _trace("Received \"memberLeft\" notification for " + member, 3);
            }

        // From interface: com.tangosol.net.MemberListener
        public void memberLeft(com.tangosol.net.MemberEvent e)
            {
            process(e);
            }

        // From interface: com.tangosol.net.events.EventDispatcherAwareInterceptor
        public void onEvent(com.tangosol.net.events.Event evt)
            {
            if (!isSilent())
                {
                _trace("Received interceptor event: " + evt, 3);
                }
            }

        // Declared at the super level
        /**
         * Event notification called right before the daemon thread terminates.
        * This method is guaranteed to be called only once and on the daemon's
        * thread.
         */
        protected void onExit()
            {
            super.onExit();

            get_Parent()._removeChild(this);
            }

        // Declared at the super level
        /**
         * Event notification to perform a regular daemon activity. To get it
        * called, another thread has to set Notification to true:
        * <code>daemon.setNotification(true);</code>
        *
        * @see #onWait
         */
        protected void onNotify()
            {
            super.onNotify();

            String sCmd = (String) getQueue().remove();
            try
                {
                Coherence app = (Coherence) get_Module();
                app.processCommand(sCmd);
                if (app.isStop())
                    {
                    System.exit(0);
                    }
                setExiting(true);
                }
            catch (InterruptedException e)
                {
                _trace("Thread " + getThreadName() + " has been interrupted", 3);
                setExiting(true);
                }
            }

        // From interface: com.tangosol.net.partition.PartitionListener
        public void onPartitionEvent(com.tangosol.net.partition.PartitionEvent evt)
            {
            if (!isSilent())
                {
                _trace("Received " + evt, 3);
                }
            }

        protected void process(com.tangosol.net.MemberEvent evtMember)
            {
            // import com.tangosol.net.Service;

            if (!isSilent())
                {
                Service service = evtMember.getService();

                _trace("Received event for " + service + '\n' + evtMember, 3);
                }
            }

        protected void process(com.tangosol.util.MapEvent evtMap)
            {
            // import com.tangosol.net.NamedCache;
            // import com.tangosol.util.WrapperException;
            // import java.util.Map;

            Map    map   = (Map) evtMap.getSource();
            String sName = "Map=" + map.getClass().getName();
            if (map instanceof NamedCache)
                {
                sName = "Cache=" + ((NamedCache) map).getCacheName();
                }

            if (!isSilent())
                {
                _trace(get_Name() + ": received event for " + sName + '\n' + evtMap, 3);
                }

            Object oKey = evtMap.getKey();
            if ("exception".equals(oKey))
                {
                throw new RuntimeException("Test exception");
                }

            if ("stack".equals(oKey))
                {
                _trace(get_StackTrace(), 3);
                }

            if ("command".equals(oKey))
                {
                Object oValue = evtMap.getNewValue();
                if (oValue instanceof String)
                    {
                    try
                        {
                        String sCommand = (String) oValue;
                        ((Coherence) get_Module()).processCommand(sCommand.replace(',', ';'));
                        }
                    catch (InterruptedException e)
                        {
                        Thread.currentThread().interrupt();
                        throw new WrapperException(e);
                        }
                    }
                }
            }

        protected void process(com.tangosol.util.ServiceEvent evtService)
            {
            // import com.tangosol.util.Service;

            if (!isSilent())
                {
                Service service = (Service) evtService.getService();

                _trace("Received event for " + service + '\n' + evtService, 3);
                }
            }

        // From interface: com.tangosol.util.ServiceListener
        public void serviceStarted(com.tangosol.util.ServiceEvent e)
            {
            process(e);
            }

        // From interface: com.tangosol.util.ServiceListener
        public void serviceStarting(com.tangosol.util.ServiceEvent e)
            {
            process(e);
            }

        // From interface: com.tangosol.util.ServiceListener
        public void serviceStopped(com.tangosol.util.ServiceEvent e)
            {
            process(e);
            }

        // From interface: com.tangosol.util.ServiceListener
        public void serviceStopping(com.tangosol.util.ServiceEvent e)
            {
            process(e);
            }

        // Accessor for the property "EventTypes"
        /**
         * Setter for property EventTypes.<p>
        * Interceptor event types this EventInterceptor should be registered
        * against.
         */
        public void setEventTypes(java.util.Set setTypes)
            {
            __m_EventTypes = setTypes;
            }

        // Accessor for the property "InterceptorId"
        /**
         * Setter for property InterceptorId.<p>
        * The EventInterceptor identifier this interceptor was registered with.
         */
        public void setInterceptorId(String sId)
            {
            __m_InterceptorId = sId;
            }

        // Accessor for the property "RefCount"
        /**
         * Setter for property RefCount.<p>
        * Reference count: how many filters or keys this component is a
        * listener for.
         */
        public void setRefCount(int c)
            {
            __m_RefCount = c;
            }

        // Accessor for the property "Silent"
        /**
         * Setter for property Silent.<p>
        * Specifies if there should be no logging.
         */
        public void setSilent(boolean fSilent)
            {
            __m_Silent = fSilent;
            }

        // Accessor for the property "WorkerGroup"
        /**
         * Setter for property WorkerGroup.<p>
         */
        public static void setWorkerGroup(ThreadGroup group)
            {
            __s_WorkerGroup = group;
            }

        // Declared at the super level
        /**
         * Starts the daemon thread associated with this component. If the
        * thread is already starting or has started, invoking this method has
        * no effect.
        *
        * Synchronization is used here to verify that the start of the thread
        * occurs; the lock is obtained before the thread is started, and the
        * daemon thread notifies back that it has started from the run() method.
         */
        public synchronized void start()
            {
            setThreadGroup(getWorkerGroup());

            super.start();
            }

        // Declared at the super level
        public String toString()
            {
            // import java.util.Date;

            return get_Name() +
                (isStarted()        ? " started at " + new Date(getStartTimestamp())
                : getRefCount() > 0 ? " listening, count=" + getRefCount()
                :                     " not running");
            }

        // ---- class: com.tangosol.coherence.component.application.console.Coherence$Worker$Queue

        /**
         * This is the Queue to which items that need to be processed are
         * added, and from which the daemon pulls items to process.
         */
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class Queue
                extends    com.tangosol.coherence.component.util.daemon.QueueProcessor.Queue
            {
            // ---- Fields declarations ----
            private static com.tangosol.util.ListMap __mapChildren;

            // Static initializer
            static
                {
                __initStatic();
                }

            // Default static initializer
            private static void __initStatic()
                {
                // register child classes
                __mapChildren = new com.tangosol.util.ListMap();
                __mapChildren.put("Iterator", Coherence.Worker.Queue.Iterator.get_CLASS());
                }

            // Default constructor
            public Queue()
                {
                this(null, null, true);
                }

            // Initializing constructor
            public Queue(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
                {
                super(sName, compParent, false);

                if (fInit)
                    {
                    __init();
                    }
                }

            // Main initializer
            public void __init()
                {
                // private initialization
                __initPrivate();

                // state initialization: public and protected properties
                try
                    {
                    setElementList(new com.tangosol.util.RecyclingLinkedList());
                    }
                catch (java.lang.Exception e)
                    {
                    // re-throw as a runtime exception
                    throw new com.tangosol.util.WrapperException(e);
                    }

                // containment initialization: children

                // signal the end of the initialization
                set_Constructed(true);
                }

            // Private initializer
            protected void __initPrivate()
                {

                super.__initPrivate();
                }

            //++ getter for static property _Instance
            /**
             * Getter for property _Instance.<p>
            * Auto generated
             */
            public static com.tangosol.coherence.Component get_Instance()
                {
                return new com.tangosol.coherence.component.application.console.Coherence.Worker.Queue();
                }

            //++ getter for static property _CLASS
            /**
             * Getter for property _CLASS.<p>
            * Property with auto-generated accessor that returns the Class
            * object for a given component.
             */
            public static Class get_CLASS()
                {
                Class clz;
                try
                    {
                    clz = Class.forName("com.tangosol.coherence/component/application/console/Coherence$Worker$Queue".replace('/', '.'));
                    }
                catch (ClassNotFoundException e)
                    {
                    throw new NoClassDefFoundError(e.getMessage());
                    }
                return clz;
                }

            //++ getter for autogen property _Module
            /**
             * This is an auto-generated method that returns the global [design
            * time] parent component.
            *
            * Note: the class generator will ignore any custom implementation
            * for this behavior.
             */
            private com.tangosol.coherence.Component get_Module()
                {
                return this.get_Parent().get_Parent();
                }

            //++ getter for autogen property _ChildClasses
            /**
             * This is an auto-generated method that returns the map of design
            * time [static] children.
            *
            * Note: the class generator will ignore any custom implementation
            * for this behavior.
             */
            protected java.util.Map get_ChildClasses()
                {
                return __mapChildren;
                }

            // ---- class: com.tangosol.coherence.component.application.console.Coherence$Worker$Queue$Iterator

            /**
             * Iterator of a snapshot of the List object that backs the Queue.
             * Supports remove(). Uses the Queue as the monitor if any
             * synchronization is required.
             */
            @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
            public static class Iterator
                    extends    com.tangosol.coherence.component.util.daemon.QueueProcessor.Queue.Iterator
                {
                // ---- Fields declarations ----

                // Default constructor
                public Iterator()
                    {
                    this(null, null, true);
                    }

                // Initializing constructor
                public Iterator(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
                    {
                    super(sName, compParent, false);

                    if (fInit)
                        {
                        __init();
                        }
                    }

                // Main initializer
                public void __init()
                    {
                    // private initialization
                    __initPrivate();


                    // signal the end of the initialization
                    set_Constructed(true);
                    }

                // Private initializer
                protected void __initPrivate()
                    {

                    super.__initPrivate();
                    }

                //++ getter for static property _Instance
                /**
                 * Getter for property _Instance.<p>
                * Auto generated
                 */
                public static com.tangosol.coherence.Component get_Instance()
                    {
                    return new com.tangosol.coherence.component.application.console.Coherence.Worker.Queue.Iterator();
                    }

                //++ getter for static property _CLASS
                /**
                 * Getter for property _CLASS.<p>
                * Property with auto-generated accessor that returns the Class
                * object for a given component.
                 */
                public static Class get_CLASS()
                    {
                    Class clz;
                    try
                        {
                        clz = Class.forName("com.tangosol.coherence/component/application/console/Coherence$Worker$Queue$Iterator".replace('/', '.'));
                        }
                    catch (ClassNotFoundException e)
                        {
                        throw new NoClassDefFoundError(e.getMessage());
                        }
                    return clz;
                    }

                //++ getter for autogen property _Module
                /**
                 * This is an auto-generated method that returns the global
                * [design time] parent component.
                *
                * Note: the class generator will ignore any custom
                * implementation for this behavior.
                 */
                private com.tangosol.coherence.Component get_Module()
                    {
                    return this.get_Parent().get_Parent().get_Parent();
                    }
                }
            }
        }
    }
