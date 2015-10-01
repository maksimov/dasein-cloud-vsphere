package org.dasein.cloud.vsphere;

import java.util.Arrays;
import java.util.List;

import javax.annotation.Nonnull;

import org.apache.log4j.Logger;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.util.APITrace;
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

    public @Nonnull boolean getOperationComplete(ManagedObjectReference taskmor, TimePeriod interval) {
        return false;

    }

    public @Nonnull boolean getOperationCurrentStatus(ManagedObjectReference taskmor) {
        return false;

    }
    
    // migrate and clean this into getOperationComplete()
    public void waitOperationComplete(ManagedObjectReference taskmor) throws CloudException, InternalException {
        APITrace.begin(provider, "ImageSupport.waitOperationComplete");
        VsphereConnection vsphereConnection = provider.getServiceInstance();
        VimPortType vimPort = vsphereConnection.getVimPort();
        ServiceContent serviceContent = vsphereConnection.getServiceContent();

        List<PropertyFilterUpdate> filtupary = null;
        List<ObjectUpdate> objupary = null;

        UpdateSet updateset = null;
        String version = "";

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
        
        
        int x = 10;
        try {
            while (x-- > 0) {
                filterSpecRef = vimPort.createFilter(serviceContent.getPropertyCollector(), spec, true);
                updateset = vimPort.waitForUpdatesEx(serviceContent.getPropertyCollector(), version, new WaitOptions());

                if (updateset == null || updateset.getFilterSet() == null) {
                    continue;
                }
                version = updateset.getVersion();

                // Make this code more general purpose when PropCol changes later.
                filtupary = updateset.getFilterSet();

                for (PropertyFilterUpdate filtup : filtupary) {
                    objupary = filtup.getObjectSet();
                    for (ObjectUpdate objup : objupary) { // <-- contains TaskInfoState
                        Object val = objup.getChangeSet().iterator().next().getVal();
                        val = objup.getChangeSet().iterator().next().getVal();
                        // TODO: Handle all "kind"s of updates.
                        if (objup.getKind() == ObjectUpdateKind.MODIFY || objup.getKind() == ObjectUpdateKind.ENTER || objup.getKind() == ObjectUpdateKind.LEAVE) {

                            for (PropertyChange propchg : objup.getChangeSet()) {
                                System.out.println("INSPECT propchg to detect complete..." + propchg.getVal());
                            }
                        }
                    }
                }
            }
            // Destroy the filter when we are done.

            vimPort.destroyPropertyFilter(filterSpecRef);
        } catch ( InvalidPropertyFaultMsg e ) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch ( RuntimeFaultFaultMsg e ) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch ( InvalidCollectorVersionFaultMsg e ) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            APITrace.end();
        }
    }
}
