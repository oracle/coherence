/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.performance;

import com.oracle.bedrock.runtime.coherence.CoherenceCacheServer;
import com.oracle.bedrock.runtime.coherence.CoherenceCluster;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.coherence.ServiceStatus;
import com.oracle.bedrock.runtime.concurrent.RemoteCallable;
import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.util.Pair;
import com.tangosol.coherence.performance.psr.Console;
import com.tangosol.coherence.performance.psr.ConsoleExtended;
import com.tangosol.coherence.performance.psr.TestResult;
import com.tangosol.run.xml.SimpleElement;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.util.Base;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static com.oracle.bedrock.deferred.DeferredHelper.within;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;


/**
 * @author jk 2015.11.25
 */
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(CustomParameterizedRunner.Factory.class)
public abstract class AbstractPerformanceTests
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a {@link AbstractPerformanceTests} instance that will runn tests
     * with the specified {@link PsrPerformanceEnvironment}.
     *
     * @param description  the description of the environment
     * @param environment  the {@link PsrPerformanceEnvironment} to use to run the tests
     */
    public AbstractPerformanceTests(String description, PsrPerformanceEnvironment environment)
        {
        f_sDescription = description;
        f_environment = environment;

        String sConfig = System.getProperty("test.client.cache.config", "client-cache-config-java-WithNearCache-4gx4.xml");
        environment.withClientConfiguration(sConfig);
        }

    // ----- test lifecycle methods -----------------------------------------

    /**
     * Create the {@link Collection} of test scenarios.
     *
     * @return  the {@link Collection} of test scenarios
     *
     * @throws Exception if there is an error
     */
    @SuppressWarnings("rawtypes")
    @Parameterized.Parameters(name = "desc={0}")
    public static Collection<Object[]> getTestParameters() throws Exception
        {
        List<Pair<String,PsrPerformanceEnvironment>> parameters;

        switch(System.getProperty("test.type", "local").toLowerCase())
            {
            case "local-current":
                parameters = getCurrentVersionScenarios();
                break;
            case "remote":
                parameters = getAllScenarios(PsrRemoteEnvironment::new);
                break;
            default:
                parameters = getAllScenarios(PsrPerformanceEnvironment::new);
            }


        if (parameters.size() > 0)
            {
            PsrPerformanceEnvironment environment = parameters.get(0).getY();
            List<File>             listLibDirs = getCoherenceJars();

            environment.deploy(listLibDirs);
            }

        System.err.println(DASHES);
        System.err.println("Test Parameters:");
        parameters.forEach((params) -> System.err.println(params.getX()));
        System.err.println(DASHES + '\n');

        return parameters.stream()
                .map((pair) -> new Object[]{pair.getX(), pair.getY()})
                .collect(Collectors.toList());
        }

    /**
     * Set-up the test environment, create output folders etc.
     */
    @BeforeClass
    public static void setup() throws Exception
        {
        File fileBuild      = PsrPerformanceEnvironment.getBuildFolder();
        File fileTests      = new File(fileBuild, "test-output");
        File fileFunctional = new File(fileTests, "functional");

        s_fileResultsFolder = new File(fileFunctional, "results");
        s_fileSnapshots     = new File(fileFunctional, "snapshots");

        if (!s_fileResultsFolder.exists())
            {
            assertThat(s_fileResultsFolder.mkdirs(), is(true));
            }

        if (!s_fileSnapshots.exists())
            {
            assertThat(s_fileSnapshots.mkdirs(), is(true));
            }
        }


    /**
     * Write the test results to an XML file.
     */
    @AfterClass
    public static void writeResults() throws Exception
        {
        for (Map.Entry<String,Map<String,Object>> entryTest : s_mapResults.entrySet())
            {
            String        sTestName      = entryTest.getKey();
            File          fileXmlResults = new File(s_fileResultsFolder, sTestName + ".xml");
            SimpleElement xmlResults     = new SimpleElement("results");

            writeResults(xmlResults, entryTest.getValue());

            try(PrintWriter writer = new PrintWriter(fileXmlResults))
                {
                writer.println(xmlResults.toString());
                }
            }
        }

    private static void writeResults(XmlElement parent, Map<String,?> map)
        {
        for (Map.Entry<String,?> entry : map.entrySet())
            {
            try
                {
                Object oValue = entry.getValue();

                if (oValue instanceof Map)
                    {
                    writeResults(parent.addElement(entry.getKey()), (Map) oValue);
                    }
                else if (oValue instanceof TestResult)
                    {
                    String     sVersion    = entry.getKey();
                    TestResult result      = (TestResult) oValue;
                    long       nOperations = result.getRate();

                    parent.addElement("v" + sVersion).setLong(nOperations);
                    }
                }
            catch (Exception e)
                {
                e.printStackTrace();
                }
            }
        }


    /**
     * Start the client test runners before each test.
     */
    @Before
    public void setupTest() throws Exception
        {
        f_environment.startClients();
        }


    /**
     * Stop the client test runners after each test.
     */
    @After
    public void cleanup() throws Exception
        {
        f_environment.closeRunners();
        }


    /**
     * Before each parameter run set up the test environment,
     * start the Console process and start the Coherence cluster.
     */
    @CustomParameterizedRunner.BeforeParmeterizedRun
    public void beforeParameters() throws Throwable
        {
        System.err.println(DASHES);
        System.err.println("Starting tests for " + f_sDescription);
        System.err.println(DASHES);

        f_environment.withTestName(getClass().getSimpleName())
                .withOutputFolder(f_sDescription)
                .before();

        String[]                         hosts   = f_environment.getClusterHostNames();
        CoherenceCluster                 cluster = f_environment.getCluster();
        Iterable<CoherenceClusterMember> members = cluster.getAll(member -> member.getName().startsWith("Data"));
        ServiceStatus status  = hosts.length == 1 ? ServiceStatus.NODE_SAFE : ServiceStatus.MACHINE_SAFE;


        for (CoherenceClusterMember member : members)
            {
            System.err.println("Waiting for " + member.getName() + " to be balanced");
            Eventually.assertThat(invoking(this).isBalanced(member, status), is(true), within(5, TimeUnit.MINUTES));
            }
        System.err.println("All storage members balanced.");

        System.err.println("Waiting for Console to be ready...");
        Eventually.assertThat(invoking(f_environment).isConsoleReady(), is(true));
        }

    public boolean isBalanced(CoherenceClusterMember member, ServiceStatus status) throws Exception
        {
        return member.submit(new IsBalanced(status)).get(1, TimeUnit.MINUTES);
        }


    @CustomParameterizedRunner.AfterParmeterizedRun
    public void afterParameters() throws Throwable
        {
        f_environment.after();
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Run a specific test script.
     *
     * @param asCommands  the commands to run
     *
     * @throws Exception  if an error occurs
     */
    public void runCommands(String... asCommands)
            throws Exception
        {
        String               sTestClass   = getClass().getSimpleName();
        String               sMethodName  = m_testName.getMethodName();
        int                  nIndex       = sMethodName.indexOf('[');
        String               sTestName    = sTestClass + '-' + ((nIndex > 0) ? sMethodName.substring(0, nIndex) : sMethodName);
        CoherenceCacheServer console      = f_environment.getConsole();

        for (String sCommand : asCommands)
            {
            System.err.println("[" + sTestName + "] Executing Console Command: " + sCommand);
            console.submit(new ConsoleExtended.RunCommand(sCommand));
            }
        }

    /**
     * Run the specific test command and capture the result.
     *
     * @param sCommand  the command to run to perform the test
     *
     * @throws Exception  if an error occurs
     */
    public void runTest(String sCommand)
            throws Exception
        {
        String                 sTestClass   = getClass().getSimpleName();
        String                 sMethodName  = m_testName.getMethodName();
        int                    nIndex       = sMethodName.indexOf('[');
        String                 sTestName    = sTestClass + '-' + ((nIndex > 0) ? sMethodName.substring(0, nIndex) : sMethodName);
        CoherenceCacheServer   console      = f_environment.getConsole();
        String                 sVersion     = f_environment.getClusterCoherenceVersion();
        String                 sConfig      = f_environment.getClusterConfig();
        String                 sTransport   = f_environment.getClusterTransport();

        System.err.println("[" + sTestName + "] Executing Test: " + sCommand);

        if (sTransport == null)
            {
            sTransport = "default";
            }

        TestResult result = console.submit(new ConsoleExtended.RunCommand(sCommand, true)).get();

        if (result != null && result.getFailureCount() == 0L)
            {
            File   fileResults = ensureResultsFile(sTestName);
            String sLine       = sVersion + '-' + sTransport + '-' + sConfig + '=' + result.getRate();

            AppendToFile.appendToFile(fileResults, sLine);
            }

        processResults(result);
        }

    protected void processResults(TestResult result)
        {
        String                 sTestClass   = getClass().getSimpleName();
        String                 sMethodName  = m_testName.getMethodName();
        int                    nIndex       = sMethodName.indexOf('[');
        String                 sTestName    = sTestClass + '-' + ((nIndex > 0) ? sMethodName.substring(0, nIndex) : sMethodName);
        String                 sVersion     = f_environment.getClusterCoherenceVersionWithoutPatch();

        System.err.println("Total Results:");
        printResults(result);

        assertThat("Result failure count was greater than 0", result.getFailureCount(), is(0L));

        Map<String,Object> mapTest = s_mapResults.computeIfAbsent(sTestName, (k) -> new HashMap<>());

        for (String key : f_environment.getKeys())
            {
            mapTest = (Map<String, Object>) mapTest.computeIfAbsent(key, (k) -> new HashMap<>());
            }

        mapTest.put(sVersion, result);
        }

    public TestResult submitTest(String sTestName, RemoteCallable<TestResult> callable)
        {
        return submitTest(sTestName, callable, 10, TimeUnit.MINUTES);
        }


    public TestResult submitTest(String sTestName, RemoteCallable<TestResult> callable, long timeout, TimeUnit units)
        {
        startJFR(sTestName);

        TestResult       result            = new TestResult();
        List<TestResult> listClientResults = new ArrayList<>();

        f_environment.submitTest(callable, timeout, units, result, listClientResults);

        stopAndCaptureJFR(sTestName);

        if (listClientResults.size() > 0)
            {
            System.err.println(DASHES);
            System.err.println("Client Results:");

            for (TestResult resultClient : listClientResults)
                {
                printResults(resultClient);
                }
            }

        return result;
        }


    protected synchronized File ensureResultsFile(String sTestName)
        {
        if (m_fileResults == null)
            {
            String sPrefix = sTestName + "-Results";
            String sSuffix = ".properties";

            m_fileResults = new File(s_fileResultsFolder, sPrefix + sSuffix);

            }
        return m_fileResults;
        }

    /**
     * Start Java Flight Recorder on the test processes
     */
    protected void startJFR(String testName)
        {
        if (f_environment.isUsingJFR())
            {
            f_environment.getCluster().forEach((member) -> member.submit(new JFRStart(testName + member.getName())));
            }

        if (f_environment.isUsingJFRClients())
            {
            f_environment.getClients().forEach((member) -> member.submit(new JFRStart(testName + member.getName())));
            }
        }

    /**
     * Start Java Flight Recorder on the test processes
     */
    protected void stopAndCaptureJFR(String sTestName)
        {
        try
            {
            File outputFolder = f_environment.getOutputFolder();

            if (f_environment.isUsingJFR())
                {
                f_environment.getCluster().forEach((member) ->
                   {
                    String sRecording      = sTestName + member.getName();
                   String sRemoteFileName = null;
                   try
                       {
                       sRemoteFileName = member.submit(new JFRStop(sRecording)).get();
                       }
                   catch (InterruptedException | ExecutionException e)
                       {
                       throw Base.ensureRuntimeException(e);
                       }

                   JFRGetRecording.getRecording(member, sRemoteFileName, new File(outputFolder, member.getName() + ".jfr"));
                    });
                }

            if (f_environment.isUsingJFRClients())
                {
                f_environment.getClients().forEach((member) ->
                   {
                   try
                       {
                       String sRecording      = sTestName + member.getName();
                       String sRemoteFileName = member.submit(new JFRStop(sRecording)).get();

                       JFRGetRecording.getRecording(member, sRemoteFileName, new File(outputFolder, member.getName() + ".jfr"));
                       }
                   catch (InterruptedException | ExecutionException e)
                       {
                       throw Base.ensureRuntimeException(e);
                       }
                   });
                }
            }
        catch (Exception e)
            {
            e.printStackTrace();
            }
        }

    public void printResults(TestResult result)
        {
        String sTest = getClass().getSimpleName();

        System.err.println(DASHES);
        System.err.println(sTest + ' ' + f_sDescription);
        System.err.println("Duration:              " + result.getDuration() + "ms");
        System.err.println("Successful Operations: " + result.getSuccessCount());
        System.err.println("Failed Operations:     " + result.getFailureCount());
        System.err.println("Total Rate:            " + result.getRate() + "ops");
        System.err.println("Total Throughput:      " + Console.toBandwidthString(result.getThroughput(), false));
        System.err.println("Latency:               " + result.getLatency());
        System.err.println(DASHES);
        }


    protected static List<Pair<String,PsrPerformanceEnvironment>> getCurrentVersionScenarios() throws Exception
        {
        String     sVersion     = new GetCoherenceVersion().call();
        File       fileCurrent  = new File(sVersion);
        List<File> listLibDirs  = Collections.singletonList(fileCurrent);

        return s_functionScenarios.apply(PsrCurrentEnvironment::new, listLibDirs);
        }


    protected static List<Pair<String,PsrPerformanceEnvironment>> getAllScenarios(Supplier<PsrPerformanceEnvironment> supplierEnvironment) throws Exception
        {
        List<File> listLibDirs = getCoherenceJars();

        return s_functionScenarios.apply(supplierEnvironment, listLibDirs);
        }


    protected static List<Pair<String,PsrPerformanceEnvironment>> getAllScenarios(Supplier<PsrPerformanceEnvironment> supplierEnvironment, List<File> listLibDirs)
        {
        try
            {
            String[] asTransports = System.getProperty("test.transports", "default").toLowerCase().split(",");

            return getAllScenarios(supplierEnvironment, listLibDirs, asTransports);
            }
        catch (Exception e)
            {
            throw new RuntimeException(e);
            }
        }


    protected static List<Pair<String,PsrPerformanceEnvironment>> getAllScenarios(Supplier<PsrPerformanceEnvironment> supplierEnvironment,
                                                                                  List<File> listLibDirs, String[] asTransports) throws Exception
        {
        List<Pair<String,PsrPerformanceEnvironment>> listParameters = new ArrayList<>();
        String[]                                  asConfigs      = System.getProperty("test.cache.configs", "server-cache-config-one-worker-thread.xml").split(",");


        for (File file : listLibDirs)
            {
            file = modifyCoherenceLocation(file);

            for (String sConfig : asConfigs)
                {
                for (String sTransport : asTransports)
                    {
                    sTransport = sTransport.equals("default") ? null : sTransport;


                    listParameters.addAll(createScenarios(supplierEnvironment, file, sTransport, sConfig));
                    }
                }
            }

        return listParameters;
        }


    protected static List<Pair<String,PsrPerformanceEnvironment>> createScenarios(Supplier<PsrPerformanceEnvironment> supplierEnvironment,
                                                                                  File fileVersion, String sTransport, String sClusterConfig)
        {
        String sVersion = fileVersion.getParentFile().getName();

        String sDescription = sVersion + "_"
                + (sTransport != null ? sTransport : "default")
                + '_' + sClusterConfig;

        PsrPerformanceEnvironment environment = createEnvironment(supplierEnvironment, fileVersion, sTransport, sClusterConfig);

        return Collections.singletonList(new Pair<>(sDescription, environment));
        }


    public static File modifyCoherenceLocation(File file)
        {
        String sFolder = System.getProperty("test.lib.folder");

        if (sFolder != null && sFolder.length() > 0)
            {
            return new File(sFolder + File.separator + "lib" + File.separator
                                    + file.getName() + File.separator + "coherence.jar");
            }

        return new File(file, "coherence.jar");
        }

    protected static PsrPerformanceEnvironment createEnvironment(
            Supplier<PsrPerformanceEnvironment> supplierEnvironment,
            File fileJar, String sTransport, String sClusterConfig)
        {
        PsrPerformanceEnvironment environment = supplierEnvironment.get()
                .withCoherenceJarURI(fileJar.toURI());

        if (sTransport != null)
            {
            environment.withClusterTransport(sTransport);
            }

        String key = sTransport == null ? "default" : sTransport;

        environment.withKeys(key);

        environment.withClusterConfiugration(sClusterConfig);

        return environment;
        }


    protected static List<File> getCoherenceJars()
            throws Exception
        {
        File   fileBuild = PsrPerformanceEnvironment.getBuildFolder();
        File   fileLibs  = new File(fileBuild, "lib");
        File[] aLibDirs  = fileLibs.listFiles();

        assertThat("Cannot locate the lib folder containing Coherence jars to test", aLibDirs, is(notNullValue()));

        String       sVersions    = System.getProperty("test.versions");
        List<String> listVersions = new ArrayList<>();

        if (sVersions != null && !sVersions.isEmpty())
            {
            listVersions.addAll(Arrays.asList(sVersions.split(",")));
            }

        return Arrays.stream(aLibDirs)
                .filter((file) -> !file.getName().startsWith("."))
                .filter((file) -> listVersions.isEmpty() || listVersions.contains(file.getName()))
                .collect(Collectors.toList());
        }

    // ----- constants ------------------------------------------------------

    public static final String DASHES = "-----------------------------------------------------------------------------"
            + "-------------------------------------------";

    // ----- data members ---------------------------------------------------

    protected static File s_fileResultsFolder;

    protected static File s_fileSnapshots;

    protected static BiFunction<Supplier<PsrPerformanceEnvironment>, List<File>, List<Pair<String,PsrPerformanceEnvironment>>> s_functionScenarios = AbstractPerformanceTests::getAllScenarios;

    @Rule
    public TestName m_testName = new TestName();

    protected static final Map<String,Map<String,Object>> s_mapResults = new HashMap<>();

    protected final String f_sDescription;

    protected final PsrPerformanceEnvironment<?> f_environment;

    protected File m_fileResults;

    }
