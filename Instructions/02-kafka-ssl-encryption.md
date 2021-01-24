# Kafka SSL Encryption Lab

Esse laboratório tem por objetivo exercitar a feature de encriptação do Kafka. O conteúdo completo pode ser encontrado [aqui](https://github.com/infobarbosa/kafka-security).<br/>

## Let's go!

Vamos primeiramente testar as nossas aplicações clientes e constatar o quanto inseguro pode ser tráfego de dados quando o Kafka não habilita encriptação.<br/>


## Certificados

Atenção! Esta sessao trata da geracao, assinatura e instalacao dos certificados em keystores e truststores.</br>
Eh uma sessao bem trabalhosa e, na minha opiniao, nao eh onde devemos gastar muita energia pois nao tem a ver com o setup Kafka em si. </br>

### Gerando uma autoridade certificadora (CA)
mkdir -p /tmp/ssl
openssl req -new -newkey rsa:4096 -days 365 -x509 -subj "/CN=Kafka-Security-CA" -keyout /tmp/ssl/ca-key -out /tmp/ssl/ca-cert -nodes

### Gerando o certificado e a keystore

Atente-se aos nomes dos hosts (FQDN).

```
keytool -genkey -keystore /tmp/ssl/kafka.server.keystore.jks -validity 365 -storepass senhainsegura -keypass senhainsegura  -dname "CN=brubeck.localdomain" -storetype pkcs12

ls -latrh
```
Vamos checar o conteúdo da keystore
```
keytool -list -v -keystore /tmp/ssl/kafka.server.keystore.jks -storepass senhainsegura
keytool -list -rfc -keystore /tmp/ssl/kafka.server.keystore.jks -storepass senhainsegura
```

### Certification request

É hora de criar o certification request file. Esse arquivo deve ser enviado para a **autoridade certificadora (CA)** pra que seja assinado.
```
keytool -keystore /tmp/ssl/kafka.server.keystore.jks -certreq -file /tmp/ssl/kafka-cert-file -storepass senhainsegura -keypass senhainsegura
```

### Assinatura do certificado

Verifique se o arquivo está acessível:
```
ls -ltrh /tmp/ssl/kafka-cert-file
```
Assinando o certificado:
```
openssl x509 -req -CA /tmp/ssl/ca-cert -CAkey /tmp/ssl/ca-key -in /tmp/ssl/kafka-cert-file -out /tmp/ssl/kafka-cert-signed -days 365 -CAcreateserial -passin pass:senhainsegura

```

**Verificando o certificado assinado**

Vamos checar o certificado assinado.
```
keytool -printcert -v -file /tmp/ssl/kafka-cert-signed
```

### Instalando os certificados

Antes de seguir, talvez você queira checar a keystore pra ter a visão do antes e depois:
```
keytool -list -v -keystore /tmp/ssl/kafka.server.keystore.jks -storepass senhainsegura
keytool -list -rfc -keystore /tmp/ssl/kafka.server.keystore.jks -storepass senhainsegura
```
Hora de executar as devidas importações.

***Keystore***

Importando a CA e o certificado assinado para a keystore:
```
keytool -keystore /tmp/ssl/kafka.server.keystore.jks -alias CARoot -import -file /tmp/ssl/ca-cert -storepass senhainsegura -keypass senhainsegura -noprompt

keytool -keystore /tmp/ssl/kafka.server.keystore.jks -import -file /tmp/ssl/kafka-cert-signed -storepass senhainsegura -keypass senhainsegura -noprompt
```
Checando se deu certo:
```
keytool -list -v -keystore /tmp/ssl/kafka.server.keystore.jks -storepass senhainsegura
keytool -list -rfc -keystore /tmp/ssl/kafka.server.keystore.jks -storepass senhainsegura
```
Se estamos no caminho certo, o output será algo como:
```
...
Alias name: mykey
...
Certificate[1]:
Owner: CN=brubeck
Issuer: CN=Kafka-Security-CA
...
Certificate[2]:
Owner: CN=Kafka-Security-CA
Issuer: CN=Kafka-Security-CA
```
Seguido de:
```
Alias name: caroot
Owner: CN=Kafka-Security-CA
Issuer: CN=Kafka-Security-CA
```

***Trustore***

Cria a truststore e importa o certificado público da CA (`ca-cert`) para ela:
```
keytool -keystore /tmp/ssl/kafka.server.truststore.jks -alias CARoot -import -file /tmp/ssl/ca-cert -storepass senhainsegura -keypass senhainsegura -noprompt
```
Checando se deu certo:
```
keytool -list -v -keystore /tmp/ssl/kafka.server.truststore.jks -storepass senhainsegura
keytool -list -rfc -keystore /tmp/ssl/kafka.server.truststore.jks -storepass senhainsegura

```

## kafka

Verifique se está tudo certo:
```
keytool -list -v -keystore /tmp/ssl/kafka.server.keystore.jks -storepass senhainsegura
keytool -list -v -keystore /tmp/ssl/kafka.server.truststore.jks -storepass senhainsegura
```

Perfeito! Agora vamos ajustar configurações no broker alterando o arquivo `/etc/kafka/server.properties`:
```
vi /etc/kafka/server.properties
```
Substitua as linhas...
```
listeners=PLAINTEXT://0.0.0.0:9092
advertised.listeners=PLAINTEXT://brubeck.localdomain:9092
```
...por:
```
listeners=PLAINTEXT://0.0.0.0:9092,SSL://0.0.0.0:9093
advertised.listeners=PLAINTEXT://brubeck.localdomain:9092,SSL://brubeck.localdomain:9093
```

Acrescente também as linhas abaixo:
```
ssl.keystore.location=/tmp/ssl/kafka.server.keystore.jks
ssl.keystore.password=senhainsegura
ssl.key.password=senhainsegura
ssl.truststore.location=/tmp/ssl/kafka.server.truststore.jks
ssl.truststore.password=senhainsegura
```
**Atente-se aa referencia a correta keystore e truststore de acordo com o broker.**
</br>
Agora reinicie o Kafka.
```
kafka-server-stop.sh
kafka-server-start.sh /opt/kafka/config/server.properties.ssl
```

Um disclaimer sobre a truststore. Ela serve para (óbvio!) assinalar quais hosts ou endpoints sao confiaveis. </br>
A rigor, a aplicacao cliente se conecta no Kafka, nao o contrario. Ou seja, a aplicacao cliente precisa "confiar" nos hosts do cluster e assim apenas ela precisaria de uma truststore.
**Entao por que entao eh necessario atribuir uma truststore no broker?"</br>
A razao eh muito simples, lembre-se que quando estamos executando o Kafka em cluster, os brokers se comunicam por varios motivos, incluindo replicacao de dados da partitions leaders para as folowers. Dessa forma um broker1, por exemplo, atua como um cliente do broker2</br>
Quanto importamos o certiticado publico da CA (ca-cert) para a truststore de cada broker estamos dizendo que os mesmos podem confiar em conexoes de hosts que possuam certificado emitido pela mesma autoridade certificadora.
</br>
Verifique se ele começou a responder na porta 9093:
```
grep "9093" /opt/kafka/logs/server.log
```

Uma outra verificação legal é testar o SSL de uma outra máquina:
```
openssl s_client -connect brubeck.localdomain:9093
```
Se aparecer CONNECTED, parabéns! </br>

**_Agora repita os mesmos passos para os outros brokers, se houver._ ;)**

## Aplicação cliente

Estamos quase lá! Vamos fazer a configuracao SSL na aplicacao cliente.</br>
Perceba que vamos utilizar uma chave para a truststore que é diferente da que usamos no servidor.<br/>
Algumas pessoas esperam que seja a mesma do servidor, mas não é necessário.

### Truststore

Gera a truststore importando a chave publica da autoridade certificadora (CA):

```
keytool -keystore /tmp/ssl/kafka.client.truststore.jks -alias CARoot -import -file /tmp/ssl/ca-cert  -storepass senhainsegura -keypass senhainsegura -noprompt
```
Agora veja se a importação está OK:
```
keytool -list -v -keystore /tmp/ssl/kafka.client.truststore.jks -storepass senhainsegura
```

A título de curiosidade, abra as classes `SslProducer.java` e `SslConsumer.java`, ambas debaixo de src/main/java/com/github/infobarbosa/kafka, e observe o uso das propriedades abaixo:
```
BOOTSTRAP_SERVERS_CONFIG=brubeck.localdomain:9093
security.protocol=SSL
ssl.truststore.location=/tmp/ssl/kafka.client.truststore.jks
ssl.truststore.password=senhainsegura
```
Obviamente essas e outras propriedades não devem ser hard coded. Como boa prática devem ser injetadas via automação de deployment.

## Segundo teste (SSL)

Vamos refazer o teste. Desta vez, utilizando as classes produtora e consumidora que já apontam para o kafka1 na porta 9093
#### Janela 1
```
cd [root path do projeto]
cd Encryption/producer-ssl/
mvn clean package

java -jar target/producer-ssl.jar
```

#### Janela 2
```
cd [root path do projeto]
cd Encryption/consumer-ssl/
mvn clean package
java -jar target/consumer-ssl.jar
```

#### Janela 3. Opcional. tcpdump na porta do servico para "escutar" o conteudo trafegado.

Esse comando pode ser executado tanto na maquina do Kafka (kafka1) como na aplicacao cliente (kafka-client)
Atenção! **lo** é a interface de rede utilizada para host-only na minha máquina.
Se o comando nao funcionar entao verifique quais interfaces estao funcionando via **ifconfig** ou **tcpdump --list-interfaces**
```
sudo tcpdump -v -XX  -i lo 'port 9093'
```
Caso queira enviar o log para um arquivo para analisar melhor:
```
sudo tcpdump -v -XX  -i lo 'port 9093' -c 1000 -w dump-ssl.txt
sudo tcpdump -v -XX  -i lo 'port 9093' -c 1000 -x > dump-ssl.txt

sudo tcpdump -v -XX  -i lo 'port 9092' -w dump-plaintext.txt -c 1000

```

Lembre-se que deixamos o broker respondendo na porta 9092 (plaintext).<br/>
Fizemos isso por uma razão, nem todas as aplicações têm condições de "rolar" para a porta encriptada imediatamente.<br/>
Desta forma, estabelecemos um tempo de carência para que as aplicações possam fazê-lo de forma gradativa.<br/>
O importante é remover o porta 9092 do listener o mais cedo possível.
Quando tiver finalizado sua configuração de listener no broker será parecida com isso:
```
listeners=PLAINTEXT://0.0.0.0:9093
advertised.listeners=PLAINTEXT://brubeck.localdomain:9093
```
## Encriptacao Intebroker

O setup que fizemos ate aqui garante apenas a encripcao de dados entre a aplicacao cliente e o cluster kafka, mas nao a comunicacao entre os diferentes brokers.

Se voce fizer um `tcpdump` em um broker qualquer vai perceber que a porta 9092 ainda esta em uso. Basicamente eh por essa porta que as particoes _followers_ fazem fetch pra se manterem sincronizadas com as particoes _leaders_.
```
sudo -i
tcpdump -v -XX  -i lo 'port 9092' -w dump9092.txt -c 1000
tcpdump -v -XX  -i lo 'port 9093' -w dump9093.txt -c 1000
```

Para habilitar a encriptação inclusive entre os brokers, abra o arquivo `server.properties` e acrescente a seguinte linha.
```
security.inter.broker.protocol=SSL
```
Done! A partir de agora voce tem uma comunicacao totalmente encriptada entre todos os componentes da solução.

Se tiver dúvidas, me envie! Tentarei ajudar como puder.<br/>
Até o próximo artigo!
