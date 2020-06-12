/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.io.json;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.tangosol.internal.util.invoke.Lambdas;
import com.tangosol.internal.util.invoke.Remotable;
import com.tangosol.internal.util.invoke.RemoteConstructor;

import com.tangosol.io.ByteArrayWriteBuffer;

import com.tangosol.io.Serializer;

import com.tangosol.util.Binary;

import java.io.IOException;
import java.io.InputStream;

import java.lang.reflect.Field;

import java.net.InetAddress;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import java.util.function.BiFunction;

import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Base class for serialization tests.
 *
 * @author Aleks Seovic  2017.10.03
* @since 20.06
 */
@SuppressWarnings("unchecked")
public abstract class AbstractSerializerTest
    {
    // ----- test setup -----------------------------------------------------

    @BeforeEach
    public void before()
        {
        m_serializer = new JsonSerializer(null, builder -> builder.useIndentation(true), false);
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Assert that an object can be serialized into Json and then de-serialized back
     * correctly. This method uses Objects.equals(o1, o2) as BiFunction for
     * objects that already implement equals and to keep old tests working until they are converted
     * to new format. This method also defaults the Json Serialize test to null or ignore
     *
     * @param expected  Object to serialize
     *
     * @return the de-serialized object.
     */
    @SuppressWarnings("UnusedReturnValue")
    protected Object assertRoundTrip(Object expected)
            throws IOException
        {
        return assertRoundTrip(expected, computeDeserializationClass(expected), Objects::equals);
        }

    /**
     * Assert that an object can be serialized into Json and then de-serialized back
     * correctly.
     *
     * @param expected          object to serialize
     * @param equalityFunction  the {@link BiFunction} to use for comparison of expected and actual as objects
     *
     * @return the de-serialized object.
     */
    @SuppressWarnings("UnusedReturnValue")
    protected Object assertRoundTrip(Object expected, BiFunction<Object, Object, Boolean> equalityFunction)
            throws IOException
        {
        return assertRoundTrip(expected, computeDeserializationClass(expected), equalityFunction);
        }

    /**
     * Assert that an object can be serialized into Json and then de-serialized back
     * correctly.
     *
     * @param expected          object to serialize
     * @param expectedClass     specify the class to deserialize back to
     * @param equalityFunction  the {@link BiFunction} to use for comparison of expected and actual as objects
     *
     * @return the de-serialized object.
     */
    protected <T> T assertRoundTrip(Object expected, Class<T> expectedClass, BiFunction<Object, Object, Boolean> equalityFunction)
            throws IOException
        {
        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(512);

        m_serializer.serialize(buf.getBufferOutput(), expected);
        System.out.println("Json Serialization of " + expected + ": " +
                           new String(buf.toByteArray()));

        T actual = m_serializer.deserialize(buf.getReadBuffer().getBufferInput(), expectedClass);

        System.out.println(
                String.format("Json deserialization of expected type: %s, actual type: %s, actual value: %s\n\n",
                              expectedClass, actual == null ? "null" : actual.getClass(), actual));
        assertTrue(equalityFunction.apply(expected, actual),
                   "Equality BiFunction failed for expected=" + expected + ", actual=" + actual);
        return actual;
        }

    /**
     * Assert json serialization of expected matches json deserialize of jsonDataFile
     * via a JsonMap comparison.
     *
     * @param expected      Java object to be json serialized
     * @param jsonDataFile  json from of expected
     */
    protected void assertJsonProperties(Object expected, String jsonDataFile)
            throws IOException, ClassNotFoundException
        {
        JsonObject generatedMap = jsonSerialize(expected);
        JsonObject expectedMap  = loadJsonObject(jsonDataFile);
        assertTrue(jsonObjectEquals(expectedMap, generatedMap, true));
        }

    /**
     * Deserialize the object and return a {@link JsonObject} to enable
     * detailed inspection of the generated Json.
     *
     * @param expected  The object to serialize
     *
     * @return {@link JsonObject} representation
     */
    protected JsonObject jsonSerialize(Object expected)
            throws IOException
        {
        ByteArrayWriteBuffer buf           = new ByteArrayWriteBuffer(512);
        Class<?>             expectedClass = expected.getClass();

        m_serializer.serialize(buf.getBufferOutput(), expected);

        String serializedJson = new String(buf.toByteArray());
        System.out.println("Json Serialization of class " + expectedClass.getSimpleName() + ": " +
                           new String(buf.toByteArray()));
        detectMissingJsonProperty(serializedJson);
        return m_serializer.deserialize(buf.getReadBuffer().getBufferInput(), JsonObject.class);
        }

    /**
     * Perform a JsonMap comparison between expected and actual.
     * Leverages JsonProperty annotations when JsonMap has a @class property
     * and that class has one or more explicit JsonProperty annotations.
     * Otherwise, fallback to an entry by entry comparison.
     *
     * @param expected  expected JsonMap
     * @param actual    actual JsonMap
     * @param warn      aid debugging by printing a warning when mismatch detected in
     *                  comparision.
     *
     * @return true if expected JsonMap compares to actual JsonMap.
     */
    public static boolean jsonObjectEquals(JsonObject expected, JsonObject actual, boolean warn)
            throws ClassNotFoundException
        {
        // get meta property @class on JsonMap. Jackson json serialize adds this and json deserialize uses it.
        String             expectedClass = (String) expected.get("@class");
        String             actualClass   = (String) actual.get("@class");
        List<JsonProperty> properties = expectedClass == null
                                        ? Collections.EMPTY_LIST
                                        : getJsonProperties(expectedClass);

        if (properties.size() == 0)
            {
            // either no @class field on JsonObject or no JsonProperty on specified class.
            // no JsonProperty annotations to validate in this comparison.
            for (Map.Entry<String, Object> expectedEntry : expected.entrySet())
                {
                Object expectedValue = expectedEntry.getValue();
                Object actualValue   = actual.get(expectedEntry.getKey());
                if (jsonPropertyEquals(expectedValue, actualValue, warn))
                    {
                    //noinspection UnnecessaryContinue
                    continue;
                    }
                else
                    {
                    if (warn)
                        {
                        System.out
                                .println("equal(" + expectedClass + ", " + actualClass + ") failed for Json property "
                                         + expectedEntry.getKey() + " expected: " + expectedValue + " actual: "
                                         + actualValue);
                        }
                    return false;
                    }
                }
            return true;
            }

        if (expectedClass.compareTo(actualClass) != 0)
            {
            if (warn)
                {
                System.out.println("equals(" + expectedClass + ", " + actualClass + ") failed due to different classes");
                }
            return false;
            }

        assertJsonPropertyAnnotations(expected.keySet(),
                                      properties,
                                      expectedClass,
                                      "properties generated by Jackson deserialization");
        assertJsonPropertyAnnotations(actual.keySet(),
                                      properties,
                                      actualClass,
                                      "properties from json data file for class " + actualClass);

        for (JsonProperty property : properties)
            {
            String propName = property.value();

            if (property.required())
                {
                if (expected.containsKey(propName) && actual.containsKey(propName) &&
                    jsonPropertyEquals(expected.get(propName), actual.get(propName), warn))
                    {
                    //noinspection UnnecessaryContinue
                    continue;
                    }
                else
                    {
                    if (warn)
                        {
                        System.out.println("equal(" + expectedClass + ", " + actualClass + ") failed for required Json property " + propName +
                                           " expected: " + expected.get(propName) + " actual: " + actual.get(propName));
                        }
                    return false;
                    }
                }
            else
                {
                if (jsonPropertyEquals(expected.get(propName), actual.get(propName), warn))
                    {
                    //noinspection UnnecessaryContinue
                    continue;
                    }
                else
                    {
                    if (warn)
                        {
                        System.out.println("equal(" + expectedClass + ", " + actualClass + ") failed for optional Json property " + propName +
                                           " expected: " + expected.get(propName) + " actual: " + actual.get(propName));
                        }
                    return false;
                    }
                }
            }
        return true;
        }

    /**
     * Assert each property name in a JsonMap with an @class property has an explicit @JsonProperty annotation.
     * Assist in identifying missing @JsonProperty annotation on serializable coherence classes.
     * Also identifies stale json data files containing mismatched property name from one that is on @JsonProperty.
     *
     * @param jsonMapProperties  property names in a JsonMap
     * @param properties         JsonProperty annotations collected for clazz
     * @param className          JsonMap property for @class.  This was class that JsonProperty an
     */
    public static void assertJsonPropertyAnnotations(Set<String> jsonMapProperties,
                                                     List<JsonProperty> properties,
                                                     String className,
                                                     String description)
        {
        for (String propertyName : jsonMapProperties)
            {
            if (propertyName.equals("@class"))
                {
                // skip meta property
                continue;
                }

            boolean fMissing = true;
            for (JsonProperty jsonProperty : properties)
                {
                if (jsonProperty.value().equals(propertyName))
                    {
                    fMissing = false;
                    break;
                    }
                }
            assertFalse(fMissing, description + ": verify @JsonProperty annotation on member " + propertyName + " on class " + className);
            }
        }

    /**
     * Compare two Json properties, following rules that are not strictly equals.
     * Two collection properties are compared via contentsEqual and allow for
     * ordering to not be preserved.
     *
     * @param val1  a Json property value from expected
     * @param val2  a Json property value from actual
     * @param warn  if true, log a warning when equality fails.
     *
     * @return true if json property equality is true for these 2 values.
     */
    public static boolean jsonPropertyEquals(Object val1, Object val2, boolean warn)
            throws ClassNotFoundException
        {
        if (val1 == null)
            {
            return val2 == null;
            }

        // due to jackson default databinding for numbers, may discover
        // more needs to be done here in future. Jackson will
        // deserialize a number value into smallest Java type that will
        // hold it. A long JsonProperty can be serialized into a json value of
        // number and then the Jackson json deserialize will deserialize it
        // back into an Integer because that data type is big enough to hold it.
        // this impacts equality checks and may need to account for this in
        // future. (compare values based on the largest number type).
        if (val1 instanceof String || val1 instanceof Number || val1 instanceof Boolean)
            {
            return val1.equals(val2);
            }

        if (val1 instanceof JsonObject && val2 instanceof JsonObject)
            {
            return jsonObjectEquals((JsonObject) val1, (JsonObject) val2, warn);
            }

        // Map possible Array value to Collection.
        if (val1.getClass().isArray())
            {
            val1 = Collections.singletonList(val1);
            }

        if (val2 != null && val2.getClass().isArray())
            {
            val2 = Collections.singletonList(val2);
            }

        if (Collection.class.isAssignableFrom(val1.getClass()) &&
            val2 != null && Collection.class.isAssignableFrom(val2.getClass()))
            {
            return contentsEqual((Collection) val1, (Collection) val2, warn);
            }

        return val1.equals(val2);

        }

    /**
     * Returns true if this collection {@code col1} contains all of the elements in collection {@code col2}.
     * Performs an order independent comparison of the two collections. Unlike containsAll, both collections must be
     * same size.
     *
     * @param col1  collection
     * @param col2  collection
     * @param warn  log a warning if comparison fails, aid test debugging
     *
     * @return true if collection {@code col2} contains all elements in {@code col1}.
     */
    private static boolean contentsEqual(Collection col1, Collection col2, boolean warn)
            throws ClassNotFoundException
        {
        if (col1.size() == col2.size())
            {
            // implement contentsEqual, not a straight array equality
            for (Object col1_item : col1)
                {
                boolean found = false;
                for (Object col2_item : col2)
                    {
                    // never log a warning on contains check. many will fail.
                    if (jsonPropertyEquals(col1_item, col2_item, false))
                        {
                        found = true;
                        break;
                        }
                    }
                if (!found)
                    {
                    if (warn)
                        {
                        System.out
                                .println("collection contentsEqual: failed to find collection1 item " + col1_item + " in "
                                         + "collection " + col2);
                        }
                    return false;
                    }
                }
            return true;
            }
        else
            {
            if (warn)
                {
                System.out.println("Collection contentsEqual failed. Collection sizes differ");
                }
            return false;
            }
        }

    /**
     * Return all fields in a class's hierarchy.
     *
     * @param fields  fields from subclasses
     * @param clazz   current class to get DeclaredFields from.
     *
     * @return list of fields from subclasses and clazz.
     */
    public static List<Field> getAllFields(List<Field> fields, Class<?> clazz)
        {
        fields.addAll(Arrays.asList(clazz.getDeclaredFields()));

        if (clazz.getSuperclass() != null)
            {
            getAllFields(fields, clazz.getSuperclass());
            }

        return fields;
        }

    /**
     * Return a  list of {@link JsonProperty} annotations on all field members of a class and
     * its superclasses. This list only includes actual annotations and it does not
     * contain implicit JsonProperty computed by Jackson databinding.
     *
     * @param className  class to inspect for JsonProperty annotations.
     *
     * @return list of {@link JsonProperty}.
     */
    public static List<JsonProperty> getJsonProperties(String className)
            throws ClassNotFoundException
        {
        List<JsonProperty> jsonProperties = new ArrayList<>();
        Class<?> clazz = Class.forName(className);
        List<Field> fields = getAllFields(new LinkedList<>(), clazz);

        for (Field field : fields)
            {
            JsonProperty prop = field.getAnnotation(JsonProperty.class);
            if (prop != null)
                {
                jsonProperties.add(prop);
                }
            }
        return jsonProperties;
        }

    /**
     * Detect any properties that where incorrectly serialized
     *
     * @param jsonSerialization  the serialized Json
     */
    protected void detectMissingJsonProperty(String jsonSerialization)
        {
        final int NOT_FOUND = -1;
        int idx = jsonSerialization.indexOf("\"m_");
        assertEquals(idx, NOT_FOUND,
                     "verify \"\"m_\" being found in json serialized form is not a missing @JsonProperty on an instance field: " +
                     (idx == NOT_FOUND ? "" : jsonSerialization.substring(idx)));
        idx = jsonSerialization.indexOf("\"f_");
        assertEquals(idx, NOT_FOUND,
                     "verify \"\"f_\" being found in json serialized form is not a missing @JsonProperty on an instance field: " +
                     (idx == NOT_FOUND ? "" : jsonSerialization.substring(idx)));
        }

    /**
     * Load a file named 'sName'.json from test/resources/json and return as a {@link JsonObject}
     *
     * @param sName  the name of the file to load
     *
     * @return the contents as a JsonMap
     */
    protected JsonObject loadJsonObject(String sName)
            throws IOException
        {
        String sResource = "json/" + sName + ".json";
        InputStream in = AbstractSerializerTest.class.getClassLoader().getResourceAsStream(sResource);

        if (in == null)
            {
            throw new RuntimeException("Unable to load json file [" + sResource + "] to compare to.");
            }

        return m_serializer.deserialize(Binary.readBinary(in).getBufferInput(), JsonObject.class);
        }

    /**
     * Load a file named 'sName'.json from test/resources/json and return as a {@link JsonObject}
     *
     * @param sName  the name of the file to load
     *
     * @return the contents as a JsonMap
     */
    @SuppressWarnings("SameParameterValue")
    protected <T> T loadJsonObject(String sName, Class<T> expectedClass)
            throws IOException
        {
        String sResource = "json/" + sName + ".json";
        InputStream in = AbstractSerializerTest.class.getClassLoader().getResourceAsStream(sResource);

        if (in == null)
            {
            throw new RuntimeException("Unable to load json file [" + sResource + "] to compare to.");
            }

        return m_serializer.deserialize(Binary.readBinary(in).getBufferInput(), expectedClass);
        }

    /**
     * Compute class to be used to deserialize {@code serializedInstance}.
     * Handles special casing for deserialization class for root objects.
     *
     * @param serializedInstance  serialized instance
     *
     * @return class to use to round trip deserialization
     */
    protected Class<?> computeDeserializationClass(Object serializedInstance)
        {
        if (serializedInstance instanceof Remotable || Lambdas.isLambda(serializedInstance))
            {
            // account for SerializationHelper.replace/realize.
            return RemoteConstructor.class;
            }
        if (serializedInstance instanceof InetAddress)
            {
            // account for Jackson databind deserialization not
            // working for impl classes like InetAddress4, only interface class.
            // field references will always be the interface class InetAddress.
            return InetAddress.class;
            }
        else if (serializedInstance != null)
            {
            return serializedInstance.getClass();
            }
        else
            {
            return Object.class;
            }
        }

    // ----- data members ---------------------------------------------------

    protected Serializer m_serializer;
    }
