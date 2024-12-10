/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.builder;

import com.oracle.coherence.common.base.Continuation;

import com.oracle.coherence.persistence.PersistenceEnvironment;
import com.oracle.coherence.persistence.PersistenceManager;
import com.oracle.coherence.persistence.PersistentStore;

import com.tangosol.coherence.config.Config;
import com.tangosol.coherence.config.ParameterList;
import com.tangosol.coherence.config.ResolvableParameterList;

import com.tangosol.config.ConfigurationException;
import com.tangosol.config.annotation.Injectable;
import com.tangosol.config.expression.Parameter;
import com.tangosol.config.expression.ParameterResolver;

import com.tangosol.internal.net.service.grid.PersistenceDependencies;

import com.tangosol.io.FileHelper;
import com.tangosol.io.ReadBuffer;

import com.tangosol.persistence.AbstractPersistenceEnvironment;
import com.tangosol.persistence.CachePersistenceHelper;
import com.tangosol.persistence.SafePersistenceWrappers;
import com.tangosol.persistence.bdb.BerkeleyDBEnvironment;

import com.tangosol.util.Base;

import java.io.File;

/**
 * Build a {@link PersistenceEnvironment}.
 * <p>
 * Provide a means to get a {@link PersistenceEnvironmentInfo} without
 * creating a {@link PersistenceEnvironment} to be used by a viewer.
 * <p>
 * Defaulting built into the builder so same defaults whether
 * read in from xml configuration or constructed solely by builder api.
 *
 * @author jf  2015.03.12
 * @since Coherence 12.2.1
 */
// TODO: rename class to PersistenceEnvironmentBuilder once the unnecessary
//       interface named PersistenceEnvironmentBuilder has been removed
public class PersistenceEnvironmentParamBuilder
        implements ParameterizedBuilder<PersistenceEnvironment>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a {@link PersistenceEnvironment} builder.
     */
    public PersistenceEnvironmentParamBuilder()
        {
        // Determine default persistence directories.
        String sHome = getDefaultPersistenceEnvironmentHomeDirectory();

        m_sActive   = sHome + File.separatorChar + CachePersistenceHelper.DEFAULT_ACTIVE_DIR;
        m_sBackup   = sHome + File.separatorChar + CachePersistenceHelper.DEFAULT_BACKUP_DIR;
        m_sSnapshot = sHome + File.separatorChar + CachePersistenceHelper.DEFAULT_SNAPSHOT_DIR;
        m_sTrash    = sHome + File.separatorChar + CachePersistenceHelper.DEFAULT_TRASH_DIR;
        m_sMode     = "active";

        // events directory must be explicitly specified for now
        }

    // ----- ParameterizedBuilder methods -----------------------------------

    @Override
    public PersistenceEnvironment<ReadBuffer> realize(ParameterResolver resolver, ClassLoader loader,
            ParameterList listParameters)
        {
        if (!getPersistenceMode().equalsIgnoreCase(PersistenceDependencies.MODE_ACTIVE) &&
            !getPersistenceMode().equalsIgnoreCase(PersistenceDependencies.MODE_ACTIVE_BACKUP) &&
            !getPersistenceMode().equalsIgnoreCase(PersistenceDependencies.MODE_ACTIVE_ASYNC) &&
            !getPersistenceMode().equalsIgnoreCase(PersistenceDependencies.MODE_ON_DEMAND))
            {
            throw new ConfigurationException("Invalid persistence mode: '" + getPersistenceMode() + "'.",
                                             "Valid values are: '" +
                                             PersistenceDependencies.MODE_ACTIVE + "', '" +
                                             PersistenceDependencies.MODE_ACTIVE_BACKUP + "', '" +
                                             PersistenceDependencies.MODE_ACTIVE_ASYNC + "' and '" +
                                             PersistenceDependencies.MODE_ON_DEMAND + "'");
            }

        String sServiceName = getValueAsString(resolver, "service-name");
        String sClusterName = getValueAsString(resolver, "cluster-name");

        PersistenceEnvironmentInfo info = getPersistenceEnvironmentInfo(sClusterName, sServiceName);

        PersistenceEnvironment<ReadBuffer> environment;
        try
            {
            // default to a BerkeleyDBEnvironment or delegate to the builder
            environment = m_bldr == null
                    ? new BerkeleyDBEnvironment(
                            info.getPersistenceActiveDirectory(),
                            info.getPersistenceBackupDirectory(),
                            info.getPersistenceEventsDirectory(),
                            info.getPersistenceSnapshotDirectory(),
                            info.getPersistenceTrashDirectory())
                    : m_bldr.realize(createResolver(sClusterName, sServiceName), loader, listParameters);
            }
        catch (Exception e)
            {
            throw Base.ensureRuntimeException(e,
                    "Error creating PersistenceEnvironment: " + this);
            }
        return wrap(environment);
        }

    // ----- PersistenceEnvironmentBuilder methods --------------------------

    /**
     * Return a {@link PersistenceEnvironmentInfo} encapsulating the configuration
     * this builder uses to construct {@link PersistenceEnvironment environments}.
     *
     * @param sClusterName  the cluster name
     * @param sServiceName  the service name
     *
     * @return a PersistenceEnvironmentInfo encapsulating the configuration
     *         this builder uses to construct environments
     */
    public PersistenceEnvironmentInfo getPersistenceEnvironmentInfo(String sClusterName, String sServiceName)
        {
        // validate the cluster name and create a filesystem-safe version
        if (sClusterName == null || (sClusterName = sClusterName.trim()).length() == 0)
            {
            throw new IllegalArgumentException("invalid cluster name");
            }

        String sCluster = FileHelper.toFilename(sClusterName);

        // validate the service name and create a filesystem-safe version
        if (sServiceName == null || (sServiceName = sServiceName.trim()).length() == 0)
            {
            throw new IllegalArgumentException("invalid service name");
            }

        String sService = FileHelper.toFilename(sServiceName);

        // create a directory reference for the active, snapshot and trash locations
        File fileActive   = isActive() ? new File(new File(new File(m_sActive), sCluster), sService) : null;
        File fileBackup   = isActiveBackup() ? new File(new File(new File(m_sBackup), sCluster), sService) : null;
        File fileSnapshot = new File(new File(new File(m_sSnapshot), sCluster), sService);
        File fileTrash    = new File(new File(new File(m_sTrash), sCluster), sService);
        
        File fileEvents = m_sEvents == null || m_sEvents.isEmpty() || !isActive()
                ? null : new File(new File(new File(m_sEvents), sCluster), sService);

        return new PersistenceEnvironmentInfo(m_sMode, fileActive, fileBackup, fileEvents, fileSnapshot, fileTrash);
        }

    public String getPersistenceMode()
        {
        return m_sMode;
        }

    /**
     * Set persistence mode.
     *
     * @param sMode  active, active-backup, active-async or on-demand
     */
    @Injectable("persistence-mode")
    public void setPersistenceMode(String sMode)
        {
        if (sMode != null && sMode.length() > 0)
            {
            m_sMode = sMode;
            }
        }

    /**
     * Set the persistence active directory.
     *
     * @param sPathname  either relative or absolute pathname
     */
    @Injectable("active-directory")
    public void setActiveDirectory(String sPathname)
        {
        if (sPathname != null && sPathname.length() > 0)
            {
            m_sActive = sPathname;
            }
        }

    /**
     * Set the persistence backup directory.
     *
     * @param sPathname  either relative or absolute pathname
     */
    @Injectable("backup-directory")
    public void setBackupDirectory(String sPathname)
        {
        if (sPathname != null && sPathname.length() > 0)
            {
            m_sBackup = sPathname;
            }
        }

    /**
     * Set the persistence active directory.
     *
     * @param sPathname  either relative or absolute pathname
     */
    @Injectable("events-directory")
    public void setEventsDirectory(String sPathname)
        {
        if (sPathname != null && sPathname.length() > 0)
            {
            m_sEvents = sPathname;
            }
        }

    /**
     * Set the persistence snapshot directory.
     *
     * @param sPathname  either relative or absolute pathname
     */
    @Injectable("snapshot-directory")
    public void setPersistenceSnapshotDirectory(String sPathname)
        {
        if (sPathname != null && sPathname.length() > 0)
            {
            m_sSnapshot = sPathname;
            }
        }

    /**
     * Set the persistence trash directory.
     *
     * @param sPathname  either relative or absolute pathname
     */
    @Injectable("trash-directory")
    public void setPersistenceTrashDirectory(String sPathname)
        {
        if (sPathname != null && sPathname.length() > 0)
            {
            m_sTrash = sPathname;
            }
        }

    /**
     * Set a {@link ParameterizedBuilder builder} to be used instantiate the
     * appropriate {@link PersistenceEnvironment}.
     *
     * @param bldrPersistence  the builder that creates a PersistenceEnvironment
     */
    @Injectable("instance")
    public void setCustomEnvironment(ParameterizedBuilder<PersistenceEnvironment<ReadBuffer>> bldrPersistence)
        {
        m_bldr = bldrPersistence;
        }

    // ----- Object methods -------------------------------------------------

    @Override
    public String toString()
        {
        StringBuilder sb = new StringBuilder()
          .append("\n        Mode: ").append(m_sMode)
          .append("\n        Active Location: ").append(m_sActive)
          .append("\n        Backup Location: ").append(m_sBackup)
          .append("\n        Events Location: ").append(m_sEvents)
          .append("\n        Snapshot Location:").append(m_sSnapshot)
          .append("\n        Trash Location:").append(m_sTrash);

        if (m_bldr != null)
            {
            sb.append("\n         Instance: ").append(m_bldr);
            }

        return sb.toString();
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Wrap a {@link PersistenceEnvironment} with a SafePersistenceEnvironment.
     *
     * @param env  a wrapped PersistenceEnvironment
     *
     * @return a wrapped PersistenceEnvironment
     */
    protected PersistenceEnvironment<ReadBuffer> wrap(PersistenceEnvironment<ReadBuffer> env)
        {
        // wrap the environment in a safe layer
        return new SafePersistenceWrappers.SafePersistenceEnvironment<>(env, DEFAULT_FACTORY);
        }

    /**
     * Create a {@link ResolvableParameterList resolver} based on the provided
     * cluster, service name and the state of this builder (active, snapshot
     * and trash directories).
     *
     * @param sClusterName  the name of the cluster
     * @param sServiceName  the name of the service
     *
     * @return a resolver based on the provided cluster and service name
     */
    protected ResolvableParameterList createResolver(String sClusterName, String sServiceName)
        {
        PersistenceEnvironmentParamBuilder.PersistenceEnvironmentInfo info =
            getPersistenceEnvironmentInfo(sClusterName, sServiceName);
        ResolvableParameterList resolver = new ResolvableParameterList();

        resolver.add(new Parameter("cluster-name", String.class, sClusterName));
        resolver.add(new Parameter("service-name", String.class, sServiceName));
        resolver.add(new Parameter("persistence-mode", String.class, info.getPersistenceMode()));
        resolver.add(new Parameter("active-directory", File.class, info.getPersistenceActiveDirectory()));
        resolver.add(new Parameter("backup-directory", File.class, info.getPersistenceBackupDirectory()));
        resolver.add(new Parameter("events-directory", File.class, info.getPersistenceEventsDirectory()));
        resolver.add(new Parameter("snapshot-directory", File.class, info.getPersistenceSnapshotDirectory()));
        resolver.add(new Parameter("trash-directory", File.class, info.getPersistenceTrashDirectory()));

        return resolver;
        }

    /**
     * Return true if the persistence mode is active.
     *
     * @return true if the persistence mode is active
     */
    protected boolean isActive()
        {
        return m_sMode.equalsIgnoreCase(PersistenceDependencies.MODE_ACTIVE) ||
               m_sMode.equalsIgnoreCase(PersistenceDependencies.MODE_ACTIVE_BACKUP) ||
               m_sMode.equalsIgnoreCase(PersistenceDependencies.MODE_ACTIVE_ASYNC);
        }

    /**
     * Return true if the persistence mode is active-backup.
     *
     * @return true if the persistence mode is active-backup
     */
    protected boolean isActiveBackup()
        {
        return m_sMode.equalsIgnoreCase(PersistenceDependencies.MODE_ACTIVE_BACKUP);
        }

    /**
     * Return the default 'base' persistence directory.
     * <p>
     * This location is the base for active, snapshot and trash locations if
     * they have not been specified in the operational configuration.
     * <p>
     * The default base location is {@code $HOME/coherence}.
     * This location can be overridden by specifying the JVM argument:
     * -D{@value CachePersistenceHelper#DEFAULT_BASE_DIR_PROPERTY}.
     *
     * @return default base directory for persistence
     */
    public static String getDefaultPersistenceEnvironmentHomeDirectory()
        {
        String sHome = Config.getProperty(CachePersistenceHelper.DEFAULT_BASE_DIR_PROPERTY);

        if (sHome == null)
            {
            sHome = System.getProperty("user.home") + File.separatorChar + CachePersistenceHelper.DEFAULT_BASE_DIR;
            }

        return sHome;
        }

    /**
     * Return a string value from the given {@link ParameterResolver} and parameter
     * name.
     *
     * @param resolver    resolver
     * @param sParamName  parameter name
     *
     * @return a string value from the given ParameterResolver and parameter
     *         name
     */
    private static String getValueAsString(ParameterResolver resolver, String sParamName)
        {
        Parameter p = resolver.resolve(sParamName);

        return p == null ? "" : p.evaluate(resolver).as(String.class);
        }

    // ----- inner class: DefaultFailureContinuation ------------------------

    /**
     * Default failure continuation factory that provides continuations that
     * only throw PersistenceExceptions.
     */
    protected static final SafePersistenceWrappers.FailureContinuationFactory DEFAULT_FACTORY =
        new SafePersistenceWrappers.FailureContinuationFactory()
        {
        @Override
        public Continuation getEnvironmentContinuation(PersistenceEnvironment env)
            {
            return new AbstractPersistenceEnvironment.DefaultFailureContinuation(env);
            }

        @Override
        public Continuation getManagerContinuation(PersistenceManager mgr)
            {
            return new AbstractPersistenceEnvironment.DefaultFailureContinuation(mgr);
            }

        @Override
        public Continuation getStoreContinuation(PersistentStore store)
            {
            return new AbstractPersistenceEnvironment.DefaultFailureContinuation(store);
            }
        };

    // ----- inner class: PersistenceEnvironmentInfo ------------------------

    /**
     * A {@link com.tangosol.persistence.PersistenceEnvironmentInfo PersistenceEnvironmentInfo}
     * implementation that exposes the active, snapshot and trash directories,
     * in addition to the persistence mode.
     */
    public static class PersistenceEnvironmentInfo
            implements com.tangosol.persistence.PersistenceEnvironmentInfo
        {
        // ----- constructors -----------------------------------------------

        /**
         * Constructs {@link PersistenceEnvironmentInfo}.
         *
         * @param dirActive    active directory
         * @param dirSnapshot  snapshot directory
         * @param dirTrash     trash directory
         * @param sMode        persistence mode (active or on-demand)
         */
        public PersistenceEnvironmentInfo(String sMode, File dirActive, File dirBackup, File dirEvents,
                                          File dirSnapshot, File dirTrash)
            {
            f_sMode       = sMode;
            f_dirActive   = dirActive;
            f_dirEvents   = dirEvents;
            f_dirBackup   = dirBackup;
            f_dirSnapshot = dirSnapshot;
            f_dirTrash    = dirTrash;
            }

        // ------ PersistenceEnvironmentInfo interface ------------------------

        @Override
        public File getPersistenceActiveDirectory()
            {
            return f_dirActive;
            }

        @Override
        public File getPersistenceBackupDirectory()
            {
            return f_dirBackup;
            }

        @Override
        public File getPersistenceEventsDirectory()
            {
            return f_dirEvents;
            }

        @Override
        public File getPersistenceSnapshotDirectory()
            {
            return f_dirSnapshot;
            }

        @Override
        public File getPersistenceTrashDirectory()
            {
            return f_dirTrash;
            }

        @Override
        public long getPersistenceActiveSpaceUsed()
            {
            return 0L;
            }

        @Override
        public long getPersistenceBackupSpaceUsed()
            {
            return 0L;
            }

        // ----- accessors --------------------------------------------------

        /**
         * Return the persistence mode.
         *
         * @return the persistence mode (active or on-demand)
         */
        public String getPersistenceMode()
            {
            return f_sMode;
            }

        /**
         * Return whether the persistence mode is active.
         *
         * @return true if the persistence mode is active
         */
        public boolean isActive()
            {
            return "active".equalsIgnoreCase(f_sMode);
            }

        // ------ Object methods --------------------------------------------

        @Override
        public String toString()
            {
            return getClass().getSimpleName() +
                    "{mode=" + f_sMode +
                    ", activeDir=" + (f_dirActive == null ? null : f_dirActive.getAbsoluteFile()) +
                    ", snapshotDir=" + f_dirSnapshot.getAbsoluteFile() +
                    ", trashDir=" + f_dirTrash.getAbsoluteFile() + '}';
            }

        // ------ data members ----------------------------------------------

        /**
         * Path to the active directory.
         */
        private final File f_dirActive;

        /**
         * Path to the backup directory.
         */
        private final File f_dirBackup;

        /**
         * Path to the events directory.
         */
        private final File f_dirEvents;

        /**
         * Path to the snapshot directory.
         */
        private final File f_dirSnapshot;

        /**
         * Path to the trash directory.
         */
        private final File f_dirTrash;

        /**
         * The persistence mode.
         */
        private final String f_sMode;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The mode used by persistence; either active, active-backup, active-async or on-demand.
     */
    protected String m_sMode;

    /**
     * The active directory used by persistence.
     */
    protected String m_sActive;

    /**
     * The backup directory used by persistence.
     */
    protected String m_sBackup;

    /**
     * The events directory used by persistence.
     */
    protected String m_sEvents;

    /**
     * The snapshot directory used by persistence.
     */
    protected String m_sSnapshot;

    /**
     * The trash directory used by persistence.
     */
    protected String m_sTrash;

    /**
     * A {@link ParameterizedBuilder} that creates a {@link PersistenceEnvironment}.
     */
    protected ParameterizedBuilder<PersistenceEnvironment<ReadBuffer>> m_bldr;
    }
