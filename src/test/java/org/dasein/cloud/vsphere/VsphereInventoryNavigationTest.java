package org.dasein.cloud.vsphere;

import com.vmware.vim25.PropertySpec;
import com.vmware.vim25.SelectionSpec;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * User: daniellemayne
 * Date: 24/09/2015
 * Time: 12:43
 */
public class VsphereInventoryNavigationTest extends VsphereTestBase {
    private VsphereInventoryNavigation vin = null;

    @Before
    public void setUp() throws Exception{
        super.setUp();
        vin = new VsphereInventoryNavigation();
    }

    @Test(expected = NullPointerException.class)
    public void nullPropertySpecInRetrieveObjectListRequestShouldThrowException() throws CloudException, InternalException {
        vin.retrieveObjectList(vsphereMock, "hostFolder", new ArrayList<SelectionSpec>(), null);
    }

    @Test(expected = CloudException.class)
    public void emptyPropertySpecInRetrieveObjectListRequestShouldThrowException() throws CloudException, InternalException {
        final List<PropertySpec> props = new ArrayList<PropertySpec>();

        vin.retrieveObjectList(vsphereMock, "hostFolder", null, props);
    }
}
