# banana-redis
Bananarama moduile with an adapter for redis. The general idea is to allow simple persistence on redis.

This module does not want to be an ORM; its main purpose is to ensure consistency in the key space

# Roadmap


* Le classi devono avere setter/getter ed un costruttore vuoto
* Mappa solo getter/setter, ignora i fields
* I setter ed i getter devono essere oggetti, non primitive (Double non double)

# TODO

* Prendere l'host di redis da configurazione (classe astratta tipo jpa)
* Rendere customizzabile il campo class che contiene la classe java (customizza sia il nome del field, sia il contenuto, eg: pippuz invece che com.freska.Pippuz) 
* Imposta redis per usare il pool delle connessioni
- [] support for object-to-hashset (map an hashet to an object, only simple properties)
