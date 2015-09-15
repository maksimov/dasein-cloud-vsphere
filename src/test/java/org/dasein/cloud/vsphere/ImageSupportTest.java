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

import com.vmware.vim25.InvalidPropertyFaultMsg;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.PropertyFilterSpec;
import com.vmware.vim25.RetrieveOptions;
import com.vmware.vim25.RetrieveResult;
import com.vmware.vim25.RuntimeFaultFaultMsg;

import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.annotate.JsonMethod;
import org.codehaus.jackson.map.DeserializationConfig.Feature;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectMapper.DefaultTyping;
import org.junit.Ignore;
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

    @Test
    public void testBankProcessAccount() {
        ImageFilterOptions options = ImageFilterOptions.getInstance();
        ImageSupport imageSupport = new ImageSupport();

        new MockUp<ImageSupport>() {

            @Mock
            public ManagedObjectReference getViewManager(){
                ObjectMapper mapper = new ObjectMapper();
                ManagedObjectReference mor = null;
                try {
                    mor = mapper.readValue(new File("src/test/resources/ImageSupport/viewManager.json"), ManagedObjectReference.class);
                } catch ( Exception e ) { }
                return mor;
            }

            @Mock
            private ManagedObjectReference getRootFolder() {
                ObjectMapper mapper = new ObjectMapper();
                ManagedObjectReference rf = null;
                try {
                    rf = mapper.readValue(new File("src/test/resources/ImageSupport/rootFolder.json"), ManagedObjectReference.class);
                } catch ( Exception e ) { }
                return rf;
            }

            @Mock
            private ManagedObjectReference createContainerView(ManagedObjectReference viewManager, ManagedObjectReference rootFolder, List<String> vmList, boolean b) {
                ObjectMapper mapper = new ObjectMapper();
                ManagedObjectReference mor = null;
                try {
                    mor = mapper.readValue(new File("src/test/resources/ImageSupport/containerView.json"), ManagedObjectReference.class);
                } catch ( Exception e ) { }
                return mor;
            }

            @Mock
            private ManagedObjectReference getPropertyCollector() {
                ObjectMapper mapper = new ObjectMapper();
                ManagedObjectReference mor = null;
                try {
                    mor = mapper.readValue(new File("src/test/resources/ImageSupport/propertyCollector.json"), ManagedObjectReference.class);
                } catch ( Exception e ) { }
                return mor;
            }

            @Mock
            private RetrieveResult retrievePropertiesEx(ManagedObjectReference propColl, List<PropertyFilterSpec> fSpecList, RetrieveOptions ro) {
                ObjectMapper mapper = new ObjectMapper();
                mapper.configure(Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                mapper.enableDefaultTypingAsProperty(DefaultTyping.OBJECT_AND_NON_CONCRETE, "type");
                RetrieveResult rr = null;
                try {
                    rr = mapper.readValue(new File("src/test/resources/ImageSupport/propertiesEx.json"), RetrieveResult.class);
                } catch ( Exception e ) { }
                return rr;
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
