#!/bin/bash
# curl -s -H "Authorization: token d92668fff6e005582dcb09c6590982a39b2523fc" https://repo.prod.meissa.de/api/v1/repos/meissa-intern/meissa-io/git/commits/HEAD | jq '.'

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




