package cp2023.solution;

import cp2023.base.ComponentId;
import cp2023.base.ComponentTransfer;
import cp2023.base.DeviceId;
import cp2023.exceptions.*;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

public class StorageSystem implements cp2023.base.StorageSystem {
    // deviceTotalSlots - stores info about how many components
    // device of given ID can store (deviceID --> capacity).
    private Map<DeviceId, Integer> deviceCapacityMap;
    private Map<ComponentId, DeviceId> compInDevPlacement;
    private Map<ComponentId, Boolean> isCompBeingTransfered;
    // deviceSpacseMap - knows if there are free to use spaces on device or not
    private Map<DeviceId, DeviceSpaceHandler> deviceSpacesMap;
    private Map<DeviceId, Semaphore> semaphoresDev;
    //private Map<DeviceId, Semaphore> semaphoresDevSpaces;
    private Semaphore semaphoreCheckTransfer;

    // CycleMap stores (srcDev, queue of destDev), meaning from srcDev we want to
    // transfer  to destDev
    private Map<DeviceId, Queue<Pair<DeviceId, ComponentId>>> cycleMap;
    private final Map<DeviceId, Boolean> wasDevChecked;
    private Map<DeviceId, SemaphoresDevSpacesHandler> semaphoresDevSpaces;
    private Map<DeviceId, Semaphore> semaphoreQueueForDevSpaces;

    public StorageSystem(Map<DeviceId, Integer> deviceTotalSlots,
                                Map<ComponentId, DeviceId> componentPlacement)
    {
        // Maps:
        deviceCapacityMap = deviceTotalSlots;
        compInDevPlacement = componentPlacement;

        isCompBeingTransfered = new HashMap<>();
        for(ComponentId compId : componentPlacement.keySet())
            isCompBeingTransfered.put(compId, false);

        // SemaphoreCheckTransfer - chroni miejsce sprawdzania czy transfer jest ok
        // zeby jednoczesnie dwa transfery tego nie robily, ani tez nie modyfikowaly jendoczesnie
        // map ktore trzymaja te informacje gdy wykonuja swoj transfer.
        semaphoreCheckTransfer = new Semaphore(1, true);

        // semaphoresDev - mapa semaforow ktore pilnuja dostepu do devices, z permit=1
        semaphoresDev = new ConcurrentHashMap<>();
        semaphoreQueueForDevSpaces = new ConcurrentHashMap<>();
        // semaphoreCanICheckDevSpace -
        //semaphoreCanICheckDevSpace = new ConcurrentHashMap<>();

        for(DeviceId devId : deviceTotalSlots.keySet()) {
            semaphoresDev.put(devId, new Semaphore(1, true));
            semaphoreQueueForDevSpaces.put(devId, new Semaphore(deviceTotalSlots.get(devId), true));
        }

        cycleMap = new HashMap<>();
        wasDevChecked = new HashMap<>();
        for(DeviceId devId : deviceTotalSlots.keySet())
        {
            cycleMap.put(devId, new LinkedList<>());
            wasDevChecked.put(devId, false);
        }

        // deviceSpacesMap - dla danego device trzyma devSpaceHandler w ktorym jest mapa
        // trzymajaca informacje ktore miejsca na urzadzeniu (0,...,capacity-1) sa zajete/wolne
        // i przez jakie komponenty sa one zajete.
        deviceSpacesMap = new ConcurrentHashMap<>();

        // Podobnie jak deviceSpaceMap trzyma mape analogiczna mape miejsc na danym urzadzeniu
        // tylko tym razem dla danego miejsca (0,...,capacity-1) trzyma semafor ktory wpuszcza
        // na dane miejsce, trzyma tez informacje o komponencie na danym miejscu
        semaphoresDevSpaces = new ConcurrentHashMap<>();
        try
        {
            for(DeviceId devId : deviceCapacityMap.keySet())
            {
                Integer capacity = deviceCapacityMap.get(devId);
                semaphoresDevSpaces.put(devId, new SemaphoresDevSpacesHandler(capacity));
                deviceSpacesMap.put(devId, new DeviceSpaceHandler(capacity));
                System.out.println("storageConstructor: " + devId + ", capacity: " + capacity);
            }

            // zajmujemy miejsca na urzadzeniach i semaforach
            for(ComponentId compId : compInDevPlacement.keySet())
            {
                DeviceId devId = compInDevPlacement.get(compId);
                // dostajemy indeks zajetego miejsca na urzadzeniu
                Integer idx =
                        deviceSpacesMap.get(devId).init_spaces_reservation(compId);
                // zajmujemy semafor tego miejsca
                semaphoresDevSpaces.get(devId).acquire(idx, compId);
                // zmniejszamy liczbe dostepnych miejsc na semaforze kolejka
                // na ktorym ustawiaja sie i czekaja transfery na miejsca
                semaphoreQueueForDevSpaces.get(devId).acquire();
            }
        }
        catch(InterruptedException e)
        {
            System.out.println(e);
        }
    }


    private void checkCycles(DeviceId srcId, DeviceId destId, ComponentId compId)
    {
        for(DeviceId id : wasDevChecked.keySet())
            wasDevChecked.put(id, false);

        boolean foundCycle = false;
        Pair<DeviceId, ComponentId> pair = new Pair<>(destId, compId);

        cycleMap.get(srcId).add(pair);

        for(DeviceId currDevId : wasDevChecked.keySet())
        {
            if(!wasDevChecked.get(currDevId))
            {
                DeviceId currDev = pair.getFirst();
            }
        }

        if(cycleMap.get(srcId).equals(pair))
        {

//            while(true)
//            {
//                if(currDev == srcId)
//                {
//                    // cycle
//                }
//                else if(cycleMap.get(currDev).isEmpty())
//                {
//                    // no cycle
//                    break;
//                }
//                else
//                {
//                    currDev = cycleMap.get(currDev).peek().getFirst();
//                }
//            }
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
            throws DeviceDoesNotExist, ComponentDoesNotNeedTransfer, ComponentAlreadyExists,
            ComponentDoesNotExist, IllegalTransferType
    {
        if(transferType == TypeOfTransfer.ADD)
        {
            if(!deviceCapacityMap.containsKey(destDevId))
                throw new DeviceDoesNotExist(destDevId);
            if(compInDevPlacement.containsKey(compId))
                throw new ComponentAlreadyExists(compId);
        }
        else if(transferType == TypeOfTransfer.REMOVE)
        {
            if(!deviceCapacityMap.containsKey(srcDevId))
                throw new DeviceDoesNotExist(srcDevId);
            if(!compInDevPlacement.containsKey(compId) ||
                    !compInDevPlacement.get(compId).equals(srcDevId))
                throw new ComponentDoesNotExist(compId, srcDevId);
            // component we want to remove is not in device of given ID
        }
        else if(transferType == TypeOfTransfer.TRANSFER)
        {
            if(!deviceCapacityMap.containsKey(srcDevId))
                throw new DeviceDoesNotExist(srcDevId);
            if(!deviceCapacityMap.containsKey(destDevId))
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

        semaphoresDev.get(srcDevId).acquire();
        // ustawiamy ze na naszym srcDev nasze miejsce jest ok do zarezerowania
        // musimy to zrobic przed naszym prepare, zeby inny transfer mogl zrobic
        // swoje prepare
        deviceSpacesMap.get(srcDevId).freeSpace(compId);
        semaphoreQueueForDevSpaces.get(srcDevId).release();

        semaphoresDev.get(srcDevId).release();

        transfer.prepare();

        // po naszym prepare inny transfer moze juz robic perform na nasze miejsce
        // wiec zwalniamy semafor
        semaphoresDevSpaces.get(srcDevId).release(compId);

        // czekamy az nam zostanie zwolniony semafor miejsca, zebysmy mogli zrobic
        // perform
        semaphoresDevSpaces.get(destDevId).acquire(idxOfMySpace, compId);
        transfer.perform();

    }

    private void do_REMOVING(ComponentTransfer transfer)
            throws InterruptedException
    {
        DeviceId srcDevId;
        ComponentId compId = transfer.getComponentId();
        srcDevId = transfer.getSourceDeviceId();

        // transfer typu remove mozemy od razu przygotowac, bo komponent ma juz
        // miejsce na urzadzeniu


        semaphoresDev.get(srcDevId).acquire();
        // po prepare zwalniamy miejsce na urzadzeniu zeby ktos mogl juz je zajac
        deviceSpacesMap.get(srcDevId).freeSpace(compId);
        // teraz zwalniamy miejsce na kolejce
        semaphoreQueueForDevSpaces.get(srcDevId).release();

        semaphoresDev.get(srcDevId).release();

        transfer.prepare();

        // wiec transfer bedzie mogl zrobic swoje prepare, ale dopiero jak zwolnimy
        // semaphoreDevSpaces to bedzie mogl zrobic preform
        semaphoresDevSpaces.get(srcDevId).release(compId);

        // jak juz zwolnimy semafor to robimy swoj perform
        transfer.perform();
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
    }


    @Override
    public void execute(ComponentTransfer transfer) throws TransferException {
        DeviceId srcDevId, destDevId;
        ComponentId compId = transfer.getComponentId();
        srcDevId = transfer.getSourceDeviceId();
        destDevId = transfer.getDestinationDeviceId();
        TypeOfTransfer transferType = setTransferType(srcDevId, destDevId);

        try {
            try
            {
                semaphoreCheckTransfer.acquire();
                isTransferOK(transferType, compId, srcDevId, destDevId);
                checkIsCompBeingTransfered(compId, transferType);
            }
            finally
            {
                semaphoreCheckTransfer.release();
            }

            switch (transferType) {
                case ADD -> {

                    // we wait for place on destDevice
                    semaphoreQueueForDevSpaces.get(destDevId).acquire();

                    // Tylko jeden transfer w danej chwili moze miec przydzielane wolne miejsce
                    semaphoresDev.get(destDevId).acquire();
                    Integer idxOfMySpace =
                            deviceSpacesMap.get(destDevId).reserveSpace(compId);
                    semaphoresDev.get(destDevId).release();

                    // Sprawdzamy jakie miejsce otrzymalismy.
//                    DevSpacesTypes spaceIGot = idx_spaceType.second;
//                    switch (spaceIGot)
//                    {
//                        case FREE -> {
//                            case_ADD_free(transfer, idx_spaceType);
//                        }
//                        case OK_TO_RESERVE -> {
//                           case_ADD_ok_to_reserve(transfer, idx_spaceType);
//                        }
//                        // BRAK OCCUPIED - zalatwia nam to kolejka semaphoreQueueForDevSpaces
//                    }

                    do_ADDING(transfer,idxOfMySpace);

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
                case REMOVE -> {

                    do_REMOVING(transfer);

                    // po zrobieniu perform usuwamy z mapy sprawdzajacej czy komponent jest
                    // transferowany i z mapy komponentow nasz komponent
                    semaphoreCheckTransfer.acquire();

                    isCompBeingTransfered.remove(compId);
                    compInDevPlacement.remove(compId);

                    semaphoreCheckTransfer.release();
                }
                case TRANSFER -> {

                    semaphoreQueueForDevSpaces.get(destDevId).acquire();

                    semaphoresDev.get(destDevId).acquire();
                    Integer idxOfMySpace =
                            deviceSpacesMap.get(destDevId).reserveSpace(compId);
                    semaphoresDev.get(destDevId).release();

                    do_the_TRANSFER(transfer, idxOfMySpace);

                    // Po skonczeniu perform zmieniamy w mapie przyporzadkowanie componentow do device
                    // i ustawiamy ze komponent nie jest juz transferowany
                    semaphoreCheckTransfer.acquire();

                    isCompBeingTransfered.put(compId, false);
                    compInDevPlacement.put(compId, destDevId);

                    semaphoreCheckTransfer.release();
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
