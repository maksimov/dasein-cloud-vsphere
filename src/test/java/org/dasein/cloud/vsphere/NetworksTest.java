package org.dasein.cloud.vsphere;

import com.vmware.vim25.PropertySpec;
import com.vmware.vim25.RetrieveResult;
import mockit.Expectations;
import mockit.NonStrictExpectations;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.network.VLAN;
import org.dasein.cloud.network.VLANState;
import org.dasein.cloud.util.Cache;
import org.dasein.cloud.util.CacheLevel;
import org.dasein.cloud.vsphere.compute.HostSupport;
import org.dasein.cloud.vsphere.network.VSphereNetwork;
import org.dasein.util.uom.time.Day;
import org.dasein.util.uom.time.TimePeriod;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * User: daniellemayne
 * Date: 22/09/2015
 * Time: 12:35
 */
public class NetworksTest extends VsphereTestBase {
    private ObjectManagement om = new ObjectManagement();
    private final RetrieveResult networks = om.readJsonFile("src/test/resources/Networks/networks.json", RetrieveResult.class);
    private VSphereNetwork network = null;
    private List<PropertySpec> networkPSpec = null;

    private Cache<VLAN> cache = null;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        network = new VSphereNetwork(vsphereMock);
        networkPSpec = network.getNetworkPSpec();
        cache = Cache.getInstance(vsphereMock, "networks", VLAN.class, CacheLevel.REGION_ACCOUNT, new TimePeriod<Day>(1, TimePeriod.DAY));
    }

    @Test
    public void listNetworks() throws CloudException, InternalException {
        cache.clear();

        new NonStrictExpectations(VSphereNetwork.class) {
            {network.retrieveObjectList(vsphereMock, "networkFolder", null, networkPSpec);
                result = networks;
            }
        };

        Iterable<VLAN> vlans = network.listVlans();
        assertNotNull(vlans);
        assertTrue(vlans.iterator().hasNext());
        VLAN vlan = vlans.iterator().next();
        assertEquals("network-57", vlan.getProviderVlanId());
        assertEquals("1-My Fancy Test Network", vlan.getName());
        assertNull("Vlans are not dc specific. DataCenter should be null", vlan.getProviderDataCenterId());
        assertEquals(VLANState.AVAILABLE, vlan.getCurrentState());
        assertEquals("1-My Fancy Test Network (network-57)", vlan.getDescription());
        assertNotNull(vlan.getTags());

        int count = 0;
        for (VLAN v : vlans) {
            count++;
        }
        assertEquals("Number of vlans returned is incorrect", 5, count);
    }

    @Test
    public void getNetwork() throws CloudException, InternalException{
        new NonStrictExpectations(VSphereNetwork.class) {
            {network.retrieveObjectList(vsphereMock, "networkFolder", null, networkPSpec);
                result = networks;
            }
        };

        VLAN vlan = network.getVlan("dvportgroup-56");
        assertEquals("dvportgroup-56", vlan.getProviderVlanId());
        assertEquals("VM Network", vlan.getName());
        assertNull("Vlans are not dc specific. DataCenter should be null", vlan.getProviderDataCenterId());
        assertEquals(VLANState.AVAILABLE, vlan.getCurrentState());
        assertEquals("VM Network (dvportgroup-56)", vlan.getDescription());
        assertNotNull(vlan.getTags());
        assertNotNull(vlan.getTag("switch.uuid"));
        assertEquals("dvs-51", vlan.getTag("switch.uuid"));
    }

    @Test
    public void getFakeNetworkShouldReturnNull() throws CloudException, InternalException{
        new NonStrictExpectations(VSphereNetwork.class) {
            {network.retrieveObjectList(vsphereMock, "networkFolder", null, networkPSpec);
                result = networks;
            }
        };

        VLAN vlan = network.getVlan("MyFakeNetwork");
        assertTrue("Vlan returned but id was made up", vlan == null);
    }

    @Test
    public void listVlansShouldNotCallCloudIfVlanCacheIsValid() throws CloudException, InternalException {
        cache.clear(); //make sure cache is empty before we begin

        new Expectations(VSphereNetwork.class) {
            {network.retrieveObjectList(vsphereMock, "networkFolder", null, networkPSpec);
                result = networks;
                times=1;
            }
        };

        network.listVlans();
        network.listVlans();
    }
}
