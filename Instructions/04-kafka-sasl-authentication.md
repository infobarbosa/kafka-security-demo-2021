# Kafka com autenticação SASL/GSSAPI 
> Atenção! Esse lab considera minha máquina cujo hostname é `brubeck` e a interface de rede `lo`.
> Caso queira reproduzir, será necessário fazer as devidas adaptações para o seu hostname. ;)

## Instalando o Kerberos
> **Atenção! Risco de perda de dados e contas! Execute por sua conta e risco.**

> https://help.ubuntu.com/community/Kerberos

```
sudo apt install krb5-kdc krb5-admin-server krb5-config -y
```
Durante a instalação devem ser informados o REALM e os servidores Kerberos e Admin.<br> 
Os valores que informei para esse lab foram `KAFKA.INFOBARBOSA`, `brubeck` e `brubeck`.<br>
Caso queira adaptar o REALM ao seu laboratório, lembre-se de alterar em todos os lugares onde essa configuração for requerida.

Liberando regras de firewall
```
sudo ufw allow 88/tcp
sudo ufw allow 88/udp
```
Caso algo dê errado e queira desinstalar:<br>
> **Atenção! Risco de perda de dados e contas! Execute por sua conta e risco.**
```
sudo apt purge -y krb5-kdc krb5-admin-server krb5-config krb5-locales krb5-user
``` 

#### kadm5.acl
```
sudo -i
cd /etc/krb5kdc
vi kadm5.acl
```
Substituir pelo conteúdo a seguir:
```
*/admin@KAFKA.INFOBARBOSA *
``` 

#### Configurações do database para o REALM KAFKA.INFOBARBOSA
```
sudo -i
kdb5_util create -s -r KAFKA.INFOBARBOSA -P senhainsegura
kadmin.local -q "add_principal -pw senhainsegura admin/admin"

service krb5-kdc restart
service krb5-admin-server restart
```

Caso algo de errado e você queira reiniciar o kerberos database:<br>
> **Atenção! Perigo de perda definitiva de dados e contas. Execute por sua conta e risco!**
```
kdb5_util destroy
```

## Criando as principals
> O trecho abaixo não faz parte do setup do Kerberos.<br>
> Trata-se da criação das principals/credenciais para o laboratório

```
sudo -i
kadmin.local -q "add_principal -randkey producer@KAFKA.INFOBARBOSA"
kadmin.local -q "add_principal -randkey consumer@KAFKA.INFOBARBOSA"
kadmin.local -q "add_principal -randkey admin@KAFKA.INFOBARBOSA"

kadmin.local -q "add_principal -randkey kafka/brubeck.localdomain@KAFKA.INFOBARBOSA"
```

Caso algo dê errado e você queira eliminar uma principal:
```
kadmin.local -q "delete_principal [INFORME AQUI A PRINCIPAL]"
```

## Criando as keytabs
```
sudo -i
mkdir -p /tmp/keytabs
kadmin.local -q "xst -kt /tmp/keytabs/producer123.user.keytab producer123@KAFKA.INFOBARBOSA"
kadmin.local -q "xst -kt /tmp/keytabs/consumer123.user.keytab consumer123@KAFKA.INFOBARBOSA"
kadmin.local -q "xst -kt /tmp/keytabs/admin.user.keytab admin@KAFKA.INFOBARBOSA"
kadmin.local -q "xst -kt /tmp/keytabs/kafka.service.keytab kafka/brubeck.localdomain@KAFKA.INFOBARBOSA"
chown -R barbosa:barbosa /tmp/keytabs
``` 

## Configuração do Kafka

### Etapa 1 - server.properties
>> Para fins didáticos vou utilizar um segundo arquivo `server.properties.sasl-ssl`

Abra o arquivo `server.properties` e adicione o endpoint para SASL
```
vi /etc/kafka/server.properties
```

#### Listeners
Encontre o parâmetro `listeners` abaixo:
```
listeners=PLAINTEXT://0.0.0.0:9092,SSL://0.0.0.0:9093
```

Acrescente um listener para SASL_SSL escutando a porta 9094
```
listeners=PLAINTEXT://0.0.0.0:9092,SSL://0.0.0.0:9093,SASL_SSL://0.0.0.0:9094
```

Agora encontre o parâmetro `advertised.listeners` e faça o mesmo:
```
advertised.listeners=PLAINTEXT://brubeck:9092,SSL://brubeck:9093
```

Altere para:
```
advertised.listeners=PLAINTEXT://brubeck:9092,SSL://brubeck:9093,SASL_SSL://brubeck:9094
```

#### Outras configs do broker
Agora inclua os parâmetros abaixo (sugiro que seja após `advertised.listeners`  pra ficar mais fácil de debugar)
```
sasl.enabled.mechanisms=GSSAPI
sasl.kerberos.service.name=kafka
ssl.client.auth=required
```

`GSSAPI` indica o uso de Kerberos como mecanismo habilitado de autenticação.

`kafka` é a principal que criamos no Kerberos há pouco.

### Etapa 2 - JAAS

Vamos agora configurar um arquivo `kafka_server_jaas.conf` com o seguinte conteúdo:
>> No lab eu considero que o arquivo foi criado debaixo do diretorio `$KAFKA_HOME/config`. 

```
KafkaServer {
    com.sun.security.auth.module.Krb5LoginModule required
    useKeyTab=true
    storeKey=true
    keyTab="/tmp/keytabs/kafka.service.keytab"
    principal="kafka/brubeck@KAFKA.INFOBARBOSA";
};
```

### Etapa 3 - Reiniciando o Kafka
Para reiniciar o Kafka agora vamos incluir via variável de ambiente `KAFKA_OPTS` o arquivo `kafka_server_jaas.conf` que criamos há pouco: 
```
export KAFKA_OPTS="-Djava.security.auth.login.config=/opt/kafka/config/kafka_server_jaas.conf"
kafka-server-start.sh /opt/kafka/config/server.properties.sasl-ssl
```

#### kafka.service (OPCIONAL! Apenas se você configurou o kafka como serviço)

Ajustar o daemon `kafka.service` para que o broker reconheca o arquivo `kafka_server_jaas.conf` durante a inicialização.
```
sudo vi /etc/systemd/system/kafka.service
```

Você deve acrescentar o parâmetro de JVM abaixo através da variável de ambiente `KAFKA_OPTS`.
```
-Djava.security.auth.login.config=/opt/kafka/config/kafka_server_jaas.conf
```

O trecho que agora estah assim:
```
Environment="KAFKA_OPTS= \
	-Dcom.sun.management.jmxremote.authenticate=false \
	-Dcom.sun.management.jmxremote.ssl=false \
	-Djava.rmi.server.hostname=brubeck \
	-Dcom.sun.management.jmxremote.port=9999 \
	-Dcom.sun.management.jmxremote.rmi.port=9999 \
	-Djava.net.preferIPv4Stack=true"
```

Ficará assim:
```
Environment="KAFKA_OPTS= \
	-Dcom.sun.management.jmxremote.authenticate=false \
	-Dcom.sun.management.jmxremote.ssl=false \
	-Djava.rmi.server.hostname=brubeck \
	-Dcom.sun.management.jmxremote.port=9999 \
	-Dcom.sun.management.jmxremote.rmi.port=9999 \
	-Djava.net.preferIPv4Stack=true \
  -Djava.security.auth.login.config=/opt/kafka/config/kafka_server_jaas.conf"
```

### Etapa 4 - Checagem

Status "Running" é só sucesso!

```
sudo journalctl -n 100 -u kafka 

sudo grep EndPoint /var/log/kafka/server.log
```

Encontrando a porta 9094 LISTEN
```
netstat -na | grep 9094
```

Agora no log do Kerberos verifique os logs de autenticação dos brokers:
```
sudo cat /var/log/krb5kdc.log
```

Você deve encontrar uma linha de log de autenticação para cada broker mais ou menos parecido com isso:
```
Jul 07 17:57:09 kerberos.infobarbosa.github.com krb5kdc[5304](info): AS_REQ (4 etypes {18 17 16 23}) 192.168.56.13: ISSUE: authtime 1562522229, etypes {rep=18 tkt=18 ses=18}, kafka/kafka3.infobarbosa.github.com@KAFKA.INFOBARBOSA for krbtgt/KAFKA.INFOBARBOSA@KAFKA.INFOBARBOSA
```

### Etapa 5 - Aplicacao cliente

Essa eh a parte mais facil. Nao eh preciso fazer alteracoes em codigo. :)

Primeiro, abra dois terminais, um para a producer-authorized e outro para a consumer-authorized. Mantenha o terminal para o kerberos aberto tambem.

#### producer-authorized
```
cd producer-authorized

java -cp target/producer-authorized.jar
```

Imediatamente a aplicacao devera iniciar a producao de mensagens no topico 'teste' no Kafka.
As mensagens devem aparecer no console mais ou menos como a seguir:
```
2019-07-07 20:52:17 - K: key 10. V: SASL authentication value 10 at ts 1562532737558. TS: 1562532737558
2019-07-07 20:52:17 - K: key 11. V: SASL authentication value 11 at ts 1562532737658. TS: 1562532737658
2019-07-07 20:52:17 - K: key 12. V: SASL authentication value 12 at ts 1562532737759. TS: 1562532737759
```

Tambem eh interessante olhar os logs. Imediatamente apos o inicio da execucao, as primeiras linhas terao uma indicacao do handshake da aplicacao com o Kerberos. Algo assim:
```
2019-07-07 20:52:15 - Set SASL client state to SEND_APIVERSIONS_REQUEST
2019-07-07 20:52:15 - Creating SaslClient: client=producer-authorized@KAFKA.INFOBARBOSA;service=kafka;serviceHostname=brubeck;mechs=[GSSAPI]
2019-07-07 20:52:15 - Added sensor with name node--1.bytes-sent
2019-07-07 20:52:15 - Added sensor with name node--1.bytes-received
2019-07-07 20:52:15 - Added sensor with name node--1.latency
2019-07-07 20:52:15 - [Producer clientId=producer-tutorial] Created socket with SO_RCVBUF = 32768, SO_SNDBUF = 131072, SO_TIMEOUT = 0 to node -1
2019-07-07 20:52:16 - [Producer clientId=producer-tutorial] Completed connection to node -1. Fetching API versions.
2019-07-07 20:52:16 - SSL handshake completed successfully with peerHost 'brubeck' peerPort 9094 peerPrincipal 'CN=brubeck' cipherSuite 'TLS_DHE_DSS_WITH_AES_256_CBC_SHA256'
2019-07-07 20:52:16 - Set SASL client state to RECEIVE_APIVERSIONS_RESPONSE
2019-07-07 20:52:16 - Set SASL client state to SEND_HANDSHAKE_REQUEST
2019-07-07 20:52:16 - Set SASL client state to RECEIVE_HANDSHAKE_RESPONSE
2019-07-07 20:52:16 - Set SASL client state to INITIAL
2019-07-07 20:52:16 - Set SASL client state to INTERMEDIATE
2019-07-07 20:52:16 - Set SASL client state to CLIENT_COMPLETE
2019-07-07 20:52:16 - Set SASL client state to COMPLETE
2019-07-07 20:52:16 - [Producer clientId=producer-tutorial] Initiating API versions fetch from node -1.
2019-07-07 20:52:16 - [Producer clientId=producer-tutorial] Recorded API versions for node -1: (Produce(0): 0 to 7 [usable: 5], Fetch(1): 0 to 10 [usable: 7], ListOffsets(2): 0 to 5 [usable: 2], Metadata(3): 0 to 7 [usable: 5], LeaderAndIsr(4): 0 to 2 [usable: 1], StopReplica(5): 0 to 1 [usable: 0], UpdateMetadata(6): 0 to 5 [usable: 4], ControlledShutdown(7): 0 to 2 [usable: 1], OffsetCommit(8): 0 to 6 [usable: 3], OffsetFetch(9): 0 to 5 [usable: 3], FindCoordinator(10): 0 to 2 [usable: 1], JoinGroup(11): 0 to 4 [usable: 2], Heartbeat(12): 0 to 2 [usable: 1], LeaveGroup(13): 0 to 2 [usable: 1], SyncGroup(14): 0 to 2 [usable: 1], DescribeGroups(15): 0 to 2 [usable: 1], ListGroups(16): 0 to 2 [usable: 1], SaslHandshake(17): 0 to 1 [usable: 1], ApiVersions(18): 0 to 2 [usable: 1], CreateTopics(19): 0 to 3 [usable: 2], DeleteTopics(20): 0 to 3 [usable: 1], DeleteRecords(21): 0 to 1 [usable: 0], InitProducerId(22): 0 to 1 [usable: 0], OffsetForLeaderEpoch(23): 0 to 2 [usable: 0], AddPartitionsToTxn(24): 0 to 1 [usable: 0], AddOffsetsToTxn(25): 0 to 1 [usable: 0], EndTxn(26): 0 to 1 [usable: 0], WriteTxnMarkers(27): 0 [usable: 0], TxnOffsetCommit(28): 0 to 2 [usable: 0], DescribeAcls(29): 0 to 1 [usable: 0], CreateAcls(30): 0 to 1 [usable: 0], DeleteAcls(31): 0 to 1 [usable: 0], DescribeConfigs(32): 0 to 2 [usable: 1], AlterConfigs(33): 0 to 1 [usable: 0], AlterReplicaLogDirs(34): 0 to 1 [usable: 0], DescribeLogDirs(35): 0 to 1 [usable: 0], SaslAuthenticate(36): 0 to 1 [usable: 0], CreatePartitions(37): 0 to 1 [usable: 0], CreateDelegationToken(38): 0 to 1 [usable: 0], RenewDelegationToken(39): 0 to 1 [usable: 0], ExpireDelegationToken(40): 0 to 1 [usable: 0], DescribeDelegationToken(41): 0 to 1 [usable: 0], DeleteGroups(42): 0 to 1 [usable: 0], UNKNOWN(43): 0)
2019-07-07 20:52:16 - [Producer clientId=producer-tutorial] Sending metadata request (type=MetadataRequest, topics=teste) to node brubeck:9094 (id: -1 rack: null)
2019-07-07 20:52:16 - Cluster ID: 1NkbeldpQxWCi9fM6cGHMg
2019-07-07 20:52:16 - Updated cluster metadata version 2 to Cluster(id = 1NkbeldpQxWCi9fM6cGHMg, nodes = [kafka3.infobarbosa.github.com:9094 (id: 3 rack: r1), brubeck:9094 (id: 1 rack: r1), kafka2.infobarbosa.github.com:9094 (id: 2 rack: r1)], partitions = [Partition(topic = teste, partition = 2, leader = 1, replicas = [2,1,3], isr = [1,2,3], offlineReplicas = []), Partition(topic = teste, partition = 1, leader = 1, replicas = [1,3,2], isr = [1,2,3], offlineReplicas = []), Partition(topic = teste, partition = 4, leader = 1, replicas = [1,2,3], isr = [1,2,3], offlineReplicas = []), Partition(topic = teste, partition = 3, leader = 3, replicas = [3,1,2], isr = [1,2,3], offlineReplicas = []), Partition(topic = teste, partition = 0, leader = 3, replicas = [3,2,1], isr = [1,2,3], offlineReplicas = []), Partition(topic = teste, partition = 10, leader = 1, replicas = [1,2,3], isr = [1,2,3], offlineReplicas = []), Partition(topic = teste, partition = 9, leader = 3, replicas = [3,1,2], isr = [1,2,3], offlineReplicas = []), Partition(topic = teste, partition = 11, leader = 2, replicas = [2,3,1], isr = [1,2,3], offlineReplicas = []), Partition(topic = teste, partition = 6, leader = 3, replicas = [3,2,1], isr = [1,2,3], offlineReplicas = []), Partition(topic = teste, partition = 5, leader = 2, replicas = [2,3,1], isr = [1,2,3], offlineReplicas = []), Partition(topic = teste, partition = 8, leader = 1, replicas = [2,1,3], isr = [1,2,3], offlineReplicas = []), Partition(topic = teste, partition = 7, leader = 1, replicas = [1,3,2], isr = [1,2,3], offlineReplicas = [])])
2019-07-07 20:52:16 - [Producer clientId=producer-tutorial] Initiating connection to node brubeck:9094 (id: 1 rack: r1)
2019-07-07 20:52:16 - Set SASL client state to SEND_APIVERSIONS_REQUEST
2019-07-07 20:52:16 - Creating SaslClient: client=producer-authorized@KAFKA.INFOBARBOSA;service=kafka;serviceHostname=brubeck;mechs=[GSSAPI]
```

#### consumer-authorized
```
cd consumer-authorized

java -cp target/consumer-authorized.jar 
```

Da mesma forma serah possivel checar mensagens nos logs:
```
2019-07-07 21:04:58,277 INFO  [c.g.i.k.SaslAuthenticationConsumer] (main:) K: key 0; V: SASL authentication value 0 at ts 1562533496428; TS: 1562533497786
2019-07-07 21:04:58,384 INFO  [c.g.i.k.SaslAuthenticationConsumer] (main:) K: key 1; V: SASL authentication value 1 at ts 1562533497903; TS: 1562533497904
2019-07-07 21:04:58,489 INFO  [c.g.i.k.SaslAuthenticationConsumer] (main:) K: key 2; V: SASL authentication value 2 at ts 1562533498004; TS: 1562533498004
```

#### kerberos

Agora vamos checar os logs do Kerberos:
```
sudo cat /var/log/krb5kdc.log
```

Perceba as mensagens que apontam a autenticação das aplicacoes clientes em cada broker:

**producer-authorized**
```
Jul 07 20:52:17 kerberos.infobarbosa.github.com krb5kdc[5304](info): TGS_REQ (4 etypes {18 17 16 23}) 192.168.56.14: ISSUE: authtime 1562532736, etypes {rep=18 tkt=18 ses=18}, producer-authorized@KAFKA.INFOBARBOSA for kafka/brubeck@KAFKA.INFOBARBOSA

```

**consumer-authorized**
```
Jul 07 21:04:42 kerberos.infobarbosa.github.com krb5kdc[5304](info): TGS_REQ (4 etypes {18 17 16 23}) 192.168.56.14: ISSUE: authtime 1562533481, etypes {rep=18 tkt=18 ses=18}, consumer-authorized@KAFKA.INFOBARBOSA for kafka/brubeck@KAFKA.INFOBARBOSA
Jul 07 21:04:42 kerberos.infobarbosa.github.com krb5kdc[5304](info): TGS_REQ (4 etypes {18 17 16 23}) 192.168.56.14: ISSUE: authtime 1562533481, etypes {rep=18 tkt=18 ses=18}, consumer-authorized@KAFKA.INFOBARBOSA for kafka/kafka2.infobarbosa.github.com@KAFKA.INFOBARBOSA
Jul 07 21:04:42 kerberos.infobarbosa.github.com krb5kdc[5304](info): TGS_REQ (4 etypes {18 17 16 23}) 192.168.56.14: ISSUE: authtime 1562533481, etypes {rep=18 tkt=18 ses=18}, consumer-authorized@KAFKA.INFOBARBOSA for kafka/kafka3.infobarbosa.github.com@KAFKA.INFOBARBOSA
```

#### Encriptação

Vamos checar se a encriptação continua funcionando.
No segundo terminal para kafka-cliente, encerre a consumer-authorized.
```
[CTRL+c]

sudo -i

sudo tcpdump -v -XX  -i lo 'port 9094' -w dump.txt -c 1000

cat dump.txt
```

Atenção! `lo` é a interface de rede utilizada para host-only na minha máquina.
Se o comando não funcionar entao verifique quais interfaces estao funcionando via `ifconfig` ou `tcpdump --list-interfaces`

Eh isso, pessoal! autenticação kerberos e encriptacao TLS funcionando. Espero que tenha funcionado pra você também.

Até a próxima!

Barbosa