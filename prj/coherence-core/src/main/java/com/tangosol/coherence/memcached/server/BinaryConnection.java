/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.memcached.server;

import com.oracle.coherence.common.base.Disposable;

import com.oracle.coherence.common.base.Logger;

import com.oracle.coherence.common.internal.net.socketbus.SharedBuffer;
import com.oracle.coherence.common.internal.net.socketbus.SharedBuffer.Disposer;
import com.oracle.coherence.common.internal.net.socketbus.SharedBuffer.Segment;

import com.oracle.coherence.common.io.BufferManager;
import com.oracle.coherence.common.io.BufferSequence;

import com.oracle.coherence.common.net.SelectionService;

import com.tangosol.coherence.memcached.Request;
import com.tangosol.coherence.memcached.Response;

import com.tangosol.internal.io.BufferSequenceWriteBufferPool;

import com.tangosol.io.MultiBufferWriteBuffer;
import com.tangosol.io.ReadBuffer;
import com.tangosol.io.ReadBuffer.BufferInput;
import com.tangosol.io.WriteBuffer.BufferOutput;

import com.tangosol.util.Base;

import java.io.DataInput;
import java.io.IOException;

import java.nio.ByteBuffer;

import java.nio.channels.ClosedChannelException;
import java.nio.channels.SocketChannel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Memcached Binary protocol connection.
 *
 * @author bb 2013.05.01
 *
 * @since Coherence 12.1.3
 */
public class BinaryConnection
        implements Connection, Disposer
    {

    // ----- constructors ---------------------------------------------------

    /**
     * Construct a Binary Connection.
     *
     * @param bufMgr   BufferManager
     * @param channel  SocketChannel
     * @param nConnId  Connection Id
     */
    public BinaryConnection(BufferManager bufMgr, SocketChannel channel, int nConnId)
        {
        m_bufferManager = bufMgr;
        m_channel       = channel;
        m_nConnId       = nConnId;
        }

    // ----- Connection methods ---------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public void setFlowControl(ConnectionFlowControl flowCtrl)
        {
        m_flowControl = flowCtrl;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public SocketChannel getChannel()
        {
        return m_channel;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getId()
        {
        return m_nConnId;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Request> read()
            throws IOException
        {
        try
            {
            long           cbAlloc       = m_cbRequired - m_cbReadable;
            SharedBuffer[] aSharedBuffer = cbAlloc > 0 ? ensureCapacity(cbAlloc) : m_aSharedBuffer;
            ByteBuffer[]   aBuffer       = getBuffers(aSharedBuffer);
            int            of            = m_ofWritable; // offset into aBuffer from where to start writing
            int            cBuffer       = m_cBufferWritable; // no. of writable buffers
            long           cb            = read(aBuffer, of, cBuffer);
            if (cb > 0)
                {
                for (; cBuffer > 0 && !aBuffer[of].hasRemaining(); ++of, --cBuffer)
                    {
                    aBuffer[of].reset(); // prepare buffers for reading
                    }
                m_ofWritable      = of;
                m_cBufferWritable = cBuffer;
                m_cbWritable     -= cb;
                long cbReady      = m_cbReadable += cb;

                if (cbReady >= m_cbRequired)
                    {
                    // we have read enough bytes to parse the complete message.
                    if (cBuffer > 0 && aBuffer[of].position() > 0)
                        {
                        // multiple buffers to process
                        ByteBuffer buffLast  = aBuffer[of];
                        // save the current write position
                        int        iPosWrite = buffLast.position();
                        // the last buffer may be partially readable. Set the limit on the last buffer for reading
                        // and also reset the position to mark where we left reading in the last iteration.
                        // We will reset the limit when we start writing.
                        try
                            {
                            buffLast.limit(iPosWrite).reset();
                            return onReady(aBuffer);
                            }
                        finally
                            {
                            // mark the position up to which we have read so that
                            // we could starting reading from that position in the
                            // next iteration.
                            buffLast.mark();
                            // reset limit for writing into the buffer.
                            buffLast.limit(buffLast.capacity()).position(iPosWrite);
                            }
                        }
                    else
                        {
                        return onReady(aBuffer);
                        }
                    }
                }
            return Collections.emptyList();
            }
        catch (IOException ioe)
            {
            throw ioe;
            }
        catch (Throwable thr)
            {
            Logger.err(thr);
            throw Base.ensureRuntimeException(thr);
            }
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public int write()
            throws IOException
        {
        try
            {
            Iterator<BinaryResponse> itr = f_delegatedWrites.iterator();
            while (itr.hasNext())
                {
                BinaryResponse response = itr.next();
                if (response.write())
                    {
                    itr.remove();
                    }
                else
                    {
                    break;
                    }
                }
            return itr.hasNext() ? SelectionService.Handler.OP_WRITE : 0;
            }
        catch (Throwable thr)
            {
            throw Base.ensureRuntimeException(thr);
            }
        }

    // ----- Disposer methods -----------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public void dispose(ByteBuffer buffer)
        {
        m_bufferManager.release(buffer);
        }

    // ----- internal methods -----------------------------------------------

    /**
     * Create and return a list of Requests from the specified array of read buffers.
     *
     * @param aBuffer  array of buffers holding data read from the socket channel
     *
     * @return the request list
     *
     * @throws IOException
     */
    protected List<Request> onReady(ByteBuffer[] aBuffer)
            throws IOException
        {
        ArrayList<Request>    listRequest    = new ArrayList<Request>();
        SharedBuffer[]        aSharedBuffer  = m_aSharedBuffer;
        int                   ofReadable     = m_ofReadable;
        long                  cbReadable     = m_cbReadable;
        long                  cbRequired     = m_cbRequired;
        while (cbReadable >= cbRequired)
            {
            DisposableReadBuffer readBuffer = null;
            if (aBuffer[ofReadable].remaining() >= cbRequired)
                {
                // We can read the reqd. bytes from the current buffer itself.
                ByteBuffer buf = aBuffer[ofReadable];

                // save the buffer limit. Carving out a segment will move the
                // buffer's limit to the end of the segment position.
                int nLimit = buf.limit();
                Segment segment = aSharedBuffer[ofReadable].getSegment(buf.position(), (int) cbRequired);
                readBuffer = new DisposableReadBuffer(new Segment[] { segment });

                // reset buffer limit and forward buffer position.
                buf.position(buf.position() + (int) cbRequired).limit(nLimit);
                }
            else
                {
                // Reqd. bytes spans multiple buffers
                long          cbTmpRequired = cbRequired;
                List<Segment> listSegments  = new ArrayList<Segment>();
                while (cbTmpRequired > 0)
                    {
                    ByteBuffer buf = aBuffer[ofReadable];

                    // save the buffer limit. Carving out a segment will move the
                    // buffer's limit to the end of the segment position.
                    int nLimit = buf.limit();
                    int cbAvailable = buf.remaining();
                    if (cbTmpRequired > cbAvailable)
                        {
                        // carve out a segment for all of the remaining buffer content
                        listSegments.add(aSharedBuffer[ofReadable].getSegment());
                        ofReadable++;
                        }
                    else
                        {
                        // only need to read buffer partially.
                        listSegments.add(aSharedBuffer[ofReadable].getSegment(buf.position(), (int) cbTmpRequired));

                        // reset buffer limit and forward buffer position after carving out the segment.
                        buf.position(buf.position() + (int) cbTmpRequired).limit(nLimit);
                        }
                    cbTmpRequired -= cbAvailable;
                    }
                Segment[] aSegment = listSegments.toArray(new Segment[listSegments.size()]);
                readBuffer = new DisposableReadBuffer(aSegment);
                }

            cbReadable -= cbRequired;
            if (m_fHeader)
                {
                BinaryHeader hdr = m_requestHeader = new BinaryHeader(readBuffer);
                cbRequired       = Math.abs(hdr.getBodyLength());
                m_fHeader        = false;
                }
            else
                {
                BinaryRequest request = new BinaryRequest(m_requestHeader, readBuffer, m_bufferManager, this);
                listRequest.add(request);
                f_queueResponses.add(request.getResponse());
                cbRequired = HEADER_LEN;
                m_fHeader  = true;
                }
            }

        m_cbRequired = cbRequired;

        if (ofReadable > 0)
            {
            // detach buffers that have been completely read and compact remaining
            // buffers to the front of the array
            for (int i = 0, cBuffers = aBuffer.length; i < cBuffers; i++)
                {
                if (i < ofReadable)
                    {
                    // everything up to ofReadable has been read, so it needs to be
                    // detached and null'd out (it may later be copied over during
                    // compaction)
                    aSharedBuffer[i].detach();
                    }
                else
                    {
                    // everything past ofReadable is still "active" and should be
                    // compacted to the head of the array
                    aSharedBuffer[i - ofReadable] = aSharedBuffer[i];
                    }
                aSharedBuffer[i] = null;
                }
            }

        m_ofWritable -= ofReadable;
        m_ofReadable = 0;
        m_cbReadable = cbReadable;
        return listRequest;
        }

    /**
     * Ensure read buffer capacity to read the required bytes.
     *
     * @param cbReqd  the number of bytes to ensure capacity to read
     *
     * @return an array of SharedBuffers
     */
    protected SharedBuffer[] ensureCapacity(long cbReqd)
        {
        SharedBuffer[] aSharedBuffer   = m_aSharedBuffer;
        int            ofReadable      = m_ofReadable;
        int            ofWritable      = m_ofWritable;
        long           cbWritable      = m_cbWritable;
        int            cBufferWritable = m_cBufferWritable;
        int            cBuffer         = aSharedBuffer.length;
        long           cbAlloc         = cbReqd - cbWritable;

        if (cbAlloc > 0)
            {
            // count no. of additional buffers needed
            int nBufNeeded    = (int) (cbReqd / BUF_SIZE) + 1;
            int nBufAvailable = cBuffer - ofWritable;
            if (nBufNeeded > nBufAvailable)
                {
                SharedBuffer[] aBufferNew = new SharedBuffer[cBuffer * 2];
                System.arraycopy(aSharedBuffer, 0, aBufferNew, 0, cBuffer);
                aSharedBuffer = aBufferNew;
                cBuffer       = aSharedBuffer.length;
                }
            BufferManager manager = m_bufferManager;
            for (int i = ofWritable; i < cBuffer && aSharedBuffer[i] == null && cbWritable < cbReqd; i++)
                {
                ByteBuffer buff = manager.acquirePref((int) Math.min(Integer.MAX_VALUE, BUF_SIZE));
                buff.clear().mark();
                int cbBuff  = buff.remaining();
                cbAlloc    -= Math.min(cbBuff, cbAlloc);
                cbWritable += cbBuff;
                ++cBufferWritable;
                aSharedBuffer[i] = new SharedBuffer(buff, this).attach();
                }
            }
        m_aSharedBuffer   = aSharedBuffer;
        m_ofReadable      = ofReadable;
        m_ofWritable      = ofWritable;
        m_cbWritable      = cbWritable;
        m_cBufferWritable = cBufferWritable;

        return aSharedBuffer;
        }

    /**
     * Write responses in the order of the received requests. If there are pending responses,
     * the current response will be queued else it will be written out on the adding thread.
     * The queued responses will be written out on the SelectionService thread whenever
     * the channel is write-ready.
     *
     * @param response  the response to write
     *
     * @throws IOException
     */
    public void writeResponse(BinaryResponse response)
            throws IOException
        {
        if (f_delegatedWrites.isEmpty() && response.write())
            {
            return;
            }
        f_delegatedWrites.add(response);
        }

    /**
     * Read from the socket channel.
     *
     * @param aBuf    array of ByteBuffers to read into
     * @param offset  offset into the ByteBuffer array to read into
     * @param length  length of the ByteBuffer array
     *
     * @return number of bytes read
     *
     * @throws IOException
     */
    protected long read(ByteBuffer[] aBuf, int offset, int length)
            throws IOException
        {
        long cb = m_channel.read(aBuf, offset, length);
        if (cb < 0)
            {
            throw new IOException("InputShutdown during reading");
            }

        return cb;
        }

    /**
     * Get an array of the underlying ByteBuffer's contained in the specified
     * array of SharedBuffers.
     *
     * @param aSharedBuffer  the array of shared buffers
     *
     * @return underlying ByteBuffer[]
     */
    protected ByteBuffer[] getBuffers(SharedBuffer[] aSharedBuffer)
        {
        ArrayList<ByteBuffer> listBuf = new ArrayList<ByteBuffer>();
        for (int i = 0, c = aSharedBuffer.length; i < c; i++)
            {
            SharedBuffer sBuf = aSharedBuffer[i];
            if (sBuf == null)
                {
                break;
                }

            listBuf.add(sBuf.get());
            }

        return listBuf.toArray(new ByteBuffer[listBuf.size()]);
        }

    /**
     * Enable Writes on Selection Service thread if there are back-logged responses
     * that couldn't be written completely by the service thread.
     *
     * @throws IOException
     */
    protected void checkWrites() throws IOException
        {
        if (!f_delegatedWrites.isEmpty())
            {
            m_flowControl.resumeWrites();
            }
        }

    // ----- inner class: BinaryHeader --------------------------------------

    /**
     * Binary Message Header.
     */
    protected static class BinaryHeader
        {
        /**
         * Read the header message.
         *
         * @param readBuffer  Read Buffer
         *
         * @throws IOException
         */
        public BinaryHeader(DisposableReadBuffer readBuffer) throws IOException
            {
            BufferInput bufInput = readBuffer.getBufferInput();
            m_readBuffer         = readBuffer;

            // magic should be 0x80
            int magic = bufInput.readUnsignedByte();
            if (magic != 0x80)
                {
                throw new IOException("invalid magic byte - " + magic);
                }

            m_nOpCode      = bufInput.readUnsignedByte();
            m_sKeyLength   = bufInput.readShort();
            m_nExtraLength = bufInput.readUnsignedByte();
            m_nDataType    = bufInput.readUnsignedByte(); // unused
            m_sReserved    = bufInput.readShort(); // unused
            m_nBodyLength  = bufInput.readInt();
            m_nOpaque      = bufInput.readInt();
            m_lVersion     = bufInput.readLong();
            }

        /**
         * Get the extras length from the header.
         *
         * @return extras length
         */
        public int getExtrasLength()
            {
            return m_nExtraLength;
            }

        /**
         * Get the body length.
         *
         * @return body length
         */
        public int getBodyLength()
            {
            return m_nBodyLength;
            }

        /**
         * Get Key offset.
         *
         * @return key offset
         */
        public int getKeyOffset()
            {
            return m_nExtraLength;
            }

        /**
         * Get key length.
         *
         * @return key length
         */
        public int getKeyLength()
            {
            return m_sKeyLength;
            }

        /**
         * Get value offset.
         *
         * @return value offset
         */
        public int getValueOffset()
            {
            return m_nExtraLength + m_sKeyLength;
            }

        /**
         * Get value length.
         *
         * @return value length
         */
        public int getValueLength()
            {
            return m_nBodyLength - m_sKeyLength - m_nExtraLength;
            }

        /**
         * Get opaque value.
         *
         * @return opaque value
         */
        public int getOpaqueValue()
            {
            return m_nOpaque;
            }

        /**
         * Get version.
         *
         * @return version
         */
        public long getVersion()
            {
            return m_lVersion;
            }

        // ----- data members -----------------------------------------------

        /**
         * Request Op Code
         */
        protected final int                  m_nOpCode;

        /**
         * Key length
         */
        protected final short                m_sKeyLength;

        /**
         * Extra Length
         */
        protected final int                  m_nExtraLength;

        /**
         * Data type
         */
        protected final int                  m_nDataType;

        /**
         * Reserved field
         */
        protected final short                m_sReserved;

        /**
         * Body length
         */
        protected final int                  m_nBodyLength;

        /**
         * Opaque value
         */
        protected final int                  m_nOpaque;

        /**
         * Version
         */
        protected final long                 m_lVersion;

        /**
         * Underlying read buffer
         */
        protected final DisposableReadBuffer m_readBuffer;
        }

    // ----- inner class: BinaryRequest -------------------------------------

    /**
     * Binary Request.
     */
    public static class BinaryRequest implements Request
        {
        /**
         * Construct a BinaryRequest.
         *
         * @param header     Request Header
         * @param readBuffer Underlying read buffer
         * @param bufMgr     BufferManager
         * @param conn       Underlying connection
         */
        public BinaryRequest(BinaryHeader header, DisposableReadBuffer readBuffer, BufferManager bufMgr,
                BinaryConnection conn)
            {
            m_header         = header;
            m_bufferManager  = bufMgr;
            m_conn           = conn;
            m_bufPayLoad     = readBuffer;
            m_lId            = conn.m_cRequests++;
            m_response       = new BinaryResponse(m_bufferManager, m_conn, this);
            }

        public long getId()
            {
            return m_lId;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getOpCode()
            {
            return header().m_nOpCode;
            }

        /**
         * Get the message header
         *
         * @return header
         */
        public BinaryHeader header()
            {
            return m_header;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public DataInput getExtras()
            {
            BinaryHeader hdr        = m_header;
            ReadBuffer   readBuffer = m_bufPayLoad;
            int          nLen       = hdr.getExtrasLength();
            return (nLen > 0) ? readBuffer.getReadBuffer(0, nLen).getBufferInput() : null;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getKey()
            {
            BinaryHeader hdr        = m_header;
            ReadBuffer   readBuffer = m_bufPayLoad;
            return MemcachedHelper.getString(
                readBuffer.getReadBuffer(hdr.getKeyOffset(), hdr.getKeyLength()).toByteArray());
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public byte[] getValue()
            {
            BinaryHeader hdr        = m_header;
            ReadBuffer   readBuffer = m_bufPayLoad;
            return readBuffer.getReadBuffer(hdr.getValueOffset(), hdr.getValueLength()).toByteArray();
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public long getVersion()
            {
            return header().getVersion();
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public BinaryResponse getResponse()
            {
            return m_response;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object getAssociatedKey()
            {
            return m_conn.getId();
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public void dispose()
            {
            header().m_readBuffer.dispose();
            m_bufPayLoad.dispose();
            }

        // ----- data members -----------------------------------------------

        /**
         * Binary Header
         */
        protected BinaryHeader m_header;

        /**
         * Binary Response
         */
        protected BinaryResponse m_response;

        /**
         * Read Buffer
         */
        protected DisposableReadBuffer m_bufPayLoad;

        /**
         * Buffer Manager
         */
        protected BufferManager m_bufferManager;

        /**
         * Binary Connection
         */
        protected BinaryConnection m_conn;

        /**
         * Request Id
         */
        protected volatile long m_lId;
        }

    // ----- inner class: BinaryResponse ------------------------------------

    /**
     * Binary Response.
     */
    public static class BinaryResponse implements Response, Disposable
        {
        /**
         * Construct a Binary Response.
         *
         * @param bufMgr  BufferManager
         * @param conn    Underlying Connection
         * @param request Associated request
         */
        public BinaryResponse(BufferManager bufMgr, BinaryConnection conn, BinaryRequest request)
            {
            m_bufferManager = bufMgr;
            m_conn          = conn;
            m_request       = request;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public BinaryResponse setResponseCode(int nResponseCode)
            {
            m_nResponseCode = nResponseCode;
            return this;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getResponseCode()
            {
            return m_nResponseCode;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public BinaryResponse setVersion(long lVersion)
            {
            m_lVersion = lVersion;
            return this;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public BinaryResponse setKey(String sKey)
            {
            m_sKey = sKey;
            return this;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public BinaryResponse setValue(byte[] value)
            {
            m_value = value;
            return this;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public BinaryResponse setExtras(ByteBuffer extras)
            {
            m_extras = extras;
            return this;
            }

        /**
         * Create the response message and send it back to the client.
         *
         * @param fDispose flag indicating iff the response needs to be discarded.
         */
        public void flush(boolean fDispose)
            {
            boolean fClose = false;
            try
                {
                BinaryRequest    request  = m_request;
                m_fDisposeOnly            = fDispose;
                m_aBuffers                = fDispose ? null : getBuffers(request.getOpCode() == 0x10);
                BinaryResponse   response = this;
                BinaryConnection conn     = m_conn;
                ResponseQueue    queue    = conn.f_queueResponses;
                boolean          fFlush   = queue.isFlushable(response, true);
                do
                    {
                    if (fFlush)
                        {
                        if (response.m_fDisposeOnly)
                            {
                            response.dispose();
                            }
                        else
                            {
                            m_conn.writeResponse(response);
                            }
                        response = queue.removeAndGetNext(response);
                        }
                    else
                        {
                        // Its imp. to recheck if the response is flushable after we have
                        // marked it deferred because we could become the head if the previous
                        // response is concurrently processed.
                        queue.markDeferred(response);
                        }
                    }
                while (fFlush = queue.isFlushable(response, /*fOwner*/false));
                conn.checkWrites();
                }
            catch (ClosedChannelException cce) { } /*ignore*/
            catch (IOException ioe)
                {
                fClose = true;
                }
            catch (Throwable thr)
                {
                fClose = true;
                Logger.err("Exception while writing response:", thr);
                }
            finally
                {
                if (fClose)
                    {
                    try
                        {
                        m_conn.getChannel().close();
                        }
                    catch (IOException ioe) { } /*ignore*/
                    }
                }
            }

        /**
         * Write the response on the socket channel.
         *
         * @return true iff response completely written on the socket channel
         *
         * @throws IOException
         */
        public boolean write()
                throws IOException
            {
            SocketChannel channel  = m_conn.getChannel();
            ByteBuffer[]  abuffers = m_aBuffers;

            int offset   = m_nOffset;
            int cBuffers = m_cBuffers;
            int nOpCode  = m_request.getOpCode();
            if (nOpCode == 0x17)
                {
                // QuitQ request
                channel.close();
                return true;
                }

            while (cBuffers > 0)
                {
                if (channel.write(abuffers, offset, cBuffers) > 0)
                    {
                    for (int i = offset; i < abuffers.length && !abuffers[i].hasRemaining(); i++)
                        {
                        offset++;
                        cBuffers--;
                        }
                    }
                else
                    {
                    m_nOffset  = offset;
                    m_cBuffers = cBuffers;
                    return false;
                    }
                }
            if (nOpCode == 0x07)
                {
                // Quit request
                channel.close();
                }
            dispose();
            return true;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public void dispose()
            {
            m_request.dispose();
            }

        public BinaryRequest getRequest()
            {
            return m_request;
            }

        /**
         * Serialize the response into ByteBuffers.
         *
         * @param fAppendEmptyPkt  Flag to indicate if as additional empty response needs to added
         *
         * @return ByteBuffer[]
         *
         * @throws IOException
         */
        protected ByteBuffer[] getBuffers(boolean fAppendEmptyPkt) throws IOException
            {
            BufferSequenceWriteBufferPool wbPool    = new BufferSequenceWriteBufferPool(m_bufferManager);
            BufferOutput                  bufOutput = new MultiBufferWriteBuffer(wbPool).getBufferOutput();

            bufOutput.write((byte) 0x81); // magic
            bufOutput.write((byte) m_request.getOpCode()); // opcode

            short keyLength = m_sKey == null ? 0 : (short) m_sKey.length();
            bufOutput.writeShort(keyLength);

            int extrasLength = m_extras == null ? 0 : m_extras.remaining();
            bufOutput.writeByte((byte) extrasLength); // extra length = flags + expiry

            bufOutput.writeByte((byte) 0); // data type unused
            bufOutput.writeShort(m_nResponseCode); // status code

            int dataLength = (m_value != null) ? m_value.length : 0;
            bufOutput.writeInt(dataLength + keyLength + extrasLength); // data length
            bufOutput.writeInt(m_request.header().getOpaqueValue()); // opaque
            bufOutput.writeLong(m_lVersion);

            BufferSequence headerSeq = wbPool.toBufferSequence();

            ByteBuffer[] aBufEmpty = null;
            if (fAppendEmptyPkt)
                {
                BinaryResponse emptyResponse = new BinaryResponse(m_bufferManager, m_conn, m_request);
                aBufEmpty = emptyResponse.getBuffers(/*fAppendEmptyPkt*/ false);
                }

            m_cBuffers = headerSeq.getBufferCount() + (m_extras != null ? 1 : 0) + (m_sKey != null ? 1 : 0)
                    + (m_value != null ? 1 : 0)
                    + (fAppendEmptyPkt ? aBufEmpty.length : 0);

            ByteBuffer[] buffers = new ByteBuffer[m_cBuffers];
            ByteBuffer[] aHdr    = headerSeq.getBuffers();
            int i = 0;
            for (; i < aHdr.length; i++)
                {
                buffers[i] = aHdr[i];
                }
            if (m_extras != null)
                {
                buffers[i++] = m_extras;
                }
            if (m_sKey != null)
                {
                buffers[i++] = ByteBuffer.wrap(m_sKey.getBytes("utf-8"));
                }
            if (m_value != null)
                {
                buffers[i++] = ByteBuffer.wrap(m_value);
                }
            if (fAppendEmptyPkt)
                {
                for (int j = 0; j < aBufEmpty.length; j++)
                    {
                    buffers[i++] = aBufEmpty[j];
                    }
                }
            return buffers;
            }

        // ----- data members -----------------------------------------------

        /**
         * The BufferManager.
         */
        protected BufferManager m_bufferManager;

        /**
         * The BinaryConnection.
         */
        protected BinaryConnection m_conn;

        /**
         * The BinaryRequest.
         */
        protected BinaryRequest m_request;

        /**
         * The Response code.
         */
        protected int m_nResponseCode;

        /**
         * The Version.
         */
        protected long m_lVersion;

        /**
         * The Key.
         */
        protected String m_sKey;

        /**
         * The Value.
         */
        protected byte[] m_value;

        /**
         * The Extras.
         */
        protected ByteBuffer m_extras;

        /**
         * The array of ByteBuffers to store Response message in.
         */
        protected ByteBuffer[] m_aBuffers;

        /**
         * The offset in the m_aBuffers[] from where data have to be written.
         */
        protected int m_nOffset;

        /**
         * The length of the m_aBuffers[] from where data have to be written.
         */
        protected int m_cBuffers;

        /**
         * Flag indicating if the response is ready to be sent to the client.
         * It couldn't be sent earlier because there were pending responses before
         * this one in the queue. The memcached client expects the responses in the
         * same order as the requests.
         */
        protected volatile boolean m_fDeferred = false;

        /**
         * Flag indicating if the response needs to be disposed and not sent to the client.
         */
        protected boolean m_fDisposeOnly;

        /**
         * Next response to be sent to the client. Responses are sent in order
         * of the requests received on the connection.
         */
        protected volatile BinaryResponse m_next;
        }

    // ----- data members -----------------------------------------------

    /**
     * The ConnectionFlowControl.
     */
    protected ConnectionFlowControl m_flowControl;

    /**
     * The BufferManager.
     */
    protected final BufferManager m_bufferManager;

    /**
     * Connected SocketChannel.
     */
    protected final SocketChannel m_channel;

    /**
     * Connection id.
     */
    protected final int m_nConnId;

    /**
     * flag to indicate if the next message to read is a header or body.
     */
    protected boolean m_fHeader = true;

    /**
     * Request header.
     */
    protected BinaryHeader m_requestHeader;

    /**
     * Responses that couldn't be completely written out and were handed to the I/O thread for writing.
     */
    protected final ConcurrentLinkedQueue<BinaryResponse> f_delegatedWrites = new ConcurrentLinkedQueue<BinaryResponse>();

    /**
     * Queue of responses. It defines the order in which responses needs to be sent to the client.
     */
    protected final ResponseQueue f_queueResponses = new ResponseQueue();

    /**
     * Message length in bytes.
     */
    protected long m_cbRequired = HEADER_LEN;

    /**
     * Count of available bytes.
     */
    protected long m_cbReadable;

    /**
     * Offset into the shared buffer from where we need to start reading messages.
     */
    protected int m_ofReadable;

    /**
     * Count of bytes that can be written to the shared buffer.
     */
    protected long m_cbWritable;

    /**
     * Offset into the shared buffer from where we need to start writing messages.
     */
    protected int m_ofWritable;

    /**
     * Number of writable buffers available.
     */
    protected int m_cBufferWritable;

    /**
     * Shared buffer array.
     */
    protected SharedBuffer[] m_aSharedBuffer = new SharedBuffer[2];

    /**
     * Request count for this connection.
     */
    protected long m_cRequests;

    // ----- constants ------------------------------------------------------

    /**
     * Binary request message header length.
     */
    protected static final int HEADER_LEN = 24;

    /**
     * Default buffer size to be allocated from the buffer manager.
     */
    protected static final int BUF_SIZE = 16 * 1024;
    }