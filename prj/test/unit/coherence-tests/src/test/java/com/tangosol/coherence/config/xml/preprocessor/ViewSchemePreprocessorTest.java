/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.preprocessor;

import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;

import com.tangosol.util.extractor.IdentityExtractor;

import com.tangosol.util.filter.AlwaysFilter;
import com.tangosol.util.filter.NeverFilter;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import static org.junit.Assert.assertThat;

/**
 * Unit tests for {@link ViewSchemePreprocessor}.
 *
 * @author rlubke
 * @since 12.2.1.4
 */
public class ViewSchemePreprocessorTest
    {
    // ---- test methods ----------------------------------------------------

    /**
     * Validate the standard execution path.
     */
    @Test
    public void shouldProduceValidPreprocessResult()
        {
        String sXml = "<view-scheme>\n"
                    + "  <scheme-name>view</scheme-name>\n"
                    + "  <view-filter>\n"
                    + "    <class-scheme>\n"
                    + "      <class-name>com.tangosol.util.filter.NeverFilter</class-name>\n"
                    + "    </class-scheme>\n"
                    + "  </view-filter>\n"
                    + "  <back-scheme>\n"
                    + "    <distributed-scheme>\n"
                    + "      <scheme-ref>distributed</scheme-ref>\n"
                    + "    </distributed-scheme>\n"
                    + "  </back-scheme>\n"
                    + "  <transformer>\n"
                    + "    <class-scheme>\n"
                    + "      <class-name>com.tangosol.util.extractor.IdentityExtractor</class-name>\n"
                    + "    </class-scheme>\n"
                    + "  </transformer>\n"
                    + "  <read-only>true</read-only>"
                    + "  <reconnect-interval>1000</reconnect-interval>"
                    + "  <listener>\n"
                    + "    <class-scheme>\n"
                    + "      <class-name>some.Listener</class-name>\n"
                    + "    </class-scheme>\n"
                    + "  </listener>\n"
                    + "</view-scheme>";

        XmlElement xml         = XmlHelper.loadXml(sXml);
        XmlElement xmlOriginal = (XmlElement) xml.clone();

        // If the preprocessor made changes as expected, the result of the call should be true
        assertThat(ViewSchemePreprocessor.INSTANCE.preprocess(null, xml), is(true));
        assertThat(xmlOriginal, is(not(xml)));

        // view-filter, transformer, read-only, reconnect-interval, listener
        // should no longer be children of view-scheme
        assertThat(xml.getElement("view-filter"), nullValue());
        assertThat(xml.getElement("transformer"), nullValue());
        assertThat(xml.getElement("read-only"), nullValue());
        assertThat(xml.getElement("reconnect-interval"), nullValue());
        assertThat(xml.getElement("listener"), nullValue());

        // front-scheme is added as a child to view-scheme
        XmlElement xmlFrontScheme = xml.getElement("front-scheme");
        assertThat(xmlFrontScheme, notNullValue());

        // new front-scheme element has a child called continuous-query-cache-scheme
        XmlElement xmlCqcScheme = xmlFrontScheme.getElement("continuous-query-cache-scheme");
        assertThat(xmlCqcScheme, is(notNullValue()));

        // read-only should now be a child of xmlCqcScheme
        XmlElement xmlReadOnly = xmlCqcScheme.getElement("read-only");
        assertThat(xmlReadOnly, is(notNullValue()));
        assertThat(xmlReadOnly.getBoolean(), is(true));

        // reconnect-interval should now be a child of continuous-query-cache-scheme
        XmlElement xmlReconnectInterval = xmlCqcScheme.getElement("reconnect-interval");
        assertThat(xmlReconnectInterval, is(notNullValue()));
        assertThat(xmlReconnectInterval.getLong(), is(1000L));

        // the transformer element should be a child of continuous-query-cache-scheme
        XmlElement xmlTransformer = xmlCqcScheme.getElement("transformer");
        assertThat(xmlTransformer, is(notNullValue()));

        // the listener element should be a child of continuous-query-cache-scheme
        XmlElement xmlListener = xmlCqcScheme.getElement("listener");
        assertThat(xmlListener, is(notNullValue()));

        // transformer element should have a class-scheme child
        XmlElement xmlClassScheme = xmlTransformer.getElement("class-scheme");
        assertThat(xmlClassScheme, is(notNullValue()));

        // class-scheme should have a class-name element with Identity as the class
        XmlElement xmlClassName = xmlClassScheme.getElement("class-name");
        assertThat(xmlClassName, is(notNullValue()));
        String sClassName = xmlClassName.getString();
        assertThat(sClassName, is(notNullValue()));
        assertThat(IdentityExtractor.class.getName(), is(sClassName.trim()));

        // the view-filter element should be a child of cqc-scheme
        XmlElement viewFilter = xmlCqcScheme.getElement("view-filter");
        assertThat(viewFilter, is(notNullValue()));

        // view-filter element should have a class-scheme child
        xmlClassScheme = viewFilter.getElement("class-scheme");
        assertThat(xmlClassScheme, is(notNullValue()));

        // class-scheme should have a class-name element with NeverFilter as the class
        xmlClassName = xmlClassScheme.getElement("class-name");
        assertThat(xmlClassName, is(notNullValue()));
        sClassName = xmlClassName.getString();
        assertThat(sClassName, is(notNullValue()));
        assertThat(NeverFilter.class.getName(), is(sClassName.trim()));
        }

    /**
     * Validate defaults are properly injected if {@code view-filter} isn't defined.
     */
    @Test
    public void shouldHaveDefaultsInserted()
        {
        String sXml = "<view-scheme>\n"
                    + "  <scheme-name>view</scheme-name>\n"
                    + "  <back-scheme>\n"
                    + "    <distributed-scheme>\n"
                    + "      <scheme-ref>distributed</scheme-ref>\n"
                    + "    </distributed-scheme>\n"
                    + "  </back-scheme>\n"
                    + "</view-scheme>";

        XmlElement xml         = XmlHelper.loadXml(sXml);
        XmlElement xmlOriginal = (XmlElement) xml.clone();

        // If the preprocessor made changes as expected, the result of the call should be true
        assertThat(ViewSchemePreprocessor.INSTANCE.preprocess(null, xml), is(true));
        assertThat(xmlOriginal, is(not(xml)));

        // view-filter should no longer be a child of view-scheme
        assertThat(xml.getElement("view-filter"), nullValue());

        // front-scheme is added as a child to view-scheme
        XmlElement xmlFrontScheme = xml.getElement("front-scheme");
        assertThat(xmlFrontScheme, notNullValue());

        // new front-scheme element has a child called continuous-query-cache-scheme
        XmlElement xmlCqcScheme = xmlFrontScheme.getElement("continuous-query-cache-scheme");
        assertThat(xmlCqcScheme, is(notNullValue()));

        // the view-filter element should be a child of continuous-query-cache-scheme
        XmlElement viewFilter = xmlCqcScheme.getElement("view-filter");
        assertThat(viewFilter, is(notNullValue()));

        // view-filter element should have a class-scheme child
        XmlElement xmlClassScheme = viewFilter.getElement("class-scheme");
        assertThat(xmlClassScheme, is(notNullValue()));

        // class-scheme should have a class-name element with AlwaysFilter as the class
        XmlElement xmlClassName = xmlClassScheme.getElement("class-name");
        assertThat(xmlClassName, is(notNullValue()));
        String sClassName = xmlClassName.getString();
        assertThat(sClassName, is(notNullValue()));
        assertThat(AlwaysFilter.class.getName(), is(sClassName.trim()));
        }

    /**
     * Validate the standard execution path.
     */
    @Test
    public void shouldHaveDefaultsInsertedAndNecessaryElementsAreMoved()
        {
        String sXml = "<view-scheme>\n"
                      + "  <scheme-name>replicated</scheme-name>\n"
                      + "  <back-scheme>\n"
                      + "    <distributed-scheme>\n"
                      + "      <scheme-ref>distributed</scheme-ref>\n"
                      + "    </distributed-scheme>\n"
                      + "  </back-scheme>\n"
                      + "  <read-only>true</read-only>"
                      + "  <reconnect-interval>1000</reconnect-interval>"
                      + "  <transformer>\n"
                      + "    <class-scheme>\n"
                      + "      <class-name>com.tangosol.util.extractor.IdentityExtractor</class-name>\n"
                      + "    </class-scheme>\n"
                      + "  </transformer>\n"
                      + "</view-scheme>";

        XmlElement xml         = XmlHelper.loadXml(sXml);
        XmlElement xmlOriginal = (XmlElement) xml.clone();

        // If the preprocessor made changes as expected, the result of the call should be true
        assertThat(ViewSchemePreprocessor.INSTANCE.preprocess(null, xml), is(true));
        assertThat(xmlOriginal, is(not(xml)));

        // view-filter, transformer, read-only, reconnect-interval should no longer be children of view-scheme
        assertThat(xml.getElement("view-filter"), nullValue());
        assertThat(xml.getElement("transformer"), nullValue());
        assertThat(xml.getElement("read-only"), nullValue());
        assertThat(xml.getElement("reconnect-interval"), nullValue());

        // front-scheme is added as a child to view-scheme
        XmlElement xmlFrontScheme = xml.getElement("front-scheme");
        assertThat(xmlFrontScheme, notNullValue());

        // new front-scheme element has a child called continuous-query-cache-scheme
        XmlElement xmlCqcScheme = xmlFrontScheme.getElement("continuous-query-cache-scheme");
        assertThat(xmlCqcScheme, is(notNullValue()));

        // read-only should now be a child of continuous-query-cache-scheme
        XmlElement xmlReadOnly = xmlCqcScheme.getElement("read-only");
        assertThat(xmlReadOnly, is(notNullValue()));
        assertThat(xmlReadOnly.getBoolean(), is(true));

        // reconnect-interval should now be a child of continuous-query-cache-scheme
        XmlElement xmlReconnectInterval = xmlCqcScheme.getElement("reconnect-interval");
        assertThat(xmlReconnectInterval, is(notNullValue()));
        assertThat(xmlReconnectInterval.getLong(), is(1000L));

        // the transformer element should be a child of continuous-query-cache-scheme
        XmlElement xmlTransformer = xmlCqcScheme.getElement("transformer");
        assertThat(xmlTransformer, is(notNullValue()));

        // transformer element should have a class-scheme child
        XmlElement xmlClassScheme = xmlTransformer.getElement("class-scheme");
        assertThat(xmlClassScheme, is(notNullValue()));

        // class-scheme should have a class-name element with Identity as the class
        XmlElement xmlClassName = xmlClassScheme.getElement("class-name");
        assertThat(xmlClassName, is(notNullValue()));
        String sClassName = xmlClassName.getString();
        assertThat(sClassName, is(notNullValue()));
        assertThat(IdentityExtractor.class.getName(), is(sClassName.trim()));

        // the view-filter element should be a child of continuous-query-cache-scheme
        XmlElement viewFilter = xmlCqcScheme.getElement("view-filter");
        assertThat(viewFilter, is(notNullValue()));

        // view-filter element should have a class-scheme child
        xmlClassScheme = viewFilter.getElement("class-scheme");
        assertThat(xmlClassScheme, is(notNullValue()));

        // class-scheme should have a class-name element with AlwaysFilter as the class
        xmlClassName = xmlClassScheme.getElement("class-name");
        assertThat(xmlClassName, is(notNullValue()));
        sClassName = xmlClassName.getString();
        assertThat(sClassName, is(notNullValue()));
        assertThat(AlwaysFilter.class.getName(), is(sClassName.trim()));

        // the distributed-scheme element should have been moved as a child of the back-scheme element
        // which should have been added as a child to view-scheme
        XmlElement xmlBackScheme = xml.getElement("back-scheme");
        assertThat(xml.getElement("distributed-scheme"), nullValue());
        assertThat(xmlBackScheme, notNullValue());
        assertThat(xmlBackScheme.getElement("distributed-scheme"), notNullValue());
        }

    /**
     * Validate {@link ViewSchemePreprocessor} returns {@code false} if no changes have been made.
     */
    @Test
    public void shouldReturnFalseIfNoChangesMade()
        {
        String sXml = "<view-scheme>\n"
                    + "  <scheme-name>replicated</scheme-name>\n"
                    + "  <back-scheme>\n"
                    + "    <distributed-scheme>\n"
                    + "      <scheme-ref>distributed</scheme-ref>\n"
                    + "    </distributed-scheme>\n"
                    + "  </back-scheme>\n"
                    + "  <front-scheme>\n"
                    + "    <continuous-query-cache-scheme>\n"
                    + "      <view-filter>\n"
                    + "        <class-scheme>\n"
                    + "          <class-name>com.tangosol.util.filter.AlwaysFilter</class-name>\n"
                    + "        </class-scheme>\n"
                    + "      </view-filter>\n"
                    + "    </continuous-query-cache-scheme>\n"
                    + "  </front-scheme>"
                    + "</view-scheme>";

        XmlElement xml      = XmlHelper.loadXml(sXml);
        XmlElement xmlClone = (XmlElement) xml.clone();

        // xml should be unmodified and FilterViewFactoryPreprocessor should return false
        assertThat(ViewSchemePreprocessor.INSTANCE.preprocess(null, xml), is(false));
        assertThat(xmlClone, is(xml));
        }
    }
