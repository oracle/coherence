/*
 * Copyright (c) 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package coherence.pof.core;

import com.tangosol.io.pof.schema.annotation.PortableType;

@PortableType(id=1000)
public class FooClass {
	private String name;

	public FooClass(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}
}
