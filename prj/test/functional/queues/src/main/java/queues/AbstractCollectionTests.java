/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package queues;

import com.oracle.coherence.common.util.Threads;
import com.oracle.coherence.testing.junit.ThreadDumpOnTimeoutRule;
import com.tangosol.internal.net.queue.NamedMapQueue;
import com.tangosol.io.DefaultSerializer;
import com.tangosol.io.Serializer;
import com.tangosol.io.pof.ConfigurablePofContext;
import com.tangosol.net.NamedCollection;
import com.tangosol.net.NamedMap;
import com.tangosol.net.Session;

import com.tangosol.net.options.WithClassLoader;
import com.tangosol.util.Binary;
import org.junit.ClassRule;
import org.junit.jupiter.api.AfterEach;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

@Timeout(100)
@SuppressWarnings("rawtypes")
public abstract class AbstractCollectionTests<NC extends NamedCollection, C extends Collection>
        implements CollectionTests<NC, C>
    {
    /**
     * Return the {@link Session} to use to create queues.
     *
     * @return  the {@link Session} to use to create queues
     */
    @Override
    public abstract Session getSession();

    @Override
    public NC getNamedCollection(String sName)
        {
        return getNamedCollection(getSession(), sName);
        }

    @Override
    public abstract NC getNamedCollection(Session session, String sName);

    @Override
    public abstract C getCollection(Session session, String sName);

    @Override
    public NamedMap getCollectionCache(NC col)
        {
        if (col instanceof NamedMapQueue)
            {
            return ((NamedMapQueue) col).getNamedMap();
            }
        return getCollectionCache(col.getName());
        }

    @Override
    public NamedMap getCollectionCache(String sName)
        {
        return getSession().getCache(sName);
        }

    @Override
    public NamedMap<Binary, Binary> getCollectionBinaryCache(NC col)
        {
        return getCollectionBinaryCache(col.getName());
        }

    @Override
    public NamedMap<Binary, Binary> getCollectionBinaryCache(String sName)
        {
        return getSession().getCache(sName, WithClassLoader.nullImplementation());
        }

    @AfterEach
    void cleanupCollections()
        {
        List<NC> listQueue = new ArrayList<>(m_mapQueue.values());
        m_mapQueue.clear();
        for (NamedCollection col : listQueue)
            {
            try
                {
                col.destroy();
                }
            catch (Exception e)
                {
                // ignored
                }
            }
        }

    @RegisterExtension
    TestExecutionExceptionHandler timeoutExceptionHandler = (context, throwable) ->
        {
        if (throwable instanceof TimeoutException && throwable.getSuppressed().length > 0
                && throwable.getSuppressed()[0] instanceof InterruptedException)
            {
            throwable.getSuppressed()[0].printStackTrace(System.out);
            System.err.println("Test timed out: " + context.getDisplayName());
            System.err.println(Threads.getThreadDump(true));
            }
        throw throwable;
        };

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldHaveCorrectSerializer(String sSerializer)
        {
        String     sName      = getNewName(sSerializer);
        NC         collection = getNamedCollection(sName);
        Serializer serializer = collection.getService().getSerializer();
        switch (sSerializer)
            {
            case "pof":
                assertThat(serializer, is(instanceOf(ConfigurablePofContext.class)));
                break;
            case "java":
                assertThat(serializer, is(instanceOf(DefaultSerializer.class)));
                break;
            default:
                fail("Invalid serializer name \"" + serializer + "\" should be pof or java");
            }
        }

    // ----- test NamedCollection methods -----------------------------------

    @Test
    public void shouldHaveName()
        {
        String sName      = getNewName();
        NC     collection = getNamedCollection(sName);
        assertThat(collection.getName(), is(sName));
        }

    @Test
    public void shouldHaveService()
        {
        String sName      = getNewName();
        NC     collection = getNamedCollection(sName);
        assertThat(collection.getService(), is(notNullValue()));
        }

    @Test
    public void shouldGetNewInstanceOfReleasedQueue()
        {
        String sName = getNewName();
        NC     col1  = getNamedCollection(sName);
        col1.release();
        NC col2 = getNamedCollection(sName);
        assertThat(col2, is(not(sameInstance(col1))));
        }

    @Test
    public void shouldGetNewInstanceOfDestroyedQueue()
        {
        String sName = getNewName();
        NC     col1  = getNamedCollection(sName);
        col1.destroy();
        NC col2 = getNamedCollection(sName);
        assertThat(col2, is(not(sameInstance(col1))));
        }

    @Test
    public void shouldBeActiveBasedOnCache()
        {
        String sName = getNewName();
        NC     queue = getNamedCollection(sName);
        assertThat(queue.isActive(), is(true));
        NamedMap<?, ?> cache = getCollectionCache(queue);
        cache.release();
        assertThat(queue.isActive(), is(cache.isActive()));
        }

    @Test
    public void shouldBeReleased()
        {
        String  sName      = getNewName();
        NC      collection = getNamedCollection(sName);

        assertThat(collection.isReleased(), is(false));

        NamedMap<?, ?> cache = getCollectionCache(collection);
        assertThat(cache.isReleased(), is(false));

        collection.release();
        assertThat(collection.isActive(), is(false));
        assertThat(collection.isReleased(), is(true));
        assertThat(collection.isDestroyed(), is(false));
        assertThat(cache.isReleased(), is(true));
        assertThat(cache.isDestroyed(), is(false));
        }

    @Test
    public void shouldBeClosed()
        {
        String  sName      = getNewName();
        NC      collection = getNamedCollection(sName);

        assertThat(collection.isReleased(), is(false));

        NamedMap<?, ?> cache = getCollectionCache(collection);
        assertThat(cache.isReleased(), is(false));

        collection.close();
        assertThat(collection.isActive(), is(false));
        assertThat(collection.isReleased(), is(true));
        assertThat(collection.isDestroyed(), is(false));
        assertThat(cache.isReleased(), is(true));
        assertThat(cache.isDestroyed(), is(false));
        }

    @Test
    public void shouldBeDestroyed()
        {
        String sName = getNewName();
        NC     queue = getNamedCollection(sName);
        assertThat(queue.isDestroyed(), is(false));

        NamedMap<?, ?> cache = getCollectionCache(queue);
        assertThat(cache.isDestroyed(), is(false));

        queue.destroy();
        assertThat(queue.isActive(), is(false));
        assertThat(queue.isDestroyed(), is(true));
        assertThat(cache.isDestroyed(), is(true));
        }

    // ----- test size() method ---------------------------------------------
    
    // ----- test empty() method --------------------------------------------

    @Test
    public void shouldBeEmpty()
        {
        Collection collection = getNewCollection();
        assertThat(collection.isEmpty(), is(true));
        }

    // ----- test contains() method --------------------------------------------
    // ----- test iterator() method --------------------------------------------
    // ----- test toArray() method --------------------------------------------

    // ----- test add() method ----------------------------------------------
    
    @SuppressWarnings("unchecked")
    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldAddToCollection(String sSerializer)
        {
        Collection collection = getNewCollection(sSerializer);
        NamedMap   cache      = getCollectionCache(getCurrentName(sSerializer));
        String     sMessage   = "message-1";

        assertThat(collection.add(sMessage), is(true));
        assertThat(cache.size(), is(1));

        Object oKey = cache.keySet().iterator().next();
        assertThat(cache.get(oKey), is(sMessage));
        }

    // ----- test remove() method -------------------------------------------

    // ----- test containsAll() method --------------------------------------

    // ----- test containsAll() method --------------------------------------

    // ----- test addAll() method -------------------------------------------

    // ----- test removeAll() method ----------------------------------------

    // ----- test removeIf() method -----------------------------------------

    // ----- test retainAll() method ----------------------------------------

    // ----- test retainAll() method ----------------------------------------

    // ----- test clear() method --------------------------------------------

    // ----- test equals() method -------------------------------------------

    // ----- test hashCode() method -----------------------------------------

    // ----- test spliterator() method --------------------------------------

    // ----- test stream() method -------------------------------------------

    // ----- test parallelStream() method -----------------------------------


    // ----- helper methods -------------------------------------------------

    @Override
    public C getNewCollection()
        {
        return getNewCollection("");
        }

    @SuppressWarnings("unchecked")
    @Override
    public C getNewCollection(String sPrefix)
        {
        return (C) getNewNamedCollection(sPrefix);
        }

    @Override
    public NC getNewNamedCollection()
        {
        return getNewNamedCollection("");
        }

    @Override
    public NC getNewNamedCollection(String sPrefix)
        {
        String  sName   = getNewName(sPrefix);
        Session session = getSession();
        return m_mapQueue.computeIfAbsent(sName, k -> getNamedCollection(session, sName));
        }

    @Override
    public C getCurrentCollection()
        {
        return getCurrentCollection("");
        }

    @SuppressWarnings("unchecked")
    @Override
    public C getCurrentCollection(String sPrefix)
        {
        return (C) getCurrentNamedCollection(sPrefix);
        }

    @Override
    public NC getCurrentNamedCollection()
        {
        return getCurrentNamedCollection("");
        }

    @Override
    public NC getCurrentNamedCollection(String sPrefix)
        {
        String  sName   = getCurrentName(sPrefix);
        Session session = getSession();
        return m_mapQueue.computeIfAbsent(sName, k -> getNamedCollection(session, sName));
        }

    @Override
    public String getNewName()
        {
        return getNewName("");
        }

    @Override
    public String getNewName(String sPrefix)
        {
        // we must use incrementAndGet() so that any other calls to the AtomicInteger.get() during a test
        // return the same number.
        if (sPrefix.isEmpty())
            {
            return COLLECTION_NAME + m_collectionSuffix.incrementAndGet();
            }
        return sPrefix + "-" + COLLECTION_NAME + m_collectionSuffix.incrementAndGet();
        }

    @Override
    public String getCurrentName()
        {
        return getCurrentName("");
        }

    @Override
    public String getCurrentName(String sPrefix)
        {
        if (sPrefix.isEmpty())
            {
            return COLLECTION_NAME + m_collectionSuffix.get();
            }
        return sPrefix + "-" + COLLECTION_NAME + m_collectionSuffix.get();
        }

    // ----- data members ---------------------------------------------------

    // We use a fixed seed as we just want a random, but the test will always run the same.
    public static final Random m_random = new Random(1234L);

    /**
     * The prefix used for the collection name.
     */
    public static final String COLLECTION_NAME = "test-queue-";

    /**
     * An atomic integer to ensure a different collection name for every test.
     */
    private static final AtomicInteger m_collectionSuffix = new AtomicInteger();

    /**
     * A map of collections used in tests.
     */
    protected final Map<String, NC> m_mapQueue = new ConcurrentHashMap<>();

    /**
     * Time out the test if it hangs
     */
    @ClassRule
    public static final ThreadDumpOnTimeoutRule timeout = ThreadDumpOnTimeoutRule.after(15, TimeUnit.MINUTES);
    }
