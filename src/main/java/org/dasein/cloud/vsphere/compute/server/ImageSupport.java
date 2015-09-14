package org.dasein.cloud.vsphere.compute.server;


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

import com.fasterxml.jackson.databind.ObjectMapper;

import com.vmware.vim25.DynamicProperty;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.ObjectContent;
import com.vmware.vim25.ObjectSpec;
import com.vmware.vim25.PropertyFilterSpec;
import com.vmware.vim25.PropertySpec;
import com.vmware.vim25.RetrieveOptions;
import com.vmware.vim25.RetrieveResult;
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

    public ImageSupport(Vsphere provider) {
        super(provider);
        this.provider = provider;
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

    @Override
    public Iterable<MachineImage> listImages(ImageFilterOptions options) throws CloudException, InternalException {
        APITrace.begin(provider, "listImages");

        
        //
        // need to cope with image filter options...
        //
        
        
        
        try {
            VsphereConnection vsphereConnection = provider.getServiceInstance();
            VimPortType vimPort = vsphereConnection.getVimPort();
            ServiceContent serviceContent = vsphereConnection.getServiceContent();





             ManagedObjectReference viewManager = serviceContent.getViewManager();

             // use a container view for virtual machines to define the traversal
             // - invoke the VimPortType method createContainerView (corresponds
             // to the ViewManager method) - pass the ViewManager MOR and
             // the other parameters required for the method invocation
             // - createContainerView takes a string[] for the type parameter;
             // declare an arraylist and add the type string to it
             List<String> vmList = new ArrayList<String>();
             vmList.add("VirtualMachine");

             ManagedObjectReference cViewRef = vimPort.createContainerView(viewManager, serviceContent.getRootFolder(), vmList, true);

             // create an object spec to define the beginning of the traversal;
             // container view is the root object for this traversal
             ObjectSpec oSpec = new ObjectSpec();
             oSpec.setObj(cViewRef);
             oSpec.setSkip(false);  // was true. set to false, get a shit load more info!
             
              
             // create a traversal spec to select all objects in the view
             TraversalSpec tSpec = new TraversalSpec();
             tSpec.setName("traverseEntities");
             tSpec.setPath("view");
             tSpec.setSkip(false);
             tSpec.setType("ContainerView");
              
             // add the traversal spec to the object spec;
             // the accessor method (getSelectSet) returns a reference
             // to the mapped XML representation of the list; using this
             // reference to add the spec will update the list
             oSpec.getSelectSet().add(tSpec);
              
             // specify the property for retrieval (virtual machine name)
             PropertySpec pSpec = new PropertySpec();
             pSpec.setType("VirtualMachine");
             pSpec.getPathSet().add("name");
             // http://pubs.vmware.com/vsphere-60/topic/com.vmware.wssdk.pg.doc/images/VirtualMachine-VirtualMachineSummary-VMConfig.jpg
             // see above when modifying code to work for listVm's
             pSpec.getPathSet().add("summary");
             pSpec.getPathSet().add("summary.guest");
             pSpec.getPathSet().add("summary.config");
             
             // Experimental, looking for owner, no joy
             //pSpec.getPathSet().add("capability");
             //pSpec.getPathSet().add("config");
             //pSpec.getPathSet().add("runtime");
             
             
             //pSpec.getPathSet().add("summary.config.template"); // can have more than one. 
             //pSpec.getPathSet().add("summary.config.vmPathName");
             //pSpec.getPathSet().add("summary.config.uuid");
             // create a PropertyFilterSpec and add the object and
             // property specs to it; use the getter method to reference
             // the mapped XML representation of the lists and add the specs
             // directly to the list
             PropertyFilterSpec fSpec = new PropertyFilterSpec();
             fSpec.getObjectSet().add(oSpec);
             fSpec.getPropSet().add(pSpec);
              
             // Create a list for the filters and add the spec to it
             List<PropertyFilterSpec> fSpecList = new ArrayList<PropertyFilterSpec>();
             fSpecList.add(fSpec);
              
             // get the data from the server
             RetrieveOptions ro = new RetrieveOptions();
             ManagedObjectReference propColl = serviceContent.getPropertyCollector();
             RetrieveResult props = vimPort.retrievePropertiesEx(propColl,fSpecList,ro);
              
             ObjectMapper mapper = new ObjectMapper();
             System.out.println(mapper.writeValueAsString(props));
             RetrieveResult mockProps = mapper.readValue(mapper.writeValueAsString(props), RetrieveResult.class);
             // go through the returned list and print out the data
             if (props != null) {
                 for (ObjectContent oc : props.getObjects()) {
                     

                     
                     String name = null;
                     Object value = null;
                     
                     List<DynamicProperty> dps = oc.getPropSet();
                     if (dps != null) {
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
                         VirtualMachineSummary virtualMachineSummary = null;
                         VirtualMachineConfigSummary virtualMachineConfigSummary = null;
                         for (DynamicProperty dp : dps) {
                                 
                                 name = dp.getName();
                                 if (name.equals("summary")) {

                                     virtualMachineSummary = (VirtualMachineSummary)dp.getVal();
                                     if (virtualMachineSummary.getConfig().isTemplate()) {
                                         // it is a image(template)!!!
                                     }
                                     //for (DynamicProperty oc2 : v) {
                                         System.out.println("inspect");
                                     //}
                                 } else if (name.equals("summary.config")) {
                                     virtualMachineConfigSummary = (VirtualMachineConfigSummary)dp.getVal();
                                     String guestId = virtualMachineConfigSummary.getGuestId();
                                     String guestUUID = virtualMachineConfigSummary.getInstanceUuid();
                                     System.out.println("inspect");
                                 } else {
                                     value = dp.getVal();
                                     System.out.println(name + " = " + value.toString());
                                 }
                         }
                     }
                 }
             }
            
System.out.println("inspect");
            //if (vminfo.isTemplate()) {
                // THIS IS WHERE WE WANT TO BE!!!!!
            //}
            
        } catch (Exception e) {
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
