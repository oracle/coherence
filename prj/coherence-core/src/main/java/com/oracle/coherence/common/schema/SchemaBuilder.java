/*
 * Copyright (c) 2021 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.common.schema;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;


/**
 * The {@code SchemaBuilder} is responsible for creating and populating a
 * {@link Schema} instance from a set of configured {@link SchemaSource}s.
 * <p/>
 * By default, the {@code SchemaBuilder} will look for the {@code SchemaSource}s
 * available in the classpath using Java {@code ServiceLoader} mechanism, but
 * more often than not, you will want to register {@code SchemaSource}s
 * explicitly by calling {@link #addSchemaSource(SchemaSource)} method directly.
 * <p/>
 * For example, the Maven plugin used to add serialization code to classes
 * marked with {@link com.tangosol.io.pof.schema.annotation.PortableType}
 * annotation defines schema as follows:
 * <pre>
 * Schema schema = new SchemaBuilder()
 *         .addSchemaSource(
 *                 new ClassFileSchemaSource()
 *                         .withClassesFromDirectory(outputDirectory)
 *                         .withTypeFilter(hasAnnotation(PortableType.class))
 *         )
 *         .build();
 * </pre>
 * In his example, we are only interested in compiled classes from the output
 * directory that are annotated with {@code @PortableType} annotation, so we use
 * {@link ClassFileSchemaSource} and the appropriate type filter to select only
 * those classes for inclusion into the schema.
 *
 * @author as  2013.07.08
 *
 * @see Schema
 * @see SchemaSource
 */
public class SchemaBuilder
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Construct a {@code SchemaBuilder} instance.
     * <p/>
     * This constructor will automatically register all {@link SchemaSource}
     * implementations that can be found in the classpath using Java {@link
     * ServiceLoader} mechanism.
     */
    public SchemaBuilder()
        {
        List<SchemaSource> schemaSources = new ArrayList<>();

        // load available schema sources from the classpath
        ServiceLoader<SchemaSource> loader =
                ServiceLoader.load(SchemaSource.class, getClass().getClassLoader());
        for (SchemaSource source : loader)
            {
            schemaSources.add(source);
            }

        m_schemaSources = schemaSources;
        }

    // ---- fluent API ------------------------------------------------------

    /**
     * Add {@link SchemaSource} to this {@code SchemaBuilder}.
     *
     * @param schemaSource  the {@link SchemaSource} to add
     *
     * @return this {@code SchemaBuilder}
     */
    public SchemaBuilder addSchemaSource(SchemaSource schemaSource)
        {
        m_schemaSources.add(schemaSource);
        return this;
        }

    /**
     * Set the {@link ExtensibleType} store that the {@link Schema} created by
     * this {@code SchemaBuilder} should use.
     *
     * @param store  the {@link ExtensibleType} store that the {@link Schema}
     *               created by this {@code SchemaBuilder} should use
     *
     * @return this {@code SchemaBuilder}
     */
    public SchemaBuilder setStore(Map<String, ExtensibleType> store)
        {
        m_store = store;
        return this;
        }

    /**
     * Create {@link Schema} instance
     *
     * @return the created {@code Schema} instance
     */
    public Schema build()
        {
        Schema schema = new Schema(m_store);
        for (SchemaSource ts : m_schemaSources)
            {
            ts.populateSchema(schema);
            }

        return schema;
        }

    // ---- data members ----------------------------------------------------

    /**
     * The list of schema sources to use.
     */
    private List<SchemaSource> m_schemaSources;

    /**
     * The {@link ExtensibleType} store that the {@link Schema} created by this
     * {@code SchemaBuilder} should use.
     */
    private Map<String, ExtensibleType> m_store;
    }
