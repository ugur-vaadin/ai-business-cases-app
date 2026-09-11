package com.vaadin.demo.nordicsupply.ui.components;

import java.io.Serializable;

/**
 * What a view tells a demo user about itself: a title, a one-line teaser for the corner card, and the full text in
 * Markdown for the expanded dialog. The user only sees one view at a time, so each readme describes that view as
 * if it were the whole application.
 */
public record Readme(String title, String teaser, String body) implements Serializable {}
