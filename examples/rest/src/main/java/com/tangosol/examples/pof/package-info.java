/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

/**
 * Required because of lack of support for java.time.* in JAXB
 * in Java 8.
 *
 * @author tam  2015.07.10
 * @since 12.2.1
 */

@XmlJavaTypeAdapters({@XmlJavaTypeAdapter(type = LocalDate.class, value = LocalDateXmlAdapter.class)})

package com.tangosol.examples.pof;

import java.time.LocalDate;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapters;