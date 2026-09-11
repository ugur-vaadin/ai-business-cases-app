package com.vaadin.demo.nordicsupply.ui.views;

import java.time.LocalDate;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import com.vaadin.demo.nordicsupply.config.PackData;
import com.vaadin.demo.nordicsupply.data.OrderDeskStats;
import com.vaadin.demo.nordicsupply.ui.MainLayout;
import com.vaadin.demo.nordicsupply.ui.components.HasReadme;
import com.vaadin.demo.nordicsupply.ui.components.PageHeading;
import com.vaadin.demo.nordicsupply.ui.components.Readme;
import com.vaadin.demo.nordicsupply.ui.components.ReadmePopup;
import com.vaadin.demo.nordicsupply.util.Formats;

/** The landing page of the order desk: a few live numbers and where to go next. */
@Route(value = "", layout = MainLayout.class)
@PageTitle("Home")
public class HomeView extends VerticalLayout implements HasReadme {

    static final Readme README = new Readme(
            "Home",
            "The order desk at a glance, with live numbers from the database.",
            """
            ## What this page does

            This is the front door of the **Nordic Supply** order desk, the back-office application an outdoor-equipment
            wholesaler runs: orders from retail customers, shipments, claims and the product catalogue.

            The tiles are live counts from the database. The rail on the left opens the usual screens: **Orders**,
            **Customers** and **Products** are plain read-only lists; **Claims** is where a customer's message becomes a
            claim; **Insights** is the self-service dashboard where you ask questions in your own words.

            ## About this demo

            Everything you see is generated, fictional data. You are signed in as a Nordic Supply employee (switch the
            user at the bottom of the rail); your changes stay in this demo session. **Help** opens the readme of the
            page you are on.
            """);

    public HomeView(OrderDeskStats stats, PackData pack) {
        setSizeFull();
        setPadding(true);
        addClassName("page");
        var heading = new PageHeading(pack.declaration().company(), "Order desk");
        var intro = new Paragraph("Live numbers from the order desk. Pick a screen from the rail, "
                + "or open Insights to ask a question of your own.");
        intro.addClassName("intro");

        var tiles = new Div();
        tiles.addClassName("tiles");
        stats.tiles(LocalDate.now()).forEach(t -> tiles.add(tile(t.label(), t.value(), t.detail())));
        add(heading, intro, tiles, new ReadmePopup(README, pack.declaration().company()));
    }

    @Override
    public Readme readme() {
        return README;
    }

    private static Div tile(String label, int value, String detail) {
        var k = new Span(label);
        k.addClassName("k");
        var v = new Span(Formats.number(value));
        v.addClassName("v");
        var d = new Span(detail);
        d.addClassName("d");
        var tile = new Div(k, v, d);
        tile.addClassName("tile");
        return tile;
    }
}
