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
sudo tcpdump -v -XX  -i lo 'port 9092' -c 1000 -x > dump-inseguro.txt
```
