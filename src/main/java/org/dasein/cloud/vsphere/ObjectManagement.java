package org.dasein.cloud.vsphere;

import java.io.File;
import java.io.IOException;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.DeserializationConfig.Feature;
import org.codehaus.jackson.map.ObjectMapper.DefaultTyping;

public class ObjectManagement {
    ObjectMapper mapper = null;

    public ObjectManagement() { 
        mapper = new ObjectMapper();
        mapper.configure(Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.enableDefaultTypingAsProperty(DefaultTyping.OBJECT_AND_NON_CONCRETE, "type");
    }

    public <T> T readJsonFile(String filename, Class<T> valueType) {
        //ObjectMapper mapper = new ObjectMapper();
        //mapper.configure(Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        //mapper.enableDefaultTypingAsProperty(DefaultTyping.OBJECT_AND_NON_CONCRETE, "type");
        try {
            return (T) mapper.readValue(new File(filename), valueType);
        } catch ( Exception e ) { 
            throw new RuntimeException("Unable to read file " + filename, e);
        }
    }

    public void writeJsonFile(Object object, String filename) {
        //ObjectMapper mapper = new ObjectMapper().setVisibility(JsonMethod.FIELD, Visibility.ANY);
        //mapper.configure(Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        //mapper.enableDefaultTypingAsProperty(DefaultTyping.OBJECT_AND_NON_CONCRETE, "type");

        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(filename), object);
        } catch ( IOException e ) {
            throw new RuntimeException("Unable to write file " + filename, e);
        }
    }
}
