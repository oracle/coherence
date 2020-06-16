/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package common;


import com.oracle.bedrock.OptionsByType;
import com.oracle.bedrock.jacoco.Dump;
import com.oracle.bedrock.runtime.java.options.JavaHome;
import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.runtime.Application;
import com.oracle.bedrock.runtime.ApplicationConsole;
import com.oracle.bedrock.runtime.LocalPlatform;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.coherence.JMXManagementMode;
import com.oracle.bedrock.runtime.coherence.options.LocalHost;
import com.oracle.bedrock.runtime.concurrent.RemoteRunnable;
import com.oracle.bedrock.runtime.console.FileWriterApplicationConsole;
import com.oracle.bedrock.runtime.java.ClassPath;
import com.oracle.bedrock.runtime.java.JavaApplication;
import com.oracle.bedrock.runtime.java.features.JmxFeature;
import com.oracle.bedrock.runtime.java.options.ClassName;
import com.oracle.bedrock.runtime.java.options.Headless;
import com.oracle.bedrock.runtime.java.options.HeapSize;
import com.oracle.bedrock.runtime.java.options.JvmOptions;
import com.oracle.bedrock.runtime.java.options.SystemProperties;
import com.oracle.bedrock.runtime.java.options.SystemProperty;
import com.oracle.bedrock.runtime.network.AvailablePortIterator;
import com.oracle.bedrock.runtime.options.Argument;
import com.oracle.bedrock.runtime.options.Console;
import com.oracle.bedrock.runtime.options.DisplayName;
import com.oracle.bedrock.runtime.coherence.callables.GetAutoStartServiceNames;
import com.tangosol.coherence.component.util.SafeService;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.CacheFactoryBuilder;
import com.tangosol.net.CacheService;
import com.tangosol.net.Cluster;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.DefaultCacheServer;
import com.tangosol.net.DefaultConfigurableCacheFactory;
import com.tangosol.net.Member;
import com.tangosol.net.NamedCache;
import com.tangosol.util.Base;
import com.tangosol.util.Service;

import java.util.Enumeration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.hamcrest.Matchers;
import org.hamcrest.core.Is;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.TestName;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import java.security.AccessController;
import java.security.PrivilegedAction;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static com.oracle.bedrock.deferred.DeferredHelper.within;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;


/**
 * An abstract class providing shared test infrastructure for Coherence-based
 * tests, across numerous versions of Coherence.
 * <p>
 * WARNING: This class *MUST NOT* use or return instances of interfaces or classes
 * that are specific and incompatible between versions of Coherence.  For example:
 * this class does not create or return instances NamedCache, otherwise
 * incompatibilities and compile-time errors will occur.
 *
 * @author bko  2015.08.21
 */
public abstract class AbstractTestInfrastructure
        extends Base
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    public AbstractTestInfrastructure()
        {
        // use DCCF so that Extend 3.4 tests compile
        this(DefaultConfigurableCacheFactory.FILE_CFG_CACHE);
        }

    /**
    * Create a new AbstractFunctionalTest that will use the cache
    * configuration file with the given path to instantiate NamedCache
    * instances.
    *
    * @param sPath  the configuration resource name or file path
    */
    public AbstractTestInfrastructure(String sPath)
        {
        if (sPath == null || sPath.trim().length() == 0)
            {
            throw new IllegalArgumentException();
            }
        m_sCacheConfigPath = sPath.trim();
        }

    /**
    * Create a new AbstractFunctionalTest that will use the given factory
    * to instantiate NamedCache instances.
    *
    * @param factory  the ConfigurableCacheFactory used to instantiate
    *                 NamedCache instances
    */
    public AbstractTestInfrastructure(ConfigurableCacheFactory factory)
        {
        if (factory == null)
            {
            throw new IllegalArgumentException();
            }
        m_factory = factory;
        }


    // ----- test lifecycle -------------------------------------------------

    /**
    * Initialize the test class.
    * <p>
    * This method starts the Coherence cluster, if it isn't already running.
    */
    @BeforeClass
    public static void _startup()
        {
        setupProps();

        startCluster();
        }

    @Before
    public void _beforeTest()
        {
        System.out.println(createMessageHeader() + " >>>> Starting test " + m_testName.getMethodName());
        String sName = AccessController.doPrivileged((PrivilegedAction<String>) () -> System.getProperty("user.name"));

        if (sName == null)
            {
            System.out.println("System property user.name is null; set it to coh");
            AccessController.doPrivileged((PrivilegedAction<String>) () -> System.setProperty("user.name", "coh"));
            }
        }

    @After
    public void _afterTest()
        {
        System.out.println(createMessageHeader() + " <<<< Completed test " + m_testName.getMethodName());
        }

    /**
    * Shutdown the test class.
    * <p/>
    * This method stops the Coherence cluster.
    */
    @AfterClass
    public static void _shutdown()
        {
        stopAllApplications();
        CacheFactory.shutdown();

        // don't use the out() method, as it will restart the cluster
        System.out.println(createMessageHeader() + " <<<<<<< Stopped cluster");
        }

    /**
     * Stops all processes that have been started by this test
     * class via calls to startCacheServer or startCacheApplication.
     */
    protected static void stopAllApplications()
        {
        for (String sApplication : m_mapApplications.keySet())
            {
            stopCacheApplication(sApplication);
            }
        }


    // ----- helpers --------------------------------------------------------

    /**
     * Initialize the system properties.
     */
    public static void setupProps()
        {
        // generate unique IP addresses and ports for this test
        Properties props = System.getProperties();

        props.setProperty("java.net.preferIPv4Stack", "true");
        props.setProperty("test.extend.port",         String.valueOf(getAvailablePorts().next()));
        props.setProperty("test.multicast.address",   generateUniqueAddress(true));

        // use INADDR_ANY available ports for multicast
        props.setProperty("test.multicast.port",
            String.valueOf(LocalPlatform.get().getAvailablePorts().next()));

        props.setProperty("tangosol.coherence.nameservice.address",
            LocalPlatform.get().getLoopbackAddress().getHostAddress());

        // assume that this process should be storage disabled
        if (com.tangosol.coherence.config.Config.getProperty(
                "tangosol.coherence.distributed.localstorage") == null)
            {
            props.setProperty("tangosol.coherence.distributed.localstorage", "false");
            }
        }

    /**
     * Ensure the cluster is running.
     */
    public static Cluster startCluster()
        {
        Properties props = System.getProperties();

        CacheFactory.shutdown(); // (defensive) ensure we don't retain config from prior test

        Cluster cluster;
        try
            {
            props.setProperty("tangosol.coherence.join.timeout", "0"); // dramatically speed up the start of this (the first node)
            cluster = CacheFactory.ensureCluster();
            }
        finally
            {
            props.remove("tangosol.coherence.join.timeout");
            }

        // capture the unicast port that we bound too
        props.setProperty("test.unicast.port",
                String.valueOf(cluster.getLocalMember().getPort()));

        System.out.println(createMessageHeader() +  " >>>>>>> Started cluster");

        return cluster;
        }

    /**
     * Display the operating system level info on TCP port usage.
     */
    public static void displayPortInfo()
        {
        String            sOS        = System.getProperty("os.name").toLowerCase();
        OptionsByType optionsByType  = null;

        if (sOS.contains("mac"))
            {
            optionsByType = OptionsByType.of(Argument.of("-tanp"),
                                             Argument.of("tcp"),
                                             Console.system());
            }
        else if (sOS.contains("linux"))
            {
            optionsByType = OptionsByType.of(Argument.of("-tanpve"),
                                             Console.system());
            }
        else if (sOS.contains("windows"))
            {
            optionsByType = OptionsByType.of(Argument.of("-baonp"),
                                             Argument.of("tcp"),
                                             Console.system());
            }

        if (optionsByType != null)
            {
            try (Application netstat = LocalPlatform.get().launch("netstat", optionsByType.asArray()))
                {
                netstat.waitFor();
                }
            }
        }

    /**
    * Return a unique IP address for this machine.
    *
    * @param fMulticast  true if the returned address should be a multicast
    *                    address
    *
    * @return a unique IP address for this machine
    */
    public static String generateUniqueAddress(boolean fMulticast)
        {
        String sAddr;
        try
            {
            sAddr = InetAddress.getLocalHost().getHostAddress();
            }
        catch (UnknownHostException e)
            {
            sAddr = "127.0.0.1";
            }

        String[] asAddr = sAddr.split("\\.");

        if (fMulticast)
            {
            sAddr = "224";
            }
        else
            {
            sAddr = asAddr[0];
            }

        Random rnd = new Random();
        for (int i = 0; i < 2; ++i)
            {
            sAddr += ".";
            sAddr += rnd.nextInt(256);
            }
        sAddr += "." + asAddr[3];

        return sAddr;
        }

    /**
    * Return the Member that represents the cache server with the given name.
    *
    * @param sServer the cache server name
    *
    * @return the Member that represents the specified cache server or null
    *         if no such member could be found
    */
    protected static Member findCacheServer(String sServer)
        {
        for (Iterator iter = CacheFactory.ensureCluster().getMemberSet().iterator();
             iter.hasNext(); )
            {
            Member member = (Member) iter.next();
            if (equals(member.getRoleName(), sServer))
                {
                return member;
                }
            }
        return null;
        }

    /**
     * Return the {@link CoherenceClusterMember} that was started with the specified name.
     *
     * @param sName  the name of the JavaApplication to find
     *
     * @return the JavaApplication that was started with the specified name
     *         or null if no JavaApplication was started with that name
     */
    protected static CoherenceClusterMember findApplication(String sName)
        {
        return m_mapApplications.get(sName);
        }

    /**
    * Start a cache application using the "start.test.application" target in
    * the specified test project's Ant build file. The cache application will
    * use the specified cache configuration file found in the test project
    * directory as well as any optional specified system properties.
    *  @param sApplication  the name of the cache application to start; this
    *                      name can be used to stop the cache application
    *                      when it is no longer needed
    * @param sClass        the class name of the cache application to start
    * @param sProject      the test project to start the cache server for
    * @param sCacheConfig  the name of the cache configuration file that will
    *                      be used by the cache application; this file must
    *                      reside within the specified test project's
    *                      directory
    * @param props         the optional system properties to pass to the
    */
    protected static CoherenceClusterMember startCacheApplication(String sApplication,
            String sClass, String sProject, String sCacheConfig, Properties props)
        {
        return startCacheApplication(sApplication, sClass, sProject, sCacheConfig, props, false);
        }

    /**
    * Start a cache application. The cache application will use the
    * specified cache configuration file as well as any optional
    * specified system properties.
    *
    * @param sApplication  the name of the cache application to start; this
    *                      name can be used to stop the cache application
    *                      when it is no longer needed
    * @param sClass        the class name of the cache application to start
    * @param sProject      the test project to start the cache server for
    * @param sCacheConfig  the name of the cache configuration file that will
    *                      be used by the cache application
    * @param props         the optional system properties to pass to the
    *                      cache application JVM
    * @param fWait         if true, this method will not return until the
    *                      cache application terminates
    */
    protected static CoherenceClusterMember startCacheApplication(String sApplication,
            String sClass, String sProject, String sCacheConfig, Properties props, boolean fWait)
        {
        if (sApplication == null || sApplication.length() == 0)
            {
            throw new IllegalArgumentException("Missing required application name");
            }

        if (props == null)
            {
            props = new Properties();
            }
        else
            {
            Properties _props = props;
            props = new Properties();
            props.putAll(_props);
            }

        if (fWait)
            {
            out(">>>>>>> Executing cache application: " + sApplication);
            }
        else
            {
            out(">>>>>>> Starting cache application: " + sApplication);
            }

        props.setProperty("test.application.name",      sApplication);
        props.setProperty("test.application.classname", sClass);
        props.setProperty("test.application.wait",      String.valueOf(fWait));
        if (sCacheConfig != null && sCacheConfig.length() > 0)
            {
            props.setProperty("tangosol.coherence.cacheconfig", sCacheConfig);
            }

        // set the role name of the cache application to it's name so that we
        // can later identify it programmatically
        props.setProperty("tangosol.coherence.role", sApplication);

        // enable remote management unless explicitly set
        if (com.tangosol.coherence.config.Config.getProperty(
                "tangosol.coherence.management") == null)
            {
            props.setProperty("tangosol.coherence.management", "all");
            }

        // add "standard" system properties
        addTestProperties(props);

        // assume that the cache application is storage disabled
        props.setProperty("tangosol.coherence.distributed.localstorage", "false");

        OptionsByType optionsByType = createCacheServerOptions(sClass);

        for (String sName : props.stringPropertyNames())
            {
            optionsByType.add(SystemProperty.of(sName, props.getProperty(sName)));
            }

        CoherenceClusterMember member;
        try
            {
            member = startCacheServer(sApplication, optionsByType, sProject, false);
            }
        catch (IOException e)
            {
            throw new RuntimeException("Error starting cache server", e);
            }

        if (fWait)
            {
            member.waitFor();
            out(">>>>>>> Executed cache application: " + sApplication);
            }
        else
            {
            out(">>>>>>> Started cache application: " + sApplication);
            }

        return member;
        }

    /**
    * Start a cache server giving it the specified name and piping
    * output to the specified project output directory.
    *
    * @param sServer   the name of the cache server to start; this name
    *                  should be used to stop the cache server when it is no
    *                  longer needed
    * @param sProject  the test project to start the cache server for
    */
    public static CoherenceClusterMember startCacheServer(String sServer, String sProject)
        {
        return startCacheServer(sServer, sProject, null);
        }

    /**
    * Start a cache server giving it the specified name and piping
    * output to the specified project output directory. The cache
    * server will use the specified cache configuration file.
    *
    * @param sServer       the name of the cache server to start; this name
    *                      should be used to stop the cache server when it is
    *                      no longer needed
    * @param sProject      the test project to start the cache server for
    * @param sCacheConfig  the name of the cache configuration file that will
    *                      be used by the cache server
    */
    protected static CoherenceClusterMember startCacheServer(String sServer, String sProject,
            String sCacheConfig)
        {
        return startCacheServer(sServer, sProject, sCacheConfig, null);
        }

    /**
    * Start a cache server giving it the specified name and piping
    * output to the specified project output directory. The cache
    * server will use the specified cache configuration file as well
    * as any optional specified system properties.
    *
    * @param sServer       the name of the cache server to start; this name
    *                      should be used to stop the cache server when it is
    *                      no longer needed
    * @param sProject      the test project to start the cache server for
    * @param sCacheConfig  the name of the cache configuration file that will
    *                      be used by the cache server
    * @param props         the optional system properties to pass to the
    *                      cache server JVM
    */
    protected static CoherenceClusterMember startCacheServer(String sServer, String sProject,
            String sCacheConfig, Properties props)
        {
        return startCacheServer(sServer, sProject, sCacheConfig, props, true);
        }

    /**
    * Start a cache server giving it the specified name and piping
    * output to the specified project output directory. The cache
    * server will use the specified cache configuration file as well
    * as any optional specified system properties.
    *
    * @param sServer       the name of the cache server to start; this name
    *                      should be used to stop the cache server when it is
    *                      no longer needed
    * @param sProject      the test project to start the cache server for
    * @param sCacheConfig  the name of the cache configuration file that will
    *                      be used by the cache server
    * @param props         the optional system properties to pass to the
    *                      cache server JVM
    * @param fGraceful     if true, a "graceful" startup of the cache server
    *                      will be performed
    */
    protected static CoherenceClusterMember startCacheServer(String sServer, String sProject,
        String sCacheConfig, Properties props, boolean fGraceful)
        {
        return startCacheServer(sServer, sProject, sCacheConfig, props, fGraceful, null);
        }

    /**
    * Start a cache server giving it the specified name and piping
    * output to the specified project output directory. The cache
    * server will use the specified cache configuration file as well
    * as any optional specified system properties.
    *
    * @param sServer       the name of the cache server to start; this name
    *                      should be used to stop the cache server when it is
    *                      no longer needed
    * @param sProject      the test project to start the cache server for
    * @param sCacheConfig  the name of the cache configuration file that will
    *                      be used by the cache server
    * @param props         the optional system properties to pass to the
    *                      cache server JVM
    * @param fGraceful     if true, a "graceful" startup of the cache server
    *                      will be performed
    * @param sClassPath    the optional classpath to use for the server being started
    */
    protected static CoherenceClusterMember startCacheServer(String sServer, String sProject,
                                                             String sCacheConfig, Properties props, boolean fGraceful, String sClassPath)
        {
        return startCacheServer(sServer, sProject, sCacheConfig, props,
                fGraceful, sClassPath, DefaultCacheServer.class);
        }

    /**
    * Start a cache server giving it the specified name and piping
    * output to the specified project output directory. The cache
    * server will use the specified cache configuration file as well
    * as any optional specified system properties.
    *
    * @param sServer       the name of the cache server to start; this name
    *                      should be used to stop the cache server when it is
    *                      no longer needed
    * @param sProject      the test project to start the cache server for
    * @param sCacheConfig  the name of the cache configuration file that will
    *                      be used by the cache server
    * @param props         the optional system properties to pass to the
    *                      cache server JVM
    * @param fGraceful     if true, a "graceful" startup of the cache server
    *                      will be performed
    * @param sClassPath    the optional classpath to use for the server being started
    * @param classMain     the Class with a main method will be used to start the
    *                      cache server, if null then {@link DefaultCacheServer} is used
    */
    protected static CoherenceClusterMember startCacheServer(String sServer, String sProject,
            String sCacheConfig, Properties props, boolean fGraceful, String sClassPath, Class classMain)
        {
        if (sServer == null || sServer.length() == 0)
            {
            throw new IllegalArgumentException("Missing required server name");
            }

        CoherenceClusterMember member = findApplication(sServer);
        if (fGraceful && member != null)
            {
            return member;
            }

        if (props == null)
            {
            props = new Properties();
            }
        else
            {
            Properties _props = props;
            props = new Properties();
            props.putAll(_props);
            }

        if (classMain == null)
            {
            classMain = DefaultCacheServer.class;
            }

        OptionsByType optionsByType = createCacheServerOptions(classMain.getCanonicalName(), sClassPath);

        props.setProperty("test.server.name", sServer);
        if (sCacheConfig != null && sCacheConfig.length() > 0)
            {
            props.setProperty("tangosol.coherence.cacheconfig", sCacheConfig);
            }

        // set the role name of the cache server to it's name so that we
        // can later identify it programmatically, if it hasn't already been specified
        if (!props.containsKey("coherence.role") && !props.containsKey("tangosol.coherence.role"))
            {
            props.setProperty("tangosol.coherence.role", sServer);
            }

        // add "standard" system properties
        addTestProperties(props);

        if (props.containsKey("test.server.distributed.localstorage"))
            {
            props.setProperty("tangosol.coherence.distributed.localstorage",
                    props.getProperty("test.server.distributed.localstorage"));
            }
        else
            {
            // assume that the cache server is storage enabled
            props.setProperty("tangosol.coherence.distributed.localstorage", "true");
            }

        if (!props.containsKey("test.unicast.address"))
            {
            props.setProperty("test.unicast.address", LocalPlatform.get().getLoopbackAddress().getHostAddress());
            }

        // only bind the NameService to the loopback address unless explicitly set
        if (!props.containsKey("tangosol.coherence.nameservice.address"))
            {
            props.setProperty("tangosol.coherence.nameservice.address",
                LocalPlatform.get().getLoopbackAddress().getHostAddress());
            }

        for (String sName : props.stringPropertyNames())
            {
            optionsByType.add(SystemProperty.of(sName, props.getProperty(sName)));
            }

        try
            {
            member = startCacheServer(sServer, optionsByType, sProject, fGraceful);
            }
        catch (IOException e)
            {
            throw new RuntimeException("Error starting cache server", e);
            }


        if (fGraceful)
            {
            try
                {
                waitForServer(member);
                }
            catch (AssertionError err)
                {
                // show OS level port usage in case of port binding conflict
                displayPortInfo();
                throw err;
                }
            }

        return member;
        }

    /**
     * Return control when all autostart services have been started or 2 minutes
     * have surpassed (for each service).
     *
     * @param server  the remote server to ensure services have started on
     */
    public static void waitForServer(CoherenceClusterMember server)
        {
        Eventually.assertThat(invoking(server).isServiceRunning("Cluster"), is(true), within(2, TimeUnit.MINUTES));

        Set<String> setServiceNames = server.invoke(new GetAutoStartServiceNames());

        for (String sServiceName : setServiceNames)
            {
            Eventually.assertThat(invoking(server).isServiceRunning(sServiceName), is(true), within(2, TimeUnit.MINUTES));
            }
        }

    /**
     * Start a cluster member using the specified {@link OptionsByType},
     * giving it the specified name and piping output to the specified project
     * output directory.
     *
     * @param sServer        the name of the cluster member to start; this name
     *                       should be used to stop the cluster member when it is
     *                       no longer needed
     * @param sProject       the test project to start the cluster member for
     * @param optionsByType  the CoherenceClusterMemberSchema to use pass to the
     *                       {@link LocalPlatform} to realize the process
     *
     * @return a {@link CoherenceClusterMember} representing the realized server process
     */
    protected static CoherenceClusterMember startCacheServer(String sServer,
                                                             OptionsByType optionsByType,
                                                             String sProject,
                                                             boolean fGraceful) throws IOException
        {
        if (sServer == null || sServer.length() == 0)
            {
            throw new IllegalArgumentException("Missing required server name");
            }

        if (optionsByType == null)
            {
            throw new IllegalArgumentException("Missing required schema");
            }

        CoherenceClusterMember application = m_mapApplications.get(sServer);
        if (fGraceful && application != null)
            {
            return application;
            }

        FileWriter         writer  = new FileWriter(new File(ensureOutputDir(sProject), sServer + ".out"));
        ApplicationConsole console = new FileWriterApplicationConsole(writer);

        out(createMessageHeader() + " >>>>>>> Starting cache server: " + sServer);

        optionsByType.add(DisplayName.of(sServer));
        optionsByType.add(Console.of(console));

        String sJavaHome = System.getProperty("server.java.home");
        if (sJavaHome != null)
            {
            optionsByType.add(JavaHome.at(sJavaHome));
            }

        CoherenceClusterMember member = LocalPlatform.get().launch(CoherenceClusterMember.class, optionsByType.asArray());
        m_mapApplications.put(sServer, member);

        out(createMessageHeader() + " >>>>>>> Started cache server: " + sServer);

        return member;
        }

    /**
     * Ensure that the specified service is running on the specified server
     *
     * @param sServerName   the server name
     * @param sServiceName  teh service name
     */
    public static void ensureRunningService(String sServerName, String sServiceName)
        {
        Eventually.assertThat(invoking(findApplication(sServerName)).
            isServiceRunning(sServiceName), is(true));
        }

    protected static OptionsByType createCacheServerOptions(String sClass)
        {
        return createCacheServerOptions(sClass, null);
        }

    /**
     * Create default {@link OptionsByType} to use for launching one or
     * more test cache servers.
     *
     * @param sClass     the name of the main class to run
     * @param sClassPath the optional class path to use for the process
     *
     * @return an OptionsByTpe
     */
    public static OptionsByType createCacheServerOptions(String sClass, String sClassPath)
        {
        if (sClassPath == null || sClassPath.isEmpty())
            {
            sClassPath = System.getProperty("java.class.path");
            }

        OptionsByType optionsByType = OptionsByType.empty();

        if (sClass == null)
            {
            optionsByType.add(ClassName.of(DefaultCacheServer.class));
            }
        else
            {
            optionsByType.add(ClassName.of(sClass));
            }

        if(sClassPath != null)
            {
            optionsByType.add(ClassPath.of(sClassPath));
            }

        optionsByType.add(JvmOptions.include("-server",
                                             "-XX:CompileCommand=exclude,com/tangosol/coherence/component/util/daemon/queueProcessor/service/Grid.onInterval",
                                             "-XX:+HeapDumpOnOutOfMemoryError",
                                             "-XX:HeapDumpPath=" + System.getProperty("test.project.dir") + File.separatorChar + "target",
                                             "-XX:+ExitOnOutOfMemoryError"));
        optionsByType.add(HeapSize.of(256, HeapSize.Units.MB, 1024, HeapSize.Units.MB));
        optionsByType.add(Headless.enabled());

        optionsByType.add(LocalHost.only());
        optionsByType.add(JMXManagementMode.NONE);

        optionsByType.add(JmxFeature.enabled());
        optionsByType.add(SystemProperty.of(JavaApplication.JAVA_RMI_SERVER_HOSTNAME,
                                            LocalPlatform.get().getLoopbackAddress().getHostAddress()));

        optionsByType.add(SystemProperty.of("test.jvm", System.getProperty("java.home") + "/bin/java"));

        if (Boolean.getBoolean("test.security.enabled"))
            {
            String sRootDir = getRootDir().getAbsolutePath();

            optionsByType.add(SystemProperty.of("java.security.debug", System.getProperty("test.security.debug")));
            optionsByType.add(SystemProperty.of("java.security.manager", ""));
            optionsByType.add(SystemProperty.of("java.security.policy", "file:" + sRootDir + "/prj/test/test-security.policy"));
            optionsByType.add(SystemProperty.of("oracle.coherence.lib", System.getProperty("oracle.coherence.lib", sRootDir + "/coherence/target")));
            optionsByType.add(SystemProperty.of("test.project.version", System.getProperty("project.version")));
            optionsByType.add(SystemProperty.of("test.tmp.dir",System.getProperty("java.io.tmpdir")));
            optionsByType.add(SystemProperty.of("sun.net.maxDatagramSockets", "1024"));
            }

        return optionsByType;
        }

    /**
    * Stop the specified cache application.
    *
    * @param sApplication  the name of the cache server that was specified
    *                      when it was started
    */
    protected static void stopCacheApplication(String sApplication)
        {
        out(">>>>>>> Stopping cache application: " + sApplication);
        stopProcess(sApplication, false);
        out(">>>>>>> Stopped cache application: " + sApplication);
        }

    private static void stopProcess(String sApplication, boolean fGraceful)
        {
        if (sApplication == null || sApplication.length() == 0)
              {
              throw new IllegalArgumentException("Missing required application name");
              }

          CoherenceClusterMember member = m_mapApplications.remove(sApplication);
          if (member != null && isAlive(member))
              {
              Cluster        cluster    = CacheFactory.getCluster();
              boolean        fInCluster = cluster.isRunning();
              RemoteRunnable runnable   = fGraceful ? new ClusterShutdownAndExit() : new RuntimeHalt();

              int nId;
              if (fInCluster)
                  {
                  Eventually.assertThat(invoking(m_helper).getLocalMemberId(member), Matchers.greaterThan(0));
                  nId = member.getLocalMemberId();
                  Eventually.assertThat(invoking(m_helper).matchMemberId(cluster, nId), Is.is(true));
                  }
              else
                  {
                  nId = -1;
                  }

              try
                  {
                  // if we're running code coverage ensure the metrics are dumped
                  try
                      {
                      CompletableFuture<Void> future = member.submit(new Dump());
                      future.join();
                      }
                  catch (Throwable t)
                      {
                      // ignored, we may well get an exception if the process is
                      // already dead or if Jacoco is not enabled.
                      }

                  member.submit(runnable);
                  try
                      {
                      member.waitFor();
                      }
                  catch(Throwable _ignore)
                      {
                      // ignored
                      }
                  }
              catch (IllegalStateException e)
                  {
                  // remote member has already exited
                  out("Failed to submit " + runnable.getClass().getName() +
                      " to application " + sApplication + " due to exception: " +
                      e.getMessage() + "\n" + getStackTrace(e));

                  return;
                  }
              finally
                  {
                  // ensure that the member is released
                  member.close();
                  }

              if (fInCluster)
                  {
                  Eventually.assertThat(invoking(cluster).getMemberSet().stream().mapToInt(m -> ((Member) m).getId())
                          .anyMatch(n -> n == nId), Is.is(false));
                  }
              }
		  }

    /**
    * Return true if the provided {@link CoherenceClusterMember member} is
    * alive (process is still running).
    *
    * @param member  the member to check
    *
    * @return whether the process is alive
    *
    * @see Process#isAlive()
    */
    private static boolean isAlive(CoherenceClusterMember member)
        {
        try
            {
            member.exitValue();
            return false;
            }
        catch (IllegalThreadStateException e)
            {
            return true;
            }
        }

    /**
    * Stop the specified cache server.
    *
    * @param sServer   the name of the cache server that was specified when
    *                  it was started
    */
    protected static void stopCacheServer(String sServer)
        {
        stopCacheServer(sServer, false);
        }

    /**
    * Stop the specified cache server.
    *
    * @param sServer    the name of the cache server that was specified when
    *                   it was started
    * @param fGraceful  if true, a "graceful" shutdown of the cache server
    */
    protected static void stopCacheServer(String sServer, boolean fGraceful)
        {
        out(createMessageHeader() + " >>>>>>> Stopping cache server: " + sServer + (fGraceful ? " gracefully" : ""));

        stopProcess(sServer, fGraceful);

        out(createMessageHeader() + " >>>>>>> Stopped cache server: " + sServer);
        }

    /**
    * Wait until the specified (partitioned) cache service becomes "balanced".
    *
    * @param service   the partitioned cache to wait for balancing
    */
    public static void waitForBalanced(CacheService service)
        {
        SafeService serviceSafe = (SafeService) service;
        PartitionedCache serviceReal = (PartitionedCache) serviceSafe.getService();

        Eventually.assertThat(invoking(serviceReal).calculateUnbalanced(), is(0));
        }

    /**
    * Return the root directory of the development line in use (e.g.
    * /dev/main).
    *
    * @return the root directory of the development line in use
    */
    public static File getRootDir()
        {
        String sPath = System.getProperty("test.root.dir");

        System.out.println("test.root.dir: " + sPath);
        if (sPath == null || sPath.isEmpty())
            {
            sPath = AbstractTestInfrastructure.class.getProtectionDomain()
                    .getCodeSource().getLocation().getPath().trim();
            if (sPath.endsWith("/"))
                {
                sPath = sPath.substring(0, sPath.length() - 1);
                }

            int iLast = sPath.lastIndexOf('/');
            if (iLast != -1)
                {
                sPath = sPath.substring(0, iLast);
                }

            // on windows, spaces in the path are represented by "%20"
            if (File.pathSeparatorChar == ';')
                {
                sPath = sPath.replaceAll("%20", " ");
                }
            }
        else
            {
            File dir = new File(sPath);
            if (dir.getName().equals("prj"))
                {
                dir = dir.getParentFile();
                }

            System.out.println("test.root.dir returned: " + dir.getAbsolutePath());
            return dir;
            }

        System.out.println("sPath: " + sPath);

        File dir = new File(sPath);
        while (dir != null && !dir.getName().equals("prj"))
            {
            dir = dir.getParentFile();
            }

        if (dir != null)
            {
            dir = dir.getParentFile();
            }

        if (dir != null && dir.exists() && dir.isDirectory())
            {
            return dir;
            }
        else
            {
            throw new RuntimeException(
                "Failed to retrieve a valid root directory: " + sPath);
            }
        }

    /**
    * Return the directory of the specified test project.
    *
    * @param sProject  the test project to return the directory for
    *
    * @return the directory of the specified test project
    */
    public static File getProjectDir(String sProject)
        {
        if (sProject == null || sProject.length() == 0)
            {
            throw new IllegalArgumentException("Missing required project name");
            }

        String sPath = System.getProperty("test.project.dir");

        File dir = new File(getRootDir(), Paths.get("prj", "test", sProject).toString());
        if (sPath == null || sPath.length() ==0)
            {
            File dirTest = new File(getRootDir(), Paths.get("prj", "test").toString());
            for (File dirCategory : dirTest.listFiles(File::isDirectory))
                {
                File dirCheck = new File(dirCategory, sProject);
                if (dirCheck.isDirectory())
                    {
                    dir = dirCheck;
                    break;
                    }
                }
            }
        else
            {
            dir = new File(sPath);
            }

       if (!dir.exists() || !dir.isDirectory())
            {
            throw new IllegalArgumentException("Invalid project: " + dir);
            }

        return dir;
        }

    /**
    * Return the output directory of the specified test project, creating it
    * if it doesn't already exist.
    *
    * @param sProject  the test project to return the output directory for
    *
    * @return the output directory of the specified test project
    */
    public static File ensureOutputDir(String sProject)
        {
        if (sProject == null || sProject.length() == 0)
            {
            throw new IllegalArgumentException("Missing required project name");
            }

        File dir = new File(getProjectDir(sProject), "target/test-output/functional");
        if (dir.exists())
            {
            if (!dir.isDirectory())
                {
                throw new IllegalArgumentException("Invalid output dir: " + dir);
                }
            }
        else
            {
            if (!dir.mkdirs())
                {
                throw new IllegalStateException("Unable to create output dir: " + dir);
                }
            }

        return dir;
        }

    /**
    * Add the system properties that begin with the specified prefix to the
    * given properties.
    *
    * @param sPrefix  the prefix of the system properties to add
    * @param props    the Properties object to add the matched system
    *                 properties to
    */
    protected static void addSystemProperties(String sPrefix, Properties props)
        {
        for (Map.Entry entry : System.getProperties().entrySet())
            {
            String sKey   = (String) entry.getKey();
            String sValue = (String) entry.getValue();
            if ((sPrefix == null || sKey.startsWith(sPrefix)) && !props.containsKey(sKey))
                {
                props.setProperty(sKey, sValue);
                }
            }
        }

    /**
    * Add the {@link System#getProperties()} that begin with the specified prefix
    * as {@link SystemProperty}s to the {@link OptionsByType} if they don't already exist.
    *
    * @param sPrefix        the prefix of the system properties to add
    * @param optionsByType  the {@link OptionsByType} to which the {@link SystemProperty}s
     *                      will be added
    */
    protected static void addSystemProperties(String sPrefix, OptionsByType optionsByType)
        {
        SystemProperties systemProperties = optionsByType.get(SystemProperties.class);

        for (Map.Entry entry : System.getProperties().entrySet())
            {
            String sKey   = (String) entry.getKey();
            if ((sPrefix == null || sKey.startsWith(sPrefix)) && !systemProperties.contains(sKey))
                {
                systemProperties = systemProperties.add(SystemProperty.of(sKey, entry.getValue()));
                }
            }

        optionsByType.add(systemProperties);
        }

    /**
     * Create a copy of the specified {@link Properties}. If
     * the Properties parameter is null an empty Properties
     * instance will be returned.
     *
     * @param props  the Properties to copy
     *
     * @return a Properties instance containing all of the
     *         property key/value pairs from the Property
     *         argument
     */
    protected static Properties copyProperties(Properties props)
        {
        Properties propsCopy = new Properties();
        if (props != null)
            {
            propsCopy.putAll(props);
            }

        return propsCopy;
        }

    /**
    * Add the "standard" test-related system properties to the given
    * {@link OptionsByType}.
    *
    * @param optionsByType  the {@link OptionsByType} to add the properties to
    */
    protected static void addTestProperties(OptionsByType optionsByType)
        {
        // add "standard" system properties
        addSystemProperties("com.oracle.common.", optionsByType);
        addSystemProperties("emma.",              optionsByType);
        addSystemProperties("java.net.",          optionsByType);
        addSystemProperties("java.security.",     optionsByType);
        addSystemProperties("javax.net.",         optionsByType);
        addSystemProperties("tangosol.",          optionsByType);
        addSystemProperties("test.",              optionsByType);
        addSystemProperties("coherence.",         optionsByType);
        }

    /**
    * Add the "standard" test-related system properties to the given
    * properties.
    *
    * @param props  the Properties object to add the properties to
    */
    protected static void addTestProperties(Properties props)
        {
        // add "standard" system properties
        addSystemProperties("com.oracle.common.", props);
        addSystemProperties("emma.",              props);
        addSystemProperties("java.net.",          props);
        addSystemProperties("java.security.",     props);
        addSystemProperties("javax.net.",         props);
        addSystemProperties("tangosol.",          props);
        addSystemProperties("test.",              props);
        addSystemProperties("coherence.",         props);
        }

    /**
    * Verify that the two key sets are equal.
    * <p/>
    * Return value is only used during debugging to go into an infinite loop
    * in case of a failure.
    *
    * @param setResult    the result key set
    * @param setTemplate  the key set to compare against
    *
    * @return true if the two key sets are equal, false otherwise
    */
    protected static boolean assertEqualKeySet(Set setResult, Set setTemplate)
        {
        return assertEqualKeySet(setResult, setTemplate, false);
        }

    /**
    * Verify that the two key sets are equal.
    * <p/>
    * Return value is only used during debugging to go into an infinite loop
    * in case of a failure.
    *
    * @param setResult    the result key set
    * @param setTemplate  the key set to compare against
    * @param fDebug       indicate if debug is true or false
    *
    * @return true if the two key sets are equal, false otherwise
    */
    protected static boolean assertEqualKeySet(Set setResult, Set setTemplate, boolean fDebug)
        {
        if (setTemplate.size() != setResult.size())
            {
            if (fDebug)
                {
                String sMsg = "Different size: result=" +
                setResult.size() + "; template=" + setTemplate.size();
                CacheFactory.log(sMsg, 1);
                return false;
                }
            err("Different size: result=\n" + setResult +
                "\ntemplate=\n" + setTemplate);
            fail("Different size: result=" + setResult.size() +
                 "; template=" + setTemplate.size());
            }

        // clone the input sets since we need to mutate them
        // in order to determine which keys are missing/extra
        Set setR = new HashSet(setResult);
        Set setT = new HashSet(setTemplate);

        if (!setR.equals(setT))
            {
            for (Iterator iterR = setR.iterator(); iterR.hasNext();)
                {
                Object o = iterR.next();
                if (setT.contains(o))
                    {
                    iterR.remove();
                    setT.remove(o);
                    }
                }
            if (setR.isEmpty())
                {
                if (fDebug)
                    {
                    String sMsg = "Missing results " + setT;
                    CacheFactory.log(sMsg, 1);
                    return false;
                    }
                fail("Missing results " + setT);
                }
            else
                {
                if (fDebug)
                    {
                    String sMsg = "Missing results " + setR;
                    CacheFactory.log(sMsg, 1);
                    return false;
                    }
                fail("Extra results " + setR);
                }
            }
        return true;
        }

    /**
    * Verify that the two entry sets are equal
    *
    * @param setResult    the result entry set
    * @param setTemplate  the entry set to compare against
    *
    * @return true if the two entry sets are equal, false otherwise
    */
    protected static boolean assertEqualEntrySet(Set setResult, Set setTemplate)
        {
        return assertEqualEntrySet(setResult, setTemplate, false);
        }

    /**
    * Verify that the two entry sets are equal
    *
    * @param setResult    the result entry set
    * @param setTemplate  the entry set to compare against
    * @param fDebug       indicate if debug is true or false
    *
    * @return true if the two entry sets are equal, false otherwise
    */
    protected static boolean assertEqualEntrySet(Set setResult, Set setTemplate, boolean fDebug)
        {
        if (setTemplate.size() != setResult.size())
            {
            if (fDebug)
                {
                String sMsg = "Different size: result=" +
                setResult.size() + "; template=" + setTemplate.size();
                CacheFactory.log(sMsg, 1);
                return false;
                }
            err("Different size: result=\n" + setResult +
                "\ntemplate=\n" + setTemplate + "\nat " + getStackTrace());
            fail("Different size: result=" + setResult.size() +
                 "; template=" + setTemplate.size());
            }

        // clone the input sets since we need to mutate them
        // in order to determine which entries are missing/extra
        Map mapR = new HashMap();
        Map mapT = new HashMap();
        if (!setResult.isEmpty())
            {
            Iterator iterR = setResult.iterator();
            Iterator iterT = setTemplate.iterator();
            while (iterR.hasNext())
                {
                Map.Entry entryR = (Map.Entry) iterR.next();
                Map.Entry entryT = (Map.Entry) iterT.next();

                mapR.put(entryR.getKey(), entryR.getValue());
                mapT.put(entryT.getKey(), entryT.getValue());
                }
            assertThat(iterT.hasNext(), is(false));
            }

        if (!mapR.equals(mapT))
            {
            for (Iterator iterR = mapR.keySet().iterator(); iterR.hasNext();)
                {
                Object oKey = iterR.next();

                if (mapT.containsKey(oKey) &&
                        equals(mapR.get(oKey), mapT.get(oKey)))
                    {
                    iterR.remove();
                    mapT.remove(oKey);
                    }
                }
            if (mapR.isEmpty())
                {
                if (fDebug)
                    {
                    String sMsg = "Missing results " + mapT;
                    CacheFactory.log(sMsg, 1);
                    return false;
                    }
                fail("Missing results " + mapT);
                }
            else
                {
                if (fDebug)
                    {
                    String sMsg = "Missing results " + mapR;
                    CacheFactory.log(sMsg, 1);
                    return false;
                    }
                fail("Extra results " + mapR);
                }
            }
        return true;
        }

    /**
    * Verify that the two key sets are complementary
    *
    * @param setKeys1  the first key set
    * @param setKeys2  the second key set
    * @param map       the complete key set map
    */
    protected static void assertComplementaryKeySet(Set setKeys1, Set setKeys2, Map map)
        {
        assertThat("Invalid keySet sizes: " + setKeys1.size() +
                           " + " + setKeys2.size() + " != " + map.size(),
                   map.size(), is(setKeys1.size() + setKeys2.size())
        );

        Set setResult = new HashSet(setKeys1);
        setResult.addAll(setKeys2);
        assertEqualKeySet(setResult, map.keySet());
        }

    /**
    * Verify that the two entry sets are complementary
    *
    * @param setEntries1  the first entry set
    * @param setEntries2  the second entry set
    * @param map          the complete entry set map
    */
    protected static void assertComplementaryEntrySet(Set setEntries1, Set setEntries2, Map map)
        {
        assertThat("Invalid entrySet sizes: " + setEntries1.size() +
                           " + " + setEntries2.size() + "!=" + map.size(),
                   map.size(), is(setEntries1.size() + setEntries2.size())
        );

        Map mapResult = new HashMap();
        for (Iterator iter = setEntries1.iterator(); iter.hasNext();)
            {
            Map.Entry entry = (Map.Entry) iter.next();
            mapResult.put(entry.getKey(), entry.getValue());
            }
        for (Iterator iter = setEntries2.iterator(); iter.hasNext();)
            {
            Map.Entry entry = (Map.Entry) iter.next();
            mapResult.put(entry.getKey(), entry.getValue());
            }

        assertEqualEntrySet(mapResult.entrySet(), map.entrySet());
        }

    /**
     * Asserts that a service was named correctly given a scope name
     *
     * @param sServiceName  the service name requested, either directly via
     *                      {@link ConfigurableCacheFactory#ensureService(String)}
     *                      or {@link ConfigurableCacheFactory#ensureCache(String, ClassLoader, NamedCache.Option[])}
     * @param sScopeName    the scope name provided to {@link ConfigurableCacheFactory}
     * @param service       the service created by {@link ConfigurableCacheFactory}
     */
    protected static void assertServiceName(String sServiceName, String sScopeName, Service service)
        {
        assertThat("Expected service to be instance of SafeService",
                   service, is(instanceOf(SafeService.class)));

        SafeService safeService          = (SafeService) service;
        String      sExpectedServiceName = sScopeName == null ? sServiceName :
                sScopeName + ":" + sServiceName;

        assertThat("Expected service name to be " + sExpectedServiceName,
                   sExpectedServiceName, is(safeService.getServiceName()));
        }

    /**
     * Generate a message header.
     */
    public static String createMessageHeader()
        {
        return new Timestamp(System.currentTimeMillis())
            + " (" + Thread.currentThread() + ')';
        }

    // ----- accessors ------------------------------------------------------

    /**
    * Return the resource name or file path of the cache configuration file
    * used by this test class.
    *
    * @return the resource name or file path of the cache configuration
    */
    public String getCacheConfigPath()
        {
        return m_sCacheConfigPath;
        }

    /**
     * Obtain a ConfigurableCacheFactory that can be used to retrieve a
     * NamedCache instances.
     *
     * @return  a ConfigurableCacheFactory that uses the cache configuration
     *          returned by {@link #getCacheConfigPath()}.
     */
     public ConfigurableCacheFactory getFactory()
         {
         return CacheFactory.VERSION.startsWith("3.4") ? getDccfFactory()
                 : getCcfFactory(!CacheFactory.VERSION.startsWith("3."));
         }

   /**
    * Obtain a DefaultConfigurableCacheFactory that can be used to retrieve
    * NamedCache instances.  This is needed to support Extend-34 tests.
    *
    * @return  a ConfigurableCacheFactory that uses the cache configuration
    *          returned by {@link #getCacheConfigPath()}.
    */
    private synchronized ConfigurableCacheFactory getDccfFactory()
        {
        ConfigurableCacheFactory factory = m_factory;

        if (factory == null)
            {
            m_factory = factory =
                new DefaultConfigurableCacheFactory(getCacheConfigPath(), getClass().getClassLoader());
            }

        return factory;
        }

    /**
    * Using a CacheFactoryBuilder, obtain a ConfigurableCacheFactory  that can be used
    * to retrieve NamedCache instances.
    *
    * @param fDefault  whether the realized CCF should be made the default
    *
    * @return  a ConfigurableCacheFactory that uses the cache configuration
    *          returned by {@link #getCacheConfigPath()}
    */
    private synchronized ConfigurableCacheFactory getCcfFactory(boolean fDefault)
        {
        ConfigurableCacheFactory factory = m_factory;

        if (factory == null)
            {
            CacheFactoryBuilder bldrCCF = CacheFactory.getCacheFactoryBuilder();

            m_factory = factory = bldrCCF.getConfigurableCacheFactory(
                    getCacheConfigPath(), getClass().getClassLoader());

            if (fDefault)
                {
                bldrCCF.setConfigurableCacheFactory(factory, "$Default$", getContextClassLoader(), false);
                }
            }

        return factory;
        }

    /**
    * Specify the ConfigurableCacheFactory that should be used to retrieve
    * NamedCache instances.
    *
    * @param  factory ConfigurableCacheFactory to be used
    */
    public synchronized void setFactory(ConfigurableCacheFactory factory)
        {
        m_factory = factory;
        }

    public static AvailablePortIterator getAvailablePorts()
        {
        if (m_ports == null)
            {
            m_ports = LocalPlatform.get().getAvailablePorts();
            }

        return m_ports;
        }

    // ----- data members ---------------------------------------------------

    /**
    * The resource name or file path of the cache configuration file used by
    * this test class.
    */
    private String m_sCacheConfigPath;

    /**
    * The ConfigurableCacheFactory used by this test class to retrieve
    * NamedCache instances.
    */
    private ConfigurableCacheFactory m_factory;

    /**
    * The Map of Applications started by this test.
    */
    private static final Map<String,CoherenceClusterMember> m_mapApplications = new ConcurrentHashMap<>();

    /**
    * The available ports that may be used by processes.
    */
    private static AvailablePortIterator m_ports;

    /**
    * A {@link TestInfrastructureHelper} instance that we can pass to Bedrock on an invoking().
    */
    protected static TestInfrastructureHelper m_helper = new TestInfrastructureHelper();

    /**
     * A {@link ClassRule} to isolate system properties set between test class
     * execution (not individual test method executions).
     */
    @ClassRule
    public static SystemPropertyIsolation s_systemPropertyIsolation = new SystemPropertyIsolation();

    @Rule
    public TestName m_testName = new TestName();
    }
