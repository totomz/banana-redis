package org.bananarama.crud.redis.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark with this annotation the filed that univocally identify an instance of an object
 * The value of the annotated field is used as key for Redis   
 * @author Tommaso Doninelli
 *
 */
@Target(value = ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RedisKey {

	/**
	 * A pattern that will be used to serialize/deserialize the value of the annotated field in a redis key. 
	 * Use the dollar sign $ as placemark for the value of the field 
	 * @return
	 */
	String value() default "$";

	
}
