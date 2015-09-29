package org.dasein.cloud.vsphere;

import com.vmware.vim25.*;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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

    public RetrieveResult retrieveObjectList(@Nonnull Vsphere provider, @Nonnull String baseFolder, @Nullable List<SelectionSpec> selectionSpecsArr, @Nonnull List<PropertySpec> pSpecs) throws InternalException, CloudException {
        if ("".equals(baseFolder)) {
            throw new CloudException("baseFolder must be non-empty string");
        }
        if (pSpecs.size() == 0) {
            throw new CloudException("PropertySpec list must have at least one element");
        }

        VsphereConnection vsphereConnection = provider.getServiceInstance();
        ServiceContent serviceContent = vsphereConnection.getServiceContent();
        VimPortType vimPortType = vsphereConnection.getVimPort();

        ManagedObjectReference rootFolder = serviceContent.getRootFolder();

        VsphereTraversalSpec traversalSpec = new VsphereTraversalSpec("VisitFolders", "childEntity", "Folder", false)
            .withSelectionSpec("VisitFolders", "DataCenterTo" + baseFolder,  baseFolder,  "Datacenter",  false);

        if (selectionSpecsArr != null) {
            traversalSpec = traversalSpec.withSelectionSpec(selectionSpecsArr);
        }

        traversalSpec = traversalSpec.withObjectSpec(rootFolder, true)
                .withPropertySpec(pSpecs);

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
            props = vimPortType.retrievePropertiesEx(vimServiceContent.getPropertyCollector(), traversalSpec.getPropertyFilterSpecList(), new RetrieveOptions());
        } catch ( InvalidPropertyFaultMsg e ) {
            throw new InternalException("InvalidPropertyFault", e);
        } catch ( RuntimeFaultFaultMsg e ) {
            throw new CloudException("RuntimeFault", e);
        } catch ( Exception e ) {
            throw new CloudException(e);
        }

        return props;
    }

    private  @Nonnull TraversalSpec getFolderTraversalSpec(@Nonnull String baseFolder) {
        // Create a traversal spec that starts from the 'root' objects

        VsphereTraversalSpec tSpec = new VsphereTraversalSpec("VisitFolders", "childEntity", "Folder", false)
            .withSelectionSpec("VisitFolders", "DataCenterTo" + baseFolder,  baseFolder,  "Datacenter",  false);

        return tSpec.getTraversalSpec();
    }

}
