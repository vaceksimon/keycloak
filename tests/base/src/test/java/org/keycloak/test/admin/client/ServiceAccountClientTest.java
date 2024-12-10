/*
 * Copyright 2024 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.keycloak.test.admin.client;

import java.util.stream.Collectors;

import com.nimbusds.oauth2.sdk.TokenResponse;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.keycloak.common.constants.ServiceAccountConstants;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.test.framework.annotations.InjectClient;
import org.keycloak.test.framework.annotations.InjectRealm;
import org.keycloak.test.framework.annotations.KeycloakIntegrationTest;
import org.keycloak.test.framework.oauth.nimbus.OAuthClient;
import org.keycloak.test.framework.realm.ClientConfig;
import org.keycloak.test.framework.realm.ClientConfigBuilder;
import org.keycloak.test.framework.realm.ManagedClient;
import org.keycloak.test.framework.realm.ManagedRealm;
import org.keycloak.test.utils.admin.ApiUtil;

/**
 *
 * @author rmartinc
 */
@KeycloakIntegrationTest
public class ServiceAccountClientTest {

    private static final String CLIENT_ID = "service-account-client";

    OAuthClient oAuthClient;

    @InjectClient(config = ServiceAccountClientConfig.class)
    ManagedClient client;

    @InjectRealm
    ManagedRealm realm;

    @Test
    public void testServiceAccountEnableDisable() throws Exception {
        ClientScopeRepresentation serviceAccountScope = ApiUtil.findClientScopeByName(
                realm.admin(), ServiceAccountConstants.SERVICE_ACCOUNT_SCOPE).toRepresentation();

        /* todo what is supposed to be the OAuth client. Are we supposed to use it in combination with another client or not?
                an OAuthClient creates a client in a realm, but you can't access the client inside
                you can't set oauth's clientid to a different client's ID

         */

        MatcherAssert.assertThat(oAuthClient.managedClient().admin().getDefaultClientScopes().stream().map(ClientScopeRepresentation::getName).collect(Collectors.toList()),
                Matchers.hasItem("service_account"));

        // perform a login and check the claims are there
        TokenResponse response = oAuthClient.clientCredentialGrant();
        AccessToken accessToken = oauth.verifyToken(response.toSuccessResponse().getTokens().getAccessToken());
        Assert.assertEquals("service-account-client", accessToken.getOtherClaims().get(ServiceAccountConstants.CLIENT_ID));
        Assert.assertNotNull(accessToken.getOtherClaims().get(ServiceAccountConstants.CLIENT_HOST));
        Assert.assertNotNull(accessToken.getOtherClaims().get(ServiceAccountConstants.CLIENT_ADDRESS));

        // update the client to remove service account
        clientRep.setServiceAccountsEnabled(Boolean.FALSE);
        client.update(clientRep);
        MatcherAssert.assertThat(client.getDefaultClientScopes().stream().map(ClientScopeRepresentation::getName).collect(Collectors.toList()),
                Matchers.not(Matchers.hasItem(ServiceAccountConstants.SERVICE_ACCOUNT_SCOPE)));
        response = oauth.doClientCredentialsGrantAccessTokenRequest("password");
        Assert.assertEquals("unauthorized_client", response.getError());

        // re-enable sevice accounts
        clientRep.setServiceAccountsEnabled(Boolean.TRUE);
        client.update(clientRep);
        MatcherAssert.assertThat(client.getDefaultClientScopes().stream().map(ClientScopeRepresentation::getName).collect(Collectors.toList()),
                Matchers.hasItem(ServiceAccountConstants.SERVICE_ACCOUNT_SCOPE));
        response = oauth.doClientCredentialsGrantAccessTokenRequest("password");
        accessToken = oauth.verifyToken(response.getAccessToken());
        Assert.assertEquals("service-account-client", accessToken.getOtherClaims().get(ServiceAccountConstants.CLIENT_ID));
        Assert.assertNotNull(accessToken.getOtherClaims().get(ServiceAccountConstants.CLIENT_HOST));
        Assert.assertNotNull(accessToken.getOtherClaims().get(ServiceAccountConstants.CLIENT_ADDRESS));

        // assign the scope as optional
        client.removeDefaultClientScope(serviceAccountScope.getId());
        client.addOptionalClientScope(serviceAccountScope.getId());

        // re-enable service accounts, should assign the scope again as default
        clientRep.setServiceAccountsEnabled(Boolean.TRUE);
        client.update(clientRep);
        MatcherAssert.assertThat(client.getDefaultClientScopes().stream().map(ClientScopeRepresentation::getName).collect(Collectors.toList()),
                Matchers.hasItem(ServiceAccountConstants.SERVICE_ACCOUNT_SCOPE));
        response = oauth.doClientCredentialsGrantAccessTokenRequest("password");
        accessToken = oauth.verifyToken(response.getAccessToken());
        Assert.assertEquals("service-account-client", accessToken.getOtherClaims().get(ServiceAccountConstants.CLIENT_ID));
        Assert.assertNotNull(accessToken.getOtherClaims().get(ServiceAccountConstants.CLIENT_HOST));
        Assert.assertNotNull(accessToken.getOtherClaims().get(ServiceAccountConstants.CLIENT_ADDRESS));

        // remove the service account and client credentials should fail
        UserRepresentation serviceAccountUser = client.getServiceAccountUser();
        testRealmResource().users().delete(serviceAccountUser.getId());
        response = oauth.doClientCredentialsGrantAccessTokenRequest("password");
        Assert.assertEquals("invalid_request", response.getError());
    }

    public static class ServiceAccountClientConfig implements ClientConfig {

        @Override
        public ClientConfigBuilder configure(ClientConfigBuilder client) {
            return client
                    .clientId(CLIENT_ID)
                    .protocol("openid-connect")
                    .secret("password")
                    .serviceAccounts(true)
                    .authenticatorType("client-secret")
                    .publicClient(false);
        }
        // todo cleanup oauth client
    }
}
