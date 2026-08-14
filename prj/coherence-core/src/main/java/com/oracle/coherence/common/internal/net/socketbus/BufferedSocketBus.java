/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.internal.net.socketbus;

import com.oracle.coherence.common.internal.util.HeapDump;
import com.oracle.coherence.common.io.BufferSequence;
import com.oracle.coherence.common.io.BufferManager;
import com.oracle.coherence.common.io.Buffers;
import com.oracle.coherence.common.net.exabus.EndPoint;
import com.oracle.coherence.common.net.exabus.util.UrlEndPoint;
import com.oracle.coherence.common.net.exabus.util.SimpleEvent;
import com.oracle.coherence.common.net.exabus.Event;
import com.oracle.coherence.common.util.Duration;
import com.oracle.coherence.common.util.MemorySize;
import com.oracle.coherence.common.util.SafeClock;

import java.io.DataInput;
import java.io.IOException;
import java.nio.ByteBuffer;

import java.util.Arrays;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import java.net.SocketException;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;

import java.util.zip.CRC32;


/**
 * BufferedSocketBus adds write buffering to the AbstractSocketBus.
 *
 * @author mf  2010.12.1
 */
public abstract class BufferedSocketBus
        extends AbstractSocketBus
    {
    /**
     * Enables lightweight per-connection performance tracing for socket bus write progression.
     */
    protected static final boolean PERF_TRACE = Boolean.getBoolean("coherence.socketbus.perf.trace");

    /**
     * Minimum interval between periodic performance snapshots.
     */
    protected static final long PERF_TRACE_INTERVAL_MILLIS =
            Long.getLong("coherence.socketbus.perf.trace.intervalMillis", 5000L);

    /**
     * Backlog threshold that triggers periodic snapshots even when the interval has not elapsed.
     */
    protected static final long PERF_TRACE_THRESHOLD_BYTES =
            Long.getLong("coherence.socketbus.perf.trace.thresholdBytes", 8L * 1024L * 1024L);

    /**
     * Diagnostic interval for reporting a ready connection with queued bytes and no visible progress owner.
     * A non-positive value disables the reporting-only watchdog.
     */
    protected static final long LIVENESS_WATCHDOG_MILLIS = Long.getLong(
            "coherence.socketbus.liveness.watchdogMillis", 0L);

    /**
     * Whether opt-in transport progression diagnostics are enabled.
     */
    protected static final boolean PROGRESS_DIAGNOSTICS =
            PERF_TRACE || LIVENESS_WATCHDOG_MILLIS > 0L;

    /** A write-progression request is pending. */
    protected static final int WRITE_PROGRESSION_PENDING = 1;

    /** At least one pending progression request permits an application-thread socket write. */
    protected static final int WRITE_PROGRESSION_SOCKET_WRITE = 1 << 1;

    /** At least one pending progression request is an explicit, non-auto flush. */
    protected static final int WRITE_PROGRESSION_EXPLICIT = 1 << 2;

    /**
     * Return the number of write wakeups dropped after a controlled partial write.
     *
     * @return the number of dropped write wakeups
     */
    public static long getDroppedWriteWakeupsForTesting()
        {
        return TEST_DROPPED_WRITE_WAKEUPS.get();
        }

    /**
     * Return the queued bytes captured when the write wakeup was dropped.
     *
     * @return the captured queued bytes
     */
    public static long getQueuedBytesAtDroppedWriteWakeupForTesting()
        {
        return TEST_QUEUED_BYTES_AT_DROPPED_WRITE_WAKEUP.get();
        }

    /**
     * Re-register the connection whose write wakeup was dropped by the functional test hook.
     *
     * @return {@code true} iff a connection was nudged
     */
    public static boolean nudgeDroppedWriteWakeupForTesting()
        {
        Runnable task = TEST_DROPPED_WRITE_WAKEUP_NUDGE.getAndSet(null);
        if (task == null)
            {
            return false;
            }
        task.run();
        return true;
        }

    /**
     * Return true iff the deliberately dropped wakeup still has an unused
     * external nudge available.
     *
     * @return true iff the test nudge has not been consumed
     */
    public static boolean isDroppedWriteWakeupNudgePendingForTesting()
        {
        return TEST_DROPPED_WRITE_WAKEUP_NUDGE.get() != null;
        }

    /**
     * Return the number of queued-write continuations handed from a producer
     * back to the transport owner lane.
     *
     * @return the number of non-owner queued-write handoffs
     */
    public static long getNonOwnerQueuedWriteHandoffsForTesting()
        {
        return TEST_NON_OWNER_QUEUED_WRITE_HANDOFFS.get();
        }

    /**
     * Return the number of write-progression requests deferred behind an active epoch writer.
     *
     * @return the number of deferred progression requests
     */
    public static long getDeferredWriteProgressionRequestsForTesting()
        {
        return TEST_DEFERRED_WRITE_PROGRESSION_REQUESTS.get();
        }

    /**
     * Return the number of deferred write-progression requests handed to a subsequent epoch writer.
     *
     * @return the number of deferred progression handoffs
     */
    public static long getDeferredWriteProgressionHandoffsForTesting()
        {
        return TEST_DEFERRED_WRITE_PROGRESSION_HANDOFFS.get();
        }

    /**
     * Return the number of receipt-deadline flushes that also carried pending application data.
     *
     * @return the number of receipt/application piggyback flushes
     */
    public static long getReceiptFlushesWithPendingDataForTesting()
        {
        return TEST_RECEIPT_FLUSHES_WITH_PENDING_DATA.get();
        }

    /**
     * Return the number of incomplete socket writes observed while the real-backpressure test hook is enabled.
     *
     * @return the number of incomplete socket writes
     */
    public static long getSocketBackpressurePartialWritesForTesting()
        {
        return TEST_SOCKET_BACKPRESSURE_PARTIAL_WRITES.get();
        }

    /**
     * Return the maximum outstanding bytes observed during an incomplete socket write.
     *
     * @return the maximum outstanding bytes
     */
    public static long getSocketBackpressureOutstandingBytesForTesting()
        {
        return TEST_SOCKET_BACKPRESSURE_OUTSTANDING_BYTES.get();
        }

    /**
     * Return the bytes written after genuine socket backpressure was observed.
     *
     * @return the bytes written after backpressure
     */
    public static long getBytesWrittenAfterSocketBackpressureForTesting()
        {
        return TEST_BYTES_WRITTEN_AFTER_SOCKET_BACKPRESSURE.get();
        }

    /**
     * Return the latest socket-write state captured by the real-backpressure test hook.
     *
     * @return the latest socket-write state
     */
    public static String getSocketBackpressureStateForTesting()
        {
        BufferedConnection connection = TEST_SOCKET_BACKPRESSURE_CONNECTION.get();
        return connection == null
                ? TEST_SOCKET_BACKPRESSURE_STATE.get()
                : TEST_SOCKET_BACKPRESSURE_STATE.get()
                    + ", currentQueued=" + connection.f_cbQueued.get()
                    + ", currentWriterActive=" + connection.isWriterActive()
                    + ", currentWriterEpoch=" + System.identityHashCode(connection.m_epochWriterActive.get())
                    + ", currentEpoch=" + System.identityHashCode(connection.m_transportEpoch)
                    + ", progressionRequest=" + connection.m_nWriteProgressionRequest.get()
                    + ", processWriteCallsAfterBackpressure=" + TEST_PROCESS_WRITE_CALLS_AFTER_BACKPRESSURE.get()
                    + ", writerBusyCallsAfterBackpressure=" + TEST_WRITER_BUSY_CALLS_AFTER_BACKPRESSURE.get();
        }

    // ----- constructors ---------------------------------------------------

    /**
     * Construct a BufferedSocketMessageBus.
     *
     * @param driver      the socket driver
     * @param pointLocal  the local endpoint
     *
     * @throws IOException if an I/O error occurs
     */
    public BufferedSocketBus(SocketBusDriver driver, UrlEndPoint pointLocal)
            throws IOException
        {
        super(driver, pointLocal);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onOpen()
        {
        final long cMillisFlush = f_driver.getDependencies().getMaximumReceiptDelayMillis();
        if (cMillisFlush > 0)
            {
            scheduleTask(new Runnable()
                {
                @Override
                public void run()
                    {
                    // reschedule self for next periodic flush
                    scheduleTask(this, cMillisFlush);

                    for (Connection conn : getRegisteredConnections())
                        {
                        if (((BufferedConnection) conn).isReceiptFlushRequired())
                            {
                            ((BufferedConnection) conn).flushPendingReceipts();
                            }
                        }
                    }
                }, cMillisFlush);
            }

        super.onOpen();
        }


    // ----- BufferedConnection interface ----------------------------------

    /**
     * BufferedConnection implements a reliable stream connection with
     * I/O offloading.
     */
    public abstract class BufferedConnection
        extends Connection
        {
        /**
         * Create a BufferedConnection for the specified peer.
         *
         * @param peer  the peer
         */
        public BufferedConnection(UrlEndPoint peer)
            {
            super(peer);
            }

        /**
         * Return the threshold at which a send operation should perform
         * an auto-flush of the unflushed write batch.
         *
         * @return  the threshold in bytes
         */
        protected long getAutoFlushThreshold()
            {
            long cb = m_cbAutoFlushThreshold;

            if (cb <= 0)
                {
                try
                    {
                    // goal is to avoid engaging the selection service writer
                    // or more specifically avoid flip-flopping between using
                    // it and not using it.  If we allow our buffered data
                    // to exceed the underlying buffer size then a write is
                    // likely to engage it, so we start writing at the point
                    // that it would only be partially full.  Obviously we
                    // don't want to do micro writes either, so we still want
                    // to be using a decent portion of the buffer before
                    // writing
                    // TODO: consider continuing to auto-resize the threshold based on how
                    // often an flush fails to fully flush the buffer
                    cb = f_driver.getDependencies().getAutoFlushThreshold();
                    if (cb <= 0)
                        {
                        cb = Math.min(getPacketSize() * 9, // try for >90% packet utilization
                                      getSendBufferSize() / 4); // ensure we don't fill tx buffer
                        }
                    m_cbAutoFlushThreshold = cb;
                    }
                catch (SocketException e) {}

                if (cb <= 0)
                    {
                    // not yet connected
                    cb = 64 * 1024;
                    }
                }

            return cb;
            }

        /**
         * Return the threshold at which forced acks of pending receipts is
         * requested from the peer.
         *
         * @return  the threshold in bytes
         */
        protected long getForceAckThreshold()
            {
            long cb = m_cbForceAckThreshold;

            if (cb <= 0)
                {
                cb = f_driver.getDependencies().getReceiptRequestThreshold();

                try
                    {
                    if (cb <= 0)
                        {
                        cb = getSendBufferSize() * 3;
                        }
                    m_cbForceAckThreshold = cb;
                    }
                catch (SocketException e) {}

                if (cb <= 0)
                    {
                    // not yet connected
                    cb = 3 * 64 * 1024;
                    }
                }

            return cb;
            }

        /**
         * Return the threshold at which to declare backlog
         *
         * @return  the threshold in bytes
         */
        protected long getBacklogExcessiveThreshold()
            {
            long cb = m_cbBacklogExcessiveThreshold;

            if (cb <= 0)
                {
                try
                    {
                    m_cbBacklogExcessiveThreshold = cb = getSendBufferSize();
                    }
                catch (SocketException e) {}

                if (cb <= 0)
                    {
                    // not yet connected
                    cb = 1024 * 1024;
                    }
                }

            return cb;
            }

        @Override
        public BufferedConnection ensureValid()
            {
            super.ensureValid();
            return this;
            }

        /**
         * Return the number of concurrently executing writers.
         *
         * @return the number of concurrently executing writers.
         */
        protected int getConcurrentWriters()
            {
            // estimate how contended this connection is by including all threads which contributed to this batch
            // as well as any threads actively preparing/enqueuing messages

            return m_cWritersActive.get() + m_cWritersBatch;
            }

        /**
         * Attempt to become the active writer for this connection.
         *
         * @return true iff the caller became the active writer
         */
        protected boolean tryActivateWriter(String sReason)
            {
            return tryActivateWriter(getReadyTransportEpoch(), sReason, /*fRequireReady*/ true);
            }

        /**
         * Attempt to acquire the owner ticket for a specific transport epoch.
         *
         * @param epoch          the epoch that will own the progression pass
         * @param sReason        the ownership reason
         * @param fRequireReady  true iff the epoch must still be READY
         *
         * @return true iff the caller acquired the owner ticket
         */
        protected boolean tryActivateWriter(TransportEpoch epoch, String sReason, boolean fRequireReady)
            {
            if (epoch == null || !m_epochWriterActive.compareAndSet(null, epoch))
                {
                return false;
                }

            if (m_transportEpoch != epoch ||
                fRequireReady && epoch.m_phase != TransportPhase.READY)
                {
                m_epochWriterActive.compareAndSet(epoch, null);
                return false;
                }

            if (PROGRESS_DIAGNOSTICS)
                {
                m_sWriterOwner      = Thread.currentThread().getName();
                m_sWriterReason     = sReason;
                m_ldtWriterAcquired = SafeClock.INSTANCE.getSafeTimeMillis();
                }
            return true;
            }

        /**
         * Relinquish active writer ownership.
         */
        protected void deactivateWriter()
            {
            deactivateWriter(m_epochWriterActive.get());
            }

        /**
         * Relinquish ownership only if the ticket still belongs to the
         * specified epoch. A stale command must never clear a newer epoch's
         * scheduling duty.
         */
        protected void deactivateWriter(TransportEpoch epoch)
            {
            if (epoch == null || !m_epochWriterActive.compareAndSet(epoch, null))
                {
                return;
                }
            if (PROGRESS_DIAGNOSTICS)
                {
                m_sWriterOwner      = null;
                m_sWriterReason     = null;
                m_ldtWriterAcquired = 0L;
                }
            }

        /**
         * Return true iff a writer is currently active for this connection.
         *
         * @return true iff a writer is currently active
         */
        protected boolean isWriterActive()
            {
            return m_epochWriterActive.get() != null;
            }

        @Override
        protected void prepareMigrationOnOwner(TransportEpoch epochRetired)
            {
            // The retired epoch's SelectionService lane already serializes this
            // transition with every transport consumer.  Relinquish its
            // scheduling ticket so a queued stale command cannot prevent the
            // replacement handshake from starting.  Exact-epoch CAS prevents
            // that command from clearing a later epoch's duty when it runs.
            deactivateWriter(epochRetired);
            }

        /**
         * Schedule an already-owned consumer write-progression pass on the SelectionService thread.
         *
         * @param fSocketWrite  true if the consumer may offer cpu to socket writes
         * @param fAuto         true iff this pass was scheduled by auto-flush style coordination
         */
        protected void scheduleActiveWriteProgression(boolean fSocketWrite, boolean fAuto)
            {
            TransportEpoch epoch = m_epochWriterActive.get();
            if (epoch == null)
                {
                throw new IllegalStateException("write progression scheduled without an epoch owner");
                }

            boolean fTrackTiming = PROGRESS_DIAGNOSTICS || isTransportMetricsTrackedForTesting();
            long    ldtScheduled = fTrackTiming ? System.nanoTime() : 0L;
            try
                {
                invokeTransport(epoch, new Runnable()
                    {
                    @Override
                    public void run()
                        {
                        if (fTrackTiming)
                            {
                            long cDelay = System.nanoTime() - ldtScheduled;
                            if (PROGRESS_DIAGNOSTICS)
                                {
                                m_cSelectorTasks++;
                                m_cSelectorTaskDelayLastNanos = cDelay;
                                m_cSelectorTaskDelayMaxNanos  = Math.max(m_cSelectorTaskDelayMaxNanos, cDelay);
                                }
                            recordSelectorTaskDelayForTesting(ldtScheduled);
                            }
                        enterTransportCallback(BufferedConnection.this, epoch);
                        try
                            {
                            processQueuedWritesOnSelectionThread(epoch, fSocketWrite, fAuto);
                            }
                        finally
                            {
                            exitTransportCallback();
                            }
                        }
                    });
                }
            catch (RuntimeException e)
                {
                deactivateWriter(epoch);
                throw e;
                }
            }

        /**
         * Coordinate consumer-owned write progression without directly performing any write-state work.
         *
         * @param fSocketWrite  true if the consumer may offer cpu to socket writes
         * @param fAuto         true iff this coordination request originated from auto-flush logic
         *
         * @return true iff no producer-published work remains waiting to be absorbed
         */
        protected boolean requestWriteProgression(boolean fSocketWrite, boolean fAuto)
            {
            if (tryActivateWriter(fAuto ? "auto-flush" : "flush"))
                {
                tracePerf("schedule", false);
                scheduleActiveWriteProgression(fSocketWrite, fAuto);
                }
            else
                {
                // Publish only after losing the optimistic ticket so the uncontended path pays no additional CAS.
                // Retry acquisition after publication to close the race with an owner that relinquished its ticket
                // between our first attempt and the publication.
                publishWriteProgressionRequest(fSocketWrite, fAuto);
                if (TEST_TRACK_DEFERRED_WRITE_PROGRESSION)
                    {
                    TEST_DEFERRED_WRITE_PROGRESSION_REQUESTS.incrementAndGet();
                    }
                if (tryActivateWriter(fAuto ? "deferred-auto-flush" : "deferred-flush"))
                    {
                    tracePerf("schedule-deferred", false);
                    scheduleActiveWriteProgression(fSocketWrite, fAuto);
                    }
                }

            return !hasProducerWriteWork();
            }

        /**
         * Publish a progression request before attempting to acquire the epoch writer ticket. This closes the
         * lost-wakeup window in which an explicit flush or receipt flush raced an active writer but had no MPSC
         * publication or queued socket bytes for the active writer's final recheck to observe.
         */
        protected void publishWriteProgressionRequest(boolean fSocketWrite, boolean fAuto)
            {
            int nRequest = WRITE_PROGRESSION_PENDING
                    | (fSocketWrite ? WRITE_PROGRESSION_SOCKET_WRITE : 0)
                    | (fAuto ? 0 : WRITE_PROGRESSION_EXPLICIT);
            int nCurrent = m_nWriteProgressionRequest.get();
            while ((nCurrent & nRequest) != nRequest)
                {
                if (m_nWriteProgressionRequest.compareAndSet(nCurrent, nCurrent | nRequest))
                    {
                    return;
                    }
                nCurrent = m_nWriteProgressionRequest.get();
                }
            }

        /**
         * Return true iff there is producer-published work that still needs to be absorbed by the consumer.
         *
         * @return true iff there is producer-published work pending
         */
        protected boolean hasProducerWriteWork()
            {
            return false;
            }

        /**
         * Drain producer-published work into consumer-owned batch state.
         *
         * @return the number of drained bytes
         */
        protected long absorbPublishedWrites()
            {
            long cbDrained = 0;
            long cbBatch;
            while ((cbBatch = drainQueue()) > 0)
                {
                cbDrained += cbBatch;
                }
            return cbDrained;
            }

        /**
         * Append any pending receipt bookkeeping into the current consumer-owned build batch.
         *
         * @param batch  the current build batch, or {@code null}
         *
         * @return the consumer-owned build batch after receipt bookkeeping has been applied
         */
        protected WriteBatch appendPendingReceiptMessages(WriteBatch batch)
            {
            SocketBusDriver.Dependencies deps = getSocketDriver().getDependencies();

            int cRecReq = m_cReceiptsUnflushed;
            int cRecRet = m_cReceiptsReturn.getAndSet(0);

            // determine if we need to ask for forced acks
            long cUnackedBytes = f_cBytesUnacked.get();
            if (cRecReq > 0 && cUnackedBytes > getForceAckThreshold())
                {
                cRecReq = -cRecReq; // negative receipts indicates forced acks
                f_cBytesUnacked.set(0); // reset counter to avoid requesting multiple forced acks
                }

            // insert receipt message if necessary
            if (cRecReq != 0 || cRecRet != 0 || m_fIdle)
                {
                m_fIdle = false;
                if (batch == null)
                    {
                    m_batchWriteUnflushed = batch = new WriteBatch();
                    }

                int        cbReceipt        = getReceiptSize();
                ByteBuffer bufferMsgReceipt = m_bufferRecycleOutboundReceipts;
                if (bufferMsgReceipt == null)
                    {
                    bufferMsgReceipt = m_bufferRecycleOutboundReceipts = deps.getBufferManager().acquire(cbReceipt * 1024);
                    int cbCap = bufferMsgReceipt.capacity();
                    bufferMsgReceipt.limit(cbCap - (cbCap % cbReceipt));
                    }

                if (bufferMsgReceipt.remaining() > cbReceipt)
                    {
                    ByteBuffer buffReceipt = bufferMsgReceipt.slice();
                    writeMsgReceipt(buffReceipt, cRecReq, cRecRet);
                    bufferMsgReceipt.position(bufferMsgReceipt.position() + cbReceipt);
                    batch.append(buffReceipt, /*fRecycle*/ false, /*body*/ null, /*receipt*/ cRecReq == 0 ? null : RECEIPT_ACKREQ_MARKER);
                    }
                else // bufferMsgReceipt.remaining() == MSG_RECEIPT_SIZE
                    {
                    // write last chunk and recycle
                    bufferMsgReceipt.mark();
                    writeMsgReceipt(bufferMsgReceipt, cRecReq, cRecRet);
                    batch.append(bufferMsgReceipt, /*fRecycle*/ true, /*body*/ null, /*receipt*/ cRecReq == 0 ? null : RECEIPT_ACKREQ_MARKER_RECYCLE);
                    m_bufferRecycleOutboundReceipts = null;
                    }
                m_cReceiptsUnflushed = 0;
                }

            return batch;
            }

        /**
         * Detach the current consumer-owned build batch from the unflushed slot.
         *
         * @return the detached build batch, or {@code null}
         */
        protected WriteBatch detachUnflushedWriteBatch()
            {
            WriteBatch batch = m_batchWriteUnflushed;
            if (batch != null)
                {
                m_batchWriteUnflushed = null;
                m_cWritersBatch       = 0;
                m_lWritersBatchBitSet = 0;
                }
            return batch;
            }

        /**
         * Publish a fully initialized batch at the tail of the resend chain.
         *
         * The normal consumer path constructs and links a batch on the
         * SelectionService lane. A producer-owned direct path must initialize
         * the batch before publishing the volatile {@code next} edge so that
         * concurrent receipt processing cannot observe an empty node.
         *
         * @param batch  the initialized, unlinked batch
         */
        protected void linkInitializedWriteBatch(WriteBatch batch)
            {
            m_batchWriteTail = m_batchWriteTail.m_next = batch;
            }

        /**
         * Run the consumer-owned queued-write processing step on the SelectionService thread.
         *
         * @param fSocketWrite  true if the consumer may offer cpu to socket writes
         */
        protected void processQueuedWritesOnSelectionThread(TransportEpoch epoch, boolean fSocketWrite, boolean fAuto)
            {
            try
                {
                // This command is bound to epoch.f_channel and therefore runs on the same owner lane as the
                // epoch's read/write callback. A stale command performs no consumer-state mutation.
                if (!isValid() || !isCurrentTransportEpoch(epoch, TransportPhase.READY))
                    {
                    return;
                    }

                int nRequest = m_nWriteProgressionRequest.get();
                if (nRequest != 0)
                    {
                    nRequest = m_nWriteProgressionRequest.getAndSet(0);
                    }
                fSocketWrite |= (nRequest & WRITE_PROGRESSION_SOCKET_WRITE) != 0;
                fAuto &= (nRequest & WRITE_PROGRESSION_EXPLICIT) == 0;

                boolean fFlushPending = isFlushable(this);
                if (progressQueuedWrites(epoch, fSocketWrite, fAuto))
                    {
                    if (fFlushPending)
                        {
                        removeFlushable(this);
                        }
                    }
                else if (!fFlushPending)
                    {
                    addFlushable(this);
                    }
                }
            finally
                {
                completeWriteProgression(epoch, fSocketWrite);
                }
            }

        /**
         * Relinquish a completed write-progression pass and transfer any work
         * that raced the pass to the current epoch owner.
         *
         * @param epoch         the epoch that owned the completed pass
         * @param fSocketWrite  true if a follow-up consumer may offer cpu to socket writes
         */
        protected void completeWriteProgression(TransportEpoch epoch, boolean fSocketWrite)
            {
            deactivateWriter(epoch);

            TransportEpoch epochReady = getReadyTransportEpoch();
            int            nRequest    = m_nWriteProgressionRequest.get();
            if (epochReady != null
                    && (nRequest != 0 || hasProducerWriteWork())
                    && tryActivateWriter(epochReady,
                            epochReady == epoch ? "producer-follow-up" : "epoch-handoff",
                            /*fRequireReady*/ true))
                {
                if (nRequest != 0 && TEST_TRACK_DEFERRED_WRITE_PROGRESSION)
                    {
                    TEST_DEFERRED_WRITE_PROGRESSION_HANDOFFS.incrementAndGet();
                    }
                // Producer publications that arrive after this pass are handled as follow-up background work.
                // If this command belonged to a retired epoch, the same check transfers the scheduling duty to
                // the replacement instead of letting the stale command clear the only wakeup.
                scheduleActiveWriteProgression(
                        fSocketWrite || (nRequest & WRITE_PROGRESSION_SOCKET_WRITE) != 0,
                        nRequest == 0 || (nRequest & WRITE_PROGRESSION_EXPLICIT) == 0);
                }
            else if (epochReady != null && f_cbQueued.get() > 0)
                {
                if (epochReady != epoch || !isTransportOwner(this, epochReady))
                    {
                    // A stale command may be running on the former owner lane. A producer-owned direct pass is
                    // also outside the SelectionService lane. Either caller can request a callback on the current
                    // owner, but must not advance the owner-only send/resend chain itself.
                    if (epochReady == epoch && TEST_TRACK_NON_OWNER_QUEUED_WRITE_HANDOFFS)
                        {
                        TEST_NON_OWNER_QUEUED_WRITE_HANDOFFS.incrementAndGet();
                        }
                    try
                        {
                        wakeup(epochReady);
                        }
                    catch (IOException e)
                        {
                        onException(epochReady.f_lId, e);
                        }
                    }
                else
                    {
                    // If this pass queued socket writes but there is no further producer work, drive the queued
                    // writes immediately instead of waiting for a future OP_WRITE callback that may never come.
                    // processWrites() performs its own writer activation; pre-activating here can strand the
                    // owner ticket and turn the follow-up into a no-op.
                    try
                        {
                        int nInterest = processWrites(/*fReady*/ false, epochReady);
                        if ((nInterest & OP_WRITE) != 0)
                            {
                            wakeup(epochReady);
                            }
                        }
                    catch (IOException e)
                        {
                        onException(epochReady.f_lId, e);
                        }
                    }
                }
            }

        /**
         * Extracted post "send" logic.
         * <p/>
         * The caller must hold the lock on this connection. The parameter ordering allows the caller to inline
         * the computation of each parameter.
         *
         * @param fFlushInProg   true if a flush was in progress before the send
         * @param fFlushPending  true if a flush was required before the send
         * @param cbPending      the number of bytes pending on the connection
         * @param fSocketWrite   true if the caller is willing to offer its cpu to perform a socket write
         */
        protected void evaluateAutoFlush(boolean fFlushInProg, boolean fFlushPending, long cbPending, boolean fSocketWrite)
            {
            if (!fFlushInProg)
                {
                if (cbPending > getAutoFlushThreshold())
                    {
                    if (!requestWriteProgression(fSocketWrite, /*fAuto*/ true))
                        {
                        if (!fFlushPending)
                            {
                            addFlushable(this);
                            }
                        // else; a flush was already pending no need to add again
                        }
                    else if (fFlushPending)
                        {
                        // flush indicated there is nothing left; and we were previously in the flush-set; do cleanup
                        removeFlushable(this);
                        }
                    }
                else if (!fFlushPending)
                    {
                    addFlushable(this);
                    }
                // else; a flush was already pending no need to add again
                }
            // else; another thread is actively waiting on flush, no need to do anything post-send flush processing
            }

        /**
         * Add a thread to the set of concurrent writers.
         */
        protected void addWriter()
            {
            // we loosely track threads using a bitset
            long lBitId = 1L << Thread.currentThread().getId(); // % 64 is not necessary as JLS says << will do it for me
            if ((m_lWritersBatchBitSet & lBitId) == 0L)
                {
                m_lWritersBatchBitSet |= lBitId;
                ++m_cWritersBatch;
                }
            }

        /**
         * Drain any queued app-thread sends into the current unflushed batch.
         */
        protected long drainQueue()
            {
            return 0L;
            }

        /**
         * Return {@code true} if an automatically coordinated small batch should remain local to the consumer so it
         * can continue to grow before being queued to the SelectionService.
         *
         * @param batch  the current unflushed batch
         *
         * @return {@code true} to defer flushing the batch, or {@code false} to enqueue it now
         */
        protected boolean shouldDeferSmallAutoFlushBatch(WriteBatch batch)
            {
            return true;
            }

        /**
         * Send any scheduled BufferSequences.
         */
        public void flush()
            {
            flush(/*fSocketWrite*/ true);
            }

        /**
         * Send any scheduled BufferSequences.
         *
         * @param fSocketWrite  true if the caller is willing to offer its cpu to perform a socket write
         */
        public void flush(boolean fSocketWrite)
            {
            flush(fSocketWrite, /*fAuto*/ false);
            }

        /**
         * Coordinate consumer-owned flushing of any scheduled BufferSequences.
         *
         * @param fSocketWrite  true if the caller is willing to offer its cpu to perform a socket write
         * @param fAuto         true iff it is an auto-flush
         *
         * @return true iff no producer-visible queued work remains after coordination
         */
        public boolean flush(boolean fSocketWrite, boolean fAuto)
            {
            return requestWriteProgression(fSocketWrite, fAuto);
            }

        /**
         * Run one consumer-owned write progression pass.
         *
         * @param fSocketWrite  true if the consumer may offer cpu to perform a socket write
         * @param fAuto         true iff it is an auto-flush
         *
         * @return true if the connection has flushed all pending data known to the consumer
         */
        protected boolean progressQueuedWrites(TransportEpoch epoch, boolean fSocketWrite, boolean fAuto)
            {
            ++m_cProgressPasses;
            absorbPublishedWrites();
            SocketBusDriver.Dependencies deps = getSocketDriver().getDependencies();

            WriteBatch batch = appendPendingReceiptMessages(m_batchWriteUnflushed);

            int cWriter = getConcurrentWriters();
            boolean fResult;

            if (batch == null)
                {
                fResult = true; // nothing to flush
                ++m_cProgressNoWork;
                }
            else if (f_cbQueued.get() == 0 && (cWriter <= 1 || (cWriter <= deps.getDirectWriteThreadThreshold() && fSocketWrite)))
                {
                ++m_cProgressDirect;
                // SS thread isn't writing, so this is the current head.  Even if the SS thread never sees this
                // batch we want to overwrite any historic batch with the current head to avoid building up
                // garbage which otherwise wouldn't be GCable until the SS thread iterated through many empty batches
                m_batchWriteSendHead = batch;
                try
                    {
                    batch.lock();  // lock because ack can come in while we're still in batch.write
                    try
                        {
                        if (batch.write(epoch))
                            {
                            m_cWritersBatch       = 0;
                            m_lWritersBatchBitSet = 0;
                            fResult = true;
                            }
                        else
                            {
                            // we didn't write everything, fall through and evaluate if we need to enqueue the batch
                            batch = m_batchWriteUnflushed;
                            }
                        }
                    finally
                        {
                        batch.unlock();
                        }
                    }
                catch (IOException e)
                    {
                    // stream is now in an unknown state; queue the remainder and reconnect
                    detachUnflushedWriteBatch();
                    enqueueWriteBatch(batch);
                    onException(epoch.f_lId, e);
                    fResult = true; // nothing more can be done
                    long cbPostDrain = absorbPublishedWrites();
                    m_cbProgressPostDrain += cbPostDrain;
                    if (cbPostDrain > 0)
                        {
                        addFlushable(this);
                        try
                            {
                            wakeup(epoch);
                            }
                        catch (IOException eWakeup)
                            {
                            onException(epoch.f_lId, eWakeup);
                            }
                        return false;
                        }
                    return fResult;
                    }
                }
            else if (m_state == ConnectionState.DEFUNCT)
                {
                scheduleDisconnect(null); // ensure receipts are emitted ASAP
                fResult = true;
                }

            if (batch == null)
                {
                fResult = true;
                }
            else if (fAuto
                    && batch.getLength() < getBacklogExcessiveThreshold() * 2
                    && shouldDeferSmallAutoFlushBatch(batch))
                {
                fResult = false; // data remains to be flushed
                }
            else
                {
                ++m_cProgressQueued;
                // either caller explictly flushed or we're buffering a significant
                // amount, enqueue to SelectionService
                detachUnflushedWriteBatch();
                enqueueWriteBatch(batch);
                fResult = true; // euqueue'd the batch
                }

            long cbPostDrain = absorbPublishedWrites();
            m_cbProgressPostDrain += cbPostDrain;
            if (cbPostDrain > 0)
                {
                addFlushable(this);
                try
                    {
                    wakeup();
                    }
                catch (IOException e)
                    {
                    onException(m_lTransportGeneration, e);
                    }
                return false;
                }

            tracePerf("progress", false);
            return fResult;
            }


        @Override
        protected boolean heartbeat()
            {
            TransportEpoch epoch = getTransportEpoch();
            if (isTransportOwner(this, epoch))
                {
                return heartbeatOnOwner(epoch);
                }
            invoke(epoch, () -> heartbeatOnOwner(epoch));
            return epoch != null;
            }

        /**
         * Evaluate heartbeat state on the epoch owner lane.
         *
         * @param epoch  the owner token captured by the periodic request
         */
        protected boolean heartbeatOnOwner(TransportEpoch epoch)
            {
            if (m_transportEpoch != epoch)
                {
                return false;
                }

            boolean fResult = false;
            if (m_cbWrite        == m_cbHeartbeatLast &&    // we've sent nothing since the last check
                f_cbQueued.get() == 0 &&                    // we have no outbound traffic queued up
                isCurrentTransportEpoch(epoch, TransportPhase.READY)) // COH-25350 - messages can be sent on the connection
                {
                // prevent the network infrastructure from closing the idle socket

                // setting m_fIdle to true will cause the next flush to minimally send an empty receipt
                // flush will also reset idle
                // some writes are seen on the socket when progression is requested
                m_fIdle = true;
                requestWriteProgression(/*fSocketWrite*/ false, /*fAuto*/ false);
                fResult = true;
                }
            // else; the connection has seen writes since the last heartbeat check, no action required

            m_cbHeartbeatLast = m_cbWrite;
            return fResult;
            }


        /**
         * Offload the specified WriteBatch to the SelectionService for processing.
         *
         * @param batch  the WriteBatch to enqueue
         */
        public void enqueueWriteBatch(WriteBatch batch)
            {
            long cbBatch = batch.getLength();

            long cbQueuedNow = f_cbQueued.addAndGet(cbBatch);

            if (cbQueuedNow == cbBatch)
                {
                // for this to occur the just enqueued batch is at the head of the queue
                // and was not even partially written by the SelectionService. In such
                // a case the SelectionService may not be aware of it and we must
                // re-register with selection service for WRITE interest
                try
                    {
                    wakeup();
                    }
                catch (IOException e)
                    {
                    onException(m_lTransportGeneration, e);
                    }
                }

            final long cbSignalExcessive = getBacklogSignalExcessiveThreshold();
            if (cbQueuedNow > cbSignalExcessive * 2 &&
                m_fBacklogScheduled.compareAndSet(false, true))
                {
                // handle the case where the SelectionService thread doesn't wake up for any writes
                // we need to prevent an endless backlog from being formed, but also want to avoid
                // issuing this invocable in most cases, so we use an extra large limit.
                // emit BACKLOG_EXCESSIVE from SelectionService
                invoke(new Runnable()
                    {
                    public void run()
                        {
                        m_fBacklogScheduled.set(false);

                        long cbSignal = getBacklogSignalBytes();
                        if (isValid() && !m_fBacklog && cbSignal > cbSignalExcessive)
                            {
                            m_fBacklog = true;
                            emitEvent(new SimpleEvent(
                                    Event.Type.BACKLOG_EXCESSIVE,
                                    getPeer()));
                            }
                        }
                    });
                }
            }

        /**
         * Write a Receipt message to a buffer.
         *
         * @param  buff     buffer to write the receipt message
         * @param  cRecReq  number of receipts associated with the unflushed WriteBatch
         * @param  cRecRet  number of receipts to return to the peer
         *
         * @return ByteBuffer to which the receipt message was written
         */
        protected ByteBuffer writeMsgReceipt(ByteBuffer buff, int cRecReq, int cRecRet)
            {
            int nProt = getProtocolVersion();
            int nPos0 = buff.position();

            if (nProt > 4)
                {
                buff.putLong(-9)  // negative msg size indicates control message of that size
                    .position(nPos0 + 16); // skip writes for header/body crc
                }
            else
                {
                buff.putInt(-9);
                }

            buff.put(MSG_RECEIPT)
                .putInt(cRecReq)
                .putInt(cRecRet);

            buff.limit(buff.position()).position(nPos0);
            if (nProt > 4)
                {
                // backfill the message header with the header and body CRCs now that they can be computed
                populateCtrlMsgHeaderCrc(buff);
                }

            return buff;
            }

        /**
         * Process a receipt from the supplied stream
         *
         * @param in  the receipt
         *
         * @throws IOException if an IO error occurs
         */
        protected void processReceipt(DataInput in)
                throws IOException
            {
            int cReceiptsRequested = in.readInt();
            ++m_cReceiptProcessCalls;
            if (cReceiptsRequested < 0 || getSocketDriver().getDependencies().getMaximumReceiptDelayMillis() == 0)
                {
                // negative or config indicates force receipt send immediately
                m_cReceiptsRequested += cReceiptsRequested < 0 ? -((long) cReceiptsRequested) : cReceiptsRequested;
                m_cReceiptsReturn.addAndGet(cReceiptsRequested < 0 ? -cReceiptsRequested : cReceiptsRequested);

                if (isReceiptFlushRequired())
                    {
                    // honor the receipt deadline and piggyback any pending application data
                    flushPendingReceipts();
                    }
                }
            else
                {
                m_cReceiptsRequested += cReceiptsRequested;
                m_cReceiptsReturn.addAndGet(cReceiptsRequested);
                }

            int cReturned = in.readInt();
            m_cReceiptsReturned += cReturned;
            if (cReturned > 0)
                {
                EndPoint   epPeer      = getPeer();
                WriteBatch batchResend = m_batchWriteResendHead;
                WriteBatch batchNext   = batchResend.next();
                for (;;)
                    {
                    int cEmit;
                    if (isReceiptBatchLockRequired())
                        {
                        batchResend.lock();
                        try
                            {
                            cEmit = batchResend.ack(cReturned, f_aoReceiptTmp);
                            }
                        finally
                            {
                            batchResend.unlock();
                            }
                        }
                    else
                        {
                        // The normal TransportEpoch path keeps the resend/send/unflushed chain on the
                        // SelectionService lane, so no producer-side synchronization is required.
                        cEmit = batchResend.ack(cReturned, f_aoReceiptTmp);
                        }
                    cReturned -= cEmit;

                    // emit receipts outside of synchronization
                    for (int i = 0; i < cEmit; ++i)
                        {
                        Object oReceipt = f_aoReceiptTmp[i];
                        f_aoReceiptTmp[i] = null;
                        if (oReceipt != RECEIPT_NO_EMIT)
                            {
                            addEvent(new SimpleEvent(Event.Type.RECEIPT, epPeer, oReceipt));
                            ++m_cReceiptsEmitted;
                            }
                        }

                    if (cReturned == 0)
                        {
                        // wait for more receipts
                        break;
                        }
                    else if (cEmit < f_aoReceiptTmp.length)
                        {
                        // this batch must be fully ack'd; move onto next batch

                        // try to null out the resend.next ref so that we don't
                        // end up with tenured garbage referencing live data as
                        // this can force short lived objects into being tenured
                        // as well and cause long GCs.  To be here clearly this batch
                        // is fully ack'd, so we'd think we could just null out it's
                        // next ref, but it is possible that if we've recently done
                        // a migration this batch is still pending a send, i.e. we're
                        // resending it not realizing that it was already ack'd.  In
                        // such a case we can't null out next but in all other cases
                        // we can.  To identify this we check if the head of the send
                        // queue is fully ack'd if so then we're in that odd state

                        WriteBatch batchSend = m_batchWriteSendHead; // yes send not resend
                        if (batchSend.m_ofAck != batchSend.m_ofAdd)
                            {
                            // since this resend batch is fully ack'd and the send head is not
                            // full ack'd that means the resend pointer is in front of the send
                            // pointer (as is normal), and thus this resend batch is now garbage
                            // and we can null out its next pointer
                            batchResend.m_next = null;
                            }

                        batchResend = batchNext;
                        batchNext   = batchResend.next();
                        }
                    // else; more to process in current batch
                    }

                m_batchWriteResendHead = batchResend;

                // Peer has acked. Reset counter; this also serves as a volatile write to make all of the above
                // visible to checkHealth
                f_cBytesUnacked.set(0);
                }

            tracePerf("receipt", false);
            }

        /**
         * Return true iff a producer may currently mutate a batch while the
         * SelectionService processes a receipt.
         *
         * @return {@code true} if receipt acknowledgement must lock the batch
         */
        protected boolean isReceiptBatchLockRequired()
            {
            return false;
            }

        /**
         * Process a receipt from the supplied stream
         *
         * @param in  the receipt
         *
         * @throws IOException if an IO error occurs
         */
        protected void processSync(DataInput in)
                throws IOException
            {
            long cMsgOut      = m_cMsgOutDelivered;
            long cMsgIn       = m_cMsgIn; // SYNCs aren't ackable and thus aren't reflected in either side's count
            long cMsgAcked    = in.readLong();
            long cMsgReceived = in.readLong();
            byte nCmdSync     = getProtocolVersion() < 3 ? SYNC_CMD_NONE : in.readByte();

            if ((nCmdSync & SYNC_CMD_DUMP) != 0)
                {
                String sDump = HeapDump.dumpHeapForBug("Bug-27585336-tmb-migration");
                getLogger().log(makeRecord(Level.WARNING, "{0} migration with {1} appears to not be progressing on {2}; {3} collected for analysis",
                        getLocalEndPoint(), getPeer(), BufferedConnection.this, sDump));
                }

            if (cMsgAcked > cMsgIn ||     // peer got more acks then we sent to it
                cMsgOut   > cMsgReceived) //   we got more acks then it sent to us
                {
                scheduleDisconnect(new IOException("out of sync during migration in " +
                                        cMsgAcked + "/" + cMsgIn + ", out " + cMsgOut + "/" + cMsgReceived));
                }
            else
                {
                long cSkip      = m_cMsgInSkip = cMsgIn - cMsgAcked;
                long cRedeliver = cMsgReceived - cMsgOut;
                getLogger().log(makeRecord(Level.FINE,
                        "{0} synchronizing migrated connection with {1} will result in {2} skips and {3} re-deliveries: {4}",
                        getLocalEndPoint(), getPeer(), cSkip, cRedeliver, BufferedConnection.this));
                }
            }

        /**
         * Return true iff receipt bookkeeping must be flushed. Any unflushed application data is piggybacked on
         * that flush; it must not suppress the receipt deadline. In particular, MPSC auto progression can leave a
         * small consumer-owned application batch pending even though no application thread will issue another flush.
         *
         * @return true iff the connection has pending receipt bookkeeping
         */
        protected boolean isReceiptFlushRequired()
            {
            return m_cReceiptsReturn.get() > 0 || m_fIdle;
            }

        /**
         * Flush pending receipt bookkeeping, piggybacking any application data already owned by the consumer.
         */
        protected void flushPendingReceipts()
            {
            if (TEST_TRACK_RECEIPT_FLUSH_WITH_PENDING_DATA && isFlushRequired())
                {
                TEST_RECEIPT_FLUSHES_WITH_PENDING_DATA.incrementAndGet();
                }
            optimisticFlush();
            }

        /**
         * Return true there is application data pending a flush.
         *
         * @return true iff there is application data pending a flush
         */
        protected boolean isFlushRequired()
            {
            WriteBatch batch = m_batchWriteUnflushed;
            return batch != null && batch.m_ofSend < batch.m_ofAdd;
            }

        /**
         * {@inheritDoc}
         */
        public int onReadySafe(int nOps, TransportEpoch epoch)
            throws IOException
            {
            if (PROGRESS_DIAGNOSTICS)
                {
                m_nReadyOpsLast            = nOps;
                m_ldtLastSelectionCallback = SafeClock.INSTANCE.getSafeTimeMillis();
                }

            int nReadOps  = processReads((nOps & OP_READ) != 0, epoch);
            int nWriteOps = TEST_WRITE_PROGRESS_PAUSED.get()
                    ? 0
                    : processWrites((nOps & OP_WRITE) != 0, epoch);

            return m_nInterestOpsLast = nReadOps | nWriteOps;
            }

        /**
         * Re-register this connection after its write wakeup was dropped by the functional test hook.
         */
        protected void resumeDroppedWriteWakeupForTesting()
            {
            try
                {
                wakeup();
                }
            catch (IOException e)
                {
                onException(m_lTransportGeneration, e);
                }
            }

        @Override
        protected void checkHealth(long ldtNow)
            {
            TransportEpoch epoch = getTransportEpoch();
            Runnable runnable = () -> checkHealthOnOwner(ldtNow, epoch);
            if (epoch == null)
                {
                invoke(null, runnable);
                }
            else
                {
                invokeTransport(epoch, runnable);
                }
            }

        /**
         * Evaluate ACK, migration-timeout, and write-duty health on the epoch
         * owner lane.
         *
         * @param ldtNow  the time captured by the periodic health request
         * @param epoch   the owner token captured by that request
         */
        protected void checkHealthOnOwner(long ldtNow, TransportEpoch epoch)
            {
            if (m_transportEpoch != epoch)
                {
                return;
                }

            long lTransportGeneration = isCurrentTransportEpoch(epoch, TransportPhase.READY)
                    && m_state == ConnectionState.ACTIVE
                    ? epoch.f_lId
                    : -1L;
            if (lTransportGeneration < 0)
                {
                checkMigrationHandshakeTimeout(ldtNow);
                return;
                }

            checkWriteProgressInvariant(ldtNow, lTransportGeneration);

            // establish write health, i.e. we've done a write or have nothing (flushed) to write

            // m_cbWrite is a dirty read as writes may be done off the SS thread, but even
            // if they are f_cbQueued will be updated after each m_cbWrite update, thus worst case we're
            // guaranteed to see the updated m_cbWrite on our next pass.
            long    cbWrite       = m_cbWrite;
            long    cbWriteLast   = m_cbWriteLastCheck;
            boolean fWriteHealthy = cbWrite > cbWriteLast || f_cbQueued.get() == 0;

            // establish read health

            // we're unhealthy if we have an inbound receipt pending and we aren't actively making read progress.
            // note we consider reads of anything to be a good sign simply because our ack could still be coming.

            long    ldtAckTimeout    = m_ldtAckTimeout;
            long    cbRead           = m_cbRead;
            Object  oReceiptUnacked  = null;
            int     ofReceiptUnacked = 0;
            Object  batchUnacked     = null;

            f_cBytesUnacked.get(); // volatile read to allow all other read-health checks to see data recently written
                                   // by the corresponding SS thread.
            boolean fReadHealthy = true;
            if (cbRead == m_cbReadLastCheck)
                {
                // we haven't read anything, but we need to check if we should have read something.  The only thing
                // we check for are acks, so lets see if we are expecting one; scan the resend queue to see if we've
                // sent anything which requires an ack, and also sent the subsequent ack request.  We could also
                // monitor messages which have started to arrive, but then stalled, but those will naturally be protected
                // by our peer running its own health check waiting for us to send the corresponding ack, and while we
                // could monitor these, we couldn't monitor messages until they started to arrive, so the monitoring would
                // be both redundant and incomplete.
                UNHEALTHY: for (WriteBatch batchResend = m_batchWriteResendHead; batchResend != null; batchResend = batchResend.next())
                    {
                    Object[] aReceipt = batchResend.m_aReceipt;
                    int      ofSafe   = aReceipt.length; // defend against concurrent batch.append/ack
                    for (int i = Math.min(ofSafe, batchResend.m_ofAck), e = Math.min(ofSafe, batchResend.m_ofSend); i < e; ++i)
                        {
                        Object oReceipt = aReceipt[i];
                        if (oReceipt instanceof ReceiptSpanMarker)
                            {
                            ReceiptSpanMarker marker      = (ReceiptSpanMarker) oReceipt;
                            int               cSpan       = Math.min(marker.getBufferCount(), e - i);
                            Object            oMsgReceipt = cSpan <= 0
                                    ? null
                                    : aReceipt[i + cSpan - 1];

                            if (oReceiptUnacked == null)
                                {
                                if (oMsgReceipt != null)
                                    {
                                    // This message requires a protocol receipt; now search for the subsequent ack request.
                                    oReceiptUnacked  = oMsgReceipt;
                                    ofReceiptUnacked = i;
                                    batchUnacked     = batchResend;
                                    }
                                }

                            i += cSpan - 1;
                            continue;
                            }

                        if (oReceiptUnacked == null)
                            {
                            if (oReceipt != null &&
                                oReceipt != RECEIPT_MSG_MARKER &&                // don't count
                                oReceipt != RECEIPT_HEADER_RECYCLE &&            // artificial receipts
                                oReceipt != RECEIPT_MSG_MARKER_HEADER_RECYCLE && // which
                                oReceipt != RECEIPT_ACKREQ_MARKER &&             // don't
                                oReceipt != RECEIPT_ACKREQ_MARKER_RECYCLE)       // get ack'd
                                {
                                // this indicates that we've sent a message which requires an ack that we've yet
                                // to receive; but we also need to know we've sent the ack request. The ack request
                                // for this would go out in our next receipt, so start searching for that.
                                oReceiptUnacked  = oReceipt; // in coherence this will be the actual Message
                                ofReceiptUnacked = i;
                                batchUnacked     = batchResend;
                                }
                            }
                        else if (oReceipt == RECEIPT_ACKREQ_MARKER ||
                                 oReceipt == RECEIPT_ACKREQ_MARKER_RECYCLE)
                            {
                            // oReceiptUnacked is non-null thus we know we've sent something requiring an ack, and
                            // we've now seen that we've sent the ack request as well, thus our health is now suspect.
                            fReadHealthy = false;
                            break UNHEALTHY;
                            }
                        }
                    }
                }
            else
                {
                // we read something; we have read health
                fReadHealthy = true;

                // but we also need to help out our peer. if we're on a slow network and the peer has a large tx buffer
                // it's possible they finished their write long before the last packet from that write will actually
                // leave TCP's tx buffer.  The peer can't tell when that has happened and started their ack timer at
                // the point they finished writing to the tx buffer.  Since we don't want the ack timeout to need to
                // be relative to message sizes or network speed we need a way to help our peer to not declare a timeout.
                // We can do this by ensuring that maintain read health while awaiting the ack, and we can do that by
                // sending dummy data, i.e. heartbeats.  But of course we only want to do that if we can see that that
                // their TCP stack is still draining the tx buffer, which of course we can infer by us still draining
                // our rx buffer.  To be here we know that we read some bytes, but we also want to know that there is
                // still more coming, so for that we need to see that we're not waiting on a message header.

                if ((m_nInterestOpsLast & OP_EAGER) == 0) // no pending read
                    {
                    m_ldtForceHeartbeat = 0; // disable heartbeat timeout
                    }
                else if (m_ldtForceHeartbeat == 0 ||                                 // start of pending read
                        (ldtNow > m_ldtForceHeartbeat && heartbeatOnOwner(epoch)))    // or time to force heartbeat
                    {
                    // force a HB multiple times during an ack timeout period, this assumes our peer has the same timeout as us
                    m_ldtForceHeartbeat = ldtNow + f_driver.getDependencies().getAckTimeoutMillis() / 3;
                    }
                // else; not time to force a heartbeat yet
                }

            if (fReadHealthy && fWriteHealthy) // common path
                {
                // we're healthy; disable any active timeout
                m_ldtAckFatalTimeout = m_ldtAckTimeout = 0;

                // only record read/write amounts when healthy
                m_cbWriteLastCheck = cbWrite;
                m_cbReadLastCheck  = cbRead;
                }
            else if (ldtAckTimeout == 0)
                {
                // health is now in doubt, set timeouts
                long cMillisTimeout = f_driver.getDependencies().getAckTimeoutMillis();

                m_ldtAckTimeout      = cMillisTimeout == 0 || getProtocolVersion() == 0 ? Long.MAX_VALUE : ldtNow + cMillisTimeout;
                cMillisTimeout       = f_driver.getDependencies().getAckFatalTimeoutMillis();
                m_ldtAckFatalTimeout = cMillisTimeout == 0 ? Long.MAX_VALUE : ldtNow + cMillisTimeout;
                }
            else if (ldtNow >= m_ldtAckFatalTimeout)
                {
                // fatal timeout expired
                long     cMillisTimeout = f_driver.getDependencies().getAckFatalTimeoutMillis();
                Duration dur            = new Duration(cMillisTimeout, Duration.Magnitude.MILLI);

                getLogger().log(makeRecord(Level.WARNING,
                        "{0} dropping connection with {1} after {2} fatal ack timeout health(read={3}, write={4}), receiptWait={5}: {6}",
                        getLocalEndPoint(), getPeer(), dur, fReadHealthy, fWriteHealthy, oReceiptUnacked, BufferedConnection.this));

                scheduleDisconnect(lTransportGeneration, new IOException("fatal ack timeout after " + dur));
                }
            else if (ldtNow >= ldtAckTimeout)
                {
                ++m_cAckTimeouts;
                tracePerf("ack-timeout", true);
                // timeout expired
                final int cMultCap = 10;
                long      cMillisTimeout = f_driver.getDependencies().getAckTimeoutMillis();
                Duration  dur            = new Duration(cMillisTimeout * Math.min(cMultCap, m_cUnackLast + 1), Duration.Magnitude.MILLI);

                getLogger().log(makeRecord(Level.WARNING,
                        "{0} initiating connection migration with {1} after {2} ack timeout health(read={3}, write={4}), receiptWait={5}: {6}",
                        getLocalEndPoint(), getPeer(), dur, fReadHealthy, fWriteHealthy, oReceiptUnacked, BufferedConnection.this));

                if (oReceiptUnacked == null)
                    {
                    m_nIdUnackLast = 0;
                    m_cUnackLast   = 0;
                    }
                else
                    {
                    // if we have successive read health timeouts then perhaps our ack timeout is just too small for
                    // an apparently really slow network and big message.  While a needless connection migration is
                    // harmless, endless ones without progress is certainly not.  So lets push up the timeout while
                    // we work to get this larger message successfully ack'd.  Note our health check algorithm already
                    // defends against large message transmission time in that we're happy so long as we see read and
                    // write progress, but it doesn't account for how long it may take to drain the OS tx buffer. So
                    // this time increase defends against that unknown.  We do limit the increase we'll extend the
                    // configured timeout by cMultiCap times the configured timeout.
                    int nId = System.identityHashCode(oReceiptUnacked) ^
                              System.identityHashCode(batchUnacked)    ^ ofReceiptUnacked; // in case the same receipt object is reused frequently
                    if (nId == m_nIdUnackLast)
                        {
                        int cStuck = ++m_cUnackLast;
                        cMillisTimeout *= Math.min(cMultCap, cStuck + 1);

                        if (cStuck == MIGRATION_LIMIT_BEFORE_DUMP)
                            {
                            // see onMigration where we also check m_cUnackLast and request that our peer collect a dump as well
                            String sName = HeapDump.dumpHeapForBug("Bug-27585336-tmb-migration");
                            getLogger().log(makeRecord(Level.WARNING,
                                    "{0} has failed to deliver {1} to {2} after {3} attempts, {4} has been collected for analysis",
                                    getLocalEndPoint(), oReceiptUnacked, BufferedConnection.this, cStuck, sName));
                            ldtNow = SafeClock.INSTANCE.getSafeTimeMillis(); // heap dump may have taken awhile
                            }
                        }
                    else
                        {
                        m_cUnackLast = 0;
                        }
                    m_nIdUnackLast = nId;
                    }

                // reset ack timeout
                m_ldtAckTimeout = ldtNow + cMillisTimeout;

                migrate(lTransportGeneration, new IOException("ack timeout after " + dur));
                }
            // else; progressing towards timeout
            }

        /**
         * Report, without repairing, a ready connection that has queued bytes but no visible progress owner.
         *
         * @param ldtNow                the current safe time
         * @param lTransportGeneration the ready transport generation
         */
        protected void checkWriteProgressInvariant(long ldtNow, long lTransportGeneration)
            {
            long cMillis = LIVENESS_WATCHDOG_MILLIS;
            if (cMillis <= 0L)
                {
                return;
                }

            long    cbQueued       = f_cbQueued.get();
            boolean fWriter        = isWriterActive();
            boolean fWriteInterest = (m_nInterestOpsLast & OP_WRITE) != 0;
            boolean fViolation     = cbQueued > 0L && isTransportReady() && !fWriter && !fWriteInterest;

            if (!fViolation)
                {
                m_ldtWriteInvariantStart = 0L;
                return;
                }

            long ldtStart = m_ldtWriteInvariantStart;
            if (ldtStart == 0L)
                {
                m_ldtWriteInvariantStart = ldtNow;
                m_cbWriteInvariantStart  = m_cbWrite;
                return;
                }

            if (m_cbWrite != m_cbWriteInvariantStart)
                {
                m_ldtWriteInvariantStart = ldtNow;
                m_cbWriteInvariantStart  = m_cbWrite;
                return;
                }

            if (ldtNow - ldtStart >= cMillis && ldtNow >= m_ldtNextLivenessReport)
                {
                m_ldtNextLivenessReport = ldtNow + Math.max(cMillis, 1000L);
                ++m_cLivenessReports;

                getLogger().log(makeRecord(Level.WARNING,
                        "{0} LIVENESS[stalled-write] peer={1}, generation={2}, queuedBytes={3}, {4}",
                        getLocalEndPoint(), getPeer(), lTransportGeneration,
                        new MemorySize(Math.max(0L, cbQueued)),
                        getProgressTraceDetail(ldtNow) + ", " + getPerfTraceDetail()));
                }
            }

        /**
         * Return producer-owned bytes pending drain into the consumer-owned batch.
         *
         * @return pending producer bytes
         */
        protected long getProducerPendingBytes()
            {
            return 0L;
            }

        @Override
        protected long getQueuedWriteBytesForTesting()
            {
            return Math.max(0L, f_cbQueued.get());
            }

        @Override
        protected boolean dropWriteWakeupForTesting()
            {
            if (getPartialWritesForTesting() == 0)
                {
                return false;
                }

            if (consumeTestCounter(TEST_DROP_WRITE_WAKEUPS_AFTER_PARTIAL_REMAINING))
                {
                TEST_DROPPED_WRITE_WAKEUPS.incrementAndGet();
                TEST_QUEUED_BYTES_AT_DROPPED_WRITE_WAKEUP.set(Math.max(0L, f_cbQueued.get()));
                TEST_WRITE_PROGRESS_PAUSED.set(true);
                TEST_DROPPED_WRITE_WAKEUP_NUDGE.set(this::resumeDroppedWriteWakeupForTesting);
                m_nInterestOpsLast &= ~OP_WRITE;
                return true;
                }

            // A later wakeup is a new durable progress obligation, whether it comes from the diagnostic nudge
            // or from the implementation under test.
            TEST_WRITE_PROGRESS_PAUSED.set(false);
            return false;
            }

        /**
         * Return producer-owned message count pending drain into the consumer-owned batch.
         *
         * @return pending producer message count
         */
        protected long getProducerPendingMessages()
            {
            return 0L;
            }

        /**
         * Return the outbound backlog that should drive sender-side backlog signaling.
         *
         * @return backlog bytes used for BACKLOG_EXCESSIVE/NORMAL emission
         */
        protected long getBacklogSignalBytes()
            {
            return Math.max(0L, f_cbQueued.get());
            }

        /**
         * Return the threshold at which sender-side backlog should be declared excessive.
         *
         * @return the excessive threshold in bytes
         */
        protected long getBacklogSignalExcessiveThreshold()
            {
            return getBacklogExcessiveThreshold();
            }

        /**
         * Return the threshold below which sender-side backlog should be declared normal again.
         *
         * @return the normal threshold in bytes
         */
        protected long getBacklogSignalNormalThreshold()
            {
            return getBacklogSignalExcessiveThreshold() / 2;
            }

        /**
         * Return connection-specific performance detail for tracing.
         *
         * @return optional suffix detail
         */
        protected String getPerfTraceDetail()
            {
            return "";
            }

        /**
         * Return common connection, scheduling, selector, and socket-write diagnostics.
         *
         * @param ldtNow  current safe time
         *
         * @return common diagnostic detail
         */
        protected String getProgressTraceDetail(long ldtNow)
            {
            String sOwner  = m_sWriterOwner;
            String sReason = m_sWriterReason;
            return "transport(ready=" + isTransportReady()
                    + ", generation=" + m_lTransportGeneration
                    + ", channel=" + getChannelIdentityForDiagnostics()
                    + "), writer(active=" + isWriterActive()
                    + ", owner=" + (sOwner == null ? "none" : sOwner)
                    + ", reason=" + (sReason == null ? "none" : sReason)
                    + ", ageMillis=" + (m_ldtWriterAcquired == 0L ? 0L : Math.max(0L, ldtNow - m_ldtWriterAcquired))
                    + "), selection(readyOps=" + m_nReadyOpsLast
                    + ", interestOps=" + m_nInterestOpsLast
                    + ", lastCallbackMillis=" + m_ldtLastSelectionCallback
                    + ", tasks=" + m_cSelectorTasks
                    + ", delayLastNanos=" + m_cSelectorTaskDelayLastNanos
                    + ", delayMaxNanos=" + m_cSelectorTaskDelayMaxNanos
                    + "), write(lastAttemptMillis=" + m_ldtLastWriteAttempt
                    + ", lastProgressMillis=" + m_ldtLastWriteProgress
                    + ", attemptedBytes=" + m_cbLastWriteAttempted
                    + ", writtenBytes=" + m_cbLastWriteResult
                    + ", zeroPasses=" + m_cZeroWritePasses
                    + ")";
            }

        /**
         * Emit an opt-in performance snapshot for troubleshooting write progression and ack stalls.
         *
         * @param sReason  the trigger reason
         * @param fForce   true to ignore interval throttling
         */
        protected void tracePerf(String sReason, boolean fForce)
            {
            if (!PERF_TRACE)
                {
                return;
                }

            long cbQueued  = f_cbQueued.get();
            long cbPending = getProducerPendingBytes();
            long ldtNow    = SafeClock.INSTANCE.getSafeTimeMillis();
            if (!fForce
                    && cbQueued < PERF_TRACE_THRESHOLD_BYTES
                    && cbPending < PERF_TRACE_THRESHOLD_BYTES
                    && ldtNow < m_ldtNextPerfTrace)
                {
                return;
                }

            m_ldtNextPerfTrace = ldtNow + PERF_TRACE_INTERVAL_MILLIS;

            WriteBatch batchUnflushed = m_batchWriteUnflushed;
            WriteBatch batchSend      = m_batchWriteSendHead;
            WriteBatch batchResend    = m_batchWriteResendHead;

            String sDetail = getPerfTraceDetail();
            if (!sDetail.isEmpty())
                {
                sDetail = ", " + sDetail;
                }
            sDetail = ", " + getProgressTraceDetail(ldtNow) + sDetail;

            getLogger().log(makeRecord(Level.INFO,
                    "{0} PERF[{1}] peer={2}, state={3}, writerActive={4}, concurrentWriters={5}, pendingMsgs={6}, pendingBytes={7}, queuedBytes={8}, unflushed={9}, sendHead={10}, resendHead={11}, bytesUnacked={12}, receiptsReturn={13}, receiptsUnflushed={14}, progress(pass={15}, direct={16}, queued={17}, idle={18}, postDrain={19}), receipts(calls={20}, req={21}, returned={22}, emitted={23}), ackTimeouts={24}{25}",
                    getLocalEndPoint(), sReason, getPeer(), m_state, isWriterActive(), getConcurrentWriters(),
                    getProducerPendingMessages(), new MemorySize(Math.max(0L, cbPending)),
                    new MemorySize(Math.max(0L, cbQueued)),
                    new MemorySize(batchUnflushed == null ? 0L : Math.max(0L, batchUnflushed.getLength())),
                    new MemorySize(batchSend == null ? 0L : Math.max(0L, batchSend.getLength())),
                    new MemorySize(batchResend == null ? 0L : Math.max(0L, batchResend.getLength())),
                    new MemorySize(Math.max(0L, f_cBytesUnacked.get())), m_cReceiptsReturn.get(), m_cReceiptsUnflushed,
                    m_cProgressPasses, m_cProgressDirect, m_cProgressQueued, m_cProgressNoWork,
                    new MemorySize(Math.max(0L, m_cbProgressPostDrain)),
                    m_cReceiptProcessCalls, m_cReceiptsRequested, m_cReceiptsReturned, m_cReceiptsEmitted,
                    m_cAckTimeouts, sDetail));
            }


        @Override
        protected void onMigrationStarted(long lTransportGeneration)
            {
            super.onMigrationStarted(lTransportGeneration);
            long ldtNow       = SafeClock.INSTANCE.getSafeTimeMillis();
            long cMillisFatal = f_driver.getDependencies().getAckFatalTimeoutMillis();
            long ldtFatal     = cMillisFatal == 0 ? Long.MAX_VALUE : ldtNow + cMillisFatal;
            long ldtAckFatal  = m_ldtAckFatalTimeout;
            long ldtMigration = m_ldtMigrationFatalTimeout;

            // preserve earlier migration and fatal ack deadlines so retries cannot extend total recovery time
            if (ldtAckFatal != 0)
                {
                ldtFatal = Math.min(ldtFatal, ldtAckFatal);
                }
            if (ldtMigration != 0)
                {
                ldtFatal = Math.min(ldtFatal, ldtMigration);
                }

            m_ldtMigrationFatalTimeout   = ldtFatal;
            m_fMigrationTimeoutScheduled = false;
            ++m_lMigrationSequence;
            tracePerf("migration-start", true);
            }

        @Override
        protected void onMigrationCompleted(long lTransportGeneration)
            {
            super.onMigrationCompleted(lTransportGeneration);
            m_ldtMigrationFatalTimeout   = 0;
            m_fMigrationTimeoutScheduled = false;
            ++m_lMigrationSequence;
            tracePerf("migration-complete", true);
            }

        @Override
        protected void onTransportReady(TransportEpoch epoch)
            {
            super.onTransportReady(epoch);

            // READY publication is a mandatory owner-lane kick. Producer work
            // may have accumulated while the replacement handshook, and
            // replay/SYNC batches may already be queued by migration.
            int nRequest = m_nWriteProgressionRequest.get();
            if ((nRequest != 0 || hasProducerWriteWork())
                    && tryActivateWriter(epoch, "epoch-ready", /*fRequireReady*/ true))
                {
                if (nRequest != 0 && TEST_TRACK_DEFERRED_WRITE_PROGRESSION)
                    {
                    TEST_DEFERRED_WRITE_PROGRESSION_HANDOFFS.incrementAndGet();
                    }
                scheduleActiveWriteProgression(
                        (nRequest & WRITE_PROGRESSION_SOCKET_WRITE) != 0,
                        nRequest != 0 && (nRequest & WRITE_PROGRESSION_EXPLICIT) == 0);
                }

            if (f_cbQueued.get() > 0)
                {
                try
                    {
                    wakeup(epoch);
                    }
                catch (IOException e)
                    {
                    onException(epoch.f_lId, e);
                    }
                }
            }

        @Override
        protected boolean isMigrationHandshakeTimeoutCurrent(long lMigrationSequence)
            {
            return m_lMigrationSequence == lMigrationSequence &&
                   isMigrationHandshakeInProgress() &&
                   m_ldtMigrationFatalTimeout != 0;
            }

        /**
         * Disconnect an active connection whose replacement handshake exceeded its fatal deadline.
         *
         * @param ldtNow  the current safe time
         */
        protected void checkMigrationHandshakeTimeout(long ldtNow)
            {
            long lMigrationSequence = -1L;
            lock();
            try
                {
                long ldtFatal = m_ldtMigrationFatalTimeout;
                if (isMigrationHandshakeInProgress() &&
                    ldtFatal != 0 && ldtNow >= ldtFatal && !m_fMigrationTimeoutScheduled)
                    {
                    lMigrationSequence           = m_lMigrationSequence;
                    m_fMigrationTimeoutScheduled = true;
                    }
                }
            finally
                {
                unlock();
                }

            if (lMigrationSequence >= 0)
                {
                long     cMillisFatal = f_driver.getDependencies().getAckFatalTimeoutMillis();
                Duration duration     = new Duration(cMillisFatal, Duration.Magnitude.MILLI);

                getLogger().log(makeRecord(Level.WARNING,
                        "{0} dropping connection with {1} after {2} replacement handshake timeout: {3}",
                        getLocalEndPoint(), getPeer(), duration, BufferedConnection.this));
                scheduleHandshakeTimeoutDisconnect(lMigrationSequence,
                        new IOException("replacement handshake timeout after " + duration));
                }
            }

        @Override
        public void onMigration()
            {
            super.onMigration();

            // don't log as a warning, we can get here simply because the other process terminated, i.e. delay
            // logging as a warning until we actually re-establish the connection.
            getLogger().log(makeRecord(Level.FINER, "{0} migrating connection with {1}",
                    getLocalEndPoint(), BufferedConnection.this));

                m_cMsgInSkip = 0; // our peer will start with a new SYNC message which will tell us exactly how much we should skip

                // Establish a migration boundary over consumer-owned state:
                // 1. absorb any producer-published work into the current build batch
                // 2. append pending receipt bookkeeping into that build batch
                // 3. promote the build batch into the queued send/resend structures
                absorbPublishedWrites();
                appendPendingReceiptMessages(m_batchWriteUnflushed);

                WriteBatch batchWriteUnflushed = detachUnflushedWriteBatch();
                if (batchWriteUnflushed != null)
                    {
                    f_cbQueued.addAndGet(batchWriteUnflushed.getLength());
                    }

                // remove any sends (from f_cbQueued) that were pre-ack'd during a former migration
                for (WriteBatch batch = m_batchWriteSendHead; batch != null && batch.m_ofAck == batch.m_ofAdd; batch = batch.next())
                    {
                    batch.m_ofSend = batch.m_ofAdd; // pretend we've sent everything
                    f_cbQueued.addAndGet(-batch.getLength());
                    }

                // rewind batches from the resend queue; Note: we scan the entire queue because it is not trivial
                // to identify where it is safe to stop scanning.  Specifically we can't stop and the first unsent
                // message since we can run into empty batches
                WriteBatch batchResendHead = m_batchWriteResendHead;
                for (WriteBatch batch = batchResendHead; batch != null; batch = batch.next())
                    {
                    f_cbQueued.addAndGet(batch.rewind());
                    }

                int nProt = getProtocolVersion();
                if (nProt > 0)
                    {
                    // place SYNC message at the start of the send queue, but don't included it in the resend queue
                    WriteBatch batchWriteSync = m_batchWriteSendHead = new WriteBatch(/*fLink*/ false);

                    int        cbHead  = nProt < 5 ? 4 : 16;       // msg header size
                    int        cbBody  = 17 + (nProt < 3 ? 0 : 1); // msg body size
                    ByteBuffer bufSync = ByteBuffer.allocate(cbHead + cbBody);

                    if (nProt > 4)
                        {
                        bufSync.putLong(-cbBody)  // negative msg size indicates control message of that size
                                .position(16); // skip writes for header/body crc
                        }
                    else
                        {
                        bufSync.putInt(-cbBody);
                        }

                    bufSync.put(MSG_SYNC)
                            .putLong(m_cMsgOutDelivered)
                            .putLong(m_cMsgIn);

                    if (nProt > 2)
                        {
                        bufSync.put(m_cUnackLast == MIGRATION_LIMIT_BEFORE_DUMP ? SYNC_CMD_DUMP : SYNC_CMD_NONE);
                        }

                    bufSync.flip();
                    if (nProt > 4)
                        {
                        // backfill the message header with the header and body CRCs now that they can be computed
                        populateCtrlMsgHeaderCrc(bufSync);
                        }

                    batchWriteSync.append(bufSync, /*fRecycle*/ false, /*body*/ null, /*receipt*/ null);
                    batchWriteSync.m_ofAck = batchWriteSync.m_ofAdd; // prevent it from accepting bundles, since it isn't resend eligible
                    batchWriteSync.m_next  = batchResendHead;
                    f_cbQueued.addAndGet(batchWriteSync.getLength());
                    }
                else if (m_state == ConnectionState.ACTIVE)
                    {
                    // this shouldn't be possible except at protocol v0 and we don't call migrate when running at v0
                    scheduleDisconnect(new IOException("protocol error; sync at protocol=" + nProt + ", with in=" + m_cMsgIn + ", out=" + m_cMsgOutDelivered));
                    }
                else
                    {
                    // the disconnect may have occurred before we negotiated a protocol version, we can't send a SYNC
                    // if we don't know the version, since by the time it gets processed on the new connection a version will
                    // have been negotiated and we would need to use that unknown version here.  But since we haven't negotiated
                    // a version yet it also means we could not have exchanged messages either, so it is safe to skip the SYNC.
                    m_batchWriteSendHead = batchResendHead;
                    }
            }

        /**
         * Populate message header.
         *
         * @param bufHead  the header buffer to be written to
         */
        protected void populateCtrlMsgHeaderCrc(ByteBuffer bufHead)
            {
            int   cbHeader = 16;
            int   nPos     = bufHead.position();
            int   nLimit   = bufHead.limit();
            int   lCrc     = 0;
            CRC32 crc32    = f_crcTx;

            // compute and write body CRC; Note, we still need to write a 0 when crc is disabled
            // as buffers may not be zero'd out to begin with
            if (crc32 != null)
                {
                crc32.reset();
                bufHead.position(nPos + cbHeader);
                lCrc = Buffers.updateCrc(crc32, bufHead);
                lCrc = lCrc == 0 ? 1: lCrc;
                }
            bufHead.putInt(nPos + 8, lCrc); // write body crc

            // compute and write header CRC
            if (crc32 != null)
                {
                crc32.reset();
                bufHead.position(nPos).limit(nPos + cbHeader - 4);
                lCrc = Buffers.updateCrc(crc32, bufHead);
                lCrc = lCrc == 0 ? 1: lCrc;
                bufHead.limit(nLimit);
                }

            bufHead.putInt(nPos + 12, lCrc); // write headed crc
            bufHead.position(nPos);
            }

        /**
         * Handle any incoming data.
         *
         * @param fReady  true iff the channel is readable
         * @param epoch   the transport epoch selected for this callback
         *
         * @return a partial SelectionService.Handler interest set
         *
         * @throws IOException if an I/O error occurs
         */
        protected abstract int processReads(boolean fReady, TransportEpoch epoch)
                throws IOException;

        /**
         * Write the contents of the WriteQueue to the channel.
         *
         * @param fReady  true iff the channel is writeable
         *
         * @return a partial SelectionService.Handler interest set
         *
         */
        protected int processWrites(boolean fReady)
            {
            lock();
            try
                {
                TransportEpoch epoch = getReadyTransportEpoch();
                if (epoch == null)
                    {
                    return 0;
                    }

                try
                    {
                    return processWrites(fReady, epoch);
                    }
                catch (IOException e)
                    {
                    onException(epoch.f_lId, e);
                    return 0;
                    }
                }
            finally
                {
                unlock();
                }
            }

        /**
         * Write the contents of the WriteQueue to a specific transport generation.
         *
         * @param fReady  true iff the channel is writeable
         * @param epoch   the transport epoch selected for this callback
         *
         * @return a partial SelectionService.Handler interest set
         *
         * @throws IOException if an I/O error occurs
         */
        protected int processWrites(boolean fReady, TransportEpoch epoch)
                throws IOException
            {
            if (!isCurrentTransportEpoch(epoch, TransportPhase.READY))
                {
                return 0;
                }

            long cbBacklog = f_cbQueued.get();

            if (fReady || cbBacklog > 0)
                {
                if (TEST_TRACK_SOCKET_BACKPRESSURE && m_fSocketBackpressureObservedForTesting)
                    {
                    TEST_PROCESS_WRITE_CALLS_AFTER_BACKPRESSURE.incrementAndGet();
                    }
                if (!tryActivateWriter(epoch, fReady ? "selection-write" : "queued-write",
                        /*fRequireReady*/ true))
                    {
                    if (TEST_TRACK_SOCKET_BACKPRESSURE && m_fSocketBackpressureObservedForTesting)
                        {
                        TEST_WRITER_BUSY_CALLS_AFTER_BACKPRESSURE.incrementAndGet();
                        }
                    return cbBacklog > 0 ? OP_WRITE : 0;
                    }

                try
                    {
                    WriteBatch batch       = m_batchWriteSendHead;
                    long       cbBundle    = getAutoFlushThreshold();
                    // This threshold must be the same threshold that drives m_fBacklog below. Subclasses may
                    // define producer-visible backlog independently of the physical socket buffer size; mixing
                    // those domains leaves the gap between the two thresholds permanently ineligible to drain.
                    long       cbExcessive = getBacklogSignalExcessiveThreshold();
                    boolean    fBacklog    = m_fBacklog;

                    // process the queue

                    // Note: Avoid the possibility of staying in this loop "forever" if the producer and consumer
                    // are matching pace.  The only reason to bail out is to allow other work to be accomplished
                    // on this SelectionService thread.
                    long cbWritten = 0;
                    try
                        {
                        for (int i = 0; cbBacklog > 0 && i < 16 && (cbBacklog <= cbExcessive || fBacklog); ++i)
                            {
                            long cbBatch = batch.getLength();
                            while (cbBatch != 0 &&        // don't bundle into an empty batch
                                   cbBatch < cbBundle &&  // batch is small enough that it is worth bundling
                                   cbBacklog > cbBatch && // there is more in the backlog, i.e. batch.bundle won't NPE
                                   batch.m_ofAck < batch.m_ofAdd) // ensure we don't bundle into fully ack'd batches as this can lead to data loss during migration
                                {
                                cbBatch = batch.bundle();
                                }

                            if (cbBatch == 0 || batch.write(epoch)) // unlike in flush we don't need to sync since acks are also processed on this thread
                                {
                                cbWritten += cbBatch;
                                cbBacklog -= cbBatch;

                                if (cbBacklog == 0)
                                    {
                                    // check to see if more has been queued
                                    cbBacklog = f_cbQueued.addAndGet(-cbWritten);
                                    cbWritten = 0;

                                    if (cbBacklog == 0)
                                        {
                                        break;
                                        }
                                    }

                                batch = batch.next();
                                }
                            else // we've exhausted the socket write buffer
                                {
                                cbWritten += (cbBatch - batch.getLength());
                                break;
                                }
                            }
                    }
                finally
                    {
                    cbBacklog = f_cbQueued.addAndGet(-cbWritten);
                    m_batchWriteSendHead = batch;
                    }

                    // change backlog status if necessary
                    long cbSignal          = getBacklogSignalBytes();
                    long cbSignalExcessive = getBacklogSignalExcessiveThreshold();
                    long cbSignalNormal    = getBacklogSignalNormalThreshold();
                    if (fBacklog)
                        {
                        if (cbSignal < cbSignalNormal)
                            {
                            m_fBacklog = false;
                            emitEvent(new SimpleEvent(Event.Type.BACKLOG_NORMAL, getPeer()));
                            }
                        }
                    else if (cbSignal > cbSignalExcessive)
                        {
                        m_fBacklog = true;
                        emitEvent(new SimpleEvent(Event.Type.BACKLOG_EXCESSIVE, getPeer()));
                        }
                    }
                finally
                    {
                    deactivateWriter(epoch);

                    int nRequest = m_nWriteProgressionRequest.get();
                    if (isCurrentTransportEpoch(epoch, TransportPhase.READY)
                            && (nRequest != 0 || hasProducerWriteWork())
                            && tryActivateWriter(epoch, "selection-follow-up", /*fRequireReady*/ true))
                        {
                        if (nRequest != 0 && TEST_TRACK_DEFERRED_WRITE_PROGRESSION)
                            {
                            TEST_DEFERRED_WRITE_PROGRESSION_HANDOFFS.incrementAndGet();
                            }
                        // we are already on the SelectionService thread for this channel, so run the follow-up
                        // progression immediately instead of re-posting it and letting producer backlog sit
                        processQueuedWritesOnSelectionThread(epoch,
                                (nRequest & WRITE_PROGRESSION_SOCKET_WRITE) != 0,
                                nRequest == 0 || (nRequest & WRITE_PROGRESSION_EXPLICIT) == 0);
                        }
                    }
                }

            return cbBacklog > 0 ? OP_WRITE : 0;
            }

        @Override
        public void dispose()
            {
            ByteBuffer bufferMsgReceipt = m_bufferRecycleOutboundReceipts;
            if (bufferMsgReceipt != null)
                {
                // recycle resources
                getSocketDriver().getDependencies().getBufferManager().release(bufferMsgReceipt);
                m_bufferRecycleOutboundReceipts = null;
                }

            super.dispose();
            }

        @Override
        public void drainReceipts()
            {
            // Note: we must drain in send order, specifially
            // resent, current, queue'd, unflushed

            lock();
            try
                {
                absorbPublishedWrites();

                WriteBatch batch = detachUnflushedWriteBatch();
                if (batch != null)
                    {
                    f_cbQueued.addAndGet(batch.getLength());
                    }

                for (batch = m_batchWriteResendHead; batch != null; batch = batch.next())
                    {
                    batch.m_ofSend = batch.m_ofAdd; // pretend we've sent it
                    f_cbQueued.addAndGet(-batch.getLength());
                    int cEmit;
                    while ((cEmit = batch.ack(Integer.MAX_VALUE, f_aoReceiptTmp)) > 0)
                        {
                        for (int i = 0; i < cEmit; ++i)
                            {
                            Object oReceipt = f_aoReceiptTmp[i];
                            f_aoReceiptTmp[i] = null;
                            if (oReceipt != RECEIPT_NO_EMIT)
                                {
                                addEvent(new SimpleEvent(Event.Type.RECEIPT, getPeer(), oReceipt));
                                }
                            }
                        }
                    }

                m_batchWriteResendHead = m_batchWriteSendHead = m_batchWriteTail = new WriteBatch(/*fLink*/ false);
                m_cReceiptsUnflushed = 0;
                m_cReceiptsReturn.set(0);
                f_cBytesUnacked.set(0);
                }
            finally
                {
                unlock();
                }
            }

        /**
         * Populate message header.
         *
         * @param buffHead  the header buffer to be written to
         * @param aBuff     the buffer array that contains the message
         * @param of        the offset
         * @param cBuffers  the number of buffers
         * @param cbBuffer  the number of bytes in message
         */
        protected void populateMessageHeader(ByteBuffer buffHead, ByteBuffer[] aBuff, int of, int cBuffers, long cbBuffer)
            {}

        /**
         * Return the receipt size that includes header and body.
         *
         * @return the receipt size
         */
        protected abstract int getReceiptSize();

        @Override
        public String toString()
            {
            WriteBatch batch          = m_batchWriteUnflushed;
            long       ldtAckTimeout  = m_ldtAckTimeout;
            String     sTimeout;

            if (ldtAckTimeout == 0)
                {
                sTimeout = "n/a";
                }
            else
                {
                long ldtNow = SafeClock.INSTANCE.getSafeTimeMillis();
                long ldtAckFatal = m_ldtAckFatalTimeout;
                sTimeout = "ack=" + new Duration(ldtAckTimeout - ldtNow, Duration.Magnitude.MILLI) +
                        (ldtAckFatal == Long.MAX_VALUE ? "" : ", conn=" + new Duration(ldtAckFatal - ldtNow, Duration.Magnitude.MILLI));
                }

            // NOTE: there may be many more pending outs, they just didn't have receipts so we won't count them until the next message with a receipt goes out
            // and the corresponding RECEIPT message comes back, at which point we'll scan the resend queue and count up the former messages which have now
            // been confirmed as delivered.  When comparing the toStrings of each side of a connection, we'll always see our out lag behind our peers in because
            // the peer must receive it before we can *know* that it was delivered, and we defer the ack of the ack until the next application RECEIPT request
            // thus peer's in is *always* greater then local out.
            return super.toString() + ", bufferedOut=" + new MemorySize(f_cbQueued.get()) +
                    ", unflushed=" + new MemorySize(batch == null ? 0 : batch.getLength()) +
                    ", delivered(in=" + m_cMsgIn + ", out=" + m_cMsgOutDelivered + ")" + // see note above regarding out
                    ", timeout(" + sTimeout + "), interestOps=" + m_nInterestOpsLast +
                    ", unflushed receipt=" + m_cReceiptsUnflushed + ", receiptReturn " + m_cReceiptsReturn +
                    ", isReceiptFlushRequired " + isReceiptFlushRequired();
            }

        /**
         * WriteBatch is used to encapsulate an array of ByteBuffers which
         * are to be written to the connection.
         */
        public class WriteBatch
            {
            public WriteBatch()
                {
                this(true);
                }

            public WriteBatch(boolean fLink)
                {
                if (fLink)
                    {
                    m_batchWriteTail = m_batchWriteTail.m_next = this;
                    }
                }

            /**
             * Return the number of bytes remaining to be sent in the batch.
             *
             * @return the number of bytes remaining to be sent in the batch
             */
            public long getLength()
                {
                return m_cbBatch;
                }

            /**
             * Return the number of append operations represented by the batch.
             *
             * @return the number of accumulated messages
             */
            public int getMessageCount()
                {
                return m_cMessages;
                }

            /**
             * Append a message to the batch.
             *
             * This method locks the batch, as it is always called from the app thread and
             * the batch may be concurrently being ack processed on the SS thread.
             *
             * @param bufHead       the buffer header to append
             * @param fRecycleHead  true iff the buffer header should be recycled
             * @param bufseqBody    optional buffer body to append
             * @param receipt       optional associated receipt
             *
             * @return the batch size
             */
            public long append(ByteBuffer bufHead, boolean fRecycleHead, BufferSequence bufseqBody, Object receipt)
                {
                lock();
                try
                    {
                    return appendInternal(bufHead, fRecycleHead, bufseqBody, receipt, /*fHeaderPrePopulated*/ false);
                    }
                finally
                    {
                    unlock();
                    }
                }

            /**
             * Append a message with a pre-populated header to the batch.
             * Unlike {@link #append}, this does NOT call populateMessageHeader() -
             * the header ByteBuffer must already contain valid framing data.
             *
             * @param bufHead      pre-populated header buffer
             * @param bufseqBody   the message body (may be null for control messages)
             * @param receipt      the delivery receipt (may be null)
             *
             * @return the new batch size
             */
            public long appendPrepared(ByteBuffer bufHead, BufferSequence bufseqBody, Object receipt)
                {
                lock();
                try
                    {
                    return appendInternal(bufHead, /*fRecycleHead*/ false, bufseqBody, receipt, /*fHeaderPrePopulated*/ true);
                    }
                finally
                    {
                    unlock();
                    }
                }

            /**
             * Append a message with a pre-populated header when the batch is already known to be consumer-owned
             * on the SelectionService thread.
             *
             * @param bufHead      pre-populated header buffer
             * @param bufseqBody   the message body (may be null for control messages)
             * @param receipt      the delivery receipt (may be null)
             *
             * @return the new batch size
             */
            public long appendPreparedConsumer(ByteBuffer bufHead, BufferSequence bufseqBody, Object receipt)
                {
                return appendInternal(bufHead, /*fRecycleHead*/ false, bufseqBody, receipt, /*fHeaderPrePopulated*/ true);
                }

            /**
             * Append a drained user message with a pre-populated header when the batch is already known to be
             * consumer-owned on the SelectionService thread.
             * <p>
             * This is the common MPSC drain path: no header recycling, no control-message shape, and the header is
             * already populated.
             *
             * @param bufHead      pre-populated header buffer
             * @param bufseqBody   the message body
             * @param receipt      the delivery receipt (may be null)
             *
             * @return the new batch size
             */
            public long appendPreparedMessageConsumer(ByteBuffer bufHead, BufferSequence bufseqBody, Object receipt)
                {
                addWriter();

                int cBodyBuffers = bufseqBody.getBufferCount();
                int cBufferAdd   = 1 + cBodyBuffers;
                int ofAdd        = ensureAdditionalBufferCapacity(cBufferAdd);

                Object[]     aoReceipt = m_aReceipt;
                ByteBuffer[] aBuffer   = m_aBuffer;

                bufseqBody.getBuffers(0, cBodyBuffers, aBuffer, ofAdd + 1);
                aBuffer[ofAdd] = bufHead;

                for (int of = ofAdd, ofEnd = ofAdd + cBufferAdd; of < ofEnd; ++of)
                    {
                    aBuffer[of].mark();
                    }

                long cbHead = bufHead.remaining();
                long cbBody = bufseqBody.getLength();
                long cb     = cbHead + cbBody;
                int  ofTail = ofAdd + cBufferAdd - 1;

                if (receipt == null && m_cbBatch == 0)
                    {
                    // Inject a protocol-level receipt at most once per batch so resend state cannot grow forever.
                    receipt = RECEIPT_NO_EMIT;
                    }

                aoReceipt[ofAdd] = getReceiptSpanMarker(cBufferAdd);
                if (receipt != null)
                    {
                    aoReceipt[ofTail] = receipt;
                    ++m_cReceiptsUnflushed;
                    }

                m_ofAdd = ofAdd + cBufferAdd;

                BufferedConnection.this.f_cBytesUnacked.addAndGet(cb);

                ++m_cMessages;

                return m_cbBatch += cb;
                }

            /**
             * Shared append implementation used by both append paths.
             * Keeps the MPSC appendPrepared path aligned with the legacy append
             * path for receipts, markers, ack bookkeeping and
             * migration/rewind behavior.
             */
            protected long appendInternal(ByteBuffer bufHead, boolean fRecycleHead,
                    BufferSequence bufseqBody, Object receipt, boolean fHeaderPrePopulated)
                {
                addWriter();

                int cBufferAdd = 1 + (bufseqBody == null
                                      ? 0
                                      : bufseqBody.getBufferCount());

                int          ofAdd     = ensureAdditionalBufferCapacity(cBufferAdd);
                Object[]     aoReceipt = m_aReceipt;
                ByteBuffer[] aBuffer   = m_aBuffer;
                long         cbBody    = 0;
                if (bufseqBody != null)
                    {
                    bufseqBody.getBuffers(0, cBufferAdd - 1, aBuffer, ofAdd + 1);
                    cbBody = bufseqBody.getLength();

                    if (!fHeaderPrePopulated)
                        {
                        populateMessageHeader(bufHead, aBuffer, ofAdd + 1, cBufferAdd, cbBody);
                        }
                    }

                aBuffer[ofAdd] = bufHead;

                long cb       = bufHead.remaining() + cbBody;
                long cbUnacked = cbBody;

                if (fRecycleHead)
                    {
                    // we don't count recycled against unacked
                    aoReceipt[ofAdd] = RECEIPT_HEADER_RECYCLE;
                    }
                else
                    {
                    cbUnacked += bufHead.remaining();
                    }

                // mark each of the append buffers so that we can reset to initial position
                // in case we need to retransmit due to migration
                for (int of = ofAdd, eOf = ofAdd + cBufferAdd; of < eOf; ++of)
                    {
                    aBuffer[of].mark();
                    }

                if (receipt == null && m_cbBatch == 0 &&
                    cBufferAdd > 1) // cBufferAdd > 1 ensures it is not a control message; SYNCs shouldn't be counted and we don't want receipts for receipts
                    {
                    // in order to prevent the resend queue from growing endlessly we ensure that we
                    // have periodic acks by injecting artificial receipts at most once per batch.
                    // this becomes a real receipt from the protocol perspective, but it will never be emitted
                    // to the user
                    receipt = RECEIPT_NO_EMIT;
                    }

                if (receipt == null)
                    {
                    // mark message boundaries with artificial receipts, unlike RECEIPT_NO_EMIT these
                    // are not real from a protocol perspective
                    aoReceipt[ofAdd + cBufferAdd - 1] = fRecycleHead && cBufferAdd == 1
                                                        ? RECEIPT_MSG_MARKER_HEADER_RECYCLE
                                                        // combo receipt indicating msg and recycling
                                                        : RECEIPT_MSG_MARKER;
                    }
                else if (cBufferAdd == 1)
                    {
                    if (receipt == RECEIPT_ACKREQ_MARKER || receipt == RECEIPT_ACKREQ_MARKER_RECYCLE)
                        {
                        aoReceipt[ofAdd] = receipt;
                        }
                    else
                        {
                        // we don't have a way to encode this. There is also no way for a user to cause this
                        // it could only be the result of a bug in the bus
                        throw new IllegalStateException();
                        }
                    }
                else
                    {
                    aoReceipt[ofAdd + cBufferAdd - 1] = receipt;
                    ++m_cReceiptsUnflushed;
                    }

                m_ofAdd = ofAdd + cBufferAdd;

                BufferedConnection.this.f_cBytesUnacked.addAndGet(cbUnacked);

                ++m_cMessages;

                return m_cbBatch += cb;
                }

            /**
             * Append the next batch into this WriteBatch.
             *
             * @return the new batch size
             */
            public long bundle()
                {
                WriteBatch batchSrc   = m_next;
                int        ofAckSrc   = batchSrc.m_ofAck;
                int        ofSndSrc   = batchSrc.m_ofSend;
                int        ofSrc      = Math.min(ofAckSrc, ofSndSrc); // ofSend can be < ofAck
                int        cBufferSrc = batchSrc.m_ofAdd - ofSrc;
                int        ofAdd      = ensureAdditionalBufferCapacity(cBufferSrc);

                System.arraycopy(batchSrc.m_aBuffer,  ofSrc, m_aBuffer,  ofAdd, cBufferSrc);
                System.arraycopy(batchSrc.m_aReceipt, ofSrc, m_aReceipt, ofAdd, cBufferSrc);

                m_ofAdd = ofAdd + cBufferSrc;

                WriteBatch batchNext = batchSrc.m_next;
                if (batchNext != null)
                    {
                    m_next = batchNext;
                    }
                // else; avoid invalidating the tail

                if (ofAckSrc > ofSndSrc) // the source batch is partially ack'd; thus this batch is fully ack'd
                    {
                    m_ofAck += (ofAckSrc - ofSndSrc);
                    }

                long cbBatch = m_cbBatch += batchSrc.getLength();
                m_cMessages += batchSrc.getMessageCount();

                // NOTE: f_cBytesUnacked and m_cReceiptsUnflushed don't need to be updated as it had already been
                // accounted for when producing the source batch

                // make the original batch unusable
                batchSrc.m_aReceipt = batchSrc.m_aBuffer = EMPTY_BUFFER_ARRAY;
                batchSrc.m_ofAck    = batchSrc.m_ofSend  = batchSrc.m_ofAdd = 0;
                batchSrc.m_cbBatch  = 0;
                batchSrc.m_cMessages = 0;
                // we don't null out batchSrc.m_next as there could still be head pointers referencing it

                return cbBatch;
                }

            /**
             * Attempt to write the batch to the connection.  When called from the client thread synchronization
             * must be held since the corresponding ack could come in and be processed on the SS thread before
             * this call returns.
             *
             * @return true iff the entire batch has been written
             *
             * @throws IOException if an I/O error occurs
             */
            public boolean write()
                throws IOException
                {
                return write(getReadyTransportEpoch());
                }

            /**
             * Attempt to write this batch on a specific transport epoch.
             *
             * @param epoch  the epoch owning the write pass
             *
             * @return true iff the entire batch has been written
             *
             * @throws IOException if an I/O error occurs
             */
            public boolean write(TransportEpoch epoch)
                throws IOException
                {
                ByteBuffer[] aBuffer = m_aBuffer;
                int          ofSend  = m_ofSend;
                int          ofAdd   = m_ofAdd;
                long         cbAttempted = m_cbBatch;

                if (PROGRESS_DIAGNOSTICS)
                    {
                    m_ldtLastWriteAttempt  = SafeClock.INSTANCE.getSafeTimeMillis();
                    m_cbLastWriteAttempted = cbAttempted;
                    }
                long cb = BufferedConnection.this.write(epoch, aBuffer, ofSend, ofAdd - ofSend);
                if (PROGRESS_DIAGNOSTICS)
                    {
                    m_cbLastWriteResult = cb;
                    }

                // advance offset, decrement cBuffer based on amount written
                if (cb > 0)
                    {
                    if (TEST_TRACK_SOCKET_BACKPRESSURE && m_fSocketBackpressureObservedForTesting)
                        {
                        TEST_BYTES_WRITTEN_AFTER_SOCKET_BACKPRESSURE.addAndGet(cb);
                        }
                    if (PROGRESS_DIAGNOSTICS)
                        {
                        m_ldtLastWriteProgress = SafeClock.INSTANCE.getSafeTimeMillis();
                        m_cZeroWritePasses     = 0L;
                        }
                    for (; ofSend < ofAdd && !aBuffer[ofSend].hasRemaining(); ++ofSend)
                        {}

                    m_cbBatch -= cb;
                    m_ofSend   = ofSend;
                    }
                else if (PROGRESS_DIAGNOSTICS && cbAttempted > 0L)
                    {
                    ++m_cZeroWritePasses;
                    }

                if (TEST_TRACK_SOCKET_BACKPRESSURE && cbAttempted > 0L && cb < cbAttempted)
                    {
                    m_fSocketBackpressureObservedForTesting = true;
                    TEST_SOCKET_BACKPRESSURE_CONNECTION.set(BufferedConnection.this);
                    TEST_SOCKET_BACKPRESSURE_OUTSTANDING_BYTES.accumulateAndGet(
                            Math.max(Math.max(0L, m_cbBatch), Math.max(0L, f_cbQueued.get())), Math::max);
                    TEST_SOCKET_BACKPRESSURE_PARTIAL_WRITES.incrementAndGet();
                    }

                if (TEST_TRACK_SOCKET_BACKPRESSURE)
                    {
                    TEST_SOCKET_BACKPRESSURE_STATE.set("attempted=" + cbAttempted
                            + ", written=" + cb
                            + ", batchRemaining=" + m_cbBatch
                            + ", queued=" + f_cbQueued.get()
                            + ", interestOps=" + m_nInterestOpsLast
                            + ", writerActive=" + isWriterActive()
                            + ", generation=" + m_lTransportGeneration);
                    }

                return ofSend == ofAdd;
                }

            /**
             * Ack some the contents of the batch up through the next receipt.
             *
             * This method will emit the receipt(s) as well.
             *
             * @param cReceipts the maximum number of receipts to consume
             * @param aoReceipt array which will be filled with receipts to emit
             *
             * @return the number of receipts copied into aoReceipt
             */
            public int ack(int cReceipts, Object[] aoReceipt)
                {
                BufferManager manager   = getSocketDriver().getDependencies().getBufferManager();
                int           cMsg      = 0;
                ByteBuffer[]  aBuff     = m_aBuffer;
                Object[]      aReceipt  = m_aReceipt;
                int           ofSend    = m_ofSend;
                int           ofAdd     = m_ofAdd;
                int           ofAck     = m_ofAck;
                int           ofReceipt = 0;

                while (ofAck < ofAdd && cReceipts > 0)
                    {
                    Object     oReceipt = aReceipt[ofAck];
                    ByteBuffer buff     = aBuff[ofAck];

                    if (oReceipt instanceof ReceiptSpanMarker)
                        {
                        ReceiptSpanMarker  marker          = (ReceiptSpanMarker) oReceipt;
                        int                cSpan = Math.min(marker.getBufferCount(), ofAdd - ofAck);
                        Object             oMessageReceipt = cSpan <= 0
                                ? null
                                : aReceipt[ofAck + cSpan - 1];

                        for (int i = 0; i < cSpan; ++i)
                            {
                            int        ofSlot   = ofAck + i;
                            ByteBuffer buffSlot = aBuff[ofSlot];

                            if (buffSlot != null)
                                {
                                if (ofSlot >= ofSend)
                                    {
                                    ByteBuffer buffNew = ByteBuffer.allocate(buffSlot.remaining());
                                    buffNew.put(buffSlot).flip();
                                    aBuff[ofSlot] = buffNew;
                                    }
                                else
                                    {
                                    aBuff[ofSlot] = null;
                                    }
                                }

                            aReceipt[ofSlot] = null;
                            }

                        ofAck += cSpan;
                        ++cMsg;

                        if (oMessageReceipt != null)
                            {
                            --cReceipts;
                            aoReceipt[ofReceipt++] = oMessageReceipt;
                            if (ofReceipt == aoReceipt.length)
                                {
                                break;
                                }
                            }

                        continue;
                        }

                    aBuff[ofAck]    = null;
                    aReceipt[ofAck] = null;

                    if (ofAck >= ofSend)
                        {
                        // we've received an ACK for something we haven't sent yet.  This can only legally happen
                        // after a connection migration, which means that we haven't *re*sent it yet, but our peer
                        // received our original transmission, we just didn't receive the ack.  The ack we're processing
                        // is in response to our original send, and there will *not* be any ack when we resend this
                        // message.  While it would be nice to simply not send the message, we can't do that as we've
                        // already told our peer (indirectly via SYNC) how many messages we'll be resending, so we
                        // must send that many.  This batch already resides in the write queue but we can't retain these
                        // buffers as once they are recycled or a receipt is emitted their contents could be changed and
                        // thus we would be sending garbage to our peer, if we send garbage as a message length then
                        // the peer can't properly skip messages.  We could alternately substitute alternate lengths in
                        // but then we'd have much more bookkeeping to do especially for messages which were partially sent.

                        ByteBuffer buffNew = ByteBuffer.allocate(buff.remaining()); // normal GCable garbage
                        buffNew.put(buff).flip();
                        aBuff[ofAck] = buffNew;
                        }

                    ++ofAck;

                    if (oReceipt == RECEIPT_HEADER_RECYCLE)
                        {
                        manager.release(buff);
                        }
                    else if (oReceipt == RECEIPT_MSG_MARKER_HEADER_RECYCLE ||
                             oReceipt == RECEIPT_ACKREQ_MARKER_RECYCLE)
                        {
                        ++cMsg;
                        manager.release(buff);
                        }
                    else if (oReceipt == RECEIPT_MSG_MARKER ||
                             oReceipt == RECEIPT_ACKREQ_MARKER)
                        {
                        ++cMsg;
                        }
                    else if (oReceipt != null) // real receipt
                        {
                        ++cMsg;
                        --cReceipts;
                        aoReceipt[ofReceipt++] = oReceipt;
                        if (ofReceipt == aoReceipt.length)
                            {
                            break;
                            }
                        }
                    }

                m_ofAck             = ofAck;
                m_cMsgOutDelivered += cMsg;

                return ofReceipt;
                }

            /**
             * Rewind the state of the batch such that any previously {@link #write sent}, but @{link #ack unacked}
             * messages can be resent.
             *
             * @return the number of bytes which were rescheduled
             */
            public long rewind()
                {
                int          ofAck   = m_ofAck;
                int          ofSend  = m_ofSend;
                ByteBuffer[] aBuffer = m_aBuffer;
                long         cbDelta = 0;

                if (ofSend < ofAck) // rare; only if we do a quick double migration
                    {
                    // we don't need to resend these since they've been acked before we rewound
                    // but we do need to adjust our batch size accordingly
                    for (; ofSend < ofAck; ++ofSend)
                        {
                        cbDelta -= aBuffer[ofSend].remaining();
                        }
                    }
                m_ofSend = ofAck; // our new send position

                for (int ofAdd = m_ofAdd; ofAck < ofAdd; ++ofAck)
                    {
                    ByteBuffer buff = aBuffer[ofAck];
                    cbDelta -= buff.remaining();
                    buff.reset(); // see append
                    cbDelta += buff.remaining();
                    }

                m_cbBatch += cbDelta;

                return cbDelta;
                }

            /**
             * Ensure the buffer array has enough capacity to add the specified
             * number of buffers.
             *
             * @param cBufferAdd  the number of buffers that will be added
             *
             * @return the index at which to add
             */
            protected int ensureAdditionalBufferCapacity(int cBufferAdd)
                {
                ByteBuffer[] aBuffer     = m_aBuffer;
                Object[]     aReceipt    = m_aReceipt;
                int          ce          = aBuffer.length;
                int          ofSrc       = Math.min(m_ofAck, m_ofSend);
                int          ofAdd       = m_ofAdd;
                int          cBufferUsed = ofAdd - ofSrc;

                if (cBufferUsed + cBufferAdd > ce)
                    {
                    // reallocate and shift the buffer array
                    ByteBuffer[] aBufferNew = new ByteBuffer[(ce + cBufferAdd) * 2];
                    System.arraycopy(aBuffer, ofSrc, aBufferNew, 0, cBufferUsed);

                    Object[] aReceiptNew = new Object[aBufferNew.length];
                    System.arraycopy(aReceipt, ofSrc, aReceiptNew, 0, cBufferUsed);

                    m_aBuffer  = aBufferNew;
                    m_aReceipt = aReceiptNew;

                    m_ofAck   -= ofSrc;
                    m_ofSend  -= ofSrc;

                    ofAdd = m_ofAdd -= ofSrc;
                    }
                else if (ofSrc + cBufferUsed + cBufferAdd > ce)
                    {
                    // we have enough space, but need to shift the buffers
                    System.arraycopy(aBuffer, ofSrc, aBuffer, 0, cBufferUsed);
                    Arrays.fill(aBuffer, cBufferUsed, ce, null);

                    System.arraycopy(aReceipt, ofSrc, aReceipt, 0, cBufferUsed);
                    Arrays.fill(aReceipt, cBufferUsed, ce, null);

                    m_ofAck  -= ofSrc;
                    m_ofSend -= ofSrc;

                    ofAdd = m_ofAdd -= ofSrc;
                    }
                // else; nothing to do

                return ofAdd;
                }

            /**
             * Return the next batch in the queue
             *
             * @return the next batch
             */
            public WriteBatch next()
                {
                return m_next;
                }

            /**
             * Return true iff there are subsequent batches in the queue.
             *
             * @return true iff there are subsequent batches in the queue.
             */
            public boolean hasNext()
                {
                return m_next != null;
                }

            /**
             * Lock this batch in order to mutate its state.
             */
            public void lock()
                {
                f_lock.lock();
                }

            /**
             * Unlock this batch.
             */
            public void unlock()
                {
                f_lock.unlock();
                }

            /**
             * The total number of bytes remaining to write in the batch.
             */
            protected long m_cbBatch;

            /**
             * The number of append operations represented by the batch.
             */
            protected int m_cMessages;

            /**
             * The ByteBuffer array.
             */
            protected ByteBuffer[] m_aBuffer = new ByteBuffer[16];

            /**
             * An array of potentially null receipts corresponding to the buffers in m_aBuffer.
             */
            protected Object[] m_aReceipt = new Object[m_aBuffer.length];

            /**
             * The next free buffer
             */
            protected int m_ofAdd;

            /**
             * The next offset to process while sending
             */
            protected int m_ofSend;

            /**
             * The next buffer to process when we receive an ack
             */
            protected int m_ofAck;

            /**
             * The next batch in the write queue.
             */
            protected volatile WriteBatch m_next;

            /**
             * Lock used to synchronize access to this WriteBatch.
             */
            private final Lock f_lock = new ReentrantLock();
            }

        // ----- data members -------------------------------------------

        /**
         * The tail of the write queue.
         * <p/>
         * This is only accessed while holding the lock on the connection.
         */
        protected WriteBatch m_batchWriteTail = new WriteBatch(/*fLink*/ false);

        /**
         * The send pointer (head) into the write queue.
         *
         * This is only accessed by the SS thread associated with the connection's channel.
         */
        protected WriteBatch m_batchWriteSendHead = m_batchWriteTail;

        /**
         * The resend pointer (head) into the write queue.
         *
         * This is only accessed by the SS thread associated with the connection's channel.
         *
         * Note that this queue has two heads and while generally the resend head is the true
         * head, in some cases the send head can actually come before it.  This can occur for
         * a brief period after a connection migration as the peer resends ACKs for messages
         * which we've yet to resend, thus allowing us to move the resend head behind the
         * send head.
         */
        protected WriteBatch m_batchWriteResendHead = m_batchWriteTail;

        /**
         * The number of unsent bytes in the write queue.
         *
         * Note this atomic also acks as the primary write barrier for the queue.  Producers will first insert
         * into the queue and only then update f_cbQueued, ensuring that the consumer can see the updated value
         * presuming that it has first checked the size.  Note that producers themselves will be sync'd on the
         * connection which will ensure visibility and consistency when updating the tail.
         */
        protected final AtomicLong f_cbQueued = new AtomicLong();

        /**
         * The value of m_cbWrite at the last heartbeat cycle
         */
        protected long m_cbHeartbeatLast;

        /**
         * True if heartbeat has identified this connection as idle and an empty receipt should be sent
         */
        protected boolean m_fIdle;

        /**
         * The auto-flush threshold.
         */
        protected long m_cbAutoFlushThreshold;

        /**
         * The force ack threshold.
         */
        protected long m_cbForceAckThreshold;

        /**
         * The excessive backlog threashold.
         */
        protected long m_cbBacklogExcessiveThreshold;

        /**
         * The last interest ops for this channel.
         */
        protected int m_nInterestOpsLast;

        /**
         * The ready ops supplied to the last selection callback.
         */
        protected volatile int m_nReadyOpsLast;

        /**
         * Safe-clock time of the last selection callback.
         */
        protected volatile long m_ldtLastSelectionCallback;

        /**
         * The current unflushed WriteBatch, or null.
         *
         * Unflushed means that its size hasn't been added for f_cbQueued and it is still usable by application threads.
         * <p>
         * This field is volatile for use by isFlushRequired and isReceiptFlushRequired methods
         */
        protected volatile WriteBatch m_batchWriteUnflushed = m_batchWriteTail;

        /**
        * An estimate of the number of threads which contributed to the current batch.
        */
        protected int m_cWritersBatch;

        /**
         * Bit set used to identify threads contributing to the current batch
         */
        protected long m_lWritersBatchBitSet;

        /**
         * True if in the backlog state, false otherwise.
         */
        protected boolean m_fBacklog;

        /**
         * True if a backlog check has been scheduled.
         */
        protected final AtomicBoolean m_fBacklogScheduled = new AtomicBoolean();

        /**
         * The number of receipts associated with the unflushed WriteBatch.
         */
        protected int m_cReceiptsUnflushed;

        /**
         * The number of receipts to return to the peer on our next send.
         */
        protected final AtomicInteger m_cReceiptsReturn = new AtomicInteger();

        /**
         * As estimate of the number of unacked bytes since the last received ack or sent forced flush.
         *
         * TODO: now that we have a resend queue we could choose to accurately track this
         */
        protected final AtomicLong f_cBytesUnacked = new AtomicLong();

        /**
         * The transport epoch that currently owns write progression for this connection.
         * A stale epoch can only release its own ticket.
         */
        protected final AtomicReference<TransportEpoch> m_epochWriterActive = new AtomicReference<>();

        /**
         * Coalesced progression requests that arrived before or while an epoch writer owned the connection.
         */
        protected final AtomicInteger m_nWriteProgressionRequest = new AtomicInteger();

        /**
         * Thread and reason associated with the active writer, for diagnostics.
         */
        protected volatile String m_sWriterOwner;
        protected volatile String m_sWriterReason;
        protected volatile long   m_ldtWriterAcquired;

        /**
         * Per-connection SelectionService scheduling diagnostics.
         */
        protected volatile long m_cSelectorTasks;
        protected volatile long m_cSelectorTaskDelayLastNanos;
        protected volatile long m_cSelectorTaskDelayMaxNanos;

        /**
         * Per-connection socket-write diagnostics.
         */
        protected volatile long m_ldtLastWriteAttempt;
        protected volatile long m_ldtLastWriteProgress;
        protected volatile long m_cbLastWriteAttempted;
        protected volatile long m_cbLastWriteResult;
        protected volatile long m_cZeroWritePasses;

        /**
         * Reporting-only liveness watchdog state.
         */
        protected long m_ldtWriteInvariantStart;
        protected long m_cbWriteInvariantStart;
        protected long m_ldtNextLivenessReport;
        protected long m_cLivenessReports;

        /** Whether this connection observed genuine socket backpressure while the functional hook was enabled. */
        protected boolean m_fSocketBackpressureObservedForTesting;

        /**
         * Next time a periodic performance snapshot may be emitted.
         */
        protected volatile long m_ldtNextPerfTrace;

        /**
         * Count of consumer-owned write progression passes.
         */
        protected long m_cProgressPasses;

        /**
         * Count of passes that attempted direct write progression.
         */
        protected long m_cProgressDirect;

        /**
         * Count of passes that enqueued work to the selection service.
         */
        protected long m_cProgressQueued;

        /**
         * Count of passes that found no work to flush.
         */
        protected long m_cProgressNoWork;

        /**
         * Total bytes drained after the main flush decision point.
         */
        protected long m_cbProgressPostDrain;

        /**
         * Count of processed receipt control messages.
         */
        protected long m_cReceiptProcessCalls;

        /**
         * Total receipts requested by the peer.
         */
        protected long m_cReceiptsRequested;

        /**
         * Total receipts returned by the peer.
         */
        protected long m_cReceiptsReturned;

        /**
         * Count of ack timeout migrations observed by this connection.
         */
        protected long m_cAckTimeouts;

        /**
         * ByteBuffer to write Receipt messages
         */
        protected ByteBuffer m_bufferRecycleOutboundReceipts;

        /**
         * The number of threads actively preparing/enqueuing messages.
         */
        protected final AtomicInteger m_cWritersActive = new AtomicInteger();

        /**
         * The total number of messages (including control messages) received from our peer.
         */
        protected long m_cMsgIn;

        /**
         * The number of upcomming inbound messages to skip (due to migration).
         */
        protected long m_cMsgInSkip;

        /**
         * The total number of receipts emitted.
         */
        protected long m_cReceiptsEmitted;

        /**
         * Reusable array for holding receipts.
         */
        protected final Object[] f_aoReceiptTmp = new Object[16];

        /**
         * The total number of messages (including control messages) which have been confirmed as delivered to our peer.
         */
        protected long m_cMsgOutDelivered;

        /**
         * The number of bytes read as of last health check.
         */
        protected long m_cbReadLastCheck;

        /**
         * The number of bytes written as of last health check.
         */
        protected long m_cbWriteLastCheck;

        /**
         * timestamp at which the current pending ack times out
         */
        protected long m_ldtAckTimeout;

        /**
         * The time at which the health check must force a heartbeat
         */
        protected long m_ldtForceHeartbeat;

        /**
         * The id of the receipt of the last message which failed the ack health check
         */
        protected int m_nIdUnackLast;

        /**
         * The number of times the specific last receipt triggered a health check timeout.
         */
        protected int m_cUnackLast;

        /**
         * timestamp at which the current pending ack fatally times out
         */
        protected long m_ldtAckFatalTimeout;

        /**
         * Timestamp at which the current replacement handshake fatally times out.
         */
        protected long m_ldtMigrationFatalTimeout;

        /**
         * The replacement-handshake sequence used to reject stale timeout tasks.
         */
        protected long m_lMigrationSequence;

        /**
         * True when a timeout task has been scheduled for the current replacement handshake.
         */
        protected boolean m_fMigrationTimeoutScheduled;

        }


    // ----- constants ------------------------------------------------------

    /**
     * The number of migrations on the same message before collecting a heap dump.
     */
    protected static final int MIGRATION_LIMIT_BEFORE_DUMP = 4;

    /**
     * Internal message type for exchanging receipts.
     */
    protected static final byte MSG_RECEIPT = 1;

    /**
     * Internal message type for syncing a connection (after a migration)
     */
    protected static final byte MSG_SYNC = 2;

    /**
     * Perform no additional commands upon syncing
     */
    protected static final byte SYNC_CMD_NONE = 0;

    /**
     * Collect a heap dump upon syncing.
     */
    protected static final byte SYNC_CMD_DUMP = 1;

    /**
     * For messages which don't include their own receipt, this dummy marker is used, allowing us
     * to identify message boundaries within a batch.  This receipt is not reflected on the wire
     * it is only used to identify message boundaries.
     *
     * Not reflected on the wire.
     */
    protected static final Object RECEIPT_MSG_MARKER = new Object();

    /**
     * Used to tag header fields as being recycleable.  As these are headers they would never have a receipt of their own.
     *
     * Not reflected on the wire.
     */
    protected static final Object RECEIPT_HEADER_RECYCLE = new Object();

    /**
     * A combination of RECEIPT_MSG_MARKER and RECEIPT_HEADER_RECYCLE for single buffer recyclable messages.
     *
     * Not reflected on the wire.
     */
    protected static final Object RECEIPT_MSG_MARKER_HEADER_RECYCLE = new Object();

    /**
     * Used to tag ack request messages.
     *
     * Not reflected on the wire.
     */
    protected static final Object RECEIPT_ACKREQ_MARKER = new Object();

    /**
     * Used to tag ack request messages which should be recycled.
     *
     * Not reflected on the wire.
     */
    protected static final Object RECEIPT_ACKREQ_MARKER_RECYCLE = new Object();

    /**
     * A dummy over-the-wire receipt, i.e. we will request a receipt from our peer, but we will not emit it to
     * our application when it comes in.  This is used in the case that the application doesn't use receipts
     * or doesn't use them often, and we want to avoid having the resend queue grow too large.
     *
     * This receipt *is* reflected on the wire, but never emitted to the user.
     */
    protected static final Object RECEIPT_NO_EMIT = new Object();

    /**
     * Receipt metadata for a contiguous user-message span in a consumer-owned write batch.
     *
     * The real receipt, if any, remains stored in the tail slot so the common MPSC append path can avoid
     * allocating one wrapper object per message.
     */
    protected static final class ReceiptSpanMarker
        {
        ReceiptSpanMarker(int cBuffers)
            {
            m_cBuffers = cBuffers;
            }

        int getBufferCount()
            {
            return m_cBuffers;
            }

        private final int m_cBuffers;
        }

    /**
     * Return a cached span marker for common message sizes and allocate only for unusually fragmented messages.
     */
    protected static ReceiptSpanMarker getReceiptSpanMarker(int cBuffers)
        {
        return cBuffers > 0 && cBuffers < RECEIPT_SPAN_MARKERS.length
                ? RECEIPT_SPAN_MARKERS[cBuffers]
                : new ReceiptSpanMarker(cBuffers);
        }

    /**
     * Cached receipt span markers keyed by buffer count.
     */
    protected static ReceiptSpanMarker[] createReceiptSpanMarkers()
        {
        ReceiptSpanMarker[] aMarker = new ReceiptSpanMarker[33];
        for (int i = 1, c = aMarker.length; i < c; ++i)
            {
            aMarker[i] = new ReceiptSpanMarker(i);
            }
        return aMarker;
        }

    protected static final ReceiptSpanMarker[] RECEIPT_SPAN_MARKERS = createReceiptSpanMarkers();

    /**
     * The number of post-partial write wakeups to drop for functional testing.
     */
    private static final AtomicInteger TEST_DROP_WRITE_WAKEUPS_AFTER_PARTIAL_REMAINING =
            new AtomicInteger(Integer.getInteger(
                    BufferedSocketBus.class.getName() + ".dropWriteWakeupsAfterPartial", 0));

    /**
     * The number of write wakeups dropped by the functional test hook.
     */
    private static final AtomicLong TEST_DROPPED_WRITE_WAKEUPS = new AtomicLong();

    /**
     * The queued bytes captured when the functional test hook dropped a write wakeup.
     */
    private static final AtomicLong TEST_QUEUED_BYTES_AT_DROPPED_WRITE_WAKEUP = new AtomicLong();

    /**
     * Whether already-delivered write callbacks should be quarantined until the diagnostic nudge.
     */
    private static final AtomicBoolean TEST_WRITE_PROGRESS_PAUSED = new AtomicBoolean();

    /**
     * The diagnostic nudge for the connection whose write wakeup was dropped.
     */
    private static final AtomicReference<Runnable> TEST_DROPPED_WRITE_WAKEUP_NUDGE = new AtomicReference<>();

    /**
     * Whether the adaptive partial-write owner handoff regression is tracking
     * producer-to-owner transitions.
     */
    private static final boolean TEST_TRACK_NON_OWNER_QUEUED_WRITE_HANDOFFS = Boolean.getBoolean(
            BufferedSocketBus.class.getName() + ".trackNonOwnerQueuedWriteHandoffs");

    /**
     * Number of queued-write continuations transferred from a producer to the
     * transport owner lane by the partial-write regression path.
     */
    private static final AtomicLong TEST_NON_OWNER_QUEUED_WRITE_HANDOFFS = new AtomicLong();

    /**
     * Whether the lost progression-request regression is tracking deferrals and handoffs.
     */
    private static final boolean TEST_TRACK_DEFERRED_WRITE_PROGRESSION = Boolean.getBoolean(
            BufferedSocketBus.class.getName() + ".trackDeferredWriteProgression");

    /** The number of test-observed progression requests deferred behind an active writer. */
    private static final AtomicLong TEST_DEFERRED_WRITE_PROGRESSION_REQUESTS = new AtomicLong();

    /** The number of deferred progression requests handed to a subsequent writer. */
    private static final AtomicLong TEST_DEFERRED_WRITE_PROGRESSION_HANDOFFS = new AtomicLong();

    /** Whether the receipt/application piggyback regression is tracking the relevant flush condition. */
    private static final boolean TEST_TRACK_RECEIPT_FLUSH_WITH_PENDING_DATA = Boolean.getBoolean(
            BufferedSocketBus.class.getName() + ".trackReceiptFlushWithPendingData");

    /** The number of receipt flushes that found pending consumer-owned application data. */
    private static final AtomicLong TEST_RECEIPT_FLUSHES_WITH_PENDING_DATA = new AtomicLong();

    /** Whether the genuine socket-backpressure regression is tracking write results. */
    private static final boolean TEST_TRACK_SOCKET_BACKPRESSURE = Boolean.getBoolean(
            BufferedSocketBus.class.getName() + ".trackSocketBackpressure");

    /** Number of incomplete writes observed by the genuine socket-backpressure regression. */
    private static final AtomicLong TEST_SOCKET_BACKPRESSURE_PARTIAL_WRITES = new AtomicLong();

    /** Maximum outstanding bytes observed during an incomplete socket write. */
    private static final AtomicLong TEST_SOCKET_BACKPRESSURE_OUTSTANDING_BYTES = new AtomicLong();

    /** Bytes written after the connection observed an incomplete socket write. */
    private static final AtomicLong TEST_BYTES_WRITTEN_AFTER_SOCKET_BACKPRESSURE = new AtomicLong();

    /** Latest socket-write state captured by the genuine socket-backpressure regression. */
    private static final AtomicReference<String> TEST_SOCKET_BACKPRESSURE_STATE =
            new AtomicReference<>("not observed");

    /** Connection currently tracked by the genuine socket-backpressure regression. */
    private static final AtomicReference<BufferedConnection> TEST_SOCKET_BACKPRESSURE_CONNECTION =
            new AtomicReference<>();

    /** Number of write-progress callbacks observed after genuine socket backpressure. */
    private static final AtomicLong TEST_PROCESS_WRITE_CALLS_AFTER_BACKPRESSURE = new AtomicLong();

    /** Number of post-backpressure callbacks that found the epoch writer ticket occupied. */
    private static final AtomicLong TEST_WRITER_BUSY_CALLS_AFTER_BACKPRESSURE = new AtomicLong();

    /**
     * Empty buffer array for use in bundling.
     */
    protected static final ByteBuffer[] EMPTY_BUFFER_ARRAY = new ByteBuffer[0];
    }
