FROM java:8-jdk
MAINTAINER Shane Mc Cormack <dataforce@dataforce.org.uk>

COPY . /dfbnc/

RUN \
  cd /dfbnc && \
  if [ -e .git/shallow ]; then git fetch --unshallow; fi && \
  git fetch --tags && \
  git submodule update --init --recursive && \
  ./gradlew jar && \
  mv /dfbnc/dist/dfbnc.jar / && \
  rm -rf /dfbnc

EXPOSE 33262 33263

VOLUME ["/var/lib/dfbnc"]

WORKDIR /var/lib/dfbnc

CMD ["/usr/bin/java", "-jar", "/dfbnc.jar", "--config", "/var/lib/dfbnc", "--foreground"]
