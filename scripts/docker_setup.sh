#!/usr/bin/env bash

docker kill hadoop-akam-sssc
docker kill kafka-akam-sssc

docker run -d --rm \
--hostname=quickstart.cloudera \
--name=hadoop-akam-sssc \
--privileged=true \
--publish-all -it \
-p 8888:8888 \
-p 7180:7180 \
-p 80:80 \
-p 2181:2181 \
-p 8020:8020 \
-p 60020:60020 \
-p 60000:60000 \
cloudera/quickstart:latest /usr/bin/docker-quickstart

docker run -d --rm \
    --net=host \
    --name=kafka-akam-sssc \
    -e KAFKA_ZOOKEEPER_CONNECT=localhost:2181 \
    -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092 \
    -e KAFKA_BROKER_ID=2 \
    -e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1 \
    confluentinc/cp-kafka:4.0.0

