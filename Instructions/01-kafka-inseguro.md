## Primeiro teste (PLAINTEXT)

#### Janela 1
```
cd ./kafka-inseguro/producer-inseguro
mvn clean package
java -jar target/producer-inseguro.jar
[Control + c]
```

#### Janela 2
```
cd ./kafka-inseguro/consumer-inseguro
mvn clean package
java -jar target/consumer-inseguro.jar
[Control + c]
```

#### Janela 3

Executar um "tcpdump" para "escutar" o conteúdo trafegado.<br/>

Esse comando pode ser executado tanto no servidor do Kafka como na aplicação cliente 
```
sudo tcpdump -v -XX  -i lo 'port 9092' -c 10
sudo tcpdump -v -XX  -i lo 'port 9092' -c 100 -w dump-inseguro.txt
sudo tcpdump -v -XX  -i lo 'port 9092' -c 1000 -x > dump-inseguro.txt
```
