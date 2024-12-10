/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net.queue.paged;

import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.net.BackingMapContext;
import com.tangosol.net.BackingMapManagerContext;

import com.tangosol.util.Binary;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.Converter;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.processor.AbstractProcessor;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;


/**
 * This entry processor removes the first element in a bucket
 * that is equal to a specified element. The entry processor
 * will return <tt>true</tt> in an element was removed otherwise
 * it will return <tt>false</tt>.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class RemoveBucketElementProcessor
        extends AbstractProcessor<Integer,Bucket,Boolean>
        implements ExternalizableLite, PortableObject
    {

    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor for POF
     */
    public RemoveBucketElementProcessor()
        {
        }

    /**
     * Create a new RemoveBucketElementProcessor that will attempt to
     * remove the specified value from a bucket, searching the bucket
     * from head to tail.
     *
     * @param element the element value to be removed.
     */
    public RemoveBucketElementProcessor(Object element)
        {
        this(element, true);
        }

    /**
     * Create a new RemoveBucketElementProcessor that will attempt to
     * remove the specified value from a bucket.
     *
     * @param element    the element value to be removed.
     * @param fHeadFirst if true the bucket is searched head to tail,
     *                   if false the bucket is searched tail to head.
     */
    public RemoveBucketElementProcessor(Object element, boolean fHeadFirst)
        {
        m_element    = element;
        m_fHeadFirst = fHeadFirst;
        }

    // ----- AbstractProcessor methods --------------------------------------

    /**
     * Attempts to remove the value from the bucket and returns true
     * if the value was remove otherwise returns false.
     *
     * @param entry the bucket cache entry
     *
     * @return true if the value was remove otherwise returns false.
     */
    @Override
    public Boolean process(InvocableMap.Entry<Integer,Bucket> entry)
        {
        if (!entry.isPresent())
            {
            return false;
            }

        BinaryEntry              binaryEntry       = (BinaryEntry) entry;
        BackingMapManagerContext context           = binaryEntry.getContext();
        BackingMapContext        backingMapContext = binaryEntry.getBackingMapContext();
        String                   elementCacheName  = PagedQueueCacheNames.Elements.getCacheName(backingMapContext);
        BackingMapContext        elementMapContext = context.getBackingMapContext(elementCacheName);
        Converter                keyConverter      = context.getKeyToInternalConverter();

        Bucket bucket = entry.getValue();

        int headId  = bucket.getHead();
        int tailId  = bucket.getTail();
        int startId = m_fHeadFirst ? headId : tailId;
        int endId   = m_fHeadFirst ? tailId + 1: headId - 1;

        if (startId != Bucket.EMPTY)
            {
            PagedQueueKey queueKey = new PagedQueueKey(bucket.getId(), startId);

            int i = startId;
            while (i != endId)
                {
                queueKey.setElementId(i);
                Binary      binaryKey    = (Binary) keyConverter.convert(queueKey);
                BinaryEntry elementEntry = (BinaryEntry) elementMapContext.getBackingMapEntry(binaryKey);

                if (elementEntry.isPresent() && equals(m_element, elementEntry.getValue()))
                    {
                    elementEntry.remove(false);
                    return true;
                    }

                if (m_fHeadFirst)
                    {
                    i = (i != Integer.MAX_VALUE) ? i + 1 : 0;
                    }
                else
                    {
                    i = (i != 0) ? i - 1 : Integer.MAX_VALUE;
                    }
                }

            }

        return false;
        }

    // ----- PortableObject methods -----------------------------------------

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        m_element    = in.readObject(0);
        m_fHeadFirst = in.readBoolean(1);
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeObject(0, m_element);
        out.writeBoolean(1, m_fHeadFirst);
        }

    // ----- ExternalizableLite methods -------------------------------------

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        m_element    = ExternalizableHelper.readObject(in);
        m_fHeadFirst = in.readBoolean();
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        ExternalizableHelper.writeObject(out, m_element);
        out.writeBoolean(m_fHeadFirst);
        }

    // ----- data members ---------------------------------------------------

    /**
     * The Object equal to the element to be removed.
     */
    protected Object m_element;

    /**
     * Flag indicating whether to search from the head of the bucket
     * or from the tail of the bucket.
     */
    protected boolean m_fHeadFirst;

    }
