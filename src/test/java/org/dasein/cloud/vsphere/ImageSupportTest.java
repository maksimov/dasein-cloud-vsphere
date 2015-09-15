package org.dasein.cloud.vsphere;


import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.compute.ImageFilterOptions;
import org.dasein.cloud.compute.MachineImage;
import org.dasein.cloud.vsphere.compute.server.ImageSupport;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vmware.vim25.InvalidPropertyFaultMsg;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.PropertyFilterSpec;
import com.vmware.vim25.RetrieveOptions;
import com.vmware.vim25.RetrieveResult;
import com.vmware.vim25.RuntimeFaultFaultMsg;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * User: rogerunwin
 * Date: 14/09/2015
 */
@RunWith(JUnit4.class)
public class ImageSupportTest {

    private ManagedObjectReference getMockViewManager() {
        ObjectMapper mapper = new ObjectMapper();
        ManagedObjectReference mor = null;
        try {
            mor = mapper.readValue(new File("src/test/resources/ImageSupport/viewManager.json"), ManagedObjectReference.class);
        } catch ( Exception e ) { }
        return mor;
    }

    private ManagedObjectReference getMockRootFolder() {
        ObjectMapper mapper = new ObjectMapper();
        ManagedObjectReference rf = null;
        try {
            rf = mapper.readValue(new File("src/test/resources/ImageSupport/rootFolder.json"), ManagedObjectReference.class);
        } catch ( Exception e ) { }
        return rf;
    }

    private ManagedObjectReference createMockContainerView() {
        ObjectMapper mapper = new ObjectMapper();
        ManagedObjectReference mor = null;
        try {
            mor = mapper.readValue(new File("src/test/resources/ImageSupport/containerView.json"), ManagedObjectReference.class);
        } catch ( Exception e ) { }
        return mor;
    }

    private ManagedObjectReference getMockPropertyCollector() {
        ObjectMapper mapper = new ObjectMapper();
        ManagedObjectReference mor = null;
        try {
            mor = mapper.readValue(new File("src/test/resources/ImageSupport/propertyCollector.json"), ManagedObjectReference.class);
        } catch ( Exception e ) { }
        return mor;
    }

    private RetrieveResult getMockRetrievePropertiesEx() {
        ObjectMapper mapper = new ObjectMapper();
        RetrieveResult rr = null;
        try {
            rr = mapper.readValue(new File("src/test/resources/ImageSupport/propertiesEx.json"), RetrieveResult.class);
        } catch ( Exception e ) { }
        return rr;
    }

    @Test
    public void listImagesTest() {
        ImageSupport imageSupport = mock(ImageSupport.class, CALLS_REAL_METHODS);
        ImageFilterOptions options = ImageFilterOptions.getInstance();
        doReturn(getMockViewManager()).when(imageSupport).getViewManager();
        doReturn(getMockRootFolder()).when(imageSupport).getRootFolder();
        
        try {
            doReturn(createMockContainerView()).when(imageSupport).createContainerView(any(ManagedObjectReference.class), any(ManagedObjectReference.class), anyList(), eq(true));
        } catch ( RuntimeFaultFaultMsg e ) { 
            System.out.println(e);
        }

        doReturn(getMockPropertyCollector()).when(imageSupport).getPropertyCollector();

        try {
            doReturn(getMockRetrievePropertiesEx()).when(imageSupport).retrievePropertiesEx(any(ManagedObjectReference.class), anyListOf(PropertyFilterSpec.class), any(RetrieveOptions.class));
        } catch ( RuntimeFaultFaultMsg e ) {
            e.printStackTrace();
        } catch ( InvalidPropertyFaultMsg e ) { 
            e.printStackTrace();
        }

        Iterable<MachineImage> result = null;
        try {
            //when(imageSupport.listImages(any(ImageFilterOptions.class))).thenCallRealMethod();

            result = imageSupport.listImages(options);
        } catch ( CloudException e ) {
            e.printStackTrace();
        } catch ( InternalException e ) {
            e.printStackTrace();
        }
    }







}
