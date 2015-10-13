package org.dasein.cloud.vsphere;

import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.PropertyChange;
import com.vmware.vim25.PropertySpec;
import com.vmware.vim25.RetrieveResult;
import mockit.Expectations;
import org.codehaus.jackson.annotate.JsonTypeInfo;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.compute.Volume;
import org.dasein.cloud.compute.VolumeState;
import org.dasein.cloud.dc.ResourcePool;
import org.dasein.cloud.dc.StoragePool;
import org.dasein.cloud.vsphere.compute.HardDisk;
import org.dasein.util.uom.time.TimePeriod;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.List;

/**
 * User: daniellemayne
 * Date: 09/10/2015
 * Time: 10:41
 */
public class HardDiskTest extends VsphereTestBase {
    private ObjectManagement om = new ObjectManagement();
    private RetrieveResult hardDisks = om.readJsonFile("src/test/resources/HardDisk/hardDisks.json", RetrieveResult.class);
    private final ResourcePool[] resourcePools = om.readJsonFile("src/test/resources/HardDisk/resourcePools.json", ResourcePool[].class);
    private final RetrieveResult datastores = om.readJsonFile("src/test/resources/HardDisk/datastores.json", RetrieveResult.class);
    private final StoragePool[] storagePools = om.readJsonFile("src/test/resources/HardDisk/storagePools.json", StoragePool[].class);
    private final ManagedObjectReference task = om.readJsonFile("src/test/resources/HardDisk/task.json", ManagedObjectReference.class);
    private final PropertyChange searchResult = om.readJsonFile("src/test/resources/HardDisk/searchResult.json", PropertyChange.class);

    private HardDisk hd = null;
    private VsphereMethod method = null;
    private List<PropertySpec> hardDiskPSpec = null;
    private List<PropertySpec> datastorePSpec = null;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        hd = new HardDisk(vsphereMock);
        method = new VsphereMethod(vsphereMock);
        hardDiskPSpec = hd.getHardDiskPSpec();
        datastorePSpec = hd.getDatastorePropertySpec();
        ObjectManagement om = new ObjectManagement();
        om.mapper.enableDefaultTypingAsProperty(ObjectMapper.DefaultTyping.NON_FINAL, "type");
        om.mapper.configure(SerializationConfig.Feature.FAIL_ON_EMPTY_BEANS, false);
        hardDisks = om.readJsonFile("src/test/resources/HardDisk/hardDisks.json", RetrieveResult.class);
    }

    @Test
    public void listHardDiskTest() throws CloudException, InternalException {
        new Expectations(HardDisk.class) {
            {hd.retrieveObjectList(vsphereMock, "vmFolder", null, hardDiskPSpec);
                result = hardDisks;
            }
            {hd.getAllResourcePoolsIncludingRoot();
                result = resourcePools;
            }
            {hd.retrieveObjectList(vsphereMock, "datastoreFolder", null, datastorePSpec);
                result = datastores;
            }
            {hd.listStoragePools();
                result = storagePools;
            }
            {hd.searchDatastores(vsphereMock, (ManagedObjectReference) any, anyString, null);
                result = task;
            }
        };

        new Expectations(VsphereMethod.class) {
            {method.getOperationComplete((ManagedObjectReference) any, (TimePeriod) any, anyInt);
                result = true;
                times=6;
            }
            {method.getTaskResult();
                result = searchResult;
                times=6;
            }
        };

        Iterable<Volume> volumes = hd.listVolumes();
        assertNotNull("Null object returned for listVolumes", volumes);
        assertTrue("Empty volume list returned", volumes.iterator().hasNext());
        Volume volume = volumes.iterator().next();
        assertNotNull("Null volume found", volume);
        assertEquals("dmTesting7Oct_2.vmdk", volume.getProviderVolumeId());
        assertEquals("Hard disk 1", volume.getName());
        assertEquals("datacenter-21", volume.getProviderRegionId());
        assertEquals("52,428,800 KB", volume.getDescription());
        assertEquals("vm-2318", volume.getProviderVirtualMachineId());
        assertEquals("0", volume.getDeviceId());
        assertEquals(52, volume.getSizeInGigabytes());
        assertTrue(volume.isRootVolume());
        assertNotNull(volume.getTags());
        String filePath = volume.getTag("filePath");
        assertEquals("[shared-datastore-1] dmTesting7Oct/dmTesting7Oct_2.vmdk", filePath);
    }

    @Test
    public void getHardDiskTest() throws CloudException, InternalException {
        new Expectations(HardDisk.class) {
            {hd.retrieveObjectList(vsphereMock, "vmFolder", null, hardDiskPSpec);
                result = hardDisks;
            }
            {hd.getAllResourcePoolsIncludingRoot();
                result = resourcePools;
            }
            {hd.retrieveObjectList(vsphereMock, "datastoreFolder", null, datastorePSpec);
                result = datastores;
            }
            {hd.listStoragePools();
                result = storagePools;
            }
            {hd.searchDatastores(vsphereMock, (ManagedObjectReference) any, anyString, null);
                result = task;
            }
        };

        new Expectations(VsphereMethod.class) {
            {method.getOperationComplete((ManagedObjectReference) any, (TimePeriod) any, anyInt);
                result = true;
                times=6;
            }
            {method.getTaskResult();
                result = searchResult;
                times=6;
            }
        };

        Volume volume = hd.getVolume("dmTesting7Oct_1.vmdk");
        assertNotNull("Valid volume object should not be null", volume);
        assertEquals("dmTesting7Oct_1.vmdk", volume.getProviderVolumeId());
        assertEquals("dmTesting7Oct_1.vmdk", volume.getName());
        assertEquals("domain-c26", volume.getProviderDataCenterId());
        assertEquals("datacenter-21", volume.getProviderRegionId());
        assertEquals("dmTesting7Oct_1.vmdk", volume.getDescription());
        assertNull("Standalone file should not have vm id", volume.getProviderVirtualMachineId());
        assertNull("Standalone file should not have device id", volume.getDeviceId());
        assertEquals(0, volume.getSizeInGigabytes());
        assertNotNull(volume.getTags());
        String filePath = volume.getTag("filePath");
        assertEquals("[shared-datastore-1] dmTesting7Oct/dmTesting7Oct_1.vmdk", filePath);
        assertFalse(volume.isRootVolume());
    }
}
