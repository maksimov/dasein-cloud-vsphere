package org.dasein.cloud.vsphere;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

import com.vmware.vim25.*;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonMethod;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.dc.DataCenter;
import org.dasein.cloud.dc.Region;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * User: daniellemayne
 * Date: 14/09/2015
 * Time: 11:08
 */
@RunWith(JUnit4.class)
public class DataCentersTest {

    private RetrieveResult buildRegionList() {
        ObjectMapper mapper = new ObjectMapper().setVisibility(JsonMethod.FIELD, JsonAutoDetect.Visibility.ANY);
        mapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.enableDefaultTypingAsProperty(ObjectMapper.DefaultTyping.OBJECT_AND_NON_CONCRETE, "type");
        RetrieveResult rr = null;
        try {
            rr = mapper.readValue(new File("src/test/resources/DataCenters/regions.json"), RetrieveResult.class);
        } catch ( Exception e ) { }
        return rr;
    }

    private RetrieveResult buildDataCenterList() {
        ObjectMapper mapper = new ObjectMapper().setVisibility(JsonMethod.FIELD, JsonAutoDetect.Visibility.ANY);
        mapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.enableDefaultTypingAsProperty(ObjectMapper.DefaultTyping.OBJECT_AND_NON_CONCRETE, "type");
        RetrieveResult rr = null;
        try {
            rr = mapper.readValue(new File("src/test/resources/DataCenters/datacenters.json"), RetrieveResult.class);
        } catch ( Exception e ) { }
        return rr;
    }

    @Test
    public void listRegionsTest() {
        DataCenters dc = mock(DataCenters.class, CALLS_REAL_METHODS);

        try {
            doReturn(buildRegionList()).when(dc).retrieveObjectList(any(Vsphere.class), anyString(), anyList(), anyList());
            when(dc.listRegions())
                    .thenCallRealMethod();

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
        DataCenters dc = mock(DataCenters.class, CALLS_REAL_METHODS);

        try {
            doReturn(buildRegionList()).when(dc).retrieveObjectList(any(Vsphere.class), anyString(), anyList(), anyList());
            when(dc.getRegion(anyString()))
                    .thenCallRealMethod();

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
        DataCenters dc = mock(DataCenters.class, CALLS_REAL_METHODS);

        try {
            doReturn(buildRegionList()).when(dc).retrieveObjectList(any(Vsphere.class), anyString(), anyList(), anyList());
            when(dc.getRegion(anyString()))
                    .thenCallRealMethod();

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
        DataCenters dc = mock(DataCenters.class, CALLS_REAL_METHODS);

        try {
            doReturn(buildRegionList())
                    .doReturn(buildRegionList())
                    .doReturn(buildDataCenterList())
                    .when(dc).retrieveObjectList(any(Vsphere.class), anyString(), anyList(), anyList());
            try {
                when(dc.listDataCenters(anyString()))
                        .thenCallRealMethod();
            } catch (CloudException e){
                // ignore
            } catch (InternalException e){
                // ignore
            }

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
        DataCenters dc = mock(DataCenters.class, CALLS_REAL_METHODS);

        try {
            doReturn(buildRegionList())
                    .doReturn(buildRegionList())
                    .doReturn(buildDataCenterList())
                    .doReturn(buildRegionList())
                    .doReturn(buildRegionList())
                    .doReturn(buildDataCenterList())
                    .when(dc).retrieveObjectList(any(Vsphere.class), anyString(), anyList(), anyList());
            try {
                when(dc.getDataCenter(anyString()))
                        .thenCallRealMethod();
            } catch (CloudException e){
                //ignore
            } catch (InternalException e){
                //ignore
            }
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
        DataCenters dc = mock(DataCenters.class, CALLS_REAL_METHODS);

        try {
            doReturn(buildRegionList())
                    .doReturn(buildRegionList())
                    .doReturn(buildDataCenterList())
                    .doReturn(buildRegionList())
                    .doReturn(buildRegionList())
                    .doReturn(buildDataCenterList())
                    .when(dc).retrieveObjectList(any(Vsphere.class), anyString(), anyList(), anyList());
            try {
                when(dc.getDataCenter(anyString()))
                        .thenCallRealMethod();
            } catch (CloudException e){
                //ignore
            } catch (InternalException e){
                //ignore
            }
            DataCenter dataCenter = dc.getDataCenter("myFakeDC");
            assertTrue("DataCenter returned but id was made up", dataCenter == null);
        } catch (CloudException e){
            e.printStackTrace();
        } catch (InternalException e){
            e.printStackTrace();
        }
    }
}
