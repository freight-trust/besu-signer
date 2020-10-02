#!/bin/bash
set -eo pipefail

docker run --rm --name accountgenerator \
  -v $PWD/ecd7b37cb0d7220eee1c970669b16b635de60196.toml:/etc/accountgenerator/keyfiles/ecd7b37cb0d7220eee1c970669b16b635de60196.toml \
  -v $PWD/customerCA.crt:/opt/cloudhsm/etc/customerCA.crt \
  -p 7545:7545 \
  adharaprojects/accountgenerator:1.0.0-ADHARA-SNAPSHOT-cloudhsm \
  --http-listen-port=7545 \
  --http-listen-host="0.0.0.0" \
  --directory="/etc/accountgenerator/keyfiles/" \
  cavium-account-generator \
  --sas="/etc/accountgenerator/scripts/sas.h" \
  --library="/opt/cloudhsm/lib/libcloudhsm_pkcs11.so" \
  --slot-pin="alice:*******" \
