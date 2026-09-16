package com.vaadin.demo.nordicsupply.session;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** The scopes a user can have, and how a narrower one relates to a wider one. */
class ScopesTest {

    @Test
    void onlyTheFourBusinessRolesAreRoleScopes() {
        assertThat(Scopes.roleScope("fi_ee")).contains(Scopes.FI_EE);
        assertThat(Scopes.roleScope("fi")).isEmpty();
    }

    @Test
    void theFilterOffersTheScopeAndThenItsCountries() {
        assertThat(Scopes.filtersWithin(Scopes.FI_EE)).containsExactly(Scopes.FI_EE, Scopes.FI, Scopes.EE);
        assertThat(Scopes.filtersWithin(Scopes.ALL)).hasSize(7).first().isEqualTo(Scopes.ALL);
    }

    @Test
    void aScopeIsWithinAnotherWhenEveryCountryIs() {
        assertThat(Scopes.isWithin(Scopes.FI, Scopes.FI_EE)).isTrue();
        assertThat(Scopes.isWithin(Scopes.SE, Scopes.FI_EE)).isFalse();
        assertThat(Scopes.isWithin(Scopes.FI_EE, Scopes.ALL)).isTrue();
    }
}
