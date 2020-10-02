#!/bin/bash

set -eo pipefail

DOCKER_IMAGE=$1
DOCKER_TEST_IMAGE=accountgenerator

# create test docker image that includes the test key file and password files
TEST_CONTAINER_ID=$(docker create ${DOCKER_IMAGE})
docker cp ./pwd ${TEST_CONTAINER_ID}:/tmp/test_password
docker commit ${TEST_CONTAINER_ID} ${DOCKER_TEST_IMAGE}

docker run --rm --name ${DOCKER_TEST_IMAGE} \
  -v $PWD/pwd:/etc/accountgenerator/keyfiles/pwd \
  -p 7545:7545 \
  ${DOCKER_IMAGE} \
  --http-listen-port=7545 \
  --http-listen-host="0.0.0.0" \
  --directory="/etc/accountgenerator/keyfiles/" \
  file-based-account-generator \
  --password-file="/etc/accountgenerator/keyfiles/pwd"

