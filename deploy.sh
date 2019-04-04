#!/bin/bash
stestlogname="`date +%Y%m%d%H%M%S`_stest.log"
stest_server=""
docker_num_in_67=`ssh -p 22008 -t SonicX@47.94.231.67 'docker ps -a | wc -l'`
docker_num_in_67=`echo $docker_num_in_67 | tr -d "\r"`
docker_num_in_122=`ssh -p 22008 -t SonicX@47.94.10.122 'docker ps -a | wc -l'`
docker_num_in_122=`echo $docker_num_in_122 | tr -d "\r"`
if [ $docker_num_in_67 -le $docker_num_in_122 ];
  then
  docker_num=$docker_num_in_67
  stest_server=47.94.231.67
  else
    docker_num=$docker_num_in_122
    stest_server=47.94.10.122
fi

if [[ ${docker_num} -le 3 ]];
then
echo $stest_server
else
    stest_server=""
  fi

if [ "$stest_server" = "" ]
then
echo "All docker server is busy, stest FAILED"
exit 1
fi

change_branch_CMD="sed -i '1c branch_name_in_CI=$TRAVIS_BRANCH' /data/workspace/docker_workspace/do_stest.sh"

if [[ "$TRAVIS_BRANCH" = "develop" || "$TRAVIS_BRANCH" = "master" ]];then
  echo "Init the docker stest env"
  echo "'$stest_server' is stest server this time"
  ssh SonicX@$stest_server -p 22008 $change_branch_CMD
  `ssh SonicX@$stest_server -p 22008 sh /data/workspace/docker_workspace/do_stest.sh >$stestlogname 2>&1` &
  sleep 300 && echo $TRAVIS_BRANCH &
  wait
  if [[ `find $stestlogname -type f | xargs grep "Connection refused"` =~ "Connection refused" || `find $stestlogname -type f | xargs grep "stest FAILED"` =~ "stest FAILED" ]];
  then
    rm -f $stestlogname
    echo "first Retry stest task"
    ssh SonicX@$stest_server -p 22008 $change_branch_CMD
    `ssh SonicX@$stest_server -p 22008 sh /data/workspace/docker_workspace/do_stest.sh >$stestlogname 2>&1` &
    sleep 300 && echo $TRAVIS_BRANCH &
    wait
  fi
  if [[ `find $stestlogname -type f | xargs grep "Connection refused"` =~ "Connection refused" || `find $stestlogname -type f | xargs grep "stest FAILED"` =~ "stest FAILED" ]];
  then
    rm -f $stestlogname
    echo "second Retry stest task"
    ssh SonicX@$stest_server -p 22008 $change_branch_CMD
    `ssh SonicX@$stest_server -p 22008 sh /data/workspace/docker_workspace/do_stest.sh >$stestlogname 2>&1` &
    sleep 300 && echo $TRAVIS_BRANCH &
    wait
  fi
  echo "stest start"
  cat $stestlogname | grep "Stest result is:" -A 10000
  echo "stest end"

  echo $?
  ret=$(cat $stestlogname | grep "stest FAILED" | wc -l)

  if [ $ret != 0 ];then
    echo $ret
    rm -f $stestlogname
    exit 1
  fi
fi
echo "bye bye"
echo $stest_server
rm -f $stestlogname
exit 0
