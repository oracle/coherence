/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io.pof.generator;

import com.tangosol.io.pof.schema.annotation.Portable;
import com.tangosol.io.pof.schema.annotation.PortableType;

/**
 * A test class used to fix a bug in {@link com.tangosol.io.pof.generator.PortableTypeGenerator}
 * when processing a classes that are and are not in a package.
 *
 * @author Jonathan Knight  2020.07.10
 */
@PortableType(id = 3000)
public class PackagedType
    {
    @Portable
    private String name;
    }
