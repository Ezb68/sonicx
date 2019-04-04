#!/bin/bash
kill -9 `cat /home/sonicx/pid.txt`
nohup  java -jar /home/sonicx/SonicXChain/SonicX.jar -p $LOCAL_WITNESS_PRIVATE_KEY --witness -c /home/sonicx/config.conf > /home/sonicx/sonicx-shell.log 2>&1 & echo $! >/home/sonicx/pid.txt