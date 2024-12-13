/*
 * Copyright (c) 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

/**
 * A JPA implementation of the Coherence CacheLoader and CacheStore interface.
 * Use these classes as a full load and store implementation.
 * It can use any JPA implementation to load and store entities to and from a data store.
 * <p>
 * Note: The persistence unit is assumed to be set to use RESOURCE_LOCAL transactions.
 *
 * @author jf  2024.12.04
 * @since 25.03
 */
package com.oracle.coherence.jpa;