package org.dasein.cloud.vsphere.compute;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.CloudProvider;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.compute.AbstractVolumeSupport;
import org.dasein.cloud.compute.Volume;
import org.dasein.cloud.compute.VolumeCapabilities;
import org.dasein.cloud.compute.VolumeCreateOptions;
import org.dasein.cloud.util.APITrace;
import org.dasein.cloud.vsphere.Vsphere;
import org.dasein.cloud.vsphere.capabilities.HardDiskCapabilities;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

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

    @Override
    public void attach(@Nonnull String volumeId, @Nonnull String toServer, @Nonnull String deviceId) throws InternalException, CloudException {
        super.attach(volumeId, toServer, deviceId);
    }

    @Nonnull
    @Override
    public String createVolume(@Nonnull VolumeCreateOptions options) throws InternalException, CloudException {
        return super.createVolume(options);
    }

    @Override
    public void detach(@Nonnull String volumeId, boolean force) throws InternalException, CloudException {
        super.detach(volumeId, force);
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
        APITrace.begin(provider, "HardDisk.listVolumes");
        try {
            List<Volume> list = new ArrayList<Volume>();



            return list;
        }
        finally {
            APITrace.end();
        }
    }

    @Override
    public boolean isSubscribed() throws CloudException, InternalException {
        return true;
    }

    @Override
    public void remove(@Nonnull String volumeId) throws InternalException, CloudException {

    }
}
