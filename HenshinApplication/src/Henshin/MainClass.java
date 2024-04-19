package Henshin;

import java.io.File;
import java.io.IOException;

import org.eclipse.emf.henshin.model.Module;
import org.eclipse.emf.henshin.model.Rule;
import org.eclipse.emf.henshin.model.Unit;

import API_ChatGPT.ChatGPT_API;


public class MainClass {
    
   /*  
    * Directory path for the Henshin module.
    * The combination of System.getProperty("user.dir") and .getParent() allows retrieving 
    * the parent directory of the current working directory dynamically.
    * This ensures portability, enabling the project to work seamlessly for each user installing 
    * it without the need to define the path of the example "bank" directory.
    */
	private static final String BASEDIR = new File(System.getProperty("user.dir")).getParent() + File.separator + "bank" + File.separator + "bank.henshin";
    
	
	// Build prompts for ChatGPT API
	private static final String prompt = String.format(""" 
**Objective**:
Analyze a set of Cypher queries for potential conflicts within a graph database context. Conflicts are identified based on interactions between queries that lead to alteration or removal of graph elements, which could affect the outcome of subsequent queries.

**Conflict Criteria**:
1. **Deletion Conflict**: Occurs when a query deletes a graph element (node or relationship) that is required by another query for its `MATCH` condition.
2. **Creation-Deletion Conflict**: Arises when a query creates an edge incident to a node that is targeted for deletion by another query.
3. **Simultaneous Deletion Conflict**: Happens when two queries attempt to delete the same graph elements.

**Graph Elements**:
- **Nodes**: Represent entities such as Bank, Account, Client, and Manager.
- **Relationships**: Indicate connections between nodes, such as CLIENTS, ACCOUNTS, MANAGERS, and OWNER.

**Tasks**:
1. **Identify Relationships**: For each query, list the nodes and relationships directly involved in `CREATE` or `DELETE` actions.
2. **Analyze Interactions**: Examine the potential impact of each query on the others, focusing on the conflict criteria outlined.
3. **Matrix Representation**: Represent the analysis results as a binary matrix, where rows and columns correspond to the queries in order. Use "1" to indicate a conflict between two queries and "0" for no conflict.

**Enhancements**:
- Provide brief descriptions of the purpose behind each query to contextualize the analysis.
- Consider edge cases, such as indirect effects a query might have due to changes in the graph structure, which could influence the applicability of other queries.
- Include examples of specific scenarios where conflicts might arise, based on the graph elements involved.

** Queries**:

""");

	private static final String prompt = String.format(""" 
**Objective**:
Analyze a set of Cypher queries for potential dependencies within a graph database context.

**Dependency Criteria**:
1. **Creation-Dependency*: Occurs when a query CREATEs a graph element (node or relationship) that is required by another query for its `MATCH` condition.
2. **Deletion-Forbid Dependency**: Arises when a query DELETEs an element that would violate a negative application condition of another query.

**Graph Elements**:
- **Nodes**: Represent entities such as Bank, Account, Client, and Manager.
- **Relationships**: Indicate connections between nodes, such as CLIENTS, ACCOUNTS, MANAGERS, and OWNER.

**Tasks**:
1. **Identify Relationships**: For each query, list the nodes and relationships directly involved in `CREATE` or `DELETE` or `WHERE NOT` actions.
2. **Analyze Interactions**: Examine the potential impact of each query on the others, focusing on the dependencies criteria outlined.
3. **Matrix Representation**: Represent the analysis results as a binary matrix, where rows and columns correspond to the queries in order. Use "1" to indicate a dependency between two queries and "0" for no dependency.

**Enhancements**:
- Provide brief descriptions of the purpose behind each query to contextualize the analysis.
- Consider edge cases, such as indirect effects a query might have due to changes in the graph structure, which could influence the applicability of other queries.
- Include examples of specific scenarios where dependencies might arise, based on the graph elements involved.

** Queries**:

""");
    
     
     
     
    public static void main(String[] args) throws IOException {
    	
        // Initialize StringBuilder for output
        StringBuilder outputBuilder = new StringBuilder();
    	
        // Load the Henshin module
        ModuleLoader moduleLoader = new ModuleLoader("");
        
        Module module = moduleLoader.loadHenshinModule(BASEDIR);
        
        
        
        // Check if module is loaded successfully
        if (module == null) {
        	outputBuilder.append("Failed to load Henshin module from: " + BASEDIR);
            return;
        }
        
        // If module is loaded successfully, process rules and generate Cypher queries
        if (module != null) { 
        	HenshinRuleParser cypherGenerator = new HenshinRuleParser(); // Instantiate HenshinRuleParser
            for (Unit unit : module.getUnits()) {
                if (unit instanceof Rule) {
                    Rule rule = (Rule) unit;
                    outputBuilder.append("Cypher Query for rule " + rule.getName() + ":");

                    // Assign node names if necessary
                    NamingNullNodesAndEdges nodeNameAssigner = new NamingNullNodesAndEdges();
                    nodeNameAssigner.assignNodeNames(rule); 
                    
                    // Process each rule by translating it to a Cypher Query
                    outputBuilder.append("\n").append(cypherGenerator.processRule(rule));
                    outputBuilder.append("\n").append("\n");
                }
            }
            //System.out.println(outputBuilder);
        }       

         // Initialize and start ChatGPT API for interaction with GPT API
         // Start chat interaction with concatenated prompts and generated Cypher queries
         ChatGPT_API chatGPTAPI = new ChatGPT_API();
         chatGPTAPI.startChat(prompt + outputBuilder);
    }
}

