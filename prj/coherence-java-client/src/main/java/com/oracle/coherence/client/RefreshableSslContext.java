/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.client;

import com.oracle.coherence.common.base.Logger;
import com.oracle.coherence.common.net.SSLSocketProvider;
import com.tangosol.internal.net.ssl.SSLContextDependencies;
import io.grpc.netty.GrpcSslContexts;
import io.netty.buffer.ByteBufAllocator;
import io.netty.handler.ssl.ApplicationProtocolNegotiator;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSessionContext;
import javax.net.ssl.TrustManager;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.util.Arrays;
import java.util.List;

/**
 * A refreshable {@link SslContext}.
 *
 * @author Jonathan Knight  2022.08.25
 * @since 22.06.2
 */
public class RefreshableSslContext
        extends SslContext
        implements SSLContextDependencies.Listener
    {
    public RefreshableSslContext(SSLContextDependencies dependencies, boolean fServer)
        {
        m_deps    = new SSLContextDependencies(dependencies, this);
        m_fServer = fServer;
        m_deps.init();
        }

    @Override
    public void onUpdate(SSLContextDependencies dependencies) throws GeneralSecurityException
        {
        try
            {
            SslContextBuilder builder;
            if (m_fServer)
                {
                builder = SslContextBuilder.forServer(dependencies.getKeyManagers()[0]);
                }
            else
                {
                builder = SslContextBuilder.forClient()
                        .keyManager(dependencies.getKeyManagers()[0]);
                }

            TrustManager[] aTrustManager = dependencies.getTrustManagers();

            SSLSocketProvider.ClientAuthMode mode = dependencies.getClientAuth();
            if (mode == null && aTrustManager.length > 0)
                {
                mode = SSLSocketProvider.ClientAuthMode.required;
                }

            ClientAuth clientAuth;
            //noinspection EnhancedSwitchMigration
            switch (mode)
                {
                case wanted:
                    clientAuth = ClientAuth.OPTIONAL;
                    break;
                case required:
                    clientAuth = ClientAuth.REQUIRE;
                    break;
                case none:
                default:
                    clientAuth = ClientAuth.NONE;
                    break;
                }

            String[] asCipher = dependencies.getEnabledCipherSuites();
            if (asCipher != null && asCipher.length > 0)
                {
                builder.ciphers(Arrays.asList(asCipher));
                }

            if (aTrustManager.length > 0)
                {
                builder.trustManager(aTrustManager[0]);
                }

            builder.clientAuth(clientAuth)
                    .sslContextProvider(dependencies.getProvider())
                    .startTls(false);

            m_delegate = GrpcSslContexts.configure(builder).build();
            }
        catch (SSLException e)
            {
            throw new GeneralSecurityException(e);
            }
        }

    @Override
    public void onError(SSLContextDependencies dependencies, Throwable t) throws KeyManagementException
        {
        if (m_delegate == null)
            {
            throw new KeyManagementException("Could not create first SSLContext. Expect communication errors.", t);
            }
        else
            {
            Logger.err("Could not properly instantiate SSLContext. The existing SSLContext will be used", t);
            }
        }

    @Override
    public boolean isClient()
        {
        return m_delegate.isClient();
        }

    @Override
    public List<String> cipherSuites()
        {
        return m_delegate.cipherSuites();
        }

    @Override
    @SuppressWarnings("deprecation")
    public ApplicationProtocolNegotiator applicationProtocolNegotiator()
        {
        return m_delegate.applicationProtocolNegotiator();
        }

    @Override
    public SSLEngine newEngine(ByteBufAllocator alloc)
        {
        return m_delegate.newEngine(alloc);
        }

    @Override
    public SSLEngine newEngine(ByteBufAllocator alloc, String peerHost, int peerPort)
        {
        return m_delegate.newEngine(alloc, peerHost, peerPort);
        }

    @Override
    public SSLSessionContext sessionContext()
        {
        return m_delegate.sessionContext();
        }

    // ----- data members ---------------------------------------------------

    private final SSLContextDependencies m_deps;

    private final boolean m_fServer;

    private volatile SslContext m_delegate;
    }
