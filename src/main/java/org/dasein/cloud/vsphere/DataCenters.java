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

import org.apache.commons.collections.IteratorUtils;
import org.apache.log4j.Logger;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonMethod;
import org.codehaus.jackson.map.ObjectMapper;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.ProviderContext;
import org.dasein.cloud.compute.AffinityGroup;
import org.dasein.cloud.compute.AffinityGroupFilterOptions;
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

import java.io.File;
import java.io.IOException;
import java.util.*;

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

    public RetrieveResult retrieveObjectList(Vsphere provider, @Nonnull String baseFolder, @Nullable List<SelectionSpec> selectionSpecsArr, @Nonnull List<PropertySpec> pSpecs) throws InternalException, CloudException {
        VsphereInventoryNavigation nav = new VsphereInventoryNavigation();
        return nav.retrieveObjectList(provider, baseFolder, selectionSpecsArr, pSpecs);
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
        APITrace.begin(getProvider(), "listDataCenters");
        try {

            Region region = getRegion(providerRegionId);

            if( region == null ) {
                throw new CloudException("No such region: " + providerRegionId);
            }
          /*  ProviderContext ctx = getProvider().getContext();

            if( ctx == null ) {
                throw new NoContextException();
            }
            Cache<DataCenter> cache = Cache.getInstance(getProvider(), "dataCenters", DataCenter.class, CacheLevel.REGION_ACCOUNT, new TimePeriod<Day>(1, TimePeriod.DAY));
            Collection<DataCenter> dcList = (Collection<DataCenter>)cache.get(ctx);

            if( dcList != null ) {
                return dcList;
            }   */
            ArrayList<DataCenter> dataCenters = new ArrayList<DataCenter>();

            // Create Property Spec
            List<PropertySpec>  pSpecs = new ArrayList<PropertySpec>();
            PropertySpec propertySpec = new PropertySpec();
            propertySpec.setAll(Boolean.FALSE);
            propertySpec.getPathSet().add("name");
            propertySpec.getPathSet().add("overallStatus");
            propertySpec.setType("ClusterComputeResource");
            pSpecs.add(propertySpec);

            RetrieveResult listobcont = retrieveObjectList(provider, "hostFolder", null, pSpecs);

            if (listobcont != null) {
                for (ObjectContent oc : listobcont.getObjects()) {
                    ManagedObjectReference mr = oc.getObj();
                    String dcnm = null, status = null;
                    List<DynamicProperty> dps = oc.getPropSet();
                    if (dps != null) {
                        for (DynamicProperty dp : dps) {
                            if (dp.getName().equals("name") ) {
                                dcnm = (String) dp.getVal();
                            }
                            else if (dp.getName().equals("overallStatus")) {
                                ManagedEntityStatus mes = (ManagedEntityStatus) dp.getVal();
                                status = mes.value();
                            }
                        }
                        DataCenter dc = toDataCenter(mr.getValue(), dcnm, providerRegionId, status);
                        if ( dc != null ) {
                            dataCenters.add(dc);
                        }
                    }
                }
            }
            if ( dataCenters.size() == 0 ) {
                // create a dummy dc based on the region (vSphere datacenter)
                // this environment does not support clusters but we need a DCM datacenter mapping
                DataCenter dc = toDataCenter(providerRegionId+"-a", region.getName(), providerRegionId, "active");
                dataCenters.add(dc);
            }
          //  cache.put(ctx, dataCenters);
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
            /*ProviderContext ctx = provider.getContext();

            if( ctx == null ) {
                throw new NoContextException(); 
            }
            Cache<Region> cache = Cache.getInstance(provider, "regions", Region.class, CacheLevel.CLOUD_ACCOUNT, new TimePeriod<Hour>(10, TimePeriod.HOUR));
            Collection<Region> regions = (Collection<Region>)cache.get(ctx);

            if( regions != null ) {
                return regions;
            } */
            Collection<Region> regions = new ArrayList<Region>();

            // Create Property Spec
            List<PropertySpec> pSpecs = new ArrayList<PropertySpec>();
            PropertySpec propertySpec = new PropertySpec();
            propertySpec.setAll(Boolean.FALSE);
            propertySpec.getPathSet().add("name");
            propertySpec.setType("Datacenter");
            pSpecs.add(propertySpec);

            RetrieveResult listobcont = retrieveObjectList(provider, "hostFolder", null, pSpecs);

            if (listobcont != null) {
               for (ObjectContent oc : listobcont.getObjects()) {
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
          //  cache.put(ctx, regions);
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
           /* ProviderContext ctx = provider.getContext();

            if( ctx == null ) {
                throw new NoContextException();
            }
            Cache<ResourcePool> cache = Cache.getInstance(provider, "resourcePools", ResourcePool.class, CacheLevel.CLOUD_ACCOUNT, new TimePeriod<Hour>(10, TimePeriod.HOUR));
            Collection<ResourcePool> resourcePools = (Collection<ResourcePool>)cache.get(ctx);

            if( resourcePools != null ) {
                return resourcePools;
            }  */
            Collection<ResourcePool> resourcePools = new ArrayList<ResourcePool>();

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

            List<SelectionSpec> selectionSpecsArr = new ArrayList<SelectionSpec>();
            selectionSpecsArr.add(sSpec);
            selectionSpecsArr.add(rpToRp);
            selectionSpecsArr.add(crToRp);

            // Create Property Spec
            List<PropertySpec> pSpecs = new ArrayList<PropertySpec>();
            PropertySpec propertySpec = new PropertySpec();
            propertySpec.setAll(Boolean.FALSE);
            propertySpec.getPathSet().add("name");
            propertySpec.getPathSet().add("owner");
            propertySpec.getPathSet().add("runtime");
            propertySpec.setType("ResourcePool");
            pSpecs.add(propertySpec);

            RetrieveResult listobcont = retrieveObjectList(provider, "hostFolder", selectionSpecsArr, pSpecs);

            if (listobcont != null) {
                for (ObjectContent oc : listobcont.getObjects()) {
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
                    if ( resourcePool != null && (providerDataCenterId == null || resourcePool.getDataCenterId().equals(providerDataCenterId)) ) {
                        resourcePools.add(resourcePool);
                    }
                }
            }
          //  cache.put(ctx, resourcePools);
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
            Iterable<org.dasein.cloud.dc.ResourcePool> rps = listResourcePools(null);
            for (org.dasein.cloud.dc.ResourcePool rp : rps) {
                if (rp.getProvideResourcePoolId().equals(providerResourcePoolId)) {
                    return rp;
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
           /* ProviderContext ctx = provider.getContext();

            if( ctx == null ) {
                throw new NoContextException();
            }
            Cache<StoragePool> cache = Cache.getInstance(provider, "storagePools", StoragePool.class, CacheLevel.CLOUD_ACCOUNT, new TimePeriod<Hour>(10, TimePeriod.HOUR));
            Collection<StoragePool> storagePools = (Collection<StoragePool>)cache.get(ctx);

            if( storagePools != null ) {
                return storagePools;
            } */
            Collection<StoragePool> storagePools = new ArrayList<StoragePool>();

            // DC -> DS
            TraversalSpec dcToDs = new TraversalSpec();
            dcToDs.setType("Datacenter");
            dcToDs.setPath("datastore");
            dcToDs.setName("dcToDs");
            dcToDs.setSkip(Boolean.FALSE);

            List<SelectionSpec> selectionSpecsArr = new ArrayList<SelectionSpec>();
            selectionSpecsArr.add(dcToDs);

            // Create Property Spec
            List<PropertySpec> pSpecs = new ArrayList<PropertySpec>();
            PropertySpec propertySpec = new PropertySpec();
            propertySpec.setAll(Boolean.FALSE);
            propertySpec.getPathSet().add("summary");
            propertySpec.getPathSet().add("host");
            propertySpec.setType("Datastore");
            pSpecs.add(propertySpec);

            RetrieveResult listobcont = retrieveObjectList(provider, "hostFolder", selectionSpecsArr, pSpecs);

            if (listobcont != null) {
                Iterable<AffinityGroup> allHosts = provider.getComputeServices().getAffinityGroupSupport().list(AffinityGroupFilterOptions.getInstance());
                for (ObjectContent oc : listobcont.getObjects()) {
                    ManagedObjectReference dsRef = oc.getObj();
                    String dsId = dsRef.getValue();
                    DatastoreSummary dsSummary = null;
                    String datastoreHostId = null, datastoreDataCenterId = null;
                    List<DynamicProperty> dps = oc.getPropSet();
                    if (dps != null) {
                        for (DynamicProperty dp : dps) {
                            if (dp.getName().equals("summary")) {
                                dsSummary = (DatastoreSummary) dp.getVal();
                            }
                            else if (dp.getName().equals("host")) {
                                ArrayOfDatastoreHostMount dhm = (ArrayOfDatastoreHostMount) dp.getVal();
                                List<DatastoreHostMount> list = dhm.getDatastoreHostMount();
                                if (list.size() == 1) {
                                    datastoreHostId = list.get(0).getKey().getValue();
                                    List<AffinityGroup> myTempList = IteratorUtils.toList(allHosts.iterator());
                                    for (AffinityGroup host : myTempList) {
                                        if (datastoreHostId.equals(host.getAffinityGroupId())) {
                                            datastoreDataCenterId = host.getDataCenterId();
                                            break;
                                        }
                                    }
                                }
                                else {
                                    boolean shared = false, firstTime= true;
                                    for (DatastoreHostMount mount : list) {
                                        String hostMountId = mount.getKey().getValue();
                                        List<AffinityGroup> myTempList = IteratorUtils.toList(allHosts.iterator());
                                        for (AffinityGroup host : myTempList) {
                                            if (hostMountId.equals(host.getAffinityGroupId())) {
                                                if (datastoreDataCenterId != null && !datastoreDataCenterId.equals(host.getDataCenterId())) {
                                                    datastoreDataCenterId = null;
                                                    shared = true;
                                                }
                                                else if (firstTime){
                                                    datastoreDataCenterId = host.getDataCenterId();
                                                    firstTime=false;
                                                }
                                                break;
                                            }
                                        }
                                        if (shared) {
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                    StoragePool storagePool = toStoragePool(dsSummary, dsId, datastoreHostId, datastoreDataCenterId);
                    if ( storagePool != null ) {
                        storagePools.add(storagePool);
                    }
                }
            }
           // cache.put(ctx, storagePools);
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
        APITrace.begin(provider, "listVMFolders");
        try {
          /*  ProviderContext ctx = provider.getContext();

            if( ctx == null ) {
                throw new NoContextException();
            }
            Cache<Folder> cache = Cache.getInstance(provider, "folders", Folder.class, CacheLevel.CLOUD_ACCOUNT, new TimePeriod<Hour>(10, TimePeriod.HOUR));
            Collection<Folder> folders = (Collection<Folder>)cache.get(ctx);*/
            Map<String, Folder> folderMap = new HashMap<String, Folder>();

           /* if( folders != null ) {
                return folders;
            } */
            Collection<Folder> folders = new ArrayList<Folder>();

            // Create Property Spec
            List<PropertySpec> pSpecs = new ArrayList<PropertySpec>();
            PropertySpec propertySpec = new PropertySpec();
            propertySpec.setAll(Boolean.FALSE);
            propertySpec.getPathSet().add("name");
            propertySpec.getPathSet().add("parent");
            propertySpec.getPathSet().add("childEntity");
            propertySpec.setType("Folder");
            pSpecs.add(propertySpec);

            RetrieveResult listobcont = retrieveObjectList(provider, "vmFolder", null, pSpecs);

            if (listobcont != null) {
                for (ObjectContent oc : listobcont.getObjects()) {
                    ManagedObjectReference fRef = oc.getObj();
                    String folderId = fRef.getValue();
                    String folderName = null, folderParent = null;
                    List<String> folderChildren = null;
                    List<DynamicProperty> dps = oc.getPropSet();
                    if (dps != null) {
                        for (DynamicProperty dp : dps) {
                            if (dp.getName().equals("name")) {
                                folderName = (String) dp.getVal();
                            }
                            else if (dp.getName().equals("parent")) {
                                ManagedObjectReference pRef = (ManagedObjectReference) dp.getVal();
                                if (pRef.getType().equals("Folder")) {
                                    folderParent = pRef.getValue();
                                }
                            }
                            else if (dp.getName().equals("childEntity")) {
                                ArrayOfManagedObjectReference cRefs = (ArrayOfManagedObjectReference) dp.getVal();
                                List<ManagedObjectReference> list = cRefs.getManagedObjectReference();
                                boolean firstTime = true;
                                for (ManagedObjectReference item : list) {
                                    if (item.getType().equals("Folder")) {
                                        if (firstTime) {
                                            folderChildren = new ArrayList<String>();
                                            firstTime=false;
                                        }
                                        folderChildren.add(item.getValue());
                                    }
                                }
                            }
                        }
                    }
                    Folder folder = toFolder(folderId, folderName, folderParent, folderChildren, FolderType.VM);
                    if ( folder != null ) {
                        folders.add(folder);
                        folderMap.put(folderId, folder);
                    }
                }
                for (Folder f : folders) {
                    if (f.getParent() != null) {
                        f.setParent(folderMap.get(f.getParent().getId()));
                    }

                    List<Folder> children = new ArrayList<Folder>();
                    if (f.getChildren() != null) {
                        for (Folder fChild : f.getChildren()) {
                            Folder tmpChild = folderMap.get(fChild.getId());
                            children.add(tmpChild);
                        }
                    }
                    f.setChildren(children);
                }
            }
           // cache.put(ctx, folders);
            return folders;

        } catch (Exception e) {
            System.out.println(e);
        }
        finally {
            APITrace.end();
        }
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

    private @Nullable Region toRegion(@Nonnull String regionId, @Nonnull String regionName ) {
        Region region = new Region();
        region.setActive(true);
        region.setAvailable(true);
        region.setJurisdiction("US");
        region.setName(regionName);
        region.setProviderRegionId(regionId);
        return region;
    }

    private @Nullable DataCenter toDataCenter(@Nonnull String dataCenterId, @Nonnull String datacenterName, @Nonnull String regionId, @Nonnull String status) {
        boolean available = true;
        if (status != null) {
            available = (!status.equalsIgnoreCase("red"));
        }
        DataCenter dc = new DataCenter(dataCenterId, datacenterName, regionId, available, available);
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

    private Folder toFolder(String folderId, String folderName, String folderParent, List<String> folderChildren, FolderType type) throws CloudException, InternalException{
        Folder f = new Folder();
        f.setId(folderId);
        f.setName(folderName);
        f.setType(type);

        if (folderParent != null) {
            Folder parent = new Folder();
            parent.setId(folderParent);
            f.setParent(parent);
        }

        if (folderChildren != null) {
            List<Folder> children = new ArrayList<Folder>();
            for (String childID : folderChildren) {
                Folder child = new Folder();
                child.setId(childID);
                children.add(child);
            }
            f.setChildren(children);
        }
        return f;
    }
}