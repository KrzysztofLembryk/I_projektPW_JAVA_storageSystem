package cp2023.solution;

import cp2023.base.ComponentId;
import cp2023.base.DeviceId;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.Vector;
import java.util.concurrent.Semaphore;

public class Graph {
    private final DeviceId nullDevice = new DeviceId(Integer.MIN_VALUE);
    private final Node nullNode = new Node(nullDevice);
    private Map<DeviceId, Node> dev_nodes_map;
    private Map<DeviceId, Integer> dev_freeSpaces;
    private final Map<ComponentId, Semaphore> semaphoreComponentTransfer;
    private final Map<DeviceId, Semaphore> semaphoresAccesDevice;
    private final Map<DeviceId, DeviceSpaceHandler> devSpacesHandlerMap;
    public Graph(Map<DeviceId, Integer> devFreeSlots, Map<ComponentId, Semaphore> semaphoreComponentTransfer,
                 Map<DeviceId, DeviceSpaceHandler> devSpacesHandlerMap, Map<DeviceId, Semaphore> semAccessDev)
    {
        this.semaphoresAccesDevice = semAccessDev;
        this.devSpacesHandlerMap = devSpacesHandlerMap;
        this.semaphoreComponentTransfer = semaphoreComponentTransfer;
        dev_nodes_map = new HashMap<>();
        dev_freeSpaces = new HashMap<>();

        for(DeviceId dev : devFreeSlots.keySet())
        {
            dev_nodes_map.put(dev, new Node(dev));
            dev_freeSpaces.put(dev, devFreeSlots.get(dev));
        }
        dev_nodes_map.put(nullDevice, nullNode);

    }

    private boolean findCycle_dfs(Node currNode, Map<DeviceId, Boolean> visited,
                                  Map<DeviceId, Pair<Boolean, Integer>> recursionStack,
                                  DeviceId finalNode, Integer startingEdgeIdx)
    {
        if(currNode.equals(nullNode))
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
        return dev_nodes_map.get(destDev).addEdge(nullNode, compId);
    }

    private void removeEdgesOfCycle_createStack(Map<DeviceId, Pair<Boolean, Integer>> recursionStack,
                                                Stack<Pair<DeviceId, ComponentId>> cycleStack_srcToDest,
                                                Vector<ComponentId> vecCompToFree,
                                                DeviceId startingDev) throws Exception
    {
        DeviceId currDev = startingDev;
        do{
            // Idziemy cyklem skierowanym ale do tylu.
            // Dostajemy numer node'a ktory chce do nas sie transferowac
            Pair<Boolean, Integer> inCycle_priorityIdx = recursionStack.get(currDev);

            // Usuwamy te krawedz z cyklu, ale robiac to dostajemy jaki komponent
            // chcial przyjsc na currDev i z jakiego urzadzenia
            Pair<ComponentId, DeviceId> compId_srcDev =
                    dev_nodes_map.get(currDev).removeEdge(inCycle_priorityIdx.second);

            // zapamietujemy komponent, zeby potem zrobic release na semaforach
            vecCompToFree.add(compId_srcDev.first);

            // dodajemy do stosu pare destDevice i komponent ktory chce przyjsc do destDevice
            cycleStack_srcToDest.push(new Pair<>(currDev, compId_srcDev.first));

            // cofamy sie w cyklu do srcDev z ktorego chcielismy transferowac compId
            currDev = compId_srcDev.second;

        }while(!currDev.equals(startingDev));
    }

    private void switchCompPlacesOnDevices(Stack<Pair<DeviceId, ComponentId>> cycleStack_srcToDest)
    {
        Pair<DeviceId, ComponentId> start = cycleStack_srcToDest.pop();
        Pair<DeviceId, ComponentId> src = start;

        while(!cycleStack_srcToDest.empty())
        {
            Pair<DeviceId, ComponentId> dest = cycleStack_srcToDest.pop();
            // Teraz robimy transfer z src do dest, nie musi to byc w sekcji krytycznej
            // bo nawet jak bedzie jakis przeplot to wiemy ze nie moze byc jednoczesnie
            // dwoch transferow tego samego komponentu, a my zmieniamy tylko miejsce gdzie
            // ten nasz dest komponent jest.
            devSpacesHandlerMap.get(dest.first).reserveSpaceCycle(src.second, dest.second);
            src = dest;
        }
        // teraz jeszce musimy zrobic z src do start
        devSpacesHandlerMap.get(start.first).reserveSpaceCycle(src.second, start.second);
    }
    private void removeCycle(Map<DeviceId, Pair<Boolean, Integer>> recursionStack,
                             DeviceId startingDev) throws Exception
    {
        Stack<Pair<DeviceId, ComponentId>> cycleStack_srcToDest = new Stack<>();
        Vector<ComponentId> vecCompToFree = new Vector<>();

        removeEdgesOfCycle_createStack(recursionStack, cycleStack_srcToDest, vecCompToFree, startingDev);

        switchCompPlacesOnDevices(cycleStack_srcToDest);

        // teraz uwalniamy semafory transferow jak juz maja miejsca na device ustalone
        for(ComponentId comp : vecCompToFree)
            semaphoreComponentTransfer.get(comp).release();

    }
    public void freeSpaceOnDev(DeviceId devId, ComponentId compToRemove)
            throws  InterruptedException
    {
        // zdejmujemy z grafu transfer o najwiekszym priorytecie na devId
        // tylko transfer typu REMOVE moze to zrobic bo zwalnia miejsce
        if(dev_nodes_map.get(devId).noTransfersToMe())
        {
         Integer freeSpaces = dev_freeSpaces.get(devId);
         dev_freeSpaces.put(devId, freeSpaces + 1);

         // jako ze zmieniamy na null i ktos w tym samym czasie moglby szukac miejsca
         // i nie znalezc a miejsce jest tak naprawde wolne, wiec musi byc s krytyczna.
         semaphoresAccesDevice.get(devId).acquire();
         devSpacesHandlerMap.get(devId).freeSpace(compToRemove);
         semaphoresAccesDevice.get(devId).release();

        }
        else
        {
            try
            {
                Pair<ComponentId, DeviceId> newComp_srcDev = dev_nodes_map.get(devId).removeEdge(0);

                // skoro ktos czeka na miejsce i je zaraz dostanie, to musi zwolnic swoje miejsce
                // chyba ze czeka transfer ADD, to on jako srcDev ma null wiec nie trzeba nic zwalniac
                if(!newComp_srcDev.second.equals(nullDevice))
                {
                    semaphoresAccesDevice.get(newComp_srcDev.second).acquire();

                    devSpacesHandlerMap.get(newComp_srcDev.second).freeSpace(newComp_srcDev.first);

                    semaphoresAccesDevice.get(newComp_srcDev.second).release();
                }

                // tutaj nie musi byc sekcji krytycznej bo robimy zamiane, a componenty sa unikatowe
                devSpacesHandlerMap.get(devId).reserveSpaceCycle(newComp_srcDev.first, compToRemove);

                semaphoreComponentTransfer.get(newComp_srcDev.first).release();
            }
            catch(Exception e)
            {
                System.out.println("Graph - freeSpaceOnDev - " + e);
            }
        }
    }
    public void checkCycle(DeviceId srcDev, DeviceId destDev, ComponentId compId)
            throws InterruptedException
    {
        // jesli jest miejsce na urzadzeniu to nie dodajemy nic do grafu, tylko przepuszczamy
        // ten transfer i zmniejszamy dostepne miejsce na destDev i wpisujemy ten component
        // na pierwsze wolne miejsce w devSpaces.
        if(dev_freeSpaces.get(destDev) > 0)
        {
            Integer freeSpaces;
            if(srcDev != null)
            {
                freeSpaces = dev_freeSpaces.get(srcDev);
                dev_freeSpaces.put(srcDev, freeSpaces + 1);

                semaphoresAccesDevice.get(srcDev).acquire();
                devSpacesHandlerMap.get(srcDev).freeSpace(compId);
                semaphoresAccesDevice.get(srcDev).release();
            }

            freeSpaces = dev_freeSpaces.get(destDev);
            dev_freeSpaces.put(destDev, freeSpaces - 1);

            semaphoresAccesDevice.get(destDev).acquire();
            devSpacesHandlerMap.get(destDev).reserveSpace(compId);
            semaphoresAccesDevice.get(destDev).release();

            semaphoreComponentTransfer.get(compId).release();
        }
        else {
            // Nie ma miejsca na destDev, wiec
            // Uzyjemy algorytmu dfs do znalezienia cyklu w naszym grafie
            Integer myIdx = addTransfer(srcDev, destDev, compId);

            Map<DeviceId, Boolean> visited = new HashMap<>();

            // potrzebujemy wiedziec jeszcze jaki indeks transferu jest w naszym cyklu
            Map<DeviceId, Pair<Boolean, Integer>> recursionStack = new HashMap<>();

            for (DeviceId dev : dev_nodes_map.keySet()) {
                visited.put(dev, false);
                recursionStack.put(dev, new Pair<>(false, 0));
            }

            if (findCycle_dfs(dev_nodes_map.get(destDev), visited, recursionStack, destDev, myIdx)) {
                //System.out.println("JEST CYKL");
                // jesli znajdziemy cykl to go usuwamy i wypuszczamy czekajce na semaforach transfery
                try {
                    removeCycle(recursionStack, destDev);
                }
                catch (Exception e) {
                    System.out.println("Graph - checkCycle - " + e);
                }
                //System.out.println("Transfer domykajacy: " + srcDev + " -> " + destDev + ", komponentu: " + compId);
            }
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

        //Graph g1 = new Graph(devTotalSlots, semaphoreComponentTransfer);
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
