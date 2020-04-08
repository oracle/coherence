/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.common.base;

/**
 * A Cloner that clones {@link Cloneable} objects.
 *
 * @author gg/mf 2012.07.12
 * @deprecated use {@link com.oracle.coherence.common.base.NaturalCloner} instead
 */
@Deprecated
public class NaturalCloner
        extends com.oracle.coherence.common.base.NaturalCloner
        implements Cloner
    {
    }
