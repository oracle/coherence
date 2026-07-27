/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package data;

import com.tangosol.run.xml.SimpleElement;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlSerializable;

/**
 * Test {@link XmlSerializable} implementation.
 *
 * @author Aleks Seovic  2026.04.29
 * @since 26.04
 */
public class TestXmlSerializable
        implements XmlSerializable
    {
    @Override
    public XmlElement toXml()
        {
        return new SimpleElement("test");
        }

    @Override
    public void fromXml(XmlElement xml)
        {
        }
    }
