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
    private PropertyChange taskError;

    public VsphereMethod(@Nonnull Vsphere provider) {
        this.provider = provider;
    }

    public @Nonnull boolean getOperationComplete(ManagedObjectReference taskmor, TimePeriod interval, int repetions) throws CloudException, InternalException {
        APITrace.begin(provider, "VsphereMethod.getOperationComplete");
        Long intervalSeconds = ((TimePeriod<Second>)interval.convertTo(TimePeriod.SECOND)).longValue();
        try {
            for (int iteration = 0; iteration < repetions; iteration++) {
                if (getOperationCurrentStatus(taskmor)) {
                    return true;
                }
                try { Thread.sleep(1000 * intervalSeconds); }
                catch( InterruptedException e ) { }
            }

            return false;
        } finally {
            APITrace.end();
        }
    }

    public @Nonnull boolean getOperationCurrentStatus(ManagedObjectReference taskmor) throws CloudException, InternalException {
        APITrace.begin(provider, "VsphereMethod.getOperationCurrentStatus");

        String version = "";
        List<PropertyFilterUpdate> filtupary = null;
        List<ObjectUpdate> objupary = null;

        VsphereConnection vsphereConnection = provider.getServiceInstance();
        VimPortType vimPort = vsphereConnection.getVimPort();
        ServiceContent serviceContent = vsphereConnection.getServiceContent();

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
        try {
            filterSpecRef = vimPort.createFilter(serviceContent.getPropertyCollector(), spec, true);
            UpdateSet updateset = vimPort.waitForUpdatesEx(serviceContent.getPropertyCollector(), version, new WaitOptions());

            if (updateset == null || updateset.getFilterSet() == null) {
                return false;
            }
            version = updateset.getVersion();
            filtupary = updateset.getFilterSet();

            for (PropertyFilterUpdate filtup : filtupary) {
                objupary = filtup.getObjectSet();
                for (ObjectUpdate objup : objupary) {
                    if (objup.getKind() == ObjectUpdateKind.MODIFY || objup.getKind() == ObjectUpdateKind.ENTER || objup.getKind() == ObjectUpdateKind.LEAVE) {
                        for (PropertyChange propchg : objup.getChangeSet()) {
                            if (propchg.getName().equals("info.result")) {
                                setTaskResult(propchg);
                            } else if (propchg.getName().equals("info.state")) {
                                setTaskState(propchg);
                            } else if (propchg.getName().equals("info.error")) {
                                setTaskError(propchg);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new CloudException(e);
        } finally {
            try {
                vimPort.destroyPropertyFilter(filterSpecRef);
            } catch (Exception e) {
                throw new CloudException(e);
            }
            APITrace.end();
        }
        if ((null != taskState) && (taskState.getVal().equals(TaskInfoState.SUCCESS))) {
            return true;
        }
        return false;
    }

    @Deprecated
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
                    for (ObjectUpdate objup : objupary) {
                        Object val = objup.getChangeSet().iterator().next().getVal();

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
            vimPort.destroyPropertyFilter(filterSpecRef);
        } catch (Exception e) {
            throw new CloudException(e);
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

    public PropertyChange getTaskError() {
        return taskError;
    }

    public void setTaskError(PropertyChange taskError) {
        this.taskError = taskError;
    }
}