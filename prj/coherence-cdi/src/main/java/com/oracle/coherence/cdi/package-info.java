/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

/**
 * Coherence CDI provides support for CDI (Contexts and Dependency Injection)
 * within Coherence cluster members.
 * <p/>
 * It allows you both to inject Coherence-managed resources, such as
 * `NamedCache` and `Session` instances into CDI managed beans, and to inject
 * CDI beans into Coherence-managed resources, such as event interceptors and
 * cache stores.
 * <p/>
 * In addition, Coherence CDI provides support for automatic injection of
 * transient objects upon deserialization. This allows you to inject CDI managed
 * beans such as services and repositories (to use DDD nomenclature) into
 * transient objects, such as entry processor and data class instances, greatly
 * simplifying implementation of true Domain Driven applications.
 *
 * @author Aleks Seovic  2019.10.09
 * @since 20.06
 */
package com.oracle.coherence.cdi;
