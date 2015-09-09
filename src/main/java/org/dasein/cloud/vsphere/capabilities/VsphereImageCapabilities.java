package org.dasein.cloud.vsphere.capabilities;

/**
 * Copyright (C) 2012-2014 Dell, Inc
 * See annotations for authorship information
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


import org.dasein.cloud.*;
import org.dasein.cloud.compute.*;
import org.dasein.cloud.util.NamingConstraints;
import org.dasein.cloud.vsphere.Vsphere;

import javax.annotation.Nonnull;

import java.util.Collections;
import java.util.Locale;

/**
 *
 */
public class VsphereImageCapabilities extends AbstractCapabilities<Vsphere> implements ImageCapabilities {
    /**
     * @param cloud todo document here... :)
     * 
     */
    public VsphereImageCapabilities( @Nonnull Vsphere cloud ) {
        super(cloud);
    }

    @Override
    public boolean canBundle(VmState fromState) throws CloudException, InternalException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean canImage(VmState fromState) throws CloudException, InternalException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public String getProviderTermForImage(Locale locale, ImageClass cls) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getProviderTermForCustomImage(Locale locale, ImageClass cls) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public VisibleScope getImageVisibleScope() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Requirement identifyLocalBundlingRequirement() throws CloudException, InternalException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Iterable<MachineImageFormat> listSupportedFormats() throws CloudException, InternalException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Iterable<MachineImageFormat> listSupportedFormatsForBundling() throws CloudException, InternalException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Iterable<ImageClass> listSupportedImageClasses() throws CloudException, InternalException {
        return Collections.unmodifiableList(Collections.singletonList(ImageClass.MACHINE));
    }

    @Override
    public Iterable<MachineImageType> listSupportedImageTypes() throws CloudException, InternalException {
        return Collections.unmodifiableList(Collections.singletonList(MachineImageType.STORAGE));
    }

    @Override
    public boolean supportsDirectImageUpload() throws CloudException, InternalException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean supportsImageCapture(MachineImageType type) throws CloudException, InternalException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean supportsImageCopy() throws CloudException, InternalException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean supportsImageRemoval() throws CloudException, InternalException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean supportsImageSharing() throws CloudException, InternalException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean supportsImageSharingWithPublic() throws CloudException, InternalException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean supportsListingAllRegions() throws CloudException, InternalException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean supportsPublicLibrary(ImageClass cls) throws CloudException, InternalException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean imageCaptureDestroysVM() throws CloudException, InternalException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public NamingConstraints getImageNamingConstraints() throws CloudException, InternalException {
        // TODO Auto-generated method stub
        return null;
    }

}
