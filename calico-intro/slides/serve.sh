#!/usr/bin/env bash

PORT=9090 marp --watch --server --html=true doc-sources/target/mdoc/
# PORT=9090 marp -w -s --html=true docs
