/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package hnswlib;

import com.oracle.coherence.hnswlib.ConcurrentIndex;
import com.oracle.coherence.hnswlib.Index;
import com.oracle.coherence.hnswlib.QueryTuple;
import com.oracle.coherence.hnswlib.SpaceName;
import com.oracle.coherence.hnswlib.exception.UnexpectedNativeException;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings({"resource", "CallToPrintStackTrace"})
public class ConcurrentIndexTest extends AbstractIndexTest
    {
    @Test
    public void testConcurrentInsertQuery()
            throws InterruptedException, UnexpectedNativeException
        {
        ExecutorService executorService = Executors.newFixedThreadPool(50);

        Index i1 = createIndexInstance(SpaceName.L2, 50);
        i1.initialize(1_050);

        float[] randomFloatArray = HnswlibTestUtils.getRandomFloatArray(50);

        Runnable addItemIndex1 = () ->
            {
            try
                {
                i1.addItem(randomFloatArray);
                }
            catch (UnexpectedNativeException e)
                {
                e.printStackTrace();
                }
            };

        Runnable queryItemIndex1 = () ->
            {
            QueryTuple queryTuple;
            try
                {
                queryTuple = i1.knnQuery(randomFloatArray, 1);
                assertEquals(50, queryTuple.getIds().length);
                assertEquals(50, queryTuple.getCoefficients().length);
                }
            catch (UnexpectedNativeException e)
                {
                e.printStackTrace();
                }
            };

        for (int i = 0; i < 1_000; i++)
            {
            executorService.submit(addItemIndex1);
            executorService.submit(queryItemIndex1);
            }

        executorService.shutdown();
        executorService.awaitTermination(5, TimeUnit.MINUTES);

        assertEquals(1_000, i1.getLength());
        i1.clear();
        }

    @Override
    protected Index createIndexInstance(SpaceName spaceName, int dimensions)
        {
        return new ConcurrentIndex(spaceName, dimensions);
        }
    }
