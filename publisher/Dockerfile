FROM ubuntu:bionic as build

ARG ROOTFS=/build/rootfs
ARG DEBS=/build/debs
ARG REQUIRED_PACKAGES="coreutils bash zlib1g libtinfo5 libselinux1"

ARG JAVA_VERSION=10.0.1
ARG JAVA_VERSION_BUILD=10
ARG JAVA_PACKAGE=serverjre
ARG JAVA_SHA256_SUM=9c22d2f232c8a298c6fdea12356042814655dec3db24c2b0ff096b21f2c36e43
ARG JAVA_URL_ELEMENT=fb4372174a714e6b8c52526dc134031e

ENV DEBIAN_FRONTEND=noninteractive

# Build pre-requisites
RUN bash -c 'mkdir -p $ROOTFS $DEBS $ROOTFS/{bin,sbin,usr/bin,usr/sbin,usr/lib,usr/share,opt}'

# Fix permissions
RUN chown -Rv _apt:root $DEBS \
        && chmod -Rv 700 $DEBS

# Install pre-requisites
RUN apt-get update \
        && apt-get -y install apt-utils curl unzip locales

# UTF8 support
RUN echo "LC_ALL=en_US.UTF-8" > /etc/environment \
        && echo "en_US.UTF-8 UTF-8" > /etc/locale.gen \
        && echo "LANG=en_US.UTF-8" > /etc/locale.conf \
        && locale-gen en_US.UTF-8 \
        && cp -R /usr/lib/locale $ROOTFS/usr/lib/locale \
        && cp -R /usr/share/locale $ROOTFS/usr/share/locale \
        && cp -R /usr/share/locales $ROOTFS/usr/share/locales \
        && cp -R /usr/share/i18n $ROOTFS/usr/share/i18n

# Unpack required packges to rootfs
RUN  cd $DEBS \
  && for pkg in $REQUIRED_PACKAGES; do \
       apt-get download $pkg \
         && apt-cache depends --recurse --no-recommends --no-suggests --no-conflicts --no-breaks --no-replaces --no-enhances --no-pre-depends -i $pkg | grep '^[a-zA-Z0-9]' | xargs apt-get download ; \
     done
RUN if [ "x$(ls $DEBS/)" = "x" ]; then \
      echo No required packages specified; \
    else \
      for pkg in $DEBS/*.deb; do \
        dpkg -x $pkg $ROOTFS; \
      done; \
    fi

# Base
RUN echo "root:x:0:0:root:/home:/bin/sh" > $ROOTFS/etc/passwd \
        && echo "root:x:0:" > $ROOTFS/etc/group

# CA certs
RUN apt-get install -y ca-certificates ca-certificates-java \
        && update-ca-certificates \
        && cp -r /etc/ssl $ROOTFS/etc \
        && cp -r /usr/share/ca-certificates $ROOTFS/usr/share

# Java
RUN curl -jkLH "Cookie: oraclelicense=accept-securebackup-cookie" -o java.tar.gz\
    http://download.oracle.com/otn-pub/java/jdk/${JAVA_VERSION}+${JAVA_VERSION_BUILD}/${JAVA_URL_ELEMENT}/${JAVA_PACKAGE}-${JAVA_VERSION}_linux-x64_bin.tar.gz &&\
  echo "$JAVA_SHA256_SUM  java.tar.gz" | sha256sum -c - &&\
  gunzip -c java.tar.gz | tar -xf - -C $ROOTFS/opt && rm -f java.tar.gz &&\
  ln -s /opt/jdk-${JAVA_VERSION} $ROOTFS/opt/jdk

FROM scratch
LABEL author="ilja@bobkevic.com"

ARG jarfile
ARG ROOTFS=/build/rootfs

ENV LC_ALL=en_US.UTF-8
ENV LANG=en_US.UTF-8
ENV JAVA_HOME=/opt/jdk
ENV PATH=${PATH}:${JAVA_HOME}/bin
ENV APP=/opt/$jarfile

COPY --from=build $ROOTFS /

COPY $jarfile $APP

EXPOSE 8080 9080

ENTRYPOINT ["/bin/bash", "-c", "exec $JAVA_HOME/bin/java $JVM_DEFAULT_ARGS $JVM_ARGS --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.nio=ALL-UNNAMED -jar $APP \"$@\"", "bash"]