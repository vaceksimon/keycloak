package org.keycloak.testsuite.pages;

import org.keycloak.testsuite.util.OAuthClient;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.pagefactory.AjaxElementLocatorFactory;

/**
 * Signing In Page with required action "Enter Backup Code for authentication"
 *
 * @author <a href="mailto:vnukala@redhat.com">Venkata Nukala</a>
 */
public class EnterRecoveryAuthnCodePage extends LanguageComboboxAwarePage {

    @FindBy(xpath = "//label[@for='recoveryCodeInput']")
    private WebElement recoveryAuthnCodeLabel;

    @FindBy(id = "recoveryCodeInput")
    private WebElement recoveryAuthnCodeTextField;

    @FindBy(id = "kc-login")
    private WebElement signInButton;

    @FindBy(className = "kc-feedback-text")
    private WebElement feedbackText;

    public EnterRecoveryAuthnCodePage() {

    }

    public EnterRecoveryAuthnCodePage(WebDriver driver) {
        this.driver = driver;
        AjaxElementLocatorFactory ajax = new AjaxElementLocatorFactory(driver, 10);
        PageFactory.initElements(ajax, this);
        oauth = new OAuthClient();
        oauth.init(driver);
    }

    public int getRecoveryAuthnCodeToEnterNumber() {
        String [] recoveryAuthnCodeLabelParts = recoveryAuthnCodeLabel.getText().split("#");
        return Integer.valueOf(recoveryAuthnCodeLabelParts[1]) - 1; // Recovery Authn Code 1 is at element 0 in the list
    }

    public void enterRecoveryAuthnCode(String recoveryCode) {
        recoveryAuthnCodeTextField.sendKeys(recoveryCode);
    }

    public void clickSignInButton() {
        signInButton.click();
    }

    @Override
    public boolean isCurrent() {

        // Check the backup code text box and label available
        try {
            driver.findElement(By.id("recoveryCodeInput"));
            driver.findElement(By.xpath("//label[@for='recoveryCodeInput']"));
        } catch (NoSuchElementException nfe) {
            return false;
        }
        return true;
    }

    @Override
    public void open() throws Exception {
        throw new UnsupportedOperationException();
    }

    public String getFeedbackText() {
        return feedbackText.getText().trim();
    }
}
