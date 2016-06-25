package org.bananarama.crud.redis;

import org.junit.Test;

import static org.junit.Assert.*;

import javax.sound.midi.Soundbank;

public class GenericTest {

	private String extractKey(String fromString, String pattern) {
		
		int patternPos = pattern.indexOf('$');
		String leftPart = pattern.substring(0, patternPos);
		String rightPart = pattern.substring(patternPos + 1, pattern.length());
		
		String result = fromString.replaceFirst(leftPart, "").replaceFirst(rightPart, "");
		System.out.println(String.format("[%s] - [%s] --> %s", pattern, fromString,  result));
		
		return result;
	}
	
	@Test
	public void testGenerationOfKey() {
		
		assertEquals("ciao", extractKey("uno:ciao:due", "uno:$:due"));
		assertEquals("ciao", extractKey("ciao", "$"));
		assertEquals("ciao", extractKey("uno:ciao", "uno:$"));
		assertEquals("ciao", extractKey("ciao:due", "$:due"));
		assertEquals("ciao", extractKey("unociao", "uno$"));
		assertEquals("ciao", extractKey("ciaodue", "$due"));
		assertNotEquals("ciao", extractKey("ciao:ciao:ciao", "ciao:$due"));
	}
	
}
