## Removendo certificados
```
rm -Rf /tmp/ssl/*
```
## Removendo keytabs
```
rm -Rf /tmp/keytabs/*
```

## Removendo o Kerberos
```
sudo apt purge -y krb5-kdc krb5-admin-server krb5-config krb5-locales krb5-user
```

## Reiniciando o Kafka
```
rm -Rf /tmp/kafka-logs/*
rm -Rf /opt/kafka/logs/*
rm -Rf /tmp/zookeeper/*

sudo rm /etc/kafka
```
