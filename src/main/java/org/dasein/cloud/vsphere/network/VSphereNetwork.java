package org.dasein.cloud.vsphere.network;

import com.vmware.vim25.*;
import org.apache.log4j.Logger;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.VisibleScope;
import org.dasein.cloud.network.*;
import org.dasein.cloud.util.APITrace;
import org.dasein.cloud.vsphere.ObjectManagement;
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

    public VSphereNetwork(Vsphere provider) {
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
        }
        return networkPSpec;
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
            ObjectManagement om = new ObjectManagement();
            List<VLAN> list = new ArrayList<VLAN>();
            List<PropertySpec> pSpecs = getNetworkPSpec();

            RetrieveResult listobcont = retrieveObjectList(provider, "networkFolder", null, pSpecs);

            if (listobcont != null) {
                List<ObjectContent> objectContents = listobcont.getObjects();
                for (ObjectContent oc : objectContents) {
                    ManagedObjectReference mo = oc.getObj();
                    String id = mo.getValue();
                    String networkType = mo.getType();
                    if (networkType.equals("Network") || networkType.equals("DistributedVirtualPortgroup")) {
                        List<DynamicProperty> props = oc.getPropSet();
                        String networkName = null, dvsId = null;
                        boolean state = false;
                        for (DynamicProperty dp : props) {
                            if (dp.getName().equals("summary")) {
                                NetworkSummary ns = (NetworkSummary) dp.getVal();
                                state = ns.isAccessible();
                                networkName = ns.getName();
                            }
                            else if (dp.getVal() instanceof DVPortgroupConfigInfo) {
                                DVPortgroupConfigInfo di = (DVPortgroupConfigInfo) dp.getVal();
                                ManagedObjectReference switchMO = di.getDistributedVirtualSwitch();
                                dvsId = switchMO.getValue();
                            }
                        }
                        VLAN vlan = toVlan(id, networkName, state, dvsId);
                        if (vlan != null) {
                            list.add(vlan);
                        }
                    }
                }
            }
            return list;
        }
        finally {
            APITrace.end();
        }
    }

    private VLAN toVlan(@Nonnull String id, @Nonnull String name, @Nullable boolean available, @Nullable String switchID) throws InternalException, CloudException {
        VLAN vlan = new VLAN();
        vlan.setName(name);
        vlan.setDescription(name + " ("+id+")");
        vlan.setProviderVlanId(id);
        vlan.setCidr("");
        if( switchID != null) {
            vlan.setTag("switch.uuid", switchID);
        }
        vlan.setProviderRegionId(getContext().getRegionId());
        vlan.setProviderOwnerId(getContext().getAccountNumber());
        vlan.setSupportedTraffic(IPVersion.IPV4);
        vlan.setVisibleScope(VisibleScope.ACCOUNT_REGION);
        vlan.setCurrentState(available ? VLANState.AVAILABLE : VLANState.PENDING);
        return vlan;
    }
}
