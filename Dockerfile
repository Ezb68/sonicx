FROM SonicXChain/sonicx-gradle

RUN set -o errexit -o nounset \
    && echo "git clone" \
    && git clone https://github.com/SonicXChain/SonicX.git \
    && cd SonicX \
    && gradle build

WORKDIR /SonicX

EXPOSE 18888
