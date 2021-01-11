# Autorização de Acessos em Kafka

Este é um laboratório rápido mas de máxima importância, sem autorização de acessos qualquer usuário pode ler e escrever em qualquer tópico.</br>
No outro extremo, se você habilitar este recurso mas não der as devidas permissões então apenas super-usuários terão acesso ao cluster.</br>

> É sempre bom lembrar que a [documentação](https://kafka.apache.org/documentation/#security_authz) oficial é sempre a melhor referência.

Vamos começar com o setup do cluster. Se você finalizou o laboratório _kafka-kerberos_ então pode tomá-lo como ponto de partida.</br>
Caso contrário, há um script _Vagrantfile_ pronto pra você subir um ambiente com as configurações de encriptação e autenticação kerberos prontas.
```
vagrant up
```
Vá tomar um café. Leva pelo menos uns 15 minutos para todas as instâncias do lab estarem prontas para uso.

## Teste preliminar

O ideal é fazer um teste antes para se ter uma ideia de antes/depois.</br>
Entre na instância _kafka-client_ e faça o seguinte:
```
vagrant ssh kafka-client

cd aplicacao1

java -cp target/aplicacao1-1.0-SNAPSHOT-jar-with-dependencies.jar com.github.infobarbosa.kafka.SaslAuthenticationProducer
CTRL+C

cd ../aplicacao2

java -cp target/aplicacao2-1.0-SNAPSHOT-jar-with-dependencies.jar com.github.infobarbosa.kafka.SaslAuthenticationConsumer
CTRL+C
```
Se estiver tudo certo então você tem um ambiente operante, ou seja, consegue produzir e consumir mensagens sem erros.</br>
Você também pode deixar as aplicações clientes em execução durante o teste. Isso é bacana pra ter a percepção do que ocorre com a aplicação antes, durante e após a conclusão do processo.

## Configuração dos brokers

Em cada instância de broker, edite o arquivo _server.properties_:

```
vagrant ssh kafka1

vi /etc/kafka/server.properties
```

Acrescente as seguintes linhas:
```
authorizer.class.name=kafka.security.auth.SimpleAclAuthorizer
super.users=User:admin;User:kafka
allow.everyone.if.no.acl.found=false
```

Salve o arquivo, saia e reinicie o kafka:
```
sudo systemctl restart kafka
sudo systemctl status kafka
```

## Autorização de Leitura

Permissões de acesso devem ser feitas através do utilitário _kafka-acls_ que vem junto com a sua instalação de Kafka.</br>
> **Atenção!**</br>
> Caso o comando _kafka-acls_ não seja reconhecido então você pode tentar executar acrescentando o path completo. Por exemplo: $KAFKA_HOME/bin/kafka-acls.sh

O comando abaixo concede a permissão de leitura no tópico **teste** para os usuarios _aplicacao1_ e _aplicacao2_:

```
kafka-acls \
  --authorizer-properties zookeeper.connect=zookeeper1.infobarbosa.github.com:2181/kafka --add \
  --allow-principal User:aplicacao1 \
  --allow-principal User:aplicacao2 \
  --operation Read \
  --group=* \
  --topic teste
```

## Autorização de Escrita
O comando abaixo concede a permissão de escrita no tópico **teste** para o usuario _aplicacao1_ :

```
kafka-acls \
  --authorizer-properties zookeeper.connect=zookeeper1.infobarbosa.github.com:2181/kafka --add \
  --allow-principal User:aplicacao1 \
  --operation Write \
  --topic teste
```

## Listagem das permissões correntes
```
kafka-acls \
  --authorizer-properties zookeeper.connect=zookeeper1.infobarbosa.github.com:2181/kafka \
  --list \
  --topic teste
```

## Remoção de permissões
```
kafka-acls \
  --authorizer-properties zookeeper.connect=zookeeper1.infobarbosa.github.com:2181/kafka --remove \
  --allow-principal User:aplicacao2 \
  --operation Read \
  --topic teste
```

É isso! Espero que você tenha gostado. </br>
Fique à vontade para me mandar suas críticas e sugestões. 

Abraço!

Barbosa @infobarbosa

