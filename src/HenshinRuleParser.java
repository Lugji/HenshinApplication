import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.emf.henshin.model.Action;
import org.eclipse.emf.henshin.model.Action.Type;
import org.eclipse.emf.henshin.model.Attribute;
import org.eclipse.emf.henshin.model.Edge;
import org.eclipse.emf.henshin.model.Graph;
import org.eclipse.emf.henshin.model.GraphElement;
import org.eclipse.emf.henshin.model.Module;
import org.eclipse.emf.henshin.model.NestedCondition;
import org.eclipse.emf.henshin.model.Node;
import org.eclipse.emf.henshin.model.Rule;
import org.eclipse.emf.henshin.model.Unit;

public class HenshinRuleParser {

    public static void parseRules(Module module) {
        for (Unit unit : module.getUnits()) {
            if (unit instanceof Rule) {
                Rule rule = (Rule) unit;
                processRule(rule);
            }
        }
    }

    public static void processRule(Rule rule) {
        StringBuilder cypherQuery = new StringBuilder();

        translateLHS(rule, cypherQuery);
        translateRHS(rule, cypherQuery);

        System.out.println("Cypher Query for Rule '" + rule.getName() + "':\n");
        System.out.println(cypherQuery.toString());
        System.out.println();
    }

    public static void translateLHS(Rule rule, StringBuilder cypherQuery) {
        translateMatch(rule.getLhs(), cypherQuery,true);
        translateNACs(rule.getLhs().getNACs(), cypherQuery);
        translateElements(Stream.concat(rule.getLhs().getNodes().stream(), rule.getLhs().getEdges().stream())
                .collect(Collectors.toList()), cypherQuery, Action.Type.DELETE, false);
    }

    public static void translateRHS(Rule rule, StringBuilder cypherQuery) {
        translateElements(Stream.concat(rule.getRhs().getNodes().stream(), rule.getRhs().getEdges().stream())
                .collect(Collectors.toList()), cypherQuery, Action.Type.CREATE, false);
    }

    private static void translateEdges(List<Edge> edges, StringBuilder cypherQuery, boolean isMatchClause) {
        edges.forEach(edge -> {
            cypherQuery.append(translateEdge(edge, isMatchClause));
            if (edges.indexOf(edge) < edges.size() - 1) {
            	if (edge.getAction().getType() == Type.FORBID) {
            		cypherQuery.append(" AND ");				
				} else {
					cypherQuery.append(", ");
				}
            }
        });
    }

    private static void translateNodes(List<Node> nodes, StringBuilder cypherQuery, boolean isMatchClause) {
        nodes.forEach(node -> cypherQuery.append(translateNode(node, isMatchClause)));
    }

    private static String translateNode(Node node, boolean isMatchClause) {
        StringBuilder cypherQuery = new StringBuilder();
        switch (node.getAction().getType()) {
            case PRESERVE:
            	cypherQuery.append(String.format("(%s:%s", node.getName(), node.getType().getName()));
                appendNodeAttributes(node, cypherQuery);
                cypherQuery.append(")");
                break;
            case DELETE:
                if (!isMatchClause) {
                    cypherQuery.append(String.format("(%s)", node.getName()));
                } else {
                	cypherQuery.append(String.format("(%s:%s", node.getName(), node.getType().getName()));
                    appendNodeAttributes(node, cypherQuery);
                    cypherQuery.append(")");
				}
                break;
            case CREATE:
            	cypherQuery.append(String.format("(%s:%s", node.getName(), node.getType().getName()));
                appendNodeAttributes(node, cypherQuery);
                cypherQuery.append(")");
                break;
            case FORBID:
            	cypherQuery.append(String.format("(%s:%s", node.getName(), node.getType().getName()));
                appendNodeAttributes(node, cypherQuery);
                cypherQuery.append(")");
                break;
            case REQUIRE:
            	break;
                
        }
        cypherQuery.append(", ");
        return cypherQuery.toString();
    }

    private static void appendNodeAttributes(Node node, StringBuilder cypherQuery) {
        if (!node.getAttributes().isEmpty()) {
            cypherQuery.append(" {");

            StringJoiner attributeJoiner = new StringJoiner(", ");
            for (Attribute attribute : node.getAttributes()) {
                String attributeName = attribute.getType().getName();
                String attributeValue = attribute.getValue();
                attributeJoiner.add(String.format("%s:%s", attributeName, attributeValue));
            }

            cypherQuery.append(attributeJoiner);
            cypherQuery.append("}");
        }
    }

    private static String translateEdge(Edge edge, boolean isMatchClause) {
        String relationshipType = edge.getType().getName();
        String sourceNodeName = edge.getSource().getName();
        String targetNodeName = edge.getTarget().getName();
        String labelName = edge.getTarget().getType().getName();
        String variableNameString = edge.getIndex();

        StringBuilder edgeString = new StringBuilder();
        
        if (edge.getAction() != null && edge.getAction().getType() == Action.Type.DELETE) {
        	if (!isMatchClause) {
        		edgeString.append(String.format("%s", variableNameString));
			} else {
	            edgeString.append(String.format("(%s)-[%s:%s]->(%s)", sourceNodeName,variableNameString,relationshipType, targetNodeName));
				
			}
			
		} else if (edge.getAction() != null && edge.getAction().getType() == Action.Type.FORBID) {
        	edgeString.append(String.format("(%s", sourceNodeName));
            appendNodeAttributes(edge.getSource(), edgeString);
            edgeString.append(String.format(")-[:%s]->(:%s",relationshipType, labelName));
            appendNodeAttributes(edge.getTarget(), edgeString);
            edgeString.append(")");
			
		} else {
        	edgeString.append(String.format("(%s", sourceNodeName));
            edgeString.append(String.format(")-[%s:%s]->(%s", variableNameString,relationshipType, targetNodeName));
            edgeString.append(")");
		}

        return edgeString.toString();
    }

    private static void translateMatch(Graph lhsGraph, StringBuilder cypherQuery, boolean isMatchClause) {
        cypherQuery.append("MATCH ");
        translateNodes(lhsGraph.getNodes(), cypherQuery, true);
        translateEdges(lhsGraph.getEdges(), cypherQuery, true);
    }

    private static void translateElements(List<GraphElement> elements, StringBuilder cypherQuery, Action.Type actionType, boolean isMatchClause) {
        List<Node> nodes = findNodesWithAction(elements.stream()
                .filter(element -> element instanceof Node)
                .map(element -> (Node) element).collect(Collectors.toList()), actionType);
        List<Edge> edges = findEdgesWithAction(elements.stream()
                .filter(element -> element instanceof Edge)
                .map(element -> (Edge) element).collect(Collectors.toList()), actionType);

        if (!nodes.isEmpty() || !edges.isEmpty()) {
            cypherQuery.append("\n");
            if (actionType == Action.Type.DELETE) {
            	cypherQuery.append("DELETE ");
                translateNodes(nodes, cypherQuery, false);
                translateEdges(edges, cypherQuery, false);
            } else if (actionType == Action.Type.CREATE) {
                cypherQuery.append("CREATE ");
                translateNodes(nodes, cypherQuery, false);
                translateEdges(edges, cypherQuery, false);
            }
        }
    }

    private static void translateNACs(List<NestedCondition> nacs, StringBuilder cypherQuery) {
        if (!nacs.isEmpty()) {
            cypherQuery.append("\nWHERE NOT ");
            nacs.forEach(nac -> translateForbiddenElements(nac.getConclusion(), cypherQuery));
        }
    }

    private static void translateForbiddenElements(Graph graph, StringBuilder cypherQuery) {
        //List<Node> forbiddenNodes = findNodesWithAction(graph.getNodes(), Action.Type.FORBID);
        List<Edge> forbiddenEdges = findEdgesWithAction(graph.getEdges(), Action.Type.FORBID);
        //translateNodes(forbiddenNodes, cypherQuery, false);
        translateEdges(forbiddenEdges, cypherQuery,false);
    }

    private static List<Node> findNodesWithAction(List<Node> nodes, Action.Type actionType) {
        return nodes.stream()
                .filter(node -> node.getAction() != null && node.getAction().getType() == actionType)
                .collect(Collectors.toList());
    }

    private static List<Edge> findEdgesWithAction(List<Edge> edges, Action.Type actionType) {
        return edges.stream()
                .filter(edge -> edge.getAction() != null && edge.getAction().getType() == actionType)
                .collect(Collectors.toList());
    }

}