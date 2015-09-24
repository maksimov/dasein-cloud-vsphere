package org.dasein.cloud.vsphere;

import com.vmware.vim25.PropertySpec;
import com.vmware.vim25.RetrieveResult;
import com.vmware.vim25.SelectionSpec;
import mockit.Expectations;
import mockit.NonStrictExpectations;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.compute.AffinityGroup;
import org.dasein.cloud.compute.AffinityGroupFilterOptions;
import org.dasein.cloud.dc.Region;
import org.dasein.cloud.util.Cache;
import org.dasein.cloud.util.CacheLevel;
import org.dasein.cloud.vsphere.compute.HostSupport;
import org.dasein.util.uom.time.Day;
import org.dasein.util.uom.time.TimePeriod;
import org.junit.Before;
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
    private final RetrieveResult hosts = om.readJsonFile("src/test/resources/HostSupport/hosts.json", RetrieveResult.class);
    private HostSupport hs = null;
    private List<PropertySpec> hostPSpec = null;
    private List<SelectionSpec> hostSSpec = null;

    private Cache<AffinityGroup> cache = null;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        hs = new HostSupport(vsphereMock);
        hostPSpec = hs.getHostPSpec();
        hostSSpec = hs.getHostSSpec();
        cache = Cache.getInstance(vsphereMock, "affinityGroups", AffinityGroup.class, CacheLevel.REGION_ACCOUNT, new TimePeriod<Day>(1, TimePeriod.DAY));
    }

    @Test
    public void listHosts() throws CloudException, InternalException {
        cache.clear();

        new Expectations(HostSupport.class) {
            {hs.retrieveObjectList(vsphereMock, "hostFolder", hostSSpec, hostPSpec);
                result = hosts;
                times=1;
            }
        };

        Iterable<AffinityGroup> hosts = hs.list(AffinityGroupFilterOptions.getInstance());
        assertNotNull(hosts);
        assertTrue(hosts.iterator().hasNext());
        AffinityGroup host = hosts.iterator().next();
        assertEquals("host-43", host.getAffinityGroupId());
        assertEquals("esquin-dev-vrtx-1-bl02.esquin.dev", host.getAffinityGroupName());
        assertEquals("domain-c26", host.getDataCenterId());
        assertEquals(0, host.getCreationTimestamp(), 0);
        assertEquals("Affinity group for esquin-dev-vrtx-1-bl02.esquin.dev", host.getDescription());
        assertNotNull(host.getTags());

        int count = 0;
        for (AffinityGroup ag : hosts) {
            count++;
        }
        assertEquals("Number of hosts returned is incorrect", 4, count);
    }

    @Test
    public void getHost() throws CloudException, InternalException{
        new NonStrictExpectations(HostSupport.class) {
            {hs.retrieveObjectList(vsphereMock, "hostFolder", hostSSpec, hostPSpec);
                result = hosts;
            }
        };

        AffinityGroup host = hs.get("host-72");
        assertEquals("host-72", host.getAffinityGroupId());
        assertEquals("esquin-dev-vrtx-1-bl03.esquin.dev", host.getAffinityGroupName());
        assertEquals("domain-c70", host.getDataCenterId());
        assertEquals(0, host.getCreationTimestamp(), 0);
        assertEquals("Affinity group for esquin-dev-vrtx-1-bl03.esquin.dev", host.getDescription());
        assertNotNull(host.getTags());
    }

    @Test
    public void getFakeHostShouldReturnNull() throws CloudException, InternalException{
        new NonStrictExpectations(HostSupport.class) {
            {hs.retrieveObjectList(vsphereMock, "hostFolder", hostSSpec, hostPSpec);
                result = hosts;
            }
        };

        AffinityGroup ag = hs.get("myFakeHost");
        assertTrue("Host returned but id was made up", ag == null);
    }

    @Test
    public void listShouldNotCallCloudIfHostCacheIsValid() throws CloudException, InternalException {
        cache.clear(); //make sure cache is empty before we begin

        new Expectations(HostSupport.class) {
            {hs.retrieveObjectList(vsphereMock, "hostFolder", hostSSpec, hostPSpec);
                result = hosts;
                times=1;
            }
        };

        hs.list(AffinityGroupFilterOptions.getInstance());
        hs.list(AffinityGroupFilterOptions.getInstance());
    }
}
