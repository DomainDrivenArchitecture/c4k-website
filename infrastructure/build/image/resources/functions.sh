#!/bin/bash

function generate-netrc-file() {
    echo "machine $GIT_HOST password $AUTH_TOKEN" > ~/.netrc
}

function get-website-data() {    
    curl -H "Authorization: token $AUTH_TOKEN" -o $SOURCE_DIR/$1 $GIT_REPO_URL
}

function get-hash-data() {
    curl -s -H "Authorization: token $AUTH_TOKEN" $GIT_COMMIT_URL | jq '.sha'
}

function write-hash-data() {
    echo $1 > $HASHFILE_DIR/$2
}

function unzip-website-data() {
    unzip $SOURCE_DIR/$1 -d $BUILD_DIR
}

function build-website() {
    (cd $BUILD_DIR; dir=$(ls); cd $dir; ./generate.sh;)
}

function move-website-files-to-target() {
    (cd $BUILD_DIR; dir=$(ls); cd $dir; rsync -ru --exclude-from "/etc/exclude.pattern" --delete target/html/* $WEBSITE_ROOT;)
}
