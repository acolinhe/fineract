package org.apache.fineract.integrationtests;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.restassured.path.json.JsonPath;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import org.apache.fineract.integrationtests.common.ClientHelper;
import org.apache.fineract.integrationtests.common.savings.SavingsAccountHelper;
import org.apache.fineract.integrationtests.common.Utils;
import org.apache.fineract.integrationtests.common.savings.SavingsProductHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class SavingsAccountsByBirthdateTest {
    private final SavingsAccountHelper savingsHelper;
    private RequestSpecification requestSpec;
    private ResponseSpecification responseSpec;
    private SavingsAccountHelper savingsAccountHelper;
    private SavingsProductHelper savingsProductHelper;

    public static final String ACCOUNT_TYPE_INDIVIDUAL = "INDIVIDUAL";

    public SavingsAccountsByBirthdateTest(SavingsAccountHelper savingsHelper) {
        this.savingsHelper = savingsHelper;
    }

    @BeforeEach
    public void setup() {
        Utils.initializeRESTAssured();
        this.requestSpec = savingsHelper.getRequestSpec();
        this.responseSpec = savingsHelper.getResponseSpec();
        this.savingsAccountHelper = new SavingsAccountHelper(this.requestSpec, this.responseSpec);
    }

    private Integer createSavingsProductDailyPosting() {
        final String savingsProductJSON = this.savingsProductHelper.withInterestCompoundingPeriodTypeAsDaily()
                .withInterestPostingPeriodTypeAsDaily().withInterestCalculationPeriodTypeAsDailyBalance().build();
        return SavingsProductHelper.createSavingsProduct(savingsProductJSON, requestSpec, responseSpec);
    }

    @Test
    public void testRetrieveSavingsAccountsByBirthdate() {
        // Set a birthdate for client
        final LocalDate birthdate = LocalDate.of(1990, 1, 1);
        String birthdateString = birthdate.toString();

        // Create a client with the given birthdate
        Integer clientId = ClientHelper.createClient(this.requestSpec, this.responseSpec, birthdateString);
        assertNotNull(clientId);

        // Create a savings account for that client
        final Integer savingsProductId = createSavingsProductDailyPosting();
        assertNotNull(savingsProductId);

        Integer savingsId = this.savingsAccountHelper.applyForSavingsApplication(clientId, savingsProductId, "INDIVIDUAL");
        assertNotNull(savingsId);

        this.savingsAccountHelper.approveSavings(savingsId);
        Map<String, Object> savingsStatusMap = this.savingsAccountHelper.activateSavings(savingsId);
        assertNotNull(savingsStatusMap);

        // Now call the new endpoint to get savings accounts by birthdate
        String response = Utils.performServerGet(requestSpec, responseSpec, "/fineract-provider/api/v1/savingsaccounts/by-birthdate?birthdate=" + birthdateString);
        JsonPath jsonPath = JsonPath.from(response);

        // Ensure response is valid -> Not Null values
        List<Map<String, Object>> accounts = jsonPath.getList("");
        assertNotNull(accounts, "Savings accounts should not be null");
        assertTrue(accounts.stream().anyMatch(account -> account.get("clientId").equals(clientId)), "Expected savings account for client ID " + clientId);
    }
}
