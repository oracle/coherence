/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.management;

import com.oracle.coherence.common.base.Logger;

import java.lang.reflect.Array;
import java.lang.reflect.Method;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.IntSummaryStatistics;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.Map;
import java.util.Objects;

import java.util.Set;
import java.util.function.Function;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularDataSupport;

/**
 * A utility written to make conversions of objects to REST compatible JSON value.
 * The idea of the class is that any method which starts with "transform" string is a
 * converter. For an input class that is provided to convert, we take the simple name
 * of the class and figures out if there is a "transform" method. If one is available
 * that function is called.
 *
 * @author sr  2017.08.21
 * @since 12.2.1.4.0
 */
public class Converter
    {
    // ----- Converter methods ----------------------------------------------

    /**
     * Convert the provided Object into a REST compatible JSON object. The return type in case
     * of complex objects will typically be JSONObject, or in case of arrays, it will be JSONArray.
     *
     * @param oObject the object which needs to be converted.
     *
     * @return  the converted object
     */
    public static Object convert(Object oObject)
        {
        // unfortunately, the transform logic mentioned in the class level comments did not work in few scenarios
        // for example there are multiple implementations of CompositeData, TabularDataSupport etc
        // so we could not call a transformCompositeData method via reflection, as the simple class
        // name of the class was different

        if (Objects.isNull(oObject))
            {
            return null;
            }
        Class clzObject = oObject.getClass();

        if (clzObject.isArray())
            {
            return convertArray(oObject);
            }

        if (oObject instanceof CompositeData)
            {
            return transformCompositeData((CompositeData) oObject);
            }
        else if (oObject instanceof TabularDataSupport)
            {
            return transformTabularData((TabularDataSupport) oObject);
            }
        else if (oObject instanceof Map)
            {
            return convertMap(oObject);
            }
        else
            {
            Function<Object, Object> converter = s_mapConverters.get(clzObject.getSimpleName());
            if (converter != null)
                {
                return converter.apply(oObject);
                }
            }

        // no converter found, return the object itself
        return oObject;
        }

    // -------------------------- protected methods -------------------------------------------------

    protected static Object convertMap(Object oMap)
        {
        Map                 map  = (Map) oMap;
        Map<String, Object> json = new LinkedHashMap<>();

        for (Object key : map.keySet())
            {
            // only String key is possible in json
            json.put(key.toString(), map.get(key));
            }

        return json;
        }

    protected static Object convertArray(Object oArray)
        {
        List<Object> listObject = new ArrayList<>();
        for (int i = 0; i < Array.getLength(oArray); i++)
            {
            listObject.add(convert(Array.get(oArray, i)));
            }
        return listObject;
        }

    // -------------------------- tranform methods -----------------------------

    protected static Object transformCompositeData(CompositeData dataComposite)
        {
        Map<String, Object> json = new LinkedHashMap<>();
        for (String key : dataComposite.getCompositeType().keySet())
            {
            json.put(key, convert(dataComposite.get(key)));
            }
        return json;
        }

    public static Object transformTabularData(TabularDataSupport dataTabular)
        {
        CompositeType rowType = dataTabular.getTabularType().getRowType();
        if (rowType.containsKey("key") &&
                rowType.containsKey("value") &&
                rowType.getType("key") instanceof SimpleType)
            {
            Map<String, Object> json = new LinkedHashMap<>();
            for (Object objValue : dataTabular.values())
                {
                String sKey   = ((CompositeData) objValue).get("key").toString();
                Object oValue = convert(((CompositeData) objValue).get("value"));
                json.put(sKey, oValue);
                }
            return json;
            }
        else
            {
            Map<String, Object> json = new LinkedHashMap<>();
            for(Map.Entry<Object, Object> entry : dataTabular.entrySet())
                {
                CompositeData value = (CompositeData) entry.getValue();
                Set<String> keySet = value.getCompositeType().keySet();
                Map<String, Object> props = new LinkedHashMap<>();
                for (String key : keySet)
                    {
                    props.put(key, value.get(key));
                    }
                json.put(entry.getKey().toString(), props);
                }
            return json;
            }
        }

    protected static Object transformDate(Date date)
        {
        return new SimpleDateFormat(ISO_8601_DATE_TIME_FORMAT).format(date);
        }


    protected static Object transformIntSummaryStatistics(IntSummaryStatistics stats)
        {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("count", stats.getCount());
        map.put("average", stats.getAverage());
        map.put("min", stats.getMin());
        map.put("max", stats.getMax());
        map.put("sum", stats.getSum());
        return map;
        }

    protected static Object transformLongSummaryStatistics(LongSummaryStatistics stats)
        {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("count", stats.getCount());
        map.put("average", stats.getAverage());
        map.put("min", stats.getMin());
        map.put("max", stats.getMax());
        map.put("sum", stats.getSum());
        return map;
        }

    protected static Object getConverterFunction(Object oObject, Method method)
        {
        try
            {
            return method.invoke(null, oObject);
            }
        catch (Exception e)
            {
            Logger.err("Exception occurred while converting object of class type " +
                    oObject.getClass().getName() + '\n', e);
            }
        return oObject;
        }

    // ----- constants -----------------------------------------------------------------

    public static final String ISO_8601_DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";

    public static final String TRANSFORM = "transform";

    // ----- static data members ------------------------------------------------------

    protected static HashMap<String, Function<Object, Object>> s_mapConverters = new HashMap<>();

    static
        {
        // create a list of converter methods, any converter method starts with transform
        // for example, let us take java,util.Date as an example. there is a transformDate method
        // for converting teh date to a standards compliant string. We generate  a list of such converter methods
        // which is called in the convert(object) method
        for (Method method : Converter.class.getDeclaredMethods())
            {
            String methodName = method.getName();
            if (methodName.startsWith(TRANSFORM))
                {
                s_mapConverters.put(method.getName().substring("transform".length()),
                        o -> getConverterFunction(o, method));
                }
            }
        }
    }
