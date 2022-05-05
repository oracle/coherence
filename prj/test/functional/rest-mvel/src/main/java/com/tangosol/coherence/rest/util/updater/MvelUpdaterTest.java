/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest.util.updater;

import java.lang.reflect.Constructor;

import java.net.URL;
import java.net.URLClassLoader;

import java.util.stream.IntStream;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by shyaradh on 2017.01.04
 */
public class MvelUpdaterTest
    {
    @Test
    public void testAsmOptimization() throws Exception
        {
        // this is a testcase added to verify the fix for Bug 25330751
        // the way Mvel optimizes ASM, of an expression is evaluated more than 50 times
        // the expression and the corresponding class is optimized using ASM
        // Now before the fix, since were were using a static ParserContext, the
        // ParserContext is initialized with the current context classloader.
        // But in container scenarios, where there can be multiple applications
        // this static initialization does not work, as the classes in
        // the current ClassLoader may not exists in the statically initialized
        // ParserContext ClassLoader. In this test case, we are trying to simulate
        // 2 different applications trying to evaluate on different objects

        ClassLoader parent = Thread.currentThread().getContextClassLoader();

        MvelUpdater    localNumberUpdater     = new MvelUpdater("LocalNumber");
        URLClassLoader loader1                =
                new URLClassLoader(new URL[]{this.getClass().getClassLoader().getResource("testclasses1.jar")}, parent);
        Class          phoneNumberClass       = loader1.loadClass("com.tangosol.examples.pof.PhoneNumber");
        Constructor    phoneNumberConstructor = phoneNumberClass.getConstructor();
        Object         phoneNumberObj         = phoneNumberConstructor.newInstance();

        Thread.currentThread().setContextClassLoader(loader1);

        IntStream.range(0, 100).forEach(i -> localNumberUpdater.update(phoneNumberObj, 100L));

        MvelUpdater    street1Updator     = new MvelUpdater("Street1");
        URLClassLoader loader2            =
                new URLClassLoader(new URL[]{this.getClass().getClassLoader().getResource("testclasses2.jar")}, parent);
        Class          addressClass       = loader2.loadClass("com.tangosol.examples.pof.Address");
        Constructor    addressconstructor = addressClass.getConstructor();
        Object         addressObj         = addressconstructor.newInstance();

        Thread.currentThread().setContextClassLoader(loader2);

        IntStream.range(0, 100).forEach(i -> street1Updator.update(addressObj, "street12"));
        }

    }
