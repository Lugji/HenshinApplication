package API_ChatGPT;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

public class ChatGPT_API {

	
	 // Constants for file paths, API key, model for GPT, chat log directory, log file extension, and conversation history storage 
     private static final String PYTHON_SCRIPT_PATH = "src/API_ChatGPT/myPythonScript.py";
     private static final String API_KEY = "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";
     private static final String MODEL = "gpt-3.5-turbo";
     private static final String CHAT_LOG_DIRECTORY = "src/API_ChatGPT/GPT_Results/";
     private static final String CHAT_LOG_EXTENSION = ".log";
     private static List<String> messages = new ArrayList<>(); // List to maintain conversation history

    
    /**
     * This method starts a chat session with the user, processes their inputs, and logs the conversation.
     * The method processes the initial message, sends it to ChatGPT for a response, 
     * and enters a loop to receive further user input until the user types "exit".
     * During the conversation, if the user inputs "compare the results", the method fetches the last modified file in HenshinConflictDetection
     * for comparison and displays its content.
     * @param initialMessage The initial message provided by the user to start the conversation.
     * @throws IOException If an I/O error occurs while accessing or writing to the chat log file.
     */
     public void startChat(String initialMessage) {
    	
    	try {
    	    // Create a File object representing the directory where chat logs will be stored
    	    File directory = new File(CHAT_LOG_DIRECTORY);
	        // If the directory doesn't exist, create it along with any necessary parent directories
    	    if (!directory.exists()) {
    	        directory.mkdirs();
    	    }
    	
    	    // Create a SimpleDateFormat object to format the timestamp in a specific pattern
        	// Create the file path for the chat log by concatenating the directory path, timestamp, and file extension
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm");
            String timestamp = dateFormat.format(new Date());
            String chatLogPath = CHAT_LOG_DIRECTORY + timestamp + CHAT_LOG_EXTENSION;

            // === InitialMessage ===
            // Process the initial message before starting the scanner loop, print it and store it to the 'messages' list 
            System.out.println("Processing initial message to ChatGPT:\n\n" + initialMessage);
            messages.add("User: " + initialMessage); 
            
            // Send initial message to ChatGPT, print the response and store it to the 'messages' list
            String initialResponse = getChatResponse();
            System.out.println("ChatGPT: " + initialResponse);
            messages.add("ChatGPT: " + initialResponse);
                    
            // Open the BufferedWriter here, so we can write the initial messages and their response to the file with time stamp before the loop
            try (Scanner scanner = new Scanner(System.in); BufferedWriter chatLogWriter = new BufferedWriter(new FileWriter(chatLogPath))) {
            	chatLogWriter.write(MODEL+"\n" +"-------------"+"\n");
                chatLogWriter.write("[" + timestamp + "] User: " + initialMessage + "\n");
                chatLogWriter.write("[" + timestamp + "] ChatGPT: " + initialResponse + "\n");
                chatLogWriter.flush();

                // === UserInput ===
                // Now start the scanner loop for further user input
                while (true) {
                    // Prompt the user for input
                    System.out.print("You: ");
                    // Read user input from the console
                    String userInput = scanner.nextLine();
                    
                    // Check if the user wants to exit the chat
                    if ("exit".equalsIgnoreCase(userInput)) {
                        break;
                    }
                    
                    // Check if the user wants to compare the results with HenshinConflictDetection results
                    if ("compare the results".equalsIgnoreCase(userInput)) {
                        // Option to compare results
                        System.out.println("Processing initial message to ChatGPT:");
                        
                        // Fetch the last modified file for comparison using the method returnPathLastFile() from FileUtils class
                        File file = new File(FileUtils.returnPathLastFile());
                        BufferedReader conflictMatrixPath = new BufferedReader(new FileReader(file));
                        StringBuilder conflictMatrix = new StringBuilder();                     
                                                
                        // Read contents from log file and append to conflictMatrix StringBuilder
                        String line;
                        while ((line = conflictMatrixPath.readLine()) != null) {
                            conflictMatrix.append(line).append("\n");
                        }
                        conflictMatrixPath.close();
                        // Display conflict matrix
                        System.out.println(conflictMatrix);
                        // Modify user input to include comparison message
                        userInput = ("Compare your binary conflict matrix with the following binary matrix from Henshin Conflict Analysis:\n" + conflictMatrix);
                    }

                             
                    //User:
                    // Add the user input to the 'messages' list
                    messages.add("User: " + userInput);                  
                    
                    //ChatGPT:
                    // Send the user input to ChatGPT, get the response, print it and store it to the 'messages' list
                    String response = getChatResponse();
                    System.out.println("ChatGPT: " + response);
                    messages.add("ChatGPT: " + response);

                    // Write user input and ChatGPT response to the log file with the time stamp
                    chatLogWriter.write("[" + timestamp + "] You: " + userInput + "\n");
                    chatLogWriter.write("[" + timestamp + "] ChatGPT: " + response + "\n");
                    chatLogWriter.flush();
                }
                // Indicate that the chat log has been saved
                System.out.println("Chat log saved to " + chatLogPath);
            }
            // Indicate the end of the chat session
            System.out.println("Goodbye!");
            
    	  
          // Handle any IO exceptions
    	} catch (IOException e) {
    	    // Print an error message indicating the nature of the exception
    	    System.err.println("Error occurred: " + e.getMessage());
    	    // Print the stack trace of the exception, providing detailed information about where the error occurred
    	    e.printStackTrace();
    	}
    } 
    
    
     /*
     The getChatResponse method performs the following tasks to get a response from ChatGPT:
       
     1) Generate Python script content: The method generates the content of a Python script.
        This script probably contains instructions on how to interact with the ChatGPT model and generate a response.
           
     2) Write Python script to a file: The contents of the generated Python script are written to a file specified 
        by PYTHON_SCRIPT_PATH. This file is executed to get the response from ChatGPT. 
           
     3) Python script execution: With a ProcessBuilder, the method starts a new process to execute the Python script. 
        The process runs the script and produces results that are captured by the Java program.
           
     4) Read Standard Output: The method reads the standard output of the executed Python script line by line.
        Each line represents part of the response generated by ChatGPT.
        These lines are appended to a StringBuilder to reconstruct the full answer.
           
     5) Error Handling: Any errors found while executing the Python script are read from the standard Error stream
        (getErrorStream()). These errors are printed in the standard error stream for debugging purposes.
           
     6) Close streams: Once all output has been read, the input and error streams are closed to free resources.
        
     7) Return Response: The method returns the entire response received from ChatGPT after truncating it leading or trailing spaces.
        This response is used to continue the conversation with the user. 
        
     Overall, the method performs the core task of obtaining a response from ChatGPT.
     However, its functionality also relies on other supporting methods, such as generatePythonScriptContent 
     and installPythonOS, to generate the Python code and determine the operating system dependent on the Python installation.  
     */  
    /**
     * This method retrieves a response from ChatGPT.
     * The method performs all necessary tasks, including generating a Python script,
     * executing the script, and capturing the output as the ChatGPT response.
     * @return The response generated by ChatGPT.
     * @throws IOException If an I/O error occurs during script execution or reading/writing files.
     */
     private static String getChatResponse() throws IOException {
    	
        // Write the generated Python script content to a file 
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(PYTHON_SCRIPT_PATH))) {
            writer.write(generatePythonScriptContent());
        }

        // === Execute the Python script ===
        // Create a new ProcessBuilder instance with the command to run the Python script and its file path.
        // This builder is used to start a new operating system process.
        ProcessBuilder pb = new ProcessBuilder(installPythonOS(), PYTHON_SCRIPT_PATH);
        // Start the process defined by the ProcessBuilder and return a Process object representing it.
        Process p = pb.start();

        // Read standard output:
        // Create a BufferedReader to read the standard output stream of the process, which contains the script's output.
        BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
        // Create a StringBuilder to construct the response from the script's output.
        StringBuilder responseBuilder = new StringBuilder();
        String line;
        // Iterate over each line of the standard output stream until there are no more lines.
        while ((line = stdInput.readLine()) != null) {
            // Append each line of the standard output to the responseBuilder to construct the complete response.
            responseBuilder.append(line).append("\n");
        }

        // Read errors (if any)
        BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));
        while ((line = stdError.readLine()) != null) {
            // Print any errors encountered during script execution
            System.err.println(line); // Consider better error handling
        }
        // Close the streams
        stdInput.close();
        stdError.close();

        return responseBuilder.toString().trim(); // Trim any trailing whitespace from the response
    }
     
     
    /**
     * This method constructs a Python script containing the necessary code to interact with OpenAI's chat API.
     * It formats the user, system and assistant messages and properly escapes special characters to ensure compatibility.
     * This assumes the existence of a valid API_KEY variable for OpenAI authentication.
     * @return The content of the Python script as a string.
     */
     private static String generatePythonScriptContent() {
    	
        StringBuilder messagesStringBuilder = new StringBuilder();
        
        // Iterate through messages and properly escape special characters
        for (String message : messages) {
        	 	
        	//escaping special characters ensures that strings are interpreted correctly by the programming language's 
        	//compiler or interpreter, especially when embedding strings within code or dealing with user input.
        	//It helps maintain the integrity of the data and prevents syntax errors or security vulnerabilities.
        	String escapedMessage = message.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
        	
            // Append formatted message to StringBuilder
            messagesStringBuilder.append(String.format("{\"role\": \"%s\", \"content\": \"%s\"},\n",
                    message.startsWith("User:") ? "user" : "assistant", escapedMessage));
        }
        
        
         /* 
         This code snippet creates a Python script as a formatted string using String.format() 
         and a multi-line string (using the syntax “”"……""”). 
         
         1) String Format: String.format() method is used to insert dynamic values 
            the Python script template. In this case, %s placeholders in the template are replaced with 
            the values ​​provided as arguments. 
            
         2) Python script template: The multi-line string provided represents a Python script template. 
            Contains Python code to interact with the OpenAI API. 
             
         3) API_key: %s is replaced with the API_KEY value, which presumably contains the API 
            Key for authentication to the OpenAI service. 
            
         4) Model: Replaces %s with the MODEL name for the OpenAI model to use.

         5) Messages: ({"role": "system", "content":..... ) 
            These messages provide specific information to the system, assisting the assistant in specializing in particular tasks.
                      
         6) Messages: %s is replaced with the value messageStringBuilder.toString().trim(), 
            which presumably contains a string representation of a series of messages. 
            These messages are passed to the OpenAI API to complete the chat. 
            
         7) Execution and response: The Python script contains code to initialize the OpenAI client. 
            defines the model to use, sends messages to the API and receives a response (Response text is printed). 
            
         8) Return statement: The constructed Python script string is returned by the method.
          
         Generally, this code block dynamically generates a Python script for interaction 
         with the OpenAI API based on the provided parameters (API keys and messages), 
         allows Java code to communicate with the OpenAI service and retrieve chat completions. 
         */
        
        return String.format("""
            from openai import OpenAI
            
            # Initialize OpenAI client
            client = OpenAI(api_key="%s")

            response = client.chat.completions.create(
              model="%s",
              temperature=0.3,
              messages=[{"role": "system", "content": "You are an assistant specialized in conflict and dependency detection in rule-based graph transformation."},
              %s],
            )
            response_text = response.choices[0].message.content
            print(response_text)
            """, API_KEY,MODEL, messagesStringBuilder.toString().trim());
    }
   
      
    /**
     * This method aims to determine the appropriate Python command based on the operating system. 
     * On Windows, it returns "python" command, while on other operating systems, it returns "python3" command.
     * This method is designed to make it work on different operating systems.
     * This assumes that 'python' and 'python3' commands are available in the system PATH.
     * @return The appropriate Python command based on the operating system.
     */
     private static String installPythonOS() {
    	 
       // Retrieve the operating system name using system properties   	 
       String osName = System.getProperty("os.name").toLowerCase();
       
       // For Windows, use the 'python' command, for UNIX-based OS, use the 'python3' command
       return osName.contains("windows") ? "python" : "python3";
    }
         
}