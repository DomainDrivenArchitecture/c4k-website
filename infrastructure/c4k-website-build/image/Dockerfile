FROM clojure:lein

# Prepare Entrypoint Script
ADD resources /tmp

ENV SOURCEDIR="/etc/websitesource"
ENV BUILDDIR="/etc/website"
ENV WEBSITEROOT="/var/www/html/website/"

RUN /tmp/install.sh
