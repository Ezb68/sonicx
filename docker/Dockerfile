FROM SonicXChain/sonicx-gradle

RUN set -o errexit -o nounset \
#  Download and build SonicX
    && echo "git clone" \
    && git clone https://github.com/SonicXChain/SonicX.git \
    && cd SonicX \
    && gradle build

# Change work directory
WORKDIR /SonicX

# open port 18888
EXPOSE 18888

