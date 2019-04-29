<h1 align="center">
  <br>
  <img width=20% src="https://github.com/SonicXChain/wiki/blob/master/images/SonicX.jpg?raw=true">
  <br>
  SonicX
  <br>
</h1>

<h4 align="center">
  Java implementation of the <a href="https://www.sonicx.org">SonicX Protocol</a>
</h4>


<p align="center">
  <a href="https://discord.gg/GsRgsTD">
    <img src="https://img.shields.io/badge/chat-on%20discord-brightgreen.svg">
  </a>
    
  <a href="https://travis-ci.org/SonicXChain/sonicx">
    <img src="https://travis-ci.org/SonicXChain/sonicx.svg?branch=develop">
  </a>
  
  <a href="https://codecov.io/gh/SonicXChain/sonicx">
    <img src="https://codecov.io/gh/SonicXChain/sonicx/branch/develop/graph/badge.svg" />
  </a>
  
  <a href="https://github.com/SonicXChain/sonicx/issues">
    <img src="https://img.shields.io/github/issues/SonicXChain/sonicx.svg">
  </a>
  
  <a href="https://github.com/SonicXChain/sonicx/pulls">
    <img src="https://img.shields.io/github/issues-pr/SonicXChain/sonicx.svg">
  </a>
  
  <a href="https://github.com/SonicXChain/sonicx/graphs/contributors"> 
    <img src="https://img.shields.io/github/contributors/SonicXChain/sonicx.svg">
  </a>
  
  <a href="LICENSE">
    <img src="https://img.shields.io/github/license/SonicXChain/sonicx.svg">
  </a>
</p>

<p align="center">
  <a href="#how-to-build">How to Build</a> •
  <a href="#running">How to Run</a> •
  <a href="#links">Links</a> •
  <a href="http://developers.sonicx.org">Documentation</a> •
  <a href="CONTRIBUTING.md">Contributing</a> •
  <a href="#community">Community</a>
</p>

## What's SonicX?

SonicX is a project dedicated to building the infrastructure for a truly decentralized Internet.

The SonicX Protocol, one of the largest blockchain based operating systems in the world, offers scalable, high-availability and high-throughput support that underlies all the decentralized applications in the SonicX ecosystem. 

SonicX enables large-scale development and engagement. With over 2000 transactions per second (TPS), high concurrency, low latency and massive data transmission, SONICX is ideal for building decentralized entertainment applications. Free features and incentive systems allow developers to create premium app experiences for users.

SonicX Protocol and the SonicX Virtual Machine (SVM) allow anyone to develop decentralized applications (DAPPs) for themselves or their communities with smart contracts thereby making decentralized crowdfunding and token issuance easier than ever.

# How to Build

## Prepare dependencies

* JDK 1.8 (JDK 1.9+ are not supported yet)
* On Linux Ubuntu system (e.g. Ubuntu 16.04.4 LTS), ensure that the machine has [__Oracle JDK 8__](https://www.digitalocean.com/community/tutorials/how-to-install-java-with-apt-get-on-ubuntu-16-04), instead of having __Open JDK 8__ in the system. If you are building the source code by using __Open JDK 8__, you will get [__Build Failed__](https://github.com/SonicXChain/sonicx/issues/337) result.
* Open **UDP** ports for connection to the network
* **MINIMUM** 2 ENERGY Cores

## Build and Deploy automatically using scripts

- Please take a look at the [SonicX Deployment Scripts](https://github.com/SonicXChain/sonicx-deployment) repository.

## Getting the code with git

* Use Git from the Terminal, see the [Setting up Git](https://help.github.com/articles/set-up-git/) and [Fork a Repo](https://help.github.com/articles/fork-a-repo/) articles.
* develop branch: the newest code 
* master branch: more stable than develop.
In the shell command, type:
```bash
git clone https://github.com/SonicXChain/sonicx.git
git checkout -t origin/master
```

* For Mac, you can also install **[GitHub for Mac](https://mac.github.com/)** then **[fork and clone our repository](https://guides.github.com/activities/forking/)**. 

* If you'd rather not use Git, [Download the ZIP](https://github.com/SonicXChain/sonicx/archive/develop.zip)

## Including SonicX as dependency

* If you don't want to checkout the code and build the project, you can include it directly as a dependency

**Using gradle:**

```
repositories {
   maven { url 'https://jitpack.io' }
}
dependencies {
   implementation 'com.github.SonicXChain:sonicx:develop-SNAPSHOT'
}
```
  
**Using maven:**

```xml
...
<repositories>
  <repository>
      <id>jitpack.io</id>
      <url>https://jitpack.io</url>
  </repository>
</repositories>
...
<dependency>
    <groupId>com.github.SonicXChain</groupId>
    <artifactId>sonicx</artifactId>
    <version>develop-SNAPSHOT</version><!--You can use any of the tag/branch name available-->
</dependency>
```




## Building from source code

* Build in the Terminal

```bash
cd sonicx
./gradlew build
```


* Build in [IntelliJ IDEA](https://www.jetbrains.com/idea/) (community version is enough):

  **Please run ./gradlew build once to build the protocol files**

  1. Start IntelliJ. Select `File` -> `Open`, then locate to the SonicX folder which you have git cloned to your local drive. Then click `Open` button on the right bottom.
  2. Check on `Use auto-import` on the `Import Project from Gradle` dialog. Select JDK 1.8 in the `Gradle JVM` option. Then click `OK`.
  3. IntelliJ will open the project and start gradle syncing, which will take several minutes, depending on your network connection and your IntelliJ configuration
  4. Enable Annotations, `Preferences` -> Search `annotations` -> check `Enable Annotation Processing`.
  5. After the syncing finished, select `Gradle` -> `Tasks` -> `build`, and then double click `build` option.
  
# Running

### Running multi-nodes

https://github.com/SonicXChain/Documentation/blob/master/SOX/Solidity_and_Full_Node_Deployment_EN.md

## Running a local node and connecting to the public testnet 

* Use the [Testnet Config](https://github.com/SonicXChain/sonicx-deployment/blob/master/test_net_config.conf) or use the [SonicX Deployment Scripts](https://github.com/SonicXChain/sonicx-deployment) 


### Running a Super Representative Node for mainnet

* Use the executable JAR(Recommended way)

```bash
java -jar FullNode.jar -p your private key --witness -c your config.conf(Example：/data/sonicx/config.conf)
Example:
java -jar FullNode.jar -p 650950B193DDDDB35B6E48912DD28F7AB0E7140C1BFDEFD493348F02295BD812 --witness -c /data/sonicx/config.conf

```

This is similar to running a private testnet, except that the IPs in the `config.conf` are officially declared by SONICX.

<details>
<summary>Correct output</summary>

```bash

20:43:18.138 INFO  [main] [o.t.p.FullNode](FullNode.java:21) Full node running.
20:43:18.486 INFO  [main] [o.t.c.c.a.Args](Args.java:429) Bind address wasn't set, Punching to identify it...
20:43:18.493 INFO  [main] [o.t.c.c.a.Args](Args.java:433) UDP local bound to: 10.0.8.146
20:43:18.495 INFO  [main] [o.t.c.c.a.Args](Args.java:448) External IP wasn't set, using checkip.amazonaws.com to identify it...
20:43:19.450 INFO  [main] [o.t.c.c.a.Args](Args.java:461) External address identified: 47.74.147.87
20:43:19.599 INFO  [main] [o.s.c.a.AnnotationConfigApplicationContext](AbstractApplicationContext.java:573) Refreshing org.springframework.context.annotation.AnnotationConfigApplicationContext@124c278f: startup date [Fri Apr 27 20:43:19 CST 2018]; root of context hierarchy
20:43:19.972 INFO  [main] [o.s.b.f.a.AutowiredAnnotationBeanPostProcessor](AutowiredAnnotationBeanPostProcessor.java:153) JSR-330 'javax.inject.Inject' annotation found and supported for autowiring
20:43:20.380 INFO  [main] [o.t.c.d.DynamicPropertiesStore](DynamicPropertiesStore.java:244) update latest block header timestamp = 0
20:43:20.383 INFO  [main] [o.t.c.d.DynamicPropertiesStore](DynamicPropertiesStore.java:252) update latest block header number = 0
20:43:20.393 INFO  [main] [o.t.c.d.DynamicPropertiesStore](DynamicPropertiesStore.java:260) update latest block header id = 00
20:43:20.394 INFO  [main] [o.t.c.d.DynamicPropertiesStore](DynamicPropertiesStore.java:265) update state flag = 0
20:43:20.559 INFO  [main] [o.t.c.c.TransactionCapsule](TransactionCapsule.java:83) Transaction create succeeded！
20:43:20.567 INFO  [main] [o.t.c.c.TransactionCapsule](TransactionCapsule.java:83) Transaction create succeeded！
20:43:20.568 INFO  [main] [o.t.c.c.TransactionCapsule](TransactionCapsule.java:83) Transaction create succeeded！
20:43:20.568 INFO  [main] [o.t.c.c.TransactionCapsule](TransactionCapsule.java:83) Transaction create succeeded！
20:43:20.569 INFO  [main] [o.t.c.c.TransactionCapsule](TransactionCapsule.java:83) Transaction create succeeded！
20:43:20.596 INFO  [main] [o.t.c.d.Manager](Manager.java:300) create genesis block
20:43:20.607 INFO  [main] [o.t.c.d.Manager](Manager.java:306) save block: BlockCapsule

```

Then observe whether block synchronization success，If synchronization successfully explains the success of the super node

</details>


### Running a Super Representative Node for private testnet
* use master branch
* You should modify the config.conf
  1. Replace existing entry in genesis.block.witnesses with your address.
  2. Replace existing entry in seed.node ip.list with your ip list.
  3. The first Super Node start, needSyncCheck should be set false
  4. Set p2pversion to 61 

* Use the executable JAR(Recommended way)

```bash
cd build/libs
java -jar FullNode.jar -p your private key --witness -c your config.conf (Example：/data/sonicx/config.conf)
Example:
java -jar FullNode.jar -p 650950B193DDDDB35B6E48912DD28F7AB0E7140C1BFDEFD493348F02295BD812 --witness -c /data/sonicx/config.conf

```
  
<details>
<summary>Show Output</summary>

```bash
> ./gradlew run -Pwitness

> Task :generateProto UP-TO-DATE
Using TaskInputs.file() with something that doesn't resolve to a File object has been deprecated and is scheduled to be removed in Gradle 5.0. Use TaskInputs.files() instead.

> Task :run 
20:39:22.749 INFO [o.t.c.c.a.Args] private.key = 63e62a71ed39e30bac7223097a173924aad5855959de517ff2987b0e0ec89f1a
20:39:22.816 WARN [o.t.c.c.a.Args] localwitness size must be one, get the first one
20:39:22.832 INFO [o.t.p.FullNode] Here is the help message.output-directory/
三月 22, 2018 8:39:23 下午 org.sonicx.core.services.RpcApiService start
信息: Server started, listening on 50051
20:39:23.706 INFO [o.t.c.o.n.GossipLocalNode] listener message
20:39:23.712 INFO [o.t.c.o.n.GossipLocalNode] sync group = a41d27f10194c53703be90c6f8735bb66ffc53aa10ea9024d92dbe7324b1aee3
20:39:23.716 INFO [o.t.c.s.WitnessService] Sleep : 1296 ms,next time:2018-03-22T20:39:25.000+08:00
20:39:23.734 WARN [i.s.t.BootstrapFactory] Env doesn't support epoll transport
20:39:23.746 INFO [i.s.t.TransportImpl] Bound to: 192.168.10.163:7080
20:39:23.803 INFO [o.t.c.n.n.NodeImpl] other peer is nil, please wait ... 
20:39:25.019 WARN [o.t.c.d.Manager] nextFirstSlotTime:[2018-03-22T17:57:20.001+08:00],now[2018-03-22T20:39:25.067+08:00]
20:39:25.019 INFO [o.t.c.s.WitnessService] ScheduledWitness[448d53b2df0cd78158f6f0aecdf60c1c10b15413],slot[1946]
20:39:25.021 INFO [o.t.c.s.WitnessService] It's not my turn
20:39:25.021 INFO [o.t.c.s.WitnessService] Sleep : 4979 ms,next time:2018-03-22T20:39:30.000+08:00
20:39:30.003 WARN [o.t.c.d.Manager] nextFirstSlotTime:[2018-03-22T17:57:20.001+08:00],now[2018-03-22T20:39:30.052+08:00]
20:39:30.003 INFO [o.t.c.s.WitnessService] ScheduledWitness[6c22c1af7bfbb2b0e07148ecba27b56f81a54fcf],slot[1947]
20:39:30.003 INFO [o.t.c.s.WitnessService] It's not my turn
20:39:30.003 INFO [o.t.c.s.WitnessService] Sleep : 4997 ms,next time:2018-03-22T20:39:35.000+08:00
20:39:33.803 INFO [o.t.c.n.n.NodeImpl] other peer is nil, please wait ... 
20:39:35.005 WARN [o.t.c.d.Manager] nextFirstSlotTime:[2018-03-22T17:57:20.001+08:00],now[2018-03-22T20:39:35.054+08:00]
20:39:35.005 INFO [o.t.c.s.WitnessService] ScheduledWitness[48e447ec869216de76cfeeadf0db37a3d1c8246d],slot[1948]
20:39:35.005 INFO [o.t.c.s.WitnessService] It's not my turn
20:39:35.005 INFO [o.t.c.s.WitnessService] Sleep : 4995 ms,next time:2018-03-22T20:39:40.000+08:00
20:39:40.005 WARN [o.t.c.d.Manager] nextFirstSlotTime:[2018-03-22T17:57:20.001+08:00],now[2018-03-22T20:39:40.055+08:00]
20:39:40.010 INFO [o.t.c.d.Manager] postponedTrxCount[0],TrxLeft[0]
20:39:40.022 INFO [o.t.c.d.DynamicPropertiesStore] update latest block header id = fd30a16160715f3ca1a5bcad18e81991cd6f47265a71815bd2c943129b258cd2
20:39:40.022 INFO [o.t.c.d.SonicxStoreWithRevoking] Address is [108, 97, 116, 101, 115, 116, 95, 98, 108, 111, 99, 107, 95, 104, 101, 97, 100, 101, 114, 95, 104, 97, 115, 104], BytesCapsule is org.sonicx.core.capsule.BytesCapsule@2ce0e954
20:39:40.023 INFO [o.t.c.d.DynamicPropertiesStore] update latest block header number = 140
20:39:40.024 INFO [o.t.c.d.SonicxStoreWithRevoking] Address is [108, 97, 116, 101, 115, 116, 95, 98, 108, 111, 99, 107, 95, 104, 101, 97, 100, 101, 114, 95, 110, 117, 109, 98, 101, 114], BytesCapsule is org.sonicx.core.capsule.BytesCapsule@83924ab
20:39:40.024 INFO [o.t.c.d.DynamicPropertiesStore] update latest block header timestamp = 1521722380001
20:39:40.024 INFO [o.t.c.d.SonicxStoreWithRevoking] Address is [108, 97, 116, 101, 115, 116, 95, 98, 108, 111, 99, 107, 95, 104, 101, 97, 100, 101, 114, 95, 116, 105, 109, 101, 115, 116, 97, 109, 112], BytesCapsule is org.sonicx.core.capsule.BytesCapsule@ca6a6f8
20:39:40.024 INFO [o.t.c.d.Manager] updateWitnessSchedule number:140,HeadBlockTimeStamp:1521722380001
20:39:40.025 WARN [o.t.c.u.RandomGenerator] index[-3] is out of range[0,3],skip
20:39:40.070 INFO [o.t.c.d.SonicxStoreWithRevoking] Address is [73, 72, -62, -24, -89, 86, -39, 67, 112, 55, -36, -40, -57, -32, -57, 61, 86, 12, -93, -115], AccountCapsule is account_name: "Kevin"
address: "IH\302\350\247V\331Cp7\334\330\307\340\307=V\f\243\215"
balance: 9223372036854775387

20:39:40.081 INFO [o.t.c.d.SonicxStoreWithRevoking] Address is [41, -97, 61, -72, 10, 36, -78, 10, 37, 75, -119, -50, 99, -99, 89, 19, 47, 21, 127, 19], AccountCapsule is type: AssetIssue
address: ")\237=\270\n$\262\n%K\211\316c\235Y\023/\025\177\023"
balance: 420

20:39:40.082 INFO [o.t.c.d.SonicxStoreWithRevoking] Address is [76, 65, 84, 69, 83, 84, 95, 83, 79, 76, 73, 68, 73, 70, 73, 69, 68, 95, 66, 76, 79, 67, 75, 95, 78, 85, 77], BytesCapsule is org.sonicx.core.capsule.BytesCapsule@ec1439
20:39:40.083 INFO [o.t.c.d.Manager] there is account List size is 8
20:39:40.084 INFO [o.t.c.d.Manager] there is account ,account address is 448d53b2df0cd78158f6f0aecdf60c1c10b15413
20:39:40.084 INFO [o.t.c.d.Manager] there is account ,account address is 548794500882809695a8a687866e76d4271a146a
20:39:40.084 INFO [o.t.c.d.Manager] there is account ,account address is 48e447ec869216de76cfeeadf0db37a3d1c8246d
20:39:40.084 INFO [o.t.c.d.Manager] there is account ,account address is 55ddae14564f82d5b94c7a131b5fcfd31ad6515a
20:39:40.085 INFO [o.t.c.d.Manager] there is account ,account address is 6c22c1af7bfbb2b0e07148ecba27b56f81a54fcf
20:39:40.085 INFO [o.t.c.d.Manager] there is account ,account address is 299f3db80a24b20a254b89ce639d59132f157f13
20:39:40.085 INFO [o.t.c.d.Manager] there is account ,account address is abd4b9367799eaa3197fecb144eb71de1e049150
20:39:40.085 INFO [o.t.c.d.Manager] there is account ,account address is 4948c2e8a756d9437037dcd8c7e0c73d560ca38d
20:39:40.085 INFO [o.t.c.d.SonicxStoreWithRevoking] Address is [108, 34, -63, -81, 123, -5, -78, -80, -32, 113, 72, -20, -70, 39, -75, 111, -127, -91, 79, -49], WitnessCapsule is org.sonicx.core.capsule.WitnessCapsule@4cb4f7fb
20:39:40.086 INFO [o.t.c.d.SonicxStoreWithRevoking] Address is [41, -97, 61, -72, 10, 36, -78, 10, 37, 75, -119, -50, 99, -99, 89, 19, 47, 21, 127, 19], WitnessCapsule is org.sonicx.core.capsule.WitnessCapsule@7be2474a
20:39:40.086 INFO [o.t.c.d.SonicxStoreWithRevoking] Address is [72, -28, 71, -20, -122, -110, 22, -34, 118, -49, -18, -83, -16, -37, 55, -93, -47, -56, 36, 109], WitnessCapsule is org.sonicx.core.capsule.WitnessCapsule@3e375891
20:39:40.086 INFO [o.t.c.d.SonicxStoreWithRevoking] Address is [68, -115, 83, -78, -33, 12, -41, -127, 88, -10, -16, -82, -51, -10, 12, 28, 16, -79, 84, 19], WitnessCapsule is org.sonicx.core.capsule.WitnessCapsule@55d77b83
20:39:40.090 INFO [o.t.c.d.Manager] countWitnessMap size is 0
20:39:40.091 INFO [o.t.c.d.SonicxStoreWithRevoking] Address is [41, -97, 61, -72, 10, 36, -78, 10, 37, 75, -119, -50, 99, -99, 89, 19, 47, 21, 127, 19], WitnessCapsule is org.sonicx.core.capsule.WitnessCapsule@310dd876
20:39:40.092 INFO [o.t.c.d.SonicxStoreWithRevoking] Address is [72, -28, 71, -20, -122, -110, 22, -34, 118, -49, -18, -83, -16, -37, 55, -93, -47, -56, 36, 109], WitnessCapsule is org.sonicx.core.capsule.WitnessCapsule@151b42bc
20:39:40.092 INFO [o.t.c.d.SonicxStoreWithRevoking] Address is [108, 34, -63, -81, 123, -5, -78, -80, -32, 113, 72, -20, -70, 39, -75, 111, -127, -91, 79, -49], WitnessCapsule is org.sonicx.core.capsule.WitnessCapsule@2d0388aa
20:39:40.092 INFO [o.t.c.d.SonicxStoreWithRevoking] Address is [68, -115, 83, -78, -33, 12, -41, -127, 88, -10, -16, -82, -51, -10, 12, 28, 16, -79, 84, 19], WitnessCapsule is org.sonicx.core.capsule.WitnessCapsule@478a55e7
20:39:40.101 INFO [o.t.c.d.SonicxStoreWithRevoking] Address is [-3, 48, -95, 97, 96, 113, 95, 60, -95, -91, -68, -83, 24, -24, 25, -111, -51, 111, 71, 38, 90, 113, -127, 91, -46, -55, 67, 18, -101, 37, -116, -46], BlockCapsule is BlockCapsule{blockId=fd30a16160715f3ca1a5bcad18e81991cd6f47265a71815bd2c943129b258cd2, num=140, parentId=dadeff07c32d342b941cfa97ba82870958615e7ae73fffeaf3c6a334d81fe3bd, generatedByMyself=true}
20:39:40.102 INFO [o.t.c.d.Manager] save block: BlockCapsule{blockId=fd30a16160715f3ca1a5bcad18e81991cd6f47265a71815bd2c943129b258cd2, num=140, parentId=dadeff07c32d342b941cfa97ba82870958615e7ae73fffeaf3c6a334d81fe3bd, generatedByMyself=true}
20:39:40.102 INFO [o.t.c.s.WitnessService] Block is generated successfully, Its Id is fd30a16160715f3ca1a5bcad18e81991cd6f47265a71815bd2c943129b258cd2,number140 
20:39:40.102 INFO [o.t.c.n.n.NodeImpl] Ready to broadcast a block, Its hash is fd30a16160715f3ca1a5bcad18e81991cd6f47265a71815bd2c943129b258cd2
20:39:40.107 INFO [o.t.c.s.WitnessService] Produced
20:39:40.107 INFO [o.t.c.s.WitnessService] Sleep : 4893 ms,next time:2018-03-22T20:39:45.000+08:00
20:39:43.805 INFO [o.t.c.n.n.NodeImpl] other peer is nil, please wait ... 
20:39:45.002 WARN [o.t.c.d.Manager] nextFirstSlotTime:[2018-03-22T20:39:45.001+08:00],now[2018-03-22T20:39:45.052+08:00]
20:39:45.003 INFO [o.t.c.s.WitnessService] ScheduledWitness[48e447ec869216de76cfeeadf0db37a3d1c8246d],slot[1]
20:39:45.003 INFO [o.t.c.s.WitnessService] It's not my turn
20:39:45.003 INFO [o.t.c.s.WitnessService] Sleep : 4997 ms,next time:2018-03-22T20:39:50.000+08:00
20:39:50.002 WARN [o.t.c.d.Manager] nextFirstSlotTime:[2018-03-22T20:39:45.001+08:00],now[2018-03-22T20:39:50.052+08:00]
20:39:50.003 INFO [o.t.c.s.WitnessService] ScheduledWitness[6c22c1af7bfbb2b0e07148ecba27b56f81a54fcf],slot[2]
20:39:50.003 INFO [o.t.c.s.WitnessService] It's not my turn
20:39:50.003 INFO [o.t.c.s.WitnessService] Sleep : 4997 ms,next time:2018-03-22T20:39:55.000+08:00

```

</details>

* In IntelliJ IDEA
  
<details>
<summary>

Open the configuration panel:

</summary>

![](docs/images/program_configure.png)

</details>  

<details>
<summary>

In the `Program arguments` option, fill in `--witness`:

</summary>

![](docs/images/set_witness_param.jpeg)

</details> 
  
Then, run `FullNode::main()` again.

# Quick Start

Read the [Quick Start](https://developers.sonicx.org/docs/getting-started-1).

# Advanced Configurations

Read the [Advanced Configurations](src/main/java/org/sonicx/core/config/README.md).

# Developer Community

* [Discord](https://discord.gg/GsRgsTD) Please join our Developer Discord
* [Gitter](https://gitter.im/SonicXChain/allcoredev) the core dev gitter

# Links

* [Website](https://www.sonicx.org/)
* [Documentation](http://developers.sonicx.org/)
* [Blog](https://SonicXChain.github.io/sonicx-blog/)

# Projects

* [SONICX Protocol](https://github.com/SonicXChain/protocol)
* [Wallet Client](https://github.com/SonicXChain/sonicx-wallet-cli)
* [Wallet Web](https://github.com/SonicXChain/sonicx-wallet-web)
