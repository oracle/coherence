/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.memcached;

import com.oracle.coherence.common.base.Continuation;
import com.oracle.coherence.common.base.Logger;

import com.tangosol.coherence.memcached.Response.ResponseCode;

import com.tangosol.coherence.memcached.processor.AddReplaceProcessor;
import com.tangosol.coherence.memcached.processor.AppendPrependProcessor;
import com.tangosol.coherence.memcached.processor.DeleteProcessor;
import com.tangosol.coherence.memcached.processor.GetProcessor;
import com.tangosol.coherence.memcached.processor.IncrDecrProcessor;
import com.tangosol.coherence.memcached.processor.MemcachedAsyncProcessor;
import com.tangosol.coherence.memcached.processor.PutProcessor;
import com.tangosol.coherence.memcached.processor.TouchProcessor;

import com.tangosol.coherence.memcached.server.Connection.ConnectionFlowControl;
import com.tangosol.coherence.memcached.server.DataHolder;
import com.tangosol.coherence.memcached.server.MemcachedHelper;

import com.tangosol.io.ByteArrayWriteBuffer;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.net.Service;
import com.tangosol.net.Session;

import com.tangosol.net.security.IdentityAsserter;
import com.tangosol.net.security.UsernameAndPassword;

import com.tangosol.util.Filter;

import com.tangosol.util.InvocableMap.EntryProcessor;

import java.io.DataInput;
import java.io.IOException;

import java.nio.ByteBuffer;

import java.security.PrivilegedExceptionAction;

import java.util.concurrent.Executor;

import javax.security.auth.Subject;

import static com.tangosol.net.cache.TypeAssertion.withoutTypeChecking;

/**
 * Memcached default request handler implementation.
 *
 * @author bb 2013.05.01
 *
 * @since Coherence 12.1.3
 */
public class DefaultRequestHandler
        implements RequestHandler
    {

    // ----- Constructors ---------------------------------------------------
    /**
     * Construct a DefaultRequestHandler according to the specified parameters.
     *
     * @param sCacheName       the cache name
     * @param parentSvc        the parent ProxyService
     * @param sAuthMethod      the authentication method to use
     * @param fBinaryPassThru  the flag indicating if binary pass-thru is enabled
     * @param asserter         Identity Asserter
     * @param executor         Task Executor
     * @param flowControl      Connection Flow Control
     */
    public DefaultRequestHandler(String sCacheName, Service parentSvc, String sAuthMethod, boolean fBinaryPassThru,
        IdentityAsserter asserter, Executor executor, ConnectionFlowControl flowControl)
        {
        f_sCacheName       = sCacheName;
        f_parentService    = parentSvc;
        f_sAuthMethod      = sAuthMethod.toUpperCase();
        f_fBinaryPassThru  = fBinaryPassThru;
        f_identityAsserter = asserter;
        f_executor         = executor;
        f_flowControl      = flowControl;
        if (f_sAuthMethod.equals(NONE_AUTH_METHOD))
            {
            // no security configured. Can get the cache reference now.
            ensureCache(/*Subject*/ null);
            }
        }

    // ----- RequestHandler methods -----------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public Subject getSubject(Request request)
        {
        int nOpCode = request.getOpCode();
        if (f_sAuthMethod.equals(NONE_AUTH_METHOD) ||
            nOpCode == 0x1e || nOpCode == 0x20 || nOpCode == 0x21)  // opCodes for auth requests
            {
            return null;
            }
        else
            {
            if (m_subject == null)
                {
                throw new SecurityException("Null subject");
                }

            return m_subject;
            }
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onGet(Request request, Response response)
            throws IOException
        {
        fireEP(request, response, new GetProcessor(f_fBinaryPassThru));
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onGetComplete(Request request, Response response, Object oReturn)
            throws IOException
        {
        int nResponseCode = getResponseCode(oReturn);
        response.setResponseCode(nResponseCode);
        if (nResponseCode == Response.ResponseCode.OK.getCode())
            {
            DataHolder holder    = toDataHolder(oReturn);
            ByteBuffer bufExtras = (ByteBuffer) ByteBuffer.allocate(FLAG_SIZE).clear().mark();

            bufExtras.putInt(holder.getFlag()).reset();
            response.setVersion(getVersion(holder)).setExtras(bufExtras).setValue(holder.getValue());
            }
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onSet(Request request, Response response)
            throws IOException
        {
        long           lVersion  = request.getVersion();
        DataInput      inExtras  = request.getExtras();
        int            nFlag     = inExtras.readInt();
        int            nExpiry   = MemcachedHelper.calculateExpiry(inExtras.readInt());
        fireEP(request, response, new PutProcessor(request.getValue(), nFlag, lVersion, nExpiry, f_fBinaryPassThru));
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onSetComplete(Request request, Response response, Object oReturn)
            throws IOException
        {
        response.setResponseCode(getResponseCode(oReturn)).setVersion(getVersion(oReturn));
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onAdd(Request request, Response response)
            throws IOException
        {
        addReplace(request, response, /*fInsert*/true);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onAddComplete(Request request, Response response, Object oReturn)
            throws IOException
        {
        addReplaceComplete(request, response, oReturn);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onReplace(Request request, Response response)
            throws IOException
        {
        addReplace(request, response, /*fInsert*/false);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onReplaceComplete(Request request, Response response, Object oReturn)
            throws IOException
        {
        addReplaceComplete(request, response, oReturn);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDelete(Request request, Response response)
            throws IOException
        {
        fireEP(request, response, new DeleteProcessor());
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDeleteComplete(Request request, Response response, Object oReturn)
            throws IOException
        {
        response.setResponseCode(getResponseCode(oReturn));
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onIncrement(Request request, Response response)
            throws IOException
        {
        incrDecr(request, response, /*fIncr*/true);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onIncrementComplete(Request request, Response response, Object oReturn)
            throws IOException
        {
        incrDecrComplete(request, response, oReturn);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDecrement(Request request, Response response)
            throws IOException
        {
        incrDecr(request, response, /*fIncr*/false);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDecrementComplete(Request request, Response response, Object oReturn)
            throws IOException
        {
        incrDecrComplete(request, response, oReturn);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onAppend(Request request, Response response)
            throws IOException
        {
        appendPrepend(request, response, /*fAppend*/true);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onAppendComplete(Request request, Response response, Object oReturn)
            throws IOException
        {
        appendPrependComplete(request, response, oReturn);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onPrepend(Request request, Response response)
            throws IOException
        {
        appendPrepend(request, response, /*fAppend*/false);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onPrependComplete(Request request, Response response, Object oReturn)
            throws IOException
        {
        appendPrependComplete(request, response, oReturn);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onFlush(Request request, Response response)
            throws IOException
        {
        DataInput      extras  = request.getExtras();
        int            nExpiry = (extras == null) ? 0 : MemcachedHelper.calculateExpiry(extras.readInt());
        EntryProcessor ep      =  (nExpiry == 0) ? new DeleteProcessor()
                                                 : new TouchProcessor(nExpiry, /*fBlind*/true, f_fBinaryPassThru);
        getCache().invokeAll((Filter) null, new MemcachedAsyncProcessor(this, request, response, ep));
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onFlushComplete(Request request, Response response, Object oReturn)
            throws IOException
        {
        response.setResponseCode(ResponseCode.OK.getCode());
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onTouch(Request request, Response response)
            throws IOException
        {
        DataInput extras  = request.getExtras();
        int       nExpiry = (extras == null) ? 0 : MemcachedHelper.calculateExpiry(extras.readInt());
        fireEP(request, response, new TouchProcessor(nExpiry, /*fBlind*/true, f_fBinaryPassThru));
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onTouchComplete(Request request, Response response, Object oReturn)
            throws IOException
        {
        response.setResponseCode(getResponseCode(oReturn));
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onGAT(Request request, Response response)
            throws IOException
        {
        DataInput inExtras = request.getExtras();
        int       nExpiry  = MemcachedHelper.calculateExpiry(inExtras.readInt());
        fireEP(request, response, new TouchProcessor(nExpiry, /*fBlind*/false, f_fBinaryPassThru));
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onGATComplete(Request request, Response response, Object oReturn)
            throws IOException
        {
        int nResponseCode = getResponseCode(oReturn);
        response.setResponseCode(nResponseCode);
        if (nResponseCode == Response.ResponseCode.OK.getCode())
            {
            DataHolder holder = toDataHolder(oReturn);
            response.setVersion(getVersion(holder));
            response.setValue(holder.getValue());
            }
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onSASLList(Request request, Response response)
            throws IOException
        {
        response.setResponseCode(Response.ResponseCode.OK.getCode()).setValue("PLAIN".getBytes());
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onSASLAuth(Request request, Response response)
            throws IOException
        {
        String sKey          = request.getKey();
        int    nResponseCode = Response.ResponseCode.OK.getCode();

        if (f_sAuthMethod.equals(NONE_AUTH_METHOD))
            {
            nResponseCode = ResponseCode.UNKNOWN.getCode();
            }
        else if (f_sAuthMethod.equals(PLAIN_AUTH_METHOD) && sKey.equals(PLAIN_AUTH_METHOD))
            {
            try
                {
                m_subject = authenticate(request.getValue());
                ensureCache(m_subject);
                }
            catch (Throwable thr)
                {
                Logger.err("Memcached authentication failure: " + thr);
                nResponseCode = ResponseCode.AUTH_ERROR.getCode();
                }
            }
        else
            {
            nResponseCode = ResponseCode.AUTH_ERROR.getCode();
            }

        response.setResponseCode(nResponseCode);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onSASLAuthStep(Request request, Response response)
            throws IOException
        {
        response.setResponseCode(ResponseCode.UNKNOWN.getCode());
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void flush()
        {
        if (m_asyncProcessor != null)
            {
            m_asyncProcessor.flush();
            }
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean checkBacklog(Continuation<Void> backlogEndedContinuation)
        {
        MemcachedAsyncProcessor proc = m_asyncProcessor;
        return proc != null && proc.checkBacklog(backlogEndedContinuation);
        }

    /**
     * Handle add or replace request.
     *
     * @param request   Request
     * @param response  Response
     * @param fInsert   Flag to indicate if its an add or replace operation.
     *
     * @throws IOException
     */
    protected void addReplace(Request request, Response response, boolean fInsert)
            throws IOException
        {
        long           lVersion  = request.getVersion();
        DataInput      inExtras  = request.getExtras();
        int            nFlag     = inExtras.readInt();
        int            nExpiry   = MemcachedHelper.calculateExpiry(inExtras.readInt());
        fireEP(request, response, new AddReplaceProcessor(request.getValue(), nFlag, lVersion, nExpiry,
            fInsert, f_fBinaryPassThru));
        }

    /**
     * Handle add or replace response from the Async EntryProcessor.
     *
     * @param request   Request
     * @param response  Response
     * @param oReturn   Object returned from the EP.
     *
     * @throws IOException
     */
    protected void addReplaceComplete(Request request, Response response, Object oReturn)
            throws IOException
        {
        response.setResponseCode(getResponseCode(oReturn)).setVersion(getVersion(oReturn));
        }

    /**
     * Handle increment/decrement request.
     *
     * @param request   Request
     * @param response  Response
     * @param fIncr     Flag to indicate if its an increment operation.
     *
     * @throws IOException
     */
    protected void incrDecr(Request request, Response response, boolean fIncr)
            throws IOException
        {
        long      lVersion = request.getVersion();
        DataInput inExtras = request.getExtras();
        long      lIncr    = inExtras.readLong();
        long      lInitial = inExtras.readLong();
        int       nExpiry  = MemcachedHelper.calculateExpiry(inExtras.readInt());
        fireEP(request, response, new IncrDecrProcessor(lInitial, lIncr, fIncr, lVersion, nExpiry, f_fBinaryPassThru));
        }


    /**
     * Handle incr or decr response from the Async EntryProcessor.
     *
     * @param request   Request
     * @param response  Response
     * @param oReturn   Object returned from the EP.
     *
     * @throws IOException
     */
    protected void incrDecrComplete(Request request, Response response, Object oReturn)
            throws IOException
        {
        int nResponseCode = getResponseCode(oReturn);
        response.setResponseCode(nResponseCode);
        if (nResponseCode == ResponseCode.OK.getCode())
            {
            DataHolder           holder = toDataHolder(oReturn);
            Long                 lValue = IncrDecrProcessor.getLong(holder.getValue());
            ByteArrayWriteBuffer buf    = new ByteArrayWriteBuffer(8);
            buf.getBufferOutput().writeLong(lValue);
            response.setValue(buf.toByteArray()).setVersion(getVersion(holder));
            }
        }

    /**
     * Handle Append/Prepend request.
     *
     * @param request   Request
     * @param response  Response
     * @param fAppend   true iff the request is an APPEND operation
     *
     * @throws IOException
     */
    protected void appendPrepend(Request request, Response response, boolean fAppend)
            throws IOException
        {
        long lVersion = request.getVersion();
        fireEP(request, response, new AppendPrependProcessor(request.getValue(), lVersion, fAppend, f_fBinaryPassThru));
        }

    /**
     * Handle append or prepend response from the Async EntryProcessor.
     *
     * @param request   Request
     * @param response  Response
     * @param oReturn   Object returned from the EP.
     *
     * @throws IOException
     */
    protected void appendPrependComplete(Request request, Response response, Object oReturn)
            throws IOException
        {
        int nResponseCode = getResponseCode(oReturn);
        response.setResponseCode(nResponseCode).setVersion(getVersion(oReturn));
        }

    /**
     * Get the response code from the EntryProcessor return value.
     *
     * @param oValue  the object received from the EntryProcessor
     *
     * @return the response code
     */
    protected int getResponseCode(Object oValue)
        {
        if (oValue instanceof Response.ResponseCode)
            {
            return ((Response.ResponseCode) oValue).getCode();
            }
        return Response.ResponseCode.OK.getCode();
        }

    /**
     * Get the version from the EntryProcessor return value.
     *
     * @param oValue  the object received from the EntryProcessor
     *
     * @return the Response code
     */
    protected long getVersion(Object oValue)
        {
        long lVersion = 0;
        if (oValue instanceof DataHolder)
            {
            lVersion = ((DataHolder) oValue).getVersion();
            }
        else if (oValue instanceof Long) // return value itself is the version
            {
            lVersion = ((Long) oValue).longValue();
            }
        return lVersion;
        }

    /**
     * Convert the object to a DataHolder.
     *
     * @param obj  the Object to convert to DataHolder
     *
     * @return the object as a DataHolder
     */
    protected DataHolder toDataHolder(Object obj)
        {
        if (obj instanceof DataHolder)
            {
            return (DataHolder) obj;
            }

        throw new IllegalArgumentException("Cannot convert " + obj + " to DataHolder");
        }

    /**
     * Perform SASL Plain Authentication and return authenticated Subject.
     *
     * @param abPayload  the value from the request message
     *
     * @return the authenticated Subject if successful, null otherwise
     */
    protected Subject authenticate(byte[] abPayload)
        {
        // payload structure - authorization token ASCII-NUL Username ASCII-NUL Password
        String sPayload = new String(abPayload);
        int    of1      = sPayload.indexOf(SEP);          // authorization token.
        int    of2      = sPayload.indexOf(SEP, of1 + 1); // username token
        String sUser    = sPayload.substring(of1 + 1, of2);
        String sPwd     = sPayload.substring(of2 + 1);
        return f_identityAsserter.assertIdentity(new UsernameAndPassword(sUser, sPwd), f_parentService);
        }

    /**
     * Ensure the NamedCache associated with the RequestHandler.
     *
     * @param subject  Subject associated with the client
     */
    protected void ensureCache(final Subject subject)
        {
        f_flowControl.pauseReads();
        f_executor.execute(new Runnable()
            {
            @Override
            public void run()
                {
                try
                    {
                    System.out.println("@@@@ Requesting Memcached Cache " + f_sCacheName + " using subject " + subject);

                    if (subject == null)
                        {
                        System.out.println("@@@@ No Subject Provided");

                        if (f_parentService instanceof Session)
                            {
                            System.out.println("@@@@ Using Session " + f_parentService + " class " + f_parentService.getClass());

                            m_cache = ((Session) f_parentService).getCache(f_sCacheName, withoutTypeChecking());
                            }
                        else
                            {
                            System.out.println("@@@@ Using Cache Factory");

                            m_cache = CacheFactory.getCache(f_sCacheName, withoutTypeChecking());
                            }
                        }
                    else
                        {
                        System.out.println("@@@@ Using Subject " + subject);

                        m_cache = Subject.doAs(subject, new PrivilegedExceptionAction<NamedCache>()
                            {
                            public NamedCache run()
                                    throws IOException
                                {
                                return f_parentService instanceof Session
                                       ? ((Session) f_parentService).getCache(f_sCacheName, withoutTypeChecking())
                                       : CacheFactory.getCache(f_sCacheName, withoutTypeChecking());
                                }
                            });
                        }
//
//                    m_cache = (subject == null)
//                                  ? f_parentService instanceof Session
//                                        ? ((Session) f_parentService).getCache(f_sCacheName, withoutTypeChecking())
//                                        : CacheFactory.getCache(f_sCacheName, withoutTypeChecking())
//                                  : Subject.doAs(subject, new PrivilegedExceptionAction<NamedCache>()
//                                       {
//                                       public NamedCache run()
//                                               throws IOException
//                                           {
//                                           return f_parentService instanceof Session
//                                                   ? ((Session) f_parentService).getCache(f_sCacheName, withoutTypeChecking())
//                                                   : CacheFactory.getCache(f_sCacheName, withoutTypeChecking());
//                                           }
//                                       });

                    System.out.println("@@@@ Memcached Cache = " + m_cache);
                    }
                catch (Throwable thr)
                    {
                    Logger.warn("Memcached adapter failed to get cache:" + f_sCacheName, thr);
                    }
                finally
                    {
                    f_flowControl.resumeReads();
                    }
                }
            });
        }

    /**
     * Execute the given EntryProcessor async.
     *
     * @param request  Request
     * @param response Response
     * @param ep      EntryProcess to execute async
     */
    protected void fireEP(Request request, Response response, EntryProcessor ep)
        {
        MemcachedAsyncProcessor asyncProcessor = new MemcachedAsyncProcessor(this, request, response, ep);
        getCache().invoke(request.getKey(), asyncProcessor);
        m_asyncProcessor = asyncProcessor;
        }

    /**
     * Return a valid cache reference.
     *
     * @return NamedCache
     *
     * @throws RuntimeException if cache ref. is null
     */
    protected NamedCache getCache()
        {
        if (m_cache == null)
            {
            throw new RuntimeException("Null cache");
            }
        return m_cache;
        }

    // ----- data members ---------------------------------------------------

    /**
     * Cache name that is associated with the memcached adapter
     */
    protected final String f_sCacheName;

    /**
     * NamedCache associated with the Request handler/connection.
     */
    protected NamedCache m_cache;

    /**
     * Flag indicating if binary pass thru is enabled.
     */
    protected final boolean f_fBinaryPassThru;

    /**
     * The configured authorization method.
     */
    protected final String f_sAuthMethod;

    /**
     * Parent proxy service associated with the Memcached Adapter.
     */
    protected final Service f_parentService;

    /**
     * The authenticated subject.
     */
    protected Subject m_subject;

    /**
     * IdentityAsserter to use with SASL PLAIN authentication.
     */
    protected final IdentityAsserter f_identityAsserter;

    /**
     * Task Executor
     */
    protected final Executor f_executor;

    /**
     * Connection FlowControl.
     */
    protected final ConnectionFlowControl f_flowControl;

    /**
     * Last EP fired by the handler. Its ref. is needed to flush the service queue.
     */
    protected MemcachedAsyncProcessor m_asyncProcessor;

    // ----- constants ------------------------------------------------------

    /**
     * ASCII NULL.
     */
    protected static final byte SEP  = 0; // US-ASCII <NUL>

    /**
     * Flag size.
     */
    protected static final int FLAG_SIZE = 4;

    /**
     * NONE Auth method; implies no authentication.
     */
    protected static final String NONE_AUTH_METHOD = "NONE";

    /**
     * PLAIN Auth method; implies username/pwd authentication.
     */
    protected static final String PLAIN_AUTH_METHOD = "PLAIN";
    }
