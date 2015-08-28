package org.dasein.cloud.vsphere.compute;

import javax.annotation.Nullable;

import org.dasein.cloud.compute.AbstractComputeServices;
import org.dasein.cloud.compute.VirtualMachineSupport;
import org.dasein.cloud.vsphere.Vsphere;

public class VsphereCompute extends AbstractComputeServices<Vsphere> {

    public VsphereCompute(Vsphere provider) {
        super(provider);
        // TODO Auto-generated constructor stub
    }

    @Override
    public @Nullable VirtualMachineSupport getVirtualMachineSupport() {
        return new Vm(getProvider());
    }
}
