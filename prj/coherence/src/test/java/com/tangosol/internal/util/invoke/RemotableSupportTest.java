/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util.invoke;

import com.tangosol.internal.util.stream.DoublePipeline;
import com.tangosol.internal.util.stream.IntPipeline;

import com.tangosol.io.pof.ConfigurablePofContext;

import com.tangosol.util.ExternalizableHelper;

import com.tangosol.util.function.Remote;
import com.tangosol.util.function.Remote.Predicate;

import org.junit.Test;

import java.io.Serializable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * A test suite for {@link RemotableSupport} (dynamic lambdas).
 *
 * @author hr  2015.06.01
 */
public class RemotableSupportTest
    {
    @Test
    public void testLambdaWithNoCapturedArgs()
            throws Throwable
        {
        Remote.Predicate<String> predicate = Remote.predicate((s) -> s.startsWith("A"));

        Remote.Predicate<String> p = teleport(predicate);

        assertTrue(p.test("Aleks"));
        assertFalse(p.test("Marija"));
        }

    @Test
    public void testLambdaWithCapturedLocalVar()
            throws Throwable
        {
        int nMin = 30;
        NumberPredicate<Integer> predicate = s -> s.intValue() > nMin;

        NumberPredicate<Integer> p = teleport(predicate);

        assertTrue(p.test(Integer.valueOf(33)));
        assertFalse(p.test(Integer.valueOf(27)));
        }

    @Test
    public void testLambdaWithCapturedLocalVars()
            throws Throwable
        {
        int cMin = 100;
        int cMax = 200;
        Remote.Predicate<Integer> between = Remote.predicate((n) -> n > cMin && n < cMax);

        Remote.Predicate<Integer> p = teleport(between);

        assertTrue(p.test(150));
        assertFalse(p.test(0));
        }

    @Test
    public void testMethodReference()
        {
        Remote.Function<Thing, String> f  = Thing::getY;
        Remote.Function<Thing, String> f2 = teleport(f);

        Thing thing = new Thing();

        assertEquals("Thingy", f.apply(thing));
        assertEquals("Thingy", f2.apply(thing));

        Remote.Supplier<String> s  = Thing::getYStaticly;
        Remote.Supplier<String> s2 = teleport(s);

        assertEquals("Thingyz", s.get());
        assertEquals("Thingyz", s2.get());

        Remote.Supplier<Thing> s3 = Thing::new;
        Remote.Supplier<Thing> s4 = teleport(s3);

        assertSame(Thing.class, s3.get().getClass());
        assertSame(Thing.class, s4.get().getClass());

        Remote.Function<String, Thing> s5 = Thing::new;
        Remote.Function<String, Thing> s6 = teleport(s5);

        assertSame(Thing.class, s5.apply("").getClass());
        assertSame(Thing.class, s6.apply("").getClass());

        Remote.BinaryOperator<Long> s7 = Long::sum;
        Remote.BinaryOperator<Long> s8 = teleport(s7);

        assertEquals(Long.valueOf(2L), s7.apply(1L, 1L));
        assertEquals(Long.valueOf(2L), s8.apply(1L, 1L));
        }

    @Test
    public void testOptional()
        {

        ConfigurablePofContext ctx = new ConfigurablePofContext("coherence-pof-config.xml");

        IntPipeline.Optional s9  = new IntPipeline.Optional((Remote.IntBinaryOperator) Math::max);
        IntPipeline.Optional s10 = (IntPipeline.Optional)
                ExternalizableHelper.fromBinary(ExternalizableHelper.toBinary(s9, ctx), ctx);

        s10.accept(10);
        assertEquals(10, s10.getValue());

        DoublePipeline.Optional s11 = new DoublePipeline.Optional((Remote.DoubleBinaryOperator) Math::max);
        DoublePipeline.Optional s12 = (DoublePipeline.Optional)
                ExternalizableHelper.fromBinary(ExternalizableHelper.toBinary(s11, ctx), ctx);

        s12.accept(10.2d);
        assertEquals(10.2d, s12.getValue(), 0d);
        }

    // ----- inner class: Thing ---------------------------------------------

    public static class Thing
        {
        public Thing() {}

        public Thing(String s) {}

        public String getY() { return "Thingy"; }

        public static String getYStaticly() { return "Thingyz"; }
        }

    public static interface NumberPredicate<T extends Number>
            extends Predicate<T>, Serializable
        {
        }

    // ----- helpers --------------------------------------------------------

    protected <F extends Serializable> F teleport(F function)
        {
        return f_support.realize(f_support.createRemoteConstructor(function));
        }

    protected RemotableSupport f_support = new RemotableSupport(RemotableSupport.class.getClassLoader());
    }
