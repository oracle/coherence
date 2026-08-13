/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.component.net.security;

import com.tangosol.coherence.component.net.Cluster;
import com.tangosol.internal.net.security.DefaultStandardDependencies;
import com.tangosol.net.ClusterPermission;
import com.tangosol.net.Service;
import com.tangosol.net.ServiceInfo;
import com.tangosol.net.cache.LocalCache;
import com.tangosol.net.security.AccessController;
import com.tangosol.net.security.PermissionInfo;
import org.junit.Test;

import javax.security.auth.Subject;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.SignatureException;
import java.security.SignedObject;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author jk 2016.04.21
 */
public class StandardTest
    {

    private static final String PROP_VALID_SUBJECT_EXPIRY = "coherence.security.subject.validation.ttl";

    @Test
    public void shouldValidate() throws Exception
        {
        AccessController controller = new AccessControllerStub();
        testValidate(controller);

        controller = new AccessControllerStubRSA();
        testValidate(controller);
        }


    @Test
    public void shouldNotValidateSameSubjectTwice() throws Exception
        {
        AccessController controller = new AccessControllerStub();
        testNotValidateSameSubjectTwice(controller);

        controller = new AccessControllerStubRSA();
        testValidate(controller);
        }

    @Test
    public void shouldUseSecureValidationToken() throws Exception
        {
        CapturingAccessController   controller   = new CapturingAccessController();
        DefaultStandardDependencies dependencies = new DefaultStandardDependencies();
        Subject                     subject      = new Subject();

        dependencies.setAccessController(controller);

        Standard standard = new Standard();

        standard.setDependencies(dependencies);
        standard.validateSubject("DistributedService", subject);

        assertTrue(controller.getPayload() instanceof Long);
        assertFalse(controller.getPayload() instanceof Double);
        }

    @Test
    public void shouldUseDefaultValidSubjectsExpiry() throws Exception
        {
        assertValidSubjectsExpiry(null, 10000);
        }

    @Test
    public void shouldUseMillisecondValidSubjectsExpiry() throws Exception
        {
        assertValidSubjectsExpiry("2500", 2500);
        }

    @Test
    public void shouldUseDurationValidSubjectsExpiry() throws Exception
        {
        assertValidSubjectsExpiry("3s", 3000);
        }

    @Test
    public void shouldUseMultiComponentValidSubjectsExpiry() throws Exception
        {
        assertValidSubjectsExpiry("1m 1s", 61000);
        }

    @Test
    public void shouldUseDefaultForMalformedValidSubjectsExpiry() throws Exception
        {
        assertValidSubjectsExpiry("not-a-duration", 10000);
        }

    @Test
    public void shouldUseDefaultForNonPositiveValidSubjectsExpiry() throws Exception
        {
        assertValidSubjectsExpiry("0", 10000);
        assertValidSubjectsExpiry("-1", 10000);
        }

    @Test
    public void shouldUseDefaultForOverflowingValidSubjectsExpiry() throws Exception
        {
        assertValidSubjectsExpiry(Long.toString((long) Integer.MAX_VALUE + 1), 10000);
        }

    @Test
    public void shouldUseMaximumValidSubjectsExpiry() throws Exception
        {
        assertValidSubjectsExpiry(Integer.toString(Integer.MAX_VALUE), Integer.MAX_VALUE);
        }

    @Test
    public void shouldUseDefaultForSubMillisecondExcessOverMaximumValidSubjectsExpiry() throws Exception
        {
        assertValidSubjectsExpiry(Integer.MAX_VALUE + "ms 1ns", 10000);
        }

    @Test
    public void shouldUseDefaultForMultiComponentOverflowingValidSubjectsExpiry() throws Exception
        {
        assertValidSubjectsExpiry("81018.51851851852d 1944444.4444444445h 74148192.62244251m", 10000);
        }

    private void assertValidSubjectsExpiry(String sValue, int cExpected) throws Exception
        {
        String sPrevious = System.getProperty(PROP_VALID_SUBJECT_EXPIRY);

        try
            {
            if (sValue == null)
                {
                System.clearProperty(PROP_VALID_SUBJECT_EXPIRY);
                }
            else
                {
                System.setProperty(PROP_VALID_SUBJECT_EXPIRY, sValue);
                }

            Standard   standard = new Standard();
            LocalCache cache    = (LocalCache) getField(standard, "__m_ValidSubjects");

            assertEquals(cExpected, cache.getExpiryDelay());
            }
        finally
            {
            if (sPrevious == null)
                {
                System.clearProperty(PROP_VALID_SUBJECT_EXPIRY);
                }
            else
                {
                System.setProperty(PROP_VALID_SUBJECT_EXPIRY, sPrevious);
                }
            }
        }

    @Test
    public void shouldVerifyMatchingSecureResponse() throws Exception
        {
        AccessControllerStub controller = new AccessControllerStub();
        Subject              subject    = new Subject();
        String               sService   = "DistributedService";
        Standard             standard   = createStandard(controller, subject, sService);
        ClusterPermission    permission = permission(sService, "*", "join");

        registerPendingSecureResponse(standard, sService, permission);

        standard.verifySecureResponse(mockService(sService), permissionInfo(controller, permission, subject));
        }

    @Test
    public void shouldRejectMismatchedSecureResponse() throws Exception
        {
        AccessControllerStub controller  = new AccessControllerStub();
        Subject              subject     = new Subject();
        String               sService    = "DistributedService";
        Standard             standard    = createStandard(controller, subject, sService);
        ClusterPermission    requested   = permission(sService, "Orders", "join");
        ClusterPermission    replayed    = permission(sService, "Accounts", "join");

        registerPendingSecureResponse(standard, sService, requested);

        SecurityException exception = assertThrows(SecurityException.class,
                () -> standard.verifySecureResponse(mockService(sService), permissionInfo(controller, replayed, subject)));

        assertTrue(exception.getMessage().contains("mismatch"));
        }

    @Test
    public void shouldRecordPendingSecureResponseFromCheckPermission() throws Exception
        {
        AccessControllerStub          controller        = new AccessControllerStub();
        Subject                       subject           = new Subject();
        String                        sService          = "DistributedService";
        Standard                      standard          = createStandard(controller, subject, sService);
        ClusterPermission             permission        = permission(sService, "Orders", "join");
        Cluster                       cluster           = mock(Cluster.class);
        Cluster.ClusterService        serviceCluster    = mock(Cluster.ClusterService.class);
        Map<String, PermissionInfo>   mapServiceContext = new HashMap<>();

        when(cluster.isRunning()).thenReturn(true);
        when(cluster.getClusterService()).thenReturn(serviceCluster);
        when(serviceCluster.getService(sService)).thenReturn(null);
        when(serviceCluster.getServiceContext()).thenReturn(mapServiceContext);

        standard.checkPermission(cluster, permission, subject);

        assertTrue(mapServiceContext.containsKey(sService));
        assertEquals(permission, mapServiceContext.get(sService).getPermission());

        standard.verifySecureResponse(mockService(sService), permissionInfo(controller, permission, subject));
        }


    void testValidate(AccessController controller) throws Exception
        {
        DefaultStandardDependencies dependencies  = new DefaultStandardDependencies();
        Subject                     subject       = new Subject();
        AccessController            controllerSpy = spy(controller);

        dependencies.setAccessController(controllerSpy);

        Standard standard = new Standard();

        standard.setDependencies(dependencies);

        standard.validateSubject("DistributedService", subject);

        verify(controllerSpy).encrypt(any(), same(subject));
        verify(controllerSpy).decrypt(nullable(SignedObject.class), same(subject), nullable(Subject.class));
        }

    void testNotValidateSameSubjectTwice(AccessController controller) throws Exception
        {
        DefaultStandardDependencies dependencies  = new DefaultStandardDependencies();
        Subject                     subject       = new Subject();
        AccessController            controllerSpy = spy(controller);

        dependencies.setAccessController(controllerSpy);

        Standard standard = new Standard();

        standard.setDependencies(dependencies);

        standard.validateSubject("DistributedService", subject);
        standard.validateSubject("DistributedService", subject);

        verify(controllerSpy, times(1)).encrypt(any(), same(subject));
        verify(controllerSpy, times(1)).decrypt(nullable(SignedObject.class), same(subject), nullable(Subject.class));
        }

    private Standard createStandard(AccessController controller, Subject subject, String sService)
            throws Exception
        {
        DefaultStandardDependencies dependencies = new DefaultStandardDependencies();

        dependencies.setAccessController(controller);

        Standard standard = new Standard();

        standard.setDependencies(dependencies);
        standard.validateSubject(sService, subject);

        return standard;
        }

    private ClusterPermission permission(String sService, String sCache, String sAction)
        {
        return new ClusterPermission("service=" + sService + (sCache == null ? "" : ",cache=" + sCache), sAction);
        }

    private PermissionInfo permissionInfo(AccessController controller, ClusterPermission permission, Subject subject)
            throws IOException, GeneralSecurityException
        {
        return new PermissionInfo(permission, permission.getServiceName(), controller.encrypt(permission, subject), subject);
        }

    private Service mockService(String sService)
        {
        ServiceInfo info    = mock(ServiceInfo.class);
        Service     service = mock(Service.class);

        when(info.getServiceName()).thenReturn(sService);
        when(service.getInfo()).thenReturn(info);

        return service;
        }

    @SuppressWarnings("unchecked")
    private void registerPendingSecureResponse(Standard standard, String sService, ClusterPermission permission)
            throws Exception
        {
        Map<String, ClusterPermission> map =
                (Map<String, ClusterPermission>) getField(standard, "__m_PendingSecureResponses");

        map.put(sService, permission);
        }

    private Object getField(Standard standard, String sName)
            throws Exception
        {
        Field field = Standard.class.getDeclaredField(sName);

        field.setAccessible(true);

        return field.get(standard);
        }



    public static class AccessControllerStub implements AccessController
        {
        private KeyPairGenerator keyPairGenerator;
        private KeyPair          keyPair;
        private Signature        signature;

        public AccessControllerStub() throws Exception
            {
            signature        = Signature.getInstance("SHA1withDSA");
            keyPairGenerator = KeyPairGenerator.getInstance("DSA");

            keyPairGenerator.initialize(1024);

            keyPair = keyPairGenerator.genKeyPair();

            }


        @Override
        public SignedObject encrypt(Object o, Subject subjEncryptor)
                throws IOException, GeneralSecurityException
            {
            return new SignedObject((Serializable) o, keyPair.getPrivate(), signature);
            }


        @Override
        public Object decrypt(SignedObject so, Subject subjEncryptor, Subject subjDecryptor)
                throws ClassNotFoundException, IOException, GeneralSecurityException
            {
            if (so.verify(keyPair.getPublic(), signature))
                {
                return so.getObject();
                }
            throw new SignatureException("Invalid signature");
            }


        @Override
        public void checkPermission(ClusterPermission permission, Subject subject)
            {
            }
        }

    public static class CapturingAccessController
            extends AccessControllerStub
        {
        public CapturingAccessController() throws Exception
            {
            }

        @Override
        public SignedObject encrypt(Object o, Subject subjEncryptor)
                throws IOException, GeneralSecurityException
            {
            m_oPayload = o;
            return super.encrypt(o, subjEncryptor);
            }

        public Object getPayload()
            {
            return m_oPayload;
            }

        private Object m_oPayload;
        }

    public static class AccessControllerStubRSA implements AccessController
        {
        private KeyPairGenerator keyPairGenerator;
        private KeyPair          keyPair;
        private Signature        signature;

        public AccessControllerStubRSA() throws Exception
            {
            signature        = Signature.getInstance("SHA256withRSA");
            keyPairGenerator = KeyPairGenerator.getInstance("RSA");

            keyPairGenerator.initialize(2048);

            keyPair = keyPairGenerator.genKeyPair();
            }

        @Override
        public SignedObject encrypt(Object o, Subject subjEncryptor)
                throws IOException, GeneralSecurityException
            {
            return new SignedObject((Serializable) o, keyPair.getPrivate(), signature);
            }

        @Override
        public Object decrypt(SignedObject so, Subject subjEncryptor, Subject subjDecryptor)
                throws ClassNotFoundException, IOException, GeneralSecurityException
            {
            if (so.verify(keyPair.getPublic(), signature))
                {
                return so.getObject();
                }
            throw new SignatureException("Invalid signature");
            }

        @Override
        public void checkPermission(ClusterPermission permission, Subject subject)
            {
            }
        }
    }
