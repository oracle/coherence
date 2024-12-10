/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package override;

import com.tangosol.config.ConfigurationException;
import com.tangosol.config.xml.AbstractNamespaceHandler;
import com.tangosol.config.xml.ElementProcessor;
import com.tangosol.config.xml.ProcessingContext;
import com.tangosol.config.xml.XmlSimpleName;
import com.tangosol.run.xml.XmlElement;

/**
 * Class to handler custom test namespace handling.
 *
 */
public class TestNamespaceHandler
        extends AbstractNamespaceHandler
    {
    /**
     * Constructs a {@link TestNamespaceHandler}.
     */
    public TestNamespaceHandler()
        {
        registerProcessor(TestElementProcessor.class);
        }

    @XmlSimpleName("test")
    public static class TestElementProcessor<T>
             implements ElementProcessor<T>
        {

        @Override
        public T process(ProcessingContext context, XmlElement xmlElement)
                throws ConfigurationException
            {
            return null;
            }
        }
    }
