/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.http.netty;

import com.tangosol.coherence.http.AbstractHttpServer;
import com.tangosol.coherence.http.HttpServer;

import com.tangosol.net.Service;

import com.tangosol.net.security.IdentityAsserter;
import com.tangosol.net.security.UsernameAndPassword;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledByteBufAllocator;

import io.netty.channel.embedded.EmbeddedChannel;

import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.net.URI;

import java.nio.charset.StandardCharsets;

import java.security.Principal;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.security.auth.Subject;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ResourceConfig;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author jk  2019.05.13
 */
public class NettyHttpServerTest
    {
    @Test
    public void shouldDiscoverServer()
        {
        HttpServer server = HttpServer.create();

        assertThat(server, is(instanceOf(NettyHttpServer.class)));
        }

    @Test
    public void shouldPreserveIndependentPipelinedRequestBodies()
            throws Exception
        {
        CapturingNettyHttpServer server = new CapturingNettyHttpServer();
        server.blockReads();

        try (Fixture fixture = new Fixture(server))
            {
            fixture.writeRequest("/one", "alpha");
            assertTrue(server.awaitStarted(1));

            fixture.writeRequest("/two", "beta");
            server.releaseReads();

            assertEquals("alpha", server.body("/one"));
            assertEquals("beta", server.body("/two"));
            }
        }

    @Test
    public void shouldReadFixedChunkedAndEmptyBodies()
            throws Exception
        {
        CapturingNettyHttpServer server = new CapturingNettyHttpServer();

        try (Fixture fixture = new Fixture(server))
            {
            fixture.writeRequest("/fixed", "fixed-body");
            fixture.writeChunkedRequest("/chunked", "chunk-", "body");
            fixture.writeEmptyRequest("/empty");

            assertEquals("fixed-body", server.body("/fixed"));
            assertEquals("chunk-body", server.body("/chunked"));
            assertEquals("", server.body("/empty"));
            }
        }

    @Test
    public void shouldWakeIncompleteBodyOnChannelClose()
            throws Exception
        {
        CapturingNettyHttpServer server = new CapturingNettyHttpServer();

        try (Fixture fixture = new Fixture(server))
            {
            HttpRequest request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/close");
            request.headers().set(HttpHeaderNames.CONTENT_LENGTH, 10);
            fixture.channel.writeInbound(request);

            assertTrue(server.awaitStarted(1));
            fixture.channel.close().sync();

            try
                {
                server.body("/close");
                fail("Expected the incomplete request body to fail");
                }
            catch (ExecutionException e)
                {
                assertTrue(e.getCause() instanceof IOException);
                }
            }
        }

    @Test
    public void shouldRejectOverlappingRequestBodies()
        {
        CapturingNettyHttpServer server = new CapturingNettyHttpServer();

        try (Fixture fixture = new Fixture(server))
            {
            HttpRequest requestOne = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/one");
            requestOne.headers().set(HttpHeaderNames.CONTENT_LENGTH, 10);
            fixture.channel.writeInbound(requestOne);

            HttpRequest requestTwo = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/two");
            requestTwo.headers().set(HttpHeaderNames.CONTENT_LENGTH, 3);
            fixture.channel.writeInbound(requestTwo);

            HttpResponse response = fixture.channel.readOutbound();
            assertEquals(HttpResponseStatus.BAD_REQUEST, response.status());
            assertFalse(fixture.channel.isOpen());
            }
        }

    @Test
    public void shouldRejectBasicAuthenticationWithoutDispatch()
        {
        CapturingNettyHttpServer server = new CapturingNettyHttpServer();
        server.setAuthMethod("basic");
        server.setIdentityAsserter(rejectingIdentityAsserter());

        try (Fixture fixture = new Fixture(server))
            {
            HttpRequest request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/auth");
            request.headers().set(HttpHeaderNames.CONTENT_LENGTH, 4);
            request.headers().set(HttpHeaderNames.AUTHORIZATION,
                                  "Basic " + AbstractHttpServer.toBase64("test:bad"));
            fixture.channel.writeInbound(request);

            HttpResponse response = fixture.channel.readOutbound();
            assertEquals(HttpResponseStatus.UNAUTHORIZED, response.status());
            assertFalse(fixture.channel.isOpen());
            assertEquals(0, server.getInvocationCount());
            }
        }

    @Test
    public void shouldAcceptBasicAuthenticationWithColonPassword()
            throws Exception
        {
        CapturingNettyHttpServer server = new CapturingNettyHttpServer();
        server.setAuthMethod("basic");
        server.setIdentityAsserter(testIdentityAsserter());

        try (Fixture fixture = new Fixture(server))
            {
            HttpRequest request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/auth");
            request.headers().set(HttpHeaderNames.AUTHORIZATION,
                                  "Basic " + AbstractHttpServer.toBase64("test:pass:word"));
            fixture.channel.writeInbound(request);

            assertEquals("", server.body("/auth"));
            assertEquals("test", server.getSubject().getPrincipals().iterator().next().getName());
            }
        }

    @Test
    public void shouldBindToConfiguredLoopbackAddress()
            throws Exception
        {
        NettyHttpServer server = new NettyHttpServer();
        server.setLocalAddress("127.0.0.1");
        server.setLocalPort(0);
        server.setAuthMethod("none");
        server.setResourceConfig(Collections.emptyMap());

        server.start();
        try
            {
            assertEquals("127.0.0.1", server.getListenAddress());
            }
        finally
            {
            server.stop();
            }
        }

    @Test
    public void shouldPreserveConfiguredWildcardBindAddress()
            throws Exception
        {
        NettyHttpServer server = new NettyHttpServer();
        server.setLocalAddress("0.0.0.0");
        server.setLocalPort(0);
        server.setAuthMethod("none");
        server.setResourceConfig(Collections.emptyMap());

        server.start();
        try
            {
            assertEquals("0.0.0.0", server.getListenAddress());
            }
        finally
            {
            server.stop();
            }
        }

    @Test
    public void shouldValidateOutputWriteBounds()
            throws Exception
        {
        CapturingNettyHttpServer server = new CapturingNettyHttpServer();

        try (Fixture fixture = new Fixture(server))
            {
            NettyHttpServer.NettyResponseWriter writer = server.new NettyResponseWriter(
                    fixture.channel.pipeline().firstContext(),
                    new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/"),
                    null);
            NettyHttpServer.NettyResponseWriter.JerseyNettyIOPipe pipe = writer.new JerseyNettyIOPipe();
            OutputStream source = pipe.getSource();

            source.write(new byte[] {'0', 'a', 'b', 'c', '4'}, 1, 3);
            source.write(new byte[] {'x'}, 0, 0);

            ByteBuf chunk = pipe.getSink().readChunk(UnpooledByteBufAllocator.DEFAULT);
            try
                {
                assertEquals("abc", chunk.toString(StandardCharsets.UTF_8));
                }
            finally
                {
                chunk.release();
                }

            assertThrows(NullPointerException.class, () -> source.write(null, 0, 1));
            assertThrows(IndexOutOfBoundsException.class, () -> source.write(new byte[1], -1, 1));
            assertThrows(IndexOutOfBoundsException.class, () -> source.write(new byte[1], 0, -1));
            assertThrows(IndexOutOfBoundsException.class, () -> source.write(new byte[1], 2, 0));
            assertThrows(IndexOutOfBoundsException.class, () -> source.write(new byte[2], 1, 2));
            }
        }

    @Test
    public void shouldDenyUnsupportedRoleChecks()
        {
        NettyHttpServer.NettySecurityContext context =
                new NettyHttpServer.NettySecurityContext("BASIC", () -> "test", false);

        assertFalse(context.isUserInRole("admin"));
        }

    private static IdentityAsserter rejectingIdentityAsserter()
        {
        return (token, service) ->
            {
            throw new SecurityException("rejected");
            };
        }

    private static IdentityAsserter testIdentityAsserter()
        {
        return new IdentityAsserter()
            {
            @Override
            public Subject assertIdentity(Object token, Service service)
                {
                UsernameAndPassword credentials = (UsernameAndPassword) token;
                if ("test".equals(credentials.getUsername())
                    && "pass:word".equals(new String(credentials.getPassword())))
                    {
                    Subject subject = new Subject();
                    subject.getPrincipals().add(new Principal()
                        {
                        @Override
                        public String getName()
                            {
                            return "test";
                            }
                        });
                    return subject;
                    }

                throw new SecurityException("rejected");
                }
            };
        }

    @Path("/")
    public static class TestResource
        {
        @GET
        public String get()
            {
            return "ok";
            }
        }

    private static class CapturingNettyHttpServer
            extends NettyHttpServer
        {
        private CapturingNettyHttpServer()
            {
            setLocalAddress("127.0.0.1");
            setLocalPort(0);
            setAuthMethod("none");
            setResourceConfig(Collections.singletonMap("/", new ResourceConfig(TestResource.class)));
            }

        @Override
        protected void handleRequest(ApplicationHandler app, ContainerRequest request, Subject subject)
            {
            m_subject.set(subject);
            m_cInvocations.incrementAndGet();

            String                    sPath  = request.getRequestUri().getPath();
            CompletableFuture<String> future = new CompletableFuture<>();
            m_mapBodies.put(sPath, future);
            m_latchStarted.countDown();

            try
                {
                CountDownLatch latch = m_latchRead;
                if (latch != null)
                    {
                    assertTrue(latch.await(5, TimeUnit.SECONDS));
                    }

                future.complete(read(request.getEntityStream()));
                }
            catch (Throwable t)
                {
                future.completeExceptionally(t);
                }
            }

        protected void blockReads()
            {
            m_latchRead = new CountDownLatch(1);
            }

        protected void releaseReads()
            {
            m_latchRead.countDown();
            }

        protected boolean awaitStarted(int cStarted)
                throws InterruptedException
            {
            long ldtEnd = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (m_cInvocations.get() < cStarted && System.nanoTime() < ldtEnd)
                {
                Thread.sleep(10);
                }
            return m_cInvocations.get() >= cStarted;
            }

        protected String body(String sPath)
                throws Exception
            {
            CompletableFuture<String> future = awaitFuture(sPath);
            return future.get(5, TimeUnit.SECONDS);
            }

        protected Subject getSubject()
            {
            return m_subject.get();
            }

        protected int getInvocationCount()
            {
            return m_cInvocations.get();
            }

        private CompletableFuture<String> awaitFuture(String sPath)
                throws InterruptedException
            {
            long ldtEnd = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            CompletableFuture<String> future;
            while ((future = m_mapBodies.get(sPath)) == null && System.nanoTime() < ldtEnd)
                {
                Thread.sleep(10);
                }

            if (future == null)
                {
                throw new AssertionError("Request was not dispatched: " + sPath);
                }
            return future;
            }

        private String read(InputStream in) throws IOException
            {
            byte[] ab = in.readAllBytes();
            return new String(ab, StandardCharsets.UTF_8);
            }

        private final Map<String, CompletableFuture<String>> m_mapBodies = new ConcurrentHashMap<>();
        private final CountDownLatch                         m_latchStarted = new CountDownLatch(1);
        private final AtomicInteger                          m_cInvocations = new AtomicInteger();
        private final AtomicReference<Subject>               m_subject      = new AtomicReference<>();

        private volatile CountDownLatch m_latchRead;
        }

    private static class Fixture
            implements AutoCloseable
        {
        private Fixture(CapturingNettyHttpServer server)
            {
            this.server = server;
            app         = server.initApplicationContainer();

            URI uri = server.initUri("127.0.0.1", 0);
            channel = new EmbeddedChannel(server.new NettyServerHandler(uri, app));
            }

        private void writeRequest(String sPath, String sBody)
            {
            byte[]      abBody  = sBody.getBytes(StandardCharsets.UTF_8);
            HttpRequest request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, sPath);
            request.headers().set(HttpHeaderNames.CONTENT_LENGTH, abBody.length);

            channel.writeInbound(request);
            channel.writeInbound(new DefaultLastHttpContent(Unpooled.wrappedBuffer(abBody)));
            }

        private void writeChunkedRequest(String sPath, String... asBody)
            {
            HttpRequest request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, sPath);
            request.headers().set(HttpHeaderNames.TRANSFER_ENCODING, HttpHeaderValues.CHUNKED);

            channel.writeInbound(request);
            for (int i = 0; i < asBody.length; i++)
                {
                ByteBuf buf = Unpooled.wrappedBuffer(asBody[i].getBytes(StandardCharsets.UTF_8));
                channel.writeInbound(i == asBody.length - 1
                                     ? new DefaultLastHttpContent(buf)
                                     : new DefaultHttpContent(buf));
                }
            }

        private void writeEmptyRequest(String sPath)
            {
            channel.writeInbound(new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, sPath));
            channel.writeInbound(LastHttpContent.EMPTY_LAST_CONTENT);
            }

        @Override
        public void close()
            {
            channel.finishAndReleaseAll();
            app.shutdown();
            }

        private final CapturingNettyHttpServer             server;
        private final NettyHttpServer.ApplicationContainer app;
        private final EmbeddedChannel                      channel;
        }
    }
