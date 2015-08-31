package org.dasein.cloud.vsphere.compute;

import java.util.ArrayList;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.compute.AbstractVMSupport;
import org.dasein.cloud.compute.Architecture;
import org.dasein.cloud.compute.VMLaunchOptions;
import org.dasein.cloud.compute.VirtualMachine;
import org.dasein.cloud.compute.VirtualMachineCapabilities;
import org.dasein.cloud.compute.VirtualMachineProduct;
import org.dasein.cloud.compute.VirtualMachineProductFilterOptions;
import org.dasein.cloud.vsphere.Vsphere;


public class Vm extends AbstractVMSupport<Vsphere> {

    protected Vm(Vsphere provider) {
        super(provider);
        // TODO Auto-generated constructor stub
    }

    private transient volatile VMCapabilities capabilities;

    @Override
    public VirtualMachineCapabilities getCapabilities() throws InternalException, CloudException {
        if( capabilities == null ) {
            capabilities = new VMCapabilities(getProvider());
        }
        return capabilities;
    }

    @Override
    public Iterable<VirtualMachineProduct> listProducts(VirtualMachineProductFilterOptions options, Architecture architecture) throws InternalException, CloudException {
        ArrayList<VirtualMachineProduct> allVirtualMachineProducts = new ArrayList<VirtualMachineProduct>();
        return allVirtualMachineProducts;
    }

    @Override
    public boolean isSubscribed() throws CloudException, InternalException {
        // TODO Auto-generated method stub
        return false;
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
