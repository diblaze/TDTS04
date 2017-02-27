import javax.swing.*;

//Ugly way to make an 'struct' in Java........... WHY?!
class RouterStruct {
  int hopTo = 0;
  int distanceTo = 0;
}

public class RouterNode {
  private int myID;
  private GuiTextArea myGUI;
  private RouterSimulator sim;
  //costs to nodes
  private int[] linkCosts = new int[RouterSimulator.NUM_NODES];
  //distance to specific node of specific neighbour
  private int[][] distanceToOfNeighbour = new int[RouterSimulator.NUM_NODES][RouterSimulator.NUM_NODES];
  //boolean table - keeps track of who is neighbour to 'this' node.
  private final boolean[] isNeighbour = new boolean[RouterSimulator.NUM_NODES];
  //keep the best routes in a RouterStruct table for easier access.
  private RouterStruct[] bestRouteToNode = new RouterStruct[RouterSimulator.NUM_NODES];

  private final boolean POISON = true;

  //--------------------------------------------------
  public RouterNode(int ID, RouterSimulator sim, int[] linkCosts) {
    myID = ID;
    this.sim = sim;
    myGUI = new GuiTextArea("  Output window for Router #" + ID + "  ");

    System.arraycopy(linkCosts, 0, this.linkCosts, 0, RouterSimulator.NUM_NODES);

    //initialize the distanceToOfNeighbour table. 
    //Set initial routes and determine inital neighbours.
    for (int i = 0; i < RouterSimulator.NUM_NODES; ++i) {
      bestRouteToNode[i] = new RouterStruct();
      bestRouteToNode[i].hopTo = i;

      //checks if is neighbor
      //if current best route is not to oneself and has an distance of infinity 
      isNeighbour[i] = ((bestRouteToNode[i].distanceTo = linkCosts[i]) != RouterSimulator.INFINITY && i != myID) ? true
          : false;

    }

    //fills the distanceToOfNeighbour[myID] to all corrosponding linkCosts. 
    System.arraycopy(linkCosts, 0, this.distanceToOfNeighbour[myID], 0, RouterSimulator.NUM_NODES);

    printDistanceTable();

    //force update to all nodes 
    updateNeighbours();

  }

  private void updateNeighbours() {

    RouterPacket updatePacket;

    //temp array of all linkCosts
    int[] tempCosts = new int[RouterSimulator.NUM_NODES];
    for (int i = 0; i < RouterSimulator.NUM_NODES; ++i) {
      tempCosts[i] = bestRouteToNode[i].distanceTo;
    }

    //update packet for all neighbours
    for (int i = 0; i < RouterSimulator.NUM_NODES; ++i) {
      //not a neighbour, continue to next node
      if (!isNeighbour[i]) {
        continue;
      }
      updatePacket = new RouterPacket(myID, i, tempCosts);

      //poison here
      if (POISON) {
        for (int j = 0; j < RouterSimulator.NUM_NODES; ++j) {
          if (bestRouteToNode[j].hopTo == i) {
            updatePacket.mincost[j] = RouterSimulator.INFINITY;
          }

        }
      }
      sendUpdate(updatePacket);
    }

  }

  //--------------------------------------------------
  public void recvUpdate(RouterPacket pkt) {
    boolean routeChanged = false;
    System.out.println("UPDATE " + myID);

    int sourceId = pkt.sourceid;
    RouterStruct route = new RouterStruct();

    //lets check the packet content
    for (int i = 0; i < RouterSimulator.NUM_NODES; ++i) {
      //no change in costs
      if (pkt.mincost[i] == distanceToOfNeighbour[i][sourceId]) {
        System.out.println("SAME COST " + myID);
        System.out.println("mincost " + pkt.mincost[i]);
        System.out.println("distanceToOfNeighbour " + distanceToOfNeighbour[i][sourceId]);
        continue;

      } else if (pkt.mincost[i] < distanceToOfNeighbour[i][sourceId]) {
        //update local table
        distanceToOfNeighbour[i][sourceId] = pkt.mincost[i];

        //possible to improve routing?
        if (linkCosts[sourceId] + pkt.mincost[i] < bestRouteToNode[i].distanceTo) {
          bestRouteToNode[i].distanceTo = linkCosts[sourceId] + pkt.mincost[i];
          bestRouteToNode[i].hopTo = sourceId;
          routeChanged = true;
        }
      }
    }

    if (routeChanged) {
      updateNeighbours();
    }

    printDistanceTable();
  }

  //--------------------------------------------------
  private void sendUpdate(RouterPacket pkt) {
    sim.toLayer2(pkt);

  }

  //--------------------------------------------------
  public void updateLinkCost(int dest, int newcost) {

    boolean routeChanged = false;
    int oldcost = linkCosts[dest];

    RouterStruct minroute = new RouterStruct();

    //update linkCosts
    linkCosts[dest] = newcost;

    //if newcost is better than oldcost, check if we can update the route

    if (newcost < oldcost) {
      for (int i = 0; i < RouterSimulator.NUM_NODES; ++i) {
        if (newcost + distanceToOfNeighbour[i][dest] < bestRouteToNode[i].distanceTo) {
          bestRouteToNode[i].distanceTo = newcost + distanceToOfNeighbour[i][dest];
          bestRouteToNode[i].hopTo = dest;
          routeChanged = true;
        }
      }
    } else {
      for (int i = 0; i < RouterSimulator.NUM_NODES; ++i) {
        if (bestRouteToNode[i].hopTo == dest) {
          minroute.distanceTo = RouterSimulator.INFINITY;
          //We have no clues which route to i could possibly be optimal. We must check them all!
          //Find the best route/distance to i based on all saved tables of nodes j.
          for (int j = 0; j < RouterSimulator.NUM_NODES; ++j) {
            if (!isNeighbour[j]) {
              continue;
            }

            if (linkCosts[j] + distanceToOfNeighbour[i][j] < minroute.distanceTo) {
              minroute.distanceTo = linkCosts[j] + distanceToOfNeighbour[i][j];
              minroute.hopTo = j;
            }
          }
          //New best route in minroute.
          bestRouteToNode[i].distanceTo = minroute.distanceTo;
          bestRouteToNode[i].hopTo = minroute.hopTo;
          routeChanged = true;
        }
      }
    }

    if (routeChanged)

    {
      updateNeighbours();
    }

    printDistanceTable();

    /*
    linkCosts[dest] = newcost;
    
    RouterPacket pkt = new RouterPacket(myID, myID, distanceToOfNeighbour[myID]);
    printDistanceTable();
    recvUpdate(pkt);
    
    */

  }

  //--------------------------------------------------
  //TODO: Fix printDistanceTable
  public void printDistanceTable() {
    myGUI.println("Current table for " + myID + " at time " + sim.getClocktime());

    for (int i = 0; i < RouterSimulator.NUM_NODES; ++i)
      myGUI.print("    " + i);
    myGUI.println();

    for (int i = 0; i < RouterSimulator.NUM_NODES; ++i)
      myGUI.print("____");
    myGUI.println();

    String extraspaces = "";

    for (int i = 0; i < RouterSimulator.NUM_NODES; ++i) {
      if (!isNeighbour[i])
        continue;

      myGUI.print(i + "    |");

      for (int j = 0; j < RouterSimulator.NUM_NODES; ++j) {
        if (distanceToOfNeighbour[j][i] / 100 != 0) {
          extraspaces = "";
        } else if (distanceToOfNeighbour[j][i] / 10 != 0) {
          extraspaces = " ";
        } else {
          extraspaces = "  ";
        }
        myGUI.print(" " + extraspaces + distanceToOfNeighbour[j][i]);
      }
      myGUI.println();
    }

    myGUI.println("Local routing table:");

    myGUI.print("      to");
    for (int i = 0; i < RouterSimulator.NUM_NODES; ++i) {
      if (i != myID)
        myGUI.print("   " + i);
    }
    myGUI.println();

    myGUI.print("distance");
    for (int i = 0; i < RouterSimulator.NUM_NODES; ++i) {
      if (i == myID)
        continue;

      if (bestRouteToNode[i].distanceTo / 100 != 0) {

        extraspaces = "";
      } else if (bestRouteToNode[i].distanceTo / 10 != 0) {
        extraspaces = " ";
      } else {
        extraspaces = "  ";
      }
      myGUI.print(" " + extraspaces + bestRouteToNode[i].distanceTo);
    }

    myGUI.println();

    myGUI.print("First Hop");
    for (int i = 0; i < RouterSimulator.NUM_NODES; ++i) {
      if (i != myID)
        myGUI.print("   " + bestRouteToNode[i].hopTo);
    }
    myGUI.println();
    myGUI.println();
  }

}
