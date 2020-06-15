/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io.nio;


import com.tangosol.util.Base;
import com.tangosol.util.Binary;
import com.tangosol.util.Converter;
import com.tangosol.util.SimpleMapEntry;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import java.lang.reflect.Array;

import java.nio.ByteBuffer;

import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;


/**
* Implements the Map interface to store Binary objects using Java's NIO
* buffers.
* <pre>
* The Buffer is used to hold blocks, which are either Entry or free blocks.
* Both share a common header:
*
*   byte    length      type     description
*   ------  ----------  -------  -------------------------------------------
*        0           1  integer  type
*        1           4  integer  offset of the next block in the Buffer
*        5           4  integer  offset of the prev block in the Buffer
*        9           4  integer  offset of the next entry in the list or -1=tail
*       13           4  integer  offset of the prev entry in the list or -1=head
*
* The offset of the next block in the buffer provides the "length overall"
* (also known as LOA) of the block, such that a block located at offset
* "oCur" with a next block offset of "oNext" will have an LOA "l" of:
*
*       l = oNext - oCur
*
* The additional structure for an Entry block is as follows:
*
*   byte    length      type     description
*   ------  ----------  -------  -------------------------------------------
*       17           4  integer  hash code
*       21           4  integer  key length (m)
*       25           m  byte[]   key
*     25+m           4  integer  value length (n)
*     29+m           n  byte[]   value
*   29+m+n  l-(29+m+n)  byte[]   fill
*
* The reason for supporting "fill" is to allow an entry to shrink and grow
* a little bit in place, for example since a free block has a minimum size
* and if the entry is followed immediately by another entry and shrinks, it
* would have to be moved if it doesn't shrink at least 17 bytes. Similarly,
* an entry could be padded to allow it to grow slightly.
*
* The additional structure for a free block is as follows:
*
*   byte    length      type     description
*   ------  ----------  -------  -------------------------------------------
*       17        l-17  byte[]   fill
*
* The Buffer is a packed list of blocks, and each block is either an Entry or
* a free block. Contiguous free blocks are automatically merged so that there
* are never any contiguous free blocks at the end of an operation. Entries
* are expected to be contiguously allocated; compaction is the act of copying
* Entry blocks so that they are contiguous.
*
* The BinaryMap manages an array of hash buckets that hold the offsets of the
* first Entry in the linked list of entries stored in the Buffer. The offset
* will be NIL (-1) if there are no entries in that bucket since 0 is a valid
* offset into the Buffer.
*
* Incremental compaction occurs only on modifications (puts and removes).
* For a put, the compaction occurs before the put is processed, and for a
* remove, it occurs after the remove is processed, to make it slightly more
* likely that a put will have adequate free space and that the removed entry
* would not have been relocated by compaction.
*
* The BinaryMap categorizes empty blocks based on their size:
*
*   code  free block size
*   ----  -----------------------------------
*      0  63 bytes or smaller
*      1  64 to 127 bytes
*      2  128 to 255 bytes
*      3  256 to 511 bytes
*      4  512 to 1023 bytes
*      5  1024 to 2047 bytes
*      6  2048 to 4095 bytes
*      7  4096 to 8191 bytes
*      8  8192 to 16383 bytes
*      9  16384 to 32767 bytes
*     10  32768 to 65535 bytes
*     11  65536 to 131071 bytes
*     12  131072 to 262143 bytes
*     13  262144 to 524287 bytes
*     14  524288 to 1048575 bytes
*     15  1048576 to 2097151 bytes
*     16  2097152 to 4194303 bytes
*     17  4194304 to 8388607 bytes
*     18  8388608 to 16777215 bytes
*     19  16777216 to 33554431 bytes
*     20  33554432 to 67108863 bytes
*     21  67108864 to 134217727 bytes
*     22  134217728 to 268435455 bytes
*     23  268435456 to 536870911 bytes
*     24  536870912 to 1073741823 bytes
*     25  1073741824 to 2147483647 bytes
*
* For each category of free blocks, the BinaryMap maintains a linked list of
* free blocks that fit that category.
*
* To determine the size of a block in bytes, use the length() method. To
* calculate the code of a block, use the getSizeCode() method, or the static
* calculateSizeCode(int) method of BinaryMap.
*
* To open an existing block at a certain offset, use the method
* openBlock(int). To create and open a block at a certain offset, use the
* method initBlock(int). To allocate and open a free block of a certain size,
* use the method allocateBlock(int). An opened block should always be closed
* using the method Block.close(), which commits pending changes to the
* underlying buffer. The only time that a block should not be closed is when
* the block is being destroyed (e.g. when a free block is merged with another
* free block); in this case, use the method Block.discard().
*
* To merge free blocks that occur before and/or after a specific free block,
* use the method Block.merge(). To split a free block into two contiguous
* free blocks, use the method Block.split(int).
*
* To remove a block from its linked list, use Block.unlink(). Unless the
* block is being destroyed, it should be re-linked using the Block.link()
* method.
* </pre>
*
* @version 1.00, 2002-09-06
* @author cp
*
* @since Coherence 2.2
*/
public class BinaryMap
        extends AbstractMap
    {
    // ----- constructors ---------------------------------------------------


    /**
    * Construct a BinaryMap on a specific buffer with the default modulo
    * growth and shrinkage (load factor) settings.
    *
    * @param buffer  the ByteBuffer that the map will store its data in
    */
    public BinaryMap(ByteBuffer buffer)
        {
        this(buffer, DEFAULT_MAXLOADFACTOR, DEFAULT_MINLOADFACTOR, false);
        }

    /**
    * Construct a BinaryMap on a specific buffer with the specified modulo
    * growth and shrinkage (load factor) settings.
    *
    * @param buffer            the ByteBuffer that the map will store its
    *                          data in
    * @param dflMaxLoadFactor  the percentage of the ratio of keys to the
    *                          modulo at which the modulo will increase;
    *                          for example, 0.9 implies that the modulo
    *                          will grow when the number of keys reaches
    *                          90% of the modulo value
    * @param dflMinLoadFactor  the percentage of the ratio of keys to the
    *                          next lower modulo at which the modulo will
    *                          decrease; this value must be less than the
    *                          maximum load factor value
    * @param fStrict           true to enable the strict (clean buffer)
    *                          option, which will degrade performance
    *                          slightly
    */
    public BinaryMap(ByteBuffer buffer, double dflMaxLoadFactor, double dflMinLoadFactor, boolean fStrict)
        {
        this();

        if (buffer == null)
            {
            throw new IllegalArgumentException("Buffer must not be null");
            }
        if (dflMaxLoadFactor <= 0.0 || dflMaxLoadFactor > 8.0)
            {
            throw new IllegalArgumentException("Illegal MaxLoadFactor value (" + dflMaxLoadFactor
                    + "); MaxLoadFactor is a percentage such that 100% is expressed as 1.00");
            }
        if (dflMinLoadFactor <= 0.0 || dflMinLoadFactor > 8.0)
            {
            throw new IllegalArgumentException("Illegal MinLoadFactor value (" + dflMinLoadFactor
                    + "); MinLoadFactor is a percentage such that 100% is expressed as 1.00");
            }
        if (!(dflMinLoadFactor < dflMaxLoadFactor))
            {
            throw new IllegalArgumentException("Illegal threshold values (MaxLoadFactor="
                    + dflMaxLoadFactor + ", MinLoadFactor=" + dflMinLoadFactor
                    + "); MinLoadFactor must be smaller than MaxLoadFactor");
            }

        setStrict(fStrict);
        setMaxLoadFactor(dflMaxLoadFactor);
        setMinLoadFactor(dflMinLoadFactor);

        initializeFreeLists();
        initializeBuckets();
        setBuffer(buffer);
        clearBuffer();

        if (MODE_DEBUG)
            {
            check("after construction");
            }
        }

    /**
    * Construct a BinaryMap using a buffer from the specified
    * ByteBufferManager, and using the default modulo growth and shrinkage
    * (load factor) settings.
    *
    * @param bufmgr  the ByteBufferManager that is responsible for providing
    *                and managing the ByteBuffer
    */
    public BinaryMap(ByteBufferManager bufmgr)
        {
        this(bufmgr, DEFAULT_MAXLOADFACTOR, DEFAULT_MINLOADFACTOR, false);
        }

    /**
    * Construct a BinaryMap using a buffer from the specified
    * ByteBufferManager, and using the specified modulo growth and shrinkage
    * (load factor) settings.
    *
    * @param bufmgr  the ByteBufferManager that is responsible for providing
    *                and managing the ByteBuffer
    * @param dflMaxLoadFactor  the percentage of the ratio of keys to the
    *                          modulo at which the modulo will increase;
    *                          for example, 0.9 implies that the modulo
    *                          will grow when the number of keys reaches
    *                          90% of the modulo value
    * @param dflMinLoadFactor  the percentage of the ratio of keys to the
    *                          next lower modulo at which the modulo will
    *                          decrease; this value must be less than the
    *                          maximum load factor value
    * @param fStrict           true to enable the strict (clean buffer)
    *                          option, which will degrade performance
    *                          slightly
    */
    public BinaryMap(ByteBufferManager bufmgr, double dflMaxLoadFactor, double dflMinLoadFactor, boolean fStrict)
        {
        this(bufmgr.getBuffer(), dflMaxLoadFactor, dflMinLoadFactor, fStrict);
        setBufferManager(bufmgr);
        }

    /**
    * Construct a BinaryMap. This constructor is provided solely for
    * inheriting implementations to avoid the requirements imposed by the
    * public constructors.
    */
    protected BinaryMap()
        {
        // configure the open block cache
        int[]   aofBlockCache = m_aofBlockCache;
        Block[] ablockCache   = m_ablockCache;
        for (int i = 0, c = MAX_OPEN_BLOCKS; i < c; ++i)
            {
            aofBlockCache[i] = NIL;
            ablockCache  [i] = instantiateBlock();
            }
        }


    // ----- Map interface --------------------------------------------------

    /**
    * Returns the number of key-value mappings in this map.
    *
    * @return the number of entries in this map
    */
    public int size()
        {
        if (MODE_DEBUG)
            {
            check("size()");
            }

        return getEntryBlockCount();
        }

    /**
    * Returns <tt>true</tt> if this map contains no key-value mappings.
    *
    * @return <tt>true</tt> if this map contains no key-value mappings
    */
    public boolean isEmpty()
        {
        if (MODE_DEBUG)
            {
            check("isEmpty()");
            }

        return getEntryBlockCount() == 0;
        }

    /**
    * Returns <tt>true</tt> if this map contains a mapping for the specified
    * key.
    *
    * @param oKey key whose presence in this map is to be tested
    *
    * @return <tt>true</tt> if this map contains a mapping for the specified
    *         key
    */
    public synchronized boolean containsKey(Object oKey)
        {
        if (MODE_DEBUG)
            {
            check("before containsKey()");
            }

        try
            {
            Block block = findEntryBlock((Binary) oKey);
            if (block != null)
                {
                block.close();
                }

            if (MODE_DEBUG)
                {
                check("after containsKey()");
                }

            return block != null;
            }
        catch (RuntimeException e)
            {
            throw validateKey(oKey, e);
            }
        }

    /**
    * Returns the value to which this map maps the specified key.  Returns
    * <tt>null</tt> if the map contains no mapping for this key.
    *
    * @param oKey  key whose associated value is to be returned
    *
    * @return the value to which this map maps the specified key, or
    *	      <tt>null</tt> if the map contains no mapping for this key
    */
    public synchronized Object get(Object oKey)
        {
        if (MODE_DEBUG)
            {
            check("before get()");
            }

        try
            {
            Object oValue = null;
            Block  block  = findEntryBlock((Binary) oKey);
            if (block != null)
                {
                oValue = block.getValue();
                block.close();
                }

            if (MODE_DEBUG)
                {
                check("after get()");
                }

            return oValue;
            }
        catch (RuntimeException e)
            {
            throw validateKey(oKey, e);
            }
        }

    /**
    * Associates the specified value with the specified key in this map.
    *
    * @param oKey    key with which the specified value is to be associated
    * @param oValue  value to be associated with the specified key
    *
    * @return previous value associated with specified key, or <tt>null</tt>
    *	      if there was no mapping for key
    */
    public synchronized Object put(Object oKey, Object oValue)
        {
        if (MODE_DEBUG)
            {
            check("before put()");
            }

        try
            {
            // incremental compact before the put
            compactNext();

            Binary binKey   = (Binary) oKey;
            Binary binValue = (Binary) oValue;
            Binary binOld   = null;
            Block  block  = findEntryBlock(binKey);
            if (block == null)
                {
                block = allocateBlock(Block.MIN_ENTRY + binKey.length() + binValue.length());
                block.setKey(binKey);
                block.setValue(binValue);
                block.link();
                block.close();

                m_cbKeyTotal   += binKey.length();
                m_cbValueTotal += binValue.length();
                ++m_cEntries;
                checkModulo();
                }
            else
                {
                // an Entry block has been found that contains this key;
                // extract the old value (which will be returned)
                binOld = block.getValue();

                int cbOld  = binOld.length();
                int cbNew  = binValue.length();
                int cbDif  = cbNew - cbOld;
                int cbFill = block.getFillLength();

                // check if the new value will fit in the old block
                if (cbDif > cbFill)
                    {
                    // verify that the data will fit in the map
                    int cbGrow = cbDif - cbFill;
                    int cbFree = getFreeCapacity();

                    ByteBufferManager bufmgr = getBufferManager();
                    if (bufmgr != null)
                        {
                        cbFree += (bufmgr.getMaxCapacity() - bufmgr.getCapacity());
                        }

                    if (cbGrow > cbFree)
                        {
                        throw reportOutOfMemory(cbGrow);
                        }

                    int cbBlock = block.length();
                    block.free();

                    block = allocateBlock(cbBlock + cbGrow);
                    block.setKey(binKey);
                    block.link();
                    }
                else if (cbDif < 0 && isStrict())
                    {
                    // value is shrinking; clear the old value
                    block.clearValue();
                    }

                block.setValue(binValue);
                block.close();

                m_cbValueTotal += cbDif;

                // if the buffer got smaller, consider shrinking it
                if (cbDif < 0)
                    {
                    checkBufferShrink();
                    }
                }

            if (MODE_DEBUG)
                {
                check("after put()");
                }

            return binOld;
            }
        catch (RuntimeException e)
            {
            throw validateEntry(oKey, oValue, e);
            }
        }

    /**
    * Removes the mapping for this key from this map if present.
    *
    * @param oKey  key whose mapping is to be removed from the map
    *
    * @return previous value associated with specified key, or <tt>null</tt>
    *	       if there was no mapping for key.  A <tt>null</tt> return can
    *	       also indicate that the map previously associated <tt>null</tt>
    *	       with the specified key, if the implementation supports
    *	       <tt>null</tt> values
    */
    public synchronized Object remove(Object oKey)
        {
        if (MODE_DEBUG)
            {
            check("before remove()");
            }

        try
            {
            Binary binValue = null;
            Block  block    = findEntryBlock((Binary) oKey);
            if (block != null)
                {
                binValue = block.getValue();

                m_cbKeyTotal   -= block.getKeyLength();
                m_cbValueTotal -= block.getValueLength();
                boolean fEmpty  = (--m_cEntries == 0);

                block.free();

                if (fEmpty)
                    {
                    clear();
                    }

                // incremental compact after the remove
                compactNext();

                // consider shrinking the buffer
                checkBufferShrink();
                }

            if (MODE_DEBUG)
                {
                check("after remove()");
                }

            return binValue;
            }
        catch (RuntimeException e)
            {
            throw validateKey(oKey, e);
            }
        }

    /**
    * Removes all mappings from this map.
    */
    public synchronized void clear()
        {
        if (MODE_DEBUG)
            {
            check("before clear()");
            }

        clearFreeLists();
        initializeBuckets();
        clearBuffer();

        // consider shrinking the buffer
        checkBufferShrink();

        if (MODE_DEBUG)
            {
            check("after clear()");
            }
        }

    /**
    * Returns a set view of the mappings contained in this map.  Each element
    * in the returned set is a <tt>Map.Entry</tt>.  The set is backed by the
    * map, so changes to the map are reflected in the set, and vice-versa.
    * If the map is modified while an iteration over the set is in progress,
    * the results of the iteration are undefined.  The set supports element
    * removal, which removes the corresponding mapping from the map, via the
    * <tt>Iterator.remove</tt>, <tt>Set.remove</tt>, <tt>removeAll</tt>,
    * <tt>retainAll</tt> and <tt>clear</tt> operations.  It does not support
    * the <tt>add</tt> or <tt>addAll</tt> operations.
    *
    * @return a set view of the mappings contained in this map.
    */
    public Set entrySet()
        {
        EntrySet set = m_set;
        if (set == null)
            {
            m_set = set = instantiateEntrySet();
            }
        return set;
        }

    /**
    * Returns a Set view of the keys contained in this map.  The Set is
    * backed by the map, so changes to the map are reflected in the Set,
    * and vice-versa.  (If the map is modified while an iteration over
    * the Set is in progress, the results of the iteration are undefined.)
    * The Set supports element removal, which removes the corresponding entry
    * from the map, via the Iterator.remove, Set.remove,  removeAll
    * retainAll, and clear operations.  It does not support the add or
    * addAll operations.<p>
    *
    * @return a Set view of the keys contained in this map
    */
    public Set keySet()
        {
        KeySet set = m_setKeys;
        if (set == null)
            {
            m_setKeys = set = instantiateKeySet();
            }
        return set;
        }

    /**
    * Returns a collection view of the values contained in this map.  The
    * collection is backed by the map, so changes to the map are reflected in
    * the collection, and vice-versa.  If the map is modified while an
    * iteration over the collection is in progress, the results of the
    * iteration are undefined.  The collection supports element removal,
    * which removes the corresponding mapping from the map, via the
    * <tt>Iterator.remove</tt>, <tt>Collection.remove</tt>,
    * <tt>removeAll</tt>, <tt>retainAll</tt> and <tt>clear</tt> operations.
    * It does not support the add or <tt>addAll</tt> operations.
    *
    * @return a collection view of the values contained in this map
    */
    public Collection values()
        {
        ValuesCollection col = m_colValues;
        if (col == null)
            {
            m_colValues = col = instantiateValuesCollection();
            }
        return col;
        }


    // ----- Map implementation helpers -------------------------------------

    /**
    * Returns the number of entry blocks.
    *
    * @return the number of entry blocks
    */
    public int getEntryBlockCount()
        {
        return m_cEntries;
        }

    /**
    * Find the Entry block with the specified key.
    *
    * @param binKey  the Binary key object
    *
    * @return a matching Entry block or null if that key is not in the map
    */
    protected Block findEntryBlock(Binary binKey)
        {
        // incremental rehash
        rehashNext();

        int   cbKey   = binKey.length();
        int   nHash   = binKey.hashCode();
        int   nBucket = calculateBucket(nHash);
        int   ofBlock = getBucketOffset(nBucket);

        while (ofBlock != NIL)
            {
            Block block = openBlock(ofBlock);
            if (block.getKeyHash() == nHash && block.getKeyLength() == cbKey)
                {
                if (binKey.equals(block.getKey()))
                    {
                    return block;
                    }
                }

            ofBlock = block.getNextNodeOffset();
            block.close();
            }

        // search previous bucket if the map is still rehashing
        if (isRehashing())
            {
            int nPreviousBucket = calculatePreviousBucket(nHash);
            if (nPreviousBucket != nBucket)
                {
                ofBlock = getBucketOffset(nPreviousBucket);
                while (ofBlock != NIL)
                    {
                    Block block = openBlock(ofBlock);
                    if (block.getKeyHash() == nHash && block.getKeyLength() == cbKey)
                        {
                        if (binKey.equals(block.getKey()))
                            {
                            return block;
                            }
                        }

                    ofBlock = block.getNextNodeOffset();
                    block.close();
                    }
                }
            }

        return null;
        }

    /**
    * Returns an array with a runtime type is that of the specified array
    * and that contains data from all of the entries in this Map. See the
    * documentation for toArray for the key set, entry set and values
    * collection of the map.
    *
    * @param ao    the array into which the data from the map entires are to
    * 	           be stored, if it is big enough; otherwise, a new array of
    * 	           the same runtime type is allocated for this purpose
    * @param conv  an object that converts a Block object into either a key,
    *              entry or value, depending on the collection which is
    *              delegating to this method
    *
    * @return an array containing the entry data (key, entry or value)
    *
    * @throws ArrayStoreException if the runtime type of the specified
    *         array is not a supertype of the runtime type required to
    *         hold the keys, entries or values
    */
    protected synchronized Object[] toArray(Object ao[], Converter conv)
        {
        // create the array to store the map contents
        int co = size();
        if (ao == null)
            {
            // implied Object[] type, see toArray()
            ao = new Object[co];
            }
        else if (ao.length < co)
            {
            // if it is not big enough, a new array of the same runtime
            // type is allocated
            ao = (Object[]) Array.newInstance(ao.getClass().getComponentType(), co);
            }
        else if (ao.length > co)
            {
            // if the collection fits in the specified array with room to
            // spare, the element in the array immediately following the
            // end of the collection is set to null
            ao[co] = null;
            }

        // walk all blocks
        int of = 0;
        int i  = 0;
        while (of != NIL)
            {
            Block block = openBlock(of);
            if (block.isEntry())
                {
                ao[i++] = conv.convert(block);
                }
            of = block.getNextBlockOffset();
            block.close();
            }

        return ao;
        }

    /**
    * If the passed key is not Binary, then throw an exception to specify
    * that the key is not Binary, otherwise throw the original exception.
    *
    * @param key  the object that should be of type Binary
    * @param e      the original RuntimeException
    *
    * @throws RuntimeException this method always throws some form of
    *         RuntimeException
    */
    protected RuntimeException validateKey(Object key, RuntimeException e)
        {
        if (key instanceof Binary)
            {
            // not our RuntimeException; re-throw
            throw e;
            }
        else
            {
            throw new IllegalArgumentException("BinaryMap key must be of type Binary");
            }
        }

    /**
    * If the passed key and/or value is not a Binary object, then throw an
    * exception to specify that the key is not Binary, otherwise throw the
    * original exception.
    *
    * @param key    the key object that should be of type Binary
    * @param value  the value object that should be of type Binary
    * @param e      the original RuntimeException
    *
    * @throws RuntimeException this method always throws some form of
    *         RuntimeException
    */
    protected RuntimeException validateEntry(Object key, Object value, RuntimeException e)
        {
        if (!(key instanceof Binary))
            {
            throw new IllegalArgumentException("BinaryMap key must be of type Binary");
            }
        else if (!(value instanceof Binary))
            {
            throw new IllegalArgumentException("BinaryMap value must be of type Binary");
            }
        else
            {
            // not our RuntimeException; re-throw
            throw e;
            }
        }

    /**
    * Report on an insufficient memory problem.
    *
    * @param cbRequired  the amount of space required
    *
    * @throws RuntimeException this method always throws some form of
    *         RuntimeException
    */
    protected RuntimeException reportOutOfMemory(int cbRequired)
        {
        throw new RuntimeException("OutOfMemory: Required=" + cbRequired
                + ", Available=" + getFreeCapacity());
        }


    // ----- inner class: EntrySet ------------------------------------------

    /**
    * Factory pattern.
    *
    * @return a new instance of the EntrySet class (or a subclass thereof)
    */
    protected EntrySet instantiateEntrySet()
        {
        return new EntrySet();
        }

    /**
    * A set of entries backed by this map.
    */
    public class EntrySet
            extends AbstractSet
        {
        // ----- Set interface ------------------------------------------

        /**
        * Returns an iterator over the elements contained in this collection.
        *
        * @return an iterator over the elements contained in this collection
        */
        public Iterator iterator()
            {
		    return new Iterator()
                {
			    private Iterator m_iter = BinaryMap.this.keySet().iterator();

    			public boolean hasNext()
                    {
	    		    return m_iter.hasNext();
		        	}

			    public Object next()
                    {
			        Binary binKey   = (Binary) m_iter.next();
                    Binary binValue = (Binary) BinaryMap.this.get(binKey);
                    return instantiateEntry(binKey, binValue);
			        }

			    public void remove()
                    {
			        m_iter.remove();
			        }
                };
            }

        /**
        * Returns the number of elements in this collection.  If the collection
        * contains more than <tt>Integer.MAX_VALUE</tt> elements, returns
        * <tt>Integer.MAX_VALUE</tt>.
        *
        * @return the number of elements in this collection.
        */
        public int size()
            {
            return BinaryMap.this.size();
            }

        /**
        * Returns <tt>true</tt> if this collection contains the specified
        * element.  More formally, returns <tt>true</tt> if and only if this
        * collection contains at least one element <tt>e</tt> such that
        * <tt>(o==null ? e==null : o.equals(e))</tt>.<p>
        *
        * @param o object to be checked for containment in this collection
        *
        * @return <tt>true</tt> if this collection contains the specified
        *         element
        */
        public boolean contains(Object o)
            {
            if (o instanceof Map.Entry)
                {
                BinaryMap map    = BinaryMap.this;
                Map.Entry entry  = (Map.Entry) o;
                Object    oKey   = entry.getKey();
                Object    oValue = entry.getValue();
                if (   oKey instanceof Binary
                    && oValue instanceof Binary)
                    {
                    synchronized (map)
                        {
                        return map.containsKey(oKey)
                            && map.get(oKey).equals(oValue);
                        }
                    }
                }

            return false;
            }

        /**
        * Removes all of the elements from this set of Keys by clearing the
        * underlying Map.
        */
        public void clear()
            {
            BinaryMap.this.clear();
            }

        /**
        * Returns an array containing all of the elements in this collection.  If
        * the collection makes any guarantees as to what order its elements are
        * returned by its iterator, this method must return the elements in the
        * same order.  The returned array will be "safe" in that no references to
        * it are maintained by the collection.  (In other words, this method must
        * allocate a new array even if the collection is backed by an Array).
        * The caller is thus free to modify the returned array.<p>
        *
        * @return an array containing all of the elements in this collection
        */
        public Object[] toArray()
            {
            return toArray((Object[]) null);
            }

        /**
        * Returns an array with a runtime type is that of the specified array
        * and that contains all of the keys in this Set.  If the Set fits
        * in the specified array, it is returned therein. Otherwise, a new
        * array is allocated with the runtime type of the specified array
        * and the size of this collection.<p>
        *
        * If the Set fits in the specified array with room to spare (i.e.,
        * the array has more elements than the Set), the element in the
        * array immediately following the end of the Set is set to
        * <tt>null</tt>.  This is useful in determining the length of the
        * Set <i>only</i> if the caller knows that the Set does
        * not contain any <tt>null</tt> keys.)<p>
        *
        * @param  ao  the array into which the elements of the Set are to
        * 	          be stored, if it is big enough; otherwise, a new array
        * 	          of the same runtime type is allocated for this purpose
        *
        * @return an array containing the elements of the Set
        *
        * @throws ArrayStoreException if the runtime type of the specified
        *         array is not a supertype of the runtime type of every
        *         element in this Set of keys
        */
        public Object[] toArray(Object ao[])
            {
            BinaryMap map = BinaryMap.this;
            return map.toArray(ao, new Converter()
                {
                public Object convert(Object o)
                    {
                    Block block = (Block) o;
                    return instantiateEntry(block.getKey(), block.getValue());
                    }
                });
            }
        }


    // ----- inner class: KeySet --------------------------------------------

    /**
    * Factory pattern.
    *
    * @return a new instance of the KeySet class (or subclass thereof)
    */
    protected KeySet instantiateKeySet()
        {
        return new KeySet();
        }

    /**
    * A set of entries backed by this map.
    */
    protected class KeySet
            extends AbstractSet
        {
        // ----- Set interface ------------------------------------------

        /**
        * Obtain an iterator over the keys in the Map.
        *
        * @return an Iterator that provides a live view of the keys in the
        *         underlying Map object
        */
		public Iterator iterator()
            {
            // have to finish rehashing to keep the buckets stable while
            // iterate
            rehashAll();

		    return new Iterator()
                {
                private int      m_cBuckets    = getBucketCount();
                private int      m_iBucket     = 0;
			    private Object[] m_aoKey       = new Object[16]; // expect to need only a few
                private int      m_cKeys       = 0;
                private int      m_iKey        = 0;
                private boolean  m_fCanRemove  = false;

    			public boolean hasNext()
                    {
                    if (m_iKey < m_cKeys)
                        {
                        return true;
                        }

                    nextBucket();

                    return m_iKey < m_cKeys;
		        	}

                private void nextBucket()
                    {
                    synchronized (BinaryMap.this)
                        {
                        int cBuckets = m_cBuckets;
                        if (cBuckets != getBucketCount())
                            {
                            // check if the map has been cleared
                            if (isEmpty())
                                {
                                // probable explanation: Iterator.remove() on
                                // every element, which calls BinaryMap.remove(),
                                // and last remove() called clear() which changed
                                // the bucket count
                                return;
                                }
                            else
                                {
                                throw new ConcurrentModificationException(cBuckets + "!=" + getBucketCount());
                                }
                            }

                        int iBucket = m_iBucket;
                        int ofBlock = NIL;
                        while (iBucket < cBuckets && ofBlock == NIL)
                            {
                            ofBlock = getBucketOffset(iBucket++);
                            }
                        m_iBucket = iBucket;

                        Object[] ao    = m_aoKey;
                        int      co    = 0;
                        int      coMax = ao.length;
                        while (ofBlock != NIL)
                            {
                            Block block = openBlock(ofBlock);

                            if (co >= coMax)
                                {
                                coMax *= 2;
                                Object[] aoNew = new Object[coMax];
                                System.arraycopy(ao, 0, aoNew, 0, co);
                                m_aoKey = ao = aoNew;
                                }

                            ao[co++] = block.getKey();

                            ofBlock = block.getNextNodeOffset();
                            block.close();
                            }

                        m_cKeys = co;
                        m_iKey  = 0;
                        }
                    }

			    public Object next()
                    {
                    if (m_iKey >= m_cKeys)
                        {
                        nextBucket();
                        }

	    		    if (m_iKey < m_cKeys)
                        {
			            Object oKey = m_aoKey[m_iKey++];
                        m_fCanRemove = true;
                        return oKey;
                        }

                    throw new NoSuchElementException();
                    }

			    public void remove()
                    {
                    if (m_fCanRemove)
                        {
    			        BinaryMap.this.remove(m_aoKey[m_iKey-1]);
                        m_fCanRemove = false;
                        }
                    else
                        {
                        throw new IllegalStateException();
                        }
			        }
                };
            }

        /**
        * Determine the number of keys in the Set.
        *
        * @return the number of keys in the Set, which is the same as the
        *         number of entries in the underlying Map
        */
		public int size()
            {
		    return BinaryMap.this.size();
		    }

        /**
        * Determine if a particular key is present in the Set.
        *
        * @return true iff the passed key object is in the key Set
        */
		public boolean contains(Object oKey)
            {
		    return BinaryMap.this.containsKey(oKey);
		    }

        /**
        * Removes the specified element from this Set of keys if it is
        * present by removing the associated entry from the underlying
        * Map.
        *
        * @param o  object to be removed from this set, if present
        *
        * @return true if the set contained the specified element
        */
        public boolean remove(Object o)
            {
            BinaryMap map = BinaryMap.this;
            synchronized (map)
                {
                if (map.containsKey(o))
                    {
                    map.remove(o);
                    return true;
                    }
                else
                    {
                    return false;
                    }
                }
            }

        /**
        * Removes all of the elements from this set of Keys by clearing the
        * underlying Map.
        */
        public void clear()
            {
            BinaryMap.this.clear();
            }

        /**
        * Returns an array containing all of the keys in this set.
        *
        * @return an array containing all of the keys in this set
        */
        public Object[] toArray()
            {
            return toArray((Object[]) null);
            }

        /**
        * Returns an array with a runtime type is that of the specified array
        * and that contains all of the keys in this Set.  If the Set fits
        * in the specified array, it is returned therein. Otherwise, a new
        * array is allocated with the runtime type of the specified array
        * and the size of this collection.<p>
        *
        * If the Set fits in the specified array with room to spare (i.e.,
        * the array has more elements than the Set), the element in the
        * array immediately following the end of the Set is set to
        * <tt>null</tt>.  This is useful in determining the length of the
        * Set <i>only</i> if the caller knows that the Set does
        * not contain any <tt>null</tt> keys.)<p>
        *
        * @param  ao  the array into which the elements of the Set are to
        * 	          be stored, if it is big enough; otherwise, a new array
        * 	          of the same runtime type is allocated for this purpose
        *
        * @return an array containing the elements of the Set
        *
        * @throws ArrayStoreException if the runtime type of the specified
        *         array is not a supertype of the runtime type of every
        *         element in this Set of keys
        */
        public Object[] toArray(Object ao[])
            {
            BinaryMap map = BinaryMap.this;
            return map.toArray(ao, new Converter()
                {
                public Object convert(Object o)
                    {
                    Block block = (Block) o;
                    return block.getKey();
                    }
                });
            }
        }


    // ----- inner class: ValuesCollection ----------------------------------

    /**
    * Factory pattern.
    *
    * @return a new instance of the ValuesCollection class (or subclass
    *         thereof)
    */
    protected ValuesCollection instantiateValuesCollection()
        {
        return new ValuesCollection();
        }

    /**
    * A collection of values backed by this map.
    */
    protected class ValuesCollection
            extends AbstractCollection
        {
        // ----- Collection interface -----------------------------------

        /**
        * Obtain an iterator over the values in the Map.
        *
        * @return an Iterator that provides a live view of the values in the
        *         underlying Map object
        */
		public Iterator iterator()
            {
		    return new Iterator()
                {
			    private Iterator m_iter = BinaryMap.this.keySet().iterator();

    			public boolean hasNext()
                    {
	    		    return m_iter.hasNext();
		        	}

			    public Object next()
                    {
			        return BinaryMap.this.get(m_iter.next());
			        }

			    public void remove()
                    {
			        m_iter.remove();
			        }
                };
            }

        /**
        * Determine the number of values in the Collection.
        *
        * @return the number of values in the Collection, which is the same
        *         as the number of entries in the underlying Map
        */
		public int size()
            {
		    return BinaryMap.this.size();
		    }

        /**
        * Removes all of the elements from this Collection of values by
        * clearing the underlying Map.
        */
        public void clear()
            {
            BinaryMap.this.clear();
            }

        /**
        * Returns an array containing all of the values in the Collection.
        *
        * @return an array containing all of the values in the Collection
        */
        public Object[] toArray()
            {
            return toArray((Object[]) null);
            }

        /**
        * Returns an array with a runtime type is that of the specified array
        * and that contains all of the values in the Collection.  If the
        * Collection fits in the specified array, it is returned therein.
        * Otherwise, a new array is allocated with the runtime type of the
        * specified array and the size of this collection.<p>
        *
        * If the Collection fits in the specified array with room to spare
        * (i.e., the array has more elements than the Collection), the
        * element in the array immediately following the end of the
        * Collection is set to <tt>null</tt>.  This is useful in determining
        * the length of the Collection <i>only</i> if the caller knows that
        * the Collection does not contain any <tt>null</tt> values.)<p>
        *
        * @param  ao  the array into which the elements of the Collection are
        * 	         to be stored, if it is big enough; otherwise, a new
        * 	         array of the same runtime type is allocated for this
        *            purpose
        *
        * @return an array containing the elements of the Collection
        *
        * @throws ArrayStoreException if the runtime type of the specified
        *         array is not a supertype of the runtime type of every
        *         element in this Collection of values
        */
        public Object[] toArray(Object ao[])
            {
            BinaryMap map = BinaryMap.this;
            return map.toArray(ao, new Converter()
                {
                public Object convert(Object o)
                    {
                    Block block = (Block) o;
                    return block.getValue();
                    }
                });
            }
        }


    // ----- inner class: Entry ---------------------------------------------

    /**
    * Factory pattern: Instantiate an Entry object.
    *
    * @param binKey    a Binary object for the key
    * @param binValue  a Binary object for the value
    *
    * @return a new instance of the Entry class (or a subclass thereof)
    */
    protected Entry instantiateEntry(Binary binKey, Binary binValue)
        {
        return new Entry(binKey, binValue);
        }

    /**
    * A map entry (key-value pair).  The <tt>Map.entrySet</tt> method returns
    * a collection-view of the map, whose elements are of this class.
    */
    public static class Entry
            extends SimpleMapEntry
        {
        /**
        * Construct an Entry with a key and a value.
        *
        * @param binKey    a Binary object for the key
        * @param binValue  a Binary object for the value
        */
        public Entry(Binary binKey, Binary binValue)
            {
            super(binKey, binValue);
            }
        }


    // ----- Buffer management ----------------------------------------------

    /**
    * Obtain the ByteBufferManager that provides the ByteBuffer objects.
    *
    * @return the ByteBufferManager object or null if there is none
    */
    public ByteBufferManager getBufferManager()
        {
        return m_bufmgr;
        }

    /**
    * Specify the ByteBufferManager for this map.
    *
    * @param bufmgr  the ByteBufferManager object (or null)
    */
    protected void setBufferManager(ByteBufferManager bufmgr)
        {
        m_bufmgr = bufmgr;
        }

    /**
    * If there is a buffer manager, check if the current buffer should be
    * grown.
    *
    * @param cbAdditional  the number of bytes pending to be allocated
    *                      in addition to the bytes currently used
    */
    protected void checkBufferGrow(int cbAdditional)
        {
        ByteBufferManager bufmgr = getBufferManager();
        if (bufmgr != null)
            {
            int cbRequired = getUsedCapacity() + cbAdditional;
            if (cbRequired > bufmgr.getGrowthThreshold())
                {
                // since the last block is about to change in size, unlink it
                Block   block = openBlock(getLastBlockOffset());
                boolean fFree = block.isFree();
                if (fFree)
                    {
                    block.unlink();
                    }

                int cbOld = getCapacity();
                setBuffer(null);
                bufmgr.grow(cbRequired);
                setBuffer(bufmgr.getBuffer());

                if (isStrict())
                    {
                    // clear additional buffer space
                    int cbNew = getCapacity();
                    if (cbNew > cbOld)
                        {
                        wipe(cbOld, cbNew - cbOld);
                        }
                    }

                if (fFree)
                    {
                    block.link();
                    }
                block.close();
                }
            }
        }

    /**
    * If there is a buffer manager, check if the current buffer should be
    * shrunk.
    */
    protected void checkBufferShrink()
        {
        ByteBufferManager bufmgr = getBufferManager();
        if (bufmgr != null)
            {
            int cbRequired = getUsedCapacity() + Block.MIN_FREE;
            if (cbRequired < bufmgr.getShrinkageThreshold())
                {
                // make sure the end of the buffer is all free space (unused)
                compactAll();

                // since the last block is about to change in size, unlink it
                Block block = openBlock(getLastBlockOffset());
                assert block.isFree();
                block.unlink();

                setBuffer(null);
                bufmgr.shrink(cbRequired);
                setBuffer(bufmgr.getBuffer());

                // relink the last block
                block.link();
                block.close();
                }
            }
        }

    /**
    * Obtain the ByteBuffer that the BinaryMap is backed by.
    *
    * @return the ByteBuffer object (never null)
    */
    protected ByteBuffer getBuffer()
        {
        ByteBuffer buffer = m_buffer;
        if (buffer == null)
            {
            throw new IllegalStateException(
                "Failed to resize the buffer due to OutOfMemoryError");
            }
        return buffer;
        }

    /**
    * Specify the ByteBuffer that the BinaryMap will be backed by.
    *
    * @param buffer  the new ByteBuffer object
    */
    protected void setBuffer(ByteBuffer buffer)
        {
        ByteBuffer bufferOrig = m_buffer;
        if (buffer != bufferOrig)
            {
            m_buffer    = buffer;
            m_streamIn  = null;
            m_streamOut = null;
            if (buffer != null)
                {
                m_streamIn  = new DataInputStream (new ByteBufferInputStream (buffer));
                m_streamOut = new DataOutputStream(new ByteBufferOutputStream(buffer));
                }
            }
        }

    /**
    * Determine the capacity of the map in bytes.
    *
    * @return the number of bytes that the buffer can hold
    */
    protected int getCapacity()
        {
        return getBuffer().capacity();
        }

    /**
    * Determine the free capacity of the map in bytes.
    *
    * @return the number of bytes in the buffer that are free
    */
    protected int getFreeCapacity()
        {
        return getCapacity() - getUsedCapacity();
        }

    /**
    * Determine the number of types in the buffer that are in use by Entry
    * blocks.
    *
    * @return the number of bytes in the buffer that are used
    */
    protected int getUsedCapacity()
        {
        return getEntryBlockCount() * Block.MIN_ENTRY + m_cbKeyTotal + m_cbValueTotal;
        }

    /**
    * Get the offset of the last block in the buffer.
    *
    * @return  the offset of the last block in the buffer
    */
    protected int getLastBlockOffset()
        {
        return m_ofBlockLast;
        }

    /**
    * Set the offset of the last block in the buffer.
    *
    * @param ofBlock  the offset of the last block in the buffer
    */
    protected void setLastBlockOffset(int ofBlock)
        {
        assert ofBlock != NIL;

        m_ofBlockLast = ofBlock;
        }

    /**
    * Determine if the buffer should be initialized and if blocks should be
    * cleared when not in use.
    *
    * @return true if freed parts of the buffer should be initialized
    */
    protected boolean isStrict()
        {
        return MODE_DEBUG || m_fStrict;
        }

    /**
    * Specify if the buffer should be initialized and if blocks should be
    * cleared when not in use.
    *
    * @param fStrict  pass true to always initialize unused portions of the
    *                 buffer (which is slower but hides unused data)
    */
    protected void setStrict(boolean fStrict)
        {
        m_fStrict = fStrict;
        }

    /**
    * Create one big free block in the buffer.
    */
    protected void clearBuffer()
        {
        setNextCompactBlock(NIL);

        ByteBuffer buffer = m_buffer;
        if (isStrict())
            {
            wipe(0, buffer.capacity());
            }

        m_cEntries      = 0;
        m_cbKeyTotal    = 0;
        m_cbValueTotal  = 0;

        Block block = initBlock(0);
        block.setType(Block.FREE);
        block.link();
        block.close();

        compactBegin();
        }

    /**
    * Get the DataInputStream that maps to the underlying ByteBuffer.
    *
    * @return the DataInputStream that reads from the underlying ByteBuffer
    */
    protected DataInputStream getBufferInput()
        {
        return m_streamIn;
        }

    /**
    * Get the DataOutputStream that maps to the underlying ByteBuffer.
    *
    * @return the DataOutputStream that writes to the underlying ByteBuffer
    */
    protected DataOutputStream getBufferOutput()
        {
        return m_streamOut;
        }

    /**
    * Wipe a portion of the buffer.
    *
    * @param of  the offset into the buffer to wipe at
    * @param cb  the number of bytes to wipe
    */
    protected void wipe(int of, int cb)
        {
        ByteBuffer buffer = getBuffer();
        buffer.position(of);

        byte[] abBlank  = FILL_BUFFER;
        int    cbBlank  = abBlank.length;

        // first get offset up to a FILL_BUFFER boundary point by determining
        // how far the offset is into the FILL_BUFFER array if the entire
        // buffer were packed with the FILL_BUFFER content back to back
        of = of % cbBlank;
        if (of > 0)
            {
            int cbWrite = Math.min(cb, cbBlank - of);
            buffer.put(abBlank, of, cbWrite);
            cb -= cbWrite;
            }

        // fill the remainder
        while (cb > 0)
            {
            int cbWrite = Math.min(cb, cbBlank);
            buffer.put(abBlank, 0, cbWrite);
            cb -= cbBlank;
            }
        }

    /**
    * Debugging support: Validate the buffer's data structures, the hash
    * buckets, free lists, etc.
    *
    * @param sDesc  a description of what is going on when this is called
    */
    public void check(String sDesc)
        {
        ByteBuffer buffer = getBuffer();
        int        cbBuf  = buffer.capacity();

        // go through each block in the buffer, register it and do some basic
        // validations
        HashSet setOffsets = new HashSet();
        boolean fPrevFree  = false;
        int ofLastActual = NIL;
        for (int of = 0, ofPrev = NIL, ofNext; of != NIL; ofPrev = of, of = ofNext)
            {
            // remember last block
            ofLastActual = of;

            if (of < 0 || of > cbBuf - Block.MIN_FREE)
                {
                throw new IllegalStateException(sDesc + ": illegal block offset " + of
                    + " (0x" + Integer.toString(of, 16) + ")");
                }
            Block block = openBlock(of);

            // check block type
            if (!(block.isFree() || block.isEntry()))
                {
                throw new IllegalStateException(sDesc + ": illegal block type " + block.getType()
                    + " for block at " + of + " (0x" + Integer.toString(of, 16) + ")");
                }

            // verify that there are no contiguous free blocks
            boolean fFree = block.isFree();
            if (fPrevFree && fFree)
                {
                throw new IllegalStateException(sDesc + ": two contiguous free blocks found at "
                    + ofPrev + " (0x" + Integer.toString(ofPrev, 16) + ") and "
                    + of + " (0x" + Integer.toString(of, 16) + ")");
                }
            fPrevFree = fFree;

            // register offset as valid
            setOffsets.add(Integer.valueOf(of));

            // verify its offsets are in range (next & prev, block & node)
            int ofPrevBlock = block.getPrevBlockOffset();
            int ofNextBlock = block.getNextBlockOffset();
            int ofPrevNode  = block.getPrevNodeOffset();
            int ofNextNode  = block.getNextNodeOffset();

            // check the range of the previous block offset
            if (ofPrevBlock == NIL)
                {
                if (of != 0)
                    {
                    throw new IllegalStateException(sDesc + ": illegal previous block offset of NIL"
                        + " for block at " + of + " (0x" + Integer.toString(of, 16) + ")");
                    }
                }
            else
                {
                if (ofPrevBlock < 0 || ofPrevBlock >= cbBuf || ofPrevBlock >= of)
                    {
                    throw new IllegalStateException(sDesc + ": previous block offset of " + ofPrevBlock
                        + " (0x" + Integer.toString(ofPrevBlock, 16) + ") for block at " + of
                        + " (0x" + Integer.toString(of, 16) + ") is out of range");
                    }
                }

            // make sure that the previous block offset matches the offset of
            // the block that we just processed
            if (ofPrevBlock != ofPrev)
                {
                throw new IllegalStateException(sDesc + ": previous block offset of " + ofPrevBlock
                    + " (0x" + Integer.toString(ofPrevBlock, 16) + ") for block at " + of
                    + " (0x" + Integer.toString(of, 16) + ") was expected to be " + ofPrev
                    + " (0x" + Integer.toString(ofPrev, 16));
                }

            // check the range of the next block offset
            if (ofNextBlock != NIL)
                {
                if (ofNextBlock < 0 || ofNextBlock >= cbBuf || ofNextBlock <= of)
                    {
                    throw new IllegalStateException(sDesc + ": next block offset of " + ofNextBlock
                        + " (0x" + Integer.toString(ofNextBlock, 16) + ") for block at " + of
                        + " (0x" + Integer.toString(of, 16) + ") is out of range");
                    }
                }

            // check the range of the previous node offset
            if (ofPrevNode == NIL)
                {
                // verify against bucket or free list
                if (block.isFree())
                    {
                    int nCode  = block.getSizeCode();
                    int ofHead = getFreeBlockOffset(nCode);
                    if (ofHead != of)
                        {
                        throw new IllegalStateException(sDesc + ": the free block at " + of
                            + " (0x" + Integer.toString(of, 16) + ") with a size code of " + nCode
                            + " has a previous node offset of NIL, but the free list head for that size"
                            + " code is at " + ofHead + " (0x" + Integer.toString(ofHead, 16) + ")");
                        }
                    }
                else
                    {
                    // entry: make sure it is in exactly on of the two
                    // possible hash buckets
                    int nHash     = block.getKeyHash();
                    int nBucket   = calculateBucket(nHash);
                    int nBucket2  = calculatePreviousBucket(nHash);
                    int ofBucket  = getBucketOffset(nBucket);
                    int ofBucket2 = getBucketOffset(nBucket2);
                    if (of == ofBucket)
                        {
                        // make sure it isn't in both buckets
                        if (nBucket != nBucket2 && of == ofBucket2)
                            {
                            throw new IllegalStateException(sDesc + ": the entry block at " + of
                                + " (0x" + Integer.toString(of, 16) + ") with a hash code of " + nHash
                                + " is found in both bucket " + nBucket + " and " + nBucket2);
                            }
                        }
                    else
                        {
                        // make sure it is in the other bucket
                        if (ofBucket2 != of)
                            {
                            throw new IllegalStateException(sDesc + ": the entry block at " + of
                                + " (0x" + Integer.toString(of, 16) + ") with a hash code of " + nHash
                                + " has a previous node offset of NIL, but the bucket heads ("
                                + nBucket + ", " + nBucket2 + ") for that hash are at offsets "
                                + ofBucket + " (0x" + Integer.toString(ofBucket, 16) + ") and "
                                + ofBucket2 + " (0x" + Integer.toString(ofBucket2, 16) + ")");
                            }
                        }
                    }
                }
            else
                {
                if (ofPrevNode < 0 || ofPrevNode >= cbBuf)
                    {
                    throw new IllegalStateException(sDesc + ": previous node offset of " + ofPrevNode
                        + " (0x" + Integer.toString(ofPrevNode, 16) + ") for block at " + of
                        + " (0x" + Integer.toString(of, 16) + ") is out of range");
                    }
                }

            // check the range of the next node offset
            if (ofNextNode != NIL)
                {
                if (ofNextNode < 0 || ofNextNode >= cbBuf)
                    {
                    throw new IllegalStateException(sDesc + ": next node offset of " + ofNextNode
                        + " (0x" + Integer.toString(ofNextNode, 16) + ") for block at " + of
                        + " (0x" + Integer.toString(of, 16) + ") is out of range");
                    }
                }

            if (block.isEntry())
                {
                // check key and value (key hash code, key length, value length)
                int cbBlock = block.getLength();
                int nHash   = block.getKeyHash();
                int cbKey   = block.getKeyLength();
                int cbValue = block.getValueLength();
                if (cbKey < 0 || cbValue < 0 || (cbKey + cbValue > cbBlock - Block.MIN_ENTRY))
                    {
                    throw new IllegalStateException(sDesc + ": block at " + of
                        + " (0x" + Integer.toString(of, 16) + ") has length of " + cbBlock
                        + " (0x" + Integer.toString(cbBlock, 16) + "), key length of " + cbKey
                        + " (0x" + Integer.toString(cbKey, 16) + ") and value length of " + cbValue
                        + " (0x" + Integer.toString(cbValue, 16) + ")");
                    }

                Binary binKey   = block.getKey();
                int    nKeyHash = binKey.hashCode();
                if (nHash != nKeyHash)
                    {
                    throw new IllegalStateException(sDesc + ": block at " + of
                        + " (0x" + Integer.toString(of, 16) + ") has hash stored as " + nHash
                        + " (0x" + Integer.toString(nHash, 16) + ") but key hashes to " + nKeyHash
                        + " (0x" + Integer.toString(nKeyHash, 16) + ")");
                    }
                }

            if (isStrict())
                {
                // check block fill (both entry/free)
                int cbFill = block.getFillLength();
                int ofFill;
                if (block.isEntry())
                    {
                    ofFill = block.getOffset() + Block.MIN_ENTRY + block.getKeyLength() + block.getValueLength();
                    }
                else
                    {
                    ofFill = block.getOffset() + Block.MIN_FREE;
                    }

                buffer.position(ofFill);
                byte bFill = FILL_BYTE;
                for (int cbRemain = cbFill; cbRemain > 0; --cbRemain)
                    {
                    byte b = buffer.get();
                    if (b != bFill)
                        {
                        throw new IllegalStateException(sDesc + ": block at " + of
                            + " (0x" + Integer.toString(of, 16) + ") is expected to have "
                            + cbFill + " (0x" + Integer.toString(cbFill, 16) + ")"
                            + " bytes of fill at offset " + ofFill
                            + " (0x" + Integer.toString(ofFill, 16) + "); found byte "
                            + ((int) b) + " (0x" + Integer.toString(b, 16) + ") but it should be "
                            + ((int) bFill) + " (0x" + Integer.toString(bFill, 16) + ")");
                        }
                    }
                }

            block.close();

            ofNext = ofNextBlock;
            }

        // go through buckets and free list and make sure that all items are
        // NIL or registered, and if not NIL that the blocks have a previous
        // node of NIL
        for (int i = 0, c = getBucketCount(); i < c; ++i)
            {
            int ofBlock = getBucketOffset(i);
            if (ofBlock != NIL)
                {
                if (!setOffsets.contains(Integer.valueOf(ofBlock)))
                    {
                    throw new IllegalStateException(sDesc + ": bucket " + i + " has offset "
                        + ofBlock + " (0x" + Integer.toString(ofBlock, 16)
                        + ") which is not a valid block offset");
                    }

                Block block  = openBlock(ofBlock);
                if (!block.isEntry())
                    {
                    throw new IllegalStateException(sDesc + ": free list " + i + " has offset "
                        + ofBlock + " (0x" + Integer.toString(ofBlock, 16)
                        + ") which is not an entry block");
                    }

                int ofPrev = block.getPrevNodeOffset();
                if (ofPrev != NIL)
                    {
                    throw new IllegalStateException(sDesc + ": bucket " + i + " has offset "
                        + ofBlock + " (0x" + Integer.toString(ofBlock, 16)
                        + ") which is a valid block offset, but the block at that offset"
                        + " has previous node offset of " + ofPrev
                        + " (0x" + Integer.toString(ofPrev, 16) + ")");
                    }
                block.close();
                }
            }

        for (int i = 0, c = getFreeListCount(); i < c; ++i)
            {
            int ofBlock = getFreeBlockOffset(i);
            if (ofBlock != NIL)
                {
                if (!setOffsets.contains(Integer.valueOf(ofBlock)))
                    {
                    throw new IllegalStateException(sDesc + ": free list " + i + " has offset "
                        + ofBlock + " (0x" + Integer.toString(ofBlock, 16)
                        + ") which is not a valid block offset");
                    }

                Block block  = openBlock(ofBlock);
                if (!block.isFree())
                    {
                    throw new IllegalStateException(sDesc + ": free list " + i + " has offset "
                        + ofBlock + " (0x" + Integer.toString(ofBlock, 16)
                        + ") which is not a free block");
                    }

                int ofPrev = block.getPrevNodeOffset();
                if (ofPrev != NIL)
                    {
                    throw new IllegalStateException(sDesc + ": free list " + i + " has offset "
                        + ofBlock + " (0x" + Integer.toString(ofBlock, 16)
                        + ") which is a valid block offset, but the block at that offset"
                        + " has previous node offset of " + ofPrev
                        + " (0x" + Integer.toString(ofPrev, 16) + ")");
                    }
                block.close();
                }
            }

        // check modulo & incremental rehash; bucket count should match at
        // least one of the two modulos and neither modulo can be larger than
        // the bucket count
        int cBuckets = getBucketCount();
        int nModulo  = getModulo();
        int nModulo2 = getPreviousModulo();
        if (nModulo == nModulo2 && nModulo != cBuckets ||
            nModulo != nModulo2 && (     nModulo > cBuckets || nModulo2 > cBuckets
                                    || !(nModulo == cBuckets || nModulo2 == cBuckets)))
            {
            throw new IllegalStateException(sDesc + ": modulos (" + nModulo + " and "
                + nModulo2 + ") are illegal given the bucket count (" + cBuckets + ")");
            }

        // the next rehash bucket should be NIL if the modulos are identical,
        // otherwise it needs to be in range
        int nRehash = getNextRehashBucket();
        if (   nModulo == nModulo2 &&  nRehash != NIL
            || nModulo != nModulo2 && (nRehash == NIL || nRehash < 0 || nRehash >= cBuckets))
            {
            throw new IllegalStateException(sDesc + ": rehash bucket (" + nRehash
                + ") is illegal given the modulos (" + nModulo + " and " + nModulo2 + ")");
            }

        // incremental compact
        int ofCompact = getNextCompactBlock();
        if (ofCompact != NIL && !setOffsets.contains(Integer.valueOf(ofCompact)))
            {
            throw new IllegalStateException(sDesc + ": increment compact block offset "
                + ofCompact + " (0x" + Integer.toString(ofCompact, 16)
                + ") is not a valid block offset");
            }

        // last block
        int ofLast = getLastBlockOffset();
        if (!setOffsets.contains(Integer.valueOf(ofLast)))
            {
            throw new IllegalStateException(sDesc + ": last block offset "
                + ofLast + " (0x" + Integer.toString(ofLast, 16)
                + ") is not a valid block offset");
            }
        if (ofLast != ofLastActual)
            {
            throw new IllegalStateException(sDesc + ": last block offset "
                + ofLast + " (0x" + Integer.toString(ofLast, 16)
                + ") is not actually last! the real last block is at offset "
                + ofLastActual + " (0x" + Integer.toString(ofLastActual, 16) + ")");
            }

        // open block cache
        int[] aofBlockCache = m_aofBlockCache;
        int   cCacheBlocks  = MAX_OPEN_BLOCKS;
        int   cOpenBlocks   = m_cOpenBlocks;
        if (cOpenBlocks != 0)
            {
            throw new IllegalStateException(sDesc + ": there are " + cOpenBlocks
                    + " open cache blocks (it should be zero)");
            }
        for (int i = 0; i < cCacheBlocks; ++i)
            {
            int of = aofBlockCache[i];
            if (of != NIL)
                {
                throw new IllegalStateException(sDesc + ": the cache block at index " + i
                        + " has an offset of " + of + " (0x" + Integer.toString(of, 16)
                        + ") (it should be NIL)");
                }
            }
        }

    /**
    * Debugging support: Command line test.
    *
    * @param asArg  command line arguments
    */
    public static void main(String[] asArg)
        {
        try
            {
            int cbBuf = 256;
            try
                {
                cbBuf = Integer.parseInt(asArg[0]);
                }
            catch (Exception e) {}
            byte[] abBuf = new byte[cbBuf];
            ByteBuffer buf = ByteBuffer.wrap(abBuf);

            Base.out("Instantiating BinaryMap for a " + cbBuf + "-byte buffer");
            BinaryMap map = new BinaryMap(buf);

            java.io.PrintStream      out = System.out;
            java.io.LineNumberReader in = new java.io.LineNumberReader(
                    new java.io.InputStreamReader(System.in));

            boolean fDone = false;
            do
                {
                out.println();
                out.print("Command: ");
                out.flush();

                String sLine = in.readLine().trim();
                out.println();

                if (sLine != null && sLine.length() > 0)
                    {
                    String[] asParts = Base.parseDelimitedString(sLine, ' ');
                    int      cParts  = asParts.length;
                    String   sCmd    = asParts[0];
                    try
                        {
                        if (   sCmd.equalsIgnoreCase("quit")
                            || sCmd.equalsIgnoreCase("bye")
                            || sCmd.equalsIgnoreCase("exit")
                            || sCmd.equalsIgnoreCase("q"))
                            {
                            fDone = true;
                            }
                        else if (sCmd.equalsIgnoreCase("dump"))
                            {
                            map.dump();
                            }
                        else if (sCmd.equalsIgnoreCase("get"))
                            {
                            if (cParts < 2)
                                {
                                out.println("get <key>");
                                }
                            else
                                {
                                out.println(str(map.get(bin(asParts[1]))));
                                }
                            }
                        else if (sCmd.equalsIgnoreCase("put"))
                            {
                            if (cParts < 3)
                                {
                                out.println("put <key> <value>");
                                }
                            else
                                {
                                out.println(str(map.put(bin(asParts[1]), bin(asParts[2]))));
                                }
                            }
                        else if (sCmd.equalsIgnoreCase("remove"))
                            {
                            if (cParts < 2)
                                {
                                out.println("remove <key>");
                                }
                            else
                                {
                                out.println(str(map.remove(bin(asParts[1]))));
                                }
                            }
                        else if (sCmd.equalsIgnoreCase("clear"))
                            {
                            out.println("before: size()=" + map.size() + ", isEmpty()=" + map.isEmpty());
                            map.clear();
                            out.println("after: size()=" + map.size() + ", isEmpty()=" + map.isEmpty());
                            }
                        else if (sCmd.equalsIgnoreCase("size"))
                            {
                            out.println("size()=" + map.size() + ", isEmpty()=" + map.isEmpty());
                            }
                        else if (sCmd.equalsIgnoreCase("list"))
                            {
                            for (Iterator iter = map.entrySet().iterator(); iter.hasNext(); )
                                {
                                Map.Entry entry = (Map.Entry) iter.next();
                                out.println(str(entry.getKey()) + "=" + str(entry.getValue()));
                                }
                            }
                        else if (sCmd.equalsIgnoreCase("keys"))
                            {
                            for (Iterator iter = map.keySet().iterator(); iter.hasNext(); )
                                {
                                out.println(str(iter.next()));
                                }
                            }
                        else if (sCmd.equalsIgnoreCase("help"))
                            {
                            out.println("get <key>");
                            out.println("put <key> <value>");
                            out.println("remove <key>");
                            out.println("size");
                            out.println("list");
                            out.println("keys");
                            out.println("quit");
                            }
                        else
                            {
                            out.println("unknown command: " + sCmd);
                            out.println("try \"help\"");
                            }
                        }
                    catch (Throwable e)
                        {
                        Base.err(e);
                        }
                    }
                }
            while (!fDone);
            }
        catch (Throwable e)
            {
            Base.err(e);
            }
        }

    /**
    * Debugging support: Dump the inner structures of the BinaryMap to stdout.
    */
    public void dump()
        {
        Base.out("BinaryMap ("
                + "entry count=" + m_cEntries + ")");
        Base.out("ByteBuffer ("
                + "length=" + getCapacity()
                + ", free=" + getFreeCapacity()
                + ", used=" + getUsedCapacity()
                + ", total key bytes=" + m_cbKeyTotal
                + ", total value bytes=" + m_cbValueTotal
                + ", strict=" + isStrict() + "):");
        if (getBuffer().hasArray())
            {
            Base.out(Base.indentString(Base.toHexDump(getBuffer().array(), 32), "  "));
            }
        else
            {
            Base.out("  <no array available>");
            }
        Base.out("Hash buckets ("
                + "bucket count=" + getBucketCount()
                + ", bucket level=" + getBucketLevel()
                + ", modulo=" + getModulo()
                + ", prev modulo=" + getPreviousModulo()
                + ", shrink at=" + getShrinkageCount()
                + ", grow at=" + getGrowthCount()
                + ", rehashing=" + isRehashing()
                + ", next bucket=" + formatIndex(getNextRehashBucket()) + "):");
        Base.out(Base.indentString(formatOffsetArray(m_aofBucket), "  "));
        Base.out("Free lists ("
                + "count=" + getFreeListCount()
                + ", next compaction offset=" + formatOffset(getNextCompactBlock()) + "):");
        Base.out(Base.indentString(formatOffsetArray(m_aofFree), "  "));
        }

    /**
    * Format an index to a String.
    *
    * @param n  the index that (may be NIL)
    *
    * @return a decimal String (or "nil") containing the index in a readable form
    */
    protected static String formatIndex(int n)
        {
        return n == NIL ? "nil" : Integer.toString(n);
        }

    /**
    * Format an offset to a String.
    *
    * @param of  the offset that (may be NIL)
    *
    * @return a hex String (or "nil") containing the offset in a readable form
    */
    protected static String formatOffset(int of)
        {
        return of == NIL ? "nil" : Base.toHexString(of, Base.getMaxHexDigits(of));
        }

    /**
    * Format an array of offsets to be readable in a dump.
    *
    * @param an  the array of offsets
    *
    * @return a String containing the offsets in a readable fashion
    */
    protected static String formatOffsetArray(int[] an)
        {
        int cn = an.length;

        int nMax = -1;
        for (int i = 0; i < cn; ++i)
            {
            if (an[i] > nMax)
                {
                nMax = an[i];
                }
            }

        int cDigitsHead   = Base.getMaxHexDigits(cn);
        int cDigitsEach   = Math.max(Base.getMaxHexDigits(nMax), 4);
        int cCharsPerLine = 137;
        int cPerLine      = (cCharsPerLine - cDigitsHead - 3) / (cDigitsEach + 1);
        int cLines        = (cn + cPerLine - 1) / cPerLine;

        // pre-allocate buffer
        int    cch = cLines * 137;
        char[] ach = new char [cch];
        for (int i = 0; i < cch; ++i)
            {
            ach[i] = ' ';
            }

        // offsets within each line
        int ofColon = cDigitsHead;
        int ofLF    = cCharsPerLine - 1;
        int ofFirst = ofColon + 3;

        int iOffset = 0;
        int ofLine  = 0;
        for (int iLine = 0, iLastLine = cLines - 1; iLine <= iLastLine; ++iLine)
            {
            // format the index into the offset array
            int n  = iOffset;
            int of = ofLine + cDigitsHead;
            for (int i = 0; i < cDigitsHead; ++i)
                {
                ach[--of] = HEX[n & 0x0F];
                n >>= 4;
                }
            ach[ofLine + ofColon] = ':';

            // format data
            for (int iEach = 0; iEach < cPerLine; ++iEach)
                {
                try
                    {
                    n  = an[iOffset++];
                    of = ofLine + ofFirst + (iEach + 1) * (cDigitsEach + 1) - 1;
                    if (n == NIL)
                        {
                        ach[--of] = 'l';
                        ach[--of] = 'i';
                        ach[--of] = 'n';
                        }
                    else
                        {
                        for (int i = 0; i < cDigitsEach; ++i)
                            {
                            ach[--of] = HEX[n & 0x0F];
                            n >>= 4;
                            }
                        }
                    }
                catch (ArrayIndexOutOfBoundsException e) {}
                }

            if (iLine != iLastLine)
                {
                ach[ofLine + ofLF] = '\n';
                }

            ofLine += cCharsPerLine;
            }

        return new String(ach, 0, cch-1);
        }

    /**
    * Internal debugging support: Turn a String into a Binary.
    *
    * @param s  a String (not null)
    *
    * @return a Binary containing the String's value (kind of like ASCII)
    */
    protected static Binary bin(String s)
        {
        return new Binary(s.getBytes());
        }

    /**
    * Internal debugging support: Turn a Binary into a String.
    *
    * @param bin  a Binary object or null
    *
    * @return a String with the Binary's contents unicode-extended or
    *         "&lt;null&gt;" if the passed Binary is null
    */
    protected static String str(Object bin)
        {
        if (bin == null) return "<null>";
        return new String(((Binary) bin).toByteArray());
        }


    // ----- free list management -------------------------------------------

    /**
    * Create an array of references to lists of free blocks indexed by size
    * code.
    */
    protected void initializeFreeLists()
        {
        m_aofFree = new int[MAX_SIZE_CODES];
        clearFreeLists();
        }

    /**
    * Clear out all references in the array of free lists.
    */
    protected void clearFreeLists()
        {
        int[] aof = m_aofFree;
        for (int i = 0, c = aof.length; i < c; ++i)
            {
            aof[i] = NIL;
            }
        }

    /**
    * Determine the number of free lists (ie the number of size codes).
    *
    * @return the number of free lists
    */
    protected int getFreeListCount()
        {
        return m_aofFree.length;
        }

    /**
    * Get the first free block in the linked list of free blocks that have
    * a certain size code.
    *
    * @param nCode  the free block size code
    *
    * @return the offset of the first free block of that size code or NIL
    */
    protected int getFreeBlockOffset(int nCode)
        {
        return m_aofFree[nCode];
        }

    /**
    * Set the head of the free block linked list for a certain size code.
    *
    * @param nCode    the free block size code
    * @param ofBlock  the offset of the first free block of that size code
    *                 or NIL
    */
    protected void setFreeBlockOffset(int nCode, int ofBlock)
        {
        m_aofFree[nCode] = ofBlock;
        }


    // ----- hash management ------------------------------------------------

    /**
    * Create an initial array of hash buckets.
    */
    protected void initializeBuckets()
        {
        // discard whatever is there
        setBucketCount(0);

        // reset to the smallest level
        setBucketLevel(0);

        int nModulo = getModulo();
        setBucketCount(nModulo);

        // there is no "previous" modulo, so it should match the current
        setPreviousModulo(nModulo);
        setNextRehashBucket(NIL);
        }

    /**
    * Determine the hash bucket level. Each level is associated with a
    * specific pre-selected modulo.
    *
    * @return the current hash bucket level
    */
    protected int getBucketLevel()
        {
        return m_nBucketLevel;
        }

    /**
    * Configure the hash bucket level. Each level is associated with a
    * specific pre-selected modulo.
    *
    * This mutator also sets the Modulo, GrowthCount and ShrinkageCount
    * properties.
    *
    * @param nLevel  the new hash bucket level
    */
    protected void setBucketLevel(int nLevel)
        {
        assert nLevel < BUCKET_COUNTS.length;

        m_nBucketLevel = nLevel;

        int nModulo       = BUCKET_COUNTS[nLevel];
        int nModuloShrink = nLevel == 0 ? 0 : BUCKET_COUNTS[nLevel-1];

        assert getMaxLoadFactor() > getMinLoadFactor();

        setModulo(nModulo);
        setGrowthCount(nLevel == BUCKET_COUNTS.length - 1
                ? Integer.MAX_VALUE // no modulo to grow to
                : (int) (nModulo * getMaxLoadFactor()));
        setShrinkageCount((int) (nModuloShrink * getMinLoadFactor()));
        }

    /**
    * Determine the number of hash buckets. This is not necessarily the
    * modulo.
    *
    * @return the number of hash buckets
    */
    protected int getBucketCount()
        {
        return m_aofBucket.length;
        }

    /**
    * Configure the number of hash buckets. This does not change any offset
    * values that are stored in the hash buckets; any additional buckets are
    * initialized to NIL.
    *
    * @param cBuckets  the new number of hash buckets
    */
    protected void setBucketCount(int cBuckets)
        {
        int[] aofOld = m_aofBucket;
        int   cOld   = aofOld == null ? 0 : aofOld.length;
        int   cNew   = cBuckets;
        if (cNew == cOld)
            {
            return;
            }

        int[] aofNew = new int[cNew];
        if (cOld > 0)
            {
            System.arraycopy(aofOld, 0, aofNew, 0, Math.min(cOld, cNew));
            }
        m_aofBucket = aofNew;

        if (cNew > cOld)
            {
            clearBucketOffsets(cOld);
            }
        }

    /**
    * Get the first Entry block in the linked list of Entry blocks that fall
    * into a certain hash bucket.
    *
    * @param nBucket  the bucket number
    *
    * @return the offset of the first Entry block in that bucket, or NIL
    */
    protected int getBucketOffset(int nBucket)
        {
        return m_aofBucket[nBucket];
        }

    /**
    * Set the head of the hash bucket linked list for a certain bucket.
    *
    * @param nBucket  the bucket number
    * @param ofBlock  the offset of the first Entry block in that bucket,
    *                 or NIL
    */
    protected void setBucketOffset(int nBucket, int ofBlock)
        {
        m_aofBucket[nBucket] = ofBlock;
        }

    /**
    * Determine the load factor for the map. This is a value typically
    * greater than zero and less than one, although technically it can be
    * greater than one. This value, multiplied by the current modulo,
    * provides the number of entries that will force growth of the map's
    * modulo.
    *
    * @return the load factor (aka the growth threshold)
    */
    protected double getMaxLoadFactor()
        {
        return m_dflLoadPercentGrow;
        }

    /**
    * Set the load factor.
    *
    * @param dflPercent  the new load factor
    */
    protected void setMaxLoadFactor(double dflPercent)
        {
        m_dflLoadPercentGrow = dflPercent;
        }

    /**
    * Determine the "unload factor" for the map. This is a value typically
    * greater than zero and less than one, although technically it can be
    * greater than one. In any case, it must be smaller than the growth
    * threshold (load factor). This value, multiplied by the next smaller
    * modulo than the current modulo (i.e. the next lower bucket level),
    * provides the number of entries that will force shrinkage of the map's
    * modulo.
    *
    * @return the "unload factor" (aka the shrinkage threshold)
    */
    protected double getMinLoadFactor()
        {
        return m_dflLoadPercentShrink;
        }

    /**
    * Set the "unload factor".
    *
    * @param dflPercent  the new "unload factor"
    */
    protected void setMinLoadFactor(double dflPercent)
        {
        m_dflLoadPercentShrink = dflPercent;
        }

    /**
    * Determine the level at which the modulo will increase.
    *
    * @return the number of entries at which the modulo will grow
    */
    protected int getGrowthCount()
        {
        return m_cEntriesGrow;
        }

    /**
    * Set the level at which the modulo will increase.
    *
    * @param cEntries  the number of entries at which the modulo will grow
    */
    protected void setGrowthCount(int cEntries)
        {
        m_cEntriesGrow = cEntries;
        }

    /**
    * Determine the level at which the modulo will decrease.
    *
    * @return the number of entries at which the modulo will shrink
    */
    protected int getShrinkageCount()
        {
        return m_cEntriesShrink;
        }

    /**
    * Set the level at which the modulo will decrease.
    *
    * @param cEntries  the number of entries at which the modulo will shrink
    */
    protected void setShrinkageCount(int cEntries)
        {
        m_cEntriesShrink = cEntries;
        }

    /**
    * Determine the current modulo.
    *
    * @return the current modulo
    */
    protected int getModulo()
        {
        return m_nModuloCurrent;
        }

    /**
    * Set the new modulo.
    *
    * @param nModulo  the new modulo
    */
    protected void setModulo(int nModulo)
        {
        m_nModuloCurrent = nModulo;
        }

    /**
    * Determine the previous modulo. If a hash bucket resize is still being
    * processed, the previous modulo will be different from the current
    * modulo.
    *
    * @return the previous modulo
    */
    protected int getPreviousModulo()
        {
        return m_nModuloPrevious;
        }

    /**
    * Set the old modulo.
    *
    * @param nModulo  the previous modulo
    */
    protected void setPreviousModulo(int nModulo)
        {
        m_nModuloPrevious = nModulo;
        }

    /**
    * Calculate the bucket for the specified hash code.
    *
    * @param nHash  the hash code for the key
    *
    * @return the bucket index
    */
    protected int calculateBucket(int nHash)
        {
        return (int) ((((long) nHash) & 0xFFFFFFFFL) % (long) getModulo());
        }

    /**
    * Calculate the old bucket for the specified hash code.
    *
    * @param nHash  the hash code for the key
    *
    * @return the bucket index using the previous modulo
    */
    protected int calculatePreviousBucket(int nHash)
        {
        return (int) ((((long) nHash) & 0xFFFFFFFFL) % (long) getPreviousModulo());
        }

    /**
    * Clear out all references in the array of hash buckets.
    */
    protected void clearBucketOffsets()
        {
        clearBucketOffsets(0);
        }

    /**
    * Clear out all references in the array of hash buckets starting with
    * the specified bucket.
    *
    * int nBucket  the first bucket to clear
    */
    protected void clearBucketOffsets(int nBucket)
        {
        for (int cBuckets = getBucketCount(); nBucket < cBuckets; ++nBucket)
            {
            setBucketOffset(nBucket, NIL);
            }
        }

    /**
    * Determine if the modulo should be changed. This should only be checked
    * when the map is growing to avoid problems with iterators when removing
    * all entries (don't want the map to rehash then).
    */
    protected void checkModulo()
        {
        // check if the grow/shrink boundaries have been hit
        int cEntries  = getEntryBlockCount();
        int nDelta    = 0;
        if (cEntries < getShrinkageCount())
            {
            nDelta = -1;
            }
        else if (cEntries >= getGrowthCount())
            {
            nDelta = +1;
            }

        if (nDelta == 0)
            {
            return;
            }

        // make sure the previous incremental rehash is done
        // (the algorithm only supports one previous modulo at a time)
        if (isRehashing())
            {
            rehashAll();
            }

        // remember the current (now previous) modulo for incremental
        // rehashing
        setPreviousModulo(getModulo());

        // configure the new modulo
        setBucketLevel(getBucketLevel() + nDelta);

        // keep no more buckets than are needed by the old and new modulo
        setBucketCount(Math.max(getModulo(), getPreviousModulo()));

        // start the incremental rehash process
        rehashBegin();
        }

    /**
    * Determine if the map is incrementally rehashing.
    *
    * @return true if the map is incrementally rehashing
    */
    protected boolean isRehashing()
        {
        return getModulo() != getPreviousModulo();
        }

    /**
    * Determine the next bucket to rehash.
    *
    * @return the next bucket to rehash
    */
    protected int getNextRehashBucket()
        {
        return m_nBucketNextRehash;
        }

    /**
    * Set the next bucket to rehash.
    *
    * @param nBucket  the next bucket to rehash
    */
    protected void setNextRehashBucket(int nBucket)
        {
        m_nBucketNextRehash= nBucket;
        }

    /**
    * Configure the incremental rehash.
    */
    protected void rehashBegin()
        {
        assert isRehashing();

        setNextRehashBucket(0);
        }

    /**
    * Rehash the specified bucket such that, when done, it will only contain
    * keys that hash to it with the current modulo.
    *
    * Blocks can be in the "wrong" bucket because they are still hashed by
    * the previous modulo; this is the result of incremental rehashing of
    * buckets that allows a modulo change in a huge BinaryMap to occur
    * instantly with the actual associated processing (rehashing) occuring
    * gradually as the map is further used.
    *
    * @param nBucket  the bucket index to rehash
    */
    protected void rehash(int nBucket)
        {
        int ofBlock = getBucketOffset(nBucket);
        while (ofBlock != NIL)
            {
            Block block = openBlock(ofBlock);

            // get the offset of the next block before we change it
            ofBlock = block.getNextNodeOffset();

            // check if this block is in the wrong Bucket
            if (calculateBucket(block.getKeyHash()) != nBucket)
                {
                block.unlink();
                block.link();
                }

            block.close();
            }
        }

    /**
    * Rehash the next incremental rehash block.
    */
    protected void rehashNext()
        {
        if (isRehashing())
            {
            int nBucket  = getNextRehashBucket();
            int cBuckets = getPreviousModulo();

            if (nBucket < cBuckets)
                {
                rehash(nBucket);
                setNextRehashBucket(++nBucket);
                }

            if (nBucket >= cBuckets)
                {
                rehashComplete();
                }
            }
        }

    /**
    * Rehash all blocks such that no block will be linked into the wrong
    * bucket. This is a no-op if a there is no re-hashing to do.
    *
    * Blocks can be in the "wrong" bucket because they are still hashed by
    * the previous modulo; this is the result of incremental rehashing of
    * buckets that allows a modulo change in a huge BinaryMap to occur
    * instantly with the actual associated processing (rehashing) occuring
    * gradually as the map is further used.
    */
    protected void rehashAll()
        {
        if (isRehashing())
            {
            for (int nBucket = getNextRehashBucket(), cBuckets = getBucketCount();
                    nBucket < cBuckets; ++nBucket)
                {
                rehash(nBucket);
                }

            rehashComplete();
            }
        }

    /**
    * Complete the incremental rehash.
    */
    protected void rehashComplete()
        {
        setNextRehashBucket(NIL);
        setPreviousModulo(getModulo());

        // keep no more buckets than are needed by the current (now only)
        // modulo
        setBucketCount(getModulo());
        }


    // ----- Block management -----------------------------------------------

    /**
    * Obtain a Block object for a new block that will be located at the
    * specified offset in the ByteBuffer. The returned block is in an open
    * state.
    *
    * @return the Block object for the new block located at the specified
    *         offset
    */
    protected Block initBlock(int of)
        {
        Block block = grabBlock(of);
        assert block.getType() == Block.NONE;
        return block;
        }

    /**
    * Obtain a Block object for the block located at the specified offset in
    * the ByteBuffer. The returned block is in an open state.
    *
    * @return the Block object for the block located at the specified offset
    */
    protected Block openBlock(int of)
        {
        Block block = grabBlock(of);
        if (block.getType() == Block.NONE)
            {
            block.readHeader();
            }
        return block;
        }

    /**
    * Allocate a free Block object of at least a certain size. Note that the
    * returned block is both open and unlinked.
    *
    * @param cb  the required block size
    *
    * @return a Block object of the required size
    */
    protected Block allocateBlock(int cb)
        {
        assert cb >= 29;

        // consider expanding the buffer
        checkBufferGrow(cb);

        int nCode  = calculateSizeCode(cb);
        int cCodes = MAX_SIZE_CODES;
        for (int i = nCode + 1; i < cCodes; ++i)
            {
            int of = getFreeBlockOffset(i);
            if (of != NIL)
                {
                Block block = openBlock(of);
                block.allocate(cb);
                return block;
                }
            }

        if (cb > getFreeCapacity())
            {
            throw reportOutOfMemory(cb);
            }

        compactUntil(cb);

        for (int i = nCode + 1; i < cCodes; ++i)
            {
            int of = getFreeBlockOffset(i);
            if (of != NIL)
                {
                Block block = openBlock(of);
                block.allocate(cb);
                return block;
                }
            }

        int of = getFreeBlockOffset(nCode);
        while (of != NIL)
            {
            Block block = openBlock(of);
            if (block.length() >= cb)
                {
                block.allocate(cb);
                return block;
                }
            of = block.getNextNodeOffset();
            block.close();
            }

        // this is an assertion, since we already verified that there is
        // enough free memory in the buffer
        throw reportOutOfMemory(cb);
        }

    /**
    * Determine if the map is incrementally compacting.
    *
    * @return true if the map is incrementally compacting
    */
    protected boolean isCompacting()
        {
        return getEntryBlockCount() > 0;
        }

    /**
    * Determine the next block to compact.
    *
    * @return the next block to compact
    */
    protected int getNextCompactBlock()
        {
        return m_ofBlockNextCompact;
        }

    /**
    * Set the next block to compact.
    *
    * @param ofBlock  the next block to compact
    */
    protected void setNextCompactBlock(int ofBlock)
        {
        m_ofBlockNextCompact = ofBlock;
        }

    /**
    * Configure the incremental compact.
    */
    protected void compactBegin()
        {
        setNextCompactBlock(0);
        }

    /**
    * Perform an an incremental compact at the specified block.
    *
    * @param cbReqFree  the number of bytes required to be free
    */
    protected void compactUntil(int cbReqFree)
        {
        assert cbReqFree <= getFreeCapacity();

        if (MODE_DEBUG)
            {
            check("before compactUntil(" + cbReqFree + ")");
            }

        ByteBuffer buffer = getBuffer();
        byte[]     abBuf  = null;
        int        cbBuf  = SIZE_COPY_BUFFER;

        int cbNextFree;
        do
            {
            cbNextFree = 0;

            int   ofBlock = getNextCompactBlock();
            setNextCompactBlock(NIL);

            Block block   = openBlock(ofBlock);
            int   ofNext  = block.getNextBlockOffset();

            if (block.isFree())
                {
                cbNextFree = block.getLength();

                // make sure that it is not the last block (nothing to do)
                if (ofNext != NIL)
                    {
                    // the next block is an entry, move it here
                    Block blockEntry = openBlock(ofNext);
                    assert blockEntry.isEntry();

                    int ofFollow = blockEntry.getNextBlockOffset(); // the block after the entry
                    int cbFill   = blockEntry.getFillLength();      // the entry fill length
                    int cbNext   = blockEntry.length();             // the entry length
                    int cbEntry  = cbNext - cbFill;                 // the entry length minus the fill
                    int ofFree   = ofBlock + cbEntry;               // where to move the free block
                    int cbFree   = block.getLength() + cbFill;

                    block.unlink();
                    blockEntry.unlink();

                    // move the entry (minus the fill) to where the free
                    // block is
                    int ofCopyFrom = ofNext  + Block.OFFSET_HASH;
                    int ofCopyTo   = ofBlock + Block.OFFSET_HASH;
                    int cbCopy     = blockEntry.getKeyLength() + blockEntry.getValueLength() + 12;
                    if (abBuf == null)
                        {
                        abBuf = new byte[cbBuf];
                        }
                    buffercopy(buffer, ofCopyFrom, ofCopyTo, cbCopy, abBuf);  // copy key/value
                    blockEntry.setOffset(ofBlock);                            // copy header
                    blockEntry.setPrevBlockOffset(block.getPrevBlockOffset());
                    blockEntry.setNextBlockOffset(ofFree);

                    // move the free block immediately after the entry
                    // (taking any entry fill)
                    block.setOffset(ofFree);
                    block.setPrevBlockOffset(ofBlock);
                    block.setNextBlockOffset(ofFollow);

                    // patch up following block to point to the free block
                    // (which is where the entry used to be)
                    boolean fMerge = false;
                    if (ofFollow != NIL)
                        {
                        Block blockFollow = openBlock(ofFollow);
                        fMerge = blockFollow.isFree();
                        blockFollow.setPrevBlockOffset(ofFree);
                        blockFollow.close();
                        }

                    // strict mode: wipe the free block clean where the key
                    // and value data used to be
                    if (isStrict())
                        {
                        block.clear();
                        }

                    block.link();
                    blockEntry.link();
                    blockEntry.close();

                    // start at this free block next incremental compact
                    ofNext     = ofFree;
                    cbNextFree = cbFree;

                    // if follow block is a free block, merge with the
                    // current one (had to wait until everything was
                    // relinked and the everything else was closed)
                    if (fMerge)
                        {
                        block.merge();
                        cbNextFree = block.getLength();
                        }
                    }
                }
            else
                {
                int cbFill = block.getFillLength();
                if (cbFill >= Block.MIN_FREE)
                    {
                    // make the fill into a free block and make that free
                    // block the next block to start compacting at
                    int ofEntry  = ofBlock;
                    int cbEntry  = block.getLength();
                    int ofFree   = ofEntry + cbEntry - cbFill;
                    int cbFree   = cbFill;
                    int ofFollow = ofNext;

                    // trim the entry block fill (and close it to advance
                    // to the new free block
                    block.setNextBlockOffset(ofFree);
                    block.close();

                    // adjust the following block to point to the new free
                    // block
                    boolean fMerge = false;
                    if (ofFollow != NIL)
                        {
                        Block blockFollow = openBlock(ofFollow);
                        fMerge = blockFollow.isFree();
                        blockFollow.setPrevBlockOffset(ofFree);
                        blockFollow.close();
                        }

                    // create and link the new free block
                    block = initBlock(ofFree);
                    block.setType(Block.FREE);
                    block.setNextBlockOffset(ofFollow);
                    block.setPrevBlockOffset(ofBlock);
                    block.link();

                    ofNext     = ofFree;
                    cbNextFree = cbFree;

                    // if the "next block" (i.e. the new free block) and
                    // the following blocks are both free then merge them
                    if (fMerge)
                        {
                        block.merge();
                        cbNextFree = block.getLength();
                        }
                    }
                else if (cbFill > 0 && ofNext != NIL)
                    {
                    // give the fill to the next block (shift the next block
                    // to the "left")
                    int ofEntry    = ofBlock;
                    int cbEntryOld = block.getLength();
                    int cbEntryNew = cbEntryOld - cbFill;

                    int ofNextOld  = ofNext;
                    int ofNextNew  = ofNext - cbFill;
                    assert ofNextNew == ofEntry + cbEntryNew;

                    // trim the entry block fill (and close it to advance
                    // to the shifting block
                    block.setNextBlockOffset(ofNextNew);
                    block.close();

                    // advance to the block that will get shifted
                    block = openBlock(ofNextOld);

                    // adjust the following block to point back to the new
                    // offset of the about-to-be-shifted block
                    int ofFollow = block.getNextBlockOffset();
                    if (ofFollow != NIL)
                        {
                        Block blockFollow = openBlock(ofFollow);
                        blockFollow.setPrevBlockOffset(ofNextNew);
                        blockFollow.close();
                        }

                    block.unlink();

                    if (block.isFree())
                        {
                        // nothing much to do to shift a free block other
                        // than clear out its bytes (for strict option)
                        // and recalculate its size as the next free block
                        if (isStrict())
                            {
                            wipe(ofNextOld, Block.MIN_FREE);
                            }
                        cbNextFree = block.getLength() + cbFill;
                        }
                    else
                        {
                        // to shift an entry is more tricky; first the key
                        // and value need to be shifted, the remainder of
                        // the used portion of the old block position needs
                        // to be cleared, and then it's safe to change the
                        // block offset (since the rest of the data is in
                        // the header); remember to include the hash data
                        // when copying the key and value data
                        int ofCopyFrom = ofNextOld + Block.OFFSET_HASH;
                        int ofCopyTo   = ofNextNew + Block.OFFSET_HASH;
                        int cbCopy     = block.getKeyLength() + block.getValueLength() + 12;
                        if (abBuf == null)
                            {
                            abBuf = new byte[cbBuf];
                            }
                        buffercopy(buffer, ofCopyFrom, ofCopyTo, cbCopy, abBuf);
                        if (isStrict())
                            {
                            wipe(ofCopyTo + cbCopy, ofCopyFrom - ofCopyTo);
                            }
                        }

                    block.setOffset(ofNextNew);
                    block.link();

                    ofNext = ofNextNew;
                    }
                }

            block.close();
            setNextCompactBlock(ofNext);
            if (ofNext == NIL)
                {
                compactComplete();
                }
            }
        while (cbNextFree < cbReqFree);

        if (MODE_DEBUG)
            {
            check("after compactUntil(" + cbReqFree + ")");
            }
        }

    /**
    * Perform an incremental compaction of the next block.
    */
    protected void compactNext()
        {
        if (isCompacting())
            {
            compactUntil(0);
            }
        }

    /**
    * Full linear compaction of the buffer.
    */
    protected void compactAll()
        {
        setNextCompactBlock(NIL);

        clearFreeLists();
        clearBucketOffsets();

        int    ofSrc      = 0;
        int    ofDest     = 0;
        int    ofPrevDest = NIL;
        byte[] abBuf      = null;
        int    cbBuf      = SIZE_COPY_BUFFER;
        ByteBuffer buffer = getBuffer();
        while (ofSrc != NIL)
            {
            Block block      = openBlock(ofSrc);
            int   ofNextSrc  = block.getNextBlockOffset();
            int   ofNextDest = ofDest;

            if (block.isEntry())
                {
                int cbBlock = block.getLength();
                int cbFill  = block.getFillLength();

                // advance next destination offset past the "slimmed down"
                // Entry block (no fill)
                ofNextDest += (cbBlock - cbFill);

                if (ofSrc != ofDest)
                    {
                    assert ofSrc > ofDest;

                    // copy key and value data  (including hash code and two
                    // lengths)
                    int ofCopyFrom = ofSrc  + Block.OFFSET_HASH;
                    int ofCopyTo   = ofDest + Block.OFFSET_HASH;
                    int cbCopy     = block.getKeyLength() + block.getValueLength() + 12;
                    if (abBuf == null)
                        {
                        abBuf = new byte[cbBuf];
                        }
                    buffercopy(buffer, ofCopyFrom, ofCopyTo, cbCopy, abBuf);

                    // shift the block itself
                    block.setOffset(ofDest);
                    }

                block.setPrevBlockOffset(ofPrevDest);
                block.setNextBlockOffset(ofNextDest);
                block.setNextNodeOffset(NIL);
                block.setPrevNodeOffset(NIL);

                block.link();
                block.close();

                ofPrevDest = ofDest;
                ofDest     = ofNextDest;
                }
            else
                {
                block.discard();
                }

            ofSrc = ofNextSrc;
            }

        assert ofDest < getCapacity();

        Block block = initBlock(ofDest);
        block.setType(Block.FREE);
        block.setPrevBlockOffset(ofPrevDest);
        if (isStrict())
            {
            block.clear();
            }
        block.link();
        block.close();

        compactBegin();
        }

    /**
    * Complete the incremental compact.
    */
    protected void compactComplete()
        {
        // start over
        compactBegin();
        }

    /**
    * Grab a block object for the specified offset. This method returns an
    * open block if one is open for that offset, or uses a recycled block if
    * one is not already open for that offset.
    *
    * @param ofBlock  the offset of the block to grab
    *
    * @return a block for the specified offset
    */
    protected Block grabBlock(int ofBlock)
        {
        int[]   aofBlockCache = m_aofBlockCache;
        Block[] ablockCache   = m_ablockCache;
        int     cCacheBlocks  = MAX_OPEN_BLOCKS;
        int     cOpenBlocks   = m_cOpenBlocks;
        Block   block         = null;
        for (int i = 0; i < cOpenBlocks; ++i)
            {
            if (aofBlockCache[i] == ofBlock)
                {
                block = ablockCache[i];
                break;
                }
            }

        if (block == null)
            {
            if (cOpenBlocks < cCacheBlocks)
                {
                aofBlockCache[cOpenBlocks] = ofBlock;
                block = ablockCache[cOpenBlocks];
                block.init(ofBlock);
                m_cOpenBlocks = cOpenBlocks + 1;
                }
            else
                {
                throw new IllegalStateException("grabBlock(): ran out of blocks");
                }
            }

        block.use();
        return block;
        }

    /**
    * When an open block changes position in the buffer, this method is
    * invoked to adjust the cache of open blocks.
    *
    * @param ofOld  the old offset of the block
    * @param ofNew  the new offset of the block
    */
    protected void adjustOpenBlockOffset(int ofOld, int ofNew)
        {
        int[] aofBlockCache = m_aofBlockCache;
        int   cOpenBlocks   = m_cOpenBlocks;
        for (int i = 0; i < cOpenBlocks; ++i)
            {
            if (aofBlockCache[i] == ofOld)
                {
                aofBlockCache[i] = ofNew;
                return;
                }
            }

        assert false: "could not find open block at offset " + ofOld + " (0x"
                + Integer.toString(ofOld, 16) + ") that is being moved to "
                + ofNew + " (0x" + Integer.toString(ofNew, 16) + ")";
        }

    /**
    * Release (recycle) the specified Block object. This method should not be
    * called directly; instead, call block.close().
    *
    * @param block  the Block object to release
    */
    protected void recycleBlock(Block block)
        {
        assert block.getOffset() == NIL;

        int[]   aofBlockCache = m_aofBlockCache;
        Block[] ablockCache   = m_ablockCache;
        int     cOpenBlocks   = m_cOpenBlocks;

        for (int i = 0, iLast = cOpenBlocks - 1; i <= iLast; ++i)
            {
            if (ablockCache[i] == block)
                {
                if (i < iLast)
                    {
                    // swap with last open block
                    aofBlockCache[i]     = aofBlockCache[iLast];
                    ablockCache  [i]     = ablockCache  [iLast];
                    aofBlockCache[iLast] = NIL;
                    ablockCache  [iLast] = block;
                    }
                else
                    {
                    // last block; leave it here
                    aofBlockCache[i] = NIL;
                    }

                // decrease open block count
                m_cOpenBlocks = cOpenBlocks - 1;
                return;
                }
            }

        assert false: "attempt to release block that was not open";
        }

    /**
    * Copy from one part of the buffer to another. This method only supports
    * copying from a latter part of the buffer to an earlier part of the
    * buffer.
    *
    * @param buffer      the ByteBuffer containing the data to copy
    * @param ofCopyFrom  the source offset into the ByteBuffer
    * @param ofCopyTo    the destination offset into the ByteBuffer
    * @param cbCopy      the number of bytes to copy
    * @param abBuf       a temporary byte array available for use
    */
    protected static void buffercopy(ByteBuffer buffer, int ofCopyFrom, int ofCopyTo, int cbCopy, byte[] abBuf)
        {
        assert ofCopyFrom > ofCopyTo;
        assert cbCopy > 0;
        assert abBuf != null && abBuf.length > 0;

        int cbBuf = abBuf.length;
        while (cbCopy > 0)
            {
            int cbChunk = Math.min(cbCopy, cbBuf);

            buffer.position(ofCopyFrom);
            buffer.get(abBuf, 0, cbChunk);

            buffer.position(ofCopyTo);
            buffer.put(abBuf, 0, cbChunk);

            ofCopyFrom += cbChunk;
            ofCopyTo   += cbChunk;
            cbCopy     -= cbChunk;
            }
        }

    /**
    * Determine which "free bucket" a block of a particular size would go
    * into.
    *
    * @param cbBlock  the size of the block
    *
    * @return the size code for a block of the specified size
    */
    protected static int calculateSizeCode(int cbBlock)
        {
        /*
        * The size codes and their corresponding bit patterns are as follows:
        *
        *       3322222222221111111111
        * code  10987654321098765432109876543210  description (block size)
        * ----  --------------------------------  ---------------------------
        *    0  00000000000000000000000000??????  63 bytes or smaller
        *    1  00000000000000000000000001??????  64 to 127 bytes
        *    2  0000000000000000000000001???????  128 to 255 bytes
        *    3  000000000000000000000001????????  256 to 511 bytes
        *    4  00000000000000000000001?????????  512 to 1023 bytes
        *    5  0000000000000000000001??????????  1024 to 2047 bytes
        *    6  000000000000000000001???????????  2048 to 4095 bytes
        *    7  00000000000000000001????????????  4096 to 8191 bytes
        *    8  0000000000000000001?????????????  8192 to 16383 bytes
        *    9  000000000000000001??????????????  16384 to 32767 bytes
        *   10  00000000000000001???????????????  32768 to 65535 bytes
        *   11  0000000000000001????????????????  65536 to 131071 bytes
        *   12  000000000000001?????????????????  131072 to 262143 bytes
        *   13  00000000000001??????????????????  262144 to 524287 bytes
        *   14  0000000000001???????????????????  524288 to 1048575 bytes
        *   15  000000000001????????????????????  1048576 to 2097151 bytes
        *   16  00000000001?????????????????????  2097152 to 4194303 bytes
        *   17  0000000001??????????????????????  4194304 to 8388607 bytes
        *   18  000000001???????????????????????  8388608 to 16777215 bytes
        *   19  00000001????????????????????????  16777216 to 33554431 bytes
        *   20  0000001?????????????????????????  33554432 to 67108863 bytes
        *   21  000001??????????????????????????  67108864 to 134217727 bytes
        *   22  00001???????????????????????????  134217728 to 268435455 bytes
        *   23  0001????????????????????????????  268435456 to 536870911 bytes
        *   24  001?????????????????????????????  536870912 to 1073741823 bytes
        *   25  01??????????????????????????????  1073741824 to 2147483647 bytes
        *       1???????????????????????????????  illegal (negative number)
        */

        int nNibble = cbBlock >>> 6;
        int cShifts = 0;

        if (nNibble > 0x0000FFFF)
            {
            nNibble >>>= 16;
            cShifts   += 16;
            }

        if (nNibble > 0x000000FF)
            {
            nNibble >>>= 8;
            cShifts   += 8;
            }

        if (nNibble > 0x0000000F)
            {
            nNibble >>>= 4;
            cShifts   += 4;
            }

        switch (nNibble)
            {
            case 0:
                // this can only happen if the size was less than 64 (since
                // the most significant bit would have been shifted out of
                // the nibble to start with)
                assert cShifts == 0;
                return 0;

            case 1:
                return cShifts + 1;

            case 2: case 3:
                // if the original block size was negative, the number of
                // shifts would be 24 and the second bit of the nibble would
                // be set, thus hitting this section
                assert cbBlock >= 0 : "Negative block size: " + cbBlock;
                return cShifts + 2;

            case 4: case 5: case 6: case 7:
                return cShifts + 3;

            case 8 : case 9 : case 10: case 11:
            case 12: case 13: case 14: case 15:
                return cShifts + 4;

            default:
                throw new AssertionError("Nibble out of range: " + nNibble);
            }

        }


    // ----- inner class: Block ---------------------------------------------

    /**
    * Factory method: Create a Block object.
    *
    * @return a new instance of the Block class or subclass thereof
    */
    protected Block instantiateBlock()
        {
        return new Block();
        }

    /**
    * A Block is the unit of storage within a Buffer. There are free blocks
    * and Entry blocks (that store key/value pairs).
    */
    public class Block
            extends Base
        {
        // ----- constructors -------------------------------------------

        /**
        * Construct a Block.
        */
        public Block()
            {
            reset();
            }


        // ----- life cycle ---------------------------------------------

        /**
        * Configure the Block object to point to a specific offset within the
        * Buffer.
        *
        * @param of  the offset of the Block within the Buffer
        */
        public void init(int of)
            {
            assert m_ofThisBlock == NIL;
            m_ofThisBlock = of;
            }

        /**
        * Increment the use count for this block.
        */
        protected void use()
            {
            ++m_cUses;
            }

        /**
        * Decrement the use count for this block and check if it is now zero.
        */
        protected boolean finishUse()
            {
            assert m_cUses > 0;

            int cUses = m_cUses;
            if (cUses > 0)
                {
                m_cUses = --cUses;
                }

            return cUses == 0;
            }

        /**
        * Reset the Block object so that it doesn't point into the Buffer.
        * Block objects can be re-used to reduce allocations by calling
        * the close() method.
        */
        public void reset()
            {
            m_nType         = NONE;
            m_ofThisBlock   = NIL;
            m_ofNextBlock   = NIL;
            m_ofPrevBlock   = NIL;
            m_ofNextNode    = NIL;
            m_ofPrevNode    = NIL;
            m_nHash         = 0;
            m_cbKey         = 0;
            m_cbValue       = 0;
            m_fHeaderMod    = false;
            m_fKeyMod       = false;
            m_fValueMod     = false;
            m_binKey        = null;
            m_binValue      = null;
            }

        /**
        * Close the Block object by resetting its contents and recycling the
        * object.
        */
        public void flush()
            {
            if (m_fHeaderMod)
                {
                writeHeader();
                }

            if (m_fKeyMod)
                {
                writeKey();
                }

            if (m_fValueMod)
                {
                writeValue();
                }
            }

        /**
        * Close the Block object, flushing any pending changes, resetting its
        * contents and recycling the object.
        */
        public void close()
            {
            if (m_ofThisBlock != NIL)
                {
                if (getNextBlockOffset() == NIL)
                    {
                    setLastBlockOffset(m_ofThisBlock);
                    }

                if (finishUse())
                    {
                    flush();
                    reset();
                    recycleBlock(this);
                    }
                }
            }

        /**
        * Recycle the Block object, discarding any changes, resetting its
        * contents and recycling the object.
        */
        public void discard()
            {
            if (m_ofThisBlock != NIL)
                {
                boolean fFinish = finishUse();
                assert fFinish;

                // adjust incremental compaction if it is pointing at this block
                if (m_ofThisBlock == getNextCompactBlock())
                    {
                    int ofPrev = getPrevBlockOffset();
                    if (ofPrev == NIL)
                        {
                        ofPrev = 0;
                        }
                    setNextCompactBlock(ofPrev);
                    }

                if (isStrict())
                    {
                    clear();
                    }

                reset();
                recycleBlock(this);
                }
            }

        /**
        * Zero the block.
        */
        public void clear()
            {
            wipe(getOffset(), getLength());
            }

        /**
        * Zero the value portion of the block.
        */
        public void clearValue()
            {
            wipe(getOffset() + OFFSET_VALUE + getKeyLength(), getValueLength() + 4);
            }


        // ----- block header accessors -------------------------------------

        /**
        * Determine the current Block type.
        *
        * @return one of {NONE, FREE, ENTRY}
        */
        public int getType()
            {
            return m_nType;
            }

        /**
        * Set the Block type.
        *
        * @param nType  the new Block type
        */
        public void setType(int nType)
            {
            assert nType == NONE || nType == FREE || nType == ENTRY : "Illegal Block Type: " + nType;

            if (nType != m_nType)
                {
                m_nType      = nType;
                m_fHeaderMod = true;
                }
            }

        /**
        * Determine if the Block is marked as free.
        *
        * @return true if and only if the type is FREE
        */
        public boolean isFree()
            {
            return getType() == FREE;
            }

        /**
        * Determine if the Block is marked as an Entry.
        *
        * @return true if and only if the type is ENTRY
        */
        public boolean isEntry()
            {
            return getType() == ENTRY;
            }

        /**
        * Determine the offset of this Block in the Buffer.
        *
        * @return the offset of this Block in the Buffer, or NIL if this Block
        *         has not been initialized
        */
        public int getOffset()
            {
            return m_ofThisBlock;
            }

        /**
        * Specify the offset of this Block in the Buffer.
        *
        * @param ofBlock  the offset of this Block
        */
        public void setOffset(int ofBlock)
            {
            int ofPrev = m_ofThisBlock;
            if (ofBlock != ofPrev)
                {
                // adjust incremental compaction if it is pointing at this block
                if (ofPrev == getNextCompactBlock())
                    {
                    setNextCompactBlock(ofBlock);
                    }

                adjustOpenBlockOffset(ofPrev, ofBlock);

                m_ofThisBlock = ofBlock;
                m_fHeaderMod  = true;
                }
            }

        /**
        * Determine the length of the block.
        *
        * @return the length, in bytes, of this block
        */
        public int getLength()
            {
            return length();
            }

        /**
        * Determine the length of the block.
        *
        * @return the length, in bytes, of this block
        */
        public int length()
            {
            int ofThis = m_ofThisBlock;
            assert ofThis != NIL: "Block is not initialized";

            int ofNext = m_ofNextBlock;
            if (ofNext == NIL)
                {
                ofNext = getBuffer().capacity();
                }

            return ofNext - ofThis;
            }

        /**
        * Determine the "free block size code" for a block of this size.
        *
        * @return the size code for this block
        */
        public int getSizeCode()
            {
            return calculateSizeCode(length());
            }

        /**
        * Determine the offset of the next Block in the Buffer.
        *
        * @return the offset of the next Block in the Buffer, or NIL if
        *         this Block is the last in the Buffer
        */
        public int getNextBlockOffset()
            {
            return m_ofNextBlock;
            }

        /**
        * Specify the offset of the next Block in the Buffer.
        *
        * @param ofBlock  the offset of the next contiguous Block
        */
        public void setNextBlockOffset(int ofBlock)
            {
            if (ofBlock != m_ofNextBlock)
                {
                m_ofNextBlock = ofBlock;
                m_fHeaderMod  = true;
                }
            }

        /**
        * Determine the offset of the previous Block in the Buffer.
        *
        * @return the offset of the previous Block in the Buffer, or NIL if
        *         this Block is the first in the Buffer
        */
        public int getPrevBlockOffset()
            {
            return m_ofPrevBlock;
            }

        /**
        * Specify the offset of the previous Block in the Buffer.
        *
        * @param ofBlock  the offset of the previous contiguous Block
        */
        public void setPrevBlockOffset(int ofBlock)
            {
            if (ofBlock != m_ofPrevBlock)
                {
                m_ofPrevBlock = ofBlock;
                m_fHeaderMod  = true;
                }
            }

        /**
        * Determine the offset of the next Block in the linked list.
        *
        * @return the offset of the next Block in the linked list, or NIL if
        *         this Block is the last in the linked list
        */
        public int getNextNodeOffset()
            {
            return m_ofNextNode;
            }

        /**
        * Specify the offset of the next Block in the linked list.
        *
        * @param ofBlock  the offset of the next Block in the linked list of
        *                 Blocks
        */
        public void setNextNodeOffset(int ofBlock)
            {
            if (ofBlock != m_ofNextNode)
                {
                m_ofNextNode = ofBlock;
                m_fHeaderMod = true;
                }
            }

        /**
        * Determine the offset of the previous Block in the linked list.
        *
        * @return the offset of the previous Block in the linked list, or NIL
        *         if this Block is the first in the linked list of Blocks
        */
        public int getPrevNodeOffset()
            {
            return m_ofPrevNode;
            }

        /**
        * Specify the offset of the previous Block in the linked list.
        *
        * @param ofBlock  the offset of the previous Block in the linked list
        *                 of blocks
        */
        public void setPrevNodeOffset(int ofBlock)
            {
            if (ofBlock != m_ofPrevNode)
                {
                m_ofPrevNode = ofBlock;
                m_fHeaderMod = true;
                }
            }


        // ----- Entry-specific accessors -----------------------------------

        /**
        * Get the hash code for the Entry block.
        *
        * @return the hash code for the Entry block
        */
        public int getKeyHash()
            {
            return m_nHash;
            }

        /**
        * Get the length of the Entry key in the block.
        *
        * @return the length, in bytes, of the key
        */
        public int getKeyLength()
            {
            return m_cbKey;
            }

        /**
        * Get the Entry key in the block, lazy loading it if necessary.
        *
        * @return the Entry key
        */
        public Binary getKey()
            {
            if (m_binKey == null)
                {
                readKey();
                }

            return m_binKey;
            }

        /**
        * Update the Entry key in the block. The write is deferred.
        *
        * @param bin  the Entry key
        */
        public void setKey(Binary bin)
            {
            Binary binOld = m_binKey;
            if (!equals(bin, binOld))
                {
                m_binKey  = bin;
                m_cbKey   = bin.length();
                m_nHash   = bin.hashCode();
                m_fKeyMod = true;
                }
            }

        /**
        * Get the length of the Entry value in the block.
        *
        * @return the length, in bytes, of the value
        */
        public int getValueLength()
            {
            return m_cbValue;
            }

        /**
        * Get the Entry value in the block, lazy loading it if necessary.
        *
        * @return the Entry value
        */
        public Binary getValue()
            {
            if (m_binValue == null)
                {
                readValue();
                }

            return m_binValue;
            }

        /**
        * Update the Entry value in the block. The write is deferred.
        *
        * @param bin  the Entry value
        */
        public void setValue(Binary bin)
            {
            Binary binOld = m_binValue;
            if (!equals(bin, binOld))
                {
                m_binValue  = bin;
                m_cbValue   = bin.length();
                m_fValueMod = true;
                }
            }

        /**
        * Get the size of the fill in the block.
        *
        * @return the length, in bytes, of the fill
        */
        public int getFillLength()
            {
            assert isFree() || isEntry() : "Attempt to get fill length for invalid block type=" + getType();
            return isEntry() ? length() - MIN_ENTRY - getKeyLength() - getValueLength()
                             : length() - MIN_FREE;
            }


        // ----- block i/o --------------------------------------------------

        /**
        * Read a block's header data from the Buffer. Also reads key hash,
        * key length and value length.
        */
        public void readHeader()
            {
            assert !m_fHeaderMod : "Attempt to re-read header for block at offset " + getOffset()
                    + " after header was modified";

            getBuffer().position(m_ofThisBlock);
            DataInputStream stream = getBufferInput();

            try
                {
                m_nType       = stream.readByte();
                m_ofNextBlock = stream.readInt();
                m_ofPrevBlock = stream.readInt();
                m_ofNextNode  = stream.readInt();
                m_ofPrevNode  = stream.readInt();

                switch (m_nType)
                    {
                    case NONE:
                        throw new AssertionError(
                            "Illegal block type (NONE) found at offset " + m_ofThisBlock);

                    case FREE:
                        break;

                    case ENTRY:
                        m_nHash    = stream.readInt();
                        m_cbKey    = stream.readInt();
                        stream.skip(m_cbKey);
                        m_cbValue  = stream.readInt();
                        break;

                    default:
                        throw new AssertionError("Illegal block type ("
                            + m_nType + ") found at offset " + m_ofThisBlock);
                    }
                }
            catch (IOException e)
                {
                throw Base.ensureRuntimeException(e);
                }

            m_fHeaderMod = false;
            }

        /**
        * Write the block's data to the Buffer.
        */
        public void writeHeader()
            {
            assert isFree() || isEntry() : "Attempt to write block of type " + getType();

            getBuffer().position(m_ofThisBlock);
            DataOutputStream stream = BinaryMap.this.getBufferOutput();

            try
                {
                stream.writeByte(m_nType);
                stream.writeInt(m_ofNextBlock);
                stream.writeInt(m_ofPrevBlock);
                stream.writeInt(m_ofNextNode);
                stream.writeInt(m_ofPrevNode);
                }
            catch (IOException e)
                {
                throw Base.ensureRuntimeException(e);
                }

            m_fHeaderMod = false;
            }

        /**
        * Read the "key" portion of an Entry block.
        */
        public void readKey()
            {
            assert isEntry();
            assert getOffset() != NIL;

            getBuffer().position(getOffset() + OFFSET_KEY);

            try
                {
                m_binKey = new Binary(BinaryMap.this.getBufferInput());
                }
            catch (IOException e)
                {
                throw Base.ensureRuntimeException(e);
                }

            assert getKeyLength() == m_binKey.length();
            }

        /**
        * Write the "key" portion of an Entry block, including the key hash.
        */
        public void writeKey()
            {
            assert isEntry();
            assert getOffset() != NIL;
            assert m_binKey != null;

            getBuffer().position(getOffset() + OFFSET_HASH);
            DataOutputStream stream = getBufferOutput();

            try
                {
                stream.writeInt(m_nHash);
                m_binKey.writeExternal(stream);
                }
            catch (IOException e)
                {
                throw Base.ensureRuntimeException(e);
                }

            m_fKeyMod = false;
            }

        /**
        * Read the "value" portion of an Entry block.
        *
        * Note that if the length of the key is modified, the value must be read
        * first or it will be unreadable.
        */
        public void readValue()
            {
            assert isEntry();
            assert getOffset() != NIL;

            getBuffer().position(getOffset() + OFFSET_VALUE + getKeyLength());

            try
                {
                m_binValue = new Binary(getBufferInput());
                }
            catch (IOException e)
                {
                throw Base.ensureRuntimeException(e);
                }

            assert getValueLength() == m_binValue.length();
            }

        /**
        * Write the "value" portion of an Entry block.
        */
        public void writeValue()
            {
            assert isEntry();
            assert getOffset() != NIL;
            assert m_binValue != null;

            getBuffer().position(getOffset() + OFFSET_VALUE + getKeyLength());

            try
                {
                m_binValue.writeExternal(getBufferOutput());
                }
            catch (IOException e)
                {
                throw Base.ensureRuntimeException(e);
                }

            m_fValueMod = false;
            }


        // ----- block management -------------------------------------------

        /**
        * Link this block (either an Entry or free block) into the
        * appropriate data structures (either the hash bucket or the free
        * list.)
        */
        public void link()
            {
            switch (m_nType)
                {
                case NONE:
                    throw new AssertionError(
                        "Illegal unlink of type NONE at offset " + m_ofThisBlock);

                case FREE:
                case ENTRY:
                    {
                    assert getNextNodeOffset() == NIL : "Attempt to link node at offset " + getOffset()
                            + " which a non-NIL next node offset " + getNextNodeOffset();
                    assert getPrevNodeOffset() == NIL : "Attempt to link node at offset " + getOffset()
                            + " which a non-NIL prev node offset " + getPrevNodeOffset();

                    int ofThis = getOffset();
                    int nWhich;
                    int ofNext;

                    boolean fFree = isFree();
                    if (fFree)
                        {
                        nWhich = getSizeCode();
                        ofNext = getFreeBlockOffset(nWhich);
                        }
                    else
                        {
                        nWhich = calculateBucket(getKeyHash());
                        ofNext = getBucketOffset(nWhich);
                        }

                    if (ofNext != NIL)
                        {
                        Block block = openBlock(ofNext);
                        block.setPrevNodeOffset(ofThis);
                        block.close();
                        }

                    setNextNodeOffset(ofNext);
                    setPrevNodeOffset(NIL);

                    if (fFree)
                        {
                        setFreeBlockOffset(nWhich, ofThis);
                        }
                    else
                        {
                        setBucketOffset(nWhich, ofThis);
                        }
                    }
                    break;

                default:
                    throw new AssertionError("Illegal block type ("
                        + m_nType + ") unlink at offset " + m_ofThisBlock);
                }
            }

        /**
        * Unlink this block (either an Entry or free block) from the
        * appropriate data structures (either the hash bucket or the free
        * list.)
        */
        public void unlink()
            {
            switch (m_nType)
                {
                case NONE:
                    throw new AssertionError(
                        "Illegal unlink of type NONE at offset " + m_ofThisBlock);

                case FREE:
                case ENTRY:
                    {
                    int ofNext = getNextNodeOffset();
                    int ofPrev = getPrevNodeOffset();

                    if (ofNext != NIL)
                        {
                        Block block = openBlock(ofNext);
                        block.setPrevNodeOffset(ofPrev);
                        block.close();
                        }

                    if (ofPrev == NIL)
                        {
                        if (isFree())
                            {
                            // this is the head of the free list
                            int nCode = getSizeCode();

                            assert getFreeBlockOffset(nCode) == getOffset() :
                                "First free block for size code " + nCode + " is at offset "
                                + getFreeBlockOffset(nCode) + " (0x"
                                + Integer.toString(getFreeBlockOffset(nCode), 16)
                                + ") but the block at offset " + getOffset() + " (0x"
                                + Integer.toString(getOffset(), 16) + ") has size code "
                                + getSizeCode() + " and a previous offset of NIL";

                            setFreeBlockOffset(nCode, ofNext);
                            }
                        else
                            {
                            // this is the head of the free list
                            int nBucket = calculateBucket(getKeyHash());
                            if (getBucketOffset(nBucket) != getOffset())
                                {
                                nBucket = calculatePreviousBucket(getKeyHash());
                                }

                            assert getBucketOffset(nBucket) == getOffset() :
                                "First Entry block for bucket " + nBucket + " is at offset "
                                + getBucketOffset(nBucket) + " but the block at offset "
                                + getOffset() + " has bucket " + nBucket + " (hash="
                                + getKeyHash() + ") and a previous offset of NIL";

                            setBucketOffset(nBucket, ofNext);
                            }
                        }
                    else
                        {
                        Block block = openBlock(ofPrev);
                        block.setNextNodeOffset(ofNext);
                        block.close();
                        }

                    setNextNodeOffset(NIL);
                    setPrevNodeOffset(NIL);
                    }
                    break;

                default:
                    throw new AssertionError("Illegal block type ("
                        + m_nType + ") unlink at offset " + m_ofThisBlock);
                }
            }

        /**
        * If possible, chop a free block into two free blocks, or chop the
        * end of an Entry block to make a free block.
        *
        * @param cbRetain  the number of bytes to allocate to the first of
        *                  the two free blocks
        */
        public void split(int cbRetain)
            {
            assert isFree();

            // figure out if a split is possible
            int ofThis = getOffset();
            int cbThis = length();
            if (cbThis - cbRetain < MIN_SPLIT)
                {
                return;
                }

            int ofThat = ofThis + cbRetain;
            Block that = initBlock(ofThat);
            that.setType(FREE);

            this.unlink();
            // that is already unlinked (it's new)

            int ofNext = getNextBlockOffset();
            if (ofNext != NIL)
                {
                Block block = openBlock(ofNext);
                block.setPrevBlockOffset(ofThat);
                block.close();
                }

            this.setNextBlockOffset(ofThat);
            that.setPrevBlockOffset(ofThis);
            that.setNextBlockOffset(ofNext);

            this.link();
            that.link();

            that.close();
            }

        /**
        * Merge a free block with any free blocks that it borders.
        */
        public void merge()
            {
            assert isFree();

            int ofPrevOld = getPrevBlockOffset();
            int ofNextOld = getNextBlockOffset();
            int ofOld     = getOffset();

            int ofPrevNew = ofPrevOld;
            int ofNextNew = ofNextOld;
            int ofNew     = ofOld;

            unlink();

            if (ofPrevOld != NIL)
                {
                Block block = openBlock(ofPrevOld);
                if (block.isFree())
                    {
                    // take prev block's position
                    ofNew     = block.getOffset(); // i.e. ofPrevOld
                    ofPrevNew = block.getPrevBlockOffset();

                    block.unlink();
                    block.discard();
                    }
                else
                    {
                    block.close();
                    }
                }

            if (ofNextOld != NIL)
                {
                Block block = openBlock(ofNextOld);
                if (block.isFree())
                    {
                    ofNextNew = block.getNextBlockOffset();

                    block.unlink();
                    block.discard();
                    }
                else
                    {
                    block.close();
                    }
                }

            // if the block location has changed, or a following block got
            // merged, then update the next block to point to this block
            // as its previous
            if ((ofOld != ofNew || ofNextOld != ofNextNew) && ofNextNew != NIL)
                {
                Block block = openBlock(ofNextNew);
                assert !block.isFree();

                block.setPrevBlockOffset(ofNew);
                block.close();
                }

            // Note: we don't have to update the "newly" previous block since
            // it points to the old previous block that this block is merging
            // into
            setOffset(ofNew);
            setPrevBlockOffset(ofPrevNew);
            setNextBlockOffset(ofNextNew);

            link();
            }

        /**
        * Allocate this free block (or at least a specified number of bytes
        * of this block) as an Entry block. Note that the block is an Entry
        * in an unlinked state at the termination of this method.
        *
        * @param cb  the minimum number of bytes required for the Entry block
        */
        public void allocate(int cb)
            {
            assert isFree();

            split(cb);
            unlink();
            setType(ENTRY);

            assert isEntry();
            assert length() >= cb;
            }

        /**
        * Free this Entry block. Note that this has the effect of closing
        * the block.
        */
        public void free()
            {
            assert isEntry();

            unlink();
            setType(FREE);
            link();

            if (isStrict())
                {
                clear();
                }

            merge();
            close();
            }


        // ----- constants ----------------------------------------------

        /**
        * Initial state, also state of an allocated block that has not
        * been linked.
        */
        public static final int NONE  = 0;

        /**
        * State of a block that is available for use.
        */
        public static final int FREE  = 1;

        /**
        * State of a block that holds an Entry object's data.
        */
        public static final int ENTRY = 2;


        /**
        * Offset of the key's hash within an Entry block.
        */
        public static final int OFFSET_HASH  = 17;

        /**
        * Offset of the key data within an Entry block.
        */
        public static final int OFFSET_KEY   = 21;

        /**
        * Offset (not counting key length) of the value data within an Entry
        * block.
        */
        public static final int OFFSET_VALUE = 25;

        /**
        * Minimum size of a block to split off a free block.
        */
        public static final int MIN_SPLIT    = 64;

        /**
        * Minimum size of a free block.
        */
        public static final int MIN_FREE     = 17;

        /**
        * Minimum size of an Entry block.
        */
        public static final int MIN_ENTRY    = 29;


        // ----- data members -------------------------------------------

        /**
        * State of the block. One of {NONE, FREE, ENTRY}.
        */
        private int m_nType;

        /**
        * Offset of this block in the Buffer.
        */
        private int m_ofThisBlock;

        /**
        * Next block in the Buffer.
        */
        private int m_ofNextBlock;

        /**
        * Previous block in the Buffer.
        */
        private int m_ofPrevBlock;

        /**
        * Next node in the linked list.
        */
        private int m_ofNextNode;

        /**
        * Previous node in the linked list.
        */
        private int m_ofPrevNode;

        /**
        * Hash code of an Entry block.
        */
        private int m_nHash;

        /**
        * Key length of an Entry block.
        */
        private int m_cbKey;

        /**
        * Value length of an Entry block.
        */
        private int m_cbValue;

        /**
        * Key of an Entry block. Lazy loaded.
        */
        private Binary m_binKey;

        /**
        * Value of an Entry block. Lazy loaded.
        */
        private Binary m_binValue;

        /**
        * The use count for the block.
        */
        private int m_cUses;

        /**
        * True if the header has been modified.
        */
        private boolean m_fHeaderMod;

        /**
        * True if the Entry "key" has been modified.
        */
        private boolean m_fKeyMod;

        /**
        * True if the Entry "value" has been modified.
        */
        private boolean m_fValueMod;
        }


    // ----- constants ------------------------------------------------------

    /**
    * True to enable debug mode.
    */
    protected static final boolean MODE_DEBUG = false;

    /**
    * Byte used as a fill byte.
    */
    protected static final byte    FILL_BYTE = (byte) (MODE_DEBUG ? '_' : 0);

    /**
    * Byte array used for wiping the buffer.
    */
    protected static final byte[]  FILL_BUFFER = new byte[16384];
    static
        {
        for (int i = 0, c = FILL_BUFFER.length; i < c; ++i)
            {
            FILL_BUFFER[i] = FILL_BYTE;
            }
        }

    /**
    * Number of size codes for free blocks.
    */
    protected static final int MAX_SIZE_CODES = 26;

    /**
    * These are potential bucket counts.
    */
    protected static final int[] BUCKET_COUNTS = new int[]
        { 7, 47, 199, 797, 3191, 12799, 51199, 204797, 819187, 3276799, 13107197, 52428767, };

    /**
    * Copy buffer size.
    */
    protected static final int SIZE_COPY_BUFFER = 1024;

    /**
    * Offset reserved for the "does-not-exist" block.
    */
    protected static final int NIL = -1;

    /**
    * Maximum number of simultaneously open blocks to support.
    */
    protected static final int MAX_OPEN_BLOCKS = 8;

    /**
    * Default value for the percentage of the modulo that the entry count
    * must reach before going to the next bucket level.
    */
    public static final double DEFAULT_MAXLOADFACTOR    = 0.875;

    /**
    * Default value for the percentage of the next lower bucket level's
    * modulo that the entry count must drop to before reverting to the
    * next lower bucket level.
    */
    public static final double DEFAULT_MINLOADFACTOR  = 0.750;

    /**
    * Hex digits.
    */
    private static final char[] HEX = "0123456789ABCDEF".toCharArray();


    // ----- data members ---------------------------------------------------

    /**
    * True to enable strict mode (always keep unused part of the buffer
    * clean), otherwise false (faster).
    */
    private boolean m_fStrict;

    /**
    * Percentage of the modulo that the entry count must reach before
    * going to the next bucket level.
    */
    private double m_dflLoadPercentGrow = DEFAULT_MAXLOADFACTOR;

    /**
    * Percentage of the next lower bucket level's modulo that the entry count
    * must drop to before reverting to the next lower bucket level. This
    * value <b>must</b> be smaller than the threshold growth percentage.
    */
    private double m_dflLoadPercentShrink = DEFAULT_MINLOADFACTOR;

    /**
    * The optional ByteBufferManager.
    */
    private ByteBufferManager m_bufmgr;

    /**
    * The ByteBuffer that the BinaryMap is backed by.
    */
    private ByteBuffer m_buffer;

    /**
    * The DataInputStream that reads from the underlying ByteBuffer.
    */
    private DataInputStream m_streamIn;

    /**
    * The DataOutputStream that writes to the underlying ByteBuffer.
    */
    private DataOutputStream m_streamOut;

    /**
    * The level of buckets that this map  is at. This is an index into the
    * BUCKET_COUNTS field, which is an array of pre-selected modulos.
    */
    private int m_nBucketLevel;

    /**
    * Hash buckets. Linked lists of Entry blocks indexed by (hash % bucketcount).
    */
    private int[] m_aofBucket;

    /**
    * Current hash modulo.
    */
    private int m_nModuloCurrent;

    /**
    * Previous hash modulo. This will be the same as the current once
    * rehashing completes.
    */
    private int m_nModuloPrevious;

    /**
    * Next hash bucket to rehash (if current != previous modulo).
    */
    private int m_nBucketNextRehash;

    /**
    * Number of Entry objects in the Map.
    */
    private int m_cEntries;

    /**
    * When the number of entries goes above this value, the bucket level will
    * increase.
    */
    private int m_cEntriesGrow;

    /**
    * When the number of entries goes below this value, the bucket level will
    * decrease.
    */
    private int m_cEntriesShrink;

    /**
    * Total size, in bytes, of all the key data.
    */
    private int m_cbKeyTotal;

    /**
    * Total size, in bytes, of all the value data.
    */
    private int m_cbValueTotal;

    /**
    * Linked lists of free blocks indexed by size code.
    */
    private int[] m_aofFree;

    /**
    * Incremental compaction offset.
    */
    private int m_ofBlockNextCompact;

    /**
    * Last block in the buffer.
    */
    private int m_ofBlockLast;

    /**
    * Array of offsets for the open and recycled (NIL offset) Block objects.
    */
    private int[] m_aofBlockCache = new int[MAX_OPEN_BLOCKS];

    /**
    * Array of open and recycled Block objects.
    */
    private Block[] m_ablockCache = new Block[MAX_OPEN_BLOCKS];

    /**
    * The number of blocks that are currently open. These blocks will occupy
    * the indexes 0..n-1 of the m_ablockCache array.
    */
    private int m_cOpenBlocks;

    /**
    * The set of entries backed by this map.
    */
    protected transient EntrySet m_set;

    /**
    * The set of keys backed by this map.
    */
    protected transient KeySet m_setKeys;

    /**
    * The collection of values backed by this map.
    */
    protected transient ValuesCollection m_colValues;
    }
