/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.common.internal.net.ssl;


import com.oracle.coherence.common.base.Blocking;
import com.oracle.coherence.common.base.Timeout;
import com.oracle.coherence.common.internal.net.WrapperSocket;
import com.oracle.coherence.common.internal.net.WrapperSelector;
import com.oracle.coherence.common.internal.net.WrapperSocketChannel;

import com.oracle.coherence.common.internal.util.Timer;

import com.oracle.coherence.common.net.SSLSocketProvider;

import java.io.IOException;
import java.io.InterruptedIOException;

import java.net.Socket;

import java.nio.ByteBuffer;

import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.IllegalBlockingModeException;
import java.nio.channels.ScatteringByteChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.NotYetConnectedException;

import java.util.concurrent.RejectedExecutionException;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;


/**
* SSLSocketChannel layers SSL security onto an existing un-secured Socket.
*
* SSLSocketChannel does not currently support blocking channels, it must be
* configured and used in non-blocking mode only.
*
* @author mf  2010.04.27
* @since Coherence 3.6
*/
public class SSLSocketChannel
    extends WrapperSocketChannel
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct an SSLSocketChannel which layers SSL protection onto the
    * provided channel.
    *
    * @param channel   the channel to layer SSL on
    * @param provider  the provider associated with the SSLSocketChannel
    *
    * @throws IOException if an I/O error occurs
    */
    public SSLSocketChannel(SocketChannel channel, SSLSocketProvider provider)
            throws IOException
        {
        super(channel, new SSLSelectorProvider(channel.provider()));

        f_providerSocket = provider;

        SSLEngine engine = openSSLEngine();

        boolean fConnected = channel.isConnected();
        engine.setUseClientMode(!fConnected);

        f_engine = engine;
        m_fBlocking = channel.isBlocking();

        int cbPacket = engine.getSession().getPacketBufferSize();

        ByteBuffer buffOut = f_buffEncOut = ByteBuffer.allocate(cbPacket);
        buffOut.flip();

        ByteBuffer buffIn = f_buffEncIn = ByteBuffer.allocate(cbPacket);
        buffIn.flip();

        ByteBuffer buffClear = f_buffClearIn = ByteBuffer.allocate(
                engine.getSession().getApplicationBufferSize());
        buffClear.flip();
        f_aBuffClear[0] = buffClear;
        }


    // ----- WrapperSocketChannel methods -----------------------------------

    /**
    * {@inheritDoc}
    */
    protected Socket wrapSocket(Socket socket)
        {
        return new WrapperSocket(socket)
            {
             public void shutdownInput()
                throws IOException
                {
                // For SSL we cannot have a half-open comm channel, i.e.
                // we must keep the underlying socket's input open, but
                // that doesn't mean we can't pretend that it is closed and
                // not emit more data from the socket to the application
                synchronized (f_aBuffSingleInbound) // required for markKeysReadable
                    {
                    m_fInputShutdown = true;
                    markKeysReadable(/*fReadable*/ true);
                    }
                }

            public void shutdownOutput()
                    throws IOException
                {
                // mimic what SSLSocket does; if we were to attempt to mimic
                // what is done in shutdownInput here we would miss the side
                // effect of our peer seeing that input was shutdown. In order
                // for them to see that we'd have to completely close the socket
                // which isn't what the user is asking for here
                throw new UnsupportedOperationException(
                        "The method shutdownOutput() is not supported in SSLSocket");
                }

            public String toString()
                {
               return "SSLSocket(" + getLocalSocketAddress() + " " + getRemoteSocketAddress() + ", buffered{clear=" + f_buffClearIn.remaining() +
               " encrypted=" + f_buffEncIn.remaining() + " out=" +
               f_buffEncOut.remaining() + "}, handshake=" +
               f_engine.getHandshakeStatus() + ", jobs=" + m_cJobsPending;
                }

            public SocketChannel getChannel()
                {
                return SSLSocketChannel.this;
                }

            public void close()
                    throws IOException
                {
                SSLSocketChannel.this.close();
                }

            public boolean isInputShutdown()
                {
                return m_fInputShutdown || super.isInputShutdown();
                }

            protected volatile boolean m_fInputShutdown;
            };
        }

    // ----- ByteChannel methods --------------------------------------------

    /**
    * {@inheritDoc}
    */
    public int read(ByteBuffer dst)
            throws IOException
        {
        ByteBuffer[] aBuffRead = f_aBuffSingleInbound;
        synchronized (aBuffRead)
            {
            try
                {
                aBuffRead[0] = dst;
                return (int) read(aBuffRead, 0, 1); // we rely on EOS logic of read(ByteBuffer[]) which isn't in readInternal
                }
            finally
                {
                aBuffRead[0] = null;
                }
            }
        }

    /**
    * {@inheritDoc}
    */
    public int write(ByteBuffer src)
            throws IOException
        {
        ByteBuffer[] aBuffWrite = f_aBuffSingleOutbound;
        int          cb;
        synchronized (aBuffWrite)
            {
            try
                {
                aBuffWrite[0] = src;
                cb = (int) writeInternal(aBuffWrite, 0, 1);
                }
            finally
                {
                aBuffWrite[0] = null;
                }
            }

        if (!runProtocol() && cb == 0)
            {
            throw new IOException("end of stream");
            }
        return cb;
        }


    // ----- Scatter/Gather ByteChannel methods -----------------------------

    /**
    * {@inheritDoc}
    */
    public long read(ByteBuffer[] dsts, int offset, int length)
            throws IOException
        {
        if (f_buffEncOut.hasRemaining())
            {
            synchronized (f_aBuffSingleOutbound)
                {
                writeEncrypted();
                }
            }

        long cb;
        if (socket().isInputShutdown())
            {
            onEndOfStream();
            if (socket().isClosed())
                {
                throw new ClosedChannelException();
                }
            cb = -1;
            }
        else
            {
            synchronized (f_aBuffSingleInbound)
                {
                cb = readInternal(dsts, offset, length);
                }
            }

        runProtocol(); // no need to check return value here, if we've hit EOS it will eventually be reflected by returning -1 here
        return cb;
        }

    /**
    * {@inheritDoc}
    */
    public long write(ByteBuffer[] srcs, int offset, int length)
            throws IOException
        {
        long cb;
        synchronized (f_aBuffSingleOutbound)
            {
            cb = writeInternal(srcs, offset, length);
            }

        if (!runProtocol() && cb == 0)
            {
            throw new IOException("end of stream");
            }
        return cb;
        }


    // ----- AbstractSelectableChannel methods ------------------------------

    /**
    * {@inheritDoc}
    */
    protected void implCloseSelectableChannel()
            throws IOException
        {
        synchronized (f_aBuffSingleInbound)
            {
            synchronized (f_aBuffSingleOutbound)
                {
                // if we are currently interrupted we need to temporarily
                // disable the interrupt so that we can properly close the
                // SSL connection, else SSLExceptions will result on our
                // peer
                boolean fInterrupted = Blocking.interrupted();
                //noinspection unused
                try (Timeout t = Timeout.override(10000))
                    {
                    for (SSLSelectionKey key = m_keyFirst; key != null;
                         key = key.m_keyNext)
                        {
                        key.setDataReadyOps(0);
                        key.setProtocolReadyOps(0);
                        }

                    try
                        {
                        // Note: we intentionally do not shutdownInput as this is
                        // disallowed by SSL unless it has been prompted by the
                        // other side closing its output.  That case is handled
                        // in readInternal
                        closeOutbound(true);
                        }
                    finally
                        {
                        f_delegate.close();
                        }
                    }
                catch (InterruptedException e)
                    {
                    fInterrupted = true;
                    }

                if (fInterrupted)
                    {
                    throw new InterruptedIOException();
                    }
                }
            }
        }

    /**
    * {@inheritDoc}
    */
    protected void implConfigureBlocking(boolean block)
            throws IOException
        {
        if (block)
            {
            throw new IllegalBlockingModeException();
            }
        super.implConfigureBlocking(block);
        m_fBlocking = block;
        }



    // ----- WrapperSelectableChannel methods -------------------------------

    /**
    * {@inheritDoc}
    */
    public WrapperSelector.WrapperSelectionKey registerInternal(WrapperSelector selector,
            int ops, Object att)
            throws IOException
        {
        SSLSelectionKey key = new SSLSelectionKey(selector,
                f_delegate.register(selector.getDelegate(), ops), att);
        synchronized (f_aBuffSingleInbound)
            {
            key.m_keyNext = m_keyFirst;
            m_keyFirst = key;

            // COH-21678 - check both clear and encrypted as the SSL engine may consume the decrypt resulting in an empty clear buffer
            if (f_buffClearIn.hasRemaining() || f_buffEncIn.hasRemaining())
                {
                markKeysReadable(true);
                }
            }

        runProtocol(); // no need to check for EOS here, we'll eventually hit it on read or write
        return key;
        }


    // ----- Object methods -------------------------------------------------

    /**
    * {@inheritDoc}
    */
    public String toString()
        {
        return "SSLSocketChannel(" + f_socket + ")";
        }


    // ----- helper methods -------------------------------------------------

    /**
    * Reads a sequence of bytes from this channel into a subsequence of the given buffers per the general
    * contract of {@link ScatteringByteChannel#read(ByteBuffer[], int, int)}.
    *
    * The caller must hold the read monitor.
    *
    * @param aBuffDst  The buffers into which bytes are to be transferred
    * @param ofBuff    The offset within the buffer array of the first buffer into which bytes are to be transferred;
    *                  must be non-negative and no larger than <code>aBuffDst.length</code>
    * @param cBuff     The maximum number of buffers to be accessed; must be non-negative and no larger
    *                  than <code>aBuffDst.length - ofBuff</code>
    *
    * @return The number of bytes read, possibly zero, or -1 if the channel has reached end-of-stream
    *
    * @throws IOException if an I/O error occurs
    */
    protected long readInternal(ByteBuffer[] aBuffDst, int ofBuff, int cBuff)
            throws IOException
        {
        if (m_fBlocking)
            {
            throw new IllegalBlockingModeException();
            }

        long       cbFill  = 0;
        int        cbRead  = 0;
        ByteBuffer buffEnd = aBuffDst[ofBuff + cBuff - 1];

        for (int ofEnd = ofBuff + cBuff; ofBuff < ofEnd;
             ofBuff += advance(aBuffDst, ofBuff, ofEnd))
            {
            cbFill += drainClearBuffer(aBuffDst, ofBuff, ofEnd);

            if (buffEnd.hasRemaining())
                {
                cbFill += decrypt(aBuffDst, ofBuff, ofEnd);

                if (buffEnd.hasRemaining())
                    {
                    fillClearBuffer();

                    // read more encrypted data
                    cbRead = readEncrypted();
                    if (cbRead <= 0)
                        {
                        // no new data coming in; copy over whatever we can
                        cbFill += drainClearBuffer(aBuffDst, ofBuff, ofEnd);
                        break;
                        }
                    }
                }
            }

        // COH-21678 - check both clear and encrypted as the SSL engine may consume the decrypt resulting in an empty clear buffer
        boolean fMore = f_buffClearIn.hasRemaining() || fillClearBuffer() > 0 || f_buffEncIn.hasRemaining();
        markKeysReadable(fMore);

        if (cbFill == 0 && cbRead == -1 && !fMore)
            {
            return -1;
            }
        else
            {
            return cbFill;
            }
        }


    /**
    * Writes a sequence of bytes to this channel from a subsequence of the given buffers per the general
    * contract of {@link GatheringByteChannel#write(ByteBuffer[], int, int)}.
    *
    * The caller must hold the write monitor.
    *
    * @param aBuffSrc  The buffers from which bytes are to be retrieved
    * @param ofBuff    The offset within the buffer array of the first buffer from which bytes are to be retrieved;
    *                  must be non-negative and no larger than <code>aBuffSrc.length</code>
    * @param cBuff     The maximum number of buffers to be accessed; must be non-negative and no larger
    *                  than <code>aBuffSrc.length - ofBuff</code>
    *
    * @return The number of bytes written, possibly zero
    *
    * @throws IOException if an I/O error occurs
    */
    protected long writeInternal(ByteBuffer[] aBuffSrc, int ofBuff, int cBuff)
            throws IOException
        {
        if (m_fBlocking)
            {
            throw new IllegalBlockingModeException();
            }
        else if (f_buffEncOut.hasRemaining())
            {
            // attempt to write any pending data, this is only done
            // to ensure that if we'd swallowed an IOException (broken pipe)
            // on a prior pass we definitely won't swallow it on this one
            writeEncrypted();
            }

        long cbTake = 0;
        try
            {
            for (int ofEnd = ofBuff + cBuff; ofBuff < ofEnd;
                 ofBuff += advance(aBuffSrc, ofBuff, ofEnd))
                {
                cbTake += encrypt(aBuffSrc, ofBuff, ofEnd);

                // send what we have
                if (writeEncrypted() == 0)
                    {
                    break;
                    }
                }
            }
        catch (IOException e)
            {
            if (cbTake == 0)
                {
                throw e;
                }
            // else; we need to return cbTake exception will occur on next write attempt
            }

        if (f_buffEncOut.hasRemaining())
            {
            delayProtocol(
                    /*nInterest*/ SelectionKey.OP_WRITE,
                    /*nExclude*/  0,
                    /*nReady*/    SelectionKey.OP_READ);
            }

        return cbTake;
        }

    /**
    * Encrypt the supplied contents, storing them in the outgoing buffer.
    *
    * The caller must hold the write monitor.
    *
    * @param aBuffSrc  an array of ByteBuffers to encrypt
    * @param ofBuff    the first buffer to encrypt
    * @param ofEnd     the end of the buffer range, not-inclusive
    *
    * @return the number of bytes consumed
    *
    * @throws IOException if an I/O error occurs
    */
    protected int encrypt(ByteBuffer[] aBuffSrc, int ofBuff, int ofEnd)
            throws IOException
        {
        ByteBuffer buffDst = f_buffEncOut;
        int        ofPos   = buffDst.position();
        int        ofLimit = buffDst.limit();
        int        cbAdded = 0;

        try
            {
            // position the buffer for additional writes
            if (ofPos == ofLimit)
                {
                buffDst.clear();
                ofPos = ofLimit = 0;
                }
            else
                {
                buffDst.limit(buffDst.capacity())
                       .position(ofLimit);

                // Note: we could choose to compact the buffer if we are close
                // to the end, but that is not the point of this buffer.  This
                // buffer is simply to ensure that we have space to write
                // encrypted data to, it is not meant to buffer the network.
                // It is the job of the delegate socket to do the network
                // buffering.  While we could do it here as well it would add
                // unnecessary and potentially expensive array copies.
                }

            if (m_cJobsPending == 0)
                {
                SSLEngineResult result;

                try
                    {
                    result = f_engine.wrap(aBuffSrc, ofBuff, ofEnd - ofBuff, buffDst);
                    }
                catch (RuntimeException e)
                    {
                    // Bug 23071870: unwrap can throw RuntimeException if other side is not SSL (maybe wrap does to)
                    throw new SSLException(e);
                    }

                cbAdded = result.bytesProduced();
                if (cbAdded == 0 && result.getStatus() == SSLEngineResult.Status.CLOSED)
                    {
                    // if the engine has been closed then we can't produce any more data to put
                    // on the wire, and if we don't try to put something on the wire then the
                    // caller would never get to see an IOException indicating a closed socket
                    // so we must generate it ourselves
                    throw new IOException("connection closed");
                    }

                return result.bytesConsumed();
                }
            else
                {
                // Defend against JDK 1.5 bug (Sun BugId: 6492872) that
                // causes a deadlock if the engine.wrap call is concurrent
                // with the handshake task.
                // being non-blocking -- pretend we've run out of buffer space.
                return 0;
                }
            }
        finally
            {
            // restore buff positions to reference encrypted segment
            buffDst.position(ofPos)
                   .limit(ofLimit + cbAdded);
            }
        }

    /**
    * Decrypt from the incoming network buffer into the supplied buffers.
    *
    * The caller must hold the read monitor.
    *
    * @param aBuffDst  the destination buffers
    * @param ofBuff    the first buffer to decrypt into
    * @param ofEnd     the end of the buffer range, not-inclusive
    *
    * @return the number of bytes produced
    *
    * @throws IOException if an I/O error occurs
    */
    protected int decrypt(ByteBuffer[] aBuffDst, int ofBuff, int ofEnd)
            throws IOException
        {
        ByteBuffer      buffSrc = f_buffEncIn;
        SSLEngineResult result;

        try
            {
            result = f_engine.unwrap(buffSrc, aBuffDst, ofBuff, ofEnd - ofBuff);
            }
        catch (RuntimeException e)
            {
            // Bug 23071870: unwrap can throw RuntimeException if other side is not SSL
            throw new SSLException(e);
            }

        int cb = result.bytesProduced();
        if (cb == 0 &&
            result.getStatus() == SSLEngineResult.Status.BUFFER_UNDERFLOW &&
            buffSrc.limit() == buffSrc.capacity())
            {
            // only compact the encrypted buffer if it is preventing us from
            // providing the app with data; this avoid the typical pattern
            // of constant compaction which is expensive
            buffSrc.compact().flip();
            }
        return cb;
        }


    /**
    * Called to terminate or accept termination of the connection.
    *
    * If the caller passes <code>true</code> to this method, it is the caller's responsibility
    * to wrap this call within a {@link Timer} block.
    *
    * @param fBlocking <code>true</code> if this call is guarded by a {@link Timeout} and should block, if necessary,
    *                  in order to flush {@link #f_buffEncOut}.
    *
    * @throws InterruptedIOException if <code>fBlocking</code> is <code>true</code> and
    *                                {@link #f_buffEncOut} could not be written within
    *                                the caller's time limit
    */
    protected void closeOutbound(boolean fBlocking) throws InterruptedIOException
        {
        f_engine.closeOutbound();

        ByteBuffer buffOut = f_buffEncOut;
        if (buffOut.hasRemaining())
            {
            try
                {
                ByteBuffer[] aBuffIn = s_aBuffEmpty;
                if (fBlocking)
                    {
                    Selector selector = null;
                    try
                        {
                        while (buffOut.hasRemaining())
                            {
                            if (writeInternal(aBuffIn, 0, 1) == 0)
                                {
                                if (selector == null)
                                    {
                                    SocketChannel delegate = f_delegate;
                                    selector = delegate.provider().openSelector();
                                    delegate.register(selector, SelectionKey.OP_WRITE);
                                    }
                                Blocking.select(selector);
                                if (Blocking.interrupted())
                                    {
                                    // select was interrupted by guarding Timer
                                    throw new InterruptedIOException();
                                    }
                                }
                            }
                        }
                    finally
                        {
                        if (selector != null)
                            {
                            selector.close();
                            }
                        }
                    }
                else
                    {
                    // write as much as possible until there is no progress
                    for (int cb = buffOut.remaining(), cbLast = -1;
                         cb != cbLast;
                         cbLast = cb, cb = buffOut.remaining())
                        {
                        writeInternal(aBuffIn, 0, 1);
                        }
                    }
                }
            catch (InterruptedIOException e)
                {
                throw e;
                }
            catch (NotYetConnectedException | IllegalBlockingModeException | IOException e) {}
            }
        }

    /**
    * Called to indicate that the inbound stream will emit no further data.
    */
    protected void onEndOfStream()
        {
        synchronized (f_aBuffSingleInbound)
            {
            synchronized (f_aBuffSingleOutbound)
                {
                if (f_engine.getSession().isValid())
                    {
                    try
                        {
                        f_engine.closeInbound();
                        closeOutbound(false);
                        }
                    catch (SSLException e)
                        {
                        // mimicking SSLSocket
                        }
                    catch (IOException ignored)
                        {
                        // won't occur
                        }
                    }
                }
            }
        }

    /**
    * Run the next stage of the SSL handshake protocol if any.
    *
    * Depending on the handshake status this method may attempt to obtain
    * either the send or receive monitor, and thus it is important that
    * callers either hold both or neither.
    *
    * This method may safely be called at any point, though in general it can
    * be avoided until it is determined that no other data is moving through
    * the channel, i.e. read or write return zero.
    *
    * @throws IOException if an I/O error occurs
    *
    * @return true iff the socket is still usable
    */
    protected boolean runProtocol()
            throws IOException
        {
        SSLEngine    engine     = f_engine;
        ByteBuffer[] aBuffEmpty = s_aBuffEmpty;
        final Object oRead      = f_aBuffSingleInbound;
        final Object oWrite     = f_aBuffSingleOutbound;

        while (true)
            {
            switch (engine.getHandshakeStatus())
                {
                case NEED_TASK:
                    m_fHandshaking = true;
                    final Runnable runnable = engine.getDelegatedTask();
                    if (runnable == null)
                        {
                        synchronized (oWrite)
                            {
                            if (m_cJobsPending > 0)
                                {
                                // we are waiting on jobs to finish, ignore
                                // the socket state until the job(s) complete
                                boolean fWrite = f_buffEncOut.hasRemaining();
                                delayProtocol(
                                        /*nInterest*/ fWrite ? SelectionKey.OP_WRITE : 0,
                                        /*nExclude*/  fWrite ? 0 : SelectionKey.OP_WRITE,
                                        /*nReady*/    SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                                }
                            }
                        return true;
                        }
                    else
                        {
                        Runnable task = new Runnable()
                            {
                            public void run()
                                {
                                try
                                    {
                                    runnable.run();
                                    }
                                finally
                                    {
                                    synchronized (oWrite)
                                        {
                                        --m_cJobsPending;
                                        continueProtocol();
                                        }
                                    }
                                }
                            };

                        synchronized (oWrite)
                            {
                            ++m_cJobsPending;
                            }

                        try
                            {
                            getSocketProvider().getDependencies().getExecutor()
                                    .execute(task);
                            task = null;
                            }
                        catch (RejectedExecutionException e) {}
                        finally
                            {
                            if (task != null)
                                {
                                task.run();
                                }
                            }
                        }
                    break;

                case NEED_WRAP:
                    m_fHandshaking = true;
                    synchronized (oWrite)
                        {
                        int cbEncrypted = encrypt(aBuffEmpty, 0, 0);
                        int cbWritten   = writeEncrypted();
                        // COH-18648:
                        //     Check to see if we encrypted or wrote anything.  We  care
                        //     about this case as in TLS 1.3, half-closes are supported which means
                        //     the inbound channel can be closed, we can, however, still technically write.
                        //     If we didn't encrypt or write anything, then treat this situation
                        //     the same as if there was still encrypted data.  If the engine signals
                        //     that it still needs to wrap, then return to release any locks and allow
                        //     the channel state to stabilize.
                        //
                        if (f_buffEncOut.hasRemaining() ||
                                /* see comment COH-18648 */ (cbEncrypted == 0 && cbWritten == 0))
                            {
                            delayProtocol(
                                    /*nInterest*/ SelectionKey.OP_WRITE,
                                    /*nExclude*/  0,
                                    /*nReady*/    SelectionKey.OP_READ);

                            if (engine.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NEED_WRAP)
                                {
                                return true;
                                }
                            // else; all engine data encrypted but still not written; continue with next phase in "parallel"
                            }
                        }
                    break;

                case NEED_UNWRAP:
                    m_fHandshaking = true;

                    int cbRead;
                    int cbFill;
                    synchronized (oRead)
                        {
                        synchronized (oWrite)
                            {
                            cbRead = readEncrypted();
                            cbFill = fillClearBuffer();

                            if (cbRead == 0 && cbFill == 0)
                                {
                                boolean fWrite = f_buffEncOut.hasRemaining();
                                delayProtocol(
                                        /*nInterest*/ SelectionKey.OP_READ | (fWrite ? SelectionKey.OP_WRITE : 0),
                                        /*nExclude*/  fWrite ? 0 : SelectionKey.OP_WRITE,
                                        /*nReady*/    SelectionKey.OP_WRITE);
                                return true;
                                }
                            else
                                {
                                if (f_buffClearIn.hasRemaining() || f_buffEncIn.hasRemaining())
                                    {
                                    // COH-21678 - there is more data available for read
                                    markKeysReadable(true);
                                    }
                                }
                            }
                        }

                    if (cbRead == -1)
                        {
                        // must occur outside of synchronization
                        onEndOfStream();
                        return false;
                        }
                    break;

                case NOT_HANDSHAKING:
                case FINISHED:
                    if (m_fHandshaking) // optimistic read
                        {
                        synchronized (oWrite)
                            {
                            endProtocol();
                            m_fHandshaking = false;
                            }
                        }
                    return true;
                }
            }
        }

    /**
    * Delay the protocol waiting to do the specified operation.
    *
    * The caller must hold the write monitor.
    *
    * @param nInterest  the protocol's interest ops
    * @param nExclude   the operations to forcefully exclude
    * @param nReady     the protocol's ready op
    */
    protected void delayProtocol(int nInterest, int nExclude, int nReady)
        {
        try
            {
            for (SSLSelectionKey key = m_keyFirst; key != null;
                 key = key.m_keyNext)
                {
                key.setProtocolReadyOps(nReady);
                key.interestProtocol(nInterest, nExclude);
                }
            }
        catch (CancelledKeyException e) {}
        }

    /**
    * Continue the protocol by waking up the selector.
    *
    * The caller must hold the write monitor.
    */
    protected void continueProtocol()
        {
        for (SSLSelectionKey key = m_keyFirst; key != null;
             key = key.m_keyNext)
            {
            try
                {
                key.setProtocolReadyOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                key.interestProtocol(SelectionKey.OP_READ | SelectionKey.OP_WRITE, 0);

                // wakeup the selector so we can continue to run the protocol
                key.selector().wakeup();
                }
            catch (CancelledKeyException e) {}
            }
        }

    /**
    * Update the SelectionKeys once the handshake protocol has completed.
    *
    * The caller must hold the write monitor.
    *
    * @throws IOException if an I/O error occurs
    */
    protected void endProtocol()
            throws IOException
        {
        try
            {
            boolean fWrite = f_buffEncOut.hasRemaining();

            for (SSLSelectionKey key = m_keyFirst; key != null;
                 key = key.m_keyNext)
                {
                key.setProtocolReadyOps(fWrite ? SelectionKey.OP_READ : 0);
                key.interestProtocol(fWrite ? SelectionKey.OP_WRITE : 0, 0);
                }
            }
        catch (CancelledKeyException ignored) {}

        if (!m_fValidated && f_engine.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING
                && f_engine.getSession().isValid())
            {
            validatePeer();
            m_fValidated = true;
            }
        }

    /**
    * Vaidate that the connected peer acceptible.
    *
    * @throws SSLException  if the peer is not acceptible
    */
    protected void validatePeer()
            throws SSLException
        {
        SSLSocketProvider provider = getSocketProvider();
        SSLSession        session  = f_engine.getSession();
        Socket            socket   = f_delegate.socket();

        try
            {
            provider.ensureSessionValidity(session, socket);
            }
        catch (SSLException e)
            {
            try
                {
                f_delegate.close();
                }
            catch (IOException eIEIO)
                {}

            throw e;
            }
        }

    /**
    * Drain as much of the clear-text buffer into the supplied buffers
    *
    * The caller must hold the read monitor.
    *
    * @param aBuffDst  the destination buffers
    * @param ofBuff    the first buffer to fill into
    * @param ofEnd     the end of the buffer range, not-inclusive
    *
    * @return the number of bytes produced
    *
    * @throws IOException if an I/O error occurs
    */
    protected int drainClearBuffer(ByteBuffer[] aBuffDst, int ofBuff, int ofEnd)
            throws IOException
        {
        ByteBuffer buffSrc = f_buffClearIn;
        byte[]     abSrc   = buffSrc.array();
        int        ofSrc   = buffSrc.arrayOffset() + buffSrc.position();
        int        cbSrc   = buffSrc.remaining();
        int        cbAdded = 0;

        for (; ofBuff < ofEnd && cbSrc > 0; ++ofBuff)
            {
            ByteBuffer buffDst = aBuffDst[ofBuff];

            int cb = Math.min(buffDst.remaining(), cbSrc);
            if (cb > 0)
                {
                buffDst.put(abSrc, ofSrc, cb);
                ofSrc += cb;
                cbAdded += cb;
                cbSrc -= cb;
                }
            }
        buffSrc.position(buffSrc.position() + cbAdded);
        return cbAdded;
        }

    /**
    * Fill the clear-text buffer by decrypting any buffered encrypted data.
    *
    * The caller must hold the read monitor.
    *
    * @return the number of encrypted bytes which were consumed
    *
    * @throws IOException if an I/O error occurs
    */
    protected int fillClearBuffer()
            throws IOException
        {
        ByteBuffer   buffEnc   = f_buffEncIn;
        ByteBuffer   buffClear = f_buffClearIn;
        int          ofPos     = buffClear.position();
        int          ofLim     = buffClear.limit();
        int          cb        = 0;
        int          cbStart   = buffEnc.remaining();

        try
            {
            if (ofPos == ofLim)
                {
                buffClear.clear();
                ofPos = ofLim = 0;
                }
            else
                {
                buffClear.position(buffClear.limit())
                         .limit(buffClear.capacity());
                }
            cb = decrypt(f_aBuffClear, 0, 1);
            return cbStart - buffEnc.remaining();
            }
        finally
            {
            buffClear.position(ofPos)
                     .limit(ofLim + cb);
            }
        }

    /**
    * Attempt to write the contents the outbound buffer to the network.
    *
    * The caller must hold the write monitor.
    *
    * @return the number of bytes written
    *
    * @throws IOException if an I/O error occurs
    */
    protected int writeEncrypted()
            throws IOException
        {
        int cb = f_delegate.write(f_buffEncOut);

        if (!f_buffEncOut.hasRemaining())
            {
            endProtocol();
            }
        return cb;
        }

    /**
    * Attempt to read from the network into the inbound encrypted buffer.
    *
    * The caller must hold the read monitor.
    *
    * @return the number of bytes read
    *
    * @throws IOException if an I/O error occurs
    */
    protected int readEncrypted()
            throws IOException
        {
        ByteBuffer buff    = f_buffEncIn;
        int        ofPos   = buff.position();
        int        ofLimit = buff.limit();
        int        cbRead  = 0;

        try
            {
            // prep the buffer for additional writes
            if (ofLimit == ofPos)
                {
                buff.clear();
                ofLimit = ofPos = 0;
                }
            else
                {
                buff.position(buff.limit())
                    .limit(buff.capacity());
                }

            return cbRead = f_delegate.read(buff);
            }
        finally
            {
            // restore the position and limit to reference just the encrypted
            // segment
            buff.position(ofPos)
                .limit(ofLimit + (cbRead < 0 ? 0 : cbRead));
            }
        }

    /**
    * Identify the distance to the next buffer which has available space.
    *
    * @param aBuff   the array of buffers to scan
    * @param ofBuff  the starting position
    * @param ofEnd   the end of the buffer range, not-inclusive
    *
    * @return the difference between ofBuff and the first available buffer
    */
    protected int advance(ByteBuffer[] aBuff, int ofBuff, int ofEnd)
        {
        int c = 0;
        while (ofBuff < ofEnd && !aBuff[ofBuff].hasRemaining())
            {
            ++ofBuff;
            ++c;
            }
        return c;
        }

    /**
    * Prepare the registered keys for selection.
    *
    * The caller must hold the read monitor.
    *
    * @param fReadable  true if buffered inbound data exists
    */
    protected void markKeysReadable(boolean fReadable)
        {
        for (SSLSelectionKey key = m_keyFirst; key != null;
             key = key.m_keyNext)
            {
            if (fReadable)
                {
                // mark the channel as read ready
                key.setDataReadyOps(SelectionKey.OP_READ);
                // add interest in OP_WRITE to help avoid getting blocked in selector
                key.interestData(SelectionKey.OP_WRITE);
                }
            else // no data buffered
                {
                // unmark the channel as read ready
                key.setDataReadyOps(0);
                // remove artificial OP_WRITE interest
                key.interestData(0);
                }
            }
        }



    // ----- helpers --------------------------------------------------------

    /**
     * Return the SocketProvider which produced this socket.
     *
     * @return the SocketProvider
     */
    protected SSLSocketProvider getSocketProvider()
        {
        return f_providerSocket;
        }

    /**
     * Create and return a new SSLEngine.
     *
     * @return the SSLEngine
     */
    public SSLEngine openSSLEngine()
        {
        SSLSocketProvider.Dependencies deps        = getSocketProvider().getDependencies();
        SSLEngine                      engine      = deps.getSSLContext().createSSLEngine();
        String[]                       asCiphers   = deps.getEnabledCipherSuites();
        String[]                       asProtocols = deps.getEnabledProtocolVersions();
        if (asCiphers != null)
            {
            engine.setEnabledCipherSuites(asCiphers);
            }

        if (asProtocols != null)
            {
            engine.setEnabledProtocols(asProtocols);
            }

        switch (deps.getClientAuth())
            {
            case wanted:
                engine.setNeedClientAuth(false);
                engine.setWantClientAuth(true);
                break;
            case required:
                engine.setWantClientAuth(true);
                engine.setNeedClientAuth(true);
                break;
            case none:
            default:
                engine.setWantClientAuth(false);
                engine.setNeedClientAuth(false);
                break;
            }

        return engine;
        }


    // ------ inner class: SSLSelectionKey ----------------------------------

    /**
    * An SSL aware SelectionKey.
    */
    public class SSLSelectionKey
        extends WrapperSelector.WrapperSelectionKey
        {
        // ----- constructor --------------------------------------------

        protected SSLSelectionKey(WrapperSelector selector, SelectionKey key, Object att)
            {
            super (selector, key, att);
            m_nOpsInterestApp = key == null ? 0 : key.interestOps();
            }

        // ----- SSLSelectionKey methods --------------------------------

        /**
        * Set SelectionKey ops which apply to the SSLSelectableChannel but
        * not necessarily to the delegate channel.
        *
        * @param nOps  the SelectionKey ops
        */
        protected void setDataReadyOps(int nOps)
            {
            m_nOpsReadyData = nOps;
            }

        /**
        * Return the SelectionKey ops which apply to the SSLSelectableChannel
        * but not necessarily to the delegate channel.
        *
        * @return  the SelectionKey ops
        */
        protected int getDataReadyOps()
            {
            return m_nOpsReadyData;
            }

        /**
        * Specify the operations of interest to the SSL data layer.
        *
        * @param nOps operations bit set
        *
        * @return the operations of interest to the SSL data layer.
        */
        protected synchronized SSLSelectionKey interestData(int nOps)
            {
            if (nOps != m_nOpsInterestData)
                {
                int nOpsApp = m_nOpsInterestApp;
                if (nOpsApp != 0)
                    {
                    ((SSLSelector) selector()).setInterestOps(m_delegate, (nOps | nOpsApp
                            | m_nOpsInterestProtocol) & ~m_nOpsInterestExclude);
                    }
                // else; application isn't interested in forward progress now; don't force it

                m_nOpsInterestData = nOps;
                }
            return this;
            }

        /**
        * Return the interest operations for the SSL data layer
        *
        * @return the interest operations for the SSL data layer
        */
        protected int interestData()
            {
            return m_nOpsInterestData;
            }

        /**
        * Specify the SelectionKey operations which are ready on the
        * SSLSelectableChannel protocol but not necessarily to the delegate
        * channel.
        *
        * @param nOps  the SelectionKey ops
        */
        protected void setProtocolReadyOps(int nOps)
            {
            m_nOpsReadyProtocol = nOps;
            }

        /**
        * Return the SelectionKey operations which are ready on the
        * SSLSelectableChannel protocol but not necessarily to the delegate
        * channel.
        *
        * @return  the SelectionKey ops
        */
        protected int getProtocolReadyOps()
            {
            return m_nOpsReadyProtocol;
            }

        /**
        * The operations of interest to the SSL protocol layer.
        *
        * @param nOps         operations bit set
        * @param nExclusions  the operations to exclude from selection
        *
        * @return the operations of interest to the SSL protocol layer
        */
        protected synchronized SSLSelectionKey interestProtocol(int nOps, int nExclusions)
            {
            if (!(nOps == m_nOpsInterestProtocol && nExclusions == m_nOpsInterestExclude))
                {
                int nOpsApp = m_nOpsInterestApp;
                if (nOpsApp != 0)
                    {
                    ((SSLSelector) selector()).setInterestOps(m_delegate, (nOps | nOpsApp
                            | m_nOpsInterestData) & ~nExclusions);
                    }
                // else; application isn't interested in forward progress now; don't force it

                m_nOpsInterestProtocol = nOps;
                m_nOpsInterestExclude  = nExclusions;
                }
            return this;
            }

        /**
        * Get the interest operations for the SSL protocol layer
        *
        * @return the interest operations for the SSL protocol layer
        */
        protected int interestProtocol()
            {
            return m_nOpsInterestProtocol;
            }


        // ----- SelectionKey methods -----------------------------------

        /**
        * {@inheritDoc}
        */
        public SelectableChannel channel()
            {
            return SSLSocketChannel.this;
            }

        /**
        * {@inheritDoc}
        */
        public void cancel()
            {
            super.cancel();

            // remove this key from the linked-list of keys
            // while this has a cost of O(N), it is rare for N > 1, and also rare
            // a very rare event
            synchronized (f_aBuffSingleInbound)
                {
                SSLSelectionKey keyPrev = null;
                for (SSLSelectionKey keyCurr = m_keyFirst;
                     keyCurr != null; keyPrev = keyCurr, keyCurr = keyCurr.m_keyNext)
                    {
                    if (keyCurr == this)
                        {
                        if (keyPrev == null)
                            {
                            m_keyFirst = this.m_keyNext;
                            }
                        else
                            {
                            keyPrev.m_keyNext = this.m_keyNext;
                            }
                        return;
                        }
                    }
                }
            }

        /**
        * {@inheritDoc}
        */
        public int interestOps()
            {
            return m_nOpsInterestApp;
            }

        /**
        * {@inheritDoc}
        */
        public synchronized SelectionKey interestOps(int ops)
            {
            m_delegate.interestOps(ops == 0
                ? 0 // if the app doesn't want progress, don't force it, TODO: consider respecting interstApp for intrestData/interestProtocol
                : (ops | m_nOpsInterestData
                    | m_nOpsInterestProtocol) & ~m_nOpsInterestExclude);
            m_nOpsInterestApp = ops;
            return this;
            }

        /**
        * {@inheritDoc}
        */
        public int readyOps()
            {
            int nReadyDelegate = m_delegate.readyOps();
            int nReadyApp      = (nReadyDelegate | m_nOpsReadyData | m_nOpsReadyProtocol) & m_nOpsInterestApp;

            if (nReadyApp == 0 && (nReadyDelegate & (m_nOpsInterestData | m_nOpsInterestProtocol)) != 0)
                {
                // nothing the application is interested in is ready, but
                // the protocol is trying to make progress, lie to the
                // app to get them to trigger the protocol
                // we may still be returning 0 in which case we're not
                // lying but the application doesn't want to make progress
                // so apparently we don't need to progress the protocol either
                return m_nOpsInterestApp;
                }
            else
                {
                return nReadyApp;
                }
            }

        // ----- Object methods -----------------------------------------

        /**
        * {@inheritDoc}
        */
        public String toString()
            {
            return "SSLSelectionKey(" + getKeyString(this) + ", delegate{" +
                    getKeyString(m_delegate) + "}, data{interest=" +
                    interestData() + " ready=" + getDataReadyOps() +
                    "}, protocol{interest=" + interestProtocol() + " ready=" +
                    getProtocolReadyOps() + "}, exclude=" +
                    m_nOpsInterestExclude + ", " + SSLSocketChannel.this + ")";
            }

        // ----- data members -------------------------------------------

        /**
        * The interest set as specified by the application
        */
        protected int m_nOpsInterestApp;

        /**
        * The operations which are ready in the SSL data layer.
        */
        protected int m_nOpsReadyData;

        /**
        * The interest set as specified by the SSL data layer.
        */
        protected int m_nOpsInterestData;

        /**
        * The operations which are ready in the SSL protocol layer.
        */
        protected int m_nOpsInterestProtocol;

        /**
        * The interest set as specified by the SSL protocol layer.
        */
        protected int m_nOpsReadyProtocol;

        /**
        * The interest operations to exclude.
        */
        protected int m_nOpsInterestExclude;

        /**
        * A link to the next SSLSelectionKey registered against the associated
        * channel.
        */
        protected SSLSelectionKey m_keyNext;
        }


    // ----- data members ---------------------------------------------------

    /**
     * The SSLSocketProvider associated with this socket.
     */
    protected final SSLSocketProvider f_providerSocket;

    /**
    * The SSLEngine which provides SSL to this channel.
    */
    protected final SSLEngine f_engine;

   /**
    * A cached copy of the configured blocking mode.
    */
    protected boolean m_fBlocking;

    /**
     * Set to true once the connection has been validated.
     */
    protected boolean m_fValidated;

    /**
    * A reusable single element buffer array for reads, which also serves as
    * the monitor to protect against multiple concurrent readers.
    */
    protected final ByteBuffer[] f_aBuffSingleInbound = new ByteBuffer[1];

    /**
    * A reusable single element buffer array for writes, which also serves as
    * the monitor to protect against multiple concurrent writers.
    *
    * If both the read and write monitor will be held then the read should be
    * acquired before write.
    */
    protected final ByteBuffer[] f_aBuffSingleOutbound = new ByteBuffer[1];

    /**
    * Buffered encrypted data waiting to be written to the delegate channel.
    * The position and limit define the range of encrypted data.
    */
    protected final ByteBuffer f_buffEncOut;

    /**
    * Buffered encrypted data from the delegate channel, waiting to be
    * decrypted.  The position and limit define the range of encrypted data.
    */
    protected final ByteBuffer f_buffEncIn;

    /**
    * Buffered "clear text" data ready to be delivered to the user of this
    * channel.  The position and limit define the range of "clear text" data.
    */
    protected final ByteBuffer f_buffClearIn;

    /**
    * An ByteBuffer array containing just the clear text buffer
    */
    protected final ByteBuffer[] f_aBuffClear = new ByteBuffer[1];

    /**
    * A linked list of registered SSLSelectionKeys, the linked list must be
    * accessed while holding the read lock, i.e f_aBuffSingleInbound
    */
    protected SSLSelectionKey m_keyFirst;

    /**
    * Flag indicating if the channel is currently handshaking.
    */
    protected volatile boolean m_fHandshaking;

    /**
    * The number of pending jobs scheduled on behalf of the engine.  This is
    * only to be modified while holding the read and write locks.
    */
    protected int m_cJobsPending;

    /**
    * Cached array of remaining buffer byte counts.
    */
    protected int[] m_acbBuff;

    /**
    * Flag indicating that the first "clear text" byte should be skipped
    * during the next encryption operation.
    */
    protected boolean m_fSkip;

    /**
    * The byte of "clear text" that should be skipped during the next
    * encryption operation.
    */
    protected byte m_bSkip;

    /**
    * A shared empty byte buffer.
    */
    protected static final ByteBuffer[] s_aBuffEmpty = {ByteBuffer.allocate(0)};
    }
