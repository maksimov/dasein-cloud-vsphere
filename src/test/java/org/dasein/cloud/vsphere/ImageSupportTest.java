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

import javax.annotation.Nonnull;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.CloudProvider;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.compute.ImageFilterOptions;
import org.dasein.cloud.compute.MachineImage;
import org.dasein.cloud.util.APITrace;
import org.dasein.cloud.vsphere.compute.server.ImageSupport;

import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.PropertyFilterSpec;
import com.vmware.vim25.RetrieveOptions;
import com.vmware.vim25.RetrieveResult;

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

    private ObjectManagement om = new ObjectManagement();

    @Test
    public void nop() {
    }

    @Test
    public void testListImages() {
        ImageFilterOptions options = ImageFilterOptions.getInstance();
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

        new MockUp<APITrace>() {
            @Mock
            public void begin(@Nonnull CloudProvider provider, @Nonnull String operationName) {}

            @Mock
            public void end() { }
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


}
