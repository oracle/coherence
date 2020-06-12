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

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonProperty;

public class User {
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (contributors_enabled ? 1231 : 1237);
    result = prime * result + ((created_at == null) ? 0 : created_at.hashCode());
    result = prime * result + (defalut_profile_image ? 1231 : 1237);
    result = prime * result + (default_profile ? 1231 : 1237);
    result = prime * result + ((description == null) ? 0 : description.hashCode());
    result = prime * result + favourites_count;
    result = prime * result + (follow_request_sent ? 1231 : 1237);
    result = prime * result + followers_count;
    result = prime * result + (following ? 1231 : 1237);
    result = prime * result + friends_count;
    result = prime * result + (geo_enabled ? 1231 : 1237);
    result = prime * result + (int) (id ^ (id >>> 32));
    result = prime * result + ((id_str == null) ? 0 : id_str.hashCode());
    result = prime * result + (isProtected ? 1231 : 1237);
    result = prime * result + (is_translator ? 1231 : 1237);
    result = prime * result + ((lang == null) ? 0 : lang.hashCode());
    result = prime * result + listed_count;
    result = prime * result + ((location == null) ? 0 : location.hashCode());
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    result = prime * result + (notifications ? 1231 : 1237);
    result = prime
      * result
      + ((profile_background_color == null) ? 0 : profile_background_color.hashCode());
    result = prime
      * result
      + ((profile_background_image_url == null) ? 0 : profile_background_image_url
      .hashCode());
    result = prime * result + (profile_background_tile ? 1231 : 1237);
    result = prime * result
      + ((profile_image_url == null) ? 0 : profile_image_url.hashCode());
    result = prime * result
      + ((profile_link_color == null) ? 0 : profile_link_color.hashCode());
    result = prime
      * result
      + ((profile_sidebar_border_color == null) ? 0 : profile_sidebar_border_color
      .hashCode());
    result = prime
      * result
      + ((profile_sidebar_fill_color == null) ? 0 : profile_sidebar_fill_color
      .hashCode());
    result = prime * result
      + ((profile_text_color == null) ? 0 : profile_text_color.hashCode());
    result = prime * result + (profile_use_background_image ? 1231 : 1237);
    result = prime * result + ((screen_name == null) ? 0 : screen_name.hashCode());
    result = prime * result + (show_all_inline_media ? 1231 : 1237);
    result = prime * result + statuses_count;
    result = prime * result + ((time_zone == null) ? 0 : time_zone.hashCode());
    result = prime * result + ((url == null) ? 0 : url.hashCode());
    result = prime * result + (int) (utc_offset ^ (utc_offset >>> 32));
    result = prime * result + (verified ? 1231 : 1237);
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
    if (!(obj instanceof User)) {
      return false;
    }
    User other = (User) obj;
    if (contributors_enabled != other.contributors_enabled) {
      return false;
    }
    if (created_at == null) {
      if (other.created_at != null) {
        return false;
      }
    } else if (!created_at.equals(other.created_at)) {
      return false;
    }
    if (defalut_profile_image != other.defalut_profile_image) {
      return false;
    }
    if (default_profile != other.default_profile) {
      return false;
    }
    if (description == null) {
      if (other.description != null) {
        return false;
      }
    } else if (!description.equals(other.description)) {
      return false;
    }
    if (favourites_count != other.favourites_count) {
      return false;
    }
    if (follow_request_sent != other.follow_request_sent) {
      return false;
    }
    if (followers_count != other.followers_count) {
      return false;
    }
    if (following != other.following) {
      return false;
    }
    if (friends_count != other.friends_count) {
      return false;
    }
    if (geo_enabled != other.geo_enabled) {
      return false;
    }
    if (id != other.id) {
      return false;
    }
    if (id_str == null) {
      if (other.id_str != null) {
        return false;
      }
    } else if (!id_str.equals(other.id_str)) {
      return false;
    }
    if (isProtected != other.isProtected) {
      return false;
    }
    if (is_translator != other.is_translator) {
      return false;
    }
    if (lang == null) {
      if (other.lang != null) {
        return false;
      }
    } else if (!lang.equals(other.lang)) {
      return false;
    }
    if (listed_count != other.listed_count) {
      return false;
    }
    if (location == null) {
      if (other.location != null) {
        return false;
      }
    } else if (!location.equals(other.location)) {
      return false;
    }
    if (name == null) {
      if (other.name != null) {
        return false;
      }
    } else if (!name.equals(other.name)) {
      return false;
    }
    if (notifications != other.notifications) {
      return false;
    }
    if (profile_background_color == null) {
      if (other.profile_background_color != null) {
        return false;
      }
    } else if (!profile_background_color.equals(other.profile_background_color)) {
      return false;
    }
    if (profile_background_image_url == null) {
      if (other.profile_background_image_url != null) {
        return false;
      }
    } else if (!profile_background_image_url.equals(other.profile_background_image_url)) {
      return false;
    }
    if (profile_background_tile != other.profile_background_tile) {
      return false;
    }
    if (profile_image_url == null) {
      if (other.profile_image_url != null) {
        return false;
      }
    } else if (!profile_image_url.equals(other.profile_image_url)) {
      return false;
    }
    if (profile_link_color == null) {
      if (other.profile_link_color != null) {
        return false;
      }
    } else if (!profile_link_color.equals(other.profile_link_color)) {
      return false;
    }
    if (profile_sidebar_border_color == null) {
      if (other.profile_sidebar_border_color != null) {
        return false;
      }
    } else if (!profile_sidebar_border_color.equals(other.profile_sidebar_border_color)) {
      return false;
    }
    if (profile_sidebar_fill_color == null) {
      if (other.profile_sidebar_fill_color != null) {
        return false;
      }
    } else if (!profile_sidebar_fill_color.equals(other.profile_sidebar_fill_color)) {
      return false;
    }
    if (profile_text_color == null) {
      if (other.profile_text_color != null) {
        return false;
      }
    } else if (!profile_text_color.equals(other.profile_text_color)) {
      return false;
    }
    if (profile_use_background_image != other.profile_use_background_image) {
      return false;
    }
    if (screen_name == null) {
      if (other.screen_name != null) {
        return false;
      }
    } else if (!screen_name.equals(other.screen_name)) {
      return false;
    }
    if (show_all_inline_media != other.show_all_inline_media) {
      return false;
    }
    if (statuses_count != other.statuses_count) {
      return false;
    }
    if (time_zone == null) {
      if (other.time_zone != null) {
        return false;
      }
    } else if (!time_zone.equals(other.time_zone)) {
      return false;
    }
    if (url == null) {
      if (other.url != null) {
        return false;
      }
    } else if (!url.equals(other.url)) {
      return false;
    }
    if (utc_offset != other.utc_offset) {
      return false;
    }
    if (verified != other.verified) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "User [name=" + name + ", profile_sidebar_border_color="
      + profile_sidebar_border_color + ", profile_background_tile="
      + profile_background_tile + ", profile_sidebar_fill_color="
      + profile_sidebar_fill_color + ", created_at=" + created_at + ", location="
      + location + ", profile_image_url=" + profile_image_url + ", follow_request_sent="
      + follow_request_sent + ", profile_link_color=" + profile_link_color
      + ", is_translator=" + is_translator + ", id_str=" + id_str + ", favourites_count="
      + favourites_count + ", contributors_enabled=" + contributors_enabled + ", url="
      + url + ", default_profile=" + default_profile + ", utc_offset=" + utc_offset
      + ", id=" + id + ", profile_use_background_image=" + profile_use_background_image
      + ", listed_count=" + listed_count + ", lang=" + lang + ", isProtected="
      + isProtected + ", followers_count=" + followers_count + ", profile_text_color="
      + profile_text_color + ", profile_background_color=" + profile_background_color
      + ", time_zone=" + time_zone + ", description=" + description + ", notifications="
      + notifications + ", geo_enabled=" + geo_enabled + ", verified=" + verified
      + ", profile_background_image_url=" + profile_background_image_url
      + ", defalut_profile_image=" + defalut_profile_image + ", friends_count="
      + friends_count + ", statuses_count=" + statuses_count + ", screen_name="
      + screen_name + ", following=" + following + ", show_all_inline_media="
      + show_all_inline_media + "]";
  }

  @JsonProperty
  String name;
  @JsonProperty
  String profile_sidebar_border_color;
  @JsonProperty
  boolean profile_background_tile;
  @JsonProperty
  String profile_sidebar_fill_color;
  @JsonProperty
  Date created_at;
  @JsonProperty
  String location;
  @JsonProperty
  String profile_image_url;
  @JsonProperty
  boolean follow_request_sent;
  @JsonProperty
  String profile_link_color;
  @JsonProperty
  boolean is_translator;
  @JsonProperty
  String id_str;
  @JsonProperty
  int favourites_count;
  @JsonProperty
  boolean contributors_enabled;
  @JsonProperty
  String url;
  @JsonProperty
  boolean default_profile;
  @JsonProperty
  long utc_offset;
  @JsonProperty
  long id;
  @JsonProperty
  boolean profile_use_background_image;
  @JsonProperty
  int listed_count;
  @JsonProperty
  String lang;
  @com.oracle.coherence.io.json.genson.annotation.JsonProperty("protected")
  @JsonProperty("protected")
  boolean isProtected;
  @JsonProperty
  int followers_count;
  @JsonProperty
  String profile_text_color;
  @JsonProperty
  String profile_background_color;
  @JsonProperty
  String time_zone;
  @JsonProperty
  String description;
  @JsonProperty
  boolean notifications;
  @JsonProperty
  boolean geo_enabled;
  @JsonProperty
  boolean verified;
  @JsonProperty
  String profile_background_image_url;
  @JsonProperty
  boolean defalut_profile_image;
  @JsonProperty
  int friends_count;
  @JsonProperty
  int statuses_count;
  @JsonProperty
  String screen_name;
  @JsonProperty
  boolean following;
  @JsonProperty
  boolean show_all_inline_media;
}