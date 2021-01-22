# Kafka SSL Encryption and Authentication Lab

Esta é a segunda parte de um laboratório que visa exercitar features de segurança do Kafka.
Na [primeira parte](kafka-ssl-encryption.md) trabalhamos a **encriptação** dos dados. Agora vamos exercitar a **encriptação** combinada com **autenticação** utilizando TSL (o antigo SSL), modelo também conhecido como 2-way Authentication.<br/>
No exercício a seguir estou assumindo que você executou a primeira parte. Caso contrário, comece por [aqui](../README.md).<br/>

## Let's go!


## Aplicação Cliente

Atenção! Esta sessão trata da geracao, assinatura e instalação dos certificados em keystore.</br>
Eh uma sessão bem trabalhosa e, na minha opinião, nao é onde devemos gastar muita energia pois não tem a ver com o setup Kafka em si. </br>

### Criando um certificado para a aplicação cliente:
```
keytool -genkey -keystore /tmp/ssl/kafka.client.keystore.jks -validity 365 -storepass senhainsegura -keypass senhainsegura  -dname "CN=brubeck.localdomain" -alias kafka-client -storetype pkcs12

keytool -list -v -keystore /tmp/ssl/kafka.client.keystore.jks -storepass senhainsegura
```

### Criação do request file

Crie o request file que será assinado pela CA
```
keytool -keystore /tmp/ssl/kafka.client.keystore.jks -certreq -file /tmp/ssl/client-cert-sign-request -alias kafka-client -storepass senhainsegura -keypass senhainsegura

```

### Assinatura do certificado

Assine o certificado utilizando a CA:
```
openssl x509 -req -CA /tmp/ssl/ca-cert -CAkey /tmp/ssl/ca-key -in /client-cert-sign-request -out /tmp/ssl/client-cert-signed -days 365 -CAcreateserial -passin pass:senhainsegura
```

> **Atenção** <br/>
> É possível que você tenha problemas de permissão ao aquivo */tmp/ssl/ca-cert.srl*, algo como
```
...
/tmp/ssl/ca-cert.srl: Permission denied
...
```
> Para resolver, basta executar o comando a seguir:
```
sudo chown barbosa:barbosa /tmp/ssl/ca-cert.srl
```
> Pronto! É só executar novamente o comando de assinatura do certificado

### Kafka Cliente, setup da keystore

Vamos checar o certificado assinado.
```
keytool -printcert -v -file /tmp/ssl/client-cert-signed
```
Se o output tiver algo como...
```
Owner: CN=brubeck.localdomain
Issuer: CN=Kafka-Security-CA
```
...então estamos no caminho certo.

Crie a relação de confiaça importanto a chave pública da CA para a Keystore:
```
keytool -keystore /tmp/ssl/kafka.client.keystore.jks -alias CARoot -import -file /tmp/ssl/ca-cert -storepass senhainsegura -keypass senhainsegura -noprompt
```

Agora importe o certificado assinado para a keystore:
```
keytool -keystore /tmp/ssl/kafka.client.keystore.jks -import -file /tmp/ssl/client-cert-signed -alias kafka-client -storepass senhainsegura -keypass senhainsegura -noprompt
```

> **Atenção!**<br/>
> Os dois comandos acima precisam ser executados nessa ordem ou você vai pegar o seguinte erro:
```
...
keytool error: java.lang.Exception: Failed to establish chain from reply
```

Verifique se está tudo lá:
```
keytool -list -v -keystore /tmp/ssl/kafka.client.keystore.jks -storepass senhainsegura
```

O output será algo assim:
```
...
Your keystore contains 2 entries
...
Certificate[1]:
Owner: CN=brubeck.localdomain
Issuer: CN=Kafka-Security-CA
...
Certificate[2]:
Owner: CN=Kafka-Security-CA
Issuer: CN=Kafka-Security-CA
...
```
### Inspecionando o codigo da aplicacao cliente

Dê uma olhada no códido das aplicações cliente.
As classes produtora e consumidora são, respectivamente:
* .../src/main/java/com/github/infobarbosa/kafka/SslAuthenticationProducer.java
* .../src/main/java/com/github/infobarbosa/kafka/SslAuthenticationConsumer.java

Perceba a presença das linhas abaixo:
```
properties.put("ssl.keystore.location", "/tmp/ssl/kafka.client.keystore.jks");
properties.put("ssl.keystore.password", "senhainsegura");
properties.put("ssl.key.password", "senhainsegura");
```

> Obviamente essas e outras propriedades não devem ser hard coded. Como boa prática devem ser injetadas via arquivo de configuração ou variáveis de ambiente.


## Kafka Broker

É necessário fazer um pequeno ajuste no broker, habilitar a propriedade **ssl.client.auth**. <br/>
Entre na máquina do Kafka:
```
vagrant ssh kafka1
```
Abra o arquivo server.properties no seu editor de texto:
```
vi /etc/kafka/server.properties
```
Inclua a nova propriedade **ssl.client.auth**:
```
ssl.client.auth=required
```
Reinicie o Kafka.

## Hora do teste!

#### Janela 1

```
cd ~/aplicacao1
```
Se ainda não tiver feito o build da aplicação, essa é a hora:
```
mvn clean package
```
Agora execute a aplicação produtora:
```
java -cp target/aplicacao1-1.0-SNAPSHOT-jar-with-dependencies.jar com.github.infobarbosa.kafka.SslAuthenticationProducer
```

#### Janela 2

```
cd ~/aplicacao2
```
Se ainda não tiver feito o build da aplicação, essa é a hora:
```
mvn clean package
```
Agora execute a aplicação consumidora:
```
java -cp target/aplicacao2-1.0-SNAPSHOT-jar-with-dependencies.jar com.github.infobarbosa.kafka.SslAuthenticationConsumer
```

#### Janela 3. tcpdump na porta do servico para "escutar" o conteúdo trafegado.

Esse comando pode ser executado tanto na máquina do Kafka (kafka1) como na aplicação cliente (kafka-client)
Atenção! **lo** é a interface de rede (loopback) utilizada na minha máquina.
Se o comando não funcionar então verifique quais interfaces estão em uso via **ifconfig** ou **tcpdump --list-interfaces**
```
sudo tcpdump -v -XX  -i lo
```
Caso queira enviar o log para um arquivo para analisar melhor:
```
sudo -i
tcpdump -v -XX  -i lo -c 100 -w dump-2way-authn.txt
#ou
tcpdump -v -XX  -i lo -c 100 -x > dump-2way-authn.txt 

```

#### Janela 4

Lembra que habilitamos a propriedade "ssl.client.auth" com o valor "required" no Kafka? O impacto desse ajuste pode ser visto aqui.<br/>
A partir de agora as aplicações cliente que tinham apenas a encriptação via SSL não conseguirão mais se comunicar com o cluster Kafka.

```
cd .../aplicacao1
java -cp target/aplicacao1-1.0-SNAPSHOT-jar-with-dependencies.jar com.github.infobarbosa.kafka.SslProducer
```

O resultado será uma mensagem de erro contendo um trecho como esse:
```
org.apache.kafka.common.errors.SslAuthenticationException: SSL handshake failed
Caused by: javax.net.ssl.SSLProtocolException: Handshake message sequence violation, 2
```

#### Janela 5

O mesmo vale para a aplicação consumidora:
```
cd .../aplicacao2
java -cp target/aplicacao2-1.0-SNAPSHOT-jar-with-dependencies.jar com.github.infobarbosa.kafka.SslConsumer
```

## Parabéns!

Você chegou lá! Se tentou executar o laboratório mas não deu certo de primeira, não desanime! Comece novamente e repita até conseguir. ;)

## Último alerta (de novo!)

Perceba que deixamos o broker respondendo na porta 9092 (plaintext).<br/>
Fizemos isso por uma razão, nem todas as aplicações têm condições de "rolar" para a porta encriptada imediatamente.<br/>
Desta forma, estabelecemos um tempo de carência para que as aplicações possam fazê-lo de forma gradativa.<br/>
O importante é remover o porta 9092 do listener o mais cedo possível.
Quando tiver finalizado sua configuração de listener no broker será parecida com isso:
```
listeners=PLAINTEXT://0.0.0.0:9093
advertised.listeners=PLAINTEXT://kafka1.infobarbosa.github.com:9093
```

Se tiver dúvidas, me envie! Tentarei ajudar como puder.<br/>

Até o próxima!
