#!/bin/bash

set -exo pipefail

function main()
{
    {
        upgradeSystem
        apt-get install -qqy unzip rsync jq imagemagick curl
        install-hugo-from-deb

        install -d /etc/lein/
        install -m 0700 /tmp/entrypoint.sh /
        install -m 0700 /tmp/functions.sh /usr/local/bin/
        install -m 0700 /tmp/exclude.pattern /etc/
        install -m 0700 /tmp/project.clj /etc/lein/

        cd /etc/lein
        lein deps
        
        cleanupDocker
    } > /dev/null
}

source /tmp/install_functions_debian.sh
source /tmp/functions.sh
DEBIAN_FRONTEND=noninteractive DEBCONF_NOWARNINGS=yes main