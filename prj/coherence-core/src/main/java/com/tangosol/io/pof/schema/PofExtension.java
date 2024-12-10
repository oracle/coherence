/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io.pof.schema;

import com.oracle.coherence.common.schema.PropertyHandler;
import com.oracle.coherence.common.schema.SchemaExtension;
import com.oracle.coherence.common.schema.TypeHandler;

import com.tangosol.io.pof.schema.handler.ClassFileHandler;
import com.tangosol.io.pof.schema.handler.XmlHandler;

import java.util.Arrays;
import java.util.Collection;

/**
 * An implementation of a {@link SchemaExtension} that registers type and
 * property handlers capable of extracting POF-related metadata from various
 * schema sources.
 *
 * @author as  2013.08.28
 */
public class PofExtension implements SchemaExtension
    {
    @Override
    public String getName()
        {
        return "pof";
        }

    @Override
    public Collection<TypeHandler> getTypeHandlers()
        {
        return Arrays.asList(new TypeHandler[] {
                new ClassFileHandler.ClassFileTypeHandler(),
                new XmlHandler.XmlTypeHandler()});
        }

    @Override
    public Collection<PropertyHandler> getPropertyHandlers()
        {
        return Arrays.asList(new PropertyHandler[] {
                new ClassFileHandler.ClassFilePropertyHandler(),
                new XmlHandler.XmlPropertyHandler()});
        }
    }
