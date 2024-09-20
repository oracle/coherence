/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package grpc.proxy.routeguide;

import com.google.protobuf.util.JsonFormat;
import com.tangosol.util.Resources;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;

/**
 * Common utilities for the RouteGuide demo.
 */
public class RouteGuideUtil {
  private static final double COORD_FACTOR = 1e7;

  /**
   * Gets the latitude for the given point.
   */
  public static double getLatitude(Point location) {
    return location.getLatitude() / COORD_FACTOR;
  }

  /**
   * Gets the longitude for the given point.
   */
  public static double getLongitude(Point location) {
    return location.getLongitude() / COORD_FACTOR;
  }

  /**
   * Gets the default features file from classpath.
   */
  public static URL getDefaultFeaturesFile() {
    return Resources.findFileOrResource("route_guide_db.json", null);
  }

  /**
   * Parses the JSON input file containing the list of features.
   */
  public static List<Feature> parseFeatures(URL file) throws IOException {
    InputStream input = file.openStream();
    try {
      Reader reader = new InputStreamReader(input, Charset.forName("UTF-8"));
      try {
        FeatureDatabase.Builder database = FeatureDatabase.newBuilder();
        JsonFormat.parser().merge(reader, database);
        return database.getFeatureList();
      } finally {
        reader.close();
      }
    } finally {
      input.close();
    }
  }

  /**
   * Indicates whether the given feature exists (i.e. has a valid name).
   */
  public static boolean exists(Feature feature) {
    return feature != null && !feature.getName().isEmpty();
  }
}
