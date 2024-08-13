/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package hnswlib;

import com.oracle.coherence.hnswlib.ConcurrentIndex;
import com.oracle.coherence.hnswlib.Index;
import com.oracle.coherence.hnswlib.SpaceName;
import org.junit.jupiter.api.Disabled;

@Disabled
public class ConcurrentIndexPerformanceTest extends AbstractPerformanceTest
    {
	@Override
	protected Index createIndexInstance(SpaceName spaceName, int dimensions) {
		return new ConcurrentIndex(spaceName, dimensions);
	}
}
