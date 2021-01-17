## Removendo certificados
```
rm /tmp/ssl/*
```
## Removendo keytabs
```
rm /tmp/keytabs/*
```

## Removendo o Kerberos
```
sudo apt purge -y krb5-kdc krb5-admin-server krb5-config krb5-locales krb5-user
```

## Reiniciando o Kafka
```
rm -Rf /tmp/kafka-logs/*
rm -Rf /opt/kafka/logs/*
sudo rm /etc/kafka
```
