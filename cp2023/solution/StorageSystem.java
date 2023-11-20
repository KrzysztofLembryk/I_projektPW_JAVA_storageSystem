package cp2023.solution;

import cp2023.base.ComponentId;
import cp2023.base.ComponentTransfer;
import cp2023.base.DeviceId;
import cp2023.exceptions.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

public class StorageSystem implements cp2023.base.StorageSystem {
    // deviceTotalSlots - stores info about how many components
    // device of given ID can store (deviceID --> capacity).
    private final Map<DeviceId, Integer> deviceTotalSlots;
    private final Map<ComponentId, DeviceId> compInDevPlacement;
    private final Map<ComponentId, Boolean> isCompBeingTransfered;
    private final Map<ComponentId, Semaphore> semaphoreComponentTransfer;
    // deviceSpacseMap - knows if there are free to use spaces on device or not
    private final Map<DeviceId, DeviceSpaceHandler> devSpacesHandlerMap;
    private final Map<DeviceId, Semaphore> semaphoresAccessDev;
    private final Semaphore semaphoreCheckTransfer;
    private final Map<DeviceId, SemaphoresDevSpacesHandler> semaphoresDevSpaces;
    private final Semaphore semaphoreGraph;
    private Graph transferGraph;

    private void initGraph()
    {
        Map<DeviceId, Integer> devFreeSpaces = new HashMap<>();
        for(DeviceId dev : deviceTotalSlots.keySet())
        {
            devFreeSpaces.put(dev, deviceTotalSlots.get(dev));
        }

        for(ComponentId compId : compInDevPlacement.keySet())
        {
            DeviceId currDev = compInDevPlacement.get(compId);
            Integer freeSpaces = devFreeSpaces.get(currDev);
            devFreeSpaces.put(currDev, freeSpaces - 1);
        }
        transferGraph = new Graph(devFreeSpaces, semaphoreComponentTransfer, devSpacesHandlerMap,
                semaphoresAccessDev);
    }

    public StorageSystem(Map<DeviceId, Integer> deviceTotalSlots,
                                Map<ComponentId, DeviceId> componentPlacement)
    {
        // Maps:
        this.deviceTotalSlots = deviceTotalSlots;
        compInDevPlacement = componentPlacement;

        semaphoreGraph = new Semaphore(1, true);

        semaphoreComponentTransfer = new ConcurrentHashMap<>();
        isCompBeingTransfered = new HashMap<>();

        for(ComponentId compId : componentPlacement.keySet())
            isCompBeingTransfered.put(compId, false);

        // SemaphoreCheckTransfer - chroni miejsce sprawdzania czy transfer jest ok
        // zeby jednoczesnie dwa transfery tego nie robily, ani tez nie modyfikowaly jendoczesnie
        // map ktore trzymaja te informacje gdy wykonuja swoj transfer.
        semaphoreCheckTransfer = new Semaphore(1, true);

        // semaphoresDev - mapa semaforow ktore pilnuja dostepu do devices, z permit=1
        semaphoresAccessDev = new ConcurrentHashMap<>();

        for(DeviceId devId : deviceTotalSlots.keySet()) {
            semaphoresAccessDev.put(devId, new Semaphore(1, true));
        }

        // deviceSpacesMap - dla danego device trzyma devSpaceHandler w ktorym jest mapa
        // trzymajaca informacje ktore miejsca na urzadzeniu (0,...,capacity-1) sa zajete/wolne
        // i przez jakie komponenty sa one zajete.
        devSpacesHandlerMap = new ConcurrentHashMap<>();

        // Podobnie jak deviceSpaceMap trzyma mape analogiczna mape miejsc na danym urzadzeniu
        // tylko tym razem dla danego miejsca (0,...,capacity-1) trzyma semafor ktory wpuszcza
        // na dane miejsce, trzyma tez informacje o komponencie na danym miejscu
        semaphoresDevSpaces = new ConcurrentHashMap<>();
        try
        {
            for(DeviceId devId : this.deviceTotalSlots.keySet())
            {
                Integer capacity = this.deviceTotalSlots.get(devId);
                semaphoresDevSpaces.put(devId, new SemaphoresDevSpacesHandler(capacity));
                devSpacesHandlerMap.put(devId, new DeviceSpaceHandler(capacity));
            }
            // zajmujemy miejsca na urzadzeniach i semaforach
            for(ComponentId compId : compInDevPlacement.keySet())
            {
                DeviceId devId = compInDevPlacement.get(compId);
                // dostajemy indeks zajetego miejsca na urzadzeniu
                Integer idx =
                        devSpacesHandlerMap.get(devId).init_spaces_reservation(compId);
                // zajmujemy semafor tego miejsca
                semaphoresDevSpaces.get(devId).acquire(idx, compId);
                // zmniejszamy liczbe dostepnych miejsc na semaforze kolejka
                // na ktorym ustawiaja sie i czekaja transfery na miejsca
            }
            initGraph();
        }
        catch(InterruptedException e)
        {
            System.out.println(e);
        }
    }

    private static TypeOfTransfer setTransferType(DeviceId srcDevId, DeviceId destDevId)
    {
        if(srcDevId == null && destDevId == null)
            return TypeOfTransfer.WRONG;
        else if(srcDevId == null && destDevId != null)
            return TypeOfTransfer.ADD;
        else if(srcDevId != null && destDevId == null)
            return TypeOfTransfer.REMOVE;
        else
            return TypeOfTransfer.TRANSFER;
    }
    private void isTransferOK(TypeOfTransfer transferType, ComponentId compId,
                              DeviceId srcDevId, DeviceId destDevId)
            throws TransferException
    {
        if(transferType == TypeOfTransfer.ADD)
        {
            if(!deviceTotalSlots.containsKey(destDevId))
                throw new DeviceDoesNotExist(destDevId);
            if(compInDevPlacement.containsKey(compId))
                throw new ComponentAlreadyExists(compId);
        }
        else if(transferType == TypeOfTransfer.REMOVE)
        {
            if(!deviceTotalSlots.containsKey(srcDevId))
                throw new DeviceDoesNotExist(srcDevId);
            if(!compInDevPlacement.containsKey(compId) ||
                    !compInDevPlacement.get(compId).equals(srcDevId))
                throw new ComponentDoesNotExist(compId, srcDevId);
            // component we want to remove is not in device of given ID
        }
        else if(transferType == TypeOfTransfer.TRANSFER)
        {
            if(!deviceTotalSlots.containsKey(srcDevId))
                throw new DeviceDoesNotExist(srcDevId);
            if(!deviceTotalSlots.containsKey(destDevId))
                throw new DeviceDoesNotExist(destDevId);
            if(!compInDevPlacement.containsKey(compId) ||
                    !compInDevPlacement.get(compId).equals(srcDevId))
                throw new ComponentDoesNotExist(compId, srcDevId);
            if(compInDevPlacement.get(compId) == destDevId)
                throw new ComponentDoesNotNeedTransfer(compId, destDevId);
        }
        else if(transferType == TypeOfTransfer.WRONG)
        {
            throw new IllegalTransferType(compId);
        }

    }

    private void checkIsCompBeingTransfered(ComponentId compId, TypeOfTransfer transferType)
            throws ComponentIsBeingOperatedOn
    {
        // wystarczy sprawdzic tylko typ transferu bo to czy jest dobry sprawdzilismy
        // juz we wczesniej wywolanej funkcji isTransferOK
        if(transferType == TypeOfTransfer.ADD)
        {
            // if there is no such compID this means we either add new comp
            // so we have another semaphore for adding components,
            // we also put (compID, false) to map that checks
            //storageSystem.getSemaphoreForNewComp().acquire();

            isCompBeingTransfered.put(compId, false);
        }
        if(!isCompBeingTransfered.get(compId))
            isCompBeingTransfered.put(compId, true);
        else
            throw new ComponentIsBeingOperatedOn(compId);
    }

    private void do_the_TRANSFER(ComponentTransfer transfer, Integer idxOfMySpace)
            throws InterruptedException
    {
        DeviceId srcDevId, destDevId;
        srcDevId = transfer.getSourceDeviceId();
        destDevId = transfer.getDestinationDeviceId();
        ComponentId compId = transfer.getComponentId();

        // jesli dostalismy miejsce ok_to_reserve to znaczy ze ktos sie z niego
        // wlasnie transferuje, wiec mozemy zrobic od razu prepare, ale z perform
        // musimy zaczekac az ten ktos skonczy swoje prepare i zwolni nam semafor

        // nie musimy robic devSpaceshandler.freeSpace bo albo byl cykl i nie musimy
        // nic zwalniac, albo byl remove i dostalismy miejsce i zwolnilismy swoje miejsce juz
        // w graph, albo bylo od razu wolne na destDev miejsce i tez zwolnilismy je w graph
//        semaphoresAccessDev.get(srcDevId).acquire();
//        // ustawiamy ze na naszym srcDev nasze miejsce jest ok do zarezerowania
//        // musimy to zrobic przed naszym prepare, zeby inny transfer mogl zrobic
//        // swoje prepare
//        devSpacesHandlerMap.get(srcDevId).freeSpace(compId);
//
//        semaphoresAccessDev.get(srcDevId).release();

        transfer.prepare();

        // po naszym prepare inny transfer moze juz robic perform na nasze miejsce
        // wiec zwalniamy semafor
        semaphoresDevSpaces.get(srcDevId).release(compId);

        // czekamy az nam zostanie zwolniony semafor miejsca, zebysmy mogli zrobic
        // perform
        semaphoresDevSpaces.get(destDevId).acquire(idxOfMySpace, compId);
        transfer.perform();

        semaphoreComponentTransfer.remove(compId);

        semaphoreCheckTransfer.acquire();

        isCompBeingTransfered.put(compId, false);
        compInDevPlacement.put(compId, destDevId);

        semaphoreCheckTransfer.release();


    }

    private void do_REMOVING(ComponentTransfer transfer)
            throws InterruptedException
    {
        DeviceId srcDevId;
        ComponentId compId = transfer.getComponentId();
        srcDevId = transfer.getSourceDeviceId();

        // transfer typu remove mozemy od razu przygotowac, bo komponent ma juz
        // miejsce na urzadzeniu
        transfer.prepare();
        // wiec transfer bedzie mogl zrobic swoje prepare, ale dopiero jak zwolnimy
        // semaphoreDevSpaces to bedzie mogl zrobic preform
        semaphoresDevSpaces.get(srcDevId).release(compId);

        // jak juz zwolnimy semafor to robimy swoj perform
        transfer.perform();

        // po zrobieniu perform usuwamy nasz komponent z mapy sprawdzajacej czy komponent jest
        // transferowany, z mapy komponentow i nasz semafor dla komponentu

        semaphoreComponentTransfer.remove(compId);

        semaphoreCheckTransfer.acquire();

        isCompBeingTransfered.remove(compId);
        compInDevPlacement.remove(compId);

        semaphoreCheckTransfer.release();
    }
    private void do_ADDING(ComponentTransfer transfer, Integer idxOfMySpace) throws InterruptedException
    {
        DeviceId destDevId = transfer.getDestinationDeviceId();
        ComponentId compId = transfer.getComponentId();
        // jesli dostalismy miejsce ok_to_reserve to znaczy ze ktos sie z niego
        // wlasnie transferuje, wiec mozemy zrobic od razu prepare, ale z perform
        // musimy zaczekac az ten ktos skonczy swoje prepare i zwolni nam semafor

        transfer.prepare();
        semaphoresDevSpaces.get(destDevId).acquire(idxOfMySpace, compId);
        transfer.perform();

        // usuwamy nasz semafor bo nasz transfer juz praktycznie sie skonczyl
        // jak przyjdzie nowy transfer to na poczatku zrobi put i da nowy semafor
        semaphoreComponentTransfer.remove(compId);

        // Jak skonczylismy juz robic performa dla naszego komponentu to mozemy
        // zmienic ze juz transfer na tym komponencie nie jest wykonywany.
        // Przed wprowadzeniem zmian dotyczacych componentu w naszym systemie
        // musimy zapewnic ze nikt aktualnie nie sprawdza czy dany komponent istnieje
        // w naszym systemie itp, bo po wykonaniu transferu moze on juz nie istniec.
        semaphoreCheckTransfer.acquire();

        isCompBeingTransfered.put(compId, false);
        compInDevPlacement.put(compId, destDevId);

        semaphoreCheckTransfer.release();
    }


    @Override
    public void execute(ComponentTransfer transfer) throws TransferException {
        DeviceId srcDevId, destDevId;
        ComponentId compId = transfer.getComponentId();
        srcDevId = transfer.getSourceDeviceId();
        destDevId = transfer.getDestinationDeviceId();
        TypeOfTransfer transferType = setTransferType(srcDevId, destDevId);

        try {
                semaphoreCheckTransfer.acquire();
                isTransferOK(transferType, compId, srcDevId, destDevId);
                checkIsCompBeingTransfered(compId, transferType);
                semaphoreCheckTransfer.release();

            semaphoreComponentTransfer.put(compId, new Semaphore(0, true));

            switch (transferType) {
                case ADD -> {

                    semaphoreGraph.acquire();

                    transferGraph.checkCycle(srcDevId, destDevId, compId);

                    semaphoreGraph.release();

                    semaphoreComponentTransfer.get(compId).acquire();
                    // Tylko jeden transfer w danej chwili moze miec przydzielane wolne miejsce
                    semaphoresAccessDev.get(destDevId).acquire();

                    Integer idxOfMySpace =
                            devSpacesHandlerMap.get(destDevId).reserveSpace(compId);

                    semaphoresAccessDev.get(destDevId).release();

                    do_ADDING(transfer,idxOfMySpace);
                }
                case REMOVE -> {

                    semaphoreGraph.acquire();

                    transferGraph.freeSpaceOnDev(srcDevId, compId);

                    semaphoreGraph.release();

                    do_REMOVING(transfer);
                }
                case TRANSFER -> {

                    semaphoreGraph.acquire();

                    transferGraph.checkCycle(srcDevId, destDevId, compId);

                    semaphoreGraph.release();

                    semaphoreComponentTransfer.get(compId).acquire();

                    // nie ma z nami cyklu to zwalniamy semafor i ustawiamy sie na kolejce czekania
                    // na miejsce na urzadzenie

                    semaphoresAccessDev.get(destDevId).acquire();
                    Integer idxOfMySpace =
                            devSpacesHandlerMap.get(destDevId).reserveSpace(compId);
                    semaphoresAccessDev.get(destDevId).release();


                    do_the_TRANSFER(transfer, idxOfMySpace);

                    // jesli cykl jest - czyli i tak bylibysmy pierwsi na kolejce po
                    // miejsce na urzadzeniu, to omijamy acquire na tym semaforze (ma 0 permitow aktualnie)
                    // i blokujemy reszte transferow dopoki cykl nie zostanie rozstrzygniety.
                    // jesli bysmy nie zablokowali przychodzacych nowych transferow to moglby powstac
                    // nowy cykl ale nigdy by nie zostal wykryty, bo zaden element kiedy sie sam dodawal
                    // nie bylby pierwszy na swojej kolejce bo wczesniejszy cykl nie bylby jeszcze usuniety.





                    // Po skonczeniu perform zmieniamy w mapie przyporzadkowanie componentow do device
                    // i ustawiamy ze komponent nie jest juz transferowany

                }
                case WRONG -> System.out.println("raczej nigdy tu nie wejdziemy :)");
            }
        }
        catch(TransferException e)
        {
            System.out.println(e);
        }
        catch(InterruptedException e)
        {
            throw new RuntimeException("panic: unexpected thread interruption", e);
        }
    }
    public void printCompMapping()
    {
        for(ComponentId id : compInDevPlacement.keySet())
            System.out.println("Comp" + id + " : " + compInDevPlacement.get(id));
    }

    public static void test()
    {
        Map<ComponentId, DeviceId> compInDevicePlacement = new HashMap<>();

        ComponentId compID1 = new ComponentId(68);
        ComponentId compID2 = new ComponentId(68);
        DeviceId dev1 = new DeviceId(1);
        compInDevicePlacement.put(compID1, dev1);


        if(compInDevicePlacement.containsKey(compID2))
            System.out.println("compID1 = compID2");
        else
            System.out.println("nie rownaja sie");

        compInDevicePlacement.put(compID2, dev1);

        for(ComponentId id : compInDevicePlacement.keySet())
        {
            if(id.compareTo(new ComponentId(68)) == 0)
                System.out.println("istnieje compId 68");
        }
        System.out.println("map size: " + compInDevicePlacement.size());


    }
    public static void main(String[] args)
    {
        StorageSystem.test();
    }
}
