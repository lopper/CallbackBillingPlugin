/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.phonegap.plugin.billing.plugin;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.phonegap.plugin.billing.plugin.Consts.PurchaseState;

/**
 * Security-related methods. For a secure implementation, all of this code
 * should be implemented on a server that communicates with the
 * application on the device. For the sake of simplicity and clarity of this
 * example, this code is included here and is executed on the device. If you
 * must verify the purchases on the phone, you should obfuscate this code to
 * make it harder for an attacker to replace the code with stubs that treat all
 * purchases as verified.
 */
public class Security {
    private static final String TAG = "Security";

    private static final String KEY_FACTORY_ALGORITHM = "RSA";
    private static final String SIGNATURE_ALGORITHM = "SHA1withRSA";
    private static final SecureRandom RANDOM = new SecureRandom();
    /**
     * This keeps track of the nonces that we generated and sent to the
     * server.  We need to keep track of these until we get back the purchase
     * state and send a confirmation message back to Android Market. If we are
     * killed and lose this list of nonces, it is not fatal. Android Market will
     * send us a new "notify" message and we will re-generate a new nonce.
     * This has to be "static" so that the {@link BillingReceiver} can
     * check if a nonce exists.
     */
    private static HashSet<Long> sKnownNonces = new HashSet<Long>();
    /**
     * A class to hold the verified purchase information.
     */
    public static class VerifiedPurchase {
        public PurchaseState purchaseState;
        public String notificationId;
        public String productId;
        public String orderId;
        public long purchaseTime;
        public String developerPayload;

        public VerifiedPurchase(PurchaseState purchaseState, String notificationId,
                String productId, String orderId, long purchaseTime, String developerPayload) {
            this.purchaseState = purchaseState;
            this.notificationId = notificationId;
            this.productId = productId;
            this.orderId = orderId;
            this.purchaseTime = purchaseTime;
            this.developerPayload = developerPayload;
        }
    }

    /** Generates a nonce (a random number used once). 
     * @throws IOException 
     * @throws JSONException */
    public static long generateNonce(){

        long nonce = RANDOM.nextLong();
        sKnownNonces.add(nonce);
        return nonce;
   
    }

    public static void removeNonce(long nonce) {
        sKnownNonces.remove(nonce);
    }

    public static boolean isNonceKnown(long nonce) {
        return sKnownNonces.contains(nonce);
    }

    private static ArrayList<VerifiedPurchase> ToVerifiedPurchases(String jsonOrdersArray)
    {
         JSONArray jTransactionsArray = null;
         int numTransactions = 0;
         try {
        	 jTransactionsArray = new JSONArray(jsonOrdersArray);

             if (jTransactionsArray != null) {
                 numTransactions = jTransactionsArray.length();
             }
         } catch (JSONException e) {
             return null;
         }


         ArrayList<VerifiedPurchase> purchases = new ArrayList<VerifiedPurchase>();
         try {
             for (int i = 0; i < numTransactions; i++) {
                 JSONObject jElement = jTransactionsArray.getJSONObject(i);
                 int response = jElement.getInt("purchaseState");
                 PurchaseState purchaseState = PurchaseState.valueOf(response);
                 String productId = jElement.getString("productId");
                 String packageName = jElement.getString("packageName");
                 long purchaseTime = jElement.getLong("purchaseTime");
                 String orderId = jElement.optString("orderId", "");
                 String notifyId = null;
                 if (jElement.has("notificationId")) {
                     notifyId = jElement.getString("notificationId");
                 }
                 String developerPayload = jElement.optString("developerPayload", null);

                 purchases.add(new VerifiedPurchase(purchaseState, notifyId, productId,
                         orderId, purchaseTime, developerPayload));
             }
         } catch (JSONException e) {
             Log.e(TAG, "JSON exception: ", e);
             return null;
         }
         
         return purchases;
    }
    
    /**
     * Verifies that the data was signed with the given signature, and returns
     * the list of verified purchases. The data is in JSON format and contains
     * a nonce (number used once) that we generated and that was signed
     * (as part of the whole data string) with a private key. The data also
     * contains the {@link PurchaseState} and product ID of the purchase.
     * In the general case, there can be an array of purchase transactions
     * because there may be delays in processing the purchase on the backend
     * and then several purchases can be batched together.
     * @param signedData the signed JSON string (signed, not encrypted)
     * @param signature the signature for the data, signed with the private key
     */
    public static void verifyPurchase(final BillingService service, final int startId, final String signedData, final String signature) {

    	
        if (signedData == null) {
            Log.e(TAG, "data is null");
            return;
        }

		new Thread(new Runnable() {
		    public void run() {
		        String response;
		        JSONObject jObject;
		        long nonce = 0L;
				try {
					jObject = new JSONObject(signedData);
					nonce = jObject.optLong("nonce");
			        if (!Security.isNonceKnown(nonce)) {
			            return;
			        }
					response = WebHelper.Post(Consts.VERIFY_PURCHASE_URL, new NameValuePair[]{
					    	new BasicNameValuePair("signedData", signedData),
					    	new BasicNameValuePair("signature", signature),
					    });
					service.verifyPurchases(startId, ToVerifiedPurchases(response));
					removeNonce(nonce);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
		        
		    }
		}).start();
        

    }


}
