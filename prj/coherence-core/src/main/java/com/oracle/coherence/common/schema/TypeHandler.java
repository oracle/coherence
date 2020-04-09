/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.common.schema;

/**
 * An interface that all type handlers must implement.
 *
 * @param <TInternal>  the internal representation of type metadata
 * @param <TExternal>  the external representation of type metadata
 *
 * @author as  2013.06.26
 */
public interface TypeHandler<TInternal extends Type, TExternal>
    {
    /**
     * Return the class of the internal type representation this type handler
     * understands.
     *
     * @return the class of the internal type representation this type handler
     *         understands
     */
    Class<TInternal> getInternalTypeClass();

    /**
     * Return the class of the external type representation this type handler
     * understands.
     *
     * @return the class of the external type representation this type handler
     *         understands
     */
    Class<TExternal> getExternalTypeClass();

    /**
     * Create the internal type representation and associate it with the
     * specified extensible type.
     *
     * @param parent  the extensible type created type should be associated with
     *
     * @return the internal type representation
     */
    TInternal createType(ExtensibleType parent);

    /**
     * Import specified type into the schema.
     *
     * @param type    the internal type to populate and import
     * @param source  the external source to read type metadata from
     * @param schema  the schema to import type into
     */
    void importType(TInternal type, TExternal source, Schema schema);

    /**
     * Export specified type from the schema.
     *
     * @param type    the internal type to export
     * @param target  the external target to populate based on the internal
     *                type metadata
     * @param schema  the schema to export type from
     */
    void exportType(TInternal type, TExternal target, Schema schema);
    }
