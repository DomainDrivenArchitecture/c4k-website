#!/bin/bash
# curl -s -H "Authorization: token xxxx" https://gitea.host/api/v1/repos/{owner}/{repo}/git/commits/HEAD | jq '.sha'

mkdir $BUILDDIR
mkdir $SOURCEDIR

set -o nounset
set -o xtrace
set -o errexit
set -eo pipefail

source /usr/local/bin/functions.sh

filename="website.zip"
hashfilename="hashfile"

echo "Downloading website data"
get-website-data $filename

echo "Check for new content"
currentHash=$( print-hash-from-file $filename )
touch $HASHFILEDIR/$hashfilename
if [[ $currentHash == $(cat $HASHFILEDIR/$hashfilename) ]]
    then
        echo "Nothing to do"
    else
        write-hashfile $currentHash $hashfilename
        unzip-website-data $filename
        echo "Executing Custom Scripts, if applicable"
        execute-scripts-when-existing
        echo "Building website"
        build-website
        echo "Moving files"
        move-website-files-to-target
fi




