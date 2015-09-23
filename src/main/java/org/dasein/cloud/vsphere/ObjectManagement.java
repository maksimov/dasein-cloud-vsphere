package org.dasein.cloud.vsphere;

import java.io.File;
import java.io.IOException;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.DeserializationConfig.Feature;
import org.codehaus.jackson.map.ObjectMapper.DefaultTyping;

public class ObjectManagement {

    public ObjectManagement() { 

    }

    public <T> T readJsonFile(String filename, Class<T> valueType) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(Feature.FAIL_ON_UNKNOWN_PROPERTIES, true);
            mapper.enableDefaultTypingAsProperty(DefaultTyping.OBJECT_AND_NON_CONCRETE, "type");
            T result = (T) mapper.readValue(new File(filename), valueType);
            return result;
        } catch ( Exception e ) { 
            throw new RuntimeException("Unable to read file " + filename, e);
        }
    }

    public void writeJsonFile(Object object, String filename) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            mapper.enableDefaultTypingAsProperty(DefaultTyping.JAVA_LANG_OBJECT, "type");
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(filename), object);
        } catch ( IOException e ) {
            throw new RuntimeException("Unable to write file " + filename, e);
        }
    }
}
