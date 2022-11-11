#!/bin/bash

function get-and-unzip-website-data() {
    filename="website.zip"
    curl -H "Authorization: token $AUTHTOKEN" -o $SOURCEDIR/$filename $GITREPOURL
    unzip $SOURCEDIR/$filename -d $BUILDDIR
}

function execute-scripts-when-existing {
    websitedir=$(ls $BUILDDIR)
    if [[ -f $BUILDDIR/$websitedir/$SCRIPTFILE ]]
        then 
            checksum="$(sha256sum $BUILDDIR/$websitedir/$SCRIPTFILE)"            
            if [[ "$SHA256SUM" == "$checksum" ]]
                then
                    chmod +x $BUILDDIR/$websitedir/$SCRIPTFILE
                    (cd $BUILDDIR; dir=$(ls); cd $dir; ./$SCRIPTFILE) #make sure paths defined in scriptfile are relative to $dir
                else
                    printf "Provided SHA256 Sum does not match calculated sum. Exiting."
                    printf "Calculated SHA256: $checksum"
                    printf "Given SHA256: $SHA256SUM"
                    exit 1
                fi
    else
        printf "No script file provided, exiting."
        exit 0
    fi
}

function build-and-extract-website() {
    (cd $BUILDDIR; dir=$(ls); cd $dir; lein run;)
}

function move-website-files-to-target() {
    (cd $BUILDDIR; dir=$(ls); cd $dir; rsync -ru --exclude-from "/etc/exclude.pattern" --delete resources/public/* $WEBSITEROOT;)
}
