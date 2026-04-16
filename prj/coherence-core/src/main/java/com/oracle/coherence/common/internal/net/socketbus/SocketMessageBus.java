/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.internal.net.socketbus;


import com.oracle.coherence.common.base.Disposable;
import com.oracle.coherence.common.internal.net.ProtocolIdentifiers;
import com.oracle.coherence.common.internal.util.HeapDump;
import com.oracle.coherence.common.io.Buffers;
import com.oracle.coherence.common.net.exabus.MessageBus;
import com.oracle.coherence.common.net.exabus.EndPoint;
import com.oracle.coherence.common.net.exabus.Event;
import com.oracle.coherence.common.net.exabus.util.UrlEndPoint;
import com.oracle.coherence.common.net.exabus.util.SimpleEvent;
import com.oracle.coherence.common.io.BufferSequence;
import com.oracle.coherence.common.io.BufferManager;
import com.oracle.coherence.common.io.BufferSequenceInputStream;
import com.oracle.coherence.common.util.MemorySize;

import java.io.IOException;
import java.io.DataInput;
import java.nio.ByteBuffer;

import java.util.Arrays;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

import java.util.LinkedList;
import java.util.zip.CRC32;

import java.net.SocketException;

import java.util.logging.Level;


/**
 * SocketMessageBus is a reliable MessageBus implementation based
 * upon sockets.
 *
 * @author mf  2010.12.03
 */
public class SocketMessageBus
        extends BufferedSocketBus
        implements MessageBus
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a SocketMessageBus.
     *
     *
     * @param driver     the socket driver
     * @param pointLocal the local endpoint
     *
     * @throws IOException if an I/O error occurs
     */
    public SocketMessageBus(SocketBusDriver driver, UrlEndPoint pointLocal)
            throws IOException
        {
        super(driver, pointLocal);
        }


    // ----- AbstractSocketBus methods --------------------------------------

    /**
     * {@inheritDoc}
     */
    protected String getProtocolName()
        {
        return getSocketDriver().getDependencies().getMessageBusProtocol();
        }

    /**
     * {@inheritDoc}
     */
    protected int getProtocolIdentifier()
        {
        return ProtocolIdentifiers.SOCKET_MESSAGE_BUS;
        }


    // ----- MessageBus methods ---------------------------------------------

    /**
     * {@inheritDoc}
     */
    public void send(EndPoint peer, BufferSequence bufseq, Object receipt)
        {
        send(peer, bufseq, receipt, /*fSocketWrite*/false);
        }

    /**
     * {@inheritDoc}
     */
    public void send(EndPoint peer, BufferSequence bufseq, Object receipt, boolean fSocketWrite)
        {
        if (bufseq == null)
            {
            throw new NullPointerException("Null BufferSequence for message:  " + receipt);
            }

        MessageConnection conn = (MessageConnection) ensureConnection(peer);
        if (conn.getProtocolVersion() < 0 && conn.deferSend(bufseq, receipt))
            {
            // we haven't finished handshaking yet
            return;
            }

        long cbMsg;
        ByteBuffer header = conn.prepareHeader(bufseq);
        cbMsg = header.remaining() + bufseq.getLength();
        conn.enqueue(new SendEntry(header, bufseq, receipt, cbMsg));

        if (!conn.isValid())
            {
            return;
            }

        conn.coordinatePostSend(fSocketWrite, !isTransportCallbackThread());
        }


    // ----- helpers --------------------------------------------------------

    /**
     * ThreadLocal header buffer slab.
     */
    private static final ThreadLocal<ByteBuffer> TL_HEADER =
            ThreadLocal.withInitial(() -> ByteBuffer.allocateDirect(1024));

    /**
     * ThreadLocal CRC32 used for message header preparation.
     */
    private static final ThreadLocal<CRC32> TL_CRC =
            ThreadLocal.withInitial(CRC32::new);

    /**
     * Lock-free MPSC queue entry for user message sends.
     */
    static class SendEntry
        {
        SendEntry(ByteBuffer header, BufferSequence body, Object receipt, long cb)
            {
            this.header  = header;
            this.body    = body;
            this.receipt = receipt;
            this.cb      = cb;
            }

        /**
         * Message header.
         */
        ByteBuffer header;

        /**
         * Message body.
         */
        BufferSequence body;

        /**
         * Message receipt.
         */
        Object receipt;

        /**
         * Message size in bytes (header + body).
         */
        long cb;

        /**
         * Next entry in intrusive list.
         */
        volatile SendEntry next;

        /**
         * Clear the payload once the entry has been drained and appended.
         */
        void clearPayload()
            {
            header  = null;
            body    = null;
            receipt = null;
            cb      = 0L;
            }
        }


    // ----- MessageConnection interface ------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    protected Connection makeConnection(UrlEndPoint peer)
        {
        return new MessageConnection(peer);
        }

    /**
     * MessageConnection implements a reliable message connection,
     */
    public class MessageConnection
        extends BufferedConnection
        {
        /**
         * Create a MessageConnection for the specified peer.
         *
         * @param peer  the peer
         */
        public MessageConnection(UrlEndPoint peer)
            {
            super(peer);
            }

        /**
         * Return the maximum size of a consumer-owned write batch before a new batch should be started.
         *
         * @return the maximum batch size in bytes
         */
        protected long getMaxWriteBatchBytes()
            {
            long cb = getSocketDriver().getDependencies().getMaxWriteBatchBytes();
            return cb > 0 ? cb : getAutoFlushThreshold();
            }

        /**
         * Return the maximum number of messages to accumulate into a consumer-owned write batch.
         *
         * @return the maximum message count, or a non-positive value if disabled
         */
        protected int getMaxWriteBatchMessages()
            {
            return getSocketDriver().getDependencies().getMaxWriteBatchMessages();
            }

        /**
         * Return the backlog threshold at which producer threads should stop free-running and wait for the
         * connection to catch up.
         *
         * @return the producer backpressure threshold in bytes
         */
        protected long getProducerBackpressureThresholdBytes()
            {
            long cb = getSocketDriver().getDependencies().getProducerBackpressureThresholdBytes();
            return cb > 0 ? cb : Math.max(128L * 1024L, getMaxWriteBatchBytes() << 1);
            }

        /**
         * Coordinate post-send progression and apply producer-side pacing once the connection backlog grows too large.
         *
         * @param fSocketWrite  true if the caller is willing to offer its cpu to socket writes
         * @param fCanBlock     true if the caller may be backpressured waiting for the connection backlog to drain
         */
        protected void coordinatePostSend(boolean fSocketWrite, boolean fCanBlock)
            {
            long cbBackpressure = getProducerBackpressureThresholdBytes();
            long cbResume       = Math.max(32L * 1024L, cbBackpressure >>> 1);
            long cbPublished    = getPublishedBacklogBytes();
            boolean fPressure   = cbPublished > cbBackpressure;

            if (tryActivateWriter())
                {
                // Once producers are being throttled, progression must behave like an explicit flush rather than
                // a soft auto-flush; otherwise the connection can livelock with a "small" batch parked forever.
                scheduleActiveWriteProgression(fSocketWrite, /*fAuto*/ !fPressure);
                }

            if (fPressure && fCanBlock)
                {
                awaitWriteBacklog(cbResume, fSocketWrite);
                }
            }

        /**
         * Return producer-visible connection backlog, excluding already-acked data.
         *
         * @return the number of outstanding producer-visible bytes
         */
        protected long getPublishedBacklogBytes()
            {
            return Math.max(0L, f_cbQueued.get() + m_cbPending.get());
            }

        /**
         * Wait for connection backlog to fall back to a reasonable level, nudging write progression as needed.
         *
         * @param cbResume      the backlog threshold below which the caller may resume publishing freely
         * @param fSocketWrite  true if the caller is willing to offer its cpu to socket writes
         */
        protected void awaitWriteBacklog(long cbResume, boolean fSocketWrite)
            {
            for (int i = 0; isValid() && getPublishedBacklogBytes() > cbResume; ++i)
                {
                if (tryActivateWriter())
                    {
                    scheduleActiveWriteProgression(fSocketWrite, /*fAuto*/ false);
                    }

                if ((i & 0x0F) == 0)
                    {
                    try
                        {
                        wakeup();
                        }
                    catch (IOException e)
                        {
                        onException(e);
                        break;
                        }
                    }

                LockSupport.parkNanos(100_000L);
                }
            }

        /**
         * Return the threshold at which to stop reading additional data from
         * the socket, based on the number of undisposed bytes in the event
         * queue.
         *
         * @return  the threshold in bytes
         */
        protected long getReadThrottleThreshold()
            {
            long cb = m_cbReadThreshold;

            if (cb <= 0)
                {
                try
                    {
                    // threshold is a multiple of the underlying buffer size
                    // the assumption is that any given socket read could
                    // return a full buffers worth of data, and that should
                    // not be considered to be "excessive", but once we've
                    // fallen a number of buffers worth behind it is time to
                    // throttle.  While this could be made a configuration
                    // setting, it is not necessary since the underlying
                    // buffer size is already configurable and will directly
                    // influence this setting.
                    m_cbReadThreshold = cb = getReceiveBufferSize() * 8;
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
         * {@inheritDoc}
         */
        protected int processReads(boolean fReady)
                throws IOException
            {
            if (f_fBacklogLocal.get() && m_cbEventQueue.get() > getReadThrottleThreshold())
                {
                // if a backlog has been declared *and* we've exceeded the read threshold then stop reading; once
                // the backlog is cleared (BACKLOG_EXCESSIVE event consumed) then we'll be awoken so we can continue.
                // Note if we only checked f_fBacklogLocal then we'd end up hurting performance as at the time the backlog
                // was cleared we'd have no messages ready to be processed.  If we only checked m_cbEventQueue then we'd
                // risk stalling indefinitely as we could cross the limit temporarily and uncross it before a backlog
                // was ever declared.  Thus we check both.
                return 0;
                }
            else if (fReady)
                {
                ReadBatch batch = m_readBatch;
                if (batch == null)
                    {
                    batch = m_readBatch = new ReadBatch();
                    batch.m_cbRequired = getMessageHeaderSize();
                    }

                batch.read();

                if (batch.m_fHeader && batch.m_cbReadable == 0)
                    {
                    // we've fully read the current message and there was no partial message
                    // in the buffer, so it could potentially be awhile before we use this
                    // connection again.  Avoid needlessly holding onto the read buffer memory.
                    // Note, in a high throughput case it is likely that we'll have read at least some
                    // of the next message and thus avoid the cost associated with allocation/disposal

                    batch.dispose();
                    m_readBatch = null;
                    return OP_READ;
                    }
                else
                    {
                    // we've at least partially read the next message, assume the remainder will be here
                    // soon
                    return OP_READ | OP_EAGER;
                    }
                }

            return OP_READ;
            }

        @Override
        public void dispose()
            {
            ReadBatch batch = m_readBatch;
            if (batch != null)
                {
                m_readBatch = null;
                batch.dispose();
                }

            super.dispose();
            }

        /**
         * Prepare a message header using a ThreadLocal slab.
         *
         * @param bufseq  the message body
         *
         * @return the populated header
         */
        ByteBuffer prepareHeader(BufferSequence bufseq)
            {
            long cbBody = bufseq.getLength();
            int  nProt  = getProtocolVersion();
            if (cbBody < 0 || (nProt < 5 && cbBody > Integer.MAX_VALUE))
                {
                throw new UnsupportedOperationException("unsupported message size " + cbBody);
                }

            int        cbHeader = getMessageHeaderSize();
            ByteBuffer slab     = TL_HEADER.get();

            if (slab.remaining() < cbHeader)
                {
                slab = ByteBuffer.allocateDirect(1024);
                TL_HEADER.set(slab);
                ++m_cHeaderSlabAllocs;
                }

            int        nSlabPos = slab.position();
            ByteBuffer header   = slab.slice();
            header.limit(cbHeader);
            slab.position(nSlabPos + cbHeader);

            int  nPos   = header.position();

            if (nProt > 4)
                {
                header.putLong(nPos, cbBody);

                int   lCrcBody = 0;
                CRC32 crc32    = f_crcTx == null ? null : TL_CRC.get();
                if (crc32 != null)
                    {
                    crc32.reset();
                    int          cBuffers = bufseq.getBufferCount();
                    ByteBuffer[] aBodyBuf = new ByteBuffer[cBuffers];
                    bufseq.getBuffers(0, cBuffers, aBodyBuf, 0);
                    lCrcBody = Buffers.updateCrc(crc32, aBodyBuf, 0, cbBody);
                    lCrcBody = lCrcBody == 0 ? 1 : lCrcBody;
                    }
                header.putInt(nPos + 8, lCrcBody);

                int lCrcHeader = 0;
                if (crc32 != null)
                    {
                    crc32.reset();
                    int nLimit = header.limit();
                    header.limit(nPos + 12);
                    lCrcHeader = Buffers.updateCrc(crc32, header);
                    lCrcHeader = lCrcHeader == 0 ? 1 : lCrcHeader;
                    header.limit(nLimit);
                    }
                header.putInt(nPos + 12, lCrcHeader);
                }
            else
                {
                header.putInt(nPos, (int) cbBody);
                }

            return header;
            }

        /**
         * Enqueue a prepared send entry.
         *
         * @param entry  the send entry
         */
        void enqueue(SendEntry entry)
            {
            long cPending = m_cPendingMsgs.incrementAndGet();
            long cbPending = m_cbPending.addAndGet(entry.cb);
            m_cPendingMsgsPeak = Math.max(m_cPendingMsgsPeak, cPending);
            m_cbPendingPeak = Math.max(m_cbPendingPeak, cbPending);

            entry.next = null;
            m_queueFifoMsTail.getAndSet(entry).next = entry;
            }

        @Override
        protected long drainQueue()
            {
            return drainQueueFifoMs();
            }

        /**
         * Drain the producer queue when using the inline MPSC FIFO mode.
         *
         * @return drained bytes
         */
        protected long drainQueueFifoMs()
            {
            SendEntry entry = pollQueueFifoMs();
            if (entry == null)
                {
                return 0L;
                }

            WriteBatch batch = m_batchWriteUnflushed;
            if (batch == null)
                {
                batch = m_batchWriteUnflushed = new WriteBatch();
                }

            long cbBatchMax = getMaxWriteBatchBytes();
            int  cBatchMax  = getMaxWriteBatchMessages();
            long cbDrained  = 0;
            long cMsg       = 0;
            long cNullRcpt  = 0;

            do
                {
                if (batch.getLength() > 0 &&
                    ((cbBatchMax > 0 && batch.getLength() + entry.cb > cbBatchMax) ||
                     (cBatchMax > 0 && batch.getMessageCount() >= cBatchMax)))
                    {
                    enqueueWriteBatch(detachUnflushedWriteBatch());
                    batch = m_batchWriteUnflushed = new WriteBatch();
                    }

                appendPreparedForDrain(batch, entry);
                cbDrained += entry.cb;
                ++cMsg;
                if (entry.receipt == null)
                    {
                    ++cNullRcpt;
                    }
                entry.clearPayload();
                }
            while ((entry = pollQueueFifoMs()) != null);

            m_cbPending.addAndGet(-cbDrained);
            m_cPendingMsgs.addAndGet(-cMsg);
            m_cMsgUserOut += cMsg;
            m_cReceiptsNull += cNullRcpt;
            ++m_cDrainCalls;
            m_cDrainedMsgs += cMsg;
            m_cbDrained += cbDrained;

            return cbDrained;
            }

        @Override
        protected boolean hasProducerWriteWork()
            {
            return m_queueFifoMsHead != m_queueFifoMsTail.get();
            }

        /**
         * Poll a single entry from the inline MPSC FIFO queue.
         *
         * @return the next drained entry, or {@code null} if the queue is empty
         */
        protected SendEntry pollQueueFifoMs()
            {
            SendEntry head = m_queueFifoMsHead;
            SendEntry next = head.next;

            if (next == null)
                {
                if (head == m_queueFifoMsTail.get())
                    {
                    return null;
                    }

                do
                    {
                    Thread.onSpinWait();
                    next = head.next;
                    }
                while (next == null);
                }

            head.next = null;
            m_queueFifoMsHead = next;
            return next;
            }

        /**
         * Append a drained producer entry into the consumer-owned batch.
         *
         * @param batch  the consumer-owned batch
         * @param entry  the producer-published send entry
         */
        protected void appendPreparedForDrain(WriteBatch batch, SendEntry entry)
            {
            batch.appendPreparedMessageConsumer(entry.header, entry.body, entry.receipt);
            }

        @Override
        protected boolean shouldDeferSmallAutoFlushBatch(WriteBatch batch)
            {
            // The MPSC producer path must not leave consumer-owned batches parked indefinitely awaiting more
            // traffic. Correctness requires idle connections to flush pending responses promptly.
            return false;
            }

        @Override
        protected long getProducerPendingBytes()
            {
            return m_cbPending.get();
            }

        @Override
        protected long getProducerPendingMessages()
            {
            return m_cPendingMsgs.get();
            }

        @Override
        protected long getBacklogSignalBytes()
            {
            return getPublishedBacklogBytes();
            }

        @Override
        protected long getBacklogSignalExcessiveThreshold()
            {
            return getProducerBackpressureThresholdBytes();
            }

        @Override
        protected long getBacklogSignalNormalThreshold()
            {
            long cbBackpressure = getProducerBackpressureThresholdBytes();
            return Math.max(32L * 1024L, cbBackpressure >>> 1);
            }

        @Override
        protected String getPerfTraceDetail()
            {
            return "mpsc(drainCalls=" + m_cDrainCalls
                    + ", queueMode=fifo-ms"
                    + ", consumerAppendFast=true"
                    + ", drainedMsgs=" + m_cDrainedMsgs
                    + ", drainedBytes=" + new MemorySize(Math.max(0L, m_cbDrained))
                    + ", pendingPeakMsgs=" + m_cPendingMsgsPeak
                    + ", pendingPeakBytes=" + new MemorySize(Math.max(0L, m_cbPendingPeak))
                    + ", headerSlabAllocs=" + m_cHeaderSlabAllocs + ")";
            }

        @Override
        protected void populateMessageHeader(ByteBuffer bufHead, ByteBuffer[] aBuffer, int of, int cBuffers, long cbBuffer)
            {
            int nProt = getProtocolVersion();
            int nPos  = bufHead.position();
            if (nProt > 4)
                {
                CRC32 crc32    = f_crcTx;
                int   lCrcBody = 0;
                int   nLimit   = bufHead.limit();

                // write message length
                bufHead.putLong(nPos, cbBuffer);
                nPos += 8;

                // write body CRC
                if (crc32 != null)
                    {
                    crc32.reset();
                    lCrcBody = Buffers.updateCrc(crc32, aBuffer, of, cbBuffer);
                    lCrcBody = lCrcBody == 0 ? 1 : lCrcBody;
                    }

                bufHead.putInt(nPos, lCrcBody);

                nPos += 4;

                // write header CRC
                int lCrcHeader = 0;
                if (crc32 != null)
                    {
                    crc32.reset();
                    bufHead.limit(nPos); // set limit for crc calc
                    lCrcHeader = Buffers.updateCrc(crc32, bufHead);
                    lCrcHeader = lCrcHeader == 0 ? 1 : lCrcHeader;
                    bufHead.limit(nLimit); // reset limit for crc write
                    }

                bufHead.putInt(nPos, lCrcHeader);
                }
            else
                {
                bufHead.putInt(nPos, (int) cbBuffer);
                }
            }

        /**
         * Return message header size based on protocol version.
         *
         * @return message header size
         */
        public int getMessageHeaderSize()
            {
            int nProt = getProtocolVersion();
            if (nProt > 4)
                {
                return 16;  // 8B body length + 4B body crc, 4B header crc
                }
            else if (nProt >= 0)
                {
                return 4; // 4B body length
                }
            else
                {
                throw new IllegalStateException("connection is not ready!");
                }
            }

        @Override
        protected int getReceiptSize()
            {
            int nProt = getProtocolVersion();

            return nProt > 4 ? 25 : 13;  // header + body (9 bytes)
            }

        @Override
        protected void setProtocolVersion(int nProt)
            {
            // drain the requests sent before this point
            lock();
            try
                {
                super.setProtocolVersion(nProt);

                LinkedList<Pair<BufferSequence, Object>> queue = m_queuePreNegotiate;
                if (queue != null)
                    {
                    if (m_fBacklog)
                        {
                        m_fBacklog = false;
                        emitEvent(new SimpleEvent(Event.Type.BACKLOG_NORMAL, getPeer()));
                        }

                    Pair<BufferSequence, Object> pair = null;
                    try
                        {
                        while (true)
                            {
                            pair = queue.poll();
                            if (pair == null)
                                {
                                break;
                                }
                            BufferSequence bufSeq   = pair.getKey();
                            Object         oReceipt = pair.getValue();
                            ByteBuffer     header   = prepareHeader(bufSeq);
                            long           cbMsg    = header.remaining() + bufSeq.getLength();

                            // This replay runs on the SelectionService thread as part of handshake completion.
                            // Producer-side backpressure must not park the selector, or no one remains to drain
                            // the queued writes and the connection deadlocks before the first socket write.
                            enqueue(new SendEntry(header, bufSeq, oReceipt, cbMsg));
                            }
                        flush(/*fSocketWrite*/ true);

                        m_queuePreNegotiate = null;
                        }
                    catch (Throwable e)
                        {
                        queue.addFirst(pair);
                        scheduleDisconnect(e);
                        }
                    }
                }
            finally
                {
                unlock();
                }
            }

        @Override
        public void drainReceipts()
            {
            lock();
            try
                {
                LinkedList<Pair<BufferSequence, Object>> queue = m_queuePreNegotiate;
                m_queuePreNegotiate = null;
                if (queue != null)
                    {
                    for (Pair<BufferSequence, Object> pair : queue)
                        {
                        Object oReceipt = pair.getValue();
                        if (oReceipt != null)
                            {
                            addEvent(new SimpleEvent(Event.Type.RECEIPT, getPeer(), oReceipt));
                            }
                        }

                    // need to change backlog status, see connection.deferSend
                    if (m_fBacklog)
                        {
                        m_fBacklog = false;
                        emitEvent(new SimpleEvent(Event.Type.BACKLOG_NORMAL, getPeer()));
                        }
                    }

                super.drainReceipts();
                }
            finally
                {
                unlock();
                }
            }

        @Override
        public String toString()
            {
            ReadBatch batch            = m_readBatch;
            long      cReceiptsEmitted = m_cReceiptsEmitted;
            long      cReceiptsNull    = m_cReceiptsNull;
            return super.toString() + ", bufferedIn(" + (batch == null ? "" : batch) + "), " +
                    "msgs(in=" + m_cMsgUserIn +
                    ", out=" + m_cReceiptsEmitted + (cReceiptsNull == 0 ? "" : "[" + (cReceiptsEmitted + cReceiptsNull) + "]") +
                    "/" + m_cMsgUserOut + ")";
            }

        // ----- inner class: ReadBatch -------------------------------------

        /**
         * ReadBatch handles the reading and processing of the inbound byte
         * stream.
         */
        public class ReadBatch
                extends AtomicReference<ByteBuffer>
                implements Disposable, SharedBuffer.Disposer
            {
            /**
             * Ensure that m_aBuffer has sufficient writable capacity.
             *
             * @param cb  the required write capacity
             *
             * @return the ensured buffer array
             */
            public ByteBuffer[] ensureCapacity(long cb)
                {
                BufferManager manager         = getSocketDriver().getDependencies().getBufferManager();
                ByteBuffer[]  aBuffer         = m_aBuffer;
                int           ofReadable      = m_ofReadable;
                int           ofWritable      = m_ofWritable;
                long          cbWritable      = m_cbWritable;
                int           cBufferWritable = m_cBufferWritable;
                int           cBuffer         = aBuffer.length;
                long          cbAlloc         = cb - cbWritable;
                long          cbMin           = Math.max(getPacketSize(), 16*1024); // minimum read buffer size, used to reduce the number of read system calls

                while (cbAlloc > 0)
                    {
                    int ofEnd = ofWritable + cBufferWritable;
                    int ofAlloc;
                    if (ofEnd < cBuffer && aBuffer[ofEnd] == null)
                        {
                        // next block has not been allocated
                        ofAlloc = ofEnd;
                        }
                    else if (ofEnd + 1 < cBuffer)
                        {
                        // there are free slots after the write tail
                        ofAlloc = ofEnd + 1;
                        }
                    else if (ofReadable > 0)
                        {
                        // there are free slots at the head, compact and allocate
                        // at the tail; shifting everything back by ofReadable
                        ofAlloc = ofEnd - ofReadable;
                        System.arraycopy(aBuffer, ofReadable, aBuffer, 0, ofAlloc);
                        Arrays.fill(aBuffer, ofAlloc, ofAlloc + ofReadable, null);
                        ofWritable -= ofReadable;
                        ofEnd      -= ofReadable;
                        ofReadable  = 0;
                        }
                    else
                        {
                        // there are no free slots, new array is required
                        ByteBuffer[] aBufferNew = new ByteBuffer[cBuffer * 2];
                        System.arraycopy(aBuffer, 0, aBufferNew, 0, cBuffer);

                        aBuffer = aBufferNew;
                        ofAlloc = cBuffer;
                        cBuffer = aBuffer.length;
                        }

                    // attempt to allocate at least the required amount plus a bit of extra
                    long cbStop = Math.max(cbMin, cbWritable + cbAlloc + getMessageHeaderSize());
                    for (int i = ofAlloc; i < cBuffer && cbWritable < cbStop; ++i)
                        {
                        ByteBuffer buff = getAndSet(null);
                        if (buff == null)
                            {
                            buff = manager.acquirePref((int) Math.min(Integer.MAX_VALUE, cbStop - cbWritable));
                            }
                        buff.clear().mark();
                        int cbBuff  = buff.remaining();
                        cbAlloc    -= Math.min(cbBuff, cbAlloc);
                        cbWritable += cbBuff;
                        ++cBufferWritable;
                        aBuffer[i] = buff;
                        }
                    }

                m_aBuffer         = aBuffer;
                m_ofReadable      = ofReadable;
                m_ofWritable      = ofWritable;
                m_cbWritable      = cbWritable;
                m_cBufferWritable = cBufferWritable;

                return aBuffer;
                }

            /**
             * {@inheritDoc}
             */
            @Override
            public void dispose()
                {
                // we must dispose of buffers in m_aBuffer which have not
                // already been handed out as messages, specifically everything
                // after m_ofReadable has to be released

                int                         of         = m_ofReadable;
                SharedBuffer                buffShared = m_bufferShared;
                ByteBuffer[]                aBuffer    = m_aBuffer;
                ByteBuffer                  buffer0    = aBuffer[of];
                if (buffShared != null && buffShared.get() == buffer0)
                    {
                    buffShared.dispose();
                    ++of;
                    }
                BufferManager manager = getSocketDriver().getDependencies().getBufferManager();
                for (int c = m_ofWritable + m_cBufferWritable; of < c; ++of)
                    {
                    manager.release(aBuffer[of]);
                    }
                ByteBuffer buf = getAndSet(Buffers.getEmptyBuffer());
                if (buf != null)
                    {
                    manager.release(buf);
                    }
                }


            @Override
            public void dispose(ByteBuffer buffer)
                {
                if (!compareAndSet(null, buffer))
                    {
                    getSocketDriver().getDependencies().getBufferManager().release(buffer);
                    }
                }

            /**
             * Process reads.
             *
             * @throws IOException on an I/O error
             */
            public void read()
                    throws IOException
                {
                long         cbAlloc = Math.abs(m_cbRequired) - m_cbWritable;
                ByteBuffer[] aBuffer = cbAlloc > 0 ? ensureCapacity(cbAlloc) : m_aBuffer;
                int          of      = m_ofWritable;
                int          cBuffer = m_cBufferWritable;

                long cb = MessageConnection.this.read(aBuffer, of, cBuffer);
                if (cb > 0)
                    {
                    for (; cBuffer > 0 && !aBuffer[of].hasRemaining(); ++of, --cBuffer)
                        {
                        aBuffer[of].reset(); // prepare for reading
                        }

                    m_ofWritable       = of;
                    m_cBufferWritable  = cBuffer;
                    m_cbWritable      -= cb;
                    long cbReady = m_cbReadable += cb;

                    if (cbReady >= Math.abs(m_cbRequired))
                        {
                        if (cBuffer > 0 && aBuffer[of].position() > 0)
                            {
                            ByteBuffer buffLast  = aBuffer[of];
                            int        iPosWrite = buffLast.position();
                            // the last buffer is partially readable
                            try
                                {
                                buffLast.limit(iPosWrite).reset();
                                onReady();
                                }
                            finally
                                {
                                // by definition buffLast still has some space
                                // left to write into, reset the position data;
                                // any message referencing this buffer would be
                                // doing so through a slice of a SharedBuffer
                                buffLast.mark();
                                buffLast.limit(buffLast.capacity()).position(iPosWrite);
                                }
                            }
                        else
                            {
                            onReady();
                            }
                        }
                    }
                else if (cb < 0)
                    {
                    migrate(new IOException("input shutdown"));
                    }
                }


            /**
             * Process the next message(s) from the already read data.
             *
             * @throws IOException on an I/O error
             */
            public void onReady()
                    throws IOException
                {
                boolean          fFlush      = false;
                long             cbReadable  = m_cbReadable;
                long             cbRequired  = m_cbRequired;
                boolean          fHeader     = m_fHeader;
                final AtomicLong cbEvents    = m_cbEventQueue;
                long             cMsgIn      = m_cMsgIn;
                long             cMsgInUser  = m_cMsgUserIn;
                long             cMsgInSkip  = m_cMsgInSkip;
                SharedBuffer     buffShared0 = m_bufferShared;
                long             lCrcBody    = m_lCrcBodyNext;
                int              cbHeader    = getMessageHeaderSize();
                int              nProt       = getProtocolVersion();
                CRC32            crc32       = f_crcRx;

                ByteBuffer       buffer0;

                if (buffShared0 == null)
                    {
                    buffShared0 = m_bufferShared = new SharedBuffer(buffer0 = m_aBuffer[m_ofReadable], this);
                    }
                else
                    {
                    buffer0 = buffShared0.get();
                    }

                try
                    {
                    // process all available headers and messages
                    while (cbReadable >= Math.abs(cbRequired))
                        {
                        if (fHeader)
                            {
                            // ofReadable is left as is, the buffers will be part
                            // of the message, though not accessible
                            fHeader     = false;
                            cbReadable -= cbHeader;

                            int          of    = m_ofReadable;
                            ByteBuffer[] aBuff = m_aBuffer;

                            long lCrcHeaderRx;  // header CRC received
                            long lCrcHeaderCalc = 0; // recalculated header CRC
                            if (nProt > 4)
                                {
                                if (crc32 != null)
                                    {
                                    crc32.reset();
                                    lCrcHeaderCalc = Buffers.updateCrc(crc32, aBuff, of, cbHeader - 4);
                                    lCrcHeaderCalc = lCrcHeaderCalc == 0 ? 1 : lCrcHeaderCalc;
                                    }

                                cbRequired     = Buffers.getLong(aBuff, of);
                                lCrcBody       = Buffers.getInt(aBuff, of);
                                lCrcHeaderRx   = Buffers.getInt(aBuff, of);

                                if (lCrcHeaderCalc != lCrcHeaderRx && lCrcHeaderRx != 0 && lCrcHeaderCalc != 0)
                                    {
                                    // CRC does not match, migrate connection
                                    throw new IOException("incorrect CRC, corrupted header buffer; CRC read: " +
                                            lCrcHeaderRx + " CRC re-calculated: " + lCrcHeaderCalc);
                                    }
                                }
                            else
                                {
                                cbRequired = Buffers.getInt(aBuff, of);
                                }
                            }
                        else // message body
                            {
                            long cbMsg = Math.abs(cbRequired);

                            Event event;
                            long lCrcBodyCalc = 0;
                            if (buffer0.remaining() >= cbMsg) // highly optimized single buffer message
                                {
                                if (crc32 != null && lCrcBody != 0)
                                    {
                                    crc32.reset();
                                    lCrcBodyCalc = Buffers.updateCrc(crc32, buffer0, cbMsg);
                                    lCrcBodyCalc = lCrcBodyCalc == 0 ? 1 : lCrcBodyCalc;
                                    if (lCrcBodyCalc != lCrcBody)
                                        {
                                        // checksum does not match, migrate connection
                                        throw new IOException("incorrect CRC, corrupted message buffer; CRC read: " +
                                                lCrcBody + " CRC re-calculated: " + lCrcBodyCalc);
                                        }
                                    }

                                int nPos = buffer0.position();
                                event = new SingleBufferMessageEvent(MessageConnection.this,
                                            buffShared0.attach(), nPos, (int) cbMsg);
                                buffer0.position(nPos + (int) cbMsg);
                                }
                            else // multi-buffer message
                                {
                                if (crc32 != null && lCrcBody != 0)
                                    {
                                    crc32.reset();
                                    lCrcBodyCalc = Buffers.updateCrc(crc32, m_aBuffer, m_ofReadable, cbMsg);
                                    lCrcBodyCalc = lCrcBodyCalc == 0 ? 1 : lCrcBodyCalc;
                                    if (lCrcBodyCalc != lCrcBody)
                                        {
                                        //  checksum does not match, migrate connection
                                        throw new IOException("incorrect checksum, corrupted message buffer");
                                        }
                                    }

                                event = makeMultiBufferMessageEvent(cbMsg);


                                // re-read updated fields
                                buffShared0 = m_bufferShared;
                                buffer0     = buffShared0.get();
                                }

                            cbEvents.addAndGet(cbMsg);

                            if (cMsgInSkip == 0)
                                {
                                fFlush = true;

                                if (cbRequired >= 0)
                                    {
                                    ++cMsgIn;
                                    ++cMsgInUser;
                                    addEvent(event);
                                    }
                                else if (onControlMessage(event))
                                    {
                                    ++cMsgIn;
                                    }
                                else // onControl returned false; thus it was a SYNC message, re-read skip count
                                    {
                                    cMsgInSkip = m_cMsgInSkip;
                                    }
                                }
                            else // we have post migration messages to skip
                                {
                                --cMsgInSkip;
                                getLogger().log(Level.FINER, "{0} skipping" + (cbRequired < 0 ? " control " : " ")
                                                + "message of {1} bytes from {2} after migration, {3} remain to be skipped on {4}",
                                        new Object[]{getLocalEndPoint(), cbMsg, getPeer(), cMsgInSkip, MessageConnection.this});

                                if (cMsgInSkip == 0)
                                    {
                                    getLogger().log(Level.FINE, "{0} resuming migrated connection with {1}",
                                            new Object[]{getLocalEndPoint(), MessageConnection.this});
                                    }
                                event.dispose();
                                }

                            fHeader     = true;
                            cbReadable -= cbMsg;
                            cbRequired  = cbHeader;
                            }
                        }
                    }
                finally
                    {
                    m_cMsgUserIn = cMsgInUser;
                    m_cMsgIn       = cMsgIn;
                    m_cMsgInSkip   = cMsgInSkip;
                    m_cbReadable   = cbReadable;
                    m_cbRequired   = cbRequired;
                    m_fHeader      = fHeader;
                    m_lCrcBodyNext = lCrcBody;

                    if (fFlush)
                        {
                        if (cbEvents.get() > (getReadThrottleThreshold() * 2) / 3)
                            {
                            // At 2/3 of the threshold we consider ourselves "backlogged",
                            // we will add a task to eventLast to end this backlog
                            // when the event is consumed. Note that we will stop
                            // adding to the backlog when we hit the threshold, which
                            // means that the event queue should have another 1/3 worth
                            // of content when we end the backlog, hopefully prevent
                            // the queues from being bursty.

                            // logic is factored out for hot-spotting benefit
                            issueLocalBacklog();
                            }

                        flushEvents();
                        }
                    }
                }

            /**
             * Extract a multi-buffer message from an array of ByteBuffers.
             *
             * @param cbMsg  the message length
             *
             * @return the message event
             */
            protected MultiBufferMessageEvent makeMultiBufferMessageEvent(long cbMsg)
                {
                ByteBuffer[] aBuffer    = m_aBuffer;
                SharedBuffer buffShared = m_bufferShared;
                int          ofReadable = m_ofReadable;
                long         cbEnd      = cbMsg;
                int          ofEnd      = ofReadable;

                // find the end buffer
                for (int cbBuf = aBuffer[ofEnd].remaining();
                     cbEnd > cbBuf;
                     cbBuf = aBuffer[++ofEnd].remaining())
                    {
                    cbEnd -= cbBuf;
                    }

                int cBufferMsg = (ofEnd - ofReadable) + 1;

                ByteBuffer buffer0 = aBuffer[ofReadable];
                ByteBuffer bufferN = aBuffer[ofEnd];

                int iLimitN = bufferN.limit();
                bufferN.limit(bufferN.position() + (int) cbEnd);

                ByteBuffer[] aBufferMsg = new ByteBuffer[cBufferMsg];

                // copy middle buffers
                System.arraycopy(aBuffer, ofReadable + 1, aBufferMsg, 1, cBufferMsg - 2);

                // no slice required here, this is the last
                // message to use this buffer
                aBufferMsg[0] = buffer0;
                SharedBuffer buffShared0 = buffShared;

                // replace buffShared with new SharedBuffer around bufferN
                m_bufferShared = buffShared = new SharedBuffer(bufferN, this);

                aBufferMsg[cBufferMsg - 1] = bufferN.slice();

                MultiBufferMessageEvent event = new MultiBufferMessageEvent(
                        MessageConnection.this,
                        getSocketDriver().getDependencies().getBufferManager(),
                        aBufferMsg, 0, cBufferMsg, cbMsg,
                        buffShared0.attach(), buffShared.attach());
                buffShared0.dispose();

                // update the read position restore the limit of the
                // last buffer (for next message)
                bufferN.position(bufferN.limit()).limit(iLimitN);

                for (; ofReadable < ofEnd; ++ofReadable)
                    {
                    aBuffer[ofReadable] = null; // allow for GC
                    }

                m_ofReadable = ofReadable;

                return event;
                }

            /**
             * Handle a control event.
             *
             * @param event  the control event
             *
             * @return true if this should count as a received message
             *
             * @throws IOException on an I/O error
             */
            public boolean onControlMessage(Event event)
                    throws IOException
                {
                try
                    {
                    if (event.getType() != Event.Type.MESSAGE)
                        {
                        throw new IllegalStateException(
                                "unexpected control event: " + event);
                        }

                    DataInput in = new BufferSequenceInputStream((BufferSequence)
                                    event.getContent());
                    byte nType = in.readByte();
                    switch (nType)
                        {
                        case MSG_RECEIPT:
                            processReceipt(in);
                            return true;

                        case MSG_SYNC:
                            processSync(in);
                            return false; // SYNC is not a resendable message and as such does not contribute to the message count

                        default:
                            String sDump = HeapDump.dumpHeapForBug("28240730");
                            getLogger().log(makeRecord(Level.WARNING,
                                    "{0} received a corrupt message from {1}; collected {2} for analysis",
                                    getLocalEndPoint(), getPeer(), sDump));

                            throw new IllegalStateException("unexpected control message type: " + nType);
                        }

                    }
                finally
                    {
                    event.dispose();
                    }
                }

            @Override
            public String toString()
                {
                return "ready=" + new MemorySize(m_cbReadable) +
                        ", pending=" + new MemorySize(Math.abs(m_cbRequired)) +
                        ", free=" + new MemorySize(m_cbWritable);
                }


            // ----- data members ---------------------------------------

            /**
             * The ByteBuffer array.
             */
            protected ByteBuffer[] m_aBuffer = new ByteBuffer[2];

            /**
             * The offset of the first buffer to read into (i.e. write to).
             */
            protected int m_ofWritable;

            /**
             * The number of unfilled buffers remaining, starting at m_ofWritable.
             */
            protected int m_cBufferWritable;

            /**
             * The number of writable bytes.
             */
            protected long m_cbWritable;

            /**
             * The array offset of the first readable buffer.
             */
            protected int m_ofReadable;

            /**
             * The number of readable bytes.
             */
            protected long m_cbReadable;

            /**
             * The number of bytes required for the next header or message.
             * <p>
             * The value will be negative for control messages.
             */
            protected long m_cbRequired;

            /**
             * The CRC for next message.
             */
            protected long m_lCrcBodyNext;

            /**
             * True if waiting for a message header, false if waiting for
             * a message.
             */
            protected boolean m_fHeader = true;

            /**
             * Shared buffer protecting remainder of buffer at m_ofReadable.
             */
            protected SharedBuffer m_bufferShared;
            }


        // ----- helpers ------------------------------------------------

        /**
         * Called as part of disposing a MessageEvent.
         *
         * @param bufseq  the pre-disposed message
         */
        public void onMessageDispose(BufferSequence bufseq)
            {
            AtomicLong atomicCb = m_cbEventQueue;
            long       cbSeq    = bufseq.getLength();
            long       cbCur;

            // Decrement the outstanding byte count by the length of the disposed
            // sequence. Ensure that the count doesn't go negative, which could
            // happen do to artificially zeroing out the count as part of the
            // inbound flow-control processing (COH-5379), see issueLocalBacklog
            do
                {
                cbCur = atomicCb.get();
                }
            while (!atomicCb.compareAndSet(cbCur, Math.max (0, cbCur - cbSeq)));
            }

        @Override
        public void onMigration()
            {
            super.onMigration();

            ReadBatch batchRead = m_readBatch;
            if (batchRead != null)
                {
                // discard any partially read messages, they will be resent in their entirety on the new connection
                getLogger().log(Level.FINER, "{0} discarding partial message from {1} consisting of {2} out of {3} bytes on {4}",
                        new Object[] {getLocalEndPoint(), getPeer(), batchRead.m_cbReadable, batchRead.m_cbRequired, MessageConnection.this});
                batchRead.dispose();
                m_readBatch = null;
                }
            }

        /**
         * Defer send if handshake is not complete.
         *
         * @param bufSeq   the message
         * @param receipt  the optional receipt
         *
         * @return true if the send has been deferred
         */
        protected boolean deferSend(BufferSequence bufSeq, Object receipt)
            {
            lock();
            try
                {
                ensureValid();

                if (getProtocolVersion() < 0)
                    {
                    LinkedList<Pair<BufferSequence, Object>> queue = m_queuePreNegotiate;

                    if (queue == null)
                        {
                        queue = m_queuePreNegotiate = new LinkedList<>();

                        // emit the BACKLOG_EXCESSIVE event to prevent OOM in case of delayed handshake
                        invoke(() ->
                            {
                            lock();
                            try
                                {
                                if (!m_fBacklog && getProtocolVersion() < 0)
                                    {
                                    m_fBacklog = true;
                                    emitEvent(new SimpleEvent(
                                            Event.Type.BACKLOG_EXCESSIVE,
                                            getPeer()));
                                    }
                                }
                            finally
                                {
                                unlock();
                                }
                            });
                        }
                    queue.add(new Pair<>(bufSeq, receipt));

                    if (m_state == ConnectionState.DEFUNCT)
                        {
                        invoke(this::drainReceipts);
                        }
                    return true;
                    }
                }
            finally
                {
                unlock();
                }
            
            return false;
            }


        // ----- data members -------------------------------------------

        /**
         * The read buffer data.
         */
        protected ReadBatch m_readBatch;

        /**
         * The limit on how much inbound data to buffer.
         */
        protected long m_cbReadThreshold;

        /**
         * Stub node used by the inline MPSC FIFO queue.
         */
        private final SendEntry m_queueFifoMsStub = new SendEntry(null, null, null, 0L);

        /**
         * Consumer-owned head for the inline MPSC FIFO queue.
         */
        private SendEntry m_queueFifoMsHead = m_queueFifoMsStub;

        /**
         * Producer-owned tail for the inline MPSC FIFO queue.
         */
        private final AtomicReference<SendEntry> m_queueFifoMsTail = new AtomicReference<>(m_queueFifoMsStub);

        /**
         * Total bytes enqueued but not yet drained into the unflushed batch.
         */
        final AtomicLong m_cbPending = new AtomicLong();

        /**
         * Total number of queued user sends pending drain into the unflushed batch.
         */
        final AtomicLong m_cPendingMsgs = new AtomicLong();

        /**
         * Peak queued user send count observed on this connection.
         */
        protected long m_cPendingMsgsPeak;

        /**
         * Peak queued producer bytes observed on this connection.
         */
        protected long m_cbPendingPeak;

        /**
         * Number of producer queue drain calls.
         */
        protected long m_cDrainCalls;

        /**
         * Total messages drained from the producer queue.
         */
        protected long m_cDrainedMsgs;

        /**
         * Total bytes drained from the producer queue.
         */
        protected long m_cbDrained;

        /**
         * Number of direct header slab allocations performed by this connection.
         */
        protected long m_cHeaderSlabAllocs;

        /**
         * The total number emitted messages;
         */
        protected long m_cMsgUserIn;

        /**
         * The total number of messages given to the connection to send.
         */
        protected long m_cMsgUserOut;

        /**
         * The total number of null receipts given with sends.
         */
        protected long m_cReceiptsNull;

        /**
         * The queue that holds the requests with bufseq and receipt before
         * connection is ready
         */
        protected LinkedList<Pair<BufferSequence, Object>> m_queuePreNegotiate = null;
        }

    // ----- inner class: TaskEvent ----------------------------

    /**
     * TaskEvent is a wrapper around a normal event, but utilizes
     * the mandatory dispose() call to run a number of tasks on behalf
     * of the bus, hopefully from the application rather then the bus
     * thread.
     */
    public static class TaskEvent
            implements Event
        {
        public TaskEvent(Event event, Runnable... aTask)
            {
            m_event = event;
            m_aTask = aTask;
            }

        public Type getType()
            {
            return m_event.getType();
            }

        public EndPoint getEndPoint()
            {
            return m_event.getEndPoint();
            }

        public Object getContent()
            {
            return m_event.getContent();
            }

        public Object dispose(boolean fTakeContent)
            {
            Object o = m_event.dispose(fTakeContent);

            for (Runnable task : m_aTask)
                {
                task.run();
                }

            return o;
            }

        public void dispose()
            {
            dispose(/*fTakeContent*/ false);
            }

        public String toString()
            {
            return m_event.toString();
            }

        private final Event      m_event;
        private final Runnable[] m_aTask;
        }

    /**
     * Wakeup all connections.
     */
    protected void wakeupConnections()
        {
        for (Connection conn : getConnections())
            {
            try
                {
                conn.wakeup();
                }
            catch (IOException e)
                {
                conn.onException(e);
                }
            }
        }
    /**
     * Put the bus in the local backlog state, issuing any requisite events.
     */
    protected void issueLocalBacklog()
        {
        if (f_fBacklogLocal.compareAndSet(false, true))
            {
            // emit backlog task event
            addEvent(new TaskEvent(new SimpleEvent(Event.Type.BACKLOG_EXCESSIVE, getLocalEndPoint()),
                    new Runnable()
                {
                public void run()
                    {
                    // Now that this event has been consumed we assume that all prior
                    // events have also been consumed, ending the backlog

                    // emit end backlog event
                    emitEvent(new SimpleEvent(
                        Event.Type.BACKLOG_NORMAL,
                        getLocalEndPoint()));

                    // Note: artificially resetting to zero here
                    // is a work around for an issue that occurs
                    // if events are disposed out of order
                    // (as is done by Coherence).
                    // Essentially we treat the processing/disposal of the BACKLOG_EXCESSIVE
                    // event as the application telling us "I'm keeping whatever I'm holding, so stop counting it against me"
                    m_cbEventQueue.set(0);

                    // backlog can only be true since this is the only way to set it to false, we simply
                    // have to emit the backlog event first to ensure that we keep our backlog events
                    // in the proper order
                    f_fBacklogLocal.set(false);

                    // one or more connections may have disabled reads during the backlog, re-enable them
                    // now that the backlog has been cleared
                    wakeupConnections();
                    }
                }));
            }
        // else; already in the backlog state
        }

    /**
     * A helper class to store a pair.
     */
    public static class Pair<K, V>
        {
        public Pair(K key, V value)
            {
            m_key = key;
            m_value = value;
            }

        public K getKey()
            {
            return m_key;
            }

        public V getValue()
            {
            return m_value;
            }

        private K m_key;
        private V m_value;
        }


    // ----- data members ---------------------------------------------------

    /**
     * True if we are locally backlogged.
     */
    protected final AtomicBoolean f_fBacklogLocal = new AtomicBoolean();

    /**
     * The approximate size of the data in the event queue.
     */
    protected final AtomicLong m_cbEventQueue = new AtomicLong();
    }
