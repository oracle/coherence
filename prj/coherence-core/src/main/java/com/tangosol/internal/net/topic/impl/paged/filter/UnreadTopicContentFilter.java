/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic.impl.paged.filter;

import com.oracle.coherence.common.base.Classes;

import com.tangosol.internal.net.topic.impl.paged.model.ContentKey;
import com.tangosol.internal.net.topic.impl.paged.model.PagedPosition;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;

import com.tangosol.net.topic.Position;

import com.tangosol.util.BinaryEntry;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.MapTrigger;

import com.tangosol.util.extractor.EntryExtractor;

import com.tangosol.util.filter.ExtractorFilter;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.HashMap;

import java.util.Map;

/**
 * A {@link com.tangosol.util.Filter} that can filter the keys of a paged topic contents cache.
 *
 * @author Jonathan Knight  2021.12.07
 * @since 22.06
 */
public class UnreadTopicContentFilter
        extends ExtractorFilter<Object, ContentKey>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor for serialization.
     */
    @SuppressWarnings("unchecked")
    public UnreadTopicContentFilter()
        {
        super(new ContentKeyExtractor());
        }

    /**
     * Create a filter to filter all keys for messages between the specified
     * head (exclusive) and tail (inclusive) positions.
     *
     * @param mapHeads  a map of topic head positions, keyed by channel
     * @param mapTails  a map of topic tail positions, keyed by channel
     */
    @SuppressWarnings("unchecked")
    public UnreadTopicContentFilter(Map<Integer, Position> mapHeads, Map<Integer, Position> mapTails)
        {
        super(new ContentKeyExtractor());
        m_mapHeads = mapHeads;
        m_mapTails = mapTails;
        }

    // ----- ExtractorFilter methods ----------------------------------------

    @Override
    protected boolean evaluateExtracted(ContentKey extracted)
        {
        int           nChannel = extracted.getChannel();
        PagedPosition posHead  = (PagedPosition) m_mapHeads.get(nChannel);
        PagedPosition posTail  = (PagedPosition) m_mapTails.get(nChannel);

        if (posHead == null || posTail == null)
            {
            // the channel is not in either map so do not match this key
            return false;
            }

        long nHead = posHead.getPage();
        long nTail = posTail.getPage();
        long nPage = extracted.getPage();

        if (nHead >= 0 && nPage == nHead)
            {
            return extracted.getElement() > posHead.getOffset();
            }
        else if (nTail >= 0 && nPage == nTail)
            {
            return extracted.getElement() <= posTail.getOffset();
            }
        else
            {
            return nPage > nHead && nPage < nTail;
            }
        }

    // ----- ExternalizableLite methods -------------------------------------

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        ClassLoader loader = Classes.getContextClassLoader();
        m_mapHeads = new HashMap<>();
        m_mapTails = new HashMap<>();
        ExternalizableHelper.readMap(in, m_mapHeads, loader);
        ExternalizableHelper.readMap(in, m_mapTails, loader);
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        ExternalizableHelper.writeMap(out, m_mapHeads);
        ExternalizableHelper.writeMap(out, m_mapTails);
        }

    // ----- PortableObject methods -----------------------------------------

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        m_mapHeads = in.readMap(0, new HashMap<>());
        m_mapTails = in.readMap(1, new HashMap<>());
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeMap(0, m_mapHeads);
        out.writeMap(1, m_mapTails);
        }

    // ----- inner class: ContentKeyExtractor -------------------------------

    /**
     * An {@link EntryExtractor} that can extract the key from a paged topic contents cache.
     */
    @SuppressWarnings("rawtypes")
    public static class ContentKeyExtractor
            extends EntryExtractor
        {
        @Override
        public Object extractFromEntry(Map.Entry entry)
            {
            return ContentKey.fromBinary(((BinaryEntry) entry).getBinaryKey(), true);
            }

        @Override
        public Object extractOriginalFromEntry(MapTrigger.Entry entry)
            {
            return ContentKey.fromBinary(((BinaryEntry) entry).getBinaryKey(), true);
            }
        }

    // ----- data members ---------------------------------------------------

    /**
     * The map of topic heads.
     */
    private Map<Integer, Position> m_mapHeads;

    /**
     * The map of topic tails.
     */
    private Map<Integer, Position> m_mapTails;
    }
