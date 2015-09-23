package org.dasein.cloud.vsphere;

import mockit.*;

import com.vmware.vim25.*;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.compute.AffinityGroup;
import org.dasein.cloud.compute.AffinityGroupFilterOptions;
import org.dasein.cloud.compute.AffinityGroupSupport;
import org.dasein.cloud.dc.*;
import org.dasein.cloud.util.Cache;
import org.dasein.cloud.util.CacheLevel;
import org.dasein.cloud.vsphere.compute.VsphereCompute;
import org.dasein.util.uom.time.Day;
import org.dasein.util.uom.time.Hour;
import org.dasein.util.uom.time.TimePeriod;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * User: daniellemayne
 * Date: 14/09/2015
 * Time: 11:08
 */
@RunWith(JUnit4.class)
public class DataCentersTest extends VsphereTestBase{

    private ObjectManagement om = new ObjectManagement();
    @Mocked
    VsphereCompute vsphereComputeMock;
    @Mocked
    AffinityGroupSupport vsphereAGMock;

    @Test
    public void listRegionsTest() throws CloudException, InternalException {
        final DataCenters dc = new DataCenters(vsphereMock);
        Cache<Region> cache = Cache.getInstance(vsphereMock, "regions", Region.class, CacheLevel.CLOUD_ACCOUNT, new TimePeriod<Hour>(10, TimePeriod.HOUR));
        cache.clear(); //make sure cache is empty before we begin

        final List<PropertySpec> regPSpecs = dc.getRegionPropertySpec();

        new Expectations(DataCenters.class) {
            {dc.retrieveObjectList(vsphereMock, "hostFolder", null, regPSpecs);
                result = om.readJsonFile("src/test/resources/DataCenters/regions.json", RetrieveResult.class);
                times=1;
            }
        };

        Iterable<Region> regions = dc.listRegions();
        assertNotNull(regions);
        assertTrue(regions.iterator().hasNext());
        Region region = regions.iterator().next();
        assertEquals(region.getName(), "WTC");
        assertEquals(region.getProviderRegionId(), "datacenter-21");

        int count = 0;
        for (Region r : regions) {
            count++;
        }
        assertEquals("Number of regions returned is incorrect", 1, count);
    }

    @Test
    public void getRegionTest() throws CloudException, InternalException{
        final DataCenters dc = new DataCenters(vsphereMock);
        final List<PropertySpec> regPSpecs = dc.getRegionPropertySpec();

        new NonStrictExpectations(DataCenters.class) {
            {dc.retrieveObjectList(vsphereMock, "hostFolder", null, regPSpecs);
                result = om.readJsonFile("src/test/resources/DataCenters/regions.json", RetrieveResult.class);
            }
        };

        Region region = dc.getRegion("datacenter-21");
        assertNotNull(region);
        assertEquals(region.getName(), "WTC");
        assertEquals(region.getProviderRegionId(), "datacenter-21");
    }

    @Test
    public void getFakeRegionTest() throws CloudException, InternalException{
        final DataCenters dc = new DataCenters(vsphereMock);
        final List<PropertySpec> regPSpecs = dc.getRegionPropertySpec();

        new NonStrictExpectations(DataCenters.class) {
            {dc.retrieveObjectList(vsphereMock, "hostFolder", null, regPSpecs);
                result = om.readJsonFile("src/test/resources/DataCenters/regions.json", RetrieveResult.class);
            }
        };

        Region region = dc.getRegion("myFakeRegion");
        assertTrue("Region returned but id was made up", region == null);
    }

    @Test
    public void listShouldNotCallCloudWhenRegionCacheIsValid() throws CloudException, InternalException {
        final DataCenters dc = new DataCenters(vsphereMock);
        Cache<Region> cache = Cache.getInstance(vsphereMock, "regions", Region.class, CacheLevel.CLOUD_ACCOUNT, new TimePeriod<Hour>(10, TimePeriod.HOUR));
        cache.clear(); //make sure cache is empty before we begin

        final List<PropertySpec> regPSpecs = dc.getRegionPropertySpec();

        new Expectations(DataCenters.class) {
            {dc.retrieveObjectList(vsphereMock, "hostFolder", null, regPSpecs);
                result = om.readJsonFile("src/test/resources/DataCenters/regions.json", RetrieveResult.class);
                times=1; //should go to cloud first time only and cache should be used for second call
            }
        };

        dc.listRegions();
        dc.listRegions();
        cache.clear(); //make sure cache is empty when we finish
    }

    @Test
    public void listRegionsShouldReturnEmptyListIfCloudReturnsNothing() throws CloudException, InternalException {
        final DataCenters dc = new DataCenters(vsphereMock);
        final List<PropertySpec> regPSpecs = dc.getRegionPropertySpec();

        new Expectations(DataCenters.class) {
            {dc.retrieveObjectList(vsphereMock, "hostFolder", null, regPSpecs);
                result = new RetrieveResult();
                times=1;
            }
        };

        Iterable<Region> regions = dc.listRegions();
        assertNotNull(regions);
        assertFalse("Cloud returned empty list but region list is not empty", regions.iterator().hasNext());
        Cache<Region> cache = Cache.getInstance(vsphereMock, "regions", Region.class, CacheLevel.CLOUD_ACCOUNT, new TimePeriod<Hour>(10, TimePeriod.HOUR));
        cache.clear(); //make sure cache is empty when we finish
    }

    @Test(expected = NoContextException.class)
    public void nullContextShouldThrowException() throws CloudException, InternalException {
        DataCenters dc = new DataCenters(vsphereMock);

        new Expectations(DataCenters.class) {
            { vsphereMock.getContext(); result = null; }
        };

        dc.listRegions();
    }

    @Test(expected = InternalException.class)
    public void invalidListRegionsRequestShouldThrowException() throws CloudException, InternalException {
        final DataCenters dc = new DataCenters(vsphereMock);
        final List<PropertySpec> props = new ArrayList<PropertySpec>();
        PropertySpec propertySpec = new PropertySpec();
        propertySpec.setAll(Boolean.FALSE);
        propertySpec.setType("Datacenter");
        propertySpec.getPathSet().add("name");
        propertySpec.getPathSet().add("config"); //invalid property on a Datacenter object
        props.add(propertySpec);

        new Expectations(DataCenters.class) {
            {dc.retrieveObjectList(vsphereMock, "hostFolder", null, props);
                result = new InternalException("Invalid Property config for Datacenter", new InvalidPropertyFaultMsg("Invalid Property config for Datacenter", new InvalidProperty()));
            };
        };

        dc.retrieveObjectList(vsphereMock, "hostFolder", null, props);
    }

    @Test(expected = NullPointerException.class)
    public void nullPropertySpecInRequestShouldThrowException() throws CloudException, InternalException {
        final DataCenters dc = new DataCenters(vsphereMock);
        dc.retrieveObjectList(vsphereMock, "hostFolder", new ArrayList<SelectionSpec>(), null);
    }

    @Test(expected = CloudException.class)
    public void emptyPropertySpecInRequestShouldThrowException() throws CloudException, InternalException {
        final DataCenters dc = new DataCenters(vsphereMock);
        final List<PropertySpec> props = new ArrayList<PropertySpec>();

        new Expectations(DataCenters.class) {
            {dc.retrieveObjectList(vsphereMock, "hostFolder", null, props);
                result = new CloudException();
            };
        };

        dc.retrieveObjectList(vsphereMock, "hostFolder", null, props);
    }

    @Test
    public void listDataCentersTest() throws CloudException, InternalException{
        final DataCenters dc = new DataCenters(vsphereMock);
        Cache<DataCenter> dCache = Cache.getInstance(vsphereMock, "dataCenters", DataCenter.class, CacheLevel.REGION_ACCOUNT, new TimePeriod<Day>(1, TimePeriod.DAY));
        dCache.clear(); //make sure cache is empty before we begin

        final List<PropertySpec> regPSpecs = dc.getRegionPropertySpec();
        final List<PropertySpec> dcPSpecs = dc.getDataCenterPropertySpec();

        new Expectations(DataCenters.class) {
            {dc.retrieveObjectList(vsphereMock, "hostFolder", null, regPSpecs);
                result = om.readJsonFile("src/test/resources/DataCenters/regions.json", RetrieveResult.class);
                minTimes=0;
            }
            {dc.retrieveObjectList(vsphereMock, "hostFolder", null, dcPSpecs);
                result = om.readJsonFile("src/test/resources/DataCenters/datacenters.json", RetrieveResult.class);
                times=1;
            }
        };

        Iterable<DataCenter> dcs = dc.listDataCenters("datacenter-21");
        assertNotNull(dcs);
        assertTrue(dcs.iterator().hasNext());
        DataCenter dataCenter = dcs.iterator().next();
        assertEquals(dataCenter.getName(), "WTC-Dev-1");
        assertEquals(dataCenter.getProviderDataCenterId(), "domain-c26");
        assertEquals(dataCenter.isActive(), true);
        assertEquals(dataCenter.isAvailable(), true);

        int count = 0;
        for (DataCenter center : dcs) {
            count++;
        }
        assertEquals("Number of datacenters returned is incorrect", 2, count);
    }

    @Test
    public void getDataCenterTest() throws CloudException, InternalException{
        final DataCenters dc = new DataCenters(vsphereMock);
        final List<PropertySpec> regPSpecs = dc.getRegionPropertySpec();
        final List<PropertySpec> dcPSpecs = dc.getDataCenterPropertySpec();

        new NonStrictExpectations(DataCenters.class) {
            {dc.retrieveObjectList(vsphereMock, "hostFolder", null, regPSpecs);
                result = om.readJsonFile("src/test/resources/DataCenters/regions.json", RetrieveResult.class);
            }
            {dc.retrieveObjectList(vsphereMock, "hostFolder", null, dcPSpecs);
                result = om.readJsonFile("src/test/resources/DataCenters/datacenters.json", RetrieveResult.class);
            }
        };

        DataCenter dataCenter = dc.getDataCenter("domain-c26");
        assertNotNull(dataCenter);
        assertEquals(dataCenter.getName(), "WTC-Dev-1");
        assertEquals(dataCenter.getProviderDataCenterId(), "domain-c26");
    }

    @Test
    public void getFakeDataCenterTest() throws CloudException, InternalException{
        final DataCenters dc = new DataCenters(vsphereMock);
        final List<PropertySpec> regPSpecs = dc.getRegionPropertySpec();
        final List<PropertySpec> dcPSpecs = dc.getDataCenterPropertySpec();

        new NonStrictExpectations(DataCenters.class) {
            {dc.retrieveObjectList(vsphereMock, "hostFolder", null, regPSpecs);
                result = om.readJsonFile("src/test/resources/DataCenters/regions.json", RetrieveResult.class);
            }
            {dc.retrieveObjectList(vsphereMock, "hostFolder", null, dcPSpecs);
                result = om.readJsonFile("src/test/resources/DataCenters/datacenters.json", RetrieveResult.class);
            }
        };

        DataCenter dataCenter = dc.getDataCenter("myFakeDC");
        assertTrue("DataCenter returned but id was made up", dataCenter == null);
    }

    @Test
    public void listShouldNotCallCloudWhenDataCenterCacheIsValid() throws CloudException, InternalException {
        final DataCenters dc = new DataCenters(vsphereMock);
        Cache<DataCenter> cache = Cache.getInstance(vsphereMock, "dataCenters", DataCenter.class, CacheLevel.REGION_ACCOUNT, new TimePeriod<Day>(1, TimePeriod.DAY));
        cache.clear(); //make sure cache is empty before we begin

        final List<PropertySpec> regPSpecs = dc.getRegionPropertySpec();
        final List<PropertySpec> dcPSpecs = dc.getDataCenterPropertySpec();

        new Expectations(DataCenters.class) {
            {dc.retrieveObjectList(vsphereMock, "hostFolder", null, regPSpecs);
                result = om.readJsonFile("src/test/resources/DataCenters/regions.json", RetrieveResult.class);
                minTimes=0;
            }
            {dc.retrieveObjectList(vsphereMock, "hostFolder", null, dcPSpecs);
                result = om.readJsonFile("src/test/resources/DataCenters/datacenters.json", RetrieveResult.class);
                times=1; //should go to the cloud the first time only
            }
        };

        dc.listDataCenters("datacenter-21");
        dc.listDataCenters("datacenter-21");
        cache.clear();
    }

    @Test(expected = CloudException.class)
    public void listDatacentersShouldThrowExceptionIfRegionNotValid() throws CloudException, InternalException {
        final DataCenters dc = new DataCenters(vsphereMock);
        dc.listDataCenters("MyFakeRegionId");
    }

    @Test
    public void listDataCentersShouldReturnDummyDCIfCloudReturnsNothing() throws CloudException, InternalException{
        final DataCenters dc = new DataCenters(vsphereMock);
        Cache<DataCenter> cache = Cache.getInstance(vsphereMock, "dataCenters", DataCenter.class, CacheLevel.REGION_ACCOUNT, new TimePeriod<Day>(1, TimePeriod.DAY));
        cache.clear(); //make sure cache is empty before we begin

        final List<PropertySpec> regPSpecs = dc.getRegionPropertySpec();
        final List<PropertySpec> dcPSpecs = dc.getDataCenterPropertySpec();

        new Expectations(DataCenters.class) {
            {dc.retrieveObjectList(vsphereMock, "hostFolder", null, regPSpecs);
                result = om.readJsonFile("src/test/resources/DataCenters/regions.json", RetrieveResult.class);
                minTimes=0;
            }
            {dc.retrieveObjectList(vsphereMock, "hostFolder", null, dcPSpecs);
                result = new RetrieveResult();
            }
        };

        Iterable<DataCenter> dcs = dc.listDataCenters("datacenter-21");
        assertNotNull(dcs);
        assertTrue(dcs.iterator().hasNext());
        DataCenter dataCenter = dcs.iterator().next();
        assertEquals(dataCenter.getName(), "WTC");
        assertEquals(dataCenter.getProviderDataCenterId(), "datacenter-21-a");
        assertEquals(dataCenter.isActive(), true);
        assertEquals(dataCenter.isAvailable(), true);

        int count = 0;
        for (DataCenter center : dcs) {
            count++;
        }
        assertEquals("Number of datacenters returned is incorrect", 1, count);
        cache.clear();
    }

    @Test
    public void listResourcePoolsTest() throws CloudException, InternalException{
        final DataCenters dc = new DataCenters(vsphereMock);
        final List<SelectionSpec> rpSSpecs = dc.getResourcePoolSelectionSpec();
        final List<PropertySpec> rpPSpecs = dc.getResourcePoolPropertySpec();

        new NonStrictExpectations(DataCenters.class) {
            {dc.retrieveObjectList(vsphereMock, "hostFolder", rpSSpecs, rpPSpecs);
                result = om.readJsonFile("src/test/resources/DataCenters/resourcePools.json", RetrieveResult.class);
            }
        };

        Iterable<ResourcePool> resourcePools = dc.listResourcePools("domain-c26");
        assertNotNull(resourcePools);
        assertTrue(resourcePools.iterator().hasNext());
        ResourcePool resourcePool = resourcePools.iterator().next();
        assertEquals(resourcePool.getName(), "Cluster1-Resource_Pool1");
        assertEquals(resourcePool.getProvideResourcePoolId(), "resgroup-76");
        assertEquals(resourcePool.getDataCenterId(), "domain-c26");
        assertEquals(resourcePool.isAvailable(), true);

        int count = 0;
        for (ResourcePool r : resourcePools) {
            count++;
        }
        assertEquals("Number of resource pools returned is incorrect", 2, count);
    }

    @Test
    public void getResourcePoolTest() throws CloudException, InternalException{
        final DataCenters dc = new DataCenters(vsphereMock);
        final List<SelectionSpec> rpSSpecs = dc.getResourcePoolSelectionSpec();
        final List<PropertySpec> rpPSpecs = dc.getResourcePoolPropertySpec();

        new NonStrictExpectations(DataCenters.class) {
            {dc.retrieveObjectList(vsphereMock, "hostFolder", rpSSpecs, rpPSpecs);
                result = om.readJsonFile("src/test/resources/DataCenters/resourcePools.json", RetrieveResult.class);
            }
        };

        ResourcePool resourcePool = dc.getResourcePool("resgroup-76");
        assertNotNull(resourcePool);
        assertEquals(resourcePool.getName(), "Cluster1-Resource_Pool1");
        assertEquals(resourcePool.getProvideResourcePoolId(),"resgroup-76");
        assertEquals(resourcePool.getDataCenterId(), "domain-c26");
        assertEquals(resourcePool.isAvailable(), true);
    }

    @Test
    public void getFakeResourcePoolTest() throws CloudException, InternalException{
        final DataCenters dc = new DataCenters(vsphereMock);
        final List<SelectionSpec> rpSSpecs = dc.getResourcePoolSelectionSpec();
        final List<PropertySpec> rpPSpecs = dc.getResourcePoolPropertySpec();

        new NonStrictExpectations(DataCenters.class) {
            {dc.retrieveObjectList(vsphereMock, "hostFolder", rpSSpecs, rpPSpecs);
                result = om.readJsonFile("src/test/resources/DataCenters/resourcePools.json", RetrieveResult.class);
            }
        };

        ResourcePool resourcePool = dc.getResourcePool("myFakeRP");
        assertTrue("ResourcePool returned but id was made up", resourcePool == null);
    }

    @Test
    public void listStoragePoolsTest() throws CloudException, InternalException{
        final DataCenters dc = new DataCenters(vsphereMock);
        final List<SelectionSpec> spSSpecs = dc.getStoragePoolSelectionSpec();
        final List<PropertySpec> spPSpecs = dc.getStoragePoolPropertySpec();

        Cache<StoragePool> cache = Cache.getInstance(vsphereMock, "storagePools", StoragePool.class, CacheLevel.CLOUD_ACCOUNT, new TimePeriod<Hour>(10, TimePeriod.HOUR));
        cache.clear();

        new NonStrictExpectations(DataCenters.class) {
            { dc.retrieveObjectList(vsphereMock, anyString, spSSpecs, spPSpecs);
                result = om.readJsonFile("src/test/resources/DataCenters/storagePools.json", RetrieveResult.class);
            }
            { vsphereMock.getComputeServices(); result = vsphereComputeMock; }
            { vsphereComputeMock.getAffinityGroupSupport(); result = vsphereAGMock; }
            { vsphereAGMock.list((AffinityGroupFilterOptions) any);
                result = om.readJsonFile("src/test/resources/DataCenters/daseinHosts.json", AffinityGroup[].class);
            }
        };

        Iterable<StoragePool> sps = dc.listStoragePools();
        assertNotNull(sps);
        assertTrue(sps.iterator().hasNext());
        StoragePool storagePool = sps.iterator().next();
        assertEquals(storagePool.getStoragePoolName(), "shared-datastore-1");
        assertEquals(storagePool.getStoragePoolId(), "datastore-37");
        assertEquals(storagePool.getRegionId(), "datacenter-21");
        assertNull("Storage pool is shared, datacenter id should be null", storagePool.getDataCenterId());
        assertNull("Storage pool is shared, affinity group id should be null", storagePool.getAffinityGroupId());
        assertEquals(storagePool.getCapacity().longValue(), 3902537, 1);
        assertEquals(storagePool.getProvisioned().longValue(), 1885831, 1);
        assertEquals(storagePool.getFreeSpace().longValue(), 2016706, 1);

        int count = 0;
        for (StoragePool sp : sps) {
            count++;
        }
        assertEquals("Number of storage pools returned is incorrect", 6, count);
    }

    @Test
    public void getStoragePoolTest() throws CloudException, InternalException{
        final DataCenters dc = new DataCenters(vsphereMock);
        final List<SelectionSpec> spSSpecs = dc.getStoragePoolSelectionSpec();
        final List<PropertySpec> spPSpecs = dc.getStoragePoolPropertySpec();

        new NonStrictExpectations(DataCenters.class) {
            { dc.retrieveObjectList(vsphereMock, anyString, spSSpecs, spPSpecs);
                result = om.readJsonFile("src/test/resources/DataCenters/storagePools.json", RetrieveResult.class);
            }
            { vsphereMock.getComputeServices(); result = vsphereComputeMock; }
            { vsphereComputeMock.getAffinityGroupSupport(); result = vsphereAGMock; }
            { vsphereAGMock.list((AffinityGroupFilterOptions) any);
                result = om.readJsonFile("src/test/resources/DataCenters/daseinHosts.json", AffinityGroup[].class);
            }
        };

        StoragePool storagePool = dc.getStoragePool("datastore-44");
        assertEquals(storagePool.getStoragePoolName(), "local-storage-1 (1)");
        assertEquals(storagePool.getStoragePoolId(), "datastore-44");
        assertEquals(storagePool.getRegionId(), "datacenter-21");
        assertEquals(storagePool.getDataCenterId(), "domain-c26");
        assertEquals(storagePool.getAffinityGroupId(), "host-43");
        assertEquals(storagePool.getCapacity().longValue(), 285212, 1);
        assertEquals(storagePool.getProvisioned().longValue(), 996, 1);
        assertEquals(storagePool.getFreeSpace().longValue(), 284216, 1);
    }

    @Test
    public void getFakeStoragePoolTest() throws CloudException, InternalException{
        final DataCenters dc = new DataCenters(vsphereMock);
        final List<SelectionSpec> spSSpecs = dc.getStoragePoolSelectionSpec();
        final List<PropertySpec> spPSpecs = dc.getStoragePoolPropertySpec();

        new NonStrictExpectations(DataCenters.class) {
            { dc.retrieveObjectList(vsphereMock, anyString, spSSpecs, spPSpecs);
                result = om.readJsonFile("src/test/resources/DataCenters/storagePools.json", RetrieveResult.class);
            }
            { vsphereMock.getComputeServices(); result = vsphereComputeMock; }
            { vsphereComputeMock.getAffinityGroupSupport(); result = vsphereAGMock; }
            { vsphereAGMock.list((AffinityGroupFilterOptions) any);
                result = om.readJsonFile("src/test/resources/DataCenters/daseinHosts.json", AffinityGroup[].class);
            }
        };

        StoragePool pool = dc.getStoragePool("myFakeSP");
        assertTrue("StoragePool returned but id was made up", pool == null);
    }

    @Test
    public void listShouldNotCallCloudWhenStoragePoolCacheIsValid() throws CloudException, InternalException {
        final DataCenters dc = new DataCenters(vsphereMock);
        final List<SelectionSpec> spSSpecs = dc.getStoragePoolSelectionSpec();
        final List<PropertySpec> spPSpecs = dc.getStoragePoolPropertySpec();

        Cache<StoragePool> cache = Cache.getInstance(vsphereMock, "storagePools", StoragePool.class, CacheLevel.CLOUD_ACCOUNT, new TimePeriod<Hour>(10, TimePeriod.HOUR));
        cache.clear();

        new Expectations(DataCenters.class) {
            { dc.retrieveObjectList(vsphereMock, anyString, spSSpecs, spPSpecs);
                result = om.readJsonFile("src/test/resources/DataCenters/storagePools.json", RetrieveResult.class);
                times=1;
            }
            { vsphereMock.getComputeServices(); result = vsphereComputeMock; minTimes=0;}
            { vsphereComputeMock.getAffinityGroupSupport(); result = vsphereAGMock; minTimes=0;}
            { vsphereAGMock.list((AffinityGroupFilterOptions) any);
                result = om.readJsonFile("src/test/resources/DataCenters/daseinHosts.json", AffinityGroup[].class);
                minTimes=0;
            }
        };

        dc.listStoragePools();
        dc.listStoragePools();
        cache.clear();
    }

    @Test
    public void listVmFoldersTest() throws CloudException, InternalException{
        final DataCenters dc = new DataCenters(vsphereMock);
        final List<PropertySpec> vfPSpecs = dc.getVmFolderPropertySpec();
        Cache<Folder> cache = Cache.getInstance(vsphereMock, "folders", Folder.class, CacheLevel.CLOUD_ACCOUNT, new TimePeriod<Hour>(10, TimePeriod.HOUR));
        cache.clear();

        new NonStrictExpectations(DataCenters.class) {
            { dc.retrieveObjectList(vsphereMock, anyString, null, vfPSpecs);
                result = om.readJsonFile("src/test/resources/DataCenters/vmFolders.json", RetrieveResult.class);
            }
        };

        Iterable<Folder> folders = dc.listVMFolders();
        assertNotNull(folders);
        assertTrue(folders.iterator().hasNext());
        Folder folder = folders.iterator().next();
        assertEquals(folder.getName(), "Folder1");
        assertEquals(folder.getId(), "group-d80");
        assertEquals(folder.getType(), FolderType.VM);
        assertNull("Parent folder should be null", folder.getParent());
        assertNotNull("Children should not be null, return empty list instead", folder.getChildren());

        int count = 0;
        for (Folder folder1 : folders) {
            count++;
        }
        assertEquals("Number of folders returned is incorrect", 7, count);
    }

    @Test
    public void getVmFolderTest() throws CloudException, InternalException{
        final DataCenters dc = new DataCenters(vsphereMock);
        final List<PropertySpec> vfPSpecs = dc.getVmFolderPropertySpec();

        new NonStrictExpectations(DataCenters.class) {
            { dc.retrieveObjectList(vsphereMock, anyString, null, vfPSpecs);
                result = om.readJsonFile("src/test/resources/DataCenters/vmFolders.json", RetrieveResult.class);
            }
        };

        Folder folder = dc.getVMFolder("group-v81");
        assertEquals(folder.getName(), "VM Folder1");
        assertEquals(folder.getId(), "group-v81");
        assertEquals(folder.getType(), FolderType.VM);
        assertEquals(folder.getParent().getName(), "vm");
        assertEquals(folder.getChildren().size(), 1);
        assertEquals(folder.getChildren().get(0).getName(), "DMNestedFolder");
    }

    @Test
    public void getFakeVmFolderTest() throws CloudException, InternalException{
        final DataCenters dc = new DataCenters(vsphereMock);
        final List<PropertySpec> vfPSpecs = dc.getVmFolderPropertySpec();

        new NonStrictExpectations(DataCenters.class) {
            { dc.retrieveObjectList(vsphereMock, anyString, null, vfPSpecs);
                result = om.readJsonFile("src/test/resources/DataCenters/vmFolders.json", RetrieveResult.class);
            }
        };

        Folder folder = dc.getVMFolder("myFakeFolder");
        assertTrue("Folder returned but id was made up", folder == null);
    }

    @Test
    public void listShouldNotCallCloudWhenVmFolderCacheIsValid() throws CloudException, InternalException {
        final DataCenters dc = new DataCenters(vsphereMock);
        final List<PropertySpec> vfPSpecs = dc.getVmFolderPropertySpec();

        Cache<Folder> cache = Cache.getInstance(vsphereMock, "folders", Folder.class, CacheLevel.CLOUD_ACCOUNT, new TimePeriod<Hour>(10, TimePeriod.HOUR));
        cache.clear();

        new NonStrictExpectations(DataCenters.class) {
            {
                dc.retrieveObjectList(vsphereMock, anyString, null, vfPSpecs);
                result = om.readJsonFile("src/test/resources/DataCenters/vmFolders.json", RetrieveResult.class);
                times=1;
            }
        };

        dc.listVMFolders();
        dc.listVMFolders();
        cache.clear();
    }
}
