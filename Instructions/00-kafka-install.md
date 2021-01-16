# Instalando o Kafka

### Instalação do OpenJDK 11
```
sudo apt-get update -y
sudo apt-get upgrade -y
sudo apt-get install -y openjdk-11-jdk
sudo apt-get install -y wget git net-tools maven
```
### Download do Kafka
```
wget https://downloads.apache.org/kafka/2.7.0/kafka_2.13-2.7.0.tgz
```
### Instalação
```
tar -xzf kafka_2.13-2.7.0.tgz
sudo mv kafka_2.13-2.7.0 /opt/kafka

sudo echo "export KAFKA_HOME=/opt/kafka" >> /etc/profile
sudo echo "export PATH=\$KAFKA_HOME/bin:\$PATH" >> /etc/profile
sudo ln -s  /opt/kafka/config /etc/kafka
reboot
```
### Start do Zookeeper
```
zookeeper-server-start.sh -daemon /etc/kafka/zookeeper.properties
```

### Start do Kafka

No diretório `kafka-security-demo-2021` execute:
```
cp kafka-inseguro/config-files/server.properties.inseguro $KAFKA_HOME/config/
cp kafka-ssl/config-files/server.properties.ssl $KAFKA_HOME/config/
cp kafka-kerberos/config-files/server.properties.sasl-ssl $KAFKA_HOME/config/
ls -altr /etc/kafka/server.properties*
```

Agora vamos iniciar o Kafka inicialmente com a configuração insegura:
```
kafka-server-start.sh -daemon /etc/kafka/server.properties.inseguro
```
#### Listagem de tópicos
bin/kafka-topics.sh --zookeeper localhost:2181 --list

#### Tópico teste
bin/kafka-topics.sh --zookeeper localhost:2181 --create --topic teste --partitions 1  --replication-factor 1

#### Descrevendo o tópico teste
bin/kafka-topics.sh --zookeeper localhost:2181 --describe --topic teste

