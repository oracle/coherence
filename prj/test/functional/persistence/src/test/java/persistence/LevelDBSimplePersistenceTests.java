package persistence;

import com.oracle.coherence.persistence.PersistenceManager;

import com.tangosol.io.ReadBuffer;

import com.tangosol.persistence.ldb.LevelDBManager;

import java.io.File;
import java.io.IOException;

/**
 * Functional tests for simple cache persistence and recovery using the
 * LevelDBPersistenceManager.
 *
 * @author jh  2012.10.18
 */
public class LevelDBSimplePersistenceTests
        extends AbstractSimplePersistenceTests
    {

    // ----- AbstractSimplePersistenceTests methods -------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    protected PersistenceManager<ReadBuffer> createPersistenceManager(File file)
            throws IOException
        {
        return new LevelDBManager(file, null, null);
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
