package org.bananarama.crud.redis;

import org.junit.Test;

import static org.junit.Assert.*;

public class ExtractKeyFromStringTest {

    @Test
    public void testGenerationOfKey() {

        assertEquals("ciao", RedisAdapter.extractKey("uno:ciao:due", "uno:$:due"));
        assertEquals("ciao", RedisAdapter.extractKey("ciao", "$"));
        assertEquals("ciao", RedisAdapter.extractKey("uno:ciao", "uno:$"));
        assertEquals("ciao", RedisAdapter.extractKey("ciao:due", "$:due"));
        assertEquals("ciao", RedisAdapter.extractKey("unociao", "uno$"));
        assertEquals("ciao", RedisAdapter.extractKey("ciaodue", "$due"));
        assertNotEquals("ciao", RedisAdapter.extractKey("ciao:ciao:ciao", "ciao:$due"));
        
        assertEquals("ci$s", RedisAdapter.extractKey("uno:ci$s:due", "uno:$:due"));
    }
        

}
