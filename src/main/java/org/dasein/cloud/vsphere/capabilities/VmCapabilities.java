package org.dasein.cloud.vsphere.capabilities;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;

import javax.annotation.Nonnull;

import org.dasein.cloud.AbstractCapabilities;
import org.dasein.cloud.Capabilities;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.Requirement;
import org.dasein.cloud.VisibleScope;
import org.dasein.cloud.compute.Architecture;
import org.dasein.cloud.compute.ImageClass;
import org.dasein.cloud.compute.Platform;
import org.dasein.cloud.compute.VMScalingCapabilities;
import org.dasein.cloud.compute.VirtualMachineCapabilities;
import org.dasein.cloud.compute.VmState;
import org.dasein.cloud.util.NamingConstraints;
import org.dasein.cloud.vsphere.Vsphere;

public class VmCapabilities extends AbstractCapabilities<Vsphere> implements VirtualMachineCapabilities {

    public VmCapabilities(Vsphere provider) {
        super(provider);
    }

    @Override
    public boolean canAlter(VmState fromState) throws CloudException, InternalException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean canClone(VmState fromState) throws CloudException, InternalException {
        return true;
    }

    @Override
    public boolean canPause(VmState fromState) throws CloudException, InternalException {
        return false;
    }

    @Override
    public boolean canReboot(VmState fromState) throws CloudException, InternalException {
        return true;
    }

    @Override
    public boolean canResume(VmState fromState) throws CloudException, InternalException {
        return false;
    }

    @Override
    public boolean canStart(VmState fromState) throws CloudException, InternalException {
        if (VmState.STOPPED == fromState) {
            return true;
        }
        return false;
    }

    @Override
    public boolean canStop(VmState fromState) throws CloudException, InternalException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean canSuspend(VmState fromState) throws CloudException, InternalException {
        if (VmState.RUNNING == fromState) {
            return true;
        }
        return false;
    }

    @Override
    public boolean canTerminate(VmState fromState) throws CloudException, InternalException {
        if (VmState.STOPPED == fromState) {
            return true;
        }
        return false;
    }

    @Override
    public boolean canUnpause(VmState fromState) throws CloudException, InternalException {
        return false;
    }

    @Override
    public int getMaximumVirtualMachineCount() throws CloudException, InternalException {
        return Capabilities.LIMIT_UNKNOWN;
    }

    @Override
    public int getCostFactor(VmState state) throws CloudException, InternalException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getProviderTermForVirtualMachine(Locale locale) throws CloudException, InternalException {
        return "virtual machine";
    }

    @Override
    public VMScalingCapabilities getVerticalScalingCapabilities() throws CloudException, InternalException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public NamingConstraints getVirtualMachineNamingConstraints() throws CloudException, InternalException {
        return NamingConstraints.getAlphaNumeric(1, 80).withRegularExpression(".{1,80}");
    }

    @Override
    public VisibleScope getVirtualMachineVisibleScope() {
        return VisibleScope.ACCOUNT_REGION;
    }

    @Override
    public VisibleScope getVirtualMachineProductVisibleScope() {
        return VisibleScope.ACCOUNT_REGION;
    }

    @Override
    public Requirement identifyDataCenterLaunchRequirement() throws CloudException, InternalException {
        return Requirement.REQUIRED;
    }

    @Override
    public Requirement identifyImageRequirement(ImageClass cls) throws CloudException, InternalException {

        return Requirement.REQUIRED;
    }

    @Override
    public Requirement identifyPasswordRequirement(Platform platform) throws CloudException, InternalException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Requirement identifyRootVolumeRequirement() throws CloudException, InternalException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Requirement identifyShellKeyRequirement(Platform platform) throws CloudException, InternalException {
        return Requirement.NONE;
    }

    @Override
    public Requirement identifyStaticIPRequirement() throws CloudException, InternalException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Requirement identifySubnetRequirement() throws CloudException, InternalException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Requirement identifyVlanRequirement() throws CloudException, InternalException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isAPITerminationPreventable() throws CloudException, InternalException {
        return false;
    }

    @Override
    public boolean isBasicAnalyticsSupported() throws CloudException, InternalException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isExtendedAnalyticsSupported() throws CloudException, InternalException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isUserDataSupported() throws CloudException, InternalException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isUserDefinedPrivateIPSupported() throws CloudException, InternalException {
        // TODO Auto-generated method stub
        return false;
    }

    private transient volatile Collection<Architecture> architectures;
    @Override
    public @Nonnull Iterable<Architecture> listSupportedArchitectures() throws InternalException, CloudException {
        if( architectures == null ) {
            architectures = Collections.unmodifiableCollection(
                    Arrays.asList(Architecture.I64, Architecture.I32)
            );
        }
        return architectures;
    }

    @Override
    public boolean supportsSpotVirtualMachines() throws InternalException, CloudException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean supportsClientRequestToken() throws InternalException, CloudException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean supportsCloudStoredShellKey() throws InternalException, CloudException {
        return false;
    }

    @Override
    public boolean isVMProductDCConstrained() throws InternalException, CloudException {
        return false;
    }

    @Override
    public boolean supportsAlterVM() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean supportsClone() {
        return true;
    }

    @Override
    public boolean supportsPause() {
        return false;
    }

    @Override
    public boolean supportsReboot() {
        return true;
    }

    @Override
    public boolean supportsResume() {
        return false;
    }

    @Override
    public boolean supportsStart() {
        return true;
    }

    @Override
    public boolean supportsStop() {
        return true;
    }

    @Override
    public boolean supportsSuspend() {
        return false;
    }

    @Override
    public boolean supportsTerminate() {
        return true;
    }

    @Override
    public boolean supportsUnPause() {
        return false;
    }

}
