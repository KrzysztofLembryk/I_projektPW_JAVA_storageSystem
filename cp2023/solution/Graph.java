package cp2023.solution;

import cp2023.base.ComponentId;
import cp2023.base.DeviceId;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.Vector;
import java.util.concurrent.Semaphore;

/**
 * Klasa Graph zawiera graf skierowany, gdzie wierzcholek to urzadzenia
 * znajdujace sie w systemie, a krawedzie wchodzace do wiercholka to transfery.
 * Klasa ta zawiera rowniez stworzona w klasie StorageSystem mape
 * <deviceId, DevSpaceHandler> gdyz w klasie graph bedziemy zaleznie od przypadku
 * zwalniac badz przydzielac miejsca komponentom na urzadzeniach.
 *
 * Gdy wchodzi nowy transfer to wywolujemy na nim metode checkCycle klasy Graph
 * ktora jesli jest wolne miejsce na urzadzeniu docelowym to nie dodaje nowej krawedzi
 * do grafu i przepuszcza transfer. Jesli nie ma wolnego miejsca to dodajemy nowa krawedz
 * do grafu po czym sprawdzamy czy nowo dodana krawedz jest w jakims cyklu. Jesli jest
 * to zamieniamy miejsca komponentow w deviceSpaceHandler i przepuszczamy wszystkie
 * transfery w cyklu (robimy release na semaforach ich komponentow).
 *
 */
public class Graph {
    private final DeviceId nullDevice = new DeviceId(Integer.MIN_VALUE);
    private final Node nullNode = new Node(nullDevice);

    // dev_nodes_map - mapa wierzcholkow grafu, dla kazdego devId
    // klasa Node trzyma wektor transferow DO devId.
    private final Map<DeviceId, Node> dev_nodes_map;

    // dev_freeSpaces - pamietamy ile jest wolnych miejsc na urzadzeniach,
    // gdyz nie chcemy wywolac metody devSpaceHandler.reserve(), gdy na
    // urzadzeniu nie ma miejsca. A takze zaleznie od tego czy istnieje
    // wolne miejsce na urzadzeniu bedziemy podejmowac rozne akcje.
    private final Map<DeviceId, Integer> dev_freeSpaces;

    // semaphoreComponentTransfer - na kzdy komponent mamy semafor, ktory
    // pozwala badz nie pozwala danemu komponentowi sie transferowac.
    // Gdy nie ma miejsca to transfery wieszaja sie na semaforach komponentow
    // ktore transferuja.
    private final Map<ComponentId, Semaphore> semaphoreComponentTransfer;

    // semaphoresAccessDevice - jednoczesnie uzywac devSpaceHandler moze
    // tylko jeden transfer jesli rezerwuje badz zwalnia miejsce.
    private final Map<DeviceId, Semaphore> semaphoresAccessDevice;
    private final Map<DeviceId, DeviceSpaceHandler> devSpacesHandlerMap;

    public Graph(Map<DeviceId, Integer> devFreeSlots, Map<ComponentId, Semaphore> semaphoreComponentTransfer,
                 Map<DeviceId, DeviceSpaceHandler> devSpacesHandlerMap, Map<DeviceId, Semaphore> semAccessDev)
    {
        this.semaphoresAccessDevice = semAccessDev;
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

    /**
     * Uzywamy algorytmu dfs do znalezienia cyklu w naszym grafie, przy czym
     * idziemy tak jakby do tylu w tym cyklu, bo jak mamy krawedz devA -> devB
     * to tylko devB wie ze do niego idzie transfer z devA, ale devA nie wie do
     * jakich urzadzen ida z niego transfery.
     *
     * Do funkcji przekazujemy deviceId finalNode czyli wierzcholek z ktorego
     * startujemy i na ktorym cykl powinen sie zakonczyc.
     *
     * Przekazujemy rowniez startingEdge, czyli indeks krawedzi ktora idziemy
     * w wektorze krawedzi wierzcholka z ktorego startujemy.
     *
     * Na recursionStack zapamietujemy czy dane device jest w cyklu i jesli jest
     * to jaka krawedz idaca do tego device do tego cyklu nalezy, czyli zapamietujemy
     * jej indeks w wektorze krawedzi.
     */
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
            // Ze startowego wierzcholka nie chcemy isc wszystkimi krawedziami w petli for,
            // ale tylko z jednej krawedzi, tej co wlasnie dodalismy do grafu.
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

            // Sprawdzamy wszystkie krawedzie od najwiekszego priorytetu do najmniejszego,
            // dopoki nie znajdziemy cyklu badz petla sie nie skonczy.
            for(int i = 0; i < size; i++)
            {
                // Na stosie odkladamy ze aktualne devId bylo rozpatrzone i odkladamy
                // ktora krawedzia poszlismy.
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
            // Dostajemy numer node'a ktory chce do nas sie transferowac.
            Pair<Boolean, Integer> inCycle_priorityIdx = recursionStack.get(currDev);

            // Usuwamy te krawedz z cyklu, ale robiac to dostajemy jaki komponent
            // chcial przyjsc na currDev i z jakiego urzadzenia.
            Pair<ComponentId, DeviceId> compId_srcDev =
                    dev_nodes_map.get(currDev).removeEdge(inCycle_priorityIdx.second);

            // Zapamietujemy komponent, zeby potem zrobic release na semaforach komponentow
            // ktore byly w cyklu.
            vecCompToFree.add(compId_srcDev.first);

            // Dodajemy do stosu pare destDevice i komponent ktory chce przyjsc do destDevice.
            cycleStack_srcToDest.push(new Pair<>(currDev, compId_srcDev.first));

            // Cofamy sie w cyklu do srcDev z ktorego chcielismy transferowac compId.
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
            // tego komponentu), nie musi to byc w sekcji krytycznej
            // bo nawet jak bedzie jakis przeplot to wiemy ze nie moze byc jednoczesnie
            // dwoch transferow tego samego komponentu, a my zmieniamy tylko miejsce gdzie
            // ten nasz komponent jest.
            devSpacesHandlerMap.get(currTransfer.first).
                    reserveSpaceCycle(currTransfer.second, nextTransfer.second);

            currTransfer = nextTransfer;
        }

        // Teraz jeszce musimy zrobic ostatni transfer na pierwsze urzadzenie, z ktorego transfer
        // obsluzylismy na samym poczatku, a do ktorego transfer oblugujemy teraz.
        devSpacesHandlerMap.get(currTransfer.first).reserveSpaceCycle(currTransfer.second, start.second);
    }

    private void removeCycle(Map<DeviceId, Pair<Boolean, Integer>> recursionStack,
                             DeviceId startingDev) throws Exception
    {
        Stack<Pair<DeviceId, ComponentId>> cycleStack_srcToDest = new Stack<>();
        Vector<ComponentId> vecCompToFree = new Vector<>();

        // Usuwamy krawedzie cyklu i tworzymy stos na ktorym odkladamy tak transfery
        // ze jak bedziemy je zdejmowac to bedziemy szli w odpowiednim kierunku cyklem,
        // a nie do tylu. Czyli na gorze stosu bedziemy miec transfer startDev -> destDev komponent
        // przy czym zapamietujemy tylko destDev i komponent transferowany.
        removeEdgesOfCycle_createStack(recursionStack, cycleStack_srcToDest, vecCompToFree, startingDev);

        // Idac tym cyklem ktory mamy na stosie zamieniamy miejsca komponentow.
        switchCompPlacesOnDevices(cycleStack_srcToDest);

        // Teraz uwalniamy semafory transferow jak juz maja miejsca na swoich destDev ustalone.
        for(ComponentId comp : vecCompToFree)
            semaphoreComponentTransfer.get(comp).release();

    }

    /**
     * Metoda freeSpaceOnDev musi rekurencyjnie zwolnic ciag miejsc
     * bo jesli mamy T1:A->B, T2:B->C i na D jest miejsce i przychodzi transfer T3:C->D
     * to ten transfer zwalnia swoje miejsce na C, wpuszcza T2 ale T2 tez musi zwolnic
     * swoje miejsce i wpuscic na semafor T1. Zatrzymamy sie albo na transferze ADD
     * albo gdy nie bedzie juz zadnego transferu przychodzacego na aktualny device
     * i wtedy tylko zwolnimy miejsce.
     */
    public void freeSpaceOnDev(DeviceId devId, ComponentId compToRemove)
            throws  InterruptedException
    {
        Node currentNode = dev_nodes_map.get(devId);

        // Jesli doszlismy do nullNode (czyli przed chwila byl transfer ADD,
        // bo tylko on dodaje nullNode) to nic nie robimy.
        // Jesli nie ma zadnych transferow do aktualnego devId to po prostu
        // zwalniamy miejsce.
        if(!currentNode.equals(nullNode) && currentNode.noTransfersToMe())
        {
            Integer freeSpaces = dev_freeSpaces.get(devId);
            dev_freeSpaces.put(devId, freeSpaces + 1);

            // Jako ze zmieniamy zajete miejsce na wolne to musi byc tu sekcja krytyczna
            // gdyz mogloby sie tak zdarzyc ze ktos jednoczesnie by szukal miejsca na tym
            // samym device co my zwalniamy i powinen byl je znalezc ale zrobil sie taki
            // przeplot ze najpierw sprawdzil a potem my zwolnilismy.
            semaphoresAccessDevice.get(devId).acquire();
            devSpacesHandlerMap.get(devId).freeSpace(compToRemove);
            semaphoresAccessDevice.get(devId).release();

        }
        // Jesli istnieja jakies transfery idace do mojego wierzcholka, to
        // wpuszczamy ten co mial najwiekszy priorytet (najdluzej czekal) na nasze
        // zwolnione miejsce i usuwamy krawedz wpuszczonego transferu.
        else if(!currentNode.noTransfersToMe())
        {
            try
            {
                Pair<ComponentId, DeviceId> newComp_srcDev = currentNode.removeEdge(0);

                // Tutaj nie musi byc sekcji krytycznej, bo robimy zamiane miejsc dwoch komponentow,
                // a componenty sa unikatowe.
                devSpacesHandlerMap.get(devId).reserveSpaceCycle(newComp_srcDev.first, compToRemove);

                // Zwalniamy semafor komponentu, ktorego transfer wlasnie wpuscilismy na nasze miejsce,
                // dzieki temu moze on zaczac wykonywac swoje prepare() w czasie gdy my zwalniamy
                // rekurencyjnie reszte transferow w ciagu.
                semaphoreComponentTransfer.get(newComp_srcDev.first).release();

                // Zwalniamy rekurnecyjnie reszte najdluzej czekajacych transferow, cofajac
                // sie do srcDev.
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
        // Jesli jest miejsce na urzadzeniu to nie dodajemy nic do grafu, tylko przepuszczamy
        // ten transfer i zmniejszamy dostepne miejsce na destDev i wpisujemy ten component
        // na pierwsze wolne miejsce w devSpaces.
        if(dev_freeSpaces.get(destDev) > 0)
        {
            // Najpierw zajmujemy swoje miejsce.
            Integer freeSpaces = dev_freeSpaces.get(destDev);
            dev_freeSpaces.put(destDev, freeSpaces - 1);

            semaphoresAccessDevice.get(destDev).acquire();
            devSpacesHandlerMap.get(destDev).reserveSpace(compId);
            semaphoresAccessDevice.get(destDev).release();

            if(srcDev != null)
            {
                // Teraz rekurencyjnie zwalniamy miejsca na srcDev.
                freeSpaceOnDev(srcDev, compId);
            }

            // Na koniec zwalniamy swoj semafor.
            semaphoreComponentTransfer.get(compId).release();
        }
        else {
            // Nie ma miejsca na destDev, wiec dodajemy krawedz do grafu i
            // uzywamy algorytmu dfs do znalezienia cyklu w naszym grafie.
            Integer myIdx = addTransfer(srcDev, destDev, compId);

            Map<DeviceId, Boolean> visited = new HashMap<>();

            // Potrzebujemy wiedziec jeszcze jaki indeks transferu jest w naszym cyklu.
            Map<DeviceId, Pair<Boolean, Integer>> recursionStack = new HashMap<>();

            for (DeviceId dev : dev_nodes_map.keySet()) {
                visited.put(dev, false);
                recursionStack.put(dev, new Pair<>(false, 0));
            }

            if (findCycle_dfs(dev_nodes_map.get(destDev), visited, recursionStack, destDev, myIdx)) {
                // Jesli znajdziemy cykl to go usuwamy i wypuszczamy czekajce na semaforach transfery.
                try {
                    removeCycle(recursionStack, destDev);
                }
                catch (Exception e) {
                    System.out.println("Graph - checkCycle - " + e);
                }
            }
            // Jesli nie ma cyklu to aktualny transfer zawiesi sie na semaforze
            // transferowanego przez niego komponentu.
        }
    }
}
