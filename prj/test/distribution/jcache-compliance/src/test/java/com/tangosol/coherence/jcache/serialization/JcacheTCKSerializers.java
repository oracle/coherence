/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.jcache.serialization;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofSerializer;
import com.tangosol.io.pof.PofWriter;
import domain.Blog;
import domain.Identifier;
import domain.Identifier2;
import org.jsr107.tck.processor.SetEntryProcessor;

import javax.cache.CacheException;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * PofSerializers for classes in JCache (JSR107) TCK
 *
 * @version        Coherence 12.1.3, 13/05/29
 * @author         Joe Fialli
 */
public class JcacheTCKSerializers
    {

    // ----- inner classes --------------------------------------------------

    public static class AbstractEntryProcessorSerializer<T>
            implements PofSerializer
        {

        @Override
        public void serialize(PofWriter pofWriter, Object o)
                throws IOException
            {
            }

        @Override
        public Object deserialize(PofReader pofReader)
                throws IOException
            {
            try
                {
                return getTypeParameterClass().newInstance();
                }
            catch (InstantiationException e)
                {
                throw new CacheException(e);
                }
            catch (IllegalAccessException e)
                {
                throw new CacheException(e);
                }
            }

        @SuppressWarnings("unchecked")
        private Class<T> getTypeParameterClass()
            {
            Type              type      = getClass().getGenericSuperclass();
            ParameterizedType paramType = (ParameterizedType) type;

            return (Class<T>) paramType.getActualTypeArguments()[0];
            }
        }

    public static class BlogSerializer
            implements PofSerializer
        {
        @Override
        public void serialize(PofWriter pofWriter, Object o)
                throws IOException
            {
            Blog b = (Blog) o;

            pofWriter.writeString(0, b.getTitle());
            pofWriter.writeString(1, b.getBody());
            pofWriter.writeRemainder(null);
            }

        @Override
        public Object deserialize(PofReader pofReader)
                throws IOException
            {
            String title = pofReader.readString(0);
            String body  = pofReader.readString(1);

            pofReader.readRemainder();

            return new Blog(title, body);
            }
        }

    public static class Identifier2Serializer
            implements PofSerializer
        {

        @Override
        public void serialize(PofWriter pofWriter, Object o)
                throws IOException
            {
            pofWriter.writeString(0, o.toString());
            pofWriter.writeRemainder(null);
            }

        @Override
        public Object deserialize(PofReader pofReader)
                throws IOException
            {
            Identifier2 result = new Identifier2(pofReader.readString(0));

            pofReader.readRemainder();

            return result;
            }
        }

    public static class IdentifierSerializer
            implements PofSerializer
        {

        @Override
        public void serialize(PofWriter pofWriter, Object o)
                throws IOException
            {
            pofWriter.writeString(0, o.toString());
            pofWriter.writeRemainder(null);
            }

        @Override
        public Object deserialize(PofReader pofReader)
                throws IOException
            {
            Identifier result = new Identifier(pofReader.readString(0));

            pofReader.readRemainder();

            return result;
            }
        }

    public static class SetEntryProcessorSerializer
            implements PofSerializer
        {

        @Override
        public void serialize(PofWriter pofWriter, Object o)
                throws IOException
            {
            SetEntryProcessor ep = (SetEntryProcessor) o;

            pofWriter.writeObject(0, ep.getValue());
            pofWriter.writeRemainder(null);
            }

        @Override
        public Object deserialize(PofReader pofReader)
                throws IOException
            {
            Object v = pofReader.readObject(0);

            pofReader.readRemainder();

            return new SetEntryProcessor(v);
            }
        }
    }
