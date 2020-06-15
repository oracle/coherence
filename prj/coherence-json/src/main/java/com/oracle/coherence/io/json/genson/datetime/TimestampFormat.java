/*
 * Copyright (c) 2019 Oracle and/or its affiliates. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Copyright (c) 2020, Oracle and/or its affiliates.
 *
 * Copyright 2011-2014 Genson - Cepoi Eugen
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License.
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.oracle.coherence.io.json.genson.datetime;


/**
 * The different formats that can be used when serializing/deserializing a {@link java.time.temporal.TemporalAccessor}
 * or {@link java.time.temporal.TemporalAmount}.
 */
public enum TimestampFormat {
    /**
     * Values will be read/written as numbers, using milliseconds wherever applicable.
     */
    MILLIS,

    /**
     * Values will be read/written as numbers, using nanoseconds wherever applicable.
     */
    NANOS,

    /**
     * Values will be read/written as arrays whose elements consist of the fields contained within the object.
     */
    ARRAY,

    /**
     * Values will be read/written as objects whose attributes consist of the fields contained within the object.
     */
    OBJECT
}
