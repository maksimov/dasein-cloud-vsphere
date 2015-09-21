package org.dasein.cloud.vsphere.compute.server;


import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
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
        ManagedObjectReference viewManager = serviceContent.getViewManager();

        //om.writeJsonFile(viewManager, "src/test/resources/ImageSupport/viewManager.json");

        return viewManager;
    }

    public ManagedObjectReference getRootFolder() {
        ManagedObjectReference rootFolder = serviceContent.getRootFolder();

        //om.writeJsonFile(rootFolder, "src/test/resources/ImageSupport/rootFolder.json");

        return rootFolder;
    }

    public ManagedObjectReference createContainerView(ManagedObjectReference viewManager, ManagedObjectReference rootFolder, List<String> vmList, boolean b) throws RuntimeFaultFaultMsg {
        ManagedObjectReference containerView = vimPort.createContainerView(viewManager, rootFolder, vmList, b);

        //om.writeJsonFile(containerView, "src/test/resources/ImageSupport/containerView.json");

        return containerView;
    }

    public ManagedObjectReference getPropertyCollector() {
        ManagedObjectReference propertyCollector = serviceContent.getPropertyCollector();

        //om.writeJsonFile(propertyCollector, "src/test/resources/ImageSupport/propertyCollector.json");

        return propertyCollector;
    }

    public RetrieveResult retrievePropertiesEx(ManagedObjectReference propColl, List<PropertyFilterSpec> fSpecList, RetrieveOptions ro) throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
        RetrieveResult propertiesEx = vimPort.retrievePropertiesEx(propColl, fSpecList, ro);

        //om.writeJsonFile(propertiesEx, "src/test/resources/ImageSupport/propertiesEx.json");

        return propertiesEx;
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
            //pSpec.getPathSet().add("summary");
            //pSpec.getPathSet().add("summary.guest.guestfullName"); 
            pSpec.getPathSet().add("summary.config");

            //pSpec.getPathSet().add("summary.config.name"); // USED
            //pSpec.getPathSet().add("summary.overallStatus");
            //pSpec.getPathSet().add("summary.runtime"); // VM support...
            //pSpec.getPathSet().add("summary.quickStats"); // VM usage and capability details

            // http://pubs.vmware.com/vsphere-60/topic/com.vmware.wssdk.pg.doc/images/VirtualMachine-VirtualMachineSummary-VMConfig.jpg
            // see above when modifying code to work for listVm's

            PropertyFilterSpec fSpec = new PropertyFilterSpec();
            fSpec.getObjectSet().add(oSpec);
            fSpec.getPropSet().add(pSpec);

            // Create a list for the filters and add the spec to it
            List<PropertyFilterSpec> fSpecList = new ArrayList<PropertyFilterSpec>();
            fSpecList.add(fSpec);

            // get the data from the server
            RetrieveResult props = null;
            try {
                props = retrievePropertiesEx(getPropertyCollector(), fSpecList, new RetrieveOptions());
            } catch ( InvalidPropertyFaultMsg e ) {
                throw new CloudException(e);
            }

            if (props != null) {
                for (ObjectContent oc : props.getObjects()) {
                    Platform platform = null;
                    String name = null;
                    String description = null;
                    String imageId = null;
                    String regionId = "datacenter-21"; //provider.getContext().getRegionId();
                    MachineImageState state = null;
                    Architecture architecture = Architecture.I64;

                    VirtualMachineSummary virtualMachineSummary = null;
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
                            }
                         }
                     }
                 }
             }
        } catch ( RuntimeFaultFaultMsg e ) {
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
