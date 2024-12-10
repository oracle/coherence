/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.component.net.security;

import com.tangosol.internal.net.security.DefaultStandardDependencies;
import com.tangosol.net.ClusterPermission;
import com.tangosol.net.security.AccessController;
import org.junit.Test;

import javax.security.auth.Subject;

import java.io.IOException;
import java.io.Serializable;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.SignatureException;
import java.security.SignedObject;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author jk 2016.04.21
 */
public class StandardTests
    {

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