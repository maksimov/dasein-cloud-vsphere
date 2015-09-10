/**
 * Copyright (C) 2012-2013 Dell, Inc.
 * See annotations for authorship information
 *
 * ====================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ====================================================================
 */

package org.dasein.cloud.vsphere;

import com.vmware.vim25.*;
import org.apache.log4j.Logger;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.ProviderContext;
import org.dasein.cloud.dc.*;
import org.dasein.cloud.util.APITrace;
import org.dasein.cloud.util.Cache;
import org.dasein.cloud.util.CacheLevel;
import org.dasein.cloud.vsphere.capabilities.VsphereDataCenterCapabilities;
import org.dasein.util.uom.storage.*;
import org.dasein.util.uom.time.Day;
import org.dasein.util.uom.time.Hour;
import org.dasein.util.uom.time.TimePeriod;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * @author Danielle Mayne
 * @version 2015.09 initial version
 * @since 2015.09
 */
public class DataCenters extends AbstractDataCenterServices<Vsphere> {
    static private final Logger logger = Vsphere.getLogger(DataCenters.class);

    private Vsphere provider;

    protected DataCenters(@Nonnull Vsphere provider) {
        super(provider);
        this.provider = provider;
    }

    @Override
    public @Nullable DataCenter getDataCenter(@Nonnull String dataCenterId) throws InternalException, CloudException {
        for( Region region : listRegions() ) {
            for( DataCenter dc : listDataCenters(region.getProviderRegionId()) ) {
                if( dataCenterId.equals(dc.getProviderDataCenterId()) ) {
                    return dc;
                }
            }
        }
        return null;
    }

    @Override
    public @Nullable Region getRegion(@Nonnull String providerRegionId) throws InternalException, CloudException {
        for( Region r : listRegions() ) {
            if( providerRegionId.equals(r.getProviderRegionId()) ) {
                return r;
            }
        }
        return null;
    }

    @Override
    public @Nonnull Collection<DataCenter> listDataCenters(@Nonnull String providerRegionId) throws InternalException, CloudException {
        APITrace.begin(provider, "listDataCenters");
        try {

            Region region = getRegion(providerRegionId);

            if( region == null ) {
                throw new CloudException("No such region: " + providerRegionId);
            }
            ProviderContext ctx = provider.getContext();

            if( ctx == null ) {
                throw new NoContextException();
            }
            Cache<DataCenter> cache = Cache.getInstance(provider, "dataCenters", DataCenter.class, CacheLevel.REGION_ACCOUNT, new TimePeriod<Day>(1, TimePeriod.DAY));
            Collection<DataCenter> dcList = (Collection<DataCenter>)cache.get(ctx);

            if( dcList != null ) {
                return dcList;
            }
            ArrayList<DataCenter> dataCenters = new ArrayList<DataCenter>();

            VsphereConnection vsphereConnection = provider.getServiceInstance();
            ServiceContent serviceContent = vsphereConnection.getServiceContent();
            VimPortType vimPortType = vsphereConnection.getVimPort();

            ManagedObjectReference rootFolder = serviceContent.getRootFolder();

            TraversalSpec traversalSpec = getHostFolderTraversalSpec();

            // Create Property Spec
            PropertySpec propertySpec = new PropertySpec();
            propertySpec.setAll(Boolean.FALSE);
            propertySpec.getPathSet().add("name");
            propertySpec.setType("ClusterComputeResource");

            List<ObjectContent> listobcont = getObjectList(rootFolder, traversalSpec, propertySpec, vimPortType);

            if (listobcont != null) {
                for (ObjectContent oc : listobcont) {
                    ManagedObjectReference mr = oc.getObj();
                    String dcnm = null;
                    List<DynamicProperty> dps = oc.getPropSet();
                    if (dps != null) {
                        //Since there is only one property PropertySpec pathset
                        //this array contains only one value
                        for (DynamicProperty dp : dps) {
                            dcnm = (String) dp.getVal();
                            DataCenter dc = toDataCenter(mr.getValue(), dcnm, providerRegionId);
                            if ( dc != null ) {
                                dataCenters.add(dc);
                            }
                        }
                    }
                }
            }
            if ( dataCenters.size() == 0 ) {
                // create a dummy dc based on the region (vSphere datacenter)
                DataCenter dc = toDataCenter(providerRegionId+"-a", region.getName(), providerRegionId);
                dataCenters.add(dc);
            }
            cache.put(ctx, dataCenters);
            return dataCenters;
        }
        finally {
            APITrace.end();
        }
    }
    private static final String VIMSERVICEINSTANCETYPE = "ServiceInstance";
    private static final String VIMSERVICEINSTANCEVALUE = "ServiceInstance";
    
    @Override
    public Collection<Region> listRegions() throws InternalException, CloudException {
        APITrace.begin(provider, "listRegions");
        try {
            ProviderContext ctx = provider.getContext();

            if( ctx == null ) {
                throw new NoContextException(); 
            }
            Cache<Region> cache = Cache.getInstance(provider, "regions", Region.class, CacheLevel.CLOUD_ACCOUNT, new TimePeriod<Hour>(10, TimePeriod.HOUR));
            Collection<Region> regions = (Collection<Region>)cache.get(ctx);

            if( regions != null ) {
                return regions;
            }
            regions = new ArrayList<Region>();

            VsphereConnection vsphereConnection = provider.getServiceInstance();
            ServiceContent serviceContent = vsphereConnection.getServiceContent();
            VimPortType vimPortType = vsphereConnection.getVimPort();

            ManagedObjectReference rootFolder = serviceContent.getRootFolder();

            TraversalSpec traversalSpec = getVmFolderTraversalSpec();
            // Create Property Spec
            PropertySpec propertySpec = new PropertySpec();
            propertySpec.setAll(Boolean.FALSE);
            propertySpec.getPathSet().add("name");
            propertySpec.setType("Datacenter");

            List<ObjectContent> listobcont = getObjectList(rootFolder, traversalSpec, propertySpec, vimPortType);

            if (listobcont != null) {
               for (ObjectContent oc : listobcont) {
                  ManagedObjectReference mr = oc.getObj();
                  String dcnm = null;
                  List<DynamicProperty> dps = oc.getPropSet();
                  if (dps != null) {
                     //Since there is only one property PropertySpec pathset
                     //this array contains only one value
                     for (DynamicProperty dp : dps) {
                        dcnm = (String) dp.getVal();
                         Region region = toRegion(mr.getValue(), dcnm);
                         if ( region != null ) {
                             regions.add(region);
                         }
                     }
                  }
               }
            }
            cache.put(ctx, regions);
            return regions;

        } catch (Exception e) {
            System.out.println(e);
        }
        finally {
            APITrace.end();
        }
        return null;
    }

    @Override
    public DataCenterCapabilities getCapabilities() {
        return new VsphereDataCenterCapabilities(provider);
    }

    @Override
    public Iterable<ResourcePool> listResourcePools(String providerDataCenterId) throws InternalException, CloudException {
        APITrace.begin(provider, "listResourcePools");
        try {
            ProviderContext ctx = provider.getContext();

            if( ctx == null ) {
                throw new NoContextException();
            }
            Cache<ResourcePool> cache = Cache.getInstance(provider, "resourcePools", ResourcePool.class, CacheLevel.CLOUD_ACCOUNT, new TimePeriod<Hour>(10, TimePeriod.HOUR));
            Collection<ResourcePool> resourcePools = (Collection<ResourcePool>)cache.get(ctx);

            if( resourcePools != null ) {
                return resourcePools;
            }
            resourcePools = new ArrayList<ResourcePool>();

            VsphereConnection vsphereConnection = provider.getServiceInstance();
            ServiceContent serviceContent = vsphereConnection.getServiceContent();
            VimPortType vimPortType = vsphereConnection.getVimPort();

            ManagedObjectReference rootFolder = serviceContent.getRootFolder();

            TraversalSpec traversalSpec = getHostFolderTraversalSpec();

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
            crToRp.setSkip(Boolean.TRUE);
            crToRp.setName("crToRp");
            crToRp.getSelectSet().add(sSpec);

            List<SelectionSpec> selectionSpecsArr = new ArrayList<>();
            selectionSpecsArr.add(sSpec);
            selectionSpecsArr.add(rpToRp);
            selectionSpecsArr.add(crToRp);

            traversalSpec.getSelectSet().addAll(selectionSpecsArr);
            // Create Property Spec
            PropertySpec propertySpec = new PropertySpec();
            propertySpec.setAll(Boolean.FALSE);
            propertySpec.getPathSet().add("name");
            propertySpec.getPathSet().add("owner");
            propertySpec.getPathSet().add("runtime");
            propertySpec.setType("ResourcePool");

            List<ObjectContent> listobcont = getObjectList(rootFolder, traversalSpec, propertySpec, vimPortType);

            if (listobcont != null) {
                for (ObjectContent oc : listobcont) {
                    ManagedObjectReference rpRef = oc.getObj();
                    String rpId = rpRef.getValue();
                    String rpName = null, rpStatus = null, owner = null;
                    List<DynamicProperty> dps = oc.getPropSet();
                    if (dps != null) {
                        for (DynamicProperty dp : dps) {
                            if (dp.getName().equals("name")) {
                                rpName = (String) dp.getVal();
                            }
                            else if (dp.getName().equals("owner")) {
                                ManagedObjectReference clusterRef = (ManagedObjectReference) dp.getVal();
                                owner = clusterRef.getValue();
                            }
                            else if (dp.getName().equals("runtime")) {
                                ResourcePoolRuntimeInfo rpri = (ResourcePoolRuntimeInfo) dp.getVal();
                                rpStatus = rpri.getOverallStatus().value();
                            }
                        }
                    }
                    ResourcePool resourcePool = toResourcePool(rpId, rpName, owner, rpStatus);
                    if ( resourcePool != null && resourcePool.getDataCenterId().equals(providerDataCenterId)) {
                        resourcePools.add(resourcePool);
                    }
                }
            }
            cache.put(ctx, resourcePools);
            return resourcePools;

        } catch (Exception e) {
            System.out.println(e);
        }
        finally {
            APITrace.end();
        }
        return null;
    }

    @Override
    public ResourcePool getResourcePool(String providerResourcePoolId) throws InternalException, CloudException {
        APITrace.begin(provider, "DataCenters.getResourcePool");
        try {
            Iterable<DataCenter> dcs = listDataCenters(getContext().getRegionId());

            for (DataCenter dc : dcs) {
                Iterable<org.dasein.cloud.dc.ResourcePool> rps = listResourcePools(dc.getProviderDataCenterId());
                for (org.dasein.cloud.dc.ResourcePool rp : rps) {
                    if (rp.getProvideResourcePoolId().equals(providerResourcePoolId)) {
                        return rp;
                    }
                }
            }
            return null;
        }
        finally {
            APITrace.end();
        }
    }

    @Override
    public Collection<StoragePool> listStoragePools() throws InternalException, CloudException {
        APITrace.begin(provider, "listStoragePools");
        try {
            ProviderContext ctx = provider.getContext();

            if( ctx == null ) {
                throw new NoContextException();
            }
            Cache<StoragePool> cache = Cache.getInstance(provider, "storagePools", StoragePool.class, CacheLevel.CLOUD_ACCOUNT, new TimePeriod<Hour>(10, TimePeriod.HOUR));
            Collection<StoragePool> storagePools = (Collection<StoragePool>)cache.get(ctx);

            if( storagePools != null ) {
                return storagePools;
            }
            storagePools = new ArrayList<StoragePool>();

            VsphereConnection vsphereConnection = provider.getServiceInstance();
            ServiceContent serviceContent = vsphereConnection.getServiceContent();
            VimPortType vimPortType = vsphereConnection.getVimPort();

            ManagedObjectReference rootFolder = serviceContent.getRootFolder();

            TraversalSpec traversalSpec = getHostFolderTraversalSpec();

            //todo
            // DC -> DS
            TraversalSpec dcToDs = new TraversalSpec();
            dcToDs.setType("Datacenter");
            dcToDs.setPath("datastore");
            dcToDs.setName("dcToDs");
            dcToDs.setSkip(Boolean.FALSE);

            List<SelectionSpec> selectionSpecsArr = new ArrayList<>();
            selectionSpecsArr.add(dcToDs);

            traversalSpec.getSelectSet().addAll(selectionSpecsArr);
            // Create Property Spec
            PropertySpec propertySpec = new PropertySpec();
            propertySpec.setAll(Boolean.FALSE);
            propertySpec.getPathSet().add("summary");
            propertySpec.setType("Datastore");

            List<ObjectContent> listobcont = getObjectList(rootFolder, traversalSpec, propertySpec, vimPortType);

            if (listobcont != null) {
                for (ObjectContent oc : listobcont) {
                    ManagedObjectReference dsRef = oc.getObj();
                    String dsId = dsRef.getValue();
                    DatastoreSummary dsSummary = null;
                    List<DynamicProperty> dps = oc.getPropSet();
                    if (dps != null) {
                        for (DynamicProperty dp : dps) {
                            if (dp.getName().equals("summary")) {
                                dsSummary = (DatastoreSummary) dp.getVal();
                            }
                        }
                    }
                    StoragePool storagePool = toStoragePool(dsSummary, dsId, "testHost", "testDataCenter");
                    if ( storagePool != null ) {
                        storagePools.add(storagePool);
                    }
                }
            }
            cache.put(ctx, storagePools);
            return storagePools;

        } catch (Exception e) {
            System.out.println(e);
        }
        finally {
            APITrace.end();
        }
        return null;
    }

    @Override
    public StoragePool getStoragePool(String providerStoragePoolId) throws InternalException, CloudException {
        APITrace.begin(provider, "DataCenters.getStoragePool");
        try {
            Collection<StoragePool> pools = listStoragePools();
            for (StoragePool pool : pools) {
                if (pool.getStoragePoolId().equals(providerStoragePoolId)) {
                    return pool;
                }
            }
            return null;
        }
        finally {
            APITrace.end();
        }
    }

    @Override
    public Collection<Folder> listVMFolders() throws InternalException, CloudException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Folder getVMFolder(String providerVMFolderId) throws InternalException, CloudException {
        APITrace.begin(provider, "DataCenters.getVMFolder");
        try {
            Collection<Folder> folders = listVMFolders();
            for (Folder folder : folders) {
                if (folder.getId().equals(providerVMFolderId)) {
                    return folder;
                }
            }
            return null;
        }
        finally {
            APITrace.end();
        }
    }

    private @Nonnull TraversalSpec getHostFolderTraversalSpec() {
        // Create a traversal spec that starts from the 'root' objects
        SelectionSpec sSpec = new SelectionSpec();
        sSpec.setName("VisitFolders");


        TraversalSpec dataCenterToHostFolder = new TraversalSpec();
        dataCenterToHostFolder.setName("DataCenterToHostFolder");
        dataCenterToHostFolder.setType("Datacenter");
        dataCenterToHostFolder.setPath("hostFolder");
        dataCenterToHostFolder.setSkip(false);
        dataCenterToHostFolder.getSelectSet().add(sSpec);

        TraversalSpec traversalSpec = new TraversalSpec();
        traversalSpec.setName("VisitFolders");
        traversalSpec.setType("Folder");
        traversalSpec.setPath("childEntity");
        traversalSpec.setSkip(false);
        List<SelectionSpec> sSpecArr = new ArrayList<SelectionSpec>();
        sSpecArr.add(sSpec);
        sSpecArr.add(dataCenterToHostFolder);
        traversalSpec.getSelectSet().addAll(sSpecArr);
        return traversalSpec;
    }

    private @Nonnull TraversalSpec getVmFolderTraversalSpec() {
        // Create a traversal spec that starts from the 'root' objects
        SelectionSpec sSpec = new SelectionSpec();
        sSpec.setName("VisitFolders");


        TraversalSpec dataCenterToHostFolder = new TraversalSpec();
        dataCenterToHostFolder.setName("DataCenterToVmFolder");
        dataCenterToHostFolder.setType("Datacenter");
        dataCenterToHostFolder.setPath("vmFolder");
        dataCenterToHostFolder.setSkip(false);
        dataCenterToHostFolder.getSelectSet().add(sSpec);

        TraversalSpec traversalSpec = new TraversalSpec();
        traversalSpec.setName("VisitFolders");
        traversalSpec.setType("Folder");
        traversalSpec.setPath("childEntity");
        traversalSpec.setSkip(false);
        List<SelectionSpec> sSpecArr = new ArrayList<SelectionSpec>();
        sSpecArr.add(sSpec);
        sSpecArr.add(dataCenterToHostFolder);
        traversalSpec.getSelectSet().addAll(sSpecArr);
        return traversalSpec;
    }

    private @Nullable List<ObjectContent> getObjectList(@Nonnull ManagedObjectReference rootFolder, @Nonnull TraversalSpec traversalSpec, @Nonnull PropertySpec propertySpec, @Nonnull VimPortType vimPortType) throws CloudException{
        // Now create Object Spec
        ObjectSpec objectSpec = new ObjectSpec();
        objectSpec.setObj(rootFolder);
        objectSpec.setSkip(Boolean.TRUE);
        objectSpec.getSelectSet().add(traversalSpec);

        // Create PropertyFilterSpec using the PropertySpec and ObjectPec
        // created above.
        PropertyFilterSpec propertyFilterSpec = new PropertyFilterSpec();
        propertyFilterSpec.getPropSet().add(propertySpec);
        propertyFilterSpec.getObjectSet().add(objectSpec);

        List<PropertyFilterSpec> listfps = new ArrayList<PropertyFilterSpec>(1);
        listfps.add(propertyFilterSpec);
        ServiceContent vimServiceContent = null;
        try {
            //VimService serviceInstance = provider.getServiceInstance();
            ManagedObjectReference ref = new ManagedObjectReference();
            ref.setType(VIMSERVICEINSTANCETYPE);
            ref.setValue(VIMSERVICEINSTANCEVALUE);
            vimServiceContent = vimPortType.retrieveServiceContent(ref);
        } catch ( RuntimeFaultFaultMsg e ) {
            throw new CloudException(e);
        }
        List<ObjectContent> listobcont = null;
        try {
            listobcont = vimPortType.retrieveProperties(vimServiceContent.getPropertyCollector(), listfps);
        } catch ( InvalidPropertyFaultMsg e ) {
            throw new CloudException(e);
        } catch ( RuntimeFaultFaultMsg e ) {
            throw new CloudException(e);
        }
        return listobcont;
    }

    private @Nullable Region toRegion(@Nonnull String regionId, @Nonnull String regionName ) {
        Region region = new Region();
        region.setActive(true);
        region.setAvailable(true);
        region.setJurisdiction("US");
        region.setName(regionName);
        region.setProviderRegionId(regionId);
        return region;
    }

    private @Nullable DataCenter toDataCenter(@Nonnull String dataCenterId, @Nonnull String datacenterName, @Nonnull String regionId) {
        DataCenter dc = new DataCenter(dataCenterId, datacenterName, regionId, true, true);
        return dc;
    }

    private ResourcePool toResourcePool(@Nonnull String resourcePoolId, @Nonnull String resourcePoolName, @Nonnull String providerDataCenterId, @Nullable String status) {
        ResourcePool rp = new ResourcePool();
        rp.setName(resourcePoolName);
        rp.setDataCenterId(providerDataCenterId);

        if (status != null) {
            rp.setAvailable( (!status.toLowerCase().equals("red") && !status.toLowerCase().equals("yellow")) );
        }
        rp.setProvideResourcePoolId(resourcePoolId);

        return rp;
    }

    private StoragePool toStoragePool(DatastoreSummary ds, String dsId, String hostName, String datacenter) {
        StoragePool sp = new StoragePool();
        sp.setAffinityGroupId(hostName);
        sp.setDataCenterId(datacenter);
        sp.setRegionId(provider.getContext().getRegionId());
        sp.setStoragePoolName(ds.getName());
        sp.setStoragePoolId(dsId);

        long capacityBytes = ds.getCapacity();
        long freeBytes = ds.getFreeSpace();
        long provisioned = capacityBytes-freeBytes;
        sp.setCapacity((Storage<Megabyte>)new Storage<org.dasein.util.uom.storage.Byte>(capacityBytes, Storage.BYTE).convertTo(Storage.MEGABYTE));
        sp.setFreeSpace((Storage<Megabyte>)new Storage<org.dasein.util.uom.storage.Byte>(freeBytes, Storage.BYTE).convertTo(Storage.MEGABYTE));
        sp.setProvisioned((Storage<Megabyte>)new Storage<org.dasein.util.uom.storage.Byte>(provisioned, Storage.BYTE).convertTo(Storage.MEGABYTE));
        return sp;
    }
}
