/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.config.xml;

import com.tangosol.config.ConfigurationException;
import com.tangosol.config.xml.AbstractNamespaceHandler;
import com.tangosol.config.xml.AttributeProcessor;
import com.tangosol.config.xml.ElementProcessor;
import com.tangosol.config.xml.ProcessingContext;

import com.tangosol.run.xml.XmlAttribute;
import com.tangosol.run.xml.XmlElement;

/**
 * The {@link NamespaceHandlerWithInnerClassXmlProcessors} is a test class used for testing namespaces where the
 * processors are defined in the form of inner classes.
 *
 * @author dr
 */
public class NamespaceHandlerWithInnerClassXmlProcessors
        extends AbstractNamespaceHandler
    {
    /**
     * An unnamed inner interface definition of an {@link AttributeProcessor}.
     */
    public interface IAnonymousAttributeProcessor<T>
            extends AttributeProcessor<T>
        {
        }

    /**
     * An unnamed inner interface definition of an {@link ElementProcessor}.
     */
    public interface IAnonymousElementProcessor<T>
            extends ElementProcessor<T>
        {
        }

    /**
     * A named inner interface definition of an {@link AttributeProcessor}.
     */
    @XmlSimpleName("ap")
    public interface INamedAttributeProcessor<T>
            extends AttributeProcessor<T>
        {
        }

    /**
     * A named inner interface definition of an {@link ElementProcessor}.
     */
    @XmlSimpleName("ep")
    public interface INamedElementProcessor<T>
            extends ElementProcessor<T>
        {
        }

    /**
     * A static inner class that is neither an {@link ElementProcessor} or {@link AttributeProcessor}.
     */
    public static class Bar
        {
        /**
         * standard method.
         */
        public void printHello()
            {
            System.out.printf("bar");
            }
        }

    /**
     * A non-static inner class that is neither an {@link ElementProcessor} or {@link AttributeProcessor}.
     */
    public class Foo
        {
        /**
         * standard method.
         */
        public void printHello()
            {
            System.out.printf("Foo");
            }
        }

    /**
     * An unnamed non-static inner class definition of an {@link AttributeProcessor}.
     */
    public class NonStaticAnonymousAttributeProcessor<X>
            implements AttributeProcessor<X>
        {
        /**
         * {@inheritDoc}
         */
        public X process(ProcessingContext context, XmlAttribute xmlAttribute)
                throws ConfigurationException
            {
            return null;
            }
        }

    /**
     * An unamed non-static inner class definition of an {@link ElementProcessor}.
     */
    public class NonStaticAnonymousElementProcessor<X>
            implements ElementProcessor<X>
        {
        /**
         * {@inheritDoc}
         */
        @Override
        public X process(ProcessingContext context, XmlElement xmlElement)
                throws ConfigurationException
            {
            return null;
            }
        }

    /**
     * A named non-static inner class definition of an {@link AttributeProcessor}.
     */
    @XmlSimpleName("non-static-named-ap")
    public class NonStaticNamedAttributeProcessor<X>
            implements AttributeProcessor<X>
        {
        /**
         * {@inheritDoc}
         */
        public X process(ProcessingContext context, XmlAttribute xmlAttribute)
                throws ConfigurationException
            {
            return null;
            }
        }

    /**
     * A named non-static inner class definition of an {@link ElementProcessor}.
     */
    @XmlSimpleName("non-static-named-ep")
    public class NonStaticNamedElementProcessor<X>
            implements ElementProcessor<X>
        {
        /**
         * {@inheritDoc}
         */
        public X process(ProcessingContext context, XmlElement xmlElement)
                throws ConfigurationException
            {
            return null;
            }
        }

    /**
     * An unnamed static inner class definition of an {@link AttributeProcessor}.
     */
    public static class StaticAnonymousAttributeHandler<X>
            implements AttributeProcessor<X>
        {
        /**
         * {@inheritDoc}
         */
        @Override
        public X process(ProcessingContext context, XmlAttribute xmlAttribute)
                throws ConfigurationException
            {
            return null;
            }
        }

    /**
     * An anonymous static inner class definition of an {@link ElementProcessor}.
     */
    public static class StaticAnonymousElementHandler<X>
            implements ElementProcessor<X>
        {
        /**
         * {@inheritDoc}
         */
        @Override
        public X process(ProcessingContext context, XmlElement xmlElement)
                throws ConfigurationException
            {
            return null;
            }
        }

    /**
     * A named static inner class definition of an {@link AttributeProcessor}.
     */
    @XmlSimpleName("static-named-ap")
    public class StaticNamedAttributeProcessor<X>
            implements AttributeProcessor<X>
        {
        /**
         * {@inheritDoc}
         */
        @Override
        public X process(ProcessingContext context, XmlAttribute xmlAttribute)
                throws ConfigurationException
            {
            return null;
            }
        }

    /**
     * A named static inner class definition of both an {@link AttributeProcessor} and {@link ElementProcessor}.
     */
    @XmlSimpleName("static-named-eap")
    public static class StaticNamedContentProcessor<X>
            implements AttributeProcessor<X>, ElementProcessor<X>
        {
        /**
         * {@inheritDoc}
         */
        @Override
        public X process(ProcessingContext context, XmlAttribute xmlAttribute)
                throws ConfigurationException
            {
            return null;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public X process(ProcessingContext context, XmlElement xmlElement)
                throws ConfigurationException
            {
            return null;
            }
        }

    /**
     * A named static inner class definition of an {@link ElementProcessor}
     */
    @XmlSimpleName("static-named-ep")
    public static class StaticNamedElementProcessor<X>
            implements ElementProcessor<X>
        {
        /**
         * {@inheritDoc}
         */
        @Override
        public X process(ProcessingContext context, XmlElement xmlElement)
                throws ConfigurationException
            {
            return null;
            }
        }
    }
