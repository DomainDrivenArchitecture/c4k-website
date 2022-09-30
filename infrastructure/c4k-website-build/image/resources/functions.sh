#!/bin/bash

function get-and-unzip-website-data() {
    filename="website.zip"
    curl -H "Authorization: token $AUTHTOKEN" -o $SOURCEDIR/$filename $GITREPOURL
    unzip $SOURCEDIR/$filename -d $BUILDDIR
}

function build-and-extract-website() {
    (cd $BUILDDIR; dir=$(ls); cd $dir; lein run;)
    # websiteartifactname=$(ls target/ | grep -Eo "*.+\.war"); unzip target/$websiteartifactname
}

function move-website-files-to-target() {
    (cd $BUILDDIR; dir=$(ls); cd $dir; rsync -ru --exclude-from "/etc/exclude.pattern" --delete resources/public/* $WEBSITEROOT;)
}
