/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.util;

/**
 * A record representing a retrieval plan for distributed document search.
 * <p/>
 * This record encapsulates the parameters needed to execute a distributed
 * search across multiple shards in the Coherence RAG framework. It determines
 * how many documents to retrieve from each shard and how many candidates
 * to consider for reranking to optimize both search quality and performance.
 * <p/>
 * The retrieval plan balances the trade-off between recall (finding all
 * relevant documents) and efficiency (minimizing network traffic and
 * computation overhead) in a distributed search environment.
 * 
 * @author Aleks Seovic  2025.07.04
 * @since 25.09
 * 
 * @param perShard   the number of documents to retrieve from each shard
 * @param rerankSize the total number of candidates to consider for reranking
 */
public record ShardRetrievalPlan(int perShard, int rerankSize)
    {
    /**
     * Computes an optimized retrieval plan for distributed search.
     * <p/>
     * This method calculates the optimal number of documents to retrieve
     * from each shard and the size of the candidate set for reranking,
     * based on the desired number of final results and the characteristics
     * of the distributed system.
     * <p/>
     * The algorithm ensures that enough documents are retrieved from each
     * shard to maintain high recall while keeping the reranking candidate
     * set manageable for performance.
     * 
     * @param topK            the number of final results desired by the user
     * @param numShards       the number of independent shards in the system
     * @param minPerShard     the minimum number of documents to retrieve per shard,
     *                        ensures minimum recall even for small topK values
     * @param maxRerank       the maximum number of candidates for reranking,
     *                        caps computational overhead
     * @param shardAggression multiplier for shard depth calculation,
     *                        higher values retrieve more docs per shard
     * @param rerankFactor    multiplier for rerank candidate set size,
     *                        higher values provide better reranking quality
     * 
     * @return a ShardRetrievalPlan with optimized retrieval parameters
     */
    public static ShardRetrievalPlan compute(int topK,
                               int numShards,
                               int minPerShard,
                               int maxRerank,
                               double shardAggression,
                               double rerankFactor)
        {
        int perShard   = Math.max(minPerShard, (int) Math.ceil(shardAggression * topK / (double) numShards));
        int rerankSize = Math.min(maxRerank, Math.max(topK, (int) (rerankFactor * topK)));

        return new ShardRetrievalPlan(perShard, rerankSize);
        }

    /**
     * Computes a retrieval plan using sensible default values.
     * <p/>
     * This convenience method uses empirically-derived default values
     * that work well for most RAG use cases:
     * <ul>
     *   <li>minPerShard = 2 (ensures minimum recall)</li>
     *   <li>maxRerank = 500 (reasonable computational limit)</li>
     *   <li>shardAggression = 2.0 (moderate over-retrieval)</li>
     *   <li>rerankFactor = 5.0 (good reranking candidate diversity)</li>
     * </ul>
     * 
     * @param topK the number of final results desired by the user
     * @param numShards the number of independent shards in the system
     * 
     * @return a ShardRetrievalPlan with default-optimized retrieval parameters
     */
    public static ShardRetrievalPlan compute(int topK, int numShards)
        {
        return compute(topK, numShards, 2, 500, 2.0, 5.0);
        }
    }
