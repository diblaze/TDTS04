import javax.swing.*;

import com.sun.glass.ui.SystemClipboard;

import java.util.*;
//import java.util.concurrent.LinkedBlockingDeque;

public class RouterNode {
  private int sizeOfNetwork = RouterSimulator.NUM_NODES;
  private int INFINITY = RouterSimulator.INFINITY;
  private GuiTextArea myGUI;
  private RouterSimulator sim;

  //Unikt node nummer
  private int myID;
  //Kostnad till alla noder i nätverket.
  //
  private int[] total_cost_to_node = new int[sizeOfNetwork];
  //Best route to nodes - next hop
  private int[] best_route_to_node = new int[sizeOfNetwork];
  //Neighbors costs
  private HashMap<Integer, int[]> neighbours_total_cost_to_node = new HashMap<Integer, int[]>();
  //Costs between us and our neighbour
  private HashMap<Integer, Integer> link_cost_to_neighbours = new HashMap<Integer, Integer>();

  //use poison REVERSE
  private final boolean usePoison = true;

  //--------------------------------------------------
  public RouterNode(int ID, RouterSimulator sim, int[] costs) {
    myID = ID;
    this.sim = sim;
    myGUI = new GuiTextArea("  Output window for Router #" + ID + "  ");
    System.arraycopy(costs, 0, this.total_cost_to_node, 0, sizeOfNetwork);

    //Initializes all of the neighbours.
    for (int i = 0; i < sizeOfNetwork; i++) {
      //If the current node does not have a cost of 0 or INFINITY - go on
      if (costs[i] != 0 && costs[i] != INFINITY) {
        //Put the link cost to the neighbour.
        link_cost_to_neighbours.put(i, costs[i]);
        //Keeps track of neighbours cost to other nodes.
        int[] current_nbrCosts = new int[sizeOfNetwork];
        //Fill the array with INFINITY - initializes.
        Arrays.fill(current_nbrCosts, INFINITY);
        //Change our nodes cost in the array
        current_nbrCosts[myID] = costs[i];
        neighbours_total_cost_to_node.put(i, current_nbrCosts);
      }

      //Initalizes the routes to each node.
      //If the cost to the node is NOT INFINITY - means we can reach it.
      if (costs[i] != INFINITY) {
        best_route_to_node[i] = i;
      } else
      //If the cost to the node is INFINITY - we can not reach it.
      {
        best_route_to_node[i] = INFINITY;
      }
    }
    printDistanceTable();

    //Informs the current nodes neighbours about all changes.
    updateNeighbours();
  }

  //--------------------------------------------------
  /** **/
  public void recvUpdate(RouterPacket pkt) {
    //Store RouterPacket source in temp variable.
    int src = pkt.sourceid;
    //Store the RouterPacket costs in a temp array.
    int[] updatedCosts = pkt.mincost;
    //Update our costs with the new costs from the RouterPacket.
    System.arraycopy(updatedCosts, 0, neighbours_total_cost_to_node.get(src), 0, sizeOfNetwork);

    //Check if any route has changed.
    if (routeChanged()) {
      //Inform the neighbours if so.
      updateNeighbours();
    }
    //Print to GUI.
    printDistanceTable();
  }

  //--------------------------------------------------
  private void updateNeighbours() {
    //For every node in our neighbours node list, send an RouterPacket and update them.
    for (int dest : neighbours_total_cost_to_node.keySet()) {
      RouterPacket pkt = new RouterPacket(myID, dest, total_cost_to_node);
      sendUpdate(pkt);
    }
  }

  //--------------------------------------------------
  private void sendUpdate(RouterPacket pkt) {
    //If we are using Poison Reverse technique then do it here before actually updating everything.
    if (usePoison) {
      pkt = poisonRoute(pkt);
    }

    sim.toLayer2(pkt);
  }

  //TODO: Check if it is working!
  private RouterPacket poisonRoute(RouterPacket pkt) {

    boolean changed = false;
    int[] tempCosts = new int[RouterSimulator.NUM_NODES];
    //Go through each node.
    //for (int i = 0; i < sizeOfNetwork; ++i) {

      //is node I our neighbor
      //if (link_cost_to_neighbours.get(i) == null) {
        //continue;
      //}

      //copy over costs to our temp array
      System.arraycopy(total_cost_to_node, 0, tempCosts, 0, RouterSimulator.NUM_NODES);

      //go through all nodes
      for (int j = 0; j < RouterSimulator.NUM_NODES; ++j) {

        //if we are at the same node I then skip this iteration.
        if (j == pkt.destid) {
          continue;
        }

        //if node J has a best route to hop back to node I, then change it to INFINITY.
        if (best_route_to_node[j] == pkt.destid) {
          tempCosts[j] = RouterSimulator.INFINITY;
          changed = true;
        }
      }
    //}

    //if somethings has changed, then update the packet costs.
    if (changed) {
      pkt.mincost = tempCosts;
    }

    return pkt;
  }

  //--------------------------------------------------
  public void updateLinkCost(int dest, int newcost) {
    //Update incoming new cost
    link_cost_to_neighbours.put(dest, newcost);

    //Check if any route has changed.
    if (routeChanged()) {
      //Inform the neighbours if so.
      updateNeighbours();
    }

    //Print to GUI.
    printDistanceTable();
  }

  //--------------------------------------------------
  private boolean routeChanged() {
    //Bool to keep track of the route changes.
    boolean changed = false;

    //for each node in the simulator
    for (int i = 0; i < sizeOfNetwork; ++i) {
      //skip ourselves
      if (i == myID) {
        continue;
      }

      //create new temp variables for calculating new routes.
      int minCost = INFINITY;
      int nextHop = -1; //DONT PUT TO 0! Will try to hop to Node 0.

      //For every neighbour in our link_cost_to_neighbours HashMap
      for (Map.Entry<Integer, Integer> neighbour : link_cost_to_neighbours.entrySet()) {
        //Store the neighbours node ID in temp variable.
        int neighbourID = neighbour.getKey();
        //Store the cost to the neighbour in temp variable.
        int cost_to_neighbour = neighbour.getValue();

        //Get the neighbours costs table.
        int[] neighbour_costs = neighbours_total_cost_to_node.get(neighbourID);

        //If the (neighbours cost to node + our own cost to current node) is better than the current temp minimum cost.
        if ((neighbour_costs[i] + cost_to_neighbour) < minCost) {
          //We found a better route!
          //Set the minimum cost to above.
          minCost = neighbour_costs[i] + cost_to_neighbour;
          //Set the best route to current neighbour iterated.
          nextHop = neighbourID;
        }
      }
      //Double check to make sure that minimum cost is not the same as the current cost,
      //nor should the current best route be the same.
      if (minCost != total_cost_to_node[i] || nextHop != best_route_to_node[i]) {
        //The minimum cost and best route has not changed!
        changed = true; //false
      }

      //The minimum cost and best route has changed!
      total_cost_to_node[i] = minCost;
      best_route_to_node[i] = nextHop;
    }

    return changed;
  }

  //--------------------------------------------------
  //Evil mess of GUI stuff...
  public void printDistanceTable() {
    myGUI.println("Current table for " + myID + "  at time " + sim.getClocktime());
    myGUI.println("NEIGHBORS:");
    myGUI.print(String.format("%-13s", "Node nr "));
    String rowBreak = "\n---------";
    for (int i = 0; i < sizeOfNetwork; i++) {
      myGUI.print(String.format("%1$-7d", i));
      rowBreak += "---------";
    }
    myGUI.println(rowBreak);

    for (int ID : neighbours_total_cost_to_node.keySet()) {
      myGUI.print(String.format("Node: %1$-7d", ID));

      for (int i = 0; i < sizeOfNetwork; i++) {
        if (neighbours_total_cost_to_node.get(ID)[i] != INFINITY) {
          myGUI.print(String.format("%1$-7d", neighbours_total_cost_to_node.get(ID)[i]));
        } else {
          myGUI.print(String.format("%1$-7s", "X"));
        }
      }
      myGUI.println();
    }
    myGUI.println("\nBest route to node:");
    for (int ID = 0; ID < sizeOfNetwork; ID++) {
      myGUI.print(String.format("Node: %1$-7d", ID));
      if (best_route_to_node[ID] != INFINITY) {
        myGUI.println(String.format("%1$-7d", best_route_to_node[ID]));
      } else {
        myGUI.println(String.format("%1$-7s", "X"));
      }
    }

    myGUI.println("\nTotal cost to node:");
    for (int ID = 0; ID < sizeOfNetwork; ID++) {
      myGUI.print(String.format("Node: %1$-7d", ID));
      if (total_cost_to_node[ID] != INFINITY) {
        myGUI.println(String.format("%1$-7d", total_cost_to_node[ID]));
      } else {
        myGUI.println(String.format("%1$-7s", "X"));
      }
    }
    myGUI.println();
  }
}
