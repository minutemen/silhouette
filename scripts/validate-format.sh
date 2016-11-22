#!/usr/bin/env bash
#
# Validates the code formatting.
#
# If there are style violations, outputs a message and exits with a non-zero status code.
#
# Licensed to the Minutemen Group under one or more contributor license
# agreements. See the COPYRIGHT file distributed with this work for
# additional information regarding copyright ownership.
#
# Licensed under the Apache License, Version 2.0 (the "License"); you
# may not use this file except in compliance with the License. You may
# obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
set -o nounset -o errexit

SCRIPTS_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

git diff --quiet || (
  echo "ERROR: The code formatting validation must be run on a repository with no pending changes."
  false
)

${SCRIPTS_DIR}/reformat.sh

git config color.diff.whitespace "red reverse ul"
git --no-pager diff -R --color --exit-code || (
  echo ""
  echo "ERROR: The code is not formatted according to the project's standards."
  echo "The differences are shown above. Your code is shown in green and the expected format is shown in red."
  echo "To perform this same validation on your environment, run 'scripts/validate-format.sh'."
  echo "To fix, format your sources running 'scripts/reformat.sh' before submitting a pull request."
  echo "After correcting, please squash your commits (eg, use 'git commit --amend') before updating your pull request."
  false
)
