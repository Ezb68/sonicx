# Quick Start

> Note: To run docker you need start docker service.

**Build a local docker image**

```shell
> cd SonicX/docker
> docker image build -t sonicx-node .
```

**Run built image（refer to the home page）**

```shell
> docker container run -p 18888:18888 -p 50051:50051 -it SonicX/sonicx-node /bin/bash
> ./gradlew run -Pwitness
```