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
import org.dasein.cloud.ProviderContext;
import org.dasein.cloud.compute.ImageFilterOptions;
import org.dasein.cloud.compute.MachineImage;
import org.dasein.cloud.vsphere.compute.server.ImageSupport;

import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.PropertyFilterSpec;
import com.vmware.vim25.RetrieveOptions;
import com.vmware.vim25.RetrieveResult;
import com.vmware.vim25.RuntimeFaultFaultMsg;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.UserSession;
import com.vmware.vim25.VimPortType;
import com.vmware.vim25.VimService;

import org.junit.Before;
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
    ProviderContext providerContextMock;
    @Mocked
    Vsphere vsphereMock;
    @Mocked
    Logger logger;
    @Mocked
    VsphereConnection connectionMock;

    @Mocked
    VimPortType vimPortMock;
    @Mocked 
    VimService vimServiceMock;
    @Mocked 
    UserSession userSessionMock;
    @Mocked
    ServiceContent serviceContentMock;

    @Mocked
    ManagedObjectReference managedObjectReference;
    
    
    
    
    protected final String ACCOUNT_NO = "TESTACCOUNTNO";
    protected final String REGION = "datacenter-21";
    protected final String ENDPOINT = "TESTENDPOINT";

    @Before
    public void setUp() throws RuntimeFaultFaultMsg {
        final ManagedObjectReference viewManagerResult = om.readJsonFile("src/test/resources/ImageSupport/propertyCollector.json", ManagedObjectReference.class);
        new NonStrictExpectations() {
            { vsphereMock.getContext(); result = providerContextMock; }
        };

        new NonStrictExpectations() {
            { providerContextMock.getAccountNumber(); result = ACCOUNT_NO; }
            { providerContextMock.getRegionId(); result = REGION; }
            { providerContextMock.getEndpoint(); result = ENDPOINT;}
        };

        new NonStrictExpectations() {
            { connectionMock.getVimPort(); result = vimPortMock;}
            { connectionMock.getServiceContent(); result = serviceContentMock; }
            { connectionMock.getUserSession(); result = userSessionMock;}
            { connectionMock.getServiceContent(); result = serviceContentMock;}
        };
        
        new NonStrictExpectations() {
            { serviceContentMock.getViewManager(); result = om.readJsonFile("src/test/resources/ImageSupport/rootFolder.json", ManagedObjectReference.class);}
            { serviceContentMock.getRootFolder(); result = om.readJsonFile("src/test/resources/ImageSupport/rootFolder.json", ManagedObjectReference.class);}
        };
        
        new NonStrictExpectations() {
            { 

                vimPortMock.createContainerView(serviceContentMock.getViewManager(), serviceContentMock.getRootFolder(), new ArrayList<String>(), true); result = om.readJsonFile("src/test/resources/ImageSupport/containerView.json", ManagedObjectReference.class);}    
        };
    }

    private ObjectManagement om = new ObjectManagement();

    @Test
    public void nop() {
    }

    //@Test
    public void testListImages() {

        ImageFilterOptions options = ImageFilterOptions.getInstance();
        ImageSupport imageSupport = new ImageSupport(vsphereMock);


        new MockUp<ImageSupport>() {

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


        ImageSupport imageSupport = new ImageSupport(vsphereMock);

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