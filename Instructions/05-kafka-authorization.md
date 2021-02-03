# Autorização de Acessos em Kafka

Este é um laboratório rápido mas de máxima importância, sem autorização de acessos qualquer usuário pode ler e escrever em qualquer tópico.</br>
No outro extremo, se você habilitar este recurso mas não der as devidas permissões então apenas super-usuários terão acesso ao cluster.</br>

> É sempre bom lembrar que a [documentação](https://kafka.apache.org/documentation/#security_authz) oficial é sempre a melhor referência.

Vamos começar com o setup do kafka server.

## Teste preliminar

O ideal é fazer um teste antes para se ter uma ideia de antes/depois.</br>
```
cd [root path do projeto]

java -jar Authorization/producer-authorized/target/producer-authorized.jar
#CTRL+C


java -jar Authorization/consumer-authorized/target/consumer-authorized.jar
CTRL+C
```
Se estiver tudo certo então você tem um ambiente operante, ou seja, consegue produzir e consumir mensagens sem erros.</br>
Você também pode deixar as aplicações clientes em execução durante o teste. Isso é bacana pra ter a percepção do que ocorre com a aplicação antes, durante e após a conclusão do processo.

## Configuração dos brokers
>> Para fins didáticos vou utilizar um segundo arquivo `server.properties.authorized`

Em cada instância de broker, edite o arquivo `server.properties`:

```
vi /etc/kafka/server.properties
```

Acrescente as seguintes linhas:
```
authorizer.class.name=kafka.security.authorizer.AclAuthorizer
super.users=User:admin;User:kafka
ssl.protocol = TLSv1.2
security.inter.broker.protocol=SASL_SSL
allow.everyone.if.no.acl.found=false
```
Ou
```
authorizer.class.name=kafka.security.authorizer.AclAuthorizer
super.users=User:admin;User:kafka
ssl.protocol = TLSv1.2
security.inter.broker.protocol=PLAINTEXT
allow.everyone.if.no.acl.found=true

```
Ou
```
super.users=User:admin;User:kafka;User:ANONYMOUS
```

Precisamos falar desse parâmetro:
```
```

### Etapa 3 - Reiniciando o Kafka
Para reiniciar o Kafka agora vamos incluir via variável de ambiente `KAFKA_OPTS` o arquivo `kafka_server_jaas.conf` que criamos durante o laboratório de *Autenticação*: 
```
export KAFKA_OPTS="-Djava.security.auth.login.config=/opt/kafka/config/kafka_server_jaas.conf"
kafka-server-stop.sh
kafka-server-start.sh /opt/kafka/config/server.properties.authorized
# ou
kafka-server-start.sh -daemon /opt/kafka/config/server.properties.authorized
```


## Autorização de Leitura

Permissões de acesso devem ser feitas através do utilitário `kafka-acls.sh` que vem junto com a sua instalação de Kafka.</br>
> **Atenção!**</br>
> Caso o comando `kafka-acls.sh` não seja reconhecido então você pode tentar executar acrescentando o path completo. Por exemplo: `$KAFKA_HOME/bin/kafka-acls.sh`

O comando abaixo concede a permissão de leitura no tópico **teste** para os usuarios `producer123` e `consumer123`:

```
kafka-acls.sh \
  --authorizer-properties zookeeper.connect=brubeck:2181 --add \
  --allow-principal User:producer123 \
  --allow-principal User:consumer123 \
  --operation Read \
  --group=* \
  --topic teste

kafka-acls.sh \
  --authorizer-properties zookeeper.connect=brubeck:2181 --add \
  --allow-principal User:consumer123 \
  --operation Read \
  --group=* \
  --topic teste 
```

## Autorização de Escrita
O comando abaixo concede a permissão de escrita no tópico **teste** para o usuário `producer123` :

```
kafka-acls.sh \
  --authorizer-properties zookeeper.connect=brubeck:2181 --add \
  --allow-principal User:producer123 \
  --operation Write \
  --topic teste
```

## Listagem das permissões correntes
```
kafka-acls.sh \
  --authorizer-properties zookeeper.connect=brubeck:2181 \
  --list \
  --topic teste
```

## Remoção de permissões
```
kafka-acls.sh \
  --authorizer-properties zookeeper.connect=brubeck:2181 --remove \
  --allow-principal User:producer123 \
  --allow-principal User:consumer123 \
  --operation Read \
  --topic teste

kafka-acls.sh \
  --authorizer-properties zookeeper.connect=brubeck:2181 --remove \
  --allow-principal User:producer123 \
  --allow-principal User:consumer123 \
  --operation Write \
  --topic teste

```

## Quotas
kafka-configs.sh  --zookeeper brubeck:2181 \
      --alter --add-config 'producer_byte_rate=10485760,consumer_byte_rate=20971520' \
      --entity-name producer123 \
      --entity-type clients

É isso! Espero que você tenha gostado. </br>
Fique à vontade para me mandar suas críticas e sugestões. 

Abraço!

Barbosa @infobarbosa

