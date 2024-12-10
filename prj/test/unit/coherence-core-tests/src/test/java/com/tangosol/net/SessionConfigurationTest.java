/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net;

import com.tangosol.coherence.config.ResolvableParameterList;
import com.tangosol.config.expression.LiteralExpression;
import com.tangosol.config.expression.Parameter;
import com.tangosol.config.expression.ParameterResolver;
import org.junit.Test;

import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;

import static org.hamcrest.collection.IsEmptyIterable.emptyIterable;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.mock;

/**
 * @author Jonathan Knight  2020.11.10
 */
public class SessionConfigurationTest
    {
    @Test
    public void shouldCreateDefaultSessionConfiguration()
        {
        SessionConfiguration session = SessionConfiguration.defaultSession();
        assertThat(session, is(notNullValue()));
        assertThat(session.getName(), is(Coherence.DEFAULT_NAME));
        assertThat(session.getScopeName(), is(Coherence.DEFAULT_SCOPE));
        assertThat(session.getConfigUri().isPresent(), is(true));
        assertThat(session.getConfigUri().get(), is(CacheFactoryBuilder.URI_DEFAULT));
        assertThat(session.getInterceptors(), is(emptyIterable()));
        assertThat(session.isEnabled(), is(true));
        assertThat(session.getPriority(), is(SessionConfiguration.DEFAULT_PRIORITY));
        }

    @Test
    public void shouldCreateDefaultSessionConfigurationWithConfigUri()
        {
        SessionConfiguration session = SessionConfiguration.create("foo.xml");
        assertThat(session, is(notNullValue()));
        assertThat(session.getName(), is(Coherence.DEFAULT_NAME));
        assertThat(session.getScopeName(), is(Coherence.DEFAULT_SCOPE));
        assertThat(session.getConfigUri().isPresent(), is(true));
        assertThat(session.getConfigUri().get(), is("foo.xml"));
        assertThat(session.getInterceptors(), is(emptyIterable()));
        assertThat(session.isEnabled(), is(true));
        assertThat(session.getPriority(), is(SessionConfiguration.DEFAULT_PRIORITY));
        }

    @Test
    public void shouldCreateSessionConfigurationWithNameAndConfigUri()
        {
        SessionConfiguration session = SessionConfiguration.create("Foo", "foo.xml");
        assertThat(session, is(notNullValue()));
        assertThat(session.getName(), is("Foo"));
        assertThat(session.getScopeName(), is(Coherence.DEFAULT_SCOPE));
        assertThat(session.getConfigUri().isPresent(), is(true));
        assertThat(session.getConfigUri().get(), is("foo.xml"));
        assertThat(session.getInterceptors(), is(emptyIterable()));
        assertThat(session.isEnabled(), is(true));
        assertThat(session.getPriority(), is(SessionConfiguration.DEFAULT_PRIORITY));
        }

    @Test
    public void shouldCreateSessionConfigurationWithNameConfigUriAndScope()
        {
        SessionConfiguration session = SessionConfiguration.create("Foo", "foo.xml", "Bar");
        assertThat(session, is(notNullValue()));
        assertThat(session.getName(), is("Foo"));
        assertThat(session.getScopeName(), is("Bar"));
        assertThat(session.getConfigUri().isPresent(), is(true));
        assertThat(session.getConfigUri().get(), is("foo.xml"));
        assertThat(session.getInterceptors(), is(emptyIterable()));
        assertThat(session.isEnabled(), is(true));
        assertThat(session.getPriority(), is(SessionConfiguration.DEFAULT_PRIORITY));
        }

    @Test
    public void shouldBuildWithDefaultName()
        {
        SessionConfiguration cfg = SessionConfiguration
                .builder()
                .build();

        assertThat(cfg.getName(), is(Coherence.DEFAULT_NAME));
        }

    @Test
    public void shouldBuildWithSpecifiedName()
        {
        SessionConfiguration cfg = SessionConfiguration
                .builder()
                .named("Foo")
                .build();

        assertThat(cfg.getName(), is("Foo"));
        }

    @Test
    public void shouldBuildWithDefaultScope()
        {
        SessionConfiguration cfg = SessionConfiguration
                .builder()
                .build();

        assertThat(cfg.getScopeName(), is(Coherence.DEFAULT_SCOPE));
        }

    @Test
    public void shouldBuildWithSpecifiedNameAndScope()
        {
        SessionConfiguration cfg = SessionConfiguration
                .builder()
                .named("Foo")
                .withScopeName("Bar")
                .build();

        assertThat(cfg.getName(), is("Foo"));
        assertThat(cfg.getScopeName(), is("Bar"));
        }

    @Test
    public void shouldBuildWithDefaultClassLoader()
        {
        SessionConfiguration cfg = SessionConfiguration
                .builder()
                .build();

        Optional<ClassLoader> optional = cfg.getClassLoader();
        assertThat(optional, is(notNullValue()));
        assertThat(optional.isPresent(), is(false));
        }

    @Test
    public void shouldBuildWithSpecifiedClassLoader()
        {
        ClassLoader          loader = mock(ClassLoader.class);
        SessionConfiguration cfg    = SessionConfiguration
                .builder()
                .withClassLoader(loader)
                .build();

        Optional<ClassLoader> optional = cfg.getClassLoader();
        assertThat(optional, is(notNullValue()));
        assertThat(optional.isPresent(), is(true));
        assertThat(optional.get(), is(sameInstance(loader)));
        }

    @Test
    public void shouldBuildWithDefaultConfigURI()
        {
        SessionConfiguration cfg = SessionConfiguration
                .builder()
                .build();

        Optional<String> option = cfg.getConfigUri();
        assertThat(option, is(notNullValue()));
        assertThat(option.isPresent(), is(false));
        }

    @Test
    public void shouldBuildWithSpecifiedConfigURI()
        {
        SessionConfiguration cfg = SessionConfiguration
                .builder()
                .withConfigUri("foo.xml")
                .build();

        Optional<String> option = cfg.getConfigUri();
        assertThat(option, is(notNullValue()));
        assertThat(option.isPresent(), is(true));
        assertThat(option.get(), is("foo.xml"));
        }

    @Test
    public void shouldBuildWithParameters()
        {
        SessionConfiguration cfg = SessionConfiguration
                .builder()
                .withParameter("param1", "test")
                .withParameter("param2", 100)
                .withParameter("param3", new LiteralExpression<>(1234))
                .withParameter(new Parameter("param4", 999L))
                .build();

        Optional<ParameterResolver> optional = cfg.getParameterResolver();
        assertThat(optional.isPresent(), is(true));
        ParameterResolver resolver = optional.get();
        assertThat(resolver.resolve("param1").evaluate(resolver).get(), is("test"));
        assertThat(resolver.resolve("param2").evaluate(resolver).get(), is(100));
        assertThat(resolver.resolve("param3").evaluate(resolver).get(), is(1234));
        assertThat(resolver.resolve("param4").evaluate(resolver).get(), is(999L));
        }

    @Test
    public void shouldBuildWithParameterResolver()
        {
        ResolvableParameterList list = new ResolvableParameterList();
        list.add(new Parameter("param1", "test"));
        list.add(new Parameter("param2", 100));
        list.add(new Parameter("param3", new LiteralExpression<>(1234)));
        list.add(new Parameter("param4", 999L));

        SessionConfiguration cfg = SessionConfiguration
                .builder()
                .withParameterResolver(list)
                .build();

        Optional<ParameterResolver> optional = cfg.getParameterResolver();
        assertThat(optional.isPresent(), is(true));
        ParameterResolver resolver = optional.get();
        assertThat(resolver.resolve("param1").evaluate(resolver).get(), is("test"));
        assertThat(resolver.resolve("param2").evaluate(resolver).get(), is(100));
        assertThat(resolver.resolve("param3").evaluate(resolver).get(), is(1234));
        assertThat(resolver.resolve("param4").evaluate(resolver).get(), is(999L));
        }

    @Test
    public void shouldBuildWithParametersAndParameterResolver()
        {
        ResolvableParameterList list = new ResolvableParameterList();
        list.add(new Parameter("param1", "testA"));
        list.add(new Parameter("param2", 100));
        list.add(new Parameter("param4", 999L));

        SessionConfiguration cfg = SessionConfiguration
                .builder()
                .withParameter("param1", "testB")
                .withParameter("param3", new LiteralExpression<>(1234))
                .withParameterResolver(list)
                .build();

        Optional<ParameterResolver> optional = cfg.getParameterResolver();
        assertThat(optional.isPresent(), is(true));
        ParameterResolver resolver = optional.get();
        assertThat(resolver.resolve("param1").evaluate(resolver).get(), is("testB"));
        assertThat(resolver.resolve("param2").evaluate(resolver).get(), is(100));
        assertThat(resolver.resolve("param3").evaluate(resolver).get(), is(1234));
        assertThat(resolver.resolve("param4").evaluate(resolver).get(), is(999L));
        }
    }
