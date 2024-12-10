/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package events;

import com.tangosol.net.BackingMapContext;
import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.net.InvocationService;
import com.tangosol.net.Member;
import com.tangosol.net.NamedCache;

import com.tangosol.net.cache.ConfigurableCacheMap;

import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.tangosol.net.events.partition.TransactionEvent;
import com.tangosol.net.events.partition.TransactionEvent.Type;
import com.tangosol.net.events.partition.UnsolicitedCommitEvent;
import com.tangosol.net.events.partition.cache.EntryEvent;

import com.tangosol.net.partition.DefaultKeyAssociator;

import com.tangosol.util.Binary;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.InvocableMap.Entry;
import com.tangosol.util.processor.AbstractProcessor;

import com.oracle.coherence.testing.AbstractFunctionalTest;

import events.common.AbstractTestInterceptor.Expectations;
import events.common.AbstractTestInterceptor.ExpectationsInformerInvocable;

import events.common.TxnAssertingInterceptor;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.Serializable;

import java.util.Set;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static events.EventTestHelper.remoteReset;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * TransactionEventTests is responsible for ...
 *
 * @author hr  2012.09.03
 * @since Coherence 12.1.2
 */
public class TransactionEventTests
        extends AbstractFunctionalTest
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default Constructor.
     */
    public TransactionEventTests()
        {
        super(CFG_FILE);
        }

    // ----- test lifecycle -------------------------------------------------

    /**
     * Initialize the test class.
     */
    @BeforeClass
    public static void startup()
        {
        startCacheServer("TransactionEventTests", "events", CFG_FILE);
        ensureRunningService("TransactionEventTests", "PartitionedXActumService");
        ensureRunningService("TransactionEventTests", "InvocationService");
        }

    /**
     * Shutdown the test class.
     */
    @AfterClass
    public static void shutdown()
        {
        stopCacheServer("TransactionEventTest");
        }

    // ----- test methods ---------------------------------------------------

    /**
     * Test that a {@link TransactionEvent} is dispatched exposing all <em>
     * mutated</em> entries in a partition lite txn.
     *
     * @throws InterruptedException
     */
    @Test
    public void testTxnEvents()
            throws InterruptedException
        {
        NamedCache cachei  = getNamedCache("dist-plo-i");
        NamedCache cacheii = getNamedCache("dist-plo-ii");
        InvocationService service = (InvocationService)
                getFactory().ensureService(EventTestHelper.INVOCATION_SERVICE_NAME);
        Set<Member> setRecipients = service.getInfo().getServiceMembers();

        if (!STORAGE_ENABLED)
            {
            setRecipients.remove(service.getCluster().getLocalMember());
            }
        try
            {
            Expectations expectPre = new Expectations(/*fCheckEventType*/ true)
                .expect("dist-plo-i",  EntryEvent.Type.INSERTING, 5, 5)
                .expect("dist-plo-i",  EntryEvent.Type.INSERTING, 10, 5)
                .expect("dist-plo-ii", EntryEvent.Type.INSERTING, 5, 5);
            // don't.expect("dist-plo-ii", EntryEvent.Type.any(), 10, any());
            Expectations expectPost = new Expectations(/*fCheckEventType*/ true)
                .expect("dist-plo-i",  EntryEvent.Type.INSERTED, 5, 5)
                .expect("dist-plo-i",  EntryEvent.Type.INSERTED, 10, 5)
                .expect("dist-plo-ii", EntryEvent.Type.INSERTED, 5, 5);
            // don't.expect("dist-plo-ii", EntryEvent.Type.any(), 10, any());

            // update the interceptor with the expected results allowing it
            // to perform the assertion correctly
            service.query(new ExpectationsInformerInvocable(TxnAssertingInterceptor.IDENTIFIER)
                    .addExpectation(TransactionEvent.Type.COMMITTING, expectPre)
                    .addExpectation(TransactionEvent.Type.COMMITTED, expectPost),
                    setRecipients);

            // perform three inserts as prescribed by the map of expectations
            // above
            cachei.invoke(5, new RippleProcessor(5, 10, cacheii.getCacheName()));

            // assert stored results
            assertEquals(5, cachei.get(5));
            assertEquals(5, cachei.get(10));
            assertEquals(5, cacheii.get(5));

            // check for any remote assertion failures held by the interceptors
            Eventually.assertThat(invoking(new EventTestHelper()).remoteFail(TxnAssertingInterceptor.IDENTIFIER, getFactory(), setRecipients), is(2));
            remoteReset(TxnAssertingInterceptor.IDENTIFIER, getFactory(), setRecipients);
            }
        finally
            {
            cachei.destroy();
            cacheii.destroy();
            }
        }

    /**
     * Test to ensure OOB events (unsolicited from PartitionedCache) are raised
     * separate from TransactionEvents.
     * <p>
     * In addition, verify that OOB synthetic mutations (eviction / expiry)
     * are exposed as {@link BinaryEntry#isSynthetic() synthetic}.
     *
     * @throws InterruptedException
     */
    @Test
    public void testTxnEventsOOB()
            throws InterruptedException
        {
        NamedCache cachei  = getNamedCache("dist-plo-i");
        NamedCache cacheii = getNamedCache("dist-plo-ii");
        InvocationService service = (InvocationService)
                getFactory().ensureService(EventTestHelper.INVOCATION_SERVICE_NAME);
        Set<Member> setRecipients = service.getInfo().getServiceMembers();

        if (!STORAGE_ENABLED)
            {
            setRecipients.remove(service.getCluster().getLocalMember());
            }
        try
            {
            Expectations expectPre = new Expectations(/*fCheckEventType*/ true)
                .expect("dist-plo-i", EntryEvent.Type.INSERTING, 5, 5);
            Expectations expectPost = new Expectations(/*fCheckEventType*/ true)
                .expect("dist-plo-i", EntryEvent.Type.INSERTED, 5, 5);
            Expectations expectPostOOB = new Expectations(/*fCheckEventType*/ true)
                .expect("dist-plo-i", EntryEvent.Type.INSERTED, 10, 5);

            // update the interceptor with the expected results allowing it
            // to perform the assertion correctly
            ExpectationsInformerInvocable invocable = new ExpectationsInformerInvocable(TxnAssertingInterceptor.IDENTIFIER)
                    .addExpectation(Type.COMMITTING, expectPre)
                    .addExpectation(Type.COMMITTED, expectPost)
                    .addExpectation(UnsolicitedCommitEvent.Type.COMMITTED, expectPostOOB);
            service.query(invocable, setRecipients);

            // insert 5->5 which should result in 2 entries 5->5 & 10->5
            cachei.invoke(5, new UnsolicitedPutProcesser(10, 5));

            // assert stored results
            assertEquals(5, cachei.get(5));
            assertEquals(5, cachei.get(10));

            // check for any remote assertion failures held by the interceptors
            // Note: we expect 3 events, COMMITTING, COMMITTED, COMMITTED(OOB)
            Eventually.assertThat(invoking(new EventTestHelper()).remoteFail(TxnAssertingInterceptor.IDENTIFIER, getFactory(), setRecipients), is(3));
            remoteReset(TxnAssertingInterceptor.IDENTIFIER, getFactory(), setRecipients);


            // test that an eviction results in a synthetic OOBEvent

            invocable = new ExpectationsInformerInvocable(TxnAssertingInterceptor.IDENTIFIER)
                    .addExpectation(UnsolicitedCommitEvent.Type.COMMITTED,
                            new Expectations(/*fCheckEventType*/ true)
                                .expect("dist-plo-i", EntryEvent.Type.REMOVED, 10, null));
            service.query(invocable, setRecipients);

            cachei.invoke(5, new EvictingProcesser(10));

            assertFalse(cachei.containsKey(10));

            // wait for post events on the event dispatcher thread to fire
            Eventually.assertThat(invoking(new EventTestHelper()).remoteFail(TxnAssertingInterceptor.IDENTIFIER, getFactory(), setRecipients), is(1));
            remoteReset(TxnAssertingInterceptor.IDENTIFIER, getFactory(), setRecipients);
            }
        finally
            {
            cachei.destroy();
            cacheii.destroy();
            }
        }

    // ----- inner class: RippleProcessor -----------------------------------

    public static class RippleProcessor
            extends AbstractProcessor
            implements Serializable
        {

        // ----- constructors -----------------------------------------------

        public RippleProcessor()
            {
            }

        public RippleProcessor(Object oValue, Object oAssociatedKey, String sAssociatedCache)
            {
            m_oValue           = oValue;
            m_oAssociatedKey   = oAssociatedKey;
            m_sAssociatedCache = sAssociatedCache;
            }

        // ----- AbstractProcessor methods ----------------------------------

        @Override
        public Object process(Entry entry)
            {
            Object                   oValue   = m_oValue;
            BinaryEntry              binEntry = (BinaryEntry) entry;
            BackingMapContext        ctxMap   = binEntry.getBackingMapContext();
            BackingMapManagerContext ctxMgr   = ctxMap.getManagerContext();

            BackingMapContext ctxAssocMap = ctxMgr.getBackingMapContext(m_sAssociatedCache);
            Binary            binKeyAssoc = (Binary) ctxMgr.getKeyToInternalConverter().convert(m_oAssociatedKey);

            // insert 3 entries this-cache{key-value, associated-key-value} associated-cache{key-value}

            binEntry.setValue(oValue);
            ctxMap.getBackingMapEntry(binKeyAssoc).setValue(oValue);
            ctxAssocMap.getBackingMapEntry(binEntry.getBinaryKey()).setValue(oValue);

            // enlist another entry but do not cause a mutation; it should not
            // be shown via the transaction event
            ctxAssocMap.getBackingMapEntry(binKeyAssoc);

            return null;
            }

        // ----- data members -----------------------------------------------

        protected Object m_oValue;
        protected Object m_oAssociatedKey;
        protected String m_sAssociatedCache;
        }

    // ----- inner class: ModAssociator -------------------------------------

    public static class ModAssociator
            extends DefaultKeyAssociator
        {

        // ----- constructors -----------------------------------------------

        public ModAssociator()
            {
            this(5);
            }

        public ModAssociator(int nMod)
            {
            m_nMod = nMod;
            }

        // ----- DefaultKeyAssociator methods -------------------------------

        @Override
        public Object getAssociatedKey(Object o)
            {
            if (o instanceof Integer)
                {
                int nKey = (Integer) o;
                return nKey % m_nMod == 0 ? m_nMod : null;
                }
            return null;
            }

        protected final int m_nMod;
        }

    // ----- inner class: UnsolicitedUpdater --------------------------------

    public static class UnsolicitedPutProcesser
            extends AbstractProcessor
        {
        public UnsolicitedPutProcesser() {}

        public UnsolicitedPutProcesser(Object oKey, Object oValue)
            {
            m_oKey   = oKey;
            m_oValue = oValue;
            }

        // ----- AbstractProcessor methods ----------------------------------

        @Override
        public Object process(Entry entry)
            {
            BinaryEntry binEntry = (BinaryEntry) entry;

            binEntry.setValue(m_oValue);

            binEntry.getBackingMap().put(
                    binEntry.getContext().getKeyToInternalConverter().convert(m_oKey),
                    binEntry.getBinaryValue());

            return null;
            }

        protected Object m_oKey;
        protected Object m_oValue;
        }

    // ----- inner class: EvictingProcessor ---------------------------------

    public static class EvictingProcesser
            extends AbstractProcessor
        {
        public EvictingProcesser() {}

        public EvictingProcesser(Object oKey)
            {
            m_oKey = oKey;
            }

        // ----- AbstractProcessor methods ----------------------------------

        @Override
        public Object process(Entry entry)
            {
            BinaryEntry binEntry    = (BinaryEntry) entry;
            Binary      binKeyEvict = (Binary) binEntry.getContext().
                    getKeyToInternalConverter().convert(m_oKey);

            ((ConfigurableCacheMap) binEntry.getBackingMap()).evict(binKeyEvict);

            return null;
            }

        protected Object m_oKey;
        }


    // ----- data members ---------------------------------------------------

    /**
     * The Cache config file to use for these tests.
     */
    public static final String CFG_FILE = "txn-server-cache-config.xml";

    /**
     * Whether the local member is storage enabled.
     */
    public static boolean STORAGE_ENABLED = false;
    }
