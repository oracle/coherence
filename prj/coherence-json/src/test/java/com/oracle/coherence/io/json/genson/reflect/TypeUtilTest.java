/*
 * Copyright (c) 2020, Oracle and/or its affiliates.
 *
 * Copyright 2011-2014 Genson - Cepoi Eugen
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License.
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.oracle.coherence.io.json.genson.reflect;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import com.oracle.coherence.io.json.genson.GenericType;

import static com.oracle.coherence.io.json.genson.reflect.TypeUtil.*;

import static org.junit.Assert.*;

public class TypeUtilTest {

  @SuppressWarnings("serial")
  public static class ParameterizedSuperType extends HashMap<Object, String> {

  }

  @Test
  public void testTypeOf() {
    try {
      typeOf(1, ParameterizedSuperType.class);
      fail();
    } catch (UnsupportedOperationException uoe) {
    }

    assertEquals(String.class, typeOf(1, expandType(lookupGenericType(Map.class, ParameterizedSuperType.class), ParameterizedSuperType.class)));
  }

  @Test
  public void testCyclicGenericTypes() throws SecurityException, NoSuchFieldException {
    // bug 2 https://groups.google.com/forum/?fromgroups=#!topic/genson/9rE026i7Vhg
    assertNotNull(TypeUtil.expandType(Interval.class.getDeclaredField("min").getGenericType(), Interval.class));
  }

  class Interval<T extends Comparable<T>> {
    T min;
  }

  @Test
  public void testGetCollectionTypeParameterizedCollections() {
    Type setOfArrayListOfIntegerType = new GenericType<Set<ArrayList<Integer>>>() {
    }.getType();
    Type arrayListOfIntegerType = new GenericType<ArrayList<Integer>>() {
    }.getType();
    assertEquals(arrayListOfIntegerType, getCollectionType(setOfArrayListOfIntegerType));
    assertEquals(Integer.class, getCollectionType(getCollectionType(setOfArrayListOfIntegerType)));
  }

  @Test
  public void testGetCollectionTypeArray() {
    assertEquals(int.class, getCollectionType(int[].class));
    assertEquals(Integer.class, getCollectionType(Integer[].class));
    assertEquals(Object.class, getCollectionType(Object[].class));
  }

  @Test
  public <E> void testGenericDeclarationAsMethod() {
    assertEquals(Object.class, getRawClass(new GenericType<E>() {
    }.getType()));
  }

  @Test
  public void testGetRawClass() {
    assertEquals(ParametrizedClass.class, getRawClass(ParametrizedClass.class));
    assertEquals(Object.class,
      getRawClass(((ParameterizedType) new GenericType<List<?>>() {
      }.getType()).getActualTypeArguments()[0]));
    assertEquals(Integer.class,
      getRawClass(((ParameterizedType) new GenericType<List<Integer>>() {
      }.getType()).getActualTypeArguments()[0]));

    Type typeVar = ((ParameterizedType) tListF).getActualTypeArguments()[0];
    assertEquals(Object.class, getRawClass(typeVar));

    typeVar = ((ParameterizedType) tListE).getActualTypeArguments()[0];
    assertEquals(Number.class, getRawClass(typeVar));
  }

  @Test
  public void testGetCollectionType() throws SecurityException, NoSuchFieldException {
    assertEquals(Object.class, getCollectionType(Collection.class));
    assertEquals(Number.class, getCollectionType(tListN));
    assertEquals(Number.class, expandType(getCollectionType(tListE), ParametrizedClass.class));
    assertEquals(Object.class, expandType(getCollectionType(tListF), ParametrizedClass.class));
    assertEquals(Object.class, expandType(getCollectionType(tListI), ParametrizedClass.class));
    assertEquals(Number.class, expandType(getCollectionType(tListIEN), ParametrizedClass.class));
    ParameterizedType colType = (ParameterizedType) expandType(getCollectionType(tListCN), ParametrizedClass.class);
    assertEquals(Collection.class, colType.getRawType());
  }

  @Test
  public void testParameterizedTypeResolution() throws SecurityException, NoSuchFieldException {
    Field g = O.class.getDeclaredField("g");
    Type gt = g.getGenericType();
    Type expGt = expandType(gt, O.class);
    Type t = Generic.class.getDeclaredField("t").getGenericType();
    Type expT = expandType(t, expGt);
    assertEquals(URL.class, expT);
  }

  private class Generic<T> {
    @SuppressWarnings("unused")
    T t;
  }

  private class O {
    @SuppressWarnings("unused")
    Generic<URL> g;
  }

  @SuppressWarnings("rawtypes")
  @Test
  public void testGenericType() {
    assertTrue(match(new GenericType<List<Number>>() {
    }.getType(), tListE, true));
    try {
      new GenericType() {
      };
      fail();
    } catch (RuntimeException re) {
    }
  }

  @Test
  public void testParameterOf() throws SecurityException, NoSuchFieldException {
    try {
      assertNull(typeOf(0, Number.class));
      fail();
    } catch (UnsupportedOperationException uoe) {
    }

    assertEquals(Number.class, expand(typeOf(0, tListN), ParametrizedClass.class));

    // doit retourner Number, car dans la declaration de la classe ParametrizedClass, E extends
    // Number
    assertEquals(Number.class, expand(typeOf(0, tListE), ParametrizedClass.class));
    // doit retourner Object, car dans la declaration de la classe ParametrizedClass, il n'y a
    // aucune borne pour F
    assertEquals(Object.class, expand(typeOf(0, tListF), ParametrizedClass.class));
    // doit retourner Object, car c'est un wildcard sans borne
    assertEquals(Object.class, expand(typeOf(0, tListI), ParametrizedClass.class));
    // doit retourner Number, car la borne sup du wildcard est Number
    assertEquals(Number.class, expand(typeOf(0, tListIEN), ParametrizedClass.class));

		/*
     * doit retourner Object puisque ? super X, correspond a toutes les superclasses de X dont
		 * Object par contre dans la methode match il faut faire un peu plus de choses car ce n'est
		 * pas le cas!
		 */
    assertEquals(Object.class, expand(typeOf(0, tListISI), ParametrizedClass.class));

		/*
		 * doit retourner Collection<?> equivalent a Collection<Object> car la borne sup du type C
		 * est Collection<?> dans la definition de la classe
		 */
    Type wildcardCollectionType = expand(typeOf(0, tListC), ParametrizedClass.class);
    assertEquals(Collection.class, getRawClass(wildcardCollectionType));
    assertEquals(Object.class, expand(typeOf(0, wildcardCollectionType), ParametrizedClass.class));

    wildcardCollectionType = expand(typeOf(0, tListCN), ParametrizedClass.class);
    assertEquals(Collection.class, getRawClass(wildcardCollectionType));
    assertEquals(Number.class, expand(typeOf(0, wildcardCollectionType), ParametrizedClass.class));

    // equivalent a <?> <Object> et rien
    assertEquals(Object.class, expand(typeOf(0, List.class), ParametrizedClass.class));
  }

  @Test
  public void testTypeMatch() throws SecurityException, NoSuchFieldException {
    assertTrue(match(Integer.class, Number.class, false));
    assertTrue(match(Number.class, Number.class, false));
    assertFalse(match(Number.class, Integer.class, false));
    assertTrue(match(Number.class, Number.class, true));
    assertFalse(match(Double.class, Number.class, true));

    assertTrue(match(tListEInt, tListE, false));
    assertFalse(match(tListEInt, tListE, true));

    assertTrue(match(tListEInt, tColEInt, false));
    assertFalse(match(tListEInt, tColEInt, true));

    assertTrue(match(new Number[0].getClass(), tArrayE, true));
    assertFalse(match(new Integer[0].getClass(), tArrayE, true));
    assertTrue(match(new Integer[0].getClass(), tArrayF, false));
    assertFalse(match(new Integer[0].getClass(), tArrayF, true));
    assertFalse(match(tArrayC, tArrayCN, false));
    assertTrue(match(tArrayCN, tArrayC, false));
  }

  @Test
  public void testLookupWithGenerics() {
    assertNotNull(lookupGenericType(Collection.class, List.class));
    assertNull(lookupGenericType(List.class, Collection.class));
  }

  @Test
  public void testMapGenerics() {
    assertEquals(String.class, expand(typeOf(0, tMapE), null));
    assertEquals(Number.class, expand(typeOf(1, tMapE), ParametrizedClass.class));
    assertFalse(match(tMapE, tMapI, false));
    assertFalse(match(tMapE, tMapI, true));
    assertTrue(match(tMapI, tMapE, false));
    assertFalse(match(tMapI, tMapE, true));
  }

  @SuppressWarnings("unused")
  private static class ParametrizedClass<E extends Number, F, C extends Collection<?>, CN extends Collection<? extends Number>> {

    public List<Number> listN;
    public List<E> listE;
    public List<F> listF;
    public List<?> listI;
    public List<? extends Number> listIEN;
    public List<? super Integer> listISI;
    public List<C> listC;
    public List<CN> listCN;
    public List<Integer> listInt;
    public List<? extends Integer> listEInt;
    public Collection<? extends Integer> colEInt;
    public Map<String, E> mapE;
    public Map<String, ? extends Integer> mapI;
    public E[] arrayE;
    public F[] arrayF;
    public C[] arrayC;
    public CN[] arrayCN;
  }

  public static Type tListN;
  public static Type tListE;
  public static Type tListF;
  public static Type tListI;
  public static Type tListIEN;
  public static Type tListISI;
  public static Type tListC;
  public static Type tListCN;
  public static Type tListInt;
  public static Type tListEInt;
  public static Type tColEInt;
  public static Type tMapE;
  public static Type tMapI;
  public static Type tArrayE;
  public static Type tArrayF;
  public static Type tArrayC;
  public static Type tArrayCN;

  static {
    try {
      tListN = ParametrizedClass.class.getField("listN").getGenericType();
      tListE = ParametrizedClass.class.getField("listE").getGenericType();
      tListF = ParametrizedClass.class.getField("listF").getGenericType();
      tListI = ParametrizedClass.class.getField("listI").getGenericType();
      tListIEN = ParametrizedClass.class.getField("listIEN").getGenericType();
      tListISI = ParametrizedClass.class.getField("listISI").getGenericType();
      tListC = ParametrizedClass.class.getField("listC").getGenericType();
      tListCN = ParametrizedClass.class.getField("listCN").getGenericType();
      tListInt = ParametrizedClass.class.getField("listInt").getGenericType();
      tListEInt = ParametrizedClass.class.getField("listEInt").getGenericType();
      tColEInt = ParametrizedClass.class.getField("colEInt").getGenericType();
      tMapE = ParametrizedClass.class.getField("mapE").getGenericType();
      tMapI = ParametrizedClass.class.getField("mapI").getGenericType();
      tArrayE = ParametrizedClass.class.getField("arrayE").getGenericType();
      tArrayF = ParametrizedClass.class.getField("arrayF").getGenericType();
      tArrayC = ParametrizedClass.class.getField("arrayC").getGenericType();
      ;
      tArrayCN = ParametrizedClass.class.getField("arrayCN").getGenericType();
      ;
    } catch (SecurityException e) {
      e.printStackTrace();
    } catch (NoSuchFieldException e) {
      e.printStackTrace();
    }
  }
}
