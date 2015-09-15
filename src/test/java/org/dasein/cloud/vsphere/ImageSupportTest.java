package org.dasein.cloud.vsphere;

/*
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;
*/

import static org.junit.Assert.*;

import java.io.File;
import java.util.List;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.compute.ImageFilterOptions;
import org.dasein.cloud.compute.MachineImage;
import org.dasein.cloud.vsphere.compute.server.ImageSupport;

import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.PropertyFilterSpec;
import com.vmware.vim25.RetrieveOptions;
import com.vmware.vim25.RetrieveResult;

import org.codehaus.jackson.map.DeserializationConfig.Feature;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectMapper.DefaultTyping;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import mockit.*;

/**
 * User: rogerunwin
 * Date: 14/09/2015
 */
@RunWith(JUnit4.class)
public class ImageSupportTest {


    public <T> T readJsonFile(String filename, Class<T> valueType) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.enableDefaultTypingAsProperty(DefaultTyping.OBJECT_AND_NON_CONCRETE, "type");
        try {
            return (T) mapper.readValue(new File(filename), valueType);
        } catch ( Exception e ) { 
            fail("Unable to read file " + filename + ", error=" + e.getMessage());
        }
        return null;
    }

    @Test
    public void testListImages() {
        ImageFilterOptions options = ImageFilterOptions.getInstance();
        ImageSupport imageSupport = new ImageSupport();

        new MockUp<ImageSupport>() {

            @Mock
            public ManagedObjectReference getViewManager() {
                return readJsonFile("src/test/resources/ImageSupport/viewManager.json", ManagedObjectReference.class);
            }

            @Mock
            private ManagedObjectReference getRootFolder() {
                return readJsonFile("src/test/resources/ImageSupport/rootFolder.json", ManagedObjectReference.class);
            }

            @Mock
            private ManagedObjectReference createContainerView(ManagedObjectReference viewManager, ManagedObjectReference rootFolder, List<String> vmList, boolean b) {
                return readJsonFile("src/test/resources/ImageSupport/containerView.json", ManagedObjectReference.class);
            }

            @Mock
            private ManagedObjectReference getPropertyCollector() {
                return readJsonFile("src/test/resources/ImageSupport/propertyCollector.json", ManagedObjectReference.class);
            }

            @Mock
            private RetrieveResult retrievePropertiesEx(ManagedObjectReference propColl, List<PropertyFilterSpec> fSpecList, RetrieveOptions ro) {
                return readJsonFile("src/test/resources/ImageSupport/propertiesEx.json", RetrieveResult.class);
            }
        };

        Iterable<MachineImage> result = null;
        try {
            result = imageSupport.listImages(options);
        } catch ( CloudException e ) {
            e.printStackTrace();
        } catch ( InternalException e ) {
            e.printStackTrace();
        }

        assertNotNull("return should not be null", result);
    }


}
