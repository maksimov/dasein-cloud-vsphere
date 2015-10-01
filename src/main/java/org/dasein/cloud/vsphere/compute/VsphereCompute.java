package org.dasein.cloud.vsphere.compute;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.dasein.cloud.compute.AbstractComputeServices;
import org.dasein.cloud.compute.AffinityGroupSupport;
import org.dasein.cloud.compute.VirtualMachineSupport;
import org.dasein.cloud.compute.VolumeSupport;
import org.dasein.cloud.vsphere.Vsphere;
import org.dasein.cloud.vsphere.compute.server.ImageSupport;


public class VsphereCompute extends AbstractComputeServices<Vsphere> {


    public VsphereCompute(Vsphere provider) {
        super(provider);
    }

    @Nullable
    @Override
    public AffinityGroupSupport getAffinityGroupSupport() {
        return new HostSupport(getProvider());
    }

    @Override
    public @Nullable VirtualMachineSupport getVirtualMachineSupport() {
        return new Vm(getProvider());
    }

    @Nullable
    @Override
    public VolumeSupport getVolumeSupport() { return new HardDisk(getProvider()); }

    public @Nonnull ImageSupport getImageSupport() {
        return new ImageSupport(getProvider());
    }
}