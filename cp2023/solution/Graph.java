package cp2023.solution;

import cp2023.base.ComponentId;
import cp2023.base.DeviceId;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

public class Graph {
    private final DeviceId nullDevice = new DeviceId(Integer.MIN_VALUE);
    private final Node nullNode = new Node(nullDevice);
    private final Map<DeviceId, Node> dev_nodes_map;
    private final Map<DeviceId, Integer> dev_freeSpaces;
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

    private void switchCompPlacesOnDevices(Stack<Pair<DeviceId, ComponentId>> transferStack_destDev_comp)
    {
        Pair<DeviceId, ComponentId> start = transferStack_destDev_comp.pop();
        Pair<DeviceId, ComponentId> currTransfer = start;


        while(!transferStack_destDev_comp.empty())
        {
            Pair<DeviceId, ComponentId> nextTransfer = transferStack_destDev_comp.pop();
            // Teraz robimy transfer componentu do dest (w tym momencie nie wiemy jaki jest srcDev
            // tego komponentu) nie musi to byc w sekcji krytycznej
            // bo nawet jak bedzie jakis przeplot to wiemy ze nie moze byc jednoczesnie
            // dwoch transferow tego samego komponentu, a my zmieniamy tylko miejsce gdzie
            // ten nasz dest komponent jest.
            devSpacesHandlerMap.get(currTransfer.first).
                    reserveSpaceCycle(currTransfer.second, nextTransfer.second);

            currTransfer = nextTransfer;
        }
        // teraz jeszce musimy zrobic ostatni transfer na pierwsze urzadzenie, z ktorego transfer
        // obsluzylismy na samym poczatku, a do ktorego transfer oblugujemy teraz
        devSpacesHandlerMap.get(currTransfer.first).reserveSpaceCycle(currTransfer.second, start.second);
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
    // metoda freeSpaceOnDev musi rekurencyjnie zwolnic ciag miejsc
    // bo jesli mamy T1:A->B, T2:B->C i na D jest miejsce i przychodzi transfer T3:C->D
    // to ten transfer zwalnia swoje miejsce na C, wpuszcza T2 ale T2 tez musi zwolnic
    // swoje miejsce i wpuscic na semafor T1. Zatrzymamy sie albo na transferze ADD
    // albo gdy nie bedzie juz transferu zadnego i tylko zwolnimy miejsce
    public void freeSpaceOnDev(DeviceId devId, ComponentId compToRemove)
            throws  InterruptedException
    {
        Node currentNode = dev_nodes_map.get(devId);
        // zdejmujemy z grafu transfer o najwiekszym priorytecie na devId
        // tylko transfer typu REMOVE moze to zrobic bo zwalnia miejsce
        if(!currentNode.equals(nullNode) && currentNode.noTransfersToMe())
        {
            Integer freeSpaces = dev_freeSpaces.get(devId);
            dev_freeSpaces.put(devId, freeSpaces + 1);

         // jako ze zmieniamy na null i ktos w tym samym czasie moglby szukac miejsca
         // i nie znalezc a miejsce jest tak naprawde wolne, wiec musi byc s krytyczna.
            semaphoresAccesDevice.get(devId).acquire();
            devSpacesHandlerMap.get(devId).freeSpace(compToRemove);
            semaphoresAccesDevice.get(devId).release();

        }
        else if(!currentNode.noTransfersToMe())
        {
            try
            {
                Pair<ComponentId, DeviceId> newComp_srcDev = currentNode.removeEdge(0);

                // tutaj nie musi byc sekcji krytycznej bo robimy zamiane, a componenty sa unikatowe
                devSpacesHandlerMap.get(devId).reserveSpaceCycle(newComp_srcDev.first, compToRemove);

                semaphoreComponentTransfer.get(newComp_srcDev.first).release();

                freeSpaceOnDev(newComp_srcDev.second, newComp_srcDev.first);


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
            // najpierw zajmujemy swoje miejsce
            Integer freeSpaces = dev_freeSpaces.get(destDev);
            dev_freeSpaces.put(destDev, freeSpaces - 1);

            semaphoresAccesDevice.get(destDev).acquire();
            devSpacesHandlerMap.get(destDev).reserveSpace(compId);
            semaphoresAccesDevice.get(destDev).release();

            if(srcDev != null)
            {
                // teraz rekurencyjnie zwalniamy miejsca na srcDev
                freeSpaceOnDev(srcDev, compId);
            }
            System.out.println();
            // na koniec zwalniamy swoj semafor
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
                // jesli znajdziemy cykl to go usuwamy i wypuszczamy czekajce na semaforach transfery
                try {
                    removeCycle(recursionStack, destDev);
                }
                catch (Exception e) {
                    System.out.println("Graph - checkCycle - " + e);
                }
            }
        }
    }
}
