package org.bananarama.crud.redis;

import org.bananarama.crud.redis.entities.DigitalOcean;
import org.bananarama.crud.redis.entities.GoogleHost;

/**
 * 
 * @author Tommaso Doninelli
 */
public class Utils {

    public static final String GOOGLE_COMMONPROPVALUE = "lorem impsum";
    public static final String GOOGLE_CREFILEVALUE = "8923y7 9ryfh9 dshfvp9asdh vpz è";
    public static final double GOOGLE_TTL = 742389.7589234;
    
    public static final String DIGITALOCEAN_COMMONPROPVALUE = "Mickey Mouse";
    public static final String DIGITALOCEAN_TOKEN = "jsiosd pfisdu vp9zu dp89v y894yqh jfbak#";
    
    public static GoogleHost generateGoogleHost(String name) {
        GoogleHost google = new GoogleHost(name);
        google.setCommonProperty(GOOGLE_COMMONPROPVALUE);
        google.setCredentialFile(GOOGLE_CREFILEVALUE);
        google.setTtl(GOOGLE_TTL);
        return google;
    }
    
    public static DigitalOcean generateDigitalOceanHost(String name) {
        DigitalOcean ocean = new DigitalOcean(name);    
        ocean.setCommonProperty("ocean-common");
        ocean.setToken("08f97sa0f9yds908gb");
        
        return ocean;
    }
    
    
}
