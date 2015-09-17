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
import org.dasein.cloud.vsphere.compute.*;
import org.dasein.util.uom.storage.Megabyte;
import org.dasein.util.uom.storage.Storage;
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

    public <T> T readJsonFile(String filename, Class<T> valueType) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.enableDefaultTypingAsProperty(ObjectMapper.DefaultTyping.OBJECT_AND_NON_CONCRETE, "type");
        try {
            return (T) mapper.readValue(new File(filename), valueType);
        } catch ( Exception e ) {
            throw new RuntimeException("Unable to read file " + filename, e);
        }
    }

    @Test
    public void listRegionsTest() {
        DataCenters dc = new DataCenters(vsphereMock);

        new MockUp<DataCenters>() {
            @Mock
            public RetrieveResult retrieveObjectList(Vsphere provider, @Nonnull String baseFolder, @Nullable List<SelectionSpec> selectionSpecsArr, @Nonnull List<PropertySpec> pSpecs) {
                return readJsonFile("src/test/resources/DataCenters/regions.json", RetrieveResult.class);
            }
        };

        try {
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
        } catch (CloudException e){
            e.printStackTrace();
        } catch (InternalException e){
            e.printStackTrace();
        }
    }

    @Test
    public void getRegionTest() {
        DataCenters dc = new DataCenters(vsphereMock);

        new MockUp<DataCenters>() {
            @Mock
            public RetrieveResult retrieveObjectList(Vsphere provider, @Nonnull String baseFolder, @Nullable List<SelectionSpec> selectionSpecsArr, @Nonnull List<PropertySpec> pSpecs) {
                return readJsonFile("src/test/resources/DataCenters/regions.json", RetrieveResult.class);
            }
        };

        try {
            Region region = dc.getRegion("datacenter-21");
            assertNotNull(region);
            assertEquals(region.getName(), "WTC");
            assertEquals(region.getProviderRegionId(), "datacenter-21");
        } catch (CloudException e){
            e.printStackTrace();
        } catch (InternalException e){
            e.printStackTrace();
        }
    }

    @Test
    public void getFakeRegionTest() {
        DataCenters dc = new DataCenters(vsphereMock);

        new MockUp<DataCenters>() {
            @Mock
            public RetrieveResult retrieveObjectList(Vsphere provider, @Nonnull String baseFolder, @Nullable List<SelectionSpec> selectionSpecsArr, @Nonnull List<PropertySpec> pSpecs) {
                return readJsonFile("src/test/resources/DataCenters/regions.json", RetrieveResult.class);
            }
        };

        try{
            Region region = dc.getRegion("myFakeRegion");
            assertTrue("Region returned but id was made up", region == null);
        } catch (CloudException e){
            e.printStackTrace();
        } catch (InternalException e){
            e.printStackTrace();
        }
    }

    @Test
    public void listDataCentersTest() {
        DataCenters dc = new DataCenters(vsphereMock);

        new MockUp<DataCenters>() {
            @Mock
            public RetrieveResult retrieveObjectList(Invocation inv, Vsphere provider, @Nonnull String baseFolder, @Nullable List<SelectionSpec> selectionSpecsArr, @Nonnull List<PropertySpec> pSpecs) {
                return readJsonFile("src/test/resources/DataCenters/datacenters.json", RetrieveResult.class);
            }
        };

        try{

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
        } catch (CloudException e){
            e.printStackTrace();
        } catch (InternalException e){
            e.printStackTrace();
        }
    }

    @Test
    public void getDataCenterTest() {
        DataCenters dc = new DataCenters(vsphereMock);

        new MockUp<DataCenters>() {
            @Mock
            public RetrieveResult retrieveObjectList(Invocation inv, Vsphere provider, @Nonnull String baseFolder, @Nullable List<SelectionSpec> selectionSpecsArr, @Nonnull List<PropertySpec> pSpecs) {
               return readJsonFile("src/test/resources/DataCenters/datacenters.json", RetrieveResult.class);
            }
        };

        try{
            DataCenter dataCenter = dc.getDataCenter("domain-c26");
            assertNotNull(dataCenter);
            assertEquals(dataCenter.getName(), "WTC-Dev-1");
            assertEquals(dataCenter.getProviderDataCenterId(), "domain-c26");
        } catch (CloudException e){
            e.printStackTrace();
        } catch (InternalException e){
            e.printStackTrace();
        }
    }

    @Test
    public void getFakeDataCenterTest() {
        DataCenters dc = new DataCenters(vsphereMock);

        new MockUp<DataCenters>() {
            @Mock
            public RetrieveResult retrieveObjectList(Invocation inv, Vsphere provider, @Nonnull String baseFolder, @Nullable List<SelectionSpec> selectionSpecsArr, @Nonnull List<PropertySpec> pSpecs) {
                if (inv.getInvocationCount() <= 2) {
                    return readJsonFile("src/test/resources/DataCenters/regions.json", RetrieveResult.class);
                }
                else {
                    return readJsonFile("src/test/resources/DataCenters/datacenters.json", RetrieveResult.class);
                }
            }
        };

        try{
            DataCenter dataCenter = dc.getDataCenter("myFakeDC");
            assertTrue("DataCenter returned but id was made up", dataCenter == null);
        } catch (CloudException e){
            e.printStackTrace();
        } catch (InternalException e){
            e.printStackTrace();
        }
    }

    @Test
    public void listResourcePoolsTest() {
        DataCenters dc = new DataCenters(vsphereMock);

        new MockUp<DataCenters>() {
            @Mock
            public RetrieveResult retrieveObjectList(Invocation inv, Vsphere provider, @Nonnull String baseFolder, @Nullable List<SelectionSpec> selectionSpecsArr, @Nonnull List<PropertySpec> pSpecs) {
                return readJsonFile("src/test/resources/DataCenters/resourcePools.json", RetrieveResult.class);
            }
        };

        try{

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
        } catch (CloudException e){
            e.printStackTrace();
        } catch (InternalException e){
            e.printStackTrace();
        }
    }

    @Test
    public void getResourcePoolTest() {
        DataCenters dc = new DataCenters(vsphereMock);

        new MockUp<DataCenters>() {
            @Mock
            public RetrieveResult retrieveObjectList(Invocation inv, Vsphere provider, @Nonnull String baseFolder, @Nullable List<SelectionSpec> selectionSpecsArr, @Nonnull List<PropertySpec> pSpecs) {
                return readJsonFile("src/test/resources/DataCenters/resourcePools.json", RetrieveResult.class);
            }
        };

        try{
            ResourcePool resourcePool = dc.getResourcePool("resgroup-76");
            assertNotNull(resourcePool);
            assertEquals(resourcePool.getName(), "Cluster1-Resource_Pool1");
            assertEquals(resourcePool.getProvideResourcePoolId(),"resgroup-76");
            assertEquals(resourcePool.getDataCenterId(), "domain-c26");
            assertEquals(resourcePool.isAvailable(), true);
        } catch (CloudException e){
            e.printStackTrace();
        } catch (InternalException e){
            e.printStackTrace();
        }
    }

    @Test
    public void getFakeResourcePoolTest() {
        DataCenters dc = new DataCenters(vsphereMock);

        new MockUp<DataCenters>() {
            @Mock
            public RetrieveResult retrieveObjectList(Invocation inv, Vsphere provider, @Nonnull String baseFolder, @Nullable List<SelectionSpec> selectionSpecsArr, @Nonnull List<PropertySpec> pSpecs) {
                return readJsonFile("src/test/resources/DataCenters/resourcePools.json", RetrieveResult.class);
            }
        };

        try{
            ResourcePool resourcePool = dc.getResourcePool("myFakeRP");
            assertTrue("ResourcePool returned but id was made up", resourcePool == null);
        } catch (CloudException e){
            e.printStackTrace();
        } catch (InternalException e){
            e.printStackTrace();
        }
    }

    @Test
    public void listStoragePoolsTest() {
        try{
            final DataCenters dc = new DataCenters(vsphereMock);

            new NonStrictExpectations() {
                { vsphereMock.getComputeServices(); result = vsphereComputeMock; }
                { vsphereComputeMock.getAffinityGroupSupport(); result = vsphereAGMock; }
            };

            new Expectations() {
                {
                    vsphereAGMock.list(AffinityGroupFilterOptions.getInstance());
                        returns (readJsonFile("src/test/resources/Datacenters/daseinHosts.json", ArrayList.class));

                    //dc.retrieveObjectList(vsphereMock, "hostFolder", null, null);
                    //    returns (readJsonFile("src/test/resources/DataCenters/storagePools.json", RetrieveResult.class));
                }
            };

            new MockUp<DataCenters>() {
                @Mock
                public RetrieveResult retrieveObjectList(Invocation inv, Vsphere provider, @Nonnull String baseFolder, @Nullable List<SelectionSpec> selectionSpecsArr, @Nonnull List<PropertySpec> pSpecs) {
                    return readJsonFile("src/test/resources/DataCenters/storagePools.json", RetrieveResult.class);
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
        } catch (CloudException e){
            e.printStackTrace();
        } catch (InternalException e){
            e.printStackTrace();
        }
    }

    @Test
    public void getStoragePoolTest() {
        DataCenters dc = new DataCenters(vsphereMock);

        new MockUp<DataCenters>() {
            @Mock
            public RetrieveResult retrieveObjectList(Invocation inv, Vsphere provider, @Nonnull String baseFolder, @Nullable List<SelectionSpec> selectionSpecsArr, @Nonnull List<PropertySpec> pSpecs) {
                return readJsonFile("src/test/resources/DataCenters/storagePools.json", RetrieveResult.class);

            }
        };

        new MockUp<HostSupport>() {
            @Mock
            public RetrieveResult retrieveObjectList(Invocation inv, Vsphere provider, @Nonnull String baseFolder, @Nullable List<SelectionSpec> selectionSpecsArr, @Nonnull List<PropertySpec> pSpecs) {
                return readJsonFile("src/test/resources/HostSupport/hosts.json", RetrieveResult.class);

            }
        };

        try{
            StoragePool storagePool = dc.getStoragePool("datastore-44");
            assertEquals(storagePool.getStoragePoolName(), "local-storage-1 (1)");
            assertEquals(storagePool.getStoragePoolId(), "datastore-44");
            assertEquals(storagePool.getRegionId(), "datacenter-21");
            assertEquals(storagePool.getDataCenterId(), "domain-c26");
            assertEquals(storagePool.getAffinityGroupId(), "host-43");
            assertEquals(storagePool.getCapacity().longValue(), 285212, 1);
            assertEquals(storagePool.getProvisioned().longValue(), 996, 1);
            assertEquals(storagePool.getFreeSpace().longValue(), 284216, 1);
        } catch (CloudException e){
            e.printStackTrace();
        } catch (InternalException e){
            e.printStackTrace();
        }
    }

    @Test
    public void getFakeStoragePoolTest() {
        DataCenters dc = new DataCenters(vsphereMock);

        new MockUp<DataCenters>() {
            @Mock
            public RetrieveResult retrieveObjectList(Invocation inv, Vsphere provider, @Nonnull String baseFolder, @Nullable List<SelectionSpec> selectionSpecsArr, @Nonnull List<PropertySpec> pSpecs) {
                if (inv.getInvocationCount() == 1) {
                    return readJsonFile("src/test/resources/DataCenters/storagePools.json", RetrieveResult.class);
                }
                else {
                    return readJsonFile("src/test/resources/HostSupport/hosts.json", RetrieveResult.class);
                }
            }
        };

        try{
            StoragePool pool = dc.getStoragePool("myFakeSP");
            assertTrue("StoragePool returned but id was made up", pool == null);
        } catch (CloudException e){
            e.printStackTrace();
        } catch (InternalException e){
            e.printStackTrace();
        }
    }

    @Test
    public void listVmFoldersTest() {
        DataCenters dc = new DataCenters(vsphereMock);

        new MockUp<DataCenters>() {
            @Mock
            public RetrieveResult retrieveObjectList(Invocation inv, Vsphere provider, @Nonnull String baseFolder, @Nullable List<SelectionSpec> selectionSpecsArr, @Nonnull List<PropertySpec> pSpecs) {
                return readJsonFile("src/test/resources/DataCenters/vmFolders.json", RetrieveResult.class);
            }
        };

        try{

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
        } catch (CloudException e){
            e.printStackTrace();
        } catch (InternalException e){
            e.printStackTrace();
        }
    }

    @Test
    public void getVmFolderTest() {
        DataCenters dc = new DataCenters(vsphereMock);

        new MockUp<DataCenters>() {
            @Mock
            public RetrieveResult retrieveObjectList(Invocation inv, Vsphere provider, @Nonnull String baseFolder, @Nullable List<SelectionSpec> selectionSpecsArr, @Nonnull List<PropertySpec> pSpecs) {
                return readJsonFile("src/test/resources/DataCenters/vmFolders.json", RetrieveResult.class);
            }
        };

        try {
            Folder folder = dc.getVMFolder("group-v81");
            assertEquals(folder.getName(), "VM Folder1");
            assertEquals(folder.getId(), "group-v81");
            assertEquals(folder.getType(), FolderType.VM);
            assertEquals(folder.getParent().getName(), "vm");
            assertEquals(folder.getChildren().size(), 1);
            assertEquals(folder.getChildren().get(0).getName(), "DMNestedFolder");
        } catch (CloudException e){
            e.printStackTrace();
        } catch (InternalException e){
            e.printStackTrace();
        }
    }

    @Test
    public void getFakeVmFolderTest() {
        DataCenters dc = new DataCenters(vsphereMock);

        new MockUp<DataCenters>() {
            @Mock
            public RetrieveResult retrieveObjectList(Invocation inv, Vsphere provider, @Nonnull String baseFolder, @Nullable List<SelectionSpec> selectionSpecsArr, @Nonnull List<PropertySpec> pSpecs) {
                return readJsonFile("src/test/resources/DataCenters/vmFolders.json", RetrieveResult.class);
            }
        };

        try{
            Folder folder = dc.getVMFolder("myFakeFolder");
            assertTrue("Folder returned but id was made up", folder == null);
        } catch (CloudException e){
            e.printStackTrace();
        } catch (InternalException e){
            e.printStackTrace();
        }
    }
}
