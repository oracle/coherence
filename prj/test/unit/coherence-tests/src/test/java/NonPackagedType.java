/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

import com.tangosol.io.pof.schema.annotation.Portable;
import com.tangosol.io.pof.schema.annotation.PortableType;

/**
 * A test class used to fix a bug in {@link com.tangosol.io.pof.generator.PortableTypeGenerator}
 * when processing a class that is not in a package.
 *
 * @author Jonathan Knight  2020.07.10
 */
@PortableType(id = 2000)
public class NonPackagedType
    {
    @Portable
    private String name;
    }
