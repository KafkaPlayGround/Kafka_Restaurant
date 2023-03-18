import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import java.time.Duration
import java.util.Properties

class Consumer1 {

    companion object {
        const val BOOTSTRAP_SERVER = "localhost:9092"
        const val TOPIC_NAME = "topic5"
        const val GROUP_ID = "group_one"
    }
}

fun main() {
    val configs = Properties()
    configs[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG]  = Consumer1.BOOTSTRAP_SERVER
    configs[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java.name
    configs[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java.name
    configs[ConsumerConfig.GROUP_ID_CONFIG] = Consumer1.GROUP_ID
    configs[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"

    val consumer = KafkaConsumer<String, String>(configs)
    consumer.subscribe(listOf(Consumer1.TOPIC_NAME))

    while (true) {
        val records = consumer.poll(Duration.ofSeconds(1))

        for (record in records) {
            println("=== $record")
        }
    }
}