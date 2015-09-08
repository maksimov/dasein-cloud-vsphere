package org.dasein.cloud.vsphere.compute;

import java.util.ArrayList;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.compute.*;
import org.dasein.cloud.vsphere.capabilities.VmCapabilities;
import org.dasein.cloud.vsphere.Vsphere;


public class Vm extends AbstractVMSupport<Vsphere> {

    protected Vm(Vsphere provider) {
        super(provider);
        // TODO Auto-generated constructor stub
    }

    private transient volatile VmCapabilities capabilities;

    @Override
    public VirtualMachineCapabilities getCapabilities() throws InternalException, CloudException {
        if( capabilities == null ) {
            capabilities = new VmCapabilities(getProvider());
        }
        return capabilities;
    }

    @Override
    public boolean isSubscribed() throws CloudException, InternalException {
        // TODO Auto-generated method stub
        return true;
    }

    @Override
    public VirtualMachine launch(VMLaunchOptions withLaunchOptions) throws CloudException, InternalException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void terminate(String vmId, String explanation) throws InternalException, CloudException {
        // TODO Auto-generated method stub
        
    }

}
