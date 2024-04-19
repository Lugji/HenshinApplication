package Henshin;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.emf.henshin.model.Action;
import org.eclipse.emf.henshin.model.Action.Type;
import org.eclipse.emf.henshin.model.Edge;
import org.eclipse.emf.henshin.model.Node;
import org.eclipse.emf.henshin.model.Rule;

/**
 * Translates Henshin rules into Cypher queries.
 * This class processes Henshin graph transformation rules and generates
 * corresponding Cypher queries for graph databases, handling node and edge
 * actions including CREATE, DELETE, PRESERVE, and FORBID.
 */

public class HenshinRuleParser {

    private Set<String> visitedEdgesIdentifiers = new HashSet<>();
    private Set<String> visitedNodes = new HashSet<>();
    
    /**
     * Processes a Henshin rule and generates a Cypher query.
     * It sequentially builds MATCH, WHERE NOT, DELETE, and CREATE clauses based on the rule's
     * left-hand side (LHS), negative application conditions (NACs), and right-hand side (RHS).
     *
     * @param rule The Henshin rule to be processed.
     * @return The generated Cypher query as a string.
     */

    public String processRule(Rule rule) {
        resetState(); 
        StringBuilder cypherQuery = new StringBuilder();
        cypherQuery.append(buildMatchClause(rule))
                   .append(buildWhereNotClause(rule))
                   .append(buildDeleteClause(rule))
                   .append(buildCreateClause(rule));
        return cypherQuery.toString();
    }
    
    /**
     * Resets the state of the parser. Clears the sets tracking visited nodes and edges,
     * and elements to be deleted, ensuring fresh processing for each rule.
     */

    private void resetState() {
        visitedEdgesIdentifiers.clear();
        visitedNodes.clear();
    }
    
    /**
     * Builds the MATCH clause for the Cypher query, which identifies graph patterns
     * that must exist for the rule to apply. This includes handling nodes and edges
     * based on their specified actions (PRESERVE, DELETE).
     * 
     * @param rule The Henshin rule being processed.
     * @return The MATCH clause of the Cypher query.
     */

    private String buildMatchClause(Rule rule) {
        List<String> matchPatterns = new ArrayList<>();
        
        // Add edge patterns
        matchPatterns.addAll(rule.getLhs().getEdges().stream()
            .filter(edge -> visitedEdgesIdentifiers.add(generateEdgeIdentifier(edge)))
            .map(this::buildLinearPath) 
            .collect(Collectors.toList()));
        
        // Add isolated node patterns
        matchPatterns.addAll(rule.getLhs().getNodes().stream()
            .filter(this::isIsolatedNode)
            .map(node -> translateNode(node, node.getAction().getType())) 
            .collect(Collectors.toList()));

        return matchPatterns.isEmpty() ? "" : "MATCH " + String.join(", ", matchPatterns);
    }
    
    /**
     * Identifies if a given node is considered isolated within the rule's context.
     * An isolated node has no incoming or outgoing edges.
     * 
     * @param node The node to check for isolation.
     * @return true if the node is isolated, false otherwise.
     */

    private boolean isIsolatedNode(Node node) {
        return node.getIncoming().isEmpty() && node.getOutgoing().isEmpty(); 
    }
    
    /**
     * Builds a linear path from a starting edge, considering the actions of connected nodes
     * and traversing through the graph based on the rule's logic. This method contributes to
     * generating the MATCH clause for the Cypher query.
     * 
     * @param startEdge The starting edge from which to build the path.
     * @return A string representing the linear path in Cypher syntax.
     */
    
    private String buildLinearPath(Edge startEdge) {
        StringBuilder cypherPath = new StringBuilder();
        Edge currentEdge = startEdge;
        
        Node sourceNode = currentEdge.getSource();
        cypherPath.append(translateNode(sourceNode, sourceNode.getAction().getType())); 

        do {
            cypherPath.append(translateEdge(currentEdge));
            
            Node targetNode = currentEdge.getTarget();
            cypherPath.append(translateNode(targetNode, targetNode.getAction().getType())); 

            currentEdge = targetNode.getOutgoing().stream()
                                    .filter(edge -> !visitedEdgesIdentifiers.contains(generateEdgeIdentifier(edge)))                                          
                                    .findFirst()
                                    .orElse(null);
                                    
            if (currentEdge != null) {
                visitedEdgesIdentifiers.add(generateEdgeIdentifier(currentEdge));
            }
        } while (currentEdge != null);

        return cypherPath.toString();
    }
    
    /**
     * Constructs the WHERE NOT clause for the Cypher query, which specifies patterns that
     * must not exist for the rule to apply. This is typically derived from the rule's
     * Negative Application Conditions (NACs).
     * 
     * @param rule The Henshin rule being processed.
     * @return The WHERE NOT clause of the Cypher query.
     */

    private String buildWhereNotClause(Rule rule) {
        String whereNotPatterns = rule.getLhs().getNACs().stream()
            .flatMap(nac -> nac.getConclusion().getEdges().stream())
            .map(edge -> {
                String sourcePattern = translateNodeForWhereNot(edge.getSource(), isForbidden(edge.getSource()));
                String targetPattern = translateNodeForWhereNot(edge.getTarget(), isForbidden(edge.getTarget()));
                return (isForbidden(edge.getSource()) || isForbidden(edge.getTarget())) 
                    ? String.format("%s-[:%s]->%s", sourcePattern, edge.getType().getName().toUpperCase(), targetPattern) 
                    : "";
            })
            .filter(pattern -> !pattern.isEmpty()) 
            .collect(Collectors.joining(" AND "));

        return whereNotPatterns.isEmpty() ? "" : "\nWHERE NOT " + whereNotPatterns;
    }
    
    /**
     * Determines if a given node is marked with a FORBID action, indicating that its
     * presence violates the rule's application conditions.
     * 
     * @param node The node to check.
     * @return true if the node is forbidden, false otherwise.
     */

    private boolean isForbidden(Node node) {
        return node.getAction() != null && node.getAction().getType() == Action.Type.FORBID;
    }
    
    /**
     * Translates a node for inclusion in the WHERE NOT clause based on whether it is forbidden.
     * This involves formatting the node with its attributes if it is forbidden.
     * 
     * @param node The node to translate.
     * @param isForbidden Indicates if the node is forbidden.
     * @return A string representation of the node for the WHERE NOT clause.
     */

    private String translateNodeForWhereNot(Node node, boolean isForbidden) {
        String nodeType = node.getType().getName();
        StringBuilder attributesStringBuilder = new StringBuilder();

        if (isForbidden && !node.getAttributes().isEmpty()) {
            String attributes = node.getAttributes().stream()
                .map(attr -> String.format("%s: '%s'", attr.getType().getName(), attr.getValue()))
                .collect(Collectors.joining(", "));
            attributesStringBuilder.append("{").append(attributes).append("}");
            return String.format("(:%s%s)", nodeType, attributesStringBuilder);
        } else if (isForbidden) {
            return String.format("(:%s)", nodeType);
        } else {
            return String.format("(%s)", node.getName());
        }
    }

    
    /**
     * Builds the DELETE clause for the Cypher query, specifying elements that should be removed
     * as a part of applying the rule. This includes nodes and edges marked for deletion.
     * 
     * @param rule The Henshin rule being processed.
     * @return The DELETE clause of the Cypher query.
     */

    private String buildDeleteClause(Rule rule) {
        Set<String> deletableElements = new HashSet<>();

        deletableElements.addAll(rule.getLhs().getEdges().stream()
            .filter(edge -> edge.getAction() != null && edge.getAction().getType() == Type.DELETE)
            .map(Edge::getIndex) 
            .collect(Collectors.toSet()));
        
        deletableElements.addAll(rule.getLhs().getNodes().stream()
            .filter(node -> node.getAction() != null && node.getAction().getType() == Type.DELETE)
            .map(Node::getName)
            .collect(Collectors.toSet()));

        return deletableElements.isEmpty() ? "" : "\nDELETE " + String.join(", ", deletableElements);
    }
    
    /**
     * Constructs the CREATE clause for the Cypher query, detailing the elements
     * that should be added to the graph as a result of the rule's application.
     * This typically involves nodes and edges marked with a CREATE action.
     * 
     * @param rule The Henshin rule being processed.
     * @return The CREATE clause of the Cypher query.
     */

    private String buildCreateClause(Rule rule) {
        StringBuilder createClauseBuilder = new StringBuilder();

        List<String> createdNodes = rule.getRhs().getNodes().stream()
            .filter(node -> node.getAction() != null && node.getAction().getType() == Type.CREATE)
            .map(node -> translateNode(node, Type.CREATE)) 
            .collect(Collectors.toList());

        List<String> createdEdges = new ArrayList<>();
        rule.getRhs().getEdges().stream()
            .filter(edge -> edge.getAction() != null && edge.getAction().getType() == Type.CREATE)
            .forEach(edge -> {
                String sourceName = edge.getSource().getName();
                String targetName = edge.getTarget().getName();
                String edgeRepresentation = String.format("(%s)-[%s:%s]->(%s)", 
                    sourceName, edge.getIndex(), edge.getType().getName().toUpperCase(), targetName);
                createdEdges.add(edgeRepresentation);
            });

        if (!createdNodes.isEmpty() || !createdEdges.isEmpty()) {
            createClauseBuilder.append("\nCREATE ");

            String combinedElements = Stream.concat(createdNodes.stream(), createdEdges.stream())
                                            .collect(Collectors.joining(", "));
            
            createClauseBuilder.append(combinedElements);
        }

        return createClauseBuilder.toString();
    }
    
    /**
     * Translates a Henshin model node into a Cypher query representation based on its action type.
     * This method handles different node actions (CREATE, DELETE, PRESERVE, FORBID) by
     * formatting the node appropriately for the Cypher query.
     * 
     * @param node The node to translate.
     * @param actionType The action type of the node, determining its treatment in the query.
     * @return A string representation of the node for inclusion in the Cypher query.
     */

    private String translateNode(Node node, Action.Type actionType) {
        String nodeName = node.getName();
        String nodeType = node.getType().getName();
        //String attributes = translateAttributes(node);
        boolean isFirstMatch = visitedNodes.add(nodeName); 

        switch (actionType) {
            case CREATE:
                return String.format("(%s:%s)", nodeName, nodeType);
            case DELETE:

                return isFirstMatch ? String.format("(%s:%s)", nodeName, nodeType) : 
                                      String.format("(%s)", nodeName);
            case PRESERVE:
                return isFirstMatch ? String.format("(%s:%s)", nodeName, nodeType) : 
                                      String.format("(%s)", nodeName);
            case FORBID:
                return String.format("(:%s)", nodeType);
            default:
                return "";
        }
    }
    
    /**
     * Formats the attributes of a node for inclusion in a Cypher query.
     * This method compiles the node's attributes into a string that can be included
     * in the query to match or create nodes with specific properties.
     * 
     * @param node The node whose attributes are to be formatted.
     * @return A string representation of the node's attributes.
     */

    /*private String translateAttributes(Node node) {
        return node.getAttributes().isEmpty() ? "" :
            node.getAttributes().stream()
                .map(attr -> String.format("%s: '%s'", attr.getType().getName(), attr.getValue()))
                .collect(Collectors.joining(", ", " {", "}"));
    }*/
    
    /**
    * Translates a Henshin model edge into a Cypher query representation, maintaining
    * the edge's direction and type. This method is crucial for accurately reflecting
    * the graph's structure in the generated Cypher query.
    * 
    * @param edge The edge to translate.
    * @return A string representation of the edge for inclusion in the Cypher query.
    */

    private String translateEdge(Edge edge) {
        return String.format("-[%s:%s]->", edge.getIndex(), edge.getType().getName().toUpperCase());
    }
    
    /**
     * Generates a unique identifier for an edge based on its source, target, and type.
     * This identifier is used to track which edges have been processed and to avoid
     * duplicating work within the translation process.
     * 
     * @param edge The edge for which to generate an identifier.
     * @return A unique identifier string for the edge.
     */

    private String generateEdgeIdentifier(Edge edge) {
        return edge.getSource().getName() + "-" + edge.getType().getName().toUpperCase() + "->" + edge.getTarget().getName();
    }

}