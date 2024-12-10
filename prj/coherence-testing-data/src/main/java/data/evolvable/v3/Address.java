/*
 * Copyright (c) 2012, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package data.evolvable.v3;

import com.tangosol.io.pof.schema.annotation.Portable;
import com.tangosol.io.pof.schema.annotation.PortableType;

@PortableType(id = 10)
public record Address(@Portable String street,
                      @Portable String city,
                      @Portable String state)
    {}
