/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.sun.tools.visualvm.modules.coherence;

import com.oracle.bedrock.OptionsByType;
import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.runtime.LocalPlatform;
import com.oracle.bedrock.runtime.coherence.CoherenceCacheServer;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.coherence.JMXManagementMode;
import com.oracle.bedrock.runtime.coherence.options.CacheConfig;
import com.oracle.bedrock.runtime.coherence.options.ClusterName;
import com.oracle.bedrock.runtime.coherence.options.LocalHost;
import com.oracle.bedrock.runtime.coherence.options.LocalStorage;
import com.oracle.bedrock.runtime.coherence.options.Logging;
import com.oracle.bedrock.runtime.coherence.options.Multicast;
import com.oracle.bedrock.runtime.coherence.options.OperationalOverride;
import com.oracle.bedrock.runtime.coherence.options.RoleName;
import com.oracle.bedrock.runtime.coherence.options.WellKnownAddress;

import com.oracle.bedrock.runtime.java.features.JmxFeature;
import com.oracle.bedrock.runtime.java.options.SystemProperty;
import com.oracle.bedrock.runtime.java.profiles.JmxProfile;
import com.oracle.bedrock.runtime.network.AvailablePortIterator;

import com.oracle.bedrock.runtime.options.DisplayName;
import com.oracle.bedrock.testsupport.junit.TestLogs;
import com.sun.tools.visualvm.modules.coherence.helper.HttpRequestSender;
import com.sun.tools.visualvm.modules.coherence.helper.JMXRequestSender;
import com.sun.tools.visualvm.modules.coherence.helper.RequestSender;
import com.tangosol.io.FileHelper;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;

import org.junit.*;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

import com.sun.tools.visualvm.modules.coherence.tablemodel.model.AbstractData;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.Data;

import java.io.File;

import java.net.UnknownHostException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Base class for VisualVM tests.
 */
public abstract class AbstractVisualVMTest
    {
    // ----- constructors --------------------------------------------------

    /**
     * Default constructor.
     */
    public AbstractVisualVMTest()
        {
        }

    // ----- test methods ---------------------------------------------------

    /**
     * Start the required cache servers using oracle-tools
     *
     * @param fRestRequestSender  if a REST request sender needs to be used
     *
     * @throws UnknownHostException if invalid host specified
     */
    public static void startupCacheServers(boolean fRestRequestSender)
            throws UnknownHostException
        {
        try
            {
            // setup temporary directories for persistence
            fileActiveDirA           = FileHelper.createTempDir();
            fileSnapshotDirA         = FileHelper.createTempDir();
            fileTrashDirA            = FileHelper.createTempDir();

            availablePortIteratorWKA = LocalPlatform.get().getAvailablePorts();
            int nWKAPortA = availablePortIteratorWKA.next();

            // Iterate to next port and waste this as sometimes the second port will
            // already be used up before the realize()
            availablePortIteratorWKA.next();

            int nWKAPortB = availablePortIteratorWKA.next();

            // Iterate to next port and waste this as sometimes the second port will
            // already be used up before the realize()
            availablePortIteratorWKA.next();

            OptionsByType optionsByTypeA = createCacheServerOptions(CLUSTER_NAME, nWKAPortA, fileActiveDirA,
                                                                    fileSnapshotDirA, fileTrashDirA,
                                                                    nWKAPortA, nWKAPortB);
            LocalPlatform platform       = LocalPlatform.get();

            OptionsByType optionsByTypeMemberA1 = OptionsByType.of(optionsByTypeA).add(DisplayName.of("memberA1"));

            if (fRestRequestSender)
                {
                optionsByTypeMemberA1.add(SystemProperty.of("coherence.management", "dynamic"));
                optionsByTypeMemberA1.add(SystemProperty.of("coherence.management.http", "inherit"));
                optionsByTypeMemberA1.add(SystemProperty.of("coherence.management.http.host", REST_MGMT_HOST));
                optionsByTypeMemberA1.add(SystemProperty.of("coherence.management.http.port", REST_MGMT_PORT));
                }

            memberA1 = platform.launch(CoherenceCacheServer.class, optionsByTypeMemberA1.asArray());
            memberA2 = platform.launch(CoherenceCacheServer.class, optionsByTypeA.add(DisplayName.of("memberA2")).asArray());

            requestSender = fRestRequestSender
                    ? new HttpRequestSender("http://" + REST_MGMT_HOST + ":" + "8080")
                    : new JMXRequestSender(memberA1.get(JmxFeature.class)
                        .getDeferredJMXConnector().get().getMBeanServerConnection());

            Eventually.assertThat(invoking(memberA1).getClusterSize(), is(2));

            // setup temporary directories for persistence
            fileActiveDirB   = FileHelper.createTempDir();
            fileSnapshotDirB = FileHelper.createTempDir();
            fileTrashDirB    = FileHelper.createTempDir();

            OptionsByType optionsByTypeB = createCacheServerOptions(CLUSTER_B_NAME, nWKAPortB, fileActiveDirB,
                                                                    fileSnapshotDirB, fileTrashDirB,
                                                                    nWKAPortA, nWKAPortB);

            OptionsByType optionsByTypeMemberB1 = OptionsByType.of(optionsByTypeB).add(DisplayName.of("memberB1"));

            memberB1 = platform.launch(CoherenceCacheServer.class, optionsByTypeMemberB1.asArray());
            memberB2 = platform.launch(CoherenceCacheServer.class, optionsByTypeB.add(DisplayName.of("memberB2")).asArray());

            Eventually.assertThat(invoking(memberB1).getClusterSize(), is(2));
            }
        catch (Exception e)
            {
            System.err.println("Error starting cache servers. " + e.getMessage());
            e.printStackTrace();

            throw new RuntimeException(e);
            }

        }

    /**
     * Shutdown all cache servers after the test.
     */
    public static void shutdownCacheServers()
        {
        CacheFactory.shutdown();

        destroyMember(memberA1);
        destroyMember(memberA2);
        destroyMember(memberB1);
        destroyMember(memberB2);

        safeDelete(fileActiveDirA);
        safeDelete(fileSnapshotDirA);
        safeDelete(fileTrashDirA);
        safeDelete(fileActiveDirB);
        safeDelete(fileSnapshotDirB);
        safeDelete(fileTrashDirB);
        }

    // ----- helpers --------------------------------------------------------

    private static void safeDelete(File file)
        {
        try
            {
            FileHelper.deleteDir(file);
            }
        catch (Throwable thrown)
            {
            // ignored
            }
        }

    /**
     * Destory a member and ignore any errors.
     *
     * @param member the {@link CoherenceClusterMember} to destroy
     */
    private static void destroyMember(CoherenceClusterMember member)
        {
        try
            {
            if (member != null)
                {
                member.close();
                }
            }
        catch (Throwable thrown)
            {
            // ignored
            }
        }

    /**
     * Establish {@link OptionsByType} to use launching cache servers.
     *
     * @param sClusterName      the cluster name
     * @param nWKAPort          the wka port
     * @param fileActiveDir     active persistence directory
     * @param fileSnapshotDir   snapshot persistence directory
     * @param fileTrashDir      trash persistence directory
     * @param nFederationPortA  the port for ClusterB
     * @param nFederationPortB  the port for ClusterB
     *
     * @return an {@link OptionsByType}
     *
     * @throws UnknownHostException if unknown host found
     */
    protected static OptionsByType createCacheServerOptions(String sClusterName, int nWKAPort, File fileActiveDir,
                                                            File fileSnapshotDir, File fileTrashDir,
                                                            int nFederationPortA, int nFederationPortB)
            throws UnknownHostException
        {


        String        hostName      = LocalPlatform.get().getLoopbackAddress().getHostAddress();

        OptionsByType optionsByType = OptionsByType.empty();

        optionsByType.addAll(JMXManagementMode.ALL,
                             JmxProfile.enabled(),
                             LocalStorage.enabled(),
                             LocalHost.of(hostName, nWKAPort),
                             WellKnownAddress.of(hostName, nWKAPort),
                             Multicast.ttl(0),
                             CacheConfig.of(System.getProperty("tangosol.coherence.cacheconfig")),
                             Logging.at(5),
                             ClusterName.of(sClusterName),
                             SystemProperty.of("test.multicast.port", Integer.toString(nWKAPort)),
                             SystemProperty.of("partition.count", Integer.toString(PARTITION_COUNT)),
                             SystemProperty.of("tangosol.coherence.management.refresh.expiry", "1s"),
                             SystemProperty.of("active.dir", fileActiveDir.getAbsolutePath()),
                             SystemProperty.of("snapshot.dir", fileSnapshotDir.getAbsolutePath()),
                             SystemProperty.of("trash.dir", fileTrashDir.getAbsolutePath()),
                             OperationalOverride.of(System.getProperty("tangosol.coherence.override")),
                             SystemProperty.of("java.rmi.server.hostname", hostName),
                             SystemProperty.of("coherence.distribution.2server", "false"),
                             SystemProperty.of("loopbackhost", hostName),
                             RoleName.of(ROLE_NAME),
                             s_logs.builder());

        return optionsByType;
        }

    /**
     * Validate the generated data for basic completeness including the number of rows
     * returned and column count for each row.
     *
     * @param type   The type of the data
     * @param data   the generated data
     * @param nExpectedSize the expected size
     */
    protected void validateData(VisualVMModel.DataType type, List<Map.Entry<Object, Data>> data, int nExpectedSize)
        {
        String sClass = type.getClassName().toString();

        // if the type is Member then add one as the number of columns displayed is
        // different to the number of columns in the model. This is the only type that that this different.
        int cColumns = type.getMetadata().length + (type.equals(VisualVMModel.DataType.MEMBER) ? 1 : 0);

        banner("Validating " + sClass + " for basic completeness");

        Assert.assertNotNull("Data for " + sClass + " should not be null", data);

        // now check that each row has the expected number of columns
        for (Map.Entry<Object, Data> entry : data)
            {
            AbstractData dataItem = (AbstractData) entry.getValue();

            Assert.assertEquals("Number of elements for " + sClass + " should be " + cColumns + " but is "
                                        + dataItem.getColumnCount() + ". Entry is " + entry.getValue(), dataItem.getColumnCount(), cColumns);
            }

        }

    /**
     * Assert that the cluster is ready for the tests.
     */
    protected void assertClusterReady()
        {
        Assert.assertTrue(requestSender != null);
        Assert.assertTrue("Cluster size should be 2 but is " + memberA1.getClusterSize(), memberA1.getClusterSize() == 2);
        Assert.assertTrue("Cluster size should be 2 but is " + memberB1.getClusterSize(), memberB1.getClusterSize() == 2);
        }

    /**
     * Populate a given {@link NamedCache} with data.
     *
     * @param nc        {@link NamedCache} to populate
     * @param cEntries  the number of entries to add
     * @param nSize     the size of each entry
     */
    protected void populateRandomData(NamedCache nc, int cEntries, int nSize)
        {
        char[]               bData  = new char[nSize];
        Map<Integer, String> buffer = new HashMap<>();

        banner("Populating cache " + nc.getCacheName() + " with " + cEntries + " objects of size " + nSize);

        for (int i = 0; i < nSize; i++)
            {
            bData[i] = ((i % 2 == 0) ? 'X' : 'Y');
            }

        String sData = Arrays.toString(bData);

        for (int j = 0; j < cEntries; j++)
            {
            buffer.put(new Integer(j), sData);
            }

        nc.putAll(buffer);
        buffer.clear();

        Assert.assertTrue("NamedCache " + nc.getCacheName() + " should contain " + cEntries + " but contains "
                          + nc.size(), nc.size() == cEntries);
        }

    /**
     * Set the current data type for ease of use for the validateColumn() method
     *
     * @param dataType the dataType to set
     */
    protected void setCurrentDataType(VisualVMModel.DataType dataType)
        {
        this.dataType = dataType;
        }

    /**
     * Get the given column for the current dataType. This is a helper method and
     * does some sanity checking to ensure that the returned column is correct
     * and not null.
     *
     * @param nColumn         the column index to validate
     * @param entry           the entry which contains the retrieved value
     */
    protected Object getColumn(int nColumn, Map.Entry<Object, Data> entry)
        {
        Assert.assertTrue("You must call setCurrentDataType before this call", dataType != null);

        Data   data    = entry.getValue();

        Assert.assertTrue("Data value is null", data != null);

        return entry.getValue().getColumn(nColumn); // actual value
        }

    /**
     * Validate a given column for the current dataType. This is a helper method and
     * does some sanity checking to ensure that the returned column is not null only.
     *
     * @param nColumn         the column index to validate
     * @param entry           the entry which contains the retrieved value
     */
    protected void validateColumnNotNull(int nColumn, Map.Entry<Object, Data> entry)
        {
        Assert.assertThat(getColumn(nColumn, entry), is(notNullValue()));
        }

    /**
     * Validate a given column for the current dataType. This is a helper method and
     * does some sanity checking to ensure that the returned column is correct
     * and not null.
     *
     * @param nColumn         the column index to validate
     * @param entry           the entry which contains the retrieved value
     * @param oExpectedValue  the expected value
     */
    protected void validateColumn(int nColumn, Map.Entry<Object, Data> entry, Object oExpectedValue)
        {
        Object oActualValue = getColumn(nColumn, entry);

        String sColumn = dataType.getMetadata()[nColumn];
        output("Validating column " + nColumn + " (" + sColumn + ")");

        String sError  = "The value of column \"" + sColumn + "\" with index " + nColumn + " was expected to be \""
                         + oExpectedValue + "\" but was \"" + oActualValue + "\"";

        if (oExpectedValue instanceof String)
            {
            Assert.assertTrue(sError, oActualValue.toString().equals(oExpectedValue));
            }
        else if (oExpectedValue instanceof Float)
            {
            Assert.assertTrue(sError, ((Float) oExpectedValue).floatValue() == ((Float) oActualValue).floatValue());
            }
        else if (oExpectedValue instanceof Integer)
            {
            Assert.assertTrue(sError, ((Integer) oExpectedValue).intValue() == ((Integer) oActualValue).intValue());
            }
        else if (oExpectedValue instanceof Long)
            {
            Assert.assertTrue(sError, ((Long) oExpectedValue).longValue() == ((Long) oActualValue).longValue());
            }
        else
            {
            throw new RuntimeException("Unable to validate type " + oExpectedValue.getClass().toString() + " for \""
                                       + sColumn + "\"");
            }
        }

    protected static void wait(String sMessage, long nMillis)
        {
        try
            {
            Thread.sleep(nMillis);
            }
        catch (InterruptedException e)
            {
            e.printStackTrace();
            }
        }

    protected static void output(String sMessage)
        {
        //System.err.println("***** " + sMessage);
        }

    protected static void banner(String sMessage)
        {
        // System.err.println("\n***** " + sMessage + "\n");
        }

    // ----- constants ------------------------------------------------------

    /**
     * Field description
     */
    public static final String ROLE_NAME = "CacheServer";

    /**
     * Active persistence directory for ClusterA.
     */
    public static File fileActiveDirA;

    /**
     * Snapshot persistence directory for ClusterA.
     */
    public static File fileSnapshotDirA;

    /**
     * Trash persistence directory for ClusterA.
     */
    public static File fileTrashDirA;

    /**
     * Active persistence directory for ClusterB.
     */
    public static File fileActiveDirB;

    /**
     * Snapshot persistence directory for ClusterB.
     */
    public static File fileSnapshotDirB;

    /**
     * Trash persistence directory for ClusterB.
     */
    public static File fileTrashDirB;

    /**
     * Starting port for General Ports.
     */
    public static int GENERAL_START_PORT = 20000;

    /**
     * Starting port for WKA.
     */
    public static int WKA_START_PORT = 30000;

    /**
     * Partition count.
     */
    protected static int PARTITION_COUNT = 277;

    /**
     * Cluster A Name.
     */
    protected static String CLUSTER_NAME   = "ClusterA";

    /**
     * Cluster B Name.
     */
    protected static String CLUSTER_B_NAME = "ClusterB";

    /**
     * Rest Management Host.
     */
    protected static String REST_MGMT_HOST = "127.0.0.1";

    /**
     * Rest Management Port
     */
    protected static String REST_MGMT_PORT = "8080";

    /**
     * port iterator for ephemeral ports.
     */
    //protected static AvailablePortIterator availablePortIterator;

    /**
     * port iterator for ephemeral ports.
     */
    protected static AvailablePortIterator availablePortIteratorWKA;

    /**
     * First cluster member of ClusterA.
     */
    protected static CoherenceCacheServer memberA1 = null;

    /**
     * Second cluster member of ClusterA.
     */
    protected static CoherenceCacheServer memberA2 = null;

    /**
     * First cluster member of ClusterB.
     */
    protected static CoherenceCacheServer memberB1 = null;

    /**
     * Second cluster member of ClusterB.
     */
    protected static CoherenceCacheServer memberB2 = null;

    /**
     * Connection to MBean server.
     */
    protected static RequestSender requestSender = null;

    // ----- data members -----------------------------------------------

    @ClassRule
    public static final TestLogs s_logs = new TestLogs(AbstractVisualVMTest.class);

    /**
     * Currently selected dataType for use by helper methods.
     */
    protected VisualVMModel.DataType dataType;

    /**
     * The model used to store the collected stats.
     */
    protected VisualVMModel model = null;
    }
