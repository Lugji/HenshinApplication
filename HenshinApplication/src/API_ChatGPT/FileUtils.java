package API_ChatGPT;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

public class FileUtils {

   /**
    * Method to retrieves the last modified file in a given folder.
    * @param folder, The folder to search for the last modified file.
    * @return The last modified file.
    * @throws IOException If an I/O error occurs.
    */
    public static File getLastModifiedFile(File folder) throws IOException {
    	
        // Check if the provided Folder is a directory and if the Folder not empty; if not, throw an exception
        if (!folder.isDirectory() || folder.listFiles() == null || folder.listFiles().length == 0) {
            throw new IllegalArgumentException("Invalid directory or empty folder");
        }

        // Find the last modified file in the folder, by walking through directory and subdirectories
        Path lastModifiedFile = Files.walk(folder.toPath())
                                     .filter(path -> !path.endsWith(".DS_Store")) // Filter .DS_Store files (macOS) 
                                     .filter(Files::isRegularFile)
                                     .max(Comparator.comparingLong(f -> {
                                    	 
                                    	// Get the last modified time of each file and compare them,
                                    	 // throw a runtime exception, if an error occurs
                                         try {  
                                             return Files.getLastModifiedTime(f).toMillis();
                                         } catch (IOException e) {
                                             throw new RuntimeException(e);
                                         }
                                     }))
                                     .orElseThrow(() -> new IllegalStateException("Unable to find last modified file"));
        
        // Convert the Path to a File and return
        return lastModifiedFile.toFile();
    }
    

   /** 
    * Method to return the absolute path of the last modified file in a directory
    * @return The absolute path of the last modified file.
    */
    public static String returnPathLastFile() {
    	
        // Get the current working directory
        String currentDir = System.getProperty("user.dir");
        // Construct the path to the directory where last modified file needs to be found
        File folder = new File(currentDir + "/logs/results/HenshinConflictDetection");
        
        try {
            // Get the last modified file in the specified directory, Print information about it 
            // and return the absolute path of it
            File lastModifiedFile = getLastModifiedFile(folder);
            System.out.println("Comparing This File: " + lastModifiedFile);
            return lastModifiedFile.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // If an error occurs or no file is found, return an empty string
        return "";
    }
}