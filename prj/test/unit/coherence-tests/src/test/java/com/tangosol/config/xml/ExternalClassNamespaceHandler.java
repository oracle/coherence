/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.config.xml;

import com.tangosol.config.xml.AbstractNamespaceHandler;

/**
 * The {@link ExternalClassNamespaceHandler} is for testing the handling of external classes.
 *
 * @author dr
 */
public class ExternalClassNamespaceHandler
        extends AbstractNamespaceHandler
    {
    /**
     * Constructs an {@link ExternalClassNamespaceHandler}
     */
    public ExternalClassNamespaceHandler()
        {
        registerProcessor("test", new TestElementProcessor());
        registerProcessor("test", new TestAttributeProcessor());
        registerElementType("test-type", TestElementProcessor.class);
        registerAttributeType("test-type", TestAttributeProcessor.class);
        registerElementType("test-simple-type", Foo.class);
        registerAttributeType("test-simple-type", Foo.class);
        }

    /**
     * A simple empty static inner class
     */
    public static class Foo
        {
        }
    }
