/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io.pof.generator.data;

import com.tangosol.io.pof.annotation.Portable;
import com.tangosol.io.pof.annotation.PortableProperty;

import java.util.Collection;

/**
 * Nucleus is a class annotated with {@link Portable} for use by
 * {@link com.tangosol.io.pof.generator.PofConfigGeneratorTest}.
 *
 * @author hr  2012.07.05
 *
 * @since Coherence 12.1.2
 */
@Portable
public class Nucleus
    {
    @PortableProperty(0)
    public Collection<Proton>  m_colProtons;
    @PortableProperty(1)
    public Collection<Neutron> m_colNeutrons;
    }
