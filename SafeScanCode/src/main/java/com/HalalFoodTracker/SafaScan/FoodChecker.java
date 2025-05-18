package com.HalalFoodTracker.SafaScan;

import java.io.*;
import java.util.*;

/**
 * Class that handles the data base
 *
 * @author Farhad, Abiru
 *
 */
public class FoodChecker {

    private final List<Set<String>> haramBatches = new ArrayList<>();

    /**
     *
     * Loads the data of all the ingredients and their label
     *
     * @param filePath location of the csv file
     */
    public void loadCSV(String filePath) {
        haramBatches.clear();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;

            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                int lastComma = line.lastIndexOf(',');
                if (lastComma == -1) continue;

                //the file is split from ingredient to label
                String ingredientPart = line.substring(0, lastComma).toLowerCase();
                String label = line.substring(lastComma + 1).trim().toLowerCase();

                //stores the haram ingredients for checking later
                if (label.equals("haram")) {
                    Set<String> cleanIngredients = new HashSet<>();
                    for (String ingredient : ingredientPart.split("[,\\s]+")) {
                        String cleaned = ingredient.replaceAll("[^a-zA-Z0-9]", "");
                        if (!cleaned.isEmpty()) {
                            cleanIngredients.add(cleaned);
                        }
                    }
                    if (!cleanIngredients.isEmpty()) {
                        haramBatches.add(cleanIngredients);
                    }
                }
            }

            if (haramBatches.isEmpty()) {
                System.out.println("No haram ingredients loaded.");
            }

        } catch (IOException e) {
            System.err.println("Error reading CSV: " + e.getMessage());
        }
    }


}
