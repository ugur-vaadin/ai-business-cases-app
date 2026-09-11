package com.vaadin.demo.nordicsupply.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.listbox.ListBox;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.popover.Popover;
import com.vaadin.flow.component.popover.PopoverPosition;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;

import com.vaadin.demo.nordicsupply.config.PackData;
import com.vaadin.demo.nordicsupply.session.CurrentUser;
import com.vaadin.demo.nordicsupply.ui.components.HasReadme;
import com.vaadin.demo.nordicsupply.ui.components.Readme;
import com.vaadin.demo.nordicsupply.ui.components.ReadmePopup;
import com.vaadin.demo.nordicsupply.ui.views.ActivityLogView;
import com.vaadin.demo.nordicsupply.ui.views.ClaimsView;
import com.vaadin.demo.nordicsupply.ui.views.CustomersView;
import com.vaadin.demo.nordicsupply.ui.views.HomeView;
import com.vaadin.demo.nordicsupply.ui.views.InsightsView;
import com.vaadin.demo.nordicsupply.ui.views.OrdersView;
import com.vaadin.demo.nordicsupply.ui.views.ProductsView;

/**
 * The app shell: a left rail with the Nordic Supply wordmark, the back-office views (Home, Orders, Customers,
 * Products, Claims, Insights), and at the bottom the Activity log, Help (opens the current view's readme) and the
 * signed-in user, who can be switched for the demo. No top bar: each view carries its own heading. Bulk change stays
 * reachable at /bulk but is not listed.
 */
public class MainLayout extends AppLayout implements AfterNavigationObserver {

    private final Button help;
    private Readme currentReadme;

    public MainLayout(PackData pack, CurrentUser currentUser) {
        var decl = pack.declaration();
        setPrimarySection(Section.DRAWER);

        var mark = new Div(VaadinIcon.ANGLE_LEFT.create());
        mark.addClassName("mark");
        var name = new Span(decl.company());
        name.addClassName("name");
        var wordmark = new Div(mark, name);
        wordmark.addClassName("wordmark");

        var nav = new SideNav();
        nav.addItem(new SideNavItem("Home", HomeView.class, VaadinIcon.HOME.create()));
        nav.addItem(new SideNavItem("Orders", OrdersView.class, VaadinIcon.TRUCK.create()));
        nav.addItem(new SideNavItem("Customers", CustomersView.class, VaadinIcon.USER.create()));
        nav.addItem(new SideNavItem("Products", ProductsView.class, VaadinIcon.PACKAGE.create()));
        nav.addItem(new SideNavItem("Claims", ClaimsView.class, VaadinIcon.ENVELOPE.create()));
        nav.addItem(new SideNavItem("Insights", InsightsView.class, VaadinIcon.BAR_CHART.create()));

        var bottomNav = new SideNav();
        bottomNav.addItem(new SideNavItem("Activity log", ActivityLogView.class, VaadinIcon.CLIPBOARD_TEXT.create()));
        help = new Button("Help", VaadinIcon.LIFEBUOY.create(), e -> {
            if (currentReadme != null) {
                ReadmePopup.open(currentReadme);
            }
        });
        help.addClassName("rail-button");
        help.addThemeVariants(ButtonVariant.TERTIARY);
        var user = new Button(currentUser.get().name(), VaadinIcon.USER_CARD.create());
        user.addClassName("rail-button");
        user.addThemeVariants(ButtonVariant.TERTIARY);
        user.setTooltipText("Signed in as " + currentUser.get() + " · click to switch");
        var users = new ListBox<CurrentUser.SignedInUser>();
        users.setItems(currentUser.all());
        users.setValue(currentUser.get());
        users.addValueChangeListener(e -> {
            if (e.getValue() != null && !e.getValue().equals(currentUser.get())) {
                currentUser.set(e.getValue());
                getUI().ifPresent(ui -> ui.getPage().reload());
            }
        });
        var switcher = new Popover(users);
        switcher.setTarget(user);
        switcher.setPosition(PopoverPosition.END_BOTTOM);
        switcher.setModal(true, false);
        var bottom = new Div(bottomNav, help, user);
        bottom.addClassName("rail-bottom");

        var rail = new VerticalLayout(wordmark, nav, bottom);
        rail.addClassName("app-rail");
        rail.setPadding(false);
        rail.setSpacing(false);
        rail.setSizeFull();
        rail.add(switcher);
        addToDrawer(rail);
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        Component content = getContent();
        currentReadme = content instanceof HasReadme r ? r.readme() : null;
        help.setEnabled(currentReadme != null);
    }
}
