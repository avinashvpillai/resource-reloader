package com.avp.resource.reload.resourcereloader;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.StringUtils;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class ResourceReloader implements ResourceLoaderAware{
	
	private ConcurrentMap<String, String> CACHE = new ConcurrentHashMap<>();
	
	@Autowired
	private ResourceLoader resourceLoader;

	@Autowired
	private Environment env;
	
	@Autowired
	private ConfigurableEnvironment cenv;
	
	//@Scheduled(fixedDelay = 10000)
	public void reloadConfiguration() throws IOException {
		ConcurrentMap<String, String> localMap = new ConcurrentHashMap<>();
		String fileName;
		Properties properties = new Properties();
		log.info("Refreshing resource data..");
		String envResourceLoc = env.getProperty("spring.config.location");
        log.info("envResourceLoc {}", envResourceLoc);
        log.info("spring.application.name: {} app.version {}",env.getProperty("spring.application.name"), env.getProperty("app.version"));
        if(!StringUtils.isEmpty(envResourceLoc)) {        	        
            Resource resources = resourceLoader.getResource(env.getProperty("spring.config.location"));      
            InputStream in = resources.getInputStream(); 
            BufferedReader reader = new BufferedReader(new InputStreamReader(in)); 
            while (true) {
                String line = reader.readLine();
                if (line == null)
                    break;
                else {
                	log.info("resource names {}",line);
                	fileName = envResourceLoc+line;
                	log.info("resource path {}",fileName);                	
                    if (null == fileName) {
                        break;
                    } else {
                    	if(fileName.contains("application.properties")) {                    		
                    		fileName = fileName.substring(5);
                    		log.info("resource loading from path {}",fileName);
                    		//properties = PropertiesLoaderUtils.loadAllProperties("application.properties");
                    		//ConfigurableApplicationContext applicationContext = new SpringApplicationBuilder(ResourceReloader.class).properties("spring.config.location:"+fileName).build().run();
                    		properties = readPropertiesFromFileSystem(fileName); 
                    		properties.forEach((key, value) -> localMap.put(key.toString(), value.toString()));
                    	}
                    	                	
                        
                    }
                }
                
            }
            reader.close();
        }else {
        	//log.info("resource loading from local");
        	//fileName = "application.properties";
           // properties = readProperties(fileName);
        }
        updateCache(localMap);
        log.info("new val of version: {}",properties.getProperty("app.version"));
        log.info("spring.application.name aftr loading: {} app.version {}",env.getProperty("spring.application.name"), env.getProperty("app.version"));
	}
	
	private Properties readProperties(String fileName) {
        InputStream stream = null;
        Properties properties = new Properties();
        try {
            log.info("Reading values from properties file {}", fileName);
            stream = this.getClass().getResourceAsStream("/" + fileName);
            properties.load(stream);
        } catch (IOException e) {
            log.error("IO Exception while reading " + fileName, e);
        } finally {
            if (null != stream) {
                try {
                    stream.close();
                } catch (IOException e) {
                    //ignore
                }
            }
        }
        return properties;
    }
	
	private Properties readPropertiesFromFileSystem(String fileName) {
        InputStream stream = null;
        Properties properties = new Properties();
        try {
            log.info("Reading values from properties file {}", fileName);
            stream = new FileInputStream(fileName);
            properties.load(stream);
        } catch (IOException e) {
            log.error("IO Exception while reading " + fileName, e);
            return readProperties(fileName);
        } finally {
            if (null != stream) {
                try {
                    stream.close();
                } catch (IOException e) {
                    //ignore
                }
            }
        }
        return properties;
    }

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
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
