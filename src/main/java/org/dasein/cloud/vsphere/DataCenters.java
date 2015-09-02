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

import org.apache.log4j.Logger;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.ProviderContext;
import org.dasein.cloud.dc.DataCenter;
import org.dasein.cloud.dc.DataCenterCapabilities;
import org.dasein.cloud.dc.DataCenterServices;
import org.dasein.cloud.dc.Folder;
import org.dasein.cloud.dc.Region;
import org.dasein.cloud.dc.ResourcePool;
import org.dasein.cloud.dc.StoragePool;
import org.dasein.cloud.util.APITrace;
import org.dasein.cloud.util.Cache;
import org.dasein.cloud.util.CacheLevel;
import org.dasein.cloud.vsphere.capabilities.VsphereDataCenterCapabilities;
import org.dasein.util.uom.time.Day;
import org.dasein.util.uom.time.Hour;
import org.dasein.util.uom.time.TimePeriod;

import com.vmware.vim25.DynamicProperty;
import com.vmware.vim25.InvalidPropertyFaultMsg;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.ObjectContent;
import com.vmware.vim25.ObjectSpec;
import com.vmware.vim25.PropertyFilterSpec;
import com.vmware.vim25.PropertySpec;
import com.vmware.vim25.RuntimeFaultFaultMsg;
import com.vmware.vim25.SelectionSpec;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.TraversalSpec;
import com.vmware.vim25.VimPortType;
import com.vmware.vim25.VimService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * Unimplemented skeleton class
 * @author INSERT NAME HERE
 * @version 2013.01 initial version
 * @since 2013.01
 */
public class DataCenters implements DataCenterServices {
    static private final Logger logger = Vsphere.getLogger(DataCenters.class);

    private Vsphere provider;

    DataCenters(@Nonnull Vsphere provider) { this.provider = provider; }

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
    public @Nonnull String getProviderTermForDataCenter(@Nonnull Locale locale) {
        return getCapabilities().getProviderTermForDataCenter(locale);
    }

    @Override
    public @Nonnull String getProviderTermForRegion(@Nonnull Locale locale) {
        return getCapabilities().getProviderTermForRegion(locale);
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
            // TODO: query the API for the data center list
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
        //
        // WIP
        //
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

            ManagedObjectReference retVal = null;
            ManagedObjectReference rootFolder = serviceContent.getRootFolder();
            
            //-----------------------------------
            
            // Create a traversal spec that starts from the 'root' objects
            SelectionSpec sSpec = new SelectionSpec();
            sSpec.setName("VisitFolders");


            TraversalSpec dataCenterToVMFolder = new TraversalSpec();
            dataCenterToVMFolder.setName("DataCenterToVMFolder");
            dataCenterToVMFolder.setType("Datacenter");
            dataCenterToVMFolder.setPath("vmFolder");
            dataCenterToVMFolder.setSkip(false);
            dataCenterToVMFolder.getSelectSet().add(sSpec);
            
            TraversalSpec traversalSpec = new TraversalSpec();
            traversalSpec.setName("VisitFolders");
            traversalSpec.setType("Folder");
            traversalSpec.setPath("childEntity");
            traversalSpec.setSkip(false);
            List<SelectionSpec> sSpecArr = new ArrayList<SelectionSpec>();
            sSpecArr.add(sSpec);
            sSpecArr.add(dataCenterToVMFolder);
            //sSpecArr.add(vAppToVM);
            //sSpecArr.add(vAppToVApp);
            traversalSpec.getSelectSet().addAll(sSpecArr);
            //----------------------------------------
            // Create Property Spec
            PropertySpec propertySpec = new PropertySpec();
            propertySpec.setAll(Boolean.FALSE);
            propertySpec.getPathSet().add("name");
            propertySpec.setType("Datacenter");

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
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            

            List<ObjectContent> listobcont = null;
            try {
                listobcont = vimPortType.retrieveProperties(vimServiceContent.getPropertyCollector(), listfps);
            } catch ( InvalidPropertyFaultMsg e ) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch ( RuntimeFaultFaultMsg e ) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            if (listobcont != null) {
               // listobcont contains Datacenter = datacenter-21 <<<<< WANT TO RETURN THIS....
               for (ObjectContent oc : listobcont) {
                  ManagedObjectReference mr = oc.getObj();
                  String dcnm = null;
                  List<DynamicProperty> dps = oc.getPropSet();
                  if (dps != null) {
                     //Since there is only one property PropertySpec pathset
                     //this array contains only one value
                     for (DynamicProperty dp : dps) {
                        dcnm = (String) dp.getVal(); // WTC
                     }
                  }
                  //This is done outside of the previous for loop to break
                  //out of the loop as soon as the required datacenter is found.
                  if (dcnm != null && dcnm.equals("hostname")) {
                     retVal = mr;
                     break;
                  }
               }
            }
            
            
            
            // TODO: query the API for the regions
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
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResourcePool getResourcePool(String providerResourcePoolId) throws InternalException, CloudException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Iterable<StoragePool> listStoragePools() throws InternalException, CloudException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public StoragePool getStoragePool(String providerStoragePoolId) throws InternalException, CloudException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Iterable<Folder> listVMFolders() throws InternalException, CloudException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Folder getVMFolder(String providerVMFolderId) throws InternalException, CloudException {
        // TODO Auto-generated method stub
        return null;
    }
}
