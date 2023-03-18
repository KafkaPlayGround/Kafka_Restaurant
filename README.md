# Kafka_Restaurant

### `실무에서의 카프카 사용하고 있는 예시`

`User Activity Tracking`

- 고객의 페이지 뷰, 클릭 등의 구체적인 행위를 수집하여 고객 행동을 분석/모니터링하고, 이를 통해 기능 개선이나 비즈니스 의사결저으이 중요한 데이터로 활용
- 가능한 한 많이 숮비하여 저장해 놓고 이후 필요에 따라 적절히 가공하여 다양한 용도로 사용
- 데이터 수집은 고객에게 제공할 핵심 가치는 아니므로, 데이터 수집을 위해 Application 성능이나 기능에 영향을 끼쳐서는 안됨.

    (비동기 Batch 전송등을 활용하여 매우 심플하게 처리하는 것이 좋은 선택임)
- 데이터 규모가 매우 크고 폭발적으로 늘어날 수 있음을 고려하여 확장에 유연한 수집/저장 프로세스를 아키텍쳐링 해야함
- 인터넷 네트워크 상의 문제로 수집 서버로 데이터가 전달되지 않을 가능성도 있는만큼, 유실없는 완벽한 수집보다는 빠르고 지속적인 수집에 더 관심(acks=1)
- 사용자 활동 추적은 개인 정보 보호에 영향을 미칠 수 있으므로 수집하는 데이터와 사용 방법을 고객에게 투명하게 공개하고 사용자가 원하는 경우

    거부할 수 있는 옵션을 제공하는 것도 중요

![image](https://user-images.githubusercontent.com/40031858/226094458-736825ec-1c87-47e6-b895-015ba9957d3a.png)

### Stream Processing

- 지속적으로 토픽에 인입되는 이벤트 메시지를 실시간으로 가공하거나 집계, 분할 하는 등의 프로세싱
- 예를 들어
  - User Activity Tracking으로 인입되는 원본 로그 메시지를 재 가공하여 새로운 토픽에 저장
  - IoT 시스템에서 지속적으로 인입되는 이벤트 데이터를 실시간으로 분석
  - Time Window를 적용하여 최근 10분간 집계 데이터를 생성하여 슬랙 채널에 자동으로 리포트
  - 시스템의 문제나 비즈니스 데이터의 문제상황을 실시간으로 캐치하려는 Alarm 발생
- Kafka Streams, Apache Storm, Spark Streaming, Apache Flink


`Kafka Streams Sample`

```java
import org.apache.kafka.streams.kstream.TimeWindows;

// Set up a 5-minute time window
final TimeWindows windowSpec = TimeWindows.of(Duration.ofMinutes(5)).advanceBy(Duration.ofMinutes(1));

// Set up the input and output topics

final StreamsBuilder builder = new StreamsBuilder();
final KStream<String, Long> input = builder.stream("original-topic");
final KTable<Windowed<String>, Long> windowedCounts = input
    .groupByKey()
    .windowedBy(windowSpec)
    .count();

// write the windowed counts to the output topic
windowedCounts.toStream().to("output-topic", Produced.with(Serdes.String(), Serdes.Long()));

// Create the Kafka Streams instance and start it
final KafkaStreams streams = new KafkaStreams(builder.build(), 
streamsConfiguration);
streams.start();

```

---

## 카프카 운영 관점에서 알아야 할 것들

#### `Partition 추가`

```bash
bin/kafka-topics.sh --list --bootstrap-server localhost:9092
bin/kafka-topics.sh --topic <topic-name> --bootstrap-server localhost:9092
bin/kafka-topics.sh --alter --topic <topic-name> --partitions 4 --bootstrap-server localhost:9092
```

#### `운영중인 Kafka Topic이라면 매우 신중하게 결정해야함`

- 서비스 운영중인 Topic에 Partition 추가는 새로운 Partition으로 메세지 rebalance가 되는 과정에서 시스템 성능에 영향을 끼칠 수 있음

    꼭 필요하다면 서비스 임팩트가 상대적으로 작은 시간을 선택해야함

- 실제 해당 Topic의 사용 사례를 고려해서, 필요시 테스트 서버에서 테스트를 해보고 실행해야 함
- 모든 메세지를 RoundRobin 방식으로 처리하고 있다면, 데이터 규모에 따른 지연시간 이후 곧 정상처리가 시작될 수 있지만,

    특정 Key-Patition에 기반한 Consumer를 운영중이라면 메세지의 유실 가능성도 있으므로, 차라리 신규 Topic을

    생성해서 Migration 전략을 짜는 것이 더 나은 선택인 경우가 많음
- 따라서 topic의 최초 생성시, 데이터 확장 규모를 고려하여 partition 개수를 여유있게 설정

### Broker 추가

`신규 Broker의 server.properties 파일 수정`

```properties
broker.id=3
listeners=PLAINTEXT://localhost:9095
log.dir=/tmp/kafka-logs3
```

`신규 Broker생성`

```bash
bin/kafka-server-start.sh config/server.properties &
```

`Partition 재배치를 할 Topic에 대한 json 파일 생성 ex) reassign-topic.json 파일 작성`

```bash
vi reassign-topic.json
-------------------------------------------------------
{"topics":[{"topic":"topic5"}], "version":1}
```

`위에서 생성한 reassign-topic.json 파일을 이용해 최종 Target 구성 json 구조 확인`


```bash
bin/kafka-reassign-partitions.sh --generate --topics-to-move-json-file
reassign-topics.json --broker-list "1,2,3" --bootstrap-server localhost:9092
```

`추가된 Broker를 고려해서 균등한 재배치 제안 json데이터가 생성됨. Proposed~~파일을 new_partition.json으로 저장`

![image](https://user-images.githubusercontent.com/40031858/226095269-3d13b7c5-7447-4cc6-b247-fc3c2448aa16.png)

`Partition 재배치 실행`

```bash
bin/kafka-reassign-partitions.sh --execute --reassignment-json-file
new_partition.json --bootstrap-server localhost:9092
```

`Topic의 Partition 재배치 상태 확인`

![image](https://user-images.githubusercontent.com/40031858/226095325-c91c736f-ba9f-4af3-bcdd-5b7f61897654.png)

### 운영중인 Kafka Cluster라면

- 처리중인 데이터 규모에 따라 Partition 재 배치에 따른 네트워크 사용량과 CPU 사용량 증가에 따른 임팩트가 있을 수 있음
- 따라서, 상대적으로 사용량이 작은 시간을 이요하는 것이 바람직
- 상황에 따라 임시로 retention을 작게 설정하거나, topic을 나눠서 실행해서 부하를 감소시키는 방안을 고려할 수 있음.
