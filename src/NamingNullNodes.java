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


public class NamingNullNodes {

	// A Map to keep track of the used bzw. given names to the nodes. Some of them needs to be distinct, e.g. in the case when there are nodes with the same label.
	private final Map<String, Set<String>> usedNodeNames;
	
	//The constructor with the HashMap to be initialized.
    public NamingNullNodes() {
        this.usedNodeNames = new HashMap<>();
    }
    
    //The method assigns names to the nodes, firstly to the LHS of the graph, which means everything that need to be MATCHed
    //for the MATCH Clause. Than to the NACs and it clears the the list since we dont want to give nodes to the RHS of the graph
    //the same as the NACs but we want to start a new fresh list. Then we assign names to the null or empty nodes of the RHS of the graph.
    //using ofc the mappings, that enables giving the same names to the nodes that needs to preserved, that means the nodes that already
    //have a mapping, otherwise (e.g. has a CREATE action) gives a distinct name.
    //And finally assigns chars to the edges too, in order to have access to them in case of a DELETE clause.
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

    //It initializes an empty map assignedEdgeIndices to track assignments of edge keys to chars.
    //Sets a starting letter nextEdgeChar, to 'a', which will be used to determine the next available letter for an edge lacking an index.
    //For each edge, it checks if its index is null or empty. If so it creates a unique key, edgeKey based on its type name, source node, and target node.
    //It checks if assignedEdgeIndices already contains a value for edgeKey, if it does, it assigns the edge the index associated with this edgeKey in the map.
    //If not, it assigns the edge a new indexfrom the current value of nextEdgeChar, which is then incremented to determine the next available letter for the next edge. 
    //The edgeKey and the assigned index are then stored in the assignedEdgeIndices map.
    
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
    
    //Quasi the default method to give names to the nodes.
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
    
    //Assigning names to the Nodes, specially to the nodes of RHS of the graph which already own a mapping from the LHS of the graph.
    //In this case we give exactly the same names as the origin nodes from the LHS of the graph.
    //If there is no mapping than just call the default method.
    private void assignNodeNamesWithMappings(List<Node> rhsNodes, List<Mapping> mappings) {
        for (Mapping mapping : mappings) {
            Node lhsNode = mapping.getOrigin();
            Node rhsNode = mapping.getImage();
            String lhsNodeName = lhsNode.getName();
            String rhsNodeName = rhsNode.getName();
            if ((lhsNodeName != null && !lhsNodeName.isEmpty()) && (rhsNodeName == null || rhsNodeName.isEmpty())) {
                //System.out.println("Mapping found: " + lhsNodeName + " (LHS) -> " + rhsNode.getType().getName() + " (RHS)");
                rhsNode.setName(lhsNodeName); 
                //System.out.println("Assigned name '" + lhsNodeName + "' to node: " + rhsNode.getType().getName());
            }
        }
        assignNodeNames(rhsNodes);
        //System.out.println("After assigning names in RHS:");
        //printNodeNames(rhsNodes);  
    }
    /**private void printNodeNames(List<Node> nodes) {
        for (Node node : nodes) {
            System.out.println("Node: " + node.getType().getName() + ", Name: " + node.getName());
        }
    }*/
    
    //The same idea as assignNodeNamesWithMappings method occurs here too.
    private void assignNamesForNACs(NestedCondition nac, List<Mapping> mappings) {
        for (Node nodeInNAC : nac.getConclusion().getNodes()) {
            boolean foundMapping = false;
            //System.out.println("Node in NAC: " + nodeInNAC.getName() + " (" + nodeInNAC.getType().getName() + ")");
            for (Mapping mapping : mappings) {
                //System.out.println("Mapping: " + mapping);
                if (mapping.getImage().getType().equals(nodeInNAC.getType())) {
                    Node correspondingNodeInLHS = mapping.getOrigin();
                    String lhsNodeName = correspondingNodeInLHS.getName();
                    //System.out.println("Corresponding Node in LHS: " + correspondingNodeInLHS.getName() + " (" + correspondingNodeInLHS.getType().getName() + ")");
                    if ((nodeInNAC.getName() == null || nodeInNAC.getName().isEmpty()) && (lhsNodeName != null && !lhsNodeName.isEmpty())) {
                        //System.out.println("Assigning name from LHS: " + lhsNodeName);
                        //System.out.println("");
                        nodeInNAC.setName(lhsNodeName);
                        foundMapping = true;
                        break;
                    }
                }
            }
            if (!foundMapping) {
                //System.out.println("No mapping found, assigning unique name");
                assignNodeNames(List.of(nodeInNAC));
            }
        }
    }

    //Based on the occurring label the method assign unique names by increasing the index by one.
    //It adds at the Map only if there isnt the label.
    private String getUniqueName(String label) {
        usedNodeNames.putIfAbsent(label, new HashSet<>());
        int index = 1; // Start index from 1
        String uniqueName = label + index;

        while (usedNodeNames.get(label).contains(uniqueName)) {
            index++;
            uniqueName = label + index;
        }

        usedNodeNames.get(label).add(uniqueName);
        return uniqueName;
    }

}