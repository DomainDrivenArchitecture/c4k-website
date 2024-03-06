#!/bin/bash

mkdir $BUILDDIR
mkdir $SOURCEDIR

set -euo pipefail

source /usr/local/bin/functions.sh

filename="website.zip"
hashfilename="hashfile"

echo "Check for new content"
touch $HASHFILEDIR/$hashfilename
currentHash=$( cat $HASHFILEDIR/$hashfilename )
newHash=$( get-hash-data )

if [[ $currentHash == $newHash ]]
    then
        echo "Nothing to do"
    else
        echo $currentHash > $HASHFILEDIR/$hashfilename
        echo "Generate .netrc file"
        generate-netrc-file
        echo "Downloading website data"
        get-website-data $filename
        unzip-website-data $filename
        echo "Building website"
        build-website
        echo "Moving files"
        move-website-files-to-target
fi
