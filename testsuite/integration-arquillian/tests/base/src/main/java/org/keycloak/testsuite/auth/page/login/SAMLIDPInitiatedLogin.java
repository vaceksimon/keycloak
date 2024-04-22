package org.keycloak.testsuite.auth.page.login;

import jakarta.ws.rs.core.UriBuilder;
import org.openqa.selenium.WebDriver;

/**
 * @author mhajas
 */
public class SAMLIDPInitiatedLogin extends SAMLRedirectLogin {

    public SAMLIDPInitiatedLogin(WebDriver driver) {
        super(driver);
    }

    public void setUrlName(String urlName) {
        setUriParameter("clientUrlName", urlName);
    }

    @Override
    public UriBuilder createUriBuilder() {
        return super.createUriBuilder().path("clients/{clientUrlName}");
    }
}

