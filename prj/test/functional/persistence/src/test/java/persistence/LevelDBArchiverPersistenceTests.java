package persistence;

import com.oracle.coherence.persistence.PersistenceEnvironment;

import com.tangosol.io.ReadBuffer;

import com.tangosol.persistence.ldb.LevelDBEnvironment;

import java.io.File;
import java.io.IOException;

/**
 * Test archival and retrieval functionality with BerkeleyDB environment.
 *
 * @author tam  2014.07.22
 */
public class LevelDBArchiverPersistenceTests
        extends AbstractArchiverPersistenceTests
    {
    // ---- AbstractArchiverPersistenceTests methods ------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    protected PersistenceEnvironment<ReadBuffer> createPersistenceEnv(File fileActive, File fileSnapshot,
        File fileTrash)
            throws IOException
        {
        return new LevelDBEnvironment(fileActive, fileSnapshot, fileTrash);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPersistenceManagerName()
        {
        return "LDB";
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCacheConfigPath()
        {
        return "simple-persistence-ldb-cache-config.xml";
        }
    }
