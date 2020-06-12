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

@SuppressWarnings("serial")
public class MediaContent implements java.io.Serializable {
  public Media media;
  public List<Image> images;

  public MediaContent() {
  }

  public MediaContent(Media media, List<Image> images) {
    this.media = media;
    this.images = images;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    MediaContent that = (MediaContent) o;

    if (images != null ? !images.equals(that.images) : that.images != null) return false;
    if (media != null ? !media.equals(that.media) : that.media != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = media != null ? media.hashCode() : 0;
    result = 31 * result + (images != null ? images.hashCode() : 0);
    return result;
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("[MediaContent: ");
    sb.append("media=").append(media);
    sb.append(", images=").append(images);
    sb.append("]");
    return sb.toString();
  }

  public void setMedia(Media media) {
    this.media = media;
  }

  public void setImages(List<Image> images) {
    this.images = images;
  }

  public Media getMedia() {
    return media;
  }

  public List<Image> getImages() {
    return images;
  }
}
