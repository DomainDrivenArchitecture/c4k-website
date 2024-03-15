#!/bin/bash

function generate-netrc-file() {
    echo "machine $GITHOST password $AUTHTOKEN" > ~/.netrc
}

function get-website-data() {    
    curl -H "Authorization: token $AUTHTOKEN" -o $SOURCEDIR/$1 $GITREPOURL
}

function get-hash-data() {
    curl -s -H "Authorization: token $AUTHTOKEN" $GITCOMMITURL | jq '.sha'
}

function write-hash-data() {
    echo $1 > $HASHFILEDIR/$2
}

function unzip-website-data() {
    unzip $SOURCEDIR/$1 -d $BUILDDIR
}

function build-website() {
    (cd $BUILDDIR; dir=$(ls); cd $dir; bash generate.sh;)
}

function move-website-files-to-target() {
    (cd $BUILDDIR; dir=$(ls); cd $dir; rsync -ru --exclude-from "/etc/exclude.pattern" --delete target/html/* $WEBSITEROOT;)
}
