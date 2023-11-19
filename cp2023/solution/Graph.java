package cp2023.solution;

import cp2023.base.ComponentId;
import cp2023.base.DeviceId;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.Semaphore;

public class Graph {
    private Map<DeviceId, Node> dev_nodes_map;
    private final Map<ComponentId, Semaphore> semaphoreComponentTransfer;
    public Graph(Map<DeviceId, Integer> devTotalSlots, Map<ComponentId, Semaphore> semaphoreComponentTransfer)
    {
        this.semaphoreComponentTransfer = semaphoreComponentTransfer;
        dev_nodes_map = new HashMap<>();

        for(DeviceId dev : devTotalSlots.keySet())
            dev_nodes_map.put(dev, new Node(dev));
    }

    private boolean findCycle_dfs(Node currNode, Map<DeviceId, Boolean> visited,
                                  Map<DeviceId, Pair<Boolean, Integer>> recursionStack,
                                  DeviceId finalNode, Integer startingEdgeIdx)
    {
        if(currNode == null)
            return false;

        DeviceId currDevId = currNode.getDevId();

        if(recursionStack.get(currDevId).first)
            return true;
        if(visited.get(currNode.getDevId()))
            return false;

        visited.put(currDevId, true);
        Vector<Node> transfersToMe = currNode.getTransfersToMe();

        if(currDevId.equals(finalNode))
        {
            recursionStack.put(currNode.getDevId(), new Pair<>(true, startingEdgeIdx));

            if(findCycle_dfs(transfersToMe.get(startingEdgeIdx), visited, recursionStack,
                    finalNode, startingEdgeIdx))
            {
                return true;
            }
            recursionStack.put(currNode.getDevId(), new Pair<>(false, startingEdgeIdx));
        }
        else
        {
            int size = transfersToMe.size();
            for(int i = 0; i < size; i++)
            {
                // na stosie odkladamy ze aktualne devId bylo rozpatrzone i odkladamy
                // ktora krawedzia poszlismy
                recursionStack.put(currNode.getDevId(), new Pair<>(true, i));

                if(findCycle_dfs(transfersToMe.get(i), visited, recursionStack,
                        finalNode, startingEdgeIdx))
                {
                    return true;
                }
            }
            recursionStack.put(currNode.getDevId(), new Pair<>(false, size - 1));
        }

        return false;
    }
    private Integer addTransfer(DeviceId srcDev, DeviceId destDev, ComponentId compId)
    {
        if(srcDev != null)
            return dev_nodes_map.get(destDev).addEdge(dev_nodes_map.get(srcDev), compId);
        return dev_nodes_map.get(destDev).addEdge(null, compId);
    }

    private void removeCycle(Map<DeviceId, Pair<Boolean, Integer>> recursionStack) throws Exception
    {
        //System.out.printf("Usuwam komponenty z cyklu: ");
        for(DeviceId devId : recursionStack.keySet())
        {
            Pair<Boolean, Integer> inCycle_priorityIdx = recursionStack.get(devId);
            if(inCycle_priorityIdx.first)
            {
                ComponentId compId = dev_nodes_map.get(devId).removeEdge(inCycle_priorityIdx.second);
                semaphoreComponentTransfer.get(compId).release();
                //System.out.printf(compId + " ");
            }
        }
        //System.out.println();
    }
    public void freeSpaceOnDev(DeviceId devId)
    {
        // zdejmujemy z grafu transfer o najwiekszym priorytecie na devId
        // tylko transfer typu REMOVE moze to zrobic bo zwalnia miejsce
        try
        {
            ComponentId compId = dev_nodes_map.get(devId).removeEdge(0);
            semaphoreComponentTransfer.get(compId).release();
        }
        catch(Exception e)
        {
            System.out.println("Graph - freeSpaceOnDev - " + e);
        }

    }
    public void checkCycle(DeviceId srcDev, DeviceId destDev, ComponentId compId)
    {
        // Uzyjemy algorytmu dfs do znalezienia cyklu w naszym grafie
        Integer myIdx = addTransfer(srcDev, destDev, compId);
        Map<DeviceId, Boolean> visited = new HashMap<>();
        // potrzebujemy wiedziec jeszcze jaki indeks transferu jest w naszym cyklu
        Map<DeviceId, Pair<Boolean, Integer>> recursionStack = new HashMap<>();

        for(DeviceId dev : dev_nodes_map.keySet())
        {
            visited.put(dev, false);
            recursionStack.put(dev, new Pair<>(false, 0));
        }

        if(findCycle_dfs(dev_nodes_map.get(destDev), visited, recursionStack, destDev, myIdx))
        {
            //System.out.println("JEST CYKL");
            // jesli znajdziemy cykl to go usuwamy i wypuszczamy czekajce na semaforach transfery
            try
            {
                removeCycle(recursionStack);
            }
            catch(Exception e)
            {
                System.out.println("Graph - checkCycle - " + e);
            }
            //System.out.println("Transfer domykajacy: " + srcDev + " -> " + destDev + ", komponentu: " + compId);
        }
//        else
//        {
//            System.out.println("NIE MA CYKLU");
//            System.out.println("Dodalem transfer: " + srcDev + " -> " + destDev + ", komponentu: " + compId);
//        }


    }

    public static void main(String[] args)
    {
        DeviceId d1 = new DeviceId(1);
        DeviceId d2 = new DeviceId(2);
        DeviceId d3 = new DeviceId(3);

        Map<DeviceId, Integer> devTotalSlots;
        devTotalSlots = new HashMap<>();
        devTotalSlots.put(d1, 2);
        devTotalSlots.put(d2, 2);
        devTotalSlots.put(d3, 3);

        ComponentId comp1 = new ComponentId(1);
        ComponentId comp2 = new ComponentId(2);
        ComponentId comp3 = new ComponentId(3);
        ComponentId comp4 = new ComponentId(4);

        Map<ComponentId, Semaphore> semaphoreComponentTransfer = new HashMap<>();
        semaphoreComponentTransfer.put(comp1, new Semaphore(0, true));
        semaphoreComponentTransfer.put(comp2, new Semaphore(0, true));
        semaphoreComponentTransfer.put(comp3, new Semaphore(0, true));
        semaphoreComponentTransfer.put(comp4, new Semaphore(0, true));

        Graph g1 = new Graph(devTotalSlots, semaphoreComponentTransfer);
        // ta czesc okej
//        g1.checkCycle(d1, d2, comp1);
//        g1.checkCycle(d2, d1, comp2);
//
//        g1.checkCycle(d1, d2, comp1);
//        g1.checkCycle(d1, d2, comp2);
//        g1.checkCycle(d2, d3, comp3);
//        g1.checkCycle(d3, d1, comp4);
//        g1.checkCycle(d2, d1, comp1);

        // teraz testowanie z nullem
//        g1.checkCycle(null, d1, comp4);
//        g1.checkCycle(d2, d1, comp2);
//        g1.checkCycle(d1, d2, comp3);

    }

}
