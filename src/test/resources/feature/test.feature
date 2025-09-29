Feature: Update PAS with MTD/YTD amounts

  Scenario Outline: Update PAS with MTD/YTD amounts
    Given User generates service request for <ApiName6> with payload <PayloadData6>
    And User updates the request true for 1 Api Payload
    Then Invokes API <ApiName6> with payload
    And Validates the ResponseCode with expected <ResponseCode>

    Given User generates service request for <ApiName7> with payload <PayloadData7>
    And User updates the request true for 1 Api Payload
    And Extracts <DataField7> from previous api-1 response and uses in current request <PayloadData7>
    And Sets Authorization header true for userType <UserType>
    Then Validates the response with DB using Query "<DBQuery>"

    Then Invokes API <ApiName7> with payload
    And Validates the ResponseCode with expected <ResponseCode>

    Examples:
      | ApiName6       | PayloadData6       | ApiName7        | PayloadData7      | DataField7       | ResponseCode | UserType | DBQuery                     |
      | PADJDetails    | SUBMITPAYMENTADJUSTMENT | PADJDetails    | SUBMITPAYMENTADJUSTMENT | SUBMITPAYMENTADJUSTMENT | 200          | Internal | SELECT * FROM PAYMENTS WHERE ID='LS-18323-8' |
      | PADJDetails    | SUBMITPAYMENTADJUSTMENT | PADJDetails    | SUBMITPAYMENTADJUSTMENT | SUBMITPAYMENTADJUSTMENT | 200          | Internal | SELECT * FROM PAYMENTS WHERE ID='LS-18323-11' |
