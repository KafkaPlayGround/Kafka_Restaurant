import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import java.util.Properties

class Producer1 {

    companion object {
        const val BOOTSTRAP_SERVER = "localhost:9092"
        const val TOPIC_NAME = "topic5"
    }
}

fun main() {
    val configs = Properties()
    configs[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = Producer1.BOOTSTRAP_SERVER
    configs[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java.name
    configs[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java.name
    configs[ProducerConfig.ACKS_CONFIG] = "all"
    configs[ProducerConfig.RETRIES_CONFIG] = "100"


    val producer = KafkaProducer<String,String>(configs)

    val message = "first message"

    val record = ProducerRecord<String,String>(Producer1.TOPIC_NAME, message)

    val metadata = producer.send(record).get()

    println("========== $message ${metadata.partition()} ${metadata.offset()}")

    producer.flush()

    producer.close()

}