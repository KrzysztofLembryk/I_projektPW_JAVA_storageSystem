package cp2023.solution;

import cp2023.base.ComponentId;
import cp2023.base.ComponentTransfer;
import cp2023.base.DeviceId;
import cp2023.exceptions.*;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

public class StorageSystem implements cp2023.base.StorageSystem {
    // deviceTotalSlots - stores info about how many components
    // device of given ID can store (deviceID --> capacity).
    private Map<DeviceId, Integer> deviceMap;

    // deviceSpacseMap - knows if there are free to use spaces on device or not
    private Map<DeviceId, DeviceSpaceHandler> deviceSpacesMap;

    private Map<DeviceId, Semaphore> semaphoresForDevices;
    private Map<DeviceId, Semaphore> semaphoresForDevSpaces;
    private final Semaphore sempahoreCheckTransfer = new Semaphore(1, true);

    // componentInDevicePlacement - remembers on which device given
    // component is stored.
    private Map<ComponentId, DeviceId> compInDevicePlacement;


     // isCompBeingTransfered - knows if given component is currently transfered,
     // since only one transfer for component can be commissioned
    private Map<ComponentId, Boolean> isCompBeingTransfered;

    // semaphoreIsCompBeingTransfered - allows only one thread to change value in
    // isCompBeingTransfered map, because it could happen that two threads simultaneously
    // would want to transfer given componenent and simultaneously would check
    // if given component was being transfered and at that very point it wasnt being transfered
    // so both threads would be able to transfer it
    private Map<ComponentId, Semaphore> semaphoreIsCompBeingTransfered;
    private Semaphore semaphoreForTransfer;

    private void initialiseSemaphoresForDevMap() throws InterruptedException
    {
        Map<DeviceId, Integer> nbrOfElemOnDev = new HashMap<>();

        for(DeviceId deviceID : deviceMap.keySet())
            nbrOfElemOnDev.put(deviceID, 0);

        // we count how many components is currently on each device
        for(DeviceId id : compInDevicePlacement.values())
        {
            Integer val = nbrOfElemOnDev.get(id);
            nbrOfElemOnDev.put(id, val + 1);
        }

        for(DeviceId id : semaphoresForDevSpaces.keySet())
            semaphoresForDevSpaces.get(id).acquire(nbrOfElemOnDev.get(id));

    }

    public StorageSystem(Map<DeviceId, Integer> deviceTotalSlots,
                                Map<ComponentId, DeviceId> componentPlacement)
    {
        this.deviceMap = deviceTotalSlots;//deviceTotalSlots; new HashMap<>()

//        for(DeviceId id : deviceTotalSlots.keySet())
//            deviceMap.put(id, deviceTotalSlots.get(id));

        this.compInDevicePlacement = componentPlacement;//componentPlacement; new HashMap<>()

//        for(ComponentId id : componentPlacement.keySet())
//            compInDevicePlacement.put(id, componentPlacement.get(id));

        isCompBeingTransfered = new HashMap<>();
        semaphoreIsCompBeingTransfered = new HashMap<>();
        semaphoreForTransfer = new Semaphore(1, true);

        deviceSpacesMap = new ConcurrentHashMap<>();
        semaphoresForDevices = new ConcurrentHashMap<>();
        semaphoresForDevSpaces = new ConcurrentHashMap<>();

        for(DeviceId devId : deviceTotalSlots.keySet())
        {
            deviceSpacesMap.put(devId, new DeviceSpaceHandler(deviceTotalSlots.get(devId)));
            semaphoresForDevices.put(devId, new Semaphore(1, false));
            semaphoresForDevSpaces.put(devId, new Semaphore(deviceTotalSlots.get(devId), false));
        }

        for(ComponentId compId : componentPlacement.keySet())
        {
            isCompBeingTransfered.put(compId, false);
            semaphoreIsCompBeingTransfered.put(compId, new Semaphore(1, true));
        }

//        System.out.println("printing devMap");
//        System.out.println(deviceMap);
//        System.out.println("printing compMap");
//        System.out.println(compInDevicePlacement);

        try
        {
            initialiseSemaphoresForDevMap();
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
            throws DeviceDoesNotExist, ComponentDoesNotNeedTransfer, ComponentAlreadyExists,
            ComponentDoesNotExist, IllegalTransferType
    {
        if(transferType == TypeOfTransfer.ADD)
        {
            if(!deviceMap.containsKey(destDevId))
                throw new DeviceDoesNotExist(destDevId);
            if(compInDevicePlacement.containsKey(compId))
                throw new ComponentAlreadyExists(compId);
        }
        else if(transferType == TypeOfTransfer.REMOVE)
        {
            if(!deviceMap.containsKey(srcDevId))
                throw new DeviceDoesNotExist(srcDevId);
            if(!compInDevicePlacement.containsKey(compId) || compInDevicePlacement.get(compId) != srcDevId)
                throw new ComponentDoesNotExist(compId, srcDevId);
            // component we want to remove is not in device of given ID
        }
        else if(transferType == TypeOfTransfer.TRANSFER)
        {
            if(!deviceMap.containsKey(srcDevId))
                throw new DeviceDoesNotExist(srcDevId);
            if(!deviceMap.containsKey(destDevId))
                throw new DeviceDoesNotExist(destDevId);
            if(!compInDevicePlacement.containsKey(compId) || compInDevicePlacement.get(compId) != srcDevId)
                throw new ComponentDoesNotExist(compId, srcDevId);
            if(compInDevicePlacement.get(compId) == destDevId)
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

        // zanim zrobimy transfer prepare musimy miec zarezerwowane miejsce
        DeviceId srcDevId, destDevId;
        ComponentId compId = transfer.getComponentId();
        srcDevId = transfer.getSourceDeviceId();
        destDevId = transfer.getDestinationDeviceId();
        TypeOfTransfer transferType = setTransferType(srcDevId, destDevId);

        try {

            System.out.println("transfer " + Thread.currentThread().getId() + ", before transfer check, "
                    + "destDevId: " + destDevId);
            sempahoreCheckTransfer.acquire();
            isTransferOK(transferType, compId, srcDevId, destDevId);
            checkIsCompBeingTransfered(compId, transferType);
            sempahoreCheckTransfer.release();
            System.out.println("transfer " + Thread.currentThread().getId() + ", before switch, "
                    + "destDevId: " + destDevId);
            switch (transferType) {
                case ADD:


                    // czekamy na wolne miejsce na naszym urzadzeniu
                    semaphoresForDevSpaces.get(destDevId).acquire();
                    transfer.prepare();
                    deviceSpacesMap.get(destDevId).reserveSpace();
                    // ustawiamy sie w kolejce do urzadzenia w ktorym rezerwujemy miejsce
                    semaphoresForDevices.get(destDevId).acquire();
                    // jesli bylo wolne miejsce i nikt juz nie korzysta z device, to tu wchodzimy i je rezerwujemy

                    // przed wykonaniem transferu musimy zapewnic ze nikt aktualnie
                    // nie sprawdza czy dany komponent istnieje w naszym systemie
                    // bo po wykonaniu transferu moze on juz nie istniec
                    sempahoreCheckTransfer.acquire();


                    isCompBeingTransfered.put(compId, false);
                    compInDevicePlacement.put(compId, destDevId);

                    sempahoreCheckTransfer.release();
                    transfer.perform();



                    semaphoresForDevices.get(destDevId).release();
                    break;
                case REMOVE:
                    // czekamy na dostep do urzadzenia, nie chcemy zeby dwa transfery jednoczesnie
                    // robily cos na urzadzeniu
                    semaphoresForDevices.get(srcDevId).acquire();

                    transfer.prepare();


                    semaphoresForDevSpaces.get(srcDevId).release();



                    sempahoreCheckTransfer.acquire();


                    isCompBeingTransfered.remove(compId);
                    compInDevicePlacement.remove(compId);

                    sempahoreCheckTransfer.release();
                    transfer.perform();

                    semaphoresForDevices.get(srcDevId).release();

                    //deviceSpacesMap.get(srcDevId).freeSpace();


                    break;
                case TRANSFER:

                    // czekamy na wolne miejsce na destDev
                    System.out.println("transfer " + Thread.currentThread().getId() + ", case: transfer "
                    + "before semDevSpaces Acquire, destDevId: " + destDevId);
                    semaphoresForDevSpaces.get(destDevId).acquire();

                    transfer.prepare();

                    //deviceSpacesMap.get(srcDevId).freeSpace();
                    semaphoresForDevSpaces.get(srcDevId).release();

                    //deviceSpacesMap.get(destDevId).reserveSpace();

                    semaphoresForDevices.get(destDevId).acquire();
                    // jak juz mamy miejsce na destDev, to czekamy na udostepnienie srcDev
                    //semaphoresForDevices.get(srcDevId).acquire();

                    sempahoreCheckTransfer.acquire();


                    isCompBeingTransfered.put(compId, false);
                    compInDevicePlacement.put(compId, destDevId);

                    sempahoreCheckTransfer.release();
                    transfer.perform();

                    semaphoresForDevices.get(destDevId).release();

                    break;

                case WRONG:
                    System.out.println("raczej nigdy tu nie wejdziemy :)");
                    break;
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
    protected Map<DeviceId, Integer> getDeviceSlotsMap()
    {
        return deviceMap;
    }
    protected Map<ComponentId, DeviceId> getCompPlacementMap()
    {
        return compInDevicePlacement;
    }
    protected Map<ComponentId, Boolean> getIsCompBeingTransfered()
    {
        return isCompBeingTransfered;
    }
    protected Semaphore getSemaphoreForTransfer()
    {
        return semaphoreForTransfer;
    }
}
