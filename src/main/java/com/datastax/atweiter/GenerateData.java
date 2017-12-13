package com.datastax.atweiter;

import org.fluttercode.datafactory.impl.DataFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GenerateData {

    public static void main(String[] args) throws IOException {
        DataFactory df = new DataFactory();
        String separator = ",";


        try (BufferedWriter bw = new BufferedWriter(new FileWriter(new File(System.getProperty("user.home"),"product.csv")));
             BufferedWriter bw2 = new BufferedWriter(new FileWriter(new File(System.getProperty("user.home"),"productLocalisation.csv")))) {
            for (int k = 0; k < 1_000_000; k++) {

                String timeuuid = com.datastax.driver.core.utils.UUIDs.timeBased().toString();
                bw.write(timeuuid);//id
                bw.write(separator);
                bw.write(df.getRandomWord(4, 8));//name
                bw.write(separator);
                bw.write(df.getBusinessName());//brand name
                bw.write(separator);

                int number = df.getNumberBetween(0, 20);
                List<String> text = new ArrayList<>(number);
                for (int i = 0; i < number; i++) {
                    text.add(df.getRandomWord(2, 12));
                }
                bw.write(String.join(" ", text));//short_description
                bw.write(separator);
                bw.write(df.getRandomWord(6, 20));//department
                bw.write(separator);
                bw.write(df.getRandomChars(8) + ".pdf"); //long description path
                bw.write(separator);

                number = df.getNumberBetween(0, 5);
                List<String> images = new ArrayList<>(number);
                for (int i = 0; i < number; i++) {
                    images.add("'" + df.getRandomChars(8) + ".png'");
                }
                bw.write("\"{" + String.join(separator, images) + "}\"");
                bw.write(separator);
                number = df.getNumberBetween(0, 5);
                List<String> videos = new ArrayList<>(number);
                for (int i = 0; i < number; i++) {
                    videos.add("'" + df.getRandomChars(8) + ".mp4'");
                }
                bw.write("\"{" + String.join(separator, videos) + "}\"");
                bw.write(separator);
                bw.write(df.getRandomChars(4, 8));//sku
                bw.write(separator);
                bw.write(df.getRandomChars(4, 8));//upc
                bw.write(separator);
                number = df.getNumberBetween(0, 10);
                List<String> tags = new ArrayList<>(number);
                for (int i = 0; i < number; i++) {
                    tags.add("'" + df.getRandomChars(8) + "'");
                }
                bw.write("\"{" + String.join(separator, tags) + "}\"");
                bw.write(separator);
                number = df.getNumberBetween(0, 10000);
                bw.write("" + number);//rating_count
                bw.write(separator);
                float minX = 0.0f;
                float maxX = 5.0f;
                Random rand = new Random();
                float rating = rand.nextFloat() * (maxX - minX) + minX;
                bw.write(String.format(java.util.Locale.US, "%.2f", rating));//avg_rating
                bw.write(separator);
                double minPrice = 0.0;
                double maxPrice = 1000.0;
                double price = rand.nextDouble() * (maxPrice - minPrice) + minPrice;
                bw.write(String.format(java.util.Locale.US, "%.2f", price));//price
                bw.write(separator);
                List<String> locationWithStock = new ArrayList<>(100);
                for (int i = 0; i < 100; i++) {
                    if(rand.nextBoolean()){
                        locationWithStock.add("'local" + i + "'");
                    }
                }
                bw.write("\"{" + String.join(separator, locationWithStock) + "}\"");
                bw.write("\n");

                for (int i = 0; i < 100; i++) {
                    bw2.write(timeuuid);
                    bw2.write(separator);
                    bw2.write("local" + i);
                    bw2.write(separator);
                    number = df.getNumberBetween(0, 100);
                    bw2.write("" + number);
                    bw2.write("\n");
                }
            }
        }
    }
}
