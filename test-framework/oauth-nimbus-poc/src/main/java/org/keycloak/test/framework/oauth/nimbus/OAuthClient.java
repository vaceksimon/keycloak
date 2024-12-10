package org.keycloak.test.framework.oauth.nimbus;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.oauth2.sdk.AuthorizationCodeGrant;
import com.nimbusds.oauth2.sdk.AuthorizationGrant;
import com.nimbusds.oauth2.sdk.AuthorizationRequest;
import com.nimbusds.oauth2.sdk.ClientCredentialsGrant;
import com.nimbusds.oauth2.sdk.GeneralException;
import com.nimbusds.oauth2.sdk.ResourceOwnerPasswordCredentialsGrant;
import com.nimbusds.oauth2.sdk.ResponseType;
import com.nimbusds.oauth2.sdk.TokenIntrospectionRequest;
import com.nimbusds.oauth2.sdk.TokenIntrospectionResponse;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.TokenResponse;
import com.nimbusds.oauth2.sdk.TokenRevocationRequest;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.oauth2.sdk.id.State;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;
import jakarta.ws.rs.core.Response;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.keycloak.TokenVerifier;
import org.keycloak.common.VerificationException;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.AsymmetricSignatureVerifierContext;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.crypto.ServerECDSASignatureVerifierContext;
import org.keycloak.jose.jwk.JSONWebKeySet;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jwk.JWKParser;
import org.keycloak.jose.jwk.OKPPublicJWK;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.test.framework.realm.ClientConfig;
import org.keycloak.test.framework.realm.ClientConfigBuilder;
import org.keycloak.test.framework.realm.ManagedClient;
import org.keycloak.test.framework.realm.ManagedRealm;
import org.keycloak.test.framework.util.ApiUtil;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OAuthClient {

    private final ManagedRealm realm;
    private final ManagedClient client;
    private final OAuthCallbackServer callbackServer;
    private final HttpClient httpClient;
    private OIDCProviderMetadata oidcProviderMetadata;
    private JSONWebKeySet publicKey;

    public OAuthClient(ManagedRealm realm, ClientConfig clientConfig, HttpClient httpClient) {
        this.realm = realm;
        this.client = registerClient(clientConfig);
        this.httpClient = httpClient;
        this.callbackServer = new OAuthCallbackServer();
    }

    private ManagedClient registerClient(ClientConfig clientConfig) {
        ClientRepresentation clientRepresentation = clientConfig.configure(ClientConfigBuilder.create()).build();
        Response response = realm.admin().clients().create(clientRepresentation);
        String id = ApiUtil.handleCreatedResponse(response);
        clientRepresentation.setId(id);

        return new ManagedClient(clientRepresentation, realm.admin().clients().get(id));
    }

    public TokenResponse clientCredentialGrant() throws IOException, GeneralException {
        AuthorizationGrant clientGrant = new ClientCredentialsGrant();
        ClientAuthentication clientAuthentication = getClientAuthentication();
        URI tokenEndpoint = getOIDCProviderMetadata().getTokenEndpointURI();

        TokenRequest tokenRequest = new TokenRequest(tokenEndpoint, clientAuthentication, clientGrant);
        return TokenResponse.parse(tokenRequest.toHTTPRequest().send());
    }

    public TokenResponse resourceOwnerCredentialGrant(String username, String password) {
        try {
            ResourceOwnerPasswordCredentialsGrant credentialsGrant = new ResourceOwnerPasswordCredentialsGrant(username, new Secret(password));
            ClientAuthentication clientAuthentication = getClientAuthentication();
            URI tokenEndpoint = getOIDCProviderMetadata().getTokenEndpointURI();

            TokenRequest tokenRequest = new TokenRequest(tokenEndpoint, clientAuthentication, credentialsGrant);
            return TokenResponse.parse(tokenRequest.toHTTPRequest().send());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public TokenResponse tokenRequest(AuthorizationCode authorizationCode) throws IOException, GeneralException {
        AuthorizationGrant grant = new AuthorizationCodeGrant(authorizationCode, callbackServer.getRedirectionUri());
        ClientAuthentication clientAuthentication = getClientAuthentication();
        URI tokenEndpoint = getOIDCProviderMetadata().getTokenEndpointURI();

        TokenRequest tokenRequest = new TokenRequest(tokenEndpoint, clientAuthentication, grant);
        return TokenResponse.parse(tokenRequest.toHTTPRequest().send());
    }

    public TokenIntrospectionResponse introspection(AccessToken accessToken) throws IOException, GeneralException {
        ClientAuthentication clientAuthentication = getClientAuthentication();
        URI introspectionEndpoint = getOIDCProviderMetadata().getIntrospectionEndpointURI();

        TokenIntrospectionRequest introspectionRequest = new TokenIntrospectionRequest(introspectionEndpoint, clientAuthentication, accessToken);
        return TokenIntrospectionResponse.parse(introspectionRequest.toHTTPRequest().send());
    }

    public HTTPResponse revokeAccessToken(AccessToken token) throws GeneralException, IOException {
        URI revocationEndpoint = getOIDCProviderMetadata().getRevocationEndpointURI();
        TokenRevocationRequest revocationRequest = new TokenRevocationRequest(revocationEndpoint, getClientAuthentication(), token);
        return revocationRequest.toHTTPRequest().send();
    }

    public URL authorizationRequest() {
        try {
            URI authorizationEndpoint = getOIDCProviderMetadata().getAuthorizationEndpointURI();
            State state = new State();
            ClientID clientID = new ClientID(client.getClientId());

            AuthorizationRequest authorizationRequest = new AuthorizationRequest.Builder(new ResponseType(ResponseType.Value.CODE), clientID)
                    .state(state)
                    .redirectionURI(callbackServer.getRedirectionUri())
                    .endpointURI(authorizationEndpoint)
                    .build();

            return authorizationRequest.toURI().toURL();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public List<URI> getCallbacks() {
        return callbackServer.getCallbacks();
    }

    public void close() {
        client.admin().remove();
        callbackServer.close();
    }

    private ClientAuthentication getClientAuthentication() {
        ClientID clientID = new ClientID(client.getClientId());
        Secret clientSecret = new Secret(client.getSecret());
        return new ClientSecretBasic(clientID, clientSecret);
    }

    private OIDCProviderMetadata getOIDCProviderMetadata() throws GeneralException, IOException {
        if (oidcProviderMetadata == null) {
            Issuer issuer = new Issuer(realm.getBaseUrl());
            oidcProviderMetadata = OIDCProviderMetadata.resolve(issuer);
        }
        return oidcProviderMetadata;
    }

    public ManagedClient managedClient() {
        return client;
    }

    public <T extends JsonWebToken> T verifyToken(String token, Class<T> clazz) {
        try {
            TokenVerifier<T> verifier = TokenVerifier.create(token, clazz);
            String kid = verifier.getHeader().getKeyId();
            String algorithm = verifier.getHeader().getAlgorithm().name();
            KeyWrapper key = getRealmPublicKey(algorithm, kid);
            AsymmetricSignatureVerifierContext verifierContext;
            switch (algorithm) {
                case Algorithm.ES256:
                case Algorithm.ES384:
                case Algorithm.ES512:
                    verifierContext = new ServerECDSASignatureVerifierContext(key);
                    break;
                default:
                    verifierContext = new AsymmetricSignatureVerifierContext(key);
            }
            verifier.verifierContext(verifierContext);
            verifier.verify();
            return verifier.getToken();
        } catch (VerificationException e) {
            throw new RuntimeException("Failed to decode token", e);
        }
    }


    private KeyWrapper getRealmPublicKey(String algorithm, String kid) {
        boolean loadedKeysFromServer = false;
        JSONWebKeySet jsonWebKeySet = publicKey;
        if (jsonWebKeySet == null) {
            jsonWebKeySet = getRealmKeys();
            publicKey = jsonWebKeySet;
            loadedKeysFromServer = true;
        }

        KeyWrapper key = findKey(jsonWebKeySet, algorithm, kid);

        if (key == null && !loadedKeysFromServer) {
            jsonWebKeySet = getRealmKeys();
            publicKey = jsonWebKeySet;

            key = findKey(jsonWebKeySet, algorithm, kid);
        }

        if (key == null) {
            throw new RuntimeException("Public key for realm:" + realm.getName() + ", algorithm: " + algorithm + " not found");
        }

        return key;
    }

    private JSONWebKeySet getRealmKeys() {
        String certUrl = realm.getBaseUrl() + "/protocol/openid-connect/certs";

        HttpGet request = new HttpGet(certUrl);
        try {
            HttpResponse response = httpClient.execute(request);
            response.getEntity().
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
//        try (CloseableHttpClient client = httpClient.get()) {
//            return SimpleHttpDefault.doGet(certUrl, client).asJson(JSONWebKeySet.class);
//        } catch (IOException e) {
//            throw new RuntimeException("Failed to retrieve keys", e);
//        }
    }

    private KeyWrapper findKey(JSONWebKeySet jsonWebKeySet, String algorithm, String kid) {
        for (JWK k : jsonWebKeySet.getKeys()) {
            if (k.getKeyId().equals(kid) && k.getAlgorithm().equals(algorithm)) {
                PublicKey publicKey = JWKParser.create(k).toPublicKey();

                KeyWrapper key = new KeyWrapper();
                key.setKid(k.getKeyId());
                key.setAlgorithm(k.getAlgorithm());
                if (k.getOtherClaims().get(OKPPublicJWK.CRV) != null) {
                    key.setCurve((String) k.getOtherClaims().get(OKPPublicJWK.CRV));
                }
                key.setPublicKey(publicKey);
                key.setUse(KeyUse.SIG);

                return key;
            }
        }
        return null;
    }
}
