#!/usr/bin/env bash

mkdir docs || true
cp -R ./report-aggregate/src/main/resources/* docs
cp -R ./readme.md docs/index.md
