#!/bin/bash

function get-and-unzip-website-data() {
    curl -H "Authorization: token $AUTHTOKEN" -O $GITREPOURL # GITREPURL = https://your.gitea.host/api/v1/repos/<owner>/<repo>/archive/main.zip
    mkdir $BUILDDIR
    unzip main.zip -D $BUILDDIR
}

function build-and-extract-website() {
    (cd $BUILDDIR; lein ring war; websiteartifactname=$(ls | grep -o *.war); unzip target/$websiteartifactname "WEB-INF/classes/public/*")
}

# set variables from environment
# read write zugriff sicherstellen
function move-website-files-to-target() {
    rsync -ru --exclude-from "/etc/exclude.pattern" --delete WEB-INF/classes/public/* $TARGETDIR
}
