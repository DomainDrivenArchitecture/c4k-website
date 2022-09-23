#!/bin/bash

# "Authorization: token YOURAUTHTOKEN"
function get-and-unzip-website-data() {
    curl -H "$AUTHTOKEN" -O $REPOZIPURL # REPOZIPURL = https://your.gitea.host/api/v1/repos/<owner>/<repo>/archive/main.zip
    mkdir $BUILDDIR
    unzip main.zip -D $BUILDDIR
}

function build-and-extract-website() {
    (cd $BUILDDIR; lein ring war; websiteartifactname=$(ls | grep -o *.war); unzip target/$websiteartifactname "WEB-INF/classes/public/*")
}

# set variables from environment
# read write zugriff sicherstellen
function move-website-files-to-target() {
    rsync -ru --exclude-from "/home/$USER/exclude.pattern" --delete WEB-INF/classes/public/* $TARGETDIR # TARGETDIR = mount/path/to/website-content-vol with write permission
}
