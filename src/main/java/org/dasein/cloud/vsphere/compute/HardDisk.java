package org.dasein.cloud.vsphere.compute;

import com.vmware.vim25.*;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.ProviderContext;
import org.dasein.cloud.compute.*;
import org.dasein.cloud.dc.Region;
import org.dasein.cloud.dc.ResourcePool;
import org.dasein.cloud.dc.StoragePool;
import org.dasein.cloud.util.APITrace;
import org.dasein.cloud.vsphere.*;
import org.dasein.cloud.vsphere.capabilities.HardDiskCapabilities;
import org.dasein.util.uom.storage.Kilobyte;
import org.dasein.util.uom.storage.Storage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.ArrayList;
import java.util.List;

/**
 * User: daniellemayne
 * Date: 28/09/2015
 * Time: 10:28
 */
public class HardDisk extends AbstractVolumeSupport<Vsphere> {
    private Vsphere provider;
    private List<PropertySpec> hardDiskPSpecs;
    private List<PropertySpec> rpPSpecs;
    private List<SelectionSpec> rpSSpecs;
    private List<PropertySpec> spPSpecs;
    private DataCenters dc;

    HardDisk(@Nonnull Vsphere provider) {
        super(provider);
        this.provider = provider;
        dc = provider.getDataCenterServices();
    }

    public RetrieveResult retrieveObjectList(Vsphere provider, @Nonnull String baseFolder, @Nullable List<SelectionSpec> selectionSpecsArr, @Nonnull List<PropertySpec> pSpecs) throws InternalException, CloudException {
        VsphereInventoryNavigation nav = new VsphereInventoryNavigation();
        return nav.retrieveObjectList(provider, baseFolder, selectionSpecsArr, pSpecs);
    }

    public List<PropertySpec> getHardDiskPSpec() {
        if (hardDiskPSpecs == null) {
            hardDiskPSpecs = VsphereTraversalSpec.createPropertySpec(hardDiskPSpecs, "VirtualMachine", false, "runtime.powerState", "config.template", "config.guestFullName", "resourcePool", "config.hardware.device");
        }
        return hardDiskPSpecs;
    }

    public List<SelectionSpec> getResourcePoolSelectionSpec() {
        if (rpSSpecs == null) {
            rpSSpecs = new ArrayList<SelectionSpec>();
            // Recurse through all ResourcePools
            SelectionSpec sSpec = new SelectionSpec();
            sSpec.setName("rpToRp");

            TraversalSpec rpToRp = new TraversalSpec();
            rpToRp.setType("ResourcePool");
            rpToRp.setPath("resourcePool");
            rpToRp.setSkip(Boolean.FALSE);
            rpToRp.setName("rpToRp");
            rpToRp.getSelectSet().add(sSpec);

            TraversalSpec crToRp = new TraversalSpec();
            crToRp.setType("ComputeResource");
            crToRp.setPath("resourcePool");
            crToRp.setSkip(Boolean.FALSE);
            crToRp.setName("crToRp");
            crToRp.getSelectSet().add(sSpec);

            rpSSpecs.add(sSpec);
            rpSSpecs.add(rpToRp);
            rpSSpecs.add(crToRp);
        }
        return rpSSpecs;
    }

    public List<PropertySpec> getResourcePoolPropertySpec() {
        rpPSpecs = VsphereTraversalSpec.createPropertySpec(rpPSpecs, "ResourcePool", false, "owner");
        return rpPSpecs;
    }

    public List<PropertySpec> getStoragePoolPropertySpec() {
        spPSpecs = VsphereTraversalSpec.createPropertySpec(spPSpecs, "Datastore", false, "browser", "summary.name");
        return spPSpecs;
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
            List<String> fileNames = new ArrayList<String>();
            ProviderContext ctx = provider.getContext();
            if (ctx != null) {
                if (ctx.getRegionId() == null) {
                    throw new CloudException("Region id is not set");
                }
            }

            //get attached volumes
            List<PropertySpec> pSpecs = getHardDiskPSpec();
            RetrieveResult listobcont = retrieveObjectList(provider, "vmFolder", null, pSpecs);

            if (listobcont != null) {
                Iterable<ResourcePool> rps = getAllResourcePoolsIncludingRoot();//return all resourcePools
                List<ObjectContent> objectContents = listobcont.getObjects();
                for (ObjectContent oc : objectContents) {
                    ManagedObjectReference mo = oc.getObj();
                    String vmId = mo.getValue();
                    String dataCenterId = null;
                    Platform guestOs = null;
                    List<DynamicProperty> dps = oc.getPropSet();
                    if (dps != null) {
                        List<Volume> tmpVolList = new ArrayList<Volume>();
                        List<String> tmpFileNames = new ArrayList<String>();
                        boolean skipObject = false;
                        for (DynamicProperty dp : dps) {
                            if (dp.getName().equals("runtime.powerState")) {
                                VirtualMachinePowerState ps = (VirtualMachinePowerState) dp.getVal();
                                if (ps.value().equals(VirtualMachinePowerState.SUSPENDED )) {
                                    skipObject = true;
                                }
                            }
                            else if (dp.getName().equals("config.template")) {
                                Boolean isTemplate = (Boolean) dp.getVal();
                                if (isTemplate) {
                                    skipObject = true;
                                }
                            }
                            else if (dp.getName().equals("config.guestFullName")) {
                                guestOs = Platform.guess((String) dp.getVal());
                            }
                            else if (dp.getName().equals("resourcePool")) {
                                ManagedObjectReference ref = (ManagedObjectReference) dp.getVal();
                                String resourcePoolId = ref.getValue();
                                for (ResourcePool rp : rps) {
                                    if (rp.getProvideResourcePoolId().equals(resourcePoolId)) {
                                        dataCenterId = rp.getDataCenterId();
                                        break;
                                    }
                                }
                            }
                            else if (dp.getName().equals("config.hardware.device")) {
                                ArrayOfVirtualDevice avd = (ArrayOfVirtualDevice) dp.getVal();
                                List<VirtualDevice> devices = avd.getVirtualDevice();
                                for (VirtualDevice device : devices) {
                                    if (device instanceof VirtualDisk) {
                                        VirtualDisk disk = (VirtualDisk)device;
                                        Volume vol = toVolume(disk, vmId, ctx.getRegionId());
                                        if (vol != null) {
                                            vol.setGuestOperatingSystem(guestOs);
                                            tmpVolList.add(vol);
                                            tmpFileNames.add(vol.getProviderVolumeId());
                                        }
                                    }
                                }
                            }
                            if (skipObject) {
                                break;
                            }
                        }
                        if (!skipObject) {
                            if (tmpVolList != null && tmpVolList.size() > 0) {
                                for (Volume v : tmpVolList) {
                                    v.setProviderDataCenterId(dataCenterId);
                                }
                                list.addAll(tmpVolList);
                                fileNames.addAll(tmpFileNames);
                            }
                        }
                    }
                }
            }

            // get .vmdk files
            List<PropertySpec> spPSpecs = getStoragePoolPropertySpec();

            RetrieveResult spListobcont = retrieveObjectList(provider, "datastoreFolder", null, spPSpecs);

            if (spListobcont != null) {
                for (ObjectContent oc : spListobcont.getObjects()) {
                    String dataCenterId = null;
                    String dsName = null;
                    ManagedObjectReference hostDatastoreBrowser = null;
                    List<DynamicProperty> dps = oc.getPropSet();
                    if (dps != null) {
                        for (DynamicProperty dp : dps) {
                            if (dp.getName().equals("summary.name")) {
                                dsName = (String) dp.getVal();
                            }
                            else if (dp.getName().equals("browser")) {
                                hostDatastoreBrowser = (ManagedObjectReference) dp.getVal();
                            }
                        }
                    }
                    for (StoragePool pool : dc.listStoragePools()) {
                        if (pool.getStoragePoolName().equals(dsName)) {
                            dataCenterId = pool.getDataCenterId();
                            break;
                        }
                    }
                    VsphereConnection vsphereConnection = provider.getServiceInstance();
                    VimPortType vimPortType = vsphereConnection.getVimPort();

                    if (dsName != null && hostDatastoreBrowser != null) {
                        try {
                            ManagedObjectReference taskmor = vimPortType.searchDatastoreSubFoldersTask(hostDatastoreBrowser, "[" + dsName + "]", null);
                            VsphereMethod method = new VsphereMethod(provider);
                            method.waitOperationComplete(taskmor);
                            if (method.getTaskState().getVal().equals(TaskInfoState.SUCCESS)) {
                                PropertyChange taskResult = method.getTaskResult();
                                ArrayOfHostDatastoreBrowserSearchResults result = (ArrayOfHostDatastoreBrowserSearchResults) taskResult.getVal();
                                List<HostDatastoreBrowserSearchResults> res = result.getHostDatastoreBrowserSearchResults();
                                for (HostDatastoreBrowserSearchResults r : res) {
                                    List<FileInfo> files = r.getFile();
                                    if (files != null) {
                                        for (FileInfo file : files) {
                                            String filePath = file.getPath();
                                            if (filePath.endsWith(".vmdk") && !filePath.endsWith("-flat.vmdk")) {
                                                if (!fileNames.contains(file.getPath())) {
                                                    Volume d = toVolume(file, dataCenterId, ctx.getRegionId());
                                                    if (d != null) {
                                                        d.setTag("filePath", r.getFolderPath() + d.getProviderVolumeId());
                                                        list.add(d);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        catch (FileFaultFaultMsg e) {
                            throw new CloudException("Error in processing search datastore request: " + e.getMessage());
                        }
                        catch (InvalidDatastoreFaultMsg e) {
                            throw new CloudException("Error in processing search datastore request: " + e.getMessage());
                        }
                        catch (RuntimeFaultFaultMsg e) {
                            throw new CloudException("Error in processing search datastore request: " + e.getMessage());
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

    @Override
    public boolean isSubscribed() throws CloudException, InternalException {
        return true;
    }

    @Override
    public void remove(@Nonnull String volumeId) throws InternalException, CloudException {
        APITrace.begin(provider, "HardDisk.remove");
        try {
            Volume volume = null;
            Iterable<Volume> volumes = listVolumes();
            for (Volume v : volumes) {
                if (v.getProviderVolumeId().equals(volumeId)) {
                    if (v.getProviderVirtualMachineId() != null) {
                        throw new CloudException("Volume is attached to vm " + v.getProviderVirtualMachineId() + " - removing not allowed");
                    }
                    volume = v;
                    break;
                }
            }

            if (volume != null) {
                VsphereConnection vsphereConnection = provider.getServiceInstance();
                ManagedObjectReference fileManager = vsphereConnection.getServiceContent().getFileManager();
                ManagedObjectReference searchIndex = vsphereConnection.getServiceContent().getSearchIndex();
                VimPortType vimPortType = vsphereConnection.getVimPort();
                Region r = dc.getRegion(volume.getProviderRegionId());
                String regionName = r.getName();
                String pathToDC = "/"+regionName;

                ManagedObjectReference datacenter = vimPortType.findByInventoryPath(searchIndex, pathToDC);

                if (datacenter != null) {
                    String filePath = volume.getTag("filePath");
                    vimPortType.deleteDatastoreFileTask(fileManager, filePath, datacenter);
                    //also delete the flat file
                    String flatfile = filePath.substring(0, filePath.indexOf(".vmdk")) + "-flat.vmdk";
                    vimPortType.deleteDatastoreFileTask(fileManager, flatfile, datacenter);
                }
            }
        }
        catch (FileFaultFaultMsg e) {
            throw new CloudException("Error in processing remove file request: " + e.getMessage());
        }
        catch (InvalidDatastoreFaultMsg e) {
            throw new CloudException("Error in processing remove file request: " + e.getMessage());
        }
        catch (RuntimeFaultFaultMsg e) {
            throw new CloudException("Error in processing remove file request: " + e.getMessage());
        }
        finally {
            APITrace.end();
        }
    }

    @Nonnull
    private List<ResourcePool> getAllResourcePoolsIncludingRoot() throws InternalException, CloudException {
        try {
            List<ResourcePool> resourcePools = new ArrayList<ResourcePool>();

            List<SelectionSpec> selectionSpecsArr = getResourcePoolSelectionSpec();
            List<PropertySpec> pSpecs = getResourcePoolPropertySpec();

            RetrieveResult listobcont = retrieveObjectList(provider, "hostFolder", selectionSpecsArr, pSpecs);

            if (listobcont != null) {
                for (ObjectContent oc : listobcont.getObjects()) {
                    ManagedObjectReference rpRef = oc.getObj();
                    String rpId = rpRef.getValue();
                    String owner = null;
                    List<DynamicProperty> dps = oc.getPropSet();
                    if (dps != null) {
                        for (DynamicProperty dp : dps) {
                            if (dp.getName().equals("owner")) {
                                ManagedObjectReference clusterRef = (ManagedObjectReference) dp.getVal();
                                owner = clusterRef.getValue();
                            }
                        }
                    }
                    if (owner != null) {
                        ResourcePool resourcePool = new ResourcePool();
                        resourcePool.setDataCenterId(owner);
                        resourcePool.setProvideResourcePoolId(rpId);
                        resourcePools.add(resourcePool);
                    }
                }
            }
            return resourcePools;
        }
        finally {
            APITrace.end();
        }
    }

    private @Nullable Volume toVolume(@Nonnull VirtualDisk disk, @Nonnull String vmId, @Nonnull String regionId) {
        Volume volume = new Volume();

        VirtualDeviceFileBackingInfo info = (VirtualDeviceFileBackingInfo)disk.getBacking();
        String filePath = info.getFileName();
        String fileName = filePath.substring(info.getFileName().lastIndexOf("/") + 1);
        volume.setTag("filePath", filePath);

        volume.setProviderVolumeId(fileName);
        volume.setName(disk.getDeviceInfo().getLabel());
        volume.setProviderRegionId(regionId);
        volume.setDescription(disk.getDeviceInfo().getSummary());
        volume.setCurrentState(VolumeState.AVAILABLE);
        volume.setDeleteOnVirtualMachineTermination(true);
        volume.setDeviceId(disk.getUnitNumber().toString());
        volume.setFormat(VolumeFormat.BLOCK);
        volume.setProviderVirtualMachineId(vmId);
        volume.setSize(new Storage<Kilobyte>(disk.getCapacityInKB(), Storage.KILOBYTE));
        volume.setType(VolumeType.SSD);

        if (volume.getProviderVolumeId() == null) {
            volume.setProviderVolumeId(vmId+"-"+volume.getName());
        }
        if (volume.getDeviceId().equals("0")) {
            volume.setRootVolume(true);
        }
        return volume;
    }

    private @Nullable Volume toVolume(@Nonnull FileInfo disk, @Nullable String dataCenterId, @Nonnull String regionId) {
        Volume volume = new Volume();
        volume.setProviderVolumeId(disk.getPath());
        volume.setName(disk.getPath());
        volume.setProviderDataCenterId(dataCenterId);
        volume.setProviderRegionId(regionId);
        volume.setDescription(disk.getPath());
        volume.setCurrentState(VolumeState.AVAILABLE);
        volume.setDeleteOnVirtualMachineTermination(true);
        volume.setFormat(VolumeFormat.BLOCK);
        if (disk.getFileSize() != null) {
            volume.setSize(new Storage<org.dasein.util.uom.storage.Byte>(disk.getFileSize(), Storage.BYTE));
        }
        volume.setType(VolumeType.SSD);
        XMLGregorianCalendar cal = disk.getModification();
        if (cal != null) {
            volume.setCreationTimestamp(cal.toGregorianCalendar().getTimeInMillis());
        }
        volume.setRootVolume(false);
        return volume;
    }
}
