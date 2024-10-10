#!/bin/bash

# sbt docs/mdoc
marp doc-sources/target/mdoc --html=true
rm -rf build
cp -R doc-sources/target/mdoc build
rm build/*.md
