package Henshin;

import org.eclipse.emf.henshin.model.Edge;
import org.eclipse.emf.henshin.model.Mapping;
import org.eclipse.emf.henshin.model.NestedCondition;
import org.eclipse.emf.henshin.model.Node;
import org.eclipse.emf.henshin.model.Rule;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Provides functionality for automatically assigning names to unnamed nodes and edges within Henshin rules.
 * This includes nodes in the left-hand side (LHS), right-hand side (RHS), and nested application conditions (NACs) of a rule,
 * as well as edges in both the LHS and RHS. This class ensures that names assigned are unique within the scope of a rule
 * and consistent across mappings between the LHS and RHS where applicable.
 */

public class NamingNullNodesAndEdges {
	
    /**
     * Stores previously used node names to avoid name duplication. The key is the node label,
     * and the value is a set of names that have been used for nodes with that label.
     */

    private final Map<String, Set<String>> usedNodeNames;
    
    /**
     * Initializes a new instance of the {@code NamingNullNodesAndEdges} class.
     * Sets up the internal structure for tracking used node names.
     */

    public NamingNullNodesAndEdges() {
        this.usedNodeNames = new HashMap<>();
    }
    
    /**
     * Assigns unique names to all unnamed nodes and consistent indices to all unnamed edges within a given rule.
     * It processes nodes and edges in the rule's LHS, RHS, and NACs, ensuring that names and indices are unique
     * and consistent with mappings where applicable.
     *
     * @param rule The Henshin rule whose nodes and edges are to be named.
     */

    public void assignNodeNames(Rule rule) {
  
        assignNodeNames(rule.getLhs().getNodes());
        for (NestedCondition nac : rule.getLhs().getNACs()) {
            assignNamesForNACs(nac, rule.getAllMappings());
            usedNodeNames.clear();
        }
        assignNodeNamesWithMappings(rule.getRhs().getNodes(), rule.getMappings());
        
        assignEdgeChars(Stream.concat(rule.getLhs().getEdges().stream(), rule.getRhs().getEdges().stream())
        		.collect(Collectors.toList()));  
      
    }
    
    /**
     * Assigns indices to unnamed edges based on their types and source/target nodes.
     * Ensures that each edge is assigned a unique index within its rule, using a character sequence starting from 'a'.
     *
     * @param edges The list of edges to process.
     */
  
    private void assignEdgeChars(List<Edge> edges) {
        Map<String, String> assignedEdgeIndices = new HashMap<>(); 
        char nextEdgeChar = 'a'; 
        for (Edge edge : edges) {
            if (edge.getIndex() == null || edge.getIndex().isEmpty()) {
                String edgeKey = edge.getType().getName() + "_" + edge.getSource().getName() + "_" + edge.getTarget().getName();
                if (assignedEdgeIndices.containsKey(edgeKey)) {
                	
                    edge.setIndex(assignedEdgeIndices.get(edgeKey)); 
                } else {
                    edge.setIndex(String.valueOf(nextEdgeChar++));
                    assignedEdgeIndices.put(edgeKey, edge.getIndex());
                }

            }
        }
    }
    
    /**
     * Assigns unique names to a list of unnamed nodes. Names are generated based on the node type
     * and are guaranteed to be unique within the scope of a rule.
     *
     * @param nodes The list of nodes to process.
     */

    private void assignNodeNames(List<Node> nodes) {
        for (Node node : nodes) {
            String nodeName = node.getName();
            if (nodeName == null || nodeName.isEmpty()) {
                String label = node.getType().getName().toLowerCase();
                nodeName = getUniqueName(label);
                node.setName(nodeName);
            }
        }
    }
    
    /**
     * Assigns names to nodes in the RHS of a rule based on their mappings from the LHS.
     * If a node in the RHS is unnamed but has a corresponding named node in the LHS, it is assigned the same name.
     * Additionally, unique names are assigned to any remaining unnamed nodes.
     *
     * @param rhsNodes The list of nodes in the RHS to process.
     * @param mappings The list of mappings between nodes in the LHS and RHS.
     */
    
    private void assignNodeNamesWithMappings(List<Node> rhsNodes, List<Mapping> mappings) {
        for (Mapping mapping : mappings) {
            Node lhsNode = mapping.getOrigin();
            Node rhsNode = mapping.getImage();
            String lhsNodeName = lhsNode.getName();
            String rhsNodeName = rhsNode.getName();
            if ((lhsNodeName != null && !lhsNodeName.isEmpty()) && (rhsNodeName == null || rhsNodeName.isEmpty())) {
                rhsNode.setName(lhsNodeName); 
            }
        }
        
        assignNodeNames(rhsNodes);
    }
    
    /**
     * Assigns names to nodes within a Nested Application Condition (NAC) based on mappings from the LHS of a rule.
     * If a mapping exists for a node in a NAC, it is assigned the name of the corresponding node in the LHS.
     * If no mapping is found, a unique name is assigned to the node.
     *
     * @param nac      The Nested Application Condition containing the nodes to be named.
     * @param mappings The list of mappings between nodes in the LHS and nodes in the NAC or RHS.
     */
  
    private void assignNamesForNACs(NestedCondition nac, List<Mapping> mappings) {
        for (Node nodeInNAC : nac.getConclusion().getNodes()) {
            boolean foundMapping = false;
            for (Mapping mapping : mappings) {
                if (mapping.getImage().getType().equals(nodeInNAC.getType())) {
                    Node correspondingNodeInLHS = mapping.getOrigin();
                    String lhsNodeName = correspondingNodeInLHS.getName();
                    if ((nodeInNAC.getName() == null || nodeInNAC.getName().isEmpty()) && (lhsNodeName != null && !lhsNodeName.isEmpty())) {
                        nodeInNAC.setName(lhsNodeName);
                        foundMapping = true;
                        break;
                    }
                }
            }
            if (!foundMapping) {
                nodeInNAC.setName(":" + nodeInNAC.getType().getName());;
            }
        }
    }
    
    /**
     * Generates a unique name for a node based on its label. Ensures that the name is unique
     * by maintaining a record of used names and appending an incrementing index until an unused name is found.
     *
     * @param label The base label for the node name.
     * @return A unique name for the node.
     */


    private String getUniqueName(String label) {
        usedNodeNames.putIfAbsent(label, new HashSet<>());
        int index = 1; 
        String uniqueName = label + index;

        while (usedNodeNames.get(label).contains(uniqueName)) {
            index++;
            uniqueName = label + index;
        }

        usedNodeNames.get(label).add(uniqueName);
        return uniqueName;
    }
}