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

package com.oracle.coherence.io.json.genson.ext.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.json.bind.annotation.JsonbCreator;
import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.annotation.JsonbTransient;

import java.util.Objects;

public class Data {

  public static class JsonPropertyOnField {

    @JsonbProperty
    @JsonProperty
    private String name;

    @SuppressWarnings("unused")
    public JsonPropertyOnField() {}
    public JsonPropertyOnField(final String name) {
      this.name = name;
    }

    @Override
    public boolean equals(final Object o) {
      if (this == o) return true;
      if (!(o instanceof JsonPropertyOnField)) return false;
      final JsonPropertyOnField that = (JsonPropertyOnField) o;
      return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
      return Objects.hash(name);
    }
  }

  public static class JsonPropertyOnGetterSetter {

    private String name;

    @SuppressWarnings("unused")
    public JsonPropertyOnGetterSetter() {}
    public JsonPropertyOnGetterSetter(final String name) {
      this.name = name;
    }

    @JsonbProperty
    @JsonProperty
    public String getName() {
      return name;
    }

    @JsonbProperty
    @JsonProperty
    public void setName(final String name) {
      this.name = name;
    }

    @Override
    public boolean equals(final Object o) {
      if (this == o) return true;
      if (!(o instanceof JsonPropertyOnGetterSetter)) return false;
      final JsonPropertyOnGetterSetter that = (JsonPropertyOnGetterSetter) o;
      return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
      return Objects.hash(name);
    }
  }

  public static class JsonPropertyOnFieldIgnored {

    @JsonbTransient
    @JsonIgnore
    private String name;

    @SuppressWarnings("WeakerAccess")
    public JsonPropertyOnFieldIgnored() {}
    public JsonPropertyOnFieldIgnored(final String name) {
      this.name = name;
    }

    @Override
    public boolean equals(final Object o) {
      if (this == o) return true;
      if (!(o instanceof JsonPropertyOnFieldIgnored)) return false;
      final JsonPropertyOnFieldIgnored that = (JsonPropertyOnFieldIgnored) o;
      return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
      return Objects.hash(name);
    }
  }

  public static class JsonPropertyOnGetterIgnoredSetter {

    private String name;

    @SuppressWarnings("unused")
    public JsonPropertyOnGetterIgnoredSetter() {}
    public JsonPropertyOnGetterIgnoredSetter(final String name) {
      this.name = name;
    }

    @JsonIgnore
    @JsonbTransient
    public String getName() {
      return name;
    }

    @JsonProperty
    @JsonbProperty
    public void setName(final String name) {
      this.name = name;
    }

    @Override
    public boolean equals(final Object o) {
      if (this == o) return true;
      if (!(o instanceof JsonPropertyOnGetterIgnoredSetter)) return false;
      final JsonPropertyOnGetterIgnoredSetter that = (JsonPropertyOnGetterIgnoredSetter) o;
      return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
      return Objects.hash(name);
    }
  }

  public static class JsonPropertyOnGetterSetterIgnored {

    private String name;

    @SuppressWarnings("WeakerAccess")
    public JsonPropertyOnGetterSetterIgnored() {}
    public JsonPropertyOnGetterSetterIgnored(final String name) {
      this.name = name;
    }

    @JsonbProperty
    @JsonProperty
    public String getName() {
      return name;
    }

    @JsonIgnore
    @JsonbTransient
    public void setName(final String name) {
      this.name = name;
    }

    @Override
    public boolean equals(final Object o) {
      if (this == o) return true;
      if (!(o instanceof JsonPropertyOnGetterSetterIgnored)) return false;
      final JsonPropertyOnGetterSetterIgnored that = (JsonPropertyOnGetterSetterIgnored) o;
      return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
      return Objects.hash(name);
    }
  }

  public static class JsonPropertyOnFieldCustomName {

    @JsonbProperty("n")
    @JsonProperty("n")
    private String name;

    @SuppressWarnings("unused")
    public JsonPropertyOnFieldCustomName() {}
    public JsonPropertyOnFieldCustomName(final String name) {
      this.name = name;
    }

    @Override
    public boolean equals(final Object o) {
      if (this == o) return true;
      if (!(o instanceof JsonPropertyOnFieldCustomName)) return false;
      final JsonPropertyOnFieldCustomName that = (JsonPropertyOnFieldCustomName) o;
      return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
      return Objects.hash(name);
    }
  }

  public static class JsonPropertyOnGetterSetterCustomName {

    private String name;

    @SuppressWarnings("unused")
    public JsonPropertyOnGetterSetterCustomName() {}
    public JsonPropertyOnGetterSetterCustomName(final String name) {
      this.name = name;
    }

    @JsonbProperty("n")
    @JsonProperty("n")
    public String getName() {
      return name;
    }

    @JsonbProperty("n")
    @JsonProperty("n")
    public void setName(final String name) {
      this.name = name;
    }

    @Override
    public boolean equals(final Object o) {
      if (this == o) return true;
      if (!(o instanceof JsonPropertyOnGetterSetterCustomName)) return false;
      final JsonPropertyOnGetterSetterCustomName that = (JsonPropertyOnGetterSetterCustomName) o;
      return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
      return Objects.hash(name);
    }
  }

  public static class JsonCreatorConstructor {

    @JsonbProperty
    @JsonProperty
    private String name;


    @JsonbCreator
    public JsonCreatorConstructor(@JsonbProperty("name") @JsonProperty("name") String name) {
      this.name = name;
    }

    @Override
    public boolean equals(final Object o) {
      if (this == o) return true;
      if (!(o instanceof JsonCreatorConstructor)) return false;
      final JsonCreatorConstructor that = (JsonCreatorConstructor) o;
      return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
      return Objects.hash(name);
    }
  }

  public static class JsonCreatorFactoryMethod {

    @JsonbProperty
    @JsonProperty
    private String name;

    private JsonCreatorFactoryMethod(String name) {
      this.name = name;
    }

    @JsonCreator
    @JsonbCreator
    public static JsonCreatorFactoryMethod newInstance(@JsonbProperty("name")  @JsonProperty("name") String name) {
      return new JsonCreatorFactoryMethod(name);
    }

    @Override
    public boolean equals(final Object o) {
      if (this == o) return true;
      if (!(o instanceof JsonCreatorFactoryMethod)) return false;
      final JsonCreatorFactoryMethod that = (JsonCreatorFactoryMethod) o;
      return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
      return Objects.hash(name);
    }
  }
}
