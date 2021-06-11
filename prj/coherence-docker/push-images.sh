#!/usr/bin/env bash
#
# Copyright (c) 2000, 2021, Oracle and/or its affiliates.
#
# Licensed under the Universal Permissive License v 1.0 as shown at
# http://oss.oracle.com/licenses/upl.
#
set -x

buildah version

if [ "${DOCKER_REGISTRY}" != "" ] && [ "${DOCKER_USERNAME}" != "" ] && [ "${DOCKER_PASSWORD}" != "" ]
then
  buildah login -u "${DOCKER_USERNAME}" -p "${DOCKER_PASSWORD}" "${DOCKER_REGISTRY}"
fi

if [ "${NO_DAEMON}" != "true" ]
then
  buildah pull "docker-daemon:${IMAGE_NAME}-amd64"
  buildah pull "docker-daemon:${IMAGE_NAME}-arm64"
fi

# Cut the registry from the front of the image name e.g. docker.io/foo/bar:1.0 becomes foo/bar:1.0
# This is because Buildah's local name of an image uses "localhost" for the registry
IMAGE_SUFFIX=$(echo "${IMAGE_NAME}" | cut -d"/" -f2-10)
LOCAL_NAME="localhost/${IMAGE_SUFFIX}"

buildah images

buildah manifest create "${IMAGE_NAME}"
buildah manifest add --arch amd64 --os linux "${IMAGE_NAME}" "${LOCAL_NAME}-amd64"
buildah manifest add --arch arm64 --os linux "${IMAGE_NAME}" "${LOCAL_NAME}-arm64"
buildah manifest inspect "${IMAGE_NAME}"

buildah manifest push --all -f v2s2 "${IMAGE_NAME}" "docker://${IMAGE_NAME}"


