/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


import com.oracle.coherence.common.base.Collector;
import com.oracle.coherence.common.base.Continuation;

import com.oracle.coherence.persistence.AsyncPersistenceException;
import com.oracle.coherence.persistence.OfflinePersistenceInfo;
import com.oracle.coherence.persistence.PersistenceEnvironment;
import com.oracle.coherence.persistence.PersistenceManager;
import com.oracle.coherence.persistence.PersistenceStatistics;
import com.oracle.coherence.persistence.PersistenceTools;
import com.oracle.coherence.persistence.PersistentStore;
import com.oracle.coherence.persistence.PersistentStoreInfo;

import com.tangosol.io.DeltaCompressor;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.OutputStreaming;
import com.tangosol.io.ReadBuffer;
import com.tangosol.io.WriteBuffer;

import com.tangosol.io.pof.PofContext;
import com.tangosol.io.pof.PofHandler;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofSerializer;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.io.pof.RawQuad;

import com.tangosol.net.Action;
import com.tangosol.net.ActionPolicy;
import com.tangosol.net.AddressProvider;
import com.tangosol.net.BackingMapContext;
import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.net.BackingMapManager;
import com.tangosol.net.CacheService;
import com.tangosol.net.Member;

import com.tangosol.net.MemberIdentityProvider;
import com.tangosol.net.cache.CacheStore;

import com.tangosol.net.partition.DistributionManager;
import com.tangosol.net.partition.PartitionAssignmentStrategy;
import com.tangosol.net.partition.PartitionSet;

import com.tangosol.run.xml.XmlElement;

import com.tangosol.util.processor.AbstractProcessor;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Serializable;
import java.io.Writer;

import java.math.BigDecimal;
import java.math.BigInteger;

import java.net.InetSocketAddress;

import java.nio.CharBuffer;

import java.security.AccessController;
import java.security.PrivilegedAction;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;


/**
* A collection of classes that do nothing.  For each class implemented, a
* factory method will exist following the naming convention "get" plus the
* class or interface name.
*
* @author cp  2000.08.02
*/
public class NullImplementation
        extends ClassLoader
    {
    /**
    * Default constructor.
    */
    private NullImplementation()
        {
        super(NullImplementation.class.getClassLoader());
        }


    // ----- factory methods --------------------------------------------

    /**
    * Returns an instance of the null enumerator.
    *
    * @return an Enumeration instance with no values to enumerate.
    */
    public static Enumeration getEnumeration()
        {
        return NullEnumerator.INSTANCE;
        }

    /**
    * Returns an instance of the null iterator.
    *
    * @return an Iterator instance with no values to enumerate.
    */
    public static <T> Iterator<T> getIterator()
        {
        return NullEnumerator.INSTANCE;
        }

    /**
     * Returns an instance of the null iterable.
     *
     * @return an Iterable that will return a {@link #getIterator()
     *         null iterator}
     */
     public static <T> Iterable<T> getIterable()
        {
        return NullEnumerator.INSTANCE;
        }

    /**
    * Returns an instance of the NullSet.
    *
    * @return an empty immutable Set
    */
    public static <T> Set<T> getSet()
        {
        return NullSet.INSTANCE;
        }

    /**
    * Returns an instance of the NullMap.
    *
    * @return an empty Map that does nothing
    */
    public static <K, V> Map<K, V> getMap()
        {
        return NullMap.INSTANCE;
        }

    /**
    * Returns an instance of the NullObservableMap.
    *
    * @return an empty ObservableMap and does nothing
    */
    public static ObservableMap getObservableMap()
        {
        return NullObservableMap.INSTANCE;
        }

    /**
    * Factory method:  Obtain a null implementation of a Reader.
    *
    * @return a conforming implementation of Reader that does as little as
    *         possible
    */
    public static Reader getReader()
        {
        return new NullReader();
        }

    /**
    * Factory method:  Obtain a null implementation of a Writer.
    *
    * @return a conforming implementation of Writer that does as little as
    *         possible
    */
    public static Writer getWriter()
        {
        return new NullWriter();
        }

    /**
    * Factory method:  Obtain a null implementation of a OutputStream.
    *
    * @return a conforming implementation of OutputStream that does as little
    *         as possible
    */
    public static OutputStream getOutputStream()
        {
        return new NullOutputStream();
        }

    /**
    * Factory method:  Obtain a null implementation of a DataOutput.
    *
    * @return a conforming implementation of DataOutput that does as little
    *         as possible
    */
    public static DataOutput getDataOutput()
        {
        return new NullOutputStream();
        }

    /**
    * Factory method:  Obtain a null implementation of a Converter.
    *
    * @return a conforming implementation of Converter that does as little
    *         as possible
    */
    public static <T, R> Converter<T, R> getConverter()
        {
        return NullConverter.INSTANCE;
        }

    /**
    * Factory method: Obtain a null implementation of a ValueExtractor.
    *
    * @return a ValueExtractor that does not actually extract anything from
    *         the passed value
    */
    public static <T, E> ValueExtractor<T, E> getValueExtractor()
        {
        return NullValueExtractor.INSTANCE;
        }

    /**
    * Factory method: Obtain a null implementation of a ClassLoader.
    *
    * @return a conforming implementation of ClassLoader that does as little
    *         as possible
    */
    public static ClassLoader getClassLoader()
        {
        return LOADER;
        }

    /**
    * Factory method: Obtain a null implementation of a PofContext.
    *
    * @return a conforming implementation of PofContext that does as little
    *         as possible
    */
    public static PofContext getPofContext()
        {
        return NullPofContext.INSTANCE;
        }

    /**
    * Factory method: Obtain a null implementation of a PofHandler.
    *
    * @return a conforming implementation of PofHandler that does as little
    *         as possible
    */
    public static PofHandler getPofHandler()
        {
        return NullPofHandler.INSTANCE;
        }

    /**
    * Factory method: Obtain a null implementation of a BackingMapManagerContext.
    *
    * @return a conforming implementation of BackingMapManagerContext that does
    *         as little as possible
    */
    public static BackingMapManagerContext getBackingMapManagerContext()
        {
        return NullBackingMapManagerContext.INSTANCE;
        }

    /**
    * Factory method: Obtain a null implementation of an EntryProcessor.
    *
    * @return an EntryProcessor implementation that returns Boolean.FALSE
    */
    public static InvocableMap.EntryProcessor getEntryProcessor()
        {
        return NullEntryProcessor.INSTANCE;
        }

    /**
    * Factory method: Obtain a null implementation of a DeltaCompressor.
    *
    * @return a DeltaCompressor implementation that always returns new buffer
    */
    public static DeltaCompressor getDeltaCompressor()
        {
        return NullDeltaCompressor.INSTANCE;
        }

    /**
    * Factory method: Obtain a null implementation of an ActionPolicy.
    *
    * @return an ActionPolicy implementation that allows all actions
    */
    public static ActionPolicy getActionPolicy()
        {
        return NullActionPolicy.INSTANCE;
        }

    /**
    * Factory method: Obtain a null implementation of a CacheStore.
    *
    * @return a CacheStore implementation that does as little as possible
    */
    public static CacheStore getCacheStore()
        {
        return NullCacheStore.INSTANCE;
        }

    /**
    * Factory method: Obtain a null implementation of a PartitionAssignmentStrategy.
    *
    * @return a PartitionAssignmentStrategy implementation that does as little as possible
    */
    public static PartitionAssignmentStrategy getPartitionAssignmentStrategy()
        {
        return NullPartitionAssignmentStrategy.INSTANCE;
        }

    /**
    * Factory method: Obtain a null implementation of an {@link AddressProvider}.
    *
    * @return an implementation of AddressProvider that does as little as possible
    */
    public static AddressProvider getAddressProvider()
        {
        return NullAddressProvider.INSTANCE;
        }

    /**
    * Factory method: Obtain a null implementation of a {@link Collector}.
    *
    * @return an implementation of Collector that does as little as possible
    */
    public static <V> Collector<V> getCollector()
        {
        return (Collector<V>) NullCollector.INSTANCE;
        }

    /**
    * Factory method: Obtain a null implementation of a {@link Continuation}.
    *
    * @return an implementation of Continuation that does nothing
    */
    public static <R> Continuation<R> getContinuation()
        {
        return (Continuation<R>) NullContinuation.INSTANCE;
        }

    /**
    * Factory method: Obtain a null implementation of a {@link PersistenceEnvironment}.
    *
    * @return an implementation of PersistenceEnvironment that does as little as possible
    */
    public static <R> PersistenceEnvironment<R> getPersistenceEnvironment()
        {
        return (PersistenceEnvironment<R>) NullPersistenceEnvironment.INSTANCE;
        }

    /**
    * Factory method: Obtain a null implementation of a {@link PersistenceEnvironment}.
    *
    * @param clz the type of a raw, environment specific object representation
    *
    * @return an implementation of PersistenceEnvironment that does as little as possible
    */
    public static <R> PersistenceEnvironment<R> getPersistenceEnvironment(Class<R> clz)
        {
        return (PersistenceEnvironment<R>) NullPersistenceEnvironment.INSTANCE;
        }

    /**
    * Factory method: Obtain a null implementation of a {@link PersistenceManager}.
    *
    * @return an implementation of PersistenceManager that does as little as possible
    */
    public static <R> PersistenceManager<R> getPersistenceManager()
        {
        return (PersistenceManager<R>) NullPersistenceManager.INSTANCE;
        }

    /**
    * Factory method: Obtain a null implementation of a {@link PersistenceManager}.
    *
    * @param clz  the type of a raw, environment specific object representation
    *
    * @return an implementation of PersistenceManager that does as little as possible
    */
    public static <R> PersistenceManager<R> getPersistenceManager(Class<R> clz)
        {
        return (PersistenceManager<R>) NullPersistenceManager.INSTANCE;
        }

    /**
    * Factory method: Obtain a null implementation of a {@link PersistentStore}.
    *
    * @return an implementation of PersistentStore that does as little as possible
    */
    public static <R> PersistentStore<R> getPersistentStore()
        {
        return (PersistentStore<R>) NullPersistentStore.INSTANCE;
        }

    /**
    * Factory method: Obtain a null implementation of a {@link PersistentStore}.
    *
    * @param clz the type of a raw, environment specific object representation
    *
    * @return an implementation of PersistentStore that does as little as possible
    */
    public static <R> PersistentStore<R> getPersistentStore(Class<R> clz)
        {
        return (PersistentStore<R>) NullPersistentStore.INSTANCE;
        }

    /**
    * Factory method: Obtain a null implementation of a {@link ResourceRegistry}.
    *
    * @return an implementation of ResourceRegistry that does nothing
    */
    public static ResourceRegistry getResourceRegistry()
        {
        return NullResourceRegistry.INSTANCE;
        }

    /**
    * Factory method: Obtain a null implementation of an {@link AutoCloseable}.
    *
    * @return an implementation of an AutoCloseable that does nothing
    */
    public static AutoCloseable getAutoCloseable()
        {
        return () -> {};
        }

    /**
     * Returns an immutable empty {@link LongArray}.
     *
     * @param <V>  the type of values the {@link LongArray} would hold
     *
     * @return an immutable empty {@link LongArray}
     */
    @SuppressWarnings("unchecked")
    public static <V> LongArray<V> getLongArray()
        {
        return (LongArray<V>) NullLongArray.INSTANCE;
        }

    /**
     * Return an instance of a null implementation {@link MemberIdentityProvider}.
     *
     * @return an instance of a null implementation {@link MemberIdentityProvider}
     */
    public static MemberIdentityProvider getMemberIdentityProvider()
        {
        return NullMemberIdentityProvider.INSTANCE;
        }

    // ----- inner classes ----------------------------------------------

    /**
    * An empty enumerator.
    */
    public static class NullEnumerator
            implements Enumeration, Iterator, Iterable
        {
        // ----- constructors -------------------------------------------

        /**
        * No public constructor.  (The whole point of this class is to minimize
        * allocations in cases where there is nothing to enumerate.)
        */
        NullEnumerator()
            {
            }

        // ----- Enumerator interface -----------------------------------

        /**
        * Tests if this enumeration contains more elements.
        *
        * @return false
        */
        public boolean hasMoreElements()
            {
            return false;
            }

        /**
        * Returns the next element of this enumeration if this enumeration
        * object has at least one more element to provide.
        *
        * @return     the next element of this enumeration.
        * @exception  NoSuchElementException  always
        */
        public Object nextElement()
            {
            throw new NoSuchElementException();
            }


        // ----- Iterator interface -------------------------------------

        /**
        * Returns true if the iteration has more elements.
        */
        public boolean hasNext()
            {
            return false;
            }

        /**
        * Returns the next element in the interation.
        *
        * @exception NoSuchElementException iteration has no more elements.
        */
        public Object next()
            {
            throw new NoSuchElementException();
            }

        /**
        * Removes from the underlying Collection the last element returned by the
        * Iterator .  This method can be called only once per call to next  The
        * behavior of an Iterator is unspecified if the underlying Collection is
        * modified while the iteration is in progress in any way other than by
        * calling this method.  Optional operation.
        *
        * @exception IllegalStateException next has not yet been called,
        *            or remove has already been called after the last call
        *            to next.
        */
        public void remove()
            {
            throw new IllegalStateException();
            }

        // ---- Iterable interface ----------------------------------------

        /**
         * Returns an Iterator instance with no values to enumerate.
         *
         * @return an Iterator instance with no values to enumerate
         */
         public Iterator iterator()
             {
             return this;
             }

        // ----- constants ----------------------------------------------

        /**
        * Since the enumerator contains no information, only one ever has to exist.
        */
        public static final NullEnumerator INSTANCE = new NullEnumerator();
        }


    /**
    * An immutable set which contains nothing.
    */
    public static class NullSet
            extends AbstractSet
            implements Serializable, ExternalizableLite, PortableObject
        {
        // ----- constructors -------------------------------------------

        /**
        * Default constructor (for ExternalizableLite and PortableObject).
        */
        public NullSet()
            {
            }

        // ----- Set interface ------------------------------------------

        /**
        * Returns an array containing all of the elements in this Set.
        * Obeys the general contract of Collection.toArray.
        *
        * @return an Object array containing all of the elements in this Set
        */
        public Object[] toArray()
            {
            return EMPTY_ARRAY;
            }

        /**
        * Returns an Iterator over the elements contained in this Collection.
        *
        * @return an Iterator over the elements contained in this Collection
        */
        public Iterator iterator()
            {
            return EMPTY_ITERATOR;
            }

        /**
        * Returns the number of elements in this Collection.
        *
        * @return the number of elements in this Collection
        */
        public int size()
            {
            return 0;
            }

        /**
        * Returns true if this Collection contains the specified element.  More
        * formally, returns true if and only if this Collection contains at least
        * one element <code>e</code> such that <code>(o==null ? e==null :
        * o.equals(e))</code>.
        *
        * @param o  the object to search for in the set
        *
        * @return true if this set contains the specified object
        */
        public boolean contains(Object o)
            {
            return false;
            }

        /**
        * Ensures that this Collection contains the specified element.
        *
        * @param o element whose presence in this Collection is to be ensured
        *
        * @return true if the Collection changed as a result of the call
        */
        public boolean add(Object o)
            {
            return false;
            }

        /**
        * Removes a single instance of the specified element from this Collection,
        * if it is present (optional operation).  More formally, removes an
        * element <code>e</code> such that <code>(o==null ? e==null :
        * o.equals(e))</code>, if the Collection contains one or more such
        * elements.  Returns true if the Collection contained the specified
        * element (or equivalently, if the Collection changed as a result of the
        * call).
        *
        * @param o element to be removed from this Collection, if present
        *
        * @return true if the Collection contained the specified element
        */
        public boolean remove(Object o)
            {
            return false;
            }

        /**
        * Removes all of the elements from this Collection.
        */
        public void clear()
            {
            }

        // ----- Object methods -----------------------------------------

        /**
        * {@inheritDoc}
        */
        public boolean equals(Object o)
            {
            return o instanceof Set && ((Set) o).isEmpty();
            }

        /**
        * {@inheritDoc}
        */
        public int hashCode()
            {
            return 0;
            }

        // ----- ExternalizableLite interface ---------------------------

        /**
        * {@inheritDoc}
        */
        public void readExternal(DataInput in)
                throws IOException
            {
            }

        /**
        * {@inheritDoc}
        */
        public void writeExternal(DataOutput out)
                throws IOException
            {
            }

        // ----- PortableObject interface -------------------------------

        /**
        * {@inheritDoc}
        */
        public void readExternal(PofReader in)
                throws IOException
            {
            }

        /**
        * {@inheritDoc}
        */
        public void writeExternal(PofWriter out)
                throws IOException
            {
            }

        // ----- constants ----------------------------------------------

        /**
        * Since the set contains no information, only one ever has to exist.
        */
        public static final Set INSTANCE = new NullSet();

        /**
        * Since the set contains no information, only one array has to exist.
        */
        private static final Object[] EMPTY_ARRAY    = new Object[0];

        /**
        * Since the set contains no information, only one iterator has to
        * exist.
        */
        private static final Iterator EMPTY_ITERATOR = getIterator();
        }


    /**
    * A Map that contains nothing and does nothing.
    */
    public static class NullMap
            extends AbstractMap
            implements Map, Serializable, ExternalizableLite, PortableObject
        {
        // ----- constructors -------------------------------------------

        /**
        * Default constructor (for ExternalizableLite and PortableObject).
        */
        public NullMap()
            {
            }

        // ----- Map interface ------------------------------------------

        /**
        * {@inheritDoc}
        */
        public int size()
            {
            return 0;
            }

        /**
        * {@inheritDoc}
        */
        public Object get(Object key)
            {
            return null;
            }

        /**
        * {@inheritDoc}
        */
        public Object put(Object key, Object value)
            {
            return null;
            }

        /**
        * {@inheritDoc}
        */
        public void putAll(Map map)
            {
            }

        /**
        * {@inheritDoc}
        */
        public Object remove(Object key)
            {
            return null;
            }

        /**
        * {@inheritDoc}
        */
        public boolean containsKey(Object key)
            {
            return false;
            }

        /**
        * {@inheritDoc}
        */
        public boolean containsValue(Object value)
            {
            return false;
            }

        /**
        * {@inheritDoc}
        */
        public Set entrySet()
            {
            return getSet();
            }

        /**
        * {@inheritDoc}
        */
        public Set keySet()
            {
            return getSet();
            }

        /**
        * {@inheritDoc}
        */
        public Collection values()
            {
            return getSet();
            }

        // ----- Object methods -----------------------------------------

        /**
        * {@inheritDoc}
        */
        public boolean equals(Object o)
            {
            return o instanceof Map && ((Map) o).isEmpty();
            }

        /**
        * {@inheritDoc}
        */
        public int hashCode()
            {
            return 0;
            }

        // ----- ExternalizableLite interface ---------------------------

        /**
        * {@inheritDoc}
        */
        public void readExternal(DataInput in)
                throws IOException
            {
            }

        /**
        * {@inheritDoc}
        */
        public void writeExternal(DataOutput out)
                throws IOException
            {
            }

        // ----- PortableObject interface -------------------------------

        /**
        * {@inheritDoc}
        */
        public void readExternal(PofReader in)
                throws IOException
            {
            }

        /**
        * {@inheritDoc}
        */
        public void writeExternal(PofWriter out)
                throws IOException
            {
            }

        // ----- constants ----------------------------------------------

        /**
        * Since the Map contains no information, only one ever has to exist.
        */
        public static final Map INSTANCE = new NullMap();
        }


    /**
    * An immutable ObservableMap which contains nothing.
    */
    public static class NullObservableMap
            extends NullMap
            implements ObservableMap
        {
        // ----- constructors -------------------------------------------

        /**
        * Default constructor (for ExternalizableLite and PortableObject).
        */
        public NullObservableMap()
            {
            }

        // ----- ObservableMap interface --------------------------------

        /**
        * {@inheritDoc}
        */
        public void addMapListener(MapListener listener)
            {
            }

        /**
        * {@inheritDoc}
        */
        public void removeMapListener(MapListener listener)
            {
            }

        /**
        * {@inheritDoc}
        */
        public void addMapListener(MapListener listener, Object oKey, boolean fLite)
            {
            }

        /**
        * {@inheritDoc}
        */
        public void removeMapListener(MapListener listener, Object oKey)
            {
            }

        /**
        * {@inheritDoc}
        */
        public void addMapListener(MapListener listener, Filter filter, boolean fLite)
            {
            }

        /**
        * {@inheritDoc}
        */
        public void removeMapListener(MapListener listener, Filter filter)
            {
            }

        // ----- constants ----------------------------------------------

        /**
        * Since the Map contains no information, only one ever has to exist.
        */
        public static final ObservableMap INSTANCE = new NullObservableMap();
        }

    /**
    * A writer that does basically nothing.  Note that multiple instances are
    * required because the API dictates that the close method must cause
    * further invocations to all other methods to throw an IOException.
    *
    * @author cp  2000.08.02
    */
    public static class NullWriter
            extends Writer
        {
        // ----- constructors -------------------------------------------

        /**
        * Default constructor.
        */
        NullWriter()
            {
            }

        // ----- Writer methods -----------------------------------------

        /**
        * {@inheritDoc}
        */
        public void write(char[] cbuf) throws IOException
            {
            check();
            }

        /**
        * {@inheritDoc}
        */
        public void write(char[] cbuf, int off, int len) throws IOException
            {
            check();
            }

        /**
        * {@inheritDoc}
        */
        public void write(int c) throws IOException
            {
            check();
            }

        /**
        * {@inheritDoc}
        */
        public void write(String str) throws IOException
            {
            check();
            }

        /**
        * {@inheritDoc}
        */
        public void write(String str, int off, int len) throws IOException
            {
            check();
            }

        /**
        * {@inheritDoc}
        */
        public void flush() throws IOException
            {
            check();
            }

        /**
        * {@inheritDoc}
        */
        public void close() throws IOException
            {
            m_fClosed = true;
            }

        /**
        * {@inheritDoc}
        */
        private void check() throws IOException
            {
            if (m_fClosed)
                {
                // same as PrintWriter
                throw new IOException("Stream closed");
                }
            }

        // ----- data members -------------------------------------------

        private boolean m_fClosed;
        }

    /**
    * A reader that does basically nothing.  Note that multiple instances are
    * required because the API dictates that the close method must cause
    * further invocations to all other methods to throw an IOException.
    *
    * @author jh  2012.03.28
    */
    public static class NullReader
            extends Reader
        {
        // ----- constructors -------------------------------------------

        /**
        * Default constructor.
        */
        NullReader()
            {
            }

        // ----- Reader methods -----------------------------------------

        /**
        * {@inheritDoc}
        */
        public void mark(int cb) throws IOException
            {
            check();
            super.mark(cb);
            }

        /**
        * {@inheritDoc}
        */
        public boolean markSupported()
            {
            return true;
            }

        /**
        * {@inheritDoc}
        */
        public int read() throws IOException
            {
            check();
            return -1;
            }

        /**
        * {@inheritDoc}
        */
        public int read(char[] ach) throws IOException
            {
            check();
            return -1;
            }

        /**
        * {@inheritDoc}
        */
        public int read(char[] cbuf, int off, int len) throws IOException
            {
            check();
            return -1;
            }

        /**
        * {@inheritDoc}
        */
        public int read(CharBuffer buf) throws IOException
            {
            check();
            return -1;
            }

        /**
        * {@inheritDoc}
        */
        public boolean ready() throws IOException
            {
            check();
            return true;
            }

        /**
        * {@inheritDoc}
        */
        public void reset() throws IOException
            {
            check();
            }

        /**
        * {@inheritDoc}
        */
        public long skip(long ch) throws IOException
            {
            check();
            throw new EOFException();
            }

        /**
        * {@inheritDoc}
        */
        public void close() throws IOException
            {
            m_fClosed = true;
            }

        // ----- internal -----------------------------------------------

        private void check() throws IOException
            {
            if (m_fClosed)
                {
                throw new IOException("Stream closed");
                }
            }

        // ----- data members -------------------------------------------

        private boolean m_fClosed;
        }

    /**
    * An OutputStream that does basically nothing.  Note that multiple
    * instances are required because the API dictates that the close method
    * must cause further invocations to all other methods to throw an
    * IOException.
    *
    * @author cp  2000.11.01
    */
    public static class NullOutputStream
            extends OutputStream
            implements OutputStreaming, DataOutput
        {
        // ----- constructors -------------------------------------------

        /**
        * Default constructor.
        */
        NullOutputStream()
            {
            }

        // ----- OutputStream methods -----------------------------------

        /**
        * {@inheritDoc}
        */
        public void write(int b) throws IOException
            {
            check();
            advance(1);
            }

        /**
        * {@inheritDoc}
        */
        public void write(byte b[]) throws IOException
            {
            check();
            advance(b.length);
            }

        /**
        * {@inheritDoc}
        */
        public void write(byte b[], int off, int len) throws IOException
            {
            check();
            advance(len);
            }

        /**
        * {@inheritDoc}
        */
        public void flush() throws IOException
            {
            check();
            }

        /**
        * {@inheritDoc}
        */
        public void close() throws IOException
            {
            m_fClosed = true;
            }

        /**
        * {@inheritDoc}
        */
        public int size()
            {
            return m_cb;
            }

        // ------ DataOutput --------------------------------------------

        /**
        * {@inheritDoc}
        */
        public void writeDouble(double v)
            throws IOException
            {
            check();
            advance(8);
            }

        /**
        * {@inheritDoc}
        */
        public void writeFloat(float v)
            throws IOException
            {
            check();
            advance(4);
            }

        /**
        * {@inheritDoc}
        */
        public void writeByte(int v)
            throws IOException
            {
            check();
            advance(1);
            }

        /**
        * {@inheritDoc}
        */
        public void writeChar(int v)
            throws IOException
            {
            check();
            advance(2);
            }

        /**
        * {@inheritDoc}
        */
        public void writeInt(int v)
            throws IOException
            {
            check();
            advance(4);
            }

        /**
        * {@inheritDoc}
        */
        public void writeShort(int v)
            throws IOException
            {
            check();
            advance(2);
            }

        /**
        * {@inheritDoc}
        */
        public void writeLong(long v)
            throws IOException
            {
            check();
            advance(8);
            }

        /**
        * {@inheritDoc}
        */
        public void writeBoolean(boolean v)
            throws IOException
            {
            check();
            advance(1);
            }

        /**
        * {@inheritDoc}
        */
        public void writeBytes(String s)
            throws IOException
            {
            check();
            advance(s.getBytes().length);
            }

        /**
        * {@inheritDoc}
        */
        public void writeChars(String s)
            throws IOException
            {
            check();
            advance(s.length() * 2);
            }

        /**
        * {@inheritDoc}
        */
        public void writeUTF(String s)
            throws IOException
            {
            writeChars(s); // who cares
            }

        // ----- internal -----------------------------------------------

        private void check() throws IOException
            {
            if (m_fClosed)
                {
                // same as PrintStream
                throw new IOException("Stream closed");
                }
            }

        private void advance(int cb)
            {
            cb  += m_cb;
            m_cb = cb < 0 ? Integer.MAX_VALUE : cb;
            }

        // ----- data members -------------------------------------------

        private boolean m_fClosed;
        private int     m_cb;
        }


    /**
    * A Converter that does nothing.
    *
    * @author cp  2002.02.08
    */
    public static class NullConverter
            implements Converter
        {
        // ----- constructors -------------------------------------------

        /**
        * Off-limits constructor.
        */
        NullConverter()
            {
            }

        // ----- Converter interface ------------------------------------

        /**
        * Convert the passed object to another object.
        *
        * @return the new, converted object
        */
        public Object convert(Object o)
            {
            return o;
            }

        // ----- constants ----------------------------------------------

        /**
        * Since the Converter contains no information, only one ever has to
        * exist.
        */
        public static final NullConverter INSTANCE = new NullConverter();
        }


    /**
    * A ValueExtractor that always results in the passed-in value.
    */
    public static class NullValueExtractor
            implements ValueExtractor, Serializable, ExternalizableLite, PortableObject
        {
        // ----- constructors -------------------------------------------

        /**
        * Default constructor (for ExternalizableLite and PortableObject).
        */
        public NullValueExtractor()
            {
            }

        // ----- ValueExtractor interface -------------------------------

        /**
        * Extract the value from the passed object. The returned value may be
        * null. For intrinsic types, the returned value is expected to be a
        * standard wrapper type in the same manner that reflection works; for
        * example, <tt>int</tt> would be returned as a <tt>java.lang.Integer</tt>.
        *
        * @return the extracted value as an Object; null is an acceptable value
        *
        * @throws ClassCastException if this ValueExtractor is incompatible with
        *         the passed object to extract a value from and the
        *         implementation <b>requires</b> the passed object to be of a
        *         certain type
        * @throws WrapperException if this ValueExtractor encounters an exception
        *         in the course of extracting the value
        * @throws IllegalArgumentException if this ValueExtractor cannot handle
        *         the passed object for any other reason; an implementor should
        *         include a descriptive message
        */
        public Object extract(Object o)
            {
            return o;
            }

        // ----- Object methods -----------------------------------------

        /**
        * Compare the ValueExtractor with another object to determine equality.
        * Two ValueExtractor objects, <i>ve1</i> and <i>ve2</i> are considered
        * equal iff <tt>ve1.extract(o)</tt> equals <tt>ve2.extract(o)</tt> for
        * all values of <tt>o</tt>.
        *
        * @return true iff this ValueExtractor and the passed object are
        *         equivalent ValueExtractors
        */
        public boolean equals(Object o)
            {
            return o instanceof NullValueExtractor;
            }

        /**
        * Determine a hash value for the ValueExtractor object according to the
        * general {@link Object#hashCode()} contract.
        *
        * @return an integer hash value for this ValueExtractor object
        */
        public int hashCode()
            {
            // regards to DA
            return 42;
            }

        /**
        * Provide a human-readable description of this ValueExtractor object.
        *
        * @return a human-readable description of this ValueExtractor object
        */
        public String toString()
            {
            return "NullValueExtractor";
            }

        // ----- ExternalizableLite interface ---------------------------

        /**
        * {@inheritDoc}
        */
        public void readExternal(DataInput in)
                throws IOException
            {
            }

        /**
        * {@inheritDoc}
        */
        public void writeExternal(DataOutput out)
                throws IOException
            {
            }

        // ----- PortableObject interface -------------------------------

        /**
        * {@inheritDoc}
        */
        public void readExternal(PofReader in)
                throws IOException
            {
            }

        /**
        * {@inheritDoc}
        */
        public void writeExternal(PofWriter out)
                throws IOException
            {
            }

        // ----- constants ----------------------------------------------

        /**
        * Since the ValueExtractor contains no information, only one ever has to
        * exist.
        */
        public static final NullValueExtractor INSTANCE = new NullValueExtractor();
        }


    /**
    * An implementation of PofContext that does nothing.
    */
    public static class NullPofContext
            implements PofContext
        {
        // ----- constructors -------------------------------------------

        /**
        * Default constructor.
        */
        NullPofContext()
            {
            }

        // ----- PofContext interface -----------------------------------

        /**
        * {@inheritDoc}
        */
        public PofSerializer getPofSerializer(int nTypeId)
            {
            throw new IllegalArgumentException();
            }

        /**
        * {@inheritDoc}
        */
        public int getUserTypeIdentifier(Object o)
            {
            throw new IllegalArgumentException();
            }

        /**
        * {@inheritDoc}
        */
        public int getUserTypeIdentifier(Class clz)
            {
            throw new IllegalArgumentException();
            }

        /**
        * {@inheritDoc}
        */
        public int getUserTypeIdentifier(String sClass)
            {
            throw new IllegalArgumentException();
            }

        /**
        * {@inheritDoc}
        */
        public String getClassName(int nTypeId)
            {
            throw new IllegalArgumentException();
            }

        /**
        * {@inheritDoc}
        */
        public Class getClass(int nTypeId)
            {
            throw new IllegalArgumentException();
            }

        /**
        * {@inheritDoc}
        */
        public boolean isUserType(Object o)
            {
            return false;
            }

        /**
        * {@inheritDoc}
        */
        public boolean isUserType(Class clz)
            {
            return false;
            }

        /**
        * {@inheritDoc}
        */
        public boolean isUserType(String sClass)
            {
            return false;
            }

        // ----- Serializer interface -----------------------------------

        /**
        * {@inheritDoc}
        */
        public void serialize(WriteBuffer.BufferOutput out, Object o)
                throws IOException
            {
            throw new UnsupportedOperationException();
            }

        /**
        * {@inheritDoc}
        */
        public Object deserialize(ReadBuffer.BufferInput in)
                throws IOException
            {
            throw new UnsupportedOperationException();
            }

        // ----- constants ----------------------------------------------

        /**
        * Singleton instance.
        */
        public static final NullPofContext INSTANCE = new NullPofContext();
        }


    /**
    * An implementation of PofHandler that does nothing.
    */
    public static class NullPofHandler
            implements PofHandler
        {
        // ----- constructors -------------------------------------------

        /**
        * Default constructor.
        */
        NullPofHandler()
            {
            }

        // ----- PofHandler interface -----------------------------------

        /**
        * {@inheritDoc}
        */
        public void registerIdentity(int nId)
            {
            }

        /**
        * {@inheritDoc}
        */
        public void onNullReference(int iPos)
            {
            }

        /**
        * {@inheritDoc}
        */
        public void onIdentityReference(int iPos, int nId)
            {
            }

        /**
        * {@inheritDoc}
        */
        public void onInt16(int iPos, short n)
            {
            }

        /**
        * {@inheritDoc}
        */
        public void onInt32(int iPos, int n)
            {
            }

        /**
        * {@inheritDoc}
        */
        public void onInt64(int iPos, long n)
            {
            }

        /**
        * {@inheritDoc}
        */
        public void onInt128(int iPos, BigInteger n)
            {
            }

        /**
        * {@inheritDoc}
        */
        public void onFloat32(int iPos, float fl)
            {
            }

        /**
        * {@inheritDoc}
        */
        public void onFloat64(int iPos, double dfl)
            {
            }

        /**
        * {@inheritDoc}
        */
        public void onFloat128(int iPos, RawQuad qfl)
            {
            }

        /**
        * {@inheritDoc}
        */
        public void onDecimal32(int iPos, BigDecimal dec)
            {
            }

        /**
        * {@inheritDoc}
        */
        public void onDecimal64(int iPos, BigDecimal dec)
            {
            }

        /**
        * {@inheritDoc}
        */
        public void onDecimal128(int iPos, BigDecimal dec)
            {
            }

        /**
        * {@inheritDoc}
        */
        public void onBoolean(int iPos, boolean f)
            {
            }

        /**
        * {@inheritDoc}
        */
        public void onOctet(int iPos, int b)
            {
            }

        /**
        * {@inheritDoc}
        */
        public void onOctetString(int iPos, Binary bin)
            {
            }

        /**
        * {@inheritDoc}
        */
        public void onChar(int iPos, char ch)
            {
            }

        /**
        * {@inheritDoc}
        */
        public void onCharString(int iPos, String s)
            {
            }

        /**
        * {@inheritDoc}
        */
        public void onDate(int iPos, int nYear, int nMonth, int nDay)
            {
            }

        /**
        * {@inheritDoc}
        */
        public void onYearMonthInterval(int iPos, int cYears, int cMonths)
            {
            }

        /**
        * {@inheritDoc}
        */
        public void onTime(int iPos, int nHour, int nMinute, int nSecond,
                int nNano, boolean fUTC)
            {
            }

        /**
        * {@inheritDoc}
        */
        public void onTime(int iPos, int nHour, int nMinute, int nSecond,
                int nNano, int nHourOffset, int nMinuteOffset)
            {
            }

        /**
        * {@inheritDoc}
        */
        public void onTimeInterval(int iPos, int cHours, int cMinutes,
                int cSeconds, int cNanos)
            {
            }

        /**
        * {@inheritDoc}
        */
        public void onDateTime(int iPos, int nYear, int nMonth, int nDay,
                int nHour, int nMinute, int nSecond, int nNano, boolean fUTC)
            {
            }

        /**
        * {@inheritDoc}
        */
        public void onDateTime(int iPos, int nYear, int nMonth, int nDay,
                int nHour, int nMinute, int nSecond, int nNano,
                int nHourOffset, int nMinuteOffset)
            {
            }

        /**
        * {@inheritDoc}
        */
        public void onDayTimeInterval(int iPos, int cDays, int cHours,
                int cMinutes, int cSeconds, int cNanos)
            {
            }

        /**
        * {@inheritDoc}
        */
        public void beginCollection(int iPos, int cElements)
            {
            }

        /**
        * {@inheritDoc}
        */
        public void beginUniformCollection(int iPos, int cElements, int nType)
            {
            }

        /**
        * {@inheritDoc}
        */
        public void beginArray(int iPos, int cElements)
            {
            }

        /**
        * {@inheritDoc}
        */
        public void beginUniformArray(int iPos, int cElements, int nType)
            {
            }

        /**
        * {@inheritDoc}
        */
        public void beginSparseArray(int iPos, int cElements)
            {
            }

        /**
        * {@inheritDoc}
        */
        public void beginUniformSparseArray(int iPos, int cElements, int nType)
            {
            }

        /**
        * {@inheritDoc}
        */
        public void beginMap(int iPos, int cElements)
            {
            }

        /**
        * {@inheritDoc}
        */
        public void beginUniformKeysMap(int iPos, int cElements, int nTypeKeys)
            {
            }

        /**
        * {@inheritDoc}
        */
        public void beginUniformMap(int iPos, int cElements,
                                    int nTypeKeys, int nTypeValues)
            {
            }

        /**
        * {@inheritDoc}
        */
        public void beginUserType(int iPos, int nUserTypeId, int nVersionId)
            {
            }

        /**
        * {@inheritDoc}
        */
        public void endComplexValue()
            {
            }

        // ----- constants ----------------------------------------------

        /**
        * Singleton instance.
        */
        public static final NullPofHandler INSTANCE = new NullPofHandler();
        }

    /**
    * An implementation of BackingMapManagerContext that does nothing.
    */
    public static class NullBackingMapManagerContext
            implements BackingMapManagerContext
        {
        // ----- constructors -------------------------------------------

        /**
        * Default constructor.
        */
        NullBackingMapManagerContext()
            {
            }

        // ----- BackingMapManagerContext interface ---------------------

        /**
        * {@inheritDoc}
        */
        public BackingMapManager getManager()
            {
            return null;
            }

        /**
        * {@inheritDoc}
        */
        public CacheService getCacheService()
            {
            return null;
            }

        /**
        * {@inheritDoc}
        */
        public ClassLoader getClassLoader()
            {
            return null;
            }

        /**
        * {@inheritDoc}
        */
        public void setClassLoader(ClassLoader loader)
            {
            }

        /**
        * {@inheritDoc}
        */
        public Converter getKeyToInternalConverter()
            {
            return NullConverter.INSTANCE;
            }

        /**
        * {@inheritDoc}
        */
        public Converter getKeyFromInternalConverter()
            {
            return NullConverter.INSTANCE;
            }

        /**
        * {@inheritDoc}
        */
        public Converter getValueToInternalConverter()
            {
            return NullConverter.INSTANCE;
            }

        /**
        * {@inheritDoc}
        */
        public Converter getValueFromInternalConverter()
            {
            return NullConverter.INSTANCE;
            }

        /**
        * {@inheritDoc}
        */
        public boolean isKeyOwned(Object oKey)
            {
            return true;
            }

        /**
        * {@inheritDoc}
        */
        public int getKeyPartition(Object oKey)
            {
            return 0;
            }

        /**
        * {@inheritDoc}
        */
        public Set getPartitionKeys(String sCacheName, int nPartition)
            {
            return null;
            }

        /**
        * {@inheritDoc}
        */
        public Map getBackingMap(String sCacheName)
            {
            return null;
            }

        /**
        * {@inheritDoc}
        */
        public Object addInternalValueDecoration(Object oValue, int nDecorId, Object oDecor)
            {
            return oValue;
            }

        /**
        * {@inheritDoc}
        */
        public Object removeInternalValueDecoration(Object oValue, int nDecorId)
            {
            return oValue;
            }

        /**
        * {@inheritDoc}
        */
        public boolean isInternalValueDecorated(Object oValue, int nDecorId)
            {
            return false;
            }

        /**
        * {@inheritDoc}
        */
        public Object getInternalValueDecoration(Object oValue, int nDecorId)
            {
            return null;
            }

        /**
        * {@inheritDoc}
        */
        public BackingMapContext getBackingMapContext(String sCacheName)
            {
            return null;
            }


        // ----- XmlConfigurable interface ------------------------------

        /**
        * {@inheritDoc}
        */
        public XmlElement getConfig()
            {
            return null;
            }

        /**
        * {@inheritDoc}
        */
        public void setConfig(XmlElement xml)
            {
            }

        // ----- constants ----------------------------------------------

        /**
        * Singleton instance.
        */
        public static final NullBackingMapManagerContext INSTANCE =
                new NullBackingMapManagerContext();
        }


    /**
    * An implementation of an EntryProcessor that does nothing and returns
    * Boolean.TRUE as a result of execution.
    */
    public static class NullEntryProcessor
            extends AbstractProcessor
            implements ExternalizableLite, PortableObject
        {
        /**
        * {@inheritDoc}
        */
        public Object process(InvocableMap.Entry entry)
            {
            return Boolean.TRUE;
            }

        // ----- ExternalizableLite interface ---------------------------

        /**
        * {@inheritDoc}
        */
        public void readExternal(DataInput in)
                throws IOException
            {
            }

        /**
        * {@inheritDoc}
        */
        public void writeExternal(DataOutput out)
                throws IOException
            {
            }

        // ----- PortableObject interface -------------------------------

        /**
        * {@inheritDoc}
        */
        public void readExternal(PofReader in)
                throws IOException
            {
            }

        /**
        * {@inheritDoc}
        */
        public void writeExternal(PofWriter out)
                throws IOException
            {
            }

        // ----- constants ----------------------------------------------

        /**
        * Singleton instance.
        */
        public static final NullEntryProcessor INSTANCE =
                new NullEntryProcessor();
        }

    /**
    * An implementation of a DeltaCompressor that does nothing and always
    * returns the new stream.
    */
    public static class NullDeltaCompressor
            implements DeltaCompressor
        {
        /**
        * {@inheritDoc}
        */
        public ReadBuffer extractDelta(ReadBuffer bufOld, ReadBuffer bufNew)
            {
            return Base.equals(bufOld, bufNew) ? null : bufNew;
            }

        /**
        * {@inheritDoc}
        */
        public ReadBuffer applyDelta(ReadBuffer bufOld, ReadBuffer bufDelta)
            {
            return bufDelta == null ? bufOld : bufDelta;
            }

        // ----- constants ----------------------------------------------

        /**
        * Singleton instance.
        */
        public static final DeltaCompressor INSTANCE =
                new NullDeltaCompressor();
        }


    /**
    * An implementation of an ActionPolicy that allows all actions.
    */
    public static class NullActionPolicy
            implements ActionPolicy
        {
        // ----- ActionPolicy interface ---------------------------------

        /**
        * {@inheritDoc}
        */
        public void init(com.tangosol.net.Service service)
            {
            }

        /**
        * {@inheritDoc}
        */
        public boolean isAllowed(com.tangosol.net.Service service, Action action)
            {
            return true;
            }

        // ----- Object methods -----------------------------------------

        /**
        * {@inheritDoc}
        */
        public String toString()
            {
            return "{NullActionPolicy allowed-actions=*}";
            }

        // ----- constants ----------------------------------------------

        /**
        * Singleton instance.
        */
        public static final ActionPolicy INSTANCE = new NullActionPolicy();
        }

    /**
    * An implementation of an CacheStore that does nothing.
    */
    public static class NullCacheStore
            implements CacheStore
        {
        /**
        * {@inheritDoc}
        */
        public Object load(Object oKey)
            {
            return oKey;
            }

        /**
        * {@inheritDoc}
        */
        public Map loadAll(Collection colKeys)
            {
            return NullImplementation.getMap();
            }

        /**
        * {@inheritDoc}
        */
        public void store(Object oKey, Object oValue)
            {
            }

        /**
        * {@inheritDoc}
        */
        public void storeAll(Map mapEntries)
            {
            }

        /**
        * {@inheritDoc}
        */
        public void erase(Object oKey)
            {
            }

        /**
        * {@inheritDoc}
        */
        public void eraseAll(Collection colKeys)
            {
            }

        // ----- constants ----------------------------------------------

        /**
        * Singleton instance.
        */
        public static final NullCacheStore INSTANCE = new NullCacheStore();
        }

    /**
    * An implementation of {@link PartitionAssignmentStrategy} that does nothing.
    */
    public static class NullPartitionAssignmentStrategy
            implements PartitionAssignmentStrategy
        {
        /**
        * {@inheritDoc}
        */
        public void init(DistributionManager manager)
            {
            }

        /**
        * {@inheritDoc}
        */
        public void analyzeOrphans(Map<Member, PartitionSet> mapConstraints)
            {
            }

        /**
        * {@inheritDoc}
        */
        public long analyzeDistribution()
            {
            return 0;
            }

        /**
        * {@inheritDoc}
        */
        public String getDescription()
            {
            return null;
            }

        // ----- constants ----------------------------------------------

        /**
        * Singleton instance.
        */
        public static final NullPartitionAssignmentStrategy INSTANCE =
                new NullPartitionAssignmentStrategy();
        }


    // ----- inner class: NullAddressProvider ---------------------------

    /**
    * Null implementation of {@link AddressProvider}.
    */
    public static class NullAddressProvider
            implements AddressProvider
        {
        /**
        * {@inheritDoc}
        */
        public InetSocketAddress getNextAddress()
            {
            return null;
            }

        /**
        * {@inheritDoc}
        */
        public void accept()
            {
            }

        /**
        * {@inheritDoc}
        */
        public void reject(Throwable eCause)
            {
            }

        // ----- constants ----------------------------------------------

        /**
        * Singleton instance.
        */
        public static final NullAddressProvider INSTANCE = new NullAddressProvider();
        }

    // ----- inner class: NullCollector ---------------------------------

    /**
    * A {@link Collector} implementation that does nothing.
    *
    * @param <V>  the value type
    */
    public static class NullCollector<V>
            implements Collector<V>
        {
        /**
        * {@inheritDoc}
        */
        public void add(V value)
            {
            }

        /**
        * {@inheritDoc}
        */
        public void flush()
            {
            }

        // ----- constants ----------------------------------------------

        /**
        * Singleton (unchecked) instance of a Collector.
        */
        public static final NullCollector INSTANCE = new NullCollector();
        }

    // ----- inner class: NullContinuation ------------------------------

    /**
    * A Continuation that does nothing.
    *
    * @param <R>
    */
    public static class NullContinuation<R>
            implements Continuation<R>
        {
        /**
        * {@inheritDoc}
        */
        public void proceed(R r)
            {
            }

        // ----- constants ----------------------------------------------

        /**
        * Singleton instance of a NullContinuation.
        */
        public static final NullContinuation INSTANCE = new NullContinuation();
        }

    // ----- inner class: NullPersistenceEnvironment --------------------

    /**
    * A {@link PersistenceEnvironment} that does nothing.
    *
    * @param <R> the raw value type
    */
    public static class NullPersistenceEnvironment<R>
            implements PersistenceEnvironment<R>
        {
        /**
        * {@inheritDoc}
        */
        public PersistenceManager<R> openActive()
            {
            return null;
            }

        /**
        * {@inheritDoc}
        */
        public PersistenceManager<R> openSnapshot(String sSnapshot)
            {
            throw new IllegalArgumentException();
            }

        /**
        * {@inheritDoc}
        */
        public PersistenceManager<R> createSnapshot(String sSnapshot, PersistenceManager<R> manager)
            {
            return NullPersistenceManager.INSTANCE;
            }

        /**
        * {@inheritDoc}
        */
        public boolean removeSnapshot(String sSnapshot)
            {
            return false;
            }

        /**
        * {@inheritDoc}
        */
        public String[] listSnapshots()
            {
            return new String[0];
            }

        /**
        * {@inheritDoc}
        */
        public void release()
            {
            }

        // ----- constants ----------------------------------------------

        /**
        * Singleton instance of a NullPersistenceEnvironment.
        */
        public static final NullPersistenceEnvironment INSTANCE = new NullPersistenceEnvironment();
        }

    // ----- inner class: NullPersistenceManager ------------------------

    /**
    * A {@link PersistenceManager} that does nothing.
    *
    * @param <R> the raw value type
    */
    public static class NullPersistenceManager<R>
            implements PersistenceManager<R>, PersistenceTools
        {
        /**
        * {@inheritDoc}
        */
        public String getName()
            {
            return null;
            }

        @Override
        public PersistentStore createStore(String sId)
            {
            return NullPersistentStore.INSTANCE;
            }

        /**
        * {@inheritDoc}
        */
        public PersistentStore<R> open(String sId, PersistentStore<R> store)
            {
            return NullPersistentStore.INSTANCE;
            }

        /**
        * {@inheritDoc}
        */
        public PersistentStore<R> open(String sId, PersistentStore<R> store, Collector<Object> collector)
            {
            return open(sId, store);
            }

        /**
        * {@inheritDoc}
        */
        public void close(String sId)
            {
            }

        /**
        * {@inheritDoc}
        */
        public boolean delete(String sId, boolean fSafe)
            {
            return false;
            }

        @Override
        public PersistentStoreInfo[] listStoreInfo()
            {
            return new PersistentStoreInfo[0];
            }

        /**
        * {@inheritDoc}
        */
        public String[] listOpen()
            {
            return new String[0];
            }

        @Override
        public boolean isEmpty(String sId)
            {
            return true;
            }

        /**
        * {@inheritDoc}
        */
        public void read(String sId, InputStream in)
                throws IOException
            {
            }

        /**
        * {@inheritDoc}
        */
        public void write(String sId, OutputStream out)
                throws IOException
            {
            }

        /**
        * {@inheritDoc}
        */
        public void read(String sId, ReadBuffer.BufferInput in)
                throws IOException
            {
            }

        /**
        * {@inheritDoc}
        */
        public void write(String sId, WriteBuffer.BufferOutput out)
                throws IOException
            {
            }

        /**
        * {@inheritDoc}
        */
        public void release()
            {
            }

        /**
         * {@inheritDoc}
         */
        public PersistenceTools getPersistenceTools()
            {
            return this;
            }

        // ----- PersistenceTools interface ---------------------------------

        @Override
        public OfflinePersistenceInfo getPersistenceInfo()
            {
            return null;
            }

        @Override
        public void validate()
            {
            }

        @Override
        public PersistenceStatistics getStatistics()
            {
            return null;
            }

        // ----- constants ----------------------------------------------

        /**
        * Singleton instance of a NullPersistenceManager.
        */
        public static final NullPersistenceManager INSTANCE = new NullPersistenceManager();
        }

    // ----- inner class: NullPersistentStore ---------------------------

    /**
    * A {@link PersistentStore} that does as little as possible.
    *
    * @param <R> the raw value type
    */
    public static class NullPersistentStore<R>
            implements PersistentStore<R>
        {
        @Override
        public String getId()
            {
            return null;
            }

        @Override
        public boolean ensureExtent(long lExtentId)
            {
            return false;
            }

        @Override
        public void deleteExtent(long lExtentId)
            {
            }

        @Override
        public void moveExtent(long lOldExtentId, long lNewExtentId)
            {
            }

        @Override
        public void truncateExtent(long lExtentId)
            {
            }

        @Override
        public long[] extents()
            {
            return new long[0];
            }

        @Override
        public R load(long lExtentId, R key)
            {
            return null;
            }

        @Override
        public boolean containsExtent(long lExtentId)
            {
            return false;
            }

        @Override
        public void store(long lExtentId, R key, R value, Object oToken)
            {
            }

        @Override
        public void erase(long lExtentId, R key, Object oToken)
            {
            }

        @Override
        public void iterate(Visitor<R> visitor)
            {
            }

        @Override
        public boolean isOpen()
            {
            return false;
            }

        @Override
        public Object begin()
            {
            return null;
            }

        @Override
        public Object begin(Collector<Object> collector, Object oReceipt)
            {
            return new Token(collector, oReceipt);
            }

        @Override
        public void commit(Object oToken)
            {
            if (oToken instanceof NullPersistentStore.Token)
                {
                ((Token) oToken).proceed(Boolean.TRUE);
                }
            }

        @Override
        public void abort(Object oToken)
            {
            if (oToken instanceof NullPersistentStore.Token)
                {
                ((Token) oToken).proceed(Boolean.FALSE);
                }
            }

        // ----- inner class: Token ------------------------------------

        /**
        * Token returned by {@link #begin(Collector, Object)}.
        */
        protected class Token implements Continuation<Boolean>
            {
            /**
            * Construct a new Token.
            *
            * @param collector the Collector used by the Token
            * @param oReceipt  the receipt used by the Token
            */
            public Token(Collector<Object> collector, Object oReceipt)
                {
                f_collector = collector;
                f_oReceipt  = oReceipt;
                }

            /**
            * {@inheritDoc}
            */
            public void proceed(Boolean FSuccess)
                {
                if (f_collector != null)
                    {
                    if (FSuccess.booleanValue())
                        {
                        f_collector.add(f_oReceipt);
                        }
                    else
                        {
                        f_collector.add(new AsyncPersistenceException("Transaction aborted")
                                .initReceipt(f_oReceipt)
                                .initPersistentStore(NullPersistentStore.this)
                                .initPersistenceManager(NullImplementation.getPersistenceManager())
                                .initPersistenceEnvironment(NullImplementation.getPersistenceEnvironment()));
                        }
                    }
                }

            // ----- data members ---------------------------------------

            /**
            * The Collector used by this Token.
            */
            private final Collector<Object> f_collector;

            /**
            * The receipt used by this Token.
            */
            private final Object f_oReceipt;
            }

        // ----- constants ----------------------------------------------

        /**
        * Singleton instance of a NullPersistentStore.
        */
        public static final NullPersistentStore INSTANCE = new NullPersistentStore();
        }

    // ----- inner class: NullResourceRegistry --------------------------

    /**
     * A {@link ResourceRegistry} implementation that does nothing.
     */
    public static class NullResourceRegistry
            implements ResourceRegistry
        {
        // ----- ResourceRegistry methods -----------------------------------

        @Override
        public <R> String registerResource(Class<R> clzResource, R resource)
            {
            return null;
            }

        @Override
        public <R> String registerResource(Class<R> clzResource, String sResourceName, R resource)
            {
            return null;
            }

        @Override
        public <R> String registerResource(Class<R> clzResource, Builder<? extends R> bldrResource,
                RegistrationBehavior behavior, ResourceLifecycleObserver<R> observer)
            {
            return null;
            }

        @Override
        public <R> String registerResource(Class<R> clzResource, String sResourceName,
                Builder<? extends R> bldrResource, RegistrationBehavior behavior,
                ResourceLifecycleObserver<R> observer) throws IllegalArgumentException
            {
            return null;
            }

        @Override
        public <R> void unregisterResource(Class<R> clzResource, String sResourceName)
            {
            // no-op
            }

        @Override
        public <R> R getResource(Class<R> clsResource)
            {
            return null;
            }

        @Override
        public <R> R getResource(Class<R> clsResource, String sResourceName)
            {
            return null;
            }

        @Override
        public void dispose()
            {
            // no-op
            }

        // ----- constants ----------------------------------------------

        /**
        * Singleton instance of a NullResourceRegistry.
        */
        public static final NullResourceRegistry INSTANCE = new NullResourceRegistry();
        }

    // ----- inner class: NullLongArray ---------------------------------

    /**
     * An immutable empty {@link LongArray}
     *
     * @param <V> the type of value in the array
     *
     * @author Jonathan Knight  2021.05.05
     * @since 21.06
     */
    public static class NullLongArray<V>
            implements LongArray<V>
        {
        /**
         * Create an immutable empty {@link LongArray}.
         */
        public NullLongArray()
            {
            }

        // ----- LongArray API ------------------------------------------

        @Override
        public V get(long lIndex)
            {
            return null;
            }

        @Override
        public long floorIndex(long lIndex)
            {
            return NOT_FOUND;
            }

        @Override
        public V floor(long lIndex)
            {
            return null;
            }

        @Override
        public long ceilingIndex(long lIndex)
            {
            return NOT_FOUND;
            }

        @Override
        public V ceiling(long lIndex)
            {
            return null;
            }

        @Override
        public V set(long lIndex, V oValue)
            {
            throw new UnsupportedOperationException();
            }

        @Override
        public long add(V oValue)
            {
            return 0;
            }

        @Override
        public boolean exists(long lIndex)
            {
            return false;
            }

        @Override
        public V remove(long lIndex)
            {
            throw new UnsupportedOperationException();
            }

        @Override
        public void remove(long lIndexFrom, long lIndexTo)
            {
            throw new UnsupportedOperationException();
            }

        @Override
        public boolean contains(V oValue)
            {
            return false;
            }

        @Override
        public void clear()
            {
            throw new UnsupportedOperationException();
            }

        @Override
        public boolean isEmpty()
            {
            return true;
            }

        @Override
        public int getSize()
            {
            return 0;
            }

        @Override
        @SuppressWarnings("unchecked")
        public Iterator<V> iterator()
            {
            return (Iterator<V>) INSTANCE_ITERATOR;
            }

        @Override
        public Iterator<V> iterator(long lIndex)
            {
            throw new NoSuchElementException();
            }

        @Override
        @SuppressWarnings("unchecked")
        public Iterator<V> reverseIterator()
            {
            return (Iterator<V>) INSTANCE_ITERATOR;
            }

        @Override
        public Iterator<V> reverseIterator(long lIndex)
            {
            throw new NoSuchElementException();
            }

        @Override
        public long getFirstIndex()
            {
            return NOT_FOUND;
            }

        @Override
        public long getLastIndex()
            {
            return NOT_FOUND;
            }

        @Override
        public long indexOf(V oValue)
            {
            return NOT_FOUND;
            }

        @Override
        public long indexOf(V oValue, long lIndex)
            {
            return NOT_FOUND;
            }

        @Override
        public long lastIndexOf(V oValue)
            {
            return NOT_FOUND;
            }

        @Override
        public long lastIndexOf(V oValue, long lIndex)
            {
            return NOT_FOUND;
            }

        @Override
        @SuppressWarnings("unchecked")
        public LongArray<V> clone()
            {
            try
                {
                return (NullLongArray<V>) super.clone();
                }
            catch (CloneNotSupportedException e)
                {
                throw new RuntimeException(e);
                }
            }

        // ----- inner class: EmptyIterator ---------------------------------

        /**
         * An empty {@link LongArray.Iterator}.
         *
         * @param <V>  the type of values in the {@link LongArray}
         */
        private static class EmptyIterator<V>
                implements LongArray.Iterator<V>
            {
            @Override
            public boolean hasNext()
                {
                return false;
                }

            @Override
            public V next()
                {
                throw new NoSuchElementException();
                }

            @Override
            public long getIndex()
                {
                throw new IllegalStateException();
                }

            @Override
            public V getValue()
                {
                throw new IllegalStateException();
                }

            @Override
            public V setValue(V oValue)
                {
                throw new IllegalStateException();
                }

            @Override
            public void remove()
                {
                throw new IllegalStateException();
                }
            }

        // ----- constants --------------------------------------------------

        /**
         * The singleton empty {@link LongArray}.
         */
        private static final NullLongArray<?> INSTANCE = new NullLongArray<>();

        /**
         * The singleton empty {@link LongArray.Iterator}.
         */
        private static final LongArray.Iterator<?> INSTANCE_ITERATOR = new NullLongArray.EmptyIterator<>();
        }

    // ----- inner class NullMemberIdentityProvider ---------------------

    /**
     * An implementation of a {@link MemberIdentityProvider} that
     * returns {@code null} for all its methods.
     *
     * @since 22.06
     */
    public static class NullMemberIdentityProvider
            implements MemberIdentityProvider
        {
        @Override
        public String getMachineName()
            {
            return null;
            }

        @Override
        public String getMemberName()
            {
            return null;
            }

        @Override
        public String getRackName()
            {
            return null;
            }

        @Override
        public String getSiteName()
            {
            return null;
            }

        @Override
        public String getRoleName()
            {
            return null;
            }

        /**
         * A singleton instance of a {@link NullMemberIdentityProvider}.
         */
        public static final NullMemberIdentityProvider INSTANCE = new NullMemberIdentityProvider();
        }

    // ----- data members -----------------------------------------------

    /**
    * Singleton implementation: Since the NullImplementation contains no
    * information, only one ever has to exist.
    */
    private static final NullImplementation LOADER = AccessController.doPrivileged(
        new PrivilegedAction<NullImplementation>()
            {
            public NullImplementation run()
                {
                return new NullImplementation();
                }
            });
    }
