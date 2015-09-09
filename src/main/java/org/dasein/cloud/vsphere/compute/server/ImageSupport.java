package org.dasein.cloud.vsphere.compute.server;

import org.apache.log4j.Logger;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.compute.AbstractImageSupport;
import org.dasein.cloud.compute.ImageFilterOptions;
import org.dasein.cloud.compute.MachineImage;
import org.dasein.cloud.vsphere.Vsphere;
import org.dasein.cloud.vsphere.capabilities.VsphereImageCapabilities;


/**
 *
 */
public class ImageSupport extends AbstractImageSupport<Vsphere> {
    private Vsphere provider;
    private VsphereImageCapabilities capabilities;
    static private final Logger logger = Vsphere.getLogger(ImageSupport.class);

    public ImageSupport(Vsphere provider) {
        super(provider);
    }

    @Override
    public VsphereImageCapabilities getCapabilities() throws CloudException, InternalException {
        if (capabilities == null) {
            capabilities = new VsphereImageCapabilities(provider);
        }
        return capabilities;
    }

    @Override
    public MachineImage getImage(String providerImageId) throws CloudException, InternalException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isSubscribed() throws CloudException, InternalException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Iterable<MachineImage> listImages(ImageFilterOptions options) throws CloudException, InternalException {
        // TODO Auto-generated method stub ROGER HERE....
        return null;
    }

    @Override
    public void remove(String providerImageId, boolean checkState) throws CloudException, InternalException {
        // TODO Auto-generated method stub
    }


}
