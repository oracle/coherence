/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package events.common;

import com.oracle.coherence.common.collections.ChainedIterator;

import com.tangosol.net.AbstractInvocable;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.events.Event;
import com.tangosol.net.events.EventInterceptor;
import com.tangosol.net.events.InterceptorRegistry;

import com.tangosol.util.Base;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.RegistrationBehavior;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

import java.io.Serializable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

/**
 * AbstractTestInterceptor is an EventInterceptor providing some common test
 * related support. This includes:
 * <ol>
 *     <li>a count of the number of onEvent invocations</li>
 *     <li>a structure for holding expected values in events</li>
 *     <li>an invocable capable of informing a sub class of expected event values</li>
 *     <li>common mechanism to log {@link #err(String) errors}</li>
 *     <li>an invocable capable of collecting those messages</li>
 * </ol>
 *
 * @author hr  2012.09.07
 * @since Coherence 12.1.2
 */
public abstract class AbstractTestInterceptor<T extends Event<? extends Enum>>
        implements EventInterceptor<T>
    {

    // ----- AbstractConfigurableEventInterceptor methods -------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public void onEvent(T event)
        {
        m_invocations.incrementAndGet();

        processEvent(event);
        }

    /**
     * Process the dispatched {@link Event}.
     *
     * @param e  the event to be processed.
     */
    protected abstract void processEvent(T e);

    // ----- helpers --------------------------------------------------------

    /**
     * Register the provided error message
     *
     * @param sMsg  an error message
     */
    protected void err(String sMsg)
        {
        m_listErrs.add(sMsg);
        }

    /**
     * Process the provided set of {@link BinaryEntry entries} validating
     * against the expectations.
     *
     * @param setEntries    a set of entries related to the event
     * @param expectations  expected values
     */
    protected void processEntries(Set<BinaryEntry> setEntries, Expectations expectations)
        {
        processEntries(setEntries, expectations, null);
        }

    /**
     * Process the provided set of {@link BinaryEntry entries} validating
     * against the expectations.
     *
     * @param setEntries    a set of entries related to the event
     * @param expectations  expected values
     * @param eventType     the event type to validate the entries against or
     *                      null
     */
    protected void processEntries(Set<BinaryEntry> setEntries, Expectations expectations, Enum eventType)
        {
        boolean fCheckEventType = expectations.isEventTypeCheck();
        for (BinaryEntry binEntry : setEntries)
            {
            try
                {
                if (fCheckEventType)
                    {
                    assertThat(eventType, notNullValue());
                    assertThat(binEntry, expectations.hasEntryAndRemove(eventType));
                    }
                else
                    {
                    assertThat(binEntry, expectations.hasEntryAndRemove());
                    }
                }
            catch (AssertionError e)
                {
                e.printStackTrace();
                err(e.getMessage());
                }
            }
        }

    /**
     * Return a unique name for registering the ExpectationHolder interceptor
     * in the InterceptorRegistry.
     *
     * @param sInterceptorName  Interceptor name for which the ExpectationHolder
     *                          is being registered
     *
     * @return  ExpectationHolder name
     */
    public static String getExpectationHolderName(String sInterceptorName)
        {
        return sInterceptorName + "-Holder";
        }

    /**
     * Return the expectations map for the given interceptor
     *
     * @param sIdentifier  Interceptor identifier
     *
     * @return Expectations map.
     */
    public Map<Enum, Expectations> getExpectations(String sIdentifier)
        {
        InterceptorRegistry registry = CacheFactory.getConfigurableCacheFactory().getInterceptorRegistry();
        ExpectationsHolder  holder   = (ExpectationsHolder)
                registry.getEventInterceptor(getExpectationHolderName(sIdentifier));

        return holder == null ? null : holder.getExpectations();
        }

    // ----- inner class: ErrorAccumulator ----------------------------------

    /**
     * An Invocable capable of accumulating errors that have occurred on a
     * {@link TxnAssertingInterceptor}.
     */
    public static class ErrorAccumulator
            extends AbstractInvocable
            implements Serializable
        {

        // ----- constructors -----------------------------------------------

        /**
         * Construct an ErrorAccumulator.
         */
        public ErrorAccumulator()
            {
            }

        /**
         * Construct an ErrorAccumulator.
         */
        public ErrorAccumulator(String sIdentifier)
            {
            m_sIdentifier = sIdentifier;
            }

        // ----- AbstractInvocable methods ----------------------------------

        /**
         * {@inheritDoc}
         */
        @Override
        public void run()
            {
            InterceptorRegistry     registry    = CacheFactory.getConfigurableCacheFactory().getInterceptorRegistry();
            AbstractTestInterceptor interceptor = (AbstractTestInterceptor) registry.getEventInterceptor(m_sIdentifier);
            ExpectationsHolder      holder      = (ExpectationsHolder) registry.getEventInterceptor(
                                                    getExpectationHolderName(m_sIdentifier));

            List      listResult = new ArrayList(interceptor.m_listErrs);

            Set<Enum> setEvents;
            if (holder != null && (setEvents = holder.m_mapExpectations.keySet()) != null &&
                    setEvents.size() > 0)
                {
                listResult.add("The following expected events did not complete: " + setEvents);
                }
            listResult.add(0, interceptor.m_invocations.get());
            setResult(listResult);
            }

        // ----- data members -----------------------------------------------

        /**
         * The interceptor identifier to lookup the interceptor from the
         * registry.
         */
        protected String m_sIdentifier;
        }

    /**
     * An Invocable capable of accumulating errors that have occurred on a
     * {@link TxnAssertingInterceptor}.
     */
    public static class ResetAccumulator
            extends AbstractInvocable
            implements Serializable
        {

        // ----- constructors -----------------------------------------------

        /**
         * Construct an ErrorAccumulator.
         */
        public ResetAccumulator()
            {
            }

        /**
         * Construct an ErrorAccumulator.
         */
        public ResetAccumulator(String sIdentifier)
            {
            m_sIdentifier = sIdentifier;
            }

        // ----- AbstractInvocable methods ----------------------------------

        /**
         * {@inheritDoc}
         */
        @Override public void run()
            {
            InterceptorRegistry     registry    = CacheFactory.getConfigurableCacheFactory().getInterceptorRegistry();
            AbstractTestInterceptor interceptor = (AbstractTestInterceptor) registry.getEventInterceptor(m_sIdentifier);

            registry.unregisterEventInterceptor(getExpectationHolderName(m_sIdentifier));

            interceptor.m_listErrs.clear();
            interceptor.m_invocations.set(0);
            interceptor.m_mapTypeCounter.clear();
            }

        // ----- data members -----------------------------------------------

        /**
         * The interceptor identifier to lookup the interceptor from the
         * registry.
         */
        protected String m_sIdentifier;
        }


    // ----- inner class: ExpectationsInformerInvocable ---------------------

    /**
     * An Invocable that sets the expected results for different event types.
     */
    public static class ExpectationsInformerInvocable
            extends AbstractInvocable
            implements Serializable
        {

        // ----- constructors -----------------------------------------------

        /**
         * Construct an ExpectationsInformerInvocable.
         */
        public ExpectationsInformerInvocable()
            {
            }

        /**
         * Construct an ExpectationsInformerInvocable with the provided
         * name of the interceptor.
         *
         * @param sIdentifier  the identifier of the event interceptor
         */
        public ExpectationsInformerInvocable(String sIdentifier)
            {
            m_sIdentifier  = sIdentifier;
            }


        // ----- accessors --------------------------------------------------

        /**
         * Adds an expectation with the provided event type.
         *
         * @param eventType     the event type that should raise the expectation
         * @param expectations  the values expected
         *
         * @return a reference to this invocable
         */
        public ExpectationsInformerInvocable addExpectation(Enum eventType, Expectations expectations)
            {
            m_mapExpectations.put(eventType, expectations);
            return this;
            }

        // ----- AbstractInvocable methods ----------------------------------

        /**
         * {@inheritDoc}
         */
        @Override public void run()
            {
            InterceptorRegistry registry = CacheFactory.getConfigurableCacheFactory().getInterceptorRegistry();
            ExpectationsHolder  holder   = new ExpectationsHolder(m_mapExpectations);

            registry.registerEventInterceptor(AbstractTestInterceptor.getExpectationHolderName(m_sIdentifier),
                    holder, RegistrationBehavior.REPLACE);
            }

        // ----- data members -----------------------------------------------

        /**
         * The interceptor identifier to lookup the interceptor from the
         * registry.
         */
        protected String m_sIdentifier;

        /**
         * A map of expected values by type.
         */
        protected Map<Enum, Expectations> m_mapExpectations = new HashMap<Enum, Expectations>();
        }

    // ----- inner class: Expectations --------------------------------------

    /**
     * A container for the expected state for a cache name, an event type
     * and the key and values of the cache.
     *
     * @param <T>  event type
     * @param <K>  key type
     * @param <V>  value type
     */
    public static class Expectations<T extends Enum, K, V>
            implements Cloneable, java.io.Serializable
        {

        // ----- constructors -----------------------------------------------

        /**
         * Create a holder of expectations.
         */
        public Expectations()
            {
            this(false);
            }

        /**
         * Create a holder of expectations.
         *
         * @param fCheckEventType  whether to check the event type
         */
        public Expectations(boolean fCheckEventType)
            {
            m_fCheckEventType = fCheckEventType;
            }

        /**
         * Creates a copy of the Expectations.
         *
         * @param e  the expectations to copy from
         */
        protected Expectations(Expectations e)
            {
            m_fCheckEventType = e.m_fCheckEventType;

            // clone each map entry into a new map
            Map<String, CacheEntryExpectation<T, K, V>> mapExpectations = m_mapExpectations;
            for (Iterator iter = e.m_mapExpectations.entrySet().iterator(); iter.hasNext(); )
                {
                Entry entry = (Entry) iter.next();

                try
                    {
                    mapExpectations.put((String) entry.getKey(), ((CacheEntryExpectation) entry.getValue()).clone());
                    }
                catch (CloneNotSupportedException ex) {}
                }

            for (AbstractExpectation expectation : (List<AbstractExpectation>) e.m_listExpectations)
                {
                try
                    {
                    m_listExpectations.add(expectation.clone());
                    }
                catch (CloneNotSupportedException ex) {}
                }
            }

        // ----- Expectations methods ---------------------------------------

        /**
         * Defines a common expectation; to expect a key and value in a specific
         * cache.
         *
         * @param sCacheName  the name of the cache the key and value will be present
         * @param eventType   the event type expected
         * @param key         the key expected in the cache
         * @param value       the value expected in the cache
         *
         * @return a reference to this Expectations
         */
        public Expectations<T, K, V> expect(String sCacheName, T eventType, K key, V value)
            {
            CacheEntryExpectation<T, K, V> expectation = m_mapExpectations.get(sCacheName);

            if (expectation == null)
                {
                m_mapExpectations.put(sCacheName, expectation =
                        new CacheEntryExpectation<T, K, V>(sCacheName, (Class<T>) eventType.getClass()));
                }

            expectation.expect(eventType, key, value);

            return this;
            }

        /**
         * Registers an expectation.
         *
         * @param expectation  the expected outcome of the event
         *
         * @return a reference to this Expectations
         */
        public Expectations<T, K, V> expect(AbstractExpectation expectation)
            {
            m_listExpectations.add(expectation);

            return this;
            }

        // ----- accessors --------------------------------------------------

        /**
         * Return an {@link CacheEntryExpectation} associated with the provided cache
         * name.
         *
         * @param sCacheName  the name of the cache the CacheEntryExpectation is
         *                    associated to
         *
         * @return the associated CacheEntryExpectation
         */
        public CacheEntryExpectation getExpectation(String sCacheName)
            {
            return m_mapExpectations.get(sCacheName);
            }

        /**
         * Returns an {@link Iterator} of expected entries.
         *
         * @param eventType  the event type the expectations are associated to
         *
         * @return an Iterator of expected entries
         */
        public Iterator<Entry<K, V>> getExpectedValues(T eventType)
            {
            Class<?> clzEventType = eventType.getClass();
            List<Iterator<Entry<K, V>>> listIters    = new ArrayList<Iterator<Entry<K, V>>>();

            for (CacheEntryExpectation<T, K, V> expectation : m_mapExpectations.values())
                {
                if (expectation.getEventTypeClass() == clzEventType)
                    {
                    listIters.add(expectation.getValues(eventType).entrySet().iterator());
                    }
                }

            return new ChainedIterator<Entry<K, V>>(listIters.toArray(new Iterator[listIters.size()]));
            }

        public Iterator<AbstractExpectation> getExpectations()
            {
            return m_listExpectations.iterator();
            }

        /**
         * Returns whether the event type should be checked.
         *
         * @return whether the event type should be checked
         */
        public boolean isEventTypeCheck()
            {
            return m_fCheckEventType;
            }

        /**
         * Determines whether all known expectations have been satisfied thus
         * removed.
         *
         * @return whether all known expectations have been satisfied
         */
        public boolean isEmpty()
            {
            for (Iterator<CacheEntryExpectation<T, K, V>> iter = m_mapExpectations.values().iterator(); iter.hasNext(); )
                {
                if (!iter.next().isEmpty())
                    {
                    return false;
                    }
                }

            for (AbstractExpectation expectation : m_listExpectations)
                {
                if (!expectation.isEmpty())
                    {
                    return false;
                    }
                }

            return true;
            }

        /**
         * Returns a {@link Matcher} used in assertions with a
         * {@link BinaryEntry} to determine whether one of the registered
         * expected values is equivalent to the provided Map.Entry. If
         * the Map.Entry was expected the expected value will be removed.
         * This matcher can be used similar to:
         * <pre><code>
         *     assertThat(binEntry, expectations.matcher());
         * </code></pre>
         *
         * @return a Matcher used to assert a given BinaryEntry is one of
         *         the expected values
         */
        public Matcher hasEntryAndRemove()
            {
            return new BaseMatcher()
                {
                @Override public boolean matches(Object item)
                    {
                    if (!(item instanceof BinaryEntry))
                        {
                        return false;
                        }

                    BinaryEntry            binEntry    = (BinaryEntry) item;
                    String                 sCacheName  = binEntry.getBackingMapContext().getCacheName();
                    CacheEntryExpectation expectation = m_mapExpectations.get(sCacheName);
                    Matcher                matcher     = expectation == null ? null : expectation.matcher();
                    boolean                fPass       = matcher != null && matcher.matches(binEntry);

                    if (expectation.isEmpty())
                        {
                        m_mapExpectations.remove(sCacheName);
                        }

                    return fPass;
                    }

                @Override public void describeTo(Description description)
                    {
                    description.appendText("The entry was not expected.");
                    }
                };
            }

        /**
         * Returns a {@link Matcher} used in assertions with a
         * {@link BinaryEntry} to determine whether one of the registered
         * expected values is equivalent to the provided BinaryEntry and
         * event type. If the BinaryEntry was expected the expected value
         * will be removed. This matcher can be used similar to:
         * <pre><code>
         *     assertThat(binEntry, expectations.matcher(eventType));
         * </code></pre>
         *
         * @return a Matcher used to assert a given BinaryEntry is one of
         *         the expected values with the correct event type
         */
        public Matcher hasEntryAndRemove(final T eventType)
            {
            return new BaseMatcher()
                {
                @Override public boolean matches(Object item)
                    {
                    if (!(item instanceof BinaryEntry))
                        {
                        return false;
                        }

                    BinaryEntry binEntry   = (BinaryEntry) item;
                    String      sCacheName = binEntry.getBackingMapContext().getCacheName();
                    CacheEntryExpectation expectation = m_mapExpectations.get(sCacheName);
                    Map<K, V>             mapExpect   = expectation == null ? null : expectation.getValues(eventType);

                    if (mapExpect == null || mapExpect.isEmpty())
                        {
                        return false;
                        }

                    Object  oKey  = binEntry.getKey();
                    Object  oVal  = binEntry.getValue();
                    V       value = mapExpect.get(oKey);
                    boolean fPass = (oVal == null && value == null) ? true : Base.equals(value, oVal);

                    if (fPass)
                        {
                        mapExpect.remove(oKey);

                        if (expectation.isEmpty())
                            {
                            m_mapExpectations.remove(sCacheName);
                            }
                        }

                    return fPass;
                    }

                @Override public void describeTo(Description description)
                    {
                    description.appendText("Expected an entry with event type: " + eventType);
                        }
                };
            }

        // ----- Object methods ---------------------------------------------

        /**
         * {@inheritDoc}
         */
        @Override protected Expectations<T, K, V> clone()
                throws CloneNotSupportedException
            {
            return new Expectations(this);
            }

        /**
         * {@inheritDoc}
         */
        @Override public String toString()
            {
            return "Expectations [mapExpectations = " + m_mapExpectations + "]";
            }

        // ----- inner class: AbstractExpectation ---------------------------

        protected static abstract class AbstractExpectation
                implements Cloneable, Serializable
            {

            public boolean shouldProcess(AbstractTestInterceptor incptr)
                {
                return true;
                }

            public abstract boolean isEmpty();

            public abstract Matcher matcher();

            @Override protected abstract AbstractExpectation clone()
                    throws CloneNotSupportedException;
            }

        // ----- inner class: CacheEntryExpectation -------------------------

        /**
         * An CacheEntryExpectation represents the values expected for an event. Each
         * expected value is a tuple of: event type, key and value. This
         * expectation holds many expected values for a single cache.
         */
        protected static class CacheEntryExpectation<T extends Enum, K, V>
                extends AbstractExpectation
            {

            // ----- constructors -------------------------------------------

            /**
             * Construct an CacheEntryExpectation with the provided cache name and
             * event type class.
             *
             * @param sCacheName    name of the cache
             * @param clzEventType  the Enum event type
             */
            protected CacheEntryExpectation(String sCacheName, Class<T> clzEventType)
                {
                m_sCacheName   = sCacheName;
                m_clzEventType = clzEventType;
                m_aMapValues   = new Map[clzEventType.getEnumConstants().length];
                }

            /**
             * Creates a copy of the CacheEntryExpectation.
             *
             * @param e  the expectation to copy from
             */
            protected CacheEntryExpectation(CacheEntryExpectation e)
                {
                m_sCacheName   = e.m_sCacheName;
                m_clzEventType = e.m_clzEventType;

                Map<K, V>[] mapThatValues = e.m_aMapValues;
                Map<K, V>[] mapThisValues = m_aMapValues = new Map[mapThatValues.length];

                for (int i = 0; i < mapThatValues.length; ++i)
                    {
                    mapThisValues[i] = mapThatValues[i] == null
                            ? null : new HashMap<K, V>(mapThatValues[i]);
                    }
                }

            // ----- accessors ----------------------------------------------

            /**
             * Returns the name of the cache for this CacheEntryExpectation.
             *
             * @return the name of the cache for this CacheEntryExpectation
             */
            public String getCacheName()
                {
                return m_sCacheName;
                }

            /**
             * Returns the event type class this expectation was initialized
             * with.
             *
             * @return the event type class
             */
            public Class<T> getEventTypeClass()
                {
                return m_clzEventType;
                }

            /**
             * Returns a map of expected entries in the event with the
             * provided event type.
             *
             * @param eventType  the event type
             *
             * @return a map of expected entries in the event
             */
            public Map<K, V> getValues(T eventType)
                {
                return m_aMapValues[eventType.ordinal()];
                }

            /**
             * Returns an {@link Iterator} of expected {@link Map.Entry entries}
             * in the event regardless of event type.
             *
             * @return an iterator of expected entries
             */
            public Iterator<Map.Entry<K, V>> getValues()
                {
                Map<K, V>[]    aMapValues = m_aMapValues;
                List<Iterator> listIter    = new ArrayList<Iterator>();

                for (int i = 0; i < aMapValues.length; ++i)
                    {
                    if (aMapValues[i] != null)
                        {
                        listIter.add(aMapValues[i].entrySet().iterator());
                        }
                    }

                return new ChainedIterator(listIter.toArray(new Iterator[listIter.size()]));
                }

            /**
             * Returns whether this expectation has no further expectations
             * this is fulfilled.
             *
             * @return whether this expectation has no further expectations
             *         this is fulfilled
             */
            public boolean isEmpty()
                {
                Map<K, V>[] aMapValues = m_aMapValues;

                for (int i = 0; i < aMapValues.length; ++i)
                    {
                    if (aMapValues[i] != null && !aMapValues[i].isEmpty())
                        {
                        return false;
                        }
                    }

                return true;
                }

            // ----- CacheEntryExpectation methods --------------------------

            /**
             * Returns a {@link Matcher} used in assertions with a
             * {@link Map.Entry} to determine whether one of the registered
             * expected values is equivalent to the provided Map.Entry. If
             * the Map.Entry was expected the expected value will be removed.
             * This matcher can be used similar to:
             * <pre><code>
             *     assertThat(mapEntry, expectation.matcher());
             * </code></pre>
             *
             * @return a Matcher used to assert a given Map.Entry is one of
             *         the expected values
             */
            public Matcher matcher()
                {
                return new BaseMatcher()
                    {
                    @Override public boolean matches(Object item)
                        {
                        if (!(item instanceof Entry))
                            {
                            return false;
                            }

                        Entry entryThis = (Entry) item;

                        for (Iterator iter = getValues(); iter.hasNext(); )
                            {
                            Entry entryThat = (Entry) iter.next();

                            if (   Base.equals(entryThis.getKey(), entryThat.getKey())
                                && Base.equals(entryThis.getValue(), entryThat.getValue()))
                                {
                                iter.remove();
                                return true;
                                }
                            }

                        return false;
                        }

                    @Override public void describeTo(Description description)
                        {
                        description.appendText("The entry was not expected.");
                        }
                    };
                }

            // ----- helpers ------------------------------------------------

            /**
             * Registers an expected event type and key and value pair.
             *
             * @param eventType  expected event type
             * @param key        expected key
             * @param value      expected value
             */
            protected void expect(T eventType, K key, V value)
                {
                int       iType     = eventType.ordinal();
                Map<K, V> mapValues = m_aMapValues[iType];

                if (mapValues == null)
                    {
                    mapValues = m_aMapValues[iType] = new HashMap<K, V>();
                    }

                mapValues.put(key, value);
                }


            // ----- Object methods -----------------------------------------

            /**
             * {@inheritDoc}
             */
            @Override protected CacheEntryExpectation clone()
                    throws CloneNotSupportedException
                {
                return new CacheEntryExpectation(this);
                }

            /**
             * {@inheritDoc}
             */
            @Override public String toString()
                {
                boolean       fReturn = false;
                StringBuilder sb = new StringBuilder("CacheEntryExpectation [cacheName = ")
                        .append(m_sCacheName)
                        .append(", mapValues = {");

                for (int i = 0; i < m_aMapValues.length; ++i)
                    {
                    Map<K, V> mapValues = m_aMapValues[i];

                    if (mapValues != null && !mapValues.isEmpty())
                        {
                        if (sb.charAt(sb.length() - 1) != '{')
                            {
                            sb.append(", ");
                            }
                        sb.append(m_clzEventType.getEnumConstants()[i].name())
                          .append("->").append(mapValues);

                        fReturn = true;
                        }
                    }
                sb.append("}");

                return fReturn ? "" : sb.toString();
                }

            // ----- data members -------------------------------------------

            /**
             * The name of the cache.
             */
            protected String      m_sCacheName;

            /**
             * The event type class.
             */
            protected Class<T>    m_clzEventType;

            /**
             * An array of map values with each array element representing
             * expected values for an event type.
             */
            protected Map<K, V>[] m_aMapValues;
            }

        // ----- data members -----------------------------------------------

        /**
         * A map of expectations by cache name.
         */
        protected Map<String, CacheEntryExpectation<T, K, V>> m_mapExpectations = new HashMap<String, CacheEntryExpectation<T, K, V>>();

        /**
         * A list of expectations for those applicable outside of cache
         * scoped expectations.
         */
        protected List<AbstractExpectation> m_listExpectations = new ArrayList<AbstractExpectation>();

        /**
         * Whether to check the event type(s).
         */
        protected boolean m_fCheckEventType;
        }

     // ----- inner class: ExpectationsHolder -------------------------------

    /**
     * A dummy EventInterceptor that is registered in the InterceptorRegistry
     * to hold Expectations for an EventInterceptor that is used by the tests.
     */
    public static class ExpectationsHolder
            implements EventInterceptor
        {

        // ----- constructors -----------------------------------------------

        /**
         * Construct an ExpectationsHolder.
         */
        public ExpectationsHolder(Map<Enum, Expectations> expectations)
            {
            Map<Enum, Expectations> mapExpectations = m_mapExpectations;
            for (Map.Entry<Enum, Expectations> entry : expectations.entrySet())
                {
                try
                    {
                    mapExpectations.put(entry.getKey(), entry.getValue().clone());
                    }
                catch (CloneNotSupportedException e) { }
                }
            }

        public Map<Enum, Expectations> getExpectations()
            {
            return m_mapExpectations;
            }

        @Override
        public void onEvent(Event event) { }  // no-op

        /**
         * Map of Expectations used for asserting results.
         */
        protected Map<Enum, Expectations> m_mapExpectations = new ConcurrentHashMap<Enum, Expectations>();
        }

    // ----- data members ---------------------------------------------------

    /**
     * CAS operation to hold the number of onEvent invocations.
     */
    protected AtomicInteger m_invocations = new AtomicInteger();

    protected ConcurrentMap<Enum, AtomicInteger> m_mapTypeCounter = new ConcurrentHashMap<Enum, AtomicInteger>();

    /**
     * A list of errors occurred.
     */
    protected List<String> m_listErrs = new CopyOnWriteArrayList<String>();
    }
