/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package config;

import com.oracle.coherence.common.base.Logger;
import com.oracle.coherence.persistence.PersistenceEnvironment;
import com.oracle.coherence.persistence.PersistenceException;
import com.oracle.coherence.persistence.PersistenceManager;
import com.oracle.coherence.persistence.PersistentStore;

import com.tangosol.coherence.config.ParameterMacroExpressionParser;
import com.tangosol.coherence.config.ResolvableParameterList;
import com.tangosol.coherence.config.builder.ParameterizedBuilder;
import com.tangosol.coherence.config.builder.PersistenceEnvironmentParamBuilder;
import com.tangosol.coherence.config.xml.OperationalConfigNamespaceHandler;

import com.tangosol.config.expression.Parameter;
import com.tangosol.config.xml.DocumentProcessor;

import com.tangosol.io.FileHelper;
import com.tangosol.io.ReadBuffer;

import com.tangosol.net.Member;

import com.tangosol.persistence.GUIDHelper;
import com.tangosol.persistence.PersistenceEnvironmentInfo;
import com.tangosol.persistence.SafePersistenceWrappers;
import com.tangosol.persistence.bdb.BerkeleyDBEnvironment;
import com.tangosol.persistence.bdb.BerkeleyDBManager;

import com.tangosol.run.xml.XmlDocumentReference;

import com.tangosol.util.Binary;
import com.tangosol.util.NullImplementation;
import com.tangosol.util.ResourceRegistry;
import com.tangosol.util.SimpleResourceRegistry;
import com.tangosol.util.UID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;

import java.util.Date;

/**
 * Unit test of PersistenceEnvironmentProcessor.
 * (adapted from ConfigurablePersistenceEnvironmentFactoryTest
 * to preserve its testing robustness)
 *
 * @author jf  2015.03.24
 */
public class PersistenceEnvironmentProcessorTests
    {
    // ----- test lifecycle -------------------------------------------------

    @Before
    public void setupTest()
            throws IOException
        {
        m_fileHome     = FileHelper.createTempDir();
        m_fileActive   = new File(new File(m_fileHome, "coherence"), "active");
        m_fileBackup   = new File(new File(m_fileHome, "coherence"), "backup");
        m_fileSnapshot = new File(new File(m_fileHome, "coherence"), "snapshots");
        m_fileTrash    = new File(new File(m_fileHome, "coherence"), "trash");

        ResourceRegistry resourceRegistry = new SimpleResourceRegistry();

        DocumentProcessor.DefaultDependencies dep =
                new DocumentProcessor.DefaultDependencies(new OperationalConfigNamespaceHandler());

        dep.setExpressionParser(ParameterMacroExpressionParser.INSTANCE);
        dep.setResourceRegistry(resourceRegistry);

        m_processor = new DocumentProcessor(dep);

        }

    @After
    public void teardownTest()
            throws IOException
        {
        FileHelper.deleteDirSilent(m_fileHome);
        }

    // ----- test methods ---------------------------------------------------

    @Test
    public void testBerkeleyDBEnvironment()
            throws IOException
        {
        File fileActive   = m_fileActive;
        File fileSnapshot = m_fileSnapshot;
        File fileTrash    = m_fileTrash;

        String sXml = "<persistence-environment>\n" + "  <active-directory>" + fileActive.getAbsolutePath()
                      + "</active-directory>\n" + "  <snapshot-directory>" + fileSnapshot.getAbsolutePath()
                      + "</snapshot-directory>\n" + "  <trash-directory>" + fileTrash.getAbsolutePath()
                      + "</trash-directory>\n" + "</persistence-environment>";

        ParameterizedBuilder<PersistenceEnvironment<ReadBuffer>> bldr = m_processor.process(new XmlDocumentReference(sXml));

        PersistenceEnvironment<ReadBuffer> env = bldr.realize(RESOLVER, getClass().getClassLoader(), null);

        env = SafePersistenceWrappers.unwrap(env);
        assertTrue(env instanceof BerkeleyDBEnvironment);

        fileActive   = new File(new File(fileActive, "Cluster"), "Service");
        fileSnapshot = new File(new File(fileSnapshot, "Cluster"), "Service");
        fileTrash    = new File(new File(fileTrash, "Cluster"), "Service");

        PersistenceEnvironmentInfo info = (PersistenceEnvironmentInfo) env;

        assertEquals(fileActive, info.getPersistenceActiveDirectory());
        assertEquals(fileSnapshot, info.getPersistenceSnapshotDirectory());
        assertEquals(fileTrash, info.getPersistenceTrashDirectory());

        PersistenceManager<ReadBuffer> manager = env.openActive();

        assertTrue(manager instanceof BerkeleyDBManager);

        BerkeleyDBManager managerImpl = (BerkeleyDBManager) manager;

        assertEquals(fileActive, managerImpl.getDataDirectory());

        env.release();
        }

    @Test
    public void testBerkeleyDBEnvironmentDefaults()
            throws IOException
        {
        File fileActive   = m_fileActive;
        File fileSnapshot = m_fileSnapshot;
        File fileTrash    = m_fileTrash;

        System.setProperty("user.home", m_fileHome.getAbsolutePath());

        String sXml = "<persistence-environment/>";
        ParameterizedBuilder<PersistenceEnvironment<ReadBuffer>> bldr =
                m_processor.process(new XmlDocumentReference(sXml));

        PersistenceEnvironment<ReadBuffer> env = bldr.realize(RESOLVER, getClass().getClassLoader(), null);

        env = SafePersistenceWrappers.unwrap(env);
        assertTrue(env instanceof BerkeleyDBEnvironment);

        fileActive   = new File(new File(fileActive, "Cluster"), "Service");
        fileSnapshot = new File(new File(fileSnapshot, "Cluster"), "Service");
        fileTrash    = new File(new File(fileTrash, "Cluster"), "Service");

        PersistenceEnvironmentInfo info = (PersistenceEnvironmentInfo) env;

        assertEquals(fileActive, info.getPersistenceActiveDirectory());
        assertEquals(fileSnapshot, info.getPersistenceSnapshotDirectory());
        assertEquals(fileTrash, info.getPersistenceTrashDirectory());

        PersistenceManager<ReadBuffer> manager = env.openActive();

        assertTrue(manager instanceof BerkeleyDBManager);

        BerkeleyDBManager managerImpl = (BerkeleyDBManager) manager;

        assertEquals(fileActive, managerImpl.getDataDirectory());

        env.release();
        }

    @Test
    public void testBerkeleyDBEnvironmentInvalid()
        {
        File fileActive   = m_fileActive;
        File fileSnapshot = m_fileSnapshot;
        File fileTrash    = m_fileTrash;

        String sXml = "<persistence-environment>\n" + "  <persistence-mode>invalid</persistence-mode>\n"
                      + "  <active-directory>" + fileActive.getAbsolutePath()
                      + "</active-directory>\n" + "  <snapshot-directory>"
                      + fileSnapshot.getAbsolutePath() + "</snapshot-directory>\n"
                      + "  <trash-directory>" + fileTrash.getAbsolutePath()
                      + "</trash-directory>\n" + "</persistence-environment>";

        ParameterizedBuilder<PersistenceEnvironment<ReadBuffer>> bldr =
                m_processor.process(new XmlDocumentReference(sXml));

        try
            {
            PersistenceEnvironment<ReadBuffer> env = bldr.realize(RESOLVER, getClass().getClassLoader(), null);
            fail("expected ConfigurationException");
            }
        catch(com.tangosol.config.ConfigurationException e)
            {
            assertTrue(e.getMessage().contains("Invalid persistence mode"));
            }
        }

    @Test
    public void testBerkeleyDBEnvironmentActive()
            throws IOException
        {
        File fileActive   = m_fileActive;
        File fileSnapshot = m_fileSnapshot;
        File fileTrash    = m_fileTrash;

        String sXml = "<persistence-environment>\n" + "  <persistence-mode>active</persistence-mode>\n"
                                           + "  <active-directory>" + fileActive.getAbsolutePath()
                                           + "</active-directory>\n" + "  <snapshot-directory>"
                                           + fileSnapshot.getAbsolutePath() + "</snapshot-directory>\n"
                                           + "  <trash-directory>" + fileTrash.getAbsolutePath()
                                           + "</trash-directory>\n" + "</persistence-environment>";

        ParameterizedBuilder<PersistenceEnvironment<ReadBuffer>> bldr =
                m_processor.process(new XmlDocumentReference(sXml));

        PersistenceEnvironment<ReadBuffer> env = bldr.realize(RESOLVER, getClass().getClassLoader(), null);

        env = SafePersistenceWrappers.unwrap(env);
        assertTrue(env instanceof BerkeleyDBEnvironment);

        fileActive   = new File(new File(fileActive, "Cluster"), "Service");
        fileSnapshot = new File(new File(fileSnapshot, "Cluster"), "Service");
        fileTrash    = new File(new File(fileTrash, "Cluster"), "Service");

        PersistenceEnvironmentInfo info = (PersistenceEnvironmentInfo) env;

        assertEquals(fileActive, info.getPersistenceActiveDirectory());
        assertEquals(fileSnapshot, info.getPersistenceSnapshotDirectory());
        assertEquals(fileTrash, info.getPersistenceTrashDirectory());

        PersistenceManager<ReadBuffer> manager = env.openActive();

        assertTrue(manager instanceof BerkeleyDBManager);

        BerkeleyDBManager managerImpl = (BerkeleyDBManager) manager;

        assertEquals(fileActive, managerImpl.getDataDirectory());

        env.release();
        }

    @Test
    public void testBerkeleyDBEnvironmentOnDemand()
            throws IOException
        {
        File fileActive   = m_fileActive;
        File fileSnapshot = m_fileSnapshot;
        File fileTrash    = m_fileTrash;

        String sXml = "<persistence-environment>\n" + "  <persistence-mode>on-demand</persistence-mode>\n"
                                           + "  <active-directory>" + fileActive.getAbsolutePath()
                                           + "</active-directory>\n" + "  <snapshot-directory>"
                                           + fileSnapshot.getAbsolutePath() + "</snapshot-directory>\n"
                                           + "  <trash-directory>" + fileTrash.getAbsolutePath()
                                           + "</trash-directory>\n" + "</persistence-environment>";

        ParameterizedBuilder<PersistenceEnvironment<ReadBuffer>> bldr =
                m_processor.process(new XmlDocumentReference(sXml));

        PersistenceEnvironment<ReadBuffer> env = bldr.realize(RESOLVER, getClass().getClassLoader(), null);

        env = SafePersistenceWrappers.unwrap(env);
        assertTrue(env instanceof BerkeleyDBEnvironment);

        fileSnapshot = new File(new File(fileSnapshot, "Cluster"), "Service");
        fileTrash    = new File(new File(fileTrash, "Cluster"), "Service");

        PersistenceEnvironmentInfo info = (PersistenceEnvironmentInfo) env;

        assertNull(info.getPersistenceActiveDirectory());
        assertEquals(fileSnapshot, info.getPersistenceSnapshotDirectory());
        assertEquals(fileTrash, info.getPersistenceTrashDirectory());

        PersistenceManager<ReadBuffer> manager = env.openActive();

        assertNull(manager);

        env.release();
        }

    @Test
    public void testBerkeleyDBEnvironmentSafe()
            throws IOException
        {
        File fileActive   = m_fileActive;
        File fileSnapshot = m_fileSnapshot;
        File fileTrash    = m_fileTrash;

        System.setProperty("user.home", m_fileHome.getAbsolutePath());

        String                                sXml     = "<persistence-environment/>";

        ParameterizedBuilder<PersistenceEnvironment<ReadBuffer>> bldr =
                m_processor.process(new XmlDocumentReference(sXml));

        PersistenceEnvironment<ReadBuffer> envSafe = bldr.realize(RESOLVER, getClass().getClassLoader(), null);
        PersistenceEnvironment<ReadBuffer> env     = SafePersistenceWrappers.unwrap(envSafe);

        assertTrue(envSafe instanceof SafePersistenceWrappers.SafePersistenceEnvironment);
        assertTrue(env instanceof BerkeleyDBEnvironment);

        fileActive   = new File(new File(fileActive, "Cluster"), "Service");
        fileSnapshot = new File(new File(fileSnapshot, "Cluster"), "Service");
        fileTrash    = new File(new File(fileTrash, "Cluster"), "Service");

        PersistenceEnvironmentInfo info = (PersistenceEnvironmentInfo) env;

        assertEquals(fileActive, info.getPersistenceActiveDirectory());
        assertEquals(fileSnapshot, info.getPersistenceSnapshotDirectory());
        assertEquals(fileTrash, info.getPersistenceTrashDirectory());

        // invoke a method with an illegal argument and assert that a
        // wrapper PersistenceException is thrown
        try
            {
            envSafe.createSnapshot(null, NullImplementation.getPersistenceManager(ReadBuffer.class));
            fail("expected PersistenceException");
            }
        catch (PersistenceException e)
            {
            // expected
            assertTrue(e.getCause() instanceof IllegalArgumentException);
            assertEquals(env, e.getPersistenceEnvironment());
            }

        PersistenceManager<ReadBuffer> managerSafe = envSafe.openActive();
        PersistenceManager<ReadBuffer> manager     = SafePersistenceWrappers.unwrap(managerSafe);

        assertTrue(managerSafe instanceof SafePersistenceWrappers.SafePersistenceManager);
        assertTrue(manager instanceof BerkeleyDBManager);

        BerkeleyDBManager managerImpl = (BerkeleyDBManager) manager;

        assertEquals(fileActive, managerImpl.getDataDirectory());

        // invoke a method with an illegal argument and assert that a
        // wrapper PersistenceException is thrown
        try
            {
            managerSafe.close(null);
            fail("expected PersistenceException");
            }
        catch (PersistenceException e)
            {
            // expected
            assertTrue(e.getCause() instanceof IllegalArgumentException);
            assertEquals(env, e.getPersistenceEnvironment());
            assertEquals(manager, e.getPersistenceManager());
            }

        PersistentStore<ReadBuffer> storeSafe = managerSafe.open(TEST_STORE_ID, null);
        PersistentStore<ReadBuffer> store     = SafePersistenceWrappers.unwrap(storeSafe);

        assertTrue(storeSafe instanceof SafePersistenceWrappers.SafePersistentStore);

        // invoke a method with an illegal argument and assert that a
        // wrapper PersistenceException is thrown
        try
            {
            storeSafe.load(-1, BINARY_KEY);
            fail("expected PersistenceException");
            }
        catch (PersistenceException e)
            {
            // expected
            assertTrue(e.getCause() instanceof IllegalArgumentException);
            assertEquals(env, e.getPersistenceEnvironment());
            assertEquals(manager, e.getPersistenceManager());
            assertEquals(store, e.getPersistentStore());
            }

        env.release();
        }

    @Test
    public void testCustomEnvironment()
            throws IOException
        {
        File fileActive   = m_fileActive;
        File fileSnapshot = m_fileSnapshot;
        File fileTrash    = m_fileTrash;

        String sXml =
                  "<persistence-environment>\n"
                + "  <instance>"
                + "    <class-name>config.PersistenceEnvironmentProcessorTests$CustomEnvironment</class-name>\n"
                + "    <init-params>\n"
                + "      <init-param>\n"
                + "        <param-value>{cluster-name}</param-value>\n"
                + "      </init-param>\n"
                + "      <init-param>\n"
                + "        <param-value>{service-name}</param-value>\n"
                + "      </init-param>\n"
                + "      <init-param>\n"
                + "        <param-value>{persistence-mode}</param-value>\n"
                + "      </init-param>\n"
                + "      <init-param>\n"
                + "        <param-value>{active-directory}</param-value>\n"
                + "      </init-param>\n"
                + "      <init-param>\n"
                + "        <param-value>{snapshot-directory}</param-value>\n"
                + "      </init-param>\n"
                + "      <init-param>\n"
                + "        <param-value>{trash-directory}</param-value>\n"
                + "      </init-param>\n"
                + "    </init-params>"
                + "  </instance>\n"
                + "  <active-directory>" + fileActive.getAbsolutePath() + "</active-directory>\n"
                + "  <snapshot-directory>" + fileSnapshot.getAbsolutePath() + "</snapshot-directory>\n"
                + "  <trash-directory>" + fileTrash.getAbsolutePath() + "</trash-directory>\n"
                + "</persistence-environment>";

        PersistenceEnvironmentParamBuilder bldr = m_processor.process(new XmlDocumentReference(sXml));

        PersistenceEnvironment<ReadBuffer> env = bldr.realize(RESOLVER, getClass().getClassLoader(), null);

        env = SafePersistenceWrappers.unwrap(env);
        assertTrue(env instanceof CustomEnvironment);

        fileActive   = new File(new File(fileActive, "Cluster"), "Service");
        fileSnapshot = new File(new File(fileSnapshot, "Cluster"), "Service");
        fileTrash    = new File(new File(fileTrash, "Cluster"), "Service");

        CustomEnvironment envImpl = (CustomEnvironment) env;

        assertEquals("Cluster", envImpl.f_sCluster);
        assertEquals("Service", envImpl.f_sService);
        assertEquals("active", envImpl.f_sMode);
        assertEquals(fileActive, envImpl.f_fileActive);
        assertEquals(fileSnapshot, envImpl.f_fileSnapshot);
        assertEquals(fileTrash, envImpl.f_fileTrash);

        PersistenceEnvironmentInfo info = bldr.getPersistenceEnvironmentInfo("Cluster", "Service");
        assertEquals(fileActive, info.getPersistenceActiveDirectory());
        assertEquals(fileSnapshot, info.getPersistenceSnapshotDirectory());
        assertEquals(fileTrash, info.getPersistenceTrashDirectory());

        env.release();
        }

    @Test
    public void testCustomEnvironmentDefaults()
            throws IOException
        {
        File fileActive   = m_fileActive;
        File fileSnapshot = m_fileSnapshot;
        File fileTrash    = m_fileTrash;

        System.setProperty("user.home", m_fileHome.getAbsolutePath());

        String sXml =
                  "<persistence-environment>\n"
                + "  <instance>"
                + "    <class-name>config.PersistenceEnvironmentProcessorTests$CustomEnvironment</class-name>\n"
                + "    <init-params>\n"
                + "      <init-param>\n"
                + "        <param-value>{cluster-name}</param-value>\n"
                + "      </init-param>\n"
                + "      <init-param>\n"
                + "        <param-value>{service-name}</param-value>\n"
                + "      </init-param>\n"
                + "      <init-param>\n"
                + "        <param-value>{persistence-mode}</param-value>\n"
                + "      </init-param>\n"
                + "      <init-param>\n"
                + "        <param-value>{active-directory}</param-value>\n"
                + "      </init-param>\n"
                + "      <init-param>\n"
                + "        <param-value>{snapshot-directory}</param-value>\n"
                + "      </init-param>\n"
                + "      <init-param>\n"
                + "        <param-value>{trash-directory}</param-value>\n"
                + "      </init-param>\n"
                + "    </init-params>"
                + "  </instance>\n"
                + "  <active-directory>" + fileActive.getAbsolutePath() + "</active-directory>\n"
                + "  <snapshot-directory>" + fileSnapshot.getAbsolutePath() + "</snapshot-directory>\n"
                + "  <trash-directory>" + fileTrash.getAbsolutePath() + "</trash-directory>\n"
                + "</persistence-environment>";

        PersistenceEnvironmentParamBuilder bldr = m_processor.process(new XmlDocumentReference(sXml));
        PersistenceEnvironment<ReadBuffer> env = bldr.realize(RESOLVER, getClass().getClassLoader(), null);

        env = SafePersistenceWrappers.unwrap(env);
        assertTrue(env instanceof CustomEnvironment);

        fileActive   = new File(new File(fileActive, "Cluster"), "Service");
        fileSnapshot = new File(new File(fileSnapshot, "Cluster"), "Service");
        fileTrash    = new File(new File(fileTrash, "Cluster"), "Service");

        CustomEnvironment envImpl = (CustomEnvironment) env;

        assertEquals("Cluster", envImpl.f_sCluster);
        assertEquals("Service", envImpl.f_sService);
        assertEquals("active", envImpl.f_sMode);
        assertEquals(fileActive, envImpl.f_fileActive);
        assertEquals(fileSnapshot, envImpl.f_fileSnapshot);
        assertEquals(fileTrash, envImpl.f_fileTrash);

        PersistenceEnvironmentInfo info = bldr.getPersistenceEnvironmentInfo("Cluster", "Service");
        assertEquals(fileActive, info.getPersistenceActiveDirectory());
        assertEquals(fileSnapshot, info.getPersistenceSnapshotDirectory());
        assertEquals(fileTrash, info.getPersistenceTrashDirectory());

        env.release();
        }

    @Test
    public void testCustomEnvironmentActive()
            throws IOException
        {
        File fileActive   = m_fileActive;
        File fileSnapshot = m_fileSnapshot;
        File fileTrash    = m_fileTrash;

        String sXml =
                  "<persistence-environment>\n"
                + "  <instance>"
                + "    <class-name>config.PersistenceEnvironmentProcessorTests$CustomEnvironment</class-name>\n"
                + "    <init-params>\n"
                + "      <init-param>\n"
                + "        <param-value>{cluster-name}</param-value>\n"
                + "      </init-param>\n"
                + "      <init-param>\n"
                + "        <param-value>{service-name}</param-value>\n"
                + "      </init-param>\n"
                + "      <init-param>\n"
                + "        <param-value>{persistence-mode}</param-value>\n"
                + "      </init-param>\n"
                + "      <init-param>\n"
                + "        <param-value>{active-directory}</param-value>\n"
                + "      </init-param>\n"
                + "      <init-param>\n"
                + "        <param-value>{snapshot-directory}</param-value>\n"
                + "      </init-param>\n"
                + "      <init-param>\n"
                + "        <param-value>{trash-directory}</param-value>\n"
                + "      </init-param>\n"
                + "    </init-params>"
                + "  </instance>\n"
                + "  <persistence-mode>active</persistence-mode>\n"
                + "  <active-directory>" + fileActive.getAbsolutePath() + "</active-directory>\n"
                + "  <snapshot-directory>" + fileSnapshot.getAbsolutePath() + "</snapshot-directory>\n"
                + "  <trash-directory>" + fileTrash.getAbsolutePath() + "</trash-directory>\n"
                + "</persistence-environment>";

        PersistenceEnvironmentParamBuilder bldr
                = m_processor.process(new XmlDocumentReference(sXml));

        PersistenceEnvironment<ReadBuffer> env = bldr.realize(RESOLVER, getClass().getClassLoader(), null);

        env = SafePersistenceWrappers.unwrap(env);
        assertTrue(env instanceof CustomEnvironment);

        fileActive   = new File(new File(fileActive, "Cluster"), "Service");
        fileSnapshot = new File(new File(fileSnapshot, "Cluster"), "Service");
        fileTrash    = new File(new File(fileTrash, "Cluster"), "Service");

        CustomEnvironment envImpl = (CustomEnvironment) env;

        assertEquals("Cluster", envImpl.f_sCluster);
        assertEquals("Service", envImpl.f_sService);
        assertEquals("active", envImpl.f_sMode);
        assertEquals(fileActive, envImpl.f_fileActive);
        assertEquals(fileSnapshot, envImpl.f_fileSnapshot);
        assertEquals(fileTrash, envImpl.f_fileTrash);

        PersistenceEnvironmentInfo info = bldr.getPersistenceEnvironmentInfo("Cluster", "Service");
        assertEquals(fileActive, info.getPersistenceActiveDirectory());
        assertEquals(fileSnapshot, info.getPersistenceSnapshotDirectory());
        assertEquals(fileTrash, info.getPersistenceTrashDirectory());

        env.release();
        }

    @Test
    public void testCustomEnvironmentActiveBackup()
            throws IOException
        {
        File fileActive   = m_fileActive;
        File fileBackup   = m_fileBackup;
        File fileSnapshot = m_fileSnapshot;
        File fileTrash    = m_fileTrash;

        String sXml =
                "<persistence-environment>\n"
                + "  <instance>"
                + "    <class-name>config.PersistenceEnvironmentProcessorTests$CustomEnvironment</class-name>\n"
                + "    <init-params>\n"
                + "      <init-param>\n"
                + "        <param-value>{cluster-name}</param-value>\n"
                + "      </init-param>\n"
                + "      <init-param>\n"
                + "        <param-value>{service-name}</param-value>\n"
                + "      </init-param>\n"
                + "      <init-param>\n"
                + "        <param-value>{persistence-mode}</param-value>\n"
                + "      </init-param>\n"
                + "      <init-param>\n"
                + "        <param-value>{active-directory}</param-value>\n"
                + "      </init-param>\n"
                + "      <init-param>\n"
                + "        <param-value>{backup-directory}</param-value>\n"
                + "      </init-param>\n"
                + "      <init-param>\n"
                + "        <param-value>{snapshot-directory}</param-value>\n"
                + "      </init-param>\n"
                + "      <init-param>\n"
                + "        <param-value>{trash-directory}</param-value>\n"
                + "      </init-param>\n"
                + "    </init-params>"
                + "  </instance>\n"
                + "  <persistence-mode>active-backup</persistence-mode>\n"
                + "  <active-directory>" + fileActive.getAbsolutePath() + "</active-directory>\n"
                + "  <backup-directory>" + fileBackup.getAbsolutePath() + "</backup-directory>\n"
                + "  <snapshot-directory>" + fileSnapshot.getAbsolutePath() + "</snapshot-directory>\n"
                + "  <trash-directory>" + fileTrash.getAbsolutePath() + "</trash-directory>\n"
                + "</persistence-environment>";

        PersistenceEnvironmentParamBuilder bldr
                = m_processor.process(new XmlDocumentReference(sXml));

        PersistenceEnvironment<ReadBuffer> env = bldr.realize(RESOLVER, getClass().getClassLoader(), null);

        env = SafePersistenceWrappers.unwrap(env);
        assertTrue(env instanceof CustomEnvironment);

        fileActive   = new File(new File(fileActive, "Cluster"), "Service");
        fileBackup   = new File(new File(fileBackup, "Cluster"), "Service");
        fileSnapshot = new File(new File(fileSnapshot, "Cluster"), "Service");
        fileTrash    = new File(new File(fileTrash, "Cluster"), "Service");

        CustomEnvironment envImpl = (CustomEnvironment) env;

        assertEquals("Cluster", envImpl.f_sCluster);
        assertEquals("Service", envImpl.f_sService);
        assertEquals("active-backup", envImpl.f_sMode);
        assertEquals(fileActive, envImpl.f_fileActive);
        assertEquals(fileBackup, envImpl.f_fileBackup);
        assertEquals(fileSnapshot, envImpl.f_fileSnapshot);
        assertEquals(fileTrash, envImpl.f_fileTrash);

        PersistenceEnvironmentInfo info = bldr.getPersistenceEnvironmentInfo("Cluster", "Service");
        assertEquals(fileActive, info.getPersistenceActiveDirectory());
        assertEquals(fileBackup, info.getPersistenceBackupDirectory());
        assertEquals(fileSnapshot, info.getPersistenceSnapshotDirectory());
        assertEquals(fileTrash, info.getPersistenceTrashDirectory());

        env.release();
        }

    @Test
    public void testCustomEnvironmentOnDemand()
            throws IOException
        {
        File fileSnapshot = m_fileSnapshot;
        File fileTrash    = m_fileTrash;

        String sXml =
                  "<persistence-environment>\n"
                + "  <instance>"
                + "    <class-name>config.PersistenceEnvironmentProcessorTests$CustomEnvironment</class-name>\n"
                + "    <init-params>\n"
                + "      <init-param>\n"
                + "        <param-value>{cluster-name}</param-value>\n"
                + "      </init-param>\n"
                + "      <init-param>\n"
                + "        <param-value>{service-name}</param-value>\n"
                + "      </init-param>\n"
                + "      <init-param>\n"
                + "        <param-value>{persistence-mode}</param-value>\n"
                + "      </init-param>\n"
                + "      <init-param>\n"
                + "        <param-value>{active-directory}</param-value>\n"
                + "      </init-param>\n"
                + "      <init-param>\n"
                + "        <param-value>{snapshot-directory}</param-value>\n"
                + "      </init-param>\n"
                + "      <init-param>\n"
                + "        <param-value>{trash-directory}</param-value>\n"
                + "      </init-param>\n"
                + "    </init-params>"
                + "  </instance>\n"
                + "  <persistence-mode>on-demand</persistence-mode>\n"
                + "  <snapshot-directory>" + fileSnapshot.getAbsolutePath() + "</snapshot-directory>\n"
                + "  <trash-directory>" + fileTrash.getAbsolutePath() + "</trash-directory>\n"
                + "</persistence-environment>";

        PersistenceEnvironmentParamBuilder bldr = m_processor.process(new XmlDocumentReference(sXml));
        PersistenceEnvironment<ReadBuffer> env = bldr.realize(RESOLVER, getClass().getClassLoader(), null);

        env = SafePersistenceWrappers.unwrap(env);
        assertTrue(env instanceof CustomEnvironment);

        fileSnapshot = new File(new File(fileSnapshot, "Cluster"), "Service");
        fileTrash    = new File(new File(fileTrash, "Cluster"), "Service");

        CustomEnvironment envImpl = (CustomEnvironment) env;

        assertEquals("Cluster", envImpl.f_sCluster);
        assertEquals("Service", envImpl.f_sService);
        assertEquals("on-demand", envImpl.f_sMode);
        assertNull(envImpl.f_fileActive);
        assertEquals(envImpl.f_fileSnapshot, fileSnapshot);
        assertEquals(envImpl.f_fileTrash, fileTrash);

        PersistenceEnvironmentInfo info = bldr.getPersistenceEnvironmentInfo("Cluster", "Service");
        assertNull(info.getPersistenceActiveDirectory());
        assertEquals(fileSnapshot, info.getPersistenceSnapshotDirectory());
        assertEquals(fileTrash, info.getPersistenceTrashDirectory());

        env.release();
        }

    @Test
    public void testCustomEnvironmentSafe()
            throws IOException
        {
        File fileActive   = m_fileActive;
        File fileSnapshot = m_fileSnapshot;
        File fileTrash    = m_fileTrash;

        System.setProperty("user.home", m_fileHome.getAbsolutePath());

        String sXml =
                  "<persistence-environment>\n"
                + "  <instance>"
                + "    <class-name>config.PersistenceEnvironmentProcessorTests$CustomEnvironment</class-name>\n"
                + "    <init-params>\n"
                + "      <init-param>\n"
                + "        <param-value>{cluster-name}</param-value>\n"
                + "      </init-param>\n"
                + "      <init-param>\n"
                + "        <param-value>{service-name}</param-value>\n"
                + "      </init-param>\n"
                + "      <init-param>\n"
                + "        <param-value>{persistence-mode}</param-value>\n"
                + "      </init-param>\n"
                + "      <init-param>\n"
                + "        <param-value>{active-directory}</param-value>\n"
                + "      </init-param>\n"
                + "      <init-param>\n"
                + "        <param-value>{snapshot-directory}</param-value>\n"
                + "      </init-param>\n"
                + "      <init-param>\n"
                + "        <param-value>{trash-directory}</param-value>\n"
                + "      </init-param>\n"
                + "    </init-params>"
                + "  </instance>\n"
                + "</persistence-environment>";

        ParameterizedBuilder<PersistenceEnvironment<ReadBuffer>> bldr =
                m_processor.process(new XmlDocumentReference(sXml));

        PersistenceEnvironment<ReadBuffer> envSafe = bldr.realize(RESOLVER, getClass().getClassLoader(), null);
        PersistenceEnvironment<ReadBuffer> env     = SafePersistenceWrappers.unwrap(envSafe);

        assertTrue(envSafe instanceof SafePersistenceWrappers.SafePersistenceEnvironment);
        assertTrue(env instanceof CustomEnvironment);

        fileActive   = new File(new File(fileActive, "Cluster"), "Service");
        fileSnapshot = new File(new File(fileSnapshot, "Cluster"), "Service");
        fileTrash    = new File(new File(fileTrash, "Cluster"), "Service");

        CustomEnvironment envImpl = (CustomEnvironment) env;

        assertEquals("Cluster", envImpl.f_sCluster);
        assertEquals("Service", envImpl.f_sService);
        assertEquals("active", envImpl.f_sMode);
        assertEquals(fileActive, envImpl.f_fileActive);
        assertEquals(fileSnapshot, envImpl.f_fileSnapshot);
        assertEquals(fileTrash, envImpl.f_fileTrash);

        // invoke a method with an illegal argument and assert that a
        // wrapper PersistenceException is thrown
        try
            {
            envSafe.createSnapshot(null, NullImplementation.getPersistenceManager(ReadBuffer.class));
            fail("expected PersistenceException");
            }
        catch (PersistenceException e)
            {
            // expected
            assertTrue(e.getCause() instanceof IllegalArgumentException);
            }

        env.release();
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Create and return a mock member.
     *
     * @param nMember  the member id
     *
     * @return the mock member
     */
    public static Member getMockMember(int nMember)
        {
        Member member = mock(Member.class);

        when(member.getId()).thenReturn(nMember);
        when(member.getUid()).thenReturn(new UID(2130706433 /* 127.0.0.1 */, new Date().getTime(), nMember));

        return member;
        }

    // ----- inner class: CustomEnvironment ---------------------------------

    public static class CustomEnvironment
            implements PersistenceEnvironment<ReadBuffer>
        {
        // ----- constructors -----------------------------------------------

        public CustomEnvironment(String sCluster, String sService, String sMode, File fileActive, File fileSnapshot,
                                 File fileTrash)
            {
            f_sCluster     = sCluster;
            f_sService     = sService;
            f_sMode        = sMode;
            f_fileActive   = fileActive;
            f_fileSnapshot = fileSnapshot;
            f_fileTrash    = fileTrash;
            f_fileBackup   = null;
            }

        public CustomEnvironment(String sCluster, String sService, String sMode, File fileActive, File fileBackup,
                                 File fileSnapshot, File fileTrash)
            {
            f_sCluster     = sCluster;
            f_sService     = sService;
            f_sMode        = sMode;
            f_fileActive   = fileActive;
            f_fileBackup   = fileBackup;
            f_fileSnapshot = fileSnapshot;
            f_fileTrash    = fileTrash;
            }

        @Override
        public PersistenceManager<ReadBuffer> openActive()
            {
            return "active".equals(f_sMode) ? NullImplementation.getPersistenceManager(ReadBuffer.class) : null;
            }

        @Override
        public PersistenceManager<ReadBuffer> openBackup()
            {
            return "active-backup".equals(f_sMode) ? NullImplementation.getPersistenceManager(ReadBuffer.class) : null;
            }

        @Override
        public PersistenceManager<ReadBuffer> openSnapshot(String sSnapshot)
            {
            return NullImplementation.getPersistenceManager(ReadBuffer.class);
            }

        @Override
        public PersistenceManager<ReadBuffer> createSnapshot(String sSnapshot, PersistenceManager<ReadBuffer> manager)
            {
            if (sSnapshot == null)
                {
                throw new IllegalArgumentException();
                }

            return NullImplementation.getPersistenceManager(ReadBuffer.class);
            }

        @Override
        public boolean removeSnapshot(String sSnapshot)
            {
            return false;
            }

        @Override
        public String[] listSnapshots()
            {
            return new String[0];
            }

        @Override
        public void release()
            {
            }

        protected final String f_sCluster;
        protected final String f_sService;
        protected final String f_sMode;
        protected final File   f_fileActive;
        protected final File   f_fileBackup;
        protected final File   f_fileSnapshot;
        protected final File   f_fileTrash;
        }

    // ----- constants ------------------------------------------------------

    /**
     * The name of the test persistent store identifier.
     */
    public static String TEST_STORE_ID = GUIDHelper.generateGUID(1, 1L, new Date().getTime(), getMockMember(1));

    /**
     * Test Binary keys.
     */
    public static final Binary BINARY_KEY = new Binary(new byte[]
        {
        0, 1, 2, 3, 4, 5, 6, 7
        });

    private static final ResolvableParameterList RESOLVER = new ResolvableParameterList()
        {{
        add(new Parameter("cluster-name", String.class, "Cluster"));
        add(new Parameter("service-name", String.class, "Service"));
        }};

    // ----- data members ---------------------------------------------------

    protected File                                  m_fileHome;
    protected File                                  m_fileActive;
    protected File                                  m_fileBackup;
    protected File                                  m_fileSnapshot;
    protected File                                  m_fileTrash;
    protected DocumentProcessor                     m_processor;
    }
