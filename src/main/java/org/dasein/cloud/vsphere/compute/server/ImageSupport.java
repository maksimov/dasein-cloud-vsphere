package org.dasein.cloud.vsphere.compute.server;


import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import org.apache.log4j.Logger;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.ProviderContext;
import org.dasein.cloud.compute.AbstractImageSupport;
import org.dasein.cloud.compute.Architecture;
import org.dasein.cloud.compute.ImageClass;
import org.dasein.cloud.compute.ImageFilterOptions;
import org.dasein.cloud.compute.MachineImage;
import org.dasein.cloud.compute.MachineImageState;
import org.dasein.cloud.compute.Platform;
import org.dasein.cloud.vsphere.ObjectManagement;
import org.dasein.cloud.vsphere.Vsphere;
import org.dasein.cloud.vsphere.VsphereConnection;
import org.dasein.cloud.vsphere.capabilities.VsphereImageCapabilities;

import com.vmware.vim25.DynamicProperty;
import com.vmware.vim25.InvalidPropertyFaultMsg;
import com.vmware.vim25.ManagedEntityStatus;
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
import com.vmware.vim25.VirtualMachineGuestSummary;
import com.vmware.vim25.VirtualMachineSummary;

import org.dasein.cloud.util.APITrace;


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

    private ObjectManagement om = new ObjectManagement();

    public ImageSupport(@Nonnull Vsphere provider) {
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

    public ImageSupport() { 
        super(null);
    }  // for mock testing NEVER use....

    @Override
    public VsphereImageCapabilities getCapabilities() throws CloudException, InternalException {
        if (capabilities == null) {
            capabilities = new VsphereImageCapabilities(provider);
        }
        return capabilities;
    }

    @Override
    public MachineImage getImage(String providerImageId) throws CloudException, InternalException {
        ImageFilterOptions options = ImageFilterOptions.getInstance(true, providerImageId);
        // TODO Auto-generated method stub
        Iterable<MachineImage> images = listImages(options);

        return images.iterator().next();
    }

    @Override
    public boolean isSubscribed() throws CloudException, InternalException {
        return true;
    }

    //
    // TODO: Going to need to move these to a VsphereStubs class
    //
    public ManagedObjectReference getViewManager() {
        return serviceContent.getViewManager();
    }

    public ManagedObjectReference getRootFolder() {
        return serviceContent.getRootFolder();

    }

    public ManagedObjectReference createContainerView(ManagedObjectReference viewManager, ManagedObjectReference rootFolder, List<String> vmList, boolean b) throws RuntimeFaultFaultMsg {
        return vimPort.createContainerView(viewManager, rootFolder, vmList, b);
    }

    public ManagedObjectReference getPropertyCollector() {
        return serviceContent.getPropertyCollector();

    }

    public RetrieveResult retrievePropertiesEx(ManagedObjectReference propColl, List<PropertyFilterSpec> fSpecList, RetrieveOptions ro) throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
        return vimPort.retrievePropertiesEx(propColl, fSpecList, ro);
    }

    public List<PropertyFilterSpec> getlistImagesPropertyFilterSpec() throws RuntimeFaultFaultMsg {

        ManagedObjectReference viewManager = getViewManager();
        ManagedObjectReference rootFolder = getRootFolder();
        List<String> vmList = new ArrayList<String>();
        vmList.add("VirtualMachine");
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
        pSpec.getPathSet().add("summary.config");

        PropertyFilterSpec fSpec = new PropertyFilterSpec();
        fSpec.getObjectSet().add(oSpec);
        fSpec.getPropSet().add(pSpec);

        // Create a list for the filters and add the spec to it
        List<PropertyFilterSpec> fSpecList = new ArrayList<PropertyFilterSpec>();
        fSpecList.add(fSpec);
        
        return fSpecList;
    }
    
    @Override
    public Iterable<MachineImage> listImages(ImageFilterOptions options) throws CloudException, InternalException {
        APITrace.begin(getProvider(), "ImageSupport.listImages");
        final ImageFilterOptions opts;

        if (options == null) {
            opts = ImageFilterOptions.getInstance();
        } else {
            opts = options;
        }

        ArrayList<MachineImage> machineImages = new ArrayList<MachineImage>();

        try {
            String regionId = getProvider().getContext().getRegionId();

            List<PropertyFilterSpec> fSpecList = getlistImagesPropertyFilterSpec();

            // get the data from the server
            RetrieveResult props = null;
            try {
                props = retrievePropertiesEx(getPropertyCollector(), fSpecList, new RetrieveOptions());
            } catch ( InvalidPropertyFaultMsg e ) {
                throw new CloudException(e);
            }

            if (props != null) {
                for (ObjectContent oc : props.getObjects()) {

                    System.out.println("LOOP");
                    Platform platform = null;
                    String name = null;
                    String description = null;
                    String imageId = null;

                    MachineImageState state = null;
                    Architecture architecture = Architecture.I64;

                    VirtualMachineConfigSummary virtualMachineConfigSummary = null;
                    List<DynamicProperty> dps = oc.getPropSet();
                    if (dps != null) {
                         for (DynamicProperty dp : dps) {
                              if (dp.getName().equals("summary.config")) {
                                 virtualMachineConfigSummary = (VirtualMachineConfigSummary) dp.getVal();
                             } else if (dp.getName().equals("summary.config.name")) {
                                 name = virtualMachineConfigSummary.getName();
                             } else if (dp.getName().equals("summary.overallStatus")) {
                                 ManagedEntityStatus s = (ManagedEntityStatus) dp.getVal();
                                 state = MachineImageState.ERROR;
                                 if (s.equals(ManagedEntityStatus.GREEN)) {
                                     state = MachineImageState.ACTIVE;
                                 }
                             }
                         }

                         description = virtualMachineConfigSummary.getGuestFullName();
                         imageId = virtualMachineConfigSummary.getGuestId();
                         platform = Platform.guess(virtualMachineConfigSummary.getGuestFullName());
                         ImageClass imageClass = ImageClass.MACHINE;

                         if (virtualMachineConfigSummary.isTemplate()) { 
                            MachineImage machineImage = MachineImage.getInstance(
                                     "ownerId",
                                     regionId,
                                     imageId,
                                     imageClass,
                                     state,
                                     name,
                                     description,
                                     architecture,
                                     platform);
                            if (options.matches(machineImage)) {
                                machineImages.add(machineImage);
                                System.out.println("MATCH");
                            }
                         }
                     }
                 }
             }
        } catch ( RuntimeFaultFaultMsg e ) {
            throw new CloudException(e);
        } catch ( Exception e ) {
            throw new CloudException(e);
        } finally {
            APITrace.end();
        }

        return machineImages;
    }

    @Override
    public void remove(String providerImageId, boolean checkState) throws CloudException, InternalException {
        // TODO Auto-generated method stub
    }


}
