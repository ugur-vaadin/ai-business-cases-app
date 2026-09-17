package com.vaadin.demo.nordicsupply;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * The Nordic Supply AI demo application. Its data comes as a "pack": a generated dataset of CSV files with a schema
 * script, a plain-text description of that schema for the model, a small declaration ({@code sql/app.json}) and a few
 * demo documents. The pack named by {@code app.pack} is loaded into H2 at start-up.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@Push
@StyleSheet("theme.css")
@StyleSheet("shell.css")
@StyleSheet("insights.css")
@StyleSheet("components.css")
public class Application implements AppShellConfigurator {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
