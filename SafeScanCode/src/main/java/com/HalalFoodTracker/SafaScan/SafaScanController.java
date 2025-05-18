package com.HalalFoodTracker.SafaScan;

import javafx.animation.TranslateTransition;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import net.sourceforge.tess4j.Tesseract;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 *
 * Controller class
 *
 * @author Abiru, Farhad
 *
 */
public class SafaScanController {

    //FXML items
    @FXML private  TextField aiInput;
    @FXML private  Button Confirm, uploadImage, exitBtn, setEng, setFr;
    @FXML private  Label aiResponse;
    @FXML private ImageView loadingIcon;

    //
    private File selectedImageFile;

    private String currentLanguage = "eng";


    //hashset for the haram ingredients
    private final Set<String> knownHaramIngredinets = new HashSet<>();
    private FoodChecker foodChecker = new FoodChecker();



    @FXML
    /**
     * change OCR to read English
     */
    public void setEnglish(ActionEvent event) {
        currentLanguage = "eng";
        System.out.println("OCR language set to English");
        setEng.setTextFill(Color.BLUE);
        setFr.setTextFill(Color.BLACK);
    }

    @FXML
    /**
     * change OCR to read French
     */
    public void setFrench(ActionEvent event) {
        currentLanguage = "fra";
        System.out.println("OCR language set to French");
        setFr.setTextFill(Color.BLUE);
        setEng.setTextFill(Color.BLACK);
    }

    @FXML
    /**
     * initialize csv path, loading gif, and makes it invisible until an image is loaded
     */
    public void initialize() {
        String csvPath = "DataBase/dataBase.csv";
        foodChecker.loadCSV(csvPath);
        loadingIcon.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/Images/loading.gif"))));
        loadingIcon.setVisible(false);


    }

    @FXML
    /**
     * handles images uploaded from user computer
     */
    public void handleUploadImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select an Image");

        // Limit to image file types
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );

        File file = fileChooser.showOpenDialog(uploadImage.getScene().getWindow());
        if (file != null) {
            selectedImageFile = file;
            System.out.println("Image selected: " + file.getAbsolutePath());
            // Call OCR on this image
            runOCROnImage(selectedImageFile);

            // You can now use this in your OCR logic:
            // tesseract.doOCR(file);
        } else {
            System.out.println("Image selection cancelled.");
        }
    }

    //getter(s)
    public File getSelectedImageFile() {
        return selectedImageFile;
    }


    /**
     * use Tesseract OCR(optical character recognition) to turn the text in and image into actual text
     * @param imageFile
     */
    private void runOCROnImage(File imageFile) {
        //have the loading icon be visible until it loads
        loadingIcon.setVisible(true);
        aiResponse.setVisible(false);

        Task<String> ocrTask = new Task<>() {
            @Override
            /**
             * gets the text from the image, shows the ai the ingredients flagged as haram, and asks for an output
             */
            protected String call() throws Exception {
                //accesses trained data(I got the high quality version)
                Path tempDir = Files.createTempDirectory("tessdata");
                SafaScanApplication.extractResourceFolderStatic("/tessdata", tempDir);

                //set the language, set path(should be the class path but this is set for the jar one)
                Tesseract tesseract = new Tesseract();
                tesseract.setDatapath("tessdata");
                tesseract.setLanguage(currentLanguage);

                //image text to string
                String text = tesseract.doOCR(imageFile);
                System.out.println("OCR Output:\n" + text);

                //shows which ingredients are haram
                String haramList = String.join(", ", knownHaramIngredinets);

                // AI call can take time, so do it here off UI thread
                String result = SafaScanApplication.aiOutput("these are the known haram ingredients : [" + haramList + "] from the extracted image, find any haram ingredients and shorttly say what its haram: OCR may be mistaked, keep that in mind(dont mention this to user) 50 word limit" + text);

                return result;
            }
        };

        ocrTask.setOnSucceeded(e -> {
            //turns off the loading screen, and make the text slide in from the right
            loadingIcon.setVisible(false);
            aiResponse.setText(ocrTask.getValue());

            // Fly in animation
            aiResponse.setTranslateX(400);  // offscreen right
            aiResponse.setVisible(true);

            TranslateTransition tt = new TranslateTransition(Duration.millis(600), aiResponse);
            tt.setFromX(400);
            tt.setToX(0);
            tt.play();
        });

        ocrTask.setOnFailed(e -> {
            loadingIcon.setVisible(false);
            aiResponse.setText("OCR failed: " + ocrTask.getException().getMessage());
            aiResponse.setVisible(true);

            TranslateTransition tt = new TranslateTransition(Duration.millis(600), aiResponse);
            tt.setFromX(400);
            tt.setToX(0);
            tt.play();
        });

        new Thread(ocrTask).start();
    }




    @FXML
    /**
     * ai output for the question textField, same as the one in the application but its taking the user input as the prompt
     */
    public void aiOutput(ActionEvent event) {


        String apiKey = System.getenv("aiAPIKey");


        // Get the input text
        String userText = aiInput.getText();

        // Run your scan logic (assuming scanTextForIngredients exists)


        // Compose prompt asking AI to explain the scan result briefly
        String question = "This will be a user asking an app that checks what ingredients are in a food: {" + userText + "}";

        var body = """
    {
      "messages": [
        {
          "role": "user",
          "content": "%s"
        }
      ],
      "model": "meta-llama/llama-4-scout-17b-16e-instruct",
      "temperature": 1,
      "max_completion_tokens": 1024,
      "top_p": 1,
      "stream": false,
      "stop": null
    }
    """.formatted(question);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.groq.com/openai/v1/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("Response code: " + response.statusCode());
            System.out.println("Response body: " + response.body());

            JSONObject json = new JSONObject(response.body());
            String aiReply = json.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content");

            System.out.println("AI Output:\n" + aiReply);
            aiResponse.setText(aiReply);

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }



    @FXML
    /**
     * exit the program
     */
    public void exitApp(ActionEvent event){
        System.exit(0);
    }


}
