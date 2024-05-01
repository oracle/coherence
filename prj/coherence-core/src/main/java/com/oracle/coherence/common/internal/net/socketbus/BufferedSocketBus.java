/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
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

import java.net.SocketException;

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
                            conn.optimisticFlush();
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
            // as well as any threads waiting to use the connection

            return m_cWritersWaiting.get() + m_cWritersBatch;
            }

        /**
         * Extracted post "send" logic.
         *
         * The caller must be synchronized on this connection. The parameter ordering allows the caller to inline
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
                    if (!flush(/*fSocketWrite*/ fSocketWrite, /*fAuto*/true))
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
            if (flush(/*fSocketWrite*/fSocketWrite, /*fAuto*/false))
                {
                removeFlushable(this);
                }
            }

        /**
         * Send any scheduled BufferSequences.
         *
         * @param fSocketWrite  true if the caller is willing to offer its cpu to perform a socket write
         * @param fAuto         true iff it is an auto-flush
         *
         * @return true if the connection has flushed all pending data
         */
        public boolean flush(boolean fSocketWrite, boolean fAuto)
            {
            SocketBusDriver.Dependencies deps = getSocketDriver().getDependencies();

            WriteBatch batch   = m_batchWriteUnflushed;
            int        cRecReq = m_cReceiptsUnflushed;
            int        cRecRet = m_cReceiptsReturn.getAndSet(0);

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
                    int        cbCap = bufferMsgReceipt.capacity();
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

            int cWriter = getConcurrentWriters();
            if (batch == null)
                {
                return true; // nothing to flush
                }
            else if (f_cbQueued.get() == 0 && (cWriter == 1 || (cWriter <= deps.getDirectWriteThreadThreshold() && fSocketWrite)))
                {
                // SS thread isn't writing, so this is the current head.  Even if the SS thread never sees this
                // batch we want to overwrite any historic batch with the current head to avoid building up
                // garbage which otherwise wouldn't be GCable until the SS thread iterated through many empty batches
                m_batchWriteSendHead = batch;
                try
                    {
                    synchronized (batch) // sync'd because ack can come in while we're still in batch.write
                        {
                        if (batch.write())
                            {
                            m_cWritersBatch       = 0;
                            m_lWritersBatchBitSet = 0;
                            return true;
                            }
                        // else; we didn't write everything, fall through and evaluate if we need to enqueue the batch
                        }
                    }
                catch (IOException e)
                    {
                    // stream is now in an unknown state; queue the remainder and reconnect
                    m_batchWriteUnflushed = null;
                    enqueueWriteBatch(batch);
                    onException(e);
                    return true; // nothing more can be done
                    }
                }
            else if (m_state == ConnectionState.DEFUNCT)
                {
                scheduleDisconnect(null); // ensure receipts are emitted ASAP
                }

            if (fAuto && batch.getLength() < getBacklogExcessiveThreshold() * 2)
                {
                return false; // data remains to be flushed
                }
            else
                {
                // either caller explictly flushed or we're buffering a significant
                // amount, enqueue to SelectionService
                m_batchWriteUnflushed = null;
                m_cWritersBatch       = 0;
                m_lWritersBatchBitSet = 0;

                enqueueWriteBatch(batch);
                return true; // euqueue'd the batch
                }
            }


        @Override
        protected boolean heartbeat()
            {
            boolean fResult = false;
            if (m_cbWrite        == m_cbHeartbeatLast &&    // we've sent nothing since the last check
                f_cbQueued.get() == 0 &&                    // we have no outbound traffic queued up
                m_state          == ConnectionState.ACTIVE) // COH-25350 - messages can be sent on the connection
                {
                // prevent the network infrastructure from closing the idle socket

                // setting m_fIdle to true will cause the next flush to minimally send an empty receipt
                // flush will also reset idle
                // some writes are seen on the socket when optimisticFlush() is called
                m_fIdle = true;
                optimisticFlush(); // attempt flush in case auto-flush is disabled
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
                    onException(e);
                    }
                }

            final long cbExcessive = getBacklogExcessiveThreshold();
            if (cbQueuedNow > cbExcessive * 2 &&
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

                        if (isValid() && !m_fBacklog && f_cbQueued.get() > cbExcessive)
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
            if (cReceiptsRequested < 0 || getSocketDriver().getDependencies().getMaximumReceiptDelayMillis() == 0)
                {
                // negative or config indicates force receipt send immediately
                m_cReceiptsReturn.addAndGet(cReceiptsRequested < 0 ? -cReceiptsRequested : cReceiptsRequested);

                if (isReceiptFlushRequired())
                    {
                    // there are no pending flushes for this connection
                    optimisticFlush();
                    }
                }
            else
                {
                m_cReceiptsReturn.addAndGet(cReceiptsRequested);
                }

            int cReturned = in.readInt();
            if (cReturned > 0)
                {
                EndPoint   epPeer      = getPeer();
                WriteBatch batchResend = m_batchWriteResendHead;
                WriteBatch batchNext   = batchResend.next();
                for (;;)
                    {
                    int cEmit;
                    if (batchNext == null && m_batchWriteUnflushed == batchResend)
                        {
                        // the app may be concurrently writing to this (the last) batch, thus we must sync
                        synchronized (batchResend)
                            {
                            cReturned -= cEmit = batchResend.ack(cReturned, f_aoReceiptTmp);
                            }
                        }
                    else
                        {
                        // there is a subsequent batch which means the application is no longer writing
                        // to this batch.  Also reading the next ref acts as a memory barrier ensuring
                        // that we have a clean view of the contents of this batch since it was written
                        // after this batch was done being written to by the app thread
                        cReturned -= cEmit = batchResend.ack(cReturned, f_aoReceiptTmp);
                        }

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
         * Return true iff there are pending receipts that needs to be flushed but no application data to flush
         *
         * @return true iff connection has pending receipts but no unflushed application data
         */
        protected boolean isReceiptFlushRequired()
            {
            return (m_cReceiptsReturn.get() > 0 || m_fIdle) && !isFlushRequired();
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
        public int onReadySafe(int nOps)
            throws IOException
            {
            return m_nInterestOpsLast = processReads((nOps & OP_READ) != 0) | processWrites((nOps & OP_WRITE) != 0);
            }

        @Override
        protected void checkHealth(long ldtNow)
            {
            if (m_state == null || m_state.ordinal() > ConnectionState.ACTIVE.ordinal())
                {
                return;
                }

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
                else if (m_ldtForceHeartbeat == 0 ||                   // start of pending read
                        (ldtNow > m_ldtForceHeartbeat && heartbeat())) // or time to force heartbeat
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

                scheduleDisconnect(new IOException("fatal ack timeout after " + dur));
                }
            else if (ldtNow >= ldtAckTimeout)
                {
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

                migrate(new IOException("ack timeout after " + dur));
                }
            // else; progressing towards timeout
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

            WriteBatch batchWriteUnflushed = m_batchWriteUnflushed;
            if (batchWriteUnflushed != null)
                {
                m_batchWriteUnflushed = null;
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
         *
         * @return a partial SelectionService.Handler interest set
         *
         * @throws IOException if an I/O error occurs
         */
        protected abstract int processReads(boolean fReady)
                throws IOException;

        /**
         * Write the contents of the WriteQueue to the channel.
         *
         * @param fReady  true iff the channel is writeable
         *
         * @return a partial SelectionService.Handler interest set
         *
         * @throws IOException if an I/O error occurs
         */
        protected int processWrites(boolean fReady)
                throws IOException
            {
            long cbBacklog = f_cbQueued.get();

            if (fReady || cbBacklog > 0)
                {
                WriteBatch batch       = m_batchWriteSendHead;
                long       cbBundle    = getAutoFlushThreshold();
                long       cbExcessive = getBacklogExcessiveThreshold();
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

                        if (cbBatch == 0 || batch.write()) // unlike in flush we don't need to sync since acks are also processed on this thread
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
                if (fBacklog)
                    {
                    if (cbBacklog < cbExcessive / 2)
                        {
                        m_fBacklog = false;
                        emitEvent(new SimpleEvent(Event.Type.BACKLOG_NORMAL, getPeer()));
                        }
                    }
                else if (cbBacklog > cbExcessive)
                    {
                    m_fBacklog = true;
                    emitEvent(new SimpleEvent(Event.Type.BACKLOG_EXCESSIVE, getPeer()));
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

            synchronized (this)
                {
                WriteBatch batch = m_batchWriteUnflushed;
                m_batchWriteUnflushed = null;
                if (batch != null)
                    {
                    enqueueWriteBatch(batch);
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
             * Append a message to the batch.
             *
             * This method is synchronized as it is always called from the app thread and
             * the batch may be concurrently being ack processed on the SS thread.
             *
             * @param bufHead       the buffer header to append
             * @param fRecycleHead  true iff the buffer header should be recycled
             * @param bufseqBody    optional buffer body to append
             * @param receipt       optional associated receipt
             *
             * @return the batch size
             */
            public synchronized long append(ByteBuffer bufHead, boolean fRecycleHead, BufferSequence bufseqBody, Object receipt)
                {
                addWriter();

                int cBufferAdd = 1 + (bufseqBody == null ? 0 : bufseqBody.getBufferCount());

                int          ofAdd     = ensureAdditionalBufferCapacity(cBufferAdd);
                Object[]     aoReceipt = m_aReceipt;
                ByteBuffer[] aBuffer   = m_aBuffer;
                long         cbBody    = 0;
                if (bufseqBody != null)
                    {
                    bufseqBody.getBuffers(0, cBufferAdd - 1, aBuffer, ofAdd + 1);
                    cbBody = bufseqBody.getLength();

                    populateMessageHeader(bufHead, aBuffer, ofAdd + 1, cBufferAdd, cbBody);
                    }

                aBuffer[ofAdd] = bufHead;

                long cb        = bufHead.remaining() + cbBody;
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
                            ? RECEIPT_MSG_MARKER_HEADER_RECYCLE // combo receipt indicating msg and recycling
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
                    // else; it is an ACK with a marker; do nothing
                    }
                else
                    {
                    aoReceipt[ofAdd + cBufferAdd - 1] = receipt;
                    ++m_cReceiptsUnflushed;
                    }

                m_ofAdd = ofAdd + cBufferAdd;

                BufferedConnection.this.f_cBytesUnacked.addAndGet(cbUnacked);

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

                // NOTE: f_cBytesUnacked and m_cReceiptsUnflushed don't need to be updated as it had already been
                // accounted for when producing the source batch

                // make the original batch unusable
                batchSrc.m_aReceipt = batchSrc.m_aBuffer = EMPTY_BUFFER_ARRAY;
                batchSrc.m_ofAck    = batchSrc.m_ofSend  = batchSrc.m_ofAdd = 0;
                batchSrc.m_cbBatch  = 0;
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
                ByteBuffer[] aBuffer = m_aBuffer;
                int          ofSend  = m_ofSend;
                int          ofAdd   = m_ofAdd;

                long cb = BufferedConnection.this.write(aBuffer, ofSend, ofAdd - ofSend);

                // advance offset, decrement cBuffer based on amount written
                if (cb > 0)
                    {
                    for (; ofSend < ofAdd && !aBuffer[ofSend].hasRemaining(); ++ofSend)
                        {}

                    m_cbBatch -= cb;
                    m_ofSend   = ofSend;
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
             * The total number of bytes remaining to write in the batch.
             */
            protected long m_cbBatch;

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
            }

        // ----- data members -------------------------------------------

        /**
         * The tail of the write queue.
         *
         * This is only accessed while synchronized on the connection.
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
         * ByteBuffer to write Receipt messages
         */
        protected ByteBuffer m_bufferRecycleOutboundReceipts;

        /**
         * The number of threads waiting to use the connection.
         */
        protected final AtomicInteger m_cWritersWaiting = new AtomicInteger();

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
         * Reusable array for holding receipts.
         */
        protected final Object[] f_aoReceiptTmp = new Object[16];
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
     * Empty buffer array for use in bundling.
     */
    protected static final ByteBuffer[] EMPTY_BUFFER_ARRAY = new ByteBuffer[0];
    }
