package com.vaadin.demo.nordicsupply.ui.components;

import com.vaadin.flow.component.select.Select;

import com.vaadin.demo.nordicsupply.session.Scope;
import com.vaadin.demo.nordicsupply.session.Scopes;

/**
 * Narrows a dashboard to one country inside what the signed-in user is allowed to see. The choice is a view of the
 * user's own data, never a way to reach more of it: the database applies the user's countries on top of whatever is
 * chosen here.
 */
public class CountryFilter extends Select<Scope> {

    private static final String WIDTH = "14rem";

    /**
     * @param scope the countries the user may see
     * @param current the choice to show, usually the whole scope
     */
    public CountryFilter(Scope scope, Scope current) {
        setItems(Scopes.filtersWithin(scope));
        setItemLabelGenerator(Scope::label);
        setValue(current);
        setLabel(null);
        addClassName("country-filter");
        setWidth(WIDTH);
    }
}
