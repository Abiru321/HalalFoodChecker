package com.HalalFoodTracker.SafaScan;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.json.JSONObject;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

/**
 * Application of the project
 *
 * @author Abiru, Farhad
 */
public class SafaScanApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {

        //directory for files, makes one if there isn't one
        File csvDir = new File("DataBase");
        if (!csvDir.exists()) {
            csvDir.mkdir();
        }



        Parent root = FXMLLoader.load(Objects.requireNonNull(SafaScanApplication.class.getResource("Menu-view.fxml")));
        Scene scene = new Scene(root);
        stage.setTitle("Safa Scan");
        stage.setScene(scene);
        stage.show();
    }


    public static void main(String[] args) {
        launch();
    }

    /**
     * adds an ai api, call the method, and give an input
     *
     * @param input a String containing your prompt
     * @return the ai's response
     */
    public static String aiOutput(String input){
        String finalOutput="";

        //the actual key is in the environtment for security
        String apiKey = System.getenv("aiAPIKey");


        //replace new lines with a space and change double quotes to stop any string errors ()
        String question = input.replace("\n", " ").replace("\"", "\\\"");

        //JSON request body to call the groq api
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
        try{
            //create a request to receive the ai's input
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.groq.com/openai/v1/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            //for console and bug fixing
            System.out.println("Response code: " + response.statusCode());
            System.out.println("Response body: " + response.body());

            // Put the ai response into a string
            String responseBody = response.body();

            JSONObject json = new JSONObject(responseBody);

            //parse the JSON to only get the message
            finalOutput = json
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content");
            return finalOutput;

        }catch (IOException e){
            e.printStackTrace();

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return  finalOutput;


    }

    /**
     *
     *extracts all the files from the resource folder and copies them
     *
     * @param resourcePath location of the resource path
     * @param targetDir location of the new directory
     * @throws IOException if an error pops up
     */
    public static void extractResourceFolderStatic(String resourcePath, Path targetDir) throws IOException {
        for (String fileName : listResourceFilesStatic(resourcePath)) {
            try (InputStream is = SafaScanApplication.class.getResourceAsStream(resourcePath + "/" + fileName)) {
                Files.copy(Objects.requireNonNull(is), targetDir.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    /**
     *
     *lists the files from a folder
     *
     * @param path location of the folder
     * @return lists them
     * @throws IOException if an error occurs
     *
     */
    private static String[] listResourceFilesStatic(String path) throws IOException {
        try (InputStream in = SafaScanApplication.class.getResourceAsStream(path);
             BufferedReader br = new BufferedReader(new InputStreamReader(Objects.requireNonNull(in)))) {
            return br.lines().toArray(String[]::new);
        } catch (Exception e) {
            return new String[]{"eng.traineddata", "fra.traineddata"};
        }
    }


}
