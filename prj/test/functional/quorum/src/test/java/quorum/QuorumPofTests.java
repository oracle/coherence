/*
 * Copyright (c) 2000, 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package quorum;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofSerializer;
import com.tangosol.io.pof.PofWriter;

import java.io.IOException;
import java.util.AbstractMap;

/**
 * Validate Quorum using POF.
 *
 * @author rl 2020.04.02
 */
public class QuorumPofTests
        extends QuorumTests
    {
    // ----- constructors ---------------------------------------------------

    public QuorumPofTests()
        {
        super(FILE_CACHE_CONFIG);
        }

    // ----- inner class: SimpleEntrySerializer -----------------------------

    /**
     * {@link PofSerializer} implementation for {@link AbstractMap.SimpleEntry}.
     */
    @SuppressWarnings("rawtypes")
    public static final class SimpleEntrySerializer
            implements PofSerializer<AbstractMap.SimpleEntry>
        {
        public void serialize(PofWriter out, AbstractMap.SimpleEntry value) throws IOException
            {
            out.writeObject(1, value.getKey());
            out.writeObject(2, value.getValue());
            }

        @SuppressWarnings("unchecked")
        public AbstractMap.SimpleEntry deserialize(PofReader in) throws IOException
            {
            return new AbstractMap.SimpleEntry(in.readObject(1), in.readObject(2));
            }
        }

    // ----- helper methods -------------------------------------------------

    @Override
    protected String getCacheConfig()
        {
        return FILE_CACHE_CONFIG;
        }

    @Override
    protected String getOverrideConfig()
        {
        return FILE_OPERATIONAL_CONFIG;
        }

    // ----- constants ------------------------------------------------------

    /**
     * Constant for the cache configuration under test.
     */
    protected static final String FILE_CACHE_CONFIG = "quorum-cache-config-pof.xml";

    /**
     *  Constant for the operational override configuration under test.
     */
    protected static final String FILE_OPERATIONAL_CONFIG = "quorum-coherence-override-pof.xml";
    }
