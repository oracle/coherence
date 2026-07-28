/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.health;

import com.tangosol.coherence.http.AbstractGenericHttpServer;

import com.tangosol.internal.http.HttpException;
import com.tangosol.internal.http.HttpMethod;
import com.tangosol.internal.http.HttpRequest;
import com.tangosol.internal.http.PathParameters;
import com.tangosol.internal.http.QueryParameters;
import com.tangosol.internal.http.Response;

import com.tangosol.net.security.IdentityAsserter;

import com.tangosol.util.ResourceRegistry;
import com.tangosol.util.SimpleResourceRegistry;

import org.junit.After;
import org.junit.Test;

import java.io.InputStream;

import java.net.URI;

import java.nio.charset.StandardCharsets;

import java.security.Principal;

import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import javax.security.auth.Subject;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

/**
 * Unit tests for health HTTP mutator security gates.
 *
 * @author jk  2026.05.14
 * @since 26.04
 */
public class HealthHttpSecurityTest
    {
    @After
    public void cleanup()
        {
        System.clearProperty(HealthHttpHandler.PROP_MUTATORS_ENABLED);
        System.clearProperty(HealthHttpHandler.PROP_AUTH);
        }

    @Test
    public void shouldLeaveReadOnlyProbeUnauthenticated()
        {
        HealthHttpHandler handler = new HealthHttpHandler();

        handler.beforeRouting(request(HttpMethod.GET, "/ready", null));
        }

    @Test
    public void shouldRejectDefaultDisabledMutatorBeforeRouting()
        {
        HealthHttpHandler handler = new HealthHttpHandler();

        assertStatus(Response.Status.FORBIDDEN, () -> handler.beforeRouting(request(HttpMethod.PUT, "/suspend", null)));
        }

    @Test
    public void shouldGateDirectSuspendBeforeClusterAccess()
        {
        HealthHttpHandler handler = new HealthHttpHandler();

        assertStatus(Response.Status.FORBIDDEN, () -> handler.suspend(request(HttpMethod.PUT, "/suspend", null)));
        }

    @Test
    public void shouldGateDirectResumeBeforeClusterAccess()
        {
        HealthHttpHandler handler = new HealthHttpHandler();

        assertStatus(Response.Status.FORBIDDEN, () -> handler.resume(request(HttpMethod.PUT, "/resume", null)));
        }

    @Test
    public void shouldRejectEnabledMutatorWithUnsupportedAuthMethod()
        {
        System.setProperty(HealthHttpHandler.PROP_MUTATORS_ENABLED, "true");
        System.setProperty(HealthHttpHandler.PROP_AUTH, "none");

        HealthHttpHandler handler = new HealthHttpHandler();

        assertStatus(Response.Status.FORBIDDEN, () -> handler.beforeRouting(request(HttpMethod.PUT, "/suspend", null)));
        }

    @Test
    public void shouldRejectEnabledBasicMutatorWithoutCredentials()
        {
        System.setProperty(HealthHttpHandler.PROP_MUTATORS_ENABLED, "true");

        HealthHttpHandler handler = new HealthHttpHandler();

        assertStatus(Response.Status.UNAUTHORIZED, () -> handler.beforeRouting(request(HttpMethod.PUT, "/suspend", null)));
        }

    @Test
    public void shouldRejectMalformedBasicCredentials()
        {
        System.setProperty(HealthHttpHandler.PROP_MUTATORS_ENABLED, "true");

        HealthHttpHandler handler = new HealthHttpHandler();

        assertStatus(Response.Status.UNAUTHORIZED, () -> handler.beforeRouting(request(HttpMethod.PUT, "/suspend", "Basic !!!")));
        }

    @Test
    public void shouldRejectCertAuthWithoutPeerSubject()
        {
        System.setProperty(HealthHttpHandler.PROP_MUTATORS_ENABLED, "true");
        System.setProperty(HealthHttpHandler.PROP_AUTH, AbstractGenericHttpServer.AUTH_CERT);

        HealthHttpHandler handler = new HealthHttpHandler();

        assertStatus(Response.Status.FORBIDDEN, () -> handler.beforeRouting(request(HttpMethod.PUT, "/resume", null)));
        }

    @Test
    public void shouldRegisterBasicAuthenticatedSubject()
        {
        System.setProperty(HealthHttpHandler.PROP_MUTATORS_ENABLED, "true");

        Subject           subject = new Subject();
        HealthHttpHandler handler = new TestHealthHttpHandler((token, service) -> subject);
        TestRequest       request = request(HttpMethod.PUT, "/suspend/PartitionedCacheOne", basic("client:pass:word"));

        handler.beforeRouting(request);

        assertThat(request.getResourceRegistry().getResource(Subject.class), is(subject));
        }

    @Test
    public void shouldRequireBasicAndPeerSubjectForCertBasic()
        {
        System.setProperty(HealthHttpHandler.PROP_MUTATORS_ENABLED, "true");
        System.setProperty(HealthHttpHandler.PROP_AUTH, AbstractGenericHttpServer.AUTH_CERT_BASIC);

        Subject           subject = new Subject(false, Set.of((Principal) () -> "peer"), Collections.emptySet(), Collections.emptySet());
        HealthHttpHandler handler = new TestHealthHttpHandler((token, service) -> subject);
        TestRequest       request = request(HttpMethod.PUT, "/resume", basic("client:pass:word"));

        request.getResourceRegistry().registerResource(Subject.class, subject);

        handler.beforeRouting(request);

        assertThat(request.getResourceRegistry().getResource(Subject.class), is(subject));
        assertThat(subject.getPrincipals().iterator().next().getName(), is("peer"));
        }

    private static TestRequest request(HttpMethod method, String sPath, String sAuthorization)
        {
        TestRequest request = new TestRequest(method, URI.create("http://127.0.0.1:1234" + sPath));
        if (sAuthorization != null)
            {
            request.withHeader(AbstractGenericHttpServer.HEADER_AUTHORIZATION, sAuthorization);
            }
        return request;
        }

    private static String basic(String sCredentials)
        {
        return "Basic " + Base64.getEncoder().encodeToString(sCredentials.getBytes(StandardCharsets.US_ASCII));
        }

    private static void assertStatus(Response.Status status, Runnable runnable)
        {
        try
            {
            runnable.run();
            fail("Expected HTTP " + status.getStatusCode());
            }
        catch (HttpException e)
            {
            assertThat(e.getStatus(), is(status.getStatusCode()));
            }
        }

    // ----- inner class: TestHealthHttpHandler ----------------------------

    private static class TestHealthHttpHandler
            extends HealthHttpHandler
        {
        TestHealthHttpHandler(IdentityAsserter asserter)
            {
            f_asserter = asserter;
            }

        @Override
        protected IdentityAsserter getIdentityAsserter()
            {
            return f_asserter;
            }

        private final IdentityAsserter f_asserter;
        }

    // ----- inner class: TestRequest --------------------------------------

    private static class TestRequest
            implements HttpRequest
        {
        TestRequest(HttpMethod method, URI uri)
            {
            f_method = method;
            f_uri    = uri;
            }

        TestRequest withHeader(String sName, String sValue)
            {
            f_mapHeaders.put(sName, sValue);
            return this;
            }

        @Override
        public HttpMethod getMethod()
            {
            return f_method;
            }

        @Override
        public URI getBaseURI()
            {
            return URI.create("http://127.0.0.1:1234/");
            }

        @Override
        public URI getRequestURI()
            {
            return f_uri;
            }

        @Override
        public String getHeaderString(String name)
            {
            return f_mapHeaders.get(name);
            }

        @Override
        public QueryParameters getQueryParameters()
            {
            return QueryParameters.EMPTY;
            }

        @Override
        public void setPathParameters(PathParameters parameters)
            {
            m_pathParameters = parameters;
            }

        @Override
        public PathParameters getPathParameters()
            {
            return m_pathParameters;
            }

        @Override
        public InputStream getBody()
            {
            return InputStream.nullInputStream();
            }

        @Override
        public Map<String, Object> getJsonBody(Function<InputStream, Map<String, Object>> fnParser)
            {
            return Collections.emptyMap();
            }

        @Override
        public ResourceRegistry getResourceRegistry()
            {
            return f_registry;
            }

        private final HttpMethod       f_method;
        private final URI              f_uri;
        private final ResourceRegistry f_registry      = new SimpleResourceRegistry();
        private final Map<String, String> f_mapHeaders = new HashMap<>();
        private       PathParameters   m_pathParameters = PathParameters.EMPTY;
        }
    }
