/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.common.net.ssl;


import com.oracle.coherence.common.base.Blocking;
import com.oracle.coherence.common.net.SocketProvider;
import com.oracle.coherence.common.net.SSLSocketProvider;

import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.Selector;
import java.nio.channels.SelectionKey;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.security.KeyStore;
import java.net.URL;

import javax.net.ssl.SSLContext;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;


/**
 * TODO: Fill in class description.
 *
 * @author falcom Feb 11, 2011
 */
public class SSLProviderTest
    {
    public static void testNonBlocking(SocketProvider providerServer, SocketProvider providerClient, String sAddr)
            throws Exception
        {
        ServerSocketChannel acceptor = providerServer.openServerSocketChannel();
        acceptor.configureBlocking(false);
        acceptor.socket().bind(providerServer.resolveAddress(sAddr));

        SocketChannel client = providerClient.openSocketChannel();
        client.configureBlocking(false);

        Selector selectorAccept = acceptor.provider().openSelector();
        Selector selector = client.provider().openSelector();

        acceptor.register(selectorAccept, acceptor.validOps(), acceptor);
        client.register(selector, SelectionKey.OP_CONNECT, client);

        client.connect(providerClient.resolveAddress(sAddr));

        while (client.isConnectionPending())
            {
            client.finishConnect();
            }
        client.keyFor(selector).interestOps(SelectionKey.OP_WRITE);

        Blocking.select(selectorAccept);
        SocketChannel server = acceptor.accept();
        server.configureBlocking(false);

        server.register(selector, SelectionKey.OP_READ, server);

        ByteBuffer bufWrite = ByteBuffer.allocate(4);
        ByteBuffer bufRead  = ByteBuffer.allocate(4);
        bufWrite.putInt(Integer.MAX_VALUE).flip();

        while (bufWrite.hasRemaining())
            {
            Blocking.select(selector);
            client.write(bufWrite);
            server.read(bufRead);
            }
        bufWrite.flip();


        bufRead.flip();

        acceptor.close();
        server.close();
        client.close();

        if (!bufRead.equals(bufWrite))
            {
            throw new Exception("write error, expected " + bufWrite + " received " + bufRead);
            }
        System.out.print('.');
        System.out.flush();
        }


    public static void main(String[] asArg)
            throws Exception
        {
        SSLContext          ctx          = SSLContext.getInstance("TLS");
        KeyManagerFactory   keymanager   = KeyManagerFactory.getInstance("SunX509");
        TrustManagerFactory trustmanager = TrustManagerFactory.getInstance("SunX509");
        KeyStore            keystore     = KeyStore.getInstance("JKS");
        char[]              achPassword  = "password".toCharArray();

        keystore.load(new URL("file:keystore.jks").openStream(), achPassword);
        keymanager.init(keystore, achPassword);
        trustmanager.init(keystore);

        ctx.init(keymanager.getKeyManagers(), trustmanager.getTrustManagers(), new SecureRandom());

        SSLSocketProvider provider = new SSLSocketProvider(
                new SSLSocketProvider.DefaultDependencies()
                    .setSSLContext(ctx));
        testNonBlocking(provider, provider, "localhost:8000");
        }
    }
