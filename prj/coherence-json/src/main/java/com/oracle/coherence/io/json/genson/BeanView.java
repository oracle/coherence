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

package com.oracle.coherence.io.json.genson;


/**
 * Interface to be implemented by classes who want to act as a view on objects of type T during
 * serialization and deserializaiton.
 * <p/>
 * To understand what a BeanView is we must first understand one of the problems it is intended to
 * solve. Imagine you store some business objects in a cache and you have internal and external
 * webservices that all return a different json representation of those objects (filtered properties
 * and even transformed properties). The external webservices can't return the same representation
 * as the internal ones as the object may contain some confidential data.
 * <ul>
 * Usually you have two choices :
 * <li>Use a different instance of the json library for each different json representation and
 * configure it with custom Serializers/Deserializers (you can use annotations to filter properties
 * but not to change its name if you have multiple names and nor to transform the data).</li>
 * <li>Use data transfer objects that will act as a "View of your Model". You will have to copy the
 * data from the cached objects to the DTOs and serialize them. As a result your cache has lost some
 * of its interest (you will create new instances of DTOs).</li>
 * </ul>
 * <p/>
 * The BeanView tries to solve this kind of problem by taking the second approach. Indeed
 * implementations of BeanView will act as a stateless bean that will extract data (and could apply
 * transformations) during serialization and as a factory and data aggregator during
 * deserialization. The parameterized type T will correspond to the type of the objects on which
 * this view can be applied. All the methods from the view respecting the conventional JavaBean
 * structure will be used (getters to extract data, setters to aggregate and static methods
 * annotated with {@link com.oracle.coherence.io.json.genson.annotation.JsonCreator JsonCreator} as factory methods). Except that the
 * getters will take an argument of type T (from which to extract the data), and the setter two
 * arguments, the value (can be a complex object, in that case Genson will try to deserialize the
 * current value into that type) and T object in which to set the data. Parameters order matters,
 * for setters the first parameter is the value to deserialize and the second is the object that you
 * are building (of type T). By default the beanview functionality is disabled, to enable it use
 * method {@link com.oracle.coherence.io.json.genson.GensonBuilder#useBeanViews(boolean)}
 * setWithBeanViewConverter(true)} from Genson.Builder. Lets have a look at this example to better
 * understand how it works.
 * <p/>
 * <pre>
 * public static class Person {
 * 	private String lastName;
 * 	String name;
 * 	int birthYear;
 * 	String thisFieldWontBeSerialized;
 *
 * 	Person(String lastName) {
 * 		this.lastName = lastName;
 *  }
 *
 * public String getLastName() {
 * 	return lastName;
 * }
 *
 * // instead of serializing and deserializing Person based on the fields and methods it contains those
 * // and only those from the BeanView will be used
 * public static class ViewOfPerson implements BeanView&lt;Person&gt; {
 * 	public ViewOfPerson() {
 *  }
 *
 * 	// This method will be called to create an instance of Person instead of using the constructor
 * 	// or annotated @Creator method from Person
 * 	&#064;JsonCreator
 * 	public static Person createNewPerson(String lastName) {
 * 		return new Person(lastName);
 *  }
 *
 * 	public String getLastName(Person p) {
 * 		return p.getLastName();
 *  }
 *
 * 	public @JsonProperty(&quot;name&quot;)
 * 	String getNameOf(Person p) {
 * 		return p.name;
 *  }
 *
 * 	// here we will transform the birth year of the person into its age and change the serialized
 * 	// name from &quot;birthYear&quot; to &quot;age&quot;
 * 	public int getAge(Person p) {
 * 		return GregorianCalendar.getInstance().get(Calendar.YEAR) - p.birthYear;
 *  }
 *
 * 	public void setName(String name, Person p) {
 * 		p.name = name;
 *  }
 *
 * 	// here it will match the property named &quot;age&quot; from the json stream and transform it into birth
 * 	// year of Person
 * 	&#064;JsonProperty(&quot;age&quot;)
 * 	public void setBirthYear(int personBirthYear, Person p) {
 * 		p.birthYear = GregorianCalendar.getInstance().get(Calendar.YEAR) - personBirthYear;
 *  }
 * }
 *
 * public static void main(String[] args) {
 * 	Genson genson = new Genson.Builder().setWithBeanViewConverter(true).create();
 * 	genson.serialize(new Person("eugen"), ViewOfPerson.class);
 * }
 * </pre>
 * <p/>
 * <p/>
 * Implementations of BeanView must be stateless, thread safe and have a default no arg constructor.
 * BeanViews will be applied at <u>runtime before the standard Converter</u>. If a view for the
 * current type is present in the context it will be used instead of the corresponding Converter. If
 * you want to understand how it works behind the scene you can have a look at <a href=
 * "http://code.google.com/p/genson/source/browse/src/main/java/com/owlike/genson/convert/BeanViewConverter.java"
 * >BeanViewConverter</a> and <a href=
 * "http://code.google.com/p/genson/source/browse/src/main/java/com/owlike/genson/reflect/BeanViewDescriptorProvider.java"
 * >BeanViewDescriptorProvider</a>.
 *
 * @param <T> the type of objects on which this view will be applied.
 * @see com.oracle.coherence.io.json.genson.convert.BeanViewConverter BeanViewConverter
 * @see com.oracle.coherence.io.json.genson.reflect.BeanViewDescriptorProvider BeanViewDescriptorProvider
 * @see com.oracle.coherence.io.json.genson.annotation.JsonCreator JsonCreator
 * @see com.oracle.coherence.io.json.genson.annotation.JsonProperty JsonProperty
 */
public interface BeanView<T> {

}