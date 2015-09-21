package org.dasein.cloud.vsphere;

/*
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;
*/

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.compute.ImageFilterOptions;
import org.dasein.cloud.compute.MachineImage;
import org.dasein.cloud.vsphere.compute.VsphereCompute;
import org.dasein.cloud.vsphere.compute.server.ImageSupport;

import com.vmware.vim25.InvalidPropertyFaultMsg;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.PropertyFilterSpec;
import com.vmware.vim25.PropertySpec;
import com.vmware.vim25.RetrieveOptions;
import com.vmware.vim25.RetrieveResult;
import com.vmware.vim25.RuntimeFaultFaultMsg;

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
    @Mocked
    Logger logger;
    @Mocked
    Vsphere vsphereMock;

    private ObjectManagement om = new ObjectManagement();

    @Test
    public void nop() {
    }

    @Test
    public void testListImages() throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg {

        ImageFilterOptions options = ImageFilterOptions.getInstance();
        final ImageSupport imageSupport = new ImageSupport(vsphereMock);
        final ManagedObjectReference propertyCollector = imageSupport.getPropertyCollector();
        final List<PropertyFilterSpec> fSpecList = imageSupport.getlistImagesPropertyFilterSpec();
        final RetrieveOptions retrieveOptions = new RetrieveOptions();
        final ManagedObjectReference viewManager = imageSupport.getViewManager();
        final ManagedObjectReference rootFolder = imageSupport.getRootFolder();
        
        new NonStrictExpectations(ImageSupport.class) {
            { imageSupport.getViewManager();
                result = om.readJsonFile("src/test/resources/ImageSupport/viewManager.json", ManagedObjectReference.class);
            }

            { imageSupport.getRootFolder();
                result = om.readJsonFile("src/test/resources/ImageSupport/rootFolder.json", ManagedObjectReference.class);
            }

            { imageSupport.createContainerView(viewManager, rootFolder , null, true);
                result = om.readJsonFile("src/test/resources/ImageSupport/containerView.json", ManagedObjectReference.class) ;
            }

            { imageSupport.getPropertyCollector();
                result = om.readJsonFile("src/test/resources/ImageSupport/propertyCollector.json", ManagedObjectReference.class);
            }

            { imageSupport.retrievePropertiesEx(propertyCollector, fSpecList, retrieveOptions);
                result = om.readJsonFile("src/test/resources/ImageSupport/propertiesEx.json", RetrieveResult.class);
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

        //assertNotNull("return should not be null", result);
    }
    
    //@Test
    public void getImage() {


        ImageSupport imageSupport = new ImageSupport();

        new MockUp<ImageSupport>() {
            @Mock
            public ManagedObjectReference getViewManager() {
                return om.readJsonFile("src/test/resources/ImageSupport/viewManager.json", ManagedObjectReference.class);
            }

            @Mock
            private ManagedObjectReference getRootFolder() {
                return om.readJsonFile("src/test/resources/ImageSupport/rootFolder.json", ManagedObjectReference.class);
            }

            @Mock
            private ManagedObjectReference createContainerView(ManagedObjectReference viewManager, ManagedObjectReference rootFolder, List<String> vmList, boolean b) {
                return om.readJsonFile("src/test/resources/ImageSupport/containerView.json", ManagedObjectReference.class);
            }

            @Mock
            private ManagedObjectReference getPropertyCollector() {
                return om.readJsonFile("src/test/resources/ImageSupport/propertyCollector.json", ManagedObjectReference.class);
            }

            @Mock
            private RetrieveResult retrievePropertiesEx(ManagedObjectReference propColl, List<PropertyFilterSpec> fSpecList, RetrieveOptions ro) {
                return om.readJsonFile("src/test/resources/ImageSupport/propertiesEx.json", RetrieveResult.class);
            }
        };

        Iterable<MachineImage> result = null;
        try {
            result = (Iterable<MachineImage>) imageSupport.getImage("dcm-agent-win2012");
            System.out.println("inspect");
        } catch ( CloudException e ) {
            e.printStackTrace();
        } catch ( InternalException e ) {
            e.printStackTrace();
        }

    }

}
