# Instalando o Kafka

### Download do Kafka
```
wget https://downloads.apache.org/kafka/2.7.0/kafka_2.13-2.7.0.tgz
tar -xzf kafka_2.13-2.7.0.tgz
mv kafka_2.13-2.7.0 /opt/kafka

sudo -i
echo "export KAFKA_HOME=/opt/kafka" >> /etc/profile
echo "export PATH=\$KAFKA_HOME/bin:\$PATH" >> /etc/profile
ln -s  $KAFKA_HOME/config /etc/kafka
mkdir -p /var/kafka/data
chmod 666 /var/kafka/data
```
### Start do Zookeeper
bin/zookeeper-server-start.sh -daemon config/zookeeper.properties

### Start do Kafka
bin/kafka-server-start.sh -daemon config/server.properties.inseguro

#### Listagem de tópicos
bin/kafka-topics.sh --zookeeper localhost:2181 --list

#### Tópico teste
bin/kafka-topics.sh --zookeeper localhost:2181 --create --topic teste --partitions 1  --replication-factor 1

#### Descrevendo o tópico teste
bin/kafka-topics.sh --zookeeper localhost:2181 --describe --topic teste

