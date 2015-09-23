package org.dasein.cloud.vsphere;

import com.vmware.vim25.PropertySpec;
import com.vmware.vim25.RetrieveResult;
import com.vmware.vim25.SelectionSpec;
import mockit.NonStrictExpectations;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.compute.AffinityGroup;
import org.dasein.cloud.compute.AffinityGroupFilterOptions;
import org.dasein.cloud.dc.Region;
import org.dasein.cloud.vsphere.compute.HostSupport;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * User: daniellemayne
 * Date: 18/09/2015
 * Time: 14:52
 */
public class HostSupportTest extends VsphereTestBase{
    private ObjectManagement om = new ObjectManagement();

    @Test
    public void listHostsTest() throws CloudException, InternalException {
        final HostSupport hs = new HostSupport(vsphereMock);
        final List<PropertySpec> hostPSpec = hs.getHostPSpec();
        final List<SelectionSpec> hostSSpec = hs.getHostSSpec();

        new NonStrictExpectations(HostSupport.class) {
            {hs.retrieveObjectList(vsphereMock, "hostFolder", hostSSpec, hostPSpec);
                result = om.readJsonFile("src/test/resources/HostSupport/hosts.json", RetrieveResult.class);
            }
        };

        Iterable<AffinityGroup> hosts = hs.list(AffinityGroupFilterOptions.getInstance());
        assertNotNull(hosts);
        assertTrue(hosts.iterator().hasNext());
        AffinityGroup host = hosts.iterator().next();
        assertEquals(host.getAffinityGroupId(), "host-43");
        assertEquals(host.getAffinityGroupName(), "esquin-dev-vrtx-1-bl02.esquin.dev");
        assertEquals(host.getDataCenterId(), "domain-c26");
        assertEquals(host.getCreationTimestamp(), 0l, 0);
        assertEquals(host.getDescription(), "Affinity group for esquin-dev-vrtx-1-bl02.esquin.dev");
        assertNotNull(host.getTags());

        int count = 0;
        for (AffinityGroup ag : hosts) {
            count++;
        }
        assertEquals("Number of hosts returned is incorrect", 4, count);
    }

    @Test
    public void getHostTest() throws CloudException, InternalException{
        final HostSupport hs = new HostSupport(vsphereMock);
        final List<PropertySpec> hostPSpec = hs.getHostPSpec();
        final List<SelectionSpec> hostSSpec = hs.getHostSSpec();

        new NonStrictExpectations(HostSupport.class) {
            {hs.retrieveObjectList(vsphereMock, "hostFolder", hostSSpec, hostPSpec);
                result = om.readJsonFile("src/test/resources/HostSupport/hosts.json", RetrieveResult.class);
            }
        };

        AffinityGroup host = hs.get("host-72");
        assertEquals(host.getAffinityGroupId(), "host-72");
        assertEquals(host.getAffinityGroupName(), "esquin-dev-vrtx-1-bl03.esquin.dev");
        assertEquals(host.getDataCenterId(), "domain-c70");
        assertEquals(host.getCreationTimestamp(), 0l, 0);
        assertEquals(host.getDescription(), "Affinity group for esquin-dev-vrtx-1-bl03.esquin.dev");
        assertNotNull(host.getTags());
    }

    @Test
    public void getFakeHostTest() throws CloudException, InternalException{
        final HostSupport hs = new HostSupport(vsphereMock);
        final List<PropertySpec> hostPSpec = hs.getHostPSpec();
        final List<SelectionSpec> hostSSpec = hs.getHostSSpec();

        new NonStrictExpectations(HostSupport.class) {
            {hs.retrieveObjectList(vsphereMock, "hostFolder", hostSSpec, hostPSpec);
                result = om.readJsonFile("src/test/resources/HostSupport/hosts.json", RetrieveResult.class);
            }
        };

        AffinityGroup ag = hs.get("myFakeHost");
        assertTrue("Host returned but id was made up", ag == null);
    }
}
