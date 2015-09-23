package org.dasein.cloud.vsphere.network;

import org.dasein.cloud.network.AbstractNetworkServices;
import org.dasein.cloud.network.VLANSupport;
import org.dasein.cloud.vsphere.Vsphere;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * User: daniellemayne
 * Date: 21/09/2015
 * Time: 12:41
 */
public class VSphereNetworkServices extends AbstractNetworkServices<Vsphere> {
    public VSphereNetworkServices(@Nonnull Vsphere cloud) { super(cloud); }

    @Nullable
    @Override
    public VLANSupport getVlanSupport() {
        return new VSphereNetwork(getProvider());
    }
}
