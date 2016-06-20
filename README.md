[![Build Status](https://travis-ci.org/totomz/banana-redis.svg?branch=master)](https://travis-ci.org/totomz/banana-redis)

**WARNING - THIS MODULE IS IN A VERY EARLY STAGE!**

I am actively working on it, big changes may happen at any time (and any contribution is welcome!)

# banana-redis
This banana (that is, a [Bananarama](https://github.com/cr0wbar/Bananarama) module) contains a simple adapter for redis.io
This module *does not want to be an ORM*; at now, is more something to keep order in the redis keyspace.

# Usage

Objects are serialized/deserialized using redis HashSets only.

First of all, extend the abstract class `org.bananarama.crud.redis.RedisAdapter`; the method `protected abstract Jedis getJedis();` must be implemented to provide a valid jedis
connection. 

This is a simple class that can be serialized/desderialized using his banana:

```java
package org.bananarama.redis.entities;

import org.bananarama.annotation.Banana;
import org.bananarama.crud.redis.RedisAdapterImpl;
import org.bananarama.crud.redis.annotations.KeyGenerator;

@Banana(adapter = RedisAdapterImpl.class)
public class Host {
    
    // This field contains the id of the instance. No need to annotat ethis field,
    // in Redis we create the key using other methods
    private String hostname;
            
    private String commonProperty;

    public Host() {
    }

    public Host(String hostname) {
        this.hostname = hostname;
    }

    public String getCommonProperty() {
        return commonProperty;
    }

    public void setCommonProperty(String commonProperty) {
        this.commonProperty = commonProperty;
    }

    // This annotation identify the method to use to convert the instance id to 
    // the string to use as key in redis
    @KeyGenerator
    public String getHostnameKey() {
        return "host:" + hostname;
    }
    
    // We have to annotate both the setter and the getter. 
    @KeyGenerator
    public void setHostnameKey(String key) {
        this.hostname = key.split(":")[1];            
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }
    
    
    
}
```
Inheritance is implemented, see the tests. 

# Current limitations

* Fields are ignored, only public properties are serialized/deserialized
* An object can have only simple types (no list or objects as properties)
* Only direct inheritance is supported 
* Instances must be `POJO`

# Roadmap

* Le classi devono avere setter/getter ed un costruttore vuoto
* Mappa solo getter/setter, ignora i fields
* I setter ed i getter devono essere oggetti, non primitive (Double non double)
