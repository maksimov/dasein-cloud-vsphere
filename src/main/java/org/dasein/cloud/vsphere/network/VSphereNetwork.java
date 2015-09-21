package org.dasein.cloud.vsphere.network;

import com.vmware.vim25.PropertySpec;
import com.vmware.vim25.RetrieveResult;
import com.vmware.vim25.SelectionSpec;
import com.vmware.vim25.TraversalSpec;
import org.apache.log4j.Logger;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.network.AbstractVLANSupport;
import org.dasein.cloud.network.VLAN;
import org.dasein.cloud.network.VLANCapabilities;
import org.dasein.cloud.util.APITrace;
import org.dasein.cloud.vsphere.Vsphere;
import org.dasein.cloud.vsphere.VsphereInventoryNavigation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * User: daniellemayne
 * Date: 21/09/2015
 * Time: 12:34
 */
public class VSphereNetwork extends AbstractVLANSupport {
    private Vsphere provider;
    public List<PropertySpec> networkPSpec;
    public List<SelectionSpec> networkSSpec;
    static private final Logger log = Vsphere.getLogger(VSphereNetwork.class);

    VSphereNetwork(Vsphere provider) {
        super(provider);
        this.provider = provider;
    }

    public RetrieveResult retrieveObjectList(Vsphere provider, @Nonnull String baseFolder, @Nullable List<SelectionSpec> selectionSpecsArr, @Nonnull List<PropertySpec> pSpecs) throws InternalException, CloudException {
        VsphereInventoryNavigation nav = new VsphereInventoryNavigation();
        return nav.retrieveObjectList(provider, baseFolder, selectionSpecsArr, pSpecs);
    }

    public List<PropertySpec> getNetworkPSpec() {
        if (networkPSpec == null) {
            networkPSpec = new ArrayList<PropertySpec>();
            // Create Property Spec
            PropertySpec propertySpec = new PropertySpec();
            propertySpec.setAll(Boolean.TRUE);
            propertySpec.setType("Network");
            networkPSpec.add(propertySpec);

            // Create Property Spec
            PropertySpec propertySpec2 = new PropertySpec();
            propertySpec2.setAll(Boolean.TRUE);
            propertySpec2.setType("DistributedVirtualSwitch");
            networkPSpec.add(propertySpec2);
        }
        return networkPSpec;
    }

    public List<SelectionSpec> getNetworkSSpec() {
        if (networkSSpec == null) {
            networkSSpec = new ArrayList<SelectionSpec>();
            TraversalSpec crToH = new TraversalSpec();
            crToH.setSkip(Boolean.FALSE);
            crToH.setType("ComputeResource");
            crToH.setPath("host");
            crToH.setName("crToH");
            networkSSpec.add(crToH);
        }
        return networkSSpec;
    }

    private transient volatile VSphereNetworkCapabilities capabilities;

    @Override
    public VLANCapabilities getCapabilities() throws CloudException, InternalException {
        if( capabilities == null ) {
            capabilities = new VSphereNetworkCapabilities(provider);
        }
        return capabilities;
    }

    @Nonnull
    @Override
    public String getProviderTermForNetworkInterface(@Nonnull Locale locale) {
        return capabilities.getProviderTermForNetworkInterface(locale);
    }

    @Nonnull
    @Override
    public String getProviderTermForSubnet(@Nonnull Locale locale) {
        return capabilities.getProviderTermForSubnet(locale);
    }

    @Nonnull
    @Override
    public String getProviderTermForVlan(@Nonnull Locale locale) {
        return capabilities.getProviderTermForVlan(locale);
    }

    @Override
    public boolean isSubscribed() throws CloudException, InternalException {
        return true;
    }

    @Nonnull
    @Override
    public Iterable<VLAN> listVlans() throws CloudException, InternalException {
        APITrace.begin(provider, "VSphereNetwork.listVlans");
        try {
            List<VLAN> list = new ArrayList<VLAN>();

            //List<SelectionSpec> selectionSpecsArr = getNetworkSSpec();
            List<PropertySpec> pSpecs = getNetworkPSpec();

            RetrieveResult listobcont = retrieveObjectList(provider, "networkFolder", null, pSpecs);


            return list;
        }
        finally {
            APITrace.end();
        }
    }
}
