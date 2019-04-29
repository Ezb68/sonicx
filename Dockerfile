FROM SonicXChain/sonicx-gradle

RUN set -o errexit -o nounset \
    && echo "git clone" \
    && git clone https://github.com/SonicXChain/sonicx.git \
    && cd sonicx \
    && gradle build

WORKDIR /sonicx

EXPOSE 18888
