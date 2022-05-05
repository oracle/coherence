/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
/**
 *
 */
package partition;

import com.tangosol.io.DeltaCompressor;
import com.tangosol.io.ReadBuffer;

/**
 * @author coh 2011.01.28
 * @since Coherence 3.7
 */
public class MyCompressor
        implements DeltaCompressor
    {

    /**
     * @{inheritDoc}
     */
    @Override
    public ReadBuffer extractDelta(ReadBuffer bufOld, ReadBuffer bufNew)
        {
        // TODO Auto-generated method stub
        return null;
        }

    /**
     * @{inheritDoc}
     */
    @Override
    public ReadBuffer applyDelta(ReadBuffer bufOld, ReadBuffer bufDelta)
        {
        // TODO Auto-generated method stub
        return null;
        }

    }
