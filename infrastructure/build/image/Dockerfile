FROM clojure:lein

# Prepare Entrypoint Script
ADD resources /tmp

ENV BUILD_DIR="/etc/website"
ENV SOURCE_DIR="/etc/websitesource"
ENV WEBSITE_ROOT="/var/www/html/website/"
ENV HASHFILE_DIR="/var/hashfile.d"

RUN /tmp/install.sh
