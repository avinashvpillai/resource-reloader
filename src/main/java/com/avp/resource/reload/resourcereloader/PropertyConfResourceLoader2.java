package com.avp.resource.reload.resourcereloader;


import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.builder.fluent.PropertiesBuilderParameters;
import org.apache.commons.configuration2.convert.DefaultListDelimiterHandler;
import org.apache.commons.configuration2.convert.ListDelimiterHandler;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.StringUtils;

import lombok.extern.slf4j.Slf4j;
/*
 * commons-configuration2 based configuration
 */
@Configuration
@Slf4j
public class PropertyConfResourceLoader2 implements ResourceLoaderAware{
	
	private ConcurrentMap<String, String> CACHE = new ConcurrentHashMap<>();
	
	@Autowired
	private ResourceLoader resourceLoader;

	@Autowired
	private Environment env;
	
	@Autowired
	private ConfigurableEnvironment cenv;
	
	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

	@Scheduled(fixedDelay = 10000)
	public void reloadEnvProperties()  {
		ConcurrentMap<String, String> localMap = new ConcurrentHashMap<>();
		String fileName;
		
		PropertiesBuilderParameters propertyParameters = new Parameters().properties();
		ListDelimiterHandler delimiter = new DefaultListDelimiterHandler(',');
		propertyParameters.setThrowExceptionOnMissing(true);
		propertyParameters.setListDelimiterHandler(delimiter);
		FileBasedConfigurationBuilder<PropertiesConfiguration> builder = new FileBasedConfigurationBuilder<PropertiesConfiguration>(PropertiesConfiguration.class);
		
		String envResourceLoc = env.getProperty("spring.config.location");
        log.info("envResourceLoc {}", envResourceLoc);
        if(!StringUtils.isEmpty(envResourceLoc)) {
            Resource resources = resourceLoader.getResource(env.getProperty("spring.config.location"));        
            InputStream in;
			try {
				in = resources.getInputStream();
				BufferedReader reader = new BufferedReader(new InputStreamReader(in)); 
	            while (true) {
	                String line = reader.readLine();
	                if (line == null)
	                    break;
	                else {
	                	log.info("resource names {}",line);
	                	fileName = envResourceLoc+line;
	                	if (null == fileName) {
	                        break;
	                    } else {
	                    	if(fileName.endsWith(".properties")) {                    		
	                    		fileName = fileName.substring(5);
	                    		log.info("resource loading from path {}",fileName);
	                    		File file = new File(fileName);
	                    		propertyParameters.setFile(file);
	                    		builder.configure(propertyParameters);
	                    		PropertiesConfiguration conf = builder.getConfiguration();
	                    		Iterator<String> keys = conf.getKeys();
	                    		log.info("data from loader: spring.application.name: {} app.version {}",conf.getProperty("spring.application.name"), conf.getProperty("app.version"));
	                    		while(keys.hasNext()) {
	                    			String key = keys.next();
	                    			localMap.put(key.toString(), conf.getProperty(key).toString());
	                    			Properties prop = new Properties();
	                    			prop.setProperty(key.toString(), conf.getProperty(key).toString());
	                    			cenv.getPropertySources().addFirst(new org.springframework.core.env.PropertiesPropertySource(key,prop));
	                    			log.info("application.properties :key {} value {}",key, conf.getProperty(key));
	                    		}
	                    	}
	                    }
	                }
	                
	            }
	            reader.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ConfigurationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            
            
        }
        updateCache(localMap);
        log.info("data from local cache: spring.application.name {} app.version {}",getValueString("spring.application.name"), getValueString("app.version"));
        log.info("data from app env: spring.application.name {} app.version {}",env.getProperty("spring.application.name"), env.getProperty("app.version"));
		log.info("**********************************************************************************************************");
	}
	
	public String getValueString(String key, String defaultValue) {
        if (CACHE.containsKey(key)) {
            return CACHE.get(key);
        }
        return defaultValue;
    }

    public String getValueString(String key) {
        return CACHE.get(key);
    }
    
    private synchronized void updateCache(ConcurrentMap<String, String> tempMap) {
        this.CACHE = tempMap;
    }
	
}
