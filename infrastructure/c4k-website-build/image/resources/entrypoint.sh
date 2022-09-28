#!/bin/bash

mkdir $BUILDDIR
mkdir $SOURCEDIR

source /usr/local/bin/functions.sh

function move-website-files-to-target() {
    (cd $BUILDDIR; dir=$(ls); cd $dir; rsync -ru --exclude-from "/etc/exclude.pattern" --delete resources/public/* $WEBSITEROOT;)
}

echo "Downloading website"
get-and-unzip-website-data
echo "Building website"
build-and-extract-website
echo "Moving files"
move-website-files-to-target    

while true; do
    sleep 1m
done
