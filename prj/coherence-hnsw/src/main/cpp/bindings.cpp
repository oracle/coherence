/*
 * Copyright (c) 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

/*
 * Copyright 2017-2024 hnswlib - Yuri Malkov at al.
 * Copyright 2020-2021 hnswlib-jna - The Stepstone Group.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License.
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

/**
 * Simplified C binding for hnswlib able to work with JNA (Java Native Access)
 * in order to get a similar native performance on Java. This code is based on the python 
 * binding available at: https://github.com/nmslib/hnswlib/blob/master/python_bindings/bindings.cpp
 * 
 * Some modifications and simplifications have been done on the C side. 
 * The multi-threading support can be used and handled on the Java side.
 * 
 * This work is still in progress. Please feel free to contribute and give ideas.
 */

#include <iostream>
#include <atomic>
#include <cmath>
#include "hnswlib/hnswlib.h"

#if _WIN32
#define DLLEXPORT __declspec(dllexport)
#else
#define DLLEXPORT
#endif

#define EXTERN_C extern "C"

#define RESULT_SUCCESSFUL 0
#define RESULT_EXCEPTION_THROWN 1
#define RESULT_INDEX_ALREADY_INITIALIZED 2
#define RESULT_QUERY_CANNOT_RETURN 3
#define RESULT_ITEM_CANNOT_BE_INSERTED_INTO_THE_VECTOR_SPACE 4
#define RESULT_ONCE_INDEX_IS_CLEARED_IT_CANNOT_BE_REUSED 5
#define RESULT_GET_DATA_FAILED 6
#define RESULT_ID_NOT_IN_INDEX 7
#define RESULT_INDEX_NOT_INITIALIZED 8

#define TRY_CATCH_NO_INITIALIZE_CHECK_AND_RETURN_INT_BLOCK(block)    if (index_cleared) return RESULT_ONCE_INDEX_IS_CLEARED_IT_CANNOT_BE_REUSED;  int result_code = RESULT_SUCCESSFUL; try { block } catch (...) { result_code = RESULT_EXCEPTION_THROWN; }; return result_code;
#define TRY_CATCH_RETURN_INT_BLOCK(block)    if (!index_initialized) return RESULT_INDEX_NOT_INITIALIZED; TRY_CATCH_NO_INITIALIZE_CHECK_AND_RETURN_INT_BLOCK(block)

typedef bool (*filter_func) (hnswlib::labeltype);

class FilterWrapper: public hnswlib::BaseFilterFunctor {
    filter_func fn;

public:
    FilterWrapper(filter_func func) : fn(func) {}

    bool operator()(hnswlib::labeltype id) {
        return fn(id);
    }
};

template<typename dist_t, typename data_t=float>
class Index {
public:
    Index(const std::string &space_name, const int dim) :
            space_name(space_name), dim(dim) {
        if(space_name=="L2") {
            l2space = new hnswlib::L2Space(dim);
        } else if(space_name=="IP") {
            l2space = new hnswlib::InnerProductSpace(dim);
        } else if(space_name=="COSINE") {
            l2space = new hnswlib::InnerProductSpace(dim);
        }
        appr_alg = NULL;
        index_initialized = false;
        index_cleared = false;
    }

    int init_new_index(const size_t maxElements, const size_t M, const size_t efConstruction, const size_t random_seed, const bool allow_replace_deleted) {
        TRY_CATCH_NO_INITIALIZE_CHECK_AND_RETURN_INT_BLOCK({
            if (appr_alg) {
                return RESULT_INDEX_ALREADY_INITIALIZED;
            }
            appr_alg = new hnswlib::HierarchicalNSW<dist_t>(l2space, maxElements, M, efConstruction, random_seed, allow_replace_deleted);
            index_initialized = true;
        });
    }

    int set_ef(size_t ef) {
     	TRY_CATCH_RETURN_INT_BLOCK({
        	appr_alg->ef_ = ef;
    	});
    }

   	int get_ef() {
   		return appr_alg->ef_;
   	}

    int get_ef_construction() {
        return appr_alg->ef_construction_;
    }

    int get_M() {
        return appr_alg->M_;
    }

    int save_index(const std::string &path_to_index) {
        TRY_CATCH_RETURN_INT_BLOCK({
            appr_alg->saveIndex(path_to_index);
        });
    }

    int load_index(const std::string &path_to_index, size_t max_elements) {
        TRY_CATCH_NO_INITIALIZE_CHECK_AND_RETURN_INT_BLOCK({
            if (appr_alg) {
                std::cerr << "Warning: Calling load_index for an already initialized index. Old index is being deallocated.";
                delete appr_alg;
            }
            appr_alg = new hnswlib::HierarchicalNSW<dist_t>(l2space, path_to_index, false, max_elements);
        });
    }

    int add_item(float* item, int id, bool replaceDeleted = false) {
        TRY_CATCH_RETURN_INT_BLOCK({
            int max_elements = get_max_elements();
            if (get_current_count() >= max_elements) {
                if (max_elements < 0x7FFFFF)
                    resize_index(max_elements << 1);
                else
                    resize_index(max_elements + (max_elements >> 1));
            }
            int current_id = id != -1 ? id : incremental_id++;
            appr_alg->addPoint(item, current_id, replaceDeleted);
        });
    }

    int hasId(int id) {
    	TRY_CATCH_RETURN_INT_BLOCK({
    		int label_c;
			auto search = (appr_alg->label_lookup_.find(id));
			if (search == (appr_alg->label_lookup_.end()) || (appr_alg->isMarkedDeleted(search->second))) {
				return RESULT_ID_NOT_IN_INDEX;
			}
		});
    }

    int getDataById(int id, float* data, int dim) {
    	TRY_CATCH_RETURN_INT_BLOCK({
			int label_c;
			auto search = (appr_alg->label_lookup_.find(id));
			if (search == (appr_alg->label_lookup_.end()) || (appr_alg->isMarkedDeleted(search->second))) {
				return RESULT_ID_NOT_IN_INDEX;
			}
			label_c = search->second;
			char* data_ptrv = (appr_alg->getDataByInternalId(label_c));
			float* data_ptr = (float*) data_ptrv;
			for (int i = 0; i < dim; i++) {
				data[i] = *data_ptr;
				data_ptr += 1;
			}
		});
    }

    float compute_similarity(float* vector1, float* vector2) {
    	float similarity;
        try {
        	similarity = (appr_alg->fstdistfunc_(vector1, vector2, (appr_alg -> dist_func_param_)));
        } catch (...) {
        	similarity = NAN;
        }
    	return similarity;
    }

    int knn_query(float* input, int k, int* indices, /* output */ float* coefficients /* output */, hnswlib::BaseFilterFunctor* filter = nullptr) {
        std::priority_queue<std::pair<dist_t, hnswlib::labeltype >> result;
        TRY_CATCH_RETURN_INT_BLOCK({
            result = appr_alg->searchKnn((void*) input, k, filter);
            if (result.size() != k)
                return RESULT_QUERY_CANNOT_RETURN;
            for (int i = k - 1; i >= 0; i--) {
                auto &result_tuple = result.top();
                coefficients[i] = result_tuple.first;
                indices[i] = result_tuple.second;
                result.pop();
            }       
        });
    }

    int mark_deleted(int label) {
        TRY_CATCH_RETURN_INT_BLOCK({
        	appr_alg->markDelete(label);
        });
    }

    void resize_index(size_t new_size) {
        appr_alg->resizeIndex(new_size);
    }

    int get_max_elements() const {
        return appr_alg->max_elements_;
    }

    int get_current_count() const {
        return appr_alg->cur_element_count;
    }

    int clear_index() {
    	TRY_CATCH_NO_INITIALIZE_CHECK_AND_RETURN_INT_BLOCK({
			delete l2space;
			if (appr_alg)
				delete appr_alg;
			index_cleared = true;
        });
    }

    std::string space_name;
    int dim;
    bool index_cleared;
    bool index_initialized;
    std::atomic<unsigned long> incremental_id{0};
    hnswlib::HierarchicalNSW<dist_t> *appr_alg;
    hnswlib::SpaceInterface<float> *l2space;

    ~Index() {
        clear_index();
    }
};

EXTERN_C DLLEXPORT Index<float>* createNewIndex(char* spaceName, int dimension){
    Index<float>* index;
    try {
        index = new Index<float>(spaceName, dimension);
    } catch (...) {
    	index = NULL;
    }
    return index;
}

EXTERN_C DLLEXPORT int initNewIndex(Index<float>* index, int maxNumberOfElements, int M = 16, int efConstruction = 200, int randomSeed = 100, bool allow_replace_deleted = false) {
	return index->init_new_index(maxNumberOfElements, M, efConstruction, randomSeed, allow_replace_deleted);
} 

EXTERN_C DLLEXPORT int addItemToIndex(Index<float>* index, float* item, int label, bool replaceDeleted = false) {
    return index->add_item(item, label, replaceDeleted);
}

EXTERN_C DLLEXPORT int getIndexLength(Index<float>* index) {
    if (index->appr_alg) {
        return index->appr_alg->cur_element_count;
    } else {
        return 0;
    }
}

EXTERN_C DLLEXPORT int getMaxIndexLength(Index<float>* index) {
    return index->get_max_elements();
}

EXTERN_C DLLEXPORT void resizeIndex(Index<float>* index, int maxNumberOfElements) {
    index->resize_index(maxNumberOfElements);
}

EXTERN_C DLLEXPORT int saveIndexToPath(Index<float>* index, char* path) {
    std::string path_string(path);
    return index->save_index(path_string);
}

EXTERN_C DLLEXPORT int loadIndexFromPath(Index<float>* index, size_t maxNumberOfElements, char* path) {
    std::string path_string(path);
    return index->load_index(path_string, maxNumberOfElements);
}

EXTERN_C DLLEXPORT int knnQuery(Index<float>* index, float* input, int k, int* indices /* output */, float* coefficients /* output */) {
    return index->knn_query(input, k, indices, coefficients);
}

EXTERN_C DLLEXPORT int knnFilterQuery(Index<float>* index, float* input, int k, filter_func filter, int* indices /* output */, float* coefficients /* output */) {
    FilterWrapper fn(filter);
    return index->knn_query(input, k, indices, coefficients, &fn);
}

EXTERN_C DLLEXPORT int clearIndex(Index<float>* index) {
    return index->clear_index();
}

EXTERN_C DLLEXPORT int setEf(Index<float>* index, int ef) {
    return index->set_ef(ef);
}

EXTERN_C DLLEXPORT int getData(Index<float>* index, int id, float* vector, int dim) {
	return index->getDataById(id, vector, dim);
}

EXTERN_C DLLEXPORT int hasId(Index<float>* index, int id) {
	return index->hasId(id);
}

EXTERN_C DLLEXPORT float computeSimilarity(Index<float>* index, float* vector1, float* vector2) {
	return index->compute_similarity(vector1, vector2);
}

EXTERN_C DLLEXPORT int getM(Index<float>* index) {
    return index->get_M();
}

EXTERN_C DLLEXPORT int getEfConstruction(Index<float>* index) {
    return index->get_ef_construction();
}

EXTERN_C DLLEXPORT int getEf(Index<float>* index) {
    return index->get_ef();
}

EXTERN_C DLLEXPORT int markDeleted(Index<float>* index, int id) {
    return index->mark_deleted(id);
}

int main(){
    return RESULT_SUCCESSFUL;
}
