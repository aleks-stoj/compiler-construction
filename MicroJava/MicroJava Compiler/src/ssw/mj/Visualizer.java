package ssw.mj;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import ssw.mj.scanner.Token;

import java.awt.*;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

public class Visualizer {
  // Config Flags
  private static final boolean debug = false;
  private static final String visualizerUrl = "https://ssw.jku.at/General/Staff/Weninger/Teaching/CB/Visualization/v1_0";
  // private static final String visualizerUrl = "http://localhost:5173/General/Staff/Weninger/Teaching/CB/Visualization/v1_0";

  public static final Gson gson = debug ? new GsonBuilder().setPrettyPrinting().create() : new Gson();

  // Remember when the browser was last opened, to ensure that we
  // don't open a huge number of tabs
  private static LocalDateTime browserLastOpened;

  public static class VisualizerException extends RuntimeException {
    public VisualizerException(String message) {
      super("[Recorder]: " + message);
    }
  }

  private interface WithDescription {
    String getDescription();
  }

  public enum VisualizationType implements WithDescription {
    SCANNER_V1 {
      @Override
      public String getDescription() {
        return "Scanner Visualization";
      }
    },
    PARSER_V1 {
      @Override
      public String getDescription() {
        return "Parser Visualization";
      }
    }
  }

  private record ScannerPayload(List<Token> receivedTokens, List<Token> expectedTokens) {
    public String toJson() {
      return gson.toJson(this);
    }
  }

  private record ParserPayload(List<Recorder.RecorderTraceEntry> traces) {
    public String toJson() {
      return gson.toJson(this);
    }
  }

  /**
   * Transforms scanner tokens to our visualizer format and ingests them to the desired method
   *
   * @param receivedTokens The tokens received from the student's scanner
   * @param expectedTokens The tokens expected by the test case
   */
  public static void createScannerVisualization(String sourcecode,
                                                List<Token> receivedTokens,
                                                List<Token> expectedTokens,
                                                boolean openInBrowserAfterwards) {
    String payload = new ScannerPayload(receivedTokens, expectedTokens).toJson();
    VisualizerIngestData visData = new VisualizerIngestData(VisualizationType.SCANNER_V1, sourcecode, payload);

    Path path = ingestFile(visData);
    if (openInBrowserAfterwards) {
      openBrowser(path.toUri().toString());
    }
  }

  /**
   * Get the secret recorder from the parser and ingest the tokens to the visualizer
   */
  public static void createParserVisualization(String sourcecode, boolean openInBrowserAfterwards) {
    Recorder recorder = getRecorderFromParserUsingReflection();

    // If we don't have a recorder, we can't create a visualization
    if (recorder == null) return;

    String payload = new ParserPayload(recorder.getTrace()).toJson();

    VisualizerIngestData visData = new VisualizerIngestData(VisualizationType.PARSER_V1, sourcecode, payload);

    Path path = ingestFile(visData);
    if (openInBrowserAfterwards) {
      openBrowser(path.toUri().toString());
    }
  }

  // Data to be sent to the ingest endpoint
  private record VisualizerIngestData(VisualizationType type, String sourcecode, String payload) {
    public String toJson() {
      return gson.toJson(this);
    }
  }

  /**
   * Create a visualization file
   *
   * @param data The visualization data
   * @return The path at which the visualization file has been created
   */
  private static Path ingestFile(VisualizerIngestData data) {
    String html = "<!DOCTYPE html>\n" +
            "<html lang=\"en\">\n" +
            "  <head>\n" +
            "    <meta charset=\"UTF-8\">\n" +
            "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
            "    <title>Compiler Visualization</title>\n" +
            "  </head>\n" +
            "  <body>\n" +
            "    <div id=\"app\"></div>\n" +
            "\n" +
            "    <script id=\"__DATA__\" type=\"application/json\">\n" +
            "      " + (data.toJson()) + "\n" + // Inject the visualization data
            "    </script>\n" +
            "\n" +
            "    <script type=\"text/javascript\">\n" +
            "      let data = JSON.parse(document.getElementById('__DATA__').textContent);\n" +
            "      let app = document.getElementById('app');\n" +
            "\n" +
            "      let iframe = document.createElement('iframe');\n" +
            "      iframe.src = '" + visualizerUrl + "';\n" +
            "      iframe.style.position = 'fixed';\n" +
            "      iframe.style.top = 0;\n" +
            "      iframe.style.left = 0;\n" +
            "      iframe.style.right = 0;\n" +
            "      iframe.style.bottom = 0;\n" +
            "      iframe.style.border = 0;\n" +
            "      iframe.style.width = '100%';\n" +
            "      iframe.style.height = '100%';\n" +
            "\n" +
            "      app.appendChild(iframe);\n" +
            "      \n" +
            "      iframe.addEventListener('load', () => {\n" +
            "        setTimeout(() => {\n" +
            "          iframe.contentWindow.postMessage({\n" +
            "            type: 'boot',\n" +
            "            data\n" +
            "          }, '*');\n" +
            "        }, 100);\n" +
            "      });\n" +
            "    </script>\n" +
            "  </body>\n" +
            "</html>";

    try {
      // Get a temporary file to write the visualization to
      Path path = Files.createTempFile("microjava-visualization", ".html");
      Files.writeString(path, html);

      System.out.println(data.type.getDescription() + " created successfully. View it at: " + path.toUri().toString());
      return path;
    } catch (IOException e) {
      throw new VisualizerException("Failed to create visualization: " + e.getMessage());
    }
  }

  /**
   * Try to open the browser if possible and not rate limited
   *
   * @param url The url to open
   */
  private static void openBrowser(String url) {
    if (browserLastOpened != null && browserLastOpened.plusSeconds(5).isAfter(LocalDateTime.now())) {
      return;
    }

    if (Desktop.isDesktopSupported()) {
      try {
        Desktop.getDesktop().browse(new URI(url));
      } catch (Exception e) {
        // Ignore
      }
    }
  }

  /**
   * Use reflection to get the secret recorder from the parser
   *
   * @return The recorder
   */
  private static Recorder getRecorderFromParserUsingReflection() {
    try {
      // Get the parser class
      Class<?> parserClass = Class.forName("ssw.mj.impl.Parser");

      // __recorder__ is a static field that has secretly been added in the Parser class by the TracingClassLoader
      // during a run of the application / the unit tests with the CLI setting
      // -Djava.system.class.loader=ssw.mj.TracingClassLoader
      Field recorderField = parserClass.getField("__recorder__");

      // Get the value of the field
      Recorder recorder = (Recorder) recorderField.get(null);

      return recorder;
    } catch (Exception e) {
      // throw new VisualizerException("Failed to get recorder from parser: " + e.getMessage());

      // We don't want to throw an exception here, since we might not have a recorder
      // when the custom class loader is not used
      return null;
    }
  }
}
