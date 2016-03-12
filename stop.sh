#!/bin/sh
pid=pid
if [ -f "$pid" ]
then
	for line in `cat $pid`
	do

		ps -ef | grep "$line"
		echo "stoping $line ............."
		kill -9 $line 2> /dev/null
		sleep 2
		ps -ef | grep "$line"
		echo 'removing file pid'
		rm -f $pid &> /dev/null
		echo "done!!!!!!!!!"
	done
else
	echo "$pid 不存在"
fi