Get started
    API Endpoint

        https://api.1xgate.com/
                
You can simply accept crypto for your businesses by integrating with 1xGate. To accept payment you just need to create a new transaction and redirect the user to to response link. All of the complexities of accepting crypto on different networks will be handled on 1xGate and you will be notified of the payment result at the end.

Authentication
To call the 1xGate APIs, you need an API key. Please register in 1xgate.com to create your own merchant API key.
When you log in to 1xGate panel, you need to create a merchant for your business. After creating your merchant the API key will show you once in the panel.

⚠️Keep it in mind that once the API key has been created we do not show the API key anymore. So copy it and store it in a secure place.

To call the endpoint to have to set the merchant API KEY as x-integration-key: YOUR_API_KEY HTTP header

Create Payment

# Here is a curl example
curl --location 'https://api.1xgate.com/v1/integrations/payments/external' \
--header 'x-integration-key: YOUR_MERCHANT_API_KEY' \
--header 'Content-Type: application/json' \
--data '{
    "currency": "usdt",
    "network": "trx",
    "amount": 2.3,
    "orderId": "ORDER_ID_IN_YOUR_DB",
    "callbacks": {
        "success": "https://google.com/q=success",
        "failed": "https://google.com/q=failed"
    }
}'
                
[POST] /v1/integrations/payments

To create a new payment you need to make a POST call to the above url.


In the response you will get _id which is payment id in 1xGate service and link that you can redirect your user to the given link for payment.


⚠️ When the has user has been redirected back to your website after payment you should wait until you got the webhook from the 1xGate see here



Result example :
{
    "data": {
        "userId": "65df57b2eb8c7a6c2b6be09d",
        "ip": "3.13.171.120",
        "merchantId": "66ad200c8950036604490832",
        "currency": "usdt",
        "network": "trx",
        "address": null,
        "memo": null,
        "amount": 2.3,
        "amountUsd": 2.29839,
        "amountPaid": 0,
        "amountPaidUsd": 0,
        "priceUnit": 0.9993,
        "wage": 0,
        "orderId": "ORDER_ID_IN_YOUR_DB",
        "type": "external",
        "status": "created",
        "inUse": true,
        "expireAt": null,
        "callbacks": {
            "success": "https://google.com/q=success",
            "failed": "https://google.com/q=failed"
        },
        "meta": {},
        "updatedAt": "2024-08-02T18:23:14.867Z",
        "createdAt": "2024-08-02T18:23:14.867Z",
        "_id": "66ad24125dececa8b9a16bf3",
        "link": "https://pay.1xgate.com/66ad24125dececa8b9a16bf3"
    }
}
                
QUERY PARAMETERS
Field	Required	Type	Description
currency	Yes	String	You payment currency. "USDT"
network	Yes	String	You blockchain network. "TRON" (TRC20)
amount	Yes	Number	Amount of payment in above currency. 10.023 (USDT on Tron network)
orderId	Yes	String	Order Id on your end. You must set a unique order id for each payment
description	No	String	Description for the payment
merchantUserEmail	No	String	Email address of user (payer) on your end
merchantUserId	No	String	User Id on your end
callbacks.success	Yes	String	Your url to redirect user when payment finished successfully.
callbacks.failed	Yes	String	Your url to redirect user if the payment is not successful.
Example for request body:

{

"currency": "usdt",
"network": "trx",
"amount": 2.3,
"orderId": "ORDER_ID_IN_YOUR_DB",
"callbacks": {
"success": "https://yourwebsite_suucess_page_to_redirect_user",
"failed": "https://yourwebsite_failed_page_to_redirect_user"
}
}
Payment webhook

# Here is a curl example
curl \
-X POST YOUR_MERCHANT_WEBHOOK_ADDRESS \
--header 'Content-Type: application/json' \
--data '{
    "_id": "63decf79eec0e4414cc6823b",
    "type": "payment",
    "status": "success",
    "modifiedAt": "2023-02-04T22:00:49.599Z",
    "amount": 2.3,
    "amountUsd": 2.29,
    "amountPaid": 2.3,
    "amountPaidUsd": 2.29,
    "currency": "usdt",
    "network": "trx"
}'
                
[POST] YOUR_MERCHANT_WEBHOOK_ADDRESS

When any changes happened to you transaction 1xGate will call your webhook address which provided by you while creating a merchant with following data:


🚨 It's highly recommended after getting the webhook for a transaction, call the get transaction endpoint to make sure the webhook is valid see here.



Request example :
{
    "_id": "63decf79eec0e4414cc6823b",
    "type": "payment",
    "status": "success",
    "modifiedAt": "2023-02-04T22:00:49.599Z",
    "amount": 2.3,
    "amountUsd": 2.29,
    "amountPaid": 2.3,
    "amountPaidUsd": 2.29,
    "currency": "usdt",
    "network": "trx"
}
                
QUERY PARAMETERS
Field	Type	Description
_id	String	The payment id
type	String	The type of the webhook. For payments it is 'payment'
status	String	This field represents status of the transaction. You can check here for a complete description
modifiedAt	Date	Last date time (ISO-8601) which this payment status has been modified
amount	Double	The amount of the transaction (in the currency of the transaction)
amountPaid	Double	The Amount that the user actually paid (in the currency of the transaction)
amountUsd	Double	The amount of the transaction in USD
amountPaidUsd	Double	The Amount that the user actually paid in USD
currency	String	The currency of the transaction
network	String	The network of the currency in which the funds paid
Get payment

# Here is a curl example
curl --location 'https://api.1xgate.com/v1/integrations/payments/66ad24125dececa8b9a16bf3' \
--header 'x-integration-key: YOUR_MERCHANT_API_KEY'
                
[GET] /v1/integrations/payments/:paymentId

After receiving a webhook, or at any time you want to get the latest update for a transaction, you can call the endpoint above. The :paymentId corresponds to the _id that was generated when the payment was created.

You can check the payment status from the response. If the status is:

created The user was not redirected to the 1xGate payment link or closed the tab.
pending The user has selected the currency and network, and 1xGate is waiting for the funds to be sent.
success The payment was completed successfully.
partiallyPaid The user sent funds but not the full amount. For example, if the required payment was 10 USDT but only 9 USDT was received. You can compare amount and amountPaid to decide whether to mark the transaction as failed or request the user to send the remaining funds.
expired The payment was not completed within the required time, and the allocated address (with memo, for certain networks) expired.
error An error occurred during the payment process.


Result example :
{
    "data": {
        "_id": "66ad24125dececa8b9a16bf3",
        "userId": "65df57b2eb8c7a6c2b6be09d",
        "ip": "3.13.171.120",
        "merchantId": "66ad200c8950036604490832",
        "currency": "usdt",
        "network": "trx",
        "address": "TK7sq71kJcMu7AvZPZbUmFGg4psfXVNew5",
        "memo": null,
        "amount": 2.3,
        "amountUsd": 2.29839,
        "amountPaid": 0,
        "amountPaidUsd": 0,
        "priceUnit": 0.9993,
        "wage": 0,
        "orderId": "ORDER_ID_IN_YOUR_DB",
        "type": "external",
        "status": "pending",
        "description": null,
        "merchantUserId": null,
        "merchantUserEmail": null,
        "transactions": [],
        "inUse": true,
        "expireAt": "2024-08-02T18:52:53.368Z",
        "hook": {
            "response": {
                "success": false,
                "code": {
                    "status": 404
                },
                "at": "2024-08-02T18:47:01.369Z",
                "body": null
            },
            "retires": 24,
            "lastHookedAt": "2024-08-02T18:47:01.369Z"
        },
        "callbacks": {
            "success": "https://google.com/q=success",
            "failed": "https://google.com/q=failed"
        },
        "meta": {},
        "updatedAt": "2024-08-02T18:47:01.314Z",
        "createdAt": "2024-08-02T18:23:14.867Z",
    }
}
                
Get Currencies and Networks

# Here is a curl example
curl --location 'https://api.1xgate.com/v1/settings/payment'
                
[GET] /v1/settings/payment

You can get available currencies and their supported network:


Some of these currencies and their networks may temporarily be deactivated or new currencies and networks will be added to our supported list.


Also, you can use 1xGate assets to show currency or network icons to your customers. You can use the same endpoint for icons https://api.1xgate.com/icons/trx.png to fetching each icon.


Result example :
{
    "data": [
        {
            "currency": "usdt",
            "name": "Tether",
            "icon": "/icons/usdt.png",
            "decimal": 6,
            "type": "crypto",
            "wallet": {},
            "networks": [
                {
                    "network": "trx",
                    "name": "trc20",
                    "icon": "/icons/trx.png",
                    "color": "ffffff",
                    "min": 2,
                    "max": 15000,
                    "waitingTime": "5m"
                }
            ]
        },
        {
            "currency": "ltc",
            "name": "Litecoin",
            "icon": "/icons/ltc.png",
            "decimal": 8,
            "type": "crypto",
            "wallet": {},
            "networks": [
                {
                    "network": "ltc",
                    "name": "ltc",
                    "icon": "/icons/ltc.png",
                    "color": "ffffff",
                    "min": 2,
                    "max": 15000,
                    "waitingTime": "5m"
                }
            ]
        },
        {
            "currency": "btc",
            "name": "Bitcoin",
            "icon": "/icons/btc.png",
            "decimal": 8,
            "type": "crypto",
            "wallet": {},
            "networks": [
                {
                    "network": "btc",
                    "name": "btc",
                    "icon": "/icons/btc.png",
                    "color": "ffffff",
                    "min": 2,
                    "max": 15000,
                    "waitingTime": "15m"
                }
            ]
        },
        {
            "currency": "eth",
            "name": "Bitcoin",
            "icon": "/icons/eth.png",
            "decimal": 8,
            "type": "crypto",
            "wallet": {},
            "networks": [
                {
                    "network": "eth",
                    "name": "eth",
                    "icon": "/icons/eth.png",
                    "color": "ffffff",
                    "min": 2,
                    "max": 15000,
                    "waitingTime": "5m"
                }
            ]
        }
    ]
}
                
Statuses
You can check the payment status from the response. If the status is:

created The user was not redirected to the 1xGate payment link or closed the tab.
pending The user has selected the currency and network, and 1xGate is waiting for the funds to be sent.
success The payment was completed successfully.
partiallyPaid The user sent funds but not the full amount. For example, if the required payment was 10 USDT but only 9 USDT was received. You can compare amount and amountPaid to decide whether to mark the transaction as failed or request the user to send the remaining funds.
expired The payment was not completed within the required time, and the allocated address (with memo, for certain networks) expired.
error An error occurred during the payment process.