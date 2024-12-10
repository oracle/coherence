/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.component.util;


import com.tangosol.net.CacheFactory;

import com.tangosol.util.ExternalizableHelper;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;


/**
 * NameService Unit Test
 *
 * @author jf  2022.02.01
 */
public class NameServiceTest
    {
    /**
     * Regression test for Bug 33381588.
     */
    @Test
    public void shouldNotNameServiceTcpAcceptorDeserializeIdentityToken()
        {
        // NameService TcpAcceptor creation failed coherence edition validation without joining cluster first.
        CacheFactory.getCluster();

        NameService svc = new NameService();

        assertNotNull("NameService.TcpAcceptor should not be null", svc.getAcceptor());

        Object o = svc.getAcceptor().deserializeIdentityToken(ExternalizableHelper.toBinary("subject=admin").toByteArray());
        assertNull("validate that NameService TcpAcceptor deserializeIdentityToken does not deserialize any data", o);
        }
    }
