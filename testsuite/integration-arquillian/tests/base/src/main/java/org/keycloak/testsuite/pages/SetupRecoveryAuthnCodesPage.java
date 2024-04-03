package org.keycloak.testsuite.pages;

import org.keycloak.testsuite.util.OAuthClient;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.pagefactory.AjaxElementLocatorFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class SetupRecoveryAuthnCodesPage extends LogoutSessionsPage {

    @FindBy(id = "kc-recovery-codes-list")
    private WebElement recoveryAuthnCodesList;

    @FindBy(id = "saveRecoveryAuthnCodesBtn")
    private WebElement saveRecoveryAuthnCodesButton;

    @FindBy(id="kcRecoveryCodesConfirmationCheck")
    private WebElement kcRecoveryCodesConfirmationCheck;

    public SetupRecoveryAuthnCodesPage() {

    }

    public SetupRecoveryAuthnCodesPage(WebDriver driver) {
        this.driver = driver;
        AjaxElementLocatorFactory ajax = new AjaxElementLocatorFactory(driver, 10);
        PageFactory.initElements(ajax, this);
        oauth = new OAuthClient();
        oauth.init(driver);
    }

    public void clickSaveRecoveryAuthnCodesButton() {
        kcRecoveryCodesConfirmationCheck.click();
        saveRecoveryAuthnCodesButton.click();
    }

    public List<String> getRecoveryAuthnCodes() {
        String recoveryAuthnCodesText =  recoveryAuthnCodesList.getText();
        List<String> recoveryAuthnCodesList = new ArrayList<>();
        Scanner scanner = new Scanner(recoveryAuthnCodesText);
        while (scanner.hasNextLine()) {
            recoveryAuthnCodesList.add(scanner.nextLine());
        }
        scanner.close();
        return recoveryAuthnCodesList;
    }

    @Override
    public boolean isCurrent() {

        // Check the backup code text box and label available
        try {
            driver.findElement(By.id("kc-recovery-codes-list"));
            driver.findElement(By.id("saveRecoveryAuthnCodesBtn"));
        } catch (NoSuchElementException nfe) {
            return false;
        }
        return true;
    }

    @Override
    public void open() throws Exception {
        throw new UnsupportedOperationException();
    }
}
