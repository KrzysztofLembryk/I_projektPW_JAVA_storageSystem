package cp2023.solution;

import cp2023.base.ComponentId;
import cp2023.base.DeviceId;
import cp2023.exceptions.*;

import java.util.Map;

public class ComponentTransfer implements cp2023.base.ComponentTransfer {
    private final StorageSystem storageSystem;
    private final ComponentId compId;
    private final DeviceId srcDevId;
    private final DeviceId destDevId;
    private boolean isPrepared;
    private boolean isPerformed;
    private final Thread myThread;
    TypeOfTransfer transferType;


    private void isTransferOK() throws DeviceDoesNotExist, ComponentDoesNotNeedTransfer,
            ComponentAlreadyExists, ComponentDoesNotExist, IllegalTransferType
    {
        Map<DeviceId, Integer> devMap = storageSystem.getDeviceSlotsMap();
        Map<ComponentId, DeviceId> compInDevMap = storageSystem.getCompPlacementMap();

        if(transferType == TypeOfTransfer.ADD)
        {
            if(!devMap.containsKey(destDevId))
                throw new DeviceDoesNotExist(destDevId);
            if(compInDevMap.containsKey(compId))
                throw new ComponentAlreadyExists(compId);
        }
        else if(transferType == TypeOfTransfer.REMOVE)
        {
            if(!devMap.containsKey(srcDevId))
                throw new DeviceDoesNotExist(srcDevId);
            if(!compInDevMap.containsKey(compId) || compInDevMap.get(compId) != srcDevId)
                throw new ComponentDoesNotExist(compId, srcDevId);
            // component we want to remove is not in device of given ID
        }
        else if(transferType == TypeOfTransfer.TRANSFER)
        {
            if(!devMap.containsKey(srcDevId))
                throw new DeviceDoesNotExist(srcDevId);
            if(!devMap.containsKey(destDevId))
                throw new DeviceDoesNotExist(destDevId);
            if(!compInDevMap.containsKey(compId) || compInDevMap.get(compId) != srcDevId)
                throw new ComponentDoesNotExist(compId, srcDevId);
            if(compInDevMap.get(compId) == destDevId)
                throw new ComponentDoesNotNeedTransfer(compId, destDevId);
        }
        else
        {
            throw new IllegalTransferType(compId);
        }

    }
    private void checkTransfer() throws ComponentIsBeingOperatedOn, DeviceDoesNotExist, IllegalTransferType,
            ComponentDoesNotNeedTransfer, ComponentAlreadyExists, ComponentDoesNotExist, InterruptedException
    {
        // PROSTSZA WERSJA Z JEDNYM SEMAPHOREM - NA RAZIE NIE WIEM JAK Z WIELOMA ZROBIC
        storageSystem.getSemaphoreForTransfer().acquire();

        boolean compIdExist = storageSystem.getCompPlacementMap().containsKey(compId);

        // we check if compID is in map that stores whether given compID is being transfered
//            if(compIdExist)
//                storageSystem.getSemaphoreCompTransfered().get(compId).acquire();

        if(!compIdExist)
        {
            // if there is no such compID this means we either add new comp
            // so we have another semaphore for adding components,
            // we also put (compID, false) to map that checks
            //storageSystem.getSemaphoreForNewComp().acquire();

            storageSystem.getIsCompBeingTransfered().put(compId, false);
        }

        if(!storageSystem.getIsCompBeingTransfered().get(compId))
            storageSystem.getIsCompBeingTransfered().put(compId, true);
        else
            throw new ComponentIsBeingOperatedOn(compId);

        isTransferOK();

//            if(compIdExist)
//                storageSystem.getSemaphoreCompTransfered().get(compId).release();
//            else
//                storageSystem.getSemaphoreForNewComp().release();

        storageSystem.getSemaphoreForTransfer().release();
    }

    public ComponentTransfer(TypeOfTransfer type, StorageSystem storSys,
                             ComponentId compId, DeviceId srcDevID,
                             DeviceId destDevID)
            throws ComponentIsBeingOperatedOn, DeviceDoesNotExist, IllegalTransferType,
                ComponentDoesNotNeedTransfer, ComponentAlreadyExists, ComponentDoesNotExist
    {
        try
        {
            this.storageSystem = storSys;
            this.transferType = type;
            this.compId = compId;
            this.srcDevId = srcDevID;
            this.destDevId = destDevID;
            this.isPrepared = false;
            this.isPerformed = false;
            this.myThread = Thread.currentThread();

            checkTransfer();
        }
        catch(InterruptedException e)
        {
            throw new RuntimeException("panic: unexpected thread interruption");
        }

    }
    @Override
    public ComponentId getComponentId()
    {
        return  compId;
    }
    @Override
    public DeviceId getSourceDeviceId()
    {
        Map<ComponentId, DeviceId> compInDevicePlacement = storageSystem.getCompPlacementMap();

        // jesli komponentu nie ma w mapie komponent -> urzadzenie, to znaczy ze dodajemy
        // nowy komponent do systemu
        if(!compInDevicePlacement.containsKey(compId))
            return null;
        else
            return srcDevId;
    }
    @Override
    public DeviceId getDestinationDeviceId()
    {
        return destDevId;
    }
    @Override
    public void prepare()
    {

    }

    @Override
    public void perform()
    {

        // at the very end of perform method we need to inform system that
        // transfer at given component has ended, and thus we can create another transfer
        storageSystem.getIsCompBeingTransfered().put(compId, false);
    }
}
