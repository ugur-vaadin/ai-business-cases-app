# AI business cases — the Nordic Supply demo application

A Vaadin 25.3 + Spring Boot application that shows the Vaadin AI components doing real work in an application people
recognise: the back office of **Nordic Supply**, a fictional outdoor-equipment wholesaler. The dataset (thousands of
customers, orders, products and shipments, months of history) ships with the application under
`src/main/resources/data/nordic_supply`. Desktop only.

| View | Route | What it is |
|---|---|---|
| Home | `/` | Live numbers from the order desk; the front door |
| Orders, Customers, Products | `/orders`, `/customers`, `/products` | Ordinary read-only lists with search, so the app looks like the application a client runs |
| Claims | `/claims` | Message to claim: the form on the left, the customer message and its chat in the right-hand panel; the model fills the form through live lookups and the form's own validation decides what is accepted |
| Insights | `/insights` | The self-service dashboard: a question becomes a grid or chart widget; the selected widget's chat opens in the sidebar (which can be hidden, leaving the prompt as a footer); Save Dashboard keeps what is on screen, sizes and order included |
| Activity log | `/activity` | Who asked what, what the model could see, what it answered, what a person decided |
| Bulk change | `/bulk` (unlisted) | Supervised bulk price change: the model proposes a change per row, a person reviews and applies, and the batch can be undone |

Every view carries a readme (`ReadmePopup`): a card in the bottom-right corner that opens a dialog, written for a
demo user who only sees that one view. The first card of a session shows the demo's welcome text; **Help** in the
rail opens the current view's readme. The signed-in user is switched at the bottom of the rail.

The dataset is a "pack": CSV files with a schema script, a plain-text description of the schema for the model
(`sql/ai-schema-h2.txt`), a small declaration (`sql/app.json`, which supplies the suggested questions) and a few demo
customer messages. It is loaded into an in-memory H2 database at start-up. The AI connects as the pack's `ai_reader`
account, which can see the exposed tables and views only; the model receives the schema text and today's date, never
a row.

## Run

Requirements: Java 21+, Maven, a Vaadin subscription key for the AI extensions (`~/.vaadin/proKey`), an OpenAI API
key.

```bash
export OPENAI_API_KEY=...                 # never in a file under this repository
mvn spring-boot:run                       # http://localhost:8080 (PORT=8093 mvn ... for another port)
```

The default model is `gpt-5.6-luna` (`AI_MODEL` overrides it). GPT-5.6 models reason by default and the chat-completions
endpoint only allows function tools with `reasoning_effort` set to `none`, which `application.properties` does. Without a
key the application starts; every AI turn then fails with an error notification that names the cause, and the failure is
recorded in the activity log.

Local model: `mvn spring-boot:run -Dspring-boot.run.profiles=local` with an OpenAI-compatible server on port 11434
(Ollama) and a model that handles tool calling; `AI_BASE_URL` and `AI_MODEL` override the defaults.

The theme uses `light-dark()`, `color-mix()` and relative `oklch()` colours; use a current Chrome, Edge or Firefox, or
Safari 17.5 or newer.

## Tests

```bash
mvn verify                                # unit tests plus the Spotless format check
```

The tests cover the parts worth pinning without a browser or a model key: the reply parser, the read-only query
guard, the claim numbering and the claim save, the price change and its undo, the entity mappings, the pack's
declaration, and the numbers behind the Home tiles. No test calls the model.

## Where things are

| | |
|---|---|
| `ui/MainLayout` | the rail: wordmark, views, Activity log, Help, user switcher |
| `ui/views/InsightsView`, `ui/components/InsightWidget`, `ui/components/QueryPanel` | Insights: the prompt panel, the dashboard, the sidebar; one widget = one controller, one chat, one orchestrator |
| `ui/views/HomeView`, `ui/views/OrdersView`, `ui/views/CustomersView`, `ui/views/ProductsView`, `ui/components/DataTable` | the back-office views and the lazy, searchable grid they share |
| `ui/views/ClaimsView`, `data/ClaimService` | case 2: the claim form on a `BeanValidationBinder<Claim>`, and the write behind Save |
| `ui/views/BulkChangeView`, `data/PriceChangeService` | case 3: the catalogue, the reviewed proposals, the dated price rows and the undo |
| `ui/components/ReadmePopup`, `ui/components/Readme`, `ui/components/HasReadme` | the per-view readme card and dialog |
| `ai/JdbcDatabaseProvider` | schema text + today's date; SELECT-only queries on the read-only account with a query timeout; the grid controller pages them itself. The only place with SQL in it |
| `domain/*` | the JPA entities of the pack's schema (`SalesOrder` is `orders`; ORDER is a JPQL keyword) and their enums |
| `data/*Repository` | Spring Data repositories, one per aggregate; every list reads through a derived method or a JPQL projection record |
| `data/SavedWidgets`, `data/ActivityLog`, `ai/TurnLogger` | persistence of kept widgets; the audit trail; the hooks that fill it |
| `src/main/resources/data/nordic_supply` | the dataset: CSVs, schema, the schema text the model reads, the demo messages |
| `config/DataConfig`, `config/PackData`, `config/AppDeclaration`, `config/AiConfig` | loading the pack into H2, reading its declaration, the model |
| `db/migration/V1__app_tables.sql`, `V2__sequences.sql` | Flyway owns the tables the application writes and the sequences its inserts draw from |
| `util/Formats`, `util/Errors` | how numbers and dates are written on screen; what a failure says |
| `META-INF/resources/theme.css`, `shell.css`, `insights.css`, `components.css` | the theme: gold accent, Barlow display face, light and dark following the OS |

## What the model sees

* The dashboard sends the schema text and today's date, never a row; queries run on the read-only account.
* The claim form sends the customer's message and the visible field values.
* Employees are visible to the model by name and role only, through the `staff` view.
* Every AI turn and every decision lands in `activity_log`, with the model name and the token counts.

## Known simplifications

* No real login: the user is chosen at the bottom of the rail from the pack's staff; saved widgets and log rows belong to it.
* Widgets are stored only when Save Dashboard is pressed; a saved chart that was stored without a configuration restores with a default column look.
* The claim form works from the message text; the photo attachment is not wired to the chat's upload.
* The bulk change writes dated price rows (reason "Supplier increase", currency EUR); undo covers the last batch of the session.
* Hibernate maps the pack's schema read-only (`ddl-auto=none`); Flyway owns the application's own tables and the sequences its inserts draw from.
* All user-facing text is English and hard-coded; there is no translation layer.
* Layouts are designed for 1280px and wider.
