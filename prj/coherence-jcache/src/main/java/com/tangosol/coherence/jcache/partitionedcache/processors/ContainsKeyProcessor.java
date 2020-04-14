/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.jcache.partitionedcache.processors;

import com.tangosol.coherence.jcache.common.Helper;
import com.tangosol.coherence.jcache.common.JCacheIdentifier;

import com.tangosol.util.BinaryEntry;
import com.tangosol.util.InvocableMap;

/**
 * JCache ContainsKey EntryProcessor
 *
 * @version Coherence 12.1.3
 * @author jf  2013.12.18
 */
public class ContainsKeyProcessor
        extends AbstractEntryProcessor
    {
    // ----- Constructors ---------------------------------------------------

    /**
     * Constructs ...
     *
     */
    public ContainsKeyProcessor()
        {
        // for PortableObject and ExternalizeableLite
        }

    /**
     * Constructs a JCache ContainsKey processor
     *
     * @param id unique JCache cache identifier
     */
    public ContainsKeyProcessor(JCacheIdentifier id)
        {
        super(id);
        }

    // ----- ContainsKeyProcessor methods -----------------------------------

    @Override
    public Object process(InvocableMap.Entry entry)
        {
        long        ldtNow       = Helper.getCurrentTimeMillis();
        BinaryEntry binEntry     = (BinaryEntry) entry;
        Boolean     fContainsKey = Boolean.FALSE;

        if (binEntry.isPresent())
            {
            if (BinaryEntryHelper.isExpired(binEntry, ldtNow))
                {
                fContainsKey = false;
                }
            else
                {
                fContainsKey = binEntry.getBinaryValue() != null;
                }
            }

        return fContainsKey;
        }
    }
