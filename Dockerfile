FROM harbor.lcp.service.163.org/library/jdk8-openjdk-skiff:v1.1-190911
MAINTAINER BILL "base_cloud@corp.netease.com"

# 拷贝代码
RUN mkdir -p /home/cloud/nasl-storage/
WORKDIR /home/cloud/nasl-storage/
RUN pwd

ENV LANG=C.UTF-8

ENV JAVA_HOME=/usr/local/openjdk-8

ENV PATH=/usr/local/openjdk-8/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin

# 运行代码
COPY ./target/nasl-storage-0.0.1-SNAPSHOT.jar nasl-storage.jar
CMD ["/bin/sh", "-c", "exec java -XX:+UseConcMarkSweepGC -XX:-UseGCOverheadLimit -Djava.security.egd=file:/dev/./urandom -Djava.library.path=/ -Dfastjson.parser.autoTypeSupport=false -jar nasl-storage.jar"]

