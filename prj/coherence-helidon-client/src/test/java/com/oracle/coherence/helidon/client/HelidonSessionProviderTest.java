/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.helidon.client;

import com.oracle.coherence.client.GrpcSessionConfiguration;
import com.tangosol.io.Serializer;
import com.tangosol.net.Coherence;
import com.tangosol.net.SessionConfiguration;
import com.tangosol.net.SessionProvider;
import io.grpc.Channel;
import io.helidon.config.ClasspathConfigSource;
import io.helidon.config.Config;
import io.helidon.config.spi.ConfigSource;
import org.junit.jupiter.api.Test;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.BeanManager;
import java.util.Collections;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Jonathan Knight  2020.12.18
 */
@SuppressWarnings("unchecked")
public class HelidonSessionProviderTest
    {
    @Test
    public void shouldNotHaveSessionWhenNoCDI()
        {
        SessionConfiguration    configuration = SessionConfiguration.builder().named("test").build();
        SessionProvider.Context context       = new ContextStub();
        HelidonSessionProvider  provider      = new HelidonSessionProvider();
        SessionProvider.Context result        = provider.createSession(configuration, context);

        assertThat(result, is(notNullValue()));
        assertThat(result.isComplete(), is(false));
        }

    @Test
    public void shouldNotHaveSessionWhenNotConfiguredSession()
        {
        ConfigSource      source      = ClasspathConfigSource.create("config-test-with-multiple-sessions.yaml");
        Config            config      = Config.builder(source).build();
        BeanManager       beanManager = mock(BeanManager.class);
        Instance<Object>  instance    = mock(Instance.class);
        Instance<Config>  instanceCfg = mock(Instance.class);

        when(beanManager.createInstance()).thenReturn(instance);
        when(instance.select(Config.class)).thenReturn(instanceCfg);
        when(instanceCfg.stream()).thenReturn(Stream.of(config));

        SessionConfiguration    configuration = SessionConfiguration.builder().named("missing").build();
        SessionProvider.Context context       = new ContextStub();
        HelidonSessionProvider  provider      = new HelidonSessionProvider();
        SessionProvider.Context result        = provider.createSession(beanManager, configuration, context);

        assertThat(result, is(notNullValue()));
        assertThat(result.isComplete(), is(false));
        }

    @Test
    public void shouldNotHaveSessionWhenNotGrpcSession()
        {
        ConfigSource      source      = ClasspathConfigSource.create("config-test-with-multiple-sessions.yaml");
        Config            config      = Config.builder(source).build();
        BeanManager       beanManager = mock(BeanManager.class);
        Instance<Object>  instance    = mock(Instance.class);
        Instance<Config>  instanceCfg = mock(Instance.class);

        when(beanManager.createInstance()).thenReturn(instance);
        when(instance.select(Config.class)).thenReturn(instanceCfg);
        when(instanceCfg.stream()).thenReturn(Stream.of(config));

        SessionConfiguration    configuration = SessionConfiguration.builder().named("Foo").build();
        SessionProvider.Context context       = new ContextStub();
        HelidonSessionProvider  provider      = new HelidonSessionProvider();
        SessionProvider.Context result        = provider.createSession(beanManager, configuration, context);

        assertThat(result, is(notNullValue()));
        assertThat(result.isComplete(), is(false));
        }

    @Test
    public void shouldHaveSessionWhenGrpcSession()
        {
        ConfigSource         source             = ClasspathConfigSource.create("config-test-with-multiple-sessions.yaml");
        Config               config             = Config.builder(source).build();
        BeanManager          beanManager        = mock(BeanManager.class);
        Instance<Object>     instance           = mock(Instance.class);
        Instance<Config>     instanceCfg        = mock(Instance.class);
        Instance<Channel>    instanceChannel    = mock(Instance.class);
        Instance<Serializer> instanceSerializer = mock(Instance.class);
        Channel              channel            = mock(Channel.class);

        when(beanManager.createInstance()).thenReturn(instance);
        when(instance.select(eq(Channel.class), any())).thenReturn(instanceChannel);
        when(instanceChannel.stream()).thenReturn(Stream.of(channel));
        when(instance.select(eq(Serializer.class), any())).thenReturn(instanceSerializer);
        when(instance.select(Config.class)).thenReturn(instanceCfg);
        when(instanceCfg.stream()).thenReturn(Stream.of(config));

        SessionConfiguration    configuration = SessionConfiguration.builder().named("Bar").build();
        ContextStub             context       = new ContextStub();
        HelidonSessionProvider  provider      = new HelidonSessionProvider();
        SessionProvider.Context result        = provider.createSession(beanManager, configuration, context);

        assertThat(result, is(sameInstance(context)));
        assertThat(((ContextStub) result).m_configuration, is(instanceOf(GrpcSessionConfiguration.class)));

        GrpcSessionConfiguration cfg = (GrpcSessionConfiguration) ((ContextStub) result).m_configuration;

        assertThat(cfg.getName(), is(configuration.getName()));
        assertThat(cfg.getScopeName(), is("bar-scope"));
        assertThat(cfg.getChannel(), is(sameInstance(channel)));
        }

    @Test
    public void shouldNotHaveDefaultGrpcSessionIfNotConfigured()
        {
        ConfigSource         source             = ClasspathConfigSource.create("config-test-with-multiple-sessions.yaml");
        Config               config             = Config.builder(source).build();
        BeanManager          beanManager        = mock(BeanManager.class);
        Instance<Object>     instance           = mock(Instance.class);
        Instance<Config>     instanceCfg        = mock(Instance.class);
        Instance<Channel>    instanceChannel    = mock(Instance.class);
        Instance<Serializer> instanceSerializer = mock(Instance.class);
        Channel              channel            = mock(Channel.class);

        when(beanManager.createInstance()).thenReturn(instance);
        when(instance.select(eq(Channel.class), any())).thenReturn(instanceChannel);
        when(instanceChannel.stream()).thenReturn(Stream.of(channel));
        when(instance.select(eq(Serializer.class), any())).thenReturn(instanceSerializer);
        when(instance.select(Config.class)).thenReturn(instanceCfg);
        when(instanceCfg.stream()).thenReturn(Stream.of(config));

        SessionConfiguration    configuration = SessionConfiguration.defaultSession();
        ContextStub             context       = new ContextStub();
        HelidonSessionProvider  provider      = new HelidonSessionProvider();
        SessionProvider.Context result        = provider.createSession(beanManager, configuration, context);

        assertThat(result, is(sameInstance(context)));
        assertThat(((ContextStub) result).m_configuration, is(nullValue()));
        }

    @Test
    public void shouldHaveDefaultGrpcSession()
        {
        ConfigSource         source             = ClasspathConfigSource.create("config-test-with-default-sessions.yaml");
        Config               config             = Config.builder(source).build();
        BeanManager          beanManager        = mock(BeanManager.class);
        Instance<Object>     instance           = mock(Instance.class);
        Instance<Config>     instanceCfg        = mock(Instance.class);
        Instance<Channel>    instanceChannel    = mock(Instance.class);
        Instance<Serializer> instanceSerializer = mock(Instance.class);
        Channel              channel            = mock(Channel.class);

        when(beanManager.createInstance()).thenReturn(instance);
        when(instance.select(eq(Channel.class), any())).thenReturn(instanceChannel);
        when(instanceChannel.stream()).thenReturn(Stream.of(channel));
        when(instance.select(eq(Serializer.class), any())).thenReturn(instanceSerializer);
        when(instance.select(Config.class)).thenReturn(instanceCfg);
        when(instanceCfg.stream()).thenReturn(Stream.of(config));

        SessionConfiguration    configuration = SessionConfiguration.defaultSession();
        ContextStub             context       = new ContextStub();
        HelidonSessionProvider  provider      = new HelidonSessionProvider();
        SessionProvider.Context result        = provider.createSession(beanManager, configuration, context);

        assertThat(result, is(sameInstance(context)));
        assertThat(((ContextStub) result).m_configuration, is(instanceOf(GrpcSessionConfiguration.class)));

        GrpcSessionConfiguration cfg = (GrpcSessionConfiguration) ((ContextStub) result).m_configuration;

        assertThat(cfg.getName(), is(configuration.getName()));
        assertThat(cfg.getScopeName(), is("default-scope"));
        assertThat(cfg.getChannel(), is(sameInstance(channel)));
        }


    @Test
    public void shouldNotHaveSystemGrpcSessionIfNotConfigured()
        {
        ConfigSource         source             = ClasspathConfigSource.create("config-test-with-multiple-sessions.yaml");
        Config               config             = Config.builder(source).build();
        BeanManager          beanManager        = mock(BeanManager.class);
        Instance<Object>     instance           = mock(Instance.class);
        Instance<Config>     instanceCfg        = mock(Instance.class);
        Instance<Channel>    instanceChannel    = mock(Instance.class);
        Instance<Serializer> instanceSerializer = mock(Instance.class);
        Channel              channel            = mock(Channel.class);

        when(beanManager.createInstance()).thenReturn(instance);
        when(instance.select(eq(Channel.class), any())).thenReturn(instanceChannel);
        when(instanceChannel.stream()).thenReturn(Stream.of(channel));
        when(instance.select(eq(Serializer.class), any())).thenReturn(instanceSerializer);
        when(instance.select(Config.class)).thenReturn(instanceCfg);
        when(instanceCfg.stream()).thenReturn(Stream.of(config));

        SessionConfiguration    configuration = SessionConfiguration.builder().named(Coherence.SYSTEM_SESSION).build();
        ContextStub             context       = new ContextStub();
        HelidonSessionProvider  provider      = new HelidonSessionProvider();
        SessionProvider.Context result        = provider.createSession(beanManager, configuration, context);

        assertThat(result, is(sameInstance(context)));
        assertThat(((ContextStub) result).m_configuration, is(nullValue()));
        }

    @Test
    public void shouldHaveSystemGrpcSession()
        {
        ConfigSource         source             = ClasspathConfigSource.create("config-test-with-default-sessions.yaml");
        Config               config             = Config.builder(source).build();
        BeanManager          beanManager        = mock(BeanManager.class);
        Instance<Object>     instance           = mock(Instance.class);
        Instance<Config>     instanceCfg        = mock(Instance.class);
        Instance<Channel>    instanceChannel    = mock(Instance.class);
        Instance<Serializer> instanceSerializer = mock(Instance.class);
        Channel              channel            = mock(Channel.class);

        when(beanManager.createInstance()).thenReturn(instance);
        when(instance.select(eq(Channel.class), any())).thenReturn(instanceChannel);
        when(instanceChannel.stream()).thenReturn(Stream.of(channel));
        when(instance.select(eq(Serializer.class), any())).thenReturn(instanceSerializer);
        when(instance.select(Config.class)).thenReturn(instanceCfg);
        when(instanceCfg.stream()).thenReturn(Stream.of(config));

        SessionConfiguration    configuration = SessionConfiguration.builder().named(Coherence.SYSTEM_SESSION).build();
        ContextStub             context       = new ContextStub();
        HelidonSessionProvider  provider      = new HelidonSessionProvider();
        SessionProvider.Context result        = provider.createSession(beanManager, configuration, context);

        assertThat(result, is(sameInstance(context)));
        assertThat(((ContextStub) result).m_configuration, is(instanceOf(GrpcSessionConfiguration.class)));

        GrpcSessionConfiguration cfg = (GrpcSessionConfiguration) ((ContextStub) result).m_configuration;

        assertThat(cfg.getName(), is(configuration.getName()));
        assertThat(cfg.getScopeName(), is("sys-scope"));
        assertThat(cfg.getChannel(), is(sameInstance(channel)));
        }

    // ----- inner class: ContextStub ---------------------------------------

    static class ContextStub
            extends SessionProvider.DefaultContext
        {
        public ContextStub()
            {
            super(Coherence.Mode.ClusterMember, mock(SessionProvider.class), Collections.emptyList());
            }

        @Override
        public SessionProvider.Context createSession(SessionConfiguration configuration)
            {
            m_configuration = configuration;
            return this;
            }

        SessionConfiguration m_configuration;
        }
    }
