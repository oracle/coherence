#!/usr/bin/env bash
#
# Copyright (c) 2000, 2021, Oracle and/or its affiliates.
#
# Licensed under the Universal Permissive License v 1.0 as shown at
# http://oss.oracle.com/licenses/upl.
#

# ---------------------------------------------------------------------------
# This script determines whether Buildah is available locally and if it is
# runs the Coherence image builder script otherwise it starts Buildah inside
# a container and exports the images to the local Docker daemon.
# ---------------------------------------------------------------------------
set -x

BASEDIR=$(dirname "$0")

# Ensure the IMAGE_NAME has been set - this is the name of the image to build
if [ "${IMAGE_NAME}" == "" ]
then
  echo "ERROR: No IMAGE_NAME environment variable has been set"
  exit 1
fi


if [ "$1" == "PUSH" ]
then
  SCRIPT_NAME="${BASEDIR}/push-images.sh"
else
  SCRIPT_NAME="${BASEDIR}/build-images.sh"
  # Ensure the AMD_BASE_IMAGE has been set - this is the name of the base image for amd64
  if [ "${AMD_BASE_IMAGE}" == "" ]
  then
    echo "ERROR: No AMD_BASE_IMAGE environment variable has been set"
    exit 1
  fi
  # Ensure the ARM_BASE_IMAGE has been set - this is the name of the base image for arm64
  if [ "${ARM_BASE_IMAGE}" == "" ]
  then
    echo "ERROR: No ARM_BASE_IMAGE environment variable has been set"
    exit 1
  fi
fi

chmod +x ${SCRIPT_NAME}

which buildah
if [ "$?" == "0" ]
then
  echo "Running Buildah locally"
  if [ "${NO_DAEMON}" == "" ]
  then
    export NO_DAEMON=true
  fi
  sh "${SCRIPT_NAME}"
else
  echo "Buildah not found locally - running in container"
  if [ "${NO_DAEMON}" == "" ]
  then
    NO_DAEMON=false
  fi
  docker rm -f buildah || true
  docker run --rm -v "${BASEDIR}:${BASEDIR}" \
      -v /var/run/docker.sock:/var/run/docker.sock \
      --privileged --network host \
      -e IMAGE_NAME="${IMAGE_NAME}" \
      -e IMAGE_ARCH="${IMAGE_ARCH}" \
      -e AMD_BASE_IMAGE="${AMD_BASE_IMAGE}" \
      -e ARM_BASE_IMAGE="${ARM_BASE_IMAGE}" \
      -e PORT_EXTEND="${PORT_EXTEND}" \
      -e PORT_GRPC="${PORT_GRPC}" \
      -e PORT_MANAGEMENT="${PORT_MANAGEMENT}" \
      -e PORT_METRICS="${PORT_METRICS}" \
      -e MAIN_CLASS="${MAIN_CLASS}" \
      -e NO_DAEMON="${NO_DAEMON}" \
      -e COHERENCE_VERSION="${COHERENCE_VERSION}" \
      -e DOCKER_REGISTRY="${DOCKER_REGISTRY}" \
      -e DOCKER_USERNAME="${DOCKER_USERNAME}" \
      -e DOCKER_PASSWORD="${DOCKER_PASSWORD}" \
      -e DOCKER_HUB_USERNAME="${DOCKER_HUB_USERNAME}" \
      -e DOCKER_HUB_PASSWORD="${DOCKER_HUB_PASSWORD}" \
      -e PROJECT_URL="${PROJECT_URL}" \
      -e PROJECT_VENDOR="${PROJECT_VENDOR}" \
      -e PROJECT_DESCRIPTION="${PROJECT_DESCRIPTION}" \
      -e HTTP_PROXY="${HTTP_PROXY}" -e HTTPS_PROXY="${HTTPS_PROXY}" -e NO_PROXY="${NO_PROXY}" \
      -e http_proxy="${http_proxy}" -e https_proxy="${https_proxy}" -e no_proxy="${no_proxy}" \
      --name buildah \
      quay.io/buildah/stable:v1.21.0 "${SCRIPT_NAME}"
fi

