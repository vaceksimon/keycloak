package org.keycloak.fedcm;

import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.AuthenticationSessionManager;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.managers.AuthenticationManager.AuthResult;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.sessions.RootAuthenticationSessionModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FedCMProvider implements RealmResourceProvider {

    private final String factoryID;
    private final KeycloakSession session;

    public FedCMProvider(KeycloakSession session, String factoryID) {
        this.session = session;
        this.factoryID = factoryID;
    }

    @Override
    public Object getResource() {
        return this;
    }

    @GET
    @Path("config.json")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> fetchConfigFile(@HeaderParam("Sec-Fetch-Dest") String secFetchDest) {
        Map<String, Object> fedCMEndpoints = new HashMap<>();
        if (secFetchDest.equals("webidentity")) {

            String server = session.getContext().getAuthServerUrl().toString();
            String realm = session.getContext().getRealm().getName();

            fedCMEndpoints.put("accounts_endpoint", server + "realms/" + realm + '/' + factoryID +  "/accounts");
            fedCMEndpoints.put("client_metadata_endpoint", server + "realms/" + realm + '/' + factoryID +  "/client_metadata");
            fedCMEndpoints.put("id_assertion_endpoint", server + "realms/" + realm + '/' + factoryID +  "/id_assert");
            fedCMEndpoints.put("login_url", server + "realms/" + realm + "/account");
            fedCMEndpoints.put("disconnect_endpoint", server + "realms/" + realm + '/' + factoryID +  "/disconnect");

            Map<String, Object> branding = new HashMap<>();
            branding.put("background_color", "#3CC1E6");
            branding.put("color", "black");
            ArrayList<Map<String, Object>> icons = new ArrayList<>();
            icons.add(new HashMap<>() {{
                put("url", "https://raw.githubusercontent.com/keycloak/keycloak-misc/main/archive/logo/keycloak_icon_32px.png");
                put("size", 32);
            }});
            branding.put("icons", icons);
            branding.put("name", "Keycloak");

            fedCMEndpoints.put("branding", branding);
        }
        else
            fedCMEndpoints.put("error", "Sec-Fetch-Dest header is not set to webidentity");
        return fedCMEndpoints;
    }

    @GET
    @Path("accounts")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> fetchAccountsList(@HeaderParam("Sec-Fetch-Dest") String secFetchDest) {
        // todo get picture URL
        // todo store somewhere approved_clients
        // todo what to put in domain_hints

        Map<String, Object> accList = new HashMap<>();
        if (secFetchDest.equals("webidentity")) {
            List<Map<String, Object>> accounts = new ArrayList<>();
            Map<String, Object> account = new HashMap<>();

            AuthResult authResult = (new AuthenticationManager()).authenticateIdentityCookie(session, session.getContext().getRealm());
            UserModel userModel = authResult.getUser();
            account.put("id", userModel.getId());
            account.put("given_name", userModel.getFirstName());
            account.put("name", userModel.getFirstName() + ' ' + userModel.getLastName());
            account.put("email", userModel.getEmail());
            account.put("picture", "https://lh3.googleusercontent.com/a-/ALV-UjW7uuw5LcTap5TgMk9aHVl1RMm9HS79mMfhBJd8JlzkJSRIesrbkM69LF7Pw4yMYmVVUhUrl249NPd39iUQeknOT6sGRn4Y1CPbzuhHR4dIAlWT9kn69BVs2_pJc7vHjpHE50g_O6e-b_Pb80ukBweKcz9tMbO--QdcukwsS1SQJzo-ULqTri-HTfXb_wzUuwfnnIlNqWdQF70yOYBcqeqaYCrGo960dhEIzch75P1mAdvRz-ZkjbR-6_JuZZCDLz1BKuFz53AzchzaUEMtZ2um2R8Kkym1eO0zmnhN5FH0vr9BTLYZabl63TVrcbqfr8bHDx3OeH4lTdeF5JjRESVWNxermDJDlYalme3FGY6veShcsEBrDJIR2TPqLF087G2-vqk-_5RVaHAY8Y8QQ6EdrSIYc4UenJUj7tO1MTOntu5x2F8JK7MN_SvrAfK9xfz51cPGoMB8i9jCokwDjmazj8kHJ23xJ1tniq8jLe9qfS7Wl-q5Ik3bGWwZNJ7EC1zw5t7aMZwk3WPzi2jBko5ybsqzYfU-jyY6VeB8Zf_G7lb1v9bTMlrQ1yr624O5ISn71Y4i2PHH8FiEskvGvT5PhgW3iQJhQ8cEATMUu2BAh69XrldkJkbZhW8DnWeayqcem_6C6tq0DfbBMnL2KrFD5i-OzbF4vjRIMkpztNhz2yYwOATBx8q4fH8hrP2dujO98h7yUL8qawCPwbe06X-Dg635HytL5v9QpeIJnyS1Sw3QqD09FNIJ6ku5C3fJN0YfJPJ5BK-SB5LGqVks5imLzwH9wgDASV14IUx2byLlT7NE1U-jtY71hNXLqyLUv6RT2Hw3oObzlKciyRk63xp6B8QXHUnC7AZdBXE3OcQNJA3hZZIUn6f9AoD3nMc0iZx7IUZmv6a2WR1dbySTBU5Tw-OMF4LyJiPkKjs6n68gmtKixPjM7jvkYpWBRbXkrYso5GNbPH5NKgPKstW9FmRVru5M=s96-c-rg-br100");
            account.put("approved_clients", new ArrayList<String>() {{
                add("123");
                add("456");
                add("example");
            }});
            account.put("login_hints", new ArrayList<String>() {{
                add(userModel.getEmail());
            }});

            accounts.add(account);

            // TODO DELETE second and madeup account just for demonstration purposes
            Map<String, Object> account2 = new HashMap<>();
            account2.put("id", "1111");
            account2.put("given_name", "Radek");
            account2.put("name", "Radek Burget");
            account2.put("email", "burgetr@fit.vut.cz");
            account2.put("picture", "https://www.fit.vut.cz/person-photo/10467/?transparent=1");
            account2.put("approved_clients", new ArrayList<String>() {{
                add("123");
                add("456");
            }});
            account2.put("login_hints", new ArrayList<String>() {{
                add("burgetr@fit.vut.cz");
            }});
            accounts.add(account2);


            accList.put("accounts", accounts);
        }
        else {
            accList.put("error", "Sec-Fetch-Dest header is not set to webidentity");
        }
        return accList;
    }

    @GET
    @Path("client_metadata")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> fetchClientMetadata(@HeaderParam("Sec-Fetch-Dest") String secFetchDest, @QueryParam("client_id") int client_id) {
        Map<String, Object> metadata = new HashMap<>();
        if(secFetchDest.equals("webidentity")) {
            if (client_id == 123) {
                metadata.put("privacy_policy_url", "https://www.seznam.cz/");
                metadata.put("terms_of_service_url", "https://www.seznam.cz/");
            } else {
                metadata.put("privacy_policy_url", "https://www.google.com/");
                metadata.put("terms_of_service_url", "https://www.google.com/");
            }
        }
        else {
            metadata.put("error", "Sec-Fetch-Dest header is not set to webidentity");
        }
        return metadata;
    }

    @POST
    @Path("id_assert")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> fetchIdentityAssertion(@HeaderParam("Sec-Fetch-Dest") String secFetchDest, @FormParam("account_id") String account_id, @FormParam("client_id") String client_id, @FormParam("nonce") String nonce, @QueryParam("disclosure_text_shown") boolean disclosure_text_shown) {
        Map<String, Object> token = new HashMap<>();
        if (secFetchDest.equals("webidentity")) {

            RealmModel realm = session.getContext().getRealm();

            ClientModel client = realm.getClientByClientId(client_id);

            // todo is this the right way to do it?
            session.getContext().setClient(client);

            // todo might not be necessary and could be supplied null istead of eventbuilder
            EventBuilder eventBuilder = new EventBuilder(realm, session);

            AuthResult authResult = new AuthenticationManager().authenticateIdentityCookie(session, realm);
            UserModel user = authResult.getUser();
            UserSessionModel userSession = authResult.getSession();


            // creating a ClientAuthenticatedSession used for ClientSessionContext
            AuthenticationSessionManager authSessionManager = new AuthenticationSessionManager(session);
            RootAuthenticationSessionModel rootAuthSession = authSessionManager.createAuthenticationSession(realm, false);
            // AuthenticationSessionAdapter
            AuthenticationSessionModel authSession = rootAuthSession.createAuthenticationSession(client);
            authSession.setAuthenticatedUser(user);
            authSession.setClientNote(OIDCLoginProtocol.SCOPE_PARAM, "openid profile email");

            AuthenticationManager.setClientScopesInSession(authSession);


            // DefaultClientSessionContext
            ClientSessionContext clientSessionCtx = TokenManager.attachAuthenticationSession(session, userSession, authSession);
            // 1) set nonce
            // todo should be set in the AuthenticationSessionModel - OIDCLoginProtocol:authenticated():230
            authSession.setClientNote(OIDCLoginProtocol.NONCE_PARAM, nonce);
            clientSessionCtx.setAttribute(OIDCLoginProtocol.NONCE_PARAM, nonce);


            TokenManager tokenManager = new TokenManager();


            TokenManager.AccessTokenResponseBuilder accessTokenResponseBuilder = tokenManager.responseBuilder(realm, client, eventBuilder, session, userSession, clientSessionCtx);
            accessTokenResponseBuilder.generateAccessToken();
            accessTokenResponseBuilder.generateIDToken();

            // 2) id token gets filled with data by building
            token.put("token", accessTokenResponseBuilder.build().getIdToken());
        }
        else {
            token.put("error", "Sec-Fetch-Dest header is not set to webidentity");
        }
        return token;
    }

    @POST
    @Path("disconnect")
    @Produces(MediaType.APPLICATION_JSON)
    public Response disconnect(@HeaderParam("Sec-Fetch-Dest") String secFetchDest, @HeaderParam("Origin") String client_origin, @FormParam("client_id") String client_id, @FormParam("account_hint") String account_hint) {
        if (secFetchDest.equals("webidentity")) {
            Map<String, String> id = new HashMap<>();
            AuthResult authResult = (new AuthenticationManager()).authenticateIdentityCookie(session, session.getContext().getRealm());
            UserModel userModel = authResult.getUser();
            id.put("account_id", userModel.getId());

            Response.ResponseBuilder rb = Response.ok(id);
            return rb.header("Access-Control-Allow-Origin", client_origin)
                    .type(MediaType.APPLICATION_JSON)
                    .header("Access-Control-Allow-Credentials", true)
                    .build();

        }
        else {
            return Response.serverError().build();
        }
    }

    @Override
    public void close() {

    }
}
