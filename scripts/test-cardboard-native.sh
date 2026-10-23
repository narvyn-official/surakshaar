#!/bin/sh
set -eu
task_root=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
task_output="$task_root/artifacts/cardboard-sdk/host-tests"
mkdir -p "$task_output"
task_cpp="$task_root/apps/android/app/src/main/cpp"
task_sdk="$task_root/apps/android/cardboard-sdk/vendor/cardboard/sdk"
"${CXX:-clang++}" -std=c++17 -Wall -Wextra -Werror -I "$task_cpp" -I "$task_sdk" \
  "$task_root/apps/android/app/src/test/cpp/cardboard_math_test.cpp" \
  "$task_sdk/polynomial_radial_distortion.cc" -o "$task_output/cardboard-math-test"
"$task_output/cardboard-math-test"
