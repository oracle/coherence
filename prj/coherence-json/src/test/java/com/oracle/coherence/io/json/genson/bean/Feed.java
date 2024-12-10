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

package com.oracle.coherence.io.json.genson.bean;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Feed {
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((alternates == null) ? 0 : alternates.hashCode());
    result = prime * result + ((description == null) ? 0 : description.hashCode());
    result = prime * result + ((id == null) ? 0 : id.hashCode());
    result = prime * result + ((items == null) ? 0 : items.hashCode());
    result = prime * result + ((title == null) ? 0 : title.hashCode());
    result = prime * result + (int) (updated ^ (updated >>> 32));
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof Feed)) {
      return false;
    }
    Feed other = (Feed) obj;
    if (alternates == null) {
      if (other.alternates != null) {
        return false;
      }
    } else if (!alternates.equals(other.alternates)) {
      return false;
    }
    if (description == null) {
      if (other.description != null) {
        return false;
      }
    } else if (!description.equals(other.description)) {
      return false;
    }
    if (id == null) {
      if (other.id != null) {
        return false;
      }
    } else if (!id.equals(other.id)) {
      return false;
    }
    if (items == null) {
      if (other.items != null) {
        return false;
      }
    } else if (!items.equals(other.items)) {
      return false;
    }
    if (title == null) {
      if (other.title != null) {
        return false;
      }
    } else if (!title.equals(other.title)) {
      return false;
    }
    if (updated != other.updated) {
      return false;
    }
    return true;
  }

  @JsonProperty
  String id;
  @JsonProperty
  String title;
  @JsonProperty
  String description;
  @com.oracle.coherence.io.json.genson.annotation.JsonProperty("alternate")
  @JsonProperty("alternate")
  List<Link> alternates;
  @JsonProperty
  long updated;
  @JsonProperty
  List<Item> items;

  @Override
  public String toString() {
    StringBuilder result = new StringBuilder().append(id).append("\n").append(title)
      .append("\n").append(description).append("\n").append(alternates).append("\n")
      .append(updated);
    int i = 1;
    for (Item item : items) {
      result.append(i++).append(": ").append(item).append("\n\n");
    }
    return result.toString();
  }
}