package org.dasein.cloud.vsphere.compute.server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.log4j.Logger;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.ProviderContext;
import org.dasein.cloud.compute.AbstractImageSupport;
import org.dasein.cloud.compute.ImageFilterOptions;
import org.dasein.cloud.compute.MachineImage;
import org.dasein.cloud.dc.DataCenter;
import org.dasein.cloud.dc.Region;
import org.dasein.cloud.util.APITrace;
import org.dasein.cloud.util.Cache;
import org.dasein.cloud.util.CacheLevel;
import org.dasein.cloud.vsphere.NoContextException;
import org.dasein.cloud.vsphere.Vsphere;
import org.dasein.cloud.vsphere.VsphereConnection;
import org.dasein.cloud.vsphere.capabilities.VsphereImageCapabilities;
import org.dasein.util.uom.time.Day;
import org.dasein.util.uom.time.TimePeriod;

import com.vmware.vim25.DynamicProperty;
import com.vmware.vim25.InvalidPropertyFaultMsg;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.ObjectContent;
import com.vmware.vim25.ObjectSpec;
import com.vmware.vim25.ObjectUpdate;
import com.vmware.vim25.ObjectUpdateKind;
import com.vmware.vim25.PropertyChange;
import com.vmware.vim25.PropertyChangeOp;
import com.vmware.vim25.PropertyFilterSpec;
import com.vmware.vim25.PropertyFilterUpdate;
import com.vmware.vim25.PropertySpec;
import com.vmware.vim25.RetrieveOptions;
import com.vmware.vim25.RetrieveResult;
import com.vmware.vim25.RuntimeFaultFaultMsg;
import com.vmware.vim25.SelectionSpec;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.TraversalSpec;
import com.vmware.vim25.UpdateSet;
import com.vmware.vim25.VimPortType;
import com.vmware.vim25.VirtualMachineRuntimeInfo;


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

        try {
            VsphereConnection vsphereConnection = provider.getServiceInstance();
            VimPortType vimPort = vsphereConnection.getVimPort();
            ServiceContent serviceContent = vsphereConnection.getServiceContent();
            ManagedObjectReference viewManager = serviceContent.getViewManager();
            ManagedObjectReference rootFolder = serviceContent.getRootFolder();
            ManagedObjectReference containerView = vimPort.createContainerView(viewManager, rootFolder, Arrays.asList("VirtualMachine"), true);
            VimPortType methods = vimPort;
            ServiceContent sContent = serviceContent;
            

             // Get references to the ViewManager and PropertyCollector
             ManagedObjectReference viewMgrRef = sContent.getViewManager();
             ManagedObjectReference propColl = sContent.getPropertyCollector();
              
             // use a container view for virtual machines to define the traversal
             // - invoke the VimPortType method createContainerView (corresponds
             // to the ViewManager method) - pass the ViewManager MOR and
             // the other parameters required for the method invocation
             // - createContainerView takes a string[] for the type parameter;
             // declare an arraylist and add the type string to it
             List<String> vmList = new ArrayList<String>();
             vmList.add("VirtualMachine");
              
             ManagedObjectReference cViewRef = methods.createContainerView(viewMgrRef, sContent.getRootFolder(), vmList, true);
              
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
             pSpec.getPathSet().add("summary");
             
             
             
             
             
             // ^^ magic is above. keep adding features to pSpec....
             
             
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
             RetrieveResult props = methods.retrievePropertiesEx(propColl,fSpecList,ro);
              
             // go through the returned list and print out the data
             if (props != null) {
                 for (ObjectContent oc : props.getObjects()) {
                     
                     /*
                     PropertySpec pprops = new PropertySpec();
                     pprops.setAll(Boolean.FALSE);
                     pprops.getPathSet().add("name");
                     pprops.setType("ManagedEntity");
                     List<PropertySpec> propspecary = new ArrayList<PropertySpec>();
                     propspecary.add(pprops);
                     
                     
                     PropertyFilterSpec spec = new PropertyFilterSpec();
                     spec.getPropSet().addAll(propspecary);

                     spec.getObjectSet().add(new ObjectSpec());
                     spec.getObjectSet().get(0).setObj(rootFolder);
                     spec.getObjectSet().get(0).setSkip(Boolean.FALSE);
                     spec.getObjectSet().get(0).getSelectSet().add(tSpec);

                     List<PropertyFilterSpec> listpfs = new ArrayList<PropertyFilterSpec>(1);
                     listpfs.add(spec);
                     
                     RetrieveOptions propObjectRetrieveOpts = new RetrieveOptions();

                     List<ObjectContent> listobjcontent = new ArrayList<ObjectContent>();
        
                     RetrieveResult rslts =
                             vimPort.retrievePropertiesEx(oc.getObj(), listpfs, propObjectRetrieveOpts);
                     
                     */
                     
                     
                     String vmName = null;
                     String path = null;
                     
                     List<DynamicProperty> dps = oc.getPropSet();
                     if (dps != null) {
                         for (DynamicProperty dp : dps) {
                                 vmName = (String) dp.getVal();
                                 path = dp.getName();
                                 System.out.println(path + " = " + vmName);
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
