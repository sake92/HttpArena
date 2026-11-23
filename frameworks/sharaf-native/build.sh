#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
mkdir -p target
scala --power package Main.scala --server=false --native-mode release-fast --native-multithreading --force -o target/server
docker build -t httparena-sharaf-native .
