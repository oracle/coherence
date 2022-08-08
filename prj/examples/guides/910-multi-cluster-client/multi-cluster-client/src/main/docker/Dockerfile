# Copyright (c) 2022, Oracle and/or its affiliates.
#
# Licensed under the Universal Permissive License v 1.0 as shown at
# https://oss.oracle.com/licenses/upl.
#
FROM gcr.io/distroless/java11-debian11

# Configure the image's health check command
# Health checks will only work with Coherence 22.06 and later
HEALTHCHECK  --start-period=10s --interval=30s \
    CMD ["java", \
    "-cp", "/app/libs/coherence.jar", \
    "com.tangosol.util.HealthCheckClient", \
    "http://127.0.0.1:8080/ready", \
    "||", "exit", "1"]

# Expose the web-server port
EXPOSE 8080

# Set the entry point to be the Java command to run
ENTRYPOINT ["java", "-cp", "/app/classes:/app/libs/*", "com.tangosol.net.Coherence"]

# Set any environment variables
# Run Coherence as a client
ENV COHERENCE_CLIENT=remote-fixed
# Enable Coherence metics
ENV COHERENCE_METRICS_HTTP_ENABLED=true
# Set the Coherence log level to debug logging
ENV COHERENCE_LOG_LEVEL=9

# Copy all the application files into the /app directory in the image
# This is the default structure supported by the Coherence Operator
COPY app app
