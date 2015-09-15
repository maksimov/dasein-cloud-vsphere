package org.dasein.cloud.vsphere;

import com.vmware.vim25.*;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * User: daniellemayne
 * Date: 14/09/2015
 * Time: 09:57
 *
 * VsphereInventoryNavigation is a convenience class for traversing the object hierarchy
 * in a Vsphere environment starting at the ServiceInstance.
 */
public class VsphereInventoryNavigation {

    private static final String VIMSERVICEINSTANCETYPE = "ServiceInstance";
    private static final String VIMSERVICEINSTANCEVALUE = "ServiceInstance";

    public RetrieveResult retrieveObjectList(Vsphere provider, @Nonnull String baseFolder, @Nullable List<SelectionSpec> selectionSpecsArr, @Nonnull List<PropertySpec> pSpecs) throws InternalException, CloudException {
        VsphereConnection vsphereConnection = provider.getServiceInstance();
        ServiceContent serviceContent = vsphereConnection.getServiceContent();
        VimPortType vimPortType = vsphereConnection.getVimPort();

        ManagedObjectReference rootFolder = serviceContent.getRootFolder();

        TraversalSpec traversalSpec = getFolderTraversalSpec(baseFolder);

        if (selectionSpecsArr != null) {
            traversalSpec.getSelectSet().addAll(selectionSpecsArr);
        }

        // Now create Object Spec
        ObjectSpec objectSpec = new ObjectSpec();
        objectSpec.setObj(rootFolder);
        objectSpec.setSkip(Boolean.TRUE);
        objectSpec.getSelectSet().add(traversalSpec);

        // Create PropertyFilterSpec using the PropertySpec and ObjectPec
        // created above.
        PropertyFilterSpec propertyFilterSpec = new PropertyFilterSpec();
        propertyFilterSpec.getPropSet().addAll(pSpecs);
        propertyFilterSpec.getObjectSet().add(objectSpec);

        List<PropertyFilterSpec> listfps = new ArrayList<PropertyFilterSpec>(1);
        listfps.add(propertyFilterSpec);
        ServiceContent vimServiceContent = null;
        try {
            ManagedObjectReference ref = new ManagedObjectReference();
            ref.setType(VIMSERVICEINSTANCETYPE);
            ref.setValue(VIMSERVICEINSTANCEVALUE);
            vimServiceContent = vimPortType.retrieveServiceContent(ref);
        } catch ( RuntimeFaultFaultMsg e ) {
            throw new CloudException(e);
        }

        RetrieveResult props = null;
        try {
            props = vimPortType.retrievePropertiesEx(vimServiceContent.getPropertyCollector(), listfps, new RetrieveOptions());
        } catch ( InvalidPropertyFaultMsg e ) {
            throw new CloudException(e);
        } catch ( RuntimeFaultFaultMsg e ) {
            throw new CloudException(e);
        }

        return props;
    }

    private  @Nonnull TraversalSpec getFolderTraversalSpec(@Nonnull String baseFolder) {
        // Create a traversal spec that starts from the 'root' objects
        SelectionSpec sSpec = new SelectionSpec();
        sSpec.setName("VisitFolders");

        TraversalSpec dataCenterToFolder = new TraversalSpec();
        dataCenterToFolder.setName("DataCenterTo" + baseFolder);
        dataCenterToFolder.setType("Datacenter");
        dataCenterToFolder.setPath(baseFolder);
        dataCenterToFolder.setSkip(false);
        dataCenterToFolder.getSelectSet().add(sSpec);

        TraversalSpec traversalSpec = new TraversalSpec();
        traversalSpec.setName("VisitFolders");
        traversalSpec.setType("Folder");
        traversalSpec.setPath("childEntity");
        traversalSpec.setSkip(false);
        List<SelectionSpec> sSpecArr = new ArrayList<SelectionSpec>();
        sSpecArr.add(sSpec);
        sSpecArr.add(dataCenterToFolder);
        traversalSpec.getSelectSet().addAll(sSpecArr);
        return traversalSpec;
    }

}
