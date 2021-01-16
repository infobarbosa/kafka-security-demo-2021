## Removendo certificados
```
rm /home/ssl/*
```
## Removendo keytabs
```
rm /home/keytabs/*
```

## Removendo o Kerberos
```
sudo apt purge -y krb5-kdc krb5-admin-server krb5-config krb5-locales krb5-user
```

## Reiniciando o Kafka
```
rm -Rf /var/kafka/data/*
rm -Rf /opt/kafka/logs/*
```
