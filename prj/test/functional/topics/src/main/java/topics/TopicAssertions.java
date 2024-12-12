/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package topics;

import com.tangosol.internal.net.topic.impl.paged.PagedTopicCaches;
import com.tangosol.internal.net.topic.impl.paged.model.ContentKey;

import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.Serializer;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.net.AbstractInvocable;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.DistributedCacheService;
import com.tangosol.net.NamedCache;
import com.tangosol.net.topic.NamedTopic;

import com.tangosol.util.Binary;
import com.tangosol.util.Converter;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.NullImplementation;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author jk 2016.01.21
 */
@SuppressWarnings({"unchecked", "resource"})
public class TopicAssertions
    {
    public static class PositionComparator
        implements Comparator<ContentKey>
        {
        @Override
        public int compare(ContentKey a, ContentKey b)
            {
            if (a.getPage() < b.getPage())
                {
                return -1;
                }
            else if (a.getPage() > b.getPage())
                {
                return 1;
                }
            else
                {
                return a.getElement() - b.getElement();
                }
            }

        public static final PositionComparator INSTANCE = new PositionComparator();
        }


    public static void assertPublishedOrder(NamedTopic<String> topic, int cElements, String... asPrefix)
        {
        String sName              = topic.getName();
        String sElementsCacheName = PagedTopicCaches.Names.CONTENT.cacheNameForTopicName(sName);

        // Create a Map of Lists to hold the elements for each prefix
        Map<String,List<String>> map = new TreeMap<>();
        Arrays.stream(asPrefix).forEach((sPrefix) -> map.put(sPrefix, new ArrayList<>()));

        DistributedCacheService    cacheService  = (DistributedCacheService) topic.getService();
        Serializer                 serializer    = cacheService.getSerializer();
        Converter<Binary, String>  convValue     = bin -> bin == null ? null : ExternalizableHelper.fromBinary(bin, serializer);
        NamedCache<Binary, Binary> cacheElements = cacheService.ensureCache(sElementsCacheName, NullImplementation.getClassLoader());

        int[] anChan = new HashSet<>(cacheElements.keySet()).stream().map(ContentKey::fromBinary).mapToInt(ContentKey::getChannel).distinct().toArray();
        for (int nChan : anChan)
            {
            // Get the starting Position - i.e. the lowest
            ContentKey position = new HashSet<>(cacheElements.keySet()).stream().map(ContentKey::fromBinary).filter(pos -> pos.getChannel() == nChan).min(PositionComparator.INSTANCE).get();

            int cTotalElements = (int) new HashSet<>(cacheElements.keySet()).stream().map(ContentKey::fromBinary).filter(pos -> pos.getChannel() == nChan).count();
            long lPage  = position.getPage();
            int  nElement = position.getElement();


            // Pull all the elements from the cache in order
            for (int i=0; i<cTotalElements; i++)
                {
                ContentKey positionTest = new ContentKey(nChan, lPage, nElement++);
                String     sValue       = convValue.apply(cacheElements.get(positionTest.toBinary(257)));

                // If the value is null then try the next page
                if (sValue == null)
                    {
                    lPage++;
                    nElement = 0;

                    positionTest = new ContentKey(nChan, lPage, nElement++);
                    sValue       = convValue.apply(cacheElements.get(positionTest.toBinary(257)));
                    }

                // Assert there was a value
                assertThat("Assertion failure: asserting " + cElements + " in topic: " + sName + ":  only at index " + i + " positionTest=" + positionTest,
                    sValue, is(notNullValue()));

                String sValueFinal = sValue;

                // Get the Map.Entry from the Map for the prefix of the value we have
                Map.Entry<String,List<String>> entryForPrefix = map.entrySet().stream()
                        .filter((entry) -> sValueFinal.startsWith(entry.getKey()))
                        .findFirst()
                        .get();

                // Get the List of values read so far
                List<String> list = entryForPrefix.getValue();

                // Assert that the value is the expected value
                assertThat(sValue, is(entryForPrefix.getKey() + list.size()));

                // Add the value just read to the list
                list.add(sValue);
                }
            }
        }

    // ----- inner class: AssertPublishedOrderInvocable ---------------------

    public static class AssertPublishedOrderInvocable
            extends AbstractInvocable
            implements ExternalizableLite, PortableObject
        {
        public AssertPublishedOrderInvocable()
            {
            }

        public AssertPublishedOrderInvocable(String sTopicName, int cElements, String[] asPrefix)
            {
            m_sTopicName = sTopicName;
            m_cElements  = cElements;
            m_asPrefix   = asPrefix;
            }

        @Override
        public void run()
            {
            ConfigurableCacheFactory ccf   = CacheFactory.getConfigurableCacheFactory();
            NamedTopic<String>       topic = ccf.ensureTopic(m_sTopicName);
            TopicAssertions.assertPublishedOrder(topic, m_cElements, m_asPrefix);
            setResult(true);
            }

        @Override
        public void readExternal(PofReader in) throws IOException
            {
            m_sTopicName = in.readString(0);
            m_cElements  = in.readInt(1);
            m_asPrefix   = in.readArray(2, String[]::new);
            }

        @Override
        public void writeExternal(PofWriter out) throws IOException
            {
            out.writeString(0, m_sTopicName);
            out.writeInt(1, m_cElements);
            out.writeObjectArray(2, m_asPrefix);
            }

        @Override
        public void readExternal(DataInput in) throws IOException
            {
            m_sTopicName = ExternalizableHelper.readSafeUTF(in);
            m_cElements  = in.readInt();
            m_asPrefix   = ExternalizableHelper.readStringArray(in);
            }

        @Override
        public void writeExternal(DataOutput out) throws IOException
            {
            ExternalizableHelper.writeSafeUTF(out, m_sTopicName);
            out.writeInt(m_cElements);
            ExternalizableHelper.writeStringArray(out, m_asPrefix);
            }

        // ----- data members -----------------------------------------------

        private String m_sTopicName;

        private int m_cElements;

        private String[] m_asPrefix;
        }
    }
