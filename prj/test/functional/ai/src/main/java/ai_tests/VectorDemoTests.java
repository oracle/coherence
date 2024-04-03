/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package ai_tests;

import com.oracle.coherence.ai.QueryResult;
import com.oracle.coherence.ai.Vector;
import com.oracle.coherence.ai.VectorStore;

import com.oracle.coherence.ai.queries.Cosine;
import com.oracle.coherence.ai.queries.Jaccard;

import com.tangosol.coherence.config.Config;

import com.tangosol.net.Session;

import io.jhdf.HdfFile;

import io.jhdf.api.Attribute;
import io.jhdf.api.Dataset;

import io.jhdf.api.dataset.ContiguousDataset;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * This test class is to demo the {@link VectorStore}.
 * <p>
 * The tests use test data files from here
 * <a href="https://github.com/erikbern/ann-benchmarks/?tab=readme-ov-file">ann-benchmarks</a>
 * which need to be downloaded before the test will run. If the files are missing the tests are skipped.
 * <p>
 * Files should be downloaded to "~/Downloads/__filename__"
 * <p>
 * The Jaccard test uses the <a href="http://ann-benchmarks.com/movielens10m-jaccard.hdf5">MovieLens-10M</a> file
 * <p>
 * The Cosine test uses the <a href="http://ann-benchmarks.com/nytimes-256-angular.hdf5">NYTimes</a> file
 * <p>
 * The Euclidean test uses the <a href="http://ann-benchmarks.com/mnist-784-euclidean.hdf5">MNIST</a> file
 */
public abstract class VectorDemoTests
    {
    /**
     * This test loads a test data file
     */
    @Test
    public void shouldRunJaccardSimilarity()
        {
        // Get the test data file
        HdfFile hdfFile = getJaccardTestFile();

        // Create the vector store wrapping the cache "jaccard-test"
        Session                            session = getSession();
        VectorStore<long[], Integer, Void> store   = VectorStore.ofLongs("jaccard-test", session);

        // Get the training data vectors to load to the store
        long[][] trainVectors = getLongVectors(hdfFile);

        // load the vectors to the store (cache) in batches of 1000
        long start = System.currentTimeMillis();
        store.add(trainVectors, Vector.KeySequence.ofInts(), 1000);
        long end = System.currentTimeMillis();
        System.out.println("Loaded " + trainVectors.length + " vectors in " + (end - start) + " millis");

        // Get the first test vector from the data file
        long[] testVector = getFirstLongTestVector(hdfFile);

        // Create a Jaccard query to find the nearest 100 vectors
        Jaccard<long[]> query = Jaccard.forLongs(testVector).withMaxResults(100).build();

        // Execute the query (the nearest vector is first in the results)
        start = System.currentTimeMillis();
        List<QueryResult<long[], Integer, Void>> results = store.query(query);
        end = System.currentTimeMillis();
        System.out.println("Executed Jaccard query in " + (end - start) + " millis");

        // assert we have 100 results
        assertThat(results, is(notNullValue()));
        assertThat(results.size(), is(100));

        // assert the results
        ContiguousDataset distances    = (ContiguousDataset) hdfFile.getDatasetByPath("distances");
        float[][]         distanceData = (float[][]) distances.getData();

        // results should match the order in the test data
        // we used the first test vector, so use the first distance vector
        float[] correctDistances = distanceData[0];
        int     index            = 0;
        for (QueryResult<long[], Integer, Void> result : results)
            {
            // The query result holds Jaccard similarity (closest to 1 is nearest)
            // The test data is Jaccard distance (lowest is nearest) which is 1 - similarity
            float expected = correctDistances[index];
            float actual   = 1 - result.getResult();
            assertThat(roundToFivePlaces(actual), is(roundToFivePlaces(expected)));
            index++;
            }
        }

    @Test
    public void shouldRunCosineSimilarity()
        {
        // Get the test data file
        HdfFile hdfFile = getCosineTestFile();

        // Create the vector store wrapping the cache "jaccard-test"
        Session                             session = getSession();
        VectorStore<float[], Integer, Void> store   = VectorStore.ofFloats("cosine-test", session);

        // Get the training data vectors to load to the store
        float[][] trainVectors = getFloatVectors(hdfFile);

        // load the vectors to the store (cache) in batches of 1000
        long start = System.currentTimeMillis();
        store.add(trainVectors, Vector.KeySequence.ofInts(), 1000);
        long end = System.currentTimeMillis();
        System.out.println("Loaded " + trainVectors.length + " vectors in " + (end - start) + " millis");

        // Get the first test vector from the data file
        float[] testVector = getFirstFloatTestVector(hdfFile);

        // Create a Cosine query to find the nearest 100 vectors
        Cosine<float[]> query = Cosine.forFloats(testVector).withMaxResults(100).build();

        // Execute the query (the nearest vector is first in the results)
        start = System.currentTimeMillis();
        List<QueryResult<float[], Integer, Void>> results = store.query(query);
        end = System.currentTimeMillis();
        System.out.println("Executed Cosine query in " + (end - start) + " millis");

        // assert we have 100 results
        assertThat(results, is(notNullValue()));
        assertThat(results.size(), is(100));

        assertDistances(hdfFile, results);
        }

    @Test
    @Disabled("We do not have a Euclidean algorithm yet")
    public void shouldRunEuclideanSimilarity()
        {
        // Get the test data file
        HdfFile hdfFile = getEuclideanTestFile();

        // Create the vector store wrapping the cache "jaccard-test"
        Session                             session = getSession();
        VectorStore<float[], Integer, Void> store   = VectorStore.ofFloats("euclidean-test", session);

        // Get the training data vectors to load to the store
        float[][] trainVectors = getFloatVectors(hdfFile);

        // iterate over the training vectors and load them to the store (cache)
        for (int vectorKey = 0; vectorKey < trainVectors.length; vectorKey++)
            {
            // load the test vector into the store
            store.addFloats(vectorKey, trainVectors[vectorKey]);
            }

        // Get the first test vector from the data file
        float[] testVector = getFirstFloatTestVector(hdfFile);

        // ToDo we do not have a Euclidean query yet...
        }


    protected void assertDistances(HdfFile hdfFile, List<? extends QueryResult<?, Integer, Void>> results)
        {
        ContiguousDataset distances    = (ContiguousDataset) hdfFile.getDatasetByPath("distances");
        float[][]         distanceData = (float[][]) distances.getData();

        // results should match the order in the test data
        // we used the first test vector, so use the first distance vector
        float[] correctDistances = distanceData[0];
        int     index            = 0;
        for (QueryResult<?, Integer, Void> result : results)
            {
            // The query result holds similarity (closest to 1 is nearest)
            // The test data is distance (lowest is nearest) which is 1 - similarity
            float expected = correctDistances[index];
            float actual   = 1 - result.getResult();
            assertThat(roundToFivePlaces(actual), is(roundToFivePlaces(expected)));
            index++;
            }
        }

    /**
     * Load the array of long vectors from the training data file.
     *
     * @param hdfFile  the data file
     *
     * @return  the long training data vectors
     */
    protected long[][] getLongVectors(HdfFile hdfFile)
        {
        Dataset   trainDataset = hdfFile.getDatasetByPath("train");
        Attribute attribute    = hdfFile.getAttribute("type");
        String    type         = attribute == null ? null : String.valueOf(attribute.getData());
        if ("sparse".equals(type))
            {
            Dataset  sizeDataset  = hdfFile.getDatasetByPath("size_train");
            return loadLongData(trainDataset, sizeDataset);
            }
        return (long[][]) trainDataset.getData();
        }

    /**
     * Load the array of float vectors from the training data file.
     *
     * @param hdfFile  the data file
     *
     * @return  the float training data vectors
     */
    protected float[][] getFloatVectors(HdfFile hdfFile)
        {
        Dataset   trainDataset = hdfFile.getDatasetByPath("train");
        Attribute attribute    = hdfFile.getAttribute("type");
        String    type         = attribute == null ? null : String.valueOf(attribute.getData());
        if ("sparse".equals(type))
            {
            Dataset  sizeDataset  = hdfFile.getDatasetByPath("size_train");
            return loadFloatData(trainDataset, sizeDataset);
            }
        return (float[][]) trainDataset.getData();
        }

    /**
     * Obtain the first test vector from the test data file.
     *
     * @param hdfFile  the test data file
     *
     * @return the first test vector
     */
    protected long[] getFirstLongTestVector(HdfFile hdfFile)
        {
        ContiguousDataset testDataset = (ContiguousDataset) hdfFile.getDatasetByPath("test");
        Attribute         attribute   = hdfFile.getAttribute("type");
        String            type        = attribute == null ? null : String.valueOf(attribute.getData());

        long[][] testData;
        if ("sparse".equals(type))
            {
            Dataset sizeTest = hdfFile.getDatasetByPath("size_test");
            testData = loadLongData(testDataset, sizeTest);
            }
        else
            {
            testData = (long[][]) testDataset.getData();
            }
        return testData[0];
        }

    /**
     * Obtain the first test vector from the test data file.
     *
     * @param hdfFile  the test data file
     *
     * @return the first test vector
     */
    protected float[] getFirstFloatTestVector(HdfFile hdfFile)
        {
        ContiguousDataset testDataset = (ContiguousDataset) hdfFile.getDatasetByPath("test");
        Attribute         attribute   = hdfFile.getAttribute("type");
        String            type        = attribute == null ? null : String.valueOf(attribute.getData());

        float[][] testData;
        if ("sparse".equals(type))
            {
            Dataset sizeTest = hdfFile.getDatasetByPath("size_test");
            testData = loadFloatData(testDataset, sizeTest);
            }
        else
            {
            testData = (float[][]) testDataset.getData();
            }
        return testData[0];
        }

    /**
     * Load a HDF5 data file to use for a Jaccard similarity test.
     *
     * @return the loaded data file.
     */
    private HdfFile getJaccardTestFile()
        {
        HdfFile hdfFile = getTestDataFile("movielens10m-jaccard.hdf5");
        String  algo    = String.valueOf(hdfFile.getAttribute("distance").getData());

        assertThat("Test data is not for a Jaccard test", algo, is("jaccard"));
        return hdfFile;
        }

    /**
     * Load a HDF5 data file to use for a Euclidean similarity test.
     *
     * @return the loaded data file.
     */
    private HdfFile getCosineTestFile()
        {
        HdfFile hdfFile = getTestDataFile("nytimes-256-angular.hdf5");
        String  algo    = String.valueOf(hdfFile.getAttribute("distance").getData());

        assertThat("Test data is not for a Cosine test", algo, is("angular"));
        return hdfFile;
        }

    /**
     * Load a HDF5 data file to use for a Euclidean similarity test.
     *
     * @return the loaded data file.
     */
    private HdfFile getEuclideanTestFile()
        {
        HdfFile hdfFile = getTestDataFile("fashion-mnist-784-euclidean.hdf5");
        String  algo    = String.valueOf(hdfFile.getAttribute("distance").getData());

        assertThat("Test data is not for a Euclidean test", algo, is("euclidean"));
        return hdfFile;
        }

    /**
     * Load a HDF5 data file.
     *
     * @param sDefaultName  the default name of the file to load
     *
     * @return the loaded data file
     */
    HdfFile getTestDataFile(String sDefaultName)
        {
        File   dataFile  = null;
        String sFilename = Config.getProperty("coherence.ai.test.data.jaccard");
        if (sFilename == null || sFilename.isBlank())
            {
            String sUserHome   = System.getProperty("user.home");
            if (sUserHome != null)
                {
                File dirUserHome  = new File(sUserHome);
                File dirDownloads = new File(dirUserHome, "Downloads");
                dataFile = new File(dirDownloads, sDefaultName);
                }
            }
        else
            {
            dataFile = new File(sFilename);
            }

        Assumptions.assumeTrue(dataFile != null, "test skipped, no hdf5 data file found");
        Assumptions.assumeTrue(dataFile.exists(), "test skipped, hdf5 data file " + dataFile + " does not exist");
        Assumptions.assumeTrue(dataFile.isFile(), "test skipped, hdf5 data file " + dataFile + " is not a file");

        return new HdfFile(dataFile);
        }

    /**
     * Load an array of arrays of longs from a dataset.
     *
     * @param dataset  the {@link Dataset} containing the array data
     * @param sizes    the {@link Dataset} containing the array sizes
     *
     * @return an array of arrays of long values
     */
    long[][] loadLongData(Dataset dataset, Dataset sizes)
        {
        long     size     = sizes.getSize();
        long     index    = 0;
        long[][] array    = new long[(int) size][];
        long[]   position = new long[1];
        int[]    length   = new int[1];

        for (long i = 0; i < size; i++)
            {
            position[0] = i;
            length[0]   = 1;
            int[] num = (int[]) sizes.getData(position, length);
            position[0] = index;
            length[0] = num[0];
            index += num[0];
            long[] data = (long[]) dataset.getData(position, length);
            array[(int) i] = data;
            }

        return array;
        }

    /**
     * Load an array of arrays of floats from a dataset.
     *
     * @param dataset  the {@link Dataset} containing the array data
     * @param sizes    the {@link Dataset} containing the array sizes
     *
     * @return an array of arrays of floats values
     */
    float[][] loadFloatData(Dataset dataset, Dataset sizes)
        {
        long      size     = sizes.getSize();
        long      index    = 0;
        float[][] array    = new float[(int) size][];
        long[]    position = new long[1];
        int[]     length   = new int[1];

        for (long i = 0; i < size; i++)
            {
            position[0] = i;
            length[0]   = 1;
            int[] num = (int[]) sizes.getData(position, length);
            position[0] = index;
            length[0] = num[0];
            index += num[0];
            float[] data = (float[]) dataset.getData(position, length);
            array[(int) i] = data;
            }

        return array;
        }

    /**
     * Round a float to five decimal places.
     *
     * @param f  the float to round
     *
     * @return the float rounded to five decimal places
     */
    float roundToFivePlaces(float f)
        {
        return (float) Math.round(f * 100000) / 100000.0f;
        }

    /**
     * Round a float to four decimal places.
     *
     * @param f  the float to round
     *
     * @return the float rounded to four decimal places
     */
    float roundToFourPlaces(float f)
        {
        return (float) Math.round(f * 10000) / 10000.0f;
        }

    /**
     * Return the Coherence {@link Session} to use for the {@link VectorStore}.
     *
     * @return the Coherence {@link Session} to use for the {@link VectorStore}
     */
    abstract Session getSession();

    // ----- data members ---------------------------------------------------
    
    /**
     * The test cluster name.
     */
    public static final String CLUSTER_NAME = "vector-demo";
    }
