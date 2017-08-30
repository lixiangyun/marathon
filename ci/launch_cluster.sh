#!/bin/bash

set -e -o pipefail

# Two parameters are expected: CHANNEL and VARIANT where CHANNEL is the respective PR and
# VARIANT could be one of four custer variants: open, strict, permissive and disabled
if [ "$#" -ne 2 ]; then
    echo "Expected 2 parameters: <channel> and <variant> e.g. launch_cluster.sh testing/pull/1739 open"
    exit 1
fi

CHANNEL="$1"
VARIANT="$2"

JOB_NAME_SANITIZED=$(echo "$JOB_NAME" | tr -c '[:alnum:]-' '-')
DEPLOYMENT_NAME="$JOB_NAME_SANITIZED-$(date +%s)"

if [ "$VARIANT" == "open" ]; then
  TEMPLATE="https://s3.amazonaws.com/downloads.dcos.io/dcos/${CHANNEL}/cloudformation/single-master.cloudformation.json"
else
  TEMPLATE="https://s3.amazonaws.com/downloads.mesosphere.io/dcos-enterprise-aws-advanced/${CHANNEL}/${VARIANT}/cloudformation/ee.single-master.cloudformation.json"
fi

echo "Workspace: ${WORKSPACE}"
echo "Using: ${TEMPLATE}"

apk update
apk --upgrade add gettext wget
wget 'https://downloads.dcos.io/dcos-test-utils/bin/linux/dcos-launch' && chmod +x dcos-launch


envsubst <<EOF > config.yaml
---
launch_config_version: 1
template_url: $TEMPLATE
deployment_name: $DEPLOYMENT_NAME
provider: aws
aws_region: eu-central-1
template_parameters:
    KeyName: default
    AdminLocation: 0.0.0.0/0
    PublicSlaveInstanceCount: 1
    SlaveInstanceCount: 5
EOF
./dcos-launch create
./dcos-launch wait

# Return dcos_url
echo "http://$(./dcos-launch describe | jq -r ".masters[0].public_ip")/"