package org.keycloak.testsuite.pages;

import org.keycloak.testsuite.util.OAuthClient;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.pagefactory.AjaxElementLocatorFactory;

public class ResetOtpPage extends AbstractPage {

    @FindBy(id = "kc-otp-reset-form-submit")
    protected WebElement submitButton;

    @FindBy(id = "kc-otp-reset-form-description")
    protected WebElement description;

    public ResetOtpPage() {

    }

    public ResetOtpPage(WebDriver driver) {
        this.driver = driver;
        AjaxElementLocatorFactory ajax = new AjaxElementLocatorFactory(driver, 10);
        PageFactory.initElements(ajax, this);
        oauth = new OAuthClient();
        oauth.init(driver);
    }

    @Override
    public boolean isCurrent() {
        return description.getText().equals("Which OTP configuration should be removed?");
    }

    @Override
    public void open() throws Exception {
        // This page is part of the reset credentials flow, so you shouldn't be able to open it by itself.
    }

    public void selectOtp(int index) {
        driver.findElement(By.id("kc-otp-credential-" + index)).click();
    }

    public void submitOtpReset() {
        submitButton.click();
    }
}
