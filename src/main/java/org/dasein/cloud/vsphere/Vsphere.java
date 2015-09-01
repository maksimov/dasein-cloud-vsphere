/**
 * Copyright (C) 2012-2013 Dell, Inc.
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

package org.dasein.cloud.vsphere;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.dasein.cloud.AbstractCloud;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.ContextRequirements;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.ProviderContext;
import org.dasein.cloud.vsphere.compute.VsphereCompute;

import com.vmware.vim25.DynamicProperty;
import com.vmware.vim25.InvalidLocaleFaultMsg;
import com.vmware.vim25.InvalidLoginFaultMsg;
import com.vmware.vim25.InvalidPropertyFaultMsg;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.ObjectContent;
import com.vmware.vim25.ObjectSpec;
import com.vmware.vim25.PropertyFilterSpec;
import com.vmware.vim25.PropertySpec;
import com.vmware.vim25.RetrieveOptions;
import com.vmware.vim25.RetrieveResult;
import com.vmware.vim25.RuntimeFaultFaultMsg;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.TraversalSpec;
import com.vmware.vim25.UserSession;
import com.vmware.vim25.VimPortType;
import com.vmware.vim25.VimService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.ws.BindingProvider;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;

/**
 * Add header info here
 * @version 2013.01 initial version
 * @since 2013.01
 */
public class Vsphere extends AbstractCloud {
    private int sessionTimeout = 0;
    private VimPortType vimPortType;
    private ServiceContent serviceContent;
    private String vimHostname;
    private UserSession userSession;

    public VimPortType getVimPortType() {
        return vimPortType;
    }

    public VimPortType getVimPort() {
        return vimPortType;
    }

    public ServiceContent getServiceContent() {
        return serviceContent;
    }

    public ServiceContent getVimServiceInstanceReference() {
        return serviceContent;
    }

    public String getVimHostname() {
        return vimHostname;
    }

    public UserSession getUserSession() {
        return userSession;
    }

    static private final Logger log = getLogger(Vsphere.class);

    static private @Nonnull String getLastItem(@Nonnull String name) {
        int idx = name.lastIndexOf('.');

        if( idx < 0 ) {
            return name;
        }
        else if( idx == (name.length()-1) ) {
            return "";
        }
        return name.substring(idx + 1);
    }

    static public @Nonnull Logger getLogger(@Nonnull Class<?> cls) {
        String pkg = getLastItem(cls.getPackage().getName());

        if( pkg.equals("vsphere") ) {
            pkg = "";
        }
        else {
            pkg = pkg + ".";
        }
        return Logger.getLogger("dasein.cloud.vsphere.std." + pkg + getLastItem(cls.getName()));
    }

    static public @Nonnull Logger getWireLogger(@Nonnull Class<?> cls) {
        return Logger.getLogger("dasein.cloud.skeleton.wire." + getLastItem(cls.getPackage().getName()) + "." + getLastItem(cls.getName()));
    }

    public Vsphere() { }

    @Override
    public @Nonnull String getCloudName() {
        ProviderContext ctx = getContext();
        String name = (ctx == null ? null : ctx.getCloudName());

        return (name == null ? "vSphere" : name);
    }

    @Override
    public @Nonnull ContextRequirements getContextRequirements() {

         return new ContextRequirements(
                 new ContextRequirements.Field("apiKey", "The API Keypair", ContextRequirements.FieldType.KEYPAIR, ContextRequirements.Field.ACCESS_KEYS, true),
                 new ContextRequirements.Field("proxyHost", "Proxy host", ContextRequirements.FieldType.TEXT, null, false),
                 new ContextRequirements.Field("proxyPort", "Proxy port", ContextRequirements.FieldType.TEXT, null, false)
         );
    }

    public void disconnect() {
        try {
            getVimPort().logout(serviceContent.getSessionManager());
        } catch ( RuntimeFaultFaultMsg e ) {
            log.warn("While logging out recieved: " + e);
        }
    }

    public @Nonnull VimService getServiceInstance() throws CloudException, InternalException {
        // TODO add caching
        ProviderContext ctx = getContext();

        VimService vimService = null;
        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.getServerSessionContext().setSessionTimeout(sessionTimeout);
            sc.init(null, null, null);

            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

            ManagedObjectReference servicesInstance = new ManagedObjectReference();
            servicesInstance.setType("ServiceInstance");
            servicesInstance.setValue("ServiceInstance");

            vimService = new com.vmware.vim25.VimService();
            vimPortType = vimService.getVimPort();
            Map<String, Object> ctxt = ((BindingProvider) vimPortType).getRequestContext();

            vimHostname = new URI(ctx.getEndpoint()).getHost();

            ctxt.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, ctx.getEndpoint());
            ctxt.put(BindingProvider.SESSION_MAINTAIN_PROPERTY, true);

            serviceContent = vimPortType.retrieveServiceContent(servicesInstance);
        } catch (Exception e) {
            throw new InternalException(e);
        }

        List<ContextRequirements.Field> fields = getContextRequirements().getConfigurableValues();
        String username = null;
        String password = null;
        try  {
            for (ContextRequirements.Field field : fields ) {
                if (field.type.equals(ContextRequirements.FieldType.KEYPAIR)){
                    byte[][] keyPair = (byte[][])ctx.getConfigurationValue(field);
                    username = new String(keyPair[0], "utf-8");
                    password = new String(keyPair[1], "utf-8");
                }
            }
        } catch ( UnsupportedEncodingException e ) {
            throw new InternalException(e);
        }

        try {
            userSession = vimPortType.login(serviceContent.getSessionManager(), username, password, null);
        } catch (Exception e) {
            throw new CloudException(e.getMessage());
        }

        return vimService;
    }

    @Override
    public @Nonnull VsphereCompute getComputeServices() {
        return new VsphereCompute(this);
    }

    @Override
    public @Nonnull DataCenters getDataCenterServices() {
        return new DataCenters(this);
    }

    @Override
    public @Nonnull String getProviderName() {
        ProviderContext ctx = getContext();
        String name = (ctx == null ? null : ctx.getProviderName());

        return (name == null ? "vSphere" : name);
    }

    
    @Override
    public @Nullable String testContext() {
        if( log.isTraceEnabled() ) {
            log.trace("ENTER - " + Vsphere.class.getName() + ".testContext()");
        }
        try {
            ProviderContext ctx = getContext();

            if( ctx == null ) {
                log.warn("No context was provided for testing");
                return null;
            }
            
            try {
                VimService instance = getServiceInstance();
            } catch ( Exception e ) {
                e.printStackTrace();
            }
            
            try {
                if( !getComputeServices().getVirtualMachineSupport().isSubscribed() ) {
                    return null;
                }
                return ctx.getAccountNumber();
            }
            catch( Throwable t ) {
                log.error("testContext(): Failed to test vSphere context: " + t.getMessage());
                if( log.isTraceEnabled() ) {
                    t.printStackTrace();
                }
                return null;
            }
        }
        finally {
            if( log.isTraceEnabled() ) {
                log.trace("EXIT - " + Vsphere.class.getName() + ".textContext()");
            }
        }
    }
}