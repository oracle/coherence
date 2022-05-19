/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package io;

import com.oracle.bedrock.options.Timeout;

import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;

import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.FileHelper;
import com.tangosol.net.CoherenceSession;
import com.tangosol.net.NamedCache;

import com.oracle.coherence.testing.AbstractTestInfrastructure;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;
import java.util.Properties;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Test for bdb-store-manager
 *
 * @author jf  2019.08.27
 */
public class BDBStoreTests
    extends AbstractTestInfrastructure
    {
    @BeforeClass
    public static void _startup()
        {
        // don't start cluster, client using a local external-scheme backed by bdb-store-manager.
        setupProps();
        }

    // Regression test for COH-19888: failed to persist external-scheme/bdb-store-managager backed cache when application
    // terminated abnormally and shutdown hook does not run.
    @Test
    public void testPersistentBDBCacheAutoRecovery()
        throws IOException
        {
        // ensure starting with no previous bdb temp directory.
        File fileBDBTempDir = FileHelper.createTempDir();

        try
            {
            System.out.println("test.bdb.directory=" + fileBDBTempDir.getAbsolutePath());

            int nTerminationValue = 4;

            Properties props = new Properties();
            props.put("test.bdb.directory", fileBDBTempDir.getAbsolutePath());
            props.put("test.abnormal_termination", Integer.toString(nTerminationValue));

            // 1. create a bdb-store-manager persistent binary store and terminate abnormally, (bypassing BDBShutdownHook)
            CoherenceClusterMember member = startCacheApplication("TestAutoRecoveryCreatePersistentCacheTerminatesAbnormal",
                "io.BDBStoreTests$AddAndGetFromBDBStore",
                "io", CACHE_CONFIG, props);

            int result = member.waitFor(Timeout.after("30s"));
            assertThat(result, is(nTerminationValue));

            // this file should still exist if cache application aborted abnormally and shutdown hook did not execute.
            File fileLock = new File(fileBDBTempDir.getAbsolutePath() + File.separator + "coherence.lck");
            assertTrue("fileLock: " + fileLock + " should exist", fileLock.exists());

            // 2. validate persistent cache despite abnormal termination of previous client that created cache.
            props.clear();
            props.put("test.bdb.directory", fileBDBTempDir.getAbsolutePath());
            props.put("test.require_keys_exist", "true");
            member = startCacheApplication("TestAutoRecoveryValidatePersistentCache",
                "io.BDBStoreTests$AddAndGetFromBDBStore",
                "io", CACHE_CONFIG, props);

            result = member.waitFor(Timeout.after("30s"));
            assertThat("check target/test-output/TestAutoRecoveryValidatePersistentCache log file for failure", result, is(0));
            }
        finally
            {
            FileHelper.deleteDirSilent(fileBDBTempDir);
            }
        }

    @Test
    public void testPersistentBDBLogCleaning()
        throws IOException, InterruptedException
        {
        // ensure starting with no previous bdb temp directory.
        File fileBDBTempDir  = FileHelper.createTempDir();
        File fileFirstBDBLog = new File(fileBDBTempDir.getAbsolutePath() + File.separator + "00000000.jdb");

        try
            {
            System.out.println("test.bdb.directory=" + fileBDBTempDir.getAbsolutePath());

            int nTerminationValue = 4;

            Properties props = new Properties();
            props.put("test.bdb.directory", fileBDBTempDir.getAbsolutePath());
            props.put("test.abnormal_termination", Integer.toString(nTerminationValue));

            // 1. create a bdb-store-manager persistent binary store and terminate abnormally, (bypassing BDBShutdownHook)
            CoherenceClusterMember member = startCacheApplication("testPersistentBDBLogCleaning_first",
                "io.BDBStoreTests$ValidateLogCleanup",
                "io", CACHE_CONFIG, props);

            int result = member.waitFor(Timeout.after("30s"));
            assertThat(result, is(nTerminationValue));

            // this file should still exist if cache application aborted abnormally and shutdown hook did not execute.
            File fileLock = new File(fileBDBTempDir.getAbsolutePath() + File.separator + "coherence.lck");
            assertTrue("fileLock: " + fileLock + " should exist", fileLock.exists());

            // 2. validate persistent cache despite abnormal termination of previous client that created cache.
            props.clear();
            props.put("test.bdb.directory", fileBDBTempDir.getAbsolutePath());
            props.put("test.require_keys_exist", "true");
            member = startCacheApplication("testPersistentBDBLogCleaning_second",
                "io.BDBStoreTests$ValidateLogCleanup",
                "io", CACHE_CONFIG, props);

            result = member.waitFor(Timeout.after("30s"));
            assertThat("check target/test-output/testPersistentBDBLogCleaning log file for failure", result, is(0));
            assertFalse("first log file: " +  fileFirstBDBLog + " should no longer exist", fileFirstBDBLog.exists());
            }
        finally
            {
            FileHelper.deleteDirSilent(fileBDBTempDir);
            }
        }

    // ----- inner class: AddAndGetFromBDBStore -----------------------------

    public static class AddAndGetFromBDBStore
        implements java.io.Serializable
        {
        public static void main(String[] args)
            {
            String  sAbnormalTermination = System.getProperty("test.abnormal_termination");
            int     nTerminationValue    = sAbnormalTermination == null ? 0 : Integer.parseInt(sAbnormalTermination);
            boolean fRequireKeysExist    = Boolean.getBoolean("test.require_keys_exist");

            System.out.println("Exit client with TerminationValue=" + nTerminationValue);

            int MAX = 2;

            try (CoherenceSession session = new CoherenceSession())
                {
                NamedCache nc = session.getCache("metaData-1");

                System.out.println("Store step: Get Cache for " + nc.getCacheName() + " entries: " + nc.size());

                if (fRequireKeysExist)
                    {
                    assertThat("expect key/values stored in previous invocation", nc.size(), is(MAX));
                    }
                else
                    {
                    assertThat("expect cache to be empty", nc.size(), is(0));
                    }

                for (int i = 0; i < MAX; i++)
                    {
                    String sKey = "key_" + i;
                    if (!nc.containsKey(sKey))
                        {
                        // if key missing, autorecovery of first pass failed, fail test.
                        assertFalse("missing value for key " + sKey, fRequireKeysExist);

                        Foo oValue = new Foo(i + 10);

                        System.out.println("adding entry key=" + sKey + " value= " + oValue);
                        nc.put(sKey, oValue);
                        }
                    }

                System.out.println("Load: Get Cache " + nc.getCacheName() + " entries: " + nc.size());

                for (int i = 0; i < MAX; i++)
                    {
                    String sKey   = "key_" + i;
                    Object oValue = nc.get(sKey);

                    if (oValue == null)
                        {
                        throw new IllegalStateException("key should have a non-null value");
                        }
                    else
                        {
                        System.out.println("get: key " + sKey + " has value: " + oValue);
                        }
                    }
                }
            catch (Throwable t)
                {
                System.out.println("Unexpected exception " + t.getMessage());
                Runtime.getRuntime().exit(1);
                }

            if (nTerminationValue != 0)
                {
                System.out.println("non-graceful termination to avoid running BDBStore shutdown hook");
                Runtime.getRuntime().halt(nTerminationValue);
                }
            }
        }

    // ----- inner class: AddAndGetFromBDBStore -----------------------------

    public static class ValidateLogCleanup
        implements java.io.Serializable
        {
        public static void main(String[] args)
            throws Exception
            {
            String  sAbnormalTermination = System.getProperty("test.abnormal_termination");
            int     nTerminationValue    = sAbnormalTermination == null ? 0 : Integer.parseInt(sAbnormalTermination);
            boolean fRequireKeysExist    = Boolean.getBoolean("test.require_keys_exist");

            System.out.println("Exit client with TerminationValue=" + nTerminationValue);

            int MAX = 500;

            try (CoherenceSession session = new CoherenceSession())
                {
                // configured bdb-store-manager optimally to force BDB log cleanup (by its cleaner) for this test case.
                NamedCache nc = session.getCache("validate-clean-log-cache");

                System.out.println("Store step: Get Cache for " + nc.getCacheName() + " entries: " + nc.size());

                if (fRequireKeysExist)
                    {
                    assertThat("expect key/values stored in previous invocation", nc.size(), is(MAX));
                    }
                else
                    {
                    assertThat("expect cache to be empty", nc.size(), is(0));
                    }
                if (!fRequireKeysExist)
                    {
                    for (int i = 0; i < MAX; i++)
                        {
                        nc.put(i, getRandomString(2024, 2024, true));
                        }

                    // BDB just appends, so this second round doubles logs
                    for (int i = 0; i < MAX; i++)
                        {
                        nc.put(i, getRandomString(2024, 2024, true));
                        }

                    for (int i = 0; i < MAX; i++)
                        {
                        if (!nc.containsKey(i))
                            {
                            // if key missing, autorecovery of first pass failed, fail test.
                            assertFalse("missing value for key " + i, fRequireKeysExist);

                            nc.put(i, getRandomString(2024, 2024, true));
                            }
                        }

                    System.out.println("Load: Get Cache " + nc.getCacheName() + " entries: " + nc.size());

                    for (int i = 0; i < MAX; i++)
                        {
                        Object oValue = nc.get(i);

                        if (oValue == null)
                            {
                            throw new IllegalStateException("key should have a non-null value");
                            }
                        }
                    }
                }
            catch (Throwable t)
                {
                System.out.println("Unexpected exception " + t.getMessage());
                Runtime.getRuntime().exit(1);
                }

            if (nTerminationValue != 0)
                {
                System.out.println("non-graceful termination to avoid running BDBStore shutdown hook");
                Runtime.getRuntime().halt(nTerminationValue);
                }
            }
        }

    // ----- inner class: Foo -----------------------------------------------

    public static class Foo
        implements ExternalizableLite
        {
        public Foo()
            {
            // for serialization
            }

        public Foo(int nValue)
            {
            m_nValue = nValue;
            }

        @Override
        public String toString()
            {
            return "Foo { value=" + m_nValue + "}";
            }

        @Override
        public void readExternal(DataInput in)
            throws IOException
            {
            m_nValue = in.readInt();
            }

        @Override
        public void writeExternal(DataOutput out)
            throws IOException
            {
            out.writeInt(m_nValue);
            }

        private int m_nValue;
        }

    // ----- constants ------------------------------------------------------

    static public final String CACHE_CONFIG = "cache-config-bdb.xml";
    }
