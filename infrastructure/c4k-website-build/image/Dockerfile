FROM clojure:lein

# Prepare Entrypoint Script
ADD resources /tmp

ENV BUILDDIR="website"
ENV WEBSITEROOT="/var/www/html/website/"

RUN /tmp/install.sh
#RUN /tmp/entrypoint.sh

# ToDo: lein not working on clojure image?