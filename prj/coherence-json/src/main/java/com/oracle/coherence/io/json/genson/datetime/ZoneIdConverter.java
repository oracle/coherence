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


import java.lang.reflect.Type;
import java.time.ZoneId;

import com.oracle.coherence.io.json.genson.Context;
import com.oracle.coherence.io.json.genson.Converter;
import com.oracle.coherence.io.json.genson.Genson;
import com.oracle.coherence.io.json.genson.stream.ObjectReader;
import com.oracle.coherence.io.json.genson.stream.ObjectWriter;

class ZoneIdConverter<E extends ZoneId> implements Converter<E> {

    // ---- Converter interface ---------------------------------------------

    public void serialize(E object, ObjectWriter writer, Context ctx) throws Exception {
        writer.beginObject().writeString("zoneId", object.getId()).endObject();
    }

    @SuppressWarnings("unchecked")
    public E deserialize(ObjectReader reader, Context ctx) throws Exception {
        String zoneId = ZoneId.systemDefault().getId();

        reader.beginObject();
        while (reader.hasNext()) {
            reader.next();
            String name = reader.name();
            if ("zoneId".equals(name)) {
                zoneId = reader.valueAsString();
            }
        }
        reader.endObject();

        return (E) ZoneId.of(zoneId);
    }

    public static final class Factory implements com.oracle.coherence.io.json.genson.Factory<Converter<? extends ZoneId>> {
        public Converter<? extends ZoneId> create(Type type, Genson genson) {
            return new ZoneIdConverter<>();
        }
    }
}
