package org.dasein.cloud.vsphere;

/*
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;
*/

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import mockit.NonStrictExpectations;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.compute.ImageFilterOptions;
import org.dasein.cloud.compute.MachineImage;
import org.dasein.cloud.vsphere.compute.server.ImageSupport;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.PropertyFilterSpec;
import com.vmware.vim25.RetrieveOptions;
import com.vmware.vim25.RetrieveResult;

/**
 * User: rogerunwin
 * Date: 14/09/2015
 */
@RunWith(JUnit4.class)
public class ImageSupportTest extends VsphereTestBase {

    @Before
    public void setUp() throws Exception {
        super.setUp();
        ObjectManagement om = new ObjectManagement();
        final ManagedObjectReference containerView = om.readJsonFile("src/test/resources/ImageSupport/containerView.json", ManagedObjectReference.class);
        final ManagedObjectReference viewManager = om.readJsonFile("src/test/resources/ImageSupport/viewManager.json", ManagedObjectReference.class);
        final ManagedObjectReference rootFolder = om.readJsonFile("src/test/resources/ImageSupport/rootFolder.json", ManagedObjectReference.class);
        final RetrieveResult propertiesEx = om.readJsonFile("src/test/resources/ImageSupport/propertiesEx.json", RetrieveResult.class); // WORKS

        new NonStrictExpectations(){
            { serviceContentMock.getViewManager();
              result = viewManager; }
            { serviceContentMock.getRootFolder();
              result = rootFolder; }
        };

        new NonStrictExpectations(){
            { vimPortMock.createContainerView((ManagedObjectReference)any, (ManagedObjectReference)any, (List<String>)any, anyBoolean); 
              result = containerView; }
            { vimPortMock.retrievePropertiesEx((ManagedObjectReference)any, (List<PropertyFilterSpec>)any, (RetrieveOptions)any); 
              result = propertiesEx; }
        };
    }

    @Test
    public void testListImagesAll() throws CloudException, InternalException {
        final ImageSupport imageSupport = new ImageSupport(vsphereMock);

        Iterable<MachineImage> result = imageSupport.listImages(ImageFilterOptions.getInstance());

        int count = 0;
        for (MachineImage image : result) {
            count++;
            assertNotNull("Returned image ProviderOwnerId should not be null", image.getProviderOwnerId());
            assertNotNull("Returned image ProviderRegionId should not be null", image.getProviderRegionId());
            assertNotNull("Returned image ProviderMachineImageId should not be null", image.getProviderMachineImageId());
            assertNotNull("Returned image ImageClass should not be null", image.getImageClass());
            assertNotNull("Returned image CurrentState should not be null", image.getCurrentState());
            assertNotNull("Returned image Name should not be null", image.getName());
            assertNotNull("Returned image Description should not be null", image.getDescription());
            assertNotNull("Returned image Architecture should not be null", image.getArchitecture());
            assertNotNull("Returned image Platform should not be null", image.getPlatform());
        }
        assertNotNull("return should not be null", result);
        assertTrue("found images should = 12, not " + count, count == 12);
    }

    //@Test
    public void getImage() throws CloudException, InternalException {
        final ImageSupport imageSupport = new ImageSupport(vsphereMock);

        Iterable<MachineImage> result = (Iterable<MachineImage>) imageSupport.getImage("dcm-agent-win2012");
        System.out.println("inspect");


    }

}