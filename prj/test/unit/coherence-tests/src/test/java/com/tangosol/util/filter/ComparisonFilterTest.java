/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util.filter;


import org.junit.Test;
import static org.junit.Assert.*;


/**
 * @author as  2015.06.02
 */
public class ComparisonFilterTest
    {
    @Test
    public void testComparisonWithinTypeHierarchy()
        {
        Zoo zoo = new Zoo();

        LessEqualsFilter<Zoo, Animal> f1 = new LessEqualsFilter<>(Zoo::getDog, zoo.getCat());
        LessEqualsFilter<Zoo, Animal> f2 = new LessEqualsFilter<>(Zoo::getCat, zoo.getDog());

        assertFalse("Dog should not be smaller than cat", f1.evaluate(zoo));
        assertTrue ("Cat should be smaller than dog", f2.evaluate(zoo));
        }

    public static class Animal implements Comparable<Animal>
        {
        private int nWeight;

        public Animal(int nWeight)
            {
            this.nWeight = nWeight;
            }

        public int compareTo(Animal other)
            {
            return nWeight - other.nWeight;
            }
        }

    public static class Dog extends Animal
        {
        public Dog(int nWeight)
            {
            super(nWeight);
            }
        }

    public static class Cat extends Animal
        {
        public Cat(int nWeight)
            {
            super(nWeight);
            }
        }

    public static class Zoo
        {
        private Dog dog = new Dog(50);
        private Cat cat = new Cat(10);

        public Dog getDog()
            {
            return dog;
            }

        public Cat getCat()
            {
            return cat;
            }
        }
    }
