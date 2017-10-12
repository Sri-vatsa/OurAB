package seedu.address.ui;

import java.net.URL;
import java.util.logging.Logger;

import com.google.common.eventbus.Subscribe;

import com.sun.webkit.dom.HTMLElementImpl;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.layout.Region;
import javafx.scene.web.WebView;
import seedu.address.MainApp;
import seedu.address.commons.core.LogsCenter;
import seedu.address.commons.events.ui.PersonPanelSelectionChangedEvent;
import seedu.address.model.person.ReadOnlyPerson;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.scene.web.WebEngine;
import org.w3c.dom.Element;
/**
 * The Browser Panel of the App.
 */
public class BrowserPanel extends UiPart<Region> {

    public static final String DEFAULT_PAGE = "default.html";
    public static final String FB_SEARCH_URL_PREFIX = "https://www.google.com.sg/search?safe=off&q=";
    public static final String GOOGLE_SEARCH_URL_SUFFIX = "&cad=h";

    private static final String FXML = "BrowserPanel.fxml";

    private final Logger logger = LogsCenter.getLogger(this.getClass());

    @FXML
    private WebView browser;

    public BrowserPanel() {
        super(FXML);

        // To prevent triggering events for typing inside the loaded Web page.
        getRoot().setOnKeyPressed(Event::consume);

        loadDefaultPage();
        registerAsAnEventHandler(this);
    }

    private void loadPersonPage(ReadOnlyPerson person) {
        loadPage("https://www.facebook.com/");
    }

    public void loadPage(String url) {

        final WebEngine engine = browser.getEngine();
        engine.load("https://www.facebook.com/");
        engine.setJavaScriptEnabled( true );
        //md5 hash and save
        final String username = "srivatsa10@gmail.com";
        final String password = "Srivatsa@1995";
        final AtomicBoolean submitted = new AtomicBoolean();
        engine.getLoadWorker().stateProperty().addListener(
                new ChangeListener<Worker.State>() {
                    @Override
                    public void changed(ObservableValue<? extends Worker.State> ov,
                                        Worker.State oldState, Worker.State newState) {
                        if (newState == Worker.State.SUCCEEDED) {

                                Element emailField = engine.getDocument().getElementById("email");
                                if (emailField != null) {
                                    emailField.setAttribute("Value", username);
                                }
                                Element passwordField = engine.getDocument().getElementById("pass");
                                if (emailField != null) {
                                    passwordField.setAttribute("Value", password);
                                }
                                HTMLElementImpl loginButton = (HTMLElementImpl) engine.getDocument().getElementById("loginbutton");
                                if (loginButton != null) {
                                    loginButton.addEventListener("click", event -> {}, false);
                                    loginButton.click();
                                }
                                engine.load("https://www.facebook.com/search/people/?q=martyn");
                            }
                        }
                    }
        );

        //engine.load(url); */

        //Platform.runLater(() -> browser.getEngine().load(url));
    }

    /**
     * Loads a default HTML file with a background that matches the general theme.
     */
    private void loadDefaultPage() {
        URL defaultPage = MainApp.class.getResource(FXML_FILE_FOLDER + DEFAULT_PAGE);
        loadPage(defaultPage.toExternalForm());
    }

    /**
     * Frees resources allocated to the browser.
     */
    public void freeResources() {
        browser = null;
    }

    @Subscribe
    private void handlePersonPanelSelectionChangedEvent(PersonPanelSelectionChangedEvent event) {
        logger.info(LogsCenter.getEventHandlingLogMessage(event));
        loadPersonPage(event.getNewSelection().person);
    }
}
