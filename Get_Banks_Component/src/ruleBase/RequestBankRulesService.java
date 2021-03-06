
package ruleBase;

import java.net.MalformedURLException;
import java.net.URL;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.WebServiceFeature;


/**
 * This class was generated by the JAX-WS RI.
 * JAX-WS RI 2.2.9-b130926.1035
 * Generated source version: 2.2
 * 
 */
@WebServiceClient(name = "RequestBankRulesService", targetNamespace = "http://wsdl/", wsdlLocation = "http://94.130.57.246:9000/rules/RequestBankRulesService?wsdl")
public class RequestBankRulesService
    extends Service
{

    private final static URL REQUESTBANKRULESSERVICE_WSDL_LOCATION;
    private final static WebServiceException REQUESTBANKRULESSERVICE_EXCEPTION;
    private final static QName REQUESTBANKRULESSERVICE_QNAME = new QName("http://wsdl/", "RequestBankRulesService");

    static {
        URL url = null;
        WebServiceException e = null;
        try {
            url = new URL("http://94.130.57.246:9000/rules/RequestBankRulesService?wsdl");
        } catch (MalformedURLException ex) {
            e = new WebServiceException(ex);
        }
        REQUESTBANKRULESSERVICE_WSDL_LOCATION = url;
        REQUESTBANKRULESSERVICE_EXCEPTION = e;
    }

    public RequestBankRulesService() {
        super(__getWsdlLocation(), REQUESTBANKRULESSERVICE_QNAME);
    }

    public RequestBankRulesService(WebServiceFeature... features) {
        super(__getWsdlLocation(), REQUESTBANKRULESSERVICE_QNAME, features);
    }

    public RequestBankRulesService(URL wsdlLocation) {
        super(wsdlLocation, REQUESTBANKRULESSERVICE_QNAME);
    }

    public RequestBankRulesService(URL wsdlLocation, WebServiceFeature... features) {
        super(wsdlLocation, REQUESTBANKRULESSERVICE_QNAME, features);
    }

    public RequestBankRulesService(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public RequestBankRulesService(URL wsdlLocation, QName serviceName, WebServiceFeature... features) {
        super(wsdlLocation, serviceName, features);
    }

    /**
     * 
     * @return
     *     returns RequestBankRules
     */
    @WebEndpoint(name = "RequestBankRulesPort")
    public RequestBankRules getRequestBankRulesPort() {
        return super.getPort(new QName("http://wsdl/", "RequestBankRulesPort"), RequestBankRules.class);
    }

    /**
     * 
     * @param features
     *     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns RequestBankRules
     */
    @WebEndpoint(name = "RequestBankRulesPort")
    public RequestBankRules getRequestBankRulesPort(WebServiceFeature... features) {
        return super.getPort(new QName("http://wsdl/", "RequestBankRulesPort"), RequestBankRules.class, features);
    }

    private static URL __getWsdlLocation() {
        if (REQUESTBANKRULESSERVICE_EXCEPTION!= null) {
            throw REQUESTBANKRULESSERVICE_EXCEPTION;
        }
        return REQUESTBANKRULESSERVICE_WSDL_LOCATION;
    }

}
