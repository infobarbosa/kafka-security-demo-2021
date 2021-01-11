# Kafka SSL Encryption Lab

Esse laboratório tem por objetivo exercitar a feature de encriptação do Kafka. O conteúdo completo pode ser encontrado [aqui](https://github.com/infobarbosa/kafka-security).<br/>

## Let's go!

Vamos primeiramente testar as nossas aplicações clientes e constatar o quanto inseguro pode ser tráfego de dados quando o Kafka não habilita encriptação.<br/>


Se ainda não tiver subido o laboratório, essa é a hora:
```
vagrant up
```

## Primeiro teste (PLAINTEXT)

#### Janela 1
```
vagrant ssh kafka-client
cd ~/aplicacao1
mvn clean package
java -cp target/aplicacao1-1.0-SNAPSHOT-jar-with-dependencies.jar com.github.infobarbosa.kafka.PlaintextProducer
[Control + c]
```

#### Janela 2
```
vagrant ssh kafka-client
cd ~/aplicacao2
mvn clean package
java -cp target/aplicacao2-1.0-SNAPSHOT-jar-with-dependencies.jar com.github.infobarbosa.kafka.PlaintextConsumer
```

#### Janela 3

Executar um "tcpdump" para "escutar" o conteúdo trafegado.<br/>

Esse comando pode ser executado tanto na máquina do Kafka (kafka1) como na aplicação cliente (kafka-client)
```
vagrant ssh kafka-client
sudo tcpdump -v -XX  -i enp0s8 -c 10
sudo tcpdump -v -XX  -i enp0s8 -c 100 -w dump.txt
```
Agora que vimos o quanto expostos estão nossos dados, é hora de fazer o setup pra resolver isso com encriptação. <br/>

Abra outra janela e entre na máquina do Kafka:
```
```

## Certificados

Atencao! Esta sessao trata da geracao, assinatura e instalacao dos certificados em keystores e truststores.</br>
Eh uma sessao bem trabalhosa e, na minha opiniao, nao eh onde devemos gastar muita energia pois nao tem a ver com o setup Kafka em si. </br>
Te apresento duas opcoes:
- Pilula vermelha: voce segue este roteiro e entende o que acontece nos bastidores;
- Pilula azul: voce pula direto para a sessao de setup do Kafka.

### Gerando o certificado e a keystore

_Se optou pela pilula vermelha, entao execute todos os passos desta sessao nos tres brokers._ </br>
Atente-se aos nomes dos hosts (FQDN).

```
vagrant ssh kafka1
mkdir -p /home/vagrant/ssl

keytool -genkey -keystore /home/vagrant/ssl/kafka1.server.keystore.jks -validity 365 -storepass easypass -keypass easypass  -dname "CN=kafka1.infobarbosa.github.com" -storetype pkcs12

ls -latrh
```
Vamos checar o conteúdo da keystore
```
keytool -list -v -keystore /home/vagrant/ssl/kafka1.server.keystore.jks -storepass easypass
```

### Certification request

É hora de criar o certification request file. Esse arquivo vamos enviar para a **autoridade certificadora (CA)** pra que seja assinado.
```
keytool -keystore /home/vagrant/ssl/kafka1.server.keystore.jks -certreq -file /home/vagrant/ssl/kafka1-cert-file -storepass easypass -keypass easypass
```

Pra simular o envio do arquivo para a CA, vamos copiar o arquivo **kafka1-cert-file** para a CA via _scp_.
```
scp -o "StrictHostKeyChecking no" /home/vagrant/ssl/kafka1-cert-file vagrant@ca:/home/vagrant/ssl
```

### Assinatura do certificado

Abra outro terminal e entre na máquina da autoridade certificadora:
```
vagrant ssh ca
```
Verifique se o arquivo está acessível:
```
ls -ltrh /home/vagrant/ssl/kafka1-cert-file
```
Tudo pronto! É hora de efetivamente assinar o certificado:

```
openssl x509 -req -CA /home/vagrant/ssl/ca-cert -CAkey /home/vagrant/ssl/ca-key -in /home/vagrant/ssl/kafka1-cert-file -out /home/vagrant/ssl/kafka1-cert-signed -days 365 -CAcreateserial -passin pass:easypass

```
Voltando ao terminal do kafka1, agora vamos simular a devolução do certificado assinado pelo time de segurança para o time de desenvolvimento, o que pode ser feito por email ou por algum processo formal na sua empresa.
```
vagrant ssh kafka1

scp -o "StrictHostKeyChecking no" vagrant@ca:/home/vagrant/ssl/kafka1-cert-signed /home/vagrant/ssl
```

**A chave pública da Autoridade Certificadora**

Para este setup também vamos precisar da chave pública divulgada pela CA.
```
scp -o "StrictHostKeyChecking no" vagrant@ca:/home/vagrant/ssl/ca-cert /home/vagrant/ssl
```

**Verificando o certificado assinado**

Vamos checar o certificado assinado.
```
keytool -printcert -v -file /home/vagrant/ssl/kafka1-cert-signed
```
Se estiver no caminho certo, você vai encontrar as seguintes linhas no output:
```
Owner: CN=kafka1.infobarbosa.github.com
Issuer: CN=Kafka-Security-CA
```
### Instalando os certificados

Antes de seguir, talvez você queira checar a keystore pra ter a visão do antes e depois:
```
keytool -list -v -keystore /home/vagrant/ssl/kafka1.server.keystore.jks -storepass easypass
```
Hora de executar as devidas importações.

***Keystore***

Importando a CA e o certificado assinado para a keystore:
```
keytool -keystore /home/vagrant/ssl/kafka1.server.keystore.jks -alias CARoot -import -file /home/vagrant/ssl/ca-cert -storepass easypass -keypass easypass -noprompt

keytool -keystore /home/vagrant/ssl/kafka1.server.keystore.jks -import -file /home/vagrant/ssl/kafka1-cert-signed -storepass easypass -keypass easypass -noprompt
```
Checando se deu certo:
```
keytool -list -v -keystore /home/vagrant/ssl/kafka1.server.keystore.jks -storepass easypass
```
Se estamos no caminho certo, o output será algo como:
```
...
Alias name: mykey
...
Certificate[1]:
Owner: CN=kafka1.infobarbosa.github.com
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

Cria a truststore e importa o certificado publico da CA (**ca-cert**) para ela:
```
keytool -keystore /home/vagrant/ssl/kafka1.server.truststore.jks -alias CARoot -import -file /home/vagrant/ssl/ca-cert -storepass easypass -keypass easypass -noprompt
```
Checando se deu certo:
```
keytool -list -v -keystore /home/vagrant/ssl/kafka1.server.truststore.jks -storepass easypass

```

## kafka

Se voce tomou a pilula vermelha entao execute os seguintes comandos em cada Kafka:
```
mkdir -p /home/vagrant/ssl

echo "copia a keystore"
scp -o "StrictHostKeyChecking no" vagrant@ca:/home/vagrant/ssl/kafka1.server.keystore.jks /home/vagrant/ssl/kafka.server.keystore.jks

echo "copia a truststore"
scp -o "StrictHostKeyChecking no" vagrant@ca:/home/vagrant/ssl/kafka1.server.truststore.jks /home/vagrant/ssl/kafka.server.truststore.jks
```
_O que voce fez foi basicamente copiar a keystore e truststore que deixei maceteado no host da CA._ </br>
_Se teve dificuldade aqui entao uma boa sugestao eh rever o script Vagrant na sessao de criacao da CA._
</br>
Verifique se estah tudo certo:
```
keytool -list -v -keystore /home/vagrant/ssl/kafka.server.keystore.jks -storepass easypass
keytool -list -v -keystore /home/vagrant/ssl/kafka.server.truststore.jks -storepass easypass
```

Perfeito! Agora vamos ajustar configurações no broker alterando o arquivo **/etc/kafka/server.properties**:
```
vi /etc/kafka/server.properties
```
Substitua as linhas...
```
listeners=PLAINTEXT://0.0.0.0:9092
advertised.listeners=PLAINTEXT://kafka1.infobarbosa.github.com:9092
```
...por:
```
listeners=PLAINTEXT://0.0.0.0:9092,SSL://0.0.0.0:9093
advertised.listeners=PLAINTEXT://kafka1.infobarbosa.github.com:9092,SSL://kafka1.infobarbosa.github.com:9093
```

Acrescente também as linhas abaixo:
```
ssl.keystore.location=/home/vagrant/ssl/kafka.server.keystore.jks
ssl.keystore.password=easypass
ssl.key.password=easypass
ssl.truststore.location=/home/vagrant/ssl/kafka.server.truststore.jks
ssl.truststore.password=easypass
```
**Atente-se aa referencia a correta keystore e truststore de acordo com o broker.**
</br>
Agora reinicie o Kafka.
```
sudo systemctl restart kafka
sudo systemctl status kafka  
```

Um disclaimer sobre a truststore. Ela serve para (obvio!) assinalar quais hosts ou endpoints sao confiaveis. </br>
A rigor, a aplicacao cliente se conecta no Kafka, nao o contrario. Ou seja, a aplicacao cliente precisa "confiar" nos hosts do cluster e assim apenas ela precisaria de uma truststore.
**Entao por que entao eh necessario atribuir uma truststore no broker?"</br>
A razao eh muito simples, lembre-se que quando estamos executando o Kafka em cluster, os brokers se comunicam por varios motivos, incluindo replicacao de dados da partitions leaders para as folowers. Dessa forma um broker1, por exemplo, atua como um cliente do broker2</br>
Quanto importamos o certiticado publico da CA (ca-cert) para a truststore de cada broker estamos dizendo que os mesmos podem confiar em conexoes de hosts que possuam certificado emitido pela mesma autoridade certificadora.
</br>
Verifique se ele começou a responder na porta 9093:
```
sudo grep "EndPoint" /var/log/kafka/server.log
```

Uma outra verificação legal é testar o SSL de uma outra máquina:
```
openssl s_client -connect kafka1.infobarbosa.github.com:9093
openssl s_client -connect kafka2.infobarbosa.github.com:9093
openssl s_client -connect kafka3.infobarbosa.github.com:9093
```
Se aparecer CONNECTED, parabéns! </br>

**_Agora repita os mesmos passos para os outros brokers, se houver._ ;)**

## Aplicação cliente

Estamos quase lá! Vamos fazer a configuracao SSL na aplicacao cliente.</br>
Novamente te ofereco duas opcoes:
Pilula vermelha: fazer a geracao e instalacao da truststore manualmente pra ver o que acontece nos bastidores;
Pilula azul: importar a truststore maceteada que deixei na CA pra voce:
```
mkdir -p /home/vagrant/ssl
echo "copia a truststore da aplicacao cliente"
scp -o "StrictHostKeyChecking no" vagrant@ca:/home/vagrant/ssl/kafka.client.truststore.jks /home/vagrant/ssl

```
Se optou pela pilula azul entao pode ir direto para o teste da aplicacao cliente.
Se seu lance eh pilula vermelha entao pode seguir os passos abaixo:
```
vagrant ssh kafka-client

mkdir -p /home/vagrant/ssl

scp -o "StrictHostKeyChecking no" vagrant@ca:/home/vagrant/ssl/ca-cert /home/vagrant/ssl

```
Perceba que vamos utilizar uma chave para a truststore que é diferente da que usamos no servidor.<br/>
Algumas pessoas esperam que seja a mesma do servidor, mas não é necessário.

### Truststore

Gera a truststore importando a chave publica da autoridade certificadora (CA):

```
keytool -keystore /home/vagrant/ssl/kafka.client.truststore.jks -alias CARoot -import -file /home/vagrant/ssl/ca-cert  -storepass weakpass -keypass weakpass -noprompt
```
Agora veja se a importação está OK:
```
keytool -list -v -keystore /home/vagrant/ssl/kafka.client.truststore.jks -storepass weakpass
```

A título de curiosidade, abra as classes SslProducer.java e SslConsumer.java, ambas debaixo de src/main/java/com/github/infobarbosa/kafka, e observe o uso das propriedades abaixo:
```
BOOTSTRAP_SERVERS_CONFIG=kafka1.infobarbosa.github.com:9093
security.protocol=SSL
ssl.truststore.location=/home/vagrant/ssl/kafka.client.truststore.jks
ssl.truststore.password=weakpass
```
Obviamente essas e outras propriedades não devem ser hard coded. Como boa prática devem ser injetadas via arquivo de configuração ou variáveis de ambiente.

## Segundo teste (SSL)

Vamos refazer o teste. Desta vez, utilizando as classes produtora e consumidora que já apontam para o kafka1 na porta 9093
#### Janela 1
```
vagrant ssh kafka-client

cd aplicacao1

java -cp target/aplicacao1-1.0-SNAPSHOT-jar-with-dependencies.jar com.github.infobarbosa.kafka.SslProducer
```

#### Janela 2
```
vagrant ssh kafka-client

cd aplicacao1

java -cp target/aplicacao1-1.0-SNAPSHOT-jar-with-dependencies.jar com.github.infobarbosa.kafka.SslConsumer
```

#### Janela 3. Opcional. tcpdump na porta do servico para "escutar" o conteudo trafegado.

Esse comando pode ser executado tanto na maquina do Kafka (kafka1) como na aplicacao cliente (kafka-client)
Atenção! **enp0s8** é a interface de rede utilizada para host-only na minha máquina.
Se o comando nao funcionar entao verifique quais interfaces estao funcionando via **ifconfig** ou **tcpdump --list-interfaces**
```
sudo -i
tcpdump -v -XX  -i enp0s8 'port 9093'
```
Caso queira enviar o log para um arquivo para analisar melhor:
```
sudo -i
tcpdump -v -XX  -i enp0s8 'port 9093' -w dump.txt -c 1000
```

Lembre-se que deixamos o broker respondendo na porta 9092 (plaintext).<br/>
Fizemos isso por uma razão, nem todas as aplicações têm condições de "rolar" para a porta encriptada imediatamente.<br/>
Desta forma, estabelecemos um tempo de carência para que as aplicações possam fazê-lo de forma gradativa.<br/>
O importante é remover o porta 9092 do listener o mais cedo possível.
Quando tiver finalizado sua configuração de listener no broker será parecida com isso:
```
listeners=PLAINTEXT://0.0.0.0:9093
advertised.listeners=PLAINTEXT://kafka1.infobarbosa.github.com:9093
```
## Encriptacao Intebroker

O setup que fizemos ate aqui garante apenas a encripcao de dados entre a aplicacao cliente e o cluster kafka, mas nao a comunicacao entre os diferentes brokers.

Se voce fizer um _tcpdump_ em um broker qualquer vai perceber que a porta 9092 ainda esta em uso. Basicamente eh por essa porta que as particoes _followers_ fazem fetch pra se manterem sincronizadas com as particoes _leaders_.
```
vagrant ssh kafka1
sudo -i
tcpdump -v -XX  -i enp0s8 'port 9092' -w dump9092.txt -c 1000
tcpdump -v -XX  -i enp0s8 'port 9093' -w dump9093.txt -c 1000
```

Para habilitar a encriptacao inclusive entre os brokers, abra o arquivo _server.properties_ e acrescente a seguinte linha.
```
security.inter.broker.protocol=SSL
```
Done! A partir de agora voce tem uma comunicacao totalmente encriptada entre todos os componentes da solucao.

Se tiver dúvidas, me envie! Tentarei ajudar como puder.<br/>
Até o próximo artigo!
