package org.keycloak.fedcm;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.managers.AuthenticationManager.AuthResult;

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
            fedCMEndpoints.put("login_url", server + "realms/" + realm); // TODO this is the endpoint where a user can login to IdP
            fedCMEndpoints.put("disconnect_endpoint", server + "realms/" + realm + '/' + factoryID +  "/disconnect");
            fedCMEndpoints.put("branding", "");
        }
        else
            fedCMEndpoints.put("error", "Sec-Fetch-Dest header is not set to webidentity");
        return fedCMEndpoints;
    }

    @GET
    @Path("accounts")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, List<Map<String, Object>>> fetchAccountsList(@HeaderParam("Sec-Fetch-Dest") String secFetchDest) {
        // todo get picture URL
        // todo store somewhere approved_clients
        // todo what to put in domain_hints
        Map<String, List<Map<String, Object>>> accList = new HashMap<>();
        List<Map<String, Object>> accounts = new ArrayList<>();
        Map<String, Object> account = new HashMap<>();


        AuthResult authResult = (new AuthenticationManager()).authenticateIdentityCookie(session, session.getContext().getRealm());
        UserModel userModel = authResult.getUser();
        account.put("id", userModel.getId());
        account.put("given_name", userModel.getFirstName());
        account.put("name", userModel.getFirstName() + ' ' + userModel.getLastName());
        account.put("email", userModel.getEmail());
        account.put("approved_clients", new ArrayList<String>() {{ add("123"); add("456"); }});
        account.put("login_hints", new ArrayList<String>() {{ add(userModel.getEmail()); }});

        accounts.add(account);
        accList.put("accounts", accounts);

        return accList;
    }

    @GET
    @Path("client_metadata")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> fetchClientMetadata(@HeaderParam("Sec-Fetch-Dest") String secFetchDest, @QueryParam("client_id") int client_id) {
        Map<String, Object> metadata = new HashMap<>();
        if(client_id == 123) {
            metadata.put("privacy_policy_url", "https://www.seznam.cz/");
            metadata.put("terms_of_service_url", "https://www.seznam.cz/");
        }
        else {
            metadata.put("privacy_policy_url", "https://www.google.com/");
            metadata.put("terms_of_service_url", "https://www.google.com/");
        }
        return metadata;
    }

    @POST
    @Path("id_assert")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, String> fetchIdentityAssertion(@HeaderParam("Sec-Fetch-Dest") String secFetchDest, @FormParam("account_id") String account_id, @FormParam("client_id") String client_id, @FormParam("nonce") String nonce, @QueryParam("disclosure_text_shown") boolean disclosure_text_shown) {

        Map<String, String> token = new HashMap<>();
        token.put("token", "not-a-real-token-just-yet-123");
        return token;
    }

    @POST
    @Path("disconnect")
    @Produces(MediaType.APPLICATION_JSON)
    public Response disconnect(@HeaderParam("Sec-Fetch-Dest") String secFetchDest, @HeaderParam("Origin") String client_origin, @FormParam("client_id") String client_id, @FormParam("account_hint") String account_hint) {
        AuthResult authResult = (new AuthenticationManager()).authenticateIdentityCookie(session, session.getContext().getRealm());
        UserModel userModel = authResult.getUser();
        Map<String, String> id = new HashMap<>();
        id.put("account_id", userModel.getId());

        Response.ResponseBuilder rb = Response.ok(id);
        Response rtn_id = rb.header("Access-Control-Allow-Origin", client_origin)
                .type(MediaType.APPLICATION_JSON)
                .header("Access-Control-Allow-Credentials", true)
                .build();

        return rtn_id;
    }

    @Override
    public void close() {

    }
}
