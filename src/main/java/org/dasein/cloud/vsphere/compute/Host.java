/**
 * Copyright (C) 2010-2015 Dell, Inc
 *
 * ====================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ====================================================================
 */

package org.dasein.cloud.vsphere.compute;

import com.vmware.vim25.InvalidProperty;
import com.vmware.vim25.RuntimeFault;
import com.vmware.vim25.mo.*;
import org.dasein.cloud.CloudErrorType;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.OperationNotSupportedException;
import org.dasein.cloud.compute.AbstractAffinityGroupSupport;
import org.dasein.cloud.compute.AffinityGroup;
import org.dasein.cloud.compute.AffinityGroupCreateOptions;
import org.dasein.cloud.compute.AffinityGroupFilterOptions;
import org.dasein.cloud.util.APITrace;
import org.dasein.cloud.util.Cache;
import org.dasein.cloud.util.CacheLevel;
import org.dasein.cloud.vsphere.Dc;
import org.dasein.cloud.vsphere.PrivateCloud;
import org.dasein.util.uom.time.Minute;
import org.dasein.util.uom.time.TimePeriod;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletResponse;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * User: daniellemayne
 * Date: 18/07/2014
 * Time: 09:12
 */
public class Host extends AbstractAffinityGroupSupport<PrivateCloud> {

    Host(@Nonnull PrivateCloud provider) {
        super(provider);
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
        APITrace.begin(getProvider(), "getAffinityGroup");
        try {
            HostSystem host = getHostSystemForAffinity(affinityGroupId);
            if (host != null) {
                String dataCenterId = null;
                ManagedEntity parent = host.getParent();
                while (parent != null) {
                    if (parent instanceof ComputeResource) {
                        dataCenterId = parent.getName();
                        break;
                    }
                    else if (parent instanceof Datacenter) {
                        dataCenterId = parent.getName()+"-a";
                        break;
                    }
                    else {
                        parent = parent.getParent();
                    }
                }

                return toAffinityGroup(host, dataCenterId);
            }
            return null;
        }
        finally {
            APITrace.end();
        }
    }

    @Nonnull
    @Override
    public Iterable<AffinityGroup> list(@Nonnull AffinityGroupFilterOptions options) throws InternalException, CloudException {
        APITrace.begin(getProvider(), "listAffinityGroups");
        try {
            ArrayList<AffinityGroup> possibles = new ArrayList<AffinityGroup>();
            String dc = options.getDataCenterId();

            ServiceInstance instance = getServiceInstance();
            Dc dcServices = getProvider().getDataCenterServices();
            Datacenter vdc = dcServices.getVmwareDatacenterFromVDCId(instance, getContext().getRegionId());
            if( vdc == null ) {
                return Collections.EMPTY_LIST;
            }
            try {
                for( ManagedEntity me : vdc.getHostFolder().getChildEntity() ) {
                    if (dc != null) {
                        if (me.getName().equals(dc)){
                            ComputeResource cluster = (ComputeResource)me;

                            for( HostSystem host : cluster.getHosts() ) {
                                possibles.add(toAffinityGroup(host, dc));
                            }
                        }
                    }
                    else {
                        ComputeResource cluster = (ComputeResource)me;

                        for( HostSystem host : cluster.getHosts() ) {
                            possibles.add(toAffinityGroup(host, me.getName()));
                        }
                    }
                }
            }
            catch (RemoteException e) {
                throw new CloudException(e);
            }
            return possibles;
        }
        finally {
            APITrace.end();
        }
    }

    @Override
    public AffinityGroup modify(@Nonnull String affinityGroupId, @Nonnull AffinityGroupCreateOptions options) throws InternalException, CloudException {
        throw new OperationNotSupportedException("Unable to modify hosts in vSphere");
    }

    public HostSystem getHostSystemForAffinity(@Nonnull String affinityGroupId) throws CloudException, InternalException {
        APITrace.begin(getProvider(), "getHostSystemForAffinity");
        try {
            ServiceInstance instance = getServiceInstance();
            Folder folder = getProvider().getVmFolder(instance);
            ManagedEntity me;

            try {
                me = new InventoryNavigator(folder).searchManagedEntity("HostSystem", affinityGroupId);
            }
            catch( InvalidProperty e ) {
                throw new CloudException("No host support in cluster: " + e.getMessage());
            }
            catch( RuntimeFault e ) {
                throw new CloudException("Error in processing request to cluster: " + e.getMessage());
            }
            catch( RemoteException e ) {
                throw new CloudException("Error in cluster processing request: " + e.getMessage());
            }

            if (me != null) {
                return (HostSystem) me;
            }
            return null;
        }
        finally {
            APITrace.end();
        }
    }

    public Collection<HostSystem> listHostSystems(@Nullable String datacenterId) throws CloudException,InternalException {
        APITrace.begin(getProvider(), "listHostSystems");
        try {
            Cache<HostSystem> cache = Cache.getInstance(getProvider(), "hosts"+datacenterId, HostSystem.class, CacheLevel.REGION_ACCOUNT, new TimePeriod<Minute>(15, TimePeriod.MINUTE));
            Collection<HostSystem> hostSystems = (Collection<HostSystem>)cache.get(getContext());

            if( hostSystems == null ) {
                hostSystems = new ArrayList<>();
                ServiceInstance instance = getServiceInstance();
                Dc dcServices = getProvider().getDataCenterServices();
                Datacenter vdc = dcServices.getVmwareDatacenterFromVDCId(instance, getContext().getRegionId());

                try {
                    for( ManagedEntity me : vdc.getHostFolder().getChildEntity() ) {
                        if (datacenterId != null) {
                            if (me.getName().equals(datacenterId)){
                                ComputeResource cluster = (ComputeResource)me;

                                for( HostSystem host : cluster.getHosts() ) {
                                    hostSystems.add(host);
                                }
                            }
                        }
                        else {
                            ComputeResource cluster = (ComputeResource)me;

                            for( HostSystem host : cluster.getHosts() ) {
                                hostSystems.add(host);
                            }
                        }
                    }
                    cache.put(getContext(), hostSystems);
                }
                catch (RemoteException e) {
                    throw new CloudException(e);
                }
            }
            return hostSystems;
        }
        finally {
            APITrace.end();
        }
    }

    public Collection<Datastore> listDatastoresForHost(HostSystem host) throws CloudException, InternalException {
        ArrayList<Datastore> list = new ArrayList<>();
        try {
            Datastore[] datastores = host.getDatastores();
            for (Datastore datastore : datastores) {
                list.add(datastore);
            }
        }
        catch (RemoteException e) {
            throw new CloudException(e);
        }
        return list;
    }


    private @Nonnull ServiceInstance getServiceInstance() throws CloudException, InternalException {
        ServiceInstance instance = getProvider().getServiceInstance();

        if( instance == null ) {
            throw new CloudException(CloudErrorType.AUTHENTICATION, HttpServletResponse.SC_UNAUTHORIZED, null, "Unauthorized");
        }
        return instance;
    }

    private AffinityGroup toAffinityGroup(@Nonnull HostSystem host, @Nonnull String dataCenterID) {
        String agID = host.getName();
        String agName = host.getName();
        String agDesc = "Affinity group for "+agName;
        long created = 0;

        AffinityGroup ag = AffinityGroup.getInstance(agID, agName, agDesc, dataCenterID, created);
        String status = host.getConfigStatus().toString();
        ag.setTag("status", status);
        return ag;
    }
}
