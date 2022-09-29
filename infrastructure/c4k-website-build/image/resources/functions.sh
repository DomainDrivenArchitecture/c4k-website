#!/bin/bash

function get-and-unzip-website-data() {
    filename="website.zip"
    curl -H "Authorization: token $AUTHTOKEN" -o $SOURCEDIR/$filename $GITREPOURL
    unzip $SOURCEDIR/$filename -d $BUILDDIR
}

function build-and-extract-website() {
    (cd $BUILDDIR; dir=$(ls); cd $dir; timeout -s SIGKILL 35s lein ring server-headless;)
    # websiteartifactname=$(ls target/ | grep -Eo "*.+\.war"); unzip target/$websiteartifactname
}
