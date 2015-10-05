package org.dasein.cloud.vsphere;

import java.util.Arrays;
import java.util.List;

import javax.annotation.Nonnull;

import org.apache.log4j.Logger;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.util.APITrace;
import org.dasein.util.uom.UnitOfMeasure;
import org.dasein.util.uom.time.Millisecond;
import org.dasein.util.uom.time.Second;
import org.dasein.util.uom.time.TimePeriod;

import com.vmware.vim25.InvalidCollectorVersionFaultMsg;
import com.vmware.vim25.InvalidPropertyFaultMsg;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.ObjectSpec;
import com.vmware.vim25.ObjectUpdate;
import com.vmware.vim25.ObjectUpdateKind;
import com.vmware.vim25.PropertyChange;
import com.vmware.vim25.PropertyFilterSpec;
import com.vmware.vim25.PropertyFilterUpdate;
import com.vmware.vim25.PropertySpec;
import com.vmware.vim25.RuntimeFaultFaultMsg;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.UpdateSet;
import com.vmware.vim25.VimPortType;
import com.vmware.vim25.WaitOptions;

public class VsphereMethod {
    static private final Logger logger = Vsphere.getLogger(Vsphere.class);

    private Vsphere provider;

    public VsphereMethod(@Nonnull Vsphere provider) {
        this.provider = provider;
    }

    public @Nonnull boolean getOperationComplete(ManagedObjectReference taskmor, TimePeriod interval, int repetions) throws CloudException, InternalException {
        Long intervalSeconds = ((TimePeriod<Second>)interval.convertTo(TimePeriod.SECOND)).longValue();

        for (int iteration = 0; iteration < repetions; iteration++) {
            try { Thread.sleep(1000 * intervalSeconds); }
            catch( InterruptedException e ) { }
            if (getOperationCurrentStatus(taskmor)) {
                return true;
            }
        }

        return false;
    }

    public @Nonnull boolean getOperationCurrentStatus(ManagedObjectReference taskmor) throws CloudException, InternalException {
        APITrace.begin(provider, "ImageSupport.waitOperationComplete");
        VsphereConnection vsphereConnection = provider.getServiceInstance();
        VimPortType vimPort = vsphereConnection.getVimPort();
        ServiceContent serviceContent = vsphereConnection.getServiceContent();

        PropertyFilterSpec spec = new PropertyFilterSpec();
        ObjectSpec oSpec = new ObjectSpec();
        oSpec.setObj(taskmor);
        oSpec.setSkip(Boolean.FALSE);
        spec.getObjectSet().add(oSpec);

        PropertySpec pSpec = new PropertySpec();
        pSpec.getPathSet().addAll(Arrays.asList("info.state", "info.error"));
        pSpec.setType(taskmor.getType());
        spec.getPropSet().add(pSpec);
        ManagedObjectReference filterSpecRef = null;
        try {
            filterSpecRef = vimPort.createFilter(serviceContent.getPropertyCollector(), spec, true);
            UpdateSet updateset = vimPort.waitForUpdatesEx(serviceContent.getPropertyCollector(), "", new WaitOptions());

            if (updateset == null || updateset.getFilterSet() == null) {
                return false;
            }

            // Make this code more general purpose when PropCol changes later.
            List<PropertyFilterUpdate> filtupary = updateset.getFilterSet();

            for (PropertyFilterUpdate filtup : filtupary) {
                List<ObjectUpdate> objupary = filtup.getObjectSet();
                for (ObjectUpdate objup : objupary) { // <-- contains TaskInfoState

                    System.out.println("val3 = " + objup.getChangeSet().get(1).getVal());
                    if (objup.getChangeSet().get(1).getVal().toString().equals("SUCCESS")) {
                        System.out.println("returning true");
                        return true;
                    }
                }

            }


            
        } catch ( InvalidPropertyFaultMsg e ) {
            throw new CloudException(e);
        } catch ( RuntimeFaultFaultMsg e ) {
            throw new CloudException(e);
        } catch ( InvalidCollectorVersionFaultMsg e ) {
            throw new CloudException(e);
        } finally {
            // Destroy the filter when we are done.
            try {
                vimPort.destroyPropertyFilter(filterSpecRef);
            } catch ( RuntimeFaultFaultMsg e ) {
                throw new CloudException(e);
            }
            APITrace.end();
        }
        return false;

    }
}
