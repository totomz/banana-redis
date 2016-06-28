package org.bananarama.crud;

import org.junit.Test;

/**
 * 
 * @author Tommaso Doninelli
 */
public class NewClass {

    public static void main(String[] args) {
        new NewClass().smain();
    }
    
    @Test
    public void smain() {
        System.out.println("################################");
        System.out.println("################################");
        System.out.println("################################");
        System.out.println("Remove this class when the ide tests are done!");
        System.out.println("################################");
        System.out.println("################################");
        System.out.println("################################");
        
        try {
            throw new RuntimeException("uzzia");
        }
        catch(RuntimeException e){
            e.printStackTrace();
        }
        
    }
}
