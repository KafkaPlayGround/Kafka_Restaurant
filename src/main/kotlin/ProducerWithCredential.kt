import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import java.util.*


fun main() {
    val configs = Properties()
    configs[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = Producer1.BOOTSTRAP_SERVER
    configs[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java.name
    configs[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java.name
    configs["security.protocol"] = "SASL_PLAINTEXT"
    configs["sasl.mechanism"] = "SCRAM-SHA-256"
    configs["sasl.jaas.config"]="org.apache.kafka.common.security.scram.ScramLoginModule required username='alice' password='alice-password';"

    val producer = KafkaProducer<String, String>(configs)

    val message = "SASL message"

    val record = ProducerRecord<String, String>(Producer1.TOPIC_NAME, message)

    val metadata = producer.send(record).get()

    println("========== $message ${metadata.partition()} ${metadata.offset()}")

    producer.flush()

    producer.close()

}