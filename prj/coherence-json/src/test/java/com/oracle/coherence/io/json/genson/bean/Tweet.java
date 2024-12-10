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

public class Tweet {
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((contributors == null) ? 0 : contributors.hashCode());
    result = prime * result + ((coordinates == null) ? 0 : coordinates.hashCode());
    result = prime * result + ((created_at == null) ? 0 : created_at.hashCode());
    result = prime * result + (favorited ? 1231 : 1237);
    result = prime * result + ((geo == null) ? 0 : geo.hashCode());
    result = prime * result + (int) (id ^ (id >>> 32));
    result = prime * result + ((id_str == null) ? 0 : id_str.hashCode());
    result = prime * result
      + ((in_reply_to_id_str == null) ? 0 : in_reply_to_id_str.hashCode());
    result = prime * result
      + ((in_reply_to_screen_name == null) ? 0 : in_reply_to_screen_name.hashCode());
    result = prime
      * result
      + ((in_reply_to_status_id_str == null) ? 0 : in_reply_to_status_id_str
      .hashCode());
    result = prime * result
      + ((in_reply_to_user_id == null) ? 0 : in_reply_to_user_id.hashCode());
    result = prime * result
      + ((in_reply_to_user_id_str == null) ? 0 : in_reply_to_user_id_str.hashCode());
    result = prime * result + ((place == null) ? 0 : place.hashCode());
    result = prime * result + ((retweet_count == null) ? 0 : retweet_count.hashCode());
    result = prime * result + (retweeted ? 1231 : 1237);
    result = prime * result
      + ((retweeted_status == null) ? 0 : retweeted_status.hashCode());
    result = prime * result + ((source == null) ? 0 : source.hashCode());
    result = prime * result + ((text == null) ? 0 : text.hashCode());
    result = prime * result + (truncated ? 1231 : 1237);
    result = prime * result + ((user == null) ? 0 : user.hashCode());
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
    if (!(obj instanceof Tweet)) {
      return false;
    }
    Tweet other = (Tweet) obj;
    if (contributors == null) {
      if (other.contributors != null) {
        return false;
      }
    } else if (!contributors.equals(other.contributors)) {
      return false;
    }
    if (coordinates == null) {
      if (other.coordinates != null) {
        return false;
      }
    } else if (!coordinates.equals(other.coordinates)) {
      return false;
    }
    if (created_at == null) {
      if (other.created_at != null) {
        return false;
      }
    } else if (!created_at.equals(other.created_at)) {
      return false;
    }
    if (favorited != other.favorited) {
      return false;
    }
    if (geo == null) {
      if (other.geo != null) {
        return false;
      }
    } else if (!geo.equals(other.geo)) {
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
    if (in_reply_to_id_str == null) {
      if (other.in_reply_to_id_str != null) {
        return false;
      }
    } else if (!in_reply_to_id_str.equals(other.in_reply_to_id_str)) {
      return false;
    }
    if (in_reply_to_screen_name == null) {
      if (other.in_reply_to_screen_name != null) {
        return false;
      }
    } else if (!in_reply_to_screen_name.equals(other.in_reply_to_screen_name)) {
      return false;
    }
    if (in_reply_to_status_id_str == null) {
      if (other.in_reply_to_status_id_str != null) {
        return false;
      }
    } else if (!in_reply_to_status_id_str.equals(other.in_reply_to_status_id_str)) {
      return false;
    }
    if (in_reply_to_user_id == null) {
      if (other.in_reply_to_user_id != null) {
        return false;
      }
    } else if (!in_reply_to_user_id.equals(other.in_reply_to_user_id)) {
      return false;
    }
    if (in_reply_to_user_id_str == null) {
      if (other.in_reply_to_user_id_str != null) {
        return false;
      }
    } else if (!in_reply_to_user_id_str.equals(other.in_reply_to_user_id_str)) {
      return false;
    }
    if (place == null) {
      if (other.place != null) {
        return false;
      }
    } else if (!place.equals(other.place)) {
      return false;
    }
    if (retweet_count == null) {
      if (other.retweet_count != null) {
        return false;
      }
    } else if (!retweet_count.equals(other.retweet_count)) {
      return false;
    }
    if (retweeted != other.retweeted) {
      return false;
    }
    if (retweeted_status == null) {
      if (other.retweeted_status != null) {
        return false;
      }
    } else if (!retweeted_status.equals(other.retweeted_status)) {
      return false;
    }
    if (source == null) {
      if (other.source != null) {
        return false;
      }
    } else if (!source.equals(other.source)) {
      return false;
    }
    if (text == null) {
      if (other.text != null) {
        return false;
      }
    } else if (!text.equals(other.text)) {
      return false;
    }
    if (truncated != other.truncated) {
      return false;
    }
    if (user == null) {
      if (other.user != null) {
        return false;
      }
    } else if (!user.equals(other.user)) {
      return false;
    }
    return true;
  }

  @JsonProperty
  String coordinates;
  @JsonProperty
  boolean favorited;
  @JsonProperty
  Date created_at;
  @JsonProperty
  boolean truncated;
  @JsonProperty
  Tweet retweeted_status;
  @JsonProperty
  String id_str;
  @JsonProperty
  String in_reply_to_id_str;
  @JsonProperty
  String contributors;
  @JsonProperty
  String text;
  @JsonProperty
  long id;
  @JsonProperty
  String retweet_count;
  @JsonProperty
  String in_reply_to_status_id_str;
  @JsonProperty
  Object geo;
  @JsonProperty
  boolean retweeted;
  @JsonProperty
  String in_reply_to_user_id;
  @JsonProperty
  String in_reply_to_screen_name;
  @JsonProperty
  Object place;
  @JsonProperty
  User user;
  @JsonProperty
  String source;
  @JsonProperty
  String in_reply_to_user_id_str;

  @Override
  public String toString() {
    return "Tweet [coordinates=" + coordinates + ", favorited=" + favorited + ", created_at="
      + created_at + ", truncated=" + truncated + ", retweeted_status="
      + retweeted_status + ", id_str=" + id_str + ", in_reply_to_id_str="
      + in_reply_to_id_str + ", contributors=" + contributors + ", text=" + text
      + ", id=" + id + ", retweet_count=" + retweet_count
      + ", in_reply_to_status_id_str=" + in_reply_to_status_id_str + ", geo=" + geo
      + ", retweeted=" + retweeted + ", in_reply_to_user_id=" + in_reply_to_user_id
      + ", in_reply_to_screen_name=" + in_reply_to_screen_name + ", place=" + place
      + ", user=" + user + ", source=" + source + ", in_reply_to_user_id_str="
      + in_reply_to_user_id_str + "]";
  }
}