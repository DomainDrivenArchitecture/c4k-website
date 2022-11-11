#!/bin/bash

mkdir $BUILDDIR
mkdir $SOURCEDIR

set -o nounset
set -o xtrace
set -o errexit
set -eo pipefail

source /usr/local/bin/functions.sh

echo "Downloading website"
get-and-unzip-website-data
echo "Executing Custom Scripts, if applicable"
execute-scripts-when-existing
echo "Building website"
build-and-extract-website
echo "Moving files"
move-website-files-to-target    
