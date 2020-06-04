/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io.pof.generator;

import com.oracle.coherence.common.schema.ClassFileSchemaSource;
import com.oracle.coherence.common.schema.Schema;
import com.oracle.coherence.common.schema.SchemaBuilder;

import com.tangosol.io.pof.schema.annotation.PortableType;

import java.io.File;
import java.io.InputStream;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Custom clas loader that instruments annotated {@link PortableType}s
 * at load time in order to test serialization.
 *
 * @author as  2012.06.04
 */
public class PortableTypeLoader
        extends ClassLoader
    {
    private Schema schema;
    private Map<String, Class> loadedClasses = new HashMap<>();

    public PortableTypeLoader()
        {
        schema = new SchemaBuilder()
                .addSchemaSource(new ClassFileSchemaSource()
                                         .withClassFile(Paths.get("data", "evolvable", "v2", "Animal.class"))
                                         .withClassFile(Paths.get("data", "evolvable", "v2", "Dog.class"))
                                         .withClassFile(Paths.get("data", "evolvable", "v2", "Pet.class"))
                                         .withClassFile(Paths.get("data", "evolvable", "v3", "Animal.class"))
                                         .withClassFile(Paths.get("data", "evolvable", "v3", "Dog.class"))
                                         .withClassFile(Paths.get("data", "evolvable", "v3", "Terrier.class"))
                                         .withClassFile(Paths.get("data", "evolvable", "v3", "Pet.class"))
                                         .withClassFile(Paths.get("data", "evolvable", "v3", "Address.class"))
                                         .withClassFile(Paths.get("data", "evolvable", "v3", "Zoo.class"))
                                         .withClassFile(Paths.get("data", "evolvable", "v3", "NonPofType.class"))
                                         .withClassFile(Paths.get("data", "evolvable", "v4", "Dog.class"))
                                         .withClassFile(Paths.get("data", "evolvable", "AllTypes.class"))
                                         .withClassFile(Paths.get("data", "evolvable", "EmptyClass.class"))
                                         .withClassFile(Paths.get("data", "evolvable", "Color.class"))
                                         .withMissingPropertiesAsObject()
                )
                .build();
        }

    @Override
    public Class<?> loadClass(String className)
            throws ClassNotFoundException
        {
        if (loadedClasses.containsKey(className))
            {
            return loadedClasses.get(className);
            }
        try
            {
            if (className.startsWith("data.evolvable.")
                    && !className.equals("data.evolvable.Color")
                    && schema.findTypeByJavaName(className) != null)
                {
                String sClassPathName = className.replace('.', '/') + ".class";

                InputStream in = getClass().getClassLoader().getResourceAsStream(sClassPathName);

                PortableTypeGenerator gen = new PortableTypeGenerator(schema, in, true);

                gen.instrumentClass();

                byte[]   clsBytes = gen.getClassBytes();
                Class<?> clazz    = defineClass(className, clsBytes, 0, clsBytes.length);

                loadedClasses.put(className, clazz);

                return clazz;
                }
            }
        catch (Exception e)
            {
            e.printStackTrace();
            }

        return super.loadClass(className);
        }
    }
