FROM java:8-jdk
MAINTAINER Shane Mc Cormack <dataforce@dataforce.org.uk>

RUN \
  apt-get update && \ 
  apt-get -y install ant && \
  rm -rf /var/lib/apt/lists/*

COPY . /dfbnc/

RUN \
  cd /dfbnc && \
  if [ -e .git/shallow ]; then git fetch --unshallow; fi && \
  git fetch --tags && \
  git submodule update --init --recursive && \
  ant jar && \
  mv /dfbnc/dist/dfbnc.jar / && \
  rm -rf /dfbnc && \
  apt-get -y purge ant

EXPOSE 33262 33263

VOLUME ["/var/lib/dfbnc"]

WORKDIR /var/lib/dfbnc

CMD ["/usr/bin/java", "-jar", "/dfbnc.jar", "--config", "/var/lib/dfbnc", "--foreground"]
