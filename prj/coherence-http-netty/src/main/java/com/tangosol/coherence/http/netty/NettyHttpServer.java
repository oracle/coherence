/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.http.netty;


import com.oracle.coherence.common.base.Logger;

import com.tangosol.coherence.http.AbstractHttpServer;
import com.tangosol.coherence.http.HttpServer;

import com.tangosol.util.Base;

import io.netty.bootstrap.ServerBootstrap;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;

import io.netty.channel.nio.NioEventLoopGroup;

import io.netty.channel.socket.SocketChannel;

import io.netty.channel.socket.nio.NioServerSocketChannel;

import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpChunkedInput;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;

import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.JdkSslContext;
import io.netty.handler.ssl.SslHandler;

import io.netty.handler.stream.ChunkedInput;
import io.netty.handler.stream.ChunkedWriteHandler;

import io.netty.util.CharsetUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.lang.reflect.Type;

import java.net.InetSocketAddress;
import java.net.URI;

import java.nio.ByteBuffer;

import java.security.Principal;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import java.util.concurrent.atomic.AtomicLong;

import javax.inject.Inject;
import javax.inject.Provider;

import javax.net.ssl.SSLEngine;

import javax.security.auth.Subject;

import javax.ws.rs.ProcessingException;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.api.TypeLiteral;

import org.glassfish.jersey.internal.PropertiesDelegate;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.internal.inject.ReferencingFactory;
import org.glassfish.jersey.internal.util.collection.Ref;

import org.glassfish.jersey.process.internal.RequestScoped;

import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ContainerException;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ContainerResponse;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.internal.ContainerUtils;

import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerResponseWriter;

import org.glassfish.jersey.spi.ExecutorServiceProvider;
import org.glassfish.jersey.spi.ScheduledExecutorServiceProvider;


/**
 * Implementation of {@link HttpServer} that uses Netty Http Server to handle
 * requests.
 * <p>
 * Most of the classes are taken from the Jersey Netty HTTP server and adapted
 * into Coherence REST HTTP server implementation.
 *
 * @author lh  2016.09.12
 * @see <a href="http://netty.io">Netty</a>
 *
 * @since 12.2.1.4.0
 */
public class NettyHttpServer
        extends AbstractHttpServer
    {
    // ----- AbstractHttpServer implementation ------------------------------

    /**
     * {@inheritDoc}
     */
    protected void startInternal()
            throws IOException
        {
        createAndStartNettyServer(initUri(getLocalAddress(), getLocalPort()), initApplicationContainer());

        resetStats();
        }

    /**
     * {@inheritDoc}
     */
    protected void stopInternal()
            throws IOException
        {
        try
            {
            m_cf.channel().close().sync();
            }
        catch (Exception e)
            {
            throw new IOException(e);
            }
        }

    /**
     * {@inheritDoc}
     */
    public String getListenAddress()
        {
        if (m_sListenAddress == null)
            {
            m_sListenAddress = ((InetSocketAddress) m_cf.channel().localAddress()).getHostString();
            }
        return m_sListenAddress;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getListenPort()
        {
        if (m_nListenPort == 0)
            {
            m_nListenPort = ((InetSocketAddress) m_cf.channel().localAddress()).getPort();
            }
        return m_nListenPort;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Object instantiateContainer(ResourceConfig config, ServiceLocator locator)
        {
        return new NettyHttpContainer(config.getApplication(), locator);
        }

    // ---- helpers ---------------------------------------------------------

    /**
     * Initializes a new {@link URI} based on the provided <code>host</code> and <code>port</code>.
     *
     * @param sHost the {@link URI}'s target host
     * @param nPort the {@link URI}'s target port
     *
     * @return a new {@link URI}
     */
    protected URI initUri(String sHost, int nPort)
        {
        if (sHost == null || sHost.isEmpty())
            {
            throw new IllegalArgumentException("Invalid host name: \"" + sHost + '"');
            }
        if (nPort < 0)
            {
            throw new IllegalArgumentException("Invalid port: \"" + nPort + '"');
            }

        UriBuilder uriBuilder = UriBuilder.fromPath(SLASH).host(sHost).port(nPort);
        uriBuilder.scheme(isSecure() ? "https" : "http");
        return uriBuilder.build();
        }

    /**
     * Normalize the path associated with an application resource by removing all trailing
     * whitespace and slash characters.
     *
     * @param sPath resource config context path
     *
     * @return the normalized resource context path
     */
    protected String normalizeResourceContextPath(String sPath)
        {
        String sPathLocal = sPath.trim();

        if (sPathLocal.length() == 1)
            {
            if (sPathLocal.charAt(0) == SLASH_CHAR)
                {
                return sPathLocal;
                }
            else
                {
                throw new IllegalArgumentException(String.format("Illegal resource 'context-path': '%s'.  REST Resource configuration 'context-path' must minimally be '/', found '%s'.", sPathLocal, sPath));
                }
            }

        if (sPathLocal.charAt(0) != SLASH_CHAR)
            {
            throw new IllegalArgumentException(String.format("Illegal resource context 'context-path': %s.  REST resource configuration 'context-path' must begin with '/'.", sPathLocal));
            }

        for (;;)
            {
            int cPathLen = sPathLocal.length();
            int cIdx     = cPathLen - 1;

            if (sPathLocal.charAt(cIdx) == SLASH_CHAR)
                {
                sPathLocal = sPathLocal.substring(0, cIdx).trim();
                continue;
                }
            break;
            }

        return sPathLocal;
        }

    /**
     * Create and initialize an {@link ApplicationContainer} configured based on the results
     * of {@link #getResourceConfig()}
     *
     * @return a newly created and initialized {@link ApplicationContainer}
     *
     * @see #getResourceConfig()
     */
    protected ApplicationContainer initApplicationContainer()
        {
        ApplicationContainer                      app = new ApplicationContainer();
        Map<String, ResourceConfig> mapResourceConfig = getResourceConfig();

        if (!mapResourceConfig.isEmpty())
            {
            for (Map.Entry<String, ResourceConfig> entry : mapResourceConfig.entrySet())
                {
                NettyHttpContainer container = (NettyHttpContainer) createContainer(entry.getValue());
                app.registerContainer(normalizeResourceContextPath(entry.getKey()), container);
                }
            }

        return app;
        }

    /**
     * Create Netty server.
     *
     * @param baseUri   base uri
     * @param container the ApplicationContainer that contains the {@link NettyHttpContainer}s
     *
     * @throws ProcessingException when there is an issue with creating and/or starting the server
     */
    protected void createAndStartNettyServer(final URI baseUri, final ApplicationContainer container)
            throws ProcessingException
        {
        final EventLoopGroup bossGroup   = createBossGroup();
        final EventLoopGroup workerGroup = createWorkerGroup();

        try
            {
            ServerBootstrap bootStrap = new ServerBootstrap();

            bootStrap.option(ChannelOption.SO_REUSEADDR, true);
            bootStrap.option(ChannelOption.SO_BACKLOG, 1024);
            bootStrap.group(bossGroup, workerGroup)
                    .channel(getServerChannelClass())
                    .childHandler(new JerseyServerInitializer(container));

            int     port = getPort(baseUri);
            Channel ch   = bootStrap.bind(port).sync().channel();

            m_cf = ch.closeFuture().addListener(future ->
                                                {
                                                container.shutdown();

                                                bossGroup.shutdownGracefully();
                                                workerGroup.shutdownGracefully();
                                                });
            }
        catch (InterruptedException e)
            {
            throw new ProcessingException(e);
            }
        }

    /**
     * Create boss group {@link EventLoopGroup} with a single thread.
     *
     * @return the boss {@link EventLoopGroup}
     */
    protected EventLoopGroup createBossGroup()
        {
        return new NioEventLoopGroup(1);
        }

    /**
     * Create the worker group {@link EventLoopGroup} with <code>Netty</code>'s default number of threads.
     *
     * @return the worker {@link EventLoopGroup}
     */
    protected EventLoopGroup createWorkerGroup()
        {
        return new NioEventLoopGroup();
        }

    /**
     * Obtain the {@link ServerChannel} implementation class to use.
     *
     * @return the {@link ServerChannel} implementation class to use
     */
    protected Class<? extends ServerChannel> getServerChannelClass()
        {
        return NioServerSocketChannel.class;
        }

    /**
     * Obtain the port for the provided {@link URI}.  If the {@link URI} has no port,
     * then the ports that are standard for the HTTP/HTTPS schemes will be returned.
     *
     * @param uri the {@link URI}
     *
     * @return the port of the provided {@link URI}
     *
     * @throws IllegalArgumentException if the {@link URI}'s scheme is not <code>http</code> or <code>https</code>
     */
    protected static int getPort(URI uri)
        {
        int nPort = uri.getPort();
        if (nPort == -1)
            {
            switch (uri.getScheme())
                {
                case "http":
                    return 80;
                case "https":
                    return 443;
                default:
                    throw new IllegalArgumentException("URI scheme must be 'http' or 'https'.");
                }
            }

        return nPort;
        }

    // ---- inner class: JerseyServerInitializer -------------------------------

    /**
     * Jersey {@link ChannelInitializer}.
     * <p>
     * Adds {@link HttpServerCodec}, {@link ChunkedWriteHandler} and {@link NettyServerHandler}
     * to the channels pipeline.
     *
     * @author Pavel Bucek (pavel.bucek at oracle.com)
     */
    protected class JerseyServerInitializer
            extends ChannelInitializer<SocketChannel>
        {
        // ---- constructors ------------------------------------------------

        /**
         * Constructor.
         *
         * @param container           the {@link ApplicationContainer} for this server instance
         */
        protected JerseyServerInitializer(ApplicationContainer container)
            {
            this.f_container = container;
            }

        // ---- methods from ChannelInitializer -----------------------------

        /**
         * {@inheritDoc}
         */
        @SuppressWarnings("deprecation")
        @Override
        public void initChannel(SocketChannel ch)
            {
            InetSocketAddress channelAddress = ch.localAddress();
            URI                      uriBase = NettyHttpServer.this.initUri(channelAddress.getHostString(),
                                                                            channelAddress.getPort());
            ChannelPipeline                p = ch.pipeline();
            if (isSecure())
                {
                p.addLast(SSL_HANDLER_NAME, new JdkSslContext(getSSLContext(),
                                                              false,
                                                              isAuthMethodCert()
                                                                  ? ClientAuth.REQUIRE
                                                                  : ClientAuth.NONE).newHandler(ch.alloc()));
                }
            p.addLast(new HttpServerCodec());
            p.addLast(new ChunkedWriteHandler());
            p.addLast(new NettyServerHandler(uriBase, f_container));
            }

        // ---- data members ------------------------------------------------

        /**
         * The {@link ApplicationContainer} for this server instance
         */
        private final ApplicationContainer f_container;
        }

    // ---- inner class: ApplicationContainer -------------------------------

    /**
     * Manages context name to {@link NettyHttpContainer} mappings.
     */
    protected final class ApplicationContainer
        {
        /**
         * Registers a {@link NettyHttpContainer} responsible for handling requests for the given
         * context.
         *
         * @param sContext  the context associated with the provided {@link NettyHttpContainer}
         * @param container a {@link NettyHttpContainer} to register
         */
        public void registerContainer(String sContext, NettyHttpContainer container)
            {
            m_mapContainers.put(sContext, container);
            }

        /**
         * Shutdown all the application handlers maintained by this {@link ApplicationContainer}.
         */
        public void shutdown()
            {
            Map<String, ResourceConfig> mapResources = getResourceConfig();
            if (mapResources.isEmpty())
                {
                return;
                }

            for (Map.Entry<String, ResourceConfig> entry : mapResources.entrySet())
                {
                NettyHttpContainer container = m_mapContainers.get(normalizeResourceContextPath(entry.getKey()));
                if (container != null)
                    {
                    container.getApplicationHandler().onShutdown(container);
                    }
                }
            }

        // ---- data members ------------------------------------------------

        /**
         * Map of context names to corresponding {@link NettyHttpContainer}s.
         */
        private final Map<String, NettyHttpContainer> m_mapContainers = new HashMap<>();
        }

    // ---- inner class: NettyHttpContainer -------------------------------

    /**
     * Netty based implementation of a {@link Container} from Jersey.
     * <p>
     * Modified to create security context for the request when SSL or Basic authentication
     * is enabled.
     *
     * @author Pavel Bucek (pavel.bucek at oracle.com)
     */
    protected class NettyHttpContainer
            implements Container
        {
        // ---- constructors ------------------------------------------------

        /**
         * Constructs a new {@link NettyHttpContainer} instance.
         *
         * @param application   the <code>JAX-RS</code> {@link Application}
         * @param parentLocator the parent {@link ServiceLocator}
         */
        public NettyHttpContainer(Application application, final ServiceLocator parentLocator)
            {
            this.m_hApplication = new ApplicationHandler(application, new NettyBinder(), parentLocator);
            this.m_hApplication.onStartup(this);
            }

        // ---- Container interface -----------------------------------------

        @Override
        public ResourceConfig getConfiguration()
            {
            return m_hApplication.getConfiguration();
            }

        @Override
        public ApplicationHandler getApplicationHandler()
            {
            return m_hApplication;
            }

        @Override
        public void reload()
            {
            reload(m_hApplication.getConfiguration());
            }

        @Override
        public void reload(ResourceConfig configuration)
            {
            m_hApplication.onShutdown(this);

            m_hApplication = new ApplicationHandler(configuration);
            m_hApplication.onReload(this);
            m_hApplication.onStartup(this);
            }

        // ---- helper methods ----------------------------------------------

        /**
         * Get {@link java.util.concurrent.ExecutorService}.
         *
         * @return Executor service associated with this f_container.
         */
        ExecutorService getExecutorService()
            {
            return m_hApplication.getInjectionManager().getInstance(ExecutorServiceProvider.class).getExecutorService();
            }

        /**
         * Get {@link ScheduledExecutorService}.
         *
         * @return Scheduled executor service associated with this f_container.
         */
        ScheduledExecutorService getScheduledExecutorService()
            {
            return m_hApplication.getInjectionManager().getInstance(ScheduledExecutorServiceProvider.class).getExecutorService();
            }

        // ---- data members ------------------------------------------------

        /**
         * The application.
         */
        private volatile ApplicationHandler m_hApplication;
        }

    // ---- inner class: NettyServerHandler -------------------------------

    /**
     * {@link io.netty.channel.ChannelInboundHandler} which serves as a bridge
     * between Netty and Jersey.
     */
    protected class NettyServerHandler
            extends ChannelInboundHandlerAdapter
        {
        // ---- constructors ------------------------------------------------

        /**
         * Constructor.
         *
         * @param baseUri   base   {@link URI} of the f_container (includes context path, if any).
         * @param container Netty  f_container implementation.
         */
        public NettyServerHandler(URI baseUri, ApplicationContainer container)
            {
            this.m_uriBase   = baseUri;
            this.m_container = container;
            }

        // ---- ChannelInboundHandlerAdapter methods ------------------------

        @Override
        public void channelRead(final ChannelHandlerContext ctx, Object msg)
            {
            if (msg instanceof HttpRequest)
                {
                // bookkeeping
                incrementRequestCount();
                long ldtStart = Base.getLastSafeTimeMillis();
                clearInputStreamList(); // clearing the content - possible leftover from previous request processing.

                final HttpRequest req = (HttpRequest) msg;

                if (HttpUtil.is100ContinueExpected(req))
                    {
                    ctx.write(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE));
                    }

                // get security context
                SecurityInfo securityInfo = getSecurityInfo(ctx, req);
                String       sAuth        = securityInfo.getAuth();
                Principal    principal    = securityInfo.getPrincipal();
                Subject      subject      = securityInfo.getSubject();
                String       uri          = req.uri();
                int          begin        = uri.indexOf(SLASH_CHAR);
                int          end          = uri.indexOf(SLASH_CHAR, begin + 1);

                String sContext = (begin >= 0 && end > 0)
                                  ? uri.substring(begin, uri.indexOf(SLASH_CHAR, begin + 1)) // extract first element of path
                                  : uri; // single element path

                NettyHttpContainer container = m_container.m_mapContainers.get(sContext);
                if (container == null)
                    {
                    sContext = null;
                    container = m_container.m_mapContainers.get(SLASH);
                    }

                if (container == null)
                    {
                    send404(ctx, req);
                    return;
                    }

                final ContainerRequest requestContext =
                        createContainerRequest(ctx, req, sContext,
                                               new NettySecurityContext(sAuth, principal, isSecure()));

                requestContext.setWriter(new NettyResponseWriter(ctx, req, container));
                requestContext.setRequestScopedInitializer(injectionManager -> {
                    injectionManager.<Ref<ChannelHandlerContext>>getInstance(ChannelHandlerContextTYPE).set(ctx);
                    injectionManager.<Ref<Channel>>getInstance(ChannelTYPE).set(ctx.channel());
                });

                // must be like this, since there is a blocking read from Jersey
                final Subject              finalSubject = subject;
                final NettyHttpContainer finalContainer = container;

                container.getExecutorService().execute(() -> {
                try
                    {
                    handleRequest(finalContainer.getApplicationHandler(), requestContext, finalSubject);
                    logRequestTime(ldtStart);
                    }
                catch (Exception e)
                    {
                    Logger.err("NettyServerHandler.channelRead()->handleRequest(), Caught an exception: " + e.getMessage());
                    }
                });
                }

            if (msg instanceof HttpContent)
                {
                HttpContent httpContent = (HttpContent) msg;
                ByteBuf         content = httpContent.content();

                if (content.isReadable())
                    {
                    m_listInputStreams.add(new ByteBufInputStream(content, true));
                    }

                if (msg instanceof LastHttpContent)
                    {
                    m_listInputStreams.add(NettyInputStream.END_OF_INPUT);
                    }
                }
            }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            {
            incrementErrors();
            if (cause != null)
                {
                Logger.err(String.format("Unexpected exception processing request: %s", cause.toString()), cause);
                }
            ctx.close();
            }

        // ---- helper methods ----------------------------------------------

        /**
         * Obtain the {@link SecurityInfo} for the current {@link HttpRequest}.
         *
         * @param ctx the {@link ChannelHandlerContext} associated with the {@link HttpRequest}
         * @param req the current {@link HttpRequest} to obtain security details from
         *
         * @return a new {@link SecurityInfo} based on the details in the associated {@link HttpRequest}
         */
        protected SecurityInfo getSecurityInfo(ChannelHandlerContext ctx, HttpRequest req)
            {
            return new SecurityInfo(ctx, req).invoke();
            }

        /**
         * Close all contained {@link InputStream}s and clear the list.
         */
        protected void clearInputStreamList()
            {
            m_listInputStreams.forEach(in ->
                           {
                           try
                               {
                               in.close();
                               }
                           catch (IOException e)
                               {
                               e.printStackTrace();
                               }
                           });

            m_listInputStreams.clear();
            }

        /**
         * Create Jersey {@link ContainerRequest} based on Netty {@link HttpRequest}.
         *
         * @param ctx             Netty channel context
         * @param req             Netty Http request
         * @param sContext        the context string
         * @param securityContext the security context
         *
         * @return created Jersey Container Request.
         */
        protected ContainerRequest createContainerRequest(ChannelHandlerContext ctx, final HttpRequest req,
                                                          String sContext, SecurityContext securityContext)
            {
            URI    uriBase = m_uriBase;
            String sReqUri = req.uri();

            if (sReqUri.charAt(0) == SLASH_CHAR)
                {
                sReqUri = sReqUri.substring(1);
                }
            if (sContext != null)
                {
                String sContextNoSlash = sContext.substring(1);
                sReqUri = sContextNoSlash.equals(sReqUri)
                          ? ""
                          : sReqUri.substring(sReqUri.indexOf(sContextNoSlash) + sContext.length());
                }

            URI requestUri = URI.create(uriBase + ContainerUtils.encodeUnsafeCharacters(sReqUri));

            ContainerRequest requestContext = new ContainerRequest(
                    uriBase, requestUri, req.method().name(), securityContext,
                    new PropertiesDelegate()
                        {
                        private final Map<String, Object> properties = new HashMap<>();

                        @Override
                        public Object getProperty(String name)
                            {
                            return properties.get(name);
                            }

                        @Override
                        public Collection<String> getPropertyNames()
                            {
                            return properties.keySet();
                            }

                        @Override
                        public void setProperty(String name, Object object)
                            {
                            properties.put(name, object);
                            }

                        @Override
                        public void removeProperty(String name)
                            {
                            properties.remove(name);
                            }
                        });

            // request entity handling.
            if ((req.headers().contains(HttpHeaderNames.CONTENT_LENGTH) && HttpUtil.getContentLength(req) > 0)
                || HttpUtil.isTransferEncodingChunked(req))
                {
                ctx.channel().closeFuture().addListener(future -> m_listInputStreams.add(NettyInputStream.END_OF_INPUT_ERROR));

                requestContext.setEntityStream(new NettyInputStream(m_listInputStreams));
                }
            else
                {
                requestContext.setEntityStream(new InputStream()
                    {
                    @Override
                    public int read() throws IOException
                        {
                        return -1;
                        }
                    });
                }

            // copying headers from netty request to jersey f_container request context.
            for (String name : req.headers().names())
                {
                requestContext.headers(name, req.headers().getAll(name));
                }

            return requestContext;
            }

        protected void send404(ChannelHandlerContext ctx, HttpRequest req)
            {
            String sPath = req.uri();
            int nQuery = sPath.indexOf('?');
            sPath = nQuery == -1 ? sPath : sPath.substring(0, nQuery);
            String sMessage = String.format("Resource identified by path '%s' not found.", sPath);

            DefaultFullHttpResponse response =
                    new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND);

            HttpHeaders headers = response.headers();
            headers.add(HttpHeaderNames.CONTENT_TYPE,
                        String.format("text/plain; charset=%s", CharsetUtil.UTF_8.displayName()));
            headers.add(HttpHeaderNames.CONTENT_LENGTH, sMessage.length());

            response.content().writeBytes(sMessage.getBytes(CharsetUtil.UTF_8));

            ctx.writeAndFlush(response);
            }

        // ---- inner class: SecurityInfo -----------------------------------

        /**
         * Helper class to obtain an authentication subject, principal, and auth
         * method (i.e., <code>http</code> or <code>https</code>).
         */
        protected class SecurityInfo
            {
            // ---- constructors --------------------------------------------

            /**
             * Creates a new SecurityInfo instance based on the current
             * {@link HttpRequest}.
             *
             * @param ctx the {@link ChannelHandlerContext} associated with the {@link HttpRequest}
             * @param req the current {@link HttpRequest} to obtain security details from
             */
            public SecurityInfo(ChannelHandlerContext ctx, HttpRequest req)
                {
                m_ctx = ctx;
                m_req = req;
                }

            // ---- SecurityInfo methods ------------------------------------

            /**
             * Obtain the authentication method of the associated request.
             *
             * @return the authentication method, if any.  This method returns <code>null</code> if {@link #invoke()}
             *         hasn't been called first.
             */
            public String getAuth()
                {
                return m_sAuth;
                }

            /**
             * Obtain a {@link Principal} from the associated request.
             *
             * @return the {@link Principal} if any.  This method returns <code>null</code> if {@link #invoke()}
             *         hasn't been called first.
             */
            public Principal getPrincipal()
                {
                return m_principal;
                }

            /**
             * Obtain the {@link Subject} from the associated request.
             *
             * @return the {@link Subject} if any.  This method returns <code>null</code> if {@link #invoke()}
             *         hasn't been called first.
             */
            public Subject getSubject()
                {
                return m_subject;
                }

            /**
             * Interrogates the associated {@link HttpRequest} to obtain values for auth method, principal,
             * and subject.
             *
             * @return this {@link SecurityInfo} populated with the security details.
             */
            public SecurityInfo invoke()
                {
                if (isAuthMethodBasic())
                    {
                    m_sAuth = SecurityContext.BASIC_AUTH;
                    m_subject = authenticate(m_req.headers().get(HEADER_AUTHORIZATION));
                    if (m_subject == null)
                        {
                        DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.UNAUTHORIZED);
                        response.headers().add(HEADER_WWW_AUTHENTICATE, DEFAULT_BASIC_AUTH_HEADER_VALUE);
                        m_ctx.writeAndFlush(response);
                        return this;
                        }
                    }
                else if (isSecure() && isAuthMethodCert())
                    {
                    final SSLEngine engine = ((SslHandler) m_ctx.channel().pipeline().get(SSL_HANDLER_NAME)).engine();
                    try
                        {
                        m_sAuth = SecurityContext.CLIENT_CERT_AUTH;
                        m_subject = NettyHttpServer.this.getSubjectFromSession(engine.getSession());
                        }
                    catch (Exception e)
                        {
                        Logger.err("Caught an exception obtaining request security details: " + e.getMessage());
                        }
                    }

                if (m_subject == null)
                    {
                    m_principal = EMPTY_PRINCIPAL;
                    }
                else
                    {
                    m_principal = m_subject.getPrincipals().iterator().next();
                    }
                return this;
                }

            // ---- data members --------------------------------------------

            /**
             * The {@link ChannelHandlerContext} associated with {@link #m_req}.
             */
            protected ChannelHandlerContext m_ctx;

            /**
             * The current {@link HttpRequest}.
             */
            protected HttpRequest m_req;

            /**
             * The authentication method, if any.
             */
            protected String m_sAuth;

            /**
             * The request {@link Subject}, if any.
             */
            protected Subject m_subject;

            /**
             * The request {@link Principal} if any.
             */
            protected Principal m_principal;
            }

        // ---- data members ------------------------------------------------

        /**
         * The base {@link URI} for this container.
         */
        private final URI m_uriBase;

        /**
         * A queue of {@link InputStream}s wrapping Netty buffers containing request data.
         */
        private final LinkedBlockingDeque<InputStream> m_listInputStreams = new LinkedBlockingDeque<>();

        /**
         * The {@link ApplicationContainer} associated with this server.
         */
        private final ApplicationContainer m_container;

        }

    // ---- inner class: NettySecurityContext -------------------------------

    /**
     * Simple implementation of the {@link SecurityContext} interface.
     */
    protected static class NettySecurityContext
            implements SecurityContext
        {

        // ---- constructors ------------------------------------------------

        /**
         * Create a new SimpleSecurityContext instance.
         *
         * @param sAuthScheme string value of the authentication scheme used
         *                    to protect resources
         * @param principal   the Principal containing the name of the
         *                    current authenticated user
         * @param fSecure     a boolean value indicating whether a request
         *                    was made using a secure channel, such as HTTPS
         */
        protected NettySecurityContext(String sAuthScheme, Principal principal, boolean fSecure)
            {
            m_sAuthScheme = sAuthScheme;
            m_principal   = principal;
            m_fSecure     = fSecure;
            }

        // ---- methods from SecurityContext --------------------------------

        /**
         * Return the string value of the authentication scheme used to
         * protect the resource.
         */
        public String getAuthenticationScheme()
            {
            return m_sAuthScheme;
            }

        /**
         * Return a Principal object containing the name of the current
         * authenticated user.
         */
        public Principal getUserPrincipal()
            {
            return m_principal;
            }

        /**
         * Return a boolean indicating whether this request was made using a
         * secure channel, such as HTTPS.
         */
        public boolean isSecure()
            {
            return m_fSecure;
            }

        /**
         * Return a boolean indicating whether the authenticated user is
         * included in the specified logical "role".
         *
         * @param sRole the name of the role
         */
        public boolean isUserInRole(String sRole)
            {
            return false;
            }

        // ---- data members ------------------------------------------------

        /**
         * The authentication scheme.
         */
        private String m_sAuthScheme;

        /**
         * The current authenticated principal.
         */
        private Principal m_principal;

        /**
         * True if the request was made using a secure channel, false
         * otherwise.
         */
        private boolean m_fSecure;
        }

    // ---- inner class: NettyResponseWriter -------------------------------

    /**
     * Based on package-private NettyResponseWriter in Jersey.
     */
    protected class NettyResponseWriter
            implements ContainerResponseWriter
        {
        // ---- constructors ------------------------------------------------

        /**
         * Creates a new {@link NettyResponseWriter} instance.
         *
         * @param ctx       the {@link ChannelHandlerContext} associated with the current request
         * @param req       the current {@link HttpRequest}
         * @param container the {@link NettyHttpContainer} responsible for handling this request
         */
        NettyResponseWriter(ChannelHandlerContext ctx, HttpRequest req, NettyHttpContainer container)
            {
            this.m_ctx       = ctx;
            this.m_request   = req;
            this.m_container = container;
            }

        // ---- methods from ContainerResponseWriter ------------------------

        /**
         * {@inheritDoc}
         */
        @Override
        public synchronized OutputStream writeResponseStatusAndHeaders(long contentLength, ContainerResponse responseContext)
                throws ContainerException
            {
            if (m_fResponseCommitted)
                {
                Logger.fine("NettyHttpServer.writeResponseStatusAndHeaders(): response already written.");
                return null;
                }

            m_fResponseCommitted = true;

            String reasonPhrase = responseContext.getStatusInfo().getReasonPhrase();
            int      statusCode = responseContext.getStatus();

            // bookkeeping
            NettyHttpServer.this.logStatusCount(statusCode);

            HttpResponseStatus status = reasonPhrase == null
                                        ? HttpResponseStatus.valueOf(statusCode)
                                        : new HttpResponseStatus(statusCode, reasonPhrase);

            DefaultHttpResponse response;
            if (contentLength == 0)
                {
                response = new DefaultFullHttpResponse(m_request.protocolVersion(), status);
                }
            else
                {
                response = new DefaultHttpResponse(m_request.protocolVersion(), status);
                }

            for (final Map.Entry<String, List<String>> e : responseContext.getStringHeaders().entrySet())
                {
                response.headers().add(e.getKey(), e.getValue());
                }

            if (contentLength == -1)
                {
                HttpUtil.setTransferEncodingChunked(response, true);
                }
            else
                {
                response.headers().set(HttpHeaderNames.CONTENT_LENGTH, contentLength);
                }

            if (HttpUtil.isKeepAlive(m_request))
                {
                response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
                }

            m_ctx.writeAndFlush(response);

            if (m_request.method() != HttpMethod.HEAD && (contentLength > 0 || contentLength == -1))
                {
                JerseyNettyIOPipe bridge = new JerseyNettyIOPipe();
                m_ctx.writeAndFlush(new HttpChunkedInput(bridge.getSink()));
                return bridge.getSource();
                }
            else
                {
                m_ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
                return null;
                }
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean suspend(long timeOut, TimeUnit timeUnit, final ContainerResponseWriter.TimeoutHandler
                timeoutHandler)
            {
            m_suspendTimeoutHandler = () -> timeoutHandler.onTimeout(NettyResponseWriter.this);

            if (timeOut <= 0)
                {
                return true;
                }

            m_suspendTimeoutFuture =
                    m_container.getScheduledExecutorService().schedule(m_suspendTimeoutHandler, timeOut, timeUnit);

            return true;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setSuspendTimeout(long timeOut, TimeUnit timeUnit) throws IllegalStateException
            {
            // suspend(0, .., ..) was called, so m_suspendTimeoutFuture is null.
            if (m_suspendTimeoutFuture != null)
                {
                m_suspendTimeoutFuture.cancel(true);
                }

            if (timeOut <= 0)
                {
                return;
                }

            m_suspendTimeoutFuture =
                    m_container.getScheduledExecutorService().schedule(m_suspendTimeoutHandler, timeOut, timeUnit);
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public void commit()
            {
            m_ctx.flush();
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public void failure(Throwable error)
            {
            m_ctx.writeAndFlush(
                    new DefaultFullHttpResponse(m_request.protocolVersion(), HttpResponseStatus.INTERNAL_SERVER_ERROR))
                    .addListener(ChannelFutureListener.CLOSE);
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean enableResponseBuffering()
            {
            return true;
            }

        // ---- inner class: JerseyNettyIOPipe

        /**
         * Creates a pipe where Jersey is the producer of data, or in the terms of this class, the <em>source</em>,
         * and Netty is the consumer, or the <em>sink</em>.
         *
         * This pipe allows communication between the ends of the pipe via a space-limited queue.  The source
         * will block if the queue is at capacity.  The sink, however, will never block as it will be running
         * on one or more (not simultaneously) netty threads.
         *
         * Closing of the pipe may be half of full duplex depending on which side of the pipe is being closed.
         * When the source side is closed, this poisons the queue allowing any elements prior to the poison to
         * be consumed by the sink.  If the sink is close, this means no data will be put on the wire, so both
         * sides of the pipe will be shut down.
         */
        protected class JerseyNettyIOPipe
                implements ChannelFutureListener
            {
            // ---- constructors --------------------------------------------

            /**
             * Constructs a new {@link JerseyNettyIOPipe}.
             */
            protected JerseyNettyIOPipe()
                {
                f_channelCtx    = m_ctx;
                f_channel       = f_channelCtx.channel();
                f_channelFuture = f_channel.closeFuture();
                f_queue         = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
                f_sink          = new Sink();
                f_source        = new Source();

                f_channelFuture.addListeners(this);
                }

            // ---- ChannelFutureListener methods ---------------------------

            /**
             * Called when the NIO channel is closed; clean up by calling {@link #closeSink()}.
             *
             * @see ChannelFuture
             * @see ChannelFutureListener
             */
            public void operationComplete(ChannelFuture future) throws Exception
                {
                closeSink(); // closes everything
                }

            // ---- Object methods ------------------------------------------

            /**
             * {@inheritDoc}
             */
            public String toString()
                {
                return "JerseyNettyIOPipe(" +
                       "channel=" + f_channel +
                       ", channel-open=" + f_channel.isOpen() +
                       ", sink-closed=" + m_fSinkClosed +
                       ", source-closed=" + m_fSourceClosed +
                       ", queue-size=" + f_queue.size() +
                       ')';
                }

            // ---- helper methods ------------------------------------------

            /**
             * Return the <em>sink</em>-side of this pipe.
             *
             * @return the <em>sink</em>-side of this pipe.
             */
            protected ChunkedInput<ByteBuf> getSink()
                {
                return f_sink;
                }

            /**
             * Return the <em>source</em>-side of this pipe.
             *
             * @return the <em>source</em>-side of this pipe.
             */
            protected OutputStream getSource()
                {
                return f_source;
                }

            /**
             * Closes the <em>sink</em> and <em>source</em>.
             *
             * @throws IOException if an error occurs
             */
            protected void closeSink() throws IOException
                {
                if (m_fSinkClosed)
                    {
                    return;
                    }

                // Set the flag directly vs calling closeSource.  Sink is closed, no point inserting poison,
                // queue will be cleared.
                m_fSourceClosed = true;
                m_fSinkClosed = true;
                f_channelFuture.removeListener(this);
                f_queue.clear();
                }

            /**
             * Check if the sink has been closed.
             *
             * @return <code>true</code> if the sink is closed, otherwise <code>false</code>
             */
            protected boolean isSinkClosed()
                {
                return m_fSinkClosed;
                }

            /**
             * Closes the <em>source</em>.
             *
             * @throws IOException if an error occurs
             */
            protected void closeSource() throws IOException
                {
                if (m_fSourceClosed)
                    {
                    return;
                    }

                m_fSourceClosed = true;
                poison();
                }

            /**
             * Check if the source has been closed.
             *
             * @return <code>true</code> if the source is closed, otherwise <code>false</code>
             */
            protected boolean isSourceClosed()
                {
                return m_fSourceClosed;
                }

            /**
             * Poison the queue to signal the <em>sink</em> to halt processing
             * once it sees the poison.
             *
             * @throws IOException if the queue was unable to be poisoned
             */
            protected void poison() throws IOException
                {
                try
                    {
                    boolean accepted = f_queue.offer(POISON, 10, TimeUnit.SECONDS);
                    if (!accepted)
                        {
                        throw new IOException();
                        }
                    }
                catch (InterruptedException e)
                    {
                    throw new IOException(e);
                    }
                }

            // ---- InnerClass: Sink ----------------------------------------

            /**
             * Implements the contract as defined by {@link ChunkedInput}.
             */
            protected class Sink
                    implements ChunkedInput<ByteBuf>
                {
                // ---- ChunkedInput methods --------------------------------

                /**
                 * {@inheritDoc}
                 */
                @Override
                public boolean isEndOfInput() throws Exception
                    {
                    if (isSinkClosed())
                        {
                        return true;
                        }

                    ByteBuffer peek = f_queue.peek();
                    return peek == POISON;
                    }

                /**
                 * {@inheritDoc}
                 */
                @Override
                public void close() throws Exception
                    {
                    closeSink();
                    }

                /**
                 * {@inheritDoc}
                 */
                @Override
                public ByteBuf readChunk(ChannelHandlerContext ctx) throws Exception
                    {
                    return readChunk(ctx.alloc());
                    }

                /**
                 * {@inheritDoc}
                 */
                @Override
                public ByteBuf readChunk(ByteBufAllocator allocator) throws Exception
                    {
                    if (isSinkClosed())
                        {
                        return null;
                        }

                    CompositeByteBuf buffer    = null;
                    int              cbWritten = 0;
                    boolean          poisoned  = false;

                    while (!f_queue.isEmpty() && cbWritten < MAX_BUFFER_LEN)
                        {
                        if (f_queue.peek() == POISON)
                            {
                            poisoned = true;
                            break;
                            }

                        ByteBuffer top          = f_queue.take();
                        int        topRemaining = top.remaining();
                        ByteBuf    toAdd        = allocator.buffer(topRemaining);

                        toAdd.setBytes(0, top);
                        toAdd.setIndex(0, topRemaining);

                        if (buffer == null)
                            {
                            buffer = allocator.compositeBuffer();
                            }
                        buffer.addComponent(true, toAdd);
                        cbWritten += topRemaining;
                        f_atmLngProgress.addAndGet(topRemaining);
                        }
                    return buffer != null && buffer.nioBufferCount() > 0
                           ? buffer
                           : poisoned ? null : Unpooled.EMPTY_BUFFER;
                    }

                /**
                 * {@inheritDoc}
                 */
                @Override
                public long length()
                    {
                    return -1;
                    }

                /**
                 * {@inheritDoc}
                 */
                @Override
                public long progress()
                    {
                    return f_atmLngProgress.get();
                    }

                // ---- data members ----------------------------------------

                /**
                 * Track how much data has been written by this sink.
                 */
                protected final AtomicLong f_atmLngProgress = new AtomicLong();
                }

            // ---- InnerClass: Source --------------------------------------

            /**
             * Implements the contract as defined by {@link OutputStream}.
             */
            protected class Source
                    extends OutputStream
                {
                // ---- OutputStream methods --------------------------------

                /**
                 * {@inheritDoc}
                 */
                @Override
                public void write(int b) throws IOException
                    {
                    checkClosed();

                    ByteBuffer buf = ByteBuffer.allocate(1);
                    buf.put((byte) b).flip();
                    writeInternal(buf);
                    }

                /**
                 * {@inheritDoc}
                 */
                @Override
                public void write(byte[] abSrc) throws IOException
                    {
                    checkClosed();

                    write(abSrc, 0, abSrc.length);
                    }

                /**
                 * {@inheritDoc}
                 */
                @Override
                public void write(byte[] abSrc, int cbOff, int cbLen) throws IOException
                    {
                    checkClosed();

                    if (abSrc == null)
                        {
                        throw new NullPointerException();
                        }
                    else if ((cbOff > abSrc.length) ||
                             (cbOff < 0) ||
                             ((cbOff + cbOff) > abSrc.length) ||
                             ((cbOff + cbOff) < 0))
                        {
                        throw new IndexOutOfBoundsException();
                        }
                    else if (cbLen == 0)
                        {
                        return;
                        }

                    writeInternal(ByteBuffer.wrap(copy(abSrc, cbOff, cbLen)));
                    }

                /**
                 * {@inheritDoc}
                 */
                @Override
                public void flush() throws IOException
                    {
                    f_channelCtx.flush();
                    }

                /**
                 * {@inheritDoc}
                 */
                @Override
                public void close() throws IOException
                    {
                    closeSource();
                    }

                // ---- helper methods --------------------------------------

                /**
                 * Queues the provided {@link ByteBuffer} for processing by the <em>sink</em>.
                 *
                 * @param buffer the {@link ByteBuffer} produced by the <em>source</em>
                 *
                 * @throws IOException if the <em>source</em> has been closed or if the buffer
                 *                     cannot be queued
                 */
                protected void writeInternal(ByteBuffer buffer) throws IOException
                    {
                    try
                        {
                        f_queue.put(buffer);
                        checkClosed(); // may have transitioned after modifying the queue
                        }
                    catch (InterruptedException ie)
                        {
                        throw new IOException(ie);
                        }
                    }

                /**
                 * Check if the <em>source</em> is closed.
                 *
                 * @throws IOException if the <em>source</em> is closed.
                 */
                protected void checkClosed() throws IOException
                    {
                    if (isSourceClosed())
                        {
                        throw new IOException("Stream already closed.");
                        }
                    }

                /**
                 * Creates a copy of the provided bytes within the specified bounds.
                 *
                 * @param abSrc source bytes
                 * @param cbOff offset within source bytes
                 * @param cbLen number of bytes to copy
                 *
                 * @return a new <code>byte[]</code> sized to <code>cbLen</code> bytes containing
                 *         the bytes within the specified range
                 */
                protected byte[] copy(byte[] abSrc, int cbOff, int cbLen)
                    {
                    byte[] abCopy = new byte[cbLen];
                    System.arraycopy(abSrc, cbOff, abCopy, 0, cbLen);
                    return abCopy;
                    }
                }

            // ---- data members --------------------------------------------

            /**
             * Context for the current request.
             */
            protected final ChannelHandlerContext f_channelCtx;

            /**
             * The channel the request came in on.
             */
            protected final Channel f_channel;

            /**
             * Hook into the open status of the channel.  Register listeners with the future
             * to be notified if the underlying NIO channel has been closed.
             */
            protected final ChannelFuture f_channelFuture;

            /**
             * The sink-side of this pipe.
             */
            protected final Sink f_sink;

            /**
             * The source-side of this pipe.
             */
            protected final Source f_source;

            /**
             * Flag for sink closed status.
             */
            protected volatile boolean m_fSinkClosed;

            /**
             * Flag for source closed status.
             */
            protected volatile boolean m_fSourceClosed;

            /**
             * Queue to share data between both sides of the pipe.
             */
            protected final LinkedBlockingQueue<ByteBuffer> f_queue;

            /**
             * Queue capacity.
             */
            protected static final int QUEUE_CAPACITY = 32;

            /**
             * When the source is processing the queue, it will attempt to package up to
             * <code>MAX_BUFFER_LEN</code> bytes into a {@link CompositeByteBuf} before returning.
             */
            protected static final int MAX_BUFFER_LEN = 1300;
            }

        // ---- data members ------------------------------------------------

        /**
         * The {@link ChannelHandlerContext} for this request.
         */
        private final ChannelHandlerContext m_ctx;

        /**
         * The current {@link HttpRequest}.
         */
        private final HttpRequest m_request;

        /**
         * The {@link NettyHttpContainer} handling this request.
         */
        private final NettyHttpContainer m_container;

        /**
         * {@link ScheduledFuture} for the current response suspension.
         */
        private volatile ScheduledFuture<?> m_suspendTimeoutFuture;

        /**
         * {@link Runnable} to invoke if the suspended response times out.
         */
        private volatile Runnable m_suspendTimeoutHandler;

        /**
         * Flag indicating if the response has committed.
         */
        private boolean m_fResponseCommitted = false;
        }

    // ----- inner class: NettyBinder ---------------------------------------

    /**
     * {@link AbstractBinder} implementation to allow DI binding of Netty-based artifacts.
     */
    public class NettyBinder
            extends AbstractBinder
        {
        /**
         * {@inheritDoc}
         */
        @Override
        protected void configure()
            {
            bindFactory(NettyChannelHandlerContextReferencingFactory.class)
                    .to(ChannelHandlerContext.class)
                    .proxy(true)
                    .proxyForSameScope(false)
                    .in(RequestScoped.class);

            bindFactory(ReferencingFactory.<ChannelHandlerContext>referenceFactory())
                    .to(ChannelHandlerContextTYPE)
                    .in(RequestScoped.class);

            bindFactory(NettyChannelReferencingFactory.class)
                    .to(Channel.class)
                    .proxy(true)
                    .proxyForSameScope(false)
                    .in(RequestScoped.class);

            bindFactory(ReferencingFactory.<Channel>referenceFactory())
                    .to(ChannelTYPE)
                    .in(RequestScoped.class);
            }
        }

    // ----- inner class: NettyChannelHandlerContextReferencingFactory ------

    /**
     * Provides dependency injection for {@link ChannelHandlerContext}.
     */
    public static class NettyChannelHandlerContextReferencingFactory
            extends ReferencingFactory<ChannelHandlerContext>
        {

        @Inject
        public NettyChannelHandlerContextReferencingFactory(Provider<Ref<ChannelHandlerContext>> referenceFactory)
            {
            super(referenceFactory);
            }
        }

    // ----- inner class: NettyChannelReferencingFactory --------------------

    /**
     * Provides dependency injection for {@link Channel}.
     */
    public static class NettyChannelReferencingFactory
            extends ReferencingFactory<Channel>
        {

        @Inject
        public NettyChannelReferencingFactory(Provider<Ref<Channel>> referenceFactory)
            {
            super(referenceFactory);
            }
        }

    // ---- data members ----------------------------------------------------

    /**
     * Dependency injection {@link Type} for {@link ChannelHandlerContext}.
     */
    private final Type ChannelHandlerContextTYPE = (new TypeLiteral<Ref<ChannelHandlerContext>>()
        {
        }).getType();

    /**
     * Dependency injection {@link Type} for {@link Channel}.
     */
    private final Type ChannelTYPE = (new TypeLiteral<Ref<Channel>>()
        {
        }).getType();

    /**
     * The Netty {@link ChannelFuture}.
     */
    protected ChannelFuture m_cf;

    /**
     * The cached listen address of this server.
     */
    protected String m_sListenAddress;

    /**
     * The cached listen port of this server.
     */
    protected int m_nListenPort;

    /**
     * The name of the {@link SslHandler} used by the Netty runtime.
     */
    private static final String SSL_HANDLER_NAME = "ssl";

    /**
     * Poison token used for queue processing by {@link NettyResponseWriter.JerseyNettyIOPipe}.
     */
    private static final ByteBuffer POISON = ByteBuffer.allocate(0);
    }
