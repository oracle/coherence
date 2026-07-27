/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package executor;

import com.oracle.coherence.concurrent.executor.ClusteredExecutorService;

import com.tangosol.net.CacheService;
import com.tangosol.net.ServiceInfo;

import org.junit.Test;

import java.lang.reflect.Proxy;

import java.security.Principal;
import java.security.PrivilegedAction;

import java.util.Set;

import javax.security.auth.Subject;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

/**
 * Functional subject-capture coverage for clustered executor submissions.
 */
public class ClusteredExecutorSubjectPropagationIT
    {
    @Test
    public void shouldCaptureLocalMemberSubject()
        {
        Subject subject = subject("local");
        TestExecutorService service = new TestExecutorService(cacheService(CacheService.TYPE_DISTRIBUTED));

        Subject captured = Subject.doAs(subject, (PrivilegedAction<Subject>) service::capture);

        assertSame(subject, captured);
        }

    @Test
    public void shouldForceRemoteCacheServiceSubjectToNull()
        {
        Subject subject = subject("remote");
        TestExecutorService service = new TestExecutorService(cacheService(CacheService.TYPE_REMOTE));

        Subject captured = Subject.doAs(subject, (PrivilegedAction<Subject>) service::capture);

        assertNull(captured);
        }

    private static CacheService cacheService(String sType)
        {
        ServiceInfo info = (ServiceInfo) Proxy.newProxyInstance(ClusteredExecutorSubjectPropagationIT.class.getClassLoader(),
                new Class<?>[] {ServiceInfo.class},
                (proxy, method, args) -> "getServiceType".equals(method.getName()) ? sType : null);

        return (CacheService) Proxy.newProxyInstance(ClusteredExecutorSubjectPropagationIT.class.getClassLoader(),
                new Class<?>[] {CacheService.class},
                (proxy, method, args) -> "getInfo".equals(method.getName()) ? info : null);
        }

    private static Subject subject(String sName)
        {
        return new Subject(false, Set.of((Principal) () -> sName), Set.of(), Set.of());
        }

    public static class TestExecutorService
            extends ClusteredExecutorService
        {
        TestExecutorService(CacheService service)
            {
            super(service);
            }

        Subject capture()
            {
            return captureSubmitterSubject();
            }
        }
    }
