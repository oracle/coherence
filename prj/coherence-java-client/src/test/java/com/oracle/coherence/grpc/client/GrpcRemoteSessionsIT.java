/*
 * Copyright (c) 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.grpc.client;

import com.oracle.coherence.cdi.Remote;
import com.oracle.coherence.cdi.Scope;

import com.tangosol.io.DefaultSerializer;
import com.tangosol.io.pof.ConfigurablePofContext;

import com.tangosol.net.Session;

import io.helidon.microprofile.server.Server;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.CDI;

import javax.inject.Inject;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Jonathan Knight  2019.11.29
 * @since 14.1.2
 */
class GrpcRemoteSessionsIT
    {
    // ----- test lifecycle -------------------------------------------------

    @BeforeAll
    static void setup()
        {
        System.setProperty("coherence.ttl", "0");
        System.setProperty("coherence.clustername", "NamedCacheServiceCdiIT");

        s_server = Server.create().start();
        }

    @AfterAll
    static void cleanupBaseTest()
        {
        s_server.stop();
        }

    // ----- test methods ---------------------------------------------------

    @Test
    void shouldInjectSessionIntoConstructor()
        {
        CtorBean bean = getBean(CtorBean.class);
        assertThat(bean.getSession(), is(notNullValue()));
        }

    @Test
    void shouldInjectRemoteSessionIntoConstructor()
        {
        CtorBean bean = getBean(CtorBean.class);
        assertThat(bean.getRemoteSession(), is(notNullValue()));
        }

    @Test
    void shouldInjectSessionIntoField()
        {
        FieldBean bean = getBean(FieldBean.class);
        assertThat(bean.getSession(), is(notNullValue()));
        }

    @Test
    void shouldInjectRemoteSessionIntoField()
        {
        FieldBean bean = getBean(FieldBean.class);
        assertThat(bean.getRemoteSession(), is(notNullValue()));
        }

    @Test
    void shouldInjectSessionIntoMethod()
        {
        MethodBean bean = getBean(MethodBean.class);
        assertThat(bean.getSession(), is(notNullValue()));
        }

    @Test
    void shouldInjectRemoteSessionIntoMethod()
        {
        MethodBean bean = getBean(MethodBean.class);
        assertThat(bean.getRemoteSession(), is(notNullValue()));
        }

    @Test
    void shouldInjectRemoteSessionWithDefaultSerializer()
        {
        SerializerBean bean = getBean(SerializerBean.class);
        GrpcRemoteSession session = bean.getSession();
        assertThat(session, is(notNullValue()));
        assertThat(session.getSerializerFormat(), is("java"));
        assertThat(session.getSerializer(), is(instanceOf(DefaultSerializer.class)));
        }

    @Test
    void shouldInjectRemoteSessionWithJavaSerializer()
        {
        SerializerBean bean = getBean(SerializerBean.class);
        GrpcRemoteSession session = bean.getJavaSession();
        assertThat(session, is(notNullValue()));
        assertThat(session.getSerializerFormat(), is("java"));
        assertThat(session.getSerializer(), is(instanceOf(DefaultSerializer.class)));
        }

    @Test
    void shouldInjectRemoteSessionWithPofSerializer()
        {
        SerializerBean bean = getBean(SerializerBean.class);
        GrpcRemoteSession session = bean.getPofSession();
        assertThat(session, is(notNullValue()));
        assertThat(session.getSerializerFormat(), is("pof"));
        assertThat(session.getSerializer(), is(instanceOf(ConfigurablePofContext.class)));
        }

    @Test
    void shouldReturnSameSession()
        {
        RemoteSessions producer = getBean(RemoteSessions.class);
        Session sessionOne = producer.createSession(RemoteSessions.name("default"));
        assertThat(sessionOne, is(notNullValue()));
        Session sessionTwo = producer.createSession(RemoteSessions.name("default"));
        assertThat(sessionTwo, is(notNullValue()));
        assertThat(sessionTwo, is(sameInstance(sessionOne)));
        }

    @Test
    void shouldReturnSameSessionFromTwoProducers()
        {
        RemoteSessions producerOne = getBean(RemoteSessions.class);
        RemoteSessions producerTwo = getBean(RemoteSessions.class);
        Session sessionOne = producerOne.createSession(RemoteSessions.name("test-pof"));
        assertThat(sessionOne, is(notNullValue()));
        Session sessionTwo = producerTwo.createSession(RemoteSessions.name("test-pof"));
        assertThat(sessionTwo, is(notNullValue()));
        assertThat(sessionTwo, is(sameInstance(sessionOne)));
        }

    @Test
    void shouldNotReturnSameSessionIfOriginalSessionIsClosed() throws Exception
        {
        RemoteSessions producer = getBean(RemoteSessions.class);
        Session sessionOne = producer.createSession(RemoteSessions.name("test-pof"));
        assertThat(sessionOne, is(notNullValue()));

        sessionOne.close();

        Session sessionTwo = producer.createSession(RemoteSessions.name("test-pof"));
        assertThat(sessionTwo, is(notNullValue()));
        assertThat(sessionTwo, is(not(sameInstance(sessionOne))));
        }

    // ----- helper methods -------------------------------------------------

    protected <T> T getBean(Class<T> type)
        {
        return CDI.current().getBeanManager().createInstance().select(type).get();
        }

    // ----- inner class: FieldBean -----------------------------------------

    @ApplicationScoped
    protected static class FieldBean
        {
        // ----- accessors --------------------------------------------------

        protected Session getSession()
            {
            return m_session;
            }

        protected GrpcRemoteSession getRemoteSession()
            {
            return m_remoteSession;
            }

        // ----- data members -----------------------------------------------

        @Inject
        @Remote
        protected Session m_session;

        @Inject
        @Remote
        protected GrpcRemoteSession m_remoteSession;
        }

    // ----- inner class: SerializerBean ------------------------------------

    @ApplicationScoped
    protected static class SerializerBean
        {
        // ----- accessors --------------------------------------------------

        protected GrpcRemoteSession getSession()
            {
            return m_session;
            }

        protected GrpcRemoteSession getJavaSession()
            {
            return m_javaSession;
            }

        protected GrpcRemoteSession getPofSession()
            {
            return m_pofSession;
            }

        // ----- data members -----------------------------------------------

        @Inject
        @Remote
        @Scope("test")
        private GrpcRemoteSession m_session;

        @Inject
        @Remote
        @Scope("test-java")
        private GrpcRemoteSession m_javaSession;

        @Inject
        @Remote
        @Scope("test-pof")
        private GrpcRemoteSession m_pofSession;
        }

    // ----- inner class: MethodBean ----------------------------------------

    @ApplicationScoped
    protected static class MethodBean
        {
        // ----- injection points -------------------------------------------

        @SuppressWarnings("unused")
        @Inject
        protected void setSession(@Remote Session session)
            {
            this.m_session = session;
            }

        @SuppressWarnings("unused")
        @Inject
        protected void setRemoteSession(@Remote GrpcRemoteSession remoteSession)
            {
            this.m_remoteSession = remoteSession;
            }

        // ----- accessors --------------------------------------------------

        protected Session getSession()
            {
            return m_session;
            }

        protected GrpcRemoteSession getRemoteSession()
            {
            return m_remoteSession;
            }

        // ----- data members -----------------------------------------------

        private Session m_session;

        private GrpcRemoteSession m_remoteSession;
        }

    // ----- inner class: CtorBean ------------------------------------------

    @ApplicationScoped
    private static class CtorBean
        {
        // ----- constructors -----------------------------------------------

        @Inject
        public CtorBean(@Remote Session session,
                        @Remote GrpcRemoteSession remoteSession)
            {
            this.f_session       = session;
            this.f_remoteSession = remoteSession;
            }

        // ----- accessors --------------------------------------------------

        protected Session getSession()
            {
            return f_session;
            }

        protected GrpcRemoteSession getRemoteSession()
            {
            return f_remoteSession;
            }

        // ----- data members -----------------------------------------------

        protected final Session f_session;

        private final GrpcRemoteSession f_remoteSession;
        }

    // ----- data members ---------------------------------------------------

    protected static Server s_server;
    }
