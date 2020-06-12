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

package com.oracle.coherence.io.json.genson;


import java.util.Objects;

/**
 * @author Aleksandar Seovic  2018.06.03
 */
@FunctionalInterface
public interface Modifier<T> {
    T apply(T obj);

   default Modifier<T> andThen(Modifier<T> after) {
        Objects.requireNonNull(after);
        return t -> after.apply(apply(t));
    }

    static <T> Modifier<T> identity() {
        return t -> t;
    }
}