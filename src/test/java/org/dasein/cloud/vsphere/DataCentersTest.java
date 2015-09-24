package org.dasein.cloud.vsphere;

import mockit.*;

import com.vmware.vim25.*;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.compute.AffinityGroup;
import org.dasein.cloud.compute.AffinityGroupFilterOptions;
import org.dasein.cloud.compute.AffinityGroupSupport;
import org.dasein.cloud.dc.*;
import org.dasein.cloud.vsphere.compute.VsphereCompute;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
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
    private final RetrieveResult regions = om.readJsonFile("src/test/resources/DataCenters/regions.json", RetrieveResult.class);
    private final RetrieveResult datacenters = om.readJsonFile("src/test/resources/DataCenters/datacenters.json", RetrieveResult.class);
    private final RetrieveResult resourcePools = om.readJsonFile("src/test/resources/DataCenters/resourcePools.json", RetrieveResult.class);
    private final RetrieveResult storagePools = om.readJsonFile("src/test/resources/DataCenters/storagePools.json", RetrieveResult.class);
    private final AffinityGroup[] daseinHosts = om.readJsonFile("src/test/resources/DataCenters/daseinHosts.json", AffinityGroup[].class);
    private final RetrieveResult vmFolders = om.readJsonFile("src/test/resources/DataCenters/vmFolders.json", RetrieveResult.class);

    private DataCenters dc = null;
    private List<PropertySpec> regPSpecs = null;
    private List<PropertySpec> dcPSpecs = null;
    private List<SelectionSpec> rpSSpecs = null;
    private List<PropertySpec> rpPSpecs = null;
    private List<SelectionSpec> spSSpecs = null;
    private List<PropertySpec> spPSpecs = null;
    private List<PropertySpec> vfPSpecs = null;
    private List<PropertySpec> props = null;

    @Mocked
    VsphereCompute vsphereComputeMock;
    @Mocked
    AffinityGroupSupport vsphereAGMock;

    @Test
    public void listRegionsTest() throws CloudException, InternalException {
        final DataCenters dc = new DataCenters(vsphereMock);
        final List<PropertySpec> regPSpecs = dc.getRegionPropertySpec();

        new NonStrictExpectations(DataCenters.class) {
            {dc.retrieveObjectList(vsphereMock, "hostFolder", null, regPSpecs);
                result = regions;
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
                result = regions;
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
                result = regions;
            }
        };

        Region region = dc.getRegion("myFakeRegion");
        assertTrue("Region returned but id was made up", region == null);
    }

    @Test(expected = InvalidPropertyFaultMsg.class)
    public void invalidListRegionsRequestThrowsException() throws CloudException, InternalException {
        final DataCenters dc = new DataCenters(vsphereMock);
        final List<PropertySpec> props = new ArrayList<PropertySpec>();
        PropertySpec propertySpec = new PropertySpec();
        propertySpec.setAll(Boolean.FALSE);
        propertySpec.setType("DataCenter");
        propertySpec.getPathSet().add("name");
        propertySpec.getPathSet().add("config");
        props.add(propertySpec);

        new Expectations(DataCenters.class) {
            {dc.retrieveObjectList(vsphereMock, "hostFolder", (List) any, props);
                result = new InvalidPropertyFaultMsg("error", new InvalidProperty());
            };
        };

        dc.retrieveObjectList(vsphereMock, "hostFolder", null, props);
    }

    @Test(expected = NullPointerException.class)
    public void nullBaseFolderInRequestThrowsException() throws CloudException, InternalException {
        final DataCenters dc = new DataCenters(vsphereMock);
        dc.retrieveObjectList(vsphereMock, null, null, null);
    }

    @Test
    public void listDataCentersTest() throws CloudException, InternalException{
        final DataCenters dc = new DataCenters(vsphereMock);
        final List<PropertySpec> regPSpecs = dc.getRegionPropertySpec();
        final List<PropertySpec> dcPSpecs = dc.getDataCenterPropertySpec();

        new NonStrictExpectations(DataCenters.class) {
            {dc.retrieveObjectList(vsphereMock, "hostFolder", null, regPSpecs);
                result = regions;
            }
            {dc.retrieveObjectList(vsphereMock, "hostFolder", null, dcPSpecs);
                result = datacenters;
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
                result = regions;
            }
            {dc.retrieveObjectList(vsphereMock, "hostFolder", null, dcPSpecs);
                result = datacenters;
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
                result = regions;
            }
            {dc.retrieveObjectList(vsphereMock, "hostFolder", null, dcPSpecs);
                result = datacenters;
            }
        };

        DataCenter dataCenter = dc.getDataCenter("myFakeDC");
        assertTrue("DataCenter returned but id was made up", dataCenter == null);
    }

    @Test
    public void listResourcePoolsTest() throws CloudException, InternalException{
        final DataCenters dc = new DataCenters(vsphereMock);
        final List<SelectionSpec> rpSSpecs = dc.getResourcePoolSelectionSpec();
        final List<PropertySpec> rpPSpecs = dc.getResourcePoolPropertySpec();

        new NonStrictExpectations(DataCenters.class) {
            {dc.retrieveObjectList(vsphereMock, "hostFolder", rpSSpecs, rpPSpecs);
                result = resourcePools;
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
                result = resourcePools;
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
                result = resourcePools;
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

        new NonStrictExpectations(DataCenters.class) {
            { dc.retrieveObjectList(vsphereMock, anyString, spSSpecs, spPSpecs);
                result = storagePools;
            }
            { vsphereMock.getComputeServices(); result = vsphereComputeMock; }
            { vsphereComputeMock.getAffinityGroupSupport(); result = vsphereAGMock; }
            { vsphereAGMock.list((AffinityGroupFilterOptions) any);
                result = daseinHosts;
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
                result = storagePools;
            }
            { vsphereMock.getComputeServices(); result = vsphereComputeMock; }
            { vsphereComputeMock.getAffinityGroupSupport(); result = vsphereAGMock; }
            { vsphereAGMock.list((AffinityGroupFilterOptions) any);
                result = daseinHosts;
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
                result = storagePools;
            }
            { vsphereMock.getComputeServices(); result = vsphereComputeMock; }
            { vsphereComputeMock.getAffinityGroupSupport(); result = vsphereAGMock; }
            { vsphereAGMock.list((AffinityGroupFilterOptions) any);
                result = daseinHosts;
            }
        };

        StoragePool pool = dc.getStoragePool("myFakeSP");
        assertTrue("StoragePool returned but id was made up", pool == null);
    }

    @Test
    public void listVmFoldersTest() throws CloudException, InternalException{
        final DataCenters dc = new DataCenters(vsphereMock);
        final List<PropertySpec> vfPSpecs = dc.getVmFolderPropertySpec();

        new NonStrictExpectations(DataCenters.class) {
            { dc.retrieveObjectList(vsphereMock, anyString, null, vfPSpecs);
                result = vmFolders;
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
                result = vmFolders;
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
                result = vmFolders;
            }
        };

        Folder folder = dc.getVMFolder("myFakeFolder");
        assertTrue("Folder returned but id was made up", folder == null);
    }
}
