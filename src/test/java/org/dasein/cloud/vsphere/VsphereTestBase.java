package org.dasein.cloud.vsphere;

import mockit.Mock;
import mockit.MockUp;
import mockit.Mocked;
import mockit.NonStrictExpectations;
import mockit.integration.junit4.JMockit;
import org.apache.http.StatusLine;
import org.apache.log4j.Logger;
import org.dasein.cloud.ProviderContext;
import org.dasein.cloud.compute.AffinityGroupSupport;
import org.dasein.cloud.vsphere.compute.Vm;
import org.dasein.cloud.vsphere.compute.VsphereCompute;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;


/**
 * User: daniellemayne
 * Date: 16/09/2015
 * Time: 13:43
 */
@RunWith(JMockit.class)
public class VsphereTestBase {
    @Mocked
    ProviderContext providerContextMock;
    @Mocked
    Vsphere vsphereMock;
    @Mocked
    VsphereCompute vsphereComputeMock;
    @Mocked
    AffinityGroupSupport vsphereAGMock;
    @Mocked
    Logger logger;


    protected final String ACCOUNT_NO = "TESTACCOUNTNO";
    protected final String REGION = "datacenter-21";
    protected final String ENDPOINT = "TESTENDPOINT";

    @Before
    public void setUp() {
        new NonStrictExpectations() {
            { vsphereMock.getContext(); result = providerContextMock; }
            { vsphereMock.getComputeServices(); result = vsphereComputeMock; }
            { vsphereComputeMock.getAffinityGroupSupport(); result = vsphereAGMock; }
        };

        new NonStrictExpectations() {
            { providerContextMock.getAccountNumber(); result = ACCOUNT_NO; }
            { providerContextMock.getRegionId(); result = REGION; }
            { providerContextMock.getEndpoint(); result = ENDPOINT;}
        };
    }

    protected StatusLine getStatusLineMock(final int statusCode){
        return new MockUp<StatusLine>(){
            @Mock
            public int getStatusCode() {
                return statusCode;
            }
        }.getMockInstance();
    }
}
