package org.keycloak.testsuite.auth.page.login;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.pagefactory.AjaxElementLocatorFactory;

/**
 * @author mhajas
 */
public class SAMLPostLogin extends Login {

    public SAMLPostLogin() {

    }

    public SAMLPostLogin(WebDriver driver) {
        setProtocol(LOGIN_ACTION);

        this.driver = driver;
        AjaxElementLocatorFactory ajax = new AjaxElementLocatorFactory(driver, 10);
        PageFactory.initElements(ajax, this);
    }
}
