package org.dasein.cloud.vsphere;

import com.vmware.vim25.PropertySpec;
import com.vmware.vim25.RetrieveResult;
import mockit.NonStrictExpectations;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.network.VLAN;
import org.dasein.cloud.network.VLANState;
import org.dasein.cloud.vsphere.compute.HostSupport;
import org.dasein.cloud.vsphere.network.VSphereNetwork;
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

    @Test
    public void listNetworksTest() throws CloudException, InternalException {
        final VSphereNetwork network = new VSphereNetwork(vsphereMock);
        final List<PropertySpec> networkPSpec = network.getNetworkPSpec();

        new NonStrictExpectations(VSphereNetwork.class) {
            {network.retrieveObjectList(vsphereMock, "networkFolder", null, networkPSpec);
                result = om.readJsonFile("src/test/resources/Networks/networks.json", RetrieveResult.class);
            }
        };

        Iterable<VLAN> vlans = network.listVlans();
        assertNotNull(vlans);
        assertTrue(vlans.iterator().hasNext());
        VLAN vlan = vlans.iterator().next();
        assertEquals(vlan.getProviderVlanId(), "network-57");
        assertEquals(vlan.getName(), "1-My Fancy Test Network");
        assertNull("Vlans are not dc specific. DataCenter should be null", vlan.getProviderDataCenterId());
        assertEquals(vlan.getCurrentState(), VLANState.AVAILABLE);
        assertEquals(vlan.getDescription(), "1-My Fancy Test Network (network-57)");
        assertNotNull(vlan.getTags());

        int count = 0;
        for (VLAN v : vlans) {
            count++;
        }
        assertEquals("Number of vlans returned is incorrect", 5, count);
    }

    @Test
    public void getNetworkTest() throws CloudException, InternalException{
        final VSphereNetwork network = new VSphereNetwork(vsphereMock);
        final List<PropertySpec> networkPSpec = network.getNetworkPSpec();

        new NonStrictExpectations(VSphereNetwork.class) {
            {network.retrieveObjectList(vsphereMock, "networkFolder", null, networkPSpec);
                result = om.readJsonFile("src/test/resources/Networks/networks.json", RetrieveResult.class);
            }
        };

        VLAN vlan = network.getVlan("dvportgroup-56");
        assertEquals(vlan.getProviderVlanId(), "dvportgroup-56");
        assertEquals(vlan.getName(), "VM Network");
        assertNull("Vlans are not dc specific. DataCenter should be null", vlan.getProviderDataCenterId());
        assertEquals(vlan.getCurrentState(), VLANState.AVAILABLE);
        assertEquals(vlan.getDescription(), "VM Network (dvportgroup-56)");
        assertNotNull(vlan.getTags());
        assertNotNull(vlan.getTag("switch.uuid"));
        assertEquals("dvs-51", vlan.getTag("switch.uuid"));
    }

    @Test
    public void getFakeNetworkTest() throws CloudException, InternalException{
        final VSphereNetwork network = new VSphereNetwork(vsphereMock);
        final List<PropertySpec> networkPSpec = network.getNetworkPSpec();

        new NonStrictExpectations(VSphereNetwork.class) {
            {network.retrieveObjectList(vsphereMock, "networkFolder", null, networkPSpec);
                result = om.readJsonFile("src/test/resources/Networks/networks.json", RetrieveResult.class);
            }
        };

        VLAN vlan = network.getVlan("MyFakeNetwork");
        assertTrue("Vlan returned but id was made up", vlan == null);
    }
}
