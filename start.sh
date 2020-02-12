#!/bin/sh
cd $(dirname $0)
java -jar ./build/libs/FullNode.jar --witness -p 4ee0a2b5d1463479a843d06634507f40c337426842cf5dae83dfd49c8bc79c5b -c ./plugin/config.conf --es &
cd -
