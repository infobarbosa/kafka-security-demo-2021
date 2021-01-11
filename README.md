# Kafka Security | Demo 2021

Este projeto tem como objetivo exercitar as features de segurança do [Kafka](https://kafka.apache.org/).<br/>
Normalmente são esperados três níveis de segurança: [**encriptação**](kafka-ssl/instructions/kafka-ssl-encryption.md), [**autenticação**](kafka-kerberos/instructions/kafka-sasl-authentication.md) e **autorização**.<br/>
Uma instalação inicial do Kafka não habilita qualquer nível de segurança. Portanto, é de responsabilidade do administrador do sistema habilitar tais recursos.<br/>
Não é propósito deste laboratório substituir as documentações disponíveis sobre o tema. As mesmas são claras e objetivas e podem ser encontradas [aqui](https://kafka.apache.org/documentation/#security) e [aqui](https://docs.confluent.io/current/security.html).<br/>
Com o laboratório pretendo simular um cenário onde há basicamente três atores (ou times):

- Desenvolvedor: profissional responsável pelo desenvolvimento da aplicação cliente do Kafka;
- Administrador: profissional responsável pela administração do ambiente Kafka;
- Segurança: profissional responsável pelo manejo de credenciais e permissões de segurança da empresa.

### Download do Kafka
wget https://downloads.apache.org/kafka/2.7.0/kafka_2.13-2.7.0.tgz

### Start do Zookeeper
bin/zookeeper-server-start.sh -daemon config/zookeeper.properties

### Start do Kafka
bin/kafka-server-start.sh -daemon config/server.properties

#### Listagem de tópicos
bin/kafka-topics.sh --zookeeper localhost:2181 --list

#### Tópico teste
bin/kafka-topics.sh --zookeeper localhost:2181 --create --topic teste --partitions 1  --replication-factor 1

#### Descrevendo o tópico teste
bin/kafka-topics.sh --zookeeper localhost:2181 --describe --topic teste

## Primeiro teste (PLAINTEXT)

#### Janela 1
```
cd ~/aplicacao1
mvn clean package
java -cp target/aplicacao1-1.0-SNAPSHOT-jar-with-dependencies.jar 
[Control + c]
```

#### Janela 2
```
cd ~/aplicacao2
mvn clean package
java -cp target/aplicacao2-1.0-SNAPSHOT-jar-with-dependencies.jar
```

#### Janela 3

Executar um "tcpdump" para "escutar" o conteúdo trafegado.<br/>

Esse comando pode ser executado tanto no servidor do Kafka como na aplicação cliente 
```
sudo tcpdump -v -XX  -i lo 'port 9092' -c 10
sudo tcpdump -v -XX  -i lo 'port 9092' -c 100 -w dump-inseguro.txt
```
