/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.tests.admin;

import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.UserConfigBuilder;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

@KeycloakIntegrationTest
public class UsersTest {

    @InjectRealm(lifecycle = LifeCycle.METHOD)
    ManagedRealm realm;

    private void createUser(String username, String password, String firstName, String lastName, String email) {
        UserRepresentation user = UserConfigBuilder.create()
                .username(username)
                .password(password)
                .name(firstName, lastName)
                .email(email)
                .enabled(true)
                .build();
        realm.admin().users().create(user);
    }

    @Test
    public void searchUserWithWildcards() {
        createUser("User", "password", "firstName", "lastName", "user@example.com");

        assertThat(realm.admin().users().search("Use%", null, null), hasSize(0));
        assertThat(realm.admin().users().search("Use_", null, null), hasSize(0));
        assertThat(realm.admin().users().search("Us_r", null, null), hasSize(0));
        assertThat(realm.admin().users().search("Use", null, null), hasSize(1));
        assertThat(realm.admin().users().search("Use*", null, null), hasSize(1));
        assertThat(realm.admin().users().search("Us*e", null, null), hasSize(1));
    }

    @Test
    public void searchUserDefaultSettings() throws Exception {
        createUser("User", "password", "firstName", "lastName", "user@example.com");

        assertCaseInsensitiveSearch();
    }

    @Test
    public void searchUserMatchUsersCount() {
        createUser("john.doe", "password", "John", "Doe Smith", "john.doe@keycloak.org");
        String search = "jo do";

        assertThat(realm.admin().users().count(search), is(1));
        List<UserRepresentation> users = realm.admin().users().search(search, null, null);
        assertThat(users, hasSize(1));
        assertThat(users.get(0).getUsername(), is("john.doe"));
    }

    /**
     * https://issues.redhat.com/browse/KEYCLOAK-15146
     */
    @Test
    public void findUsersByEmailVerifiedStatus() {
        UserRepresentation user1 = UserConfigBuilder.create()
                .username("user1")
                .password("password")
                .name("user1FirstName", "user1LastName")
                .email("user1@example.com")
                .emailVerified()
                .enabled(true)
                .build();
        realm.admin().users().create(user1);

        UserRepresentation user2 = UserConfigBuilder.create()
                .username("user2")
                .password("password")
                .name("user2FirstName", "user2LastName")
                .email("user2@example.com")
                .enabled(true)
                .build();
        realm.admin().users().create(user2);

        boolean emailVerified;
        emailVerified = true;
        List<UserRepresentation> usersEmailVerified = realm.admin().users().search(null, null, null, null, emailVerified, null, null, null, true);
        assertThat(usersEmailVerified, is(not(empty())));
        assertThat(usersEmailVerified.get(0).getUsername(), is("user1"));

        emailVerified = false;
        List<UserRepresentation> usersEmailNotVerified = realm.admin().users().search(null, null, null, null, emailVerified, null, null, null, true);
        assertThat(usersEmailNotVerified, is(not(empty())));
        assertThat(usersEmailNotVerified.get(0).getUsername(), is("user2"));
    }

    /**
     * https://issues.redhat.com/browse/KEYCLOAK-15146
     */
    @Test
    public void countUsersByEmailVerifiedStatus() {
        UserRepresentation user1 = UserConfigBuilder.create()
                .username("user1")
                .password("password")
                .name("user1FirstName", "user1LastName")
                .email("user1@example.com")
                .emailVerified()
                .enabled(true)
                .build();
        realm.admin().users().create(user1);

        UserRepresentation user2 = UserConfigBuilder.create()
                .username("user2")
                .password("password")
                .name("user2FirstName", "user2LastName")
                .email("user2@example.com")
                .enabled(true)
                .build();
        realm.admin().users().create(user2);

        UserRepresentation user3 = UserConfigBuilder.create()
                .username("user3")
                .password("password")
                .name("user3FirstName", "user3LastName")
                .email("user3@example.com")
                .emailVerified()
                .enabled(true)
                .build();
        realm.admin().users().create(user3);

        boolean emailVerified;
        emailVerified = true;
        assertThat(realm.admin().users().countEmailVerified(emailVerified), is(2));
        assertThat(realm.admin().users().count(null,null,null,emailVerified,null), is(2));

        emailVerified = false;
        assertThat(realm.admin().users().countEmailVerified(emailVerified), is(1));
        assertThat(realm.admin().users().count(null,null,null,emailVerified,null), is(1));
    }

    @Test
    public void countUsersWithViewPermission() {
        createUser("user1", "password", "user1FirstName", "user1LastName", "user1@example.com");
        createUser("user2", "password", "user2FirstName", "user2LastName", "user2@example.com");
        assertThat(realm.admin().users().count(), is(2));
    }

    @Test
    public void countUsersBySearchWithViewPermission() {
        UserRepresentation user1 = UserConfigBuilder.create()
                .username("user1")
                .password("password")
                .name("user1FirstName", "user1LastName")
                .email("user1@example.com")
                .emailVerified()
                .enabled(true)
                .build();
        realm.admin().users().create(user1);

        UserRepresentation user2 = UserConfigBuilder.create()
                .username("user2")
                .password("password")
                .name("user2FirstName", "user2LastName")
                .email("user2@example.com")
                .enabled(true)
                .build();
        realm.admin().users().create(user2);

        UserRepresentation user3 = UserConfigBuilder.create()
                .username("user3")
                .password("password")
                .name("user3FirstName", "user3LastName")
                .email("user3@example.com")
                .emailVerified()
                .enabled(true)
                .build();
        realm.admin().users().create(user3);

        // Prefix search count
        assertSearchMatchesCount(realm.admin(), "user", 3);
        assertSearchMatchesCount(realm.admin(), "user*", 3);
        assertSearchMatchesCount(realm.admin(), "er", 0);
        assertSearchMatchesCount(realm.admin(), "", 3);
        assertSearchMatchesCount(realm.admin(), "*", 3);
        assertSearchMatchesCount(realm.admin(), "user2FirstName", 1);
        assertSearchMatchesCount(realm.admin(), "user2First", 1);
        assertSearchMatchesCount(realm.admin(), "user2First*", 1);
        assertSearchMatchesCount(realm.admin(), "user1@example", 1);
        assertSearchMatchesCount(realm.admin(), "user1@example*", 1);
        assertSearchMatchesCount(realm.admin(), null, 3);

        // Infix search count
        assertSearchMatchesCount(realm.admin(), "*user*", 3);
        assertSearchMatchesCount(realm.admin(), "**", 3);
        assertSearchMatchesCount(realm.admin(), "*foobar*", 0);
        assertSearchMatchesCount(realm.admin(), "*LastName*", 3);
        assertSearchMatchesCount(realm.admin(), "*FirstName*", 3);
        assertSearchMatchesCount(realm.admin(), "*@example.com*", 3);

        // Exact search count
        assertSearchMatchesCount(realm.admin(), "\"user1\"", 1);
        assertSearchMatchesCount(realm.admin(), "\"1\"", 0);
        assertSearchMatchesCount(realm.admin(), "\"\"", 0);
        assertSearchMatchesCount(realm.admin(), "\"user1FirstName\"", 1);
        assertSearchMatchesCount(realm.admin(), "\"user1LastName\"", 1);
        assertSearchMatchesCount(realm.admin(), "\"user1@example.com\"", 1);
    }

    @Test
    public void countUsersByFiltersWithViewPermission() {
        createUser("user1", "password", "user1FirstName", "user1LastName", "user1@example.com");
        createUser("user2", "password", "user2FirstName", "user2LastName", "user2@example.com");
        //search username
        assertThat(realm.admin().users().count(null, null, null, "user"), is(2));
        assertThat(realm.admin().users().count(null, null, null, "user1"), is(1));
        assertThat(realm.admin().users().count(null, null, null, "notExisting"), is(0));
        assertThat(realm.admin().users().count(null, null, null, ""), is(2));
        //search first name
        assertThat(realm.admin().users().count(null, "FirstName", null, null), is(2));
        assertThat(realm.admin().users().count(null, "user2FirstName", null, null), is(1));
        assertThat(realm.admin().users().count(null, "notExisting", null, null), is(0));
        assertThat(realm.admin().users().count(null, "", null, null), is(2));
        //search last name
        assertThat(realm.admin().users().count("LastName", null, null, null), is(2));
        assertThat(realm.admin().users().count("user2LastName", null, null, null), is(1));
        assertThat(realm.admin().users().count("notExisting", null, null, null), is(0));
        assertThat(realm.admin().users().count("", null, null, null), is(2));
        //search in email
        assertThat(realm.admin().users().count(null, null, "@example.com", null), is(2));
        assertThat(realm.admin().users().count(null, null, "user1@example.com", null), is(1));
        assertThat(realm.admin().users().count(null, null, "user1@test.com", null), is(0));
        assertThat(realm.admin().users().count(null, null, "", null), is(2));
        //search for combinations
        assertThat(realm.admin().users().count("LastName", "FirstName", null, null), is(2));
        assertThat(realm.admin().users().count("user1LastName", "FirstName", null, null), is(1));
        assertThat(realm.admin().users().count("user1LastName", "", null, null), is(1));
        assertThat(realm.admin().users().count("LastName", "", null, null), is(2));
        assertThat(realm.admin().users().count("LastName", "", null, null), is(2));
        assertThat(realm.admin().users().count(null, null, "@example.com", "user"), is(2));
        //search not specified (defaults to simply /count)
        assertThat(realm.admin().users().count(null, null, null, null), is(2));
        assertThat(realm.admin().users().count("", "", "", ""), is(2));
    }

    private void assertSearchMatchesCount(RealmResource realmResource, String search, Integer expectedCount) {
        Integer count = realmResource.users().count(search);
        assertThat(count, is(expectedCount));
        assertThat(realmResource.users().search(search, null, null), hasSize(count));
    }

    private void assertCaseInsensitiveSearch() {
        // not-exact case-insensitive search
        assertThat(realm.admin().users().search("user"), hasSize(1));
        assertThat(realm.admin().users().search("User"), hasSize(1));
        assertThat(realm.admin().users().search("USER"), hasSize(1));
        assertThat(realm.admin().users().search("Use"), hasSize(1));

        // exact case-insensitive search
        assertThat(realm.admin().users().search("user", true), hasSize(1));
        assertThat(realm.admin().users().search("User", true), hasSize(1));
        assertThat(realm.admin().users().search("USER", true), hasSize(1));
        assertThat(realm.admin().users().search("Use", true), hasSize(0));
    }
}
