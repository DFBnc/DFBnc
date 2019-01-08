FROM openjdk:alpine
MAINTAINER Shane Mc Cormack <dataforce@dataforce.org.uk>

ENV LANG en_US.UTF-8
ENV LANGUAGE en_US:en
ENV LC_ALL en_US.UTF-8
ENV JAVA_TOOL_OPTIONS -Dfile.encoding=UTF8

COPY . /tmp/dfbnc/

RUN \
  addgroup -g 3456 -S dfbnc && \
  adduser -S -G dfbnc -u 3456 -s /bin/bash -h /home/dfbnc dfbnc && \
  mkdir /var/lib/dfbnc && \
  chown -R dfbnc /tmp/dfbnc && \
  chown -R dfbnc /home/dfbnc && \
  chown -R dfbnc /var/lib/dfbnc && \
  mv /tmp/dfbnc/ssl.sh /home/dfbnc/ssl.sh && \
  chmod a+x /home/dfbnc/ssl.sh

RUN \
  apk add --no-cache git openssl coreutils bash

USER dfbnc

RUN \
  cd /tmp/dfbnc && \
  find -type f -name .git -exec bash -c 'f="{}"; cd $(dirname $f); echo "gitdir: ../../.git/modules/$(realpath --relative-to=/tmp/dfbnc .)" > .git' \; && \
  if [ -e $(git rev-parse --git-dir)/shallow ]; then git init; git fetch --unshallow; fi && \
  git fetch --tags && \
  git submodule foreach 'if [ -e $(git rev-parse --git-dir)/shallow ]; then git init; git fetch --unshallow; fi' && \
  git submodule foreach 'git fetch --tags' && \
  ./gradlew jar && \
  mv /tmp/dfbnc/dist/dfbnc.jar /home/dfbnc/ && \
  rm -rf /tmp/dfbnc

EXPOSE 33262 33263

WORKDIR /var/lib/dfbnc

ENTRYPOINT ["/home/dfbnc/ssl.sh"]

CMD ["/usr/bin/java", "-jar", "/home/dfbnc/dfbnc.jar", "--config", "/var/lib/dfbnc", "--foreground"]
