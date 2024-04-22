package org.keycloak.testsuite.auth.page.login;

import org.keycloak.authentication.requiredactions.DeleteAccount;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.pagefactory.AjaxElementLocatorFactory;

import static org.keycloak.testsuite.util.UIUtils.clickLink;

public class DeleteAccountActionConfirmPage extends RequiredActions {

  @FindBy(css = "button[name='cancel-aia']")
  WebElement cancelActionButton;

  @FindBy(css = "input[type='submit']")
  WebElement confirmActionButton;

  public DeleteAccountActionConfirmPage(WebDriver driver) {
      this.driver = driver;
      AjaxElementLocatorFactory ajax = new AjaxElementLocatorFactory(driver, 10);
      PageFactory.initElements(ajax, this);
  }


  @Override
  public String getActionId() {
    return DeleteAccount.PROVIDER_ID;
  }

  @Override
  public boolean isCurrent() {
    return driver.getCurrentUrl().contains("login-actions/required-action") && driver.getCurrentUrl().contains("execution=delete_account");
  }


  public void clickCancelAIA() {
    clickLink(cancelActionButton);
  }

  public void clickConfirmAction() {
    clickLink(confirmActionButton);
  }

  public boolean isErrorMessageDisplayed() {
    return driver.findElements(By.cssSelector(".alert-error")).size() == 1;
  }

  public String getErrorMessageText() {
    return driver.findElement(By.cssSelector("#kc-content-wrapper > div > span.kc-feedback-text")).getText();
  }
}
