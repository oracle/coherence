/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.jcache.partitionedcache.processors;

import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.net.CacheService;
import com.tangosol.net.PartitionedService;

import com.tangosol.util.BinaryEntry;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.processor.AbstractProcessor;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * Get PartitionCount via an entry processor
 *
 * @author   rhl 2013.05.15
 * @version  Coherence 12.1.3
 */
public class GetPartitionCountEP
        extends AbstractProcessor
        implements ExternalizableLite, PortableObject
    {

    // ----- AbstractProcessor methods --------------------------------------
    /**
     * Get Partition Count from any entry.
     *
     * @param entry any entry
     *
     * @return partition count if a partitioned service; otherwise return -1 to indicate not a partitioned service.
     */
    public Object process(InvocableMap.Entry entry)
        {
        CacheService service = ((BinaryEntry) entry).getContext().getCacheService();

        return service instanceof PartitionedService ? ((PartitionedService) service).getPartitionCount() : -1;
        }

    // ----- ExternalizableLite interface -----------------------------------

    @Override
    public void readExternal(DataInput in)
            throws IOException
        {
        }

    @Override
    public void writeExternal(DataOutput out)
            throws IOException
        {
        }

    // ----- PofObject interface --------------------------------------------

    @Override
    public void readExternal(PofReader pofReader)
            throws IOException
        {
        }

    @Override
    public void writeExternal(PofWriter pofWriter)
            throws IOException
        {
        }
    }
