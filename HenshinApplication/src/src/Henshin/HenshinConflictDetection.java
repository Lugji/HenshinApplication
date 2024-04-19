package Henshin;

import java.io.File;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.emf.henshin.model.Rule;
import org.eclipse.emf.henshin.multicda.cda.ConflictAnalysis;
import org.eclipse.emf.henshin.multicda.cda.MultiGranularAnalysis;
import org.eclipse.emf.henshin.multicda.cda.units.Reason;
import org.eclipse.emf.henshin.multicda.cda.units.Span;
import org.eclipse.emf.henshin.preprocessing.Granularity;
import org.eclipse.emf.henshin.preprocessing.HenshinRuleLoader;
import org.eclipse.emf.henshin.preprocessing.RulePreparator;

	/**
	 * This class is designed to perform conflict detection analysis on Henshin transformation rules.
	 * It utilizes the multi-granularity conflict analysis (CDA) capabilities provided by the Henshin
	 * framework to identify conflicts among a set of transformation rules. The class supports
	 * different levels of granularity for the analysis, namely atoms, binary, coarse, and fine, allowing
	 * users to customize the depth of the conflict detection process.
	 * 
	 * The class provides functionality to initialize the environment, load Henshin modules from a specified
	 * directory, and perform conflict detection analysis with the desired granularity levels. Additionally,
	 * it supports logging of the analysis results and execution times for further review.
	 */

public class HenshinConflictDetection {
	
	public static List<Granularity> granularities =  Arrays.asList(
			Granularity.binary,
			Granularity.coarse,
			Granularity.fine
			);
	
	String logTimeStamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
	
	String path = Paths.get(getDomainName(), logTimeStamp + ".log").toString();
	
	private ResourceSetImpl resourceSet;

	private boolean WRITE_LOGS = true;
	
    
	/**
	 * Runs the conflict detection analysis process. This method initializes the environment,
	 * loads the Henshin rules from the provided directory path, prepares the rules for analysis,
	 * and then executes the conflict detection analysis with the specified granularity levels.
	 * 
	 * @param granularities The list of granularity levels to be used in the analysis.
	 * @param henshinFolderPath The directory path where the Henshin modules are located.
	 */
		
	public void run(List<Granularity> granularities, String henshinFolderPath) {
		init();
		List<Rule> rules = getRules(henshinFolderPath);
		prepareRules(rules);
		//List<RulePair> nonDeleting = NonDeletingPreparator.prepareNonDeletingVersions(rules);
		doMultiGranularConflictAnalysis(granularities, rules);
	}
	
    /**
     * Initializes the necessary environment for loading and processing Henshin models.
     * This includes registering the XMI and Ecore resource factories and initializing
     * the resource set. It also prepares the logging infrastructure if logging is enabled.
     */

	public void init() {
	
		EcorePackage.eINSTANCE.eClass();
		Resource.Factory.Registry reg = Resource.Factory.Registry.INSTANCE;
		Map<String, Object> m = reg.getExtensionToFactoryMap();
		m.put("xmi", new XMIResourceFactoryImpl());
		resourceSet = new ResourceSetImpl();
		resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("ecore",
				new EcoreResourceFactoryImpl());
		if (WRITE_LOGS )
			initLogs();
	}
	
    /**
     * Loads Henshin transformation rules from the specified directory path.
     * 
     * @param henshinFolderPath The directory path where the Henshin modules are located.
     * @return A list of loaded Henshin rules. If the directory does not exist or no rules
     *         can be loaded, an empty list is returned.
     */

	public List<Rule> getRules(String henshinFolderPath) {

	    String fullSubDirectoryPath = henshinFolderPath;
	    File dir = new File(fullSubDirectoryPath);
	    if (!dir.isDirectory()) {
	        System.out.println("Directory does not exist: " + fullSubDirectoryPath);
	        return Collections.emptyList(); 
	    }

	    return HenshinRuleLoader.loadAllRulesFromFileSystemPaths(dir);
	}
	
    /**
     * Gets the domain name of this class. Used for logging purposes to identify the source
     * of log entries.
     * 
     * @return The class's simple name.
     */

	public String getDomainName() {
		return getClass().getSimpleName();
	}
	
    /**
     * Performs the multi-granular conflict analysis using the specified granularities on the given rules and non-deleting rule pairs.
     * Logs the analysis results.
     * 
     * @param granularities The granularities at which to perform the analysis.
     * @param rules The list of Henshin rules.
     * @param nonDeleting The list of non-deleting versions of the rules.
     */

	protected void doMultiGranularConflictAnalysis(List<Granularity> granularities,  List<Rule> rules) {

		if (granularities.contains(Granularity.binary)) {
			logn("Computing binary granularity:");
			for (Rule r1 : rules) {
				for (Rule r2 : rules) {
					long time = System.currentTimeMillis();
					MultiGranularAnalysis ca = 
							 new ConflictAnalysis(r1, r2);
					Span result = ca.computeResultsBinary();
					log(result == null ? "0 " : "1 ");
					tlog(System.currentTimeMillis() - time + " ");
				}
				logbn("   | " + r1.getName());
			}
			logbn("");
		}

		if (granularities.contains(Granularity.coarse)) {
			logn("Computing minimal conflict reasons:");
			for (Rule r1 : rules) {
				for (Rule r2 : rules) {
					long time = System.currentTimeMillis();
					MultiGranularAnalysis ca = 
							 new ConflictAnalysis(r1, r2);
					Set<? extends Reason> result = ca.computeResultsCoarse();
					log(result.size() + " ");
					tlog(System.currentTimeMillis() - time + " ");
				}
				logbn("   | " + r1.getName());
			}
			logbn("");
		}

		if (granularities.contains(Granularity.fine)) {
			logn("Computing initial conflict reasons:");
			for (Rule r1 : rules) {
				List<Integer> resultRow = new ArrayList<Integer>();
				for (Rule r2 : rules) {
					long time = System.currentTimeMillis();
					MultiGranularAnalysis ca = 
							 new ConflictAnalysis(r1, r2);
					Set<? extends Reason> result = ca.computeResultsFine();
					log(result.size() + " ");
					tlog(System.currentTimeMillis() - time + " ");
					resultRow.add(result.size());
				}
				logbn("   | " + r1.getName());
			}
			logbn("");
		}
	}
	
    /**
     * Initializes the logging facilities, creating log directories and files.
     */
	
	protected void initLogs() {
		
	    try {
	        String logDirectoryPath = "logs";
	        String timeLogPath = logDirectoryPath + File.separator + "time" + File.separator + path;
	        String resultsLogPath = logDirectoryPath + File.separator + "results" + File.separator + path;

	        Path timeLogFilePath = Paths.get(timeLogPath);
	        Path resultsLogFilePath = Paths.get(resultsLogPath);

	        if (!Files.exists(timeLogFilePath)) {
	            Files.createDirectories(timeLogFilePath.getParent());
	            Files.createFile(timeLogFilePath);
	        }

	        if (!Files.exists(resultsLogFilePath)) {
	            Files.createDirectories(resultsLogFilePath.getParent());
	            Files.createFile(resultsLogFilePath);
	        }
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	}
	
	   /**
     * Logs a message with a newline character at the end to both the console and the log file.
     * 
     * @param string The message to log.
     */

	protected void logbn(String string) {
		log(string+"\n");
		tlog(string+"\n");
	}
	
    /**
     * Logs a timestamped message to the time log.
     * 
     * @param string The message to log.
     */

	protected void tlog(String string) {
		if (WRITE_LOGS) {
			try {
				Files.write(Paths.get("logs"+File.separator+"time"+File.separator+path), string.getBytes(), StandardOpenOption.APPEND);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
    /**
     * Logs a message to both the console and the results log file.
     * 
     * @param string The message to log.
     */
	
	protected void log(String string) {
		System.out.print(string);

		if (WRITE_LOGS) {
		try {
			
			Files.write(Paths.get("logs"+File.separator+"results"+File.separator+path), string.getBytes(), StandardOpenOption.APPEND);
		} catch (IOException e) {
			e.printStackTrace();
		}
		}
	}
	
	   /**
     * Logs a message with a newline character at the end to both the console and the results log file.
     * 
     * @param string The message to log.
     */

	protected void logn(String string) {
		log(string+ "\n");
	}
	
    /**
     * Prepares the rules for analysis by removing multi-rules and applying other preprocessing steps.
     * 
     * @param rules The list of Henshin rules to prepare.
     */

	private static void prepareRules(List<Rule> rules) {
		List<Rule> prepared = new ArrayList<Rule>();
		rules.removeAll(rules.stream().filter(r -> !r.getMultiRules().isEmpty()).collect(Collectors.toList()));
		rules.forEach(r -> prepared.add(RulePreparator.prepareRule(r)));
		rules.clear();
		rules.addAll(prepared);
	}
	
	/**
	 * The main entry point for the application that performs conflict detection on Henshin transformation rules.
	 * This method performs the following steps:
	 * Determines the current working directory and its parent directory.
	 * Constructs the path to the Henshin files located in the "bank" folder within the parent directory.
	 * Creates an instance of {@link HenshinConflictDetection} and runs the conflict detection process
	 * on the Henshin files found in the specified directory, using a predefined set of granularities for analysis.
	 * It assumes a specific directory structure where the Henshin files are located in a directory named "bank"
	 * one level up from the current working directory. This setup is typical for certain project configurations and
	 * must be adjusted if the directory structure differs.
	 *
	 * @param args the command-line arguments (not used).
	 */
	
	public static void main(String[] args) {
		
        String currentDir = System.getProperty("user.dir");
        
        String parentDir = new File(currentDir).getParent();

		String henshinFolderPath = parentDir +File.separator+ "bank";
		
		new HenshinConflictDetection().run(granularities,henshinFolderPath);

	}

}