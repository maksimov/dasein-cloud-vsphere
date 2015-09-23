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

import com.vmware.vim25.InvalidPropertyFaultMsg;
import com.vmware.vim25.RuntimeFaultFaultMsg;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.UserSession;
import com.vmware.vim25.VimPortType;
import com.vmware.vim25.VimService;


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
    Logger logger;
    @Mocked
    VsphereConnection connectionMock;
    @Mocked
    VimPortType vimPortMock;
    @Mocked 
    VimService vimServiceMock;
    @Mocked 
    UserSession userSessionMock;
    @Mocked
    ServiceContent serviceContentMock;

    protected final String ACCOUNT_NO = "TESTACCOUNTNO";
    protected final String REGION = "datacenter-21";
    protected final String ENDPOINT = "TESTENDPOINT";

    @Before
    public void setUp() throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg {
        new NonStrictExpectations() {
            { vsphereMock.getContext(); result = providerContextMock; }
        };

        new NonStrictExpectations() {
            { providerContextMock.getAccountNumber(); result = ACCOUNT_NO; }
            { providerContextMock.getRegionId(); result = REGION; }
            { providerContextMock.getEndpoint(); result = ENDPOINT;}
        };

        new NonStrictExpectations() {
            { connectionMock.getVimPort(); result = vimPortMock;}
            { connectionMock.getServiceContent(); result = serviceContentMock; }
            { connectionMock.getUserSession(); result = userSessionMock;}
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
