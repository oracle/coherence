#!/usr/bin/env bash
#
# Copyright (c) 2000, 2025, Oracle and/or its affiliates.
#
# Licensed under the Universal Permissive License v 1.0 as shown at
# https://oss.oracle.com/licenses/upl.
#

echo "Metrics Format Test"
echo "$(pwd)"

cd src/main/python || exit 1
rm -rf .pyenv || true
git clone --branch v2.3.22 https://github.com/pyenv/pyenv.git .pyenv
export PYENV_ROOT=$(pwd)/.pyenv
export PATH=${PYENV_ROOT}/bin:${PATH}
eval "$(pyenv init --path)"
echo $PATH

${PYENV_ROOT}/bin/pyenv install 3.11
${PYENV_ROOT}/bin/pyenv versions
${PYENV_ROOT}/bin/pyenv global 3.11
which python

pip install abnf

echo "Current path: $(pwd)"
python ./verify_metrics_format.py