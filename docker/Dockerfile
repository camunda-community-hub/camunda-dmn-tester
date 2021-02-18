FROM eed3si9n/sbt:jdk11-alpine
LABEL MAINTAINER="pme123"
RUN ["apk", "add", "--no-cache", "curl"]
ADD data/* /opt/workspace/
ENTRYPOINT [ "/bin/bash" ]
CMD ["./amm", "unitTestRunner.sc"]
