package org.dasein.cloud.vsphere;

import java.util.Arrays;
import java.util.List;

import javax.annotation.Nonnull;

import com.vmware.vim25.*;

import org.apache.log4j.Logger;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.util.APITrace;
import org.dasein.util.uom.time.Second;
import org.dasein.util.uom.time.TimePeriod;

public class VsphereMethod {
    static private final Logger logger = Vsphere.getLogger(Vsphere.class);

    private Vsphere provider;

    private PropertyChange taskResult;
    private PropertyChange taskState;

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
    
    // migrate and clean this into getOperationComplete()
    public void waitOperationComplete(ManagedObjectReference taskmor) throws CloudException, InternalException {
        APITrace.begin(provider, "VsphereMethod.waitOperationComplete");
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
        pSpec.getPathSet().addAll(Arrays.asList("info.state", "info.error", "info.result"));
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
                                if (propchg.getName().equals("info.result")) {
                                    setTaskResult(propchg);
                                }
                                else if (propchg.getName().equals("info.state")) {
                                    setTaskState(propchg);
                                }
                            }
                        }
                    }
                }
                if (taskState.getVal().equals(TaskInfoState.SUCCESS)) {
                    break;
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

    public PropertyChange getTaskResult() {
        return taskResult;
    }

    public void setTaskResult(PropertyChange taskResult) {
        this.taskResult = taskResult;
    }

    public PropertyChange getTaskState() {
        return taskState;
    }

    public void setTaskState(PropertyChange taskState) {
        this.taskState = taskState;
    }
}