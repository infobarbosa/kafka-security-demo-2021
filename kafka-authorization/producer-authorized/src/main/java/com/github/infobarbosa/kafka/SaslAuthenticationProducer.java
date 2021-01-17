package com.github.infobarbosa.kafka;

import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class SaslAuthenticationProducer {
    private final static Logger logger = LoggerFactory.getLogger(SaslAuthenticationProducer.class);

    public static void main(String[] args) {
        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "brubeck:9094");
        properties.put(ProducerConfig.ACKS_CONFIG, "all");
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.CLIENT_ID_CONFIG, "producer-tutorial");
        properties.put(ProducerConfig.LINGER_MS_CONFIG, "100");
        properties.put(ProducerConfig.RETRIES_CONFIG, "3");

        properties.put("security.protocol", "SASL_SSL");
        properties.put("sasl.kerberos.service.name", "kafka");
        properties.put("sasl.jaas.config", "com.sun.security.auth.module.Krb5LoginModule required useKeyTab=true storeKey=true keyTab=\"/tmp/keytabs/producer123.user.keytab\" principal=\"producer123@KAFKA.INFOBARBOSA\";");
        properties.put("ssl.truststore.location", "/tmp/ssl/kafka.client.truststore.jks");
        properties.put("ssl.truststore.password", "senhainsegura");

        KafkaProducer<String, String> producer = new KafkaProducer<String, String>(properties);
        final String topic = "teste";

        ProducerRecord<String, String> record = null;

        for(int i=0; i < 100000; i++){
            String key = "key " + i;
            String value = "[AUTHZ Lab] value " + i + " at ts " + System.currentTimeMillis();

            record = new ProducerRecord<>(topic, key, value);
            producer.send(record, new Callback() {
                @Override
                public void onCompletion(RecordMetadata metadata, Exception exception) {
                    if(exception != null)
                        logger.error("Erro processando a mensagem "  + key, exception.getMessage());
                    else
                        logger.info("K: " + key
                                + ". V: " + value
                                + ". TS: " + metadata.timestamp()
                        );
                }
            });

            //coloca pra dormir um pouco
            try {
                Thread.sleep(100);
            }
            catch(InterruptedException e){
                logger.error("problemas durante o sono.", e);
            }
        }

        try{
            producer.close();
        }
        catch(Exception e){
            logger.error("Problemas fechando o producer.", e);
        }
    }
}
