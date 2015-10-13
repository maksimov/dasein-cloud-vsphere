package org.dasein.cloud.vsphere.compute;

import java.util.ArrayList;

import com.vmware.vim25.*;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.compute.*;
import org.dasein.cloud.vsphere.VsphereConnection;
import org.dasein.cloud.vsphere.capabilities.VmCapabilities;
import org.dasein.cloud.vsphere.Vsphere;


public class Vm extends AbstractVMSupport<Vsphere> {
    Vsphere provider = null;

    protected Vm(Vsphere provider) {
        super(provider);
        this.provider = provider;
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

    public ManagedObjectReference reconfigVMTask(ManagedObjectReference vmRef, VirtualMachineConfigSpec spec) throws CloudException, InternalException {
        VsphereConnection vsphereConnection = provider.getServiceInstance();
        VimPortType vimPort = vsphereConnection.getVimPort();
        try {
            return vimPort.reconfigVMTask(vmRef, spec);
        } catch (ConcurrentAccessFaultMsg concurrentAccessFaultMsg) {
            throw new CloudException("ConcurrentAccessFaultMsg when altering vm", concurrentAccessFaultMsg);
        } catch (DuplicateNameFaultMsg duplicateNameFaultMsg) {
            throw new CloudException("DuplicateNameFaultMsg when altering vm", duplicateNameFaultMsg);
        } catch (FileFaultFaultMsg fileFaultFaultMsg) {
            throw new CloudException("FileFaultFaultMsg when altering vm", fileFaultFaultMsg);
        } catch (InsufficientResourcesFaultFaultMsg insufficientResourcesFaultFaultMsg) {
            throw new CloudException("InsufficientResourcesFaultFaultMsg when altering vm", insufficientResourcesFaultFaultMsg);
        } catch (InvalidDatastoreFaultMsg invalidDatastoreFaultMsg) {
            throw new CloudException("InvalidDatastoreFaultMsg when altering vm", invalidDatastoreFaultMsg);
        } catch (InvalidNameFaultMsg invalidNameFaultMsg) {
            throw new CloudException("InvalidNameFaultMsg when altering vm", invalidNameFaultMsg);
        } catch (InvalidStateFaultMsg invalidStateFaultMsg) {
            throw new CloudException("InvalidStateFaultMsg when altering vm", invalidStateFaultMsg);
        } catch (RuntimeFaultFaultMsg runtimeFaultFaultMsg) {
            throw new CloudException("RuntimeFaultFaultMsg when altering vm", runtimeFaultFaultMsg);
        } catch (TaskInProgressFaultMsg taskInProgressFaultMsg) {
            throw new CloudException("TaskInProgressFaultMsg when altering vm", taskInProgressFaultMsg);
        } catch (VmConfigFaultFaultMsg vmConfigFaultFaultMsg) {
            throw new CloudException("VmConfigFaultFaultMsg when altering vm", vmConfigFaultFaultMsg);
        }
    }
}
