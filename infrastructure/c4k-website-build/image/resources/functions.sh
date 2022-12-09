#!/bin/bash
# curl -s -H "Authorization: token xxxx" https://gitea.host/api/v1/repos/{owner}/{repo}/git/commits/HEAD | jq '.sha'

function get-website-data() {    
    curl -H "Authorization: token $AUTHTOKEN" -o $SOURCEDIR/$1 $GITREPOURL
}

function write-hashfile() {
    echo $1 > $HASHFILEDIR/$2
}

function print-hash-from-file() {
    (cd $SOURCEDIR; sha256sum $1 | cut -d " " -f 1;)
}

function unzip-website-data() {
    unzip $SOURCEDIR/$1 -d $BUILDDIR
}

function execute-scripts-when-existing {
    websitedir=$(ls $BUILDDIR)
    if [[ -f $BUILDDIR/$websitedir/$SCRIPTFILE ]]
        then 
            checksum="$(sha256sum $BUILDDIR/$websitedir/$SCRIPTFILE | grep -oE "^[a-z0-9]+")"            
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
        printf "No script file provided."
    fi
}

function build-website() {
    (cd $BUILDDIR; dir=$(ls); cd $dir; lein run;)
}

function move-website-files-to-target() {
    (cd $BUILDDIR; dir=$(ls); cd $dir; rsync -ru --exclude-from "/etc/exclude.pattern" --delete resources/public/* $WEBSITEROOT;)
}
