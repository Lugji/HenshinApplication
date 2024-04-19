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
import org.eclipse.emf.henshin.multicda.cda.DependencyAnalysis;
import org.eclipse.emf.henshin.multicda.cda.MultiGranularAnalysis;
import org.eclipse.emf.henshin.multicda.cda.units.Reason;
import org.eclipse.emf.henshin.multicda.cda.units.Span;
import org.eclipse.emf.henshin.preprocessing.Granularity;
import org.eclipse.emf.henshin.preprocessing.HenshinRuleLoader;
import org.eclipse.emf.henshin.preprocessing.RulePreparator;

	/**
	 * Performs dependency analysis on Henshin transformation rules using multi-granular analysis.
	 * This class is designed to identify dependencies among transformation rules, similar to how
	 * {@link HenshinConflictDetection} detects conflicts, but focusing on dependencies as opposed to conflicts.
	 * It supports various levels of granularity for analysis, from binary to fine, and logs the analysis results.
	 *
	 * The analysis is driven by granularities defined as {@link Granularity}, ranging from less detailed (binary) to
	 * more detailed (coarse, fine) levels. It initializes the necessary resources, loads the Henshin rules from a specified
	 * directory, and then applies multi-granular dependency analysis on them. The results are logged fo further inspection.
	 */

public class HenshinDependencyDetection {

	public static List<Granularity> granularities =  Arrays.asList(
			Granularity.binary,
			Granularity.coarse,
			Granularity.fine
			);
	
	String logTimeStamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
	
	String path = Paths.get(getDomainName(), logTimeStamp + ".log").toString();
	
	private ResourceSetImpl resourceSet;

	private boolean WRITE_LOGS = true;
		
	public void run(List<Granularity> granularities, String henshinFolderPath) {
		init();
		List<Rule> rules = getRules(henshinFolderPath);
		prepareRules(rules);
		//List<RulePair> nonDeleting = NonDeletingPreparator.prepareNonDeletingVersions(rules);
		doMultiGranularDependencyAnalysis(granularities, rules);
	}
	

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

	public List<Rule> getRules(String henshinFolderPath) {

	    String fullSubDirectoryPath = henshinFolderPath;
	    File dir = new File(fullSubDirectoryPath);
	    if (!dir.isDirectory()) {
	        System.out.println("Directory does not exist: " + fullSubDirectoryPath);
	        return Collections.emptyList(); 
	    }

	    return HenshinRuleLoader.loadAllRulesFromFileSystemPaths(dir);
	}


	public String getDomainName() {
		return getClass().getSimpleName();
	}

    protected void doMultiGranularDependencyAnalysis(List<Granularity> granularities, List<Rule> rules) {

        if (granularities.contains(Granularity.binary)) {
            logn("Computing binary granularity:");
            for (Rule r1 : rules) {
                for (Rule r2 : rules) {
                    long time = System.currentTimeMillis();
                    MultiGranularAnalysis dependencyAnalysis =
                            new DependencyAnalysis(r1, r2);
                    Span result = dependencyAnalysis.computeResultsBinary();
                    log(result == null ? "0 " : "1 ");
                    tlog(System.currentTimeMillis() - time + " ");
                }
                logbn("   | " + r1.getName());
            }
            logbn("");
        }

        if (granularities.contains(Granularity.coarse)) {
            logn("Computing minimal dependency reasons:");
            for (Rule r1 : rules) {
                for (Rule r2 : rules) {
                    long time = System.currentTimeMillis();
                    MultiGranularAnalysis dependencyAnalysis =
                            new DependencyAnalysis(r1, r2);
                    Set<? extends Reason> result = dependencyAnalysis.computeResultsCoarse();
                    log(result.size() + " ");
                    tlog(System.currentTimeMillis() - time + " ");
                }
                logbn("   | " + r1.getName());
            }
            logbn("");
        }

        if (granularities.contains(Granularity.fine)) {
            logn("Computing initial dependency reasons:");
            for (Rule r1 : rules) {
                List<Integer> resultRow = new ArrayList<>();
                for (Rule r2 : rules) {
                    long time = System.currentTimeMillis();
                    MultiGranularAnalysis dependencyAnalysis =
                            new DependencyAnalysis(r1, r2);
                    Set<? extends Reason> result = dependencyAnalysis.computeResultsFine();
                    log(result.size() + " ");
                    tlog(System.currentTimeMillis() - time + " ");
                    resultRow.add(result.size());
                }
                logbn("   | " + r1.getName());
            }
            logbn("");
        }
    }

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

	protected void logbn(String string) {
		log(string+"\n");
		tlog(string+"\n");
	}

	protected void tlog(String string) {
		if (WRITE_LOGS) {
			try {
				Files.write(Paths.get(("logs"+File.separator+"time"+File.separator+path)), string.getBytes(), StandardOpenOption.APPEND);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	protected void log(String string) {
		System.out.print(string);

		if (WRITE_LOGS) {
		try {
			Files.write(Paths.get(("logs"+File.separator+"results"+File.separator+path)), string.getBytes(), StandardOpenOption.APPEND);
		} catch (IOException e) {
			e.printStackTrace();
		}
		}
	}

	protected void logn(String string) {
		log(string+ "\n");
	}

	private static void prepareRules(List<Rule> rules) {
		List<Rule> prepared = new ArrayList<Rule>();
		rules.removeAll(rules.stream().filter(r -> !r.getMultiRules().isEmpty()).collect(Collectors.toList()));
		rules.forEach(r -> prepared.add(RulePreparator.prepareRule(r)));
		rules.clear();
		rules.addAll(prepared);
	}
	
	public static void main(String[] args) {
		
        String currentDir = System.getProperty("user.dir");
        
        String parentDir = new File(currentDir).getParent();


		String henshinFolderPath = parentDir +File.separator+ "bank";;
		
		new HenshinDependencyDetection().run(granularities,henshinFolderPath);

	}

}