package uk.ac.abdn.foodsafety.meatprobe;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.stream.Stream;

import uk.ac.abdn.foodsafety.common.Constants;
import uk.ac.abdn.foodsafety.common.FoodSafetyException;
import uk.ac.abdn.foodsafety.sensordata.MeatProbeReading;

/**
 * 
 * @author nhc
 *
 * Parser for an entire directory of meat probe files.
 */
public final class MeatProbeFilesParser {
    /** Charset used for decoding all meat probe files */
    private static final Charset UTF8 = Charset.forName("UTF-8");
    
    /** The directory containing the meat probe files */
    private Path dir_path;
    
    /**
     * Converts the directory into a java.nio.file.Path and stores it
     * @param directory Path to the directory containing the meat probe files,
     * e.g. "mydata/meatprobefiles/"
     */
    public MeatProbeFilesParser(final String directory) {
        this.dir_path = Paths.get(directory); 
    }
    
    /**
     * Parses all files in the directory containing the meat probe files
     * @return A Stream containing every meat probe reading
     */
    public Stream<MeatProbeReading> parse() {
        try {
            return 
                    //For each file in the given directory
                    Files.walk(this.dir_path)
                    //Read all lines of the file
                    .map(MeatProbeFilesParser::readAllLines)
                    //Concatenate the lines of all the files
                    .reduce(Stream.empty(), Stream::concat)
                    //Remove the header lines
                    .filter(line -> !line.contains("MeatProbe"))
                    //Split remaining lines by the commas
                    .map(line -> line.split(","))
                    //Skip any line that did not have exactly 2 commas (i.e. 3 parts)
                    .filter(parts -> parts.length == 3)
                    //Parse the line into a MeatProbeReading (or null if not parseable)
                    .map(MeatProbeFilesParser::parseSingleReading)
                    //Remove null values
                    .filter(reading -> reading != null);
        } catch (final IOException e) {
            throw FoodSafetyException.meatProbeIOfailed(e);
        }
    }
    
    /**
     * Parses a String array into a fully typed single reading.
     * @param triple A String[] of length 3, e.g. {"42", "15/12/2015 02:16:14", "37.0"}
     * @return A parsed reading, or null if the data could not be parsed
     */
    private static MeatProbeReading parseSingleReading(final String[] triple) {
        try {
            return new MeatProbeReading(
                    Integer.parseInt(triple[0]), 
                    LocalDateTime.parse(
                            triple[1], 
                            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))
                        .atZone(Constants.UK), 
                    Double.parseDouble(triple[2]));
        } catch (final NumberFormatException e) {
            return null;
        } catch (final DateTimeParseException e) {
            return null;
        }
    }
    
    /**
     * Reads and decodes all lines of the file on the given path
     * @param path Path to a meat probe file
     * @return a Stream with one element per line in the file
     */
    private static Stream<String> readAllLines(final Path path) {
        try {
            return Files.isDirectory(path) 
                    ? Stream.empty() 
                    : Files.readAllLines(path, UTF8).stream();
        } catch (final IOException e) {
            throw FoodSafetyException.meatProbeIOfailed(e);
        }
    }
}
