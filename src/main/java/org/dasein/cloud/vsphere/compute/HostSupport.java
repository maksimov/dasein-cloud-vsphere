package org.dasein.cloud.vsphere.compute;

import com.vmware.vim25.*;
import org.apache.log4j.Logger;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.OperationNotSupportedException;
import org.dasein.cloud.ProviderContext;
import org.dasein.cloud.compute.AbstractAffinityGroupSupport;
import org.dasein.cloud.compute.AffinityGroup;
import org.dasein.cloud.compute.AffinityGroupCreateOptions;
import org.dasein.cloud.compute.AffinityGroupFilterOptions;
import org.dasein.cloud.dc.DataCenter;
import org.dasein.cloud.dc.DataCenterServices;
import org.dasein.cloud.util.APITrace;
import org.dasein.cloud.util.Cache;
import org.dasein.cloud.util.CacheLevel;
import org.dasein.cloud.vsphere.*;
import org.dasein.util.uom.time.Day;
import org.dasein.util.uom.time.TimePeriod;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * User: daniellemayne
 * Date: 11/09/2015
 * Time: 15:05
 */
public class HostSupport extends AbstractAffinityGroupSupport<Vsphere> {
    static private final Logger logger = Vsphere.getLogger(HostSupport.class);

    private Vsphere provider;

    protected HostSupport(@Nonnull Vsphere provider) {
        super(provider);
        this.provider = provider;
    }

    @Nonnull
    @Override
    public AffinityGroup create(@Nonnull AffinityGroupCreateOptions options) throws InternalException, CloudException {
        throw new OperationNotSupportedException("Unable to create physical hosts in vSphere");
    }

    @Override
    public void delete(@Nonnull String affinityGroupId) throws InternalException, CloudException {
        throw new OperationNotSupportedException("Unable to delete physical hosts in vSphere");
    }

    @Nonnull
    @Override
    public AffinityGroup get(@Nonnull String affinityGroupId) throws InternalException, CloudException {
        return super.get(affinityGroupId);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Nonnull
    @Override
    public Iterable<AffinityGroup> list(@Nonnull AffinityGroupFilterOptions options) throws InternalException, CloudException {
        APITrace.begin(provider, "Host.list");
        try {
            ProviderContext ctx = provider.getContext();
            if( ctx == null ) {
                throw new NoContextException();
            }
            Cache<AffinityGroup> cache = Cache.getInstance(provider, "affinityGroups", AffinityGroup.class, CacheLevel.REGION_ACCOUNT, new TimePeriod<Day>(1, TimePeriod.DAY));
            Collection<AffinityGroup> hostList = (Collection<AffinityGroup>)cache.get(ctx);

            if( hostList != null ) {
                return hostList;
            }
            List<AffinityGroup> hosts = new ArrayList<AffinityGroup>();

            VsphereInventoryNavigation nav = new VsphereInventoryNavigation();

            TraversalSpec crToH = new TraversalSpec();
            crToH.setSkip(Boolean.FALSE);
            crToH.setType("ComputeResource");
            crToH.setPath("host");
            crToH.setName("crToH");

            List<SelectionSpec> selectionSpecsArr = new ArrayList<>();
            selectionSpecsArr.add(crToH);

            List<PropertySpec> pSpecs = new ArrayList<>();
            // Create Property Spec
            PropertySpec propertySpec = new PropertySpec();
            propertySpec.setAll(Boolean.FALSE);
            propertySpec.getPathSet().add("name");
            propertySpec.getPathSet().add("overallStatus");
            propertySpec.setType("HostSystem");
            pSpecs.add(propertySpec);

            // Create Property Spec
            PropertySpec propertySpec2 = new PropertySpec();
            propertySpec2.setAll(Boolean.FALSE);
            propertySpec2.getPathSet().add("host");
            propertySpec2.setType("ClusterComputeResource");
            pSpecs.add(propertySpec2);

            List<ObjectContent> listobcont = nav.retrieveObjectList(provider, "hostFolder", selectionSpecsArr, pSpecs);

            if (listobcont != null) {
                for (ObjectContent oc : listobcont) {
                    ManagedObjectReference mr = oc.getObj();
                    if (mr.getType().equals("Host")) {
                        String hostName = null, status = null;
                        List<DynamicProperty> dps = oc.getPropSet();
                        if (dps != null) {
                            for (DynamicProperty dp : dps) {
                                if (dp.getName().equals("name") ) {
                                    hostName = (String) dp.getVal();
                                }
                                else if (dp.getName().equals("overallStatus")) {
                                    status = (String) dp.getVal();
                                }
                                AffinityGroup host = toAffinityGroup(mr.getValue(), hostName, status, "tempDC");
                                if ( host != null ) {
                                    hosts.add(host);
                                }
                            }
                        }
                    }
                    else {
                        List<DynamicProperty> dps = oc.getPropSet();
                        if (dps != null) {
                            for (DynamicProperty dp : dps) {
                                //TODO pull out dc and host mappings so we can set the correct dcId for each host
                            }
                        }
                    }
                }
            }
            cache.put(ctx, hosts);
            return hosts;
        }
        finally {
            APITrace.end();
        }
    }

    @Override
    public AffinityGroup modify(@Nonnull String affinityGroupId, @Nonnull AffinityGroupCreateOptions options) throws InternalException, CloudException {
        throw new OperationNotSupportedException("Unable to modify hosts in vSphere");
    }

    private AffinityGroup toAffinityGroup(@Nonnull String hostID, @Nonnull String hostName, @Nullable String status, @Nonnull String dataCenterID) {
        String agID = hostID;
        String agName = hostName;
        String agDesc = "Affinity group for "+agName;
        long created = 0;

        AffinityGroup ag = AffinityGroup.getInstance(agID, agName, agDesc, dataCenterID, created);
        ag.setTag("status", status);
        return ag;
    }
}
