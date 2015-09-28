package org.dasein.cloud.vsphere.compute;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.CloudProvider;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.compute.AbstractVolumeSupport;
import org.dasein.cloud.compute.Volume;
import org.dasein.cloud.compute.VolumeCapabilities;
import org.dasein.cloud.vsphere.Vsphere;
import org.dasein.cloud.vsphere.capabilities.HardDiskCapabilities;

import javax.annotation.Nonnull;

/**
 * User: daniellemayne
 * Date: 28/09/2015
 * Time: 10:28
 */
public class HardDisk extends AbstractVolumeSupport<Vsphere> {
    private Vsphere provider;

    HardDisk(@Nonnull Vsphere provider) {
        super(provider);
        this.provider = provider;
    }

    private transient volatile HardDiskCapabilities capabilities;
    @Override
    public VolumeCapabilities getCapabilities() throws CloudException, InternalException {
        if( capabilities == null ) {
            capabilities = new HardDiskCapabilities(provider);
        }
        return capabilities;
    }

    @Nonnull
    @Override
    public Iterable<Volume> listVolumes() throws InternalException, CloudException {
        return null;
    }

    @Override
    public boolean isSubscribed() throws CloudException, InternalException {
        return true;
    }

    @Override
    public void remove(@Nonnull String volumeId) throws InternalException, CloudException {

    }
}
