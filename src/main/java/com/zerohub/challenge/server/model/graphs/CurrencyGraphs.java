package com.zerohub.challenge.server.model.graphs;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.graph.Graph;
import com.google.common.graph.SuccessorsFunction;

import java.util.*;

import static com.google.common.collect.Sets.difference;
import static com.google.common.graph.Graphs.reachableNodes;

public class CurrencyGraphs {

    /**
     *
     * Get shortest path in graph between 2 nodes.
     * @param graph
     * @param nodeU
     * @param nodeV
     * @param <N> type of nodes
     * @return path
     * @throws IllegalArgumentException if {@code nodeU} or {@code nodeV} is not present in {@code graph}
     */
    public static <N> ImmutableList<N> shortestPath(SuccessorsFunction<N> graph, N nodeU, N nodeV) {
        if (nodeU.equals(nodeV)) {
            return ImmutableList.of(nodeU);
        }
        Set<N> successors = ImmutableSet.copyOf(graph.successors(nodeU));
        if (successors.contains(nodeV)) {
            return ImmutableList.of(nodeU, nodeV);
        }

        Map<N, N> visitedNodeToPathPredecessor = new HashMap<>(); // encodes shortest path tree
        for (N node : successors) {
            visitedNodeToPathPredecessor.put(node, nodeU);
        }
        Queue<N> currentNodes = new ArrayDeque<N>(successors);
        Queue<N> nextNodes = new ArrayDeque<N>();

        // Perform a breadth-first traversal starting with the successors of nodeU.
        while (!currentNodes.isEmpty()) {
            while (!currentNodes.isEmpty()) {
                N currentNode = currentNodes.remove();
                for (N nextNode : graph.successors(currentNode)) {
                    if (visitedNodeToPathPredecessor.containsKey(nextNode)) {
                        continue; // we already have a shortest path to nextNode
                    }
                    visitedNodeToPathPredecessor.put(nextNode, currentNode);
                    if (nextNode.equals(nodeV)) {
                        ImmutableList.Builder<N> builder = ImmutableList.builder();
                        N node = nodeV;
                        builder.add(node);
                        while (!node.equals(nodeU)) {
                            node = visitedNodeToPathPredecessor.get(node);
                            builder.add(node);
                        }
                        return builder.build().reverse();
                    }
                    nextNodes.add(nextNode);
                }
            }
            Queue<N> emptyQueue = currentNodes;
            currentNodes = nextNodes;
            nextNodes = emptyQueue; // reusing empty queue faster than allocating new one
        }

        return ImmutableList.of();
    }

    /**
     *
     * Returns the nodes in a graph that are not reachable from a node.
     * @param graph
     * @param node
     * @param <N>
     * @return set of nodes
     */
    public static <N> ImmutableSet<N> unreachableNodes(Graph<N> graph, N node) {
        return ImmutableSet.copyOf(difference(graph.nodes(), reachableNodes(graph, node)));
    }
}
