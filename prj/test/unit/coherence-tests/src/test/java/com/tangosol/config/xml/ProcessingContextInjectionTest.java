/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.config.xml;

import com.oracle.coherence.common.util.Duration.Magnitude;
import com.oracle.coherence.common.util.MemorySize;

import com.tangosol.coherence.config.ParameterMacroExpressionParser;
import com.tangosol.coherence.config.unit.Millis;
import com.tangosol.coherence.config.unit.Seconds;

import com.tangosol.config.expression.Expression;
import com.tangosol.config.expression.NullParameterResolver;
import com.tangosol.config.expression.ParameterResolver;
import com.tangosol.config.expression.Value;
import com.tangosol.config.xml.DocumentProcessor.DefaultDependencies;
import com.tangosol.config.xml.SimpleBean.Switch;

import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;

import com.tangosol.util.ResourceRegistry;
import com.tangosol.util.SimpleResourceRegistry;

import junit.framework.Assert;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;
import java.util.TreeSet;

/**
 * Unit tests for testing injection with the {@link ProcessingContext}.
 *
 * @author bo
 * @author dr
 */
public class ProcessingContextInjectionTest
    {
    /**
     * Ensure that we can configure a Boolean property taken from an attribute.
     *
     * @throws Exception  when injection fails
     */
    @Test
    public void testBooleanPropertyFromAttribute()
            throws Exception
        {
        String            sXml = "<element boolean-property=\"true\" mandatory-property=\"funky\"/>";
        XmlElement        xml  = XmlHelper.loadXml(sXml).getRoot();
        ProcessingContext ctx  = new DefaultProcessingContext(xml);
        SimpleBean        bean = ctx.inject(new SimpleBean(), xml);

        assertTrue(bean.isBooleanProperty());
        }

    /**
     * Ensure that we can configure a Boolean property taken from an element.
     *
     * @throws Exception  when injection fails
     */
    @Test
    public void testBooleanPropertyFromElement()
            throws Exception
        {
        String sXml = "<element mandatory-property=\"funky\"><boolean-property>true</boolean-property></element>";
        XmlElement        xml  = XmlHelper.loadXml(sXml).getRoot();
        ProcessingContext ctx  = new DefaultProcessingContext(xml);
        SimpleBean        bean = ctx.inject(new SimpleBean(), xml);

        assertTrue(bean.isBooleanProperty());
        }

    /**
     * Ensure that we can configure a String property taken from an attribute.
     *
     * @throws Exception  when injection fails
     */
    @Test
    public void testStringPropertyFromAttribute()
            throws Exception
        {
        String            sXml = "<element string-property=\"hello world\" mandatory-property=\"funky\"/>";
        XmlElement        xml  = XmlHelper.loadXml(sXml).getRoot();
        ProcessingContext ctx  = new DefaultProcessingContext(xml);
        SimpleBean        bean = ctx.inject(new SimpleBean(), xml);

        assertEquals(bean.getStringProperty(), "hello world");
        }

    /**
     * Ensure that we can configure a String property taken from an element.
     *
     * @throws Exception  when injection fails
     */
    @Test
    public void testStringPropertyFromElement()
            throws Exception
        {
        String sXml = "<element mandatory-property=\"funky\"><string-property>hello world</string-property></element>";
        XmlElement        xml  = XmlHelper.loadXml(sXml).getRoot();
        ProcessingContext ctx  = new DefaultProcessingContext(xml);
        SimpleBean        bean = ctx.inject(new SimpleBean(), xml);

        assertEquals(bean.getStringProperty(), "hello world");
        }

    /**
     * Ensure that we can configure a String property taken from an attribute.
     *
     * @throws Exception  when injection fails
     */
    @Test
    public void testEnumPropertyFromAttribute()
            throws Exception
        {
        String            sXml = "<element mandatory-property=\"funky\" enum-property=\"On\"/>";
        XmlElement        xml  = XmlHelper.loadXml(sXml).getRoot();
        ProcessingContext ctx  = new DefaultProcessingContext(xml);
        SimpleBean        bean = ctx.inject(new SimpleBean(), xml);

        assertTrue(bean.getEnumProperty() == SimpleBean.Switch.On);
        }

    /**
     * Ensure that we can configure a String property taken from an element.
     *
     * @throws Exception  when injection fails
     */
    @Test
    public void testEnumPropertyFromElement()
            throws Exception
        {
        String            sXml = "<element mandatory-property=\"funky\"><enum-property>Off</enum-property></element>";
        XmlElement        xml  = XmlHelper.loadXml(sXml).getRoot();
        ProcessingContext ctx  = new DefaultProcessingContext(xml);
        SimpleBean        bean = ctx.inject(new SimpleBean(), xml);

        assertTrue(bean.getEnumProperty() == SimpleBean.Switch.Off);
        }

    /**
     * Ensure that we can configure a StringBuffer property taken from an attribute.
     *
     * @throws Exception  when injection fails
     */
    @Test
    public void testExplicitTypeBasedProcessorPropertyFromAttribute()
            throws Exception
        {
        String            sXml = "<element string-buffer=\"funky\"/>";
        XmlElement        xml  = XmlHelper.loadXml(sXml).getRoot();
        ProcessingContext ctx  = new DefaultProcessingContext(xml);

        ctx.registerProcessor(StringBuffer.class, new SimpleAttributeProcessor<StringBuffer>(StringBuffer.class));

        SimpleBean bean = ctx.inject(new SimpleBean(), xml);

        assertTrue(bean.getStringBufferProperty().toString().equals("funky"));
        }

    /**
     * Ensure that we can configure a StringBuffer property taken from an attribute.
     *
     * @throws Exception  when injection fails
     */
    @Test
    public void testAutomaticTypeBasedProcessorPropertyFromAttribute()
            throws Exception
        {
        String            sXml = "<element string-buffer=\"double funky\"/>";
        XmlElement        xml  = XmlHelper.loadXml(sXml).getRoot();
        ProcessingContext ctx  = new DefaultProcessingContext(xml);

        ctx.registerAttributeType(StringBuffer.class);

        SimpleBean bean = ctx.inject(new SimpleBean(), xml);

        assertTrue(bean.getStringBufferProperty().toString().equals("double funky"));
        }

    /**
     * Ensure that we can alias a property.
     *
     * @throws Exception  when injection fails
     */
    @Test
    public void testPropertyPathDefinitionWithElement()
            throws Exception
        {
        String            sXml = "<element mandatory-property=\"funky\"><alias>On</alias></element>";
        XmlElement        xml  = XmlHelper.loadXml(sXml).getRoot();
        ProcessingContext ctx  = new DefaultProcessingContext(xml);

        ctx.definePropertyPath("enum-property", "alias");

        SimpleBean bean = ctx.inject(new SimpleBean(), xml);

        assertTrue(bean.getEnumProperty() == SimpleBean.Switch.On);
        }

    /**
     * Ensure that a bean property without an @Inject annotation is not injected.
     *
     * @throws Exception  when injection fails
     */
    @Test
    public void testNotInjectedPropertyFromElement()
            throws Exception
        {
        String            sXml = "<element><not-injected-property>double funky</not-injected-property></element>";
        XmlElement        xml  = XmlHelper.loadXml(sXml).getRoot();
        ProcessingContext ctx  = new DefaultProcessingContext(xml);
        SimpleBean        bean = ctx.inject(new SimpleBean(), xml);

        assertTrue(bean.getNotInjectedProperty().equals("notinjected"));
        }

    /**
     * Ensure that a bean property with no property value is not injected
     *
     * @throws Exception  when injection fails
     */
    @Test
    public void testNoInjectionWithEmptyDeclaration()
            throws Exception
        {
        String            sXml = "<element></element>";
        XmlElement        xml  = XmlHelper.loadXml(sXml).getRoot();
        ProcessingContext ctx  = new DefaultProcessingContext(xml);
        SimpleBean        bean = new SimpleBean();

        bean.replaceStringProperty("gudday");
        ctx.inject(bean, xml);
        assertTrue(bean.getStringProperty().equals("gudday"));
        }

    /**
     * Ensure that a bean property that is not defined in the xml may be injected from a cookie.
     *
     * @throws Exception  when injection fails
     */
    @Test
    public void testPropertyInjectedFromProcessingContextCookie()
            throws Exception
        {
        String            sXml = "<element><mandatory-property>funky</mandatory-property></element>";
        XmlElement        xml  = XmlHelper.loadXml(sXml).getRoot();
        ProcessingContext ctx  = new DefaultProcessingContext(xml);

        ctx.addCookie(Switch.class, "enum-property", Switch.On);

        SimpleBean bean = ctx.inject(new SimpleBean(), xml);

        assertTrue(bean.getEnumProperty() == SimpleBean.Switch.On);
        }

    /**
     * Ensure that a bean property that is not defined in the xml may be injected from a {@link ResourceRegistry}.
     *
     * @throws Exception  when injection fails
     */
    @Test
    public void testPropertyInjectedFromResourceRegistry()
            throws Exception
        {
        String           sXml     = "<element><mandatory-property>funky</mandatory-property></element>";
        XmlElement       xml      = XmlHelper.loadXml(sXml).getRoot();

        ResourceRegistry registry = new SimpleResourceRegistry();

        registry.registerResource(Switch.class, "enum-property", Switch.On);

        DefaultDependencies dep  = new DocumentProcessor.DefaultDependencies().setResourceRegistry(registry);

        ProcessingContext   ctx  = new DefaultProcessingContext(dep, xml);
        SimpleBean          bean = ctx.inject(new SimpleBean(), xml);

        assertTrue(bean.getEnumProperty() == SimpleBean.Switch.On);
        }

    /**
     * Ensure that we can inject an empty List of elements
     *
     * @throws Exception  when injection fails
     */
    @Test
    public void testEmptyListInjectionPropertyFromElements()
            throws Exception
        {
        String                 sXml      = "<element><switch-list></switch-list></element>";

        SimpleNamespaceHandler nsHandler = new SimpleNamespaceHandler();

        nsHandler.registerProcessor("switch", new SimpleElementProcessor<Switch>(Switch.class));

        XmlElement               xml = XmlHelper.loadXml(sXml).getRoot();
        DefaultProcessingContext ctx = new DefaultProcessingContext(xml);

        ctx.ensureNamespaceHandler("", nsHandler);

        SimpleBean       bean     = ctx.inject(new SimpleBean(), xml);

        Iterator<Switch> switches = bean.getSwitches();

        Assert.assertFalse(switches.hasNext());
        }

    /**
     * Ensure that we can inject a List of elements
     *
     * @throws Exception  when injection fails
     */
    @Test
    public void testListInjectionPropertyFromElements()
            throws Exception
        {
        String sXml = "<element><switch-list><switch>On</switch><switch>Off</switch></switch-list></element>";

        SimpleNamespaceHandler nsHandler = new SimpleNamespaceHandler();

        nsHandler.registerProcessor("switch", new SimpleElementProcessor<Switch>(Switch.class));

        XmlElement               xml = XmlHelper.loadXml(sXml).getRoot();
        DefaultProcessingContext ctx = new DefaultProcessingContext(xml);

        ctx.ensureNamespaceHandler("", nsHandler);

        SimpleBean       bean     = ctx.inject(new SimpleBean(), xml);

        Iterator<Switch> switches = bean.getSwitches();

        Assert.assertTrue(switches.hasNext());
        Assert.assertEquals(Switch.On, switches.next());
        Assert.assertTrue(switches.hasNext());
        Assert.assertEquals(Switch.Off, switches.next());
        Assert.assertFalse(switches.hasNext());
        }

    /**
     * Ensure that we can inject an empty array of elements
     *
     * @throws Exception  when injection fails
     */
    @Test
    public void testEmptyArrayInjectionPropertyFromElements()
            throws Exception
        {
        String                 sXml      = "<element><switch-array></switch-array></element>";

        SimpleNamespaceHandler nsHandler = new SimpleNamespaceHandler();

        nsHandler.registerProcessor("switch", new SimpleElementProcessor<Switch>(Switch.class));

        XmlElement               xml = XmlHelper.loadXml(sXml).getRoot();
        DefaultProcessingContext ctx = new DefaultProcessingContext(xml);

        ctx.ensureNamespaceHandler("", nsHandler);

        SimpleBean       bean     = ctx.inject(new SimpleBean(), xml);

        Iterator<Switch> switches = bean.getSwitches();

        Assert.assertFalse(switches.hasNext());
        }

    /**
     * Ensure that we can inject an array of elements
     *
     * @throws Exception  when injection fails
     */
    @Test
    public void testArrayInjectionPropertyFromElements()
            throws Exception
        {
        String sXml = "<element><switch-array><switch>On</switch><switch>Off</switch></switch-array></element>";

        SimpleNamespaceHandler nsHandler = new SimpleNamespaceHandler();

        nsHandler.registerProcessor("switch", new SimpleElementProcessor<Switch>(Switch.class));

        XmlElement               xml = XmlHelper.loadXml(sXml).getRoot();
        DefaultProcessingContext ctx = new DefaultProcessingContext(xml);

        ctx.ensureNamespaceHandler("", nsHandler);

        SimpleBean       bean     = ctx.inject(new SimpleBean(), xml);

        Iterator<Switch> switches = bean.getSwitches();

        Assert.assertTrue(switches.hasNext());
        Assert.assertEquals(Switch.On, switches.next());
        Assert.assertTrue(switches.hasNext());
        Assert.assertEquals(Switch.Off, switches.next());
        Assert.assertFalse(switches.hasNext());
        }

    /**
     * Ensure that we can inject a {@link TreeSet} of elements.
     *
     * @throws Exception  when injection fails
     */
    @Test
    public void testTreeSetInjectionPropertyFromElements()
            throws Exception
        {
        String sXml = "<element><switch-treeset>" + "<switch>Off</switch>" + "<switch>On</switch>"
                      + "<switch>Off</switch>" + "<switch>Off</switch>" + "<switch>On</switch>" + "<switch>On</switch>"
                      + "</switch-treeset></element>";

        SimpleNamespaceHandler nsHandler = new SimpleNamespaceHandler();

        nsHandler.registerProcessor("switch", new SimpleElementProcessor<Switch>(Switch.class));

        XmlElement               xml = XmlHelper.loadXml(sXml).getRoot();
        DefaultProcessingContext ctx = new DefaultProcessingContext(xml);

        ctx.ensureNamespaceHandler("", nsHandler);

        SimpleBean       bean     = ctx.inject(new SimpleBean(), xml);

        Iterator<Switch> switches = bean.getSwitches();

        Assert.assertTrue(switches.hasNext());
        Assert.assertEquals(Switch.On, switches.next());
        Assert.assertTrue(switches.hasNext());
        Assert.assertEquals(Switch.Off, switches.next());
        Assert.assertFalse(switches.hasNext());
        }

    /**
     * Ensure that we can inject a MemorySize {@link Expression}.
     *
     * @throws Exception  when injection fails
     */
    @Test
    public void testMemorySizeExpressionPropertyFromElement()
            throws Exception
        {
        String                 sXml      = "<element><memory-size>1kb</memory-size></element>";

        SimpleNamespaceHandler nsHandler = new SimpleNamespaceHandler();

        DocumentProcessor.Dependencies dependencies =
            new DocumentProcessor.DefaultDependencies(nsHandler)
                .setExpressionParser(new ParameterMacroExpressionParser());

        XmlElement               xml            = XmlHelper.loadXml(sXml).getRoot();
        DefaultProcessingContext ctx            = new DefaultProcessingContext(dependencies, xml);

        SimpleBean               bean           = ctx.inject(new SimpleBean(), xml);

        Expression<MemorySize>   exprMemorySize = bean.getMemorySize();
        ParameterResolver        resolver       = new NullParameterResolver();

        Assert.assertNotNull(exprMemorySize);
        Assert.assertEquals(1024, exprMemorySize.evaluate(resolver).getByteCount());
        }

    /**
     * Ensure that we can inject a Value {@link Expression}.
     *
     * @throws Exception  when injection fails
     */
    @Test
    public void testValueExpressionPropertyFromElement()
            throws Exception
        {
        String                 sXml      = "<element><value>On</value></element>";

        SimpleNamespaceHandler nsHandler = new SimpleNamespaceHandler();

        DocumentProcessor.Dependencies dependencies =
            new DocumentProcessor.DefaultDependencies(nsHandler)
                .setExpressionParser(new ParameterMacroExpressionParser());

        XmlElement               xml       = XmlHelper.loadXml(sXml).getRoot();
        DefaultProcessingContext ctx       = new DefaultProcessingContext(dependencies, xml);

        SimpleBean               bean      = ctx.inject(new SimpleBean(), xml);

        Expression<Value>        exprValue = bean.getValue();
        ParameterResolver        resolver  = new NullParameterResolver();

        Assert.assertNotNull(exprValue);
        Assert.assertEquals("On", exprValue.evaluate(resolver).get());
        Assert.assertEquals(Switch.On, exprValue.evaluate(resolver).as(Switch.class));
        }

    /**
     * Ensure that we can inject {@link Seconds} using the default {@link Seconds} magnitude.
     *
     * @throws Exception  when injection fails
     */
    @Test
    public void testDefaultSecondsPropertyFromElement()
            throws Exception
        {
        String                 sXml      = "<element><expiry-time>5</expiry-time></element>";

        SimpleNamespaceHandler nsHandler = new SimpleNamespaceHandler();

        DocumentProcessor.Dependencies dependencies =
            new DocumentProcessor.DefaultDependencies(nsHandler)
                .setExpressionParser(new ParameterMacroExpressionParser());

        XmlElement               xml     = XmlHelper.loadXml(sXml).getRoot();
        DefaultProcessingContext ctx     = new DefaultProcessingContext(dependencies, xml);

        SimpleBean               bean    = ctx.inject(new SimpleBean(), xml);

        Seconds                  seconds = bean.getExpiryTime();

        Assert.assertNotNull(seconds);
        Assert.assertEquals("5s", seconds.toString());
        Assert.assertEquals(5, seconds.get());
        }

    /**
     * Ensure that we can inject {@link Seconds} using some other magnitude.
     *
     * @throws Exception  when injection fails
     */
    @Test
    public void testSpecificSecondsPropertyFromElement()
            throws Exception
        {
        String                 sXml      = "<element><expiry-time>1m</expiry-time></element>";

        SimpleNamespaceHandler nsHandler = new SimpleNamespaceHandler();

        DocumentProcessor.Dependencies dependencies =
            new DocumentProcessor.DefaultDependencies(nsHandler)
                .setExpressionParser(new ParameterMacroExpressionParser());

        XmlElement               xml     = XmlHelper.loadXml(sXml).getRoot();
        DefaultProcessingContext ctx     = new DefaultProcessingContext(dependencies, xml);

        SimpleBean               bean    = ctx.inject(new SimpleBean(), xml);

        Seconds                  seconds = bean.getExpiryTime();

        Assert.assertNotNull(seconds);
        Assert.assertEquals("1m", seconds.toString());
        Assert.assertEquals(60, seconds.get());
        }

    /**
     * Ensure that we can inject a {@link Millis} {@link Expression}.
     *
     * @throws Exception  when injection fails
     */
    @Test
    public void testDefaultMillisExpressionPropertyFromElement()
            throws Exception
        {
        String                 sXml      = "<element><session-timeout>100</session-timeout></element>";

        SimpleNamespaceHandler nsHandler = new SimpleNamespaceHandler();

        DocumentProcessor.Dependencies dependencies =
            new DocumentProcessor.DefaultDependencies(nsHandler)
                .setExpressionParser(new ParameterMacroExpressionParser());

        XmlElement               xml                = XmlHelper.loadXml(sXml).getRoot();
        DefaultProcessingContext ctx                = new DefaultProcessingContext(dependencies, xml);

        SimpleBean               bean               = ctx.inject(new SimpleBean(), xml);

        Expression<Millis>       exprSessionTimeout = bean.getSessionTimeout();
        ParameterResolver        resolver           = new NullParameterResolver();

        Assert.assertNotNull(exprSessionTimeout);
        Assert.assertEquals("100ms", exprSessionTimeout.evaluate(resolver).toString());
        Assert.assertEquals(100, exprSessionTimeout.evaluate(resolver).as(Magnitude.MILLI));
        }

    /**
     * Ensure that we can inject using getter injection (from the same element)
     *
     * @throws Exception  when injection fails
     */
    @Test
    public void testGetterInjectionInSameElement()
            throws Exception
        {
        String                 sXml      = "<element><name>Mr Happy</name></element>";

        SimpleNamespaceHandler nsHandler = new SimpleNamespaceHandler();

        DocumentProcessor.Dependencies dependencies =
            new DocumentProcessor.DefaultDependencies(nsHandler)
                .setExpressionParser(new ParameterMacroExpressionParser());

        XmlElement               xml  = XmlHelper.loadXml(sXml).getRoot();
        DefaultProcessingContext ctx  = new DefaultProcessingContext(dependencies, xml);

        SimpleBean               bean = ctx.inject(new SimpleBean(), xml);

        Assert.assertNotNull(bean);
        Assert.assertNotNull(bean.getOtherBean());
        Assert.assertEquals("Mr Happy", bean.getOtherBean().getName());
        }

    /**
     * Ensure that we can inject using getter injection (from a nested element)
     *
     * @throws Exception  when injection fails
     */
    @Test
    public void testGetterInjectionInNestedElement()
            throws Exception
        {
        String                 sXml      = "<element><nested><name>Mr Happy</name></nested></element>";

        SimpleNamespaceHandler nsHandler = new SimpleNamespaceHandler();

        DocumentProcessor.Dependencies dependencies =
            new DocumentProcessor.DefaultDependencies(nsHandler)
                .setExpressionParser(new ParameterMacroExpressionParser());

        XmlElement               xml  = XmlHelper.loadXml(sXml).getRoot();
        DefaultProcessingContext ctx  = new DefaultProcessingContext(dependencies, xml);

        SimpleBean               bean = ctx.inject(new SimpleBean(), xml);

        Assert.assertNotNull(bean);
        Assert.assertNotNull(bean.getOtherBean());
        Assert.assertNotNull(bean.getNestedOtherBean());
        Assert.assertEquals("unknown", bean.getOtherBean().getName());
        Assert.assertEquals("Mr Happy", bean.getNestedOtherBean().getName());
        }
    }
