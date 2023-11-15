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
    private Map<DeviceId, Integer> deviceMap;
    // componentInDevicePlacement - remembers on which device given
    // component is stored.
    private Map<ComponentId, DeviceId> compInDevPlacement;
    private Map<ComponentId, Boolean> isCompBeingTransfered;
    // deviceSpacseMap - knows if there are free to use spaces on device or not
    private Map<DeviceId, DeviceSpaceHandler> deviceSpacesMap;

    private Map<DeviceId, Semaphore> semaphoresDev;
    private Map<DeviceId, Semaphore> semaphoresDevSpaces;
    private Semaphore semaphoreCheckTransfer;

    // CycleMap stores (srcDev, queue of destDev), meaning from srcDev we want to
    // transfer  to destDev
    private Map<DeviceId, Queue<Pair<DeviceId, ComponentId>>> cycleMap = new HashMap<>();


    private void initialiseSemaphoresForDevMap() throws InterruptedException
    {
        Map<DeviceId, Integer> nbrOfElemOnDev = new HashMap<>();

        for(DeviceId deviceID : deviceMap.keySet())
            nbrOfElemOnDev.put(deviceID, 0);

        // we count how many components is currently on each device
        for(DeviceId id : compInDevPlacement.values())
        {
            Integer val = nbrOfElemOnDev.get(id);
            nbrOfElemOnDev.put(id, val + 1);
        }

        for(DeviceId id : semaphoresDevSpaces.keySet())
            semaphoresDevSpaces.get(id).acquire(nbrOfElemOnDev.get(id));

    }

    public StorageSystem(Map<DeviceId, Integer> deviceTotalSlots,
                                Map<ComponentId, DeviceId> componentPlacement)
    {
        this.deviceMap = deviceTotalSlots;
        this.compInDevPlacement = componentPlacement;
        this.isCompBeingTransfered = new HashMap<>();
        this.deviceSpacesMap = new ConcurrentHashMap<>();

        semaphoreCheckTransfer = new Semaphore(1, true);
        semaphoresDev = new ConcurrentHashMap<>();
        semaphoresDevSpaces = new ConcurrentHashMap<>();

        for(DeviceId devId : deviceTotalSlots.keySet())
        {
            deviceSpacesMap.put(devId, new DeviceSpaceHandler(deviceTotalSlots.get(devId)));
            semaphoresDev.put(devId, new Semaphore(1, false));
            semaphoresDevSpaces.put(devId, new Semaphore(deviceTotalSlots.get(devId), false));
            cycleMap.put(devId, new LinkedList<>());
        }

        for(ComponentId compId : componentPlacement.keySet())
            isCompBeingTransfered.put(compId, false);

        try
        {
            initialiseSemaphoresForDevMap();
        }
        catch(InterruptedException e)
        {
            System.out.println(e);
        }
    }

    private void checkCycles(DeviceId srcId, DeviceId destId, ComponentId compId)
    {
        Pair<DeviceId, ComponentId> pair = new Pair<>(destId, compId);
        cycleMap.get(srcId).add(pair);

        if(cycleMap.get(srcId).equals(pair))
        {
            DeviceId currDev = pair.getFirst();
            while(true)
            {
                if(currDev == srcId)
                {
                    // cycle
                }
                else if(cycleMap.get(currDev).isEmpty())
                {
                    // no cycle
                    break;
                }
                else
                {
                    currDev = cycleMap.get(currDev).peek().getFirst();
                }
            }
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
            if(!deviceMap.containsKey(destDevId))
                throw new DeviceDoesNotExist(destDevId);
            if(compInDevPlacement.containsKey(compId))
                throw new ComponentAlreadyExists(compId);
        }
        else if(transferType == TypeOfTransfer.REMOVE)
        {
            if(!deviceMap.containsKey(srcDevId))
                throw new DeviceDoesNotExist(srcDevId);
            if(!compInDevPlacement.containsKey(compId) ||
                    !compInDevPlacement.get(compId).equals(srcDevId))
                throw new ComponentDoesNotExist(compId, srcDevId);
            // component we want to remove is not in device of given ID
        }
        else if(transferType == TypeOfTransfer.TRANSFER)
        {
            if(!deviceMap.containsKey(srcDevId))
                throw new DeviceDoesNotExist(srcDevId);
            if(!deviceMap.containsKey(destDevId))
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

                    // czekamy na wolne miejsce na naszym urzadzeniu
                    semaphoresDevSpaces.get(destDevId).acquire();

                    transfer.prepare();
                    // ustawiamy sie w kolejce do urzadzenia w ktorym zarezerwowalismy juz miejsce
                    semaphoresDev.get(destDevId).acquire();

                    transfer.perform();

                    // przed wprowadzeniem zmian dotyczacych componentu w naszym systemie
                    // musimy zapewnic ze nikt aktualnie nie sprawdza czy dany komponent istnieje
                    // w naszym systemie itp, bo po wykonaniu transferu moze on juz nie istniec.
                    semaphoreCheckTransfer.acquire();

                    isCompBeingTransfered.put(compId, false);
                    compInDevPlacement.put(compId, destDevId);

                    semaphoreCheckTransfer.release();

                    semaphoresDev.get(destDevId).release();
                }
                case REMOVE -> {

                    semaphoresDev.get(srcDevId).acquire();

                    transfer.prepare();

                    semaphoresDevSpaces.get(srcDevId).release();

                    transfer.perform();

                    semaphoreCheckTransfer.acquire();

                    isCompBeingTransfered.remove(compId);
                    compInDevPlacement.remove(compId);

                    semaphoreCheckTransfer.release();

                    semaphoresDev.get(srcDevId).release();
                }
                case TRANSFER -> {
                    semaphoresDevSpaces.get(destDevId).acquire();

                    transfer.prepare();

                    semaphoresDevSpaces.get(srcDevId).release();

                    semaphoresDev.get(destDevId).acquire();

                    transfer.perform();

                    semaphoreCheckTransfer.acquire();

                    isCompBeingTransfered.put(compId, false);
                    compInDevPlacement.put(compId, destDevId);

                    semaphoreCheckTransfer.release();

                    semaphoresDev.get(destDevId).release();
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
