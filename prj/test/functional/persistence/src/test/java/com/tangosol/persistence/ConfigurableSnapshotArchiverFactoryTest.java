/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.persistence;

import com.oracle.coherence.persistence.PersistenceEnvironment;

import com.oracle.datagrid.persistence.PersistenceTools;

import com.tangosol.io.FileHelper;
import com.tangosol.io.ReadBuffer;

import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit test for ConfigurableSnapshotArchiverFactory.
 *
 * @author jh  2014.08.15
 */
public class ConfigurableSnapshotArchiverFactoryTest
    {
    // ----- test methods ---------------------------------------------------

    @Test
    public void testCustomArchiver()
        {
        XmlElement xml = XmlHelper.loadXml(
                "<custom-archiver>\n" +
                "  <class-name>com.tangosol.persistence.ConfigurableSnapshotArchiverFactoryTest$CustomArchiver</class-name>\n" +
                "  <init-params>\n" +
                "    <init-param>\n" +
                "      <param-value>{cluster-name}</param-value>\n" +
                "    </init-param>\n" +
                "    <init-param>\n" +
                "      <param-value>{service-name}</param-value>\n" +
                "    </init-param>\n" +
                "  </init-params>" +
                "</custom-archiver>");

        ConfigurableSnapshotArchiverFactory factory = new ConfigurableSnapshotArchiverFactory();
        factory.setConfig(xml);

        SnapshotArchiver archiver = factory.createSnapshotArchiver("Cluster", "Service");
        assertTrue(archiver instanceof CustomArchiver);

        CustomArchiver archiverImpl = (CustomArchiver) archiver;
        assertEquals("Cluster", archiverImpl.f_sCluster);
        assertEquals("Service", archiverImpl.f_sService);
        }

    @Test
    public void testCustomArchiverWithURI()
        {
        XmlElement xml = XmlHelper.loadXml(
                "<custom-archiver>\n" +
                        "  <class-name>com.tangosol.persistence.ConfigurableSnapshotArchiverFactoryTest$CustomArchiver</class-name>\n" +
                        "  <init-params>\n" +
                        "    <init-param>\n" +
                        "      <param-value>{cluster-name}</param-value>\n" +
                        "    </init-param>\n" +
                        "    <init-param>\n" +
                        "      <param-value>{service-name}</param-value>\n" +
                        "    </init-param>\n" +
                        "    <init-param>\n" +
                        "      <param-type>string</param-type>\n" +
                        "      <param-value>my-custom-uri</param-value>\n" +
                        "    </init-param>\n" +
                        "  </init-params>" +
                        "</custom-archiver>");

        ConfigurableSnapshotArchiverFactory factory = new ConfigurableSnapshotArchiverFactory();
        factory.setConfig(xml);

        SnapshotArchiver archiver = factory.createSnapshotArchiver("Cluster", "Service");
        assertTrue(archiver instanceof CustomArchiver);

        CustomArchiver archiverImpl = (CustomArchiver) archiver;
        assertEquals("Cluster", archiverImpl.f_sCluster);
        assertEquals("Service", archiverImpl.f_sService);
        assertEquals("my-custom-uri", archiverImpl.m_sURI);
        }

     @Test
     public void testDirectoryArchiver() throws IOException
        {
        File fileArchive = FileHelper.createTempDir();

        XmlElement xml = XmlHelper.loadXml(
                    "<directory-archiver id=\"archiver\">\n" +
                    "  <archive-directory>" + fileArchive.getAbsolutePath() + "</archive-directory>\n" +
                    "</directory-archiver>");

        ConfigurableSnapshotArchiverFactory factory = new ConfigurableSnapshotArchiverFactory();
        factory.setConfig(xml);

        SnapshotArchiver archiver = factory.createSnapshotArchiver("Cluster", "Service");
        assertTrue(archiver instanceof DirectorySnapshotArchiver);

        DirectorySnapshotArchiver archiverImpl = (DirectorySnapshotArchiver) archiver;
        assertEquals("Cluster", archiverImpl.f_sClusterName);
        assertEquals("Service", archiverImpl.f_sServiceName);
        assertEquals(fileArchive.getAbsolutePath() + SEP + "Cluster" + SEP + "Service",
                archiverImpl.getSharedDirectoryPath().getAbsolutePath());
        }

    // ----- inner class: CustomArchiver ---------------------------------

    public static class CustomArchiver
            implements SnapshotArchiver
        {
        public CustomArchiver(String sCluster, String sService)
            {
            f_sCluster = sCluster;
            f_sService = sService;
            }

        public CustomArchiver(String sCluster, String sService, String sURI)
            {
            f_sCluster = sCluster;
            f_sService = sService;
            m_sURI     = sURI;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public String[] list()
            {
            return new String[0];
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public Snapshot get(String sSnapshot)
            {
            return null;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean remove(String sSnapshot)
            {
            return false;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public void archive(Snapshot snapshot, PersistenceEnvironment<ReadBuffer> env)
            {
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public void retrieve(Snapshot snapshot, PersistenceEnvironment<ReadBuffer> env)
            {
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public PersistenceTools getPersistenceTools(String sSnapshot)
            {
            return null;
            }

        protected final String f_sCluster;
        protected final String f_sService;
        protected String       m_sURI;
        }

    protected final static String SEP = File.separator;
    }
