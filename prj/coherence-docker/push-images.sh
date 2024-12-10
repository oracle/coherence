#!/usr/bin/env bash
#
# Copyright (c) 2000, 2024, Oracle and/or its affiliates.
#
# Licensed under the Universal Permissive License v 1.0 as shown at
# https://oss.oracle.com/licenses/upl.
#
set -x -e

buildah version

if [ "${DOCKER_REGISTRY}" != "" ] && [ "${DOCKER_USERNAME}" != "" ] && [ "${DOCKER_PASSWORD}" != "" ]
then
  buildah login -u "${DOCKER_USERNAME}" -p "${DOCKER_PASSWORD}" "${DOCKER_REGISTRY}"
fi

push_image() {
  if [ "${NO_DAEMON}" != "true" ]
  then
    buildah pull "docker-daemon:${IMAGE_NAME}${1}-amd64"
    buildah pull "docker-daemon:${IMAGE_NAME}${1}-arm64"
  fi

  buildah rmi "${IMAGE_NAME}${1}" || true
  buildah manifest rm "${IMAGE_NAME}${1}" || true
  buildah manifest create "${IMAGE_NAME}${1}"
  buildah manifest add --arch amd64 --os linux "${IMAGE_NAME}${1}" "${IMAGE_NAME}${1}-amd64"
  buildah manifest add --arch arm64 --os linux "${IMAGE_NAME}${1}" "${IMAGE_NAME}${1}-arm64"
  buildah manifest inspect "${IMAGE_NAME}${1}"

  buildah manifest push --all -f v2s2 "${IMAGE_NAME}${1}" "docker://${IMAGE_NAME}${1}"
}

buildah images

push_image ""
push_image "-java17"

if [ "${NO_GRAAL}" != "true" ]
then
  # Push Graal based images
  push_image "-graal"
fi

# prune all old images that are no longer associated to a tag
buildah rmi --prune || true



