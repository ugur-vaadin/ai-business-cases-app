package com.vaadin.demo.nordicsupply.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import tools.jackson.databind.json.JsonMapper;

/**
 * The files of one pack under {@code classpath:data/<pack>/}: the declaration, the schema text the model reads, the
 * SQL scripts and the demo documents. One bean knows where the pack lives so that no view resolves classpath paths of
 * its own; the declaration and the schema text are read once at start-up.
 */
public class PackData {

    private static final Logger LOG = LoggerFactory.getLogger(PackData.class);

    /** Where a pack keeps the demo messages the claim form reads. */
    private static final String DOCUMENTS_FOLDER = "documents/emails";

    private final String pack;
    private final AppDeclaration declaration;
    private final String schemaText;
    private final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    public PackData(String pack) {
        this.pack = pack;
        try {
            var json = read("sql/app.json");
            this.declaration = AppDeclaration.from(JsonMapper.builder().build().readTree(json));
            this.schemaText = read("sql/ai-schema-h2.txt");
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Pack '" + pack + "' is not on the classpath under data/" + pack + "/. Run scripts/sync-data.sh "
                            + pack,
                    e);
        }
    }

    public String pack() {
        return pack;
    }

    public AppDeclaration declaration() {
        return declaration;
    }

    /** The schema text the model reads, plus today's date, which the text refers to and the application supplies. */
    public String schemaTextForToday() {
        return schemaText + "\nToday is " + LocalDate.now() + ".\n";
    }

    public String read(String relative) throws IOException {
        return resource(relative).getContentAsString(StandardCharsets.UTF_8);
    }

    public Resource resource(String relative) {
        return resolver.getResource("classpath:data/" + pack + "/" + relative);
    }

    /** The demo documents (customer messages) the claim form reads, sorted by file name. */
    public List<Resource> documents() {
        try {
            var found = resolver.getResources("classpath:data/" + pack + "/" + DOCUMENTS_FOLDER + "/*");
            var list = new ArrayList<>(List.of(found));
            list.sort((a, b) -> String.valueOf(a.getFilename()).compareTo(String.valueOf(b.getFilename())));
            return list;
        } catch (IOException e) {
            LOG.warn("pack {} declares documents but none could be listed", pack, e);
            return List.of();
        }
    }
}
