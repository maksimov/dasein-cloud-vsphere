package org.dasein.cloud.vsphere.compute.server;


import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.compute.AbstractImageSupport;
import org.dasein.cloud.compute.ImageFilterOptions;
import org.dasein.cloud.compute.MachineImage;
import org.dasein.cloud.util.APITrace;
import org.dasein.cloud.vsphere.Vsphere;
import org.dasein.cloud.vsphere.VsphereConnection;
import org.dasein.cloud.vsphere.capabilities.VsphereImageCapabilities;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.annotate.JsonMethod;
import org.codehaus.jackson.map.DeserializationConfig.Feature;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectMapper.DefaultTyping;


import com.vmware.vim25.DynamicProperty;
import com.vmware.vim25.InvalidPropertyFaultMsg;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.ObjectContent;
import com.vmware.vim25.ObjectSpec;
import com.vmware.vim25.PropertyFilterSpec;
import com.vmware.vim25.PropertySpec;
import com.vmware.vim25.RetrieveOptions;
import com.vmware.vim25.RetrieveResult;
import com.vmware.vim25.RuntimeFaultFaultMsg;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.TraversalSpec;
import com.vmware.vim25.VimPortType;
import com.vmware.vim25.VirtualMachineConfigSummary;
import com.vmware.vim25.VirtualMachineSummary;


/**
 *
 */
public class ImageSupport extends AbstractImageSupport<Vsphere> {
    private Vsphere provider;
    private VsphereImageCapabilities capabilities;
    static private final Logger logger = Vsphere.getLogger(ImageSupport.class);

    private VsphereConnection vsphereConnection;
    private VimPortType vimPort;
    private ServiceContent serviceContent;

    public ImageSupport(Vsphere provider) {
        super(provider);
        this.provider = provider;
        try {
            vsphereConnection = provider.getServiceInstance();
        } catch ( CloudException e ) {
            e.printStackTrace();
        } catch ( InternalException e ) {
            e.printStackTrace();
        }
        vimPort = vsphereConnection.getVimPort();
        serviceContent = vsphereConnection.getServiceContent();
        
    }

    @Override
    public VsphereImageCapabilities getCapabilities() throws CloudException, InternalException {
        if (capabilities == null) {
            capabilities = new VsphereImageCapabilities(provider);
        }
        return capabilities;
    }

    @Override
    public MachineImage getImage(String providerImageId) throws CloudException, InternalException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isSubscribed() throws CloudException, InternalException {
        // TODO Auto-generated method stub
        return false;
    }

    //
    // TODO: Going to need to move these to a VsphereStubs class
    //
    public ManagedObjectReference getViewManager() {
        ManagedObjectReference viewManager = serviceContent.getViewManager();

        ObjectMapper mapper = new ObjectMapper().setVisibility(JsonMethod.FIELD, Visibility.ANY);
        mapper.enableDefaultTypingAsProperty(DefaultTyping.OBJECT_AND_NON_CONCRETE, "type");
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File("src/test/resources/ImageSupport/viewManager.json"), viewManager);
        } catch ( IOException e ) {}

        return viewManager;
    }
    
    public ManagedObjectReference getRootFolder() {
        ManagedObjectReference rootFolder = serviceContent.getRootFolder();

        ObjectMapper mapper = new ObjectMapper().setVisibility(JsonMethod.FIELD, Visibility.ANY);
        mapper.enableDefaultTypingAsProperty(DefaultTyping.OBJECT_AND_NON_CONCRETE, "type");
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File("src/test/resources/ImageSupport/rootFolder.json"), rootFolder);
        } catch ( IOException e ) {}

        return rootFolder;
    }

    public ManagedObjectReference createContainerView(ManagedObjectReference viewManager, ManagedObjectReference rootFolder, List<String> vmList, boolean b) throws RuntimeFaultFaultMsg {
        ManagedObjectReference containerView = vimPort.createContainerView(viewManager, rootFolder, vmList, b);

        ObjectMapper mapper = new ObjectMapper().setVisibility(JsonMethod.FIELD, Visibility.ANY);
        mapper.enableDefaultTypingAsProperty(DefaultTyping.OBJECT_AND_NON_CONCRETE, "type");
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File("src/test/resources/ImageSupport/containerView.json"), containerView);
        } catch ( IOException e ) {}

        return containerView;
    }

    public ManagedObjectReference getPropertyCollector() {
        ManagedObjectReference propertyCollector = serviceContent.getPropertyCollector();

        ObjectMapper mapper = new ObjectMapper().setVisibility(JsonMethod.FIELD, Visibility.ANY);
        mapper.enableDefaultTypingAsProperty(DefaultTyping.OBJECT_AND_NON_CONCRETE, "type");
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File("src/test/resources/ImageSupport/propertyCollector.json"), propertyCollector);
        } catch ( IOException e ) {}

        return propertyCollector;
    }

    public RetrieveResult retrievePropertiesEx(ManagedObjectReference propColl, List<PropertyFilterSpec> fSpecList, RetrieveOptions ro) throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
        RetrieveResult propertiesEx = vimPort.retrievePropertiesEx(propColl, fSpecList, ro);

        ObjectMapper mapper = new ObjectMapper().setVisibility(JsonMethod.FIELD, Visibility.ANY);
        mapper.enableDefaultTypingAsProperty(DefaultTyping.OBJECT_AND_NON_CONCRETE, "type");
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File("src/test/resources/ImageSupport/propertiesEx.json"), propertiesEx);
        } catch ( IOException e ) {}

        return propertiesEx;
    }


    @Override
    public Iterable<MachineImage> listImages(ImageFilterOptions options) throws CloudException, InternalException {
        APITrace.begin(provider, "listImages");
        ArrayList<MachineImage> machineImages = new ArrayList<MachineImage>();
        //
        // need to cope with image filter options...
        //

        try {
            ManagedObjectReference viewManager = getViewManager();

            List<String> vmList = new ArrayList<String>();
            vmList.add("VirtualMachine");

            ManagedObjectReference rootFolder = getRootFolder();

            ManagedObjectReference cViewRef = createContainerView(viewManager, rootFolder, vmList, true);

            // create a traversal spec to select all objects in the view
            TraversalSpec tSpec = new TraversalSpec();
            tSpec.setName("traverseEntities");
            tSpec.setPath("view");
            tSpec.setSkip(false);
            tSpec.setType("ContainerView");

            // create an object spec to define the beginning of the traversal;
            // container view is the root object for this traversal
            ObjectSpec oSpec = new ObjectSpec();
            oSpec.setObj(cViewRef);
            oSpec.setSkip(false);
            oSpec.getSelectSet().add(tSpec);

            // specify the property for retrieval (virtual machine name)
            PropertySpec pSpec = new PropertySpec();
            pSpec.setType("VirtualMachine");
            pSpec.getPathSet().add("name");
            //pSpec.getPathSet().add("summary.guest");
            pSpec.getPathSet().add("summary.config");
            // http://pubs.vmware.com/vsphere-60/topic/com.vmware.wssdk.pg.doc/images/VirtualMachine-VirtualMachineSummary-VMConfig.jpg
            // see above when modifying code to work for listVm's

            PropertyFilterSpec fSpec = new PropertyFilterSpec();
            fSpec.getObjectSet().add(oSpec);
            fSpec.getPropSet().add(pSpec);

            // Create a list for the filters and add the spec to it
            List<PropertyFilterSpec> fSpecList = new ArrayList<PropertyFilterSpec>();
            fSpecList.add(fSpec);

            // get the data from the server
            RetrieveResult props = retrievePropertiesEx(getPropertyCollector(), fSpecList, new RetrieveOptions());
            
            ObjectMapper mapper = new ObjectMapper().setVisibility(JsonMethod.FIELD, Visibility.ANY);
            mapper.configure(Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            mapper.enableDefaultTypingAsProperty(DefaultTyping.OBJECT_AND_NON_CONCRETE, "type");
            
            //System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(props));
            //RetrieveResult mockProps = mapper.readValue(mapper.writeValueAsString(props), RetrieveResult.class);
            RetrieveResult rr = mapper.readValue(new File("src/test/resources/ImageSupport/propertiesEx.json"), RetrieveResult.class);
            if (rr != null) {
                for (ObjectContent oc : (List<ObjectContent>)rr.getObjects()) {
                    String name = null;
                    Object value = null;
                    List<DynamicProperty> dps = (List<DynamicProperty>)oc.getPropSet();
                    if (dps != null) {
                         VirtualMachineSummary virtualMachineSummary = null;
                         VirtualMachineConfigSummary virtualMachineConfigSummary = null;
                         for (DynamicProperty dp : dps) {
                             name = dp.getName();
                             if (name.equals("summary")) {
                                 virtualMachineSummary = (VirtualMachineSummary) dp.getVal();

                                 if (virtualMachineSummary.getConfig().isTemplate() == false) {
                                     break;
                                 }
                                 System.out.println("TEMPLATE!");


                             } else if (name.equals("summary.config")) {
                                 virtualMachineConfigSummary = (VirtualMachineConfigSummary) dp.getVal();
                                 if (virtualMachineConfigSummary.isTemplate() == false) {
                                     break;
                                 }
                                 System.out.println("TEMPLATE!");
                                 //virtualMachineConfigSummary = (VirtualMachineConfigSummary)dp.getVal();
                                 //if (virtualMachineConfigSummary.isTemplate() == false) {
                                 //    break;
                                 //}
                                 //String guestId = virtualMachineConfigSummary.getGuestId();
                                 //String guestUUID = virtualMachineConfigSummary.getInstanceUuid();
                                 //System.out.println("inspect");
                             } else {
                                 value = dp.getVal();
                                 System.out.println(name + " = " + value.toString());
                             }
                         }
                     }
                 }
                /*
                 * datacenter-21 or domain-c26
                MachineImage image = MachineImage.getInstance(
                        ownerId, 
                        regionId, // provider.getServiceInstance(); should betray this.
                        imageId, // guestId/guestUuid
                        imageClass, // always vmx
                        state, // found
                        name, // guestId/guestUuid
                        description, // guest full name
                        architecture, // featureRequirement could betray cpu type...
                        platform) // guestId/guestFullName
                */
                MachineImage machineImage = null; //MachineImage.getInstance("UNKNOWN", regionId, imageId, imageClass, state, name, description, architecture, platform).
                machineImages.add(machineImage);
             }
             
            

            
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e);
        }
        finally {
            APITrace.end();
        }
        
        
        return null;
    }







    @Override
    public void remove(String providerImageId, boolean checkState) throws CloudException, InternalException {
        // TODO Auto-generated method stub
    }


}
