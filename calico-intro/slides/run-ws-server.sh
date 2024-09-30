#!/bin/bash

while true; do cat ../client/main.scala; done | pv --rate | websocat -s 9091 -q
