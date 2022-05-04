/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package events.common;

import com.tangosol.net.events.annotation.Interceptor;
import com.tangosol.net.events.partition.PartitionedServiceDispatcher;
import com.tangosol.net.events.partition.TransferEvent;
import com.tangosol.net.events.partition.TransferEvent.RecoveryTransferEvent;

import com.tangosol.util.BinaryEntry;
import com.tangosol.util.Filter;

import events.common.AbstractTestInterceptor.Expectations.AbstractExpectation;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

import java.io.Serializable;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.MatcherAssert.assertThat;

import static events.common.TestTransferInterceptor.IDENTIFIER;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;

/**
 * Test interceptor for {@link com.tangosol.net.events.partition.TransferEvent}s.
 * @author nsa 2011.08.13
 * @since 3.7.1
 */
@Interceptor(identifier = IDENTIFIER)
public class TestTransferInterceptor
        extends AbstractTestInterceptor<TransferEvent>
        implements Filter, Serializable
    {

    // ---- constructors ----------------------------------------------------

    /**
     * Default constructor.
     */
    public TestTransferInterceptor()
        {
        }

    // ----- EventInterceptor methods ---------------------------------------

    /**
     * {@inheritDoc}
     */
    public void processEvent(TransferEvent e)
        {
        Map<Enum, Expectations> mapExpectations = getExpectations(IDENTIFIER);
        Expectations            expectations    = mapExpectations == null
                                                    ? null : mapExpectations.get(e.getType());

        ConcurrentMap<Enum, AtomicInteger> mapTypeCounter = m_mapTypeCounter;

        AtomicInteger cType = mapTypeCounter.get(e.getType());
        if (cType == null)
            {
            AtomicInteger cTypePrev = mapTypeCounter.putIfAbsent(e.getType(), cType = new AtomicInteger());
            cType = cTypePrev == null ? cType : cTypePrev;
            }

        cType.incrementAndGet();

        if (expectations != null)
            {
            m_event.set(e);
            try
                {
                for (Set<BinaryEntry> setEntries : e.getEntries().values())
                    {
                    processEntries(setEntries, expectations);
                    }

                for (Iterator<AbstractExpectation> iter = expectations.getExpectations(); iter.hasNext(); )
                    {
                    AbstractExpectation expectation = iter.next();
                    try
                        {
                        if (expectation.shouldProcess(this))
                            {
                            assertThat(this, expectation.matcher());

                            if (expectation.isEmpty())
                                {
                                iter.remove();
                                }
                            }
                        }
                    catch (AssertionError ex)
                        {
                        ex.printStackTrace();
                        err(ex.getMessage());
                        }
                    }
                }
            finally
                {
                m_event.set(null);
                }
            }

        if (expectations != null && expectations.isEmpty())
            {
            mapExpectations.remove(e.getType());
            }
        }

    /**
     * {@inheritDoc}
     */
    // TODO: this method should not be called
    public boolean evaluate(Object oDispatcher)
        {
        if (oDispatcher instanceof PartitionedServiceDispatcher)
            {
            PartitionedServiceDispatcher serviceDispatcher = (PartitionedServiceDispatcher) oDispatcher;

            return !serviceDispatcher.getService().getInfo().getServiceName().equals("ResultsService");
            }
        return false;
        }

    public AtomicInteger getEventTypeCount(TransferEvent.Type eventType)
        {
        AtomicInteger typeCounter = m_mapTypeCounter.get(eventType);
        if (typeCounter == null)
            {
            AtomicInteger typeCounterPrev = m_mapTypeCounter.putIfAbsent(eventType, typeCounter = new AtomicInteger());
            typeCounter = typeCounterPrev == null ? typeCounter : typeCounterPrev;
            }
        return typeCounter;
        }

    public TransferEvent getEvent()
        {
        return m_event.get();
        }

    public static class TransferExpectation
            extends AbstractExpectation
        {

        public TransferExpectation(TransferEvent.Type eventType, Matcher<? super Integer> mtcrEventsCount)
            {
            m_eventType       = eventType;
            m_mtcrEventsCount    = mtcrEventsCount;
            }

        protected TransferExpectation(TransferExpectation that)
            {
            m_eventCounter       = new AtomicInteger(that.m_eventCounter.get());
            m_eventType       = that.m_eventType;
            m_mtcrEventsCount    = that.m_mtcrEventsCount;
            }

        @Override public boolean shouldProcess(AbstractTestInterceptor incptr)
            {
            return m_mtcrEventsCount.matches(m_eventCounter.incrementAndGet());
            }

        @Override public boolean isEmpty()
            {
            return m_mtcrEventsCount.matches(m_eventCounter.get());
            }

        @Override public Matcher matcher()
            {
            return new BaseMatcher()
                {
                @Override public boolean matches(Object item)
                    {
                    if (!(item instanceof TestTransferInterceptor))
                        {
                        return false;
                        }

                    TestTransferInterceptor incptr = (TestTransferInterceptor) item;
                    AtomicInteger typeCount = incptr.getEventTypeCount(m_eventType);
                    int cType = typeCount.get();

                    if (m_mtcrEventsCount.matches(cType))
                        {
                        typeCount.compareAndSet(cType, 0);
                        return true;
                        }

                    return false;
                    }

                @Override public void describeTo(Description description)
                    {
                    description.appendText("Expected " + m_eventType.name() + " count of [")
                               .appendDescriptionOf(m_mtcrEventsCount)
                               .appendText("].");
                    }
                };
            }

        @Override
        protected TransferExpectation clone()
                throws CloneNotSupportedException
            {
            return new TransferExpectation(this);
            }

        // ----- data members -----------------------------------------------

        protected AtomicInteger m_eventCounter = new AtomicInteger();
        protected TransferEvent.Type       m_eventType;
        protected Matcher<? super Integer> m_mtcrEventsCount;
        }

    public static class RecoverTransferExpectation
            extends TransferExpectation
        {
        public RecoverTransferExpectation(Matcher<? super Integer> mtcrEventsCount)
            {
            this(mtcrEventsCount, null);
            }

        public RecoverTransferExpectation(Matcher<? super Integer> mtcrEventsCount, String sSnapshotName)
            {
            super(TransferEvent.Type.RECOVERED, mtcrEventsCount);

            m_sSnapshotName = sSnapshotName;
            }

        protected RecoverTransferExpectation(RecoverTransferExpectation that)
            {
            super(that);

            m_sSnapshotName = that.m_sSnapshotName;
            }

        @Override
        public boolean shouldProcess(AbstractTestInterceptor incptr)
            {
            assertThat(((TestTransferInterceptor) incptr).getEvent(), instanceOf(RecoveryTransferEvent.class));

            RecoveryTransferEvent event = (RecoveryTransferEvent) ((TestTransferInterceptor) incptr).getEvent();

            assertEquals(m_sSnapshotName, event.getSnapshotName());

            return super.shouldProcess(incptr);
            }

        @Override
        protected RecoverTransferExpectation clone()
                throws CloneNotSupportedException
            {
            return new RecoverTransferExpectation(this);
            }

        // ----- data members -----------------------------------------------

        protected String m_sSnapshotName;
        }

    // ----- constants ------------------------------------------------------

    public static final String IDENTIFIER = "TestTransferInterceptor";

    public ThreadLocal<TransferEvent> m_event = new ThreadLocal<>();
    }
