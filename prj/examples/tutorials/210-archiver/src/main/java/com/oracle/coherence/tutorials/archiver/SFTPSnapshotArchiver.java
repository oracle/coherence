/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.tutorials.archiver;

import com.oracle.coherence.common.base.Blocking;

import com.oracle.coherence.persistence.PersistenceManager;

import com.tangosol.io.FileHelper;
import com.tangosol.io.ReadBuffer;

import com.tangosol.net.GuardSupport;

import com.tangosol.persistence.AbstractSnapshotArchiver;
import com.tangosol.persistence.CachePersistenceHelper;
import com.tangosol.persistence.GUIDHelper;
import com.tangosol.persistence.Snapshot;
import com.tangosol.persistence.SnapshotArchiver;

import org.apache.sshd.client.SshClient;

import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.sftp.client.SftpClient;
import org.apache.sshd.sftp.client.SftpClient.CloseableHandle;
import org.apache.sshd.sftp.client.SftpClientFactory;
import org.apache.sshd.sftp.common.SftpException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.net.URI;

import java.nio.file.Files;
import java.nio.file.Path;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.apache.sshd.sftp.common.SftpConstants.SSH_FX_NO_SUCH_FILE;

/**
 * An implementation of a {@link SnapshotArchiver} that uses SFTP
 * to store and retrieve archived snapshots.<br>
 * To utilize this, refer to the example tangosol-coherence-override.xml
 * file in the test source tree.<br>
 * Replace the third argument with your URI similar to the following:<br>
 * sftp://user:passwd@localhost:port/path<br>
 * You may omit the password if you have setup SSH equivalence.<br>
 * You should
 *
 * @author si, tm 2026.02.17
 * @since 15.1.2
 */
public class SFTPSnapshotArchiver
        extends AbstractSnapshotArchiver
    {
    // ----- logging ------------------------------------------------------

    /**
     * The {@link Logger} for this class.
     */
    private static final Logger LOGGER = Logger.getLogger(SFTPSnapshotArchiver.class.getName());

    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a new SFTPSnapshotArchiver which uses the URI SFTP URI.
     *
     * @param sClusterName   the name of the cluster
     * @param sServiceName   the service name
     * @param sURI           a URI for the SFTP server
     */
    public SFTPSnapshotArchiver(String sClusterName, String sServiceName, String sURI)
        {
        super(sClusterName, sServiceName);

        if (sURI == null)
            {
            throw new IllegalArgumentException("URI must be specified");
            }

        try
            {
            URI      uri            = new URI(sURI);
            String   sBaseDirectory = uri.getPath();
            String[] asUserInfo     = uri.getUserInfo().split(":");
            int      nPort          = uri.getPort();

            m_sUsername = asUserInfo[0];
            m_sPassword = asUserInfo.length == 2 ? asUserInfo[1] : null;
            m_sHostname = uri.getHost();
            m_nPort = nPort == -1 ? 22 : nPort;

            // ensure that the base directory exists  and then create the cluster and
            // service directories off that
            try (CloseableHandle ignored = ensureRemoteDirectory(sBaseDirectory, true))
                {
                // no-op
                }
            sBaseDirectory = sBaseDirectory + FTP_DIR_SEP + FileHelper.toFilename(sClusterName);
            try (CloseableHandle ignored = ensureRemoteDirectory(sBaseDirectory, false))
                {
                // no-op
                }
            sBaseDirectory = sBaseDirectory + FTP_DIR_SEP + FileHelper.toFilename(sServiceName);
            try (CloseableHandle ignored = ensureRemoteDirectory(sBaseDirectory, false))
                {
                // no-op
                }

            m_sBaseDirectory = sBaseDirectory;

            if (!"sftp".equals(uri.getScheme()))
                {
                throw new RuntimeException("Invalid protocol: " + uri.getScheme());
                }
            }
        catch (Exception e)
            {
            throw CachePersistenceHelper.ensurePersistenceException(e, "Unable to instantiate SFTP Archiver");
            }
        }

    // ----- AbstractSnapshotArchiver methods -------------------------------

    /**
     * Internal implementation to return the identifiers of the archived
     * snapshots known to this archiver.
     *
     * @return a string array of the known archived snapshot identifiers
     */
    @Override
    protected String[] listInternal()
        {
        try
            {
            // ensureRemoteDirectory returns a CloseableHandle that must be closed
            // (openDir handle) so use try-with-resources to avoid resource leaks.
            try (CloseableHandle ignored = ensureRemoteDirectory(m_sBaseDirectory, true))
                {
                return stripSpecialFiles(listDirectory(m_sBaseDirectory));
                }
            }
        catch (Exception e)
            {
            throw CachePersistenceHelper.ensurePersistenceException(e, "Unable to execute listInternal()");
            }
        }

    /**
     * List a directory given a directory name.
     *
     * @param sDirectory directory to list
     *                   
     * @return a {@link List} of names
     * @throws IOException
     */
    private List<String> listDirectory(String sDirectory) throws IOException
        {
        try (CloseableHandle dir = m_sftpClient.openDir(sDirectory))
            {
            return listDirectory(dir);
            }
        }

    /**
     * List a directory given a {@link CloseableHandle}.
     *
     * @param closeableHandle  {@link CloseableHandle}
     * @return a {@link List} of names
     * @throws IOException
     */
    private List<String> listDirectory(CloseableHandle closeableHandle) throws IOException
        {
        Iterable<SftpClient.DirEntry> dirEntries = m_sftpClient.listDir(closeableHandle);
        List<String> listResults = new ArrayList<>();

        dirEntries.forEach(e -> listResults.add(e.getFilename()));

        return listResults;
        }

    /**
     * List the stores for a given snapshot.
     *
     * @param sSnapshot the snapshot name to list stores for
     *
     * @return a {@link String}[] of store names
     */
    @Override
    protected String[] listStoresInternal(String sSnapshot)
        {
        String sDir = getSnapshotDirectory(sSnapshot);

        try (CloseableHandle closeableHandle = ensureRemoteDirectory(sDir, /* fExcludeDirs */ true))
            {
            ensureConnection();
            return stripSpecialFiles(listDirectory(closeableHandle));
            }
        catch (Exception e)
            {
            throw CachePersistenceHelper.ensurePersistenceException(e, "Unable to execute listStoresInternal()");
            }
        }

    /**
     * Internal implementation to Archive the specified snapshot using SFTP.
     *
     * @param snapshot  the snapshot to archive
     * @param mgr       the PersistenceManager used to read the stores from
     */
    @Override
    protected void archiveInternal(Snapshot snapshot, PersistenceManager<ReadBuffer> mgr)
        {
        String       sSnapshot       = snapshot.getName();

        // change to the remote directory, which should be created if it doesn't exist
        try (CloseableHandle ignored = ensureRemoteDirectory(getSnapshotDirectory(sSnapshot), false))
            {
            for (String sStore : snapshot.listStores())
                {
                File fileMetaTempDir = null;

                try (OutputStream os = m_sftpClient.write(getFullRemotePath(sSnapshot, sStore)))
                    {
                    LOGGER.info(() -> "Archiving store " + sStore + " for snapshot " + sSnapshot);
                    recordStartTime();

                    if (CachePersistenceHelper.isGlobalPartitioningSchemePID(GUIDHelper.getPartition(sStore)))
                        {
                        // Create a temporary directory to write archived snapshot metadata properties
                        fileMetaTempDir = FileHelper.createTempDir();

                        writeMetadata(fileMetaTempDir, mgr, sStore);

                        Path pathMetadata = new File(fileMetaTempDir, CachePersistenceHelper.META_FILENAME).toPath();

                        // store the metadata.properties file via SFTP
                        try (OutputStream osMetadata = m_sftpClient.write(getFullRemotePath(sSnapshot, CachePersistenceHelper.META_FILENAME)))
                            {
                            Files.copy(pathMetadata, osMetadata);
                            }
                        }

                    mgr.write(sStore, os);
                    // issue heartbeat as operations could take a relatively long time
                    GuardSupport.heartbeat();
                    }
                catch (IOException e)
                    {
                    throw CachePersistenceHelper.ensurePersistenceException(e, "Error in archiveInternal()");
                    }
                finally
                    {
                    if (fileMetaTempDir != null)
                        {
                        FileHelper.deleteDirSilent(fileMetaTempDir);
                        }
                    }

                recordEndTime();
                }
            }
        catch (IOException e)
            {
            throw CachePersistenceHelper.ensurePersistenceException(e, "Error in archiveInternal()");
            }
        }

    /**
     * Internal implementation to retrieve the specified snapshot.
     *
     * @param snapshot  the snapshot to retrieve
     * @param mgr       the PersistenceManager used to write the stores to
     */
    @Override
    protected void retrieveInternal(Snapshot snapshot, PersistenceManager<ReadBuffer> mgr)
        {
        String      sSnapshot = snapshot.getName();

        // change to the remote directory which should exist
        try (CloseableHandle ignored = ensureRemoteDirectory(getSnapshotDirectory(sSnapshot), true))
            {
            for (String sStore : snapshot.listStores())
                {
                try (InputStream is = m_sftpClient.read(getFullRemotePath(sSnapshot, sStore)))
                    {
                    LOGGER.fine(() -> "Retrieving store " + sStore + " for snapshot " + sSnapshot);
                    recordStartTime();

                    if (CachePersistenceHelper.isGlobalPartitioningSchemePID(GUIDHelper.getPartition(sStore)))
                        {
                        // validate that the metadata file exists for partition 0
                        if (getMetadata(sSnapshot) == null)
                            {
                            throw new IllegalArgumentException("Cannot load properties file "
                                                               + CachePersistenceHelper.META_FILENAME + " for snapshot "
                                                               + sSnapshot);
                            }
                        }

                    mgr.read(sStore, is);  // instruct the mgr to read the store from the stream

                    // issue heartbeat as operations could take a relatively long time
                    GuardSupport.heartbeat();
                    }
                catch (Exception e)
                    {
                    throw CachePersistenceHelper.ensurePersistenceException(e, "Error reading store " + sStore);
                    }
                recordEndTime();
                }
            }
        catch (IOException e)
            {
            throw CachePersistenceHelper.ensurePersistenceException(e, "Error in retrieveInternal()");
            }
        }

    /**
     * Internal implementation to remove the specified archived snapshot.
     *
     * @param sSnapshot  the snapshot name to remove
     *
     * @return true if the snapshot was removed
     */
    @Override
    protected boolean removeInternal(String sSnapshot)
        {
        // change to the remote directory which should exist
        try (CloseableHandle ignored = ensureRemoteDirectory(getSnapshotDirectory(sSnapshot), true))
            {
            String[] asStores = listStoresInternal(sSnapshot);

            for (String sStore : asStores)
                {
                LOGGER.warning(() -> "Removing " + getFullRemotePath(sSnapshot, sStore));
                m_sftpClient.remove(getFullRemotePath(sSnapshot, sStore));

                // issue heartbeat as operations could take a relatively long time
                GuardSupport.heartbeat();
                }

            // remove meta.properties
            m_sftpClient.remove(getFullRemotePath(sSnapshot, CachePersistenceHelper.META_FILENAME));

            // now remove the parent directory
            try (CloseableHandle ignoredBase = ensureRemoteDirectory(m_sBaseDirectory, true))
                {
                LOGGER.warning(() -> "Removing " + getSnapshotDirectory(sSnapshot));
                m_sftpClient.rmdir(getSnapshotDirectory(sSnapshot));
                }
            }
        catch (Exception e)
            {
            LOGGER.log(Level.WARNING, e,
                    () -> "Unable to remove stores for snapshot " + sSnapshot + ", " + e.getMessage());
            return false;
            }

        return true;
        }

    /**
     * Internal implementation to retrieve the metadata stored for the archived
     * snapshot.
     *
     * @param sSnapshot  the snapshot name to retrieve metadata
     *
     * @return the metadata for the archived snapshot
     *
     * @throws IOException if any I/O related problems
     */
    @Override
    protected Properties getMetadata(String sSnapshot)
            throws IOException
        {
        try (CloseableHandle ignored = ensureRemoteDirectory(getSnapshotDirectory(sSnapshot), true))
            {
            File fileTemp = null;

            try (InputStream is = m_sftpClient.read(getFullRemotePath(sSnapshot, CachePersistenceHelper.META_FILENAME)))
                {
                fileTemp = FileHelper.createTempDir();
                Files.copy(is, new File(fileTemp, CachePersistenceHelper.META_FILENAME).toPath());

                return CachePersistenceHelper.readMetadata(fileTemp);
                }
            catch (SftpException e)
                {
                throw new IOException("Unable to read metadata", e);
                }
            finally
                {
                if (fileTemp != null)
                    {
                    FileHelper.deleteDirSilent(fileTemp);
                    }
                }
            }
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Return a snapshot directory fully qualified path.
     *
     * @param sSnapshot  the snapshot name to get path for
     *
     * @return a snapshot directory fully qualified path
     */
    private String getSnapshotDirectory(String sSnapshot)
        {
        return m_sBaseDirectory + FTP_DIR_SEP + FileHelper.toFilename(sSnapshot);
        }

    /**
     * Return the full path to the file.
     *
     * @param sSnapshot  the snapshot name to get path for
     * @param sFileName  the file name to build path for
     *
     * @return the path to the snapshot file in the remote location
     */
    private String getFullRemotePath(String sSnapshot, String sFileName)
        {
        return getSnapshotDirectory(sSnapshot) + FTP_DIR_SEP + sFileName;
        }

    /**
     * Strips out special files such as ".." "." and "metadata.properties" from a list of files.
     *
     * @param listFiles List of files
     *
     * @return a String array of file names
     */
    private String[] stripSpecialFiles(List<String> listFiles)
        {
        if (listFiles == null)
            {
            throw new IllegalArgumentException("File list must not be null");
            }

        List<String> arrayList = new ArrayList<>();

        for (String sFileName : listFiles)
            {
            if (!".".equals(sFileName) && !"..".equals(sFileName)
                    && !CachePersistenceHelper.META_FILENAME.equals(sFileName))
                {
                arrayList.add(sFileName);
                }
            }

        return arrayList.toArray(new String[arrayList.size()]);
        }

    /**
     * Ensures that we have a connection to the SFTP endpoint.
     *
     * @throws IOException if a connection cannot be established or there is an error
     */
    private void ensureConnection()
            throws IOException
        {
        synchronized (m_lock)
            {
            try
                {
                if (m_sftpClient != null && !m_sftpClient.isOpen())
                    {
                    // Ensure we don't leak old sessions/threads when reconnecting.
                    closeConnectionInternal();
                    }

                if (m_sftpClient == null)
                    {
                    SshClient     sshClient     = SshClient.setUpDefaultClient();
                    ClientSession clientSession = null;
                    boolean       fSuccess      = false;

                    try
                        {
                        if (m_sPassword != null && !m_sPassword.isEmpty())
                            {
                            sshClient.addPasswordIdentity(m_sPassword);
                            }

                        sshClient.start();
                        clientSession = sshClient.connect(m_sUsername, m_sHostname, m_nPort)
                                .verify(15L, TimeUnit.SECONDS)
                                .getSession();
                        clientSession.auth().verify(15L, TimeUnit.SECONDS);

                        SftpClientFactory factory = SftpClientFactory.instance();
                        m_sftpClient    = factory.createSftpClient(clientSession);
                        m_clientSession = clientSession;
                        m_sshClient     = sshClient;
                        fSuccess        = true;
                        }
                    finally
                        {
                        if (!fSuccess)
                            {
                            // Best-effort cleanup on partial connection failures.
                            closeQuietly(clientSession);
                            closeQuietly(sshClient);
                            // Ensure fields are not left in a half-initialized state.
                            m_sftpClient    = null;
                            m_clientSession = null;
                            m_sshClient     = null;
                            }
                        }
                    }
                }
            catch (Exception e)
                {
                throw new IOException("Unable to connect", e);
                }
            }

        }

    /**
     * Close and clear any existing SFTP/SSH resources.
     */
    private void closeConnectionInternal()
        {
        // Close in reverse order of creation.
        closeQuietly(m_sftpClient);
        m_sftpClient = null;

        closeQuietly(m_clientSession);
        m_clientSession = null;

        closeQuietly(m_sshClient);
        m_sshClient = null;
        }

    private static void closeQuietly(Object o)
        {
        if (o == null)
            {
            return;
            }

        try
            {
            if (o instanceof AutoCloseable)
                {
                ((AutoCloseable) o).close();
                }
            }
        catch (Exception ignored)
            {
            // no-op
            }
        }

    /**
     * Ensure the remote directory exists. If the directory exists then change
     * the current working directory to the directory.
     * <p>
     * If the directory does not exist and fFailIfNotExists is true then raise
     * an exception otherwise create the directory and change to it.
     *
     * @param sDirectory       the directory to create or change to
     * @param fFailIfNotExists fail if the directory does not exist
     *
     * @return a {@link CloseableHandle} that refers to the directory
     */
    private CloseableHandle ensureRemoteDirectory(String sDirectory, boolean fFailIfNotExists)
        {
        CloseableHandle closeableHandle = null;

        try
            {
            ensureConnection();

            try
                {
                closeableHandle = m_sftpClient.openDir(sDirectory);
                return closeableHandle;
                }
            catch (SftpException e)
                {
                if (e.getStatus() != SSH_FX_NO_SUCH_FILE)
                    {
                    // Unexpected error opening the directory; nothing to clean up.
                    throw e;
                    }

                if (fFailIfNotExists)
                    {
                    throw new IOException("Unable to change to directory " + sDirectory, e);
                    }
                }

            // Directory does not exist and we are allowed to create it.
            try
                {
                LOGGER.fine(() -> "Creating SFTP Directory " + sDirectory);
                m_sftpClient.mkdir(sDirectory);
                }
            catch (Exception e)
                {
                // Multiple SFTP archivers could have registered the directory did not
                // exist, but it could have been created by one of them.
                //
                // Wait a bit and then try to open it in case someone else created it.
                Blocking.sleep(1000L);
                }

            closeableHandle = m_sftpClient.openDir(sDirectory);
            return closeableHandle;
            }
        catch (Exception e)
            {
            // Best-effort cleanup: avoid leaking an openDir handle on exceptional paths.
            closeQuietly(closeableHandle);
            throw CachePersistenceHelper.ensurePersistenceException(e, "Unable to ensureDirectory " + sDirectory);
            }
        }

    @Override
    protected boolean isEmpty(String sSnapshot, String sStore)
        {
        return false;
        }

    // ----- Object methods -------------------------------------------------

    @Override
    public String toString()
        {
        return (this.getClass().getName() + " (host=" + m_sHostname + ", port=" + m_nPort + ", username=" + m_sUsername
                + ", path=" + m_sBaseDirectory + ")");
        }

    // ----- constants -------------------------------------------------------

    /**
     * FTP URI format.
     */
    public static final String URI_FORMAT = "sftp://user:password@host:port/full/path";

    /**
     * Directory separator for FTP commands.
     */
    public static final String FTP_DIR_SEP = "/";

    // ----- data members ---------------------------------------------------

    /**
     * Username to connect to SFTP server.
     */
    private String m_sUsername;

    /**
     * Password to connect to SFTP server. Could be null if SSH equiv is being used.
     */
    private String m_sPassword;

    /**
     * Hostname of the SFTP server.
     */
    private String m_sHostname;

    /**
     * SFTP port to connect to.
     */
    private int m_nPort;

    /**
     * Base directory of the SFTP server.
     */
    private String m_sBaseDirectory;

    /**
     * SshClient instance.
     */
    private SshClient m_sshClient;

    /**
     * The underlying SSH session for the {@link #m_sftpClient}.
     */
    private ClientSession m_clientSession;

    /**
     * Lock to guard connection lifecycle.
     */
    private final Object m_lock = new Object();

    /**
     * An opened channel to an sftp endpoint.
     */
    private SftpClient m_sftpClient = null;
    }
