/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
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
        NumberPredicate<Integer> predicate = s -> s > nMin;

        NumberPredicate<Integer> p = teleport(predicate);

        assertTrue(p.test(33));
        assertFalse(p.test(27));
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

    @Test
    public void testPredicateAndToBinary()
        {
        Remote.Predicate<Integer> p1 = o -> true;
        Remote.Predicate<Integer> p2 = o -> true;
        Remote.Predicate<Integer> p3 = p1.and(p2);

        ExternalizableHelper.toBinary(p3); // this should not throw
        }

    @Test
    public void testPredicateAndLogic()
        {
        Remote.Predicate<Integer> pTrue  = o -> true;
        Remote.Predicate<Integer> pFalse = o -> false;

        assertTrue(((Remote.Predicate<Integer>) (o -> true)).and(pTrue).test(1));
        assertTrue(pTrue.and((Remote.Predicate<Integer>) (o -> true)).test(1));

        assertFalse(((Remote.Predicate<Integer>) (o -> true)).and(pFalse).test(1));
        assertFalse(pFalse.and((Remote.Predicate<Integer>) (o -> true)).test(1));
        }

    @Test
    public void testPredicateNegateToBinary()
        {
        Remote.Predicate<Integer> p1 = o -> true;
        Remote.Predicate<Integer> p2 = p1.negate();

        ExternalizableHelper.toBinary(p2); // this should not throw
        }

    @Test
    public void testPredicateNegateLogic()
        {
        Remote.Predicate<Integer> pTrue  = o -> true;
        Remote.Predicate<Integer> pFalse = o -> false;
        Remote.Predicate<Integer> pTrueNeg = pTrue.negate();
        Remote.Predicate<Integer> pFalseNeg = pFalse.negate();

        assertFalse(pTrueNeg.test(1));
        assertTrue(pFalseNeg.test(1));
        }

    @Test
    public void testPredicateOrToBinary()
        {
        Remote.Predicate<Integer> p1 = o -> true;
        Remote.Predicate<Integer> p2 = o -> true;
        Remote.Predicate<Integer> p3 = p1.or(p2);

        ExternalizableHelper.toBinary(p3); // this should not throw
        }

    @Test
    public void testPredicateOrLogic()
        {
        Remote.Predicate<Integer> pTrue  = o -> true;
        Remote.Predicate<Integer> pFalse = o -> false;

        assertTrue(((Remote.Predicate<Integer>) (o -> true)).or(pTrue).test(1));
        assertTrue(pTrue.or((Remote.Predicate<Integer>) (o -> true)).test(1));

        assertTrue(((Remote.Predicate<Integer>) (o -> true)).or(pFalse).test(1));
        assertTrue(pFalse.or((Remote.Predicate<Integer>) (o -> true)).test(1));
        assertFalse(pFalse.or((Remote.Predicate<Integer>) (o -> false)).test(1));
        }

    @Test
    public void testPredicateIsEqualToBinary()
        {
        Remote.Predicate<Integer> pTrue = Remote.Predicate.isEqual(true);
        Remote.Predicate<Integer> pNull = Remote.Predicate.isEqual(null);

        ExternalizableHelper.toBinary(pTrue); // this should not throw
        ExternalizableHelper.toBinary(pNull); // this should not throw
        }

    @Test
    public void testPredicateIsEqualLogic()
        {
        Remote.Predicate<Boolean> pTrue = Remote.Predicate.isEqual(true);
        Remote.Predicate<Boolean> pNull = Remote.Predicate.isEqual(null);

        assertTrue(pTrue.test(true));
        assertFalse(pTrue.test(false));
        assertTrue(pNull.test(null));
        assertFalse(pNull.test(true));
        }

    @Test
    public void testBiPredicateAndToBinary()
        {
        Remote.BiPredicate<Integer, Integer> p1 = (o, o2) -> true;
        Remote.BiPredicate<Integer, Integer> p2 = (o, o2) -> true;
        Remote.BiPredicate<Integer, Integer> p3 = p1.and(p2);

        ExternalizableHelper.toBinary(p3); // this should not throw
        }

    @Test
    public void testBiPredicateAndLogic()
        {
        Remote.BiPredicate<Integer, Integer> pTrue  = (o, o2) -> true;
        Remote.BiPredicate<Integer, Integer> pFalse = (o, o2) -> false;

        assertTrue(((Remote.BiPredicate<Integer, Integer>) ((o, o2) -> true)).and(pTrue).test(1, 1));
        assertTrue(pTrue.and((Remote.BiPredicate<Integer, Integer>) ((o, o2) -> true)).test(1, 1));

        assertFalse(((Remote.BiPredicate<Integer, Integer>) ((o, o2) -> true)).and(pFalse).test(1, 1));
        assertFalse(pFalse.and((Remote.BiPredicate<Integer, Integer>) ((o, o2) -> true)).test(1, 1));
        }

    @Test
    public void testBiPredicateNegateToBinary()
        {
        Remote.BiPredicate<Integer, Integer> p1 = (o, o2) -> true;
        Remote.BiPredicate<Integer, Integer> p2 = p1.negate();

        ExternalizableHelper.toBinary(p2); // this should not throw
        }

    @Test
    public void testBiPredicateNegateLogic()
        {
        Remote.BiPredicate<Integer, Integer> pTrue  = (o, o2) -> true;
        Remote.BiPredicate<Integer, Integer> pFalse = (o, o2) -> false;
        Remote.BiPredicate<Integer, Integer> pTrueNeg = pTrue.negate();
        Remote.BiPredicate<Integer, Integer> pFalseNeg = pFalse.negate();

        assertFalse(pTrueNeg.test(1, 1));
        assertTrue(pFalseNeg.test(1, 1));
        }

    @Test
    public void testBiPredicateOrToBinary()
        {
        Remote.BiPredicate<Integer, Integer> p1 = (o, o2) -> true;
        Remote.BiPredicate<Integer, Integer> p2 = (o, o2) -> true;
        Remote.BiPredicate<Integer, Integer> p3 = p1.or(p2);

        ExternalizableHelper.toBinary(p3); // this should not throw
        }

    @Test
    public void testBiPredicateOrLogic()
        {
        Remote.BiPredicate<Integer, Integer> pTrue  = (o, o2) -> true;
        Remote.BiPredicate<Integer, Integer> pFalse = (o, o2) -> false;

        assertTrue(((Remote.BiPredicate<Integer, Integer>) ((o, o2) -> true)).or(pTrue).test(1, 1));
        assertTrue(pTrue.or((Remote.BiPredicate<Integer, Integer>) ((o, o2) -> true)).test(1, 1));

        assertTrue(((Remote.BiPredicate<Integer, Integer>) ((o, o2) -> true)).or(pFalse).test(1, 1));
        assertTrue(pFalse.or((Remote.BiPredicate<Integer, Integer>) ((o, o2) -> true)).test(1, 1));
        assertFalse(pFalse.or((Remote.BiPredicate<Integer, Integer>) ((o, o2) -> false)).test(1, 1));
        }

    @Test
    public void testDoublePredicateAndToBinary()
        {
        Remote.DoublePredicate p1 = o -> true;
        Remote.DoublePredicate p2 = o -> true;
        Remote.DoublePredicate p3 = p1.and(p2);

        ExternalizableHelper.toBinary(p3); // this should not throw
        }

    @Test
    public void testDoublePredicateAndLogic()
        {
        Remote.DoublePredicate pTrue  = o -> true;
        Remote.DoublePredicate pFalse = o -> false;

        assertTrue(((Remote.DoublePredicate) (o -> true)).and(pTrue).test(1.0d));
        assertTrue(pTrue.and((Remote.DoublePredicate) (o -> true)).test(1.0d));

        assertFalse(((Remote.DoublePredicate) (o -> true)).and(pFalse).test(1.0d));
        assertFalse(pFalse.and((Remote.DoublePredicate) (o -> true)).test(1.0d));
        }

    @Test
    public void testDoublePredicateNegateToBinary()
        {
        Remote.DoublePredicate p1 = o -> true;
        Remote.DoublePredicate p2 = p1.negate();

        ExternalizableHelper.toBinary(p2); // this should not throw
        }

    @Test
    public void testDoublePredicateNegateLogic()
        {
        Remote.DoublePredicate pTrue     = o -> true;
        Remote.DoublePredicate pFalse    = o -> false;
        Remote.DoublePredicate pTrueNeg  = pTrue.negate();
        Remote.DoublePredicate pFalseNeg = pFalse.negate();

        assertFalse(pTrueNeg.test(1.0d));
        assertTrue(pFalseNeg.test(1.0d));
        }

    @Test
    public void testDoublePredicateOrToBinary()
        {
        Remote.DoublePredicate p1 = o -> true;
        Remote.DoublePredicate p2 = o -> true;
        Remote.DoublePredicate p3 = p1.or(p2);

        ExternalizableHelper.toBinary(p3); // this should not throw
        }

    @Test
    public void testDoublePredicateOrLogic()
        {
        Remote.DoublePredicate pTrue  = o -> true;
        Remote.DoublePredicate pFalse = o -> false;

        assertTrue(((Remote.DoublePredicate) (o -> true)).or(pTrue).test(1.0d));
        assertTrue(pTrue.or((Remote.DoublePredicate) (o -> true)).test(1.0d));

        assertTrue(((Remote.DoublePredicate) (o -> true)).or(pFalse).test(1.0d));
        assertTrue(pFalse.or((Remote.DoublePredicate) (o -> true)).test(1.0d));
        assertFalse(pFalse.or((Remote.DoublePredicate) (o -> false)).test(1.0d));
        }

    @Test
    public void testIntPredicateAndToBinary()
        {
        Remote.IntPredicate p1 = o -> true;
        Remote.IntPredicate p2 = o -> true;
        Remote.IntPredicate p3 = p1.and(p2);

        ExternalizableHelper.toBinary(p3); // this should not throw
        }

    @Test
    public void testIntPredicateAndLogic()
        {
        Remote.IntPredicate pTrue  = o -> true;
        Remote.IntPredicate pFalse = o -> false;

        assertTrue(((Remote.IntPredicate) (o -> true)).and(pTrue).test(1));
        assertTrue(pTrue.and((Remote.IntPredicate) (o -> true)).test(1));

        assertFalse(((Remote.IntPredicate) (o -> true)).and(pFalse).test(1));
        assertFalse(pFalse.and((Remote.IntPredicate) (o -> true)).test(1));
        }

    @Test
    public void testIntPredicateNegateToBinary()
        {
        Remote.IntPredicate p1 = o -> true;
        Remote.IntPredicate p2 = p1.negate();

        ExternalizableHelper.toBinary(p2); // this should not throw
        }

    @Test
    public void testIntPredicateNegateLogic()
        {
        Remote.IntPredicate pTrue     = o -> true;
        Remote.IntPredicate pFalse    = o -> false;
        Remote.IntPredicate pTrueNeg  = pTrue.negate();
        Remote.IntPredicate pFalseNeg = pFalse.negate();

        assertFalse(pTrueNeg.test(1));
        assertTrue(pFalseNeg.test(1));
        }

    @Test
    public void testIntPredicateOrToBinary()
        {
        Remote.IntPredicate p1 = o -> true;
        Remote.IntPredicate p2 = o -> true;
        Remote.IntPredicate p3 = p1.or(p2);

        ExternalizableHelper.toBinary(p3); // this should not throw
        }

    @Test
    public void testIntPredicateOrLogic()
        {
        Remote.IntPredicate pTrue  = o -> true;
        Remote.IntPredicate pFalse = o -> false;

        assertTrue(((Remote.IntPredicate) (o -> true)).or(pTrue).test(1));
        assertTrue(pTrue.or((Remote.IntPredicate) (o -> true)).test(1));

        assertTrue(((Remote.IntPredicate) (o -> true)).or(pFalse).test(1));
        assertTrue(pFalse.or((Remote.IntPredicate) (o -> true)).test(1));
        assertFalse(pFalse.or((Remote.IntPredicate) (o -> false)).test(1));
        }

    @Test
    public void testLongPredicateAndToBinary()
        {
        Remote.LongPredicate p1 = o -> true;
        Remote.LongPredicate p2 = o -> true;
        Remote.LongPredicate p3 = p1.and(p2);

        ExternalizableHelper.toBinary(p3); // this should not throw
        }

    @Test
    public void testLongPredicateAndLogic()
        {
        Remote.LongPredicate pTrue  = o -> true;
        Remote.LongPredicate pFalse = o -> false;

        assertTrue(((Remote.LongPredicate) (o -> true)).and(pTrue).test(1L));
        assertTrue(pTrue.and((Remote.LongPredicate) (o -> true)).test(1L));

        assertFalse(((Remote.LongPredicate) (o -> true)).and(pFalse).test(1L));
        assertFalse(pFalse.and((Remote.LongPredicate) (o -> true)).test(1L));
        }

    @Test
    public void testLongPredicateNegateToBinary()
        {
        Remote.LongPredicate p1 = o -> true;
        Remote.LongPredicate p2 = p1.negate();

        ExternalizableHelper.toBinary(p2); // this should not throw
        }

    @Test
    public void testLongPredicateNegateLogic()
        {
        Remote.LongPredicate pTrue     = o -> true;
        Remote.LongPredicate pFalse    = o -> false;
        Remote.LongPredicate pTrueNeg  = pTrue.negate();
        Remote.LongPredicate pFalseNeg = pFalse.negate();

        assertFalse(pTrueNeg.test(1L));
        assertTrue(pFalseNeg.test(1L));
        }

    @Test
    public void testLongPredicateOrToBinary()
        {
        Remote.LongPredicate p1 = o -> true;
        Remote.LongPredicate p2 = o -> true;
        Remote.LongPredicate p3 = p1.or(p2);

        ExternalizableHelper.toBinary(p3); // this should not throw
        }

    @Test
    public void testLongPredicateOrLogic()
        {
        Remote.LongPredicate pTrue  = o -> true;
        Remote.LongPredicate pFalse = o -> false;

        assertTrue(((Remote.LongPredicate) (o -> true)).or(pTrue).test(1L));
        assertTrue(pTrue.or((Remote.LongPredicate) (o -> true)).test(1L));

        assertTrue(((Remote.LongPredicate) (o -> true)).or(pFalse).test(1L));
        assertTrue(pFalse.or((Remote.LongPredicate) (o -> true)).test(1L));
        assertFalse(pFalse.or((Remote.LongPredicate) (o -> false)).test(1L));
        }

    @Test
    public void testDoubleUnaryOperatorComposeToBinary()
        {
        Remote.DoubleUnaryOperator p1 = o -> 1.0d;
        Remote.DoubleUnaryOperator p2 = o -> 1.0d;

        ExternalizableHelper.toBinary(p1.compose(p2)); // this should not throw
        }

    @Test
    public void testDoubleUnaryOperatorAndThenToBinary()
        {
        Remote.DoubleUnaryOperator p1 = o -> 1.0d;
        Remote.DoubleUnaryOperator p2 = o -> 1.0d;

        ExternalizableHelper.toBinary(p1.andThen(p2)); // this should not throw
        }

    @Test
    public void testIntUnaryOperatorComposeToBinary()
        {
        Remote.IntUnaryOperator p1 = o -> 1;
        Remote.IntUnaryOperator p2 = o -> 1;

        ExternalizableHelper.toBinary(p1.compose(p2)); // this should not throw
        }

    @Test
    public void testIntUnaryOperatorAndThenToBinary()
        {
        Remote.IntUnaryOperator p1 = o -> 1;
        Remote.IntUnaryOperator p2 = o -> 1;

        ExternalizableHelper.toBinary(p1.andThen(p2)); // this should not throw
        }

    @Test
    public void testLongUnaryOperatorComposeToBinary()
        {
        Remote.LongUnaryOperator p1 = o -> 1L;
        Remote.LongUnaryOperator p2 = o -> 1L;

        ExternalizableHelper.toBinary(p1.compose(p2)); // this should not throw
        }

    @Test
    public void testLongUnaryOperatorAndThenToBinary()
        {
        Remote.LongUnaryOperator p1 = o -> 1L;
        Remote.LongUnaryOperator p2 = o -> 1L;

        ExternalizableHelper.toBinary(p1.andThen(p2)); // this should not throw
        }

    @Test
    public void testComparatorThenComparingComparatorToBinary()
        {
        Remote.Comparator<Integer> p1 = (o1, o2) -> 0;
        Remote.Comparator<Integer> p2 = (o1, o2) -> 0;

        ExternalizableHelper.toBinary(p1.thenComparing(p2)); // this should not throw
        }

    @Test
    public void testComparatorThenComparingFunctionAndComparatorToBinary()
        {
        Remote.Comparator<Integer> p1 = (o1, o2) -> 0;
        Remote.Comparator<Integer> p2 = (o1, o2) -> 0;

        ExternalizableHelper.toBinary(p1.thenComparing(integer -> 1, p2)); // this should not throw
        }

    @Test
    public void testComparatorThenComparingFunctionToBinary()
        {
        Remote.Comparator<Integer> p1 = (o1, o2) -> 0;

        ExternalizableHelper.toBinary(p1.thenComparing(integer -> 1)); // this should not throw
        }

    @Test
    public void testComparatorThenComparingToIntFunctionToBinary()
        {
        Remote.Comparator<Integer> p1 = (o1, o2) -> 0;

        ExternalizableHelper.toBinary(p1.thenComparingInt(integer -> 1)); // this should not throw
        }

    @Test
    public void testComparatorThenComparingToLongFunctionToBinary()
        {
        Remote.Comparator<Long> p1 = (o1, o2) -> 0;

        ExternalizableHelper.toBinary(p1.thenComparingLong(o -> 1)); // this should not throw
        }

    @Test
    public void testComparatorThenComparingToDoubleFunctionToBinary()
        {
        Remote.Comparator<Double> p1 = (o1, o2) -> 0;

        ExternalizableHelper.toBinary(p1.thenComparingDouble(o -> 1)); // this should not throw
        }

    @Test
    public void testComparatorComparingFunctionAndComparatorToBinary()
        {
        Remote.Comparator<Integer> p1 = Remote.Comparator.comparing(integer -> 1, (o1, o2) -> 1);

        ExternalizableHelper.toBinary(p1); // this should not throw
        }

    @Test
    public void testComparatorComparingFunctionToBinary()
        {
        Remote.Comparator<Integer> p1 = Remote.Comparator.comparing(integer -> 1);

        ExternalizableHelper.toBinary(p1); // this should not throw
        }

    @Test
    public void testComparatorComparingIntToBinary()
        {
        Remote.Comparator<Integer> p1 = Remote.Comparator.comparingInt(integer -> 1);

        ExternalizableHelper.toBinary(p1); // this should not throw
        }

    @Test
    public void testComparatorComparingLongToBinary()
        {
        Remote.Comparator<Long> p1 = Remote.Comparator.comparingLong(integer -> 1L);

        ExternalizableHelper.toBinary(p1); // this should not throw
        }

    @Test
    public void testComparatorComparingDoubleToBinary()
        {
        Remote.Comparator<Double> p1 = Remote.Comparator.comparingDouble(integer -> 1.0d);

        ExternalizableHelper.toBinary(p1); // this should not throw
        }

    @Test
    public void testConsumerToBinary()
        {
        Remote.Consumer<Integer> p1 = integer -> {};
        Remote.Consumer<Integer> p2 = integer -> {};

        ExternalizableHelper.toBinary(p1.andThen(p2)); // this should not throw
        }

    @Test
    public void testBiConsumerToBinary()
        {
        Remote.BiConsumer<Integer, Integer> p1 = (i, i2) -> {};
        Remote.BiConsumer<Integer, Integer> p2 = (i, i2) -> {};

        ExternalizableHelper.toBinary(p1.andThen(p2)); // this should not throw
        }

    @Test
    public void testDoubleConsumerToBinary()
        {
        Remote.DoubleConsumer p1 = value -> {};
        Remote.DoubleConsumer p2 = value -> {};

        ExternalizableHelper.toBinary(p1.andThen(p2)); // this should not throw
        }

    @Test
    public void testIntConsumerToBinary()
        {
        Remote.IntConsumer p1 = value -> {};
        Remote.IntConsumer p2 = value -> {};

        ExternalizableHelper.toBinary(p1.andThen(p2)); // this should not throw
        }

    @Test
    public void testLongConsumerToBinary()
        {
        Remote.LongConsumer p1 = value -> {};
        Remote.LongConsumer p2 = value -> {};

        ExternalizableHelper.toBinary(p1.andThen(p2)); // this should not throw
        }

   @Test
   public void testFunctionComposeToBinary()
       {
       Remote.Function<Integer, Integer> p1 = integer -> 1;
       Remote.Function<Integer, Integer> p2 = integer -> 1;

       ExternalizableHelper.toBinary(p1.andThen(p2)); // this should not throw
       }

    @Test
    public void testFunctionAndThenToBinary()
        {
        Remote.Function<Integer, Integer> p1 = integer -> 1;
        Remote.Function<Integer, Integer> p2 = integer -> 1;

        ExternalizableHelper.toBinary(p1.andThen(p2)); // this should not throw
        }

    @Test
    public void testBiFunctionAndThenToBinary()
        {
        Remote.BiFunction<Integer, Integer, Integer> p1 = (o, o1) -> 1;

        ExternalizableHelper.toBinary(p1.andThen(integer -> 1)); // this should not throw
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
