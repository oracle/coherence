/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.dev.introspect;

import org.junit.Test;

import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;

/**
 * ClassAnnotationSeekerTest tests the functionality within
 * ClassAnnotationSeeker.
 *
 * @author hr  2012.07.30
 *
 * @since Coherence 12.1.2
 */
public class ClassAnnotationSeekerTest
    {
    @Test
    public void testFindClassNames()
        {
        ClassAnnotationSeeker.Dependencies simpleDeps = new ClassAnnotationSeeker.Dependencies()
            .setDiscriminator("com/tangosol/dev/introspect")
            .setScanner(new ClassAnnotationScanner());
        ClassAnnotationSeeker simpleSeeker  = new ClassAnnotationSeeker(simpleDeps);
        Set<String> setClassNames = simpleSeeker.findClassNames(TestAnnotation.class);

        assertThat(setClassNames, hasSize(greaterThan(0)));
        assertThat(setClassNames.iterator().next(), instanceOf(String.class));
        }
    }
