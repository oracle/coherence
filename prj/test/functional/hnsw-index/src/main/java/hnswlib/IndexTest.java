/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package hnswlib;

import com.oracle.coherence.hnswlib.ConcurrentIndex;
import com.oracle.coherence.hnswlib.Index;
import com.oracle.coherence.hnswlib.SpaceName;
import com.oracle.coherence.hnswlib.exception.IndexNotInitializedException;
import com.oracle.coherence.hnswlib.exception.OnceIndexIsClearedItCannotBeReusedException;
import com.oracle.coherence.hnswlib.exception.UnexpectedNativeException;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class IndexTest
        extends AbstractIndexTest
    {
    @Override
    protected Index createIndexInstance(SpaceName spaceName, int dimensions)
        {
        return new Index(spaceName, dimensions);
        }

    @Test
    public void testSynchronisedIndex() throws UnexpectedNativeException
        {
        Index i1 = createIndexInstance(SpaceName.COSINE, 50);
        i1.initialize(500_000, 16, 200, 100, false);
        Index syncIndex = Index.synchronizedIndex(i1);
        assertEquals(syncIndex.getLength(), i1.getLength());
        assertThat(syncIndex, instanceOf(ConcurrentIndex.class));
        syncIndex.clear();
        }

    @Test
    public void testSynchronisedIndexFailAfterReferenceClear()
            throws UnexpectedNativeException
        {
        assertThrows(OnceIndexIsClearedItCannotBeReusedException.class, () ->
            {
            Index i1 = createIndexInstance(SpaceName.COSINE, 50);
            i1.initialize(500_000, 16, 200, 100, true);
            Index syncIndex = Index.synchronizedIndex(i1);
            syncIndex.clear();
            //has to fail as i1 was cleared through syncIndex
            i1.addItem(HnswlibTestUtils.getRandomFloatArray(50));
            });
        }

    @Test
    public void testComputeSimilarity()
        {
        Index index = createIndexInstance(SpaceName.COSINE, 2);
        index.initialize();
        float similarityClose = index.computeSimilarity(
                new float[] {1F, 2F},
                new float[] {1F, 3F}
        );
        float similarityFar = index.computeSimilarity(
                new float[] {1F, 100F},
                new float[] {50F, 450F}
        );
        // both values are minus, so the closer one should be closer to zero than the farther one
        assertEquals(Float.compare(similarityClose, similarityFar), 1);
        }

    @Test
    public void testComputeSimilarityWhenNotInitialized()
        {
        assertThrows(IndexNotInitializedException.class, () ->
            {
            Index index = createIndexInstance(SpaceName.COSINE, 2);
            index.computeSimilarity(
                    new float[] {1F, 100F},
                    new float[] {50F, 450F});
            });
        }

    }
