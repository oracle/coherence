/*
 * Copyright (c) 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package coherence.pof.app;

import coherence.pof.core.FooClass;

import com.tangosol.io.pof.schema.annotation.PortableMap;
import com.tangosol.io.pof.schema.annotation.PortableType;

import java.util.HashMap;
@PortableType(id=1000)
public class PofClass {
	private String name;
	public PofClass(String name) {
		this.name = name;
	}
	public String getName() {
		return name;
	}

	@PortableMap(since=1)
	private HashMap<Long, FooClass> fooMap = new HashMap<>();
}
